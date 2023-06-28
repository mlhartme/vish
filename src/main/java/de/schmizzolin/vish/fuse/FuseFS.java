/*
 * Copyright Michael Hartmeier, https://github.com/mlhartme/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.schmizzolin.vish.fuse;

import foreign.fuse.fuse_args;
import foreign.fuse.fuse_fill_dir_t;
import foreign.fuse.fuse_h;
import foreign.fuse.fuse_operations;
import foreign.fuse.stat;

import java.io.File;
import java.io.PrintWriter;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public abstract class FuseFS {
    protected final PrintWriter log;

    protected FuseFS() {
        this(new PrintWriter(System.out));
    }

    protected FuseFS(PrintWriter log) {
        if (log == null) {
            throw new IllegalArgumentException();
        }
        this.log = log;
    }

    public String name() {
        return getClass().getSimpleName();
    }

    //-- file system methods

    public abstract void getAttr(String path, MemorySegment statAddr) throws ErrnoException;
    public abstract void readDir(String path, Consumer<String> filler) throws ErrnoException;

    /** @param offset into src file, not dest buffer */
    public abstract int read(String path, ByteBuffer buffer, int offset) throws ErrnoException;

    //-- mount/unmount

    /** Represents a running mount point */
    public static class Mount extends Thread implements AutoCloseable {
        private final Arena arena;
        private final MemorySegment fuse;
        private final MemorySegment mountpoint;
        private final MemorySegment channel;

        public Mount(Arena arena, MemorySegment fuse, MemorySegment mountpoint, MemorySegment channel) {
            this.arena = arena;
            this.fuse = fuse;
            this.mountpoint = mountpoint;
            this.channel = channel;
        }


        @Override
        public void run() {
            // TODO fuse_h.fuse_set_signal_handlers(session) != -1

            int err = fuse_h.fuse_loop(fuse);

            // TODO fuse_h.fuse_remove_signal_handlers(session);

            if (err != 0) {
                throw new IllegalStateException("loop returned " + err);
            }
        }

        @Override
        public void close() throws InterruptedException {
            fuse_h.fuse_exit(fuse);
            fuse_h.fuse_unmount(mountpoint, channel);
            join();

            fuse_h.fuse_destroy(fuse);
            arena.close();
        }
    }

    public Mount mount(File dest, boolean debug) {
        System.load("/usr/local/lib/libfuse.dylib");

        var arena = Arena.openShared();
        try {
            MemorySegment args = args(arena, dest, debug);
            MemorySegment channel;
            MemorySegment mountpoint = fuse_args.argv$get(args, fuse_args.argc$get(args) - 1);

            if (fuse_h.fuse_parse_cmdline(args, MemorySegment.NULL, MemorySegment.NULL, MemorySegment.NULL) ==-1) {
                throw new IllegalArgumentException("TODO");
            }
            channel = fuse_h.fuse_mount(mountpoint, args);
            if (channel == MemorySegment.NULL) {
                throw new IllegalArgumentException("mount failed");
            }
            MemorySegment operations = operations(arena);
            MemorySegment fuse = fuse_h.fuse_new(channel, args, operations, operations.byteSize(), MemorySegment.NULL);
            if (fuse == MemorySegment.NULL) {
                throw new IllegalArgumentException("new failed");
            }

            Mount result = new Mount(arena, fuse, mountpoint, channel);
            result.setDaemon(false); // TODO
            result.start();
            Thread.sleep(1000); // TODO: some proper check if filesystem is ready?
            return result;
        } catch (Exception e) {
            arena.close();
            throw new RuntimeException("TODO", e);
        }
    }

    private MemorySegment args(Arena arena, File dest, boolean debug) {
        List<String> args;

        args = new ArrayList<>();
        args.add(name());
        if (debug) {
            args.add("-d");
        }
        args.add("-f"); // foreground
        args.add("-s"); // single-threaded
        args.add(dest.getAbsolutePath());

        var argC = args.size();
        var argV = arena.allocateArray(ValueLayout.OfAddress.ADDRESS, argC);
        for (int i = 0; i < argC; i++) {
            argV.setAtIndex(ValueLayout.OfAddress.ADDRESS, i, arena.allocateUtf8String(args.get(i)));
        }
        MemorySegment result = fuse_args.allocate(arena);
        fuse_args.argc$set(result, argC);
        fuse_args.argv$set(result, argV);
        fuse_args.allocated$set(result, 0);
        return result;
    }

    private MemorySegment operations(Arena arena) {
        MemorySegment operations = fuse_operations.allocate(arena);
        fuse_operations.getattr$set(operations,
                fuse_operations.getattr.allocate(
                        (path, statPtr) -> {
                            try {
                                getAttr(path.getUtf8String(0), stat.ofAddress(statPtr, arena.scope()));
                                return 0;
                            } catch (ErrnoException e) {
                                if (e.getCause() != null) {
                                    e.getCause().printStackTrace(log);
                                }
                                return e.returnCode();
                            }
                        },
                        arena.scope()));

        fuse_operations.readdir$set(operations,
                fuse_operations.readdir.allocate(
                        (path, buffer, filler, offset, fileInfo) -> {
                            fuse_fill_dir_t f = fuse_fill_dir_t.ofAddress(filler, arena.scope());
                            Consumer<String> consumer = str -> f.apply(buffer, arena.allocateUtf8String(str), MemorySegment.NULL, 0);
                            try {
                                readDir(path.getUtf8String(0), consumer);
                                return 0;
                            } catch (ErrnoException e) {
                                if (e.getCause() != null) {
                                    e.getCause().printStackTrace(log);
                                }
                                return e.returnCode();
                            }
                        }, arena.scope()));

        fuse_operations.read$set(operations,
                fuse_operations.read.allocate(
                        (path, buffer, count, offset, info) -> {
                            ByteBuffer bb = MemorySegment.ofAddress(buffer.address(), count, arena.scope()).asByteBuffer();
                            try {
                                return read(path.getUtf8String(0), bb, toInt(offset));
                            } catch (ErrnoException e) {
                                if (e.getCause() != null) {
                                    e.getCause().printStackTrace(log);
                                }
                                return e.returnCode();
                            }
                        },
                        arena.scope()));

        return operations;
    }
    //--

    private static int toInt(long l) {
        if (l > Integer.MAX_VALUE || l < Integer.MIN_VALUE) {
            throw new IllegalStateException("" + l);
        }
        return (int) l;
    }
}

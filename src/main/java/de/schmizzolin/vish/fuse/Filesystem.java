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

import de.schmizzolin.vish.util.Stdlib;
import foreign.fuse.fuse_args;
import foreign.fuse.fuse_fill_dir_t;
import foreign.fuse.fuse_h;
import foreign.fuse.fuse_operations;
import foreign.fuse.stat;
import foreign.fuse.timespec;

import java.io.File;
import java.io.PrintWriter;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public abstract class Filesystem {
    protected final PrintWriter log;
    private final int euid;
    private final int egid;

    protected Filesystem() {
        this(new PrintWriter(System.out));
    }

    protected Filesystem(PrintWriter log) {
        if (log == null) {
            throw new IllegalArgumentException();
        }
        this.log = log;
        this.euid = Stdlib.geteuid();
        this.egid = Stdlib.getegid();

    }

    public String name() {
        return getClass().getSimpleName();
    }

    //-- file system methods

    public abstract Attr getAttr(String path) throws ErrnoException;
    public abstract void readDir(String path, Consumer<String> filler) throws ErrnoException;

    /** @param offset into src file, not dest buffer */
    public abstract int read(String path, ByteBuffer buffer, int offset) throws ErrnoException;

    //-- mount/unmount

    public Mount mount(File dest) {
        return mount(dest, false);
    }

    public Mount mount(File dest, boolean debug) {
        System.load("/usr/local/lib/libfuse.dylib");
        String destPath = dest.getAbsolutePath();
        var arena = Arena.ofShared();
        try {
            MemorySegment args = args(arena, destPath, debug);
            MemorySegment channel;
            MemorySegment mountpoint = fuse_args.argv$get(args).getAtIndex(ValueLayout.ADDRESS, fuse_args.argc$get(args) - 1);
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

            return new Mount(arena, destPath, fuse, channel);
        } catch (Exception e) {
            arena.close();
            throw new RuntimeException("TODO", e);
        }
    }

    private MemorySegment args(Arena arena, String dest, boolean debug) {
        List<String> args;

        args = new ArrayList<>();
        args.add(name());
        if (debug) {
            args.add("-d");
        }
        args.add("-f"); // foreground
        args.add("-s"); // single-threaded
        args.add(dest);

        var argC = args.size();
        var argV = arena.allocateArray(ValueLayout.ADDRESS, argC);
        for (int i = 0; i < argC; i++) {
            var adr = arena.allocateUtf8String(args.get(i));
            argV.setAtIndex(ValueLayout.ADDRESS, i, adr);
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
                                var attr = getAttr(path.getUtf8String(0));
                                var statAddr = stat.ofAddress(statPtr, Arena.global());
                                stat.st_mode$set(statAddr, attr.mode());
                                stat.st_size$set(statAddr, attr.size());
                                timespec.tv_sec$set(stat.st_mtimespec$slice(statAddr), attr.lastModified());
                                stat.st_uid$set(statAddr, euid);
                                stat.st_gid$set(statAddr, egid);

                                return 0;
                            } catch (ErrnoException e) {
                                if (e.getCause() != null) {
                                    e.getCause().printStackTrace(log);
                                }
                                return e.returnCode();
                            }
                        },
                        arena));

        fuse_operations.readdir$set(operations,
                fuse_operations.readdir.allocate(
                        (path, buffer, filler, offset, fileInfo) -> {
                            fuse_fill_dir_t f = fuse_fill_dir_t.ofAddress(filler, Arena.global());
                            try (Arena local = Arena.ofConfined()) {
                                Consumer<String> consumer = str -> f.apply(buffer, local.allocateUtf8String(str), MemorySegment.NULL, 0);
                                readDir(path.getUtf8String(0), consumer);
                                return 0;
                            } catch (ErrnoException e) {
                                if (e.getCause() != null) {
                                    e.getCause().printStackTrace(log);
                                }
                                return e.returnCode();
                            }
                        }, arena));

        fuse_operations.read$set(operations,
                fuse_operations.read.allocate(
                        (path, buffer, count, offset, info) -> {
                            ByteBuffer bb = buffer.reinterpret(count).asByteBuffer();
                            try {
                                return read(path.getUtf8String(0), bb, toInt(offset));
                            } catch (ErrnoException e) {
                                if (e.getCause() != null) {
                                    e.getCause().printStackTrace(log);
                                }
                                return e.returnCode();
                            }
                        },
                        arena));

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

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

import foreign.fuse.fuse_fill_dir_t;
import foreign.fuse.fuse_h;
import foreign.fuse.fuse_operations;
import foreign.fuse.stat;

import java.io.File;
import java.io.IOException;
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

    public void mount(File dest, boolean debug) {
        // see https://github.com/osxfuse/osxfuse/wiki/Mount-options
        List<String> args;

        args = new ArrayList<>();
        args.add(name());
        if (debug) {
            args.add("-d");
        }
        args.add("-f"); // foreground
        args.add("-s"); // single-threaded

        args.add(dest.getAbsolutePath());
        System.load("/usr/local/lib/libfuse.dylib");

        try (var arena = Arena.openShared()) {
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

            var argC = args.size();
            var argV = arena.allocateArray(ValueLayout.OfAddress.ADDRESS, argC);
            for (int i = 0; i < argC; i++) {
                argV.setAtIndex(ValueLayout.OfAddress.ADDRESS, i, arena.allocateUtf8String(args.get(i)));
            }
            fuse_h.fuse_main_real(argC, argV, operations, operations.byteSize(), MemorySegment.NULL);
        }
    }

    public Thread start(File dir, boolean debug) throws InterruptedException {
        Thread result;

        result = new Thread(() -> {
            try {
                mount(dir, debug);
            } catch (Throwable e) {
                e.printStackTrace();
                throw e;
            }
        });
        result.setDaemon(false); // TODO
        result.start();
        Thread.sleep(1000); // TODO: some proper check if filesystem is ready?
        return result;
    }

    public void umount(File dir) throws IOException {
        ProcessBuilder b;
        int result;

        b = new ProcessBuilder("umount", dir.getAbsolutePath());
        b.directory(dir.getParentFile());
        var p = b.start();
        try {
            result = p.waitFor();
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
        if (result != 0) {
            throw new IOException("umount failed: " + result);
        }
    }

    //--

    private static int toInt(long l) {
        if (l > Integer.MAX_VALUE || l < Integer.MIN_VALUE) {
            throw new IllegalStateException("" + l);
        }
        return (int) l;
    }
}

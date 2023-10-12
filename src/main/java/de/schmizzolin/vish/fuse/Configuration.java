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

/** Factory for Mounts */
public class Configuration {
    private PrintWriter log;
    private int umountTimeoutSeconds;
    private boolean debug;

    public Configuration() {
        log = new PrintWriter(System.out);
        umountTimeoutSeconds = 5;
        debug = false;
    }

    public Configuration log(PrintWriter newLog) {
        this.log = newLog;
        return this;
    }
    public Configuration umountTimeoutSeconds(int newUmountTimeoutSeconds) {
        this.umountTimeoutSeconds = newUmountTimeoutSeconds;
        return this;
    }

    public Configuration debug(boolean newDebug) {
        this.debug = newDebug;
        return this;
    }

    //--

    public Mount apply(Filesystem filesystem, File dest, String name) {
        System.load("/usr/local/lib/libfuse.dylib");
        String destPath = dest.getAbsolutePath();
        var arena = Arena.ofShared();
        try {
            MemorySegment args = args(name, arena, destPath, debug);
            MemorySegment channel;
            MemorySegment mountpoint = fuse_args.argv$get(args).getAtIndex(ValueLayout.ADDRESS, fuse_args.argc$get(args) - 1);
            if (fuse_h.fuse_parse_cmdline(args, MemorySegment.NULL, MemorySegment.NULL, MemorySegment.NULL) ==-1) {
                throw new IllegalArgumentException("TODO");
            }
            channel = fuse_h.fuse_mount(mountpoint, args);
            if (channel == MemorySegment.NULL) {
                throw new IllegalArgumentException("mount failed");
            }
            MemorySegment operations = createOperations(filesystem, arena, log);
            MemorySegment fuse = fuse_h.fuse_new(channel, args, operations, operations.byteSize(), MemorySegment.NULL);
            if (fuse == MemorySegment.NULL) {
                throw new IllegalArgumentException("new failed");
            }

            return new Mount(arena, destPath, fuse, channel, umountTimeoutSeconds);
        } catch (RuntimeException e) {
            arena.close();
            throw e;
        }
    }

    //--

    private static MemorySegment args(String name, Arena arena, String dest, boolean debug) {
        List<String> args;

        args = new ArrayList<>();
        args.add(name);
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

    private static MemorySegment createOperations(Filesystem fs, Arena arena, PrintWriter log) {
        int euid = Stdlib.geteuid();
        int egid = Stdlib.getegid();

        MemorySegment operations = fuse_operations.allocate(arena);
        fuse_operations.getattr$set(operations,
                fuse_operations.getattr.allocate(
                        (path, statPtr) -> {
                            try {
                                var attr = fs.getAttr(path.getUtf8String(0));
                                var statAddr = stat.ofAddress(statPtr, Arena.global() /* from native code, never free */);
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
                                fs.readDir(path.getUtf8String(0), consumer);
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
                                return fs.read(path.getUtf8String(0), bb, toInt(offset));
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

    private static int toInt(long l) {
        if (l > Integer.MAX_VALUE || l < Integer.MIN_VALUE) {
            throw new IllegalStateException("" + l);
        }
        return (int) l;
    }
}

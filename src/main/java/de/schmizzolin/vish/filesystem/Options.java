package de.schmizzolin.vish.filesystem;

import de.schmizzolin.vish.util.Stdlib;
import foreign.fuse.fuse_args;
import foreign.fuse.fuse_fill_dir_t;
import foreign.fuse.fuse_operations;
import foreign.fuse.stat;
import foreign.fuse.timespec;

import java.io.PrintWriter;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Factory for Mounts */
public class Options {
    private String name;
    private PrintWriter log;
    private int umountTimeoutSeconds;
    private boolean debug;

    public Options() {
        name = null;
        log = new PrintWriter(System.out);
        umountTimeoutSeconds = 5;
        debug = false;
    }

    public Options name(String newName) {
        this.name = newName;
        return this;
    }
    public String getName() {
        return name;
    }

    public Options log(PrintWriter newLog) {
        this.log = newLog;
        return this;
    }
    public Options umountTimeoutSeconds(int newUmountTimeoutSeconds) {
        this.umountTimeoutSeconds = newUmountTimeoutSeconds;
        return this;
    }
    public int getUmountTimeoutSeconds() {
        return umountTimeoutSeconds;
    }

    public Options debug(boolean newDebug) {
        this.debug = newDebug;
        return this;
    }

    //--

    public MemorySegment args(String forceName, Arena arena, String dest) {
        List<String> args;

        args = new ArrayList<>();
        args.add(forceName);
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

    public MemorySegment createOperations(Filesystem fs, Arena arena) {
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
                                report(e);
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
                                report(e);
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
                                report(e);
                                return e.returnCode();
                            }
                        },
                        arena));

        return operations;
    }

    private void report(ErrnoException e) {
        if (e.getCause() != null) {
            e.getCause().printStackTrace(log);
        }
    }

    private static int toInt(long l) {
        if (l > Integer.MAX_VALUE || l < Integer.MIN_VALUE) {
            throw new IllegalStateException("" + l);
        }
        return (int) l;
    }
}

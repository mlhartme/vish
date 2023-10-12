package de.schmizzolin.vish.fuse;

import de.schmizzolin.vish.util.Stdlib;
import foreign.fuse.fuse_args;
import foreign.fuse.fuse_fill_dir_t;
import foreign.fuse.fuse_h;
import foreign.fuse.fuse_operations;
import foreign.fuse.stat;
import foreign.fuse.timespec;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Represents a running mount point. Invoke close to unmount.
 * Created and returned by Filesystem.mount(), usually used in a try-with-resources block.
 * Also sets up a shutdown hook to call close when not already done.
 */
public class Mount extends Thread implements AutoCloseable {
    public static Mount create(Filesystem fs, PrintWriter log, File dest, String name, int umountTimeoutSeconds, boolean debug) {
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
            MemorySegment operations = createOperations(fs, arena, log);
            MemorySegment fuse = fuse_h.fuse_new(channel, args, operations, operations.byteSize(), MemorySegment.NULL);
            if (fuse == MemorySegment.NULL) {
                throw new IllegalArgumentException("new failed");
            }

            return new Mount(log, arena, destPath, fuse, channel, umountTimeoutSeconds);
        } catch (RuntimeException e) {
            arena.close();
            throw e;
        }
    }

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

    private final PrintWriter log;
    private final Arena arena;

    private final String mountpoint;

    private final MemorySegment fuse;
    private final MemorySegment channel;
    private final int umountTimeoutSeconds;

    public Mount(PrintWriter log, Arena arena, String mountpoint, MemorySegment fuse, MemorySegment channel, int umountTimeoutSeconds) {
        if (umountTimeoutSeconds <= 0) {
            throw new IllegalArgumentException("umount timeout: " + umountTimeoutSeconds);
        }
        if (log == null) {
            throw new IllegalArgumentException();
        }
        this.log = log;
        this.arena = arena;
        this.mountpoint = mountpoint;
        this.fuse = fuse;
        this.channel = channel;
        this.umountTimeoutSeconds = umountTimeoutSeconds;
        this.setDaemon(true); // do not keep running just because there's an active mount
        this.start();
    }


    @Override
    public void run() {
        AtomicBoolean inShutdown = new AtomicBoolean();
        inShutdown.set(false);
        // shutdown hook to invoke close. The is called on both normal vm temincation and on handle ctrl-c.
        Thread closeOnShutdown = new Thread(() -> {
            try {
                inShutdown.set(true);
                // System.err.println("closing on shutdown");
                close();
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException("close failed: " + e.getMessage(), e);
            }
        });
        Runtime.getRuntime().addShutdownHook(closeOnShutdown);
        int err = fuse_h.fuse_loop(fuse);
        if (!inShutdown.get()) {
            Runtime.getRuntime().removeShutdownHook(closeOnShutdown);
        }
        if (err != 0) {
            throw new IllegalStateException("loop returned " + err);
        }
    }

    /** Trigger umount, which in turn cause fuse loop to terminate */
    @Override
    public void close() throws InterruptedException, IOException {
        for (int i = 0; i < umountTimeoutSeconds * 20; i++) {
            // fuse_exit is not needed: when successful, umount terminates fuse_loop
            fuse_h.fuse_unmount(MemorySegment.NULL /* mount systems hangs if I specify the moint point here */, channel);
            if (!this.isAlive()) {
                fuse_h.fuse_destroy(fuse);
                arena.close();
                return;
            }
            Thread.sleep(50);
        }
        throw new IOException("umount timed out - device is busy: " + mountpoint);
    }
}

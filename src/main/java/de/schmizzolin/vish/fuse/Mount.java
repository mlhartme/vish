package de.schmizzolin.vish.fuse;

import foreign.fuse.fuse_h;

import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Represents a running mount point. Created by fuse filesystem, invoke close to unmount.
 * Uses a shutdown hook to make sure that close is called.
 */
public class Mount extends Thread implements AutoCloseable {
    private final Arena arena;

    private final String mountpoint;

    private final MemorySegment fuse;
    private final MemorySegment channel;

    public Mount(Arena arena, String mountpoint, MemorySegment fuse, MemorySegment channel) {
        this.arena = arena;
        this.mountpoint = mountpoint;
        this.fuse = fuse;
        this.channel = channel;
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
        for (int i = 0; i < 100; i++) {
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

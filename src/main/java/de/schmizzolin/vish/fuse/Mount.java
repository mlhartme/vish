package de.schmizzolin.vish.fuse;

import foreign.fuse.fuse_h;

import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

/**
 * Represents a running mount point. Created by fuse filesystem, invoke close to unmount.
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
    }


    @Override
    public void run() {
        int err = fuse_h.fuse_loop(fuse);
        if (err != 0) {
            throw new IllegalStateException("loop returned " + err);
        }
    }

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

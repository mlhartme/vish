package de.schmizzolin.vish.fuse;

import foreign.fuse.fuse_h;

import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

/**
 * Represents a running mount point
 */
public class Mount extends Thread implements AutoCloseable {
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
    public void close() throws InterruptedException, IOException {
        Thread.sleep(5000);

        for (int i = 0; i < 100; i++) {
            // fuse_exit is not needed: when successful, umount terminates fuse_loop
            fuse_h.fuse_unmount(MemorySegment.NULL /* mount systems hangs if I specify the moint point here */, channel);
            if (!this.isAlive()) {
                fuse_h.fuse_destroy(fuse);
                arena.close();
                return;
            }
            Thread.sleep(100);
        }
        throw new IOException("umount timed out - device is busy");
    }
}

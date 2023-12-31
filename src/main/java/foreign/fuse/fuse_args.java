// Generated by jextract

package foreign.fuse;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;
/**
 * {@snippet :
 * struct fuse_args {
 *     int argc;
 *     char** argv;
 *     int allocated;
 * };
 * }
 */
public class fuse_args {

    public static MemoryLayout $LAYOUT() {
        return constants$0.const$0;
    }
    public static VarHandle argc$VH() {
        return constants$0.const$1;
    }
    /**
     * Getter for field:
     * {@snippet :
     * int argc;
     * }
     */
    public static int argc$get(MemorySegment seg) {
        return (int)constants$0.const$1.get(seg);
    }
    /**
     * Setter for field:
     * {@snippet :
     * int argc;
     * }
     */
    public static void argc$set(MemorySegment seg, int x) {
        constants$0.const$1.set(seg, x);
    }
    public static int argc$get(MemorySegment seg, long index) {
        return (int)constants$0.const$1.get(seg.asSlice(index*sizeof()));
    }
    public static void argc$set(MemorySegment seg, long index, int x) {
        constants$0.const$1.set(seg.asSlice(index*sizeof()), x);
    }
    public static VarHandle argv$VH() {
        return constants$0.const$2;
    }
    /**
     * Getter for field:
     * {@snippet :
     * char** argv;
     * }
     */
    public static MemorySegment argv$get(MemorySegment seg) {
        return (java.lang.foreign.MemorySegment)constants$0.const$2.get(seg);
    }
    /**
     * Setter for field:
     * {@snippet :
     * char** argv;
     * }
     */
    public static void argv$set(MemorySegment seg, MemorySegment x) {
        constants$0.const$2.set(seg, x);
    }
    public static MemorySegment argv$get(MemorySegment seg, long index) {
        return (java.lang.foreign.MemorySegment)constants$0.const$2.get(seg.asSlice(index*sizeof()));
    }
    public static void argv$set(MemorySegment seg, long index, MemorySegment x) {
        constants$0.const$2.set(seg.asSlice(index*sizeof()), x);
    }
    public static VarHandle allocated$VH() {
        return constants$0.const$3;
    }
    /**
     * Getter for field:
     * {@snippet :
     * int allocated;
     * }
     */
    public static int allocated$get(MemorySegment seg) {
        return (int)constants$0.const$3.get(seg);
    }
    /**
     * Setter for field:
     * {@snippet :
     * int allocated;
     * }
     */
    public static void allocated$set(MemorySegment seg, int x) {
        constants$0.const$3.set(seg, x);
    }
    public static int allocated$get(MemorySegment seg, long index) {
        return (int)constants$0.const$3.get(seg.asSlice(index*sizeof()));
    }
    public static void allocated$set(MemorySegment seg, long index, int x) {
        constants$0.const$3.set(seg.asSlice(index*sizeof()), x);
    }
    public static long sizeof() { return $LAYOUT().byteSize(); }
    public static MemorySegment allocate(SegmentAllocator allocator) { return allocator.allocate($LAYOUT()); }
    public static MemorySegment allocateArray(long len, SegmentAllocator allocator) {
        return allocator.allocate(MemoryLayout.sequenceLayout(len, $LAYOUT()));
    }
    public static MemorySegment ofAddress(MemorySegment addr, Arena arena) { return RuntimeHelper.asArray(addr, $LAYOUT(), 1, arena); }
}



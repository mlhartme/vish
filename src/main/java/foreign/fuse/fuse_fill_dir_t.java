// Generated by jextract

package foreign.fuse;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;
/**
 * {@snippet :
 * int (*fuse_fill_dir_t)(void* buf,char* name,struct stat* stbuf,long long off);
 * }
 */
public interface fuse_fill_dir_t {

    int apply(java.lang.foreign.MemorySegment buf, java.lang.foreign.MemorySegment name, java.lang.foreign.MemorySegment stbuf, long off);
    static MemorySegment allocate(fuse_fill_dir_t fi, Arena scope) {
        return RuntimeHelper.upcallStub(constants$5.const$1, fi, constants$5.const$0, scope);
    }
    static fuse_fill_dir_t ofAddress(MemorySegment addr, Arena arena) {
        MemorySegment symbol = addr.reinterpret(arena, null);
        return (java.lang.foreign.MemorySegment _buf, java.lang.foreign.MemorySegment _name, java.lang.foreign.MemorySegment _stbuf, long _off) -> {
            try {
                return (int)constants$5.const$2.invokeExact(symbol, _buf, _name, _stbuf, _off);
            } catch (Throwable ex$) {
                throw new AssertionError("should not reach here", ex$);
            }
        };
    }
}


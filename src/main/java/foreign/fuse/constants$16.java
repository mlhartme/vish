// Generated by jextract

package foreign.fuse;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;
final class constants$16 {

    // Suppresses default constructor, ensuring non-instantiability.
    private constants$16() {}
    static final MethodHandle const$0 = RuntimeHelper.upcallHandle(fuse_operations.setxattr.class, "apply", constants$15.const$5);
    static final MethodHandle const$1 = RuntimeHelper.downcallHandle(
        constants$15.const$5
    );
    static final VarHandle const$2 = constants$5.const$3.varHandle(MemoryLayout.PathElement.groupElement("setxattr"));
    static final FunctionDescriptor const$3 = FunctionDescriptor.of(JAVA_INT,
        RuntimeHelper.POINTER,
        RuntimeHelper.POINTER,
        RuntimeHelper.POINTER,
        JAVA_LONG,
        JAVA_INT
    );
    static final MethodHandle const$4 = RuntimeHelper.upcallHandle(fuse_operations.getxattr.class, "apply", constants$16.const$3);
    static final MethodHandle const$5 = RuntimeHelper.downcallHandle(
        constants$16.const$3
    );
}



// Generated by jextract

package foreign.fuse;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;
final class constants$23 {

    // Suppresses default constructor, ensuring non-instantiability.
    private constants$23() {}
    static final FunctionDescriptor const$0 = FunctionDescriptor.of(JAVA_INT,
        RuntimeHelper.POINTER,
        RuntimeHelper.POINTER,
        JAVA_INT,
        RuntimeHelper.POINTER
    );
    static final MethodHandle const$1 = RuntimeHelper.upcallHandle(fuse_operations.lock.class, "apply", constants$23.const$0);
    static final MethodHandle const$2 = RuntimeHelper.downcallHandle(
        constants$23.const$0
    );
    static final VarHandle const$3 = constants$5.const$3.varHandle(MemoryLayout.PathElement.groupElement("lock"));
    static final MethodHandle const$4 = RuntimeHelper.upcallHandle(fuse_operations.utimens.class, "apply", constants$5.const$4);
    static final VarHandle const$5 = constants$5.const$3.varHandle(MemoryLayout.PathElement.groupElement("utimens"));
}



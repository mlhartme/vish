// Generated by jextract

package foreign.fuse;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;
final class constants$22 {

    // Suppresses default constructor, ensuring non-instantiability.
    private constants$22() {}
    static final FunctionDescriptor const$0 = FunctionDescriptor.of(JAVA_INT,
        RuntimeHelper.POINTER,
        JAVA_LONG,
        RuntimeHelper.POINTER
    );
    static final MethodHandle const$1 = RuntimeHelper.upcallHandle(fuse_operations.ftruncate.class, "apply", constants$22.const$0);
    static final MethodHandle const$2 = RuntimeHelper.downcallHandle(
        constants$22.const$0
    );
    static final VarHandle const$3 = constants$5.const$3.varHandle(MemoryLayout.PathElement.groupElement("ftruncate"));
    static final MethodHandle const$4 = RuntimeHelper.upcallHandle(fuse_operations.fgetattr.class, "apply", constants$7.const$0);
    static final VarHandle const$5 = constants$5.const$3.varHandle(MemoryLayout.PathElement.groupElement("fgetattr"));
}


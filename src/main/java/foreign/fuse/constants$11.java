// Generated by jextract

package foreign.fuse;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;
final class constants$11 {

    // Suppresses default constructor, ensuring non-instantiability.
    private constants$11() {}
    static final VarHandle const$0 = constants$5.const$3.varHandle(MemoryLayout.PathElement.groupElement("chmod"));
    static final FunctionDescriptor const$1 = FunctionDescriptor.of(JAVA_INT,
        RuntimeHelper.POINTER,
        JAVA_INT,
        JAVA_INT
    );
    static final MethodHandle const$2 = RuntimeHelper.upcallHandle(fuse_operations.chown.class, "apply", constants$11.const$1);
    static final MethodHandle const$3 = RuntimeHelper.downcallHandle(
        constants$11.const$1
    );
    static final VarHandle const$4 = constants$5.const$3.varHandle(MemoryLayout.PathElement.groupElement("chown"));
    static final FunctionDescriptor const$5 = FunctionDescriptor.of(JAVA_INT,
        RuntimeHelper.POINTER,
        JAVA_LONG
    );
}



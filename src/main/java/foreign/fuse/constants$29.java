// Generated by jextract

package foreign.fuse;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;
final class constants$29 {

    // Suppresses default constructor, ensuring non-instantiability.
    private constants$29() {}
    static final VarHandle const$0 = constants$5.const$3.varHandle(MemoryLayout.PathElement.groupElement("renamex"));
    static final MethodHandle const$1 = RuntimeHelper.upcallHandle(fuse_operations.statfs_x.class, "apply", constants$5.const$4);
    static final VarHandle const$2 = constants$5.const$3.varHandle(MemoryLayout.PathElement.groupElement("statfs_x"));
    static final MethodHandle const$3 = RuntimeHelper.upcallHandle(fuse_operations.setvolname.class, "apply", constants$4.const$2);
    static final VarHandle const$4 = constants$5.const$3.varHandle(MemoryLayout.PathElement.groupElement("setvolname"));
    static final MethodHandle const$5 = RuntimeHelper.upcallHandle(fuse_operations.exchange.class, "apply", constants$6.const$2);
}


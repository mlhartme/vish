package de.schmizzolin.vish.talk;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;


public class MemoryDemo {
    private static final Linker LINKER = Linker.nativeLinker();

    // Header
    //   struct passwd *getpwnam(char *);
    //   struct passwd {
    //        char    *name;
    //        char    *passwd;
    //        int     uid;
    //        …
    //   };
    public static void main(String[] args) throws Throwable {
        printpid();
        MemoryLayout layout = MemoryLayout.structLayout(
                ValueLayout.ADDRESS.withName("name"),
                ValueLayout.ADDRESS.withName("passwd"),
                ValueLayout.JAVA_INT.withName("uid")
        );
        MethodHandle handle = downcall("getpwnam", ValueLayout.ADDRESS.withTargetLayout(layout), ValueLayout.ADDRESS);
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment addr = (MemorySegment) handle.invoke(arena.allocateUtf8String("mhm"));
            VarHandle field = layout.varHandle(MemoryLayout.PathElement.groupElement("uid"));
            System.out.println("uid: " + field.get(addr));
        }
    }

    public static void printpid() throws Throwable {
        System.out.println("pid: " + downcall("getpid", ValueLayout.JAVA_INT).invoke());
    }

    private static MethodHandle downcall(String name, MemoryLayout result, MemoryLayout... args) {
        MemorySegment addr = LINKER.defaultLookup().find(name).get();
        FunctionDescriptor desc = FunctionDescriptor.of(result, args);
        return LINKER.downcallHandle(addr, desc);
    }
}


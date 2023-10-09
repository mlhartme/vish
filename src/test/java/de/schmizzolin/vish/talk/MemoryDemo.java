package de.schmizzolin.vish.talk;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;


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


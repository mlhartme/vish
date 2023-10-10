package de.schmizzolin.vish.talk;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;


public class UpDemo {
    private static final Linker LINKER = Linker.nativeLinker();

    // Header
    //   typedef void (*handler_t)(int);                  // pointer to handler: void handler(int)
    //   handler_t signal(int signum, handler_t action);  // register handler
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


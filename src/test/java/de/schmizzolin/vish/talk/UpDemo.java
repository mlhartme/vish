package de.schmizzolin.vish.talk;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;


public class UpDemo {
    private static final Linker LINKER = Linker.nativeLinker();

    public static void signalHandler(int signum) {
        System.out.println("got signal " + signum);
    }

    // Header
    //   typedef void (*handler_t)(int);                  // pointer to handler: void handler(int)
    //   handler_t signal(int signum, handler_t action);  // register handler
    public static void main(String[] args) throws Throwable {
        printpid();
        FunctionDescriptor desc = FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT);
        MethodHandle target = MethodHandles.lookup().findStatic(UpDemo.class, "signalHandler",
                desc.toMethodType());
        MemorySegment stub = LINKER.upcallStub(target, desc, Arena.global());
        downcall("signal", ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.ADDRESS).invoke(30, stub);
        System.out.println("waiting ...");
        Thread.sleep(20000);
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


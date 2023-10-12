package de.schmizzolin.vish.talk;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

public class DownDemo {
    private static Linker LINKER = Linker.nativeLinker();

    // Header file
    //    int getpid();
    public static void main(String[] args) throws Throwable {
        MemorySegment addr = LINKER.defaultLookup().find("getpid").get();
        MethodHandle handle = LINKER.downcallHandle(addr, FunctionDescriptor.of(ValueLayout.JAVA_INT));
        System.out.println("pid: " + handle.invoke());
    }
}


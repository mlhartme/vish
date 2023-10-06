package de.schmizzolin.vish.talk;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

public class Demo {
    private static final Linker LINKER = Linker.nativeLinker();

    public static void main(String[] args) throws Throwable {
        MethodHandle getpid = downcall("getpid", desc(ValueLayout.JAVA_INT));
        System.out.println("pid: " + (int) getpid.invoke());

        printUid(System.getProperty("user.name"));
        MemorySegment stub = upcall("printSignal", desc(null, ValueLayout.JAVA_INT));
        downcall("signal", desc(ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.ADDRESS)).invoke(30, stub);
        System.out.println("waiting ...");
        Thread.sleep(20000);
    }

    private static MemorySegment upcall(String name, FunctionDescriptor desc) throws NoSuchMethodException, IllegalAccessException {
        MethodHandle handle = MethodHandles.lookup().findStatic(Demo.class, name, desc.toMethodType());
        return LINKER.upcallStub(handle, desc, Arena.global());
    }


    private static MethodHandle downcall(String name, FunctionDescriptor desc) {
        MemorySegment addr = LINKER.defaultLookup().find(name).get();
        return LINKER.downcallHandle(addr, desc);
    }

    public static FunctionDescriptor desc(ValueLayout result, ValueLayout... args) {
        return result == null ? FunctionDescriptor.ofVoid(args) : FunctionDescriptor.of(result, args);
    }

    public static void printSignal(int sig) {
        System.out.println("got signal " + sig);
    }

    public static void printUid(String name) throws Throwable {
        MemoryLayout layout = MemoryLayout.structLayout(
                ValueLayout.ADDRESS.withName("name"),
                ValueLayout.ADDRESS.withName("passwd"),
                ValueLayout.JAVA_INT.withName("uid")
        );
        var getpwnam = downcall("getpwnam", desc(ValueLayout.ADDRESS.withTargetLayout(layout), ValueLayout.ADDRESS));
        var struct = (MemorySegment) getpwnam.invoke(Arena.ofAuto().allocateUtf8String(System.getProperty("user.name")));
        var field = layout.varHandle(MemoryLayout.PathElement.groupElement("uid"));
        var uid = (int) field.get(struct);
        System.out.println("uid: " + uid);
    }
}


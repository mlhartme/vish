package de.schmizzolin.vish.talk;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

/**
 * Related Headers (simplified, shortened)
 * <li>
 *     <code>int getpid();</code>
 *     <see><a href="https://www.gnu.org/software/libc/manual/html_node/Process-Identification.html">Docs</a></see>
 * </li>
 * <li>
 *     <code>
 *         void handler (int signum);
 *         handler_t signal (int signum, handler_t action);
 *     </code>
 *     <see><a href="https://www.gnu.org/software/libc/manual/html_node/Basic-Signal-Handling.html">Docs</a></see>
 * </li>
 * <li>
 *     <code>
 *         struct passwd {
 *           char *name;
 *           char *passwd;
 *           int uid;
 *           int gid;
 *         }
 *     </code>
 *     <see><a href="https://www.gnu.org/software/libc/manual/html_node/Lookup-User.html">Docs</a></see>
 * </li>
 */
public class Demo {
    private static final Linker LINKER = Linker.nativeLinker();

    public static void main(String[] args) throws Throwable {
        printPid();
        printUid(System.getProperty("user.name"));
        printSignals();
    }

    //-- downcall example

    public static void printPid() throws Throwable {
        MethodHandle getpid = downcall("getpid", desc(ValueLayout.JAVA_INT));
        System.out.println("pid: " + (int) getpid.invoke());
    }

    //-- upcall example

    public static void printSignals() throws Throwable {
        MemorySegment stub = upcall("signalHandler", desc(null, ValueLayout.JAVA_INT));
        downcall("signal", desc(ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.ADDRESS)).invoke(30, stub);
        System.out.println("waiting ...");
        Thread.sleep(20000);
    }

    public static void signalHandler(int sig) {
        System.out.println("got signal " + sig);
    }

    //-- memory example

    public static void printUid(String name) throws Throwable {
        MemoryLayout layout = MemoryLayout.structLayout(
                ValueLayout.ADDRESS.withName("name"),
                ValueLayout.ADDRESS.withName("passwd"),
                ValueLayout.JAVA_INT.withName("uid")
        );
        try (Arena arena = Arena.ofConfined()) {
            var getpwnam = downcall("getpwnam", desc(ValueLayout.ADDRESS.withTargetLayout(layout), ValueLayout.ADDRESS));
            var cString = arena.allocateUtf8String(name);
            var struct = (MemorySegment) getpwnam.invoke(cString);
            var field = layout.varHandle(MemoryLayout.PathElement.groupElement("uid"));
            var uid = (int) field.get(struct);
            System.out.println("uid: " + uid);
        }
    }

    //-- helper methods

    private static MethodHandle downcall(String name, FunctionDescriptor desc) {
        MemorySegment addr = LINKER.defaultLookup().find(name).get();
        return LINKER.downcallHandle(addr, desc);
    }

    private static MemorySegment upcall(String name, FunctionDescriptor desc) throws NoSuchMethodException, IllegalAccessException {
        MethodHandle handle = MethodHandles.lookup().findStatic(Demo.class, name, desc.toMethodType());
        return LINKER.upcallStub(handle, desc, Arena.global());
    }

    private static FunctionDescriptor desc(ValueLayout result, ValueLayout... args) {
        return result == null ? FunctionDescriptor.ofVoid(args) : FunctionDescriptor.of(result, args);
    }

}


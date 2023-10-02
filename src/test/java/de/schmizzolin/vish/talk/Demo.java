/*
 * Copyright Michael Hartmeier, https://github.com/mlhartme/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.schmizzolin.vish.talk;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

/**
 * https://www.gnu.org/software/libc/manual/html_node/Process-Identification.html
 *
 * Essentially <code>int getpid();</code>
 */
public class Demo {
    public static void main(String[] args) throws Throwable {
        printPid();
        printPwnam("mhm");

        printSignal30();
    }

    private static final Linker LINKER = Linker.nativeLinker();

    public static MethodHandle downcall(String name, MemoryLayout result, MemoryLayout ... args) {
        MemorySegment addr = LINKER.defaultLookup().find(name).get();
        return LINKER.downcallHandle(addr, FunctionDescriptor.of(result, args));
    }

    public static void printPid() throws Throwable {
        MethodHandle getpid = downcall("getpid", JAVA_INT);
        System.out.println("pid: " + getpid.invoke());
    }

    public static void printPwnam(String name) throws Throwable {
        ValueLayout C_STRING = ADDRESS.withTargetLayout(MemoryLayout.sequenceLayout(JAVA_BYTE));
        MemoryLayout layout = MemoryLayout.structLayout(
                C_STRING.withName("name"),
                C_STRING.withName("passwd"),
                JAVA_INT.withName("uid"),
                JAVA_INT.withName("gid"),
                JAVA_LONG.withName("change"),
                C_STRING.withName("class"),
                C_STRING.withName("info"),
                C_STRING.withName("dir"),
                C_STRING.withName("shell")
        );
        MethodHandle getpwnam = downcall("getpwnam", ADDRESS.withTargetLayout(layout), C_STRING);
        MemorySegment struct = (MemorySegment) getpwnam.invoke(Arena.ofAuto().allocateUtf8String(name));
        if (struct.address() == 0) {
            System.out.println("not found: " + name);
        } else {
            MemorySegment info = (MemorySegment) layout.varHandle(MemoryLayout.PathElement.groupElement("info")).get(struct);
            System.out.println(name + " info: " + info.getUtf8String(0));
        }
    }

    //-- upcall

    public static void printSignal30() throws Throwable {
        MemorySegment handler = upcall(Demo.class, "signalPrinter", JAVA_INT);

        MethodHandle signal = downcall("signal", ADDRESS, JAVA_INT, ADDRESS);
        signal.invoke(30, handler);
        Thread.sleep(10000);
    }

    public static void signalPrinter(int signal) {
        System.out.println("got signal: " + signal);
    }

    public static MemorySegment upcall(Class<?> clazz, String method, MemoryLayout... args) throws NoSuchMethodException, IllegalAccessException {
        FunctionDescriptor descriptor = FunctionDescriptor.ofVoid(args);
        MethodHandle handle = MethodHandles.lookup().findStatic(clazz, method, descriptor.toMethodType());
        return LINKER.upcallStub(handle, descriptor, Arena.global());
    }
}

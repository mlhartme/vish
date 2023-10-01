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
package de.schmizzolin.vish.signal;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.ADDRESS;

/**
 * https://www.gnu.org/software/libc/manual/html_node/Advanced-Signal-Handling.html
 */
public class Signal {
    static final Linker LINKER = Linker.nativeLinker();
    static final SymbolLookup LOOKUP = LINKER.defaultLookup();

    public static void main(String[] args) throws Throwable {
        Getpid.printPid();

        try (Arena arena = Arena.ofConfined()) {
            MemorySegment handler = allocateHandlerStub(arena);
            MemorySegment struct = allocateStruct(arena, handler, 0, 0);
            sigaction(30, struct, MemorySegment.NULL);

            Thread.sleep(10000);
        }
    }

    //-- upcall

    static void handleSignal(int signal) {
        System.out.println("got signal: " + signal);
    }

    static MemorySegment allocateHandlerStub(Arena arena) {
        try {
            FunctionDescriptor descriptor = FunctionDescriptor.ofVoid(JAVA_INT);
            MethodHandle handle = MethodHandles.lookup().findStatic(Signal.class, "handleSignal", descriptor.toMethodType());
            return LINKER.upcallStub(handle, descriptor, arena);
        } catch (NoSuchMethodException | IllegalAccessException ex) {
            throw new AssertionError(ex);
        }
    }

    //-- struct

    static MemorySegment allocateStruct(Arena arena, MemorySegment handler, int mask, int flags) {
        StructLayout layout = MemoryLayout.structLayout(
                ADDRESS.withName("sa_handler"),
                JAVA_INT.withName("sa_mask"),
                JAVA_INT.withName("sa_flags")
        ).withName("sigaction");

        MemorySegment struct = arena.allocate(layout);
        layout.varHandle(element("sa_handler")).set(struct, handler);
        layout.varHandle(element("sa_mask")).set(struct, mask);
        layout.varHandle(element("sa_flags")).set(struct, flags);
        return struct;
    }
    static MemoryLayout.PathElement element(String name) {
        return MemoryLayout.PathElement.groupElement(name);
    }

    //-- downcall

    static int sigaction(int signum, MemorySegment act, MemorySegment oldact) throws Throwable {
        FunctionDescriptor descriptor = FunctionDescriptor.of(JAVA_INT, JAVA_INT, ADDRESS, ADDRESS);
        MethodHandle handle = LINKER.downcallHandle(LOOKUP.find("sigaction").get(), descriptor);
        return (int) handle.invokeExact(signum, act, oldact);
    }
}

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

import java.lang.foreign.AddressLayout;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;

/**
 * https://www.gnu.org/software/libc/manual/html_node/Advanced-Signal-Handling.html
 */
public class Signal {
    private static final MethodHandles.Lookup MH_LOOKUP = MethodHandles.lookup();
    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup SYMBOL_LOOKUP = LINKER.defaultLookup();
    private static final AddressLayout POINTER = ValueLayout.ADDRESS.withTargetLayout(MemoryLayout.sequenceLayout(JAVA_BYTE));


    public static void main(String[] args) throws Throwable {
        try (Arena arena = Arena.ofConfined()) {
            Getpid.printPid();

            MemorySegment handler = allocateHandlerStub(arena);
            MemorySegment struct = allocateStruct(handler, arena);
            sigaction(30, struct, MemorySegment.NULL);

            Thread.sleep(10000);
        }
    }

    //-- upcall

    public static void handleSignal(int signal) {
        System.out.println("got signal: " + signal);
    }

    static MemorySegment allocateHandlerStub(Arena arena) {
        try {
            FunctionDescriptor descriptor = FunctionDescriptor.ofVoid(JAVA_INT);
            MethodHandle handle = MH_LOOKUP.findStatic(Signal.class, "handleSignal", descriptor.toMethodType());
            return LINKER.upcallStub(handle, descriptor, arena);
        } catch (NoSuchMethodException | IllegalAccessException ex) {
            throw new AssertionError(ex);
        }
    }

    //-- struct

    private static MemorySegment allocateStruct(MemorySegment handler, Arena arena) {
        StructLayout layout = MemoryLayout.structLayout(
                POINTER.withName("sa_handler"),
                JAVA_INT.withName("sa_mask"),
                JAVA_INT.withName("sa_flags")
        ).withName("sigaction");

        MemorySegment struct = arena.allocate(layout);
        layout.varHandle(element("sa_handler")).set(struct, handler);
        layout.varHandle(element("sa_mask")).set(struct, 0);
        layout.varHandle(element("sa_flags")).set(struct, 0);
        return struct;
    }
    private static MemoryLayout.PathElement element(String name) {
        return MemoryLayout.PathElement.groupElement(name);
    }

    //-- downcall

    static final FunctionDescriptor sigactionDescriptor = FunctionDescriptor.of(JAVA_INT, JAVA_INT, POINTER, POINTER);
    private static final MethodHandle sigactionHandle = SYMBOL_LOOKUP.find("sigaction")
            .map(addr -> LINKER.downcallHandle(addr, sigactionDescriptor)).orElseThrow();

    public static int sigaction(int signum, MemorySegment act, MemorySegment oldact) {
        try {
            return (int) sigactionHandle.invokeExact(signum, act, oldact);
        } catch (Throwable e) {
            throw new AssertionError("should not reach here", e);
        }
    }
}

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

import de.schmizzolin.vish.examples.Downcall;

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
import java.lang.invoke.VarHandle;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;

public class Signal {
    private static final MethodHandles.Lookup MH_LOOKUP = MethodHandles.lookup();
    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup SYMBOL_LOOKUP = LINKER.defaultLookup();
    private static final AddressLayout POINTER = ValueLayout.ADDRESS.withTargetLayout(MemoryLayout.sequenceLayout(JAVA_BYTE));


    public static void main(String[] args) throws Throwable {
        Downcall.printPid();

        MemorySegment struct = Arena.global().allocate(sigactionLayout);
        sigaction.sa_flagsHandle.set(struct, 0);
        sigaction.sa_maskHandle.set(struct, 0);
        sigaction.sa_handlerHandle.set(struct, sigactionAllocate(Arena.global()));
        System.out.println("before");
        sigaction(30, struct, MemorySegment.NULL);
        System.out.println("after");
        Thread.sleep(10000);
    }


    public static void handleSignal(int signal) {
        System.out.println("got signal: " + signal);
    }
    //--

    static final FunctionDescriptor sigactionDescriptor = FunctionDescriptor.of(JAVA_INT,
            JAVA_INT,
            POINTER,
            POINTER
    );
    private static final MethodHandle sigactionHandle
            = SYMBOL_LOOKUP.find("sigaction").map(addr -> LINKER.downcallHandle(addr, sigactionDescriptor)).orElseThrow();

    public static int sigaction(int x0, MemorySegment x1, MemorySegment x2) {
        try {
            return (int) sigactionHandle.invokeExact(x0, x1, x2);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }

    static final StructLayout sigactionLayout = MemoryLayout.structLayout(
            POINTER.withName("sa_handler"),
            JAVA_INT.withName("sa_mask"),
            JAVA_INT.withName("sa_flags")
    ).withName("sigaction");

    static MemorySegment    sigactionAllocate(Arena scope) {
        try {
            FunctionDescriptor descriptor = FunctionDescriptor.ofVoid(JAVA_INT);
            MethodHandle handle = MH_LOOKUP.findStatic(Signal.class, "handleSignal", descriptor.toMethodType());
            return LINKER.upcallStub(handle, descriptor, scope);
        } catch (NoSuchMethodException | IllegalAccessException ex) {
            throw new AssertionError(ex);
        }
    }

    public static class sigaction {
        static final VarHandle sa_handlerHandle = sigactionLayout.varHandle(MemoryLayout.PathElement.groupElement("sa_handler"));
        static final VarHandle sa_maskHandle = sigactionLayout.varHandle(MemoryLayout.PathElement.groupElement("sa_mask"));
        static final VarHandle sa_flagsHandle = sigactionLayout.varHandle(MemoryLayout.PathElement.groupElement("sa_flags"));

        public interface sa_handler {
            void apply(int _x0);
        }
    }
}

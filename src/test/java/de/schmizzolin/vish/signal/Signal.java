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
import java.lang.foreign.SegmentAllocator;
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
    static final AddressLayout POINTER = ValueLayout.ADDRESS.withTargetLayout(MemoryLayout.sequenceLayout(JAVA_BYTE));

    static MethodHandle downcallHandle(String name, FunctionDescriptor fdesc) {
        return SYMBOL_LOOKUP.find(name).map(addr -> LINKER.downcallHandle(addr, fdesc)).orElseThrow();
    }


    public static void main(String[] args) throws Throwable {
        Downcall.printPid();

        MemorySegment struct = Arena.global().allocate(sigactionLayout);
        sigaction.sa_flagsHandle.set(struct, 0);
        sigaction.sa_maskHandle.set(struct, 0);
        sigaction.sa_handlerHandle.set(struct,
                sigactionAllocate(signal -> System.out.println("got signal " + signal), Arena.global()));
        System.out.println("before");
        sigaction(30, struct, MemorySegment.NULL);
        System.out.println("after");
        Thread.sleep(10000);
    }


    static MethodHandle upcallHandle(Class<?> fi, String name, FunctionDescriptor fdesc) {
        try {
            return MH_LOOKUP.findVirtual(fi, name, fdesc.toMethodType());
        } catch (Throwable ex) {
            throw new AssertionError(ex);
        }
    }

    static <Z> MemorySegment upcallStub(MethodHandle fiHandle, Z z, FunctionDescriptor fdesc, Arena scope) {
        try {
            fiHandle = fiHandle.bindTo(z);
            return LINKER.upcallStub(fiHandle, fdesc, scope);
        } catch (Throwable ex) {
            throw new AssertionError(ex);
        }
    }

    //--

    static final FunctionDescriptor sigactionDescriptor = FunctionDescriptor.of(JAVA_INT,
            JAVA_INT,
            POINTER,
            POINTER
    );
    private static final MethodHandle sigactionHandle = downcallHandle("sigaction", sigactionDescriptor);

    public static int sigaction(int x0, MemorySegment x1, MemorySegment x2) {
        try {
            return (int)sigactionHandle.invokeExact(x0, x1, x2);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }

    static final StructLayout sigactionLayout = MemoryLayout.structLayout(
            POINTER.withName("sa_handler"),
            JAVA_INT.withName("sa_mask"),
            JAVA_INT.withName("sa_flags")
    ).withName("sigaction");

    static FunctionDescriptor sigactionHandlerDescriptor = FunctionDescriptor.ofVoid(JAVA_INT);
    static MemorySegment sigactionAllocate(sigaction.sa_handler fi, Arena scope) {
        return upcallStub(upcallHandle(sigaction.sa_handler.class, "apply", sigactionHandlerDescriptor),
                fi, sigactionHandlerDescriptor, scope);
    }

    public static class sigaction {
        static final VarHandle sa_handlerHandle = sigactionLayout.varHandle(MemoryLayout.PathElement.groupElement("sa_handler"));
        static final VarHandle sa_maskHandle = sigactionLayout.varHandle(MemoryLayout.PathElement.groupElement("sa_mask"));

        public interface sa_handler {
            void apply(int _x0);
        }

        static final VarHandle sa_flagsHandle = sigactionLayout.varHandle(MemoryLayout.PathElement.groupElement("sa_flags"));
    }
}

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
    public static void main(String[] args) throws Throwable {
        Downcall.printPid();

        MemorySegment struct = sigaction.allocate(Arena.global());
        sigaction.sa_flags$set(struct, 0);
        sigaction.sa_mask$set(struct, 0);
        sigaction.sa_handler$set(struct,
                sigaction.sa_handler.allocate(signal -> System.out.println("got signal " + signal), Arena.global()));
        System.out.println("before");
        sigaction(30, struct, MemorySegment.NULL);
        System.out.println("after");
        Thread.sleep(10000);
    }


    private static final Linker LINKER = Linker.nativeLinker();
    private static final MethodHandles.Lookup MH_LOOKUP = MethodHandles.lookup();
    private static final SymbolLookup SYMBOL_LOOKUP;
    private static final SegmentAllocator THROWING_ALLOCATOR = (x, y) -> { throw new AssertionError("should not reach here"); };
    static final AddressLayout POINTER = ValueLayout.ADDRESS.withTargetLayout(MemoryLayout.sequenceLayout(JAVA_BYTE));

    static {

        SymbolLookup loaderLookup = SymbolLookup.loaderLookup();
        SYMBOL_LOOKUP = name -> loaderLookup.find(name).or(() -> LINKER.defaultLookup().find(name));
    }

    static <T> T requireNonNull(T obj, String symbolName) {
        if (obj == null) {
            throw new UnsatisfiedLinkError("unresolved symbol: " + symbolName);
        }
        return obj;
    }

    static MethodHandle downcallHandle(String name, FunctionDescriptor fdesc) {
        return SYMBOL_LOOKUP.find(name).
                map(addr -> LINKER.downcallHandle(addr, fdesc)).
                orElse(null);
    }

    static MethodHandle downcallHandle(FunctionDescriptor fdesc) {
        return LINKER.downcallHandle(fdesc);
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

    public static class sigaction {
        static final StructLayout const$0 = MemoryLayout.structLayout(
                POINTER.withName("sa_handler"),
                JAVA_INT.withName("sa_mask"),
                JAVA_INT.withName("sa_flags")
        ).withName("sigaction");
        static final FunctionDescriptor const$1 = FunctionDescriptor.ofVoid(
                JAVA_INT
        );
        static final MethodHandle const$2 = upcallHandle(sa_handler.class, "apply", const$1);
        static final MethodHandle const$3 = downcallHandle(
                const$1
        );
        static final VarHandle const$4 = const$0.varHandle(MemoryLayout.PathElement.groupElement("sa_handler"));
        static final VarHandle const$5 = const$0.varHandle(MemoryLayout.PathElement.groupElement("sa_mask"));


        public static MemoryLayout $LAYOUT() {
            return const$0;
        }
        public interface sa_handler {

            void apply(int _x0);
            static MemorySegment allocate(sa_handler fi, Arena scope) {
                return upcallStub(const$2, fi, const$1, scope);
            }
            static sa_handler ofAddress(MemorySegment addr, Arena arena) {
                MemorySegment symbol = addr.reinterpret(arena, null);
                return (int __x0) -> {
                    try {
                        const$3.invokeExact(symbol, __x0);
                    } catch (Throwable ex$) {
                        throw new AssertionError("should not reach here", ex$);
                    }
                };
            }
        }

        public static void sa_handler$set(MemorySegment seg, MemorySegment x) {
            const$4.set(seg, x);
        }
        public static void sa_mask$set(MemorySegment seg, int x) {
            const$5.set(seg, x);
        }

        static final VarHandle xxx = const$0.varHandle(MemoryLayout.PathElement.groupElement("sa_flags"));
        public static void sa_flags$set(MemorySegment seg, int x) {
            xxx.set(seg, x);
        }
        public static MemorySegment allocate(SegmentAllocator allocator) { return allocator.allocate($LAYOUT()); }
    }
}

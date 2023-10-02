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

        MemorySegment handler = allocateHandlerStub();
        signal(30, handler);
        Thread.sleep(10000);
    }

    //-- upcall

    static void handleSignal(int signal) {
        System.out.println("got signal: " + signal);
    }

    static MemorySegment allocateHandlerStub() throws NoSuchMethodException, IllegalAccessException {
        FunctionDescriptor descriptor = FunctionDescriptor.ofVoid(JAVA_INT);
        MethodHandle handle = MethodHandles.lookup().findStatic(Signal.class, "handleSignal", descriptor.toMethodType());
        return LINKER.upcallStub(handle, descriptor, Arena.global());
    }

    //-- downcall

    static void signal(int signum, MemorySegment handler) throws Throwable {
        FunctionDescriptor descriptor = FunctionDescriptor.of(ADDRESS, JAVA_INT, ADDRESS);
        MethodHandle handle = LINKER.downcallHandle(LOOKUP.find("signal").get(), descriptor);
        handle.invoke(signum, handler);
    }
}

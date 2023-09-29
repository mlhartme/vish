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
package de.schmizzolin.vish.examples;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;

/**
 * https://www.gnu.org/software/libc/manual/html_node/Advanced-Signal-Handling.html
 */
public class Upcall {
    public static void main(String[] args) throws Throwable {
        Downcall.printPid();

        Linker linker = Linker.nativeLinker();
        SymbolLookup stdlibs = linker.defaultLookup();

        MemorySegment signalAddress = stdlibs.find("signal").get();

        MethodHandle upHandle = MethodHandles.lookup()
                .findStatic(Upcall.class, "signalHandler", MethodType.methodType(void.class, int.class));
        MemorySegment upAddress = linker.upcallStub(upHandle, FunctionDescriptor.ofVoid(JAVA_INT), Arena.ofAuto());

        FunctionDescriptor signalDescriptor = FunctionDescriptor.of(ADDRESS, JAVA_INT, ADDRESS);
        MethodHandle signalHandle = linker.downcallHandle(signalAddress, signalDescriptor);
        MemorySegment old = (MemorySegment) signalHandle.invokeExact(31, upAddress);
        System.out.println("old address " + old.address() + " -> " + upAddress.address());
        System.out.println("sleeping ...");
        Thread.sleep(10000);
        System.out.println("done");
    }

    public static void signalHandler(int signal) {
        System.out.println("signal received: " + signal);
    }
}

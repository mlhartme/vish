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

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;

import static java.lang.foreign.ValueLayout.JAVA_INT;

public class Downcall {
    public static void main(String[] args) throws Throwable {
        Linker linker = Linker.nativeLinker();
        SymbolLookup stdlibs = linker.defaultLookup();
        MemorySegment getpid = stdlibs.find("getpid").get();
        FunctionDescriptor descriptor = FunctionDescriptor.of(JAVA_INT);
        MethodHandle handle = linker.downcallHandle(getpid, descriptor);
        int pid = (int) handle.invoke();
        System.out.println("pid: " + pid);
    }
}

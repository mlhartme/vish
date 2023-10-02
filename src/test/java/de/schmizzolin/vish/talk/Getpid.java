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

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import static java.lang.foreign.ValueLayout.JAVA_INT;

/**
 * https://www.gnu.org/software/libc/manual/html_node/Process-Identification.html
 *
 * Essentially <code>int getpid();</code>
 */
public class Getpid {
    public static void main(String[] args) throws Throwable {
        printPid();
    }

    public static MethodHandle downcall(String name, MemoryLayout result, MemoryLayout ... args) {
        Linker linker = Linker.nativeLinker();
        MemorySegment addr = linker.defaultLookup().find(name).get();
        return linker.downcallHandle(addr, FunctionDescriptor.of(result, args));
    }

    public static void printPid() throws Throwable {
        MethodHandle getpid = downcall("getpid", JAVA_INT);
        System.out.println("pid: " + getpid.invoke());
    }
}

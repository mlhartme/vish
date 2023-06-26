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
package de.schmizzolin.vish.util;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

import static java.lang.foreign.ValueLayout.JAVA_INT;

/** http://pubs.opengroup.org/onlinepubs/009695399/functions/kill.html */
public final class Stdlib {
    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup STDLIB = LINKER.defaultLookup();
    private static final MethodHandle GETPID = lookup("getpid");
    private static final MethodHandle GETEUID = lookup("geteuid");
    private static final MethodHandle GETEGID = lookup("getegid");

    private static MethodHandle lookup(String name, ValueLayout... args) {
        MemorySegment segment;

        segment = STDLIB.find(name).get();
        return LINKER.downcallHandle(segment, FunctionDescriptor.of(JAVA_INT, args));
    }

    private static int invoke(MethodHandle handle) {
        try {
            return (int) handle.invoke();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    //--

    public static long getpid() {
        return invoke(GETPID);
    }

    public static int geteuid() {
        return invoke(GETEUID);
    }

    public static int getegid() {
        return invoke(GETEGID);
    }

    private Stdlib() {
    }
}

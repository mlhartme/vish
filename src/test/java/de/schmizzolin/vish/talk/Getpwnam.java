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
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

// From pwd.h on mac os
//  struct passwd {
//        char    *pw_name;               /* user name */
//        char    *pw_passwd;             /* encrypted password */
//                uid_t   pw_uid;                 /* user uid */
//                gid_t   pw_gid;                 /* user gid */
//                __darwin_time_t pw_change;              /* password change time */
//                char    *pw_class;              /* user access class */
//                char    *pw_gecos;              /* Honeywell login info */
//                char    *pw_dir;                /* home directory */
//                char    *pw_shell;              /* default shell */
//                __darwin_time_t pw_expire;              /* account expiration */
//              };
public class Getpwnam {
    public static void main(String[] args) throws Throwable {
        printPwnam("mhm");
    }

    public static void printPwnam(String name) throws Throwable {
        Linker linker = Linker.nativeLinker();
        SymbolLookup lookup = linker.defaultLookup();
        MemorySegment addr = lookup.find("getpwnam").get();
        MemoryLayout layout = MemoryLayout.structLayout(
                ADDRESS.withName("name"),
                ADDRESS.withName("passwd"),
                JAVA_INT.withName("uid"),
                JAVA_INT.withName("gid"),
                JAVA_LONG.withName("change"),
                ADDRESS.withName("class"),
                ADDRESS.withName("gecos"),
                ADDRESS.withName("dir"),
                ADDRESS.withName("shell")
        );
        MethodHandle handle = linker.downcallHandle(addr, FunctionDescriptor.of(ADDRESS, ADDRESS));
        MemorySegment struct = (MemorySegment) handle.invoke(Arena.ofAuto().allocateUtf8String(name));
        if (struct.address() == 0) {
            System.out.println("not found: " + name);
            return;
        }
        struct = struct.reinterpret(layout.byteSize());
        MemorySegment nameAddr = (MemorySegment) layout.varHandle(MemoryLayout.PathElement.groupElement("gecos")).get(struct);
        nameAddr = nameAddr.reinterpret(100); // TODO
        System.out.println("name: " + nameAddr.getUtf8String(0));
    }
}

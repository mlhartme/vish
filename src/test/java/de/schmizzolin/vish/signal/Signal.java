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

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

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

    private static MethodHandle sigaction$MH() {
        return RuntimeHelper.requireNonNull(constants$1.const$2,"sigaction");
    }

    public static int sigaction(int x0, MemorySegment x1, MemorySegment x2) {
        var mh$ = sigaction$MH();
        try {
            return (int)mh$.invokeExact(x0, x1, x2);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
}

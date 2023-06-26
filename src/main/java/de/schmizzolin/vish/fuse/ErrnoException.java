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
package de.schmizzolin.vish.fuse;

import java.io.IOException;

public class ErrnoException extends IOException {
    private final Errno errno;

    public ErrnoException(Errno errno) {
        super(errno.toString());
        this.errno = errno;
    }

    public ErrnoException(Errno errno, Exception cause) {
        this(errno);
        initCause(cause);
    }


    public Errno errno() {
        return errno;
    }

    public int returnCode() {
        return -errno.code();
    }
}

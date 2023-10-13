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
package de.schmizzolin.vish.filesystem;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

public interface Filesystem {
    default String name() {
        return getClass().getSimpleName();
    }

    //-- file system methods

    Attr getAttr(String path) throws ErrnoException;
    void readDir(String path, Consumer<String> filler) throws ErrnoException;

    /** @param offset into src file, not dest buffer */
    int read(String path, ByteBuffer buffer, int offset) throws ErrnoException;

    //-- mount/unmount

    default Mount mount(File dest) {
        return mount(dest, new Options());
    }

    default Mount mount(File dest, Options options) {
        return Mount.create(this, dest, options);
    }
}

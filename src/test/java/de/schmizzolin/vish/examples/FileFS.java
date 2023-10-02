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

import de.schmizzolin.vish.fuse.Attr;
import de.schmizzolin.vish.fuse.Filesystem;
import de.schmizzolin.vish.fuse.Errno;
import de.schmizzolin.vish.fuse.ErrnoException;

import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/** Serves a single file */
public class FileFS extends Filesystem {
    private static final String NAME = "file";
    private final byte[] contents;

    public FileFS(String contents) {
        this.contents = contents.getBytes(StandardCharsets.UTF_8);
    }

    public Attr getAttr(String path) throws ErrnoException {
        switch (path) {
            case "/" -> {
                return Attr.directory(0);
            }
            case "/" + NAME -> {
                return Attr.file(contents.length, 0);
            }
            default -> throw new ErrnoException(Errno.ENOENT);
        }
    }

    public void readDir(String path, Consumer<String> dest) throws ErrnoException {
        if (!"/".equals(path)) {
            throw new ErrnoException(Errno.ENOENT);
        }
        dest.accept(".");
        dest.accept("..");
        dest.accept(NAME);
    }

    @Override
    public int read(String path, ByteBuffer buffer, int offset) throws ErrnoException {
        if (!("/" + NAME).equals(path)) {
            throw new ErrnoException(Errno.ENOENT);
        }
        int count = Math.min(buffer.limit(), contents.length - offset);
        int start = Math.min(contents.length, offset);
        buffer.put(0, contents, start, count);
        return count;
    }
}

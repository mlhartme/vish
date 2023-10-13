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

import de.schmizzolin.vish.filesystem.Attr;
import de.schmizzolin.vish.filesystem.Filesystem;
import de.schmizzolin.vish.filesystem.Errno;
import de.schmizzolin.vish.filesystem.ErrnoException;
import de.schmizzolin.vish.filesystem.Mount;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Serves a single file */
public class FileFS implements Filesystem {
    public static void main(String[] args) throws InterruptedException {
        File dir = new File("target/single-volume");
        Mount mount = new FileFS("Hello, World\n").mount(dir);
        System.out.println("mounted " + dir + " ... press ctrl-c to quit.");
        mount.join();
    }

    private static final String NAME = "file";
    private final byte[] contents;

    public FileFS(String contents) {
        this.contents = contents.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public Attr getAttr(String path) throws ErrnoException {
        return switch (path) {
            case "/" -> Attr.directory(0);
            case "/" + NAME -> Attr.file(contents.length, 0);
            default -> throw new ErrnoException(Errno.ENOENT);
        };
    }

    @Override
    public void readDir(String path, Consumer<String> dest) throws ErrnoException {
        if (!"/".equals(path)) {
            throw new ErrnoException(Errno.ENOENT);
        }
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

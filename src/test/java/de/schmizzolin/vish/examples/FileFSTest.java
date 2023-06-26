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

import de.schmizzolin.vish.fuse.FuseFS;
import net.oneandone.sushi.fs.MkdirException;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.launcher.Failure;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FileFSTest {
    private static final World WORLD = World.createMinimal();

    private FileNode dir;
    private Thread loop;

    private FuseFS fs;

    @BeforeEach
    public void before() throws MkdirException {
        dir = WORLD.guessProjectHome(getClass()).join("target/fusevolume").mkdirsOpt();
        loop = null;
    }

    @AfterEach
    public void after() throws IOException, InterruptedException {
        if (loop != null) {
            fs.umount(dir.toPath().toFile());
            loop.join();
        }
    }

    @Test
    public void single() throws InterruptedException, IOException {
        String content = WORLD.guessProjectHome(getClass()).join("pom.xml").readString();
        content = content + content + content + content + content;
        content = content + content + content + content + content;
        start(new FileFS(content));
        assertEquals(Arrays.asList("file"), dir.list().stream().map((file) -> file.getName()).toList());
        assertEquals(content, dir.join("file").readString());
    }

    private void start(FuseFS fs) throws InterruptedException {
        if (loop != null) {
            throw new IllegalStateException();
        }
        this.fs = fs;
        loop = fs.start(dir.toPath().toFile(), true);
    }
}

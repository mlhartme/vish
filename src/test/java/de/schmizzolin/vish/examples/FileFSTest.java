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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FileFSTest {
    private static final File BASE = new File("."); // TODO: not always project directory

    @Test
    public void single() throws InterruptedException, IOException {
        File dir = new File(BASE, "target/single-volume");;

        String content = Files.readString(new File(BASE, "pom.xml").toPath());
        content = content + content + content + content + content;
        content = content + content + content + content + content;
        try (FuseFS.Mount mount = new FileFS(content).mount(dir, true)) {
            assertEquals(Arrays.asList("file"), Arrays.asList(dir.listFiles()).stream().map((file) -> file.getName()).toList());
            assertEquals(content, Files.readString(new File(dir, "file").toPath()));
        }
    }
}

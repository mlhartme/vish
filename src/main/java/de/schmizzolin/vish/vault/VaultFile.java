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
package de.schmizzolin.vish.vault;

import de.schmizzolin.vish.filesystem.Attr;
import de.schmizzolin.vish.filesystem.ErrnoException;

import java.nio.ByteBuffer;

public class VaultFile extends VaultNode {
    private final long modified;
    private final byte[] bytes;

    public VaultFile(String name, int uid, int gid, long lazyModified, byte[] lazyBytes) {
        super(name, uid, gid);
        this.modified = lazyModified;
        this.bytes = lazyBytes;
    }

    @Override
    protected Attr getAttr() {
        return Attr.file(bytes.length, modified);
    }

    public int read(ByteBuffer buffer, int offset) throws ErrnoException {
        int count = Math.min(buffer.limit(), bytes.length - offset);
        int start = Math.min(bytes.length, offset);
        buffer.put(0, bytes, start, count);
        return count;
    }
}

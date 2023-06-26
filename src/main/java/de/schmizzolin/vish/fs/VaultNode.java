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
package de.schmizzolin.vish.fs;

import de.schmizzolin.vish.fuse.ErrnoException;

import java.lang.foreign.MemorySegment;

/** File or directory */
public abstract class VaultNode {
    protected final String name;

    protected final int uid;
    protected final int gid;

    protected VaultDirectory parent;

    public VaultNode(String name, int uid, int gid) {
        if (name.contains("/")) {
            throw new IllegalArgumentException(name);
        }
        this.name = name;
        this.parent = null; // initialized by directory.add
        this.uid = uid;
        this.gid = gid;
    }

    protected VaultNode find(String path) throws ErrnoException {
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        if (path.equals(name) || path.isEmpty()) {
            return this;
        }
        return null;
    }

    protected abstract void getAttr(MemorySegment stat) throws ErrnoException;
}

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

import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.response.LogicalResponse;
import de.schmizzolin.vish.fuse.Attr;
import de.schmizzolin.vish.fuse.Errno;
import de.schmizzolin.vish.fuse.ErrnoException;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class VaultDirectory extends VaultNode {
    public static VaultDirectory createMerged(String name, long modified, int uid, int gid, Map<String, String> data) {
        VaultDirectory result = new VaultDirectory(null, name, modified, uid, gid, null, new ArrayList<>());
        for (var entry : data.entrySet()) {
            result.add(new VaultFile(entry.getKey(), uid, gid, modified, entry.getValue().getBytes(StandardCharsets.UTF_8)));
        }
        return result;
    }

    public static VaultDirectory createLazy(VaultFs filesystem, String name, long modified, int uid, int gid, String path) {
        return new VaultDirectory(filesystem, name, modified, uid, gid, path, null);
    }

    private final VaultFs filesystem;
    private final String path;
    private final long modified;

    private List<VaultNode> lazyChildren;

    private VaultDirectory(VaultFs filesystem, String name, long modified, int uid, int gid, String path, List<VaultNode> lazyChildren) {
        super(name, uid, gid);
        this.filesystem = filesystem;
        this.modified = modified;
        this.path = path;
        this.lazyChildren = lazyChildren;
    }

    public List<String> list() throws ErrnoException {
        List<String> lst = new ArrayList<>();
        read(lst::add);
        return lst;
    }

    @Override
    public VaultNode find(String destPath) throws ErrnoException {
        VaultNode tmp;
        String car;
        String cdr;

        tmp = super.find(destPath);
        if (tmp != null) {
            return tmp;
        }
        while (destPath.startsWith("/")) {
            destPath = destPath.substring(1);
        }
        if (!destPath.contains("/")) {
            for (VaultNode p : children()) {
                if (p.name.equals(destPath)) {
                    return p;
                }
            }
            return null;
        }
        car = destPath.substring(0, destPath.indexOf("/"));
        cdr = destPath.substring(destPath.indexOf("/"));
        for (VaultNode p : children()) {
            if (p.name.equals(car)) {
                return p.find(cdr);
            }
        }
        return null;
    }

    @Override
    protected Attr getAttr() {
        return Attr.directory(modified);
    }

    public void read(Consumer<String> filler) throws ErrnoException {
        for (VaultNode p : children()) {
            filler.accept(p.name);
        }
    }

    //--
    private List<VaultNode> children() throws ErrnoException {
        LogicalResponse response;

        if (lazyChildren == null) {
            lazyChildren = new ArrayList<>();
            try {
                response = filesystem.vault.logical().list(path);
            } catch (VaultException e) {
                lazyChildren = null;
                throw new ErrnoException(Errno.EIO, e);
            }
            for (String childName : response.getListData()) {
                VaultNode child;

                if (!path.endsWith("/")) {
                    throw new IllegalStateException(path);
                }
                String childPath = path + childName;
                if (childName.endsWith("/")) {
                    child = filesystem.directory(childName.substring(0, childName.length() - 1), childPath);
                } else {
                    child = filesystem.file(childName, childPath);
                }
                add(child);
            }
        }
        return lazyChildren;
    }

    private void add(VaultNode child) {
        if (child.parent != null) {
            throw new IllegalArgumentException();
        }
        lazyChildren.add(child);
        child.parent = this;
    }
}

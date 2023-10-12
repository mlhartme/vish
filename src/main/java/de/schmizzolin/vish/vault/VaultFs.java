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

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.json.Json;
import com.bettercloud.vault.json.JsonObject;
import com.bettercloud.vault.response.LogicalResponse;
import de.schmizzolin.vish.fuse.Attr;
import de.schmizzolin.vish.fuse.Filesystem;
import de.schmizzolin.vish.fuse.Errno;
import de.schmizzolin.vish.fuse.ErrnoException;
import de.schmizzolin.vish.util.Stdlib;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Vault filesystem */
public class VaultFs extends Filesystem {
    private final VaultDirectory root;

    public final Vault vault;
    public final boolean merged;
    public final int uid;
    public final int gid;

    public final long dirModified;

    public VaultFs(Vault vault, String path, boolean merged) {
        if (path.length() <= 1 || path.startsWith("/") || !path.endsWith("/")) {
            throw new IllegalArgumentException(path);
        }

        this.vault = vault;
        this.merged = merged;
        this.uid = Stdlib.geteuid();
        this.gid = Stdlib.getegid();
        this.dirModified = System.currentTimeMillis() / 1000;
        this.root = directory("root", path);
    }

    //--

    public VaultNode getPath(String path) throws ErrnoException {
        VaultNode result;

        if (!path.startsWith("/")) {
            throw new IllegalArgumentException(path);
        }
        result = root.find(path.substring(1));
        if (result == null) {
            throw new ErrnoException(Errno.ENOENT);
        }
        return result;
    }

    //-- filesystem implementation

    @Override
    public Attr getAttr(String path) throws ErrnoException {
        return getPath(path).getAttr();
    }

    @Override
    public int read(String path, ByteBuffer dest, int offset) throws ErrnoException {
        if (getPath(path) instanceof VaultFile file) {
            return file.read(dest, offset);
        } else {
            throw new ErrnoException(Errno.EISDIR);
        }
    }

    @Override
    public void readDir(String path, Consumer<String> filler) throws ErrnoException {
        if (getPath(path) instanceof VaultDirectory directory) {
            filler.accept(".");
            filler.accept("..");
            directory.read(filler);
        } else {
            throw new ErrnoException(Errno.ENOTDIR);
        }
    }

    //--

    public VaultDirectory directory(String name, String path) {
        return VaultDirectory.createLazy(this, name, dirModified, uid, gid, path);
    }

    public VaultNode file(String name, String path) throws ErrnoException {
        LogicalResponse response;
        long modified;
        byte[] bytes;

        try {
            response = vault.logical().read(path);
        } catch (VaultException e) {
            throw new ErrnoException(Errno.EIO, e);
        }
        String jsonString = new String(response.getRestResponse().getBody(), StandardCharsets.UTF_8);
        JsonObject obj = Json.parse(jsonString).asObject().get("data").asObject();
        obj = obj.get("metadata").asObject();
        ZonedDateTime time = ZonedDateTime.parse(obj.getString("created_time"));

        modified = time.toEpochSecond();
        if (merged) {
            bytes = mergedData(response.getData()).getBytes(StandardCharsets.UTF_8);
            return new VaultFile(name, uid, gid, modified, bytes);
        } else {
            return VaultDirectory.createMerged(name, modified, uid, gid, response.getData());
        }
    }


    private static String mergedData(Map<String, String> data) {
        List<String> keys;
        StringBuilder result;

        keys = new ArrayList<>(data.keySet());
        Collections.sort(keys);
        result = new StringBuilder();
        for (String key : keys) {
            result.append(key).append(": ");
            result.append(data.get(key));
            result.append('\n');
        }
        return result.toString();
    }
}

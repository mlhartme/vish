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
package de.schmizzolin.vish;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import de.schmizzolin.vish.vault.VaultFs;
import de.schmizzolin.vish.fuse.Filesystem;
import de.schmizzolin.vish.fuse.Mount;
import net.oneandone.inline.Cli;
import net.oneandone.inline.Console;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.LogManager;

public final class Main {
    public static void main(String[] args) throws Exception {
        try (InputStream src = Main.class.getResourceAsStream("/logging.properties")) {
            LogManager.getLogManager().readConfiguration(src);
        }
        System.exit(doMain(args));
    }

    public static int doMain(String... args) throws Exception {
        return Cli.single(Main.class, "unused -m=false -fslog -timeout=600 path? command*").run(args);
    }

    //--
    private final Console console;

    private final String fslog;

    private final int timeout;
    /** null means "all projects */

    private final boolean merged;
    private final String path;

    private final List<String> command;

    public Main(Console console, boolean merged, String fslog, int timeout, String path, List<String> command) throws IOException {
        // TODO: fsLog
        this.console = console;
        if (path != null) {
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            if (!path.endsWith("/")) {
                path = path + "/";
            }
        }
        this.merged = merged;
        this.fslog = fslog;
        this.timeout = timeout;
        this.path = path;
        this.command = command.isEmpty() ? Arrays.asList(defaultCommand()) : command;
    }

    public static String defaultCommand() {
        String str;

        str = System.getenv("SHELL");
        return str == null ? "bash" : str;
    }

    public void run() throws IOException, InterruptedException, VaultException {
        Vault vault;
        Path cwd;
        Filesystem fs;

        if (path == null) {
            console.info.println(help());
            return;
        }
        vault = vault(vaultPrefix(path));
        fs = new VaultFs(vault, path, merged, getLogger());
        cwd = Files.createTempDirectory("vish-tmp");
        try (Mount mount = fs.mount(cwd.toFile(), false)) {
            console.verbose.println("mount thread started");
            body(cwd.toFile());
        } finally {
            console.verbose.println("cleanup");
            Files.delete(cwd);
            console.verbose.println("umount done");
        }
    }

    public static String vaultPrefix(String path) throws IOException {
        String result;

        result = tryVaultPrefix("secrets", path);
        if (result == null) {
            result = tryVaultPrefix("secret", path);
            if (result == null) {
                throw new IOException("cannot determine secrets prefix for path " + path);
            }
        }
        return result;
    }

    private static String tryVaultPrefix(String marker, String path) {
        var idx = path.indexOf(marker);
        if (idx == -1
                || (idx > 0 && path.charAt(idx - 1) != '/')
                || (idx + marker.length() < path.length() && path.charAt(idx + marker.length()) != '/')) {
            return null;
        }
        return path.substring(0, idx + marker.length());
    }

    private String help() {
        StringBuilder builder;
        Package pkg;

        builder = new StringBuilder();
        pkg = getClass().getPackage();
        if (pkg  != null) {
            builder.append("vish ").append(pkg.getSpecificationVersion())
                    .append(" (").append(pkg.getImplementationVersion()).append(")\n");
        }
        builder.append("    executes a command with vault secrets mounted read-only into the current directory\n");
        builder.append("    requires Java 20");
        builder.append("\n");
        builder.append("usage: 'vish' ['-m']['-v']['-e']['-fslog' <fslog>]['-timeout' <n>] <path> <command*>\n");
        builder.append("    -m           merged properties in a single - possibly empty - file\n");
        builder.append("    -v           verbose output\n");
        builder.append("    -e           print exception stacktraces\n");
        builder.append("    <fslog>      file to write filesystem logs to, default is /tmp/vish-<user>.log\n");
        builder.append("    <path>       vault path to be mounted\n");
        builder.append("    <command>    shell command to execute, default is $SHELL\n");
        builder.append("file system notes:\n");
        builder.append("    modified dates for files are picked from vault");
        builder.append("environment:\n");
        builder.append("    VAULT_ADDR   url of the vault server\n");
        builder.append("examples:\n");
        builder.append("    vish your/secrets\n");
        builder.append("                 start a shell in your secrets\n");
        builder.append("    vish your/secrets grep -r -i partifactory .\n");
        builder.append("                 to find secrets containing 'partifactory'\n");
        builder.append("    vish your/secrets find . -type d -empty\n");
        builder.append("                 to find all empty directories (which are usually deleted nodes)\n");
        builder.append("    vish your/secrets find . -mmin -60 -type\n");
        builder.append("                 to find secrets modified in the last 60 minutes\n");
        return builder.toString();
    }


    public void body(File dir) throws IOException, InterruptedException {
        ProcessBuilder builder;
        int result;
        Process process;

        console.verbose.println("Mounted vault secrets path " + path);
        console.verbose.println("executing " + command);

        builder = new ProcessBuilder(command);
        builder.environment().put("PS1", "vault> "); // TODO
        builder.directory(dir);
        builder.inheritIO();
        process = builder.start();
        if (timeout > 0) {
            if (!process.waitFor(timeout, TimeUnit.MINUTES)) {
                console.error.println();
                console.error.println("Command timed out, aborting ...");
                process.destroy();
                return;
            }
            result = process.exitValue();
        } else {
            result = process.waitFor();
        }
        if (result != 0) {
            console.error.println(command + " returned exit code: " + result);
        }
    }

    //--

    private Vault vault(String prefix) throws IOException, VaultException {
        VaultConfig config = new VaultConfig()
                .address(vaultAddr())
                .prefixPath(prefix)
                .token(vaultToken())
                .build();
        return new Vault(config);
    }

    private static String vaultAddr() throws IOException {
        String str;

        str = System.getenv("VAULT_ADDR");
        if (str == null) {
            throw new IOException("missing VAULT_ADDR");
        }
        return str;
    }

    private String vaultToken() throws IOException {
        File file = new File(System.getProperty("user.home"), ".vault-token");

        if (!file.exists()) {
            throw new IOException("missing token, try 'vault auth'");
        }
        return Files.readString(file.toPath()).trim();
    }

    //--

    private PrintWriter getLogger() throws IOException {
        String name = fslog != null ? fslog : "/tmp/vish-" + System.getProperty("user.name") + ".log";
        console.verbose.println("writing filesystem logs to " + name);
        return new PrintWriter(new FileWriter(name), true);
    }
}

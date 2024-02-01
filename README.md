# Vish

Mounts vault secrets as a filesystem and opens a shell inside.

Run Prerequisites:
* Intel or ARM Mac
* Java 21 (which brings https://openjdk.org/jeps/442)
* OSX Fuse https://osxfuse.github.io
  (setup is tedious: allow kernel extensions, allow this particular developer, several reboots)

Build Prerequisites
* Maven with MAVEN_OPTS=--enable-preview
* Vault for testing

Todo:
* integration tests with local 'vault'
* brew packaging and setup
* Linux support
* write support
* compile to native code with graal
    

Inspired by 
* https://github.com/SerCeMan/jnr-fuse
* https://www.davidvlijmincx.com/posts/writing_a_simple_filesystem_using_fuse_and_java_17/
* https://foojay.io/today/project-panama-for-newbies-part-1/
* https://github.com/cryptomator/jfuse


Naming

Vish sounds like a wish or a spell, but also contains vicious. And it ends with 'sh' for a shell.

# Vish

Mounts vault secrets as a filesystem and opens a shell inside.

Run Prerequisites:
* Mac OS
* Java 20
* OSX Fuse https://osxfuse.github.io

Build
* with MAVEN_OPTS=--enable-preview

Todo:
* brew packaging and setup
* integration tests with local 'vault'
* compile to native code with graal
* add Linux support
* write support


Inspired by 
* https://github.com/SerCeMan/jnr-fuse
* https://www.davidvlijmincx.com/posts/writing_a_simple_filesystem_using_fuse_and_java_17/
* https://foojay.io/today/project-panama-for-newbies-part-1/
* https://github.com/cryptomator/jfuse


Naming

Vish sounds like a wish or a spell, but also contains vicious. And it ends with 'sh' for a shell.

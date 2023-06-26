#!/bin/sh
set -e
dest=work/generated
rm -rf $dest
mkdir $dest
alias jextract="work/jextract-20/bin/java --enable-native-access=org.openjdk.jextract -m org.openjdk.jextract/org.openjdk.jextract.JextractTool"
# depend on Ventura -- macOS 13
include=/Library/Developer/CommandLineTools/SDKs/MacOSX13.sdk/usr/include
echo "generate fuse bindings ..."
jextract -D "FUSE_USE_VERSION=26" "-D_FILE_OFFSET_BITS=64" --source --output $dest/fuse -t foreign.fuse -I work/fuse/include/ -I $include work/fuse/include/fuse.h
mkdir $dest/work

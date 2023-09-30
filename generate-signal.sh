#!/bin/sh
set -e

./get-fuse.sh
./build-jextract-21.sh

alias jextract="work/bin/jextract/bin/java --enable-native-access=org.openjdk.jextract -m org.openjdk.jextract/org.openjdk.jextract.JextractTool"

echo "wiping old bindings ..."
rm -rf target/signal
mkdir target/signal

# depend on Ventura -- macOS 13
include=/Library/Developer/CommandLineTools/SDKs/MacOSX13.sdk/usr/include
echo "generate fuse bindings ..."
jextract -D "FUSE_USE_VERSION=29" "-D_FILE_OFFSET_BITS=64" \
  --source --output target/signal -t foreign.signal -I $include $include/signal.h

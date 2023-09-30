#!/bin/sh
set -e

./get-fuse.sh
./build-jextract-21.sh

alias jextract="work/bin/jextract/bin/java --enable-native-access=org.openjdk.jextract -m org.openjdk.jextract/org.openjdk.jextract.JextractTool"

echo "wiping old bindings ..."
rm -rf target/signal
mkdir target/signal

include=/Library/Developer/CommandLineTools/SDKs/MacOSX14.sdk/usr/include
echo "generate simple signal bindings ..."
jextract --source --output target/signal -t foreign.signal -I $include simple_signal.h

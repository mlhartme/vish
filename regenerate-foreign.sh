#!/bin/sh
set -e

./get-fuse.sh
if [ `uname -p` = "arm" ] ; then
  ./build-jextract-21.sh
else
  ./get-jextract-21.sh
fi

alias jextract="work/bin/jextract/bin/java --enable-native-access=org.openjdk.jextract -m org.openjdk.jextract/org.openjdk.jextract.JextractTool"

echo "wiping old bindings ..."
rm -rf src/main/java/foreign
mkdir src/main/java/foreign

include=/Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/usr/include
echo "generate fuse bindings ..."
jextract -D "FUSE_USE_VERSION=29" "-D_FILE_OFFSET_BITS=64" \
	--include-function fuse_main_real \
  --include-function fuse_parse_cmdline \
  --include-function fuse_mount \
  --include-function fuse_new \
  --include-function fuse_set_signal_handlers \
  --include-function fuse_loop \
  --include-function fuse_remove_signal_handlers \
  --include-function fuse_destroy \
  --include-function fuse_unmount \
  --include-function fuse_exit \
	--include-struct fuse_args \
	--include-struct fuse_session \
	--include-struct fuse_operations \
	--include-struct stat \
	--include-struct timespec \
	--include-typedef fuse_fill_dir_t \
	--include-constant S_IFDIR \
	--include-constant S_IFREG \
  --source --output src/main/java -t foreign.fuse -I work/fuse/include/ -I $include work/fuse/include/fuse.h

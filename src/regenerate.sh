#!/bin/sh
set -e
dest=work/generated
rm -rf $dest
mkdir $dest

# TODO
alias jextract="$HOME/bin/jextract/bin/java --enable-native-access=org.openjdk.jextract -m org.openjdk.jextract/org.openjdk.jextract.JextractTool"

# depend on Ventura -- macOS 13
include=/Library/Developer/CommandLineTools/SDKs/MacOSX13.sdk/usr/include
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
  --source --output $dest/fuse -t foreign.fuse -I work/fuse/include/ -I $include work/fuse/include/fuse.h

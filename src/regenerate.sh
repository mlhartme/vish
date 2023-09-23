#!/bin/sh
set -e

if [ ! -d work ] ; then
  mkdir work
  cd work
  echo "clone osx fuse ... https://github.com/osxfuse/fuse/"
  # TODO: vanilla fuse didn't work -- FUSE_USE_VERSION problem?
  git clone "git@github.com:osxfuse/fuse.git"
  cd ..
fi

# TODO
alias jextract="$HOME/bin/jextract/bin/java --enable-native-access=org.openjdk.jextract -m org.openjdk.jextract/org.openjdk.jextract.JextractTool"

rm -rf src/main/java/foreign
mkdir src/main/java/foreign

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
  --source --output src/main/java -t foreign.fuse -I work/fuse/include/ -I $include work/fuse/include/fuse.h

#!/bin/sh
set -e
if [ ! -d work ] ; then
  mkdir work
  cd work
  echo "clone osx fuse ... https://github.com/osxfuse/fuse/"
  # TODO: vanilla fuse didn't work -- FUSE_USE_VERSION problem?
  git clone "git@github.com:osxfuse/fuse.git"
  echo "get jextract ... https://github.com/openjdk/jextract"
  #   curl "https://download.java.net/java/early_access/jextract/2/openjdk-19-jextract+2-3_linux-x64_bin.tar.gz" | tar zx
  # curl "https://download.java.net/java/early_access/jextract/2/openjdk-19-jextract+2-3_macos-x64_bin.tar.gz" | tar zx
  curl https://download.java.net/java/early_access/jextract/1/openjdk-20-jextract+1-2_macos-x64_bin.tar.gz | tar zx
  cd ..
fi
if [ ! -d work/generated ] ; then
  src/regenerate.sh
fi

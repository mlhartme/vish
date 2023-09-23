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
if [ ! -d work/generated ] ; then
  src/regenerate.sh
fi

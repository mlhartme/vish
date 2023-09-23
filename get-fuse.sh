#!/bin/sh
set -e

dest=work/fuse
if [ ! -d $dest ] ; then
  mkdir -p $dest
  echo "clone osx fuse ... https://github.com/osxfuse/fuse/"
  # TODO: vanilla fuse didn't work -- FUSE_USE_VERSION problem?
  git clone "git@github.com:osxfuse/fuse.git" $dest
    fi
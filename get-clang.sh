#!/bin/sh
# download clang for intel mac
set -e

dest=work/bin/clang
if [ ! -d $dest ] ; then
  mkdir -p $dest
  cd $dest
  arch=`uname -p`
  echo "architecture: $arch"
  if [ "$arch" = "arm" ] ; then
    url=https://github.com/llvm/llvm-project/releases/download/llvmorg-14.0.6/clang+llvm-14.0.6-arm64-apple-darwin22.3.0.tar.xz
  else
    url=https://github.com/llvm/llvm-project/releases/download/llvmorg-14.0.6/clang+llvm-14.0.6-x86_64-apple-darwin.tar.xz
  fi
  echo "downloading llvm from $url ..."
  curl -f -q -L $url | tar zx --strip-components=1
fi

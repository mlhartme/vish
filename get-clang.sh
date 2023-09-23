#!/bin/sh
# download clang
set -e

dest=work/clang
if [ ! -d $dest ] ; then
  mkdir -p $dest
  cd $dest
  curl -f -q -L https://github.com/llvm/llvm-project/releases/download/llvmorg-13.0.1/clang+llvm-13.0.1-x86_64-apple-darwin.tar.xz | tar zx --strip-components=1
fi

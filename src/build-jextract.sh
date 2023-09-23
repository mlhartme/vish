#!/bin/sh
# builds jextract for jdk 21 for intel mac
set -e

builddir=$HOME/tmp/jextract-build
installdir=$HOME/bin/jextract
java17=`/usr/libexec/java_home -v 17`
java21=/Users/mhm/Packages/jdk-21.jdk/Contents/Home

mkdir $installdir

mkdir $builddir
cd $builddir
echo "fetching clang+llvm ..."
mkdir clang
cd clang
curl https://github.com/llvm/llvm-project/releases/download/llvmorg-13.0.1/clang+llvm-13.0.1-x86_64-apple-darwin.tar.xz | tar zt
curl -f -q -L https://github.com/llvm/llvm-project/releases/download/llvmorg-13.0.1/clang+llvm-13.0.1-x86_64-apple-darwin.tar.xz | tar zx --strip-components=1
cd ..

echo "fetching jextract sources ..."
git clone https://github.com/openjdk/jextract.git
cd jextract
git checkout jdk21
export JAVA_HOME=$java17

echo "building ..."
sh ./gradlew -Pjdk21_home=$java21 -Pllvm_home=$builddir/clang clean verify

echo "installing ..."
cp -r $builddir/jextract/build/jextract/ $installdir

echo "done"

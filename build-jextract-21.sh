#!/bin/sh
# builds jextract for jdk 21 for intel mac
set -e

dest=work/bin/jextract
if [ -d $dest ] ; then
  exit 0;
fi
./get-clang.sh

java17=`/usr/libexec/java_home -v 17`
java21=/Users/mhm/Packages/jdk-21.jdk/Contents/Home
builddir=work/jextract-src
rm -rf $builddir
mkdir $builddir

echo "fetching jextract sources ..."
git clone https://github.com/openjdk/jextract.git $builddir
cd $builddir
git checkout jdk21

echo "building ..."
export JAVA_HOME=$java17
sh ./gradlew -Pjdk21_home=$java21 -Pllvm_home=../bin/clang clean verify

echo "installing ..."
cd ../..
pwd
cp -r $builddir/build/jextract/ $dest

echo "done"

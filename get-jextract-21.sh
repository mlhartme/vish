#!/bin/sh
echo "get jextract for Mac x64 ... see https://github.com/openjdk/jextract and https://jdk.java.net/jextract/"
dest=work/bin/jextract
if [ ! -d $dest ] ; then
  mkdir -p $dest
  cd $dest
  curl https://download.java.net/java/early_access/jextract/1/openjdk-21-jextract+1-2_macos-x64_bin.tar.gz | tar zx --strip-components=1
fi


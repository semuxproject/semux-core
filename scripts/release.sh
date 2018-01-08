#!/bin/sh

commit=`git rev-parse --short=7 HEAD`
version=`cat pom.xml | grep '^    <version>.*</version>$' | awk -F'[><]' '{print $3}'`
version="$version-$commit"
name=semux

# change work directory
cd "$(dirname "$0")/../"

# build
mvn clean install -DskipTests || exit

# go to dist
cd dist

# Windows
WINDIST=${name}-windows-${version}
WINBALL=${WINDIST}.zip
mv windows ${WINDIST}
zip -r ${WINBALL} ${WINDIST} || exit
sha1sum ${WINBALL} > ${WINDIST}.sha1
sha1sum --check ${WINDIST}.sha1

# Linux
LINUXDIST=${name}-linux-${version}
LINUXBALL=${name}-linux-${version}.tar.gz
mv linux ${LINUXDIST}
tar -czvf ${LINUXBALL} ${LINUXDIST} || exit
sha1sum ${LINUXBALL} > ${LINUXDIST}.sha1
sha1sum --check ${LINUXDIST}.sha1

# macOS
MACDIST=${name}-macos-${version}
MACBALL=${name}-macos-${version}.tar.gz
mv macos ${MACDIST}
tar -czvf ${MACBALL} ${MACDIST} || exit
sha1sum ${MACBALL} > ${MACDIST}.sha1
sha1sum --check ${MACDIST}.sha1

# clean
rm -r ${name}-windows-${version}
rm -r ${name}-linux-${version} 
rm -r ${name}-macos-${version}

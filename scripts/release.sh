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

# Linux
LINUXDIST=${name}-linux-${version}
LINUXBALL=${name}-linux-${version}.tar.gz
mv linux ${LINUXDIST}
tar -czvf ${LINUXBALL} ${LINUXDIST} || exit

# macOS
MACDIST=${name}-macos-${version}
MACBALL=${name}-macos-${version}.tar.gz
mv macos ${MACDIST}
tar -czvf ${MACBALL} ${MACDIST} || exit

# Checksum
sha256sum *.tar.gz *.zip > sha256.txt

# clean
rm -r ${name}-windows-${version}
rm -r ${name}-linux-${version} 
rm -r ${name}-macos-${version}

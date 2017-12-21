#!/bin/sh

version=`cat pom.xml | grep '^    <version>.*</version>$' | awk -F'[><]' '{print $3}'`
name=semux

# change work directory
cd "$(dirname "$0")/../"

# build
mvn clean install -DskipTests || exit

# go to dist
cd dist

# Windows
mv windows ${name}-windows-${version}
zip -r ${name}-windows-${version}.zip ${name}-windows-${version} || exit

# Linux
mv linux ${name}-linux-${version}
tar -czvf ${name}-linux-${version}.tar.gz ${name}-linux-${version} || exit

# macOS
mv macos ${name}-macos-${version}
tar -czvf ${name}-macos-${version}.tar.gz ${name}-macos-${version} || exit

# clean
rm -r ${name}-windows-${version}
rm -r ${name}-linux-${version} 
rm -r ${name}-macos-${version}

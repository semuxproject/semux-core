#!/bin/sh

version=`cat pom.xml | grep '^    <version>.*</version>$' | awk -F'[><]' '{print $3}'`
name=semux-${version}

# change work directory
cd "$(dirname "$0")/../"

# build
mvn clean install -DskipTests || exit

# archive
cd dist
mv unix ${name}-unix
tar -czvf ${name}-unix.tar.gz ${name}-unix || exit
mv macos ${name}-macos
tar -czvf ${name}-macos.tar.gz ${name}-macos || exit
mv windows ${name}-windows
zip -r ${name}-windows.zip ${name}-windows || exit

# clean
rm -r ${name}-unix ${name}-macos ${name}-windows

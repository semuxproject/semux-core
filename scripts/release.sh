#!/bin/sh

version=`cat pom.xml | grep '^    <version>.*</version>$' | awk -F'[><]' '{print $3}'`
name=semux-${version}

# change work directory
cd "$(dirname "$0")/../"

# build
mvn clean && mvn install -DskipTests || exit

# archive
cd dist
mv unix ${name}-unix
tar -czvf ${name}-unix.tar.gz ${name}-unix || exit
mv windows ${name}-windows
zip -r ${name}-windows.zip ${name}-windows || exit


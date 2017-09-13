#!/bin/sh

version=`cat pom.xml | grep '^    <version>.*</version>$' | awk -F'[><]' '{print $3}'`
name=semux-${version}

# change work directory
cd "$(dirname "$0")/../"

# build
mvn clean && mvn install -DskipTests || exit

# archive
mv dist ${name}
tar -czvf ${name}.tar.gz ${name} || exit
zip -r ${name}.zip ${name} || exit

# clean
rm -fr ${name}

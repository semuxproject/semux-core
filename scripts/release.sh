#!/bin/sh

name=semux-1.0-alpha

# change work directory
cd "$(dirname "$0")/../"

# build
mvn clean && mvn install || exit

# archive
mv dist $name
tar -czvf $name.tar.gz $name || exit
zip -r $name.zip $name || exit

# clean
rm -fr $name

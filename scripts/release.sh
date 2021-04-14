#!/bin/sh

# Quick on failure
set -e

# Change the work directory
cd "$(dirname "$0")/.."

# Extract the name, version and revision
name=semux
version=`cat pom.xml | grep '^    <version>.*</version>$' | awk -F'[><]' '{print $3}'`
revision=`git rev-parse --short=7 HEAD`
label="$version-$revision"

# Make a clean build
mvn clean install -DskipTests

# Navigate to the dist folder
cd dist

# Make a Windows release
folder=$name-windows-$label
archive=$folder.zip
mv windows $folder
zip -r $archive $folder
rm -fr $folder

# Make a Linux release
folder=$name-linux-$label
archive=$folder.tar.gz
mv linux $folder
tar -czvf $archive $folder
rm -fr $folder

# Make a macOS release
folder=$name-macos-$label
archive=$folder.tar.gz
mv macos $folder
tar -czvf $archive $folder
rm -fr $folder
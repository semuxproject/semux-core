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

# Download JVM
wget -nc https://github.com/AdoptOpenJDK/openjdk11-binaries/releases/download/jdk-11.0.11%2B9/OpenJDK11U-jre_x64_windows_hotspot_11.0.11_9.zip
unzip OpenJDK11U-jre_x64_windows_hotspot_11.0.11_9.zip
mv jdk-11.0.11+9-jre ./windows/jvm

wget -nc https://github.com/AdoptOpenJDK/openjdk11-binaries/releases/download/jdk-11.0.11%2B9/OpenJDK11U-jre_x64_linux_hotspot_11.0.11_9.tar.gz
tar -xvf OpenJDK11U-jre_x64_linux_hotspot_11.0.11_9.tar.gz
mv jdk-11.0.11+9-jre ./linux/jvm

wget -nc https://github.com/AdoptOpenJDK/openjdk11-binaries/releases/download/jdk-11.0.11%2B9/OpenJDK11U-jre_x64_mac_hotspot_11.0.11_9.tar.gz
tar -xvf OpenJDK11U-jre_x64_mac_hotspot_11.0.11_9.tar.gz
mv jdk-11.0.11+9-jre/Contents/Home ./macos/jvm
rm -fr jdk-11.0.11+9-jre

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
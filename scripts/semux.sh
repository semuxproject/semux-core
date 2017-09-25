#!/bin/sh

# change work directory
cd "$(dirname "$0")"

# check java version
if ! [ -x "$(command -v java)" ]; then
    echo "Error: Java is not installed"
    exit 1
fi
version=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f2)
if [ $version -lt "8" ]; then
    echo "Error: Java 8 or above is required"
    exit 1
fi

# start kernel
java -Xms1g -Xmx3g -cp "./config:./lib/*" org.semux.Semux $@

#!/bin/sh

# change work directory
cd "$(dirname "$0")"

# start kernel
java -jar semux.jar $@
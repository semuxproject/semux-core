#!/bin/sh

# change work directory
cd "$(dirname "$0")"

# start kernel
java -cp "./config:./lib/*" org.semux.Semux $@

#!/bin/sh

# change work directory
cd "$(dirname "$0")"

# start kernel
java -cp semux.jar org.semux.wrapper.Wrapper \
--jvmoptions "" \
--gui "$@"

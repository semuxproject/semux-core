#!/bin/sh

# change work directory
cd "$(dirname "$0")"

# default JVM options
jvm_options=`java -cp semux.jar org.semux.JvmOptions`

# start kernel
java ${jvm_options} -cp semux.jar org.semux.Main --cli "$@"

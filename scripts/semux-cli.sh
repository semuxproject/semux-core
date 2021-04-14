#!/bin/sh

# change work directory
cd "$(dirname "$0")"

# Java binary
java_bin=./jvm/bin/java

# default JVM options
jvm_options=`${java_bin} -cp ./lib/semux.jar org.semux.JvmOptions --cli`

# start kernel
${java_bin} ${jvm_options} -cp ./lib/semux.jar org.semux.Main --cli "$@"

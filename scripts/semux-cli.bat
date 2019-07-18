@echo off

set java_bin=.\jvm\bin\java

%java_bin% -cp semux.jar org.semux.JvmOptions --cli > jvm_options.txt
set /p jvm_options=<jvm_options.txt

%java_bin% %jvm_options% -cp semux.jar org.semux.Main --cli %*

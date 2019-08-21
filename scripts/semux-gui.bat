@echo off

set java_bin=.\jvm\bin\javaw

%java_bin% -cp semux.jar org.semux.JvmOptions --gui > jvm_options.txt
set /p jvm_options=<jvm_options.txt

start "" "%java_bin%" %jvm_options% -cp semux.jar org.semux.Main --gui %*

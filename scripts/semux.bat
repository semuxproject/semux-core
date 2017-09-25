@echo off

java -Xms1g -Xmx3g -cp "./config;./lib/*" org.semux.Semux %*

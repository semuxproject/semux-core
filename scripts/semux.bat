@echo off

java -Xms512m -Xmx2g -cp "./config;./lib/*" org.semux.Semux %*

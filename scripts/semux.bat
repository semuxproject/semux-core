@echo off

start javaw -cp "./config;./lib/*" org.semux.Semux %* >> debug.log 2>&1 &
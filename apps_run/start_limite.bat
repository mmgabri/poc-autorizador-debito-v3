@echo off
set JAVA_OPTS=-XX:+UseG1GC -Xms2g -Xmx2g
java %JAVA_OPTS% -jar ..\apps\limite\target\limite-1.0-SNAPSHOT.jar
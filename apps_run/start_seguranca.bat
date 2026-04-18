@echo off
set JAVA_OPTS=-XX:+UseG1GC -Xms2g -Xmx2g
java %JAVA_OPTS% -jar ..\apps\seguranca\target\seguranca-1.0-SNAPSHOT.jar
pause
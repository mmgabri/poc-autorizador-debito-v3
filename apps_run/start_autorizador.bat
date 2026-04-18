@echo off
set JAVA_OPTS=-XX:+UseG1GC -Xms4g -Xmx4g
java %JAVA_OPTS% -jar ..\apps\autorizador\target\autorizador-1.0-SNAPSHOT.jar
pause

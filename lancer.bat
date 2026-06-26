@echo off
chcp 65001 > nul
setlocal

set MAVEN_OPTS=--sun-misc-unsafe-memory-access=allow

echo === Storymagine - Redacteur ===
echo.

mvn -pl redacteur -am compile exec:java

echo.
pause
endlocal
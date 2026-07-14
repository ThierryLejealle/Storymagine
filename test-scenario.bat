@echo off
chcp 65001 > nul
setlocal

set MAVEN_OPTS=--sun-misc-unsafe-memory-access=allow

echo === Storymagine - Testeur de scenario ===
echo.

mvn -pl chat -am compile exec:java -Dexec.mainClass=storymagine.chat.ui.cli.ScenarioTesterCli

echo.
pause
endlocal
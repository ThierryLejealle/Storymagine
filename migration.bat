@echo off
chcp 65001 > nul
setlocal

set MAVEN_OPTS=--sun-misc-unsafe-memory-access=allow

echo === Storymagine - Migration des scenarios chat ===
echo.
echo Option : --dry-run (liste ce qui serait fait, ne modifie rien)
echo.

mvn -q -pl chat -am compile exec:java -Dexec.mainClass=storymagine.chat.ui.cli.MigrateScenarioLayoutCli -Dexec.args="%*"

echo.
pause
endlocal
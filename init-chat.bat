@echo off
chcp 65001 > nul
setlocal

set MAVEN_OPTS=--sun-misc-unsafe-memory-access=allow

echo === Storymagine - Nouveau scenario de chat ===
echo.

mvn -q -pl chat -am compile exec:java -Dexec.mainClass=storymagine.chat.ui.cli.InitChatCli

echo.
pause
endlocal
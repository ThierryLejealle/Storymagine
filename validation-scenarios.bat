@echo off
chcp 65001 > nul
setlocal

set MAVEN_OPTS=--sun-misc-unsafe-memory-access=allow

echo === Storymagine - Validation des scenarios chat ===
echo.
echo Verifie chaque scenario sans toucher aux parties en cours.
echo Sans argument : chatscenarios du projet + repertoire des histoires.
echo Avec un ou plusieurs chemins : ces repertoires.
echo.

mvn -q -pl chat -am compile exec:java -Dexec.mainClass=storymagine.chat.ui.cli.ValidateScenarioCli -Dexec.args="%*"

echo.
pause
endlocal

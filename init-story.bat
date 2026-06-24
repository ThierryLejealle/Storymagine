@echo off
chcp 65001 > nul
setlocal EnableDelayedExpansion

set MAVEN_OPTS=--sun-misc-unsafe-memory-access=allow

set TEMPLATE_DIR=%~dp0scenarios\modele
if "!TEMPLATE_DIR:~-1!"=="\" set TEMPLATE_DIR=!TEMPLATE_DIR:~0,-1!

echo === Storymagine - Nouvelle histoire ===
echo.

mvn -pl redacteur -am compile exec:java -Dexec.mainClass="storymagine.redacteur.ui.cli.InitStoryCli" "-Dinit.template.dir=!TEMPLATE_DIR!"

echo.
pause
endlocal
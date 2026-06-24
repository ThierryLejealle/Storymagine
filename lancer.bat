@echo off
chcp 65001 > nul
setlocal EnableDelayedExpansion

set MAVEN_OPTS=--sun-misc-unsafe-memory-access=allow
set DEFAULT_SCENARIO_ROOT=c:\dev\llm\scenarios
set PROJECT_SCENARIO_ROOT=%~dp0scenarios
if "!PROJECT_SCENARIO_ROOT:~-1!"=="\" set PROJECT_SCENARIO_ROOT=!PROJECT_SCENARIO_ROOT:~0,-1!

echo === Storymagine - Redacteur ===
echo.

:: -- 1. Choix du repertoire des scenarios -------------------------------------
echo Ou se trouvent les scenarios ?
echo   ENTREE - Repertoire par defaut : %DEFAULT_SCENARIO_ROOT%
echo   1      - Repertoire du projet  : !PROJECT_SCENARIO_ROOT!
echo   2      - Autre repertoire...
echo.

:ask_dir
set DIR_CHOICE=
set /p DIR_CHOICE="Choix [ENTREE/1/2] : "

if "!DIR_CHOICE!"=="" (
    set SCENARIO_ROOT=%DEFAULT_SCENARIO_ROOT%
) else if "!DIR_CHOICE!"=="1" (
    set SCENARIO_ROOT=!PROJECT_SCENARIO_ROOT!
) else if "!DIR_CHOICE!"=="2" (
    set /p SCENARIO_ROOT="Chemin du repertoire : "
    if "!SCENARIO_ROOT:~-1!"=="\" set SCENARIO_ROOT=!SCENARIO_ROOT:~0,-1!
) else (
    echo  --> Entree invalide, choisir ENTREE, 1 ou 2.
    goto ask_dir
)

:: Verifier que le repertoire existe
if not exist "!SCENARIO_ROOT!" (
    echo.
    echo ERREUR : le repertoire '!SCENARIO_ROOT!' n'existe pas.
    echo.
    pause
    exit /b 1
)

echo.
echo Repertoire : !SCENARIO_ROOT!
echo.

mvn -pl redacteur -am compile exec:java "-Dredacteur.scenario.root=!SCENARIO_ROOT!"

echo.
pause
endlocal
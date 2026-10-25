@echo off
setlocal enabledelayedexpansion

chcp 65001 >nul

echo === Storymagine - Nettoyage des logs ===
echo.
echo Seront supprimes :
echo   - bench\ (logs testLLM et testMTP)
echo   - scenarios\*\generated\^<horodatage^>\ (logs de generation - fichiers exportes conserves)
echo   - chatscenarios\*\run\logs\ et chatscenarios\*\run\tests\ (logs et rapports du chat)
echo.
set /p CONFIRM=Continuer ? (o/n) : 
if /i not "%CONFIRM%"=="o" (
    echo Annule.
    goto :fin
)

echo.

if exist "%~dp0bench" (
    echo Suppression de bench\ ...
    rd /q /s "%~dp0bench"
) else (
    echo bench\ absent, rien a faire.
)

echo.
echo Nettoyage des logs de generation redacteur...
for /d %%S in ("%~dp0scenarios\*") do (
    if exist "%%S\generated" (
        for /d %%R in ("%%S\generated\*") do (
            echo   Suppression %%R
            rd /q /s "%%R"
        )
    )
)

echo.
echo Nettoyage des logs et rapports chat...
for /d %%S in ("%~dp0chatscenarios\*") do (
    if exist "%%S\run\logs" (
        echo   Suppression %%S\run\logs
        rd /q /s "%%S\run\logs"
    )
    if exist "%%S\run\tests" (
        echo   Suppression %%S\run\tests
        rd /q /s "%%S\run\tests"
    )
)

echo.
echo Nettoyage termine.

:fin
pause
endlocal
@echo off
setlocal

chcp 65001 >nul

if "%1"=="" (
    set MODE=menu
) else (
    set MODE=%1
)

powershell -NoProfile -Command "[Console]::OutputEncoding = [System.Text.Encoding]::UTF8; mvn -q -pl testllm -am compile exec:java '-Dexec.mainClass=storymagine.testllm.ui.cli.TestLlmCli' '-Dexec.args=%MODE%'; exit $LASTEXITCODE"

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ERREUR Maven (code %ERRORLEVEL%)
    exit /b 1
)
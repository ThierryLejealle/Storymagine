@echo off
setlocal

chcp 65001 >nul

powershell -NoProfile -Command "[Console]::OutputEncoding = [System.Text.Encoding]::UTF8; mvn -q -pl testllm -am compile exec:java '-Dexec.mainClass=storymagine.testllm.ui.cli.TestContextCli'; exit $LASTEXITCODE"

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ERREUR Maven (code %ERRORLEVEL%)
    exit /b 1
)
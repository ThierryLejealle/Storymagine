@echo off
echo Keepalive Ollama - ping toutes les 5 minutes du modele charge
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0keepalive.ps1"
echo.
echo Le script s'est arrete (erreur ou fermeture).
pause
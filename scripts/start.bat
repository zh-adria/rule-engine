@echo off
setlocal
rem ASCII wrapper to avoid cmd.exe code-page corruption in the real menu.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start.ps1" %*
endlocal

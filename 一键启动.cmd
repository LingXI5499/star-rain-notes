@echo off
setlocal
chcp 65001 >nul
title Star Rain Notes
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-dev.ps1" %*
set "START_RESULT=%ERRORLEVEL%"
if not "%START_RESULT%"=="0" (
    echo.
    pause
)
exit /b %START_RESULT%

@echo off
rem ======================================================================
rem   run.bat
rem ======================================================================
rem   Purpose:
rem   Starts Golf Analyst from the unzipped folder on Windows, with the
rem   Java already installed on this computer (17 or newer). It tells the
rem   program which folder it is in (golfanalyst.home), so your own data
rem   goes in Documents\Golf Analyst and never inside this folder.
rem
rem   Passed in:  nothing
rem   Passed out: nothing (the program's window opens)
rem ======================================================================
setlocal
where javaw >nul 2>nul
if errorlevel 1 (
  echo Java was not found. Golf Analyst needs Java 17 or newer.
  echo Install it from https://adoptium.net and then run this again.
  pause
  exit /b 1
)
start "" javaw "-Dgolfanalyst.home=%~dp0." -jar "%~dp0golfanalyst.jar"

@echo off
rem ======================================================================
rem   app.bat
rem ======================================================================
rem   Purpose:
rem   Opens the Golf Analyst window, building the program first if it has
rem   not been built yet.
rem
rem   Passed in:  optionally a data folder, as a full path in quotes if it
rem               has spaces. With nothing given the program uses the data
rem               folder next to it, or the sample-data folder if there is
rem               no data folder.
rem   Passed out: the program window
rem ======================================================================
setlocal
cd /d "%~dp0"

call jdk.bat
if errorlevel 1 exit /b 1

if not exist out\golfanalyst\gui\App.class (
  call build.bat
  if errorlevel 1 exit /b 1
)

if "%~1"=="" (
  "%JAVA_CMD%" -cp out golfanalyst.gui.App
) else (
  "%JAVA_CMD%" -cp out golfanalyst.gui.App %*
)
endlocal

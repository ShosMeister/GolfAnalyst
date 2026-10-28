@echo off
rem ======================================================================
rem   run.bat
rem ======================================================================
rem   Purpose:
rem   Prints the Golf Analyst reports for a data folder, building the
rem   program first if it has not been built yet.
rem
rem   Passed in:  the data folder, as a full path in quotes if it has
rem               spaces. With nothing given, the sample-data folder is
rem               used.
rem   Passed out: the reports, printed in this window
rem ======================================================================
setlocal
cd /d "%~dp0"

call "%~dp0jdk.bat"
if errorlevel 1 exit /b 1

if not exist out\golfanalyst\Main.class (
  call "%~dp0build.bat"
  if errorlevel 1 exit /b 1
)

if "%~1"=="" (
  "%JAVA_CMD%" -cp out golfanalyst.Main sample-data
) else (
  "%JAVA_CMD%" -cp out golfanalyst.Main %*
)
endlocal

@echo off
rem ======================================================================
rem   test.bat
rem ======================================================================
rem   Purpose:
rem   Builds Golf Analyst and runs its self-tests: hand-checked answers
rem   for the handicap rules, the statistics and the file loader, then
rem   checks of the Home and Round screens (no window is opened).
rem
rem   Passed in:  nothing
rem   Passed out: "N passed, 0 failed" when all is well; exit code 0 or 1
rem ======================================================================
setlocal
cd /d "%~dp0"

call jdk.bat
if errorlevel 1 exit /b 1

call build.bat
if errorlevel 1 exit /b 1

"%JAVA_CMD%" -cp out golfanalyst.Main --selftest
"%JAVA_CMD%" -Djava.awt.headless=true -cp out golfanalyst.gui.App --selftest
endlocal

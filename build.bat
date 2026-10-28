@echo off
rem ======================================================================
rem   build.bat
rem ======================================================================
rem   Purpose:
rem   Compiles Golf Analyst into the "out" folder.
rem
rem   Needs:      a JDK (not just a JRE), version 17 or newer. jdk.bat
rem               finds it.
rem   Passed in:  nothing
rem   Passed out: the "out" folder; exit code 0 if it built, 1 if not
rem ======================================================================
setlocal enabledelayedexpansion
cd /d "%~dp0"

call jdk.bat
if errorlevel 1 exit /b 1

if exist out rmdir /s /q out
mkdir out

set SOURCES=
for %%f in (src\golfanalyst\*.java src\golfanalyst\gui\*.java) do set SOURCES=!SOURCES! "%%f"

"%JAVAC_CMD%" -encoding UTF-8 -d out !SOURCES!
if errorlevel 1 (
  echo.
  echo Build FAILED.
  exit /b 1
)
echo Build OK.
endlocal

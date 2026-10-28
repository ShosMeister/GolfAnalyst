@echo off
rem ======================================================================
rem   jdk.bat
rem ======================================================================
rem   Purpose:
rem   Finds a JDK and tells the calling script where it is. The other
rem   scripts call this one so they all find Java the same way.
rem
rem   Looks, in this order: on the PATH, in JAVA_HOME, in the folders the
rem   common JDK installers use, and in the JDK bundled with NetBeans.
rem
rem   Passed in:  nothing
rem   Passed out: sets JAVAC_CMD (how to run the compiler) and JAVA_CMD
rem               (how to run a program) in the calling script.
rem               Exit code 0 if a JDK was found, 1 if not.
rem
rem   This script deliberately has no setlocal, so that the two variables
rem   survive after it ends.
rem ======================================================================
set "JAVAC_CMD="
set "JAVA_CMD="

where javac >nul 2>nul
if not errorlevel 1 (
  set "JAVAC_CMD=javac"
  set "JAVA_CMD=java"
  exit /b 0
)

if defined JAVA_HOME if exist "%JAVA_HOME%\bin\javac.exe" (
  set "JAVAC_CMD=%JAVA_HOME%\bin\javac.exe"
  set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
  exit /b 0
)

for /d %%d in ("C:\Program Files\Eclipse Adoptium\jdk*" "C:\Program Files\Java\jdk*" "C:\Program Files\Microsoft\jdk*" "C:\Program Files\Apache NetBeans\jdk" "D:\Program Files\Apache NetBeans\jdk") do (
  if exist "%%~d\bin\javac.exe" (
    set "JAVAC_CMD=%%~d\bin\javac.exe"
    set "JAVA_CMD=%%~d\bin\java.exe"
    echo Using the JDK found in %%~d
    exit /b 0
  )
)

echo javac was not found.
echo Install a JDK ^(version 17 or newer^) and make sure it is on the PATH,
echo or set JAVA_HOME to the JDK folder. A JRE alone is not enough, because
echo the compiler is part of the JDK.
echo To check, open a new Command Prompt and type:  javac -version
exit /b 1

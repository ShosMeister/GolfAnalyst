@echo off
rem ======================================================================
rem   make-installer.bat
rem ======================================================================
rem   Purpose:
rem   Builds the Windows installer for Golf Analyst:
rem     1. the jar, with Maven (NetBeans' own Maven is found if there is
rem        no mvn on the PATH);
rem     2. the program's files: the jar, images (every picture, exactly as
rem        it is, each copy checked byte for byte), help, sample-data and
rem        zipcodes.tsv;
rem     3. a trimmed Java runtime, with jdeps to find the modules the
rem        program uses and jlink to make it; then the built program checks
rem        with that runtime that every page finds its background picture;
rem     4. the installer, with jpackage (it needs the WiX Toolset, see
rem        docs\BUILDING.md);
rem     5. the "runs anywhere" zip for Mac and Linux (and Windows with its
rem        own Java): the program's files, run.bat, run.sh and its README.txt;
rem        installer\README.txt, for everyone, goes next to the installer.
rem   Everything it makes is in target\installer, which is not committed.
rem
rem   Passed in:  type=exe - a setup program (the default)
rem               type=msi - a Windows Installer package (handy for a silent
rem                          install: msiexec /i GolfAnalyst_v4.01.msi /qn)
rem               ("type=msi" and "type msi" both work, because Windows
rem               splits a word at the = sign)
rem   Passed out: target\installer\GolfAnalyst_v4.01.exe (or .msi),
rem               target\installer\GolfAnalyst_v4.01.zip and
rem               target\installer\README.txt
rem               Exit code 0 when it was made, 1 if a step failed.
rem ======================================================================
setlocal EnableExtensions
set "PROJECT=%~dp0.."
for %%p in ("%PROJECT%") do set "PROJECT=%%~fp"
set "APP_NAME=Golf Analyst"
rem The program's version (Theme.VERSION and pom.xml) and the same for jpackage, which wants three numbers: 4.01 is 4.0.1
set "PROGRAM_VERSION=4.01"
set "APP_VERSION=4.0.1"
set "FILE_NAME=GolfAnalyst_v%PROGRAM_VERSION%"
set "JAR_BUILT=golfanalyst-%PROGRAM_VERSION%.jar"
set "JAR_NAME=golfanalyst.jar"
set "UPGRADE_UUID=dd680a9f-f467-44e8-a52f-db33fa38a817"
set "TYPE=exe"

rem ---------------------------------------------------------------------
rem   The choice
rem ---------------------------------------------------------------------
:nextChoice
if "%~1"=="" goto choicesDone
set "WORD=%~1"
set "VALUE=%~2"
for /f "tokens=1,2 delims==" %%a in ("%~1") do (
  set "WORD=%%a"
  if not "%%b"=="" set "VALUE=%%b"
)
if /i not "%WORD%"=="type" goto badChoice
if /i "%VALUE%"=="exe" (set "TYPE=exe") else if /i "%VALUE%"=="msi" (set "TYPE=msi") else goto badChoice
echo %~1| "%SystemRoot%\System32\find.exe" "=" >nul
if errorlevel 1 shift
shift
goto nextChoice
:badChoice
echo Not understood: %*
echo Use: make-installer.bat [type=exe ^| type=msi]
exit /b 1
:choicesDone

set "OUT=%PROJECT%\target\installer"
set "WORK=%OUT%\work"
set "INPUT=%WORK%\input"
set "RESULT=%OUT%\%FILE_NAME%.%TYPE%"

rem ---------------------------------------------------------------------
rem   The tools: the JDK (with jpackage), Maven and WiX
rem ---------------------------------------------------------------------
call "%PROJECT%\jdk.bat"
if errorlevel 1 exit /b 1
set "JDK_BIN="
if /i "%JAVAC_CMD%"=="javac" (
  for %%f in (javac.exe) do set "JDK_BIN=%%~dp$PATH:f"
) else (
  for %%f in ("%JAVAC_CMD%") do set "JDK_BIN=%%~dpf"
)
if not exist "%JDK_BIN%jpackage.exe" (
  echo jpackage was not found next to %JAVAC_CMD%. It comes with JDK 17 and newer.
  exit /b 1
)
for %%f in ("%JDK_BIN%..") do set "JAVA_HOME=%%~ff"

set "MVN_CMD="
for %%f in (mvn.cmd) do if not "%%~$PATH:f"=="" set "MVN_CMD=%%~$PATH:f"
if not defined MVN_CMD (
  for %%d in ("C:\Program Files\Apache NetBeans" "D:\Program Files\Apache NetBeans") do (
    if exist "%%~d\java\maven\bin\mvn.cmd" set "MVN_CMD=%%~d\java\maven\bin\mvn.cmd"
  )
)
if not defined MVN_CMD (
  echo Maven was not found. Put mvn on the PATH, or install NetBeans, which has its own.
  exit /b 1
)

where wix >nul 2>nul
if errorlevel 1 if exist "%USERPROFILE%\.dotnet\tools\wix.exe" set "PATH=%USERPROFILE%\.dotnet\tools;%PATH%"
where wix >nul 2>nul
if errorlevel 1 where candle >nul 2>nul
if errorlevel 1 (
  echo The WiX Toolset was not found, so no installer can be made. See docs\BUILDING.md.
  exit /b 1
)

echo.
echo Building %APP_NAME% v%PROGRAM_VERSION% (installer version %APP_VERSION%, %TYPE%)
echo.

rem ---------------------------------------------------------------------
rem   1. The jar
rem ---------------------------------------------------------------------
echo [1/5] Building the jar with Maven
pushd "%PROJECT%"
call "%MVN_CMD%" -q -B package -DskipTests
if errorlevel 1 (
  popd
  echo Maven could not build the jar.
  exit /b 1
)
popd

rem ---------------------------------------------------------------------
rem   2. The program's files
rem ---------------------------------------------------------------------
echo [2/5] Gathering the program's files
if exist "%WORK%" rmdir /s /q "%WORK%"
mkdir "%INPUT%\images\backgrounds" "%INPUT%\help" "%INPUT%\sample-data"
if not exist "%PROJECT%\target\%JAR_BUILT%" (
  echo Maven did not make target\%JAR_BUILT%: is the version in pom.xml %PROGRAM_VERSION%?
  exit /b 1
)
copy /y "%PROJECT%\target\%JAR_BUILT%" "%INPUT%\%JAR_NAME%" >nul || goto copyFailed
copy /y "%PROJECT%\help\*.txt" "%INPUT%\help" >nul || goto copyFailed
copy /y "%PROJECT%\sample-data\courses.tsv" "%INPUT%\sample-data" >nul || goto copyFailed
copy /y "%PROJECT%\sample-data\rounds.tsv" "%INPUT%\sample-data" >nul || goto copyFailed
copy /y "%PROJECT%\zipcodes.tsv" "%INPUT%" >nul || goto copyFailed
echo       Pictures put in (each copy checked byte for byte):
set "PICTURES=0"
for %%f in ("%PROJECT%\images\*.*") do call :placePicture "%%~f" "%INPUT%\images" "images" || goto copyFailed
for %%f in ("%PROJECT%\images\backgrounds\*.*") do call :placePicture "%%~f" "%INPUT%\images\backgrounds" "images\backgrounds" || goto copyFailed
echo       %PICTURES% pictures in all.

rem ---------------------------------------------------------------------
rem   3. The trimmed Java runtime, and the built program's picture check
rem ---------------------------------------------------------------------
echo [3/5] Making a trimmed Java runtime
set "MODULES="
for /f "delims=" %%m in ('call "%JDK_BIN%jdeps.exe" --print-module-deps --ignore-missing-deps --multi-release 17 "%INPUT%\%JAR_NAME%"') do set "MODULES=%%m"
if not defined MODULES (
  echo jdeps could not list the modules.
  exit /b 1
)
echo       Modules: %MODULES%
"%JDK_BIN%jlink.exe" --add-modules %MODULES% --strip-debug --no-header-files --no-man-pages --compress=zip-6 --output "%WORK%\runtime"
if errorlevel 1 (
  echo jlink could not make the runtime.
  exit /b 1
)
echo       Checking the built program: every page and its background picture
"%WORK%\runtime\bin\java.exe" -Djava.awt.headless=true "-Dgolfanalyst.home=%INPUT%" -cp "%INPUT%\%JAR_NAME%" golfanalyst.gui.App --check-pictures
if errorlevel 1 (
  echo The built program is missing a background picture, so no installer was made.
  exit /b 1
)

rem ---------------------------------------------------------------------
rem   4. The installer
rem ---------------------------------------------------------------------
echo [4/5] Making the installer with jpackage
"%JDK_BIN%jpackage.exe" --type %TYPE% --name "%APP_NAME%" --app-version %APP_VERSION% --vendor "Angelo Mileto" ^
  --description "Golf scores, handicap and statistics" --copyright "Angelo Mileto" ^
  --input "%INPUT%" --main-jar %JAR_NAME% --main-class golfanalyst.gui.App --runtime-image "%WORK%\runtime" ^
  --java-options "-Dgolfanalyst.home=$APPDIR" --icon "%PROJECT%\installer\GolfAnalyst.ico" ^
  --license-file "%PROJECT%\installer\credits.txt" --win-per-user-install --win-menu --win-menu-group "%APP_NAME%" ^
  --win-shortcut --win-shortcut-prompt --win-upgrade-uuid %UPGRADE_UUID% --dest "%WORK%\package"
if errorlevel 1 (
  echo jpackage could not make the installer.
  exit /b 1
)
if exist "%RESULT%" del /q "%RESULT%"
move /y "%WORK%\package\%APP_NAME%-%APP_VERSION%.%TYPE%" "%RESULT%" >nul
if errorlevel 1 (
  echo The installer was made but could not be moved to %RESULT%.
  exit /b 1
)

rem ---------------------------------------------------------------------
rem   5. The "runs anywhere" zip
rem ---------------------------------------------------------------------
echo [5/5] Making the runs-anywhere zip
set "ZIP_NAME=%FILE_NAME%"
set "ZIP_DIR=%WORK%\zip\%ZIP_NAME%"
mkdir "%ZIP_DIR%"
xcopy /e /i /q /y "%INPUT%" "%ZIP_DIR%" >nul || goto copyFailed
copy /y "%PROJECT%\installer\zip\run.bat" "%ZIP_DIR%" >nul || goto copyFailed
copy /y "%PROJECT%\installer\zip\run.sh" "%ZIP_DIR%" >nul || goto copyFailed
copy /y "%PROJECT%\installer\zip\README.txt" "%ZIP_DIR%" >nul || goto copyFailed
copy /y "%PROJECT%\installer\README.txt" "%OUT%" >nul || goto copyFailed
if exist "%OUT%\%ZIP_NAME%.zip" del /q "%OUT%\%ZIP_NAME%.zip"
rem tar (in Windows 10 and later) writes zip paths with forward slashes, which Macs and Linux need
"%SystemRoot%\System32\tar.exe" -a -c -f "%OUT%\%ZIP_NAME%.zip" -C "%WORK%\zip" "%ZIP_NAME%"
if errorlevel 1 (
  echo The zip could not be made.
  exit /b 1
)
echo.
echo Made %RESULT%
echo Made %OUT%\%ZIP_NAME%.zip
echo Made %OUT%\README.txt
exit /b 0

rem ---------------------------------------------------------------------
rem   placePicture(file, folder, shown): copies one picture into the
rem   program's files, checks the copy is byte for byte the original, and
rem   lists it. Exit code 1 if the copy failed or differs.
rem ---------------------------------------------------------------------
:placePicture
copy /y "%~1" "%~2" >nul || exit /b 1
"%SystemRoot%\System32\fc.exe" /b "%~1" "%~2\%~nx1" >nul || exit /b 1
set /a PICTURES=PICTURES+1
echo         %~3\%~nx1
exit /b 0

:copyFailed
echo A program file could not be copied, or a copied picture differs from the original.
exit /b 1

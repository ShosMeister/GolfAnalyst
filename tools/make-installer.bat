@echo off
rem ======================================================================
rem   make-installer.bat
rem ======================================================================
rem   Purpose:
rem   Builds the Windows installer for Golf Analyst:
rem     1. the jar, with Maven (NetBeans' own Maven is found if there is
rem        no mvn on the PATH);
rem     2. the program's files: the jar, images, help, sample-data and
rem        zipcodes.tsv (with or without the background photographs);
rem     3. a trimmed Java runtime, with jdeps to find the modules the
rem        program uses and jlink to make it;
rem     4. the installer, with jpackage (it needs the WiX Toolset, see
rem        docs\BUILDING.md);
rem     5. the "runs anywhere" zip for Mac and Linux (and Windows with its
rem        own Java): the program's files, run.bat, run.sh and README.txt.
rem   A test copy also gets README-testers.txt, in the zip and next to the
rem   installer.
rem   Everything it makes is in target\installer, which is not committed.
rem
rem   Passed in (in any order; "photos=none" and "photos none" both work,
rem   because Windows splits a word at the = sign):
rem     photos=all   - every picture in images\backgrounds, exactly as it
rem                    is (the default for now: a TEST COPY, named
rem                    ...-testcopy, with the photographs' notice shown
rem                    by the installer)
rem     photos=none  - the public build: leaves out the pictures listed in
rem                    installer\photos-none-leave-out.txt
rem     type=exe     - a setup program (the default)
rem     type=msi     - a Windows Installer package (handy for a silent
rem                    install: msiexec /i GolfAnalyst-4.0.0.msi /qn)
rem   Passed out: target\installer\GolfAnalyst-4.0.0[-testcopy].exe/.msi and
rem               target\installer\GolfAnalyst-4.0.0[-testcopy].zip
rem               Exit code 0 when it was made, 1 if a step failed.
rem ======================================================================
setlocal EnableExtensions
set "PROJECT=%~dp0.."
for %%p in ("%PROJECT%") do set "PROJECT=%%~fp"
set "APP_NAME=Golf Analyst"
set "FILE_NAME=GolfAnalyst"
set "APP_VERSION=4.0.0"
set "JAR_BUILT=golfanalyst-4.00.jar"
set "JAR_NAME=golfanalyst.jar"
set "UPGRADE_UUID=dd680a9f-f467-44e8-a52f-db33fa38a817"
set "PHOTOS=all"
set "TYPE=exe"

rem ---------------------------------------------------------------------
rem   The choices
rem ---------------------------------------------------------------------
:nextChoice
if "%~1"=="" goto choicesDone
set "WORD=%~1"
set "VALUE=%~2"
for /f "tokens=1,2 delims==" %%a in ("%~1") do (
  set "WORD=%%a"
  if not "%%b"=="" set "VALUE=%%b"
)
if /i "%WORD%"=="photos" goto choosePhotos
if /i "%WORD%"=="type" goto chooseType
goto badChoice
:choosePhotos
if /i "%VALUE%"=="all" (set "PHOTOS=all") else if /i "%VALUE%"=="none" (set "PHOTOS=none") else goto badChoice
goto shiftChoice
:chooseType
if /i "%VALUE%"=="exe" (set "TYPE=exe") else if /i "%VALUE%"=="msi" (set "TYPE=msi") else goto badChoice
goto shiftChoice
:shiftChoice
echo %~1| "%SystemRoot%\System32\find.exe" "=" >nul
if errorlevel 1 shift
shift
goto nextChoice
:badChoice
echo Not understood: %*
echo Use: make-installer.bat [photos=all ^| photos=none] [type=exe ^| type=msi]
exit /b 1
:choicesDone

set "SUFFIX="
if "%PHOTOS%"=="all" set "SUFFIX=-testcopy"
set "OUT=%PROJECT%\target\installer"
set "WORK=%OUT%\work"
set "INPUT=%WORK%\input"
set "RESULT=%OUT%\%FILE_NAME%-%APP_VERSION%%SUFFIX%.%TYPE%"

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
echo Building %APP_NAME% %APP_VERSION% (%TYPE%, photos=%PHOTOS%)
if "%PHOTOS%"=="all" echo This is a TEST COPY build: it holds the background photographs.
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
copy /y "%PROJECT%\target\%JAR_BUILT%" "%INPUT%\%JAR_NAME%" >nul || goto copyFailed
copy /y "%PROJECT%\images\*.*" "%INPUT%\images" >nul || goto copyFailed
copy /y "%PROJECT%\help\*.txt" "%INPUT%\help" >nul || goto copyFailed
copy /y "%PROJECT%\sample-data\courses.tsv" "%INPUT%\sample-data" >nul || goto copyFailed
copy /y "%PROJECT%\sample-data\rounds.tsv" "%INPUT%\sample-data" >nul || goto copyFailed
copy /y "%PROJECT%\zipcodes.tsv" "%INPUT%" >nul || goto copyFailed
echo       Background pictures put in:
for %%f in ("%PROJECT%\images\backgrounds\*.*") do call :placePicture "%%~f"
if "%PHOTOS%"=="none" (
  echo       Left out ^(photos=none^):
  for /f "usebackq eol=# delims=" %%n in ("%PROJECT%\installer\photos-none-leave-out.txt") do echo         %%n
)

rem ---------------------------------------------------------------------
rem   3. The trimmed Java runtime
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

rem ---------------------------------------------------------------------
rem   4. The installer
rem ---------------------------------------------------------------------
echo [4/5] Making the installer with jpackage
if "%PHOTOS%"=="all" (
  copy /b "%PROJECT%\installer\testcopy-notice.txt" + "%PROJECT%\installer\credits.txt" "%WORK%\license.txt" >nul
) else (
  copy /y "%PROJECT%\installer\credits.txt" "%WORK%\license.txt" >nul
)
"%JDK_BIN%jpackage.exe" --type %TYPE% --name "%APP_NAME%" --app-version %APP_VERSION% --vendor "Angelo Mileto" ^
  --description "Golf scores, handicap and statistics" --copyright "Angelo Mileto" ^
  --input "%INPUT%" --main-jar %JAR_NAME% --main-class golfanalyst.gui.App --runtime-image "%WORK%\runtime" ^
  --java-options "-Dgolfanalyst.home=$APPDIR" --icon "%PROJECT%\installer\GolfAnalyst.ico" ^
  --license-file "%WORK%\license.txt" --win-per-user-install --win-menu --win-menu-group "%APP_NAME%" ^
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
set "ZIP_NAME=%FILE_NAME%-%APP_VERSION%%SUFFIX%"
set "ZIP_DIR=%WORK%\zip\%ZIP_NAME%"
mkdir "%ZIP_DIR%"
xcopy /e /i /q /y "%INPUT%" "%ZIP_DIR%" >nul || goto copyFailed
copy /y "%PROJECT%\installer\zip\run.bat" "%ZIP_DIR%" >nul || goto copyFailed
copy /y "%PROJECT%\installer\zip\run.sh" "%ZIP_DIR%" >nul || goto copyFailed
copy /y "%PROJECT%\installer\zip\README.txt" "%ZIP_DIR%" >nul || goto copyFailed
if "%PHOTOS%"=="all" (
  copy /y "%PROJECT%\installer\README-testers.txt" "%ZIP_DIR%" >nul || goto copyFailed
  copy /y "%PROJECT%\installer\README-testers.txt" "%OUT%" >nul || goto copyFailed
)
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
if "%PHOTOS%"=="all" (echo Build type: TEST COPY ^(photos=all^) - not for sharing.) else (echo Build type: public ^(photos=none^).)
exit /b 0

rem ---------------------------------------------------------------------
rem   placePicture(file): copies one background picture into the program's
rem   files, unless photos=none and it is on the leave-out list.
rem ---------------------------------------------------------------------
:placePicture
if "%PHOTOS%"=="none" (
  findstr /i /x /c:"%~nx1" "%PROJECT%\installer\photos-none-leave-out.txt" >nul
  if not errorlevel 1 exit /b 0
)
copy /y "%~1" "%INPUT%\images\backgrounds" >nul
echo         %~nx1
exit /b 0

:copyFailed
echo A program file could not be copied.
exit /b 1

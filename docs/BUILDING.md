# Building Golf Analyst

## Every day

- `build.bat` compiles into `out\`, `test.bat` runs both self-tests, `app.bat` opens the window from the project.
- NetBeans opens the folder as a Maven project (`pom.xml`, main class `golfanalyst.gui.App`).

Run from the project, the program reads and writes everything in the project folder, as it always has: `images\`,
`help\`, `sample-data\`, `zipcodes.tsv`, `golfanalyst.location` and your own `data\`.

## The Windows installer

```
tools\make-installer.bat                 GolfAnalyst_v4.01.exe, GolfAnalyst_v4.01.zip and README-testers.txt
tools\make-installer.bat type=msi        the same with a .msi (for a silent install: msiexec /i <file> /qn)
```

The results are in `target\installer\` (never committed). The script lists every picture that went in.

### What it needs

- A JDK 17 or newer with `jpackage`, `jlink` and `jdeps` (NetBeans' bundled JDK has them; `jdk.bat` finds it).
- Maven: `mvn` on the PATH, or NetBeans' own (`<NetBeans>\java\maven\bin\mvn.cmd`), found by the script.
- The WiX Toolset, which jpackage uses to make the .exe or .msi. WiX 5 is a .NET tool (needs the .NET SDK):

  ```
  dotnet tool install --global wix --version 5.0.2
  wix extension add -g WixToolset.Util.wixext/5.0.2 WixToolset.UI.wixext/5.0.2
  ```

  Without WiX, `jpackage --type app-image` can still make a folder with `Golf Analyst.exe` and its own Java, but no
  installer.

### What it does

1. `mvn package` makes `target\golfanalyst-4.01.jar` (the version in pom.xml; the script's `PROGRAM_VERSION` must
   match it, and `APP_VERSION` is the same in three numbers for jpackage).
2. Gathers the program's files in `target\installer\work\input`: the jar, every picture in `images\` and
   `images\backgrounds\` (each copy compared byte for byte with the original, and listed), `help\`,
   `sample-data\courses.tsv` and `rounds.tsv`, `zipcodes.tsv`.
3. `jdeps` lists the Java modules the program uses (`java.base,java.desktop`) and `jlink` makes a trimmed runtime
   with only those. Then the built program, run with that runtime on the gathered files
   (`golfanalyst.gui.App --check-pictures`, headless), opens every page and checks each finds its own background
   picture; if one does not, no installer is made.
4. `jpackage` makes the installer: name "Golf Analyst", version 4.0.1 (jpackage wants three numbers; the program
   itself says 4.01), vendor Angelo Mileto, per-user install (no administrator rights; it goes in
   `%LOCALAPPDATA%\Golf Analyst`), a Start menu entry, an offered desktop shortcut, the icon
   `installer\GolfAnalyst.ico`, and the license page from `installer\credits.txt` (the About text, the GeoNames
   credit and the photograph credit). The launcher passes
   `-Dgolfanalyst.home=$APPDIR`, which tells the program it is installed.

**A new version:** change `Theme.VERSION`, `pom.xml` and the script's `PROGRAM_VERSION` / `APP_VERSION` together
(4.01 is 4.0.1, 4.10 is 4.1.0). Because the upgrade UUID stays the same and the number is higher, installing it
replaces the old version in place.

**Never change the upgrade UUID** in the script (`UPGRADE_UUID`): it is how Windows knows a later version replaces
this one in place.

### Photographs

The four course photographs (round, course, stats, analyzer) are used with the written permission of On The Green
Magazine's editor, Alan Blondin (`docs\HimmelsbachPermission.pdf`). Every build includes every picture exactly as it
is; never move, resize, re-save or alter one, and never remove or change the www.onthegreenmagazine.com line on them.

### The icon

`installer\GolfAnalyst.ico` is made from the title bar's picture, `images\Home.png`, and committed:

```
java tools\MakeIcon.java images\Home.png installer\GolfAnalyst.ico
```

Home.png is only 80 x 80, so the 256 size is enlarged and a little soft; a bigger original would make it sharper.

## The "runs anywhere" zip

The same script also makes `target\installer\GolfAnalyst_v4.01.zip` for Mac and Linux (and Windows with
its own Java), until native installers can be built there (jpackage only builds for the system it runs on). It
holds the jar, the program's files, `run.bat`, `run.sh` and `README.txt` (from `installer\zip\`; Java 17 or newer is
needed) and `README-testers.txt`, which also goes next to the installer. The zip is made with
Windows' own `tar`, so its paths use forward slashes, as Macs and Linux need; `.gitattributes` keeps `run.sh` with
plain line ends. `run.bat` and `run.sh` pass `-Dgolfanalyst.home`, so the zip keeps your data in Documents too.

## Trying a build

```
msiexec /i target\installer\GolfAnalyst_v4.01.msi /qn      (after type=msi: silent install, no administrator rights)
msiexec /x target\installer\GolfAnalyst_v4.01.msi /qn      (silent uninstall)
```

## Where the installed program keeps things

The installed program never writes inside its own folder.

| What | Where |
|---|---|
| The program, images, help, sample data, ZIP list | `%LOCALAPPDATA%\Golf Analyst` (read only to the program) |
| Your own data (rounds, courses, settings, backups) | `Documents\Golf Analyst`, the real Documents folder (OneDrive too) |
| `golfanalyst.location` (a data folder chosen in Settings) | `%APPDATA%\Golf Analyst` |
| Your name and choices while you use the sample data | `%APPDATA%\Golf Analyst\sample-data\settings.txt` |

Uninstalling removes only the program; `Documents\Golf Analyst` and `%APPDATA%\Golf Analyst` are left alone.

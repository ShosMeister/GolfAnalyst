# Building Golf Analyst

## Every day

- `build.bat` compiles into `out\`, `test.bat` runs both self-tests, `app.bat` opens the window from the project.
- NetBeans opens the folder as a Maven project (`pom.xml`, main class `golfanalyst.gui.App`).

Run from the project, the program reads and writes everything in the project folder, as it always has: `images\`,
`help\`, `sample-data\`, `zipcodes.tsv`, `golfanalyst.location` and your own `data\`.

## The Windows installer

```
tools\make-installer.bat                 the TEST COPY (every photograph), GolfAnalyst-4.0.0-testcopy.exe
tools\make-installer.bat photos=none     the public build, GolfAnalyst-4.0.0.exe
tools\make-installer.bat type=msi        the same as a .msi (for a silent install: msiexec /i <file> /qn)
```

The results are in `target\installer\` (never committed). The script prints which background pictures went in and
which kind of build it was.

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

1. `mvn package` makes `target\golfanalyst-4.00.jar`.
2. Gathers the program's files in `target\installer\work\input`: the jar, `images\` (backgrounds as chosen),
   `help\`, `sample-data\courses.tsv` and `rounds.tsv`, `zipcodes.tsv`.
3. `jdeps` lists the Java modules the program uses (`java.base,java.desktop`) and `jlink` makes a trimmed runtime
   with only those.
4. `jpackage` makes the installer: name "Golf Analyst", version 4.0.0 (jpackage wants three numbers; the program
   itself says 4.00), vendor Angelo Mileto, per-user install (no administrator rights; it goes in
   `%LOCALAPPDATA%\Golf Analyst`), a Start menu entry, an offered desktop shortcut, the icon
   `installer\GolfAnalyst.ico`, and the license page from `installer\credits.txt` (with
   `installer\testcopy-notice.txt` in front for a test copy). The launcher passes
   `-Dgolfanalyst.home=$APPDIR`, which tells the program it is installed.

**Never change the upgrade UUID** in the script (`UPGRADE_UUID`): it is how Windows knows a later version replaces
this one in place.

### Photographs

- `photos=all` (the default for now): every picture in `images\backgrounds`, exactly as it is. The file name says
  `-testcopy` and the license page starts with the test-copy notice.
- `photos=none`: leaves out the pictures listed in `installer\photos-none-leave-out.txt` (the four course photos with
  the Himmelsbach Communications notice, and the Home picture until its source is confirmed). Those pages show their
  plain background; the screen self-test checks that every page still starts.

### The icon

`installer\GolfAnalyst.ico` is made from the title bar's picture, `images\Home.png`, and committed:

```
java tools\MakeIcon.java images\Home.png installer\GolfAnalyst.ico
```

Home.png is only 80 x 80, so the 256 size is enlarged and a little soft; a bigger original would make it sharper.

## Where the installed program keeps things

The installed program never writes inside its own folder.

| What | Where |
|---|---|
| The program, images, help, sample data, ZIP list | `%LOCALAPPDATA%\Golf Analyst` (read only to the program) |
| Your own data (rounds, courses, settings, backups) | `Documents\Golf Analyst`, the real Documents folder (OneDrive too) |
| `golfanalyst.location` (a data folder chosen in Settings) | `%APPDATA%\Golf Analyst` |
| Your name and choices while you use the sample data | `%APPDATA%\Golf Analyst\sample-data\settings.txt` |

Uninstalling removes only the program; `Documents\Golf Analyst` and `%APPDATA%\Golf Analyst` are left alone.

# GolfAnalyst v4.00 (Java) - notes for Claude

A single-golfer golf statistics program with a Swing window. Ported from HyperCard (1991) to Excel/VBA to Java.
Courses and rounds live in two plain tab-separated text files. Written for one user who tests on Windows.

## Build, run, test
- Windows scripts in the project folder: `build.bat`, `app.bat` (opens the window), `run.bat` (text reports), `test.bat`.
  `jdk.bat` finds a JDK (PATH, JAVA_HOME, common folders, NetBeans' bundled JDK). The scripts call each other by
  full path (`call "%~dp0jdk.bat"`) so they work when `NoDefaultCurrentDirectoryInExePath` is set; keep it that way.
  `test.bat` exits 1 if either test run fails.
- NetBeans opens it as a Maven project (`pom.xml`, source folder is `src`, main class `golfanalyst.gui.App`).
  The program must run with the project folder as its working folder: it reads `images/`, `help/` and `data/`
  (or `sample-data/` if there is no `data/`).
- Tests: `test.bat` runs the engine tests (`golfanalyst.Main --selftest`) and the screen tests
  (`golfanalyst.gui.App --selftest`, headless). No JUnit. **All tests must pass before a change is finished.**
- Java language level 17 (the pom sets release 17). The user's NetBeans bundles a newer JDK; keep to Java 17 features.
- The JDK in some environments is headless: a real window cannot be opened there. Screens are tested without a
  JFrame (see Architecture).

## Folder map
- `src/golfanalyst/`: `Tee`, `Round`, `Problem`, `Dataset`, `DataLoader` (reads/validates the two files),
  `Handicap` (the rules), `Stats`, `RoundWriter` (appends or replaces a round, keeps the file's column order),
  `CourseWriter` (appends or updates tees the same way, and refuses a line DataLoader would reject), `LineReplacer`
  (swaps lines in place and writes safely after a `Backups` copy; used by both writers), `Backups` (the one backup
  routine: a dated folder per backup in the data folder's `backups` folder), `DataStore` (the one place screens
  get data; also knows if it is the sample data), `Settings` (settings.txt), `DataLocation` (golfanalyst.location:
  which data folder to use), `Importer`, `Exporter`, `CourseDeleter`, `ZipCodes`, `StatsBoard` (the Stats Board's
  rows, totals, sorting and gold; no Swing), `BestRound` (Best Round's choices, best single round and composite; no
  Swing), `CourseAnalysis` (the Analyzer's figures; no Swing), `Period` (the Print window's periods with a given today),
  `Main` (text reports, `--selftest`), `SelfTest`.
- `src/golfanalyst/print/`: the printed reports, drawn by code with java.awt only (no Swing windows) in design units:
  `Sheet` (drawing helpers, colours), `Table` (list tables), `Card` (scorecard-style tables and the figures box),
  `Report` (the base: page furniture), `ReportData`, `RoundFigures`, the seven reports (`HandicapReport`,
  `RoundLogReport`, `ScorecardReport` (also Best Round), `StatsBoardReport`, `MyScorecardReport`, `CourseNotesReport`),
  `ReportPrintable` (PrinterJob), `Thumbnail` (the small picture), `Csv` (spreadsheet files), `Samples` (a tool that
  draws the sample PNGs), `PrintSelfTest` and `ReportSelfTest` (run by the engine's self-test). build.bat compiles it.
- `src/golfanalyst/gui/`: `App` (entry), `AppFrame` (thin JFrame), `AppPanel` (ALL the window logic: pages, header,
  footer, navigation, Help/Info, the sample-data question), `Shell` (what a page may ask of the window), `Page`,
  `HomePage`, `RoundPage`, `CoursePage`, `StatsPage`, `HeaderBar`, `FooterBar`, `HelpWindow` + `HelpFormatter` (styles the help
  text), `Theme` (colours, fonts, images, drawing helpers), custom controls
  (`WoodButton`, `IconButton`, `ValueBox`, `WoodPanel`, `WoodFrame`, `WoodTag`, `ImageCheckBox`, `BackdropPanel`,
  `WoodScrollBarUI`), dialogs (`ConfirmForm`, `ImportForm`, `ExportForm`, `SettingsForm`, `PrintForm` + `PagePreview`),
  `GuiSelfTest`.
- `images/` (wood buttons, textures, `backgrounds/<page>.jpg|gif`), `help/` (plain text Info and Help files),
  `sample-data/` (made up, plus the Edgewater course the user added, TeeIDs 5-8; its round 37 is the latest round, so
  New Round opens on Edgewater Senior and the screen tests expect its pars), `data/` (the user's real data, never overwrite it).

## Architecture rules (each one came from a real problem)
- The engine (`golfanalyst`) has no Swing code. Screens get data only from `DataStore`, and after any change the
  store reloads from the files, so what is on screen is what is in the files.
- Pages talk to the window only through `Shell`. **A page must not call the Shell while it is being constructed**
  (the window is not ready; this crashed the first run). Tests check it.
- Put window logic in `AppPanel`, not `AppFrame`, so it can be tested without a display.
- Never store totals, greens in regulation or sand saves. They are always calculated from score, putts, par and the
  sand flag. The files hold only what the golfer typed.
- IDs: every course/tee (`TeeID`) and every round (`RoundID`) is unique.
- Every page that writes to the files calls `Shell.readyToSave()` first. On `sample-data` it asks "This is the sample
  data. Create your own data folder first?" (Yes makes an empty `data/` beside it and saves nothing; No saves there;
  Cancel saves nothing). Dialogs go through `AppPanel`'s asker so tests can answer them.
- Changing existing lines (an edited round, edited tees) goes through `LineReplacer` (via `RoundWriter.replace`
  and `CourseWriter.update`): it splices in only those lines (same ID; columns the program does not know keep their
  value), leaves every other character of the file as it was, makes a backup copy first, and writes via a
  temporary file. Courses are checked with the loader before writing. Then the store reloads.
- Backups: one routine, `Backups.backupFolder(dataDir, reason, fileNames)`, called by `LineReplacer.write(file,
  text, reason)` and by Import, Clear and Delete course, so every save that changes or removes an existing line first
  copies the files into a folder of their own: `<data>/backups/YYYY-MM-DD_HHMMSS_reason` (`-2`, `-3` after the time
  in the same second), the copies keeping their REAL names (rounds.tsv, courses.tsv, settings.txt), so restoring is
  copying them back over the data folder's files. Reasons: `edit-round`, `delete-round`, `edit-course` (the everyday
  kinds, pruned to `BackupsToKeep` per kind, default 20, All keeps everything; the folder just made is never pruned)
  and `import`, `clear`, `delete-course` (never pruned). Older backups (dated `rounds-...`/`courses-...` files,
  `clear-...`, `import-...`, `delete-course-...` folders, `.bak` files) do not match the pattern and are never
  touched. Adding a round or tees makes no copy. If the copy fails it throws `Backups.Failed` (an IOException) before
  anything is written; the screen shows its message in the footer. If `BackupCopyFolder` is set, each new backup
  folder is also copied there; if that fails the save still happens and `DataStore.backupWarningNote()` adds a
  warning to the footer.
- Settings: `settings.txt` in the data folder, read by `Settings` (keys ignoring capitals) and always written back
  with PascalCase keys; unknown keys are kept. Keys: `Golfer`, `IncludeHistorical`, `BackupsToKeep` (10, 20, 50, 100
  or All), `BackupCopyFolder`, `LastExportFolder`. On the sample data only the name is saved (Export does not save
  `LastExportFolder` there). Which data folder to use is in `golfanalyst.location` next to the program
  (`DataLocation`), read by `App` after the command-line argument and before `data`/`sample-data`; Settings' Change...
  writes it (copying the data first if asked, never over a file) and it takes effect at the next start.
- Never write the byte-order mark as an invisible character in source: use `LineReplacer.BYTE_ORDER_MARK`.
- A page with unsaved changes overrides `Page.canLeave()` to ask "Discard your changes?" (`Shell.askYesNo`); the
  Home button, the arrows, Cancel and the window's close box all go through it. Round entry (anything typed since
  New Round), Edit Round and New Course (anything typed or ticked since the empty screen) ask. The close box is
  `DO_NOTHING_ON_CLOSE` plus `AppPanel.canQuit()` (v4.41): nothing needs cleaning up on exit (saves are written at
  once; edits via a temporary file), so it works from every screen and only unsaved typing is protected.

## The user's code style (strict; apply to everything, including edits)
- A large comment block at the start of every class and function: what it does, what is passed in, what is passed
  out. Use multi-line block comments `/* ... */`, not `//` on every line. A short 3-line block is fine for a tiny
  function. (The boxed look comes from `tools/boxfmt.py` if the `template/` folder exists; otherwise just match
  the existing files.)
- Multi-line comments must always be /* ... */. Short one-line remarks and section dividers may use //.
- The opening brace `{` always goes on its own line (classes, methods, if, for, try, switch...). `else`, `catch`
  and `finally` go on their own line under the closing brace. Array initializers like `{1, 2}` may stay on one line.
- Loop counters: never `i`, `j`, `k`. Use `lp`, then `lp1`, `lp2` when nested. Use `row`, `col`, `hole`, `x`, `y`
  when the loop walks something with a real name.
- Constants in ALL_CAPS. Variables in mixedCase with meaningful names (no single letters).
- No shorthand assignments: never `x++`, `x--`, `++x`, `x += y`, `x -= y` (and the like). Write them out:
  `x = x + 1`, `total = total + score`, including in `for` loops (`lp = lp + 1`) and lambdas. The user finds the
  shorthand easy to overlook when reading. Put brackets round a right-hand side that has a `? :` in it.
- For a simple fix, tell the user the file name and line numbers so they can edit it themselves instead of
  rebuilding a package.

## Domain rules already decided
- Handicap: current USGA / World Handicap System. Best 8 of the last 20 (table for fewer), needs 54 holes, net
  double bogey per hole (par + 5 before an index exists), differential = 113/slope x (adjusted gross - rating - PCC),
  Low Index, soft/hard caps, Exceptional Score Reduction. Nine-hole rounds: 9-hole differential + expected
  (0.52 x index + 1.2); pending until an index exists; a nine with no rating is skipped for the handicap but counts in
  statistics. A "score" belongs to a hole; the handicap counts **rounds** (use that word in text).
- Statistics: fairway % over par 4 and 5 only; GIR when (score - putts) <= par - 2; sand save = sand flag and
  score <= par; two-putt % means two putts or fewer. Putts count only strokes on the green; holed from off the green
  is 0 putts. Average, best and putts are kept separate for 18-hole and 9-hole rounds.
- Data files (tab separated, header row, columns found by name, `#` comments, per-hole values comma separated in one
  cell): `courses.tsv` (TeeID, Course, City, State, Zip, Tee, Holes, Rating, Slope, Yardage, Pars, StrokeIndex,
  FrontRating, FrontSlope, BackRating, BackSlope) and `rounds.tsv` (RoundID, TeeID, Date, Holes, PCC, Scores, Putts,
  Fairways, Sand, Historical). `Holes` in rounds is blank, F or B. One golfer; the name is in `data/settings.txt`.
- Historical rounds (v4.30): `Historical` = Y (optional column; blank or N otherwise) marks an old round kept only to
  look back at. `Round.historical()`; `Handicap.compute` always skips them (Handicap Index, Low Index, Rounds Used);
  the text reports use `Stats.current(rounds)`; the viewer shows them with "historical" in the 18-Hole Diff box and a footer
  note; Edit keeps the flag; Home's round counts leave them out; the start-up message says how many there are.
  The round screen's Historical box (`historicalCheck`, details frame column 3 under 2-Putt, a Sand-style check cell)
  sets it: white and open in New/Edit, grey in view, Tab order Date, Historical, Save, Space ticks it; ticking shows
  "historical" in 18-Hole Diff at once. Editing the file by hand still works. Saving a ticked round to a file with
  no Historical column adds the column to the end of the header line only (`RoundWriter.withHistoricalColumn`).
- Include historical rounds (v4.60): last row of Home's Golfer's Stats, an `ImageCheckBox` in a value cell, default
  ticked, saved as `IncludeHistorical=true|false` in `settings.txt` (`DataStore.includeHistorical()` /
  `setIncludeHistorical`; settings lines are read, changed and written back so the name and the choice keep each
  other). It decides `DataStore.overall()` (every Golfer's Stats figure and Total/18-Hole/9-Hole Rounds), the Stats
  Board and Best Round, all through `Stats.counted(rounds, includeHistorical)` (`StatsBoard.build` and
  `BestRound.choices/latestChoice/roundsOf` take the flag; their old forms leave historical rounds out). Never the
  handicap; the Rounds screen still shows every round. Unticked, the Stats Board and Best Round footers end with
  `StatsPage.HISTORICAL_LEFT_OUT`.
- Courses: never copy ratings, slopes, front/back nine ratings or yardage from one tee to another (the Excel version
  did; invented ratings spoil the handicap). The only copy allowed is the user pressing the arrow on the FIRST tee,
  which fills its pars and stroke index into EMPTY par/SI cells of the other three tees (never overwrites). A tee
  with a blank rating is skipped; the "skipped" warning counts only slope, front/back and yardage typed by hand,
  not pars/SI (the arrow may have filled them). The hole handicap column is shown as Hdcp (its official name is the stroke
  index; the header is bold 15 pt, the same font as the Par header and the cells, and has the tool tip "Hole handicap number (stroke index): 1 is the hardest hole.") and screen and help text call it the
  "handicap number": users know it as Handicap (US cards) or Stroke Index (UK and European cards). The file column
  and the code names (`StrokeIndex`, `strokeIndex`, `indexFields`) stay as they are. Tee names are unique within a course,
  ignoring capitals. Names are trimmed; capitals change only when the whole name was typed all lower or all upper
  case (so TPC Sawgrass and McDonough survive). State is two capital letters, zip five digits, yardage optional.
  A course is matched by name (any capitals/spaces) plus zip; new tees use the existing course's own spelling.

## Look and feel decided with the user
- One window: fixed light-wood header (Home, title in a sunken field, Help, Info, all three with a gold ring),
  changing middle page with its own background photo, fixed footer (status text; the gold progress bar shows only
  while a long job runs: `FooterBar.setProgress`).
- Wood buttons: white bold text, turns red under the mouse. Round buttons get a red ring on hover.
  Caption tags on light wood (text colour in `Theme.TAG_TEXT`). Boxes that cannot be typed in are grey.
- Round entry: columns Hole, Par, Score, Putt, Fwy, Sand. Digits only. Cursor goes down all scores (hole 9 to 10
  included) then down all putts. A score 2-9 or a putt moves on at once; a lone 1 in a score waits (ace or 10+).
  In a Putt box F and S tick Fwy and Sand (their scorecard notation).
- Round page look (entry, view, edit) matches New Course: Out and In tables in hugging `WoodFrame`s, spreadsheet
  cells 22 px tall with `GRID_LINE` hairlines under and right of each, columns Hole 34, Par 30, Score 42, Putt 38,
  Fwy 32, Sand 36; headings grey 11 pt, cells 14 pt bold; par always grey; Score/Putt/Date white to type, cream
  `FIELD_FOCUS` with a 2 px `GOLD` outline at the cursor, grey in view mode; Fwy/Sand check pictures 18 x 18 in white
  (grey in view) cells. Right of In: a results `WoodFrame` (teak inside, same 264 px height, rows level with the
  holes: Rating, Eagles..Others, 18-Hole Diff, Index After; `WoodTag` 86 x 20 in 90 x 22, values 82 px 13 pt).
  Under them, 6 px down and as wide (682): the details `WoodFrame` (teak inside; Date, Score, Putts / Fairway, Sand
  Saves, Greens In Reg / 2-Putt; Save + Cancel or Edit + Close, 112 x 32, at its right end). The arrows are centred
  8 px below. The group is centred on the photo; the arithmetic is in the `RoundPage` class comment.
- Peek (in `BackdropPanel`, so every page has it): press and hold the left button on the yellow version text (its
  bounds + 6 px) and only the photo and the text are painted (`paintChildren` skipped, `isPaintingOrigin` true so
  self-repainting controls stay hidden, keys swallowed); releasing anywhere shows everything. Hand cursor and the
  tool tip "Press and hold to see the whole picture." No page may put a control that takes the mouse over the label
  (see-through layout panels without mouse listeners are fine).
- Stats Board (v4.40, `StatsPage` on `StatsBoard`): one row per tee for 18-hole rounds, then a tan "Nine-hole rounds"
  band and rows per tee and nine ("Course:Tee (Front 9)" / "(Back 9)", or "Course:Tee" on a nine-hole course); never
  mixed. Columns Course : Tee Box 266 (left), Rating\Slope 96 ("70.4 / 123", "-" for an unrated nine), then eight of 62:
  Rnds, Avg, Best, Fwy %, GIR %, 2-Putt %, Putts/Rnd, Sand % (tool tip on Sand %). Home's definitions (it uses
  `Stats.summarize` per row); historical rounds follow Home's Include historical rounds box; one decimal with ".0" dropped (Home itself still shows
  ".0"); "-" for nothing to divide by. One `WoodFrame` centred with 14 px of photo round it: a pinned band (heading
  `WoodTag`s 3 px smaller than their 25 px slots, all one font size set by the headings alone, the sort shown as a
  small triangle in the tag's top right corner because no arrow fits beside "Putts/Rnd"; then tan total rows "All
  18-hole rounds" and "All nine-hole rounds", BOTH always shown, 0 under Rnds and dashes when empty) painted with
  `WoodFrame.paintFrameWood`, then a scroll pane of 25 px white hairline rows with the vertical bar always shown
  (`WoodScrollBarUI`, 17 px; track from the frame's own wood, thumb of turned light wood filling the track when
  everything fits). The "Nine-hole rounds" band row is always in the list. The list is never under 12 rows
  (`MIN_ROWS`): unused rows are blank hairline rows; an empty section shows a faint watermark row (13 pt italic,
  (165,160,150)): "No rounds yet. Press New Round on the Home screen." (no rounds at all; "No 18-hole rounds yet"
  when there are only nine-hole ones) and "No nine-hole rounds yet". Blank and watermark rows cannot be picked.
  Width 858 + 17 + 18 = 893; height 99 + 25 x rows (band always 81), 12 to 19 rows in a 626 px page (frame 399 to 574). Heading click sorts (again reverses), each section on its own, "-" always
  last, kept for the session. Gold (255,236,150, bold) = best per column per section among rows with 2+ rounds when
  2+ such rows exist, ties all gold. Click highlights a row (cream + 2 px gold outline), Up/Down move it over the band,
  Enter or double-click calls `Shell.browseRoundsOn(teeId)` (Rounds on the tee's newest round, Show set to the tee,
  so the arrows include that tee's 18-hole and nine-hole rounds). Close on that round goes back to the Stats Board
  (v4.41; `RoundPage.backToStats`, set by `showLatestOn`, cleared by `showLatest`/`showNew`; tool tip says where it
  goes); the Home button always goes Home.
- Best Round (v4.50): a second `RoundPage` made with `bestRound` true (card BEST in `AppPanel`), so it reuses every
  building block and calculation of the round viewer; `fillRound(round)` shows any round, saved or not. Top bar:
  Course / tee pulldown of `BestRound.Choice` (Stats Board kinds, sorted by course then tees, default the latest
  round's kind) and Single / Composite radio buttons (no PCC box, as in the user's mockup); no Edit, Save, Show or
  arrows; Close goes Home. Single = lowest total (tie: newest), title "Best Single: 00037:Edgewater:Senior".
  Composite = per hole the lowest score (tie: fewest putts, then newest), putts/Fwy/Sand from that round, shown as
  an unsaved Round (id `BestRound.COMPOSITE_ID`) with Date "Composite" and "-" for Diff and Index; title "Best
  Composite: Edgewater:Senior". Historical rounds follow Home's Include historical rounds box. Photo: `bestround` (any extension), else `BestRound`,
  else `round`. Footer: "Best of 5 rounds on X: 83 on 2026-10-22." / "Best score on every hole from 5 rounds on X:
  76. The best single round is 83." / "There are no rounds yet." (pulldown and buttons off).
- Edit Round: Home's Edit Round opens the round viewer at the latest round; the viewer has an Edit button (where
  Save sits when entering). Edit mode keeps Course/tee and Holes locked and opens date, PCC, scores, putts, Fwy and
  Sand; the header says "Edit 00031:Sample Links:White"; help files `round-edit-*`. Save replaces only that round
  (backup copy in the backups folder) and shows it read-only again; Cancel goes back to it as it was.
- New Course looks like the Round page: course bar (light panel, ordinary boxed text fields) on top; below, a
  centred row of five `WoodFrame`s 14 px apart over the photo (not stretched): a caption frame (WoodTags Tee name,
  Rating / Slope, Front 9, Back 9, Yards / Par, Hole, then hole numbers 14 pt bold) and one frame per tee. All
  frames have identical rows of 21 px. Cells are spreadsheet style: fixed 56 px wide, borderless, white, centred
  bold 15 pt, 1 px hairlines `Theme.GRID_LINE` (under every cell, right of the left cell of a pair); headings and
  total par grey (`FIELD_FACE`); the cursor's cell has a 2 px `GOLD` outline and `FIELD_FOCUS` cream fill. Sizes:
  frame 526 tall, page 616 of 626 px; 728 of 916 px wide. Frames sit in a see-through scroll pane; Save and Cancel
  (WoodButtons) below it. Tee cells are never greyed. Typing: par 3-6 moves on; stroke index 2-9 move on, a lone
  1 waits then 0-8; slope moves on after two digits from 5-9 or three from 1 (max 155); rating after the digit
  after the point. The cursor goes down one tee's pars, then its stroke index, then the next tee.
- New Course checks as you type, like round entry: Save stays off until everything is valid; the footer always
  names the next thing to fill in or fix by tee and hole ("Ready to save." when done). Only real mistakes go pink
  at once (tee name already used, rating 0, stroke index out of range or repeated); cells not filled yet stay
  white. Save goes Home with "Added 3 tees for Sample Links."
- Home buttons: data entry on the left, viewing on the right:
  left New Course, New Round, Edit Round, Import, Export, Clear; right Rounds, Stats Board, Analyzer, Best Round,
  Print, Settings (six a column; the grid fills row by row).
- Help: the i button says WHAT is on the screen; the ? button says HOW to use it. Files in `help/`
  (`home-*`, `round-new-*`, `round-view-*`, `round-edit-*`, `course-*`, `stats-*`, `best-*`, `analyzer-*`, `print-*`). Update them with every feature. Keep the user's friendly
  voice. The files stay plain text: the first line is the window title; a line starting with "# " is a section
  heading (put "# " in front of every new section heading); "Label: text" (colon within 45 characters, no period
  before it) shows the label in bold; "3. First sentence. More" shows the first sentence in bold
  (`HelpFormatter`, shown by `HelpWindow` on parchment under a wood title band). The help window is modal (v4.22):
  the main window cannot be used until it is closed, so the help always matches the screen it was opened from.
- Version numbers: Until the first published release the version stays at 4.00, whatever is added. Do not bump
  Theme.VERSION or pom.xml and do not add new version lines to help/home-info.txt. When a feature or fix is added, do
  two things: (a) add it to the matching group of the v4.00 entry in help/home-info.txt (the groups are: the opening
  line, Handicap and statistics, Rounds, Courses, Looking at your golf, Looking after your data, Around the program),
  keeping each paragraph short and in the same friendly voice; (b) add one line to CHANGELOG.md. After the first
  release: a bug fix raises the second pair of digits by one (4.01) and a feature raises the first of them (4.10),
  and each release gets its own history line. Release zips are named `GolfAnalyst_vX.XX.zip`. (The "(v4.30)"-style
  tags in these notes are the development milestones listed in CHANGELOG.md, not versions.)

## Status and backlog
Built: Home, round entry, saved-round viewer with the Show (course/tee) filter, Info/Help, New Course (v4.10: adds
a course or more tees to one; up to four tees at once; v4.11: wood-frame look, copy arrow, live checking), the
sample-data title/footer and save question, styled Help/Info windows, Edit Round and Edit Course (both v4.20), the New Course style round page
and the press-and-hold peek on the version text (v4.21), modal Help/Info windows (v4.22), historical rounds (v4.30), the
Stats Board (v4.40), Close back to the Stats Board and the close-box question (v4.41), Best Round (v4.50), the
Historical box on the round screens and Include historical rounds on Home (v4.60), the backups folder, Delete Round
and Clear (v4.70), dialogs sized to their text (v4.71), Import, Correct... and the ZIP code check (v4.80); since
then, still v4.00: every dialog in the wooden look, Delete course, backups as dated folders, Settings, Export, and the
Analyzer's figures (`CourseAnalysis`) and the Analyzer screen, and Print. Every Home button is built.
Not built either: deleting a tee, merging two courses.
- **Edit Course** (built, v4.20): no separate button. On New Course, typing a saved course's name and zip loads its
  tees (first four) into the frames; title "Edit Course"; empty frames take new tees. City/state editable (a change
  rewrites every tee of the course); nine-hole box locked. A loaded tee must keep its rating. Only changed tees are
  rewritten (a backup copy first); "Ready to save." adds a note when a changed tee has rounds. If the name/zip
  stops matching after edits, nothing is wiped: Save stays off until it matches again. Tidying a name never
  triggers a lookup (setText empties the box for a moment, which once wiped the frames).
- **Delete Round** (built, v4.70): `RoundPage.deleteButton` (WoodButton 112 x 24, 12 pt) in the details frame's third
  column under Historical, visible only in EDIT mode (never NEW, VIEW or Best Round). `readyToSave()` first, then
  `Shell.confirmTwice` (question naming the SAVED round, "Keep it" default / "Delete...", plus
  `RoundPage.UNSAVED_IGNORED` when boxes were changed; then "To confirm, type DELETE."). `DataStore.deleteRound` ->
  `RoundWriter.delete` (a `LineMaker` returning null removes the line and its own line break) -> `LineReplacer.write`
  (backup first). Then the previous round (else the next) read-only, or Home if none; footer "Round 00037 deleted. A
  copy of your rounds file as it was is in backups\2026-11-01_143012_delete-round."
- **Clear** (built, v4.70): Home's Clear -> `Shell.clearData()` -> `AppPanel.clearData`: `ConfirmForm.clearChoice`
  (two radio buttons, neither ticked, Continue off until one is), summary ("Keep my data" default / "Clear..."),
  type CLEAR. `DataStore.clear(everything)`: `Backups.backupFolder(..., CLEAR, ...)` into `backups/YYYY-MM-DD_HHMMSS_clear/`
  (never pruned; failure clears nothing), then each file keeps only its own header row via `LineReplacer.writeSafely`;
  everything also resets settings.txt. Goes Home; footer "Cleared. A copy of everything as it was is in
  backups\..._clear". Sample data: `CLEAR_SAMPLE_QUESTION` (Yes only makes the data folder) and `DataStore.clear`
  refuses it too. Dialogs are `ConfirmForm` panels shown by `AppPanel`'s `Confirmer`, which the tests replace.
- `ConfirmForm` is the ONE dialog helper; no page or window code uses JOptionPane or its own JDialog (only the system
  file picker behind Browse). Kinds: twoChoice, twoChoiceWithList, typed, clearChoice, yesNoCancel, yesNo (No default),
  notice, textInput; `withTitle` sets the band's title (Import, Delete round, Delete course, Clear data, Confirm,
  Discard changes, Save anyway, Sample data, Data problems, Save failed, Golfer's name). `dress(panel, title, content,
  buttons)` gives the look (the Import window uses it too): window title always "Golf Analyst"; 3 px (70,35,5)
  border; 46 px `Band` from Wood_Light.gif stretched, inner line (255,235,190,150) 2 px in, 3 px (70,35,5) bottom line,
  title 21 pt bold italic white with shadow (`Theme.drawCentered`); parchment `Theme.HELP_PAPER` body, 14 px
  top/bottom and 16 px side padding; message (60,35,10) 14 pt in `messageFont()` (system message font, Segoe UI on
  Windows) in a JTextArea wrapping at 420 px whose height is worked out for that width before pack() (a wrapping text
  box asked for its size with no width reports too few lines; that once hid the last line behind the buttons); 12 px
  gap; WoodButtons (`dialogButton`, at least 100 x 32, 10 px apart, right aligned; Browse 96 x 28); the default has
  `WoodButton.setDefaultRing` and the focus; Esc cancels, Enter presses the default; captions `caption` WoodTags
  96 x 24; boxes `textBox` white, hairline (185,180,170), 13 pt, 5 px inside; lists `readOnlyList`; check boxes
  ImageCheckBox + `checkLabel` 13 pt. `showPanel` packs, at least 440 wide, modal, centred on the program window.
  Pages reach dialogs through `Shell` (confirmTwice with title/list/prompt, confirmOnce, showNotice, askText, askYesNo).
  `GuiSelfTest.checkDialogLayouts` lays each out with its longest text and checks the band, border, WoodButtons,
  overlaps and the 12 px gap.
- **Delete course** (built, v4.00 before release): `CoursePage.deleteCourseButton` (130 x 32, the same bold 16 pt as
  Save and Cancel, on their line) in the bottom button bar, 50 px left of Save (not at the page's left edge: the peek
  label is there), Edit Course only.
  The course bar has no room for it: in Edit Course it is fullest (Correct... showing); Course box 210 px and City
  130 px leave about 45 px to spare (`GuiSelfTest.checkCourseBarFits` lays it out at 916 px). `readyToSave()` first, then `Shell.confirmTwice("Delete course", ...)` with
  `CourseDeleter.Summary.question` (tees, rounds, historical counted; `UNSAVED_IGNORED` when the screen has changes;
  "No rounds have been played on it." with no list when there are none) and the rounds list, then "To confirm, type
  the name of the course exactly: X" (`ConfirmForm.sameWords`: capitals and extra spaces ignored).
  `DataStore.deleteCourse` -> `CourseDeleter.delete`: `Backups.backupFolder(..., DELETE_COURSE, ...)` (never
  pruned; failure deletes nothing), splice out only the course's TeeIDs and the rounds on them (LineMaker null), write
  both to `.new` files, move courses then rounds; if the rounds move fails courses.tsv is copied back from the backup.
  Then Home with "Deleted X: 3 sets of tees and 12 rounds. A copy of your files as they were is in
  backups\..._delete-course". Getting it back: Import the backup folder (only the missing course and rounds are
  added). The Stats Board drops a highlight whose row is gone; Best Round and the viewer rebuild from the store.
- **Import** (built, v4.80): `Importer` (engine) `check` reads each data row alone with the loader (`onlyLine` blanks
  the others so line numbers stay true) and returns a `Plan`; nothing is written. Tees match by `sameName` course + tee
  (zips agree when both set): matched = skipped, mine never changed; new = next TeeIDs, course spelling of mine or of
  an earlier new tee. Rounds: TeeID refers to the imported courses file when one is given, else must be mine ("no tee
  with that number"); duplicate = same tee, date, holes, scores, putts (vs mine or earlier in file); new RoundIDs;
  "Mark as historical" sets Y. `apply`: `Backups.backupFolder(..., IMPORT, ...)` first, check both results with
  the loader, append via `LineReplacer.writeSafely` (a Historical header column may be added). `ImportForm` (gui) is
  the window, built with `ConfirmForm`'s pieces and shown by `ConfirmForm.showPanel`; `AppPanel.importData` (sample
  safeguard via `readyToSave`, `ImportAsker` hook for tests). Footer "Imported 3 tees and 35 rounds. 4 skipped, 2
  rejected. A copy of your files as they were is in backups\..._import."
- **Settings** (built, v4.00 before release): Home's Settings -> `Shell.openSettings()` -> `AppPanel.openSettings`:
  `SettingsForm` (shared dialog look; captions 190 px, boxes 300 px): Your name, Backups to keep, Also copy every
  backup to (Browse..., Clear; "not set"), Data folder (Change..., Open data folder; on the sample data "Create my own
  data folder" instead), Open backups folder. Nothing is saved until OK. Change... asks `SettingsForm.folderQuestion`
  (Use this folder as it is / Copy my data there and use it / Cancel; only the first when a data file is there).
  The folder picker, opening folders and questions go through `SettingsForm.Tools` (`setSettingsTools` in tests).
  A JTextField with a column count ignores a set preferred width: `setColumns(0)` first (Settings and Export do).
- **Export** (built, v4.00 before release): Home's Export -> `AppPanel.exportData`: `ExportForm` (Everything /
  One course and its rounds + course list / Rounds from a date + From and To (blank = today, both inclusive);
  Include historical rounds following Home; Save into + Browse... starting at `LastExportFolder`, else Documents; a
  line saying what will be written; Export off until a folder is chosen and something would be written).
  `Exporter.plan` then `Exporter.write`: a NEW folder `GolfAnalyst-export_YYYY-MM-DD_HHMMSS` (`-2`... if taken) with
  courses.tsv and rounds.tsv made of the golfer's OWN lines (same header, order, line endings, IDs; comments left
  out; Historical column dropped when no exported round is historical) and ReadMe.txt; temporary files then a move;
  any failure deletes the half-made folder. Only reads: no backup, works on the sample data (LastExportFolder is not
  saved there). Footer "Exported 12 sets of tees and 37 rounds to ...." Tests include a round trip through Import.
- **Analyzer data** (built, v4.00 before release): `CourseAnalysis` (engine). `rows`/`latestRow` are
  Best Round's choices; `analyse(row, rounds, tees, includeHistorical)` gives per hole par, rounds, counts and exact
  percentages in `CATEGORIES` (Dbl Eagle <= -3, Eagle, Birdie, Par, Bogey, Dbl Bogey, Other >= +3), average score and
  over par, best, worst, average putts, fairway % (par 4/5; NaN on par 3), GIR %, sand saves % (NaN without sand);
  Out/In (18-hole rows only) and All totals (the same figures per round over those holes); a Summary;
  `hardestHoles(n)`/`easiestHoles(n)` by average over par, ties by hole number (over par is worked out as strokes over
  par / count, not average minus par, so equal averages tie exactly in floating point).
- **Analyzer screen** (built, v4.00 before release): `AnalyzerPage` (card `ANALYZER`, `Shell.analyzer()`, Home's
  Analyzer button; background `analyzer.jpg`; help `analyzer-*`). Bar like Best Round's: Course / tee pulldown
  (`CourseAnalysis.rows`, default `latestRow`) and Counts / Percent / Averages (Counts ticked on every opening).
  Under it, centred: a caption `WoodFrame` (WoodTags 108 x 22 in 24 px rows) and 10 px right a grid `WoodFrame`
  (white JLabel cells 36 x 24, 13 pt, bold heading row, hairlines under and right; Out/In (18-hole rows only)/All in
  (236,236,236)). Width 130 + 10 + (21 x 36 + 22 = 778) = 918 of the 921 px inside the page's 12 px edges. Under
  both, a teak `WoodFrame` summary (Rounds, Avg score, Best score / Putts / round, Fairways, Greens / Sand saves,
  Hardest holes on (255,214,208), Easiest holes on (255,236,150), each list shown in hole-number order, `holeList`) with Close 112 x 32 (goes Home). Counts and Percent
  rows: Hole, Hole par, Rounds and the seven results (Percent rounded to whole numbers; totals are of holes played);
  Averages rows: Avg score, Avg vs par (signed one decimal, whole number in totals), Best, Worst, Avg putts, Fairway
  %, Greens %, Sand saves %. Zero counts/percentages and NaN show a faint "-" (170,165,155); in Averages a 0 is
  shown. Tints only with 2+ rounds: red `hardestHoles(3)`, gold the first three of `easiestHoles` not red, on the hole
  numbers and (Averages) the Avg vs par row. Footer "6 rounds on X." + `TINT_NOTE` (" Red marks your hardest holes, where you lose the most strokes; gold marks
  your easiest.") when tinted (always hardest / easiest, never best: best is a score) +
  `StatsPage.HISTORICAL_LEFT_OUT` when historical rounds are left out; no rounds: "There are no rounds yet.",
  controls off, an 18-hole grid of dashes. Tab order (`BarTraversal`): pulldown, Counts, Percent, Averages, Close.
- `docs/mockups/` holds the design pictures (committed) and the `print_actual_*.png` pictures drawn from the sample
  data by `golfanalyst.print.Samples` (run `java -cp out golfanalyst.print.Samples docs\mockups` after build.bat) to
  compare with them. `Incoming/` (git-ignored) is where the user drops new pictures; move them to docs/mockups.
- **Print** (built, v4.00 before release): Home's Print -> `Shell.printReports()` -> `AppPanel.printReports` ->
  `PrintForm` (shared dialog look, title "Print"; no fixed heights: every part is sized from its contents, 8 px spare under the last hint, 760 x 538
  here; `GuiSelfTest.checkPrintDialogSizes` builds it with every font 20% larger via `Theme.setFontScale` and checks
  nothing is cut off). Left: seven radio choices with
  grey hints (`PrintForm.Kind`). Right: a sunken panel (dark grey top/left, white bottom/right, face (251,247,238))
  with the report's name, its options (one card each) and `PagePreview` (188 x 150: page 1 of the real report via
  `Thumbnail`). "Send it to:" A printer or PDF / A spreadsheet file (.csv) (off with the tool tip "Only for the
  reports that are tables" for the three scorecards), a grey note box sized to its words, Cancel and Print... (Save...).
  Options: Round Log tees ("All courses" first, "Course:Tee" names), `Period` (All time ... Custom dates...; From/To
  grey unless Custom, typed as on the round screen) and Include historical rounds; a round's scorecard: rounds newest
  first; Best Round / Course notes: Best Round's / the Analyzer's rows; Stats Board: Include historical rounds only
  (the screen has no Show choices); My scorecard: every set of tees and the index line. Printing goes through
  `PrintForm.Tools` (`AppPanel.setPrintTools` in tests): `ReportPrintable.send` shows the system print dialog
  (orientation set: Handicap Report upright, the rest on their side) and scales the design width to the printable
  width, the printable height deciding the rows per page. Footer "Sent to the printer." / "Saved N rows to ....".
  Reports: design units 0.01 inch, Letter 850 x 1100 or 1100 x 850, margins 60; furniture in `Report`; the sample
  note only on the sample data. Handicap Report: the counting rounds are the engine's own lowest differentials on
  record (ties to the older round), the Differential column the round's own differential; the first note names an
  exceptional score reduction, a cap or the short-record adjustment when they change the index. Round Log: pages
  repeat the header, "(continued)" from page 2, the note under every page's table with only that page's parts
  (room kept for it: 22 rounds a Letter page), the summary only on the last page and never split. Tables draw the
  dagger and H bold 15, cards their check marks bold 14. My Scorecard: Strokes blank where a hole gives none (totals
  kept), no Pick-up totals. The Stats Board's widths are the screen's scaled to fill the 980 units; its shading is
  `StatsBoard.best`, the screen's own rule (Best column included). Course notes tints the Avg vs par cells as the
  hole numbers. `Samples` draws with the test name "Angelo" in memory (the sample data has no name). The All courses
  Round Log uses the user's widths, 1016 wide: 36 units past the right margin. Spreadsheet files (`Csv`): UTF-8 with
  a byte order mark, quoted fields, CRLF, written via a temporary file; the save window starts in LastExportFolder
  (else Documents) and a file already there is replaced only after `askReplace`. The Print window is modal, so the header's Help
  and Info are behind it: it has its own i and ? `IconButton`s (32 px, bottom left) that open `print-info` and
  `print-help` through `PrintForm.Tools.showHelp`; `HelpWindow.show` takes any `Window` as owner for this.
- **Correct...** (built, v4.80): in Edit Course `courseField`/`zipField` are locked; `CoursePage.correctButton`
  (90 x 24) sets `correcting` (no lookups while it is on). Save: `DataStore.correctCourse` ->
  `CourseWriter.correctCourse` rewrites only the Course, Zip, City and State cells of every tee (backup first);
  another course's name+zip is `SAME_COURSE` with both cells pink; no change is `NOTHING_CORRECTED`.
- **ZIP code check** (built, v4.80): `zipcodes.tsv` in the program folder (40,979 rows from GeoNames US.zip: postal
  code, place name, state code; military APO/FPO rows have no state and are left out). `ZipCodes` (engine) loads it
  once (`standard()`), quietly empty if missing; `check` gives NONE/FILL/MATCH/STATE_DIFFERS/CITY_DIFFERS/UNKNOWN.
  CoursePage: `zipChanged` fills an empty city AND state when the zip changes; `refresh` puts the note first in the
  footer and outlines State in `ZIP_WARNING` orange on STATE_DIFFERS; Save then asks `Shell.confirmOnce` ("... Save
  anyway?", Go back default). Tests use `useZipCodes` (the older course tests run with an empty list).

## Working agreements
- Explain decisions plainly and give a recommendation; say when something is untested (no Windows, no real window
  in some environments). Ask for the exact error text when something fails on the user's machine.
- Do not invent course ratings or other data. Keep the user's data files safe; never write over them.

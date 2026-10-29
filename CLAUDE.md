# GolfAnalyst v4.22 (Java) - notes for Claude

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
  (swaps lines in place and writes safely with a `.bak`; used by both writers), `DataStore` (the one place screens
  get data; also knows if it is the sample data), `Main` (text reports, `--selftest`), `SelfTest`.
- `src/golfanalyst/gui/`: `App` (entry), `AppFrame` (thin JFrame), `AppPanel` (ALL the window logic: pages, header,
  footer, navigation, Help/Info, the sample-data question), `Shell` (what a page may ask of the window), `Page`,
  `HomePage`, `RoundPage`, `CoursePage`, `HeaderBar`, `FooterBar`, `HelpWindow` + `HelpFormatter` (styles the help
  text), `Theme` (colours, fonts, images, drawing helpers), custom controls
  (`WoodButton`, `IconButton`, `ValueBox`, `WoodPanel`, `WoodFrame`, `WoodTag`, `ImageCheckBox`, `BackdropPanel`),
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
  value), leaves every other character of the file as it was, copies the file to `<file>.bak`, and writes via a
  temporary file. Courses are checked with the loader before writing. Then the store reloads.
- Never write the byte-order mark as an invisible character in source: use `LineReplacer.BYTE_ORDER_MARK`.
- A page with unsaved changes overrides `Page.canLeave()` to ask "Discard your changes?" (`Shell.askYesNo`); the
  Home button, the arrows and Cancel all go through it.

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
  Fairways, Sand). `Holes` in rounds is blank, F or B. One golfer; the name is in `data/settings.txt`.
- Courses: never copy ratings, slopes, front/back nine ratings or yardage from one tee to another (the Excel version
  did; invented ratings spoil the handicap). The only copy allowed is the user pressing the arrow on the FIRST tee,
  which fills its pars and stroke index into EMPTY par/SI cells of the other three tees (never overwrites). A tee
  with a blank rating is skipped; the "skipped" warning counts only slope, front/back and yardage typed by hand,
  not pars/SI (the arrow may have filled them). The hole handicap column is shown as Hdcp/SI (header 11 pt bold,
  tool tip "Hole handicap number (stroke index): 1 is the hardest hole.") and screen and help text call it the
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
- Edit Round: Home's Edit Round opens the round viewer at the latest round; the viewer has an Edit button (where
  Save sits when entering). Edit mode keeps Course/tee and Holes locked and opens date, PCC, scores, putts, Fwy and
  Sand; the header says "Edit 00031:Sample Links:White"; help files `round-edit-*`. Save replaces only that round
  (backup in `rounds.tsv.bak`) and shows it read-only again; Cancel goes back to it as it was.
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
  left New Course, New Round, Edit Round, Import, Clear; right Rounds, Stats Board, Analyzer, Best Round, Print.
- Help: the i button says WHAT is on the screen; the ? button says HOW to use it. Files in `help/`
  (`home-*`, `round-new-*`, `round-view-*`, `round-edit-*`, `course-*`). Update them with every feature. Keep the user's friendly
  voice. The files stay plain text: the first line is the window title; a line starting with "# " is a section
  heading (put "# " in front of every new section heading); "Label: text" (colon within 45 characters, no period
  before it) shows the label in bold; "3. First sentence. More" shows the first sentence in bold
  (`HelpFormatter`, shown by `HelpWindow` on parchment under a wood title band). The help window is modal (v4.22):
  the main window cannot be used until it is closed, so the help always matches the screen it was opened from.
- Version numbers: second pair of digits, `.0x` is a bug fix and `.x0` is a feature (v4.00 -> v4.01 fix, v4.10
  feature). Set in `Theme.VERSION` and `pom.xml`. Release zips are named `GolfAnalyst_vX.XX.zip`.

## Status and backlog
Built: Home, round entry, saved-round viewer with the Show (course/tee) filter, Info/Help, New Course (v4.10: adds
a course or more tees to one; up to four tees at once; v4.11: wood-frame look, copy arrow, live checking), the
sample-data title/footer and save question, styled Help/Info windows, Edit Round and Edit Course (both v4.20), the New Course style round page
and the press-and-hold peek on the version text (v4.21).
Not built (buttons are in place and say "not built yet"): Import, Clear, Stats Board, Analyzer, Best Round, Print.
Not built either: deleting a round or a tee, renaming a course or changing its zip.
- **Edit Course** (built, v4.20): no separate button. On New Course, typing a saved course's name and zip loads its
  tees (first four) into the frames; title "Edit Course"; empty frames take new tees. City/state editable (a change
  rewrites every tee of the course); nine-hole box locked. A loaded tee must keep its rating. Only changed tees are
  rewritten (`courses.tsv.bak` first); "Ready to save." adds a note when a changed tee has rounds. If the name/zip
  stops matching after edits, nothing is wiped: Save stays off until it matches again. Tidying a name never
  triggers a lookup (setText empties the box for a moment, which once wiped the frames).
- **Clear**: always ask twice and make a dated backup of the files first; it is how the program is reset for a
  different golfer.
- **Import**: bulk rounds and/or courses from the program's own tab-separated format; show what will be added and
  what is rejected (with line numbers) before writing anything; give duplicate IDs fresh numbers.

## Working agreements
- Explain decisions plainly and give a recommendation; say when something is untested (no Windows, no real window
  in some environments). Ask for the exact error text when something fails on the user's machine.
- Do not invent course ratings or other data. Keep the user's data files safe; never write over them.

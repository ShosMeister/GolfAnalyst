# GolfAnalyst v4.00 (Java) - notes for Claude

A single-golfer golf statistics program with a Swing window. Ported from HyperCard (1991) to Excel/VBA to Java.
Courses and rounds live in two plain tab-separated text files. Written for one user who tests on Windows.

## Build, run, test
- Windows scripts in the project folder: `build.bat`, `app.bat` (opens the window), `run.bat` (text reports), `test.bat`.
  `jdk.bat` finds a JDK (PATH, JAVA_HOME, common folders, NetBeans' bundled JDK).
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
  `Handicap` (the rules), `Stats`, `RoundWriter` (appends a round, keeps the file's column order), `DataStore`
  (the one place screens get data), `Main` (text reports, `--selftest`), `SelfTest`.
- `src/golfanalyst/gui/`: `App` (entry), `AppFrame` (thin JFrame), `AppPanel` (ALL the window logic: pages, header,
  footer, navigation, Help/Info), `Shell` (what a page may ask of the window), `Page`, `HomePage`, `RoundPage`,
  `HeaderBar`, `FooterBar`, `HelpWindow`, `Theme` (colours, fonts, images, drawing helpers), custom controls
  (`WoodButton`, `IconButton`, `ValueBox`, `WoodPanel`, `WoodFrame`, `WoodTag`, `ImageCheckBox`, `BackdropPanel`),
  `GuiSelfTest`.
- `images/` (wood buttons, textures, `backgrounds/<page>.jpg|gif`), `help/` (plain text Info and Help files),
  `sample-data/` (random, not real), `data/` (the user's real data, never overwrite it).

## Architecture rules (each one came from a real problem)
- The engine (`golfanalyst`) has no Swing code. Screens get data only from `DataStore`, and after any change the
  store reloads from the files, so what is on screen is what is in the files.
- Pages talk to the window only through `Shell`. **A page must not call the Shell while it is being constructed**
  (the window is not ready; this crashed the first run). Tests check it.
- Put window logic in `AppPanel`, not `AppFrame`, so it can be tested without a display.
- Never store totals, greens in regulation or sand saves. They are always calculated from score, putts, par and the
  sand flag. The files hold only what the golfer typed.
- IDs: every course/tee (`TeeID`) and every round (`RoundID`) is unique.

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

## Look and feel decided with the user
- One window: fixed light-wood header (Home, title in a sunken field, Help, Info, all three with a gold ring),
  changing middle page with its own background photo, fixed footer (status text; the gold progress bar shows only
  while a long job runs: `FooterBar.setProgress`).
- Wood buttons: white bold text, turns red under the mouse. Round buttons get a red ring on hover.
  Caption tags on light wood (text colour in `Theme.TAG_TEXT`). Boxes that cannot be typed in are grey.
- Round entry: columns Hole, Par, Score, Putt, Fwy, Sand. Digits only. Cursor goes down all scores (hole 9 to 10
  included) then down all putts. A score 2-9 or a putt moves on at once; a lone 1 in a score waits (ace or 10+).
  In a Putt box F and S tick Fwy and Sand (their scorecard notation).
- Home buttons: data entry on the left, viewing on the right:
  left New Course, New Round, Edit Round, Import, Clear; right Rounds, Stats Board, Analyzer, Best Round, Print.
- Help: the i button says WHAT is on the screen; the ? button says HOW to use it. Files in `help/`
  (`home-*`, `round-new-*`, `round-view-*`). Update them with every feature. Keep the user's friendly voice.
- Version numbers: second pair of digits, `.0x` is a bug fix and `.x0` is a feature (v4.00 -> v4.01 fix, v4.10
  feature). Set in `Theme.VERSION` and `pom.xml`. Release zips are named `GolfAnalyst_vX.XX.zip`.

## Status and backlog
Built: Home, round entry, saved-round viewer with the Show (course/tee) filter, Info/Help.
Not built (buttons are in place and say "not built yet"): New Course, Edit Round, Import, Clear, Stats Board,
Analyzer, Best Round, Print.
- **New Course** (next). From the Excel and HyperCard versions: ask course name and 5-digit zip first; if the course
  exists, offer to add more tees (editing existing rows is not built). City in proper case, State two capital
  letters, Zip five digits. Up to four tee sets entered at once, named by default Ladies, Beginner, Intermediate,
  Expert (renamable): name, rating, slope, yardage, 18 pars (3-6), 18 stroke index numbers (each 1-18 once),
  optional front-nine and back-nine rating and slope, and an option for a nine-hole course (Holes = 9, stroke index
  1-9). A tee whose rating is blank is skipped. Do NOT copy numbers from another tee to fill it in (the Excel
  version did; invented ratings would spoil the handicap). New TeeIDs are max + 1, appended to `courses.tsv` in the
  file's own column order (model it on `RoundWriter`). Digits-only boxes with the same auto-advance idea, and the
  page background is `images/backgrounds/course.jpg`.
- **Edit Round**: needs replacing one line of `rounds.tsv` without disturbing comments or other lines.
- **Clear**: always ask twice and make a dated backup of the files first; it is how the program is reset for a
  different golfer.
- **Import**: bulk rounds and/or courses from the program's own tab-separated format; show what will be added and
  what is rejected (with line numbers) before writing anything; give duplicate IDs fresh numbers.

## Working agreements
- Explain decisions plainly and give a recommendation; say when something is untested (no Windows, no real window
  in some environments). Ask for the exact error text when something fails on the user's machine.
- Do not invent course ratings or other data. Keep the user's data files safe; never write over them.

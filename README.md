# GolfAnalyst v4.00 (Java)

Golf Analyst with its window. It has the one-window layout of the earlier versions: a wood header
with Home, Help and Info buttons that stays the same on every screen, a status footer that stays
the same, and a middle section that changes, each page with its own golf photo behind it. Behind
the window is the engine: data loading and checking, the USGA / World Handicap System Handicap
Index (18-hole and 9-hole rounds), and the performance statistics.

Needs a JDK, version 17 or newer (not just a "Java runtime"; the compiler is part of the JDK).
No other libraries.

## Running it

**In NetBeans** (the usual way): File > Open Project, choose this folder (it shows a Maven icon).
Right-click the project > Properties > Run and set the main class to `golfanalyst.gui.App`. The
project file already sets it, so Run (F6) should work as it is. To open a different data folder,
put its path in the Arguments box. The program must run with this project folder as its working
folder (NetBeans does that) so it can find the `images`, `help` and data folders.

**From a Command Prompt or PowerShell in this folder (Windows):**

    app.bat                      opens the window
    app.bat "C:\Golf\my data"      opens the window on your own data folder (use a full path)
    build.bat                    compiles into the "out" folder (jdk.bat finds your JDK for all of them)
    run.bat                      prints the reports as text for the sample-data folder
    test.bat                     builds and runs all the hand-checked tests (no window is opened)

**Mac / Linux** (or any shell):

    mkdir out
    javac -encoding UTF-8 -d out src/golfanalyst/*.java src/golfanalyst/gui/*.java
    java -cp out golfanalyst.gui.App
    java -cp out golfanalyst.Main --selftest                       (engine tests)
    java -Djava.awt.headless=true -cp out golfanalyst.gui.App --selftest     (screen tests)

If `build.bat` says javac was not found, set `JAVA_HOME` to your JDK folder (NetBeans shows
it under Tools > Java Platforms) or install a JDK. The scripts call each other by their full path,
so they work even on a computer set not to look in the current folder for programs. `test.bat` ends
with exit code 1 if either set of tests fails. Tell me the exact message if any step fails.

## Which data folder it opens

1. the folder you name when you start it;
2. otherwise a folder called `data` next to the program;
3. otherwise, if there is no `data` folder, `sample-data` (the window title and the footer say so);
4. otherwise a new empty `data` folder with just the file headers.

`sample-data` is randomly generated and is not real. It is there to show the format. The program
adds courses to `courses.tsv` and rounds to `rounds.tsv` itself when you press Save. While it is
showing the sample data, Save first asks "This is the sample data. Create your own data folder
first?". Yes makes an empty `data` folder next to `sample-data` (header rows only, nothing copied,
an existing folder is never written over) and saves nothing; close the program and open it again to
use it. No saves into the sample data; Cancel saves nothing.

## The window

Pages so far: **Home** (handicap, statistics and the buttons), **Round** (enter a new round,
or look back through saved ones with the four arrow buttons, limited to one course or set of tees with the
Show box) and **New Course** (a course and up to four sets of tees). The Home buttons are in two columns:
data entry on the left (New Course, New Round, Edit Round, Import, Clear) and viewing on the right (Rounds,
Stats Board, Analyzer, Best Round, Print). Analyzer and Print are in place and say "not built yet" in
the footer; the rest work.

- **Editing a round:** Edit Round on the Home screen opens the saved rounds at the latest one. Step to the
  round with the arrows (or narrow them with the Show box) and press Edit. The date, PCC, scores, putts,
  Fwy and Sand open for changing; the course/tee and holes stay locked. The header says "Edit 00031:...".
  Save replaces only that round's line in `rounds.tsv` (same RoundID; comments, other rounds and any
  columns of your own are left exactly as they were) and the handicap and statistics are worked out again.
  Before each save a dated copy of the file as it was goes in the `backups` folder inside the data folder
  (`rounds-YYYY-MM-DD-HHMMSS.tsv`, the 20 newest kept), so a bad edit can be undone by copying it back. Cancel, the arrows and the Home button ask "Discard your changes?" if anything was changed.
- **Deleting a round:** in Edit mode a small Delete round button sits under Historical. It asks twice (the
  round named, Keep it as the default; then type DELETE), makes the dated copy, removes only that round's
  line (nothing renumbered) and shows the round before it.
- **Clear:** Home's Clear button clears your rounds only, or everything (courses, name and settings too),
  after a choice, a summary (Keep my data as the default) and typing CLEAR. First courses.tsv, rounds.tsv
  and settings.txt are copied into `backups/clear-YYYY-MM-DD-HHMMSS/` (never pruned); each file keeps only
  its header row. The sample data is never cleared.
- **Import:** Home's Import button adds rounds and/or courses from files in the program's own layout
  (another copy's data folder, or a spreadsheet saved as Text (Tab delimited)). Check files shows what
  would be added, skipped (already here) and rejected (with line numbers) before anything is written.
  Tees are matched by course and tee name (and zip); new ones get new TeeIDs, rounds are remapped and
  get new RoundIDs, duplicates are skipped. Import copies both files into `backups/import-.../` first and
  only adds lines at the end. It never changes a tee or round you already have.
- **Correcting a course:** in Edit Course the name and zip are locked; Correct... opens them, and Save
  writes the corrected name and zip (with the city and state) into every tee of the course. TeeIDs and
  rounds do not change. A name and zip that belong to another course are refused.
- **ZIP code check:** New Course fills in an empty city and state from the zip, warns (and asks at Save)
  when the state does not match, and hints when only the city differs. The list is `zipcodes.tsv` in the
  program folder (Zip, City, State); without it the check does nothing.

  ZIP code data from GeoNames ([www.geonames.org](https://www.geonames.org)), used under a Creative
  Commons Attribution license. The data is provided as is.

- **Entering a course:** the page looks like the round page: a centred row of wood frames over the
  photo, captions down the left frame and one frame per tee, with spreadsheet-style cells (the cell with
  the cursor has a gold outline). Type the course name and its 5-digit zip first. Then each tee: name,
  rating and slope, the optional front-nine and
  back-nine rating and slope, optional yardage, then the par and handicap number (the Hdcp/SI column) of every hole. The total
  par is shown as you type. Cells take digits only and move on by themselves: a par (3-6) at once; a
  handicap number from 2 to 9 at once, a 1 waits (then 0-8 for 10 to 18); a slope after two digits from 5-9
  or three from 1 (55 to 155); a rating after the digit after the point. The arrow beside the first
  tee's name copies its pars and handicap numbers into the empty Par and Hdcp/SI cells of the other tees; a
  rating, slope or yardage is never copied. A tee with no rating is skipped. Names are tidied: extra
  spaces go, and the capitals change only when the whole name was typed in lower case or in capitals.
  Everything is checked as you type: the footer names the next thing to fill in or fix, by tee and hole;
  a real mistake (a handicap number used twice, a tee name already used) turns pink at once; Save comes on
  only when all is well. Save adds the tees with new TeeIDs to `courses.tsv` in the file's own column
  order and goes back Home.
- **Fixing a course:** type the name and zip of a course that is already in the program (any capitals) on
  the New Course screen. The title becomes Edit Course and its tees (the first four) load into the frames;
  empty frames take new tees. Change what you need and press Save: only the tees you changed are rewritten
  (same TeeID, columns of your own kept, everything else in the file untouched), new tees are added, and the
  file as it was is copied to the `backups` folder first (`courses-YYYY-MM-DD-HHMMSS.tsv`). A tee already in the program must keep its rating;
  changing the city or state changes it for every tee of the course; the nine-hole setting is locked. If a
  changed tee already has rounds, the footer says your handicap will be worked out again for them.

- **Entering a round from the keyboard:** the cursor starts on hole 1's score and goes down all the scores
  (hole 9 to hole 10 included), then down all the putts. Only digits are accepted. A putts box takes one
  digit; a score from 2 to 9 moves on at once, and a 1 waits for a second digit (a hole in one, or 10 or
  more) or Enter. In a Putt box, F ticks or unticks Fwy and S does the same for Sand, like the letters on a
  scorecard. They are optional; the boxes can be clicked.
- **The round page** looks like the course page: the Out and In tables are spreadsheet-style grids in
  wood frames, with a wood results column beside them (rating, eagles to others, differential, index
  after) and a wood details frame under them (date, totals, fairways, sand saves, greens, two-putts, and
  the buttons). The whole group is centred on the photo with the arrows under it.
- **Stats Board:** your statistics for every course and set of tees, one row each, with nine-hole rounds
  in their own rows (front nine, back nine, or a nine-hole course). Click a heading to sort, click a
  row to highlight it and double-click it to see its rounds. Gold marks the best figure in each
  column among the courses with two or more rounds.
- **Best Round:** for any course and set of tees (the same kinds as the Stats Board), either your
  lowest single round or a composite of your best score on every hole, shown like a saved round.
  In the composite each hole's putts, fairway and sand come from the round that made that score.
- **See the whole picture:** on any screen, press and hold the mouse on the yellow "Golf Analyst"
  version text at the bottom left. Everything but the photo disappears until you let go.
- **Hover:** the text on every wood button turns red when the mouse is over it, as in the earlier
  versions, and the round Home / Help / Info / arrow buttons light up with a red ring.
- **Info and Help** (the round buttons at the top right) open that screen's text in its own window.
  The text lives in plain files in the `help` folder: `home-info.txt` and `home-help.txt`, then
  `round-new-info.txt` and `round-new-help.txt` while you are entering a round,
  `round-view-info.txt` and `round-view-help.txt` while you are looking at saved ones,
  `round-edit-info.txt` and `round-edit-help.txt` while you are editing one, and
  `course-info.txt` and `course-help.txt` on the New Course screen.
  The first line of a file is the window title; blank lines separate paragraphs. Edit them with any
  text editor; nothing needs rebuilding. The files stay plain text, and a few simple marks give the
  window its styles: put "# " at the start of a line to make it a section heading (do this for every new
  section you add); a line that starts with a label and a colon, like "Low Index: ...", shows the label
  in bold; and a numbered step like "3. Check the date. ..." shows its first sentence in bold.
- **Pictures** are in the `images` folder: the wood buttons and textures, the round buttons and the
  check boxes. Each page's background photo is in `images/backgrounds`, named for the page
  (`home.gif`, `round.jpg`, and so on). Drop in another picture with the same name to change a page's
  photo. If a picture is missing the program still runs, using plain colours.
- **The photos of the Myrtle Beach courses** carry their copyright and credit lines in the picture, which
  are left intact. They are fine for your own copy; if you give the program to others, replace them or get
  permission.
- **Status messages and problems:** the footer says what is wrong with something you typed. If the data
  files have problems (a bad row, a missing rating), it says how many, and clicking it lists them with
  file names and line numbers.

Code for the window is in `src/golfanalyst/gui`: `AppFrame` (the window itself, a thin shell) around
`AppPanel` (everything the window does: the pages, navigation, Help and Info), `HeaderBar`, `FooterBar`,
`Page` and its three pages `HomePage`, `RoundPage` and `CoursePage`, the custom controls (`WoodButton`, `IconButton`,
`ValueBox`, `WoodPanel`, `WoodFrame`, `WoodTag`, `ImageCheckBox`, `BackdropPanel`), `Theme` (colours,
fonts, pictures, in one place), `HelpWindow` (with `HelpFormatter`, which styles the help text) and
`App`. The pages talk to the window only through the `Shell` interface, so they can be tested without
opening a window.

## Your data: two tab-separated text files

Edit them in a text editor or a spreadsheet. Lines starting with `#` and blank lines
are ignored. The first other line is the header row; columns are found by name, in any
order. Per-hole values are comma-separated numbers in one cell (18 of them for a full
round, 9 for a nine-hole round).

**courses.tsv**: one row per course/tee. `TeeID` must be unique.

    TeeID  Course  Tee  Rating  Slope  Pars  StrokeIndex      (required)
    City  State  Zip  Yardage                                 (optional)
    Holes                                                      (optional: 18 or 9, default 18)
    FrontRating  FrontSlope  BackRating  BackSlope            (optional, see below)

- `StrokeIndex` is the scorecard's hole handicap ranking (1 = hardest hole), each number used
  once: 1-18 for an 18-hole tee, 1-9 for a nine-hole course. The handicap needs it to find
  which holes get handicap strokes.
  On the New Course screen the column is headed **Hdcp/SI** and the messages call it the
  "handicap number": golfers know it as Handicap (US scorecards) or Stroke Index (UK and
  European scorecards). The file column and the names in the code stay `StrokeIndex`.
- A **nine-hole course** has `Holes` = 9, nine pars, nine stroke index values, and its
  `Rating` and `Slope` are its nine-hole ratings.
- An **18-hole tee** can have `FrontRating`/`FrontSlope` (holes 1-9) and
  `BackRating`/`BackSlope` (holes 10-18). Without them, a nine-hole round played on that nine
  still counts in your statistics but cannot be used for the handicap (the loader warns you).
  Each rating needs its slope and vice versa.

**rounds.tsv**: one row per round. `RoundID` must be unique.

    RoundID  TeeID  Date  Scores  Putts  Fairways  Sand          (required)
    Holes  PCC  Historical                                         (optional)

`Holes` is blank for a whole tee (18 holes, or the nine holes of a nine-hole course), `F` for
the front nine or `B` for the back nine of an 18-hole tee. Scores, Putts and the four flag
columns (Fairways, Sand) need one value per hole played. Dates can be `2026-10-22` or
`10/22/2026`. Flags are 1 for yes and 0 for no. PCC (normally 0) is the day's Playing Conditions Calculation.
`Historical` is `Y` for an old round kept only to look back at: it can be stepped through in
Rounds (marked historical) but is never part of the handicap. The Home screen's "Include historical
rounds" box (saved as `IncludeHistorical=true|false` in `settings.txt`, ticked when missing)
decides whether the statistics, the Stats Board and Best Round count it. Blank or `N` is
an ordinary round. The Historical box on the round screen sets it, so the file need not be edited
by hand; saving a ticked round to a file without the column adds it to the header. Totals are
never stored; they are always calculated, so the file cannot disagree with itself.

**Putts** count only strokes taken while the ball is on the putting green, with any club. A
stroke from the fringe is not a putt. A hole holed from off the green (a chip-in, a holed
bunker shot) is **0 putts**.

**Greens in regulation and sand saves are not entered.** They are calculated from the score
and putts, so they cannot be inflated. A file that still has old `Saves` or `GIR` columns loads
with those columns ignored and a warning.

A row that cannot be understood is skipped and reported with its line number. A row that
looks odd (for example a fairway marked on a par 3) is kept and reported as a warning.

## Rules used

- **Handicap Index:** current USGA / World Handicap System. Differential =
  (113 / Slope) x (Adjusted Gross - Course Rating - PCC). Average of the lowest 8 of the
  last 20; a table with small adjustments for fewer scores; maximum 54.0. An initial index
  needs 54 holes played, in any mix of 9-hole and 18-hole rounds.
- **Adjusted gross:** each hole is limited to net double bogey (par + 2 + handicap strokes
  on that hole), using the index in effect when the round was played. With no index yet
  the limit is par + 5 (8, 9 and 10 on par 3, 4 and 5).
- **Nine-hole rounds:** a 9-hole differential is calculated from the nine-hole rating and
  slope (PCC counts half), then 0.52 x index + 1.2 is added as the expected score for the
  other nine holes, using the index in effect on the day. The nine-hole Course Handicap for the
  net double bogey limit is (index / 2) x (9-hole slope / 113) + (9-hole rating - 9-hole par).
- **Low Index, soft cap, hard cap:** applied once 20 scores are on record.
- **Exceptional Score Reduction:** 1.0 or 2.0 off the last 20 differentials after a round
  that beats the index by 7.0 or 10.0.
- **Statistics:** fairway % counts par 4 and par 5 holes only; "2 putts or fewer" % is holes
  holed in two putts or fewer (the rest were 3+ putts), so a chip-in counts as one.
- **Greens in regulation (calculated):** a hole is a green in regulation when the strokes
  before the first putt (score minus putts) are no more than par minus 2. GIR % is over every
  hole played. A chip-in for birdie on a par 4 (3 strokes, 0 putts) is not a green in regulation;
  a hole in one is. Holing out from off the green in par minus 2 strokes or fewer is treated
  the same way as a hole in one.
- **Sand saves (calculated):** a hole with a bunker shot is a save when it was finished in par
  or better. Sand saves are divided by sand shots. The Sand flag is still entered, and it is the
  one thing here the program cannot check, so mark every hole where you played from a bunker. Average score, best score and average putts are
  shown separately for 18-hole and 9-hole rounds, because they cannot be compared. The
  per-hole percentages pool every hole played.

## Please double-check

- **The nine-hole expected score** (0.52 x index + 1.2) reproduces the USGA's published
  example (index 14.0 gives 8.5, which turned a 7.2 into 15.7), but I could only check that
  one point. Compare a few nine-hole rounds against an official handicap record.
- **Strokes on a nine:** for a nine-hole round the nine holes' stroke index values are
  ranked 1-9 in order and handicap strokes go to the lowest ranks. That is my reading of
  the USGA's stroke index guidance for nine-hole rounds.
- **Very first index from nine-hole rounds:** until 54 holes are posted a nine-hole round
  cannot be given an expected score, so it is shown as pending. At the 54th hole a provisional
  index is worked out by adding pending nine-hole differentials together in pairs, and that
  index supplies the expected score for each pending round. The USGA describes the outcome
  but not these steps, so an all-nine-hole record just starting out may differ slightly from
  an official one. Once an index exists this no longer matters.
- Rounds of 10 to 17 holes are not supported, and the program cannot know the day's
  Playing Conditions Calculation, so enter it in the PCC column if you know it.
- The engine has not been compared with a real handicap service. Run your own recent
  rounds through it and compare with your official index before trusting it.
- Built and tested on Java 21; it only uses language features from Java 17 and earlier.
- **The New Course page size** was worked out by arithmetic: each frame is 24 rows of 21 px plus
  padding and wood, 526 px, and the whole page needs 616 of the 626 px the default window gives it.
  Across, the five frames and their gaps take 728 of 916 px. On a smaller window a scroll bar appears
  beside the frames; Save and Cancel stay in view below them.

## Editing in NetBeans or VS Code

This folder has a `pom.xml` (a Maven project file), so the editors can open it as a project
without importing anything. The source stays in the `src` folder, so `build.bat` and the
command-line steps above keep working as before.

- **NetBeans:** File > Open Project, choose this folder (it shows a Maven icon). To run it,
  right-click the project > Properties > Run: the main class is `golfanalyst.gui.App`. Put a data
  folder (for example `sample-data`) in the arguments if you want one other than the default.
- **VS Code:** install the Extension Pack for Java, then File > Open Folder on this folder.
- **Visual Studio** (the full Windows IDE) cannot build Java projects.

I could not run Maven here, so the project file is untested. If your editor reports a problem
with it, tell me the exact message.

## Code layout (src/golfanalyst)

| File | What it does |
|---|---|
| Tee, Round | one course/tee, one round (18 or 9 holes) |
| Problem, Dataset | load problems; everything loaded |
| DataLoader | reads and checks the two files |
| Handicap | the handicap rules |
| Stats | fairways, GIR, putts, sand saves, scoring |
| RoundWriter, DataStore | adds a round to rounds.tsv; the one place the screens get their data |
| Main | prints the reports as text; `--selftest` runs the engine tests |
| SelfTest | the engine checks with hand-worked answers |
| gui/* | the window, its pages and controls (see above); `gui/GuiSelfTest` checks the screens |

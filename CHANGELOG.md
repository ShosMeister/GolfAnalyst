# Golf Analyst: development milestones

Nothing here is shown to the user. Until the first release, every feature added to bring back the original design is part of v4.00 and the
version number stays at 4.00 (see the versioning policy in CLAUDE.md). These are the steps the program went through on the way, kept so
that nothing about how it was built is lost. The wording is the help text's own, as it stood before the history was collapsed to one v4.00 entry.

- Golf Analyst v4.00: Rewritten in Java so that it runs on Windows, Mac and Linux without needing Excel. Your courses and rounds are kept in two plain text files that you can edit yourself. The Handicap Index now follows the current USGA rules (the World Handicap System). Nine-hole rounds are supported, which is handy for leagues like Spark Golf. Greens In Regulation and Sand Saves are now calculated instead of entered. The Last 5 and Partial Rounds are gone, since a nine-hole round is now a round of its own.

- Golf Analyst v4.10: Added the New Course screen, so courses and up to four sets of tees at a time are typed in instead of edited into courses.tsv by hand. The window now tells you when you are looking at the sample data, and offers to make your own data folder before anything is saved there.

- Golf Analyst v4.11: The New Course screen now looks like the round screen, with each set of tees in its own wooden frame. An arrow on the first tee copies its pars and handicap numbers into the empty cells of the other tees. Everything is checked as you type, and Save is enabled only when the course is ready to save.

- Golf Analyst v4.20: Added Edit Round, so a saved round can be fixed right on the screen instead of in the rounds file. Saved courses can be fixed the same way from New Course. Only what you changed is rewritten, and a copy of the file as it was is kept in rounds.tsv.bak or courses.tsv.bak. The Help and Info windows now show headings and bold labels.

- Golf Analyst v4.21: The round screen now looks like the New Course screen, with the holes in neat grids inside wooden frames, a wooden column beside them for the rating and the round's figures, and the totals and buttons in a wooden frame underneath, all centered on the photo. Press and hold the yellow Golf Analyst text at the bottom left of any screen to see the whole photo.

- Golf Analyst v4.22: The Help and Info windows now have to be closed before you can go back to the screen behind them, so the help you are reading always belongs to the screen you are on.

- Golf Analyst v4.30: Old rounds can be kept as historical rounds. You can still look back at them in Rounds, but they are left out of your Handicap Index and your statistics, so those show how you play today.

- Golf Analyst v4.40: Added the Stats Board, which shows your statistics course by course and tee by tee, with your nine-hole rounds kept apart. Click a heading to sort it, and the best figure in each column is marked in gold. Double-click a course to see its rounds.

- Golf Analyst v4.41: Close on the Rounds screen goes back to the Stats Board when you came from there, instead of to the Home screen. Closing the window, pressing Home or pressing Cancel while you are part way through typing a new round or a course now asks "Discard your changes?" first.

- Golf Analyst v4.50: Added the Best Round screen. For any course and set of tees it shows your lowest single round, or a composite of your best score on every hole, so you can see how close your best day is to your potential.

- Golf Analyst v4.60: Added a Historical box on the round screens, so historical rounds can be marked without editing a file, and an Include historical rounds box on the Home screen, so your statistics, the Stats Board and Best Round can count your historical rounds or leave them out.

- Golf Analyst v4.70: Added Delete Round and Clear, each with two confirmations. Editing or deleting existing data keeps a dated backup copy; Clear keeps a separate dated folder of the files before they are emptied.

- Golf Analyst v4.71: Fixed all the dialog boxes that ask for your input, so their text is always fully readable.

- Golf Analyst v4.80: Added Import, so rounds and courses from another copy of the program or from a spreadsheet can be added after a check. A course's name and ZIP can now be corrected. The New Course screen fills in the city and state from the ZIP code and warns if they do not match.

- v4.00 (before release): Every dialog box now has the same wooden look: a wood title band, parchment, wood buttons.

- v4.00 (before release): Added Delete course on the New Course screen. It removes a course, all its sets of tees and every round played on them after two confirmations, keeping copies of both files in a backups folder first.

- v4.00 (before release): Fixed the New Course bar cutting off the Nine-hole course box in Edit Course: Delete course moved to the bottom, beside Save, and the Course and City boxes made a little narrower.

- v4.00 (before release): On the New Course screen the hole handicap column is headed Hdcp, in the same size as Par, and Delete course is a full-size button like Save and Cancel, set well apart from them.

- v4.00 (before release): Backups are now kept as dated folders named for what was done (for example 2026-11-01_143012_edit-round), with the files under their usual names, so putting one back is just copying them over yours.

- v4.00 (before release): Added Settings on the Home screen: your name, how many backups to keep, a second folder (a USB stick or cloud folder) that gets a copy of every backup, and which data folder to use.

- v4.00 (before release): Added Export: a copy of everything, one course, or your rounds from a date, in a new folder that another copy of the program can Import.

- v4.00 (before release): The Analyzer's figures are now worked out hole by hole, ready for its screen, which is still to come.

- v4.00 (before release): Added the Analyzer screen: every hole of a course as counts, percentages or averages, with your three hardest holes in red and your three best in gold.

- v4.00 (before release): The Analyzer now calls your best holes your easiest holes, to match the hardest holes, and lists both in hole order.

- v4.00 (before release): Added Print: the Handicap Report, Round Log, a round's scorecard, Best Round, Stats Board, My scorecard for a course and Course notes, on paper, as a PDF, or (for the tables) as a spreadsheet file.

- v4.00 (before release): The Print window has its own i and ? buttons for its Info and Help.

- v4.00 (before release): Printing polished: bigger daggers and check marks, the Round Log's note on every page that needs it, a cleaner My Scorecard, a full-width Stats Board with the same course and tee names as the other reports, bold daggers in the notes, tinted Avg vs par in Course notes, and a Print window that sizes itself to its words.

- v4.00 (before release): The Rounds screen's Show box lists the 18-hole rounds, front nines and back nines of a set of tees separately (the same rows as Best Round and the Analyzer), and a double-click on a Stats Board row opens exactly the rounds in that row.

- v4.00 (before release): A Windows installer (tools\make-installer.bat: its own trimmed Java, per-user, Start menu entry, photos=all test copy or photos=none public build) and a "runs anywhere" zip; installed, the program never writes in its own folder and keeps your data in Documents\Golf Analyst.

- v4.00 (before release): The course photographs are used with the permission of On The Green Magazine and credited in About; a check that every page finds its own background picture.

- v4.00 (before release): The installer and the runs-anywhere zip are one build for everyone, with every picture exactly as it is, the photograph credit on the license page and in the tester README.

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

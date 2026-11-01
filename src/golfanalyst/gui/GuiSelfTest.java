package golfanalyst.gui;

import golfanalyst.DataLoader;
import golfanalyst.DataStore;
import golfanalyst.Dataset;
import golfanalyst.Handicap;
import golfanalyst.Round;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;

/*
 +--------------------------------------------------------------------------+
 | GuiSelfTest                                                              |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | Checks the Home and Round screens without opening a window. It copies    |
 | the sample data to a temporary folder, builds the screens, types into    |
 | them by code, and checks what they calculate, what they refuse and what  |
 | they save. The real files are never touched.                             |
 |                                                                          |
 | What is covered:                                                         |
 | - Home shows the handicap and round counts from the engine               |
 | - Round entry: totals, calculated greens in regulation, a chip-in,       |
 |   a bad entry that turns Save off, a complete round that turns it on     |
 | - Save adds a line to rounds.tsv that loads back identically             |
 | - Front-nine entry, and browsing saved rounds with the arrows            |
 | - New Course: every typing rule, the names rule, finding a course        |
 |   already there, each check on Save, the cursor order, adding tees to    |
 |   a course and saving a new nine-hole course                             |
 | - Help text styling: headings, labels, numbered steps, the title line,   |
 |   the styles that reach the window, and every real help file             |
 | - The sample-data safeguard: title, footer, and Yes / No / Cancel for    |
 |   both a course and a round                                              |
 | - The round screen's look: frame and cell sizes, the group centred on    |
 |   the photo, and the white / cream / grey faces in each mode             |
 | - The peek: holding the mouse on the version text hides the controls     |
 |   and the keyboard until it is let go                                    |
 | - Historical rounds: shown and marked in the viewer, kept historical     |
 |   when edited, left out of the Home counts                               |
 | - The Stats Board: rows, band, totals, scroll bar, sorting, the gold,    |
 |   highlighting a row, the arrow keys and opening a row's rounds          |
 | - Best Round: Single and Composite, the course and tees pulldown, the    |
 |   titles and footer, and only a Close button                             |
 |                                                                          |
 | How to run:                                                              |
 |   java -cp out golfanalyst.gui.App --selftest                            |
 |                                                                          |
 | Public Methods:                                                          |
 | run() - runs every check, returns true if all passed                     |
 +--------------------------------------------------------------------------+
 */
public final class GuiSelfTest
{

    private static int passedCount = 0;
    private static int failedCount = 0;

    private GuiSelfTest()
    {
    }

    /*
     +--------------------------------------------------------------------------+
     | TestShell                                                                |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | A stand-in for the window: it keeps the data store and remembers the     |
     | last status message and which page was asked for, so the tests can       |
     | look at them.                                                            |
     +--------------------------------------------------------------------------+
     */
    private static final class TestShell implements Shell
    {
        private final DataStore store;
        private String status = "";
        private String lastPage = "";
        private int windowCalls = 0;
        private final List<String> asked = new ArrayList<>();
        private boolean answerYes = true;

        TestShell(DataStore store)
        {
            this.store = store;
        }

        /*
         * store(): the data store.
         */
        @Override
        public DataStore store()
        {
            return store;
        }

        /*
         * setStatus(text): remembers the message.
         */
        @Override
        public void setStatus(String text)
        {
            windowCalls = windowCalls + 1;
            status = text;
        }

        /*
         * refreshTitle(): nothing to do without a header.
         */
        @Override
        public void refreshTitle()
        {
            windowCalls = windowCalls + 1;
        }

        /*
         * goHome(): remembers that Home was asked for.
         */
        @Override
        public void goHome()
        {
            windowCalls = windowCalls + 1;
            lastPage = "home";
        }

        /*
         * newRound(): remembers that a new round was asked for.
         */
        @Override
        public void newRound()
        {
            windowCalls = windowCalls + 1;
            lastPage = "new";
        }

        /*
         * browseRounds(): remembers that browsing was asked for.
         */
        @Override
        public void browseRounds()
        {
            windowCalls = windowCalls + 1;
            lastPage = "browse";
        }

        /*
         * browseRoundsOn(teeId): remembers which tees the rounds were asked for.
         */
        @Override
        public void browseRoundsOn(int teeId)
        {
            windowCalls = windowCalls + 1;
            lastPage = "browse " + teeId;
        }

        /*
         * statsBoard(): remembers that the Stats Board was asked for.
         */
        @Override
        public void statsBoard()
        {
            windowCalls = windowCalls + 1;
            lastPage = "stats";
        }

        /*
         * bestRound(): remembers that Best Round was asked for.
         */
        @Override
        public void bestRound()
        {
            windowCalls = windowCalls + 1;
            lastPage = "best";
        }

        /*
         * newCourse(): remembers that New Course was asked for.
         */
        @Override
        public void newCourse()
        {
            windowCalls = windowCalls + 1;
            lastPage = "course";
        }

        /*
         * readyToSave(): always yes; the sample-data question is tested on the real AppPanel.
         */
        @Override
        public boolean readyToSave()
        {
            windowCalls = windowCalls + 1;
            return true;
        }

        /*
         * editRound(): remembers that Edit Round was asked for.
         */
        @Override
        public void editRound()
        {
            windowCalls = windowCalls + 1;
            lastPage = "edit";
        }

        /*
         * askYesNo(question): remembers the question and gives the answer the test has set.
         */
        @Override
        public boolean askYesNo(String question)
        {
            windowCalls = windowCalls + 1;
            asked.add(question);
            return answerYes;
        }
    }

    /*
     * check(name, ok): records one test result, printing FAIL and the name if it is wrong.
     * Passed in:  name - what is being checked, ok - true if it passed
     * Passed out: nothing
     */
    private static void check(String name, boolean ok)
    {
        if (ok)
        {
            passedCount = passedCount + 1;
        }
        else
        {
            failedCount = failedCount + 1;
            System.out.println("FAIL  " + name);
        }
    }

    /*
     * copyFolder(from, to): copies the sample data into a temporary folder so the real files stay untouched.
     * Passed in:  from - the folder to copy, to - where to copy it
     * Passed out: nothing (throws IOException if a copy fails)
     */
    private static void copyFolder(Path from, Path to) throws IOException
    {
        Files.createDirectories(to);
        try (Stream<Path> files = Files.list(from))
        {
            for (Path file : (Iterable<Path>) files::iterator)
            {
                if (Files.isRegularFile(file))
                {
                    Files.copy(file, to.resolve(file.getFileName()));
                }
            }
        }
    }

    /*
     * findByText(root, text): finds the button showing the given words, anywhere in a screen.
     * Passed in:  root - where to look, text - the words on the button
     * Passed out: the button (the test fails with an error if there is none)
     */
    private static JComponent findByText(Container root, String text)
    {
        JComponent found = search(root, text, false);
        if (found == null)
        {
            throw new IllegalStateException("no button with the text: " + text);
        }
        return found;
    }

    /*
     * findByTip(root, tip): finds the button with the given hover tip, anywhere in a screen.
     * Passed in:  root - where to look, tip - its tool tip text
     * Passed out: the button (the test fails with an error if there is none)
     */
    private static JComponent findByTip(Container root, String tip)
    {
        JComponent found = search(root, tip, true);
        if (found == null)
        {
            throw new IllegalStateException("no button with the tip: " + tip);
        }
        return found;
    }

    /*
     * search(root, text, byTip): walks the screen's parts looking for a button by its words or its tip.
     * Passed in:  root - where to look, text - what to match, byTip - true to match the tip
     * Passed out: the button, or null
     */
    private static JComponent search(Container root, String text, boolean byTip)
    {
        for (Component child : root.getComponents())
        {
            if (child instanceof AbstractButton)
            {
                AbstractButton button = (AbstractButton) child;
                String label = byTip ? button.getToolTipText() : button.getText();
                if (text.equals(label) && button.isVisible())
                {
                    return button;
                }
            }
            if (child instanceof Container)
            {
                JComponent found = search((Container) child, text, byTip);
                if (found != null)
                {
                    return found;
                }
            }
        }
        return null;
    }

    /*
     * type(field, text): types into a score or putts box, as the user would.
     * Passed in:  field - the box, text - what to type
     * Passed out: nothing
     */
    private static void type(JTextField field, String text)
    {
        field.setText(text);
    }

    /*
     +--------------------------------------------------------------------------+
     | run()                                                                    |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Runs every check and prints the totals.                                  |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: true if every check passed                                   |
     +--------------------------------------------------------------------------+
     */
    public static boolean run()
    {
        System.setProperty("java.awt.headless", "true");
        try
        {
            runChecks();
        }
        catch (IOException | RuntimeException failure)
        {
            failedCount = failedCount + 1;
            System.out.println("FAIL  the test stopped with an error: " + failure);
            failure.printStackTrace(System.out);
        }
        System.out.println(passedCount + " passed, " + failedCount + " failed (screens)");
        return failedCount == 0;
    }

    /*
     +--------------------------------------------------------------------------+
     | runChecks()                                                              |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The checks themselves. The sample tee 1 (Sample Links, White) has pars   |
     | 4,3,5,4,4,3,5,4,4,4,3,5,4,4,3,5,4,4, so a round of par + 1 on every      |
     | hole with two putts scores 90 and hits no green in regulation.           |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing (throws if the data cannot be copied or read)        |
     +--------------------------------------------------------------------------+
     */
    private static void runChecks() throws IOException
    {
        Path images = Path.of(System.getProperty("golfanalyst.images", "images"));
        Theme.load(images);
        Path temp = Files.createTempDirectory("golfanalyst-gui-test");
        copyFolder(Path.of("sample-data"), temp);
        DataStore store = new DataStore(temp);
        TestShell shell = new TestShell(store);
        int roundsBefore = store.roundsByDate().size();
        check("sample data loads for the screen tests", roundsBefore > 0);

        // Building a page must not call the window: in the real program the window is still being built,
        // its pages are not registered yet, and a call then fails (this was a real start-up crash)
        HomePage home = new HomePage(shell);
        RoundPage page = new RoundPage(shell);
        CoursePage coursePage = new CoursePage(shell);
        check("building the pages makes no calls to the window", shell.windowCalls == 0);

        // Home
        home.onShow();
        check("Home shows the handicap index from the engine",
                home.handicapIndex.getText().equals(Handicap.format(store.handicap().currentIndex())));
        check("Home shows the number of rounds", home.totalRounds.getText().equals(Integer.toString(roundsBefore)));

        // Round entry: a bogey on every hole, two putts everywhere
        page.showNew();
        check("a new round starts with Save off", !page.saveButton.isEnabled());
        String startTee = String.valueOf(page.teeCombo.getSelectedItem());
        check("a new round opens on the tee of the latest round (Edgewater Senior, round 37)",
                startTee.startsWith("00005: ") && startTee.contains("Edgewater") && startTee.contains("Senior"));
        int[] pars = {4, 5, 4, 3, 4, 5, 4, 3, 4, 4, 3, 4, 5, 4, 3, 4, 5, 4};
        int parThree = 3; // hole 4, Edgewater's first par 3
        for (int hole = 0; hole < 18; hole = hole + 1)
        {
            check("hole " + (hole + 1) + " shows its par from the course", page.parBoxes[hole].getText().equals(Integer.toString(pars[hole])));
            type(page.puttFields[hole], "2");
            type(page.scoreFields[hole], Integer.toString(pars[hole] + 1));
        }
        check("score total is 90", page.scoreBox.getText().equals("90"));
        check("putts total is 36", page.puttsBox.getText().equals("36"));
        check("no greens in regulation on bogeys with two putts", page.girBox.getText().equals("0 / 18"));
        check("a complete round turns Save on", page.saveButton.isEnabled());
        check("the footer says it is ready", shell.status.equals("Ready to save."));
        check("the projected index is shown", !page.indexBox.getText().equals("-"));

        // fairways: switched off on par 3s, counted on par 4s and 5s
        check("the fairway box is off on a par 3", !page.fairwayChecks[parThree].isEnabled());
        check("the fairway box is on for a par 4", page.fairwayChecks[0].isEnabled());
        page.fairwayChecks[0].setSelected(true);
        page.fairwayChecks[2].setSelected(true);

        // a chip-in on hole 1: par 4 holed in 3 with 0 putts
        type(page.scoreFields[0], "3");
        type(page.puttFields[0], "0");
        check("a chip-in (0 putts) is accepted", page.saveButton.isEnabled());
        check("a chip-in birdie is not a green in regulation", page.girBox.getText().equals("0 / 18"));
        check("the chip-in counts as a birdie", page.resultBoxes[1].getText().equals("1"));

        // a bad entry: putts equal to the score
        type(page.puttFields[0], "3");
        check("putts equal to the score turns Save off", !page.saveButton.isEnabled());
        check("the footer explains the problem", shell.status.contains("must be fewer than the score"));
        type(page.puttFields[0], "0");

        // sand: bunker shot with par on hole 4 (par 3) is a save
        page.sandChecks[parThree].setSelected(true);
        type(page.scoreFields[parThree], "3");
        type(page.puttFields[parThree], "1");
        check("a par from the sand is a sand save", page.sandBox.getText().equals("1 / 1"));

        // Save writes to the file and reads back the same
        page.saveButton.doClick();
        check("saving adds a round", store.roundsByDate().size() == roundsBefore + 1);
        Dataset reread = DataLoader.load(temp);
        check("the file loads back with no errors", reread.errorCount() == 0);
        Round saved = reread.rounds().get(reread.rounds().size() - 1);
        check("the saved round has the typed scores",
                saved.scores()[0] == 3 && saved.putts()[0] == 0 && saved.scores()[parThree] == 3 && saved.putts()[parThree] == 1
                        && saved.totalScore() == 87);
        check("the saved round remembers the fairways and the bunker",
                saved.fairway()[0] && saved.fairway()[2] && !saved.fairway()[parThree] && saved.sand()[parThree]);
        check("after saving the screen shows the saved round read-only", !page.saveButton.isVisible());
        check("the saved round's total is shown", page.scoreBox.getText().equals("87"));
        List<String> lines = Files.readAllLines(temp.resolve(DataLoader.ROUNDS_FILE), StandardCharsets.UTF_8);
        check("the new line is the last line of the file", lines.get(lines.size() - 1).startsWith((saved.id()) + "\t"));

        // Browsing
        page.showRoundAt(0);
        check("at the first round, Previous is off and Next is on", !page.previousButton.isEnabled() && page.nextButton.isEnabled());
        page.lastButton.doClick();
        check("Last goes to the newest round", page.scoreBox.getText().equals("87") && !page.nextButton.isEnabled());
        page.previousButton.doClick();
        check("Previous steps back one round", page.nextButton.isEnabled());

        // Boxes that cannot be typed in look like displays (grey); boxes that can look like places to type (white)
        page.showLatest();
        check("looking at a round: PCC has the grey display face", page.pccField.getBackground().equals(Theme.FIELD_FACE));
        check("looking at a round: the date has the grey display face", page.dateField.getBackground().equals(Theme.FIELD_FACE));
        check("looking at a round: a score box has the grey display face", page.scoreFields[0].getBackground().equals(Theme.FIELD_FACE));
        page.showNew();
        check("entering a round: PCC is white", page.pccField.getBackground().equals(java.awt.Color.WHITE));
        check("entering a round: the date is white", page.dateField.getBackground().equals(java.awt.Color.WHITE));
        check("entering a round: a score box is white", page.scoreFields[0].getBackground().equals(java.awt.Color.WHITE));
        page.holesCombo.setSelectedIndex(1);
        check("a hole not being played has the grey face", page.scoreFields[12].getBackground().equals(Theme.FIELD_FACE));

        // Typing rules for the putts and score boxes: digits only, one digit moves on, a lone 1 in a score waits
        int[] advances = new int[1];
        javax.swing.text.PlainDocument puttDoc = new javax.swing.text.PlainDocument();
        puttDoc.setDocumentFilter(new RoundPage.EntryFilter(false, () -> false, () -> advances[0] = advances[0] + 1, null));
        try
        {
            puttDoc.insertString(0, "x", null);
            check("a letter cannot be typed in a putts box", puttDoc.getLength() == 0 && advances[0] == 0);
            puttDoc.insertString(0, "7", null);
            check("one putts digit is kept and moves on", puttDoc.getText(0, puttDoc.getLength()).equals("7") && advances[0] == 1);
            puttDoc.insertString(1, "3", null);
            check("a digit typed into a full putts box replaces it and moves on",
                    puttDoc.getText(0, puttDoc.getLength()).equals("3") && advances[0] == 2);
            puttDoc.remove(0, 1);
            puttDoc.insertString(0, "0", null);
            check("0 is a valid number of putts", puttDoc.getText(0, puttDoc.getLength()).equals("0") && advances[0] == 3);

            advances[0] = 0;
            javax.swing.text.PlainDocument scoreDoc = new javax.swing.text.PlainDocument();
            scoreDoc.setDocumentFilter(new RoundPage.EntryFilter(true, () -> false, () -> advances[0] = advances[0] + 1, null));
            scoreDoc.insertString(0, "q", null);
            scoreDoc.insertString(0, "-", null);
            check("letters and signs cannot be typed in a score box", scoreDoc.getLength() == 0 && advances[0] == 0);
            scoreDoc.insertString(0, "0", null);
            check("a score cannot start with 0", scoreDoc.getLength() == 0 && advances[0] == 0);
            scoreDoc.insertString(0, "5", null);
            check("a score of 5 moves on at once", scoreDoc.getText(0, scoreDoc.getLength()).equals("5") && advances[0] == 1);
            scoreDoc.remove(0, 1);
            scoreDoc.insertString(0, "1", null);
            check("a lone 1 waits for a second digit", scoreDoc.getText(0, scoreDoc.getLength()).equals("1") && advances[0] == 1);
            scoreDoc.insertString(1, "2", null);
            check("a 1 then a 2 makes 12 and moves on", scoreDoc.getText(0, scoreDoc.getLength()).equals("12") && advances[0] == 2);
            scoreDoc.insertString(2, "9", null);
            check("a digit typed into a full score box starts it over", scoreDoc.getText(0, scoreDoc.getLength()).equals("9") && advances[0] == 3);

            javax.swing.text.PlainDocument loadingDoc = new javax.swing.text.PlainDocument();
            loadingDoc.setDocumentFilter(new RoundPage.EntryFilter(false, () -> true, () -> advances[0] = advances[0] + 1, null));
            advances[0] = 0;
            loadingDoc.insertString(0, "12", null);
            check("a saved round's values load as they are, with no moving on", loadingDoc.getText(0, loadingDoc.getLength()).equals("12") && advances[0] == 0);
        }
        catch (javax.swing.text.BadLocationException unexpected)
        {
            check("the typing rules ran without error: " + unexpected.getMessage(), false);
        }

        // The F and S keys in a putts box tick Fwy and Sand, like the notation on a scorecard
        StringBuilder letters = new StringBuilder();
        javax.swing.text.PlainDocument letterDoc = new javax.swing.text.PlainDocument();
        letterDoc.setDocumentFilter(new RoundPage.EntryFilter(false, () -> false, () -> advances[0] = advances[0] + 1, letter -> letters.append(letter)));
        try
        {
            letterDoc.insertString(0, "f", null);
            letterDoc.insertString(0, "S", null);
            letterDoc.insertString(0, "z", null);
            check("F and S are passed on in capitals and never typed in", letters.toString().equals("FSZ") && letterDoc.getLength() == 0);
        }
        catch (javax.swing.text.BadLocationException unexpected)
        {
            check("the letter keys ran without error: " + unexpected.getMessage(), false);
        }
        page.showNew();
        try
        {
            page.puttFields[0].getDocument().insertString(0, "f", null);
            check("F in a putts box ticks the Fwy box of a par 4", page.fairwayChecks[0].isSelected());
            page.puttFields[0].getDocument().insertString(0, "F", null);
            check("F again unticks it", !page.fairwayChecks[0].isSelected());
            page.puttFields[parThree].getDocument().insertString(0, "f", null);
            check("F does nothing on a par 3", !page.fairwayChecks[parThree].isSelected());
            page.puttFields[parThree].getDocument().insertString(0, "s", null);
            check("S in a putts box ticks the Sand box", page.sandChecks[parThree].isSelected());
            check("the letters were not typed into the box", page.puttFields[0].getText().isEmpty() && page.puttFields[parThree].getText().isEmpty());
        }
        catch (javax.swing.text.BadLocationException unexpected)
        {
            check("the markers ran without error: " + unexpected.getMessage(), false);
        }

        // The cursor goes down all the scores (across the Out and In tables), then down all the putts
        page.showNew();
        check("after a score the cursor goes to the next hole's score", page.componentAfter(page.scoreFields[0]) == page.scoreFields[1]);
        check("after hole 9's score it goes to hole 10's score", page.componentAfter(page.scoreFields[8]) == page.scoreFields[9]);
        check("after hole 18's score it goes to hole 1's putts", page.componentAfter(page.scoreFields[17]) == page.puttFields[0]);
        check("after hole 9's putts it goes to hole 10's putts", page.componentAfter(page.puttFields[8]) == page.puttFields[9]);
        check("after the last putts it goes to the date", page.componentAfter(page.puttFields[17]) == page.dateField);
        page.holesCombo.setSelectedIndex(1);
        check("front nine: after hole 9's score it goes to hole 1's putts", page.componentAfter(page.scoreFields[8]) == page.puttFields[0]);
        check("front nine: after hole 9's putts it goes to the date", page.componentAfter(page.puttFields[8]) == page.dateField);
        page.holesCombo.setSelectedIndex(2);
        check("back nine: after hole 18's score it goes to hole 10's putts", page.componentAfter(page.scoreFields[17]) == page.puttFields[9]);
        page.holesCombo.setSelectedIndex(0);

        // Wording: a score belongs to a hole; rounds are what the handicap counts
        for (String file : new String[] {"home-info.txt", "home-help.txt", "round-new-info.txt", "round-new-help.txt",
                "round-view-info.txt", "round-view-help.txt", "round-edit-info.txt", "round-edit-help.txt", "course-info.txt", "course-help.txt"})
        {
            String helpText = Files.readString(Path.of("help", file));
            check(file + " does not call a round a score", !helpText.contains("Scores Used") && !helpText.contains("20 scores")
                    && !helpText.contains("your best 8 scores") && !helpText.contains("scores on record"));
        }
        String entryHelp = Files.readString(Path.of("help", "round-new-help.txt"));
        check("the entering help explains the F and S keys and says they are optional",
                entryHelp.contains("F ticks or unticks Fwy") && entryHelp.contains("S does the same for Sand")
                        && entryHelp.contains("you do not have to use them"));
        check("the F and S keys are explained in the Help, not the Info", !Files.readString(Path.of("help", "round-new-info.txt")).contains("type F"));
        check("the Name help is not 'whose rounds these are'", !Files.readString(Path.of("help", "home-info.txt")).contains("Whose rounds"));

        // The Show drop-down limits the arrows to one course or one set of tees
        page.showLatest();
        check("the Show drop-down is visible when looking at rounds", page.filterCombo.isVisible());
        RoundPage.FilterItem riverbend = null;
        RoundPage.FilterItem linksAllTees = null;
        for (int lp = 0; lp < page.filterCombo.getItemCount(); lp = lp + 1)
        {
            RoundPage.FilterItem item = page.filterCombo.getItemAt(lp);
            if (item.label().startsWith("Riverbend GC"))
            {
                riverbend = item;
            }
            if (item.label().startsWith("Sample Links, all tees"))
            {
                linksAllTees = item;
            }
        }
        check("a tee with rounds is offered", riverbend != null);
        check("a course played on two tees gets an all-tees choice", linksAllTees != null);
        int riverbendRounds = 0;
        int linksRounds = 0;
        for (Round round : store.roundsByDate())
        {
            riverbendRounds = riverbendRounds + (round.teeId() == riverbend.teeId() ? 1 : 0);
            linksRounds = linksRounds + ((round.teeId() == 1 || round.teeId() == 2) ? 1 : 0);
        }
        page.filterCombo.setSelectedItem(riverbend);
        check("choosing a tee shows a round on that tee", page.title().contains("Riverbend GC"));
        page.firstButton.doClick();
        int steps = 1;
        boolean allRiverbend = page.title().contains("Riverbend GC");
        while (page.nextButton.isEnabled())
        {
            page.nextButton.doClick();
            steps = steps + 1;
            allRiverbend = allRiverbend && page.title().contains("Riverbend GC");
        }
        check("the arrows step through only that tee's rounds", steps == riverbendRounds && allRiverbend);
        check("the footer says which round of how many", shell.status.startsWith("Round " + riverbendRounds + " of " + riverbendRounds));
        page.filterCombo.setSelectedItem(linksAllTees);
        page.firstButton.doClick();
        steps = 1;
        while (page.nextButton.isEnabled())
        {
            page.nextButton.doClick();
            steps = steps + 1;
        }
        check("an all-tees choice steps through every tee of the course", steps == linksRounds);
        page.filterCombo.setSelectedIndex(0);
        page.firstButton.doClick();
        steps = 1;
        while (page.nextButton.isEnabled())
        {
            page.nextButton.doClick();
            steps = steps + 1;
        }
        check("all rounds steps through every saved round", steps == store.roundsByDate().size());

        // Help: separate files for entering and for looking, and they all exist
        page.showNew();
        check("while entering, the round help files are the entry ones", page.helpKey().equals("round-new"));
        page.showLatest();
        check("while looking, the round help files are the viewing ones", page.helpKey().equals("round-view"));
        for (String key : new String[] {"home", "round-new", "round-view", "round-edit", "course"})
        {
            for (String kind : new String[] {"info", "help"})
            {
                check("help file exists: " + key + "-" + kind + ".txt", Files.exists(Path.of("help", key + "-" + kind + ".txt")));
            }
        }
        check("the viewing help does not talk about a Save button", !Files.readString(Path.of("help", "round-view-help.txt")).contains("Save adds"));

        // Front nine
        page.showNew();
        page.holesCombo.setSelectedIndex(1);
        check("front nine switches the back nine off", !page.scoreFields[10].isEnabled() && page.scoreFields[0].isEnabled());
        for (int hole = 0; hole < 9; hole = hole + 1)
        {
            type(page.puttFields[hole], "2");
            type(page.scoreFields[hole], Integer.toString(pars[hole] + 1));
        }
        check("nine holes complete a front-nine round", page.saveButton.isEnabled());
        check("front nine score total is 45", page.scoreBox.getText().equals("45"));
        int beforeNine = store.roundsByDate().size();
        page.saveButton.doClick();
        check("a front-nine round saves", store.roundsByDate().size() == beforeNine + 1);
        Round nine = store.roundsByDate().get(store.roundsByDate().size() - 1);
        check("the saved nine knows it was the front nine", nine.isNineHole() && nine.startHole() == 0 && nine.holes() == 9);
        check("the nine is written with F in the Holes column",
                Files.readString(temp.resolve(DataLoader.ROUNDS_FILE)).contains("\tF\t"));

        // Cancel goes home
        page.showNew();
        page.cancelButton.doClick();
        check("Cancel returns to Home", shell.lastPage.equals("home"));

        // A bad date and a bad PCC
        page.showNew();
        for (int hole = 0; hole < 18; hole = hole + 1)
        {
            type(page.puttFields[hole], "2");
            type(page.scoreFields[hole], Integer.toString(pars[hole] + 1));
        }
        page.dateField.setText("31/13/2026");
        check("a bad date turns Save off", !page.saveButton.isEnabled());
        page.dateField.setText("2026-10-24");
        page.pccField.setText("7");
        check("a bad PCC turns Save off", !page.saveButton.isEnabled());
        page.pccField.setText("0");
        check("fixing them turns Save back on", page.saveButton.isEnabled());

        // The real window logic (AppPanel is everything in the window except the JFrame itself):
        // build it exactly as the program does, then click through it
        Path windowTemp = Files.createTempDirectory("golfanalyst-window-test");
        copyFolder(Path.of("sample-data"), windowTemp);
        DataStore windowStore = new DataStore(windowTemp);
        AppPanel panel = new AppPanel(windowStore, Path.of("help"), "");
        check("the window starts on the Home page", panel.header().title().equals("Golf Analyst Stats Home"));
        check("the Home button is off on the Home page", !((IconButton) findByTip(panel, "Home")).isEnabled());
        check("the footer says how many rounds loaded", panel.footer().status().contains("rounds loaded"));
        ((WoodButton) findByText(panel, "New Round")).doClick();
        check("New Round shows the round page", panel.header().title().equals("New Round"));
        check("the Home button is on away from Home", ((IconButton) findByTip(panel, "Home")).isEnabled());
        ((IconButton) findByTip(panel, "Home")).doClick();
        check("the Home button goes back to Home", panel.header().title().equals("Golf Analyst Stats Home"));
        ((WoodButton) findByText(panel, "Rounds")).doClick();
        check("Rounds shows a saved round titled with its id", panel.header().title().matches("\\d{5}:.+:.+"));
        ((IconButton) findByTip(panel, "First round")).doClick();
        check("the First arrow changes the title to the first round", panel.header().title().startsWith("00001:"));
        ((WoodButton) findByText(panel, "Close")).doClick();
        check("Close goes back to Home", panel.header().title().equals("Golf Analyst Stats Home"));
        ((WoodButton) findByText(panel, "Analyzer")).doClick();
        check("an unbuilt button says so", panel.footer().status().contains("not built yet"));
        ((WoodButton) findByText(panel, "Stats Board")).doClick();
        check("the Stats Board button opens the Stats Board", panel.header().title().equals("Stats Board")
                && panel.footer().status().equals(StatsPage.FOOTER_HINT));
        StatsPage windowStats = panel.statsPage();
        windowStats.select(windowStats.rowPanels.get(0).row.key());
        windowStats.openSelected();
        check("Enter on a Stats Board row opens Rounds on that course and tees",
                panel.header().title().matches("\\d{5}:.+:.+") && panel.roundPage().filterCombo.getSelectedItem() != null
                        && ((RoundPage.FilterItem) panel.roundPage().filterCombo.getSelectedItem()).teeId() == windowStats.rowPanels.get(0).row.teeId());
        String pickedKey = windowStats.selectedKey();
        check("Close says it goes back to the Stats Board", RoundPage.BACK_TO_STATS.equals(panel.roundPage().cancelButton.getToolTipText()));
        ((WoodButton) findByText(panel, "Close")).doClick();
        check("Close goes back to the Stats Board it came from, with the same row picked",
                panel.header().title().equals("Stats Board") && pickedKey.equals(windowStats.selectedKey()));
        ((IconButton) findByTip(panel, "Home")).doClick();
        ((WoodButton) findByText(panel, "Rounds")).doClick();
        check("opened from Home, Close says it goes Home", RoundPage.BACK_HOME.equals(panel.roundPage().cancelButton.getToolTipText()));
        ((WoodButton) findByText(panel, "Close")).doClick();
        check("and Close then goes Home again", panel.header().title().equals("Golf Analyst Stats Home"));
        windowStats.openSelected();
        ((WoodButton) findByText(panel, "Edit")).doClick();
        ((WoodButton) findByText(panel, "Cancel")).doClick();
        check("from the Stats Board, Cancel in Edit still goes back to the round", panel.header().title().matches("\\d{5}:.+:.+"));
        ((WoodButton) findByText(panel, "Close")).doClick();
        check("and Close after editing still goes back to the Stats Board", panel.header().title().equals("Stats Board"));
        ((IconButton) findByTip(panel, "Home")).doClick();
        ((WoodButton) findByText(panel, "New Course")).doClick();
        check("New Course shows the course page", panel.header().title().equals("New Course"));
        check("the course page asks for the name and zip first", panel.footer().status().contains("5-digit zip"));
        panel.coursePage().cancelButton.doClick();
        check("Cancel on the course page goes back to Home", panel.header().title().equals("Golf Analyst Stats Home"));
        ((WoodButton) findByText(panel, "Edit Round")).doClick();
        check("Edit Round opens the newest round with its Edit button", panel.header().title().matches("\\d{5}:.+:.+")
                && panel.roundPage().editButton.isVisible() && panel.footer().status().contains("press Edit"));
        panel.roundPage().editButton.doClick();
        check("pressing Edit puts Edit in the header", panel.header().title().startsWith("Edit "));
        RoundPage windowRounds = panel.roundPage();
        int editHole = windowStore.roundsByDate().get(windowStore.roundsByDate().size() - 1).startHole();
        type(windowRounds.puttFields[editHole], "0");
        List<String> windowAsked = new ArrayList<>();
        panel.setAsker(question ->
        {
            windowAsked.add(question);
            return javax.swing.JOptionPane.NO_OPTION;
        });
        ((IconButton) findByTip(panel, "Home")).doClick();
        check("the Home button with unsaved changes asks first, and No stays",
                windowAsked.equals(List.of("Discard your changes?")) && panel.header().title().startsWith("Edit "));
        panel.setAsker(question -> javax.swing.JOptionPane.YES_OPTION);
        ((IconButton) findByTip(panel, "Home")).doClick();
        check("answering Yes goes Home", panel.header().title().equals("Golf Analyst Stats Home"));
        check("your own data: the window title does not say sample", !panel.windowTitle().contains("SAMPLE"));
        check("your own data: saving does not ask the sample question", panel.readyToSave());
        // The window's close box: it quits at once unless something typed is not saved
        List<String> quitAsked = new ArrayList<>();
        panel.setAsker(question ->
        {
            quitAsked.add(question);
            return javax.swing.JOptionPane.NO_OPTION;
        });
        panel.goHome();
        check("on Home the close box quits without asking", panel.canQuit() && quitAsked.isEmpty());
        ((WoodButton) findByText(panel, "New Round")).doClick();
        check("an untouched New Round quits without asking", panel.canQuit() && quitAsked.isEmpty());
        type(panel.roundPage().scoreFields[0], "5");
        check("with a score typed on New Round, the close box asks first and No stays",
                !panel.canQuit() && quitAsked.equals(List.of(RoundPage.DISCARD_QUESTION)));
        ((IconButton) findByTip(panel, "Home")).doClick();
        check("the Home button on New Round now asks too, and No stays", panel.header().title().equals("New Round")
                && quitAsked.size() == 2);
        ((WoodButton) findByText(panel, "Cancel")).doClick();
        check("so does Cancel on New Round", panel.header().title().equals("New Round") && quitAsked.size() == 3);
        panel.setAsker(question -> javax.swing.JOptionPane.YES_OPTION);
        check("answering Yes lets the program quit", panel.canQuit());
        ((WoodButton) findByText(panel, "Cancel")).doClick();
        check("and Yes on Cancel goes Home", panel.header().title().equals("Golf Analyst Stats Home"));
        quitAsked.clear();
        panel.setAsker(question ->
        {
            quitAsked.add(question);
            return javax.swing.JOptionPane.NO_OPTION;
        });
        ((WoodButton) findByText(panel, "New Course")).doClick();
        check("an untouched New Course quits without asking", panel.canQuit() && quitAsked.isEmpty());
        panel.coursePage().courseField.setText("Pine Hollow");
        check("with a name typed on New Course, the close box asks first", !panel.canQuit()
                && quitAsked.equals(List.of(RoundPage.DISCARD_QUESTION)));
        ((WoodButton) findByText(panel, "Cancel")).doClick();
        check("Cancel on New Course asks too, and No stays", panel.header().title().equals("New Course") && quitAsked.size() == 2);
        panel.setAsker(question -> javax.swing.JOptionPane.YES_OPTION);
        ((WoodButton) findByText(panel, "Cancel")).doClick();
        check("Yes on Cancel leaves New Course", panel.header().title().equals("Golf Analyst Stats Home"));
        ((WoodButton) findByText(panel, "Stats Board")).doClick();
        check("the Stats Board has nothing to lose, so it quits without asking", panel.canQuit());
        ((IconButton) findByTip(panel, "Home")).doClick();
        ((WoodButton) findByText(panel, "Best Round")).doClick();
        check("the Best Round button opens Best Round on Single", panel.header().title().startsWith("Best Single: ")
                && panel.bestPage().singleButton.isSelected());
        ((WoodButton) findByText(panel.bestPage(), "Close")).doClick();
        check("Close on Best Round goes Home", panel.header().title().equals("Golf Analyst Stats Home"));
        deleteFolder(windowTemp);

        checkCourseTyping();
        checkHelpFormatter();
        checkCoursePage(coursePage, store, shell, temp);
        checkRoundEditing(page, store, shell, temp);
        checkRoundLook(page);
        checkPeek();
        checkFooterHand();
        checkHistoricalScreens();
        checkHistoricalBox();
        checkIncludeHistoricalScreens();
        checkStatsPage();
        checkBestRoundPage();
        checkSampleSafeguard();

        List<Path> leftovers = new ArrayList<>();
        try (Stream<Path> files = Files.list(temp))
        {
            files.forEach(leftovers::add);
        }
        for (Path file : leftovers)
        {
            Files.deleteIfExists(file);
        }
        Files.deleteIfExists(temp);
    }

    /*
     * deleteFolder(folder): deletes a temporary test folder and everything in it.
     * Passed in:  folder - the folder
     * Passed out: nothing (throws IOException if something cannot be deleted)
     */
    private static void deleteFolder(Path folder) throws IOException
    {
        List<Path> paths = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(folder))
        {
            walk.forEach(paths::add);
        }
        for (int lp = paths.size() - 1; lp >= 0; lp = lp - 1)
        {
            Files.deleteIfExists(paths.get(lp));
        }
    }

    /*
     +--------------------------------------------------------------------------+
     | typeKeys(kind, nine, keys, advances)                                     |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Types keys one at a time into a box with the New Course typing rules,    |
     | as the user would, and counts how often the cursor would move on.        |
     |                                                                          |
     | Passed in:  kind     - which kind of box                                 |
     |             nine     - true for a nine-hole course                       |
     |             keys     - the keys, in order                                |
     |             advances - one-element array that receives the move count    |
     | Passed out: what the box holds afterwards                                |
     +--------------------------------------------------------------------------+
     */
    private static String typeKeys(CoursePage.BoxFilter.Kind kind, boolean nine, String keys, int[] advances)
    {
        advances[0] = 0;
        javax.swing.text.PlainDocument doc = new javax.swing.text.PlainDocument();
        doc.setDocumentFilter(new CoursePage.BoxFilter(kind, () -> nine, () -> false, () -> advances[0] = advances[0] + 1));
        try
        {
            for (char key : keys.toCharArray())
            {
                doc.insertString(doc.getLength(), String.valueOf(key), null);
            }
            return doc.getText(0, doc.getLength());
        }
        catch (javax.swing.text.BadLocationException unexpected)
        {
            return "error: " + unexpected.getMessage();
        }
    }

    /*
     +--------------------------------------------------------------------------+
     | checkCourseTyping()                                                      |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The New Course typing rules, box by box: par 3-6, stroke index (a lone   |
     | 1 waits, then 0-8), slope (two digits from 5-9, three from 1, nothing    |
     | above 155), rating (moves on after the digit after the point), yardage,  |
     | zip and state; and the name tidying rule.                                |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    private static void checkCourseTyping()
    {
        CoursePage.BoxFilter.Kind par = CoursePage.BoxFilter.Kind.PAR;
        CoursePage.BoxFilter.Kind index = CoursePage.BoxFilter.Kind.STROKE_INDEX;
        CoursePage.BoxFilter.Kind slope = CoursePage.BoxFilter.Kind.SLOPE;
        CoursePage.BoxFilter.Kind rating = CoursePage.BoxFilter.Kind.RATING;
        int[] moves = new int[1];

        check("par: 7, 2 and letters cannot be typed", typeKeys(par, false, "72x", moves).isEmpty() && moves[0] == 0);
        check("par: 4 is kept and moves on", typeKeys(par, false, "4", moves).equals("4") && moves[0] == 1);
        check("par: a digit typed into a full box starts it over", typeKeys(par, false, "45", moves).equals("5") && moves[0] == 2);

        check("stroke index: a lone 1 waits", typeKeys(index, false, "1", moves).equals("1") && moves[0] == 0);
        check("stroke index: 1 then 2 makes 12 and moves on", typeKeys(index, false, "12", moves).equals("12") && moves[0] == 1);
        check("stroke index: 1 then 8 makes 18", typeKeys(index, false, "18", moves).equals("18") && moves[0] == 1);
        check("stroke index: after a 1, a 9 is refused (nothing above 18)", typeKeys(index, false, "19", moves).equals("1") && moves[0] == 0);
        check("stroke index: 2 moves on at once", typeKeys(index, false, "2", moves).equals("2") && moves[0] == 1);
        check("stroke index: 9 moves on at once", typeKeys(index, false, "9", moves).equals("9") && moves[0] == 1);
        check("stroke index: cannot start with 0", typeKeys(index, false, "0", moves).isEmpty());
        check("stroke index on a nine-hole course: 1 moves on at once", typeKeys(index, true, "1", moves).equals("1") && moves[0] == 1);

        check("slope: 0, 2, 3 and 4 cannot start it", typeKeys(slope, false, "0", moves).isEmpty() && typeKeys(slope, false, "2", moves).isEmpty()
                && typeKeys(slope, false, "3", moves).isEmpty() && typeKeys(slope, false, "4", moves).isEmpty());
        check("slope: 5 then 4 is refused (below 55)", typeKeys(slope, false, "54", moves).equals("5") && moves[0] == 0);
        check("slope: 55 moves on after two digits", typeKeys(slope, false, "55", moves).equals("55") && moves[0] == 1);
        check("slope: 99 moves on after two digits", typeKeys(slope, false, "99", moves).equals("99") && moves[0] == 1);
        check("slope: 1 and 12 wait", typeKeys(slope, false, "12", moves).equals("12") && moves[0] == 0);
        check("slope: 123 moves on after three digits", typeKeys(slope, false, "123", moves).equals("123") && moves[0] == 1);
        check("slope: 155 is the highest", typeKeys(slope, false, "155", moves).equals("155") && moves[0] == 1);
        check("slope: 156 is refused", typeKeys(slope, false, "156", moves).equals("15") && moves[0] == 0);
        check("slope: 16 is refused", typeKeys(slope, false, "16", moves).equals("1"));

        check("rating: 72 waits for the point", typeKeys(rating, false, "72", moves).equals("72") && moves[0] == 0);
        check("rating: moves on after the digit after the point", typeKeys(rating, false, "72.3", moves).equals("72.3") && moves[0] == 1);
        check("rating: a third digit before the point is refused", typeKeys(rating, false, "723", moves).equals("72"));
        check("rating: a nine-hole rating like 34.6", typeKeys(rating, false, "34.6", moves).equals("34.6") && moves[0] == 1);

        check("yardage: four digits move on", typeKeys(CoursePage.BoxFilter.Kind.YARDAGE, false, "6450", moves).equals("6450") && moves[0] == 1);
        check("yardage: cannot start with 0", typeKeys(CoursePage.BoxFilter.Kind.YARDAGE, false, "0", moves).isEmpty());
        check("zip: five digits move on", typeKeys(CoursePage.BoxFilter.Kind.ZIP, false, "29100", moves).equals("29100") && moves[0] == 1);
        check("zip: letters are refused", typeKeys(CoursePage.BoxFilter.Kind.ZIP, false, "a", moves).isEmpty());
        check("state: two letters become capitals and move on", typeKeys(CoursePage.BoxFilter.Kind.STATE, false, "sc", moves).equals("SC") && moves[0] == 1);
        check("state: digits are refused", typeKeys(CoursePage.BoxFilter.Kind.STATE, false, "1", moves).isEmpty());

        check("names: extra spaces are removed", CoursePage.tidyName("  Sample    Links ").equals("Sample Links"));
        check("names: all lower case gets capitals", CoursePage.tidyName("oak hollow").equals("Oak Hollow"));
        check("names: all upper case gets capitals", CoursePage.tidyName("PINE TOWN").equals("Pine Town"));
        check("names: TPC in a mixed name survives", CoursePage.tidyName("TPC Sawgrass").equals("TPC Sawgrass"));
        check("names: McDonough survives", CoursePage.tidyName("McDonough").equals("McDonough"));
        check("names: a hyphenated name gets both capitals", CoursePage.tidyName("winston-salem").equals("Winston-Salem"));
    }

    /*
     +--------------------------------------------------------------------------+
     | fillTee(page, tee, name, rating, slope, pars, index)                     |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Types one tee into the New Course screen.                                |
     |                                                                          |
     | Passed in:  page   - the screen                                          |
     |             tee    - which tee, 0 to 3                                   |
     |             name, rating, slope - its top boxes                          |
     |             pars, index - one value per hole of the course               |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    private static void fillTee(CoursePage page, int tee, String name, String rating, String slope, int[] pars, int[] index)
    {
        page.teeNameFields[tee].setText(name);
        page.ratingFields[tee].setText(rating);
        page.slopeFields[tee].setText(slope);
        for (int hole = 0; hole < pars.length; hole = hole + 1)
        {
            page.parFields[tee][hole].setText(Integer.toString(pars[hole]));
            page.indexFields[tee][hole].setText(Integer.toString(index[hole]));
        }
    }

    /*
     +--------------------------------------------------------------------------+
     | checkRoundEditing(page, store, shell, temp)                              |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Editing a saved round on a copy of the sample data: the Edit button,     |
     | what opens and what stays locked, the "Edit ..." title and help files,   |
     | "Discard your changes?" on the arrows and Cancel (No stays, Yes          |
     | leaves), and Save: only that round's line changes, the comments and      |
     | every other line are untouched, rounds.tsv.bak holds the file as it      |
     | was, and the totals are recalculated.                                    |
     |                                                                          |
     | Passed in:  page  - the round screen built on the test shell             |
     |             store - the test data store, shell - the test shell          |
     |             temp  - the temporary data folder                            |
     | Passed out: nothing (throws IOException if the data cannot be read)      |
     +--------------------------------------------------------------------------+
     */
    private static void checkRoundEditing(RoundPage page, DataStore store, TestShell shell, Path temp) throws IOException
    {
        Path roundsFile = temp.resolve(DataLoader.ROUNDS_FILE);
        Path backup = temp.resolve(DataLoader.ROUNDS_FILE + golfanalyst.RoundWriter.BACKUP_SUFFIX);
        int last = store.roundsByDate().size() - 1;
        Round original = store.roundsByDate().get(last);
        page.showRoundAt(last);
        check("a saved round shows Edit and no Save", page.editButton.isVisible() && !page.saveButton.isVisible());
        check("looking at a round asks nothing on leaving", page.canLeave());

        page.editButton.doClick();
        String prefix = String.format("Edit %05d:", original.id());
        check("the title says it is edit mode", page.title().startsWith(prefix));
        check("edit mode has its own help files", page.helpKey().equals("round-edit"));
        int hole = original.startHole();
        check("edit mode keeps the course/tee and holes locked", !page.teeCombo.isEnabled() && !page.holesCombo.isEnabled());
        check("edit mode opens the date, PCC, scores, putts and Sand", page.dateField.isEditable() && page.pccField.isEditable()
                && page.scoreFields[hole].isEditable() && page.puttFields[hole].isEditable() && page.sandChecks[hole].isEnabled());
        check("edit mode shows Save and Cancel, not Edit", page.saveButton.isVisible() && !page.editButton.isVisible()
                && page.cancelButton.getText().equals("Cancel"));
        check("the round's numbers are loaded for editing", page.scoreFields[hole].getText().equals(Integer.toString(original.scores()[0])));
        shell.asked.clear();
        check("with nothing changed, leaving asks nothing", page.canLeave() && shell.asked.isEmpty());

        int newScore = original.scores()[0] + 1;
        type(page.scoreFields[hole], Integer.toString(newScore));
        check("a change is noticed", page.hasChanges());
        shell.answerYes = false;
        page.previousButton.doClick();
        check("an arrow with unsaved changes asks \"Discard your changes?\"", shell.asked.contains("Discard your changes?"));
        check("answering No stays on the edit", page.title().startsWith(prefix));
        page.cancelButton.doClick();
        check("Cancel with unsaved changes asks too, and No stays", page.title().startsWith(prefix) && shell.asked.size() == 2);

        List<String> before = Files.readAllLines(roundsFile, StandardCharsets.UTF_8);
        String beforeText = Files.readString(roundsFile, StandardCharsets.UTF_8);
        int roundsBefore = store.roundsByDate().size();
        check("an edited round that checks out can be saved", page.saveButton.isEnabled());
        page.saveButton.doClick();
        List<String> after = Files.readAllLines(roundsFile, StandardCharsets.UTF_8);
        int changedLines = 0;
        boolean othersUntouched = before.size() == after.size();
        for (int lp = 0; othersUntouched && lp < before.size(); lp = lp + 1)
        {
            if (!before.get(lp).equals(after.get(lp)))
            {
                changedLines = changedLines + 1;
                othersUntouched = before.get(lp).startsWith(original.id() + "\t") && after.get(lp).startsWith(original.id() + "\t");
            }
        }
        check("saving changes only that round's line, with the same RoundID", othersUntouched && changedLines == 1);
        check("the comments in rounds.tsv are untouched", after.get(0).equals(before.get(0)) && after.get(0).startsWith("#"));
        check("the backup holds rounds.tsv as it was", Files.exists(backup)
                && Files.readString(backup, StandardCharsets.UTF_8).equals(beforeText));
        Round saved = null;
        for (Round round : store.roundsByDate())
        {
            saved = round.id() == original.id() ? round : saved;
        }
        check("after saving everything is reloaded with the new score", saved != null && saved.scores()[0] == newScore
                && saved.totalScore() == original.totalScore() + 1 && store.roundsByDate().size() == roundsBefore);
        check("after saving the round is shown read-only again", !page.title().startsWith("Edit") && page.editButton.isVisible());
        check("the footer says the changes were saved and where the backup is", shell.status.startsWith("Changes saved")
                && shell.status.contains("rounds.tsv.bak"));
        check("the edited file loads with no errors", DataLoader.load(temp).errorCount() == 0);

        page.editButton.doClick();
        type(page.puttFields[hole], "0");
        shell.answerYes = true;
        String fileNow = Files.readString(roundsFile, StandardCharsets.UTF_8);
        page.previousButton.doClick();
        check("answering Yes discards the changes and moves on", !page.title().startsWith("Edit")
                && Files.readString(roundsFile, StandardCharsets.UTF_8).equals(fileNow));
        page.editButton.doClick();
        page.cancelButton.doClick();
        check("Cancel with nothing changed goes back to the round", !page.title().startsWith("Edit") && page.editButton.isVisible());
    }

    /*
     +--------------------------------------------------------------------------+
     | checkHelpFormatter()                                                     |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The help text styling: the title line is the window title and is not     |
     | repeated; "# " makes a heading; a label up to its colon is bold; the     |
     | first sentence of a numbered step is bold; anything else is plain; the   |
     | styles and spacing that reach the window; and every real help file.      |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing (throws IOException if a help file cannot be read)   |
     +--------------------------------------------------------------------------+
     */
    private static void checkHelpFormatter() throws IOException
    {
        String sample = "Golf Analyst Home - Info\n\n# The tees\n\nLow Index: The lowest index you had.\n"
                + "1. Choose the course and tees. Open the list at the top.\n\nThis is the Home screen.\n";
        List<HelpFormatter.Piece> pieces = HelpFormatter.pieces(sample);
        check("help: the first line is the title", HelpFormatter.title(sample).equals("Golf Analyst Home - Info"));
        check("help: the title is not repeated in the body", pieces.stream().noneMatch(piece -> piece.text().contains("Golf Analyst Home")));
        check("help: blank lines are skipped, leaving six pieces", pieces.size() == 6);
        check("help: \"# \" makes a heading and is not shown", pieces.get(0).equals(
                new HelpFormatter.Piece("The tees", HelpFormatter.Style.HEADING, true)));
        check("help: a label is bold up to and including its colon",
                pieces.get(1).equals(new HelpFormatter.Piece("Low Index:", HelpFormatter.Style.BOLD, false))
                        && pieces.get(2).equals(new HelpFormatter.Piece(" The lowest index you had.", HelpFormatter.Style.PLAIN, true)));
        check("help: a numbered step has its first sentence in bold",
                pieces.get(3).equals(new HelpFormatter.Piece("1. Choose the course and tees.", HelpFormatter.Style.BOLD, false))
                        && pieces.get(4).equals(new HelpFormatter.Piece(" Open the list at the top.", HelpFormatter.Style.PLAIN, true)));
        check("help: an ordinary paragraph is plain", pieces.get(5).equals(
                new HelpFormatter.Piece("This is the Home screen.", HelpFormatter.Style.PLAIN, true)));
        check("help: a colon after a period is not a label", HelpFormatter.pieces("T\nTwo rules to remember. Count: putts.\n").get(0).style()
                == HelpFormatter.Style.PLAIN);
        check("help: a colon after the first 45 characters is not a label", HelpFormatter.pieces(
                "T\nThis line runs on for well over forty-five letters before: here\n").get(0).style() == HelpFormatter.Style.PLAIN);
        check("help: a line starting with # but no space is not a heading",
                HelpFormatter.pieces("T\n#notaheading\n").get(0).style() == HelpFormatter.Style.PLAIN);

        javax.swing.text.DefaultStyledDocument document = new javax.swing.text.DefaultStyledDocument();
        HelpWindow.fill(document, pieces);
        javax.swing.text.AttributeSet heading = document.getCharacterElement(0).getAttributes();
        check("help window: a heading is 18 pt bold, underlined, dark brown",
                javax.swing.text.StyleConstants.getFontSize(heading) == 18 && javax.swing.text.StyleConstants.isBold(heading)
                        && javax.swing.text.StyleConstants.isUnderline(heading)
                        && javax.swing.text.StyleConstants.getForeground(heading).equals(Theme.HELP_HEADING));
        javax.swing.text.AttributeSet headingParagraph = document.getParagraphElement(0).getAttributes();
        check("help window: 16 px above a heading and 4 below",
                javax.swing.text.StyleConstants.getSpaceAbove(headingParagraph) == 16f
                        && javax.swing.text.StyleConstants.getSpaceBelow(headingParagraph) == 4f);
        int plainAt = 0;
        try
        {
            plainAt = document.getText(0, document.getLength()).indexOf("The lowest");
        }
        catch (javax.swing.text.BadLocationException unexpected)
        {
            check("help window: the text can be read back", false);
        }
        javax.swing.text.AttributeSet plain = document.getCharacterElement(plainAt).getAttributes();
        check("help window: body text is 15 pt plain black", javax.swing.text.StyleConstants.getFontSize(plain) == 15
                && !javax.swing.text.StyleConstants.isBold(plain) && javax.swing.text.StyleConstants.getForeground(plain).equals(java.awt.Color.BLACK));
        check("help window: 9 px below a paragraph",
                javax.swing.text.StyleConstants.getSpaceBelow(document.getParagraphElement(plainAt).getAttributes()) == 9f);
        javax.swing.text.AttributeSet label = document.getCharacterElement(plainAt - 3).getAttributes();
        check("help window: a label is bold", javax.swing.text.StyleConstants.isBold(label));

        for (String file : new String[] {"home-info.txt", "home-help.txt", "round-new-info.txt", "round-new-help.txt",
                "round-view-info.txt", "round-view-help.txt", "round-edit-info.txt", "round-edit-help.txt", "course-info.txt", "course-help.txt"})
        {
            String text = Files.readString(Path.of("help", file));
            List<HelpFormatter.Piece> filePieces = HelpFormatter.pieces(text);
            boolean hasHeading = filePieces.stream().anyMatch(piece -> piece.style() == HelpFormatter.Style.HEADING);
            check(file + ": has its section headings marked", hasHeading);
            check(file + ": no # is shown", filePieces.stream().noneMatch(piece -> piece.text().startsWith("#")));
            check(file + ": the title is not repeated", !filePieces.get(0).text().equals(HelpFormatter.title(text)));
        }
    }

    /*
     * layoutAll(part): lays out a component and everything inside it, as a window would before showing it.
     * Passed in:  part - the component
     * Passed out: nothing
     */
    private static void layoutAll(Component part)
    {
        if (part instanceof Container box)
        {
            box.doLayout();
            for (Component child : box.getComponents())
            {
                layoutAll(child);
            }
        }
    }

    /*
     * isGoldOutline(field): whether a box has the 2 px gold cursor outline.
     */
    private static boolean isGoldOutline(JComponent field)
    {
        return field.getBorder() instanceof javax.swing.border.LineBorder line
                && line.getLineColor().equals(Theme.GOLD) && line.getThickness() == 2;
    }

    /*
     +--------------------------------------------------------------------------+
     | checkRoundLook(page)                                                     |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The round screen's look at the default page size (944 x 626): the Out    |
     | and In frames and the results column all 264 px tall, the details frame  |
     | 6 px under them and exactly as wide, the arrows 8 px under that and      |
     | centred, the whole group centred on the photo; the cell sizes; and the   |
     | faces: white cells to type in, cream with a gold outline at the cursor,  |
     | grey while looking at a saved round, white again while editing it.       |
     |                                                                          |
     | Passed in:  page - the round screen built on the test shell              |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    private static void checkRoundLook(RoundPage page)
    {
        page.showNew();
        page.setSize(944, 626);
        layoutAll(page);
        java.awt.Dimension tableSize = new java.awt.Dimension(234, 264);
        check("the Out and In frames hug their grids: 234 x 264",
                page.outFrame.getSize().equals(tableSize) && page.inFrame.getSize().equals(tableSize));
        check("the results column is as tall as the tables: 194 x 264", page.resultsFrame.getSize().equals(new java.awt.Dimension(194, 264)));
        check("the results column is 10 px right of the In frame", page.resultsFrame.getX() == page.inFrame.getX() + 234 + 10);
        check("the details frame is 6 px under the tables", page.detailsFrame.getY() == 264 + 6);
        check("the details frame is as wide as the tables and the results column (682)",
                page.detailsFrame.getWidth() == 682 && page.group.getWidth() == 682 && page.detailsFrame.getX() == 0);
        check("the details frame is 100 px tall", page.detailsFrame.getHeight() == 100);
        check("the arrows are 8 px under the details frame",
                page.navRow.getY() == page.detailsFrame.getY() + page.detailsFrame.getHeight() + 8);
        check("the arrows are centred", Math.abs(page.navRow.getX() + page.navRow.getWidth() / 2 - page.group.getWidth() / 2) <= 1);
        java.awt.Rectangle group = javax.swing.SwingUtilities.convertRectangle(page.group.getParent(), page.group.getBounds(), page);
        int leftGap = group.x;
        int rightGap = page.getWidth() - (group.x + group.width);
        check("the group is centred across the page (" + leftGap + " / " + rightGap + " px of photo)", Math.abs(leftGap - rightGap) <= 1 && leftGap > 0);
        java.awt.Container middle = page.group.getParent();
        int topGap = page.group.getY();
        int bottomGap = middle.getHeight() - (page.group.getY() + page.group.getHeight());
        check("the group is centred down the page (" + topGap + " / " + bottomGap + " px of photo)", Math.abs(topGap - bottomGap) <= 1 && topGap > 0);
        check("everything fits the 626 px page", group.y + group.height <= 626 - 6);

        check("a hole number cell is 34 x 22 on white", page.holeLabels[0].getSize().equals(new java.awt.Dimension(34, 22))
                && page.holeLabels[0].getBackground().equals(java.awt.Color.WHITE));
        check("a par cell is 30 x 22", page.parBoxes[0].getSize().equals(new java.awt.Dimension(30, 22)));
        check("a score cell is 42 x 22, bold 14 pt", page.scoreFields[0].getSize().equals(new java.awt.Dimension(42, 22))
                && page.scoreFields[0].getFont().isBold() && page.scoreFields[0].getFont().getSize2D() == 14f);
        check("a putt cell is 38 x 22", page.puttFields[0].getSize().equals(new java.awt.Dimension(38, 22)));
        check("the Fwy and Sand cells are 32 and 36 wide", page.fairwayChecks[0].getSize().equals(new java.awt.Dimension(32, 22))
                && page.sandChecks[0].getSize().equals(new java.awt.Dimension(36, 22)));
        check("a results value is 82 wide, 13 pt", page.ratingBox.getWidth() == 82 && page.indexBox.getWidth() == 82
                && page.resultBoxes[0].getFont().getSize2D() == 13f);
        check("the date cell is 86 wide and the other details 62", page.dateField.getWidth() == 86
                && page.scoreBox.getWidth() == 62 && page.twoPuttBox.getWidth() == 62);
        check("Save and Cancel are 112 x 32", page.saveButton.getSize().equals(new java.awt.Dimension(112, 32))
                && page.cancelButton.getSize().equals(new java.awt.Dimension(112, 32)));
        check("Save is above Cancel, 6 px apart", page.cancelButton.getY() == page.saveButton.getY() + 32 + 6);

        check("entering: a score cell is white", page.scoreFields[0].getBackground().equals(java.awt.Color.WHITE));
        check("entering: the date cell is white", page.dateField.getBackground().equals(java.awt.Color.WHITE));
        check("entering: the Fwy and Sand cells are white", page.fairwayChecks[0].getBackground().equals(java.awt.Color.WHITE)
                && page.sandChecks[0].getBackground().equals(java.awt.Color.WHITE));
        focusOn(page.scoreFields[0], true);
        check("the cursor's cell is cream with a 2 px gold outline",
                page.scoreFields[0].getBackground().equals(Theme.FIELD_FOCUS) && isGoldOutline(page.scoreFields[0]));
        focusOn(page.scoreFields[0], false);
        check("when the cursor leaves it is white with its hairlines again", page.scoreFields[0].getBackground().equals(java.awt.Color.WHITE)
                && page.scoreFields[0].getBorder() instanceof javax.swing.border.MatteBorder);
        focusOn(page.puttFields[3], true);
        type(page.scoreFields[3], "3");
        type(page.puttFields[3], "3");
        check("a wrong cell stays pink even with the cursor in it", page.puttFields[3].getBackground().equals(Theme.FIELD_ERROR));
        type(page.puttFields[3], "");
        type(page.scoreFields[3], "");
        focusOn(page.puttFields[3], false);

        page.showLatest();
        check("looking: the score and putt cells are grey", page.scoreFields[0].getBackground().equals(Theme.FIELD_FACE)
                && page.puttFields[0].getBackground().equals(Theme.FIELD_FACE));
        check("looking: the date cell is grey", page.dateField.getBackground().equals(Theme.FIELD_FACE));
        check("looking: the Fwy and Sand cells are grey", page.fairwayChecks[0].getBackground().equals(Theme.FIELD_FACE)
                && page.sandChecks[0].getBackground().equals(Theme.FIELD_FACE));
        focusOn(page.scoreFields[0], true);
        check("looking: a clicked cell gets no gold outline", !isGoldOutline(page.scoreFields[0]));
        focusOn(page.scoreFields[0], false);
        layoutAll(page);
        check("looking: Edit is where Save was, above Close", page.editButton.isVisible() && !page.saveButton.isVisible()
                && page.editButton.getY() == 0 && page.cancelButton.getText().equals("Close"));
        page.editButton.doClick();
        check("editing: the score cells and the date are white again", page.scoreFields[0].getBackground().equals(java.awt.Color.WHITE)
                && page.dateField.getBackground().equals(java.awt.Color.WHITE)
                && page.sandChecks[0].getBackground().equals(java.awt.Color.WHITE));
        page.cancelButton.doClick();
        check("Cancel with no changes goes back to looking", page.scoreFields[0].getBackground().equals(Theme.FIELD_FACE));
        page.showNew();
    }

    /*
     +--------------------------------------------------------------------------+
     | checkPeek()                                                              |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The press-and-hold peek on the version text of the shared background:    |
     | paints a panel to an image to find the label, then presses and lets go   |
     | of the mouse there and checks that the panel's controls are not painted  |
     | while it is held and are painted again after. Also the hand cursor, the  |
     | tool tip, that a press elsewhere does nothing, that a release anywhere   |
     | ends it, and that keys are swallowed while peeking.                      |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    private static void checkPeek()
    {
        java.awt.KeyboardFocusManager originalKeys = java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager();
        KeySpy keys = new KeySpy();
        java.awt.KeyboardFocusManager.setCurrentKeyboardFocusManager(keys);
        int[] paints = {0};
        BackdropPanel backdrop = new BackdropPanel();
        backdrop.setLayout(new java.awt.BorderLayout());
        JComponent control = new JComponent()
        {
            @Override
            protected void paintComponent(java.awt.Graphics graphics)
            {
                paints[0] = paints[0] + 1;
            }
        };
        backdrop.add(control, java.awt.BorderLayout.CENTER);
        backdrop.setSize(944, 626);
        layoutAll(backdrop);
        java.awt.image.BufferedImage picture = new java.awt.image.BufferedImage(944, 626, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D canvas = picture.createGraphics();
        backdrop.paint(canvas);
        java.awt.Rectangle label = backdrop.versionLabelBounds();
        check("the controls are painted normally", paints[0] == 1);
        check("the version label is at the bottom left (" + label + ")", label.x == 10 && label.y + label.height <= 626
                && label.y > 626 - 50 && label.width > 100);
        boolean yellow = false;
        for (int x = label.x; x < label.x + label.width && !yellow; x = x + 1)
        {
            for (int y = label.y; y < label.y + label.height && !yellow; y = y + 1)
            {
                yellow = picture.getRGB(x, y) == 0xFFFFFF00;
            }
        }
        check("the yellow version text is painted inside its bounds", yellow);

        int insideX = label.x + label.width / 2;
        int insideY = label.y + label.height / 2;
        sendMouse(backdrop, java.awt.event.MouseEvent.MOUSE_MOVED, insideX, insideY);
        check("the mouse shows a hand over the label", backdrop.getCursor().getType() == java.awt.Cursor.HAND_CURSOR);
        check("the label has the peek tool tip", BackdropPanel.PEEK_TIP.equals(backdrop.getToolTipText(
                mouseEvent(backdrop, java.awt.event.MouseEvent.MOUSE_MOVED, insideX, insideY))));
        check("the peek tool tip says what to do", BackdropPanel.PEEK_TIP.equals("Press and hold to see the whole picture."));
        sendMouse(backdrop, java.awt.event.MouseEvent.MOUSE_MOVED, 400, 300);
        check("the hand goes away off the label", backdrop.getCursor().getType() == java.awt.Cursor.DEFAULT_CURSOR);
        check("there is no tool tip off the label", backdrop.getToolTipText(mouseEvent(backdrop, java.awt.event.MouseEvent.MOUSE_MOVED, 400, 300)) == null);

        sendMouse(backdrop, java.awt.event.MouseEvent.MOUSE_PRESSED, 400, 300);
        check("pressing away from the label does not peek", !backdrop.isPeeking());
        sendMouse(backdrop, java.awt.event.MouseEvent.MOUSE_RELEASED, 400, 300);

        sendMouse(backdrop, java.awt.event.MouseEvent.MOUSE_PRESSED, label.x - 5, label.y - 5);
        check("pressing within 6 px of the label peeks", backdrop.isPeeking());
        sendMouse(backdrop, java.awt.event.MouseEvent.MOUSE_RELEASED, label.x - 5, label.y - 5);

        sendMouse(backdrop, java.awt.event.MouseEvent.MOUSE_PRESSED, insideX, insideY);
        check("holding the button on the label peeks", backdrop.isPeeking());
        paints[0] = 0;
        backdrop.paint(canvas);
        check("while peeking the controls are not painted", paints[0] == 0);
        java.awt.event.KeyEvent key = new java.awt.event.KeyEvent(control, java.awt.event.KeyEvent.KEY_TYPED,
                System.currentTimeMillis(), 0, java.awt.event.KeyEvent.VK_UNDEFINED, '5');
        check("while peeking the keyboard is ignored", keys.swallows(key));
        sendMouse(backdrop, java.awt.event.MouseEvent.MOUSE_DRAGGED, 2000, -300);
        check("dragging off the label and out of the window still peeks", backdrop.isPeeking());
        sendMouse(backdrop, java.awt.event.MouseEvent.MOUSE_RELEASED, 2000, -300);
        check("letting go anywhere, even outside the window, ends the peek", !backdrop.isPeeking());
        paints[0] = 0;
        backdrop.paint(canvas);
        check("after letting go the controls are painted again", paints[0] == 1);
        check("after letting go the keyboard works again", !keys.swallows(key));
        canvas.dispose();
        java.awt.KeyboardFocusManager.setCurrentKeyboardFocusManager(originalKeys);
    }

    /*
     +--------------------------------------------------------------------------+
     | KeySpy                                                                   |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | A keyboard focus manager for the peek test that can say whether one of   |
     | the extra key handlers added to it (the peek's key blocker) would        |
     | swallow a key.                                                           |
     +--------------------------------------------------------------------------+
     */
    private static final class KeySpy extends java.awt.DefaultKeyboardFocusManager
    {
        /*
         * swallows(key): whether an added key handler takes the key before any control sees it.
         */
        boolean swallows(java.awt.event.KeyEvent key)
        {
            List<java.awt.KeyEventDispatcher> handlers = getKeyEventDispatchers();
            if (handlers == null)
            {
                return false;
            }
            for (java.awt.KeyEventDispatcher handler : handlers)
            {
                if (handler.dispatchKeyEvent(key))
                {
                    return true;
                }
            }
            return false;
        }
    }

    /*
     * mouseEvent(target, kind, x, y): a left-button mouse event at a point.
     */
    private static java.awt.event.MouseEvent mouseEvent(Component target, int kind, int x, int y)
    {
        boolean held = kind == java.awt.event.MouseEvent.MOUSE_PRESSED || kind == java.awt.event.MouseEvent.MOUSE_DRAGGED;
        int modifiers = (held ? java.awt.event.InputEvent.BUTTON1_DOWN_MASK : 0);
        int button = (kind == java.awt.event.MouseEvent.MOUSE_MOVED || kind == java.awt.event.MouseEvent.MOUSE_DRAGGED
                ? java.awt.event.MouseEvent.NOBUTTON : java.awt.event.MouseEvent.BUTTON1);
        return new java.awt.event.MouseEvent(target, kind, System.currentTimeMillis(), modifiers, x, y, 1, false, button);
    }

    /*
     * sendMouse(target, kind, x, y): delivers a left-button mouse event to a component's own mouse listeners.
     */
    private static void sendMouse(Component target, int kind, int x, int y)
    {
        target.dispatchEvent(mouseEvent(target, kind, x, y));
    }

    /*
     +--------------------------------------------------------------------------+
     | checkFooterHand()                                                        |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Clicking the footer message lists the problems found in the data files.  |
     | Checks that the message shows the pointing hand only while there are     |
     | problems to list: not on the sample data (which has none), but on a      |
     | data folder with a bad round line.                                       |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing (throws IOException if a temporary file fails)       |
     +--------------------------------------------------------------------------+
     */
    private static void checkFooterHand() throws IOException
    {
        AppPanel clean = new AppPanel(new DataStore(Path.of("sample-data")), Path.of("help"), "");
        check("footer: with no problems in the data the message shows the ordinary arrow",
                clean.footer().statusCursor().getType() == java.awt.Cursor.DEFAULT_CURSOR);
        clean.setStatus("Something else.");
        check("footer: it stays the arrow after another message", clean.footer().statusCursor().getType() == java.awt.Cursor.DEFAULT_CURSOR);

        Path temp = Files.createTempDirectory("golfanalyst-footer-test");
        Files.copy(Path.of("sample-data", DataLoader.COURSES_FILE), temp.resolve(DataLoader.COURSES_FILE));
        Files.writeString(temp.resolve(DataLoader.ROUNDS_FILE), golfanalyst.RoundWriter.STANDARD_HEADER + "\n1\t1\tnot a date\t\t\t5\t2\t0\t0\n",
                StandardCharsets.UTF_8);
        DataStore broken = new DataStore(temp);
        AppPanel withProblems = new AppPanel(broken, Path.of("help"), "");
        check("footer: with problems in the data the message shows the pointing hand", !broken.dataset().problems().isEmpty()
                && withProblems.footer().statusCursor().getType() == java.awt.Cursor.HAND_CURSOR);
        deleteFolder(temp);
    }

    /*
     +--------------------------------------------------------------------------+
     | checkHistoricalScreens()                                                 |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | A historical round on the screens, in a temporary data folder with the   |
     | sample courses, one ordinary round and one historical round: the viewer  |
     | shows it with "historical" for the differential and says so in the       |
     | footer, Home counts both rounds with "Include historical rounds"         |
     | ticked and only the ordinary one unticked, the start-up message says     |
     | how many are historical, and editing it keeps it historical.             |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing (throws IOException if a temporary file fails)       |
     +--------------------------------------------------------------------------+
     */
    private static void checkHistoricalScreens() throws IOException
    {
        Path temp = Files.createTempDirectory("golfanalyst-historical-screens");
        Files.copy(Path.of("sample-data", DataLoader.COURSES_FILE), temp.resolve(DataLoader.COURSES_FILE));
        String fives = "5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5";
        String twos = "2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2";
        String zeros = "0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0";
        String rows = "RoundID\tTeeID\tDate\tHoles\tPCC\tScores\tPutts\tFairways\tSand\tHistorical\n"
                + "1\t1\t2005-06-19\t\t\t" + fives + "\t" + twos + "\t" + zeros + "\t" + zeros + "\tY\n"
                + "2\t1\t2026-03-01\t\t\t" + fives + "\t" + twos + "\t" + zeros + "\t" + zeros + "\t\n";
        Path roundsFile = temp.resolve(DataLoader.ROUNDS_FILE);
        Files.writeString(roundsFile, rows, StandardCharsets.UTF_8);
        DataStore store = new DataStore(temp);
        TestShell shell = new TestShell(store);
        RoundPage page = new RoundPage(shell);
        HomePage home = new HomePage(shell);

        page.showRoundAt(0);
        check("a historical round can be looked at", page.title().startsWith("00001:"));
        check("its differential box says historical", page.differentialBox.getText().equals("historical")
                && page.indexBox.getText().equals("-"));
        check("the footer says it is historical", shell.status.endsWith(RoundPage.HISTORICAL_NOTE));
        page.showRoundAt(1);
        check("an ordinary round still shows its differential", !page.differentialBox.getText().equals("historical")
                && !shell.status.contains("Historical"));

        home.onShow();
        check("Home counts the historical round while Include historical rounds is ticked", home.totalRounds.getText().equals("2"));
        store.setIncludeHistorical(false);
        home.onShow();
        check("Home counts only the ordinary round when it is unticked", home.totalRounds.getText().equals("1"));
        store.setIncludeHistorical(true);
        AppPanel panel = new AppPanel(store, Path.of("help"), "");
        check("the start-up message says how many rounds are historical",
                panel.footer().status().contains("2 rounds loaded (1 historical)."));

        page.showRoundAt(0);
        page.editButton.doClick();
        check("editing a historical round still says historical", page.differentialBox.getText().equals("historical"));
        type(page.scoreFields[0], "6");
        page.saveButton.doClick();
        List<String> after = Files.readAllLines(roundsFile, StandardCharsets.UTF_8);
        check("saving the edit keeps it historical in the file", after.get(1).startsWith("1\t1\t2005-06-19\t")
                && after.get(1).contains("\t6,5,5,") && after.get(1).endsWith("\tY"));
        check("after saving it is still shown as historical", page.differentialBox.getText().equals("historical")
                && store.handicap().entries().size() == 1);
        deleteFolder(temp);
    }

    /*
     +--------------------------------------------------------------------------+
     | checkHistoricalBox()                                                     |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The Historical check box on the round screen, on its own data folder:    |
     | the sample courses and three 90s on Sample Links:White, in a rounds      |
     | file with the standard header (no Historical column, like the sample     |
     | data).                                                                   |
     |                                                                          |
     | It checks that:                                                          |
     | - a new round starts unticked, with the box white and open, and the      |
     |   cursor goes Date, Historical, Save;                                    |
     | - Space and a click tick it, and ticking shows "historical" at once      |
     |   and counts as a change;                                                |
     | - saving it ticked adds a Historical column to the header (with a        |
     |   .bak) and writes Y, and the handicap and statistics leave it out;      |
     | - looking at it, the box is ticked, grey and switched off;               |
     | - Edit keeps it ticked, and unticking and saving clears the Y and brings |
     |   the round into the handicap and statistics;                            |
     | - on Best Round the box is grey and switched off.                        |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing (throws IOException if a temporary file fails)       |
     +--------------------------------------------------------------------------+
     */
    private static void checkHistoricalBox() throws IOException
    {
        Path temp = Files.createTempDirectory("golfanalyst-historical-box");
        Files.copy(Path.of("sample-data", DataLoader.COURSES_FILE), temp.resolve(DataLoader.COURSES_FILE));
        String fives = "5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5";
        String twos = "2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2";
        String zeros = "0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0";
        StringBuilder rows = new StringBuilder(golfanalyst.RoundWriter.STANDARD_HEADER).append('\n');
        for (int lp = 1; lp <= 3; lp = lp + 1)
        {
            rows.append(lp).append("\t1\t2026-0").append(lp).append("-01\t\t\t").append(fives).append('\t').append(twos)
                    .append('\t').append(zeros).append('\t').append(zeros).append('\n');
        }
        Path roundsFile = temp.resolve(DataLoader.ROUNDS_FILE);
        Files.writeString(roundsFile, rows.toString(), StandardCharsets.UTF_8);
        DataStore store = new DataStore(temp);
        TestShell shell = new TestShell(store);
        RoundPage page = new RoundPage(shell);
        Double indexBefore = store.handicap().currentIndex();
        int[] pars = store.teeMap().get(1).pars();

        page.showNew();
        check("historical box: a new round starts unticked, white and open", !page.historicalCheck.isSelected()
                && page.historicalCheck.isEnabled() && page.historicalCheck.getBackground().equals(Color.WHITE));
        for (int hole = 0; hole < 18; hole = hole + 1)
        {
            type(page.puttFields[hole], "2");
            type(page.scoreFields[hole], Integer.toString(pars[hole]));
        }
        page.dateField.setText("2026-04-01");
        check("historical box: the cursor goes from the date to the box, then to Save",
                page.componentAfter(page.dateField) == page.historicalCheck && page.componentAfter(page.historicalCheck) == page.saveButton);
        check("historical box: an ordinary new round shows its differential", !page.differentialBox.getText().equals("historical"));
        java.awt.event.KeyEvent space = new java.awt.event.KeyEvent(page.historicalCheck, java.awt.event.KeyEvent.KEY_PRESSED,
                System.currentTimeMillis(), 0, java.awt.event.KeyEvent.VK_SPACE, ' ');
        for (java.awt.event.KeyListener listener : page.historicalCheck.getKeyListeners())
        {
            listener.keyPressed(space);
        }
        check("historical box: Space ticks it", page.historicalCheck.isSelected());
        page.historicalCheck.toggle();
        check("historical box: a click unticks it", !page.historicalCheck.isSelected());
        page.historicalCheck.toggle();
        check("historical box: ticking shows historical at once", page.differentialBox.getText().equals("historical")
                && page.indexBox.getText().equals("-") && page.hasChanges());
        page.saveButton.doClick();
        List<String> lines = Files.readAllLines(roundsFile, StandardCharsets.UTF_8);
        check("historical box: saving adds a Historical column to the header", lines.get(0).equals(golfanalyst.RoundWriter.STANDARD_HEADER + "\tHistorical")
                && Files.exists(temp.resolve(DataLoader.ROUNDS_FILE + ".bak")));
        check("historical box: the other rows are left as they were", lines.get(1).equals(rows.toString().split("\n")[1]));
        check("historical box: saving it ticked writes Y", lines.size() == 5 && lines.get(4).startsWith("4\t1\t2026-04-01\t")
                && lines.get(4).endsWith("\tY"));
        check("historical box: the handicap leaves the ticked round out", store.handicap().entries().size() == 3
                && java.util.Objects.equals(store.handicap().currentIndex(), indexBefore));
        check("historical box: the statistics count it only while Include historical rounds is ticked", store.overall().rounds() == 4);
        store.setIncludeHistorical(false);
        check("historical box: unticked, the statistics leave it out", store.overall().rounds() == 3);
        store.setIncludeHistorical(true);
        check("historical box: looking at it, the box is ticked, grey and switched off", page.historicalCheck.isSelected()
                && !page.historicalCheck.isEnabled() && page.historicalCheck.getBackground().equals(Theme.FIELD_FACE)
                && page.differentialBox.getText().equals("historical"));

        page.editButton.doClick();
        check("historical box: Edit keeps it ticked and opens it", page.historicalCheck.isSelected()
                && page.historicalCheck.isEnabled() && page.historicalCheck.getBackground().equals(Color.WHITE) && !page.hasChanges());
        page.historicalCheck.toggle();
        check("historical box: unticking shows the differential again", !page.differentialBox.getText().equals("historical")
                && page.hasChanges());
        page.saveButton.doClick();
        lines = Files.readAllLines(roundsFile, StandardCharsets.UTF_8);
        check("historical box: unticking and saving clears the Y", lines.get(4).startsWith("4\t1\t2026-04-01\t") && lines.get(4).endsWith("\t"));
        store.setIncludeHistorical(false);
        check("historical box: the round now counts in the handicap and statistics", store.handicap().entries().size() == 4
                && store.overall().rounds() == 4 && !java.util.Objects.equals(store.handicap().currentIndex(), indexBefore));
        check("historical box: looking at it now, the box is unticked and grey", !page.historicalCheck.isSelected()
                && !page.historicalCheck.isEnabled() && page.historicalCheck.getBackground().equals(Theme.FIELD_FACE));
        page.editButton.doClick();
        check("historical box: Edit keeps an ordinary round unticked", !page.historicalCheck.isSelected());
        page.cancelButton.doClick();

        RoundPage best = new RoundPage(shell, true);
        best.showBest();
        check("historical box: on Best Round it is grey and switched off", !best.historicalCheck.isEnabled()
                && best.historicalCheck.getBackground().equals(Theme.FIELD_FACE) && !best.historicalCheck.isSelected());
        deleteFolder(temp);
    }

    /*
     +--------------------------------------------------------------------------+
     | checkIncludeHistoricalScreens()                                          |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The Home screen's "Include historical rounds" box, on its own data       |
     | folder: the sample courses, three 90s on Sample Links:White and a        |
     | historical 72 there.                                                     |
     |                                                                          |
     | It checks that:                                                          |
     | - building Home, the Stats Board and Best Round makes no calls to the    |
     |   window;                                                                |
     | - the box starts ticked, with its tool tip, and the Home figures count   |
     |   the historical round;                                                  |
     | - unticking it updates the round counts, personal best and averages at   |
     |   once and saves the choice, while the Handicap Index, Low Index and     |
     |   Rounds Used stay the same;                                             |
     | - the Stats Board and Best Round follow it each time they are shown, and |
     |   say in the footer when historical rounds are left out;                 |
     | - the Rounds screen still steps through every round;                     |
     | - the choice is still there when the program starts again.               |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing (throws IOException if a temporary file fails)       |
     +--------------------------------------------------------------------------+
     */
    private static void checkIncludeHistoricalScreens() throws IOException
    {
        Path temp = Files.createTempDirectory("golfanalyst-include-screens");
        Files.copy(Path.of("sample-data", DataLoader.COURSES_FILE), temp.resolve(DataLoader.COURSES_FILE));
        String fives = "5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5";
        String fours = "4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4";
        String twos = "2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2";
        String zeros = "0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0";
        StringBuilder rows = new StringBuilder("RoundID\tTeeID\tDate\tHoles\tPCC\tScores\tPutts\tFairways\tSand\tHistorical\n");
        for (int lp = 1; lp <= 3; lp = lp + 1)
        {
            rows.append(lp).append("\t1\t2026-0").append(lp).append("-01\t\t\t").append(fives).append('\t').append(twos)
                    .append('\t').append(zeros).append('\t').append(zeros).append("\t\n");
        }
        rows.append("4\t1\t2005-06-19\t\t\t").append(fours).append('\t').append(twos).append('\t').append(zeros).append('\t')
                .append(zeros).append("\tY\n");
        Files.writeString(temp.resolve(DataLoader.ROUNDS_FILE), rows.toString(), StandardCharsets.UTF_8);
        DataStore store = new DataStore(temp);
        TestShell shell = new TestShell(store);
        HomePage home = new HomePage(shell);
        StatsPage stats = new StatsPage(shell);
        RoundPage best = new RoundPage(shell, true);
        RoundPage viewer = new RoundPage(shell);
        check("include historical: building Home, the Stats Board and Best Round makes no calls to the window", shell.windowCalls == 0);

        home.onShow();
        String index = home.handicapIndex.getText();
        String low = home.lowIndex.getText();
        String used = home.scoresUsed.getText();
        check("include historical: the box starts ticked, with its tool tip", home.includeHistorical.isSelected()
                && home.includeHistorical.getToolTipText().equals(HomePage.INCLUDE_HISTORICAL_TIP)
                && HomePage.INCLUDE_HISTORICAL_TIP.startsWith("Count old (historical) rounds in your averages"));
        check("include historical: ticked, Home counts the historical round", home.totalRounds.getText().equals("4")
                && home.rounds18.getText().equals("4") && home.personalBest.getText().startsWith("72 /"));
        stats.onShow();
        check("include historical: ticked, the Stats Board counts it, with no footer note", stats.board.allEighteen().rounds() == 4
                && !shell.status.contains("Historical rounds are left out"));
        best.showBest();
        check("include historical: ticked, Best Round's best single round is the historical 72",
                best.title().equals("Best Single: 00004:Sample Links:White") && !shell.status.contains("left out"));

        home.includeHistorical.toggle();
        check("include historical: unticking updates the Home counts and statistics at once", home.totalRounds.getText().equals("3")
                && home.rounds18.getText().equals("3") && home.personalBest.getText().startsWith("90 /")
                && home.average18.getText().equals("90.0"));
        check("include historical: the Handicap Index, Low Index and Rounds Used stay the same", home.handicapIndex.getText().equals(index)
                && home.lowIndex.getText().equals(low) && home.scoresUsed.getText().equals(used));
        check("include historical: unticking is saved in settings.txt",
                Files.readString(temp.resolve("settings.txt")).contains("IncludeHistorical=false"));
        stats.onShow();
        check("include historical: unticked, the Stats Board leaves it out and says so", stats.board.allEighteen().rounds() == 3
                && shell.status.endsWith(StatsPage.HISTORICAL_LEFT_OUT));
        best.showBest();
        check("include historical: unticked, Best Round picks a current round and says so",
                !best.title().contains("00004") && shell.status.endsWith(StatsPage.HISTORICAL_LEFT_OUT));
        best.compositeButton.doClick();
        check("include historical: unticked, the composite says so too", best.scoreBox.getText().equals("90")
                && shell.status.endsWith(StatsPage.HISTORICAL_LEFT_OUT));
        viewer.showRoundAt(0);
        int steps = 1;
        while (viewer.nextButton.isEnabled())
        {
            viewer.nextButton.doClick();
            steps = steps + 1;
        }
        check("include historical: the Rounds screen still steps through every round", steps == 4);

        TestShell again = new TestShell(new DataStore(temp));
        HomePage homeAgain = new HomePage(again);
        homeAgain.onShow();
        check("include historical: the choice is still there when the program starts again", !homeAgain.includeHistorical.isSelected()
                && homeAgain.totalRounds.getText().equals("3"));
        homeAgain.includeHistorical.toggle();
        check("include historical: ticking it again counts the historical round again", homeAgain.totalRounds.getText().equals("4")
                && again.store().includeHistorical());
        deleteFolder(temp);
    }

    /*
     +--------------------------------------------------------------------------+
     | checkColumnsLineUp(page, which)                                          |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Checks that every row of the Stats Board list (course rows, the          |
     | nine-hole band, watermark rows and blank rows) starts at the left edge   |
     | and is the full 858 px wide, and that each course row's cells sit        |
     | exactly under their heading slots in the band. A layout that lined the   |
     | different kinds of row up differently once shifted them sideways.        |
     |                                                                          |
     | Passed in:  page  - a Stats Board that has been laid out                 |
     |             which - words for the check names, e.g. "one course"         |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    private static void checkColumnsLineUp(StatsPage page, String which)
    {
        boolean fullWidth = true;
        for (Component part : page.list.getComponents())
        {
            fullWidth = fullWidth && part.getX() == 0 && part.getWidth() == StatsPage.GRID_WIDTH;
        }
        check(which + ": every list row starts at the left edge and is 858 px wide", fullWidth);
        boolean underHeadings = !page.rowPanels.isEmpty();
        for (StatsPage.RowPanel panel : page.rowPanels)
        {
            for (golfanalyst.StatsBoard.Column column : golfanalyst.StatsBoard.Column.values())
            {
                Component slot = page.headings.get(column).getParent();
                javax.swing.JLabel cell = panel.cells[column.ordinal()];
                int slotX = javax.swing.SwingUtilities.convertPoint(slot, 0, 0, page.frame).x;
                int cellX = javax.swing.SwingUtilities.convertPoint(cell, 0, 0, page.frame).x;
                underHeadings = underHeadings && slotX == cellX && slot.getWidth() == cell.getWidth();
            }
        }
        check(which + ": each cell sits exactly under its heading", underHeadings);
    }

    /*
     +--------------------------------------------------------------------------+
     | checkBestRoundPage()                                                     |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The Best Round screen on its own small data folder (the sample courses   |
     | with three rounds on Sample Links:White: an 88 with a birdie 3 on hole   |
     | 1, a 72 of pars, and a front nine of 45, the newest).                    |
     |                                                                          |
     | It checks that:                                                          |
     | - building it makes no calls to the window;                              |
     | - it starts on the kind of the latest round, with Single chosen;         |
     | - the titles and footer say what is shown;                               |
     | - there is no Edit, Save or arrow button, only Close, and every box is   |
     |   grey;                                                                  |
     | - changing the course and tees, and Single / Composite, update the       |
     |   screen at once, with the composite's Date, Diff and Index shown as     |
     |   Composite and dashes;                                                  |
     | - with no rounds the footer says so and the controls are off.            |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing (throws IOException if a temporary file fails)       |
     +--------------------------------------------------------------------------+
     */
    private static void checkBestRoundPage() throws IOException
    {
        Path temp = Files.createTempDirectory("golfanalyst-best-test");
        Files.copy(Path.of("sample-data", DataLoader.COURSES_FILE), temp.resolve(DataLoader.COURSES_FILE));
        String twos = "2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2";
        String zeros = "0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0";
        String nineZeros = "0,0,0,0,0,0,0,0,0";
        Files.writeString(temp.resolve(DataLoader.ROUNDS_FILE), golfanalyst.RoundWriter.STANDARD_HEADER + "\n"
                + "1\t1\t2026-03-01\t\t\t3,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5\t1,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2\t" + zeros + "\t" + zeros + "\n"
                + "2\t1\t2026-04-01\t\t\t4,3,5,4,4,3,5,4,4,4,3,5,4,4,3,5,4,4\t" + twos + "\t" + zeros + "\t" + zeros + "\n"
                + "3\t1\t2026-05-01\tF\t\t5,5,5,5,5,5,5,5,5\t2,2,2,2,2,2,2,2,2\t" + nineZeros + "\t" + nineZeros + "\n",
                StandardCharsets.UTF_8);
        DataStore store = new DataStore(temp);
        TestShell shell = new TestShell(store);
        RoundPage page = new RoundPage(shell, true);
        check("building Best Round makes no calls to the window", shell.windowCalls == 0);
        check("Best Round has its own help files", page.helpKey().equals("best") && page.isBestRound());
        page.showBest();
        check("it starts on the kind of the latest round", String.valueOf(page.bestCombo.getSelectedItem()).equals("Sample Links:White (Front 9)")
                && page.bestCombo.getItemCount() == 2);
        check("it starts on Single", page.singleButton.isSelected() && !page.compositeButton.isSelected());
        check("Single's title names the round", page.title().equals("Best Single: 00003:Sample Links:White"));
        check("Single's footer gives the best and its date", shell.status.equals("Best of 1 round on Sample Links:White (Front 9): 45 on 2026-05-01."));
        check("there is no Edit, Save or arrow button, only Close", !page.editButton.isVisible() && !page.saveButton.isVisible()
                && !page.navRow.isVisible() && page.cancelButton.isVisible() && page.cancelButton.getText().equals("Close"));
        check("there is no Show box", !page.filterCombo.isVisible());
        check("every box is grey", page.scoreFields[0].getBackground().equals(Theme.FIELD_FACE)
                && page.dateField.getBackground().equals(Theme.FIELD_FACE) && page.sandChecks[0].getBackground().equals(Theme.FIELD_FACE));

        page.bestCombo.setSelectedIndex(0);
        check("changing the course and tees shows the best round there at once", page.title().equals("Best Single: 00002:Sample Links:White")
                && page.scoreBox.getText().equals("72") && page.dateField.getText().equals("2026-04-01"));
        check("the footer counts that kind's rounds", shell.status.equals("Best of 2 rounds on Sample Links:White: 72 on 2026-04-01."));
        check("a real round shows its differential", !page.differentialBox.getText().equals("-"));

        page.compositeButton.doClick();
        check("Composite's title names the course and tees, with no round number", page.title().equals("Best Composite: Sample Links:White"));
        check("Composite takes the best score on every hole", page.scoreBox.getText().equals("71")
                && page.scoreFields[0].getText().equals("3") && page.puttFields[0].getText().equals("1"));
        check("Composite's footer compares it with the best single round",
                shell.status.equals("Best score on every hole from 2 rounds on Sample Links:White: 71. The best single round is 72."));
        check("Composite shows Composite for the date and dashes for the Diff and Index", page.dateField.getText().equals("Composite")
                && page.differentialBox.getText().equals("-") && page.indexBox.getText().equals("-"));
        check("the Composite date is grey, not pink", page.dateField.getBackground().equals(Theme.FIELD_FACE));
        check("the rating is the tee's", page.ratingBox.getText().equals("70.4 / 123"));
        page.singleButton.doClick();
        check("Single again shows the real round", page.title().equals("Best Single: 00002:Sample Links:White")
                && page.dateField.getText().equals("2026-04-01"));

        Path empty = Files.createTempDirectory("golfanalyst-best-empty");
        Files.copy(temp.resolve(DataLoader.COURSES_FILE), empty.resolve(DataLoader.COURSES_FILE));
        Files.writeString(empty.resolve(DataLoader.ROUNDS_FILE), golfanalyst.RoundWriter.STANDARD_HEADER + "\n", StandardCharsets.UTF_8);
        TestShell emptyShell = new TestShell(new DataStore(empty));
        RoundPage emptyPage = new RoundPage(emptyShell, true);
        emptyPage.showBest();
        check("with no rounds the footer says so", emptyShell.status.equals(RoundPage.NO_ROUNDS_YET));
        check("with no rounds the pulldown and the buttons are off", !emptyPage.bestCombo.isEnabled()
                && !emptyPage.singleButton.isEnabled() && !emptyPage.compositeButton.isEnabled() && emptyPage.title().equals("Best Round"));
        deleteFolder(empty);
        deleteFolder(temp);
    }

    /*
     +--------------------------------------------------------------------------+
     | checkStatsPage()                                                         |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The Stats Board on a copy of the sample data: building it makes no       |
     | calls to the window; the rows match the engine's; the pinned band, its   |
     | headings (one font size, the sort arrow, the Sand tool tip) and totals;  |
     | the frame and scroll bar sizes, the bar always shown and its thumb       |
     | filling the track when every row fits; sorting by clicking a heading,    |
     | reversing, the hover, and the sort kept when the page is shown again;    |
     | highlighting a row and keeping it across a sort; the Up and Down keys    |
     | passing over the nine-hole band; Enter and a double-click opening the    |
     | rounds; the 12-row minimum with blank rows that cannot be picked, with   |
     | no rounds and with one course; the nine-hole total and band always       |
     | shown; and the faint watermark rows, shown only while a section is       |
     | empty.                                                                   |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing (throws IOException if a temporary file fails)       |
     +--------------------------------------------------------------------------+
     */
    private static void checkStatsPage() throws IOException
    {
        Path temp = Files.createTempDirectory("golfanalyst-stats-test");
        copyFolder(Path.of("sample-data"), temp);
        DataStore store = new DataStore(temp);
        TestShell shell = new TestShell(store);
        StatsPage page = new StatsPage(shell);
        check("building the Stats Board makes no calls to the window", shell.windowCalls == 0);
        check("the Stats Board's title and help files", page.title().equals("Stats Board") && page.helpKey().equals("stats"));
        page.onShow();
        page.setSize(944, 626);
        layoutAll(page);
        golfanalyst.StatsBoard.Board board = golfanalyst.StatsBoard.build(store.roundsByDate(), store.teeMap());
        check("one row on screen per row of the board", page.rowPanels.size() == board.eighteen().size() + board.nine().size()
                && !board.nine().isEmpty());
        check("the 18-hole rows come first, then the nine-hole band, then the nine-hole rows",
                page.list.getComponent(board.eighteen().size()) == page.nineBand
                        && page.nineBand.getText().equals(StatsPage.NINE_BAND)
                        && page.nineBand.getBackground().equals(StatsPage.NINE_BAND_FACE));
        check("the footer explains the board", shell.status.equals(StatsPage.FOOTER_HINT));

        check("the frame is 893 wide (858 grid + 17 scroll bar + 9 + 9 wood)", page.frame.getWidth() == 893);
        check("the band is pinned at the top of the frame, 875 x 81", page.band.getParent() == page.frame
                && page.band.getY() == 9 && page.band.getSize().equals(new java.awt.Dimension(875, 81)));
        check("the list sits under the band", page.scroll.getY() == page.band.getY() + page.band.getHeight()
                && page.scroll.getWidth() == 875);
        checkColumnsLineUp(page, "sample data");
        int rows = page.list.getComponentCount();
        check("the frame grows with the rows (" + rows + " rows)", page.scroll.getHeight() == Math.min(Math.max(rows, 12), 19) * 25);
        check("the frame stays inside the page", page.frame.getHeight() <= 626 - 28);
        check("the vertical scroll bar is always shown and is 17 px wide",
                page.scroll.getVerticalScrollBarPolicy() == javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS
                        && page.scroll.getVerticalScrollBar().getWidth() == 17);
        check("there is never a sideways scroll bar",
                page.scroll.getHorizontalScrollBarPolicy() == javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        javax.swing.JScrollBar bar = page.scroll.getVerticalScrollBar();
        check("when every row fits, the thumb fills the whole track", rows <= 19
                && page.barUI.thumbArea().height == bar.getHeight() - 2 * 17 && page.barUI.thumbArea().height > 0);
        java.awt.image.BufferedImage picture = new java.awt.image.BufferedImage(944, 626, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D canvas = picture.createGraphics();
        page.paint(canvas);
        canvas.dispose();
        check("the page, band and scroll bar paint without error", true);

        float headingSize = page.headings.get(golfanalyst.StatsBoard.Column.COURSE).getFont().getSize2D();
        boolean sameSize = true;
        for (WoodTag tag : page.headings.values())
        {
            sameSize = sameSize && tag.getFont().getSize2D() == headingSize;
        }
        check("every heading uses the same font size (" + headingSize + " pt)", sameSize && headingSize >= 9f);
        check("the headings have the agreed text", page.headings.get(golfanalyst.StatsBoard.Column.PUTTS).text().startsWith("Putts/Rnd")
                && page.headings.get(golfanalyst.StatsBoard.Column.RATING).text().startsWith("Rating\\Slope"));
        check("the sorted heading shows an up arrow, the others none", page.headings.get(golfanalyst.StatsBoard.Column.COURSE).sortArrow() == 1
                && page.headings.get(golfanalyst.StatsBoard.Column.AVERAGE).sortArrow() == 0);
        java.awt.FontMetrics headingMetrics = page.getFontMetrics(page.headings.get(golfanalyst.StatsBoard.Column.PUTTS).getFont());
        check("Putts/Rnd fits its tag at the shared size", headingMetrics.stringWidth("Putts/Rnd") <= 62 - 3 - 8);
        check("Sand % explains itself", StatsPage.SAND_TIP.equals(page.headings.get(golfanalyst.StatsBoard.Column.SAND).getToolTipText())
                && StatsPage.SAND_TIP.equals("Sand saves: how often I was in the sand and still made par or better"));
        check("a heading tag is 3 px smaller than its 25 px slot", page.headings.get(golfanalyst.StatsBoard.Column.AVERAGE).getHeight() == 22
                && page.headings.get(golfanalyst.StatsBoard.Column.AVERAGE).getWidth() == 59);
        check("the band has both total rows", page.totalCells.size() == 20
                && ((javax.swing.JLabel) page.totalCells.get(0)).getText().equals("All 18-hole rounds")
                && ((javax.swing.JLabel) page.totalCells.get(10)).getText().equals("All nine-hole rounds")
                && ((javax.swing.JLabel) page.totalCells.get(1)).getText().isEmpty()
                && page.totalCells.get(2).getBackground().equals(StatsPage.TOTAL_FACE));
        check("the 18-hole total row shows the 18-hole rounds", ((javax.swing.JLabel) page.totalCells.get(2)).getText()
                .equals(Integer.toString(board.allEighteen().rounds())));
        boolean anyGold = false;
        for (StatsPage.RowPanel panel : page.rowPanels)
        {
            for (javax.swing.JLabel cell : panel.cells)
            {
                anyGold = anyGold || (cell.getBackground().equals(StatsPage.BEST_FACE) && cell.getFont().isBold());
            }
        }
        check("gold marks best figures in bold", anyGold);

        WoodTag average = page.headings.get(golfanalyst.StatsBoard.Column.AVERAGE);
        sendMouse(average, java.awt.event.MouseEvent.MOUSE_ENTERED, 5, 5);
        check("a heading's text turns red under the mouse", average.isHover());
        sendMouse(average, java.awt.event.MouseEvent.MOUSE_EXITED, 5, 5);
        check("and back when the mouse leaves", !average.isHover());
        sendMouse(average, java.awt.event.MouseEvent.MOUSE_CLICKED, 5, 5);
        List<golfanalyst.StatsBoard.Row> wanted = new ArrayList<>(golfanalyst.StatsBoard.sort(board.eighteen(), golfanalyst.StatsBoard.Column.AVERAGE, true));
        wanted.addAll(golfanalyst.StatsBoard.sort(board.nine(), golfanalyst.StatsBoard.Column.AVERAGE, true));
        boolean inOrder = page.rowPanels.size() == wanted.size();
        for (int row = 0; row < wanted.size() && inOrder; row = row + 1)
        {
            inOrder = page.rowPanels.get(row).row.key().equals(wanted.get(row).key());
        }
        check("clicking a heading sorts each section by it, lowest first", inOrder && average.sortArrow() == 1
                && page.headings.get(golfanalyst.StatsBoard.Column.COURSE).sortArrow() == 0
                && page.sortColumn() == golfanalyst.StatsBoard.Column.AVERAGE && page.isAscending());
        check("the totals do not move when sorting", ((javax.swing.JLabel) page.totalCells.get(0)).getText().equals("All 18-hole rounds"));
        sendMouse(average, java.awt.event.MouseEvent.MOUSE_CLICKED, 5, 5);
        check("clicking it again reverses the order", !page.isAscending() && average.sortArrow() == -1
                && page.rowPanels.get(0).row.key().equals(golfanalyst.StatsBoard.sort(board.eighteen(), golfanalyst.StatsBoard.Column.AVERAGE, false).get(0).key()));
        page.onShow();
        check("the sort is kept when the page is shown again", page.sortColumn() == golfanalyst.StatsBoard.Column.AVERAGE && !page.isAscending());

        StatsPage.RowPanel third = page.rowPanels.get(2);
        String thirdKey = third.row.key();
        sendMouse(third, java.awt.event.MouseEvent.MOUSE_PRESSED, 50, 10);
        boolean cream = true;
        for (int col = 0; col < third.cells.length; col = col + 1)
        {
            cream = cream && third.cells[col].getBackground().equals(third.best[col] ? StatsPage.BEST_FACE : Theme.FIELD_FOCUS);
        }
        check("clicking a row highlights the whole line (gold cells stay gold)", thirdKey.equals(page.selectedKey())
                && third.isHighlighted() && cream);
        check("only one row is highlighted", page.rowPanels.stream().filter(StatsPage.RowPanel::isHighlighted).count() == 1);
        sendMouse(page.headings.get(golfanalyst.StatsBoard.Column.COURSE), java.awt.event.MouseEvent.MOUSE_CLICKED, 5, 5);
        check("the highlight follows its row through a sort", thirdKey.equals(page.selectedKey())
                && page.panelFor(thirdKey).isHighlighted() && page.panelFor(thirdKey) != third);

        int lastEighteen = board.eighteen().size() - 1;
        page.select(page.rowPanels.get(lastEighteen).row.key());
        page.getActionMap().get("down").actionPerformed(null);
        check("Down from the last 18-hole row passes over the nine-hole band",
                page.selectedKey().equals(page.rowPanels.get(lastEighteen + 1).row.key()));
        page.getActionMap().get("up").actionPerformed(null);
        check("Up comes back over it", page.selectedKey().equals(page.rowPanels.get(lastEighteen).row.key()));
        page.select(page.rowPanels.get(0).row.key());
        page.getActionMap().get("up").actionPerformed(null);
        check("Up on the first row stays there", page.selectedKey().equals(page.rowPanels.get(0).row.key()));
        check("the keys are bound to Up, Down and Enter",
                "down".equals(page.getInputMap(javax.swing.JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).get(javax.swing.KeyStroke.getKeyStroke("DOWN")))
                        && "open".equals(page.getInputMap(javax.swing.JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).get(javax.swing.KeyStroke.getKeyStroke("ENTER"))));
        int firstTee = page.rowPanels.get(0).row.teeId();
        page.getActionMap().get("open").actionPerformed(null);
        check("Enter opens the highlighted row's rounds", shell.lastPage.equals("browse " + firstTee));
        StatsPage.RowPanel nineRow = page.rowPanels.get(lastEighteen + 1);
        nineRow.dispatchEvent(new java.awt.event.MouseEvent(nineRow, java.awt.event.MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(),
                0, 20, 10, 2, false, java.awt.event.MouseEvent.BUTTON1));
        check("a double-click opens that row's rounds", shell.lastPage.equals("browse " + nineRow.row.teeId()));

        check("the sample data has nine-hole rows, so there is no nine-hole watermark", page.watermarks.isEmpty());

        // A copy with no rounds, then with one course
        Path emptyFolder = Files.createTempDirectory("golfanalyst-stats-empty");
        Files.copy(temp.resolve(DataLoader.COURSES_FILE), emptyFolder.resolve(DataLoader.COURSES_FILE));
        Path emptyRounds = emptyFolder.resolve(DataLoader.ROUNDS_FILE);
        Files.writeString(emptyRounds, golfanalyst.RoundWriter.STANDARD_HEADER + "\n", StandardCharsets.UTF_8);
        TestShell emptyShell = new TestShell(new DataStore(emptyFolder));
        StatsPage emptyPage = new StatsPage(emptyShell);
        emptyPage.onShow();
        emptyPage.setSize(944, 626);
        layoutAll(emptyPage);
        check("with no rounds the footer says so", emptyShell.status.equals(StatsPage.NO_ROUNDS));
        check("with no rounds the band still has both total rows, with 0 and dashes", emptyPage.totalCells.size() == 20
                && ((javax.swing.JLabel) emptyPage.totalCells.get(2)).getText().equals("0")
                && ((javax.swing.JLabel) emptyPage.totalCells.get(3)).getText().equals("-")
                && ((javax.swing.JLabel) emptyPage.totalCells.get(10)).getText().equals("All nine-hole rounds")
                && ((javax.swing.JLabel) emptyPage.totalCells.get(12)).getText().equals("0")
                && ((javax.swing.JLabel) emptyPage.totalCells.get(19)).getText().equals("-"));
        check("with no rounds the list has no course rows, only the band row and blank rows", emptyPage.rowPanels.isEmpty()
                && emptyPage.nineBand != null && emptyPage.list.getComponentCount() == StatsPage.MIN_ROWS);
        check("with no rounds the list is still 12 rows (300 px) tall", emptyPage.scroll.getHeight() == 300
                && emptyPage.frame.getHeight() == 399 && emptyPage.band.getHeight() == 81);
        check("the scroll bar has a real track and its thumb fills it", emptyPage.barUI.thumbArea().height == 300 - 2 * 17);
        check("with no rounds the first row says so faintly", emptyPage.watermarks.size() == 2
                && emptyPage.watermarks.get(0).getText().equals(StatsPage.NO_ROUNDS_MARK)
                && emptyPage.watermarks.get(0).getFont().isItalic() && emptyPage.watermarks.get(0).getFont().getSize2D() == 13f
                && emptyPage.watermarks.get(0).getForeground().equals(StatsPage.WATERMARK_TEXT)
                && emptyPage.list.getComponent(0) == emptyPage.watermarks.get(0).getParent());
        check("and the row under the nine-hole band says there are none", emptyPage.watermarks.get(1).getText().equals(StatsPage.NO_NINE_MARK)
                && emptyPage.list.getComponent(1) == emptyPage.nineBand && emptyPage.list.getComponent(2) == emptyPage.watermarks.get(1).getParent());
        javax.swing.JPanel blank = emptyPage.blankRows.get(0);
        sendMouse(blank, java.awt.event.MouseEvent.MOUSE_PRESSED, 50, 10);
        sendMouse(emptyPage.watermarks.get(0).getParent(), java.awt.event.MouseEvent.MOUSE_PRESSED, 50, 10);
        emptyPage.getActionMap().get("down").actionPerformed(null);
        check("blank rows and watermark rows cannot be picked", emptyPage.selectedKey() == null
                && blank.getMouseListeners().length == 0 && emptyPage.blankRows.size() == StatsPage.MIN_ROWS - 3);

        Files.writeString(emptyRounds, golfanalyst.RoundWriter.STANDARD_HEADER + "\n"
                + "1\t1\t2026-10-22\t\t\t5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5\t2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2\t"
                + "0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0\t0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0\n", StandardCharsets.UTF_8);
        TestShell oneShell = new TestShell(new DataStore(emptyFolder));
        StatsPage onePage = new StatsPage(oneShell);
        onePage.onShow();
        onePage.setSize(944, 626);
        layoutAll(onePage);
        checkColumnsLineUp(onePage, "one course");
        check("with one course: its row, the nine-hole band, the watermark and blank rows make 12", onePage.rowPanels.size() == 1
                && onePage.list.getComponent(1) == onePage.nineBand && onePage.list.getComponentCount() == StatsPage.MIN_ROWS
                && onePage.scroll.getHeight() == 300);
        check("with an 18-hole row the no-rounds watermark is gone, the nine-hole one stays", onePage.watermarks.size() == 1
                && onePage.watermarks.get(0).getText().equals(StatsPage.NO_NINE_MARK));
        check("with no nine-hole rounds the nine-hole total shows 0 and dashes",
                ((javax.swing.JLabel) onePage.totalCells.get(12)).getText().equals("0")
                && ((javax.swing.JLabel) onePage.totalCells.get(13)).getText().equals("-")
                && ((javax.swing.JLabel) onePage.totalCells.get(2)).getText().equals("1"));
        check("the footer is the usual hint once there is a round", oneShell.status.equals(StatsPage.FOOTER_HINT));
        deleteFolder(emptyFolder);
        deleteFolder(temp);
    }

    /*
     * focusOn(field, gained): tells a cell's listeners the cursor arrived (true) or left (false), as Swing would; a screen with no window cannot really move the cursor.
     * Passed in:  field - the cell, gained - true when the cursor arrives
     * Passed out: nothing
     */
    private static void focusOn(JTextField field, boolean gained)
    {
        java.awt.event.FocusEvent event = new java.awt.event.FocusEvent(field,
                gained ? java.awt.event.FocusEvent.FOCUS_GAINED : java.awt.event.FocusEvent.FOCUS_LOST);
        for (java.awt.event.FocusListener listener : field.getFocusListeners())
        {
            if (gained)
            {
                listener.focusGained(event);
            }
            else
            {
                listener.focusLost(event);
            }
        }
    }

    /*
     +--------------------------------------------------------------------------+
     | checkCoursePage(page, store, shell, temp)                                |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The New Course screen on a copy of the sample data: the frame sizes,     |
     | the cell look and the cursor's gold outline, finding a course already    |
     | there, the checks made as you type (Save off until all is well, the      |
     | footer naming the next thing to fix, pink only for real mistakes), the   |
     | total par, the copy arrow, the cursor order, adding tees to an existing  |
     | course (with one skipped) and saving a new nine-hole course.             |
     |                                                                          |
     | Passed in:  page  - a New Course screen built on the test shell          |
     |             store - the test data store, shell - the test shell          |
     |             temp  - the temporary data folder                            |
     | Passed out: nothing (throws IOException if the data cannot be read)      |
     +--------------------------------------------------------------------------+
     */
    private static void checkCoursePage(CoursePage page, DataStore store, TestShell shell, Path temp) throws IOException
    {
        int[] pars = {4, 3, 5, 4, 4, 3, 5, 4, 4, 4, 3, 5, 4, 4, 3, 5, 4, 4};
        int[] index = new int[18];
        int[] reversed = new int[18];
        for (int hole = 0; hole < 18; hole = hole + 1)
        {
            index[hole] = hole + 1;
            reversed[hole] = 18 - hole;
        }

        // The look: five frames of the agreed sizes, spreadsheet cells, gold outline on the cursor's cell
        check("there are five frames: captions and four tees", page.framesRow.getComponentCount() == 5);
        check("the caption frame is 136 x 526", page.framesRow.getComponent(0).getPreferredSize().equals(new java.awt.Dimension(136, 526)));
        boolean teeSizes = true;
        for (int lp = 1; lp < 5; lp = lp + 1)
        {
            teeSizes = teeSizes && page.framesRow.getComponent(lp).getPreferredSize().equals(new java.awt.Dimension(134, 526));
        }
        check("each tee frame is 134 x 526, so the rows line up", teeSizes);
        check("a cell is 56 x 21", page.parFields[0][0].getPreferredSize().equals(new java.awt.Dimension(56, 21)));
        check("a cell is bold 15 pt", page.parFields[0][0].getFont().isBold() && page.parFields[0][0].getFont().getSize2D() == 15f);
        JLabel indexHeader = page.indexHeaders[0];
        check("the handicap column is headed Hdcp/SI in bold 11 pt", indexHeader.getText().equals("Hdcp/SI")
                && indexHeader.getFont().isBold() && indexHeader.getFont().getSize2D() == 11f);
        check("the Hdcp/SI header explains itself in a tool tip",
                "Hole handicap number (stroke index): 1 is the hardest hole.".equals(indexHeader.getToolTipText()));
        int headerWidth = indexHeader.getFontMetrics(indexHeader.getFont()).stringWidth(indexHeader.getText());
        check("the Hdcp/SI header fits its 56 px cell (" + headerWidth + " px)", headerWidth <= 56 - 4);

        page.showNew();
        check("New Course names the tees Ladies, Beginner, Intermediate and Expert",
                page.teeNameFields[0].getText().equals("Ladies") && page.teeNameFields[3].getText().equals("Expert"));
        check("New Course starts with the title New Course", page.title().equals("New Course") && page.helpKey().equals("course"));
        check("the tee cells are open and white from the start", page.ratingFields[0].isEnabled()
                && page.ratingFields[0].getBackground().equals(java.awt.Color.WHITE));
        check("the total par is a grey cell", page.parTotalBoxes[0].getBackground().equals(Theme.FIELD_FACE));
        check("Save is off on an empty screen", !page.saveButton.isEnabled());
        check("the footer asks for the course and zip first", shell.status.contains("5-digit zip"));
        focusOn(page.parFields[0][0], true);
        check("the cell with the cursor has a gold outline and a cream fill",
                page.parFields[0][0].getBackground().equals(Theme.FIELD_FOCUS)
                        && page.parFields[0][0].getBorder() instanceof javax.swing.border.LineBorder
                        && ((javax.swing.border.LineBorder) page.parFields[0][0].getBorder()).getLineColor().equals(Theme.GOLD));
        focusOn(page.parFields[0][0], false);
        check("it goes back to normal when the cursor leaves", page.parFields[0][0].getBackground().equals(java.awt.Color.WHITE)
                && !(page.parFields[0][0].getBorder() instanceof javax.swing.border.LineBorder));

        // A course already in the program, typed with other capitals and spaces: its tees load for fixing
        page.courseField.setText("sample   LINKS");
        check("with a name but no zip, the footer asks for the zip", shell.status.equals("Type the course's 5-digit zip."));
        page.zipField.setText("29000");
        golfanalyst.Tee savedWhite = store.teeMap().get(1);
        check("a course already there is found whatever the capitals", page.cityField.getText().equals("Anytown")
                && page.stateField.getText().equals("SC"));
        check("the title becomes Edit Course", page.title().equals("Edit Course"));
        check("its city and state can be fixed, its nine-hole box is locked", page.cityField.isEditable()
                && page.stateField.isEditable() && !page.nineHoleCheck.isEnabled());
        check("its tees load into the frames", page.teeNameFields[0].getText().equals("White")
                && page.ratingFields[0].getText().equals("70.4") && page.slopeFields[0].getText().equals("123")
                && page.yardageFields[0].getText().equals("6050")
                && page.parFields[0][0].getText().equals(Integer.toString(savedWhite.pars()[0]))
                && page.indexFields[0][17].getText().equals(Integer.toString(savedWhite.strokeIndex()[17]))
                && page.teeNameFields[1].getText().equals("Blue"));
        check("the frames left over are empty for new tees", page.teeNameFields[2].getText().equals("Intermediate")
                && page.ratingFields[2].getText().isEmpty() && page.parFields[3][0].getText().isEmpty());
        check("the total par of a loaded tee is shown", page.parTotalBoxes[0].getText().equals("72"));
        check("with nothing changed, Save is off and the footer says what to do", !page.saveButton.isEnabled()
                && shell.status.contains("is already in the program") && shell.status.contains("Change what you need"));

        fillTee(page, 2, "blue", "69.0", "120", pars, index);
        check("a new tee named like a saved one is named at once, ignoring capitals",
                shell.status.equals("There is already a tee called blue on this course."));
        check("a real mistake turns its cell pink at once", page.teeNameFields[2].getBackground().equals(Theme.FIELD_ERROR));
        check("Save stays off while there is a mistake", !page.saveButton.isEnabled());
        page.teeNameFields[2].setText("Gold");
        check("fixing it clears the pink", !page.teeNameFields[2].getBackground().equals(Theme.FIELD_ERROR));
        check("a new tee on a saved course is ready to save", shell.status.equals("Ready to save.") && page.saveButton.isEnabled());

        page.indexFields[2][4].setText("3");
        check("a repeated handicap number is named by tee and hole",
                shell.status.equals("Gold tee, hole 5: handicap number 3 is already used on hole 3."));
        check("a repeated stroke index is pink", page.indexFields[2][4].getBackground().equals(Theme.FIELD_ERROR));
        page.indexFields[2][4].setText("5");
        page.parFields[2][6].setText("");
        check("a missing par is named by tee and hole", shell.status.equals("Gold tee, hole 7: the par is missing."));
        check("a cell not filled in yet is not pink", page.parFields[2][6].getBackground().equals(java.awt.Color.WHITE));
        check("Save is off while a par is missing", !page.saveButton.isEnabled());
        check("the total par counts only the pars typed", page.parTotalBoxes[2].getText().equals("67"));
        page.parFields[2][6].setText("5");
        page.slopeFields[2].setText("");
        check("a missing slope is named", shell.status.equals("Gold tee: the slope is missing."));
        page.slopeFields[2].setText("120");

        String whiteRating = page.ratingFields[0].getText();
        page.ratingFields[0].setText("");
        check("a saved tee cannot be emptied out: it needs its rating",
                shell.status.equals("White is already in the program, so it needs its rating.") && !page.saveButton.isEnabled());
        page.ratingFields[0].setText(whiteRating);

        page.ratingFields[1].setText("71.8");
        check("changing a tee with rounds says the handicap will be worked out again", shell.status.equals(
                "Ready to save. Blue has saved rounds, so your handicap will be worked out again for them."));
        page.frontSlopeFields[1].setText("");
        page.frontRatingFields[1].setText("35.0");
        check("a front-nine rating needs its slope", shell.status.contains("go together") && !page.saveButton.isEnabled());
        page.frontSlopeFields[1].setText("119");
        page.yardageFields[3].setText("5000");

        Path coursesFile = temp.resolve(DataLoader.COURSES_FILE);
        List<String> before = Files.readAllLines(coursesFile, StandardCharsets.UTF_8);
        String beforeText = Files.readString(coursesFile, StandardCharsets.UTF_8);
        double blueBefore = Double.NaN;
        for (Handicap.Entry entry : store.handicap().entries())
        {
            blueBefore = entry.round().teeId() == 2 && Double.isNaN(blueBefore) ? entry.differential() : blueBefore;
        }
        int nextId = store.nextTeeId();
        int teesBefore = store.dataset().tees().size();
        page.saveButton.doClick();
        check("Save updates the changed tee, adds the new one and says so", shell.status.equals("Updated 1 tee and added 1 tee for"
                + " Sample Links. The file as it was is in courses.tsv.bak.  Expert was skipped because it has no rating."));
        check("after saving, the window goes Home", shell.lastPage.equals("home"));
        List<String> after = Files.readAllLines(coursesFile, StandardCharsets.UTF_8);
        boolean othersUntouched = after.size() == before.size() + 1;
        int changedLines = 0;
        for (int lp = 0; othersUntouched && lp < before.size(); lp = lp + 1)
        {
            if (!before.get(lp).equals(after.get(lp)))
            {
                changedLines = changedLines + 1;
                othersUntouched = before.get(lp).startsWith("2\t") && after.get(lp).startsWith("2\t");
            }
        }
        check("only the edited tee's line changes; comments and other tees are untouched", othersUntouched && changedLines == 1
                && after.get(0).equals(before.get(0)) && after.get(0).startsWith("#"));
        check("the new tee is added at the end", after.get(after.size() - 1).startsWith(nextId + "\t"));
        check("the file as it was is kept in courses.tsv.bak", Files.readString(temp.resolve("courses.tsv.bak"),
                StandardCharsets.UTF_8).equals(beforeText));
        Dataset reread = DataLoader.load(temp);
        check("the course file loads back with no errors", reread.errorCount() == 0);
        check("one tee was added", reread.tees().size() == teesBefore + 1 && store.dataset().tees().size() == teesBefore + 1);
        golfanalyst.Tee blue = reread.teeMap().get(2);
        golfanalyst.Tee gold = reread.teeMap().get(nextId);
        check("the edited tee keeps its TeeID, name and yardage, with the new numbers", blue != null && blue.teeName().equals("Blue")
                && blue.rating() == 71.8 && blue.frontRating() == 35.0 && blue.frontSlope() == 119 && blue.yardage() == 6540);
        check("the new tee joins the course under its own spelling", gold != null && gold.courseName().equals("Sample Links")
                && gold.city().equals("Anytown") && gold.zip().equals("29000") && gold.teeName().equals("Gold"));
        double blueAfter = Double.NaN;
        for (Handicap.Entry entry : store.handicap().entries())
        {
            blueAfter = entry.round().teeId() == 2 && Double.isNaN(blueAfter) ? entry.differential() : blueAfter;
        }
        check("the handicap is worked out again with the new rating", !Double.isNaN(blueBefore) && blueAfter != blueBefore);

        // Changing the city changes every tee of the course
        page.showNew();
        page.courseField.setText("Sample Links");
        page.zipField.setText("29000");
        page.cityField.setText("Newtown");
        check("a new city is ready to save", shell.status.equals("Ready to save.") && page.saveButton.isEnabled());
        page.courseField.setText("Sample Link");
        check("changing the course name after an edit keeps the edit", page.cityField.getText().equals("Newtown")
                && page.teeNameFields[0].getText().equals("White") && page.title().equals("Edit Course"));
        check("and Save stays off until the name is put back", !page.saveButton.isEnabled()
                && shell.status.startsWith("The course name or zip no longer matches Sample Links."));
        page.courseField.setText("sample   links");
        check("putting the name back (any capitals) makes it ready again", page.saveButton.isEnabled()
                && page.cityField.getText().equals("Newtown"));
        page.saveButton.doClick();
        check("a new city updates every tee of the course", shell.status.startsWith("Updated 3 tees for Sample Links."));
        boolean allMoved = true;
        for (golfanalyst.Tee tee : DataLoader.load(temp).tees())
        {
            allMoved = allMoved && (!tee.courseName().equals("Sample Links") || tee.city().equals("Newtown"));
        }
        check("every tee of the course has the new city", allMoved);

        // A new nine-hole course, names typed in all lower and all upper case
        page.showNew();
        page.courseField.setText("  oak   hollow  ");
        page.zipField.setText("29100");
        check("a new course leaves the city and state open", page.cityField.isEditable() && shell.status.startsWith("A new course"));
        page.cityField.setText("PINE TOWN");
        page.stateField.setText("sc");
        check("the state is turned into capitals as it is typed", page.stateField.getText().equals("SC"));
        page.setNineHoles(true);
        check("a nine-hole course hides holes 10 to 18", !page.parFields[0][9].isVisible() && page.parFields[0][8].isVisible());
        check("a nine-hole course hides the front and back ratings", !page.frontRatingFields[0].isVisible() && !page.backSlopeFields[0].isVisible());
        check("nine holes: after the slope the cursor goes to the yards", page.componentAfter(page.slopeFields[0]) == page.yardageFields[0]);
        check("nine holes: after hole 9's par it goes to hole 1's stroke index", page.componentAfter(page.parFields[0][8]) == page.indexFields[0][0]);
        check("nine holes: after hole 9's stroke index it goes to the next tee", page.componentAfter(page.indexFields[0][8]) == page.teeNameFields[1]);
        int[] ninePars = java.util.Arrays.copyOf(pars, 9);
        int[] nineIndex = {5, 9, 1, 7, 3, 8, 2, 6, 4};
        fillTee(page, 0, "white", "34.1", "113", ninePars, nineIndex);
        page.yardageFields[0].setText("2900");
        check("nine holes: the total par is over nine holes", page.parTotalBoxes[0].getText().equals("36"));
        check("nine holes: a complete tee is ready to save", page.saveButton.isEnabled());
        int nineId = store.nextTeeId();
        page.saveButton.doClick();
        check("a new course saves with its tidied name", shell.status.equals("Added 1 tee for Oak Hollow."));
        golfanalyst.Tee nine = DataLoader.load(temp).teeMap().get(nineId);
        check("the nine-hole tee is saved with 9 holes, tidied names and its yardage", nine != null && nine.holes() == 9
                && nine.pars().length == 9 && nine.city().equals("Pine Town") && nine.state().equals("SC")
                && nine.teeName().equals("White") && nine.yardage() == 2900);

        // The copy arrow: only into empty par and SI cells of the other tees, never a rating
        page.showNew();
        page.courseField.setText("Copy Hills");
        page.zipField.setText("29200");
        page.copyButton.doClick();
        check("the copy arrow with nothing to copy says what to do", shell.status.startsWith("Type the pars and handicap numbers of Ladies first"));
        fillTee(page, 0, "Ladies", "70.2", "121", pars, index);
        page.parFields[2][3].setText("5");
        page.copyButton.doClick();
        check("the copy arrow says which tees it filled", shell.status.equals("Copied the pars and handicap numbers of Ladies into the empty cells of"
                + " Beginner, Intermediate, Expert. Check any holes that differ on the card."));
        boolean copied = true;
        for (int hole = 0; hole < 18; hole = hole + 1)
        {
            copied = copied && page.parFields[1][hole].getText().equals(Integer.toString(pars[hole]))
                    && page.indexFields[3][hole].getText().equals(Integer.toString(index[hole]));
        }
        check("the pars and stroke index are copied to the other tees", copied);
        check("a par typed before copying is kept", page.parFields[2][3].getText().equals("5"));
        check("the rating and slope are never copied", page.ratingFields[1].getText().isEmpty() && page.slopeFields[3].getText().isEmpty());
        check("copying does not move the cursor or need a click on each cell", page.parTotalBoxes[1].getText().equals("72"));
        page.copyButton.doClick();
        check("copying again with every cell filled copies nothing", shell.status.startsWith("The Par and Hdcp/SI cells of the other tees are already filled"));
        page.saveButton.doClick();
        check("tees holding only copied pars and stroke index are skipped quietly", shell.status.equals("Added 1 tee for Copy Hills."));

        // The cursor order on an 18-hole course (Save is off, so after the last tee comes Cancel)
        page.showNew();
        page.courseField.setText("Order Test");
        page.zipField.setText("29300");
        check("after the course name the cursor goes to the zip", page.componentAfter(page.courseField) == page.zipField);
        check("after the zip it goes to the city", page.componentAfter(page.zipField) == page.cityField);
        check("after the state it goes to the first tee's name", page.componentAfter(page.stateField) == page.teeNameFields[0]);
        check("after the name it goes to the rating, then the slope", page.componentAfter(page.teeNameFields[0]) == page.ratingFields[0]
                && page.componentAfter(page.ratingFields[0]) == page.slopeFields[0]);
        check("after the slope come the front and back ratings", page.componentAfter(page.slopeFields[0]) == page.frontRatingFields[0]
                && page.componentAfter(page.backSlopeFields[0]) == page.yardageFields[0]);
        check("after the yards it goes down the pars", page.componentAfter(page.yardageFields[0]) == page.parFields[0][0]
                && page.componentAfter(page.parFields[0][8]) == page.parFields[0][9]);
        check("after hole 18's par it goes to hole 1's stroke index", page.componentAfter(page.parFields[0][17]) == page.indexFields[0][0]);
        check("after hole 18's stroke index it goes to the next tee's name", page.componentAfter(page.indexFields[0][17]) == page.teeNameFields[1]);
        check("after the last tee it goes to Cancel while Save is off", page.componentAfter(page.indexFields[3][17]) == page.cancelButton);
        check("the copy arrow is not in the cursor order", page.componentAfter(page.teeNameFields[0]) != page.copyButton);

        page.cancelButton.doClick();
        check("Cancel on the course page goes Home", shell.lastPage.equals("home"));
    }

    /*
     +--------------------------------------------------------------------------+
     | checkSampleSafeguard()                                                   |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Runs the real window logic on a copy of the sample data in a folder      |
     | named sample-data: the title and footer say it is the sample data, and   |
     | saving a course or a round asks first. Cancel saves nothing; Yes makes   |
     | an empty data folder beside it and saves nothing; Yes again leaves that  |
     | folder alone; No saves into the sample data.                             |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing (throws IOException if the copy fails)               |
     +--------------------------------------------------------------------------+
     */
    private static void checkSampleSafeguard() throws IOException
    {
        Path base = Files.createTempDirectory("golfanalyst-sample-test");
        Path sample = base.resolve(DataStore.SAMPLE_FOLDER);
        Path ownData = base.resolve(DataStore.DATA_FOLDER);
        copyFolder(Path.of("sample-data"), sample);
        String coursesBefore = Files.readString(sample.resolve(DataLoader.COURSES_FILE));
        String roundsBefore = Files.readString(sample.resolve(DataLoader.ROUNDS_FILE));
        DataStore store = new DataStore(sample);
        check("a folder named sample-data is known to be the sample data", store.isSampleData());
        AppPanel panel = new AppPanel(store, Path.of("help"), "");
        check("the window title says it is the sample data", panel.windowTitle().equals(Theme.VERSION_TEXT + " - SAMPLE DATA"));
        check("the footer says it is the sample data", panel.footer().status().startsWith("This is the sample data"));
        List<String> asked = new ArrayList<>();
        int[] answer = {javax.swing.JOptionPane.CANCEL_OPTION};
        panel.setAsker(question ->
        {
            asked.add(question);
            return answer[0];
        });

        ((WoodButton) findByText(panel, "New Course")).doClick();
        CoursePage page = panel.coursePage();
        int[] ninePars = {4, 3, 5, 4, 4, 3, 5, 4, 4};
        int[] nineIndex = {5, 9, 1, 7, 3, 8, 2, 6, 4};
        page.courseField.setText("Test Nine");
        page.zipField.setText("29300");
        page.setNineHoles(true);
        fillTee(page, 0, "Red", "33.0", "110", ninePars, nineIndex);
        page.saveButton.doClick();
        check("saving on the sample data asks the agreed question",
                asked.size() == 1 && asked.get(0).equals("This is the sample data. Create your own data folder first?"));
        check("Cancel saves nothing and stays on the page", Files.readString(sample.resolve(DataLoader.COURSES_FILE)).equals(coursesBefore)
                && panel.header().title().equals("New Course") && !Files.exists(ownData));

        answer[0] = javax.swing.JOptionPane.YES_OPTION;
        page.saveButton.doClick();
        check("Yes makes a data folder beside the sample data", Files.isDirectory(ownData));
        check("the new data folder has header rows only",
                Files.readAllLines(ownData.resolve(DataLoader.COURSES_FILE)).equals(List.of(golfanalyst.CourseWriter.STANDARD_HEADER))
                        && Files.readAllLines(ownData.resolve(DataLoader.ROUNDS_FILE)).equals(List.of(golfanalyst.RoundWriter.STANDARD_HEADER)));
        check("Yes saves nothing into the sample data", Files.readString(sample.resolve(DataLoader.COURSES_FILE)).equals(coursesBefore));
        check("Yes says to close and reopen the program", panel.footer().status().contains("Close Golf Analyst and open it again"));

        Files.writeString(ownData.resolve(DataLoader.COURSES_FILE), "# mine\n", java.nio.file.StandardOpenOption.APPEND);
        String ownCourses = Files.readString(ownData.resolve(DataLoader.COURSES_FILE));
        page.saveButton.doClick();
        check("Yes again leaves an existing data folder alone", Files.readString(ownData.resolve(DataLoader.COURSES_FILE)).equals(ownCourses)
                && panel.footer().status().contains("already a data folder"));

        answer[0] = javax.swing.JOptionPane.NO_OPTION;
        page.saveButton.doClick();
        check("No saves into the sample data", panel.footer().status().equals("Added 1 tee for Test Nine.")
                && !Files.readString(sample.resolve(DataLoader.COURSES_FILE)).equals(coursesBefore));

        // A round asks the same question
        ((WoodButton) findByText(panel, "New Round")).doClick();
        RoundPage roundPage = panel.roundPage();
        for (int hole = 0; hole < 18; hole = hole + 1)
        {
            type(roundPage.puttFields[hole], "2");
            type(roundPage.scoreFields[hole], "6");
        }
        answer[0] = javax.swing.JOptionPane.CANCEL_OPTION;
        int askedBefore = asked.size();
        roundPage.saveButton.doClick();
        check("saving a round on the sample data asks too", roundPage.saveButton.isEnabled() && asked.size() == askedBefore + 1);
        check("Cancel saves no round", Files.readString(sample.resolve(DataLoader.ROUNDS_FILE)).equals(roundsBefore));
        deleteFolder(base);
    }
}

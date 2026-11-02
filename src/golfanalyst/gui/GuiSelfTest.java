package golfanalyst.gui;

import golfanalyst.DataLoader;
import golfanalyst.DataStore;
import golfanalyst.Dataset;
import golfanalyst.Exporter;
import golfanalyst.Handicap;
import golfanalyst.Round;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
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
        private boolean confirmAnswer = false;
        private String typedText = null;

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
         * analyzer(): remembers that the Analyzer was asked for.
         */
        @Override
        public void analyzer()
        {
            windowCalls = windowCalls + 1;
            lastPage = "analyzer";
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

        /*
         * confirmTwice(question, keepLabel, goLabel, word): remembers the question and gives the answer the test has set.
         */
        @Override
        public boolean confirmTwice(String title, String question, String list, String keepLabel, String goLabel, String prompt,
                String word)
        {
            windowCalls = windowCalls + 1;
            asked.add(question);
            return confirmAnswer;
        }

        /*
         * openSettings(): remembers that Settings was asked for (Settings itself is tested on the real AppPanel).
         */
        @Override
        public void openSettings()
        {
            windowCalls = windowCalls + 1;
            lastPage = "settings";
        }

        /*
         * exportData(): remembers that Export was asked for (Export itself is tested on the real AppPanel).
         */
        @Override
        public void exportData()
        {
            windowCalls = windowCalls + 1;
            lastPage = "export";
        }

        /*
         * importData(): remembers that Import was asked for (Import itself is tested on the real AppPanel).
         */
        @Override
        public void importData()
        {
            windowCalls = windowCalls + 1;
            lastPage = "import";
        }

        /*
         * confirmOnce(question, keepLabel, goLabel): remembers the question and gives the answer the test has set.
         */
        @Override
        public boolean confirmOnce(String title, String question, String keepLabel, String goLabel)
        {
            windowCalls = windowCalls + 1;
            asked.add(question);
            return confirmAnswer;
        }

        /*
         * showNotice(title, message, list): remembers the message.
         */
        @Override
        public void showNotice(String title, String message, String list)
        {
            windowCalls = windowCalls + 1;
            asked.add(title + ": " + message);
        }

        /*
         * askText(title, question, initial): remembers the question and gives the text the test has set (null for Cancel).
         */
        @Override
        public String askText(String title, String question, String initial)
        {
            windowCalls = windowCalls + 1;
            asked.add(question);
            return typedText;
        }

        /*
         * clearData(): remembers that Clear was asked for (Clear itself is tested on the real AppPanel).
         */
        @Override
        public void clearData()
        {
            windowCalls = windowCalls + 1;
            lastPage = "clear";
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
     * woodTagTexts(root): the words on every WoodTag anywhere under root, top of the tree first.
     * Passed in:  root - where to look
     * Passed out: the words, one per tag
     */
    private static List<String> woodTagTexts(Container root)
    {
        List<String> texts = new ArrayList<>();
        for (Component part : root.getComponents())
        {
            if (part instanceof WoodTag)
            {
                texts.add(((WoodTag) part).text());
            }
            if (part instanceof Container)
            {
                texts.addAll(woodTagTexts((Container) part));
            }
        }
        return texts;
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
        coursePage.useZipCodes(golfanalyst.ZipCodes.load(Path.of("no-zip-list-for-these-tests.tsv")));
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
                "round-view-info.txt", "round-view-help.txt", "round-edit-info.txt", "round-edit-help.txt", "course-info.txt", "course-help.txt",
                "analyzer-info.txt", "analyzer-help.txt"})
        {
            String helpText = Files.readString(Path.of("help", file));
            check(file + " does not call a round a score", !helpText.contains("Scores Used") && !helpText.contains("20 scores")
                    && !helpText.contains("your best 8 scores") && !helpText.contains("scores on record"));
        }
        String entryHelp = Files.readString(Path.of("help", "round-new-help.txt"));
        check("the entering help explains the F and S keys and says they are optional",
                entryHelp.contains("F checks or unchecks Fwy") && entryHelp.contains("S does the same for Sand")
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
        for (String key : new String[] {"home", "round-new", "round-view", "round-edit", "course", "analyzer"})
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
        ((WoodButton) findByText(panel, "Print")).doClick();
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
        checkBackupFailure();
        checkExtraCopyWarning();
        checkConfirmForms();
        checkDialogLayouts();
        checkImportScreen();
        checkCorrectCourseScreen();
        checkZipScreen();
        checkDeleteCourseScreen();
        checkCourseBarFits();
        checkSettingsScreen();
        checkExportScreen();
        checkAnalyzerScreen();
        checkDeleteRound();
        checkClearData();
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
        deleteFolder(temp);
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
     | every other line are untouched, a dated copy of the file as it was is    |
     | in the backups folder, and the totals are recalculated.                  |
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
        Path oldBak = temp.resolve(DataLoader.ROUNDS_FILE + ".bak");
        String oldBakText = (Files.exists(oldBak) ? Files.readString(oldBak, StandardCharsets.UTF_8) : null);
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
        List<Path> copies = golfanalyst.SelfTest.backupCopies(roundsFile);
        check("the backup copy holds rounds.tsv as it was, and an old .bak is left as it was", !copies.isEmpty()
                && Files.readString(copies.get(copies.size() - 1), StandardCharsets.UTF_8).equals(beforeText)
                && java.util.Objects.equals(oldBakText, (Files.exists(oldBak) ? Files.readString(oldBak, StandardCharsets.UTF_8) : null)));
        Round saved = null;
        for (Round round : store.roundsByDate())
        {
            saved = round.id() == original.id() ? round : saved;
        }
        check("after saving everything is reloaded with the new score", saved != null && saved.scores()[0] == newScore
                && saved.totalScore() == original.totalScore() + 1 && store.roundsByDate().size() == roundsBefore);
        check("after saving the round is shown read-only again", !page.title().startsWith("Edit") && page.editButton.isVisible());
        check("the footer says the changes were saved and where the backup is", shell.status.startsWith("Changes saved")
                && shell.status.contains("backups folder"));
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
                "round-view-info.txt", "round-view-help.txt", "round-edit-info.txt", "round-edit-help.txt", "course-info.txt", "course-help.txt",
                "analyzer-info.txt", "analyzer-help.txt"})
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
     | checkConfirmForms()                                                      |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The careful questions, without a screen: the two-button question has     |
     | its Keep and Go buttons; the typed one's OK stays off until the box      |
     | holds the word (any capitals, spaces round it ignored) and goes off      |
     | again if it changes; Cancel answers CANCELLED; Clear's choice starts     |
     | with neither ticked and Continue off, and gives the choice ticked.       |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    private static void checkConfirmForms()
    {
        ConfirmForm question = ConfirmForm.twoChoice("Delete it?", "Keep it", "Delete...");
        check("confirm: the question has Keep it and Delete... buttons", question.cancelButton.getText().equals("Keep it")
                && question.goButton.getText().equals("Delete...") && question.goButton.isEnabled() && question.message().equals("Delete it?"));
        question.cancelButton.doClick();
        check("confirm: Keep it answers no", question.answer() == ConfirmForm.CANCELLED);
        question.goButton.doClick();
        check("confirm: Delete... answers yes", question.answer() == ConfirmForm.GO);

        ConfirmForm typed = ConfirmForm.typed("To confirm, type DELETE.", "DELETE");
        check("confirm: the typed question's OK starts off", !typed.goButton.isEnabled() && typed.message().equals("To confirm, type DELETE."));
        typed.wordField.setText("DELET");
        check("confirm: OK stays off until the whole word is typed", !typed.goButton.isEnabled());
        typed.wordField.setText("delete");
        check("confirm: OK comes on for the word in any capitals", typed.goButton.isEnabled());
        typed.wordField.setText("DELETED");
        check("confirm: OK goes off again for another word", !typed.goButton.isEnabled());
        typed.wordField.setText(" Delete ");
        check("confirm: spaces round the word do not matter", typed.goButton.isEnabled());
        typed.cancelButton.doClick();
        check("confirm: Cancel answers no even with the word typed", typed.answer() == ConfirmForm.CANCELLED);

        ConfirmForm choice = ConfirmForm.clearChoice("You have 3 rounds.", AppPanel.CLEAR_ROUNDS_TEXT, AppPanel.CLEAR_EVERYTHING_TEXT);
        check("confirm: Clear starts with neither choice ticked and Continue off", !choice.roundsOnlyChoice.isSelected()
                && !choice.everythingChoice.isSelected() && !choice.goButton.isEnabled() && choice.goButton.getText().equals("Continue"));
        check("confirm: Clear's choices say what they do", ConfirmForm.choiceText(choice.roundsOnlyChoice).equals(
                "Clear my rounds only (keep my courses and my name)") && ConfirmForm.choiceText(choice.everythingChoice).equals(
                "Clear everything: my rounds, my courses, my name and my settings (like a brand-new copy of the program)"));
        choice.everythingChoice.doClick();
        check("confirm: choosing one turns Continue on", choice.goButton.isEnabled());
        choice.goButton.doClick();
        check("confirm: Continue gives the choice ticked", choice.answer() == ConfirmForm.EVERYTHING);
        choice.roundsOnlyChoice.doClick();
        choice.goButton.doClick();
        check("confirm: or rounds only", choice.answer() == ConfirmForm.ROUNDS_ONLY);
    }

    /*
     +--------------------------------------------------------------------------+
     | checkDialogLayouts()                                                     |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Lays out every careful question with the longest message it can show     |
     | (a very long course and tee name, big counts, a long golfer's name, and  |
     | the extra sentence about unsaved changes) and checks each one with       |
     | checkDialogLayout. It also checks which button is the default. The       |
     | measurements are printed so they can be read in test.bat's output.       |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    private static void checkDialogLayouts()
    {
        int[] pars = new int[golfanalyst.Tee.HOLES];
        int[] index = new int[golfanalyst.Tee.HOLES];
        int[] eights = new int[golfanalyst.Tee.HOLES];
        int[] putts = new int[golfanalyst.Tee.HOLES];
        for (int hole = 0; hole < golfanalyst.Tee.HOLES; hole = hole + 1)
        {
            pars[hole] = 4;
            index[hole] = hole + 1;
            eights[hole] = 8;
            putts[hole] = 3;
        }
        String longCourse = "The Ocean Course at Kiawah Island Golf Resort and Championship Links of the Lowcountry";
        golfanalyst.Tee longTee = new golfanalyst.Tee(9999, longCourse, "Kiawah Island", "SC", "29455",
                "Championship Blue and White Combination", golfanalyst.Tee.HOLES, 77.2, 155, 7876, pars, index, 0, 0, 0, 0);
        boolean[] none = new boolean[golfanalyst.Tee.HOLES];
        Round longRound = new Round(99999, 9999, java.time.LocalDate.of(2026, 12, 31), 0, 0, golfanalyst.Tee.HOLES, eights, putts,
                none, none.clone());
        String longName = "Bartholomew Maximilian Featherstonehaugh-Cholmondeley the Third";

        ConfirmForm deleteForm = ConfirmForm.twoChoice(RoundPage.deleteQuestion(longRound, longTee, false), "Keep it", "Delete...");
        checkDialogLayout("Delete round", deleteForm);
        check("dialog: Keep it is the Delete round default", deleteForm.defaultButton() == deleteForm.cancelButton
                && deleteForm.cancelButton.getText().equals("Keep it"));
        checkDialogLayout("Delete round, unsaved changes",
                ConfirmForm.twoChoice(RoundPage.deleteQuestion(longRound, longTee, true), "Keep it", "Delete..."));
        ConfirmForm typeDelete = ConfirmForm.typed("To confirm, type DELETE.", "DELETE");
        checkDialogLayout("type DELETE", typeDelete);
        check("dialog: OK is the typed question's default (on only for the word)", typeDelete.defaultButton() == typeDelete.goButton);
        ConfirmForm clearForm = ConfirmForm.clearChoice(AppPanel.clearCounts(99999, 9999, longName), AppPanel.CLEAR_ROUNDS_TEXT,
                AppPanel.CLEAR_EVERYTHING_TEXT);
        checkDialogLayout("Clear data", clearForm);
        check("dialog: Cancel is the Clear data default", clearForm.defaultButton() == clearForm.cancelButton);
        ConfirmForm summary = ConfirmForm.twoChoice(AppPanel.clearSummary(true, 99999, 9999, longName), "Keep my data", "Clear...");
        checkDialogLayout("Clear summary, everything", summary);
        check("dialog: Keep my data is the summary's default", summary.defaultButton() == summary.cancelButton
                && summary.cancelButton.getText().equals("Keep my data"));
        checkDialogLayout("Clear summary, rounds only",
                ConfirmForm.twoChoice(AppPanel.clearSummary(false, 99999, 9999, longName), "Keep my data", "Clear..."));
        checkDialogLayout("type CLEAR", ConfirmForm.typed("To confirm, type CLEAR.", "CLEAR"));
        ConfirmForm sample = AppPanel.sampleForm(AppPanel.SAMPLE_QUESTION);
        checkDialogLayout("sample data, saving", sample);
        check("dialog: the saving question has Yes (the default), No and Cancel", sample.defaultButton() == sample.goButton
                && sample.goButton.getText().equals("Yes") && sample.noButton.getText().equals("No")
                && sample.cancelButton.getText().equals("Cancel"));
        ConfirmForm clearSample = AppPanel.sampleForm(AppPanel.CLEAR_SAMPLE_QUESTION);
        checkDialogLayout("sample data, Clear", clearSample);
        check("dialog: Clear's sample question has Yes and No only", clearSample.cancelButton == null
                && clearSample.noButton.getText().equals("No") && AppPanel.sampleForm("Discard your changes?") == null);
        sample.noButton.doClick();
        check("dialog: the sample-data questions are titled Sample data", sample.title().equals(AppPanel.SAMPLE_TITLE)
                && clearSample.title().equals("Sample data"));

        ConfirmForm discard = ConfirmForm.yesNo(RoundPage.DISCARD_QUESTION).withTitle(AppPanel.DISCARD_TITLE);
        checkDialogLayout("Discard changes", discard);
        check("dialog: Discard changes has Yes and No, with No the default", discard.title().equals("Discard changes")
                && discard.goButton.getText().equals("Yes") && discard.defaultButton() == discard.cancelButton
                && discard.cancelButton.getText().equals("No") && discard.defaultButton().hasDefaultRing());
        StringBuilder problems = new StringBuilder();
        for (int lp = 0; lp < 40; lp = lp + 1)
        {
            problems.append("ERROR  rounds.tsv line ").append(lp + 2).append(": TeeID 99999 is not in courses.tsv\n");
        }
        ConfirmForm problemsForm = ConfirmForm.notice(AppPanel.PROBLEMS_MESSAGE, problems.toString()).withTitle(AppPanel.PROBLEMS_TITLE);
        checkDialogLayout("Data problems, 40 lines", problemsForm);
        check("dialog: Data problems shows the list read-only with OK", problemsForm.listArea != null && !problemsForm.listArea.isEditable()
                && problemsForm.defaultButton().getText().equals("OK") && problemsForm.cancelButton == null);
        ConfirmForm nameForm = ConfirmForm.textInput("Golfer's name:", longName).withTitle(HomePage.NAME_TITLE);
        checkDialogLayout("Golfer's name", nameForm);
        nameForm.textField.setText("  Pat Golfer ");
        nameForm.goButton.doClick();
        check("dialog: the name box gives what was typed, OK is the default", nameForm.answer() == ConfirmForm.GO
                && nameForm.text().equals("  Pat Golfer ") && nameForm.defaultButton() == nameForm.goButton);
        checkDialogLayout("Save failed", ConfirmForm.notice("The round could not be saved:\n" + longCourse
                + " could not be written because the disk is full.", null).withTitle(RoundPage.SAVE_FAILED_TITLE));
        ConfirmForm typedName = ConfirmForm.typed("To confirm, type the name of the course exactly: " + longCourse, longCourse);
        typedName.wordField.setText("  the OCEAN course at kiawah   island golf resort and championship links of the lowcountry ");
        check("dialog: the typed word ignores capitals and extra spaces", typedName.goButton.isEnabled());
        typedName.wordField.setText("The Ocean Course");
        check("dialog: and stays off for anything else", !typedName.goButton.isEnabled());
        check("dialog: No answers NO", sample.answer() == ConfirmForm.NO);
        java.awt.Font standard = javax.swing.UIManager.getFont("OptionPane.messageFont");
        check("dialog: the words use the system's message font, like Discard your changes?", sample.messageArea().getFont()
                .equals(ConfirmForm.messageFont()) && (standard == null || ConfirmForm.messageFont().equals(standard)));
        java.awt.Font original = standard;
        javax.swing.UIManager.put("OptionPane.messageFont", new javax.swing.plaf.FontUIResource("Segoe UI", java.awt.Font.PLAIN, 12));
        ConfirmForm windowsLook = ConfirmForm.twoChoice(RoundPage.deleteQuestion(longRound, longTee, true), "Keep it", "Delete...");
        check("dialog: with Windows' Segoe UI the Delete round words use it at 14 pt, dark brown", windowsLook.messageArea().getFont().getSize() == 14
                && windowsLook.messageArea().getFont().getName().equals("Segoe UI")
                && windowsLook.messageArea().getForeground().equals(ConfirmForm.MESSAGE_TEXT));
        checkDialogLayout("Delete round, unsaved, Segoe UI", windowsLook);
        checkDialogLayout("Clear data, Segoe UI", ConfirmForm.clearChoice(AppPanel.clearCounts(99999, 9999, longName),
                AppPanel.CLEAR_ROUNDS_TEXT, AppPanel.CLEAR_EVERYTHING_TEXT));
        javax.swing.UIManager.put("OptionPane.messageFont", original);
    }

    /*
     +--------------------------------------------------------------------------+
     | checkDialogLayout(name, form)                                            |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Gives a question its own preferred size (at least 440 px wide), lays     |
     | it out with no screen, and checks that:                                  |
     | - no part overlaps another (at every level of the panel);                |
     | - the message box is as tall as its words need at the width it got, so   |
     |   no line is cut off;                                                    |
     | - the message ends above the buttons, with at least 12 px between        |
     |   everything above and the row of buttons;                               |
     | - the buttons fit in their row and the row fits in the panel.            |
     | Then it prints the numbers.                                              |
     |                                                                          |
     | Passed in:  name - which question, for the check names                   |
     |             form - the question                                          |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    private static void checkDialogLayout(String name, ConfirmForm form)
    {
        checkPanelLayout(name, form, form.messageArea(), form.buttonRow());
    }

    /*
     * checkPanelLayout(name, form, message, row): the checks of checkDialogLayout for any dialog panel (ConfirmForm or the Import window), given its message box and its row of buttons.
     * Passed in:  name - for the check names, form - the panel, message - its message box, row - its row of buttons
     * Passed out: nothing
     */
    private static void checkPanelLayout(String name, javax.swing.JPanel form, javax.swing.JTextArea message, javax.swing.JPanel row)
    {
        java.awt.Dimension wanted = form.getPreferredSize();
        form.setSize(Math.max(wanted.width, ConfirmForm.MIN_WIDTH), wanted.height);
        layoutAll(form);
        int messageBottom = javax.swing.SwingUtilities.convertPoint(message, 0, message.getHeight(), form).y;
        int aboveBottom = 0;
        for (Component part : row.getParent().getComponents())
        {
            if (part != row)
            {
                aboveBottom = Math.max(aboveBottom, javax.swing.SwingUtilities.convertPoint(part, 0, part.getHeight(), form).y);
            }
        }
        Component band = form.getComponent(0);
        java.awt.Insets edge = form.getInsets();
        check("dialog " + name + ": the 46 px wooden band is at the top inside the 3 px border", band instanceof ConfirmForm.Band
                && band.getHeight() == ConfirmForm.BAND_HEIGHT && band.getY() == ConfirmForm.BORDER && edge.top == ConfirmForm.BORDER
                && edge.left == ConfirmForm.BORDER && band.getWidth() == form.getWidth() - 2 * ConfirmForm.BORDER
                && form.getBackground().equals(Theme.HELP_PAPER));
        boolean woodButtons = row.getComponentCount() > 0;
        for (Component button : row.getComponents())
        {
            woodButtons = woodButtons && button instanceof WoodButton && button.getHeight() >= 28 && button.getWidth() >= 96;
        }
        check("dialog " + name + ": the buttons are WoodButtons", woodButtons);
        int rowTop = javax.swing.SwingUtilities.convertPoint(row, 0, 0, form).y;
        javax.swing.JTextArea measure = new javax.swing.JTextArea(message.getText());
        measure.setFont(message.getFont());
        measure.setLineWrap(true);
        measure.setWrapStyleWord(true);
        measure.setBorder(message.getBorder());
        measure.setSize(message.getWidth(), Short.MAX_VALUE);
        int needed = measure.getPreferredSize().height;
        int lineHeight = message.getFontMetrics(message.getFont()).getHeight();
        int buttonsWidth = 0;
        for (Component button : row.getComponents())
        {
            buttonsWidth = Math.max(buttonsWidth, button.getX() + button.getWidth());
        }
        check("dialog " + name + ": no part overlaps another", noOverlaps(form));
        check("dialog " + name + ": the message box is tall enough for every line", message.getHeight() >= needed);
        check("dialog " + name + ": the message ends above the buttons", messageBottom < rowTop);
        check("dialog " + name + ": at least 12 px between the content and the buttons", rowTop - aboveBottom >= ConfirmForm.GAP);
        check("dialog " + name + ": the buttons fit, and the panel is at least 440 px wide", buttonsWidth <= row.getWidth()
                && row.getY() + row.getHeight() <= form.getHeight() - ConfirmForm.PADDING && form.getWidth() >= ConfirmForm.MIN_WIDTH);
        System.out.println(String.format("  layout  %-30s %3d x %3d px, message %3d x %3d (%d lines, needs %3d), "
                + "message bottom %3d, buttons top %3d, gap %2d", name, form.getWidth(), form.getHeight(), message.getWidth(),
                message.getHeight(), needed / lineHeight, needed, messageBottom, rowTop, rowTop - aboveBottom));
    }

    /*
     * layoutAll(container): lays out a container and everything inside it, as a window would when it is shown.
     * Passed in:  container - the top of the tree
     * Passed out: nothing
     */
    private static void layoutAll(Container container)
    {
        container.doLayout();
        for (Component child : container.getComponents())
        {
            if (child instanceof Container)
            {
                layoutAll((Container) child);
            }
        }
    }

    /*
     * noOverlaps(container): whether no two visible children overlap, inside this container and every container within it.
     * Passed in:  container - the top of the tree
     * Passed out: true if nothing overlaps
     */
    private static boolean noOverlaps(Container container)
    {
        Component[] children = container.getComponents();
        for (int lp1 = 0; lp1 < children.length; lp1 = lp1 + 1)
        {
            for (int lp2 = lp1 + 1; lp2 < children.length; lp2 = lp2 + 1)
            {
                if (children[lp1].isVisible() && children[lp2].isVisible() && children[lp1].getBounds().intersects(children[lp2].getBounds()))
                {
                    return false;
                }
            }
            if (children[lp1] instanceof Container && !noOverlaps((Container) children[lp1]))
            {
                return false;
            }
        }
        return true;
    }

    /*
     +--------------------------------------------------------------------------+
     | checkImportScreen()                                                      |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Import through the real window logic, with the Import window answered    |
     | by code: Home's Import button; Import off until a check finds something  |
     | and off again after a change; the preview; Cancel changes nothing;       |
     | Import adds the lines, stays on Home and says so in the footer; on the   |
     | sample data the sample-data question comes first and Cancel imports      |
     | nothing; and the window's layout with its longest preview.               |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing (throws IOException if a temporary file fails)       |
     +--------------------------------------------------------------------------+
     */
    private static void checkImportScreen() throws IOException
    {
        Path temp = Files.createTempDirectory("golfanalyst-import-screen");
        Path data = temp.resolve(DataStore.DATA_FOLDER);
        Files.createDirectories(data);
        Files.copy(Path.of("sample-data", DataLoader.COURSES_FILE), data.resolve(DataLoader.COURSES_FILE));
        Files.copy(Path.of("sample-data", DataLoader.ROUNDS_FILE), data.resolve(DataLoader.ROUNDS_FILE));
        DataStore store = new DataStore(data);
        golfanalyst.Tee white = store.teeMap().get(1);
        Path other = temp.resolve("other");
        Files.createDirectories(other);
        golfanalyst.Tee hills = new golfanalyst.Tee(1, "Import Hills", "Fort Mill", "SC", "29708", "Blue", white.holes(), 71.2, 128, 6400,
                white.pars(), white.strokeIndex(), 0, 0, 0, 0);
        Files.writeString(other.resolve(DataLoader.COURSES_FILE), golfanalyst.CourseWriter.STANDARD_HEADER + "\n"
                + golfanalyst.CourseWriter.formatLine(hills, golfanalyst.CourseWriter.STANDARD_HEADER.split("\t")) + "\n", StandardCharsets.UTF_8);
        int[] fives = new int[golfanalyst.Tee.HOLES];
        int[] twos = new int[golfanalyst.Tee.HOLES];
        java.util.Arrays.fill(fives, 5);
        java.util.Arrays.fill(twos, 2);
        boolean[] none = new boolean[golfanalyst.Tee.HOLES];
        Round played = new Round(1, 1, java.time.LocalDate.parse("2030-05-02"), 0, 0, golfanalyst.Tee.HOLES, fives, twos, none, none.clone());
        Files.writeString(other.resolve(DataLoader.ROUNDS_FILE), golfanalyst.RoundWriter.STANDARD_HEADER + "\n"
                + golfanalyst.RoundWriter.formatLine(played, hills, golfanalyst.RoundWriter.STANDARD_HEADER.split("\t")) + "\n", StandardCharsets.UTF_8);
        String coursesBefore = Files.readString(data.resolve(DataLoader.COURSES_FILE), StandardCharsets.UTF_8);
        int tees = store.dataset().tees().size();
        int rounds = store.roundsByDate().size();

        TestShell homeShell = new TestShell(store);
        HomePage home = new HomePage(homeShell);
        ((WoodButton) findByText(home, "Import")).doClick();
        check("import: Home's Import button asks the window to import", homeShell.lastPage.equals("import"));

        AppPanel panel = new AppPanel(store, Path.of("help"), "");
        List<String> seen = new ArrayList<>();
        panel.setImportAsker(form ->
        {
            seen.add("start: import " + (form.importButton.isEnabled() ? "on" : "off"));
            form.checkButton.doClick();
            seen.add("empty check: " + form.preview.getText());
            form.fillFromFolder(other);
            form.checkButton.doClick();
            seen.add("checked: import " + (form.importButton.isEnabled() ? "on" : "off") + ": " + form.preview.getText());
            form.historicalBox.toggle();
            seen.add("changed: import " + (form.importButton.isEnabled() ? "on" : "off") + ": " + form.preview.getText());
            form.historicalBox.toggle();
            form.checkButton.doClick();
            form.cancelButton.doClick();
            return form.answer();
        });
        panel.importData();
        check("import: Import starts off", seen.get(0).equals("start: import off"));
        check("import: Check files with nothing picked says so", seen.get(1).equals("empty check: " + ImportForm.NOTHING_PICKED));
        check("import: a check that finds something turns Import on and shows the preview", seen.get(2).equals(
                "checked: import on: 1 new tee will be added.\n1 round will be added.\n"));
        check("import: changing anything turns Import off until the next check", seen.get(3).equals("changed: import off: "
                + ImportForm.CHECK_AGAIN));
        check("import: Cancel imports nothing", Files.readString(data.resolve(DataLoader.COURSES_FILE), StandardCharsets.UTF_8)
                .equals(coursesBefore) && !Files.exists(data.resolve("backups")));

        panel.setImportAsker(form ->
        {
            form.fillFromFolder(other);
            form.checkButton.doClick();
            form.importButton.doClick();
            return form.answer();
        });
        panel.importData();
        java.util.List<Path> folders = new ArrayList<>();
        try (Stream<Path> list = Files.list(data.resolve("backups")))
        {
            list.forEach(folders::add);
        }
        check("import: Import adds the tee and the round and stays on Home", store.dataset().tees().size() == tees + 1
                && store.roundsByDate().size() == rounds + 1 && panel.currentPageName().equals(AppPanel.HOME)
                && Files.readString(data.resolve(DataLoader.COURSES_FILE), StandardCharsets.UTF_8).startsWith(coursesBefore));
        check("import: the footer says what was imported and where the copy is", folders.size() == 1
                && panel.footer().status().equals("Imported 1 tee and 1 round. 0 skipped, 0 rejected. A copy of your files as they were is in backups"
                        + java.io.File.separator + folders.get(0).getFileName() + "."));

        Path parent = Files.createTempDirectory("golfanalyst-import-sample");
        Path sample = parent.resolve(DataStore.SAMPLE_FOLDER);
        Files.createDirectories(sample);
        Files.copy(Path.of("sample-data", DataLoader.COURSES_FILE), sample.resolve(DataLoader.COURSES_FILE));
        Files.copy(Path.of("sample-data", DataLoader.ROUNDS_FILE), sample.resolve(DataLoader.ROUNDS_FILE));
        String sampleCourses = Files.readString(sample.resolve(DataLoader.COURSES_FILE), StandardCharsets.UTF_8);
        AppPanel samplePanel = new AppPanel(new DataStore(sample), Path.of("help"), "");
        List<String> sampleAsked = new ArrayList<>();
        samplePanel.setAsker(question ->
        {
            sampleAsked.add(question);
            return javax.swing.JOptionPane.CANCEL_OPTION;
        });
        samplePanel.setImportAsker(form ->
        {
            sampleAsked.add("import window");
            return ConfirmForm.CANCELLED;
        });
        samplePanel.importData();
        check("import: on the sample data the sample question comes first, and Cancel imports nothing",
                sampleAsked.equals(List.of(AppPanel.SAMPLE_QUESTION))
                        && Files.readString(sample.resolve(DataLoader.COURSES_FILE), StandardCharsets.UTF_8).equals(sampleCourses));
        deleteFolder(parent);

        ImportForm layout = new ImportForm((courses, roundsFile, historical) -> null);
        StringBuilder longPreview = new StringBuilder("812 new tees will be added (95 already here, skipped; 37 rejected).\n");
        for (int lp = 0; lp < 60; lp = lp + 1)
        {
            longPreview.append("  rounds-from-the-old-spreadsheet-of-the-club.tsv, line ").append(lp + 2)
                    .append(": no tee with that number (TeeID 99999)\n");
        }
        layout.preview.setText(longPreview.toString());
        layout.coursesPath.setText("C:\\Users\\Someone With A Long Name\\Documents\\Golf\\Old copies\\data\\courses.tsv");
        checkPanelLayout("Import, longest preview", layout, layout.introArea(), layout.buttonRow());
        check("import: Check files is the window's default, Import starts off", !layout.importButton.isEnabled());
        deleteFolder(temp);
    }

    /*
     +--------------------------------------------------------------------------+
     | checkCorrectCourseScreen()                                               |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Correct... on the New Course screen, on a copy of the sample data: it    |
     | shows only in Edit Course; the name and zip are locked until it is       |
     | pressed; another course's name and zip turn both pink and keep Save off; |
     | no change says so and keeps Save off; a new name saves into every tee    |
     | (TeeIDs and rounds unchanged, a backup copy first) and says so.          |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing (throws IOException if a temporary file fails)       |
     +--------------------------------------------------------------------------+
     */
    private static void checkCorrectCourseScreen() throws IOException
    {
        Path temp = Files.createTempDirectory("golfanalyst-correct-screen");
        Files.copy(Path.of("sample-data", DataLoader.COURSES_FILE), temp.resolve(DataLoader.COURSES_FILE));
        Files.copy(Path.of("sample-data", DataLoader.ROUNDS_FILE), temp.resolve(DataLoader.ROUNDS_FILE));
        DataStore store = new DataStore(temp);
        TestShell shell = new TestShell(store);
        CoursePage page = new CoursePage(shell);
        page.useZipCodes(golfanalyst.ZipCodes.load(temp.resolve("no-zip-list.tsv")));
        page.showNew();
        check("correct: no Correct... button on a new course", !page.correctButton.isVisible() && page.courseField.isEditable());
        page.courseField.setText("Sample Links");
        page.zipField.setText("29000");
        List<Integer> ids = store.teesForCourse("Sample Links", "29000").stream().map(golfanalyst.Tee::id).toList();
        int rounds = store.roundsByDate().size();
        check("correct: in Edit Course the name and zip are locked, with Correct... beside them", page.title().equals("Edit Course")
                && page.correctButton.isVisible() && !page.courseField.isEditable() && !page.zipField.isEditable()
                && page.courseField.getBackground().equals(Theme.FIELD_FACE) && page.correctButton.getPreferredSize().width == 90
                && page.correctButton.getPreferredSize().height == 24);
        page.correctButton.doClick();
        check("correct: Correct... unlocks them and says what it does", page.isCorrecting() && page.courseField.isEditable()
                && page.zipField.isEditable() && shell.status.equals(CoursePage.CORRECT_NOTE));
        page.courseField.setText("Parkside Nine");
        check("correct: another course's name and zip turn both pink and keep Save off", !page.saveButton.isEnabled()
                && shell.status.equals(CoursePage.SAME_COURSE) && page.courseField.getBackground().equals(Theme.FIELD_ERROR)
                && page.zipField.getBackground().equals(Theme.FIELD_ERROR) && page.title().equals("Edit Course"));
        page.courseField.setText("Sample Links");
        check("correct: no change says so and keeps Save off", !page.saveButton.isEnabled()
                && shell.status.equals(CoursePage.NOTHING_CORRECTED) && !page.zipField.getBackground().equals(Theme.FIELD_ERROR));
        page.courseField.setText("Sample Links Golf Club");
        check("correct: a new name can be saved", page.saveButton.isEnabled());
        page.saveButton.doClick();
        List<Integer> after = store.teesForCourse("Sample Links Golf Club", "29000").stream().map(golfanalyst.Tee::id).toList();
        check("correct: every tee has the new name and keeps its TeeID, every round stays", after.equals(ids)
                && store.teesForCourse("Sample Links", "29000").isEmpty() && store.roundsByDate().size() == rounds
                && store.dataset().errorCount() == 0);
        check("correct: the footer says so, and a backup copy was made", shell.status.equals("Corrected the course. Its " + ids.size()
                + " sets of tees and all their rounds are unchanged. A copy of the file as it was is in the backups folder.")
                && shell.lastPage.equals("home") && !golfanalyst.SelfTest.backupCopies(temp.resolve(DataLoader.COURSES_FILE)).isEmpty());
        page.showNew();
        check("correct: a new course is not in correcting mode", !page.isCorrecting() && !page.correctButton.isVisible());
        deleteFolder(temp);
    }

    /*
     +--------------------------------------------------------------------------+
     | checkZipScreen()                                                         |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The zip check on the New Course screen with a small zip list: the page   |
     | is built without calling the window; a known zip fills an empty city     |
     | and state but never what was typed; a different state warns, outlines    |
     | State in orange and asks at Save ("Go back" keeps the screen); a         |
     | different city only hints; an unknown zip gets a note; no list does      |
     | nothing; and the Save question's layout with long names.                 |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing (throws IOException if a temporary file fails)       |
     +--------------------------------------------------------------------------+
     */
    private static void checkZipScreen() throws IOException
    {
        Path temp = Files.createTempDirectory("golfanalyst-zip-screen");
        Files.copy(Path.of("sample-data", DataLoader.COURSES_FILE), temp.resolve(DataLoader.COURSES_FILE));
        Files.copy(Path.of("sample-data", DataLoader.ROUNDS_FILE), temp.resolve(DataLoader.ROUNDS_FILE));
        Path list = temp.resolve(golfanalyst.ZipCodes.FILE);
        Files.writeString(list, "Zip\tCity\tState\n29708\tFort Mill\tSC\n28202\tCharlotte\tNC\n"
                + "99950\tKetchikan Gateway Borough Census Area Unorganized Territory North\tAK\n", StandardCharsets.UTF_8);
        golfanalyst.ZipCodes zips = golfanalyst.ZipCodes.load(list);
        DataStore store = new DataStore(temp);
        TestShell shell = new TestShell(store);
        CoursePage page = new CoursePage(shell);
        check("zip: the New Course page is built without calling the window", shell.windowCalls == 0);
        page.useZipCodes(zips);
        int[] pars = new int[18];
        int[] index = new int[18];
        for (int hole = 0; hole < 18; hole = hole + 1)
        {
            pars[hole] = 4;
            index[hole] = hole + 1;
        }

        page.showNew();
        page.courseField.setText("Zip Test Links");
        page.zipField.setText("29708");
        check("zip: a known zip fills in an empty city and state", page.cityField.getText().equals("Fort Mill")
                && page.stateField.getText().equals("SC") && page.cityField.isEditable());

        page.showNew();
        page.courseField.setText("Zip Test Links");
        page.cityField.setText("Charlotte");
        page.stateField.setText("NC");
        page.zipField.setText("29708");
        check("zip: what was typed is never replaced", page.cityField.getText().equals("Charlotte") && page.stateField.getText().equals("NC"));
        check("zip: a different state warns in the footer", shell.status.startsWith("ZIP 29708 is in SC, but the state says NC."));
        javax.swing.border.Border border = page.stateField.getBorder();
        check("zip: and outlines State in orange", border instanceof javax.swing.border.LineBorder
                && ((javax.swing.border.LineBorder) border).getLineColor().equals(CoursePage.ZIP_WARNING));
        fillTee(page, 0, "Blue", "70.4", "125", pars, index);
        check("zip: Save stays on", page.saveButton.isEnabled());
        int tees = store.dataset().tees().size();
        int askedBefore = shell.asked.size();
        shell.confirmAnswer = false;
        page.saveButton.doClick();
        check("zip: Save asks first, and Go back saves nothing", shell.asked.size() == askedBefore + 1
                && shell.asked.get(askedBefore).equals("ZIP 29708 is Fort Mill, SC, but you entered Charlotte, NC. Save anyway?")
                && store.dataset().tees().size() == tees);

        page.stateField.setText("SC");
        check("zip: a different city only hints", shell.status.startsWith("ZIP 29708 is listed under Fort Mill. That is fine if the course "
                + "is in a nearby town.") && !(page.stateField.getBorder() instanceof javax.swing.border.LineBorder
                && ((javax.swing.border.LineBorder) page.stateField.getBorder()).getLineColor().equals(CoursePage.ZIP_WARNING)));
        int askedNow = shell.asked.size();
        page.saveButton.doClick();
        check("zip: with only the city different, Save does not ask and saves", shell.asked.size() == askedNow
                && store.dataset().tees().size() == tees + 1 && shell.lastPage.equals("home"));

        page.showNew();
        page.courseField.setText("Zip Test Hills");
        page.zipField.setText("99999");
        check("zip: an unknown zip gets a note", shell.status.startsWith("That ZIP code is not in my list (it may be new). Please check it.")
                && page.cityField.getText().isEmpty());

        CoursePage noList = new CoursePage(shell);
        noList.useZipCodes(golfanalyst.ZipCodes.load(temp.resolve("nowhere.tsv")));
        noList.showNew();
        noList.courseField.setText("Zip Test Hills");
        noList.zipField.setText("29708");
        check("zip: with no list nothing is filled and nothing is said", noList.cityField.getText().isEmpty()
                && !shell.status.contains("ZIP"));

        checkDialogLayout("Save anyway?, long names", ConfirmForm.twoChoice(zips.saveQuestion("99950",
                "Lake Wobegon Heights on the Northern Shore of Lake Superior", "MN"), "Go back", "Save anyway"));
        deleteFolder(temp);
    }

    /*
     +--------------------------------------------------------------------------+
     | FakeTools                                                                |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The Settings window's tools for the self-test: the folder "picked", the  |
     | answer to the folder question, whether folders can be opened, and a      |
     | record of what was asked, opened and said.                               |
     +--------------------------------------------------------------------------+
     */
    private static final class FakeTools implements SettingsForm.Tools
    {
        private Path nextFolder = null;
        private Path lastStart = null;
        private int answer = ConfirmForm.CANCELLED;
        private boolean canOpen = true;
        private final List<ConfirmForm> asked = new ArrayList<>();
        private final List<Path> opened = new ArrayList<>();
        private final List<String> said = new ArrayList<>();
        private int createCalls = 0;

        /*
         * chooseFolder(parent, title, start): the folder the test has set (and where the picker would have opened).
         */
        @Override
        public Path chooseFolder(Component parent, String title, Path start)
        {
            lastStart = start;
            return nextFolder;
        }

        /*
         * open(folder): remembers it; false when the test says folders cannot be opened.
         */
        @Override
        public boolean open(Path folder)
        {
            opened.add(folder);
            return canOpen;
        }

        /*
         * ask(form): remembers the question and gives the answer the test has set.
         */
        @Override
        public int ask(ConfirmForm form)
        {
            asked.add(form);
            return answer;
        }

        /*
         * createOwnDataFolder(): counts the presses.
         */
        @Override
        public void createOwnDataFolder()
        {
            createCalls = createCalls + 1;
        }

        /*
         * say(text): remembers the footer message.
         */
        @Override
        public void say(String text)
        {
            said.add(text);
        }
    }

    /*
     +--------------------------------------------------------------------------+
     | checkExportScreen()                                                      |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The Export window through the real window logic, with the folder picker  |
     | and the window's showing given by code: how it starts (Everything        |
     | ticked, the historical box following Home's, Export off until a folder   |
     | is chosen); Browse... opening at the Documents folder and then at the    |
     | last export folder; the summary line for each choice; picking a course   |
     | or typing a date ticking that choice; dates that cannot be read; Cancel  |
     | writing nothing; Export writing a new folder, the footer and             |
     | LastExportFolder; the sample data (exported, settings.txt not written);  |
     | and the layout with the longest folder path (no overlaps).               |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing (throws IOException if a temporary file fails)       |
     +--------------------------------------------------------------------------+
     */
    private static void checkExportScreen() throws IOException
    {
        Path temp = Files.createTempDirectory("golfanalyst-export-screen");
        Path data = temp.resolve(DataStore.DATA_FOLDER);
        Path into = temp.resolve("exports");
        Files.createDirectories(data);
        Files.createDirectories(into);
        Files.copy(Path.of("sample-data", DataLoader.COURSES_FILE), data.resolve(DataLoader.COURSES_FILE));
        Files.copy(Path.of("sample-data", DataLoader.ROUNDS_FILE), data.resolve(DataLoader.ROUNDS_FILE));
        DataStore store = new DataStore(data);
        store.setIncludeHistorical(false);
        String coursesBefore = Files.readString(data.resolve(DataLoader.COURSES_FILE), StandardCharsets.UTF_8);
        String roundsBefore = Files.readString(data.resolve(DataLoader.ROUNDS_FILE), StandardCharsets.UTF_8);
        int allTees = store.dataset().tees().size();
        int allRounds = store.dataset().rounds().size();

        AppPanel panel = new AppPanel(store, Path.of("help"), "");
        FakeTools tools = new FakeTools();
        panel.setSettingsTools(tools, form -> ConfirmForm.CANCELLED);
        List<ExportForm> forms = new ArrayList<>();
        java.util.concurrent.atomic.AtomicReference<java.util.function.Function<ExportForm, Integer>> script =
                new java.util.concurrent.atomic.AtomicReference<>();
        panel.setExportAsker(form ->
        {
            forms.add(form);
            return script.get().apply(form);
        });

        script.set(form ->
        {
            check("export screen: it starts on Everything, the historical box as on Home, Export off until a folder is chosen",
                    form.everythingChoice.isSelected() && !form.historicalBox.isSelected() && !form.exportButton.isEnabled()
                            && form.summaryArea().getText().equals("This will write " + allTees + " sets of tees and " + allRounds + " rounds."
                            + ExportForm.PICK_FOLDER) && form.exportButton.hasDefaultRing());
            form.folderBox.setText(temp.resolve("nowhere").toString());
            check("export screen: a folder that does not exist keeps Export off", !form.exportButton.isEnabled()
                    && form.summaryArea().getText().endsWith(ExportForm.NO_FOLDER));
            form.folderBox.setText("");
            tools.nextFolder = into;
            form.browseButton.doClick();
            check("export screen: Browse... opens at the Documents folder the first time and fills the box",
                    tools.lastStart.equals(Path.of(System.getProperty("user.home"), "Documents"))
                            && form.folderBox.getText().equals(into.toString()) && form.exportButton.isEnabled()
                            && form.summaryArea().getText().equals("This will write " + allTees + " sets of tees and " + allRounds + " rounds."));
            form.courseList.setSelectedIndex(0);
            Exporter.CourseItem course = (Exporter.CourseItem) form.courseList.getSelectedItem();
            check("export screen: picking a course ticks One course and counts its tees", form.courseChoice.isSelected()
                    && form.plan().tees().size() == course.tees() && form.summaryArea().getText().startsWith("This will write "
                    + golfanalyst.Importer.count(course.tees(), "set of tees", "sets of tees")));
            form.fromBox.setText("13/45/2026");
            check("export screen: typing a date ticks Rounds from a date; a date that cannot be read says so", form.datesChoice.isSelected()
                    && form.summaryArea().getText().equals(ExportForm.TYPE_FROM) && !form.exportButton.isEnabled() && form.plan() == null);
            form.fromBox.setText("10/07/2026");
            form.toBox.setText("soon");
            boolean badTo = form.summaryArea().getText().equals(ExportForm.BAD_TO) && !form.exportButton.isEnabled();
            form.toBox.setText("2026-10-01");
            boolean after = form.summaryArea().getText().equals(ExportForm.FROM_AFTER_TO) && !form.exportButton.isEnabled();
            form.toBox.setText("");
            check("export screen: a To date that cannot be read, or before the From date, says so", badTo && after
                    && form.exportButton.isEnabled() && form.plan().rounds().stream().allMatch(round -> !round.date().isBefore(LocalDate.of(2026, 10, 7))));
            form.historicalBox.toggle();
            form.everythingChoice.doClick();
            form.cancelButton.doClick();
            return form.answer();
        });
        panel.exportData();
        check("export screen: Cancel writes nothing", filesIn(into).isEmpty() && !Files.exists(data.resolve(golfanalyst.Settings.FILE + ".new"))
                && golfanalyst.Settings.load(data).lastExportFolder() == null);

        script.set(form ->
        {
            form.folderBox.setText(into.toString());
            form.exportButton.doClick();
            return form.answer();
        });
        panel.exportData();
        List<String> made = filesIn(into);
        Path folder = (made.size() == 1 ? into.resolve(made.get(0)) : into);
        check("export screen: Export writes one new GolfAnalyst-export_ folder with the three files", made.size() == 1
                && made.get(0).startsWith(Exporter.FOLDER_PREFIX) && filesIn(folder).equals(List.of(Exporter.README,
                DataLoader.COURSES_FILE, DataLoader.ROUNDS_FILE)));
        check("export screen: the footer says what was exported and where", panel.footer().status().equals("Exported " + allTees
                + " sets of tees and " + allRounds + " rounds to " + folder.toAbsolutePath().normalize() + "."));
        check("export screen: the folder is remembered as LastExportFolder and the data files are not changed",
                into.toAbsolutePath().normalize().equals(golfanalyst.Settings.load(data).lastExportFolder())
                && Files.readString(data.resolve(DataLoader.COURSES_FILE), StandardCharsets.UTF_8).equals(coursesBefore)
                && Files.readString(data.resolve(DataLoader.ROUNDS_FILE), StandardCharsets.UTF_8).equals(roundsBefore)
                && !Files.exists(data.resolve(golfanalyst.Backups.FOLDER)));
        script.set(form ->
        {
            tools.nextFolder = null;
            form.browseButton.doClick();
            form.cancelButton.doClick();
            return form.answer();
        });
        panel.exportData();
        check("export screen: Browse... opens at the last export folder next time", into.toAbsolutePath().normalize().equals(tools.lastStart));

        Path sample = temp.resolve(DataStore.SAMPLE_FOLDER);
        Files.createDirectories(sample);
        Files.copy(Path.of("sample-data", DataLoader.COURSES_FILE), sample.resolve(DataLoader.COURSES_FILE));
        Files.copy(Path.of("sample-data", DataLoader.ROUNDS_FILE), sample.resolve(DataLoader.ROUNDS_FILE));
        AppPanel samplePanel = new AppPanel(new DataStore(sample), Path.of("help"), "");
        samplePanel.setSettingsTools(tools, form -> ConfirmForm.CANCELLED);
        Path sampleInto = temp.resolve("sample-exports");
        Files.createDirectories(sampleInto);
        samplePanel.setExportAsker(form ->
        {
            form.folderBox.setText(sampleInto.toString());
            form.exportButton.doClick();
            return form.answer();
        });
        samplePanel.exportData();
        check("export screen: the sample data can be exported, and its settings.txt is not written", filesIn(sampleInto).size() == 1
                && !Files.exists(sample.resolve(golfanalyst.Settings.FILE)) && samplePanel.footer().status().startsWith("Exported "));

        StringBuilder longPath = new StringBuilder("C:\\Users\\Pat Golfer\\OneDrive - Golf Analyst Club\\Documents");
        while (longPath.length() < 240)
        {
            longPath.append("\\A very long folder name for golf exports");
        }
        ExportForm shortForm = new ExportForm(store.dataset(), true, null, tools);
        int shortWidth = shortForm.getPreferredSize().width;
        ExportForm layout = new ExportForm(store.dataset(), true, null, tools);
        layout.folderBox.setText(longPath.toString());
        layout.fromBox.setText("13/45/2026");
        layout.toBox.setText("soon");
        checkPanelLayout("Export, longest folder path", layout, layout.introArea(), layout.buttonRow());
        check("export screen: the longest folder path does not make the window wider", layout.getPreferredSize().width == shortWidth
                && shortWidth <= 640);
        check("export screen: nothing overlaps or runs off the window", fitsInside(layout));
    }

    /*
     +--------------------------------------------------------------------------+
     | fitsInside(root)                                                         |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Whether, in every container under root (laid out already), the visible   |
     | parts lie inside their container and no two of them overlap.             |
     |                                                                          |
     | Passed in:  root - the laid-out panel                                    |
     | Passed out: true when nothing overlaps or runs off                       |
     +--------------------------------------------------------------------------+
     */
    private static boolean fitsInside(Container root)
    {
        boolean fine = true;
        Component[] parts = root.getComponents();
        for (int lp1 = 0; lp1 < parts.length; lp1 = lp1 + 1)
        {
            java.awt.Rectangle one = parts[lp1].getBounds();
            if (!parts[lp1].isVisible() || one.isEmpty())
            {
                continue;
            }
            fine = fine && one.x >= 0 && one.y >= 0 && one.x + one.width <= root.getWidth() && one.y + one.height <= root.getHeight();
            for (int lp2 = lp1 + 1; lp2 < parts.length; lp2 = lp2 + 1)
            {
                fine = fine && (!parts[lp2].isVisible() || !one.intersects(parts[lp2].getBounds()));
            }
            if (parts[lp1] instanceof Container inside && !(parts[lp1] instanceof javax.swing.JComboBox)
                    && !(parts[lp1] instanceof javax.swing.text.JTextComponent) && !(parts[lp1] instanceof javax.swing.AbstractButton))
            {
                fine = fine && fitsInside(inside);
            }
        }
        return fine;
    }

    /*
     * filesIn(folder): the names of what is in a folder, sorted.
     */
    private static List<String> filesIn(Path folder) throws IOException
    {
        try (Stream<Path> list = Files.list(folder))
        {
            return list.map(path -> path.getFileName().toString()).sorted().toList();
        }
    }

    /*
     +--------------------------------------------------------------------------+
     | checkAnalyzerScreen()                                                    |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The Analyzer through the real window logic on rounds worked out by hand  |
     | (the same ones as the engine's checkCourseAnalysis): tee 1 of 18 holes,  |
     | round 1 with aces on 1 and 4, a birdie on 2, a double on 3 and an 8 on   |
     | 5; round 2 all bogeys; round 3 all pars, historical; round 4 a back      |
     | nine of pars, the latest. It checks that the page is built without       |
     | calling the window; Home's button; the default row (the back nine,       |
     | holes 10-18, one round, no tint); another row (18 holes with Out, In     |
     | and All in grey); the three views and their numbers; the tints match     |
     | hardestHoles and easiestHoles, the summary lists in hole-number order    |
     | (holes 7 and 8 tie with 3 and 6 but lose on hole number); Include        |
     | historical rounds on and off; the                                        |
     | Tab order; Close; no rounds; and the layout (widths, no overlaps).       |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing (throws IOException if a temporary file fails)       |
     +--------------------------------------------------------------------------+
     */
    private static void checkAnalyzerScreen() throws IOException
    {
        TestShell buildShell = new TestShell(new DataStore(Path.of("sample-data")));
        new AnalyzerPage(buildShell);
        check("analyzer: the page is built without calling the window", buildShell.windowCalls == 0);

        Path data = Files.createTempDirectory("golfanalyst-analyzer-screen").resolve(DataStore.DATA_FOLDER);
        Files.createDirectories(data);
        int[] pars = {4, 5, 4, 3, 4, 5, 4, 3, 4, 4, 4, 3, 5, 4, 3, 4, 5, 4};
        int[] ranks = new int[golfanalyst.Tee.HOLES];
        for (int lp = 0; lp < golfanalyst.Tee.HOLES; lp = lp + 1)
        {
            ranks[lp] = lp + 1;
        }
        golfanalyst.Tee tee = new golfanalyst.Tee(1, "Analysis Links", "Fort Mill", "SC", "29708", "White", golfanalyst.Tee.HOLES, 70.0, 120,
                0, pars, ranks, 35.0, 120, 35.0, 120);
        Files.writeString(data.resolve(DataLoader.COURSES_FILE), golfanalyst.CourseWriter.STANDARD_HEADER + "\n"
                + golfanalyst.CourseWriter.formatLine(tee, golfanalyst.CourseWriter.STANDARD_HEADER.split("\t")) + "\n", StandardCharsets.UTF_8);
        int[] firstScores = pars.clone();
        firstScores[0] = 1;
        firstScores[1] = 4;
        firstScores[2] = 6;
        firstScores[3] = 1;
        firstScores[4] = 8;
        int[] firstPutts = new int[golfanalyst.Tee.HOLES];
        java.util.Arrays.fill(firstPutts, 2);
        firstPutts[0] = 0;
        firstPutts[3] = 0;
        boolean[] allFairways = new boolean[golfanalyst.Tee.HOLES];
        java.util.Arrays.fill(allFairways, true);
        for (int hole = 0; hole < golfanalyst.Tee.HOLES; hole = hole + 1)
        {
            allFairways[hole] = pars[hole] > 3;
        }
        boolean[] sandOnSix = new boolean[golfanalyst.Tee.HOLES];
        sandOnSix[5] = true;
        int[] bogeys = new int[golfanalyst.Tee.HOLES];
        for (int lp = 0; lp < golfanalyst.Tee.HOLES; lp = lp + 1)
        {
            bogeys[lp] = pars[lp] + 1;
        }
        int[] twos = new int[golfanalyst.Tee.HOLES];
        java.util.Arrays.fill(twos, 2);
        List<Round> rounds = List.of(
                new Round(1, 1, LocalDate.parse("2026-05-01"), 0, 0, golfanalyst.Tee.HOLES, firstScores, firstPutts, allFairways, sandOnSix),
                new Round(2, 1, LocalDate.parse("2026-05-02"), 0, 0, golfanalyst.Tee.HOLES, bogeys, twos, new boolean[golfanalyst.Tee.HOLES],
                        sandOnSix.clone()),
                new Round(3, 1, LocalDate.parse("2026-05-03"), 0, 0, golfanalyst.Tee.HOLES, pars.clone(), twos.clone(),
                        new boolean[golfanalyst.Tee.HOLES], new boolean[golfanalyst.Tee.HOLES], true),
                new Round(4, 1, LocalDate.parse("2026-05-10"), 0, golfanalyst.Tee.NINE, golfanalyst.Tee.NINE,
                        java.util.Arrays.copyOfRange(pars, golfanalyst.Tee.NINE, golfanalyst.Tee.HOLES), java.util.Arrays.copyOf(twos, golfanalyst.Tee.NINE),
                        new boolean[golfanalyst.Tee.NINE], new boolean[golfanalyst.Tee.NINE]));
        String header = golfanalyst.RoundWriter.STANDARD_HEADER + "\tHistorical";
        StringBuilder roundLines = new StringBuilder(header).append("\n");
        for (Round round : rounds)
        {
            roundLines.append(golfanalyst.RoundWriter.formatLine(round, tee, header.split("\t"))).append("\n");
        }
        Files.writeString(data.resolve(DataLoader.ROUNDS_FILE), roundLines.toString(), StandardCharsets.UTF_8);
        DataStore store = new DataStore(data);
        check("analyzer: the hand-made rounds load", store.dataset().problems().isEmpty() && store.roundsByDate().size() == 4
                && store.roundsByDate().stream().filter(Round::historical).count() == 1);

        TestShell homeShell = new TestShell(store);
        HomePage home = new HomePage(homeShell);
        ((WoodButton) findByText(home, "Analyzer")).doClick();
        check("analyzer: Home's Analyzer button asks the window for it", homeShell.lastPage.equals("analyzer"));

        AppPanel panel = new AppPanel(store, Path.of("help"), "");
        panel.analyzer();
        AnalyzerPage page = panel.analyzerPage();
        check("analyzer: it opens on the latest round's row, Counts ticked", page.countsButton.isSelected()
                && ((golfanalyst.BestRound.Choice) page.rowCombo.getSelectedItem()).part() == golfanalyst.StatsBoard.Part.BACK
                && page.rowCombo.getItemCount() == 2 && page.captions().equals(AnalyzerPage.COUNT_ROWS)
                && panel.header().title().equals("Analyzer: Analysis Links:White (Back 9)"));
        boolean backNumbers = page.columnCount() == 10 && page.cell(0, 9).getText().equals("All");
        for (int col = 0; col < 9; col = col + 1)
        {
            backNumbers = backNumbers && page.cell(0, col).getText().equals(Integer.toString(col + 10))
                    && page.cell(1, col).getText().equals(Integer.toString(pars[col + 9]));
        }
        check("analyzer: a back nine shows holes 10-18 and All, with no Out or In", backNumbers);
        boolean noTint = true;
        for (int col = 0; col < 9; col = col + 1)
        {
            noTint = noTint && page.cell(0, col).getBackground().equals(Color.WHITE);
        }
        check("analyzer: a row with a single round is not tinted", noTint && page.hardestValue.getText().equals("-")
                && page.easiestValue.getText().equals("-") && page.roundsValue.getText().equals("1")
                && panel.footer().status().equals("1 round on Analysis Links:White (Back 9)."));
        check("analyzer: Counts shows the pars made, and a zero as a faint dash", page.cell(6, 0).getText().equals("1")
                && page.cell(3, 0).getText().equals("-") && page.cell(3, 0).getForeground().equals(AnalyzerPage.FAINT)
                && page.cell(6, 9).getText().equals("9"));

        page.rowCombo.setSelectedIndex(0);
        golfanalyst.CourseAnalysis.Analysis shown = page.analysis();
        check("analyzer: choosing the 18-hole row shows 18 holes, Out, In and All in light grey", page.columnCount() == 21
                && page.cell(0, 18).getText().equals("Out") && page.cell(0, 19).getText().equals("In") && page.cell(0, 20).getText().equals("All")
                && page.cell(5, 18).getBackground().equals(AnalyzerPage.TOTAL_FACE) && page.cell(5, 20).getBackground().equals(AnalyzerPage.TOTAL_FACE)
                && page.cell(1, 20).getText().equals("72") && page.cell(2, 20).getText().equals("3")
                && panel.header().title().equals("Analyzer: Analysis Links:White"));
        check("analyzer: the seven results counted (historical rounds included)", page.cell(3, 0).getText().equals("1")
                && page.cell(4, 3).getText().equals("1") && page.cell(5, 1).getText().equals("1") && page.cell(8, 2).getText().equals("1")
                && page.cell(9, 4).getText().equals("1") && page.cell(6, 20).getText().equals("31") && page.cell(7, 20).getText().equals("18"));
        List<Integer> hard = shown.hardestHoles(3);
        List<Integer> easy = new ArrayList<>();
        for (int hole : shown.easiestHoles(18))
        {
            if (easy.size() < 3 && !hard.contains(hole))
            {
                easy.add(hole);
            }
        }
        boolean tintsMatch = hard.equals(List.of(5, 3, 6)) && easy.equals(List.of(1, 4, 2));
        for (int col = 0; col < 18; col = col + 1)
        {
            int hole = col + 1;
            Color wanted = Color.WHITE;
            if (hard.contains(hole))
            {
                wanted = AnalyzerPage.HARD_TINT;
            }
            else if (easy.contains(hole))
            {
                wanted = AnalyzerPage.EASY_TINT;
            }
            tintsMatch = tintsMatch && page.cell(0, col).getBackground().equals(wanted) && page.cell(5, col).getBackground().equals(Color.WHITE);
        }
        check("analyzer: the three hardest holes are red and the three easiest gold, as hardestHoles and easiestHoles say", tintsMatch
                && page.hardestValue.getBackground().equals(AnalyzerPage.HARD_TINT)
                && page.easiestValue.getBackground().equals(AnalyzerPage.EASY_TINT));
        check("analyzer: the summary lists them in hole-number order (chosen 5, 3, 6 and 1, 4, 2)",
                page.hardestValue.getText().equals("3, 5, 6") && page.easiestValue.getText().equals("1, 2, 4"));
        check("analyzer: ties are still broken by hole number: 7 and 8 tie with 3 and 6 but are not chosen",
                shown.hardestHoles(5).equals(List.of(5, 3, 6, 7, 8)) && !page.hardestValue.getText().contains("7")
                && !page.hardestValue.getText().contains("8") && page.cell(0, 6).getBackground().equals(Color.WHITE)
                && page.cell(0, 7).getBackground().equals(Color.WHITE));
        check("analyzer: the summary labels say hardest and easiest", woodTagTexts(page.summaryFrame).containsAll(List.of("Hardest holes", "Easiest holes"))
                && !woodTagTexts(page.summaryFrame).contains("Best holes"));
        check("analyzer: a hole list is sorted, keeps fewer than three, and is a dash when empty",
                AnalyzerPage.holeList(List.of(11, 5)).equals("5, 11") && AnalyzerPage.holeList(List.of(9, 13, 8)).equals("8, 9, 13")
                && AnalyzerPage.holeList(List.of()).equals("-"));
        check("analyzer: the footer counts the rounds and explains the tints", panel.footer().status()
                .equals("3 rounds on Analysis Links:White. Red marks your hardest holes, where you lose the most strokes; gold marks your easiest."));
        check("analyzer: the summary", page.avgScoreValue.getText().equals("78.0") && page.bestScoreValue.getText().equals("72")
                && page.puttsValue.getText().equals("34.7") && page.fairwaysValue.getText().equals("33%")
                && page.sandValue.getText().equals("50%"));

        page.percentButton.doClick();
        check("analyzer: Percent shows whole-number percentages of the rounds on each hole", page.captions().equals(AnalyzerPage.COUNT_ROWS)
                && page.cell(3, 0).getText().equals("33") && page.cell(6, 1).getText().equals("33") && page.cell(6, 6).getText().equals("67")
                && page.cell(4, 0).getText().equals("-") && page.cell(2, 0).getText().equals("3"));
        check("analyzer: Percent in Out, In and All is of the holes played", page.cell(6, 20).getText().equals("57")
                && page.cell(6, 19).getText().equals("67") && page.cell(7, 18).getText().equals("33"));

        page.averagesButton.doClick();
        check("analyzer: Averages has its own rows", page.captions().equals(AnalyzerPage.AVERAGE_ROWS) && page.cell(10, 0).getText().equals("-"));
        check("analyzer: averages to one decimal, vs par with a sign (whole in the totals)", page.cell(3, 0).getText().equals("3.3")
                && page.cell(4, 4).getText().equals("+1.7") && page.cell(4, 0).getText().equals("-0.7") && page.cell(4, 20).getText().equals("+6")
                && page.cell(5, 4).getText().equals("4") && page.cell(6, 4).getText().equals("8") && page.cell(7, 0).getText().equals("1.3")
                && page.cell(5, 20).getText().equals("72") && page.cell(6, 20).getText().equals("90"));
        check("analyzer: fairway % none on a par 3, greens %, sand saves only with sand", page.cell(8, 3).getText().equals("-")
                && page.cell(8, 0).getText().equals("33") && page.cell(9, 2).getText().equals("33") && page.cell(10, 5).getText().equals("50")
                && page.cell(10, 6).getText().equals("-"));
        check("analyzer: in Averages the Avg vs par cells of the tinted holes are tinted too", page.cell(4, 4).getBackground()
                .equals(AnalyzerPage.HARD_TINT) && page.cell(4, 0).getBackground().equals(AnalyzerPage.EASY_TINT)
                && page.cell(4, 5).getBackground().equals(AnalyzerPage.HARD_TINT) && page.cell(4, 7).getBackground().equals(Color.WHITE)
                && page.cell(3, 4).getBackground().equals(Color.WHITE));

        java.awt.FocusTraversalPolicy policy = page.getFocusTraversalPolicy();
        check("analyzer: Tab goes pulldown, Counts, Percent, Averages, Close", policy.getFirstComponent(page) == page.rowCombo
                && policy.getComponentAfter(page, page.rowCombo) == page.countsButton
                && policy.getComponentAfter(page, page.countsButton) == page.percentButton
                && policy.getComponentAfter(page, page.percentButton) == page.averagesButton
                && policy.getComponentAfter(page, page.averagesButton) == page.closeButton
                && policy.getComponentAfter(page, page.closeButton) == page.rowCombo);

        page.setSize(945, 626);
        layoutAll(page);
        java.awt.Rectangle captions = javax.swing.SwingUtilities.convertRectangle(page.captionFrame.getParent(), page.captionFrame.getBounds(), page);
        java.awt.Rectangle gridBox = javax.swing.SwingUtilities.convertRectangle(page.gridFrame.getParent(), page.gridFrame.getBounds(), page);
        java.awt.Rectangle summary = javax.swing.SwingUtilities.convertRectangle(page.summaryFrame.getParent(), page.summaryFrame.getBounds(), page);
        check("analyzer: the frames are 130 and 778 px wide, 10 px apart, inside the 921 px within the page's edges",
                captions.width == 130 && gridBox.width == 21 * AnalyzerPage.CELL_WIDTH + 22 && gridBox.x - captions.x - captions.width == 10
                && captions.x >= AnalyzerPage.PAGE_EDGE && gridBox.x + gridBox.width <= 945 - AnalyzerPage.PAGE_EDGE
                && Math.abs(captions.x - (945 - gridBox.x - gridBox.width)) <= 1);
        check("analyzer: rows of 24 px line up with the captions, and the summary sits under both frames", gridBox.height == captions.height
                && gridBox.height == 11 * AnalyzerPage.CELL_HEIGHT + 22 && summary.y > gridBox.y + gridBox.height
                && summary.x == captions.x && summary.width == gridBox.x + gridBox.width - captions.x && summary.y + summary.height <= 626
                && page.closeButton.getWidth() == 112 && page.closeButton.getHeight() == 32);
        check("analyzer: no part of the page overlaps another", noOverlaps(page.gridPanel()) && noOverlaps(page.summaryFrame)
                && noOverlaps(page.captionFrame) && !captions.intersects(gridBox));
        page.closeButton.doClick();
        check("analyzer: Close goes Home", panel.header().title().equals("Golf Analyst Stats Home"));

        store.setIncludeHistorical(false);
        panel.analyzer();
        page.rowCombo.setSelectedIndex(0);
        check("analyzer: without historical rounds they are left out, and the footer says so", page.analysis().rounds().size() == 2
                && page.cell(2, 20).getText().equals("2") && page.cell(6, 0).getText().equals("-")
                && panel.footer().status().equals("2 rounds on Analysis Links:White." + AnalyzerPage.TINT_NOTE + StatsPage.HISTORICAL_LEFT_OUT)
                && page.hardestValue.getText().equals("3, 5, 6") && page.easiestValue.getText().equals("1, 2, 4"));

        Path empty = Files.createTempDirectory("golfanalyst-analyzer-empty").resolve(DataStore.DATA_FOLDER);
        Files.createDirectories(empty);
        Files.copy(data.resolve(DataLoader.COURSES_FILE), empty.resolve(DataLoader.COURSES_FILE));
        Files.writeString(empty.resolve(DataLoader.ROUNDS_FILE), header + "\n", StandardCharsets.UTF_8);
        AppPanel emptyPanel = new AppPanel(new DataStore(empty), Path.of("help"), "");
        emptyPanel.analyzer();
        AnalyzerPage emptyPage = emptyPanel.analyzerPage();
        check("analyzer: no rounds: the message, the controls off and a grid of dashes", emptyPanel.footer().status()
                .equals(AnalyzerPage.NO_ROUNDS_YET) && !emptyPage.rowCombo.isEnabled() && !emptyPage.countsButton.isEnabled()
                && !emptyPage.percentButton.isEnabled() && !emptyPage.averagesButton.isEnabled() && emptyPage.columnCount() == 21
                && emptyPage.cell(1, 0).getText().equals("-") && emptyPage.cell(9, 20).getText().equals("-")
                && emptyPage.roundsValue.getText().equals("-") && emptyPanel.header().title().equals("Analyzer")
                && emptyPage.getFocusTraversalPolicy().getFirstComponent(emptyPage) == emptyPage.closeButton);
    }

    /*
     +--------------------------------------------------------------------------+
     | checkSettingsScreen()                                                    |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Home's new buttons and the Settings window, through the real window      |
     | logic with the window's tools given by code: Home's twelve buttons in    |
     | place and the page built without calling the window; the window's        |
     | layout; Cancel saves nothing; OK saves the name, Backups to keep and the |
     | extra folder; Change... with an empty folder (copy), a folder with data  |
     | (use as it is) and a folder with a data file (only "use as it is"        |
     | offered); the open buttons; and the sample data.                         |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing (throws IOException if a temporary file fails)       |
     +--------------------------------------------------------------------------+
     */
    private static void checkSettingsScreen() throws IOException
    {
        Path temp = Files.createTempDirectory("golfanalyst-settings-screen");
        Path data = temp.resolve(DataStore.DATA_FOLDER);
        Path program = temp.resolve("program");
        Files.createDirectories(data);
        Files.createDirectories(program);
        Files.copy(Path.of("sample-data", DataLoader.COURSES_FILE), data.resolve(DataLoader.COURSES_FILE));
        Files.copy(Path.of("sample-data", DataLoader.ROUNDS_FILE), data.resolve(DataLoader.ROUNDS_FILE));
        DataStore store = new DataStore(data);

        TestShell homeShell = new TestShell(store);
        HomePage home = new HomePage(homeShell);
        check("settings: Home is built without calling the window", homeShell.windowCalls == 0);
        List<String> labels = new ArrayList<>();
        for (Component part : ((Container) findByText(home, "New Course").getParent()).getComponents())
        {
            labels.add(((WoodButton) part).getText());
        }
        check("settings: Home has six buttons in each column, Export between Import and Clear, Settings after Print", labels.equals(
                List.of("New Course", "Rounds", "New Round", "Stats Board", "Edit Round", "Analyzer", "Import", "Best Round", "Export", "Print",
                        "Clear", "Settings")));
        ((WoodButton) findByText(home, "Settings")).doClick();
        String settingsAsked = homeShell.lastPage;
        ((WoodButton) findByText(home, "Export")).doClick();
        check("settings: the Settings and Export buttons ask the window", settingsAsked.equals("settings") && homeShell.lastPage.equals("export"));

        AppPanel panel = new AppPanel(store, Path.of("help"), "");
        panel.setProgramDir(program);
        FakeTools tools = new FakeTools();
        List<SettingsForm> forms = new ArrayList<>();
        java.util.concurrent.atomic.AtomicReference<java.util.function.Function<SettingsForm, Integer>> script =
                new java.util.concurrent.atomic.AtomicReference<>();
        panel.setSettingsTools(tools, form ->
        {
            forms.add(form);
            return script.get().apply(form);
        });

        script.set(form ->
        {
            form.nameBox.setText("Cancelled Name");
            form.keepChoice.setSelectedItem("50");
            form.cancelButton.doClick();
            return form.answer();
        });
        panel.openSettings();
        check("settings: Cancel saves nothing", !Files.exists(data.resolve(golfanalyst.Settings.FILE)) && store.golferName().isEmpty());
        SettingsForm first = forms.get(0);
        checkPanelLayout("Settings", first, first.introArea(), first.buttonRow());
        check("settings: the window starts with the settings as they are", first.backupsToKeep().equals("50")
                && SettingsForm.NOT_SET.equals(first.copyFolderBox.getText()) && first.okButton.hasDefaultRing()
                && first.dataFolderBox.getText().equals(data.toAbsolutePath().normalize().toString()) && !first.isSample());

        Path usb = Files.createTempDirectory("golfanalyst-settings-usb");
        script.set(form ->
        {
            form.nameBox.setText("  Pat Golfer ");
            form.keepChoice.setSelectedItem("All");
            tools.nextFolder = usb;
            form.browseCopy.doClick();
            form.okButton.doClick();
            return form.answer();
        });
        panel.openSettings();
        golfanalyst.Settings saved = golfanalyst.Settings.load(data);
        check("settings: OK saves the name, Backups to keep and the extra folder", store.golferName().equals("Pat Golfer")
                && saved.backupsToKeep() == golfanalyst.Settings.KEEP_ALL && usb.equals(saved.backupCopyFolder())
                && panel.footer().status().equals("Settings saved.") && panel.homePage().golferName.getText().equals("Pat Golfer"));
        script.set(form ->
        {
            form.clearCopy.doClick();
            form.okButton.doClick();
            return form.answer();
        });
        panel.openSettings();
        check("settings: Clear empties the extra folder", golfanalyst.Settings.load(data).backupCopyFolder() == null
                && forms.get(forms.size() - 1).copyFolderBox.getText().equals(SettingsForm.NOT_SET));

        Path emptyFolder = Files.createTempDirectory("golfanalyst-settings-empty");
        tools.nextFolder = emptyFolder;
        tools.answer = SettingsForm.CHOICE_COPY;
        script.set(form ->
        {
            form.changeData.doClick();
            form.cancelButton.doClick();
            return form.answer();
        });
        panel.openSettings();
        ConfirmForm question = tools.asked.get(tools.asked.size() - 1);
        check("settings: Change... says the folder is empty and offers both ways", question.message().contains("This folder is empty.")
                && question.goButton.getText().equals(SettingsForm.USE_AS_IT_IS) && question.noButton.getText().equals(SettingsForm.COPY_DATA)
                && question.defaultButton() == question.cancelButton && question.title().equals("Data folder"));
        checkDialogLayout("Data folder question", question);
        check("settings: Change... then Cancel changes nothing", !Files.exists(program.resolve(golfanalyst.DataLocation.FILE))
                && !Files.exists(emptyFolder.resolve(DataLoader.ROUNDS_FILE)));
        script.set(form ->
        {
            form.changeData.doClick();
            form.okButton.doClick();
            return form.answer();
        });
        String roundsBefore = Files.readString(data.resolve(DataLoader.ROUNDS_FILE), StandardCharsets.UTF_8);
        panel.openSettings();
        check("settings: copy and use: the data is copied, the location file names the folder, the old folder is kept",
                emptyFolder.toAbsolutePath().normalize().equals(golfanalyst.DataLocation.read(program))
                        && Files.readString(emptyFolder.resolve(DataLoader.ROUNDS_FILE), StandardCharsets.UTF_8).equals(roundsBefore)
                        && Files.readString(data.resolve(DataLoader.ROUNDS_FILE), StandardCharsets.UTF_8).equals(roundsBefore)
                        && panel.footer().status().contains("the next time it is opened")
                        && forms.get(forms.size() - 1).noteArea.getText().contains("the next time it is opened"));

        Path withData = Files.createTempDirectory("golfanalyst-settings-data");
        Files.copy(Path.of("sample-data", DataLoader.COURSES_FILE), withData.resolve(DataLoader.COURSES_FILE));
        Files.copy(Path.of("sample-data", DataLoader.ROUNDS_FILE), withData.resolve(DataLoader.ROUNDS_FILE));
        String theirs = Files.readString(withData.resolve(DataLoader.ROUNDS_FILE), StandardCharsets.UTF_8);
        tools.nextFolder = withData;
        tools.answer = SettingsForm.CHOICE_USE;
        panel.openSettings();
        question = tools.asked.get(tools.asked.size() - 1);
        check("settings: a folder with data is counted and only 'use as it is' is offered", question.message().contains("This folder has ")
                && question.message().contains(" rounds and 4 courses.") && question.noButton == null
                && withData.toAbsolutePath().normalize().equals(golfanalyst.DataLocation.read(program))
                && Files.readString(withData.resolve(DataLoader.ROUNDS_FILE), StandardCharsets.UTF_8).equals(theirs));
        Path oneFile = Files.createTempDirectory("golfanalyst-settings-one");
        Files.writeString(oneFile.resolve(golfanalyst.Settings.FILE), "Golfer=Someone\n", StandardCharsets.UTF_8);
        tools.nextFolder = oneFile;
        panel.openSettings();
        question = tools.asked.get(tools.asked.size() - 1);
        check("settings: a folder with only a settings file offers only 'use as it is' and nothing is written there",
                question.noButton == null && question.message().contains("already there")
                        && Files.readString(oneFile.resolve(golfanalyst.Settings.FILE), StandardCharsets.UTF_8).equals("Golfer=Someone\n")
                        && !Files.exists(oneFile.resolve(DataLoader.ROUNDS_FILE)));

        tools.canOpen = false;
        script.set(form ->
        {
            form.openData.doClick();
            form.openBackups.doClick();
            form.cancelButton.doClick();
            return form.answer();
        });
        panel.openSettings();
        check("settings: the open buttons say so when this computer cannot open folders", tools.said.size() >= 1
                && tools.said.get(tools.said.size() - 2).startsWith("This computer cannot open folders from Golf Analyst. The data folder is ")
                && (tools.said.get(tools.said.size() - 1).equals("There are no backups yet.")
                        || tools.said.get(tools.said.size() - 1).contains("The backups folder is ")));

        Path parent = Files.createTempDirectory("golfanalyst-settings-sample");
        Path sample = parent.resolve(DataStore.SAMPLE_FOLDER);
        Files.createDirectories(sample);
        Files.copy(Path.of("sample-data", DataLoader.COURSES_FILE), sample.resolve(DataLoader.COURSES_FILE));
        Files.copy(Path.of("sample-data", DataLoader.ROUNDS_FILE), sample.resolve(DataLoader.ROUNDS_FILE));
        AppPanel samplePanel = new AppPanel(new DataStore(sample), Path.of("help"), "");
        samplePanel.setProgramDir(program);
        List<SettingsForm> sampleForms = new ArrayList<>();
        samplePanel.setSettingsTools(tools, form ->
        {
            sampleForms.add(form);
            form.nameBox.setText("Sample Golfer");
            form.createOwn.doClick();
            form.okButton.doClick();
            return form.answer();
        });
        int createBefore = tools.createCalls;
        samplePanel.openSettings();
        SettingsForm sampleForm = sampleForms.get(0);
        golfanalyst.Settings sampleSaved = golfanalyst.Settings.load(sample);
        check("settings: on the sample data only the name is saved, and the folder row says it is the sample data",
                sampleForm.isSample() && !sampleForm.keepChoice.isEnabled() && !sampleForm.browseCopy.isEnabled()
                        && sampleForm.dataFolderBox.getText().equals(SettingsForm.SHOWING_SAMPLE)
                        && sampleSaved.golfer().equals("Sample Golfer") && sampleSaved.get(golfanalyst.Settings.BACKUPS_TO_KEEP).isEmpty());
        check("settings: Create my own data folder uses the sample-data safeguard", tools.createCalls == createBefore + 1
                && sampleForm.noteArea.getText().startsWith(SettingsForm.SAMPLE_NOTE));
        checkPanelLayout("Settings, sample data", sampleForm, sampleForm.introArea(), sampleForm.buttonRow());
        deleteFolder(parent);
        for (Path folder : List.of(usb, emptyFolder, withData, oneFile, temp))
        {
            deleteFolder(folder);
        }
    }

    /*
     +--------------------------------------------------------------------------+
     | checkCourseBarFits()                                                     |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Lays the New Course screen out at the window's real width (916 px) in    |
     | Edit Course, where the course bar is fullest (Correct... is showing),    |
     | and checks that every part of the bar sits on its one row and inside     |
     | it, so nothing is cut off; that Delete course is in the bottom bar,      |
     | wide enough for its words, as tall as Save and Cancel with the same      |
     | font, on the same line, at least 40 px left of Save; and that it         |
     | overlaps neither Save, Cancel nor the yellow version text (the peek)     |
     | with its 6 px margin.                                                    |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing (throws IOException if a temporary file fails)       |
     +--------------------------------------------------------------------------+
     */
    private static void checkCourseBarFits() throws IOException
    {
        Path temp = Files.createTempDirectory("golfanalyst-course-bar");
        Files.copy(Path.of("sample-data", DataLoader.COURSES_FILE), temp.resolve(DataLoader.COURSES_FILE));
        Files.copy(Path.of("sample-data", DataLoader.ROUNDS_FILE), temp.resolve(DataLoader.ROUNDS_FILE));
        AppPanel panel = new AppPanel(new DataStore(temp), Path.of("help"), "");
        panel.setSize(916, 760);
        panel.newCourse();
        CoursePage page = panel.coursePage();
        page.useZipCodes(golfanalyst.ZipCodes.load(temp.resolve("no-zip-list.tsv")));
        page.courseField.setText("Edgewater");
        page.zipField.setText("29720");
        layoutAll(panel);
        Container bar = page.courseField.getParent();
        boolean oneRow = page.correctButton.isVisible();
        StringBuilder rows = new StringBuilder();
        for (Component part : bar.getComponents())
        {
            if (part.isVisible())
            {
                oneRow = oneRow && part.getY() < 10 && part.getY() + part.getHeight() <= bar.getHeight()
                        && part.getX() + part.getWidth() <= bar.getWidth();
                rows.append(part.getX()).append(',').append(part.getY()).append(' ');
            }
        }
        check("course bar: in Edit Course every part fits on its one row (" + bar.getWidth() + " px wide)", oneRow);
        java.awt.Rectangle delete = javax.swing.SwingUtilities.convertRectangle(page.deleteCourseButton.getParent(),
                page.deleteCourseButton.getBounds(), page);
        java.awt.Rectangle save = javax.swing.SwingUtilities.convertRectangle(page.saveButton.getParent(), page.saveButton.getBounds(), page);
        java.awt.Rectangle peek = page.versionLabelBounds();
        peek.grow(6, 6);
        java.awt.Rectangle cancel = javax.swing.SwingUtilities.convertRectangle(page.cancelButton.getParent(), page.cancelButton.getBounds(),
                page);
        int wordsWidth = page.deleteCourseButton.getFontMetrics(page.deleteCourseButton.getFont()).stringWidth("Delete course");
        check("course bar: Delete course is 130 x 32 with Save's font, wide enough for its words (" + wordsWidth + " px)",
                page.deleteCourseButton.isVisible() && delete.width == 130 && delete.height == save.height && save.height == 32
                        && page.deleteCourseButton.getFont().equals(page.saveButton.getFont()) && wordsWidth <= delete.width - 16);
        check("course bar: Delete course is on Save's line, at least 40 px to its left (" + (save.x - delete.x - delete.width) + " px)",
                delete.y == save.y && save.x - (delete.x + delete.width) >= 40);
        check("course bar: Delete course overlaps neither Save, Cancel nor the yellow version text", !delete.intersects(save)
                && !delete.intersects(cancel) && !delete.intersects(peek));
        System.out.println("  layout  bottom bar: Delete course " + delete + ", Save " + save + ", Cancel " + cancel + ", version label "
                + page.versionLabelBounds());
        deleteFolder(temp);
    }

    /*
     +--------------------------------------------------------------------------+
     | checkDeleteCourseScreen()                                                |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Delete course on the New Course screen: the button only in Edit Course;  |
     | the question (with typed changes mentioned); Keep it deletes nothing;    |
     | Delete removes the course and goes Home with the footer; screens that    |
     | remembered the course (a Stats Board highlight, Best Round, the round    |
     | viewer) simply no longer show it; the sample-data question comes first   |
     | on the sample data; the typed name; and the dialog's layout with the     |
     | longest question and list.                                               |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing (throws IOException if a temporary file fails)       |
     +--------------------------------------------------------------------------+
     */
    private static void checkDeleteCourseScreen() throws IOException
    {
        Path temp = Files.createTempDirectory("golfanalyst-delete-course-screen");
        golfanalyst.SelfTest.deleteCourseFolder(temp);
        DataStore store = new DataStore(temp);
        TestShell shell = new TestShell(store);
        CoursePage page = new CoursePage(shell);
        page.useZipCodes(golfanalyst.ZipCodes.load(temp.resolve("no-zip-list.tsv")));
        StatsPage stats = new StatsPage(shell);
        RoundPage best = new RoundPage(shell, true);
        RoundPage viewer = new RoundPage(shell);
        page.showNew();
        check("delete course: no Delete course button on a new course", !page.deleteCourseButton.isVisible());
        page.courseField.setText("Edgewater");
        page.zipField.setText("29720");
        check("delete course: the button shows in Edit Course, 130 x 32", page.deleteCourseButton.isVisible()
                && page.deleteCourseButton.getPreferredSize().width == 130 && page.deleteCourseButton.getPreferredSize().height == 32);
        String coursesBefore = Files.readString(temp.resolve(DataLoader.COURSES_FILE), StandardCharsets.UTF_8);
        int rounds = (int) store.roundsByDate().stream().filter(round -> round.teeId() >= 5 && round.teeId() <= 8).count();
        shell.confirmAnswer = false;
        page.deleteCourseButton.doClick();
        String asked = shell.asked.get(shell.asked.size() - 1);
        check("delete course: the question names the course and counts what goes", asked.startsWith("Delete Edgewater (29720)? This removes "
                + "the course, its 4 sets of tees and all " + rounds + " rounds played on them (1 of them historical).")
                && !asked.endsWith(golfanalyst.CourseDeleter.UNSAVED_IGNORED));
        check("delete course: Keep it deletes nothing", Files.readString(temp.resolve(DataLoader.COURSES_FILE), StandardCharsets.UTF_8)
                .equals(coursesBefore) && page.title().equals("Edit Course"));
        page.yardageFields[0].setText("9999");
        page.deleteCourseButton.doClick();
        check("delete course: typed changes are mentioned", shell.asked.get(shell.asked.size() - 1)
                .endsWith(golfanalyst.CourseDeleter.UNSAVED_IGNORED));

        stats.onShow();
        String edgewaterKey = null;
        for (StatsPage.RowPanel row : stats.rowPanels)
        {
            edgewaterKey = (row.row.label().startsWith("Edgewater:") ? row.row.key() : edgewaterKey);
        }
        stats.select(edgewaterKey);
        best.showBest();
        boolean bestWasEdgewater = best.title().contains("Edgewater");
        viewer.showLatest();
        boolean viewerWasEdgewater = viewer.title().contains("Edgewater");

        shell.confirmAnswer = true;
        page.deleteCourseButton.doClick();
        check("delete course: Delete removes the course and goes Home, saying so", store.teesForCourse("Edgewater", "29720").isEmpty()
                && shell.lastPage.equals("home") && shell.status.startsWith("Deleted Edgewater: 4 sets of tees and " + rounds
                        + " rounds. A copy of your files as they were is in backups"));
        stats.onShow();
        check("delete course: the Stats Board lets go of a highlight on the deleted course", edgewaterKey != null
                && stats.selectedKey() == null && stats.rowPanels.stream().noneMatch(row -> row.row.label().startsWith("Edgewater:")));
        best.showBest();
        viewer.showLatest();
        check("delete course: Best Round and the round viewer show what is left", bestWasEdgewater && viewerWasEdgewater
                && !best.title().contains("Edgewater:") && !viewer.title().contains("Edgewater:"));

        Path parent = Files.createTempDirectory("golfanalyst-delete-course-sample");
        Path sample = parent.resolve(DataStore.SAMPLE_FOLDER);
        Files.createDirectories(sample);
        Files.copy(Path.of("sample-data", DataLoader.COURSES_FILE), sample.resolve(DataLoader.COURSES_FILE));
        Files.copy(Path.of("sample-data", DataLoader.ROUNDS_FILE), sample.resolve(DataLoader.ROUNDS_FILE));
        String sampleCourses = Files.readString(sample.resolve(DataLoader.COURSES_FILE), StandardCharsets.UTF_8);
        AppPanel panel = new AppPanel(new DataStore(sample), Path.of("help"), "");
        panel.coursePage().useZipCodes(golfanalyst.ZipCodes.load(sample.resolve("no-zip-list.tsv")));
        List<String> sampleAsked = new ArrayList<>();
        panel.setAsker(question ->
        {
            sampleAsked.add(question);
            return javax.swing.JOptionPane.CANCEL_OPTION;
        });
        panel.setConfirmer((form, title) ->
        {
            sampleAsked.add("confirm");
            return ConfirmForm.CANCELLED;
        });
        panel.newCourse();
        panel.coursePage().courseField.setText("Edgewater");
        panel.coursePage().zipField.setText("29720");
        panel.coursePage().deleteCourseButton.doClick();
        check("delete course: on the sample data the sample question comes first, and Cancel deletes nothing",
                sampleAsked.equals(List.of(AppPanel.SAMPLE_QUESTION))
                        && Files.readString(sample.resolve(DataLoader.COURSES_FILE), StandardCharsets.UTF_8).equals(sampleCourses));

        List<String> steps = new ArrayList<>();
        panel.setConfirmer((form, title) ->
        {
            steps.add(title);
            if (form.wordField != null)
            {
                form.wordField.setText("edgewater north");
                steps.add("wrong name: OK " + (form.goButton.isEnabled() ? "on" : "off"));
                form.wordField.setText("  EDGEWATER ");
                steps.add("right name: OK " + (form.goButton.isEnabled() ? "on" : "off"));
                form.cancelButton.doClick();
                return form.answer();
            }
            form.goButton.doClick();
            return form.answer();
        });
        boolean deleted = panel.confirmTwice(CoursePage.DELETE_COURSE_TITLE, "Delete Edgewater (29720)?", "a list", "Keep it", "Delete...",
                "To confirm, type the name of the course exactly: Edgewater", "Edgewater");
        check("delete course: OK stays off until the name is typed (capitals and spaces ignored), and Cancel keeps it", !deleted
                && steps.equals(List.of("Delete course", "Confirm", "wrong name: OK off", "right name: OK on")));
        deleteFolder(parent);

        StringBuilder list = new StringBuilder();
        for (int lp = 0; lp < 60; lp = lp + 1)
        {
            list.append("2026-10-").append(10 + lp % 20).append("   Championship Blue and White Combination   104 (9 holes)   (historical)\n");
        }
        golfanalyst.CourseDeleter.Summary longest = new golfanalyst.CourseDeleter.Summary(
                "The Ocean Course at Kiawah Island Golf Resort and Championship Links of the Lowcountry", "29455", 12, 999, 345,
                list.toString().trim());
        ConfirmForm question = ConfirmForm.twoChoiceWithList(longest.question(true), longest.list(), "Keep it", "Delete...")
                .withTitle(CoursePage.DELETE_COURSE_TITLE);
        checkDialogLayout("Delete course, longest", question);
        check("delete course: Keep it is the default", question.defaultButton() == question.cancelButton
                && question.title().equals("Delete course") && question.listArea != null && !question.listArea.isEditable());
        checkDialogLayout("Delete course, type the name", ConfirmForm.typed(longest.prompt(), longest.courseName())
                .withTitle(AppPanel.CONFIRM_TITLE));
        deleteFolder(temp);
    }

    /*
     +--------------------------------------------------------------------------+
     | checkDeleteRound()                                                       |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Delete round on its own copy of the sample data.                         |
     |                                                                          |
     | It checks that:                                                          |
     | - the button shows only in Edit mode (not New Round, view or Best Round) |
     |   and is 112 x 24;                                                       |
     | - the question names the saved round and mentions typed changes;         |
     | - answering no deletes nothing;                                          |
     | - answering yes removes only that line, shows the round before it        |
     |   read-only and says where the backup is;                                |
     | - deleting the first round shows the next one, and deleting the last     |
     |   one left goes Home;                                                    |
     | - the real window's two questions only say yes when DELETE is typed.     |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing (throws IOException if a temporary file fails)       |
     +--------------------------------------------------------------------------+
     */
    private static void checkDeleteRound() throws IOException
    {
        Path temp = Files.createTempDirectory("golfanalyst-delete-round");
        Files.copy(Path.of("sample-data", DataLoader.COURSES_FILE), temp.resolve(DataLoader.COURSES_FILE));
        Files.copy(Path.of("sample-data", DataLoader.ROUNDS_FILE), temp.resolve(DataLoader.ROUNDS_FILE));
        Path roundsFile = temp.resolve(DataLoader.ROUNDS_FILE);
        DataStore store = new DataStore(temp);
        TestShell shell = new TestShell(store);
        RoundPage page = new RoundPage(shell);
        RoundPage best = new RoundPage(shell, true);
        page.showNew();
        check("delete: no Delete round button while entering a new round", !page.deleteButton.isVisible());
        page.showLatest();
        check("delete: no Delete round button while looking at a round", !page.deleteButton.isVisible());
        best.showBest();
        check("delete: no Delete round button on Best Round", !best.deleteButton.isVisible());
        page.editButton.doClick();
        check("delete: the Delete round button shows in Edit mode, 112 x 24", page.deleteButton.isVisible()
                && page.deleteButton.getText().equals("Delete round") && page.deleteButton.getPreferredSize().width == 112
                && page.deleteButton.getPreferredSize().height == 24);

        int count = store.roundsByDate().size();
        Round last = store.roundsByDate().get(count - 1);
        Round before = store.roundsByDate().get(count - 2);
        golfanalyst.Tee tee = store.teeMap().get(last.teeId());
        String before0 = Files.readString(roundsFile, StandardCharsets.UTF_8);
        type(page.scoreFields[0], Integer.toString(last.scores()[0] + 1));
        shell.confirmAnswer = false;
        page.deleteButton.doClick();
        String asked = shell.asked.get(shell.asked.size() - 1);
        check("delete: the question names the saved round", asked.startsWith(String.format("Delete round %05d (%s:%s, %s, score %d)? "
                + "It will be removed from your rounds, and your handicap and statistics will be worked out again. A copy of your file is "
                + "kept first.", last.id(), tee.courseName(), tee.teeName(), last.date(), last.totalScore())));
        check("delete: the question says typed changes are ignored", asked.endsWith(RoundPage.UNSAVED_IGNORED));
        check("delete: answering no deletes nothing", Files.readString(roundsFile, StandardCharsets.UTF_8).equals(before0)
                && store.roundsByDate().size() == count && page.title().startsWith("Edit"));

        shell.confirmAnswer = true;
        page.deleteButton.doClick();
        List<String> beforeLines = List.of(before0.split("\n", -1));
        List<String> afterLines = List.of(Files.readString(roundsFile, StandardCharsets.UTF_8).split("\n", -1));
        List<String> expected = new ArrayList<>(beforeLines);
        expected.removeIf(line -> line.startsWith(last.id() + "\t"));
        check("delete: only that round's line is removed", afterLines.equals(expected) && expected.size() == beforeLines.size() - 1);
        check("delete: the round before it is shown read-only", page.title().startsWith(String.format("%05d:", before.id()))
                && page.editButton.isVisible() && !page.deleteButton.isVisible() && store.roundsByDate().size() == count - 1);
        List<Path> copies = golfanalyst.SelfTest.backupCopies(roundsFile);
        check("delete: the footer says it was deleted and where the copy is", shell.status.equals(String.format(
                "Round %05d deleted. A copy of your rounds file as it was is in backups%s%s.", last.id(), java.io.File.separator,
                copies.get(copies.size() - 1).getParent().getFileName())));
        check("delete: the copy holds the file as it was", Files.readString(copies.get(copies.size() - 1), StandardCharsets.UTF_8).equals(before0));

        Round first = store.roundsByDate().get(0);
        Round second = store.roundsByDate().get(1);
        page.showRoundAt(0);
        page.editButton.doClick();
        page.deleteButton.doClick();
        check("delete: deleting the first round shows the next one", page.title().startsWith(String.format("%05d:", second.id()))
                && store.roundsByDate().stream().noneMatch(round -> round.id() == first.id()));

        Path single = Files.createTempDirectory("golfanalyst-delete-single");
        Files.copy(Path.of("sample-data", DataLoader.COURSES_FILE), single.resolve(DataLoader.COURSES_FILE));
        List<String> sampleLines = Files.readAllLines(Path.of("sample-data", DataLoader.ROUNDS_FILE), StandardCharsets.UTF_8);
        String headerLine = sampleLines.stream().filter(line -> line.startsWith("RoundID")).findFirst().orElse("");
        String oneRound = sampleLines.stream().filter(line -> line.matches("\\d+\t.*")).findFirst().orElse("");
        Files.writeString(single.resolve(DataLoader.ROUNDS_FILE), headerLine + "\n" + oneRound + "\n", StandardCharsets.UTF_8);
        TestShell singleShell = new TestShell(new DataStore(single));
        singleShell.confirmAnswer = true;
        RoundPage singlePage = new RoundPage(singleShell);
        singlePage.showLatest();
        singlePage.editButton.doClick();
        singlePage.deleteButton.doClick();
        check("delete: deleting the only round goes Home", singleShell.lastPage.equals("home")
                && singleShell.store().roundsByDate().isEmpty() && !singlePage.hasChanges()
                && Files.readString(single.resolve(DataLoader.ROUNDS_FILE), StandardCharsets.UTF_8).equals(headerLine + "\n"));
        deleteFolder(single);

        AppPanel panel = new AppPanel(store, Path.of("help"), "");
        List<String> titles = new ArrayList<>();
        panel.setConfirmer((form, title) ->
        {
            titles.add(form.message());
            if (form.wordField != null)
            {
                form.wordField.setText("Delete");
            }
            form.goButton.doClick();
            return form.answer();
        });
        check("delete: the window asks the question, then for DELETE, and says yes when it is typed",
                panel.confirmTwice("Delete round", "Delete it?", null, "Keep it", "Delete...", "To confirm, type DELETE.", "DELETE") && titles.equals(List.of("Delete it?", "To confirm, type DELETE.")));
        panel.setConfirmer((form, title) ->
        {
            if (form.wordField != null)
            {
                form.wordField.setText("remove");
                form.cancelButton.doClick();
                return form.answer();
            }
            form.goButton.doClick();
            return form.answer();
        });
        check("delete: the wrong word keeps the round", !panel.confirmTwice("Delete round", "Delete it?", null, "Keep it", "Delete...", "To confirm, type DELETE.", "DELETE"));
        panel.setConfirmer((form, title) ->
        {
            form.cancelButton.doClick();
            return form.answer();
        });
        check("delete: Keep it keeps the round without the second question", !panel.confirmTwice("Delete round", "Delete it?", null, "Keep it", "Delete...", "To confirm, type DELETE.", "DELETE"));
        deleteFolder(temp);
    }

    /*
     +--------------------------------------------------------------------------+
     | checkClearData()                                                         |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Clear through the real window logic, with the questions answered by      |
     | code: Home's Clear button; stopping at each question changes nothing;    |
     | rounds only and everything leave the right files and go Home with the    |
     | empty state and the footer naming the backup folder; and on the sample   |
     | data nothing is cleared (Yes only makes the golfer's own data folder).   |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing (throws IOException if a temporary file fails)       |
     +--------------------------------------------------------------------------+
     */
    private static void checkClearData() throws IOException
    {
        Path temp = Files.createTempDirectory("golfanalyst-clear");
        Path data = temp.resolve(DataStore.DATA_FOLDER);
        Files.createDirectories(data);
        Files.copy(Path.of("sample-data", DataLoader.COURSES_FILE), data.resolve(DataLoader.COURSES_FILE));
        Files.copy(Path.of("sample-data", DataLoader.ROUNDS_FILE), data.resolve(DataLoader.ROUNDS_FILE));
        DataStore store = new DataStore(data);
        store.setGolferName("Pat Golfer");
        int rounds = store.roundsByDate().size();
        int tees = store.dataset().tees().size();
        String roundsText = Files.readString(data.resolve(DataLoader.ROUNDS_FILE), StandardCharsets.UTF_8);
        String coursesText = Files.readString(data.resolve(DataLoader.COURSES_FILE), StandardCharsets.UTF_8);
        AppPanel panel = new AppPanel(store, Path.of("help"), "");
        TestShell homeShell = new TestShell(store);
        HomePage home = new HomePage(homeShell);
        ((WoodButton) findByText(home, "Clear")).doClick();
        check("clear: Home's Clear button asks the window to clear", homeShell.lastPage.equals("clear"));

        List<String> messages = new ArrayList<>();
        int[] stopAt = {0};
        int[] pick = {ConfirmForm.ROUNDS_ONLY};
        panel.setConfirmer((form, title) ->
        {
            messages.add(form.message());
            if (messages.size() == stopAt[0])
            {
                form.cancelButton.doClick();
                return form.answer();
            }
            if (form.roundsOnlyChoice != null)
            {
                (pick[0] == ConfirmForm.EVERYTHING ? form.everythingChoice : form.roundsOnlyChoice).doClick();
            }
            if (form.wordField != null)
            {
                form.wordField.setText("clear");
            }
            form.goButton.doClick();
            return form.answer();
        });
        for (int step = 1; step <= 3; step = step + 1)
        {
            messages.clear();
            stopAt[0] = step;
            panel.clearData();
            check("clear: stopping at question " + step + " clears nothing", Files.readString(data.resolve(DataLoader.ROUNDS_FILE),
                    StandardCharsets.UTF_8).equals(roundsText) && store.roundsByDate().size() == rounds && !Files.exists(data.resolve("backups")));
        }
        check("clear: the first question shows the counts", messages.get(0).equals("You have " + rounds + " rounds and " + tees
                + " courses and tees, and your name is Pat Golfer."));
        check("clear: the summary says what goes and that a copy is kept", messages.get(1).equals("This will remove " + rounds
                + " rounds.\nA copy of everything is kept first.") && messages.get(2).equals("To confirm, type CLEAR."));

        messages.clear();
        stopAt[0] = 0;
        panel.clearData();
        String header = roundsText.lines().filter(line -> line.startsWith("RoundID")).findFirst().orElse("");
        check("clear rounds only: rounds.tsv keeps its header row only, courses and name kept",
                Files.readString(data.resolve(DataLoader.ROUNDS_FILE), StandardCharsets.UTF_8).trim().equals(header)
                        && Files.readString(data.resolve(DataLoader.COURSES_FILE), StandardCharsets.UTF_8).equals(coursesText)
                        && store.roundsByDate().isEmpty() && store.golferName().equals("Pat Golfer"));
        check("clear rounds only: the window goes Home with the empty state", panel.currentPageName().equals(AppPanel.HOME)
                && panel.homePage().totalRounds.getText().equals("0"));
        List<Path> folders = new ArrayList<>();
        try (Stream<Path> files = Files.list(data.resolve("backups")))
        {
            files.forEach(folders::add);
        }
        check("clear rounds only: the footer names the clear folder, which holds the files as they were", folders.size() == 1
                && panel.footer().status().equals("Cleared. A copy of everything as it was is in backups" + java.io.File.separator
                        + folders.get(0).getFileName() + ".")
                && Files.readString(folders.get(0).resolve(DataLoader.ROUNDS_FILE), StandardCharsets.UTF_8).equals(roundsText));

        messages.clear();
        pick[0] = ConfirmForm.EVERYTHING;
        panel.clearData();
        check("clear everything: the summary names the courses and the name too", messages.get(1).startsWith("This will remove 0 rounds, "
                + tees + " courses and tees and your name."));
        check("clear everything: both files keep only their header rows and the name is gone",
                Files.readString(data.resolve(DataLoader.COURSES_FILE), StandardCharsets.UTF_8).trim().equals(golfanalyst.CourseWriter.STANDARD_HEADER)
                        && store.dataset().tees().isEmpty() && store.golferName().isEmpty() && store.includeHistorical()
                        && panel.homePage().golferName.getText().equals("(click to enter)"));

        Path parent = Files.createTempDirectory("golfanalyst-clear-sample");
        Path sample = parent.resolve(DataStore.SAMPLE_FOLDER);
        Files.createDirectories(sample);
        Files.copy(Path.of("sample-data", DataLoader.COURSES_FILE), sample.resolve(DataLoader.COURSES_FILE));
        Files.copy(Path.of("sample-data", DataLoader.ROUNDS_FILE), sample.resolve(DataLoader.ROUNDS_FILE));
        String sampleRounds = Files.readString(sample.resolve(DataLoader.ROUNDS_FILE), StandardCharsets.UTF_8);
        AppPanel samplePanel = new AppPanel(new DataStore(sample), Path.of("help"), "");
        List<String> sampleAsked = new ArrayList<>();
        int[] sampleAnswer = {javax.swing.JOptionPane.NO_OPTION};
        samplePanel.setAsker(question ->
        {
            sampleAsked.add(question);
            return sampleAnswer[0];
        });
        samplePanel.setConfirmer((form, title) ->
        {
            sampleAsked.add("confirmer");
            return ConfirmForm.GO;
        });
        samplePanel.clearData();
        check("clear: on the sample data it asks the sample question and clears nothing", sampleAsked.equals(List.of(AppPanel.CLEAR_SAMPLE_QUESTION))
                && Files.readString(sample.resolve(DataLoader.ROUNDS_FILE), StandardCharsets.UTF_8).equals(sampleRounds)
                && !Files.exists(parent.resolve(DataStore.DATA_FOLDER)));
        sampleAnswer[0] = javax.swing.JOptionPane.YES_OPTION;
        samplePanel.clearData();
        check("clear: Yes on the sample data only makes your own data folder", Files.isDirectory(parent.resolve(DataStore.DATA_FOLDER))
                && Files.readString(sample.resolve(DataLoader.ROUNDS_FILE), StandardCharsets.UTF_8).equals(sampleRounds)
                && !sampleAsked.contains("confirmer"));
        deleteFolder(parent);
        deleteFolder(temp);
    }

    /*
     * checkExtraCopyWarning(): with the extra backup folder missing (a USB stick not plugged in), saving an edited round still saves, makes the normal backup and says in the footer that the extra copy could not be made.
     * Passed in:  nothing
     * Passed out: nothing (throws IOException if a temporary file fails)
     */
    private static void checkExtraCopyWarning() throws IOException
    {
        Path temp = Files.createTempDirectory("golfanalyst-extra-copy");
        Files.copy(Path.of("sample-data", DataLoader.COURSES_FILE), temp.resolve(DataLoader.COURSES_FILE));
        Files.copy(Path.of("sample-data", DataLoader.ROUNDS_FILE), temp.resolve(DataLoader.ROUNDS_FILE));
        golfanalyst.Settings.load(temp).with(golfanalyst.Settings.BACKUP_COPY_FOLDER, temp.resolve("no-usb-stick").toString()).save(temp);
        DataStore store = new DataStore(temp);
        TestShell shell = new TestShell(store);
        RoundPage page = new RoundPage(shell);
        page.showLatest();
        page.editButton.doClick();
        String newScore = Integer.toString(Integer.parseInt(page.scoreFields[0].getText()) + 1);
        type(page.scoreFields[0], newScore);
        page.saveButton.doClick();
        check("extra copy: a missing extra folder only adds a warning to the footer, and the edit is saved",
                shell.status.startsWith("Changes saved") && shell.status.contains("  The extra backup copy could not be made: ")
                        && golfanalyst.Backups.backupsOf(temp, golfanalyst.Backups.EDIT_ROUND).size() == 1
                        && page.scoreFields[0].getText().equals(newScore) && !page.title().startsWith("Edit"));
        deleteFolder(temp);
    }

    /*
     * checkBackupFailure(): when the backup copy cannot be made, saving an edited round stops, the footer says why and rounds.tsv is unchanged.
     * Passed in:  nothing
     * Passed out: nothing (throws IOException if a temporary file fails)
     */
    private static void checkBackupFailure() throws IOException
    {
        Path temp = Files.createTempDirectory("golfanalyst-backup-failure");
        Files.copy(Path.of("sample-data", DataLoader.COURSES_FILE), temp.resolve(DataLoader.COURSES_FILE));
        Files.copy(Path.of("sample-data", DataLoader.ROUNDS_FILE), temp.resolve(DataLoader.ROUNDS_FILE));
        Files.writeString(temp.resolve(golfanalyst.Backups.FOLDER), "a file where the folder should be", StandardCharsets.UTF_8);
        String before = Files.readString(temp.resolve(DataLoader.ROUNDS_FILE), StandardCharsets.UTF_8);
        DataStore store = new DataStore(temp);
        TestShell shell = new TestShell(store);
        RoundPage page = new RoundPage(shell);
        page.showLatest();
        page.editButton.doClick();
        type(page.scoreFields[0], Integer.toString(Integer.parseInt(page.scoreFields[0].getText()) + 1));
        page.saveButton.doClick();
        check("backup failure: the footer says no copy could be made and nothing was changed",
                shell.status.startsWith("No backup copy of rounds.tsv could be made") && shell.status.contains("nothing was changed"));
        check("backup failure: rounds.tsv is unchanged and the round is still being edited",
                Files.readString(temp.resolve(DataLoader.ROUNDS_FILE), StandardCharsets.UTF_8).equals(before) && page.title().startsWith("Edit"));
        deleteFolder(temp);
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
     |   backup copy) and writes Y, and the handicap and statistics leave it    |
     |   out;                                                                   |
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
                && golfanalyst.SelfTest.backupCopies(roundsFile).size() == 1);
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
        JLabel parHeader = page.parHeaders[0];
        check("the handicap column is headed Hdcp", indexHeader.getText().equals("Hdcp") && parHeader.getText().equals("Par"));
        check("Par and Hdcp use the same bold 15 pt font as the cells", indexHeader.getFont().equals(parHeader.getFont())
                && indexHeader.getFont().isBold() && indexHeader.getFont().getSize2D() == 15f
                && indexHeader.getFont().equals(page.parFields[0][0].getFont()));
        check("the Hdcp header explains itself in a tool tip",
                "Hole handicap number (stroke index): 1 is the hardest hole.".equals(indexHeader.getToolTipText()));
        int headerWidth = indexHeader.getFontMetrics(indexHeader.getFont()).stringWidth(indexHeader.getText());
        check("the Hdcp header fits its 56 px cell (" + headerWidth + " px)", headerWidth <= 56 - 4);

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
                + " Sample Links. A copy of the file as it was is in the backups folder.  Expert was skipped because it has no rating."));
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
        List<Path> courseCopies = golfanalyst.SelfTest.backupCopies(coursesFile);
        check("the file as it was is copied to the backups folder", !courseCopies.isEmpty()
                && Files.readString(courseCopies.get(courseCopies.size() - 1), StandardCharsets.UTF_8).equals(beforeText));
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
        check("copying again with every cell filled copies nothing", shell.status.startsWith("The Par and Hdcp cells of the other tees are already filled"));
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

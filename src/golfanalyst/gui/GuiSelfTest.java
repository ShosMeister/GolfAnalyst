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
import java.awt.Component;
import java.awt.Container;
import javax.swing.AbstractButton;
import javax.swing.JComponent;
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
            windowCalls++;
            status = text;
        }

        /*
         * refreshTitle(): nothing to do without a header.
         */
        @Override
        public void refreshTitle()
        {
            windowCalls++;
        }

        /*
         * goHome(): remembers that Home was asked for.
         */
        @Override
        public void goHome()
        {
            windowCalls++;
            lastPage = "home";
        }

        /*
         * newRound(): remembers that a new round was asked for.
         */
        @Override
        public void newRound()
        {
            windowCalls++;
            lastPage = "new";
        }

        /*
         * browseRounds(): remembers that browsing was asked for.
         */
        @Override
        public void browseRounds()
        {
            windowCalls++;
            lastPage = "browse";
        }

        /*
         * newCourse(): remembers that New Course was asked for.
         */
        @Override
        public void newCourse()
        {
            windowCalls++;
            lastPage = "course";
        }

        /*
         * readyToSave(): always yes; the sample-data question is tested on the real AppPanel.
         */
        @Override
        public boolean readyToSave()
        {
            windowCalls++;
            return true;
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
            passedCount++;
        }
        else
        {
            failedCount++;
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
            failedCount++;
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
        int[] pars = {4, 3, 5, 4, 4, 3, 5, 4, 4, 4, 3, 5, 4, 4, 3, 5, 4, 4};
        for (int hole = 0; hole < 18; hole++)
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
        check("the fairway box is off on a par 3", !page.fairwayChecks[1].isEnabled());
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

        // sand: bunker shot with par on hole 2 (par 3) is a save
        page.sandChecks[1].setSelected(true);
        type(page.scoreFields[1], "3");
        type(page.puttFields[1], "1");
        check("a par from the sand is a sand save", page.sandBox.getText().equals("1 / 1"));

        // Save writes to the file and reads back the same
        page.saveButton.doClick();
        check("saving adds a round", store.roundsByDate().size() == roundsBefore + 1);
        Dataset reread = DataLoader.load(temp);
        check("the file loads back with no errors", reread.errorCount() == 0);
        Round saved = reread.rounds().get(reread.rounds().size() - 1);
        check("the saved round has the typed scores",
                saved.scores()[0] == 3 && saved.putts()[0] == 0 && saved.scores()[1] == 3 && saved.putts()[1] == 1
                        && saved.totalScore() == 87);
        check("the saved round remembers the fairways and the bunker",
                saved.fairway()[0] && saved.fairway()[2] && !saved.fairway()[1] && saved.sand()[1]);
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
        puttDoc.setDocumentFilter(new RoundPage.EntryFilter(false, () -> false, () -> advances[0]++, null));
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
            scoreDoc.setDocumentFilter(new RoundPage.EntryFilter(true, () -> false, () -> advances[0]++, null));
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
            loadingDoc.setDocumentFilter(new RoundPage.EntryFilter(false, () -> true, () -> advances[0]++, null));
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
        letterDoc.setDocumentFilter(new RoundPage.EntryFilter(false, () -> false, () -> advances[0]++, letter -> letters.append(letter)));
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
            page.puttFields[1].getDocument().insertString(0, "f", null);
            check("F does nothing on a par 3", !page.fairwayChecks[1].isSelected());
            page.puttFields[1].getDocument().insertString(0, "s", null);
            check("S in a putts box ticks the Sand box", page.sandChecks[1].isSelected());
            check("the letters were not typed into the box", page.puttFields[0].getText().isEmpty() && page.puttFields[1].getText().isEmpty());
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
                "round-view-info.txt", "round-view-help.txt", "course-info.txt", "course-help.txt"})
        {
            String helpText = Files.readString(Path.of("help", file));
            check(file + " does not call a round a score", !helpText.contains("Scores Used") && !helpText.contains("20 scores")
                    && !helpText.contains("your best 8 scores") && !helpText.contains("scores on record"));
        }
        String entryHelp = Files.readString(Path.of("help", "round-new-help.txt"));
        check("the entering help explains the F and S keys and says they are optional",
                entryHelp.contains("F ticks or unticks Fwy") && entryHelp.contains("S does the same for Sand")
                        && entryHelp.contains("you do not have to use them"));
        check("the entering info mentions the F and S keys", Files.readString(Path.of("help", "round-new-info.txt")).contains("type F in the Putt box"));
        check("the Name help is not 'whose rounds these are'", !Files.readString(Path.of("help", "home-info.txt")).contains("Whose rounds"));

        // The Show drop-down limits the arrows to one course or one set of tees
        page.showLatest();
        check("the Show drop-down is visible when looking at rounds", page.filterCombo.isVisible());
        RoundPage.FilterItem riverbend = null;
        RoundPage.FilterItem linksAllTees = null;
        for (int lp = 0; lp < page.filterCombo.getItemCount(); lp++)
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
            riverbendRounds += round.teeId() == riverbend.teeId() ? 1 : 0;
            linksRounds += (round.teeId() == 1 || round.teeId() == 2) ? 1 : 0;
        }
        page.filterCombo.setSelectedItem(riverbend);
        check("choosing a tee shows a round on that tee", page.title().contains("Riverbend GC"));
        page.firstButton.doClick();
        int steps = 1;
        boolean allRiverbend = page.title().contains("Riverbend GC");
        while (page.nextButton.isEnabled())
        {
            page.nextButton.doClick();
            steps++;
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
            steps++;
        }
        check("an all-tees choice steps through every tee of the course", steps == linksRounds);
        page.filterCombo.setSelectedIndex(0);
        page.firstButton.doClick();
        steps = 1;
        while (page.nextButton.isEnabled())
        {
            page.nextButton.doClick();
            steps++;
        }
        check("all rounds steps through every saved round", steps == store.roundsByDate().size());

        // Help: separate files for entering and for looking, and they all exist
        page.showNew();
        check("while entering, the round help files are the entry ones", page.helpKey().equals("round-new"));
        page.showLatest();
        check("while looking, the round help files are the viewing ones", page.helpKey().equals("round-view"));
        for (String key : new String[] {"home", "round-new", "round-view", "course"})
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
        for (int hole = 0; hole < 9; hole++)
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
        for (int hole = 0; hole < 18; hole++)
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
        ((WoodButton) findByText(panel, "Stats Board")).doClick();
        check("an unbuilt button says so", panel.footer().status().contains("not built yet"));
        ((WoodButton) findByText(panel, "New Course")).doClick();
        check("New Course shows the course page", panel.header().title().equals("New Course"));
        check("the course page asks for the name and zip first", panel.footer().status().contains("5-digit zip"));
        panel.coursePage().cancelButton.doClick();
        check("Cancel on the course page goes back to Home", panel.header().title().equals("Golf Analyst Stats Home"));
        check("your own data: the window title does not say sample", !panel.windowTitle().contains("SAMPLE"));
        check("your own data: saving does not ask the sample question", panel.readyToSave());
        deleteFolder(windowTemp);

        checkCourseTyping();
        checkHelpFormatter();
        checkCoursePage(coursePage, store, shell, temp);
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
        for (int lp = paths.size() - 1; lp >= 0; lp--)
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
        doc.setDocumentFilter(new CoursePage.BoxFilter(kind, () -> nine, () -> false, () -> advances[0]++));
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
        for (int hole = 0; hole < pars.length; hole++)
        {
            page.parFields[tee][hole].setText(Integer.toString(pars[hole]));
            page.indexFields[tee][hole].setText(Integer.toString(index[hole]));
        }
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
                "round-view-info.txt", "round-view-help.txt", "course-info.txt", "course-help.txt"})
        {
            String text = Files.readString(Path.of("help", file));
            List<HelpFormatter.Piece> filePieces = HelpFormatter.pieces(text);
            boolean hasHeading = filePieces.stream().anyMatch(piece -> piece.style() == HelpFormatter.Style.HEADING);
            check(file + ": has its section headings marked", hasHeading || file.equals("round-view-help.txt"));
            check(file + ": no # is shown", filePieces.stream().noneMatch(piece -> piece.text().startsWith("#")));
            check(file + ": the title is not repeated", !filePieces.get(0).text().equals(HelpFormatter.title(text)));
        }
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
        for (int hole = 0; hole < 18; hole++)
        {
            index[hole] = hole + 1;
            reversed[hole] = 18 - hole;
        }

        // The look: five frames of the agreed sizes, spreadsheet cells, gold outline on the cursor's cell
        check("there are five frames: captions and four tees", page.framesRow.getComponentCount() == 5);
        check("the caption frame is 136 x 526", page.framesRow.getComponent(0).getPreferredSize().equals(new java.awt.Dimension(136, 526)));
        boolean teeSizes = true;
        for (int lp = 1; lp < 5; lp++)
        {
            teeSizes = teeSizes && page.framesRow.getComponent(lp).getPreferredSize().equals(new java.awt.Dimension(134, 526));
        }
        check("each tee frame is 134 x 526, so the rows line up", teeSizes);
        check("a cell is 56 x 21", page.parFields[0][0].getPreferredSize().equals(new java.awt.Dimension(56, 21)));
        check("a cell is bold 15 pt", page.parFields[0][0].getFont().isBold() && page.parFields[0][0].getFont().getSize2D() == 15f);

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

        // A course already in the program, typed with other capitals and spaces
        page.courseField.setText("sample   LINKS");
        check("with a name but no zip, the footer asks for the zip", shell.status.equals("Type the course's 5-digit zip."));
        page.zipField.setText("29000");
        check("a course already there is found whatever the capitals", page.cityField.getText().equals("Anytown")
                && page.stateField.getText().equals("SC"));
        check("its city and state are locked", !page.cityField.isEditable() && !page.stateField.isEditable());
        check("the footer lists the tees it has", shell.status.contains("White, Blue"));
        check("the cursor skips the locked city and state", page.componentAfter(page.zipField) == page.teeNameFields[0]);

        fillTee(page, 0, "white", "69.0", "120", pars, index);
        check("the total par is worked out as you type", page.parTotalBoxes[0].getText().equals("72"));
        check("a tee name already on the course is named at once, ignoring capitals",
                shell.status.equals("There is already a tee called white on this course."));
        check("a real mistake turns its cell pink at once", page.teeNameFields[0].getBackground().equals(Theme.FIELD_ERROR));
        check("Save stays off while there is a mistake", !page.saveButton.isEnabled());
        page.teeNameFields[0].setText("Gold");
        check("fixing it clears the pink", !page.teeNameFields[0].getBackground().equals(Theme.FIELD_ERROR));
        check("one complete tee is ready to save", shell.status.equals("Ready to save.") && page.saveButton.isEnabled());

        fillTee(page, 1, "GOLD", "71.0", "125", pars, reversed);
        check("two new tees with the same name are a mistake", shell.status.equals("There is already a tee called GOLD on this course.")
                && !page.saveButton.isEnabled());
        page.teeNameFields[1].setText("Red");

        page.indexFields[1][4].setText("16");
        check("a repeated stroke index is named by tee and hole",
                shell.status.equals("Red tee, hole 5: stroke index 16 is already used on hole 3."));
        check("a repeated stroke index is pink", page.indexFields[1][4].getBackground().equals(Theme.FIELD_ERROR));
        page.indexFields[1][4].setText("14");

        page.parFields[1][6].setText("");
        check("a missing par is named by tee and hole", shell.status.equals("Red tee, hole 7: the par is missing."));
        check("a cell not filled in yet is not pink", page.parFields[1][6].getBackground().equals(java.awt.Color.WHITE));
        check("Save is off while a par is missing", !page.saveButton.isEnabled());
        check("the total par counts only the pars typed", page.parTotalBoxes[1].getText().equals("67"));
        page.parFields[1][6].setText("5");

        page.slopeFields[1].setText("");
        check("a missing slope is named", shell.status.equals("Red tee: the slope is missing."));
        page.slopeFields[1].setText("125");

        page.frontRatingFields[1].setText("35.0");
        check("a front-nine rating needs its slope", shell.status.contains("go together") && !page.saveButton.isEnabled());
        page.frontSlopeFields[1].setText("119");

        page.yardageFields[2].setText("5000");
        check("with everything filled in, the footer says it is ready", shell.status.equals("Ready to save.") && page.saveButton.isEnabled());
        int nextId = store.nextTeeId();
        int teesBefore = store.dataset().tees().size();
        page.saveButton.doClick();
        check("Save adds the tees and says so, naming a tee skipped for having no rating",
                shell.status.equals("Added 2 tees for Sample Links.  Intermediate was skipped because it has no rating."));
        check("after saving, the window goes Home", shell.lastPage.equals("home"));
        Dataset reread = DataLoader.load(temp);
        check("the course file loads back with no errors", reread.errorCount() == 0);
        check("two tees were added", reread.tees().size() == teesBefore + 2 && store.dataset().tees().size() == teesBefore + 2);
        golfanalyst.Tee gold = reread.teeMap().get(nextId);
        golfanalyst.Tee red = reread.teeMap().get(nextId + 1);
        check("the new TeeIDs are the highest plus one, then plus two", gold != null && red != null);
        check("the new tees join the course under its own spelling", gold != null && gold.courseName().equals("Sample Links")
                && gold.city().equals("Anytown") && gold.zip().equals("29000"));
        check("the typed numbers are saved", red != null && red.rating() == 71.0 && red.slope() == 125
                && red.strokeIndex()[0] == 18 && red.pars()[6] == 5 && red.frontRating() == 35.0 && red.frontSlope() == 119
                && red.backSlope() == 0 && red.yardage() == 0);

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
        check("the copy arrow with nothing to copy says what to do", shell.status.startsWith("Type the pars and stroke index of Ladies first"));
        fillTee(page, 0, "Ladies", "70.2", "121", pars, index);
        page.parFields[2][3].setText("5");
        page.copyButton.doClick();
        check("the copy arrow says which tees it filled", shell.status.equals("Copied the pars and stroke index of Ladies into the empty cells of"
                + " Beginner, Intermediate, Expert. Check any holes that differ on the card."));
        boolean copied = true;
        for (int hole = 0; hole < 18; hole++)
        {
            copied = copied && page.parFields[1][hole].getText().equals(Integer.toString(pars[hole]))
                    && page.indexFields[3][hole].getText().equals(Integer.toString(index[hole]));
        }
        check("the pars and stroke index are copied to the other tees", copied);
        check("a par typed before copying is kept", page.parFields[2][3].getText().equals("5"));
        check("the rating and slope are never copied", page.ratingFields[1].getText().isEmpty() && page.slopeFields[3].getText().isEmpty());
        check("copying does not move the cursor or need a click on each cell", page.parTotalBoxes[1].getText().equals("72"));
        page.copyButton.doClick();
        check("copying again with every cell filled copies nothing", shell.status.startsWith("The par and SI cells of the other tees are already filled"));
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
        for (int hole = 0; hole < 18; hole++)
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

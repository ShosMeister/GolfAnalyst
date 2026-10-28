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
                "round-view-info.txt", "round-view-help.txt"})
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
        for (String key : new String[] {"home", "round-new", "round-view"})
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
        for (Path file : (Iterable<Path>) Files.list(windowTemp)::iterator)
        {
            Files.deleteIfExists(file);
        }
        Files.deleteIfExists(windowTemp);

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
}

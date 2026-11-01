package golfanalyst.gui;

import golfanalyst.DataStore;
import golfanalyst.Problem;
import golfanalyst.Stats;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Window;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.ToIntFunction;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

/*
 +--------------------------------------------------------------------------+
 | AppPanel                                                                 |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | Everything the window does, except being a window. The header (Home,     |
 | title, Help, Info) is fixed at the top and the footer (status, progress) |
 | is fixed at the bottom; only the middle changes, one page at a time      |
 | (Home, Round, and more later). It carries out what the pages ask of a    |
 | Shell: switching pages, showing messages, and opening the Info and Help  |
 | windows from the text files in the help folder.                          |
 |                                                                          |
 | It is a plain panel so that its real start-up and navigation can be      |
 | tested on a computer with no display; AppFrame just puts it in a window. |
 |                                                                          |
 | If problems were found while loading the data files, the footer says     |
 | how many and a click on it lists them with their line numbers.           |
 |                                                                          |
 | When the program is running on the sample data, the window title and     |
 | the start-up footer say so, and before anything is saved it asks         |
 | whether to make the golfer's own data folder first (readyToSave).        |
 |                                                                          |
 | Public Methods:                                                          |
 | navigate(pageName)  - shows a page                                       |
 | windowTitle()       - the text for the window's title bar                |
 | the Shell methods   - store, setStatus, refreshTitle, goHome, newRound,  |
 |                       browseRounds, editRound, newCourse, readyToSave,   |
 |                       askYesNo, confirmTwice, clearData                  |
 +--------------------------------------------------------------------------+
 */
@SuppressWarnings({"serial", "this-escape"})
public final class AppPanel extends JPanel implements Shell
{

    public static final String HOME = "home";
    public static final String ROUND = "round";
    public static final String COURSE = "course";
    public static final String STATS = "stats";
    public static final String BEST = "best";
    static final String SAMPLE_QUESTION = "This is the sample data. Create your own data folder first?";
    static final String CLEAR_SAMPLE_QUESTION = "This is the sample data. There is nothing of yours to clear. Create your own data folder first?";
    static final String CLEAR_TITLE = "Clear data";
    static final String CLEAR_ROUNDS_TEXT = "Clear my rounds only (keep my courses and my name)";
    static final String CLEAR_EVERYTHING_TEXT = "Clear everything: my rounds, my courses, my name and my settings (like a brand-new copy of the program)";
    private static final String DIALOG_TITLE = "Golf Analyst";

    /*
     * Confirmer: shows a ConfirmForm and gives its answer (on screen normally; the self-test answers by code).
     */
    interface Confirmer
    {
        /*
         * ask(form, title): the form's answer.
         */
        int ask(ConfirmForm form, String title);
    }

    private final transient DataStore store;
    private final transient Path helpDir;
    private final HeaderBar header = new HeaderBar();
    private final FooterBar footer = new FooterBar();
    private final CardLayout cards = new CardLayout();
    private final JPanel center = new JPanel(cards);
    private final transient Map<String, Page> pages = new LinkedHashMap<>();
    private final RoundPage roundPage;
    private final CoursePage coursePage;
    private final StatsPage statsPage;
    private final RoundPage bestPage;
    private String current = HOME;
    private transient ToIntFunction<String> asker = question -> askOnScreen(question);
    private transient Confirmer confirmer = (form, title) -> form.showOn(this, title);

    /*
     +--------------------------------------------------------------------------+
     | AppPanel(store, helpDir, startNote)                                      |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Builds the pages, the header and footer, and what the Home, Help and     |
     | Info buttons do. Shows the Home page. A page must not call back into     |
     | this panel while it is being built (the panel is not ready for it);      |
     | the self-test checks that.                                               |
     |                                                                          |
     | Passed in:  store     - the loaded data                                  |
     |             helpDir   - the folder with the help text files              |
     |             startNote - a note for the footer at start-up (or "")        |
     | Passed out: the panel                                                    |
     +--------------------------------------------------------------------------+
     */
    public AppPanel(DataStore store, Path helpDir, String startNote)
    {
        super(new BorderLayout());
        this.store = store;
        this.helpDir = helpDir;
        HomePage homePage = new HomePage(this);
        roundPage = new RoundPage(this);
        coursePage = new CoursePage(this);
        statsPage = new StatsPage(this);
        bestPage = new RoundPage(this, true);
        pages.put(HOME, homePage);
        pages.put(ROUND, roundPage);
        pages.put(COURSE, coursePage);
        pages.put(STATS, statsPage);
        pages.put(BEST, bestPage);
        center.add(homePage, HOME);
        center.add(roundPage, ROUND);
        center.add(coursePage, COURSE);
        center.add(statsPage, STATS);
        center.add(bestPage, BEST);
        add(header, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);

        header.homeButton().addActionListener(event -> leaveForHome());
        header.helpButton().addActionListener(event -> showHelp(false));
        header.infoButton().addActionListener(event -> showHelp(true));
        navigate(HOME);
        setStatus(startStatus(startNote));
    }

    /*
     * startStatus(note): the message shown when the program opens: a count of problems found, or the note and how many rounds loaded (and how many of them are historical).
     * Passed in:  note - a note to show first (may be empty)
     * Passed out: the message
     */
    private String startStatus(String note)
    {
        String sample = store.isSampleData() ? "This is the sample data, not your own.  " : "";
        long problems = store.dataset().problems().size();
        if (problems > 0)
        {
            return sample + problems + " problem(s) found in the data files - click here to see them.";
        }
        int allRounds = store.dataset().rounds().size();
        int historical = allRounds - Stats.current(store.dataset().rounds()).size();
        String loaded = allRounds + " rounds loaded" + (historical > 0 ? " (" + historical + " historical)." : ".");
        return sample + (note.isEmpty() ? "Ready.  " + loaded : note + "  " + loaded);
    }

    /*
     * windowTitle(): the text for the window's title bar: the program and version, and SAMPLE DATA when that is what is open.
     * Passed in:  nothing
     * Passed out: for example "Golf Analyst v4.10 - SAMPLE DATA"
     */
    public String windowTitle()
    {
        return Theme.VERSION_TEXT + (store.isSampleData() ? " - SAMPLE DATA" : "");
    }

    /*
     +--------------------------------------------------------------------------+
     | navigate(pageName)                                                       |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Shows one page in the middle of the window, lets it refresh itself,      |
     | and puts its title in the header. The Home button is switched off on     |
     | the Home page.                                                           |
     |                                                                          |
     | Passed in:  pageName - HOME, ROUND, COURSE, STATS or BEST                |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    public void navigate(String pageName)
    {
        current = pageName;
        cards.show(center, pageName);
        pages.get(pageName).onShow();
        header.homeButton().setEnabled(!HOME.equals(pageName));
        refreshTitle();
    }

    /*
     +--------------------------------------------------------------------------+
     | showHelp(info)                                                           |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Opens the Info or Help window for the page being shown, from its text    |
     | file. It needs the real program window to open over; without one         |
     | (as in the self-test) it says so in the footer instead.                  |
     |                                                                          |
     | Passed in:  info - true for the Info text, false for the Help text       |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    private void showHelp(boolean info)
    {
        Window owner = SwingUtilities.getWindowAncestor(this);
        if (!(owner instanceof Frame))
        {
            setStatus("The help window needs the program window.");
            return;
        }
        Page page = pages.get(current);
        HelpWindow.show((Frame) owner, helpDir, page.helpKey() + (info ? "-info" : "-help"),
                "Golf Analyst " + (info ? "Info" : "Help"));
    }

    /*
     * showProblems(): lists every problem found in the data files, with its file and line number, in a window.
     * Passed in:  nothing
     * Passed out: nothing
     */
    private void showProblems()
    {
        if (store.dataset().problems().isEmpty())
        {
            return;
        }
        StringBuilder text = new StringBuilder();
        for (Problem problem : store.dataset().problems())
        {
            text.append(problem).append('\n');
        }
        JTextArea area = new JTextArea(text.toString(), 18, 80);
        area.setEditable(false);
        area.setFont(Theme.font(Font.PLAIN, 13f));
        JOptionPane.showMessageDialog(this, new JScrollPane(area), "Problems found in the data files",
                JOptionPane.WARNING_MESSAGE);
    }

    /*
     * store(): the loaded data folder, handicap and statistics.
     */
    @Override
    public DataStore store()
    {
        return store;
    }

    /*
     * setStatus(text): shows a message in the footer. Clicking the message lists the problems in the data files, so it is clickable (with the pointing hand) only while there are problems to list.
     */
    @Override
    public void setStatus(String text)
    {
        footer.setStatus(text);
        footer.setStatusClickAction((store.dataset().problems().isEmpty() ? null : () -> showProblems()));
    }

    /*
     * refreshTitle(): puts the current page's title in the header (does nothing if the page is not registered yet).
     */
    @Override
    public void refreshTitle()
    {
        Page page = pages.get(current);
        if (page != null)
        {
            header.setTitle(page.title());
        }
    }

    /*
     * goHome(): shows the Home page.
     */
    @Override
    public void goHome()
    {
        navigate(HOME);
    }

    /*
     * newRound(): shows the round page ready to enter a new round.
     */
    @Override
    public void newRound()
    {
        roundPage.showNew();
        navigate(ROUND);
    }

    /*
     * browseRounds(): shows the round page at the latest saved round.
     */
    @Override
    public void browseRounds()
    {
        roundPage.showLatest();
        navigate(ROUND);
    }

    /*
     * browseRoundsOn(teeId): opens the round screen on the newest round on those tees, with Show set to them.
     */
    @Override
    public void browseRoundsOn(int teeId)
    {
        roundPage.showLatestOn(teeId);
        navigate(ROUND);
    }

    /*
     * statsBoard(): opens the Stats Board.
     */
    @Override
    public void statsBoard()
    {
        navigate(STATS);
    }

    /*
     * bestRound(): opens the Best Round screen afresh, on the course and tees of the latest round, showing Single.
     */
    @Override
    public void bestRound()
    {
        bestPage.showBest();
        navigate(BEST);
    }

    /*
     +--------------------------------------------------------------------------+
     | canQuit()                                                                |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | What the window's close box asks before the program quits. Nothing       |
     | needs tidying up (every save is written to the files at once), so the    |
     | only thing to protect is typing that has not been saved: the page on     |
     | show asks "Discard your changes?" if it has any, as the Home button      |
     | does.                                                                    |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: true if the program may quit                                 |
     +--------------------------------------------------------------------------+
     */
    public boolean canQuit()
    {
        return pages.get(current).canLeave();
    }

    /*
     * leaveForHome(): what the Home button does: goes Home, unless the page on show has unsaved changes and the answer to "Discard your changes?" is No.
     */
    private void leaveForHome()
    {
        if (pages.get(current).canLeave())
        {
            goHome();
        }
    }

    /*
     * editRound(): shows the round viewer at the latest round, with its Edit button, or says there is nothing to edit yet.
     */
    @Override
    public void editRound()
    {
        if (store.roundsByDate().isEmpty())
        {
            setStatus("There are no saved rounds to edit yet.");
            return;
        }
        roundPage.showLatest();
        navigate(ROUND);
        setStatus("Find the round with the arrows or the Show box, then press Edit.");
    }

    /*
     * askYesNo(question): asks a Yes / No question in a small window over this one (closing it counts as No).
     * Passed in:  question - the words to ask
     * Passed out: true for Yes
     */
    @Override
    public boolean askYesNo(String question)
    {
        return asker.applyAsInt(question) == JOptionPane.YES_OPTION;
    }

    /*
     * newCourse(): shows the New Course page, empty.
     */
    @Override
    public void newCourse()
    {
        coursePage.showNew();
        navigate(COURSE);
    }

    /*
     +--------------------------------------------------------------------------+
     | readyToSave()                                                            |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | On the golfer's own data, says yes at once. On the sample data it        |
     | asks SAMPLE_QUESTION:                                                    |
     |   Yes    - makes an empty data folder next to sample-data (header rows   |
     |            only, nothing copied, nothing written over), says to close    |
     |            and reopen the program, and saves nothing;                    |
     |   No     - saves into the sample data;                                   |
     |   Cancel - saves nothing.                                                |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: true to go ahead and save                                    |
     +--------------------------------------------------------------------------+
     */
    @Override
    public boolean readyToSave()
    {
        if (!store.isSampleData())
        {
            return true;
        }
        int answer = asker.applyAsInt(SAMPLE_QUESTION);
        if (answer == JOptionPane.NO_OPTION)
        {
            return true;
        }
        if (answer != JOptionPane.YES_OPTION)
        {
            return false;
        }
        makeOwnDataFolder();
        return false;
    }

    /*
     * makeOwnDataFolder(): makes an empty data folder next to sample-data (never writing over one that is there) and says in the footer to reopen the program.
     */
    private void makeOwnDataFolder()
    {
        Path folder = store.dataFolderBeside();
        if (Files.isDirectory(folder))
        {
            setStatus("There is already a data folder at " + folder + ". Close Golf Analyst and open it again to use it.");
            return;
        }
        try
        {
            DataStore.createEmptyFolder(folder);
            setStatus("Your own data folder was made at " + folder + ". Close Golf Analyst and open it again to use it.");
        }
        catch (IOException failure)
        {
            setStatus("The data folder could not be made: " + failure.getMessage());
        }
    }

    /*
     +--------------------------------------------------------------------------+
     | confirmTwice(question, keepLabel, goLabel, word)                         |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Asks the two careful questions (see Shell): the question with Keep as    |
     | the default, then "To confirm, type WORD." Both go through the           |
     | confirmer, so the self-test can answer them.                             |
     |                                                                          |
     | Passed in:  question, keepLabel, goLabel - the first question            |
     |             word - the word to type                                      |
     | Passed out: true only if both were answered yes                          |
     +--------------------------------------------------------------------------+
     */
    @Override
    public boolean confirmTwice(String question, String keepLabel, String goLabel, String word)
    {
        if (confirmer.ask(ConfirmForm.twoChoice(question, keepLabel, goLabel), DIALOG_TITLE) != ConfirmForm.GO)
        {
            return false;
        }
        return confirmer.ask(ConfirmForm.typed("To confirm, type " + word + ".", word), DIALOG_TITLE) == ConfirmForm.GO;
    }

    /*
     +--------------------------------------------------------------------------+
     | clearData()                                                              |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | What Home's Clear button does. On the sample data it clears nothing and  |
     | asks CLEAR_SAMPLE_QUESTION (Yes makes the golfer's own data folder).     |
     | Otherwise: the "Clear data" choice (rounds only, or everything), the     |
     | summary with "Keep my data" as the default, then "To confirm, type       |
     | CLEAR." If all three are answered, DataStore.clear copies the files      |
     | into backups\clear-... first (if that fails nothing is cleared and the   |
     | footer says why), clears, and the window goes Home.                      |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    @Override
    public void clearData()
    {
        if (store.isSampleData())
        {
            if (asker.applyAsInt(CLEAR_SAMPLE_QUESTION) == JOptionPane.YES_OPTION)
            {
                makeOwnDataFolder();
            }
            return;
        }
        int rounds = store.roundsByDate().size();
        int tees = store.dataset().tees().size();
        String name = store.golferName();
        int choice = confirmer.ask(ConfirmForm.clearChoice(clearCounts(rounds, tees, name), CLEAR_ROUNDS_TEXT, CLEAR_EVERYTHING_TEXT),
                CLEAR_TITLE);
        if (choice == ConfirmForm.CANCELLED)
        {
            return;
        }
        boolean everything = choice == ConfirmForm.EVERYTHING;
        if (!confirmTwice(clearSummary(everything, rounds, tees, name), "Keep my data", "Clear...", "CLEAR"))
        {
            return;
        }
        Path folder;
        try
        {
            folder = store.clear(everything);
        }
        catch (IOException failure)
        {
            setStatus((failure instanceof golfanalyst.Backups.Failed ? "" : "The data could not be cleared: ") + failure.getMessage());
            return;
        }
        navigate(HOME);
        setStatus("Cleared. A copy of everything as it was is in " + golfanalyst.Backups.FOLDER + java.io.File.separator
                + folder.getFileName() + ".");
    }

    /*
     * setConfirmer(newConfirmer): replaces the on-screen careful questions with answers given by code (for the self-test).
     * Passed in:  newConfirmer - takes the form and its title and gives its answer
     * Passed out: nothing
     */
    void setConfirmer(Confirmer newConfirmer)
    {
        confirmer = newConfirmer;
    }

    /*
     * askOnScreen(question): asks a question in a small window over this one: the two sample-data questions in the shared ConfirmForm window (Yes / No / Cancel, or Yes / No for Clear's), Yes / No for anything else.
     * Passed in:  question - the words to ask
     * Passed out: JOptionPane.YES_OPTION, NO_OPTION or CANCEL_OPTION (closing the window counts as Cancel)
     */
    private int askOnScreen(String question)
    {
        ConfirmForm sample = sampleForm(question);
        if (sample != null)
        {
            int answer = sample.showOn(this, DIALOG_TITLE);
            if (answer == ConfirmForm.GO)
            {
                return JOptionPane.YES_OPTION;
            }
            return (answer == ConfirmForm.NO ? JOptionPane.NO_OPTION : JOptionPane.CANCEL_OPTION);
        }
        int answer = JOptionPane.showConfirmDialog(this, question, DIALOG_TITLE, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        return answer == JOptionPane.CLOSED_OPTION ? JOptionPane.CANCEL_OPTION : answer;
    }

    /*
     * sampleForm(question): the shared-window form for a sample-data question (Yes / No / Cancel for saving, Yes / No for Clear), or null for any other question.
     * Passed in:  question - the words to ask
     * Passed out: the form, or null
     */
    static ConfirmForm sampleForm(String question)
    {
        if (question.equals(SAMPLE_QUESTION))
        {
            return ConfirmForm.yesNoCancel(question, true);
        }
        return (question.equals(CLEAR_SAMPLE_QUESTION) ? ConfirmForm.yesNoCancel(question, false) : null);
    }

    /*
     * clearCounts(rounds, tees, name): the first line of the Clear data window, e.g. "You have 37 rounds and 12 courses and tees, and your name is Pat."
     * Passed in:  rounds, tees - how many there are, name - the golfer's name or ""
     * Passed out: the words
     */
    static String clearCounts(int rounds, int tees, String name)
    {
        return "You have " + roundsWords(rounds) + " and " + teesWords(tees) + (name.isEmpty() ? "" : ", and your name is " + name) + ".";
    }

    /*
     * clearSummary(everything, rounds, tees, name): Clear's summary question, e.g. "This will remove 37 rounds." and "A copy of everything is kept first."
     * Passed in:  everything - true for Clear everything, rounds, tees - how many there are, name - the golfer's name or ""
     * Passed out: the words
     */
    static String clearSummary(boolean everything, int rounds, int tees, String name)
    {
        String removed = (everything ? roundsWords(rounds) + ", " + teesWords(tees) + (name.isEmpty() ? "" : " and your name") : roundsWords(rounds));
        return "This will remove " + removed + ".\nA copy of everything is kept first.";
    }

    /*
     * roundsWords(count): "1 round" or "37 rounds".
     */
    private static String roundsWords(int count)
    {
        return count + (count == 1 ? " round" : " rounds");
    }

    /*
     * teesWords(count): "1 course and tee" or "12 courses and tees".
     */
    private static String teesWords(int count)
    {
        return count + (count == 1 ? " course and tee" : " courses and tees");
    }

    /*
     * setAsker(newAsker): replaces the on-screen question with an answer given by code (for the self-test, which has no screen).
     * Passed in:  newAsker - takes the question and gives a JOptionPane answer
     * Passed out: nothing
     */
    void setAsker(ToIntFunction<String> newAsker)
    {
        asker = newAsker;
    }

    /*
     * header(): the header bar (for tests).
     */
    HeaderBar header()
    {
        return header;
    }

    /*
     * footer(): the footer bar (for tests).
     */
    FooterBar footer()
    {
        return footer;
    }

    /*
     * roundPage(): the round page (for tests).
     */
    RoundPage roundPage()
    {
        return roundPage;
    }

    /*
     * bestPage(): the Best Round page (for tests).
     */
    RoundPage bestPage()
    {
        return bestPage;
    }

    /*
     * statsPage(): the Stats Board page (for tests).
     */
    StatsPage statsPage()
    {
        return statsPage;
    }

    /*
     * coursePage(): the New Course page (for tests).
     */
    CoursePage coursePage()
    {
        return coursePage;
    }

    /*
     * homePage(): the Home page (for tests).
     */
    HomePage homePage()
    {
        return (HomePage) pages.get(HOME);
    }

    /*
     * currentPageName(): the name of the page on show, e.g. HOME (for tests).
     */
    String currentPageName()
    {
        return current;
    }
}

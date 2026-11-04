package golfanalyst.gui;

import golfanalyst.DataStore;
import golfanalyst.Problem;
import golfanalyst.Stats;
import java.awt.BorderLayout;
import java.awt.CardLayout;
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
 |                       askYesNo, confirmTwice, clearData, importData,     |
 |                       openSettings, exportData, printReports             |
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
    public static final String ANALYZER = "analyzer";
    static final String SAMPLE_QUESTION = "This is the sample data. Create your own data folder first?";
    static final String SAMPLE_QUESTION_INSTALLED = "This is the sample data that comes with Golf Analyst, and it is kept as it is. "
            + "Create your own data folder now?";
    static final String OWN_FOLDER_THERE = "There is already a folder at ";
    static final String OWN_FOLDER_USE = " Use it as your data folder?";
    static final String CLEAR_SAMPLE_QUESTION = "This is the sample data. There is nothing of yours to clear. Create your own data folder first?";
    static final String CLEAR_TITLE = "Clear data";
    static final String CLEAR_ROUNDS_TEXT = "Clear my rounds only (keep my courses and my name)";
    static final String CLEAR_EVERYTHING_TEXT = "Clear everything: my rounds, my courses, my name and my settings (like a brand-new copy of the program)";
    static final String CONFIRM_TITLE = "Confirm";
    static final String DISCARD_TITLE = "Discard changes";
    static final String SAMPLE_TITLE = "Sample data";
    static final String PROBLEMS_TITLE = "Data problems";
    static final String EXPORT_FAILED_TITLE = "Export failed";
    static final String PROBLEMS_MESSAGE = "These problems were found in the data files. Each one names the file and the line to fix.";

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

    /*
     * SettingsAsker: shows the Settings window and gives its answer (on screen normally; the self-test answers by code).
     */
    interface SettingsAsker
    {
        /*
         * ask(form): ConfirmForm.GO once OK is pressed, CANCELLED otherwise.
         */
        int ask(SettingsForm form);
    }

    /*
     * ExportAsker: shows the Export window and gives its answer (on screen normally; the self-test answers by code).
     */
    interface ExportAsker
    {
        /*
         * ask(form): ConfirmForm.GO once Export is pressed, CANCELLED otherwise.
         */
        int ask(ExportForm form);
    }

    /*
     * PrintAsker: shows the Print window and gives its answer (on screen normally; the self-test answers by code).
     */
    interface PrintAsker
    {
        /*
         * ask(form): ConfirmForm.GO once a report was sent, CANCELLED otherwise.
         */
        int ask(PrintForm form);
    }

    /*
     * ImportAsker: shows the Import window and gives its answer (on screen normally; the self-test answers by code).
     */
    interface ImportAsker
    {
        /*
         * ask(form): ConfirmForm.GO once Import is pressed, CANCELLED otherwise.
         */
        int ask(ImportForm form);
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
    private final AnalyzerPage analyzerPage;
    private String current = HOME;
    private transient ToIntFunction<String> asker = question -> askOnScreen(question);
    private transient Confirmer confirmer = (form, title) -> form.showOn(this);
    private transient ImportAsker importAsker = form -> form.showOn(this);
    private transient SettingsForm.Tools settingsTools = new ScreenTools();
    private transient SettingsAsker settingsAsker = form -> form.showOn(this);
    private transient ExportAsker exportAsker = form -> form.showOn(this);
    private transient PrintForm.Tools printTools = new ScreenPrintTools();
    private transient PrintAsker printAsker = form -> form.showOn(this);
    private transient golfanalyst.ProgramFolders folders = golfanalyst.ProgramFolders.active();

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
        analyzerPage = new AnalyzerPage(this);
        pages.put(HOME, homePage);
        pages.put(ROUND, roundPage);
        pages.put(COURSE, coursePage);
        pages.put(STATS, statsPage);
        pages.put(BEST, bestPage);
        pages.put(ANALYZER, analyzerPage);
        center.add(homePage, HOME);
        center.add(roundPage, ROUND);
        center.add(coursePage, COURSE);
        center.add(statsPage, STATS);
        center.add(bestPage, BEST);
        center.add(analyzerPage, ANALYZER);
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
     | Passed in:  pageName - HOME, ROUND, COURSE, STATS, BEST or ANALYZER      |
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
     * showProblems(): lists every problem found in the data files, with its file and line number, in the shared dialog titled Data problems.
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
        showNotice(PROBLEMS_TITLE, PROBLEMS_MESSAGE, text.toString().trim());
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
     * browseRoundsOn(teeId, part): opens the round screen on the newest round of one Stats Board row, with Show set to it.
     */
    @Override
    public void browseRoundsOn(int teeId, golfanalyst.StatsBoard.Part part)
    {
        roundPage.showLatestOn(teeId, part);
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
     * analyzer(): opens the Analyzer afresh (its onShow picks the latest round's course and tees and Counts).
     */
    @Override
    public void analyzer()
    {
        navigate(ANALYZER);
    }

    /*
     * analyzerPage(): the Analyzer page (for tests).
     */
    AnalyzerPage analyzerPage()
    {
        return analyzerPage;
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
     | Installed, the sample data is part of the program and is never written   |
     | to, so it asks SAMPLE_QUESTION_INSTALLED instead: Yes makes the own data |
     | folder, No saves nothing.                                                |
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
        if (folders.isInstalled())
        {
            if (asker.applyAsInt(SAMPLE_QUESTION_INSTALLED) == JOptionPane.YES_OPTION)
            {
                makeOwnDataFolder();
            }
            return false;
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
     +--------------------------------------------------------------------------+
     | makeOwnDataFolder()                                                      |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Makes the golfer's own data folder (data next to sample-data from the    |
     | project, Documents\Golf Analyst when installed) with header rows only,   |
     | never writing over anything, and says in the footer to reopen the        |
     | program. If the folder is already there it first asks whether to use     |
     | it as it is (saying what it holds); No changes nothing. Installed, the   |
     | folder is also written to golfanalyst.location, so the next start opens  |
     | it at once.                                                              |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    private void makeOwnDataFolder()
    {
        Path folder = store.ownDataFolder();
        try
        {
            if (Files.isDirectory(folder))
            {
                if (!askYesNo(ownFolderQuestion(folder)))
                {
                    setStatus("Nothing was changed.");
                    return;
                }
                remember(folder);
                setStatus("Golf Analyst will use " + folder + " the next time it is opened. Close Golf Analyst and open it again to use it.");
                return;
            }
            DataStore.createEmptyFolder(folder);
            remember(folder);
            setStatus("Your own data folder was made at " + folder + ". Close Golf Analyst and open it again to use it.");
        }
        catch (IOException failure)
        {
            setStatus("The data folder could not be made: " + failure.getMessage());
        }
    }

    /*
     * ownFolderQuestion(folder): the question when the own data folder is already there: where it is, what it holds, and whether to use it.
     */
    static String ownFolderQuestion(Path folder)
    {
        return OWN_FOLDER_THERE + folder + ". " + golfanalyst.DataLocation.describe(folder).words() + OWN_FOLDER_USE;
    }

    /*
     * remember(folder): installed, writes the folder to golfanalyst.location in the user's settings folder (from the project it is found by its place).
     */
    private void remember(Path folder) throws IOException
    {
        if (folders.isInstalled())
        {
            golfanalyst.DataLocation.write(folders.locationFolder(), folder);
        }
    }

    /*
     +--------------------------------------------------------------------------+
     | confirmTwice(title, question, list, keepLabel, goLabel, prompt, word)    |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Asks the two careful questions (see Shell) in the shared dialog: the     |
     | question titled title, Keep as the default; then the typed word, titled  |
     | Confirm. Both go through the confirmer, so the self-test can answer      |
     | them.                                                                    |
     |                                                                          |
     | Passed in:  see Shell.confirmTwice                                       |
     | Passed out: true only if both were answered yes                          |
     +--------------------------------------------------------------------------+
     */
    @Override
    public boolean confirmTwice(String title, String question, String list, String keepLabel, String goLabel, String prompt,
            String word)
    {
        ConfirmForm first = (list == null ? ConfirmForm.twoChoice(question, keepLabel, goLabel)
                : ConfirmForm.twoChoiceWithList(question, list, keepLabel, goLabel));
        if (ask(first.withTitle(title)) != ConfirmForm.GO)
        {
            return false;
        }
        return ask(ConfirmForm.typed(prompt, word).withTitle(CONFIRM_TITLE)) == ConfirmForm.GO;
    }

    /*
     * confirmOnce(title, question, keepLabel, goLabel): one careful question in the shared dialog, Keep as the default (through the confirmer, so the self-test can answer it).
     */
    @Override
    public boolean confirmOnce(String title, String question, String keepLabel, String goLabel)
    {
        return ask(ConfirmForm.twoChoice(question, keepLabel, goLabel).withTitle(title)) == ConfirmForm.GO;
    }

    /*
     * showNotice(title, message, list): a message with OK in the shared dialog (through the confirmer).
     */
    @Override
    public void showNotice(String title, String message, String list)
    {
        ask(ConfirmForm.notice(message, list).withTitle(title));
    }

    /*
     * askText(title, question, initial): a question with one box in the shared dialog (through the confirmer); the text typed, or null for Cancel.
     */
    @Override
    public String askText(String title, String question, String initial)
    {
        ConfirmForm form = ConfirmForm.textInput(question, initial).withTitle(title);
        return (ask(form) == ConfirmForm.GO ? form.text() : null);
    }

    /*
     * ask(form): shows a dialog through the confirmer and gives its answer.
     */
    private int ask(ConfirmForm form)
    {
        return confirmer.ask(form, form.title());
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
        int choice = ask(ConfirmForm.clearChoice(clearCounts(rounds, tees, name), CLEAR_ROUNDS_TEXT, CLEAR_EVERYTHING_TEXT)
                .withTitle(CLEAR_TITLE));
        if (choice == ConfirmForm.CANCELLED)
        {
            return;
        }
        boolean everything = choice == ConfirmForm.EVERYTHING;
        if (!confirmTwice(CLEAR_TITLE, clearSummary(everything, rounds, tees, name), null, "Keep my data", "Clear...",
                "To confirm, type CLEAR.", "CLEAR"))
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
                + folder.getFileName() + "." + store.backupWarningNote());
    }

    /*
     +--------------------------------------------------------------------------+
     | importData()                                                             |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | What Home's Import button does. On the sample data it first asks the     |
     | sample-data question, as saving does. Then the Import window: the        |
     | golfer picks files and presses Check files (nothing is written) and      |
     | then Import. The store makes the backup folder first (if that fails      |
     | nothing is written and the footer says why), adds the new lines at the   |
     | end of the files and reloads; the window stays on Home and the footer    |
     | says what was imported and where the copy is.                            |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    @Override
    public void importData()
    {
        if (!readyToSave())
        {
            return;
        }
        ImportForm form = new ImportForm((courses, rounds, historical) -> store.checkImport(courses, rounds, historical));
        golfanalyst.Importer.Plan plan = (importAsker.ask(form) == ConfirmForm.GO ? form.plan() : null);
        if (plan == null || !plan.hasWork())
        {
            return;
        }
        Path folder;
        try
        {
            folder = store.applyImport(plan);
        }
        catch (IOException failure)
        {
            setStatus((failure instanceof golfanalyst.Backups.Failed ? "" : "Nothing was imported: ") + failure.getMessage());
            return;
        }
        navigate(HOME);
        int skipped = plan.teesSkipped() + plan.roundsSkipped();
        int rejected = plan.teesRejected() + plan.roundsRejected();
        setStatus("Imported " + golfanalyst.Importer.count(plan.newTees().size(), "tee", "tees") + " and "
                + golfanalyst.Importer.count(plan.newRounds().size(), "round", "rounds") + ". " + skipped + " skipped, " + rejected
                + " rejected. A copy of your files as they were is in " + golfanalyst.Backups.FOLDER + java.io.File.separator
                + folder.getFileName() + "." + store.backupWarningNote());
    }

    /*
     +--------------------------------------------------------------------------+
     | openSettings()                                                           |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | What Home's Settings button does: shows the Settings window and, if OK   |
     | is pressed, saves what was chosen (Cancel changes nothing). The name is  |
     | saved everywhere; on the sample data nothing else is. Backups to keep    |
     | and the extra backup folder go into settings.txt. A new data folder      |
     | (Change...) has the data copied into it first if that was chosen (never  |
     | writing over a file), then is written to golfanalyst.location next to    |
     | the program; it is used from the next time the program is opened, and    |
     | the footer says so.                                                      |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    @Override
    public void openSettings()
    {
        SettingsForm form = new SettingsForm(store, settingsTools);
        if (settingsAsker.ask(form) != ConfirmForm.GO)
        {
            return;
        }
        try
        {
            if (!form.name().equals(store.golferName()))
            {
                store.setGolferName(form.name());
            }
            if (!form.isSample())
            {
                golfanalyst.Settings.load(store.dir()).with(golfanalyst.Settings.BACKUPS_TO_KEEP, form.backupsToKeep())
                        .with(golfanalyst.Settings.BACKUP_COPY_FOLDER, (form.copyFolder() == null ? "" : form.copyFolder().toString()))
                        .save(store.dir());
            }
        }
        catch (IOException failure)
        {
            setStatus("The settings could not be saved: " + failure.getMessage());
            return;
        }
        String message = "Settings saved.";
        Path newFolder = form.newDataFolder();
        if (newFolder != null)
        {
            try
            {
                if (form.copyData())
                {
                    golfanalyst.DataLocation.copyData(store.dir(), newFolder);
                }
                golfanalyst.DataLocation.write(folders.locationFolder(), newFolder);
                message = message + " Golf Analyst will use " + newFolder.toAbsolutePath().normalize()
                        + " the next time it is opened.";
            }
            catch (IOException failure)
            {
                message = message + " The data folder was not changed: " + failure.getMessage();
            }
        }
        if (current.equals(HOME))
        {
            pages.get(HOME).onShow();
        }
        setStatus(message);
    }

    /*
     +--------------------------------------------------------------------------+
     | exportData()                                                             |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | What Home's Export button does: shows the Export window and, if Export   |
     | is pressed, writes a new GolfAnalyst-export_... folder inside the        |
     | chosen folder (Exporter.write). Export only reads the data, so no        |
     | backup is made, nothing in the data folder changes and it works on the   |
     | sample data too. The chosen folder is remembered as LastExportFolder     |
     | (except on the sample data, whose files are never written). The footer   |
     | says "Exported 12 sets of tees and 37 rounds to ...". If the export      |
     | fails, the half-made folder has already been removed; a message says     |
     | why and the footer says nothing was exported.                            |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    @Override
    public void exportData()
    {
        Path last = golfanalyst.Settings.load(store.settingsDir()).lastExportFolder();
        Path start = folders.documentsFolder();
        if (last != null && Files.isDirectory(last))
        {
            start = last;
        }
        ExportForm form = new ExportForm(store.dataset(), store.includeHistorical(), start, settingsTools);
        if (exportAsker.ask(form) != ConfirmForm.GO || form.plan() == null || form.folder() == null)
        {
            return;
        }
        golfanalyst.Exporter.Plan plan = form.plan();
        Path written;
        try
        {
            written = golfanalyst.Exporter.write(store.dir(), form.folder(), plan, java.time.LocalDateTime.now());
        }
        catch (IOException failure)
        {
            showNotice(EXPORT_FAILED_TITLE, "Nothing was exported: " + failure.getMessage(), null);
            setStatus("Nothing was exported: " + failure.getMessage());
            return;
        }
        String message = "Exported " + plan.counts() + " to " + written.toAbsolutePath().normalize() + ".";
        if (!store.isSampleData())
        {
            try
            {
                golfanalyst.Settings.load(store.dir()).with(golfanalyst.Settings.LAST_EXPORT_FOLDER,
                        form.folder().toAbsolutePath().normalize().toString()).save(store.dir());
            }
            catch (IOException failure)
            {
                message = message + " The folder could not be remembered: " + failure.getMessage();
            }
        }
        setStatus(message);
    }

    /*
     * setExportAsker(newAsker): replaces the on-screen Export window with answers given by code (for the self-test).
     */
    void setExportAsker(ExportAsker newAsker)
    {
        exportAsker = newAsker;
    }

    /*
     +--------------------------------------------------------------------------+
     | printReports()                                                           |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | What Home's Print button does: shows the Print window, made from the     |
     | data as it is now and today's date. The window itself sends the chosen   |
     | report through the print tools (the system print dialog) or saves it as  |
     | a spreadsheet file (the save window starts in LastExportFolder, as       |
     | Export's does, else the Documents folder); once it has gone the footer   |
     | says "Sent to the printer." or "Saved 15 rows to ...". Cancel changes    |
     | nothing.                                                                 |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    @Override
    public void printReports()
    {
        golfanalyst.print.ReportData data = golfanalyst.print.ReportData.of(store, java.time.LocalDate.now(), Theme.VERSION);
        Path last = golfanalyst.Settings.load(store.settingsDir()).lastExportFolder();
        Path start = folders.documentsFolder();
        if (last != null && Files.isDirectory(last))
        {
            start = last;
        }
        PrintForm form = new PrintForm(data, start, printTools);
        if (printAsker.ask(form) == ConfirmForm.GO)
        {
            setStatus(form.doneMessage());
        }
    }

    /*
     * setPrintTools(tools, newAsker): replaces the printer and the on-screen Print window with code (for the self-test).
     */
    void setPrintTools(PrintForm.Tools tools, PrintAsker newAsker)
    {
        printTools = tools;
        printAsker = newAsker;
    }

    /*
     +--------------------------------------------------------------------------+
     | ScreenPrintTools                                                         |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | What the Print window gets from the real program window: the system's    |
     | own print dialog and printing (ReportPrintable.send), the system's save  |
     | window, the shared question before replacing a file, the help windows    |
     | and the footer.                                                          |
     +--------------------------------------------------------------------------+
     */
    private final class ScreenPrintTools implements PrintForm.Tools
    {
        /*
         * print(report): the system print dialog, then printing; false if it was cancelled.
         */
        @Override
        public boolean print(golfanalyst.print.Report report) throws java.awt.print.PrinterException
        {
            return golfanalyst.print.ReportPrintable.send(report);
        }

        /*
         * chooseSaveFile(parent, folder, name): the system's save window for a .csv file, opening in folder with the name filled in.
         */
        @Override
        public Path chooseSaveFile(java.awt.Component parent, Path folder, String name)
        {
            javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
            if (folder != null && Files.isDirectory(folder))
            {
                chooser.setCurrentDirectory(folder.toFile());
                chooser.setSelectedFile(folder.resolve(name).toFile());
            }
            else
            {
                chooser.setSelectedFile(new java.io.File(name));
            }
            chooser.setDialogTitle("Save the spreadsheet file");
            chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Spreadsheet files (*.csv)", "csv"));
            if (chooser.showSaveDialog(parent) != javax.swing.JFileChooser.APPROVE_OPTION)
            {
                return null;
            }
            return chooser.getSelectedFile().toPath();
        }

        /*
         * showHelp(parent, fileKey): the help window over the Print window (it is modal, so the header's buttons cannot be reached).
         */
        @Override
        public void showHelp(java.awt.Component parent, String fileKey)
        {
            Window owner = SwingUtilities.getWindowAncestor(parent);
            if (owner == null)
            {
                setStatus("The help window needs the program window.");
                return;
            }
            HelpWindow.show(owner, helpDir, fileKey, "Print");
        }

        /*
         * askReplace(file): the shared Yes / No question (No is the default) before a file that is already there is replaced.
         */
        @Override
        public boolean askReplace(Path file)
        {
            return askYesNo(file.getFileName() + " is already there. Replace it?");
        }

        /*
         * say(text): a footer message.
         */
        @Override
        public void say(String text)
        {
            setStatus(text);
        }
    }

    /*
     +--------------------------------------------------------------------------+
     | ScreenTools                                                              |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | What the Settings window gets from the real program window: the folder   |
     | picker, the system's file manager (Desktop.open), the shared question    |
     | dialog, the sample-data safeguard and the footer.                        |
     +--------------------------------------------------------------------------+
     */
    private final class ScreenTools implements SettingsForm.Tools
    {
        /*
         * chooseFolder(parent, title, start): the system's folder picker, opening at start.
         */
        @Override
        public Path chooseFolder(java.awt.Component parent, String title, Path start)
        {
            javax.swing.JFileChooser chooser = new javax.swing.JFileChooser((start != null && Files.isDirectory(start) ? start.toFile() : null));
            chooser.setDialogTitle(title);
            chooser.setFileSelectionMode(javax.swing.JFileChooser.DIRECTORIES_ONLY);
            return (chooser.showOpenDialog(parent) == javax.swing.JFileChooser.APPROVE_OPTION ? chooser.getSelectedFile().toPath() : null);
        }

        /*
         * open(folder): opens the folder in the system's file manager; false if this computer cannot.
         */
        @Override
        public boolean open(Path folder)
        {
            try
            {
                if (!java.awt.Desktop.isDesktopSupported() || !java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.OPEN))
                {
                    return false;
                }
                java.awt.Desktop.getDesktop().open(folder.toFile());
                return true;
            }
            catch (IOException | RuntimeException cannot)
            {
                return false;
            }
        }

        /*
         * ask(form): shows a question through the confirmer.
         */
        @Override
        public int ask(ConfirmForm form)
        {
            return AppPanel.this.ask(form);
        }

        /*
         * createOwnDataFolder(): the sample-data safeguard: on Yes the golfer's own data folder is made.
         */
        @Override
        public void createOwnDataFolder()
        {
            if (asker.applyAsInt(SAMPLE_QUESTION) == JOptionPane.YES_OPTION)
            {
                makeOwnDataFolder();
            }
        }

        /*
         * say(text): a footer message.
         */
        @Override
        public void say(String text)
        {
            setStatus(text);
        }
    }

    /*
     * setSettingsTools(tools, newAsker): replaces the Settings window's tools and its on-screen showing with code (for the self-test).
     */
    void setSettingsTools(SettingsForm.Tools tools, SettingsAsker newAsker)
    {
        settingsTools = tools;
        settingsAsker = newAsker;
    }

    /*
     * setFolders(newFolders): where the program's files, golfanalyst.location and the own data folder are (fake folders for the self-test).
     */
    void setFolders(golfanalyst.ProgramFolders newFolders)
    {
        folders = newFolders;
    }

    /*
     * setImportAsker(newAsker): replaces the on-screen Import window with answers given by code (for the self-test).
     * Passed in:  newAsker - takes the form and gives its answer
     * Passed out: nothing
     */
    void setImportAsker(ImportAsker newAsker)
    {
        importAsker = newAsker;
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
            int answer = sample.showOn(this);
            if (answer == ConfirmForm.GO)
            {
                return JOptionPane.YES_OPTION;
            }
            return (answer == ConfirmForm.NO ? JOptionPane.NO_OPTION : JOptionPane.CANCEL_OPTION);
        }
        String title = (question.equals(RoundPage.DISCARD_QUESTION) ? DISCARD_TITLE : CONFIRM_TITLE);
        int answer = ConfirmForm.yesNo(question).withTitle(title).showOn(this);
        return (answer == ConfirmForm.GO ? JOptionPane.YES_OPTION : JOptionPane.NO_OPTION);
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
            return ConfirmForm.yesNoCancel(question, true).withTitle(SAMPLE_TITLE);
        }
        if (question.equals(CLEAR_SAMPLE_QUESTION) || question.equals(SAMPLE_QUESTION_INSTALLED) || question.startsWith(OWN_FOLDER_THERE))
        {
            return ConfirmForm.yesNoCancel(question, false).withTitle(SAMPLE_TITLE);
        }
        return null;
    }

    /*
     * page(card): the page shown for a card name such as HOME or STATS (for the picture check and the self-test).
     */
    Page page(String card)
    {
        return pages.get(card);
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

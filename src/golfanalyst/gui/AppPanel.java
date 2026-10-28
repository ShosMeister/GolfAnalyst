package golfanalyst.gui;

import golfanalyst.DataStore;
import golfanalyst.Problem;
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
 |                       browseRounds, newCourse, readyToSave               |
 +--------------------------------------------------------------------------+
 */
@SuppressWarnings({"serial", "this-escape"})
public final class AppPanel extends JPanel implements Shell
{

    public static final String HOME = "home";
    public static final String ROUND = "round";
    public static final String COURSE = "course";
    static final String SAMPLE_QUESTION = "This is the sample data. Create your own data folder first?";

    private final transient DataStore store;
    private final transient Path helpDir;
    private final HeaderBar header = new HeaderBar();
    private final FooterBar footer = new FooterBar();
    private final CardLayout cards = new CardLayout();
    private final JPanel center = new JPanel(cards);
    private final transient Map<String, Page> pages = new LinkedHashMap<>();
    private final RoundPage roundPage;
    private final CoursePage coursePage;
    private String current = HOME;
    private transient ToIntFunction<String> asker = question -> askOnScreen(question);

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
        pages.put(HOME, homePage);
        pages.put(ROUND, roundPage);
        pages.put(COURSE, coursePage);
        center.add(homePage, HOME);
        center.add(roundPage, ROUND);
        center.add(coursePage, COURSE);
        add(header, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);

        header.homeButton().addActionListener(event -> goHome());
        header.helpButton().addActionListener(event -> showHelp(false));
        header.infoButton().addActionListener(event -> showHelp(true));
        footer.setStatusClickAction(() -> showProblems());

        navigate(HOME);
        setStatus(startStatus(startNote));
    }

    /*
     * startStatus(note): the message shown when the program opens: a count of problems found, or the note and how many rounds loaded.
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
        String loaded = store.dataset().rounds().size() + " rounds loaded.";
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
     | Passed in:  pageName - HOME, ROUND or COURSE                             |
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
     * setStatus(text): shows a message in the footer.
     */
    @Override
    public void setStatus(String text)
    {
        footer.setStatus(text);
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
        Path folder = store.dataFolderBeside();
        if (Files.isDirectory(folder))
        {
            setStatus("There is already a data folder at " + folder + ". Close Golf Analyst and open it again to use it.");
            return false;
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
        return false;
    }

    /*
     * askOnScreen(question): asks a Yes / No / Cancel question in a small window over this one.
     * Passed in:  question - the words to ask
     * Passed out: JOptionPane.YES_OPTION, NO_OPTION or CANCEL_OPTION (closing the window counts as Cancel)
     */
    private int askOnScreen(String question)
    {
        int answer = JOptionPane.showConfirmDialog(this, question, "Golf Analyst", JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        return answer == JOptionPane.CLOSED_OPTION ? JOptionPane.CANCEL_OPTION : answer;
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
     * coursePage(): the New Course page (for tests).
     */
    CoursePage coursePage()
    {
        return coursePage;
    }
}

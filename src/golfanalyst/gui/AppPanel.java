package golfanalyst.gui;

import golfanalyst.DataStore;
import golfanalyst.Problem;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Window;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
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
 | Public Methods:                                                          |
 | navigate(pageName)  - shows a page                                       |
 | the Shell methods   - store, setStatus, refreshTitle, goHome, newRound,  |
 |                       browseRounds                                       |
 +--------------------------------------------------------------------------+
 */
@SuppressWarnings({"serial", "this-escape"})
public final class AppPanel extends JPanel implements Shell
{

    public static final String HOME = "home";
    public static final String ROUND = "round";

    private final transient DataStore store;
    private final transient Path helpDir;
    private final HeaderBar header = new HeaderBar();
    private final FooterBar footer = new FooterBar();
    private final CardLayout cards = new CardLayout();
    private final JPanel center = new JPanel(cards);
    private final transient Map<String, Page> pages = new LinkedHashMap<>();
    private final RoundPage roundPage;
    private String current = HOME;

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
        pages.put(HOME, homePage);
        pages.put(ROUND, roundPage);
        center.add(homePage, HOME);
        center.add(roundPage, ROUND);
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
        long problems = store.dataset().problems().size();
        if (problems > 0)
        {
            return problems + " problem(s) found in the data files - click here to see them.";
        }
        String loaded = store.dataset().rounds().size() + " rounds loaded.";
        return note.isEmpty() ? "Ready.  " + loaded : note + "  " + loaded;
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
     | Passed in:  pageName - HOME or ROUND                                     |
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
}

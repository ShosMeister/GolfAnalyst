package golfanalyst.gui;

import golfanalyst.DataStore;
import golfanalyst.Handicap;
import golfanalyst.Stats;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/*
 +--------------------------------------------------------------------------+
 | HomePage                                                                 |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | The first screen: your handicap on the left, your overall statistics on  |
 | the right and the action buttons in the middle, over the photograph of   |
 | the golfers on the tee. All of the figures come from the loaded data and |
 | are calculated, never typed in. Click the golfer's name to change it.    |
 | The "Include historical rounds" box at the bottom of Golfer's Stats      |
 | decides whether the statistics and round counts include historical       |
 | rounds; the handicap figures never do.                                   |
 |                                                                          |
 | The buttons are laid out in two columns: data entry on the left (New     |
 | Course, New Round, Edit Round, Import, Clear) and viewing on the right   |
 | (Rounds, Stats Board, Analyzer, Best Round, Print). The grid fills       |
 | across each row, so the order they are added in is row by row. Only New  |
 | Course, New Round, Edit Round and Rounds work now; the others are in     |
 | place and say so in the footer until they are built.                     |
 |                                                                          |
 | Public Methods:                                                          |
 | onShow() - refreshes every figure from the data                          |
 +--------------------------------------------------------------------------+
 */
@SuppressWarnings({"serial", "this-escape"})
public final class HomePage extends Page
{

    private static final int LEFT_PANEL_WIDTH = 300;
    private static final int RIGHT_PANEL_WIDTH = 330;
    private static final int CHECK_PICTURE = 18;
    static final String INCLUDE_HISTORICAL_TIP = "Count old (historical) rounds in your averages, percentages, the Stats Board and Best Round. "
            + "They never count toward your Handicap Index.";
    final ValueBox handicapIndex = new ValueBox();
    final ValueBox lowIndex = new ValueBox();
    final ValueBox totalRounds = new ValueBox();
    final ValueBox rounds18 = new ValueBox();
    final ValueBox rounds9 = new ValueBox();
    final ValueBox scoresUsed = new ValueBox();
    final ValueBox golferName = new ValueBox();
    final ValueBox average18 = new ValueBox();
    final ValueBox average9 = new ValueBox();
    final ValueBox personalBest = new ValueBox();
    final ValueBox fairways = new ValueBox();
    final ValueBox sandSaves = new ValueBox();
    final ValueBox greens = new ValueBox();
    final ValueBox twoPutt = new ValueBox();
    final ValueBox includeCell = new ValueBox();
    final ImageCheckBox includeHistorical = new ImageCheckBox();

    /*
     +--------------------------------------------------------------------------+
     | HomePage(shell)                                                          |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Builds the two wood panels and the button grid. The "Include             |
     | historical rounds" box is set by onShow, not here, because the page      |
     | must not ask the window for anything while it is being built.            |
     |                                                                          |
     | Passed in:  shell - the window this page sits in                         |
     | Passed out: the page                                                     |
     +--------------------------------------------------------------------------+
     */
    public HomePage(Shell shell)
    {
        super(shell, "home");
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(18, 20, 14, 20));

        WoodPanel left = new WoodPanel("Handicap");
        left.addRow("Handicap Index", handicapIndex);
        left.addRow("Low Index (365 days)", lowIndex);
        left.addRow("Total Rounds", totalRounds);
        left.addRow("18-Hole Rounds", rounds18);
        left.addRow("9-Hole Rounds", rounds9);
        left.addRow("Rounds Used", scoresUsed);
        left.setPreferredSize(new Dimension(LEFT_PANEL_WIDTH, left.getPreferredSize().height));

        golferName.setPreferredSize(new Dimension(150, 26));
        golferName.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        golferName.setToolTipText("Click to change the golfer's name");
        golferName.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent event)
            {
                changeGolferName();
            }
        });
        WoodPanel right = new WoodPanel("Golfer's Stats");
        right.addRow("Name", golferName);
        right.addRow("Average (18 holes)", average18);
        right.addRow("Average (9 holes)", average9);
        right.addRow("Personal Best", personalBest);
        right.addRow("Fairways Hit", fairways);
        right.addRow("Sand Saves", sandSaves);
        right.addRow("Greens In Reg", greens);
        right.addRow("Two-Putt", twoPutt);
        includeHistorical.setPictureSize(CHECK_PICTURE);
        includeHistorical.setToolTipText(INCLUDE_HISTORICAL_TIP);
        includeHistorical.setOnChange(() -> changeIncludeHistorical());
        includeCell.setLayout(new BorderLayout());
        includeCell.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 4));
        includeCell.setToolTipText(INCLUDE_HISTORICAL_TIP);
        includeCell.add(includeHistorical, BorderLayout.EAST);
        right.addRow("Include historical rounds", includeCell);
        right.setPreferredSize(new Dimension(RIGHT_PANEL_WIDTH, right.getPreferredSize().height));

        JPanel buttons = new JPanel(new GridLayout(0, 2, 12, 12));
        buttons.setOpaque(false);
        buttons.add(actionButton("New Course", () -> shell.newCourse()));
        buttons.add(actionButton("Rounds", () -> shell.browseRounds()));
        buttons.add(actionButton("New Round", () -> shell.newRound()));
        buttons.add(actionButton("Stats Board", () -> shell.statsBoard()));
        buttons.add(actionButton("Edit Round", () -> shell.editRound()));
        buttons.add(actionButton("Analyzer", () -> shell.setStatus("Analyzer is not built yet.")));
        buttons.add(actionButton("Import", () -> shell.setStatus("Import is not built yet.")));
        buttons.add(actionButton("Best Round", () -> shell.bestRound()));
        buttons.add(actionButton("Clear", () -> shell.setStatus("Clear is not built yet.")));
        buttons.add(actionButton("Print", () -> shell.setStatus("Print is not built yet.")));
        JPanel buttonColumn = new JPanel(new BorderLayout());
        buttonColumn.setOpaque(false);
        buttonColumn.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));
        buttonColumn.add(buttons, BorderLayout.NORTH);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(left, BorderLayout.WEST);
        top.add(buttonColumn, BorderLayout.CENTER);
        top.add(right, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);
    }

    /*
     * actionButton(text, action): makes one of the wood buttons.
     * Passed in:  text - the words on it, action - what it does when pressed
     * Passed out: the button
     */
    private WoodButton actionButton(String text, Runnable action)
    {
        WoodButton button = new WoodButton(text);
        button.addActionListener(event -> action.run());
        return button;
    }

    /*
     * title(): the header text for this screen.
     */
    @Override
    public String title()
    {
        return "Golf Analyst Stats Home";
    }

    /*
     * helpKey(): the start of this screen's help file names (home-info.txt and home-help.txt).
     */
    @Override
    public String helpKey()
    {
        return "home";
    }

    /*
     * oneDecimal(value): formats a number to one decimal place, or "-" when there is no data.
     * Passed in:  value - the number (NaN means no data)
     * Passed out: text like "94.3", or "-"
     */
    private static String oneDecimal(double value)
    {
        return Double.isNaN(value) ? "-" : String.format("%.1f", value);
    }

    /*
     * percent(value): formats a percentage to one decimal place with a % sign, or "-" when there is no data.
     * Passed in:  value - the percentage, 0 to 100 (NaN means no data)
     * Passed out: text like "43.8%", or "-"
     */
    private static String percent(double value)
    {
        return Double.isNaN(value) ? "-" : String.format("%.1f%%", value);
    }

    /*
     +--------------------------------------------------------------------------+
     | onShow()                                                                 |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Fills every box from the data store: handicap, rounds counted, and       |
     | the overall statistics, and ticks the "Include historical rounds" box    |
     | from the setting. Called each time the page is shown, so a round saved   |
     | a moment ago is already included.                                        |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    @Override
    public void onShow()
    {
        DataStore store = shell.store();
        Handicap.Result handicap = store.handicap();
        Stats.Summary all = store.overall();
        int scoreCount = handicap.currentDifferentials().size();

        handicapIndex.setValue(Handicap.format(handicap.currentIndex()));
        lowIndex.setValue(Handicap.format(handicap.lowIndex()));
        totalRounds.setValue(Integer.toString(all.rounds()));
        rounds18.setValue(Integer.toString(all.rounds18()));
        rounds9.setValue(Integer.toString(all.rounds9()));
        scoresUsed.setValue(Handicap.differentialsUsed(scoreCount) + " of " + scoreCount);

        String name = store.golferName();
        golferName.setValue(name.isEmpty() ? "(click to enter)" : name);
        average18.setValue(oneDecimal(all.avgScore18()));
        average9.setValue(oneDecimal(all.avgScore9()));
        String best18 = all.bestScore18() == 0 ? "-" : Integer.toString(all.bestScore18());
        String best9 = all.bestScore9() == 0 ? "-" : Integer.toString(all.bestScore9());
        personalBest.setValue(best18 + " / " + best9);
        fairways.setValue(percent(all.fairwayPct()));
        sandSaves.setValue(percent(all.sandSavePct()));
        greens.setValue(percent(all.girPct()));
        twoPutt.setValue(percent(all.twoPuttPct()));
        includeHistorical.setSelected(store.includeHistorical());
    }

    /*
     +--------------------------------------------------------------------------+
     | changeGolferName()                                                       |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Asks for the golfer's name and keeps it in settings.txt in the data      |
     | folder. Cancelling leaves the name as it was.                            |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    private void changeGolferName()
    {
        Object answer = JOptionPane.showInputDialog(this, "Golfer's name:", "Golfer",
                JOptionPane.PLAIN_MESSAGE, null, null, shell.store().golferName());
        if (answer == null)
        {
            return;
        }
        try
        {
            shell.store().setGolferName(answer.toString().trim());
        }
        catch (IOException failure)
        {
            shell.setStatus("The name could not be saved: " + failure.getMessage());
        }
        onShow();
    }
    /*
     +--------------------------------------------------------------------------+
     | changeIncludeHistorical()                                                |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | What a click on the "Include historical rounds" box does: saves the      |
     | choice in settings.txt and fills the figures again at once. Ticked,      |
     | the statistics and round counts include historical rounds; unticked,     |
     | they leave them out. The Handicap Index, Low Index and Rounds Used       |
     | never include them. If the setting cannot be saved the box goes back     |
     | and the footer says why.                                                 |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    private void changeIncludeHistorical()
    {
        try
        {
            shell.store().setIncludeHistorical(includeHistorical.isSelected());
        }
        catch (IOException failure)
        {
            shell.setStatus("The setting could not be saved: " + failure.getMessage());
        }
        onShow();
    }
}

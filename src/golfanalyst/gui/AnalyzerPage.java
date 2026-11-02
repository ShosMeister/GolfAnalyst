package golfanalyst.gui;

import golfanalyst.BestRound;
import golfanalyst.CourseAnalysis;
import golfanalyst.DataStore;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FocusTraversalPolicy;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingConstants;

/*
 +--------------------------------------------------------------------------+
 | AnalyzerPage                                                             |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | The Analyzer screen: how you play each hole of one course and tees, from |
 | the figures CourseAnalysis works out. Across the top a light bar like    |
 | Best Round's: the Course / tee pulldown (the same rows and default as    |
 | Best Round) and the Counts, Percent and Averages buttons. Below it,      |
 | centred over the photo: a wood frame of WoodTag captions (108 x 22 in    |
 | 24 px rows) and, 10 px to its right, a wood frame holding a white grid   |
 | of 36 x 24 cells (13 pt, hairlines under and right of every cell): one   |
 | column per hole of the row (1-18, 1-9 or 10-18), then Out and In (18-    |
 | hole rows only) and All in light grey. Under both, a wood frame with the |
 | teak showing inside holds the summary (Rounds, Avg score, Best score;    |
 | Putts / round, Fairways, Greens; Sand saves, Hardest holes in red, Best  |
 | holes in gold) and Close at its right end, which goes Home.              |
 |                                                                          |
 | Width: caption frame 9 + 2 + 108 + 2 + 9 = 130, gap 10, grid frame       |
 | 9 + 2 + 21 x 36 + 2 + 9 = 778: 918 of the 921 px inside the page's 12 px |
 | edges. Height: at most 11 rows, 11 x 24 + 4 + 18 = 286.                  |
 |                                                                          |
 | Views: COUNTS and PERCENT show Hole, Hole par, Rounds and the seven      |
 | results (percentages rounded to whole numbers; Out, In and All are of    |
 | the holes played); AVERAGES shows Avg score, Avg vs par, Best, Worst,    |
 | Avg putts, Fairway %, Greens % and Sand saves %. A zero count or         |
 | percentage, or a figure with nothing to show, is a faint grey "-".       |
 | With 2 rounds or more the three hardest holes (most strokes over par)    |
 | have red hole numbers and the three best gold ones (never both); in      |
 | Averages their Avg vs par cells are tinted too.                          |
 |                                                                          |
 | Nothing here asks the window for anything while the page is built: the   |
 | pulldown is filled by onShow.                                            |
 |                                                                          |
 | Public Methods:                                                          |
 | onShow() - fills the pulldown (latest round's row, Counts) and shows it  |
 | the pulldown, buttons and cells (for the self-test)                      |
 +--------------------------------------------------------------------------+
 */
@SuppressWarnings({"serial", "this-escape"})
public final class AnalyzerPage extends Page
{

    static final String TITLE = "Analyzer";
    static final String NO_ROUNDS_YET = "There are no rounds yet.";
    static final String TINT_NOTE = " Red marks the holes where you lose the most strokes, gold your best holes.";
    static final Color HARD_TINT = new Color(255, 214, 208);
    static final Color BEST_TINT = new Color(255, 236, 150);
    static final Color TOTAL_FACE = new Color(236, 236, 236);
    static final Color FAINT = new Color(170, 165, 155);
    static final String DASH = "-";
    static final int CELL_WIDTH = 36;
    static final int CELL_HEIGHT = 24;
    static final int TAG_WIDTH = 108;
    static final int TAG_HEIGHT = 22;
    static final int FRAME_GAP = 10;
    static final int PAGE_EDGE = 12;
    static final int TINTED = 3;
    static final int MIN_ROUNDS_TO_TINT = 2;
    static final List<String> COUNT_ROWS = List.of("Hole", "Hole par", "Rounds", "Dbl Eagle", "Eagle", "Birdie", "Par", "Bogey",
            "Dbl Bogey", "Other");
    static final List<String> AVERAGE_ROWS = List.of("Hole", "Hole par", "Rounds", "Avg score", "Avg vs par", "Best", "Worst",
            "Avg putts", "Fairway %", "Greens %", "Sand saves %");
    static final int AVG_VS_PAR_ROW = 4;
    private static final int PAD = 2;
    private static final float CELL_FONT = 13f;
    private static final int VALUE_HEIGHT = 22;
    final JComboBox<BestRound.Choice> rowCombo = new JComboBox<>();
    final JRadioButton countsButton = new JRadioButton("Counts", true);
    final JRadioButton percentButton = new JRadioButton("Percent");
    final JRadioButton averagesButton = new JRadioButton("Averages");
    final WoodButton closeButton = new WoodButton("Close");
    final JLabel roundsValue = valueLabel(70);
    final JLabel avgScoreValue = valueLabel(70);
    final JLabel bestScoreValue = valueLabel(70);
    final JLabel puttsValue = valueLabel(70);
    final JLabel fairwaysValue = valueLabel(70);
    final JLabel greensValue = valueLabel(70);
    final JLabel sandValue = valueLabel(70);
    final JLabel hardestValue = valueLabel(110);
    final JLabel bestHolesValue = valueLabel(110);
    final WoodFrame captionFrame = new WoodFrame();
    final WoodFrame gridFrame = new WoodFrame();
    final WoodFrame summaryFrame = new WoodFrame();
    private final JPanel captionColumn = new JPanel(new GridBagLayout());
    private final JPanel grid = new JPanel(new GridBagLayout());
    private final List<String> captions = new ArrayList<>();
    private final List<List<JLabel>> cells = new ArrayList<>();
    private transient CourseAnalysis.Analysis analysis = null;
    private final List<Integer> hardHoles = new ArrayList<>();
    private final List<Integer> bestHoles = new ArrayList<>();
    private String title = TITLE;
    private boolean updating = false;

    /*
     +--------------------------------------------------------------------------+
     | AnalyzerPage(shell)                                                      |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Builds the bar, the two grid frames and the summary frame (see the class |
     | comment), shows an empty 18-hole grid of dashes, and wires the controls. |
     | It does not ask the window for anything.                                 |
     |                                                                          |
     | Passed in:  shell - the window this page sits in                         |
     | Passed out: the page                                                     |
     +--------------------------------------------------------------------------+
     */
    public AnalyzerPage(Shell shell)
    {
        super(shell, "analyzer");
        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(10, PAGE_EDGE, 6, PAGE_EDGE));
        add(buildSelectorBar(), BorderLayout.NORTH);

        captionColumn.setOpaque(false);
        captionColumn.setBorder(BorderFactory.createEmptyBorder(PAD, PAD, PAD, PAD));
        captionFrame.add(captionColumn, BorderLayout.CENTER);
        grid.setOpaque(false);
        grid.setBorder(BorderFactory.createEmptyBorder(PAD, PAD, PAD, PAD));
        gridFrame.add(grid, BorderLayout.CENTER);
        buildSummary();

        JPanel group = new JPanel(new GridBagLayout());
        group.setOpaque(false);
        GridBagConstraints at = new GridBagConstraints();
        at.gridx = 0;
        at.gridy = 0;
        at.anchor = GridBagConstraints.NORTH;
        group.add(captionFrame, at);
        at.gridx = 1;
        at.insets = new Insets(0, FRAME_GAP, 0, 0);
        group.add(gridFrame, at);
        at.gridx = 0;
        at.gridy = 1;
        at.gridwidth = 2;
        at.fill = GridBagConstraints.HORIZONTAL;
        at.insets = new Insets(8, 0, 0, 0);
        group.add(summaryFrame, at);
        JPanel middle = new JPanel(new GridBagLayout());
        middle.setOpaque(false);
        GridBagConstraints centre = new GridBagConstraints();
        centre.anchor = GridBagConstraints.NORTH;
        centre.weighty = 1.0;
        middle.add(group, centre);
        add(middle, BorderLayout.CENTER);

        rowCombo.addActionListener(event -> changed());
        for (JRadioButton button : new JRadioButton[] {countsButton, percentButton, averagesButton})
        {
            button.addActionListener(event -> changed());
        }
        closeButton.addActionListener(event -> shell.goHome());
        setFocusTraversalPolicyProvider(true);
        setFocusTraversalPolicy(new BarTraversal());
        fill();
    }

    /*
     +--------------------------------------------------------------------------+
     | buildSelectorBar()                                                       |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The light bar across the top, like Best Round's: "Course / tee:", the    |
     | pulldown and the Counts, Percent and Averages buttons (Counts ticked).   |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: the bar                                                      |
     +--------------------------------------------------------------------------+
     */
    private JPanel buildSelectorBar()
    {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        bar.setBackground(Theme.PANEL_BODY);
        bar.setBorder(BorderFactory.createLineBorder(Theme.FRAME_DARK, 1));
        bar.add(new JLabel("Course / tee:"));
        rowCombo.setPreferredSize(new Dimension(330, 26));
        rowCombo.setToolTipText("The course and tees to analyse");
        bar.add(rowCombo);
        bar.add(Box.createHorizontalStrut(30));
        ButtonGroup views = new ButtonGroup();
        for (JRadioButton button : new JRadioButton[] {countsButton, percentButton, averagesButton})
        {
            button.setOpaque(false);
            views.add(button);
            bar.add(button);
        }
        countsButton.setToolTipText("How many times you made each score on every hole");
        percentButton.setToolTipText("The same, as a percentage of the rounds played on each hole");
        averagesButton.setToolTipText("Average score, putts, fairways, greens and sand saves on every hole");
        return bar;
    }

    /*
     +--------------------------------------------------------------------------+
     | buildSummary()                                                           |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The summary frame under the grid, teak showing inside: three columns of  |
     | caption and value (Rounds, Avg score, Best score; Putts / round,         |
     | Fairways, Greens; Sand saves, Hardest holes on red, Best holes on gold)  |
     | and Close (112 x 32) at its right end.                                   |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    private void buildSummary()
    {
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        String[][] names = {{"Rounds", "Putts / round", "Sand saves"}, {"Avg score", "Fairways", "Hardest holes"},
            {"Best score", "Greens", "Best holes"}};
        JLabel[][] values = {{roundsValue, puttsValue, sandValue}, {avgScoreValue, fairwaysValue, hardestValue},
            {bestScoreValue, greensValue, bestHolesValue}};
        for (int col = 0; col < names.length; col = col + 1)
        {
            for (int row = 0; row < names[col].length; row = row + 1)
            {
                GridBagConstraints at = new GridBagConstraints();
                at.gridx = col * 2;
                at.gridy = row;
                at.insets = new Insets((row == 0 ? 0 : 2), 0, 0, 3);
                form.add(tag(names[col][row]), at);
                at.gridx = col * 2 + 1;
                at.insets = new Insets((row == 0 ? 0 : 2), 0, 0, 14);
                form.add(values[col][row], at);
            }
        }
        hardestValue.setBackground(HARD_TINT);
        bestHolesValue.setBackground(BEST_TINT);
        hardestValue.setToolTipText("The holes where you lose the most strokes to par");
        bestHolesValue.setToolTipText("The holes where you lose the fewest strokes to par");
        GridBagConstraints space = new GridBagConstraints();
        space.gridx = 6;
        space.gridy = 0;
        space.weightx = 1.0;
        form.add(Box.createHorizontalStrut(1), space);
        Dimension closeSize = new Dimension(112, 32);
        closeButton.setPreferredSize(closeSize);
        closeButton.setMinimumSize(closeSize);
        closeButton.setToolTipText("Back to the Home screen");
        GridBagConstraints right = new GridBagConstraints();
        right.gridx = 7;
        right.gridy = 0;
        right.gridheight = 3;
        right.anchor = GridBagConstraints.EAST;
        form.add(closeButton, right);
        summaryFrame.add(form, BorderLayout.CENTER);
    }

    /*
     * tag(text): a 108 x 22 wood caption.
     */
    private static WoodTag tag(String text)
    {
        WoodTag caption = new WoodTag(text);
        Dimension size = new Dimension(TAG_WIDTH, TAG_HEIGHT);
        caption.setPreferredSize(size);
        caption.setMinimumSize(size);
        return caption;
    }

    /*
     * valueLabel(width): a summary value: a white bold 13 pt cell with hairlines, width x 22.
     */
    private static JLabel valueLabel(int width)
    {
        JLabel label = new JLabel(DASH, SwingConstants.CENTER);
        label.setOpaque(true);
        label.setBackground(Color.WHITE);
        label.setForeground(Theme.FIELD_TEXT);
        label.setFont(Theme.font(Font.BOLD, CELL_FONT));
        label.setBorder(BorderFactory.createLineBorder(Theme.GRID_LINE, 1));
        Dimension size = new Dimension(width, VALUE_HEIGHT);
        label.setPreferredSize(size);
        label.setMinimumSize(size);
        return label;
    }

    // ------------------------------------------------------------------ showing

    /*
     +--------------------------------------------------------------------------+
     | onShow()                                                                 |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Opens the screen afresh: fills the pulldown with every course, tees and  |
     | kind of round you have played (Best Round's list), picks the one of      |
     | your latest round, ticks Counts and shows it.                            |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    @Override
    public void onShow()
    {
        DataStore store = shell.store();
        BestRound.Choice latest = CourseAnalysis.latestRow(store.roundsByDate(), store.teeMap(), store.includeHistorical());
        updating = true;
        rowCombo.removeAllItems();
        for (BestRound.Choice row : CourseAnalysis.rows(store.roundsByDate(), store.teeMap(), store.includeHistorical()))
        {
            rowCombo.addItem(row);
            if (latest != null && row.key().equals(latest.key()))
            {
                rowCombo.setSelectedItem(row);
            }
        }
        countsButton.setSelected(true);
        updating = false;
        changed();
    }

    /*
     +--------------------------------------------------------------------------+
     | changed()                                                                |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | What a change of row or view does: works out the row's figures, picks    |
     | the tinted holes, fills the grid and the summary, and puts the count     |
     | and the tint note (and the Stats Board's note when historical rounds     |
     | are left out) in the footer. With no rounds the pulldown and buttons go  |
     | off and the grid shows dashes.                                           |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    private void changed()
    {
        if (updating)
        {
            return;
        }
        DataStore store = shell.store();
        BestRound.Choice row = (BestRound.Choice) rowCombo.getSelectedItem();
        boolean any = row != null;
        rowCombo.setEnabled(any);
        countsButton.setEnabled(any);
        percentButton.setEnabled(any);
        averagesButton.setEnabled(any);
        String leftOut = (store.includeHistorical() ? "" : StatsPage.HISTORICAL_LEFT_OUT);
        hardHoles.clear();
        bestHoles.clear();
        if (!any)
        {
            analysis = null;
            title = TITLE;
            fill();
            shell.setStatus(NO_ROUNDS_YET + leftOut);
            shell.refreshTitle();
            return;
        }
        analysis = CourseAnalysis.analyse(row, store.roundsByDate(), store.teeMap(), store.includeHistorical());
        int rounds = analysis.rounds().size();
        if (rounds >= MIN_ROUNDS_TO_TINT)
        {
            hardHoles.addAll(analysis.hardestHoles(TINTED));
            for (int hole : analysis.bestHoles(analysis.holes().size()))
            {
                if (bestHoles.size() < TINTED && !hardHoles.contains(hole))
                {
                    bestHoles.add(hole);
                }
            }
        }
        title = TITLE + ": " + row.label();
        fill();
        shell.setStatus(rounds + (rounds == 1 ? " round on " : " rounds on ") + row.label() + "."
                + (hardHoles.isEmpty() ? "" : TINT_NOTE) + leftOut);
        shell.refreshTitle();
    }

    /*
     +--------------------------------------------------------------------------+
     | fill()                                                                   |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Builds the captions and the grid for the view that is ticked, from the   |
     | figures worked out (or an 18-hole grid of dashes with none), tints the   |
     | hardest and best holes, and fills the summary.                           |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    private void fill()
    {
        boolean averages = averagesButton.isSelected();
        captions.clear();
        captions.addAll((averages ? AVERAGE_ROWS : COUNT_ROWS));
        captionColumn.removeAll();
        for (int row = 0; row < captions.size(); row = row + 1)
        {
            JPanel holder = new JPanel(new GridBagLayout());
            holder.setOpaque(false);
            fixSize(holder, TAG_WIDTH, CELL_HEIGHT);
            holder.add(tag(captions.get(row)), new GridBagConstraints());
            GridBagConstraints at = new GridBagConstraints();
            at.gridx = 0;
            at.gridy = row;
            captionColumn.add(holder, at);
        }

        List<String> headings = new ArrayList<>();
        List<CourseAnalysis.HoleStats> holes = (analysis == null ? List.of() : analysis.holes());
        List<CourseAnalysis.Totals> totals = new ArrayList<>();
        if (analysis == null)
        {
            for (int hole = 1; hole <= golfanalyst.Tee.HOLES; hole = hole + 1)
            {
                headings.add(Integer.toString(hole));
            }
            headings.addAll(List.of(CourseAnalysis.OUT, CourseAnalysis.IN, CourseAnalysis.ALL));
        }
        else
        {
            for (CourseAnalysis.HoleStats hole : holes)
            {
                headings.add(Integer.toString(hole.hole()));
            }
            if (analysis.out() != null)
            {
                totals.add(analysis.out());
                totals.add(analysis.in());
            }
            totals.add(analysis.all());
            for (CourseAnalysis.Totals total : totals)
            {
                headings.add(total.label());
            }
        }
        int holeColumns = headings.size() - (analysis == null ? 3 : totals.size());
        grid.removeAll();
        cells.clear();
        for (int row = 0; row < captions.size(); row = row + 1)
        {
            List<JLabel> line = new ArrayList<>();
            for (int col = 0; col < headings.size(); col = col + 1)
            {
                String text;
                if (row == 0)
                {
                    text = headings.get(col);
                }
                else if (analysis == null)
                {
                    text = DASH;
                }
                else if (col < holeColumns)
                {
                    text = holeText(holes.get(col), row, averages);
                }
                else
                {
                    text = totalText(totals.get(col - holeColumns), row, averages);
                }
                JLabel cell = gridCell(text, (row == 0), (col >= holeColumns));
                if (col < holeColumns && (row == 0 || (averages && row == AVG_VS_PAR_ROW)))
                {
                    int hole = Integer.parseInt(headings.get(col));
                    if (hardHoles.contains(hole))
                    {
                        cell.setBackground(HARD_TINT);
                    }
                    else if (bestHoles.contains(hole))
                    {
                        cell.setBackground(BEST_TINT);
                    }
                }
                GridBagConstraints at = new GridBagConstraints();
                at.gridx = col;
                at.gridy = row;
                grid.add(cell, at);
                line.add(cell);
            }
            cells.add(line);
        }
        fillSummary();
        revalidate();
        repaint();
    }

    /*
     +--------------------------------------------------------------------------+
     | holeText(hole, row, averages)                                            |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The words in one hole's cell for a grid row (row 0 is the heading).      |
     | Counts and percentages: a zero is "-"; percentages are rounded to whole  |
     | numbers. Averages: one decimal for score and putts, a sign and one       |
     | decimal for vs par, whole numbers for the percentages, "-" with nothing  |
     | to show.                                                                 |
     |                                                                          |
     | Passed in:  hole     - the hole's figures                                |
     |             row      - the grid row                                      |
     |             averages - true for the Averages view                        |
     | Passed out: the text                                                     |
     +--------------------------------------------------------------------------+
     */
    private String holeText(CourseAnalysis.HoleStats hole, int row, boolean averages)
    {
        if (row == 1)
        {
            return Integer.toString(hole.par());
        }
        if (row == 2)
        {
            return count(hole.rounds());
        }
        if (!averages)
        {
            int category = row - 3;
            return (percentButton.isSelected() ? percent(hole.percents()[category], true) : count(hole.counts()[category]));
        }
        switch (row)
        {
            case 3:
                return decimal(hole.avgScore());
            case 4:
                return (Double.isNaN(hole.avgOverPar()) ? DASH : String.format("%+.1f", hole.avgOverPar()));
            case 5:
                return count(hole.best());
            case 6:
                return count(hole.worst());
            case 7:
                return decimal(hole.avgPutts());
            case 8:
                return percent(hole.fairwayPct(), false);
            case 9:
                return percent(hole.girPct(), false);
            default:
                return percent(hole.sandPct(), false);
        }
    }

    /*
     * totalText(total, row, averages): the words in an Out, In or All cell, like holeText but with vs par as a whole number with a sign so it fits.
     */
    private String totalText(CourseAnalysis.Totals total, int row, boolean averages)
    {
        if (row == 1)
        {
            return Integer.toString(total.par());
        }
        if (row == 2)
        {
            return count(total.rounds());
        }
        if (!averages)
        {
            int category = row - 3;
            return (percentButton.isSelected() ? percent(total.percents()[category], true) : count(total.counts()[category]));
        }
        switch (row)
        {
            case 3:
                return decimal(total.avgScore());
            case 4:
                return (Double.isNaN(total.avgOverPar()) ? DASH : String.format("%+d", Math.round(total.avgOverPar())));
            case 5:
                return count(total.best());
            case 6:
                return count(total.worst());
            case 7:
                return decimal(total.avgPutts());
            case 8:
                return percent(total.fairwayPct(), false);
            case 9:
                return percent(total.girPct(), false);
            default:
                return percent(total.sandPct(), false);
        }
    }

    /*
     * count(value): a whole number, or "-" for zero.
     */
    private static String count(int value)
    {
        return (value == 0 ? DASH : Integer.toString(value));
    }

    /*
     * decimal(value): one decimal place, or "-" for nothing to show.
     */
    private static String decimal(double value)
    {
        return (Double.isNaN(value) ? DASH : String.format("%.1f", value));
    }

    /*
     * percent(value, zeroIsDash): a percentage rounded to a whole number, "-" for nothing to show (and for exactly zero when zeroIsDash).
     */
    private static String percent(double value, boolean zeroIsDash)
    {
        if (Double.isNaN(value) || (zeroIsDash && value == 0.0))
        {
            return DASH;
        }
        return Long.toString(Math.round(value));
    }

    /*
     * gridCell(text, heading, total): one 36 x 24 cell: white (light grey for Out, In and All), 13 pt (bold for the heading row), hairlines under and right; "-" is faint grey.
     */
    private static JLabel gridCell(String text, boolean heading, boolean total)
    {
        JLabel cell = new JLabel(text, SwingConstants.CENTER);
        cell.setOpaque(true);
        cell.setBackground((total ? TOTAL_FACE : Color.WHITE));
        cell.setForeground((DASH.equals(text) ? FAINT : Theme.FIELD_TEXT));
        cell.setFont(Theme.font((heading ? Font.BOLD : Font.PLAIN), CELL_FONT));
        cell.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, Theme.GRID_LINE));
        fixSize(cell, CELL_WIDTH, CELL_HEIGHT);
        return cell;
    }

    /*
     * fixSize(part, width, height): keeps a part at one size.
     */
    private static void fixSize(JComponent part, int width, int height)
    {
        Dimension size = new Dimension(width, height);
        part.setPreferredSize(size);
        part.setMinimumSize(size);
        part.setMaximumSize(size);
    }

    /*
     * fillSummary(): the summary values for the row (dashes with no rounds; the hole lists only when tinted).
     */
    private void fillSummary()
    {
        CourseAnalysis.Summary summary = (analysis == null ? null : analysis.summary());
        boolean any = summary != null && summary.rounds() > 0;
        roundsValue.setText((any ? Integer.toString(summary.rounds()) : DASH));
        avgScoreValue.setText((any ? decimal(summary.avgScore()) : DASH));
        bestScoreValue.setText((any ? count(summary.bestScore()) : DASH));
        puttsValue.setText((any ? decimal(summary.puttsPerRound()) : DASH));
        fairwaysValue.setText((any ? withPercent(summary.fairwayPct()) : DASH));
        greensValue.setText((any ? withPercent(summary.girPct()) : DASH));
        sandValue.setText((any ? withPercent(summary.sandPct()) : DASH));
        hardestValue.setText(holeList(hardHoles));
        bestHolesValue.setText(holeList(bestHoles));
    }

    /*
     * withPercent(value): "40%", or "-" for nothing to show.
     */
    private static String withPercent(double value)
    {
        return (Double.isNaN(value) ? DASH : Math.round(value) + "%");
    }

    /*
     * holeList(holes): "2, 15, 12", or "-" for none.
     */
    private static String holeList(List<Integer> holes)
    {
        if (holes.isEmpty())
        {
            return DASH;
        }
        List<String> numbers = new ArrayList<>();
        holes.forEach(hole -> numbers.add(Integer.toString(hole)));
        return String.join(", ", numbers);
    }

    // ------------------------------------------------------------- for the window

    /*
     * title(): "Analyzer: Edgewater:Senior", or "Analyzer" with no rounds.
     */
    @Override
    public String title()
    {
        return title;
    }

    /*
     * helpKey(): the start of the help file names (analyzer-info.txt and analyzer-help.txt).
     */
    @Override
    public String helpKey()
    {
        return "analyzer";
    }

    // --------------------------------------------------------------- for tests

    /*
     * captions(): the caption of each grid row, top to bottom.
     */
    List<String> captions()
    {
        return List.copyOf(captions);
    }

    /*
     * cell(row, col): one grid cell (row 0 is the hole numbers).
     */
    JLabel cell(int row, int col)
    {
        return cells.get(row).get(col);
    }

    /*
     * columnCount(): how many columns the grid has (holes, then Out, In, All).
     */
    int columnCount()
    {
        return (cells.isEmpty() ? 0 : cells.get(0).size());
    }

    /*
     * analysis(): the figures shown, or null with no rounds.
     */
    CourseAnalysis.Analysis analysis()
    {
        return analysis;
    }

    /*
     * gridPanel(): the panel of cells (for the self-test's layout check).
     */
    Container gridPanel()
    {
        return grid;
    }

    /*
     +--------------------------------------------------------------------------+
     | BarTraversal                                                             |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Tab order: the pulldown, Counts, Percent, Averages, then Close, round    |
     | again; parts that are switched off are skipped.                          |
     +--------------------------------------------------------------------------+
     */
    private final class BarTraversal extends FocusTraversalPolicy
    {
        /*
         * order(): the parts in Tab order.
         */
        private List<Component> order()
        {
            return List.of(rowCombo, countsButton, percentButton, averagesButton, closeButton);
        }

        /*
         * step(from, way): the next usable part from one part, forward (1) or back (-1).
         */
        private Component step(Component from, int way)
        {
            List<Component> parts = order();
            int place = Math.max(0, parts.indexOf(from));
            for (int lp = 1; lp <= parts.size(); lp = lp + 1)
            {
                Component next = parts.get(Math.floorMod(place + way * lp, parts.size()));
                if (next.isEnabled() && next.isVisible())
                {
                    return next;
                }
            }
            return closeButton;
        }

        /*
         * getComponentAfter(root, part): the next part.
         */
        @Override
        public Component getComponentAfter(Container root, Component part)
        {
            return step(part, 1);
        }

        /*
         * getComponentBefore(root, part): the part before.
         */
        @Override
        public Component getComponentBefore(Container root, Component part)
        {
            return step(part, -1);
        }

        /*
         * getFirstComponent(root): the first usable part.
         */
        @Override
        public Component getFirstComponent(Container root)
        {
            return step(closeButton, 1);
        }

        /*
         * getLastComponent(root): Close.
         */
        @Override
        public Component getLastComponent(Container root)
        {
            return closeButton;
        }

        /*
         * getDefaultComponent(root): the first usable part.
         */
        @Override
        public Component getDefaultComponent(Container root)
        {
            return getFirstComponent(root);
        }
    }
}

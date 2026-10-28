package golfanalyst.gui;

import golfanalyst.DataLoader;
import golfanalyst.DataStore;
import golfanalyst.Handicap;
import golfanalyst.Round;
import golfanalyst.Stats;
import golfanalyst.Tee;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FocusTraversalPolicy;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import javax.swing.text.DocumentFilter;

/*
 +--------------------------------------------------------------------------+
 | RoundPage                                                                |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | The screen for entering a new round and for looking back at saved ones.  |
 | It has two tables, Out (holes 1-9) and In (holes 10-18), each hole with  |
 | its par, fairway and sand check boxes, putts and score. A column on the  |
 | right counts eagles, birdies, pars, bogeys, double bogeys and others,    |
 | and a form at the bottom shows the date, totals and the round's figures. |
 |                                                                          |
 | Everything under "calculated" is worked out as you type and cannot be    |
 | edited: par comes from the course, the result of each hole from the      |
 | score, and the round's greens in regulation, sand saves, two-putts,      |
 | differential and the index after the round from the rules. Fairways are  |
 | not counted on par 3s, so that box is switched off for them.             |
 |                                                                          |
 | Two modes:                                                               |
 | NEW  - the boxes are open for typing; Save adds the round to rounds.tsv  |
 | VIEW - a saved round is shown read-only; the four arrow buttons step     |
 |        through the saved rounds (First, Previous, Next, Last). The       |
 |        Show drop-down limits them to one course, one course and its      |
 |        tees, or all rounds.                                              |
 |                                                                          |
 | A round can be 18 holes, the front nine or the back nine of an 18-hole   |
 | course, or a nine-hole course. Holes not being played are greyed.        |
 |                                                                          |
 | Public Methods:                                                          |
 | showNew()          - starts a new round                                  |
 | showRoundAt(index) - shows a saved round (0 is the oldest)               |
 | showLatest()       - shows the newest saved round, or a new round if none|
 +--------------------------------------------------------------------------+
 */
@SuppressWarnings({"serial", "this-escape"})
public final class RoundPage extends Page
{

    private static final int HOLES = Tee.HOLES;
    private static final int NINE = Tee.NINE;
    private static final int MAX_SCORE = 30;
    private static final int MAX_PUTTS = 15;
    private static final int MIN_PCC = -1;
    private static final int MAX_PCC = 3;
    private static final int RESULT_BUCKETS = 6;
    private static final String[] RESULT_NAMES = {"Eagles", "Birdies", "Pars", "Bogeys", "DBogeys", "Others"};
    private static final int NAV_INDENT = 250;
    private static final int ROW_HEIGHT = 24;

    private enum Mode
    {
        NEW, VIEW
    }

    /**
     * One entry of the Show drop-down: which saved rounds the arrows step through. course is null for
     * all rounds; teeId is -1 for all the tees of that course.
     */
    record FilterItem(String label, String course, int teeId)
    {
        @Override
        public String toString()
        {
            return label;
        }
    }

    /** One entry of the course/tee drop-down: shows "00006: Jacksons Chase (Beginner)". */
    private record TeeItem(Tee tee)
    {
        @Override
        public String toString()
        {
            return String.format("%05d: %s", tee.id(), tee.label());
        }
    }

    final JComboBox<TeeItem> teeCombo = new JComboBox<>();
    final JComboBox<String> holesCombo = new JComboBox<>();
    final JTextField pccField = new JTextField("0", 2);
    final JTextField dateField = new JTextField(9);
    final JLabel[] holeLabels = new JLabel[HOLES];
    final ValueBox[] parBoxes = new ValueBox[HOLES];
    final ImageCheckBox[] fairwayChecks = new ImageCheckBox[HOLES];
    final ImageCheckBox[] sandChecks = new ImageCheckBox[HOLES];
    final JTextField[] puttFields = new JTextField[HOLES];
    final JTextField[] scoreFields = new JTextField[HOLES];
    final ValueBox outPutts = new ValueBox();
    final ValueBox outScore = new ValueBox();
    final ValueBox inPutts = new ValueBox();
    final ValueBox inScore = new ValueBox();
    final ValueBox ratingBox = new ValueBox();
    final ValueBox[] resultBoxes = new ValueBox[RESULT_BUCKETS];
    final ValueBox scoreBox = new ValueBox();
    final ValueBox puttsBox = new ValueBox();
    final ValueBox fairwayBox = new ValueBox();
    final ValueBox sandBox = new ValueBox();
    final ValueBox girBox = new ValueBox();
    final ValueBox twoPuttBox = new ValueBox();
    final ValueBox differentialBox = new ValueBox();
    final ValueBox indexBox = new ValueBox();
    final WoodButton saveButton = new WoodButton("Save");
    final WoodButton cancelButton = new WoodButton("Cancel");
    final JComboBox<FilterItem> filterCombo = new JComboBox<>();
    final JLabel filterLabel = new JLabel("Show:");
    final IconButton firstButton = new IconButton("First.png", "First round", Theme.NAV_ICON_SIZE);
    final IconButton previousButton = new IconButton("Previous.png", "Previous round", Theme.NAV_ICON_SIZE);
    final IconButton nextButton = new IconButton("Next.png", "Next round", Theme.NAV_ICON_SIZE);
    final IconButton lastButton = new IconButton("Last.png", "Last round", Theme.NAV_ICON_SIZE);

    private final transient EntryTraversal focusPolicy = new EntryTraversal();
    private Mode mode = Mode.NEW;
    private int viewIndex = -1;
    private final List<Integer> shown = new ArrayList<>();
    private String filterCourse = null;
    private int filterTeeId = -1;
    private int viewedRoundId = -1;
    private boolean updating = false;
    private int playedStart = 0;
    private int playedCount = HOLES;

    /*
     +--------------------------------------------------------------------------+
     | RoundPage(shell)                                                         |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Builds the screen: the course/date bar across the top, the two hole      |
     | tables and the count column, the form at the bottom and the four         |
     | arrow buttons, then starts in NEW mode.                                  |
     |                                                                          |
     | Passed in:  shell - the window this page sits in                         |
     | Passed out: the page                                                     |
     +--------------------------------------------------------------------------+
     */
    public RoundPage(Shell shell)
    {
        super(shell, "round");
        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 14, 6, 14));

        add(buildSelectorBar(), BorderLayout.NORTH);

        JPanel tables = new JPanel(new GridBagLayout());
        tables.setOpaque(false);
        GridBagConstraints at = new GridBagConstraints();
        at.fill = GridBagConstraints.BOTH;
        at.weightx = 1.0;
        at.insets = new Insets(0, 0, 0, 10);
        at.gridx = 0;
        tables.add(buildHalf(0, outPutts, outScore, "Out"), at);
        at.gridx = 1;
        tables.add(buildHalf(NINE, inPutts, inScore, "In"), at);
        at.gridx = 2;
        at.weightx = 0.0;
        at.insets = new Insets(0, 0, 0, 0);
        tables.add(buildCountColumn(), at);
        JPanel tablesTop = new JPanel(new BorderLayout());
        tablesTop.setOpaque(false);
        tablesTop.add(tables, BorderLayout.NORTH);
        add(tablesTop, BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout(0, 6));
        south.setOpaque(false);
        south.add(buildBottomForm(), BorderLayout.CENTER);
        south.add(buildNavRow(), BorderLayout.SOUTH);
        add(south, BorderLayout.SOUTH);

        wireActions();
        setFocusTraversalPolicyProvider(true);
        setFocusTraversalPolicy(focusPolicy);
        prepareNew();
        updateNav();
    }

    // ------------------------------------------------------------------ building

    /*
     +--------------------------------------------------------------------------+
     | buildSelectorBar()                                                       |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The light bar across the top: which course and tees, how many holes,     |
     | and the Playing Conditions Calculation (normally 0).                     |
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
        bar.add(teeCombo);
        bar.add(new JLabel("Holes:"));
        bar.add(holesCombo);
        bar.add(new JLabel("PCC:"));
        bar.add(pccField);
        bar.add(filterLabel);
        filterCombo.setPreferredSize(new Dimension(310, 26));
        filterCombo.setToolTipText("Which saved rounds the arrow buttons step through");
        bar.add(filterCombo);
        pccField.setHorizontalAlignment(SwingConstants.CENTER);
        pccField.setToolTipText("Playing Conditions Calculation for the day, -1 to 3. Normally 0.");
        pccField.getDocument().addDocumentListener(new ChangeWatcher(() -> recalc()));
        return bar;
    }

    /*
     +--------------------------------------------------------------------------+
     | entryField(isScore, hole)                                                |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Makes a score or putts box. Only digits can be typed. A putts box        |
     | takes one digit (0 to 9). A score box takes one digit too, except        |
     | that a 1 waits for a second digit (10 to 30), because it could be a      |
     | hole in one or the start of a score like 12. As soon as the box is       |
     | complete the cursor moves on by itself, following the order set in       |
     | focusOrder: down the scores of every hole, then down the putts.          |
     | Enter and Tab move on in the same order, and clicking or tabbing         |
     | into a box selects what is in it so the next digit typed replaces it.    |
     |                                                                          |
     | In a putts box the letters F and S are not typed; they tick or untick    |
     | the Fwy and Sand boxes of that hole, like the F and S on your            |
     | scorecard. Any other letter is ignored.                                  |
     |                                                                          |
     | Passed in:  isScore - true for a score box, false for a putts box        |
     |             hole    - the hole this box belongs to, 0 for hole 1         |
     | Passed out: the text box                                                 |
     +--------------------------------------------------------------------------+
     */
    private JTextField entryField(boolean isScore, int hole)
    {
        JTextField field = new JTextField(3);
        field.setHorizontalAlignment(SwingConstants.CENTER);
        field.setFont(Theme.font(Font.BOLD, 15f));
        field.setPreferredSize(new Dimension(54, ROW_HEIGHT));
        Consumer<Character> markerKey = isScore ? null : letter -> toggleMarker(hole, letter);
        ((AbstractDocument) field.getDocument()).setDocumentFilter(new EntryFilter(isScore, () -> updating,
                () -> SwingUtilities.invokeLater(() -> field.transferFocus()), markerKey));
        field.getDocument().addDocumentListener(new ChangeWatcher(() -> recalc()));
        field.addActionListener(event -> field.transferFocus());
        field.addFocusListener(new FocusAdapter()
        {
            @Override
            public void focusGained(FocusEvent event)
            {
                SwingUtilities.invokeLater(() -> field.selectAll());
            }
        });
        return field;
    }

    /*
     +--------------------------------------------------------------------------+
     | toggleMarker(hole, letter)                                               |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Handles the F and S keys typed in a putts box: F ticks or unticks the    |
     | fairway box of that hole (not on a par 3, where it is switched off),     |
     | and S does the same for the sand box.                                    |
     |                                                                          |
     | Passed in:  hole   - the hole, 0 for hole 1                              |
     |             letter - the key typed, in capitals                          |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    private void toggleMarker(int hole, char letter)
    {
        ImageCheckBox box = letter == 'F' ? fairwayChecks[hole] : (letter == 'S' ? sandChecks[hole] : null);
        if (box != null && box.isEnabled())
        {
            box.setSelected(!box.isSelected());
            recalc();
        }
    }

    /*
     +--------------------------------------------------------------------------+
     | buildHalf(firstHole, puttTotal, scoreTotal, totalLabel)                  |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Builds one hole-by-hole table (Out or In) inside a wooden frame:         |
     | a heading row, nine hole rows and a totals row.                          |
     |                                                                          |
     | Passed in:  firstHole  - 0 for holes 1-9, 9 for holes 10-18              |
     |             puttTotal  - box for the putts total of this half            |
     |             scoreTotal - box for the score total of this half            |
     |             totalLabel - "Out" or "In"                                   |
     | Passed out: the framed table                                             |
     +--------------------------------------------------------------------------+
     */
    private WoodFrame buildHalf(int firstHole, ValueBox puttTotal, ValueBox scoreTotal, String totalLabel)
    {
        JPanel grid = new JPanel(new GridBagLayout());
        grid.setBackground(Color.WHITE);
        grid.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        GridBagConstraints at = new GridBagConstraints();
        at.insets = new Insets(1, 2, 1, 2);
        at.fill = GridBagConstraints.HORIZONTAL;
        String[] headings = {"Hole", "Par", "Score", "Putt", "Fwy", "Sand"};
        for (int col = 0; col < headings.length; col++)
        {
            at.gridx = col;
            at.gridy = 0;
            JLabel heading = new JLabel(headings[col], SwingConstants.CENTER);
            heading.setFont(Theme.font(Font.BOLD, 13f));
            heading.setOpaque(true);
            heading.setBackground(Theme.FIELD_FACE);
            grid.add(heading, at);
        }
        for (int row = 0; row < NINE; row++)
        {
            int hole = firstHole + row;
            at.weighty = 1.0;
            at.gridy = row + 1;
            holeLabels[hole] = new JLabel(Integer.toString(hole + 1), SwingConstants.CENTER);
            holeLabels[hole].setFont(Theme.font(Font.BOLD, 15f));
            parBoxes[hole] = new ValueBox();
            parBoxes[hole].setHorizontalAlignment(SwingConstants.CENTER);
            parBoxes[hole].setPreferredSize(new Dimension(38, ROW_HEIGHT));
            fairwayChecks[hole] = new ImageCheckBox();
            fairwayChecks[hole].setOnChange(() -> recalc());
            sandChecks[hole] = new ImageCheckBox();
            sandChecks[hole].setOnChange(() -> recalc());
            puttFields[hole] = entryField(false, hole);
            scoreFields[hole] = entryField(true, hole);
            at.gridx = 0;
            grid.add(holeLabels[hole], at);
            at.gridx = 1;
            grid.add(parBoxes[hole], at);
            at.gridx = 2;
            grid.add(scoreFields[hole], at);
            at.gridx = 3;
            grid.add(puttFields[hole], at);
            at.gridx = 4;
            grid.add(fairwayChecks[hole], at);
            at.gridx = 5;
            grid.add(sandChecks[hole], at);
        }
        at.weighty = 0.0;
        at.gridy = NINE + 1;
        at.gridx = 0;
        at.gridwidth = 2;
        JLabel total = new JLabel(totalLabel, SwingConstants.RIGHT);
        total.setFont(Theme.font(Font.BOLD, 16f));
        grid.add(total, at);
        at.gridwidth = 1;
        for (ValueBox box : new ValueBox[] {puttTotal, scoreTotal})
        {
            box.setHorizontalAlignment(SwingConstants.CENTER);
            box.setPreferredSize(new Dimension(48, ROW_HEIGHT));
        }
        at.gridx = 2;
        grid.add(scoreTotal, at);
        at.gridx = 3;
        grid.add(puttTotal, at);
        WoodFrame frame = new WoodFrame();
        frame.add(grid, BorderLayout.CENTER);
        return frame;
    }

    /*
     +--------------------------------------------------------------------------+
     | buildCountColumn()                                                       |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The wooden column on the right: the course rating and slope, then how    |
     | many eagles, birdies, pars, bogeys, double bogeys and others the         |
     | round has.                                                               |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: the framed column                                            |
     +--------------------------------------------------------------------------+
     */
    private WoodFrame buildCountColumn()
    {
        JPanel column = new JPanel(new GridBagLayout());
        column.setOpaque(false);
        GridBagConstraints at = new GridBagConstraints();
        at.gridx = 0;
        at.fill = GridBagConstraints.HORIZONTAL;
        at.insets = new Insets(0, 0, 0, 0);
        at.gridy = 0;
        column.add(new WoodTag("Rating"), at);
        at.gridy = 1;
        at.insets = new Insets(0, 0, 6, 0);
        ratingBox.setHorizontalAlignment(SwingConstants.CENTER);
        ratingBox.setPreferredSize(new Dimension(104, ROW_HEIGHT));
        column.add(ratingBox, at);
        for (int lp = 0; lp < RESULT_BUCKETS; lp++)
        {
            at.gridy = 2 + 2 * lp;
            at.insets = new Insets(0, 0, 0, 0);
            column.add(new WoodTag(RESULT_NAMES[lp]), at);
            resultBoxes[lp] = new ValueBox();
            resultBoxes[lp].setHorizontalAlignment(SwingConstants.CENTER);
            resultBoxes[lp].setPreferredSize(new Dimension(104, ROW_HEIGHT));
            at.gridy = 3 + 2 * lp;
            at.insets = new Insets(0, 0, 6, 0);
            column.add(resultBoxes[lp], at);
        }
        WoodFrame frame = new WoodFrame();
        frame.add(column, BorderLayout.CENTER);
        return frame;
    }

    /*
     * addPair(form, tag, box, row, column): puts a wood caption and its value box side by side in the bottom form.
     * Passed in:  form - the panel, tag - caption text, box - the box, row/column - grid position
     * Passed out: nothing
     */
    private void addPair(JPanel form, String tag, JComponent box, int row, int column)
    {
        GridBagConstraints at = new GridBagConstraints();
        at.gridy = row;
        at.fill = GridBagConstraints.HORIZONTAL;
        at.insets = new Insets(1, 3, 1, 0);
        at.gridx = column * 2;
        at.weightx = 0.0;
        form.add(new WoodTag(tag), at);
        at.gridx = column * 2 + 1;
        at.insets = new Insets(1, 0, 1, 12);
        form.add(box, at);
    }

    /*
     +--------------------------------------------------------------------------+
     | buildBottomForm()                                                        |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The wooden form under the tables. Left: the date and the round's score   |
     | and putts. Middle: fairways, sand saves, greens in regulation and        |
     | two-putts for this round. Right: the 18-hole differential and the        |
     | index after the round, then the Save and Cancel buttons.                 |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: the framed form                                              |
     +--------------------------------------------------------------------------+
     */
    private WoodFrame buildBottomForm()
    {
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        dateField.setHorizontalAlignment(SwingConstants.CENTER);
        dateField.setFont(Theme.font(Font.BOLD, 14f));
        dateField.setToolTipText("The date played, as 2026-10-22 or 10/22/2026");
        dateField.getDocument().addDocumentListener(new ChangeWatcher(() -> recalc()));
        for (ValueBox box : new ValueBox[] {scoreBox, puttsBox, fairwayBox, sandBox, girBox, twoPuttBox, differentialBox, indexBox})
        {
            box.setHorizontalAlignment(SwingConstants.CENTER);
            box.setPreferredSize(new Dimension(84, ROW_HEIGHT));
        }
        addPair(form, "Date", dateField, 0, 0);
        addPair(form, "Score", scoreBox, 1, 0);
        addPair(form, "Putts", puttsBox, 2, 0);
        addPair(form, "Fairway", fairwayBox, 0, 1);
        addPair(form, "Sand Saves", sandBox, 1, 1);
        addPair(form, "Greens In Reg", girBox, 2, 1);
        addPair(form, "2-Putt", twoPuttBox, 3, 1);
        addPair(form, "18-Hole Diff", differentialBox, 0, 2);
        addPair(form, "Index After", indexBox, 1, 2);
        saveButton.setPreferredSize(new Dimension(110, 32));
        cancelButton.setPreferredSize(new Dimension(110, 32));
        GridBagConstraints at = new GridBagConstraints();
        at.gridx = 6;
        at.gridy = 0;
        at.gridheight = 4;
        at.weightx = 1.0;
        at.anchor = GridBagConstraints.EAST;
        JPanel buttons = new JPanel(new GridBagLayout());
        buttons.setOpaque(false);
        GridBagConstraints inside = new GridBagConstraints();
        inside.gridy = 0;
        inside.insets = new Insets(0, 0, 8, 0);
        buttons.add(saveButton, inside);
        inside.gridy = 1;
        inside.insets = new Insets(0, 0, 0, 0);
        buttons.add(cancelButton, inside);
        form.add(buttons, at);
        WoodFrame frame = new WoodFrame();
        frame.add(form, BorderLayout.CENTER);
        return frame;
    }

    /*
     * buildNavRow(): the First / Previous / Next / Last arrows, set in from the left so they clear the version text.
     * Passed in:  nothing
     * Passed out: the row
     */
    private JPanel buildNavRow()
    {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(0, NAV_INDENT, 0, 0));
        row.add(firstButton);
        row.add(previousButton);
        row.add(nextButton);
        row.add(lastButton);
        return row;
    }

    /*
     * wireActions(): connects the combo boxes, arrows and the Save and Cancel buttons to what they do.
     * Passed in:  nothing
     * Passed out: nothing
     */
    private void wireActions()
    {
        teeCombo.addActionListener(event ->
        {
            if (!updating)
            {
                rebuildHolesChoices(selectedTee());
                applyHoles();
                recalc();
            }
        });
        holesCombo.addActionListener(event ->
        {
            if (!updating)
            {
                applyHoles();
                recalc();
            }
        });
        filterCombo.addActionListener(event ->
        {
            FilterItem item = (FilterItem) filterCombo.getSelectedItem();
            if (!updating && item != null)
            {
                filterCourse = item.course();
                filterTeeId = item.teeId();
                rebuildShown();
                goTo(shown.size() - 1);
            }
        });
        firstButton.addActionListener(event -> goTo(0));
        previousButton.addActionListener(event -> goTo(mode == Mode.NEW ? shown.size() - 1 : positionShown() - 1));
        nextButton.addActionListener(event -> goTo(positionShown() + 1));
        lastButton.addActionListener(event -> goTo(shown.size() - 1));
        saveButton.addActionListener(event -> save());
        cancelButton.addActionListener(event -> shell.goHome());
    }

    // ------------------------------------------------------------ focus order

    /*
     * usable(component): whether the cursor can land on a component: it is shown, switched on and focusable, and a text box must be one you can type in.
     * Passed in:  component - the component
     * Passed out: true if it can take the cursor
     */
    private static boolean usable(Component component)
    {
        boolean typable = !(component instanceof JTextComponent) || ((JTextComponent) component).isEditable();
        return component.isVisible() && component.isEnabled() && component.isFocusable() && typable;
    }

    /*
     +--------------------------------------------------------------------------+
     | focusOrder()                                                             |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Lists, in the order the cursor visits them, everything on the screen     |
     | it can land on: the course, holes and PCC boxes, the Show box, then      |
     | the score of every hole being played in turn (hole 1 down to hole 9,     |
     | then hole 10 down to hole 18), then the putts of every hole in the       |
     | same order, then the date, the Save and Cancel buttons and the four      |
     | arrows. Holes that are not being played, and boxes that cannot be        |
     | typed in, are left out. Swing's own order goes by position on the        |
     | screen, which would jump between the Out and In tables.                  |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: the list, in visiting order                                  |
     +--------------------------------------------------------------------------+
     */
    private List<Component> focusOrder()
    {
        List<Component> order = new ArrayList<>();
        Component[] before = {teeCombo, holesCombo, pccField, filterCombo};
        for (Component component : before)
        {
            if (usable(component))
            {
                order.add(component);
            }
        }
        for (JTextField[] column : new JTextField[][] {scoreFields, puttFields})
        {
            for (int hole = 0; hole < HOLES; hole++)
            {
                if (usable(column[hole]))
                {
                    order.add(column[hole]);
                }
            }
        }
        Component[] after = {dateField, saveButton, cancelButton, firstButton, previousButton, nextButton, lastButton};
        for (Component component : after)
        {
            if (usable(component))
            {
                order.add(component);
            }
        }
        return order;
    }

    /*
     * focusOrderFirstEntry(): the score box of the first hole being played, or null if there is none.
     */
    private Component focusOrderFirstEntry()
    {
        for (int hole = 0; hole < HOLES; hole++)
        {
            if (usable(scoreFields[hole]))
            {
                return scoreFields[hole];
            }
        }
        return null;
    }

    /*
     * componentAfter(component): where the cursor goes after a component, following focusOrder (for the self-test).
     * Passed in:  component - the component the cursor is in
     * Passed out: the next component in the order
     */
    Component componentAfter(Component component)
    {
        return focusPolicy.getComponentAfter(this, component);
    }

    /*
     +--------------------------------------------------------------------------+
     | EntryTraversal                                                           |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Makes Tab, Shift+Tab, Enter and the automatic move after a digit all     |
     | follow focusOrder, so the cursor goes down the scores and then down      |
     | the putts however the boxes are laid out on the screen.                  |
     +--------------------------------------------------------------------------+
     */
    private final class EntryTraversal extends FocusTraversalPolicy
    {
        /*
         * getComponentAfter(root, component): the next component in the order, wrapping round at the end.
         */
        @Override
        public Component getComponentAfter(Container root, Component component)
        {
            List<Component> order = focusOrder();
            if (order.isEmpty())
            {
                return null;
            }
            int at = order.indexOf(component);
            return order.get((at + 1) % order.size());
        }

        /*
         * getComponentBefore(root, component): the previous component in the order, wrapping round at the start.
         */
        @Override
        public Component getComponentBefore(Container root, Component component)
        {
            List<Component> order = focusOrder();
            if (order.isEmpty())
            {
                return null;
            }
            int at = order.indexOf(component);
            return order.get(at <= 0 ? order.size() - 1 : at - 1);
        }

        /*
         * getFirstComponent(root): the first component in the order.
         */
        @Override
        public Component getFirstComponent(Container root)
        {
            List<Component> order = focusOrder();
            return order.isEmpty() ? null : order.get(0);
        }

        /*
         * getLastComponent(root): the last component in the order.
         */
        @Override
        public Component getLastComponent(Container root)
        {
            List<Component> order = focusOrder();
            return order.isEmpty() ? null : order.get(order.size() - 1);
        }

        /*
         * getDefaultComponent(root): where the cursor starts: the first hole's score when entering a round.
         */
        @Override
        public Component getDefaultComponent(Container root)
        {
            Component first = focusOrderFirstEntry();
            return first != null ? first : getFirstComponent(root);
        }
    }

    // ------------------------------------------------------------------- titles

    /*
     * title(): "New Round" while entering, or "00006:Jacksons Chase:Beginner" for a saved round.
     */
    @Override
    public String title()
    {
        if (mode == Mode.NEW)
        {
            return "New Round";
        }
        Round round = shell.store().roundsByDate().get(viewIndex);
        Tee tee = shell.store().teeMap().get(round.teeId());
        return String.format("%05d:%s:%s", round.id(), tee.courseName(), tee.teeName());
    }

    /*
     * helpKey(): the start of the help file names: round-new-info.txt and round-new-help.txt while entering a round, round-view-info.txt and round-view-help.txt while looking at saved ones.
     */
    @Override
    public String helpKey()
    {
        return mode == Mode.NEW ? "round-new" : "round-view";
    }

    /*
     * onShow(): refreshes the arrow buttons each time the page is shown, and when a new round is being entered puts the cursor in the score of the first hole being played.
     */
    @Override
    public void onShow()
    {
        updateNav();
        if (mode == Mode.NEW)
        {
            Component first = focusOrderFirstEntry();
            if (first != null)
            {
                SwingUtilities.invokeLater(() -> first.requestFocusInWindow());
            }
        }
    }

    /*
     * roundCount(): how many rounds are saved.
     */
    private int roundCount()
    {
        return shell.store().roundsByDate().size();
    }

    /*
     * selectedTee(): the tee chosen in the drop-down, or null if there are none.
     */
    private Tee selectedTee()
    {
        TeeItem item = (TeeItem) teeCombo.getSelectedItem();
        return item == null ? null : item.tee();
    }

    // -------------------------------------------------------------------- modes

    /*
     +--------------------------------------------------------------------------+
     | fillTeeChoices(preferredTeeId)                                           |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Fills the course/tee drop-down from the loaded data and selects one.     |
     |                                                                          |
     | Passed in:  preferredTeeId - the tee to select, or -1 to select the      |
     |                              tee of the latest round (or the first)      |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    private void fillTeeChoices(int preferredTeeId)
    {
        DataStore store = shell.store();
        int wanted = preferredTeeId;
        if (wanted < 0 && roundCount() > 0)
        {
            wanted = store.roundsByDate().get(roundCount() - 1).teeId();
        }
        teeCombo.removeAllItems();
        for (Tee tee : store.dataset().tees())
        {
            TeeItem item = new TeeItem(tee);
            teeCombo.addItem(item);
            if (tee.id() == wanted)
            {
                teeCombo.setSelectedItem(item);
            }
        }
    }

    /*
     * rebuildHolesChoices(tee): offers 18 holes / front nine / back nine for an 18-hole course, or just 9 holes for a nine-hole one.
     * Passed in:  tee - the chosen course/tee (may be null)
     * Passed out: nothing
     */
    private void rebuildHolesChoices(Tee tee)
    {
        boolean wasUpdating = updating;
        updating = true;
        holesCombo.removeAllItems();
        if (tee != null && tee.holes() == Tee.NINE)
        {
            holesCombo.addItem("9 holes");
        }
        else if (tee != null)
        {
            holesCombo.addItem("18 holes");
            holesCombo.addItem("Front nine");
            holesCombo.addItem("Back nine");
        }
        updating = wasUpdating;
    }

    /*
     +--------------------------------------------------------------------------+
     | applyHoles()                                                             |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Works out which holes are being played from the course and the Holes     |
     | choice, then sets up every row: its par, whether it can be typed in,     |
     | and whether it is greyed out because it is not being played. Fairways    |
     | are switched off on par 3s. Anything typed on a hole that is no longer   |
     | played is cleared.                                                       |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    private void applyHoles()
    {
        Tee tee = selectedTee();
        int choice = Math.max(0, holesCombo.getSelectedIndex());
        playedStart = 0;
        playedCount = tee == null ? 0 : tee.holes();
        if (tee != null && tee.holes() == HOLES && choice == 1)
        {
            playedCount = NINE;
        }
        else if (tee != null && tee.holes() == HOLES && choice == 2)
        {
            playedStart = NINE;
            playedCount = NINE;
        }
        boolean typing = mode == Mode.NEW;
        for (int hole = 0; hole < HOLES; hole++)
        {
            boolean onTee = tee != null && hole < tee.holes();
            boolean played = onTee && hole >= playedStart && hole < playedStart + playedCount;
            parBoxes[hole].setValue(onTee ? Integer.toString(tee.pars()[hole]) : "");
            holeLabels[hole].setEnabled(played);
            scoreFields[hole].setEnabled(played);
            puttFields[hole].setEnabled(played);
            scoreFields[hole].setEditable(typing);
            puttFields[hole].setEditable(typing);
            sandChecks[hole].setEnabled(played && typing);
            fairwayChecks[hole].setEnabled(played && typing && tee.pars()[hole] != 3);
            if (!played)
            {
                boolean wasUpdating = updating;
                updating = true;
                scoreFields[hole].setText("");
                puttFields[hole].setText("");
                updating = wasUpdating;
                sandChecks[hole].setSelected(false);
                fairwayChecks[hole].setSelected(false);
            }
            else if (tee.pars()[hole] == 3)
            {
                fairwayChecks[hole].setSelected(false);
            }
        }
        for (int hole = 0; hole < HOLES; hole++)
        {
            mark(scoreFields[hole], false);
            mark(puttFields[hole], false);
        }
        if (tee == null)
        {
            ratingBox.setValue("");
        }
        else if (tee.isRated(playedStart, playedCount))
        {
            ratingBox.setValue(String.format("%.1f / %d", tee.ratingFor(playedStart, playedCount),
                    tee.slopeFor(playedStart, playedCount)));
        }
        else
        {
            ratingBox.setValue("no rating");
        }
    }

    /*
     * clearEntries(): empties every score and putts box and unticks every check box.
     */
    private void clearEntries()
    {
        boolean wasUpdating = updating;
        updating = true;
        for (int hole = 0; hole < HOLES; hole++)
        {
            scoreFields[hole].setText("");
            puttFields[hole].setText("");
            sandChecks[hole].setSelected(false);
            fairwayChecks[hole].setSelected(false);
        }
        updating = wasUpdating;
    }

    /*
     * setMode(newMode): switches between typing (NEW) and looking (VIEW): which controls are open and what the buttons say.
     * Passed in:  newMode - NEW or VIEW
     * Passed out: nothing
     */
    private void setMode(Mode newMode)
    {
        mode = newMode;
        boolean typing = newMode == Mode.NEW;
        teeCombo.setEnabled(typing);
        holesCombo.setEnabled(typing);
        pccField.setEditable(typing);
        dateField.setEditable(typing);
        saveButton.setVisible(typing);
        filterLabel.setVisible(!typing);
        filterCombo.setVisible(!typing);
        cancelButton.setText(typing ? "Cancel" : "Close");
        mark(pccField, false);
        mark(dateField, false);
    }

    /*
     +--------------------------------------------------------------------------+
     | prepareNew()                                                             |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Sets the screen up as a blank new round: the tee of the latest round     |
     | is chosen, the date is today, nothing is ticked, and every box is        |
     | open for typing. It does not talk to the window (no title or status      |
     | change), so it is safe to call while the page is still being built;      |
     | the window is not ready to be called at that point.                      |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    private void prepareNew()
    {
        updating = true;
        setMode(Mode.NEW);
        viewIndex = -1;
        viewedRoundId = -1;
        fillTeeChoices(-1);
        rebuildFilter();
        rebuildHolesChoices(selectedTee());
        dateField.setText(LocalDate.now().toString());
        pccField.setText("0");
        clearEntries();
        applyHoles();
        updating = false;
    }

    /*
     +--------------------------------------------------------------------------+
     | showNew()                                                                |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Starts a blank round (see prepareNew), then refreshes the arrows,        |
     | the calculated boxes, the footer message and the header title.           |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    public void showNew()
    {
        prepareNew();
        updateNav();
        recalc();
        shell.refreshTitle();
    }

    /*
     +--------------------------------------------------------------------------+
     | showRoundAt(index)                                                       |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Shows a saved round, read-only: its course, holes, date and every        |
     | hole's score, putts, fairway and sand. The calculated boxes are filled   |
     | from the data, so they show what the handicap history recorded.          |
     |                                                                          |
     | Passed in:  index - which saved round, 0 for the oldest; an index        |
     |                     outside the saved rounds is ignored                  |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    public void showRoundAt(int index)
    {
        if (index < 0 || index >= roundCount())
        {
            return;
        }
        if (shown.isEmpty() || !shown.contains(index))
        {
            filterCourse = null;
            filterTeeId = -1;
            rebuildFilter();
        }
        Round round = shell.store().roundsByDate().get(index);
        Tee tee = shell.store().teeMap().get(round.teeId());
        updating = true;
        setMode(Mode.VIEW);
        viewIndex = index;
        viewedRoundId = round.id();
        fillTeeChoices(round.teeId());
        rebuildHolesChoices(tee);
        int choice = 0;
        if (round.isNineHole() && tee.holes() == HOLES)
        {
            choice = round.startHole() == 0 ? 1 : 2;
        }
        holesCombo.setSelectedIndex(choice);
        dateField.setText(round.date().toString());
        pccField.setText(Integer.toString(round.pcc()));
        clearEntries();
        applyHoles();
        for (int lp = 0; lp < round.holes(); lp++)
        {
            int hole = round.startHole() + lp;
            scoreFields[hole].setText(Integer.toString(round.scores()[lp]));
            puttFields[hole].setText(Integer.toString(round.putts()[lp]));
            fairwayChecks[hole].setSelected(round.fairway()[lp]);
            sandChecks[hole].setSelected(round.sand()[lp]);
        }
        updating = false;
        updateNav();
        recalc();
        shell.setStatus("Round " + (positionShown() + 1) + " of " + shown.size() + filterDescription());
        shell.refreshTitle();
    }

    /*
     * showLatest(): shows the newest saved round, or starts a new round if there are none.
     * Passed in:  nothing
     * Passed out: nothing
     */
    public void showLatest()
    {
        rebuildFilter();
        if (shown.isEmpty())
        {
            showNew();
            shell.setStatus("There are no saved rounds yet, so this is a new round.");
        }
        else
        {
            showRoundAt(shown.get(shown.size() - 1));
        }
    }

    /*
     +--------------------------------------------------------------------------+
     | rebuildFilter()                                                          |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Fills the Show drop-down from the saved rounds: all rounds, then         |
     | each course that has rounds (with an "all tees" line when it has         |
     | rounds on more than one set of tees), then each course/tee that has      |
     | rounds, each with its count. Only courses and tees with rounds are       |
     | offered. The choice made before is kept if it is still there,            |
     | otherwise all rounds is chosen. The list of rounds the arrows step       |
     | through is rebuilt to match.                                             |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    private void rebuildFilter()
    {
        List<Round> rounds = shell.store().roundsByDate();
        Map<String, Map<Integer, Integer>> counts = new TreeMap<>();
        for (Round round : rounds)
        {
            Tee tee = shell.store().teeMap().get(round.teeId());
            counts.computeIfAbsent(tee.courseName(), key -> new TreeMap<>()).merge(round.teeId(), 1, Integer::sum);
        }
        boolean wasUpdating = updating;
        updating = true;
        filterCombo.removeAllItems();
        FilterItem all = new FilterItem("All rounds  -  " + rounds.size(), null, -1);
        filterCombo.addItem(all);
        FilterItem chosen = all;
        for (Map.Entry<String, Map<Integer, Integer>> course : counts.entrySet())
        {
            int courseTotal = 0;
            for (int count : course.getValue().values())
            {
                courseTotal += count;
            }
            if (course.getValue().size() > 1)
            {
                FilterItem courseItem = new FilterItem(course.getKey() + ", all tees  -  " + courseTotal, course.getKey(), -1);
                filterCombo.addItem(courseItem);
                if (course.getKey().equals(filterCourse) && filterTeeId < 0)
                {
                    chosen = courseItem;
                }
            }
            for (Map.Entry<Integer, Integer> teeEntry : course.getValue().entrySet())
            {
                Tee tee = shell.store().teeMap().get(teeEntry.getKey());
                FilterItem teeItem = new FilterItem(tee.label() + "  -  " + teeEntry.getValue(), tee.courseName(), tee.id());
                filterCombo.addItem(teeItem);
                if (tee.id() == filterTeeId)
                {
                    chosen = teeItem;
                }
            }
        }
        filterCombo.setSelectedItem(chosen);
        filterCourse = chosen.course();
        filterTeeId = chosen.teeId();
        updating = wasUpdating;
        rebuildShown();
    }

    /*
     +--------------------------------------------------------------------------+
     | rebuildShown()                                                           |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Makes the list of saved rounds (as positions in the date-ordered         |
     | list of all rounds) that match the Show choice. The arrow buttons        |
     | step through this list.                                                  |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    private void rebuildShown()
    {
        shown.clear();
        List<Round> rounds = shell.store().roundsByDate();
        for (int lp = 0; lp < rounds.size(); lp++)
        {
            Round round = rounds.get(lp);
            Tee tee = shell.store().teeMap().get(round.teeId());
            boolean matches = filterCourse == null
                    || (filterTeeId >= 0 ? round.teeId() == filterTeeId : tee.courseName().equals(filterCourse));
            if (matches)
            {
                shown.add(lp);
            }
        }
    }

    /*
     * positionShown(): where the round on screen sits among the rounds the arrows step through (0 is the first, -1 if it is not one of them).
     */
    private int positionShown()
    {
        return shown.indexOf(viewIndex);
    }

    /*
     * goTo(position): shows the round at a position in the filtered list; does nothing if the position is outside it.
     * Passed in:  position - 0 for the first round of the Show choice
     * Passed out: nothing
     */
    private void goTo(int position)
    {
        if (position >= 0 && position < shown.size())
        {
            showRoundAt(shown.get(position));
        }
    }

    /*
     * filterDescription(): words for the footer when the Show choice is not "all rounds".
     * Passed in:  nothing
     * Passed out: "", or text like "  (Sample Links (White))"
     */
    private String filterDescription()
    {
        if (filterCourse == null)
        {
            return "";
        }
        if (filterTeeId >= 0)
        {
            return "  (" + shell.store().teeMap().get(filterTeeId).label() + ")";
        }
        return "  (" + filterCourse + ", all tees)";
    }

    /*
     * updateNav(): switches the four arrows on or off to match where we are in the saved rounds.
     */
    private void updateNav()
    {
        int size = shown.size();
        boolean has = size > 0;
        boolean viewing = mode == Mode.VIEW;
        int position = positionShown();
        firstButton.setEnabled(has && (!viewing || position > 0));
        previousButton.setEnabled(has && (!viewing || position > 0));
        nextButton.setEnabled(has && viewing && position < size - 1);
        lastButton.setEnabled(has && (!viewing || position < size - 1));
    }

    // -------------------------------------------------------------- calculating

    /*
     * number(field): reads a whole number from a text box.
     * Passed in:  field - the box
     * Passed out: the number, or null if the box is empty
     */
    private static Integer number(JTextField field)
    {
        String text = field.getText().trim();
        return text.isEmpty() ? null : Integer.valueOf(text);
    }

    /*
     +--------------------------------------------------------------------------+
     | mark(field, bad)                                                         |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Gives a box its face. A box you can type in is white, so it looks like   |
     | a place to type. A box that cannot be typed in (everything when you      |
     | are looking at a saved round, or a hole that is not being played) is     |
     | the grey of the calculated boxes, so it looks like a display. A box      |
     | with something wrong in it is pink.                                      |
     |                                                                          |
     | Passed in:  field - the box                                              |
     |             bad   - true for pink                                        |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    private void mark(JTextField field, boolean bad)
    {
        boolean typing = mode == Mode.NEW && field.isEnabled() && field.isEditable();
        field.setBackground(bad ? Theme.FIELD_ERROR : (typing ? Color.WHITE : Theme.FIELD_FACE));
    }

    /*
     +--------------------------------------------------------------------------+
     | pccValue()                                                               |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Reads the PCC box.                                                       |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: the value from -1 to 3 (a blank box counts as 0), or         |
     |             null if the box holds something else                         |
     +--------------------------------------------------------------------------+
     */
    private Integer pccValue()
    {
        String text = pccField.getText().trim();
        if (text.isEmpty())
        {
            return 0;
        }
        try
        {
            int value = Integer.parseInt(text);
            return value >= MIN_PCC && value <= MAX_PCC ? value : null;
        }
        catch (NumberFormatException notANumber)
        {
            return null;
        }
    }

    /*
     +--------------------------------------------------------------------------+
     | recalc()                                                                 |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Works out everything calculated on the screen from what is typed:        |
     | the totals for each half and for the round, the count of eagles to       |
     | others, fairways, sand saves, greens in regulation, two-putts, and       |
     | (when every played hole is complete) the 18-hole differential and        |
     | the index after the round. It also marks boxes that cannot be right      |
     | (score or putts out of range, putts not fewer than the score), says      |
     | what is wrong or what is missing in the footer, and turns Save on        |
     | only when the round is complete and valid.                               |
     |                                                                          |
     | Passed in:  nothing (reads the boxes)                                    |
     | Passed out: nothing (fills the calculated boxes)                         |
     +--------------------------------------------------------------------------+
     */
    private void recalc()
    {
        if (updating)
        {
            return;
        }
        Tee tee = selectedTee();
        saveButton.setEnabled(false);
        if (tee == null)
        {
            shell.setStatus("There are no courses yet. Add one to courses.tsv.");
            return;
        }
        int complete = 0;
        int girCount = 0;
        int twoPuttCount = 0;
        int fairwayHits = 0;
        int fairwayChances = 0;
        int sandShots = 0;
        int sandSaves = 0;
        int[] scoreSums = new int[2];
        int[] puttSums = new int[2];
        int[] halfComplete = new int[2];
        int[] results = new int[RESULT_BUCKETS];
        String problem = null;
        for (int hole = 0; hole < HOLES; hole++)
        {
            mark(scoreFields[hole], false);
            mark(puttFields[hole], false);
        }
        for (int hole = playedStart; hole < playedStart + playedCount; hole++)
        {
            int par = tee.pars()[hole];
            if (par >= 4)
            {
                fairwayChances++;
                if (fairwayChecks[hole].isSelected())
                {
                    fairwayHits++;
                }
            }
            Integer score = number(scoreFields[hole]);
            Integer putts = number(puttFields[hole]);
            if (score == null || putts == null)
            {
                continue;
            }
            if (score < 1 || score > MAX_SCORE)
            {
                mark(scoreFields[hole], true);
                problem = problem == null ? "Hole " + (hole + 1) + ": the score must be 1 to " + MAX_SCORE + "." : problem;
                continue;
            }
            if (putts > MAX_PUTTS)
            {
                mark(puttFields[hole], true);
                problem = problem == null ? "Hole " + (hole + 1) + ": putts must be 0 to " + MAX_PUTTS + "." : problem;
                continue;
            }
            if (putts >= score)
            {
                mark(puttFields[hole], true);
                mark(scoreFields[hole], true);
                problem = problem == null
                        ? "Hole " + (hole + 1) + ": putts (" + putts + ") must be fewer than the score (" + score
                        + "). A hole holed from off the green is 0 putts."
                        : problem;
                continue;
            }
            complete++;
            int half = hole < NINE ? 0 : 1;
            halfComplete[half]++;
            scoreSums[half] += score;
            puttSums[half] += putts;
            if (Stats.isGreenInRegulation(par, score, putts))
            {
                girCount++;
            }
            if (putts <= 2)
            {
                twoPuttCount++;
            }
            results[Stats.resultBucket(score - par)]++;
            if (sandChecks[hole].isSelected())
            {
                sandShots++;
                if (Stats.isSandSave(par, score))
                {
                    sandSaves++;
                }
            }
        }

        boolean any = complete > 0;
        outScore.setValue(halfComplete[0] > 0 ? Integer.toString(scoreSums[0]) : "");
        outPutts.setValue(halfComplete[0] > 0 ? Integer.toString(puttSums[0]) : "");
        inScore.setValue(halfComplete[1] > 0 ? Integer.toString(scoreSums[1]) : "");
        inPutts.setValue(halfComplete[1] > 0 ? Integer.toString(puttSums[1]) : "");
        scoreBox.setValue(any ? Integer.toString(scoreSums[0] + scoreSums[1]) : "");
        puttsBox.setValue(any ? Integer.toString(puttSums[0] + puttSums[1]) : "");
        fairwayBox.setValue(fairwayChances > 0 ? fairwayHits + " / " + fairwayChances : "-");
        sandBox.setValue(sandShots > 0 ? sandSaves + " / " + sandShots : "-");
        girBox.setValue(any ? girCount + " / " + complete : "-");
        twoPuttBox.setValue(any ? twoPuttCount + " / " + complete : "-");
        for (int lp = 0; lp < RESULT_BUCKETS; lp++)
        {
            resultBoxes[lp].setValue(any ? Integer.toString(results[lp]) : "");
        }

        LocalDate date = DataLoader.parseDateOrNull(dateField.getText());
        Integer pcc = pccValue();
        boolean ready = problem == null && complete == playedCount && date != null && pcc != null;
        mark(pccField, pcc == null);
        mark(dateField, date == null);
        showProjection(ready, date, pcc);
        if (mode == Mode.NEW)
        {
            saveButton.setEnabled(ready);
            if (problem != null)
            {
                shell.setStatus(problem);
            }
            else if (date == null)
            {
                shell.setStatus("Enter the date as 2026-10-22 or 10/22/2026.");
            }
            else if (pcc == null)
            {
                shell.setStatus("PCC must be a whole number from " + MIN_PCC + " to " + MAX_PCC + ".");
            }
            else if (complete < playedCount)
            {
                shell.setStatus("Enter the putts and score for every hole (" + complete + " of " + playedCount + " done).");
            }
            else
            {
                shell.setStatus("Ready to save.");
            }
        }
    }

    /*
     +--------------------------------------------------------------------------+
     | showProjection(ready, date, pcc)                                         |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Fills the 18-hole differential and index-after boxes. For a saved        |
     | round they come from the handicap history. For a round being             |
     | entered, the history is replayed with this round added (without          |
     | saving it), so you can see what it will do to your index.                |
     |                                                                          |
     | Passed in:  ready - true when every played hole is complete and valid    |
     |             date  - the typed date (null if not valid)                   |
     |             pcc   - the typed PCC (null if not valid)                    |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    private void showProjection(boolean ready, LocalDate date, Integer pcc)
    {
        Handicap.Entry entry = null;
        if (mode == Mode.VIEW)
        {
            for (Handicap.Entry candidate : shell.store().handicap().entries())
            {
                if (candidate.round().id() == viewedRoundId)
                {
                    entry = candidate;
                }
            }
        }
        else if (ready)
        {
            entry = shell.store().project(buildRound(shell.store().nextRoundId(), date, pcc));
        }
        if (entry == null)
        {
            differentialBox.setValue("-");
            indexBox.setValue("-");
            return;
        }
        differentialBox.setValue(Double.isNaN(entry.differential()) ? "pending" : String.format("%.1f", entry.differential()));
        indexBox.setValue(Handicap.format(entry.indexAfter()));
    }

    /*
     +--------------------------------------------------------------------------+
     | buildRound(id, date, pcc)                                                |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Turns the typed values into a Round. Only the holes being played are     |
     | included. Call it only when every played hole is complete and valid.     |
     |                                                                          |
     | Passed in:  id   - the id the round will have                            |
     |             date - the date played                                       |
     |             pcc  - the Playing Conditions Calculation                    |
     | Passed out: the round                                                    |
     +--------------------------------------------------------------------------+
     */
    private Round buildRound(int id, LocalDate date, int pcc)
    {
        Tee tee = selectedTee();
        int[] scores = new int[playedCount];
        int[] putts = new int[playedCount];
        boolean[] fairway = new boolean[playedCount];
        boolean[] sand = new boolean[playedCount];
        for (int lp = 0; lp < playedCount; lp++)
        {
            int hole = playedStart + lp;
            scores[lp] = Integer.parseInt(scoreFields[hole].getText().trim());
            putts[lp] = Integer.parseInt(puttFields[hole].getText().trim());
            fairway[lp] = fairwayChecks[hole].isSelected() && tee.pars()[hole] != 3;
            sand[lp] = sandChecks[hole].isSelected();
        }
        return new Round(id, tee.id(), date, pcc, playedStart, playedCount, scores, putts, fairway, sand);
    }

    /*
     +--------------------------------------------------------------------------+
     | save()                                                                   |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Adds the round being entered to rounds.tsv, then shows it as a saved     |
     | round. If the file cannot be written a message says why and nothing      |
     | changes.                                                                 |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    private void save()
    {
        LocalDate date = DataLoader.parseDateOrNull(dateField.getText());
        Integer pcc = pccValue();
        if (date == null || pcc == null)
        {
            return;
        }
        Round round = buildRound(shell.store().nextRoundId(), date, pcc);
        try
        {
            shell.store().addRound(round);
        }
        catch (IOException failure)
        {
            JOptionPane.showMessageDialog(this, "The round could not be saved:\n" + failure.getMessage(),
                    "Save failed", JOptionPane.ERROR_MESSAGE);
            return;
        }
        filterCourse = null;
        filterTeeId = -1;
        rebuildFilter();
        List<Round> rounds = shell.store().roundsByDate();
        for (int lp = 0; lp < rounds.size(); lp++)
        {
            if (rounds.get(lp).id() == round.id())
            {
                showRoundAt(lp);
                shell.setStatus("Round saved.  Round " + (positionShown() + 1) + " of " + shown.size() + ".");
            }
        }
    }

    // ------------------------------------------------------------ small helpers

    /*
     +--------------------------------------------------------------------------+
     | ChangeWatcher                                                            |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Runs an action whenever the text in a box changes, so the screen can     |
     | recalculate as the user types.                                           |
     +--------------------------------------------------------------------------+
     */
    private static final class ChangeWatcher implements DocumentListener
    {
        private final Runnable action;

        /*
         * ChangeWatcher(action): remembers what to run.
         */
        ChangeWatcher(Runnable action)
        {
            this.action = action;
        }

        /*
         * insertUpdate(event): text was added.
         */
        @Override
        public void insertUpdate(DocumentEvent event)
        {
            action.run();
        }

        /*
         * removeUpdate(event): text was removed.
         */
        @Override
        public void removeUpdate(DocumentEvent event)
        {
            action.run();
        }

        /*
         * changedUpdate(event): a style change; not used for plain text.
         */
        @Override
        public void changedUpdate(DocumentEvent event)
        {
            action.run();
        }
    }

    /*
     +--------------------------------------------------------------------------+
     | EntryFilter                                                              |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Decides what can be typed into a putts or score box, and tells the       |
     | screen when the box is complete so the cursor can move on.               |
     |                                                                          |
     | The rules: only digits are accepted, anything else is ignored. A putts   |
     | box holds one digit. A score box holds one digit, or two when the        |
     | first is a 1 (10 to 19) and so on up to 30, and cannot start with 0.     |
     | Typing a digit into a box that is already full starts it over with       |
     | that digit. Deleting is always allowed. The box is complete (and         |
     | "advance" is run) after a putts digit, after a score digit from 2 to     |
     | 9, and after the second digit of a score. A lone 1 in a score box is     |
     | not complete: it waits.                                                  |
     |                                                                          |
     | While a saved round is being loaded into the screen ("loading" is        |
     | true) any digits are accepted as they are and nothing advances.          |
     |                                                                          |
     | Letters are never typed into the box. If a letterAction was given, a     |
     | single letter typed is passed to it in capitals instead (the putts       |
     | boxes use this for the F and S keys).                                    |
     |                                                                          |
     | Fields (passed in when the filter is made):                              |
     | isScore      - true for a score box, false for a putts box               |
     | loading      - says whether the screen is filling the box itself         |
     | advance      - what to do when the box is complete                       |
     | letterAction - what to do with a letter typed, or null to ignore them    |
     +--------------------------------------------------------------------------+
     */
    static final class EntryFilter extends DocumentFilter
    {
        private static final int PUTT_DIGITS = 1;
        private static final int SCORE_DIGITS = 2;
        private static final char LOWEST_SELF_COMPLETING_DIGIT = '2';
        private final boolean isScore;
        private final BooleanSupplier loading;
        private final Runnable advance;
        private final Consumer<Character> letterAction;

        /*
         * EntryFilter(isScore, loading, advance, letterAction): remembers the kind of box, whether the screen is loading it, what to do when it is complete, and what to do with a letter typed.
         */
        EntryFilter(boolean isScore, BooleanSupplier loading, Runnable advance, Consumer<Character> letterAction)
        {
            this.isScore = isScore;
            this.loading = loading;
            this.advance = advance;
            this.letterAction = letterAction;
        }

        /*
         * insertString(...): typed or pasted text is checked the same way as a replacement.
         */
        @Override
        public void insertString(FilterBypass bypass, int offset, String text, AttributeSet attributes)
                throws BadLocationException
        {
            replace(bypass, offset, 0, text, attributes);
        }

        /*
         +--------------------------------------------------------------------------+
         | replace(bypass, offset, length, text, attributes)                        |
         +--------------------------------------------------------------------------+
         | Purpose:                                                                 |
         | Applies the rules above to a change in the box, and runs "advance"       |
         | when the box is complete.                                                |
         |                                                                          |
         | Passed in:  bypass, offset, length, text, attributes - the change        |
         |             being made, as Swing describes it                            |
         | Passed out: nothing (the box is changed, or not, accordingly)            |
         +--------------------------------------------------------------------------+
         */
        @Override
        public void replace(FilterBypass bypass, int offset, int length, String text, AttributeSet attributes)
                throws BadLocationException
        {
            String incoming = text == null ? "" : text;
            if (incoming.isEmpty())
            {
                super.replace(bypass, offset, length, text, attributes);
                return;
            }
            if (!incoming.chars().allMatch(Character::isDigit))
            {
                if (letterAction != null && incoming.length() == 1 && !loading.getAsBoolean())
                {
                    letterAction.accept(Character.toUpperCase(incoming.charAt(0)));
                }
                return;
            }
            if (loading.getAsBoolean())
            {
                super.replace(bypass, offset, length, text, attributes);
                return;
            }
            int maxDigits = isScore ? SCORE_DIGITS : PUTT_DIGITS;
            String current = bypass.getDocument().getText(0, bypass.getDocument().getLength());
            String result = current.substring(0, offset) + incoming + current.substring(offset + length);
            if (incoming.length() == 1 && result.length() > maxDigits)
            {
                result = incoming;
            }
            if (result.length() > maxDigits || (isScore && result.startsWith("0")))
            {
                return;
            }
            bypass.replace(0, bypass.getDocument().getLength(), result, attributes);
            boolean complete = !isScore || result.length() == SCORE_DIGITS || result.charAt(0) >= LOWEST_SELF_COMPLETING_DIGIT;
            if (complete)
            {
                advance.run();
            }
        }
    }
}

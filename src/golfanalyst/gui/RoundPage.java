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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
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
 | its par, score, putts and fairway and sand check boxes. They look like   |
 | the New Course page: spreadsheet cells with hairlines in a wood frame.   |
 | A wood column on the right, its rows level with the holes, shows the     |
 | rating, the eagles to others, the differential and the index after the   |
 | round. A wood frame under them shows the date, the totals and the        |
 | round's figures, with the buttons at its right end; the four arrows sit  |
 | centred under that. The whole group is centred on the photo.             |
 |                                                                          |
 | Sizes (page area about 944 x 626): a hole table is 34 + 30 + 42 + 38 +   |
 | 32 + 36 = 212 wide and 11 rows of 22 = 242 tall, + 2 + 2 padding + 9 +   |
 | 9 wood = 234 x 264. The results column is 90 + 82 = 172 wide, the same   |
 | 11 rows, so 194 x 264. Across: 234 + 10 + 234 + 10 + 194 = 682. The      |
 | details frame is 3 rows of 22 + 2 gaps of 2 = 70 (the two buttons, 32 +  |
 | 6 + 32, are also 70) + 6 + 6 padding + 9 + 9 wood = 100 tall. Down: 264  |
 | + 6 + 100 + 8 + 46 arrows = 424; with the margins 10 + 6, the course     |
 | bar 35 and its gap 8 that is 483 of 626, so about 71 px of photo shows   |
 | above and below the group, and 117 px each side.                         |
 |                                                                          |
 | Everything under "calculated" is worked out as you type and cannot be    |
 | edited: par comes from the course, the result of each hole from the      |
 | score, and the round's greens in regulation, sand saves, two-putts,      |
 | differential and the index after the round from the rules. Fairways are  |
 | not counted on par 3s, so that box is switched off for them.             |
 |                                                                          |
 | Three modes:                                                             |
 | NEW  - the boxes are open for typing; Save adds the round to rounds.tsv  |
 | VIEW - a saved round is shown read-only; the four arrow buttons step     |
 |        through the saved rounds (First, Previous, Next, Last). The       |
 |        Show drop-down limits them to one course, one course and its      |
 |        tees, or all rounds. The Edit button opens the round for editing. |
 | EDIT - the saved round on show is open for fixing: date, PCC, scores,    |
 |        putts, Fwy and Sand (the course/tee and holes stay locked). Save  |
 |        replaces only its line in rounds.tsv (same RoundID; the file as   |
 |        it was is kept in rounds.tsv.bak) and shows it again read-only;   |
 |        Cancel goes back to it unchanged. Leaving with unsaved changes    |
 |        (Cancel, the arrows or the Home button) asks "Discard your        |
 |        changes?" first.                                                  |
 |                                                                          |
 | A round can be 18 holes, the front nine or the back nine of an 18-hole   |
 | course, or a nine-hole course. Holes not being played are greyed.        |
 |                                                                          |
 | Public Methods:                                                          |
 | showNew()          - starts a new round                                  |
 | showRoundAt(index) - shows a saved round (0 is the oldest)               |
 | showLatest()       - shows the newest saved round, or a new round if none|
 | canLeave()         - asks before leaving a round with unsaved changes    |
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
    private static final String[] HEADINGS = {"Hole", "Par", "Score", "Putt", "Fwy", "Sand"};
    private static final int[] COLUMN_WIDTHS = {34, 30, 42, 38, 32, 36};
    private static final int HOLE_COL = 0;
    private static final int PAR_COL = 1;
    private static final int SCORE_COL = 2;
    private static final int PUTT_COL = 3;
    private static final int FWY_COL = 4;
    private static final int SAND_COL = 5;
    private static final int ROW_HEIGHT = 22;
    private static final int CELL_PAD = 2;
    private static final float HEADING_FONT = 11f;
    private static final float CELL_FONT = 14f;
    private static final float VALUE_FONT = 13f;
    private static final int CHECK_PICTURE = 18;
    private static final int TAG_WIDTH = 86;
    private static final int TAG_HEIGHT = 20;
    private static final int CAPTION_CELL_WIDTH = 90;
    private static final int RESULT_VALUE_WIDTH = 82;
    private static final int DATE_WIDTH = 86;
    private static final int DETAIL_VALUE_WIDTH = 62;
    private static final int PAIR_GAP = 3;
    private static final int COLUMN_GAP = 14;
    private static final int DETAIL_ROW_GAP = 2;
    private static final int BUTTON_WIDTH = 112;
    private static final int BUTTON_HEIGHT = 32;
    private static final int BUTTON_GAP = 6;
    private static final int FRAME_GAP = 10;
    private static final int DETAILS_GAP = 6;
    private static final int NAV_GAP = 8;
    private static final Border CURSOR_OUTLINE = BorderFactory.createLineBorder(Theme.GOLD, 2);

    private enum Mode
    {
        NEW, VIEW, EDIT
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
    static final String DISCARD_QUESTION = "Discard your changes?";
    final WoodButton saveButton = new WoodButton("Save");
    final WoodButton editButton = new WoodButton("Edit");
    final WoodButton cancelButton = new WoodButton("Cancel");
    final JComboBox<FilterItem> filterCombo = new JComboBox<>();
    final JLabel filterLabel = new JLabel("Show:");
    final IconButton firstButton = new IconButton("First.png", "First round", Theme.NAV_ICON_SIZE);
    final IconButton previousButton = new IconButton("Previous.png", "Previous round", Theme.NAV_ICON_SIZE);
    final IconButton nextButton = new IconButton("Next.png", "Next round", Theme.NAV_ICON_SIZE);
    final IconButton lastButton = new IconButton("Last.png", "Last round", Theme.NAV_ICON_SIZE);
    final JPanel group = new JPanel(new GridBagLayout());
    final WoodFrame outFrame;
    final WoodFrame inFrame;
    final WoodFrame resultsFrame;
    final WoodFrame detailsFrame;
    final JPanel navRow;

    private final transient EntryTraversal focusPolicy = new EntryTraversal();
    private Mode mode = Mode.NEW;
    private int viewIndex = -1;
    private final List<Integer> shown = new ArrayList<>();
    private String filterCourse = null;
    private int filterTeeId = -1;
    private int viewedRoundId = -1;
    private boolean viewedHistorical = false;
    static final String HISTORICAL_NOTE = " Historical: kept to look back at, not in your handicap or statistics.";
    private boolean updating = false;
    private int playedStart = 0;
    private int playedCount = HOLES;
    private String editStart = "";
    private JTextField cursorCell = null;
    private final Set<JTextField> wrongFields = new HashSet<>();

    /*
     +--------------------------------------------------------------------------+
     | RoundPage(shell)                                                         |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Builds the screen: the course bar across the top, then one group         |
     | centred on the photo: the Out and In tables and the results column       |
     | side by side, the details frame under them (as wide as all three),       |
     | and the four arrow buttons centred under that. Then starts in NEW mode.  |
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

        group.setOpaque(false);
        GridBagConstraints at = new GridBagConstraints();
        at.gridy = 0;
        at.gridx = 0;
        at.insets = new Insets(0, 0, 0, FRAME_GAP);
        outFrame = buildHalf(0, outPutts, outScore, "Out");
        group.add(outFrame, at);
        at.gridx = 1;
        inFrame = buildHalf(NINE, inPutts, inScore, "In");
        group.add(inFrame, at);
        at.gridx = 2;
        at.insets = new Insets(0, 0, 0, 0);
        resultsFrame = buildResultsColumn();
        group.add(resultsFrame, at);
        at.gridy = 1;
        at.gridx = 0;
        at.gridwidth = 3;
        at.fill = GridBagConstraints.HORIZONTAL;
        at.insets = new Insets(DETAILS_GAP, 0, 0, 0);
        detailsFrame = buildDetails();
        group.add(detailsFrame, at);
        at.gridy = 2;
        at.fill = GridBagConstraints.NONE;
        at.insets = new Insets(NAV_GAP, 0, 0, 0);
        navRow = buildNavRow();
        group.add(navRow, at);
        JPanel middle = new JPanel(new GridBagLayout());
        middle.setOpaque(false);
        middle.add(group, new GridBagConstraints());
        add(middle, BorderLayout.CENTER);

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
        makeCell(field, COLUMN_WIDTHS[isScore ? SCORE_COL : PUTT_COL], CELL_FONT);
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
     * hairlines(): the thin spreadsheet lines of a cell: one under it and one on its right.
     */
    private static Border hairlines()
    {
        return BorderFactory.createMatteBorder(0, 0, 1, 1, Theme.GRID_LINE);
    }

    /*
     * fixSize(part, width): makes a cell exactly one row tall and the given width.
     * Passed in:  part - the cell, width - its width in pixels
     * Passed out: nothing
     */
    private static void fixSize(JComponent part, int width)
    {
        Dimension size = new Dimension(width, ROW_HEIGHT);
        part.setPreferredSize(size);
        part.setMinimumSize(size);
    }

    /*
     * put(sheet, part, col, row, width): adds one cell to a grid, touching its neighbours (no gaps).
     * Passed in:  sheet - the panel, part - the cell, col/row - where, width - columns it spans
     * Passed out: nothing
     */
    private static void put(JPanel sheet, JComponent part, int col, int row, int width)
    {
        GridBagConstraints at = new GridBagConstraints();
        at.gridx = col;
        at.gridy = row;
        at.gridwidth = width;
        at.fill = GridBagConstraints.BOTH;
        sheet.add(part, at);
    }

    /*
     * textCell(text, face, size, width): a cell that only shows text: a heading, a hole number, or Out / In.
     * Passed in:  text - what it shows, face - its colour, size - font size, width - its width
     * Passed out: the cell
     */
    private static JLabel textCell(String text, Color face, float size, int width)
    {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(Theme.font(Font.BOLD, size));
        label.setOpaque(true);
        label.setBackground(face);
        label.setBorder(hairlines());
        fixSize(label, width);
        return label;
    }

    /*
     * valueCell(box, width, size): makes a calculated box a flat grey cell with hairlines.
     * Passed in:  box - the box, width - its width, size - font size
     * Passed out: nothing
     */
    private static void valueCell(ValueBox box, int width, float size)
    {
        box.setFlat(true);
        box.setFont(Theme.font(Font.BOLD, size));
        box.setHorizontalAlignment(SwingConstants.CENTER);
        box.setBorder(hairlines());
        fixSize(box, width);
    }

    /*
     * tagCell(text): a wood caption tag (86 x 20) centred in a see-through 90 x 22 cell.
     * Passed in:  text - the caption
     * Passed out: the cell
     */
    private static JPanel tagCell(String text)
    {
        WoodTag tag = new WoodTag(text);
        Dimension tagSize = new Dimension(TAG_WIDTH, TAG_HEIGHT);
        tag.setPreferredSize(tagSize);
        tag.setMinimumSize(tagSize);
        JPanel cell = new JPanel(new GridBagLayout());
        cell.setOpaque(false);
        cell.add(tag, new GridBagConstraints());
        fixSize(cell, CAPTION_CELL_WIDTH);
        return cell;
    }

    /*
     +--------------------------------------------------------------------------+
     | makeCell(field, width, size)                                             |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Turns a text box into a spreadsheet cell: no sunken box, bold centred    |
     | text, exactly one row tall, with the thin cell lines. While the cursor   |
     | is in a cell that can be typed in, it gets a 2 px gold outline and a     |
     | cream fill (see showFace).                                               |
     |                                                                          |
     | Passed in:  field - the box                                              |
     |             width - its width in pixels                                  |
     |             size  - the font size                                        |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    private void makeCell(JTextField field, int width, float size)
    {
        field.setColumns(0);
        field.setBorder(hairlines());
        field.setFont(Theme.font(Font.BOLD, size));
        field.setHorizontalAlignment(SwingConstants.CENTER);
        fixSize(field, width);
        field.addFocusListener(new FocusAdapter()
        {
            @Override
            public void focusGained(FocusEvent event)
            {
                cursorCell = field;
                showFace(field);
            }

            @Override
            public void focusLost(FocusEvent event)
            {
                if (cursorCell == field)
                {
                    cursorCell = null;
                }
                showFace(field);
            }
        });
    }

    /*
     * checkCell(box, width): makes a Fwy or Sand check box a table cell: the picture shrunk to 18 x 18 and centred, hairlines, and a face set by showCheckFaces.
     * Passed in:  box - the check box, width - its width
     * Passed out: nothing
     */
    private static void checkCell(ImageCheckBox box, int width)
    {
        box.setPictureSize(CHECK_PICTURE);
        box.setOpaque(true);
        box.setBackground(Color.WHITE);
        box.setBorder(hairlines());
        fixSize(box, width);
    }

    /*
     +--------------------------------------------------------------------------+
     | buildHalf(firstHole, puttTotal, scoreTotal, totalLabel)                  |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Builds one hole-by-hole table (Out or In) inside a wooden frame that     |
     | hugs it. The table is spreadsheet style: borderless cells 22 px tall,    |
     | 1 px hairlines under and right of every cell, no gaps, 2 px of white     |
     | round the cells. A grey heading row (Hole, Par, Score, Putt, Fwy,        |
     | Sand), nine hole rows (hole number on white, par grey, score and putt    |
     | cells, the two check boxes) and a total row with grey cells under        |
     | Score and Putt.                                                          |
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
        grid.setBorder(BorderFactory.createEmptyBorder(CELL_PAD, CELL_PAD, CELL_PAD, CELL_PAD));
        for (int col = 0; col < HEADINGS.length; col = col + 1)
        {
            put(grid, textCell(HEADINGS[col], Theme.FIELD_FACE, HEADING_FONT, COLUMN_WIDTHS[col]), col, 0, 1);
        }
        for (int row = 0; row < NINE; row = row + 1)
        {
            int hole = firstHole + row;
            holeLabels[hole] = textCell(Integer.toString(hole + 1), Color.WHITE, CELL_FONT, COLUMN_WIDTHS[HOLE_COL]);
            parBoxes[hole] = new ValueBox();
            valueCell(parBoxes[hole], COLUMN_WIDTHS[PAR_COL], CELL_FONT);
            fairwayChecks[hole] = new ImageCheckBox();
            fairwayChecks[hole].setOnChange(() -> recalc());
            checkCell(fairwayChecks[hole], COLUMN_WIDTHS[FWY_COL]);
            sandChecks[hole] = new ImageCheckBox();
            sandChecks[hole].setOnChange(() -> recalc());
            checkCell(sandChecks[hole], COLUMN_WIDTHS[SAND_COL]);
            puttFields[hole] = entryField(false, hole);
            scoreFields[hole] = entryField(true, hole);
            put(grid, holeLabels[hole], HOLE_COL, row + 1, 1);
            put(grid, parBoxes[hole], PAR_COL, row + 1, 1);
            put(grid, scoreFields[hole], SCORE_COL, row + 1, 1);
            put(grid, puttFields[hole], PUTT_COL, row + 1, 1);
            put(grid, fairwayChecks[hole], FWY_COL, row + 1, 1);
            put(grid, sandChecks[hole], SAND_COL, row + 1, 1);
        }
        int totalRow = NINE + 1;
        put(grid, textCell(totalLabel, Color.WHITE, CELL_FONT, COLUMN_WIDTHS[HOLE_COL] + COLUMN_WIDTHS[PAR_COL]), HOLE_COL, totalRow, 2);
        valueCell(scoreTotal, COLUMN_WIDTHS[SCORE_COL], CELL_FONT);
        valueCell(puttTotal, COLUMN_WIDTHS[PUTT_COL], CELL_FONT);
        put(grid, scoreTotal, SCORE_COL, totalRow, 1);
        put(grid, puttTotal, PUTT_COL, totalRow, 1);
        put(grid, textCell("", Color.WHITE, CELL_FONT, COLUMN_WIDTHS[FWY_COL]), FWY_COL, totalRow, 1);
        put(grid, textCell("", Color.WHITE, CELL_FONT, COLUMN_WIDTHS[SAND_COL]), SAND_COL, totalRow, 1);
        WoodFrame frame = new WoodFrame();
        frame.add(grid, BorderLayout.CENTER);
        return frame;
    }

    /*
     +--------------------------------------------------------------------------+
     | buildResultsColumn()                                                     |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The wooden column right of the In table. It has the same wood border     |
     | and 2 px padding as the hole tables and the same 22 px rows, so it is    |
     | exactly as tall and its rows line up with theirs, but the teak shows     |
     | through inside instead of white. A blank row level with the headings,    |
     | nine caption and value rows level with the holes (Rating, Eagles,        |
     | Birdies, Pars, Bogeys, DBogeys, Others, 18-Hole Diff, Index After), and  |
     | a blank row level with the Out / In totals.                              |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: the framed column                                            |
     +--------------------------------------------------------------------------+
     */
    private WoodFrame buildResultsColumn()
    {
        JPanel column = new JPanel(new GridBagLayout());
        column.setOpaque(false);
        column.setBorder(BorderFactory.createEmptyBorder(CELL_PAD, CELL_PAD, CELL_PAD, CELL_PAD));
        List<String> captions = new ArrayList<>();
        List<ValueBox> values = new ArrayList<>();
        captions.add("Rating");
        values.add(ratingBox);
        for (int lp = 0; lp < RESULT_BUCKETS; lp = lp + 1)
        {
            resultBoxes[lp] = new ValueBox();
            captions.add(RESULT_NAMES[lp]);
            values.add(resultBoxes[lp]);
        }
        captions.add("18-Hole Diff");
        values.add(differentialBox);
        captions.add("Index After");
        values.add(indexBox);
        Dimension rowSize = new Dimension(CAPTION_CELL_WIDTH + RESULT_VALUE_WIDTH, ROW_HEIGHT);
        put(column, (JComponent) Box.createRigidArea(rowSize), 0, 0, 2);
        for (int row = 0; row < captions.size(); row = row + 1)
        {
            valueCell(values.get(row), RESULT_VALUE_WIDTH, VALUE_FONT);
            put(column, tagCell(captions.get(row)), 0, row + 1, 1);
            put(column, values.get(row), 1, row + 1, 1);
        }
        put(column, (JComponent) Box.createRigidArea(rowSize), 0, captions.size() + 1, 2);
        WoodFrame frame = new WoodFrame();
        frame.add(column, BorderLayout.CENTER);
        return frame;
    }

    /*
     * addPair(form, tag, box, row, column): puts a wood caption and its value cell side by side in the details frame, 3 px apart, with 14 px before the next column.
     * Passed in:  form - the panel, tag - caption text, box - the cell, row/column - grid position
     * Passed out: nothing
     */
    private void addPair(JPanel form, String tag, JComponent box, int row, int column)
    {
        int top = (row == 0 ? 0 : DETAIL_ROW_GAP);
        GridBagConstraints at = new GridBagConstraints();
        at.gridy = row;
        at.gridx = column * 2;
        at.insets = new Insets(top, 0, 0, PAIR_GAP);
        WoodTag caption = new WoodTag(tag);
        Dimension tagSize = new Dimension(TAG_WIDTH, TAG_HEIGHT);
        caption.setPreferredSize(tagSize);
        caption.setMinimumSize(tagSize);
        form.add(caption, at);
        at.gridx = column * 2 + 1;
        at.insets = new Insets(top, 0, 0, COLUMN_GAP);
        form.add(box, at);
    }

    /*
     +--------------------------------------------------------------------------+
     | buildDetails()                                                           |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The wooden frame under the tables, as wide as the tables and results     |
     | column together, with the teak showing inside. Three columns of          |
     | captions and values: Date, Score, Putts; Fairway, Sand Saves, Greens In  |
     | Reg; 2-Putt. At the right end, Save above Cancel (entering or editing)   |
     | or Edit above Close (looking at a saved round).                          |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: the framed form                                              |
     +--------------------------------------------------------------------------+
     */
    private WoodFrame buildDetails()
    {
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        makeCell(dateField, DATE_WIDTH, VALUE_FONT);
        dateField.setToolTipText("The date played, as 2026-10-22 or 10/22/2026");
        dateField.getDocument().addDocumentListener(new ChangeWatcher(() -> recalc()));
        for (ValueBox box : new ValueBox[] {scoreBox, puttsBox, fairwayBox, sandBox, girBox, twoPuttBox})
        {
            valueCell(box, DETAIL_VALUE_WIDTH, VALUE_FONT);
        }
        addPair(form, "Date", dateField, 0, 0);
        addPair(form, "Score", scoreBox, 1, 0);
        addPair(form, "Putts", puttsBox, 2, 0);
        addPair(form, "Fairway", fairwayBox, 0, 1);
        addPair(form, "Sand Saves", sandBox, 1, 1);
        addPair(form, "Greens In Reg", girBox, 2, 1);
        addPair(form, "2-Putt", twoPuttBox, 0, 2);
        Dimension buttonSize = new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT);
        for (WoodButton button : new WoodButton[] {saveButton, editButton, cancelButton})
        {
            button.setPreferredSize(buttonSize);
            button.setMinimumSize(buttonSize);
        }
        editButton.setToolTipText("Open this round for editing");
        JPanel buttons = new JPanel(new GridBagLayout());
        buttons.setOpaque(false);
        GridBagConstraints inside = new GridBagConstraints();
        inside.gridy = 0;
        inside.insets = new Insets(0, 0, BUTTON_GAP, 0);
        buttons.add(saveButton, inside);
        buttons.add(editButton, inside);
        inside.gridy = 1;
        inside.insets = new Insets(0, 0, 0, 0);
        buttons.add(cancelButton, inside);
        GridBagConstraints at = new GridBagConstraints();
        at.gridx = 6;
        at.gridy = 0;
        at.gridheight = 3;
        at.weightx = 1.0;
        at.anchor = GridBagConstraints.EAST;
        form.add(buttons, at);
        WoodFrame frame = new WoodFrame();
        frame.add(form, BorderLayout.CENTER);
        return frame;
    }

    /*
     * buildNavRow(): the First / Previous / Next / Last arrows in one row, 8 px apart.
     * Passed in:  nothing
     * Passed out: the row
     */
    private JPanel buildNavRow()
    {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        row.setOpaque(false);
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
        editButton.addActionListener(event -> startEdit());
        cancelButton.addActionListener(event -> cancel());
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
            for (int hole = 0; hole < HOLES; hole = hole + 1)
            {
                if (usable(column[hole]))
                {
                    order.add(column[hole]);
                }
            }
        }
        Component[] after = {dateField, saveButton, editButton, cancelButton, firstButton, previousButton, nextButton, lastButton};
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
        for (int hole = 0; hole < HOLES; hole = hole + 1)
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
     * title(): "New Round" while entering, "00006:Jacksons Chase:Beginner" for a saved round, and "Edit 00006:Jacksons Chase:Beginner" while editing it.
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
        String name = String.format("%05d:%s:%s", round.id(), tee.courseName(), tee.teeName());
        return mode == Mode.EDIT ? "Edit " + name : name;
    }

    /*
     * helpKey(): the start of the help file names: round-new while entering a round, round-view while looking at saved ones, round-edit while editing one (each with -info.txt and -help.txt).
     */
    @Override
    public String helpKey()
    {
        return mode == Mode.NEW ? "round-new" : (mode == Mode.EDIT ? "round-edit" : "round-view");
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
        boolean typing = mode != Mode.VIEW;
        for (int hole = 0; hole < HOLES; hole = hole + 1)
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
            Color checkFace = ((played && typing) ? Color.WHITE : Theme.FIELD_FACE);
            fairwayChecks[hole].setBackground(checkFace);
            sandChecks[hole].setBackground(checkFace);
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
        for (int hole = 0; hole < HOLES; hole = hole + 1)
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
        for (int hole = 0; hole < HOLES; hole = hole + 1)
        {
            scoreFields[hole].setText("");
            puttFields[hole].setText("");
            sandChecks[hole].setSelected(false);
            fairwayChecks[hole].setSelected(false);
        }
        updating = wasUpdating;
    }

    /*
     * setMode(newMode): switches between typing a new round (NEW), looking (VIEW) and editing a saved round (EDIT): which controls are open and which buttons show. The course/tee and holes can only be chosen for a new round.
     * Passed in:  newMode - NEW, VIEW or EDIT
     * Passed out: nothing
     */
    private void setMode(Mode newMode)
    {
        mode = newMode;
        boolean typing = newMode != Mode.VIEW;
        teeCombo.setEnabled(newMode == Mode.NEW);
        holesCombo.setEnabled(newMode == Mode.NEW);
        pccField.setEditable(typing);
        dateField.setEditable(typing);
        saveButton.setVisible(typing);
        editButton.setVisible(!typing);
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
        viewedHistorical = false;
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
        viewedHistorical = round.historical();
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
        for (int lp = 0; lp < round.holes(); lp = lp + 1)
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
        shell.setStatus("Round " + (positionShown() + 1) + " of " + shown.size() + filterDescription()
                + (viewedHistorical ? "." + HISTORICAL_NOTE : ""));
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
                courseTotal = courseTotal + count;
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
        for (int lp = 0; lp < rounds.size(); lp = lp + 1)
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
        if (position >= 0 && position < shown.size() && canLeave())
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
        boolean viewing = mode != Mode.NEW;
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
     | Records whether a box has something wrong in it and gives it its face    |
     | (see showFace).                                                          |
     |                                                                          |
     | Passed in:  field - the box                                              |
     |             bad   - true for pink                                        |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    private void mark(JTextField field, boolean bad)
    {
        if (bad)
        {
            wrongFields.add(field);
        }
        else
        {
            wrongFields.remove(field);
        }
        showFace(field);
    }

    /*
     * canType(field): whether a box can be typed in now: not while looking at a saved round, nor on a hole that is not being played.
     */
    private boolean canType(JTextField field)
    {
        return mode != Mode.VIEW && field.isEnabled() && field.isEditable();
    }

    /*
     +--------------------------------------------------------------------------+
     | showFace(field)                                                          |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Gives a box its face. A box you can type in is white, so it looks like   |
     | a place to type, and cream with a gold outline while the cursor is in    |
     | it. A box that cannot be typed in (everything when you are looking at    |
     | a saved round, or a hole that is not being played) is the grey of the    |
     | calculated boxes, so it looks like a display. A box with something       |
     | wrong in it is pink.                                                     |
     |                                                                          |
     | Passed in:  field - the box                                              |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    private void showFace(JTextField field)
    {
        boolean atCursor = field == cursorCell && canType(field);
        Color face = (canType(field) ? Color.WHITE : Theme.FIELD_FACE);
        if (atCursor)
        {
            face = Theme.FIELD_FOCUS;
        }
        if (wrongFields.contains(field))
        {
            face = Theme.FIELD_ERROR;
        }
        field.setBackground(face);
        if (field != pccField)
        {
            field.setBorder(atCursor ? CURSOR_OUTLINE : hairlines());
        }
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
        for (int hole = 0; hole < HOLES; hole = hole + 1)
        {
            mark(scoreFields[hole], false);
            mark(puttFields[hole], false);
        }
        for (int hole = playedStart; hole < playedStart + playedCount; hole = hole + 1)
        {
            int par = tee.pars()[hole];
            if (par >= 4)
            {
                fairwayChances = fairwayChances + 1;
                if (fairwayChecks[hole].isSelected())
                {
                    fairwayHits = fairwayHits + 1;
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
            complete = complete + 1;
            int half = hole < NINE ? 0 : 1;
            halfComplete[half] = halfComplete[half] + 1;
            scoreSums[half] = scoreSums[half] + score;
            puttSums[half] = puttSums[half] + putts;
            if (Stats.isGreenInRegulation(par, score, putts))
            {
                girCount = girCount + 1;
            }
            if (putts <= 2)
            {
                twoPuttCount = twoPuttCount + 1;
            }
            results[Stats.resultBucket(score - par)] = results[Stats.resultBucket(score - par)] + 1;
            if (sandChecks[hole].isSelected())
            {
                sandShots = sandShots + 1;
                if (Stats.isSandSave(par, score))
                {
                    sandSaves = sandSaves + 1;
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
        for (int lp = 0; lp < RESULT_BUCKETS; lp = lp + 1)
        {
            resultBoxes[lp].setValue(any ? Integer.toString(results[lp]) : "");
        }

        LocalDate date = DataLoader.parseDateOrNull(dateField.getText());
        Integer pcc = pccValue();
        boolean ready = problem == null && complete == playedCount && date != null && pcc != null;
        mark(pccField, pcc == null);
        mark(dateField, date == null);
        showProjection(ready, date, pcc);
        if (mode != Mode.VIEW)
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
     | saving it), so you can see what it will do to your index. A historical   |
     | round has no differential: the box says "historical".                    |
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
        if (mode != Mode.NEW && viewedHistorical)
        {
            differentialBox.setValue("historical");
            indexBox.setValue("-");
            return;
        }
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
            entry = shell.store().project(buildRound(mode == Mode.EDIT ? viewedRoundId : shell.store().nextRoundId(), date, pcc));
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
     | A historical round being edited stays historical.                        |
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
        for (int lp = 0; lp < playedCount; lp = lp + 1)
        {
            int hole = playedStart + lp;
            scores[lp] = Integer.parseInt(scoreFields[hole].getText().trim());
            putts[lp] = Integer.parseInt(puttFields[hole].getText().trim());
            fairway[lp] = fairwayChecks[hole].isSelected() && tee.pars()[hole] != 3;
            sand[lp] = sandChecks[hole].isSelected();
        }
        boolean historical = mode == Mode.EDIT && viewedHistorical;
        return new Round(id, tee.id(), date, pcc, playedStart, playedCount, scores, putts, fairway, sand, historical);
    }

    /*
     +--------------------------------------------------------------------------+
     | save()                                                                   |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Adds the round being entered to rounds.tsv, then shows it as a saved     |
     | round. If the file cannot be written a message says why and nothing      |
     | changes. On the sample data the window first asks whether to make        |
     | your own data folder (Shell.readyToSave); if not saving, nothing         |
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
        if (date == null || pcc == null || !shell.readyToSave())
        {
            return;
        }
        boolean editing = mode == Mode.EDIT;
        Round round = buildRound(editing ? viewedRoundId : shell.store().nextRoundId(), date, pcc);
        try
        {
            if (editing)
            {
                shell.store().replaceRound(round);
            }
            else
            {
                shell.store().addRound(round);
            }
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
        for (int lp = 0; lp < rounds.size(); lp = lp + 1)
        {
            if (rounds.get(lp).id() == round.id())
            {
                showRoundAt(lp);
                shell.setStatus((editing ? "Changes saved; the file as it was is in rounds.tsv.bak." : "Round saved.")
                        + "  Round " + (positionShown() + 1) + " of " + shown.size() + ".");
            }
        }
    }

    /*
     * entries(): everything that can be changed while editing, as one piece of text, so changes can be spotted by comparing it with how it was when editing began.
     * Passed in:  nothing
     * Passed out: the text
     */
    private String entries()
    {
        StringBuilder text = new StringBuilder(dateField.getText()).append('|').append(pccField.getText());
        for (int hole = 0; hole < HOLES; hole = hole + 1)
        {
            text.append('|').append(scoreFields[hole].getText()).append(',').append(puttFields[hole].getText())
                    .append(',').append(fairwayChecks[hole].isSelected()).append(',').append(sandChecks[hole].isSelected());
        }
        return text.toString();
    }

    /*
     * hasChanges(): whether a round being edited has been changed since Edit was pressed.
     */
    boolean hasChanges()
    {
        return mode == Mode.EDIT && !entries().equals(editStart);
    }

    /*
     +--------------------------------------------------------------------------+
     | startEdit()                                                              |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | What the Edit button does: opens the saved round on show for editing.    |
     | The date, PCC, scores, putts, Fwy and Sand open for typing; the          |
     | course/tee and holes stay locked. The header says "Edit ..." and the     |
     | cursor goes to the first score.                                          |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    private void startEdit()
    {
        if (mode != Mode.VIEW || viewIndex < 0)
        {
            return;
        }
        updating = true;
        setMode(Mode.EDIT);
        applyHoles();
        updating = false;
        editStart = entries();
        updateNav();
        recalc();
        shell.setStatus("Editing this round. Change what you need and press Save, or Cancel to leave it as it was.");
        shell.refreshTitle();
        Component first = focusOrderFirstEntry();
        if (first != null)
        {
            SwingUtilities.invokeLater(() -> first.requestFocusInWindow());
        }
    }

    /*
     * cancel(): what Cancel / Close does: while editing, goes back to the saved round as it was (asking first if anything was changed); otherwise back Home.
     */
    private void cancel()
    {
        if (mode != Mode.EDIT)
        {
            shell.goHome();
        }
        else if (canLeave())
        {
            showRoundAt(viewIndex);
        }
    }

    /*
     * canLeave(): while a round being edited has unsaved changes, asks "Discard your changes?"; Yes lets the window or the arrows move away.
     * Passed in:  nothing
     * Passed out: true when it is all right to leave
     */
    @Override
    public boolean canLeave()
    {
        return !hasChanges() || shell.askYesNo(DISCARD_QUESTION);
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

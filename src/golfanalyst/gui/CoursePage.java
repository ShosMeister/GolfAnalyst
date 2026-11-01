package golfanalyst.gui;

import golfanalyst.DataStore;
import golfanalyst.Tee;
import golfanalyst.ZipCodes;
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
import java.awt.Rectangle;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.BooleanSupplier;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;

/*
 +--------------------------------------------------------------------------+
 | CoursePage                                                               |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | The screen for adding a course and up to four sets of tees at once.      |
 |                                                                          |
 | The bar across the top is the course: name and 5-digit zip first, then   |
 | city, state and whether it is a nine-hole course. If that name           |
 | (whatever the capitals) and zip are already in the program, the city,    |
 | state and nine-hole box are filled in from it and locked, the footer     |
 | lists the tees it has, and the new tees are added to it.                 |
 |                                                                          |
 | Below it, like the round page, is a centred row of five wood frames      |
 | over the photo: a caption frame (Tee name, Rating / Slope, Front 9,      |
 | Back 9, Yards / Par, Hole and the hole numbers) and one frame per tee,   |
 | named Ladies, Beginner, Intermediate and Expert to start with (they can  |
 | be renamed). Inside each frame the boxes are spreadsheet cells: white,   |
 | borderless, 56 px wide and 21 px tall, with thin lines between them.     |
 | The cell with the cursor gets a gold outline and a cream fill. Only the  |
 | total par, worked out as you type, is grey.                              |
 |                                                                          |
 | The first tee has a small arrow button that copies its pars and stroke   |
 | index into the EMPTY par and stroke index cells of the other three       |
 | tees. Nothing else is ever copied: a rating copied from another tee      |
 | would spoil the handicap. A tee whose rating is left blank is skipped.   |
 |                                                                          |
 | Everything is checked as you type. The footer always names the next      |
 | thing to fill in or fix, by tee and hole, and Save stays off until all   |
 | is well ("Ready to save."). A real mistake (a stroke index used twice,   |
 | a tee name already used) turns its cell pink at once; a cell simply not  |
 | filled in yet stays white. Save adds the tees to courses.tsv (new        |
 | TeeIDs, the file's own column order) and goes back Home saying what was  |
 | added.                                                                   |
 |                                                                          |
 | Sizes (default window 960 x 770, header 76, footer 30): the page gets    |
 | about 626 px. A frame is 24 rows of 21 px (504) + 2 + 2 padding + 9 + 9  |
 | wood = 526. With the course bar 32 + 6, the button bar 32 + 6 and the    |
 | margins 8 + 6 that is 616. Across: 136 + 4 x 134 + 4 gaps of 14 = 728    |
 | of the 916 px available. The frames sit in a see-through scroll pane,    |
 | so a smaller window gets a scroll bar instead of cutting anything off.   |
 |                                                                          |
 | Public Methods:                                                          |
 | showNew()   - empties the screen ready for a new course                  |
 | tidyName(text) - the spacing and capitals rule for names                 |
 +--------------------------------------------------------------------------+
 */
@SuppressWarnings({"serial", "this-escape"})
public final class CoursePage extends Page
{

    static final int TEES = 4;
    private static final int HOLES = Tee.HOLES;
    private static final int NINE = Tee.NINE;
    private static final String[] DEFAULT_TEE_NAMES = {"Ladies", "Beginner", "Intermediate", "Expert"};
    private static final int ROW_HEIGHT = 21;
    private static final int CELL_WIDTH = 56;
    private static final int CAPTION_WIDTH = 114;
    private static final int COPY_WIDTH = 24;
    private static final int CELL_PAD = 2;
    private static final int FRAME_GAP = 14;
    private static final int BAR_BOX_HEIGHT = 24;
    private static final float CELL_FONT = 15f;
    private static final float NAME_FONT = 14f;
    private static final float INDEX_HEADER_FONT = 11f;
    private static final String INDEX_HEADER = "Hdcp/SI";
    private static final String INDEX_TIP = "Hole handicap number (stroke index): 1 is the hardest hole.";
    private static final float SMALLEST_NAME_FONT = 10f;
    private static final int MIN_SLOPE = 55;
    private static final int MAX_SLOPE = 155;
    private static final String READY_PROMPT = "Type the course name and its 5-digit zip first.";
    private static final String READY_TO_SAVE = "Ready to save.";

    final JTextField courseField = new JTextField();
    final JTextField zipField = new JTextField();
    final JTextField cityField = new JTextField();
    final JTextField stateField = new JTextField();
    final ImageCheckBox nineHoleCheck = new ImageCheckBox();
    final JTextField[] teeNameFields = new JTextField[TEES];
    final JTextField[] ratingFields = new JTextField[TEES];
    final JTextField[] slopeFields = new JTextField[TEES];
    final JTextField[] frontRatingFields = new JTextField[TEES];
    final JTextField[] frontSlopeFields = new JTextField[TEES];
    final JTextField[] backRatingFields = new JTextField[TEES];
    final JTextField[] backSlopeFields = new JTextField[TEES];
    final JTextField[] yardageFields = new JTextField[TEES];
    final JLabel[] parTotalBoxes = new JLabel[TEES];
    final JTextField[][] parFields = new JTextField[TEES][HOLES];
    final JTextField[][] indexFields = new JTextField[TEES][HOLES];
    final JLabel[] indexHeaders = new JLabel[TEES];
    final WoodButton copyButton = new WoodButton("►");
    final WoodButton saveButton = new WoodButton("Save");
    final WoodButton cancelButton = new WoodButton("Cancel");
    final WoodButton correctButton = new WoodButton("Correct...");
    static final String CORRECT_NOTE = "Changing the name or zip corrects this course for all its tees. To add a different course, "
            + "press Cancel and start again.";
    static final String SAME_COURSE = "A course with that name and zip is already in the program.";
    static final String NOTHING_CORRECTED = "The name, zip, city and state are the same as before, so there is nothing to save.";
    static final String SAVE_ANYWAY_TITLE = "Save anyway";
    static final Color ZIP_WARNING = new Color(240, 140, 20);
    private static final int CORRECT_WIDTH = 90;
    private static final int CORRECT_HEIGHT = 24;
    private boolean correcting = false;
    private JTextField alsoBadField = null;
    private transient ZipCodes zipCodes = null;
    ZipCodes.Check zipCheck = new ZipCodes.Check(ZipCodes.Verdict.NONE, null, "");
    private javax.swing.border.Border stateBorder = null;
    final JScrollPane scroller;
    final JPanel framesRow = new JPanel(new FlowLayout(FlowLayout.CENTER, FRAME_GAP, 0));

    private final List<JTextField> allBoxes = new ArrayList<>();
    private String blankScreen = "";
    private final Set<JTextField> cells = new HashSet<>();
    private final List<JComponent> frontRow = new ArrayList<>();
    private final List<JComponent> backRow = new ArrayList<>();
    private final List<List<JComponent>> holeRows = new ArrayList<>();
    private final transient EntryTraversal focusPolicy = new EntryTraversal();
    private transient List<Tee> existingTees = new ArrayList<>();
    private final transient Tee[] loadedTees = new Tee[TEES];
    private final String[] loadedEntries = new String[TEES];
    private String loadedPlace = "";
    private JTextField badField = null;
    private JTextField cursorCell = null;
    private boolean updating = false;

    /*
     +--------------------------------------------------------------------------+
     | CoursePage(shell)                                                        |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Builds the screen: the course bar, the row of wood frames in its         |
     | see-through scroll pane and the Save / Cancel bar, then empties it.      |
     | Like every page it must not call the window while it is being built.     |
     |                                                                          |
     | Passed in:  shell - the window this page sits in                         |
     | Passed out: the page                                                     |
     +--------------------------------------------------------------------------+
     */
    public CoursePage(Shell shell)
    {
        super(shell, "course");
        setLayout(new BorderLayout(0, 6));
        setBorder(BorderFactory.createEmptyBorder(8, 14, 6, 14));

        add(buildCourseBar(), BorderLayout.NORTH);

        buildFrames();
        JPanel framesTop = new JPanel(new BorderLayout());
        framesTop.setOpaque(false);
        framesTop.add(framesRow, BorderLayout.NORTH);
        scroller = new JScrollPane(framesTop, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroller.setOpaque(false);
        scroller.getViewport().setOpaque(false);
        scroller.setBorder(BorderFactory.createEmptyBorder());
        scroller.getVerticalScrollBar().setUnitIncrement(ROW_HEIGHT);
        add(scroller, BorderLayout.CENTER);

        add(buildButtonBar(), BorderLayout.SOUTH);

        copyButton.addActionListener(event -> copyFirstTee());
        saveButton.addActionListener(event -> save());
        cancelButton.addActionListener(event ->
        {
            if (canLeave())
            {
                shell.goHome();
            }
        });
        nineHoleCheck.setOnChange(() -> setNineHoles(nineHoleCheck.isSelected()));
        setFocusTraversalPolicyProvider(true);
        setFocusTraversalPolicy(focusPolicy);
        prepareNew();
    }

    // ------------------------------------------------------------------ building

    /*
     +--------------------------------------------------------------------------+
     | buildCourseBar()                                                         |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The light bar across the top: course name, zip (with the Correct...      |
     | button for a course already in the program), city, state and the         |
     | nine-hole box. Changing the name or zip looks the course up; a complete  |
     | zip fills in an empty city and state from the zip list.                  |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: the bar                                                      |
     +--------------------------------------------------------------------------+
     */
    private JPanel buildCourseBar()
    {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 3));
        bar.setBackground(Theme.PANEL_BODY);
        bar.setBorder(BorderFactory.createLineBorder(Theme.FRAME_DARK, 1));
        nameBox(courseField, 230, BAR_BOX_HEIGHT);
        numberBox(zipField, BoxFilter.Kind.ZIP, 64, BAR_BOX_HEIGHT);
        nameBox(cityField, 150, BAR_BOX_HEIGHT);
        numberBox(stateField, BoxFilter.Kind.STATE, 40, BAR_BOX_HEIGHT);
        courseField.setToolTipText("The name of the course, as on the scorecard");
        zipField.setToolTipText("The course's 5-digit zip code");
        stateField.setToolTipText("Two letters, for example SC");
        nineHoleCheck.setToolTipText("Tick for a nine-hole course (handicap numbers 1 to 9)");
        zipField.getDocument().addDocumentListener(new ChangeWatcher(() -> zipChanged()));
        DocumentListener lookup = new ChangeWatcher(() -> courseChanged());
        courseField.getDocument().addDocumentListener(lookup);
        zipField.getDocument().addDocumentListener(lookup);
        Dimension correctSize = new Dimension(CORRECT_WIDTH, CORRECT_HEIGHT);
        correctButton.setPreferredSize(correctSize);
        correctButton.setMinimumSize(correctSize);
        correctButton.setFont(Theme.font(Font.BOLD, 12f));
        correctButton.setFocusable(false);
        correctButton.setToolTipText("Correct this course's name or zip (for all its tees)");
        correctButton.addActionListener(event -> startCorrecting());
        bar.add(new JLabel("Course:"));
        bar.add(courseField);
        bar.add(new JLabel("Zip:"));
        bar.add(zipField);
        bar.add(correctButton);
        bar.add(new JLabel("City:"));
        bar.add(cityField);
        bar.add(new JLabel("State:"));
        bar.add(stateField);
        bar.add(nineHoleCheck);
        bar.add(new JLabel("Nine-hole course"));
        return bar;
    }

    /*
     * buildButtonBar(): the Save and Cancel buttons, on the right under the scroll pane.
     * Passed in:  nothing
     * Passed out: the bar
     */
    private JPanel buildButtonBar()
    {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        bar.setOpaque(false);
        saveButton.setPreferredSize(new Dimension(110, 32));
        cancelButton.setPreferredSize(new Dimension(110, 32));
        bar.add(saveButton);
        bar.add(cancelButton);
        return bar;
    }

    /*
     +--------------------------------------------------------------------------+
     | buildFrames()                                                            |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Builds the centred row of five wood frames: the caption frame, then one  |
     | frame per tee. Every frame has the same rows, each 21 px tall, so they   |
     | line up across the page:                                                 |
     |   0      tee name                                                        |
     |   1      rating and slope                                                |
     |   2      front 9 rating and slope (hidden on a nine-hole course)         |
     |   3      back 9 rating and slope (hidden on a nine-hole course)          |
     |   4      yards and the total par                                         |
     |   5      the Hole / Par / SI headings                                    |
     |   6-23   holes 1 to 18 (10 to 18 hidden on a nine-hole course)           |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing (fills framesRow)                                    |
     +--------------------------------------------------------------------------+
     */
    private void buildFrames()
    {
        framesRow.setOpaque(false);
        for (int hole = 0; hole < HOLES; hole = hole + 1)
        {
            holeRows.add(new ArrayList<>());
        }
        framesRow.add(buildCaptionFrame());
        for (int tee = 0; tee < TEES; tee = tee + 1)
        {
            framesRow.add(buildTeeFrame(tee));
        }
    }

    /*
     * framed(sheet): puts a white sheet of cells inside a wood frame, with about 2 px of white round the cells.
     * Passed in:  sheet - the white panel of rows
     * Passed out: the frame
     */
    private static WoodFrame framed(JPanel sheet)
    {
        sheet.setBackground(Color.WHITE);
        sheet.setBorder(BorderFactory.createEmptyBorder(CELL_PAD, CELL_PAD, CELL_PAD, CELL_PAD));
        WoodFrame frame = new WoodFrame();
        frame.add(sheet, BorderLayout.CENTER);
        return frame;
    }

    /*
     * put(sheet, part, col, row, width): adds one cell to a frame's sheet, touching its neighbours (no gaps).
     * Passed in:  sheet - the white panel, part - the cell, col/row - where, width - columns it spans
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
     * fixSize(part, width): makes a cell exactly one row tall and the given width.
     */
    private static void fixSize(JComponent part, int width)
    {
        Dimension size = new Dimension(width, ROW_HEIGHT);
        part.setPreferredSize(size);
        part.setMinimumSize(size);
    }

    /*
     * hairlines(rightLine): the thin cell lines: one under every cell, and one on the right of the left cell of a pair.
     * Passed in:  rightLine - true for the left cell of a pair
     * Passed out: the border
     */
    private static Border hairlines(boolean rightLine)
    {
        return BorderFactory.createMatteBorder(0, 0, 1, rightLine ? 1 : 0, Theme.GRID_LINE);
    }

    /*
     * greyCell(text, rightLine): a grey cell that cannot be typed in: a Par or SI heading, or the total par.
     * Passed in:  text - what it shows, rightLine - true for the left cell of a pair
     * Passed out: the cell
     */
    private static JLabel greyCell(String text, boolean rightLine)
    {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(Theme.font(Font.BOLD, CELL_FONT));
        label.setOpaque(true);
        label.setBackground(Theme.FIELD_FACE);
        label.setBorder(hairlines(rightLine));
        fixSize(label, CELL_WIDTH);
        return label;
    }

    /*
     * captionTag(text): a light wood caption tag for the caption frame.
     */
    private static WoodTag captionTag(String text)
    {
        WoodTag tag = new WoodTag(text);
        fixSize(tag, CAPTION_WIDTH);
        return tag;
    }

    /*
     +--------------------------------------------------------------------------+
     | buildCaptionFrame()                                                      |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The frame on the left: wood caption tags for the top rows (Tee name,     |
     | Rating / Slope, Front 9, Back 9, Yards / Par, Hole), then the hole       |
     | numbers 1 to 18 in 14 pt bold.                                           |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: the frame                                                    |
     +--------------------------------------------------------------------------+
     */
    private WoodFrame buildCaptionFrame()
    {
        JPanel sheet = new JPanel(new GridBagLayout());
        String[] captions = {"Tee name", "Rating / Slope", "Front 9", "Back 9", "Yards / Par", "Hole"};
        for (int row = 0; row < captions.length; row = row + 1)
        {
            WoodTag tag = captionTag(captions[row]);
            put(sheet, tag, 0, row, 1);
            if (row == 2)
            {
                frontRow.add(tag);
            }
            if (row == 3)
            {
                backRow.add(tag);
            }
        }
        for (int hole = 0; hole < HOLES; hole = hole + 1)
        {
            JLabel number = new JLabel(Integer.toString(hole + 1), SwingConstants.CENTER);
            number.setFont(Theme.font(Font.BOLD, NAME_FONT));
            number.setBorder(hairlines(false));
            fixSize(number, CAPTION_WIDTH);
            put(sheet, number, 0, 6 + hole, 1);
            holeRows.get(hole).add(number);
        }
        return framed(sheet);
    }

    /*
     +--------------------------------------------------------------------------+
     | buildTeeFrame(tee)                                                       |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | One tee's frame: two cells across. The name spans both (on the first     |
     | tee it shares its row with the copy arrow); then rating and slope,       |
     | front 9 and back 9 rating and slope, yards and total par, the Par / SI   |
     | headings, and the par and stroke index of every hole.                    |
     |                                                                          |
     | Passed in:  tee - which tee, 0 to 3                                      |
     | Passed out: the frame                                                    |
     +--------------------------------------------------------------------------+
     */
    private WoodFrame buildTeeFrame(int tee)
    {
        JPanel sheet = new JPanel(new GridBagLayout());
        teeNameFields[tee] = nameBox(new JTextField(), 2 * CELL_WIDTH, ROW_HEIGHT);
        teeNameFields[tee].setHorizontalAlignment(SwingConstants.CENTER);
        if (tee == 0)
        {
            JPanel nameRow = new JPanel(new BorderLayout());
            nameRow.setOpaque(false);
            fixSize(nameRow, 2 * CELL_WIDTH);
            fixSize(teeNameFields[tee], 2 * CELL_WIDTH - COPY_WIDTH);
            fixSize(copyButton, COPY_WIDTH);
            copyButton.setFocusable(false);
            copyButton.setToolTipText("Copy these pars and handicap numbers into the empty Par and Hdcp/SI cells of the other tees");
            nameRow.add(teeNameFields[tee], BorderLayout.CENTER);
            nameRow.add(copyButton, BorderLayout.EAST);
            put(sheet, nameRow, 0, 0, 2);
        }
        else
        {
            put(sheet, teeNameFields[tee], 0, 0, 2);
        }
        makeCell(teeNameFields[tee], false);
        teeNameFields[tee].setFont(Theme.font(Font.BOLD, NAME_FONT));
        JTextField name = teeNameFields[tee];
        name.getDocument().addDocumentListener(new ChangeWatcher(() -> fitName(name)));

        ratingFields[tee] = numberCell(BoxFilter.Kind.RATING, true);
        slopeFields[tee] = numberCell(BoxFilter.Kind.SLOPE, false);
        put(sheet, ratingFields[tee], 0, 1, 1);
        put(sheet, slopeFields[tee], 1, 1, 1);

        frontRatingFields[tee] = numberCell(BoxFilter.Kind.RATING, true);
        frontSlopeFields[tee] = numberCell(BoxFilter.Kind.SLOPE, false);
        put(sheet, frontRatingFields[tee], 0, 2, 1);
        put(sheet, frontSlopeFields[tee], 1, 2, 1);
        frontRow.addAll(List.of(frontRatingFields[tee], frontSlopeFields[tee]));

        backRatingFields[tee] = numberCell(BoxFilter.Kind.RATING, true);
        backSlopeFields[tee] = numberCell(BoxFilter.Kind.SLOPE, false);
        put(sheet, backRatingFields[tee], 0, 3, 1);
        put(sheet, backSlopeFields[tee], 1, 3, 1);
        backRow.addAll(List.of(backRatingFields[tee], backSlopeFields[tee]));

        yardageFields[tee] = numberCell(BoxFilter.Kind.YARDAGE, true);
        yardageFields[tee].setToolTipText("Total yardage (you can leave it blank)");
        parTotalBoxes[tee] = greyCell("", false);
        parTotalBoxes[tee].setToolTipText("The total par, worked out as you type");
        put(sheet, yardageFields[tee], 0, 4, 1);
        put(sheet, parTotalBoxes[tee], 1, 4, 1);

        put(sheet, greyCell("Par", true), 0, 5, 1);
        indexHeaders[tee] = greyCell(INDEX_HEADER, false);
        indexHeaders[tee].setFont(Theme.font(Font.BOLD, INDEX_HEADER_FONT));
        indexHeaders[tee].setToolTipText(INDEX_TIP);
        put(sheet, indexHeaders[tee], 1, 5, 1);
        for (int hole = 0; hole < HOLES; hole = hole + 1)
        {
            parFields[tee][hole] = numberCell(BoxFilter.Kind.PAR, true);
            parFields[tee][hole].getDocument().addDocumentListener(new ChangeWatcher(() -> updateTotals()));
            indexFields[tee][hole] = numberCell(BoxFilter.Kind.STROKE_INDEX, false);
            put(sheet, parFields[tee][hole], 0, 6 + hole, 1);
            put(sheet, indexFields[tee][hole], 1, 6 + hole, 1);
            holeRows.get(hole).add(parFields[tee][hole]);
            holeRows.get(hole).add(indexFields[tee][hole]);
        }
        return framed(sheet);
    }

    /*
     * numberCell(kind, rightLine): a spreadsheet cell with typing rules (see numberBox and makeCell).
     * Passed in:  kind - which typing rules, rightLine - true for the left cell of a pair
     * Passed out: the cell
     */
    private JTextField numberCell(BoxFilter.Kind kind, boolean rightLine)
    {
        JTextField field = numberBox(new JTextField(), kind, CELL_WIDTH, ROW_HEIGHT);
        makeCell(field, rightLine);
        return field;
    }

    /*
     +--------------------------------------------------------------------------+
     | makeCell(field, rightLine)                                               |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Turns a text box into a spreadsheet cell: no sunken box, bold 15 pt      |
     | centred text, exactly one row tall, with the thin cell lines. The cell   |
     | with the cursor gets a 2 px gold outline (and a cream fill, see          |
     | colourBoxes) until the cursor leaves it.                                 |
     |                                                                          |
     | Passed in:  field     - the box                                          |
     |             rightLine - true for the left cell of a pair                 |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    private void makeCell(JTextField field, boolean rightLine)
    {
        Border normal = hairlines(rightLine);
        Border outlined = BorderFactory.createLineBorder(Theme.GOLD, 2);
        field.setBorder(normal);
        field.setFont(Theme.font(Font.BOLD, CELL_FONT));
        field.setHorizontalAlignment(SwingConstants.CENTER);
        fixSize(field, field.getPreferredSize().width);
        field.addFocusListener(new FocusAdapter()
        {
            @Override
            public void focusGained(FocusEvent event)
            {
                cursorCell = field;
                field.setBorder(outlined);
                colourBoxes();
            }

            @Override
            public void focusLost(FocusEvent event)
            {
                if (cursorCell == field)
                {
                    cursorCell = null;
                }
                field.setBorder(normal);
                colourBoxes();
            }
        });
        cells.add(field);
    }

    /*
     * fitName(field): keeps a tee name inside its cell: 14 pt, made smaller (down to 10 pt) only when a long name would not fit.
     * Passed in:  field - a tee name cell
     * Passed out: nothing
     */
    private static void fitName(JTextField field)
    {
        int room = field.getPreferredSize().width - 6;
        float size = NAME_FONT;
        Font font = Theme.font(Font.BOLD, size);
        while (size > SMALLEST_NAME_FONT && field.getFontMetrics(font).stringWidth(field.getText()) > room)
        {
            size = size - 1f;
            font = Theme.font(Font.BOLD, size);
        }
        if (!font.equals(field.getFont()))
        {
            field.setFont(font);
        }
    }

    /*
     +--------------------------------------------------------------------------+
     | nameBox(field, width, height)                                            |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Sets up a box for a name (course, city, tee). Anything can be typed;     |
     | when the cursor leaves the box the name is tidied (see tidyName).        |
     |                                                                          |
     | Passed in:  field - the box, width/height - its size                     |
     | Passed out: the same box                                                 |
     +--------------------------------------------------------------------------+
     */
    private JTextField nameBox(JTextField field, int width, int height)
    {
        field.setFont(Theme.font(Font.BOLD, 13f));
        field.setPreferredSize(new Dimension(width, height));
        field.addFocusListener(new FocusAdapter()
        {
            @Override
            public void focusLost(FocusEvent event)
            {
                tidyBox(field);
            }
        });
        commonBehaviour(field);
        return field;
    }

    /*
     +--------------------------------------------------------------------------+
     | numberBox(field, kind, width, height)                                    |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Sets up a box with typing rules (see BoxFilter): par, stroke index,      |
     | slope, rating, yardage, zip or state. When the box is complete the       |
     | cursor moves on by itself, in the order set in focusOrder.               |
     |                                                                          |
     | Passed in:  field - the box, kind - which rules, width/height - size     |
     | Passed out: the same box                                                 |
     +--------------------------------------------------------------------------+
     */
    private JTextField numberBox(JTextField field, BoxFilter.Kind kind, int width, int height)
    {
        field.setHorizontalAlignment(SwingConstants.CENTER);
        field.setFont(Theme.font(Font.BOLD, 13f));
        field.setPreferredSize(new Dimension(width, height));
        ((AbstractDocument) field.getDocument()).setDocumentFilter(new BoxFilter(kind, () -> nineHoleCheck.isSelected(),
                () -> updating, () -> SwingUtilities.invokeLater(() -> field.transferFocus())));
        commonBehaviour(field);
        return field;
    }

    /*
     * commonBehaviour(field): what every box does: an ordinary boxed look (cells change it), Enter moves on, the cursor arriving selects the text and scrolls the box into view, and every change is checked.
     * Passed in:  field - the box
     * Passed out: nothing
     */
    private void commonBehaviour(JTextField field)
    {
        field.setBorder(BorderFactory.createLineBorder(new Color(150, 150, 150), 1));
        field.addActionListener(event -> field.transferFocus());
        field.addFocusListener(new FocusAdapter()
        {
            @Override
            public void focusGained(FocusEvent event)
            {
                field.scrollRectToVisible(new Rectangle(0, -ROW_HEIGHT, field.getWidth(), field.getHeight() + 2 * ROW_HEIGHT));
                SwingUtilities.invokeLater(() -> field.selectAll());
            }
        });
        field.getDocument().addDocumentListener(new ChangeWatcher(() -> typed()));
        allBoxes.add(field);
    }

    // ------------------------------------------------------------ the course step

    /*
     * courseReady(): whether the course name and a 5-digit zip have been typed.
     */
    private boolean courseReady()
    {
        return !courseField.getText().isBlank() && zipField.getText().matches("\\d{5}");
    }

    /*
     +--------------------------------------------------------------------------+
     | courseChanged()                                                          |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Runs when the course name or zip changes. Looks the course up; if it is  |
     | already in the program, fills in its city and state, locks its           |
     | nine-hole box and loads its tees into the frames for fixing (the title   |
     | becomes Edit Course). If it was found a moment ago but no longer         |
     | matches, those boxes and frames are emptied again, unless something was  |
     | already changed: then nothing is thrown away, Save stays off and the     |
     | footer asks for the name and zip to be put back (see findMistake).       |
     | While correcting (Correct...) it is the same course, so it is not looked |
     | up again.                                                                |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    private void courseChanged()
    {
        if (updating)
        {
            return;
        }
        if (correcting)
        {
            refresh();
            return;
        }
        List<Tee> found = typedCourse();
        boolean keepEdits = found.isEmpty() && editing() && hasEdits();
        if (!found.equals(existingTees) && !keepEdits)
        {
            existingTees = found;
            updating = true;
            cityField.setText(found.isEmpty() ? "" : found.get(0).city());
            stateField.setText(found.isEmpty() ? "" : found.get(0).state());
            nineHoleCheck.setSelected(!found.isEmpty() && found.get(0).holes() == NINE);
            loadTees(found);
            updating = false;
        }
        applyState();
        refresh();
        shell.refreshTitle();
    }

    /*
     +--------------------------------------------------------------------------+
     | loadTees(tees)                                                           |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Puts a course's saved tees into the frames, ready to be fixed: name,     |
     | rating, slope, front and back nine, yardage, pars and stroke index.      |
     | Only the first four fit; frames left over are emptied (with their usual  |
     | names) for new tees. What was loaded is remembered, so Save knows which  |
     | tees were changed. Called with an empty list it empties every frame.     |
     |                                                                          |
     | Passed in:  tees - the course's tees, in file order                      |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    private void loadTees(List<Tee> tees)
    {
        boolean wasUpdating = updating;
        updating = true;
        for (int tee = 0; tee < TEES; tee = tee + 1)
        {
            Tee saved = tee < tees.size() ? tees.get(tee) : null;
            loadedTees[tee] = saved;
            for (JTextField field : teeBoxes(tee))
            {
                field.setText("");
            }
            if (saved == null)
            {
                teeNameFields[tee].setText(DEFAULT_TEE_NAMES[tee]);
                continue;
            }
            teeNameFields[tee].setText(saved.teeName());
            ratingFields[tee].setText(Double.toString(saved.rating()));
            slopeFields[tee].setText(Integer.toString(saved.slope()));
            frontRatingFields[tee].setText(saved.frontRating() > 0 ? Double.toString(saved.frontRating()) : "");
            frontSlopeFields[tee].setText(saved.frontSlope() > 0 ? Integer.toString(saved.frontSlope()) : "");
            backRatingFields[tee].setText(saved.backRating() > 0 ? Double.toString(saved.backRating()) : "");
            backSlopeFields[tee].setText(saved.backSlope() > 0 ? Integer.toString(saved.backSlope()) : "");
            yardageFields[tee].setText(saved.yardage() > 0 ? Integer.toString(saved.yardage()) : "");
            for (int hole = 0; hole < saved.holes(); hole = hole + 1)
            {
                parFields[tee][hole].setText(Integer.toString(saved.pars()[hole]));
                indexFields[tee][hole].setText(Integer.toString(saved.strokeIndex()[hole]));
            }
        }
        for (int tee = 0; tee < TEES; tee = tee + 1)
        {
            loadedEntries[tee] = frameEntries(tee);
        }
        loadedPlace = placeEntries();
        updating = wasUpdating;
    }

    /*
     * typedCourse(): the saved tees of the course whose name and zip are typed in the course bar (empty for a new course or while the zip is not complete).
     */
    private List<Tee> typedCourse()
    {
        return courseReady() ? shell.store().teesForCourse(courseField.getText(), zipField.getText()) : new ArrayList<>();
    }

    /*
     * hasEdits(): whether anything was changed since a saved course was loaded: a loaded tee, the city or state, or anything typed in an empty frame (other than its name).
     */
    private boolean hasEdits()
    {
        if (placeChanged())
        {
            return true;
        }
        for (int tee = 0; tee < TEES; tee = tee + 1)
        {
            if (frameChanged(tee))
            {
                return true;
            }
            for (JTextField field : teeBoxes(tee))
            {
                if (loadedTees[tee] == null && field != teeNameFields[tee] && !field.getText().isEmpty())
                {
                    return true;
                }
            }
        }
        return false;
    }

    /*
     * frameEntries(tee): everything typed in one tee's frame, as one piece of text, so a change can be spotted.
     */
    private String frameEntries(int tee)
    {
        StringBuilder text = new StringBuilder();
        for (JTextField field : teeBoxes(tee))
        {
            text.append(field.getText().trim()).append('|');
        }
        return text.toString();
    }

    /*
     * placeEntries(): the city and state as typed, as one piece of text, so a change can be spotted.
     */
    private String placeEntries()
    {
        return cityField.getText().trim() + "|" + stateField.getText().trim();
    }

    /*
     * editing(): whether a course already in the program is on the screen (its tees loaded for fixing).
     */
    private boolean editing()
    {
        return !existingTees.isEmpty();
    }

    /*
     * frameChanged(tee): whether a loaded tee's frame was changed since it was loaded.
     */
    private boolean frameChanged(int tee)
    {
        return loadedTees[tee] != null && !frameEntries(tee).equals(loadedEntries[tee]);
    }

    /*
     * placeChanged(): whether the city or state of a course already in the program was changed (that changes all its tees).
     */
    private boolean placeChanged()
    {
        return editing() && !placeEntries().equals(loadedPlace);
    }

    /*
     * notLoaded(): the course's saved tees that are not in a frame (only four fit); their names still count as taken.
     */
    private List<Tee> notLoaded()
    {
        List<Tee> rest = new ArrayList<>(existingTees);
        for (Tee loaded : loadedTees)
        {
            rest.remove(loaded);
        }
        return rest;
    }

    /*
     * setNineHoles(nine): ticks or unticks the nine-hole box, shows or hides the holes and rows that go with it, and checks everything again.
     * Passed in:  nine - true for a nine-hole course
     * Passed out: nothing
     */
    void setNineHoles(boolean nine)
    {
        nineHoleCheck.setSelected(nine);
        applyState();
        refresh();
    }

    /*
     +--------------------------------------------------------------------------+
     | applyState()                                                             |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Brings the screen up to date: the nine-hole box is locked for a course   |
     | already in the program (its rounds depend on it), and so are its name    |
     | and zip until Correct... is pressed; a nine-hole course                  |
     | hides the front / back rows and holes 10 to 18 in every frame. Then the  |
     | colours and par totals. The tee cells are never greyed: they can be      |
     | typed in from the start. It never calls the window, so it is safe        |
     | while the page is being built.                                           |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    private void applyState()
    {
        boolean nine = nineHoleCheck.isSelected();
        nineHoleCheck.setEnabled(!editing());
        boolean lockCourse = editing() && !correcting;
        courseField.setEditable(!lockCourse);
        zipField.setEditable(!lockCourse);
        correctButton.setVisible(editing());
        correctButton.setEnabled(!correcting);
        for (JComponent part : frontRow)
        {
            part.setVisible(!nine);
        }
        for (JComponent part : backRow)
        {
            part.setVisible(!nine);
        }
        for (int hole = NINE; hole < HOLES; hole = hole + 1)
        {
            for (JComponent part : holeRows.get(hole))
            {
                part.setVisible(!nine);
            }
        }
        framesRow.revalidate();
        framesRow.repaint();
        colourBoxes();
        updateTotals();
    }

    /*
     * playedHoles(): how many holes the course has: 9 when the nine-hole box is ticked, otherwise 18.
     */
    private int playedHoles()
    {
        return nineHoleCheck.isSelected() ? NINE : HOLES;
    }

    /*
     * teeBoxes(tee): every box of one tee, in the order the cursor visits them (hidden ones included).
     * Passed in:  tee - 0 to 3
     * Passed out: the boxes
     */
    private List<JTextField> teeBoxes(int tee)
    {
        List<JTextField> boxes = new ArrayList<>(List.of(teeNameFields[tee], ratingFields[tee], slopeFields[tee],
                frontRatingFields[tee], frontSlopeFields[tee], backRatingFields[tee], backSlopeFields[tee], yardageFields[tee]));
        boxes.addAll(List.of(parFields[tee]));
        boxes.addAll(List.of(indexFields[tee]));
        return boxes;
    }

    /*
     * typed(): runs after any box changes (but not while the screen fills boxes itself): checks everything again.
     */
    private void typed()
    {
        if (!updating)
        {
            refresh();
        }
    }

    /*
     +--------------------------------------------------------------------------+
     | refresh()                                                                |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Checks everything as you type. A real mistake (findWrong) comes          |
     | first: the footer names it and its cell turns pink. Otherwise the        |
     | footer names the next thing still to fill in, with no pink, because a    |
     | cell you have not reached yet is not a mistake. Save is on only when     |
     | nothing is left, and the footer then says "Ready to save." A note from   |
     | the zip check (see zipChanged) comes first in the footer.                |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    private void refresh()
    {
        if (updating)
        {
            return;
        }
        zipCheck = zipCodes().check(zipField.getText(), cityField.getText(), stateField.getText());
        String wrong = findWrong();
        JTextField pink = badField;
        JTextField alsoPink = alsoBadField;
        String problem = wrong != null ? wrong : findMistake();
        badField = wrong != null ? pink : null;
        alsoBadField = wrong != null ? alsoPink : null;
        if (problem == null && editing() && changedTees().isEmpty() && newTeeCount() == 0 && !courseCorrected())
        {
            problem = (correcting ? NOTHING_CORRECTED : existingTees.get(0).courseName() + " is already in the program; its tees"
                    + " are shown here. Change what you need, or fill in an empty frame to add a tee.");
        }
        saveButton.setEnabled(problem == null);
        colourBoxes();
        String status = (problem == null ? READY_TO_SAVE + roundsNote() : problem);
        shell.setStatus((zipCheck.message().isEmpty() ? status : zipCheck.message() + "  " + status));
    }

    /*
     * newTeeCount(): how many frames hold a new tee (one with a rating that was not loaded from the file).
     */
    private int newTeeCount()
    {
        int count = 0;
        for (int tee = 0; tee < TEES; tee = tee + 1)
        {
            if (loadedTees[tee] == null && hasRating(tee))
            {
                count = count + 1;
            }
        }
        return count;
    }

    /*
     * roundsNote(): when a changed tee already has rounds, a sentence saying their handicap will be worked out again; otherwise "".
     */
    private String roundsNote()
    {
        List<String> names = new ArrayList<>();
        for (int tee = 0; tee < TEES; tee = tee + 1)
        {
            if (frameChanged(tee) && shell.store().roundsOnTee(loadedTees[tee].id()) > 0)
            {
                names.add(teeNameFields[tee].getText().trim());
            }
        }
        if (names.isEmpty())
        {
            return "";
        }
        return " " + String.join(", ", names) + (names.size() == 1 ? " has" : " have")
                + " saved rounds, so your handicap will be worked out again for them.";
    }

    /*
     * colourBoxes(): pink for a real mistake, cream for the cell with the cursor, white for a box you can type in, grey for one you cannot; an orange outline on State when it is not the zip's state.
     */
    private void colourBoxes()
    {
        for (JTextField field : allBoxes)
        {
            boolean typing = field.isEnabled() && field.isEditable();
            Color face = typing ? Color.WHITE : Theme.FIELD_FACE;
            if (field == cursorCell)
            {
                face = Theme.FIELD_FOCUS;
            }
            if (field == badField || field == alsoBadField)
            {
                face = Theme.FIELD_ERROR;
            }
            field.setBackground(face);
        }
        if (stateBorder == null)
        {
            stateBorder = stateField.getBorder();
        }
        boolean stateDiffers = zipCheck.verdict() == ZipCodes.Verdict.STATE_DIFFERS;
        stateField.setBorder((stateDiffers ? BorderFactory.createLineBorder(ZIP_WARNING, 2) : stateBorder));
    }

    /*
     * updateTotals(): shows each tee's total par: the pars typed so far, over the holes the course has (blank when none are typed).
     */
    private void updateTotals()
    {
        for (int tee = 0; tee < TEES; tee = tee + 1)
        {
            int total = 0;
            boolean any = false;
            for (int hole = 0; hole < playedHoles(); hole = hole + 1)
            {
                String text = parFields[tee][hole].getText();
                if (text.matches("[3-6]"))
                {
                    total = total + Integer.parseInt(text);
                    any = true;
                }
            }
            parTotalBoxes[tee].setText(any ? Integer.toString(total) : "");
        }
    }

    /*
     +--------------------------------------------------------------------------+
     | copyFirstTee()                                                           |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | What the arrow on the first tee does: copies its pars and stroke index   |
     | into the par and SI cells of the other three tees, but only into cells   |
     | that are EMPTY, so nothing you typed is replaced. Nothing else is        |
     | copied (never a rating, slope or yardage). The footer says which tees    |
     | received numbers, or why nothing was copied.                             |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    private void copyFirstTee()
    {
        String first = teeNameFields[0].getText().trim().isEmpty() ? "the first tee" : teeNameFields[0].getText().trim();
        boolean anyToCopy = false;
        for (int hole = 0; hole < playedHoles(); hole = hole + 1)
        {
            anyToCopy = anyToCopy || !parFields[0][hole].getText().isEmpty() || !indexFields[0][hole].getText().isEmpty();
        }
        if (!anyToCopy)
        {
            shell.setStatus("Type the pars and handicap numbers of " + first + " first, then press the arrow to copy them.");
            return;
        }
        List<String> filled = new ArrayList<>();
        updating = true;
        for (int tee = 1; tee < TEES; tee = tee + 1)
        {
            boolean got = false;
            for (int hole = 0; hole < playedHoles(); hole = hole + 1)
            {
                for (JTextField[][] column : new JTextField[][][] {parFields, indexFields})
                {
                    String source = column[0][hole].getText();
                    if (column[tee][hole].getText().isEmpty() && !source.isEmpty())
                    {
                        column[tee][hole].setText(source);
                        got = true;
                    }
                }
            }
            if (got)
            {
                String name = teeNameFields[tee].getText().trim();
                filled.add(name.isEmpty() ? "Tee " + (tee + 1) : name);
            }
        }
        updating = false;
        updateTotals();
        refresh();
        if (filled.isEmpty())
        {
            shell.setStatus("The Par and Hdcp/SI cells of the other tees are already filled, so nothing was copied.");
        }
        else
        {
            shell.setStatus("Copied the pars and handicap numbers of " + first + " into the empty cells of " + String.join(", ", filled)
                    + ". Check any holes that differ on the card.");
        }
    }

    // ------------------------------------------------------------------- names

    /*
     +--------------------------------------------------------------------------+
     | tidyName(text)                                                           |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Tidies a name: spaces at the ends are removed and runs of spaces become  |
     | one. The capitals are changed only if every letter was typed in lower    |
     | case or every letter in upper case; then each word starts with a         |
     | capital and the rest is lower case. So "  pebble   BEACH " is left as    |
     | "pebble BEACH" (mixed), "TPC Sawgrass" and "McDonough" stay as typed,    |
     | and "riverbend gc" becomes "Riverbend Gc".                               |
     |                                                                          |
     | Passed in:  text - the name as typed                                     |
     | Passed out: the tidied name                                              |
     +--------------------------------------------------------------------------+
     */
    static String tidyName(String text)
    {
        String clean = text.trim().replaceAll("\\s+", " ");
        boolean hasLetters = clean.chars().anyMatch(Character::isLetter);
        boolean allLower = clean.equals(clean.toLowerCase(Locale.ROOT));
        boolean allUpper = clean.equals(clean.toUpperCase(Locale.ROOT));
        if (!hasLetters || !(allLower || allUpper))
        {
            return clean;
        }
        StringBuilder result = new StringBuilder();
        boolean wordStart = true;
        for (char letter : clean.toCharArray())
        {
            result.append(wordStart ? Character.toUpperCase(letter) : Character.toLowerCase(letter));
            wordStart = letter == ' ' || letter == '-';
        }
        return result.toString();
    }

    /*
     * tidyBox(field): tidies the name in a box, changing the text only if the tidy name is different.
     * Passed in:  field - a name box
     * Passed out: nothing
     */
    private void tidyBox(JTextField field)
    {
        String tidy = tidyName(field.getText());
        if (!tidy.equals(field.getText()))
        {
            boolean wasUpdating = updating;
            updating = true;
            field.setText(tidy);
            updating = wasUpdating;
            refresh();
        }
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
     | Lists, in the order the cursor visits them, everything it can land on:   |
     | course, zip, city and state, then for each tee in turn its name,         |
     | rating, slope, front-nine rating and slope, back-nine rating and         |
     | slope and yards, then its pars down the holes, then its stroke index     |
     | down the holes; then Save and Cancel. Hidden, greyed and locked boxes    |
     | are left out.                                                            |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: the list, in visiting order                                  |
     +--------------------------------------------------------------------------+
     */
    private List<Component> focusOrder()
    {
        List<Component> order = new ArrayList<>();
        List<Component> candidates = new ArrayList<>(List.of(courseField, zipField, cityField, stateField));
        for (int tee = 0; tee < TEES; tee = tee + 1)
        {
            candidates.addAll(teeBoxes(tee));
        }
        candidates.add(saveButton);
        candidates.add(cancelButton);
        for (Component component : candidates)
        {
            if (usable(component))
            {
                order.add(component);
            }
        }
        return order;
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
     | Makes Tab, Shift+Tab, Enter and the automatic move after a box is        |
     | complete all follow focusOrder, so the cursor goes down one tee's pars   |
     | and stroke index before moving to the next tee.                          |
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
         * getDefaultComponent(root): where the cursor starts: the course name.
         */
        @Override
        public Component getDefaultComponent(Container root)
        {
            return courseField;
        }
    }

    // ------------------------------------------------------------------- page

    /*
     * title(): the header text for this screen: New Course, or Edit Course once a course already in the program is on it.
     */
    @Override
    public String title()
    {
        return editing() ? "Edit Course" : "New Course";
    }

    /*
     * helpKey(): the start of this screen's help file names (course-info.txt and course-help.txt).
     */
    @Override
    public String helpKey()
    {
        return "course";
    }

    /*
     * onShow(): puts the cursor in the course name each time the page is shown.
     */
    @Override
    public void onShow()
    {
        SwingUtilities.invokeLater(() -> courseField.requestFocusInWindow());
    }

    /*
     * prepareNew(): empties every box and names the tees Ladies, Beginner, Intermediate and Expert. Does not call the window.
     */
    private void prepareNew()
    {
        updating = true;
        for (JTextField field : allBoxes)
        {
            field.setText("");
        }
        for (int tee = 0; tee < TEES; tee = tee + 1)
        {
            teeNameFields[tee].setText(DEFAULT_TEE_NAMES[tee]);
        }
        nineHoleCheck.setSelected(false);
        existingTees = new ArrayList<>();
        for (int tee = 0; tee < TEES; tee = tee + 1)
        {
            loadedTees[tee] = null;
            loadedEntries[tee] = "";
        }
        loadedPlace = "";
        badField = null;
        alsoBadField = null;
        correcting = false;
        zipCheck = new ZipCodes.Check(ZipCodes.Verdict.NONE, null, "");
        updating = false;
        saveButton.setEnabled(false);
        applyState();
        scroller.getVerticalScrollBar().setValue(0);
        blankScreen = screenText();
    }

    /*
     * screenText(): everything that can be typed or ticked on the screen, as one piece of text, so typing can be spotted by comparing it with the empty screen.
     */
    private String screenText()
    {
        StringBuilder text = new StringBuilder();
        for (JTextField field : new JTextField[] {courseField, zipField, cityField, stateField})
        {
            text.append(field.getText()).append('|');
        }
        text.append(nineHoleCheck.isSelected()).append('|');
        for (JTextField field : teeNameFields)
        {
            text.append(field.getText()).append('|');
        }
        for (JTextField field : allBoxes)
        {
            text.append(field.getText()).append('|');
        }
        return text.toString();
    }

    /*
     * canLeave(): leaving with nothing typed goes straight away; once anything has been typed it asks "Discard your changes?" first (the Home button, Cancel and the window's close box all ask).
     * Passed in:  nothing
     * Passed out: true to let the window leave
     */
    @Override
    public boolean canLeave()
    {
        return screenText().equals(blankScreen) || shell.askYesNo(RoundPage.DISCARD_QUESTION);
    }

    /*
     * showNew(): empties the screen for a new course and says what to type first.
     * Passed in:  nothing
     * Passed out: nothing
     */
    public void showNew()
    {
        prepareNew();
        shell.setStatus(READY_PROMPT);
        shell.refreshTitle();
    }

    // ------------------------------------------------------------------ checking

    /*
     * mistake(field, message): remembers which box is wrong so it can be marked pink and given the cursor.
     * Passed in:  field - the box, message - what is wrong
     * Passed out: the message
     */
    private String mistake(JTextField field, String message)
    {
        badField = field;
        return message;
    }

    /*
     * teeLabel(tee): how a tee is named in a message: "Blue tee", or "Tee 2" when it has no name.
     */
    private String teeLabel(int tee)
    {
        String name = teeNameFields[tee].getText().trim();
        return name.isEmpty() ? "Tee " + (tee + 1) : name + " tee";
    }

    /*
     * hasRating(tee): whether a tee's rating box has something in it (a tee without one is skipped).
     */
    private boolean hasRating(int tee)
    {
        return !ratingFields[tee].getText().isBlank();
    }

    /*
     +--------------------------------------------------------------------------+
     | findWrong()                                                              |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Looks only for real mistakes, the kind that turn a cell pink at once:    |
     | while correcting, a name and zip that belong to another course (Course   |
     | and Zip both pink);                                                      |
     | a rated tee whose name is already used on the course (ignoring           |
     | capitals, saved tees included), a rating of 0, and on any tee a stroke   |
     | index outside 1 to 18 (1 to 9 on a nine-hole course) or used twice.      |
     | Cells not filled in yet are not mistakes here (see findMistake).         |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: the message, or null (badField is set to the cell)           |
     +--------------------------------------------------------------------------+
     */
    private String findWrong()
    {
        badField = null;
        alsoBadField = null;
        if (correcting && courseClash())
        {
            alsoBadField = zipField;
            return mistake(courseField, SAME_COURSE);
        }
        Set<String> names = new HashSet<>();
        for (Tee tee : notLoaded())
        {
            names.add(tee.teeName().trim().toLowerCase(Locale.ROOT));
        }
        for (int tee = 0; tee < TEES; tee = tee + 1)
        {
            String name = teeNameFields[tee].getText().trim();
            if (hasRating(tee) && !name.isEmpty() && !names.add(name.toLowerCase(Locale.ROOT)))
            {
                return mistake(teeNameFields[tee], "There is already a tee called " + name + " on this course.");
            }
            String rating = ratingFields[tee].getText();
            if (rating.matches("\\d{1,2}(\\.\\d?)?") && Double.parseDouble(rating) <= 0)
            {
                return mistake(ratingFields[tee], teeLabel(tee) + ": the rating must be more than 0.");
            }
            int[] usedOn = new int[HOLES + 1];
            for (int hole = 0; hole < playedHoles(); hole = hole + 1)
            {
                String text = indexFields[tee][hole].getText();
                if (text.isEmpty())
                {
                    continue;
                }
                String where = teeLabel(tee) + ", hole " + (hole + 1) + ": ";
                int rank = Integer.parseInt(text);
                if (rank < 1 || rank > playedHoles())
                {
                    return mistake(indexFields[tee][hole], where + "the handicap number must be 1 to " + playedHoles() + ".");
                }
                if (usedOn[rank] > 0)
                {
                    return mistake(indexFields[tee][hole], where + "handicap number " + rank + " is already used on hole " + usedOn[rank] + ".");
                }
                usedOn[rank] = hole + 1;
            }
        }
        return null;
    }

    /*
     +--------------------------------------------------------------------------+
     | findMistake()                                                            |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Checks everything needed before saving and returns the first thing to    |
     | fill in or fix, naming the tee and hole: the course name and a 5-digit   |
     | zip are needed; a state must be two letters; at least one tee must have  |
     | a rating (until then the footer says what kind of course this is); each  |
     | tee with a rating needs a name not used by another tee of the course     |
     | (ignoring capitals, tees already saved included), a rating like 70.4,    |
     | a slope of 55 to 155, front and back rating and slope in pairs, a par    |
     | of 3 to 6 on every hole and a stroke index that uses each number from    |
     | 1 to 18 (1 to 9 on a nine-hole course) once.                             |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: the message, or null when all is well (badField is set to    |
     |             the box to fix)                                              |
     +--------------------------------------------------------------------------+
     */
    private String findMistake()
    {
        badField = null;
        if (courseField.getText().isBlank())
        {
            return mistake(courseField, READY_PROMPT);
        }
        if (!zipField.getText().matches("\\d{5}"))
        {
            return mistake(zipField, "Type the course's 5-digit zip.");
        }
        if (editing() && !correcting && !typedCourse().equals(existingTees))
        {
            return mistake(courseField, "The course name or zip no longer matches " + existingTees.get(0).courseName()
                    + ". Put it back to save your changes, or press Cancel to leave them.");
        }
        if (!stateField.getText().isEmpty() && !stateField.getText().matches("[A-Z]{2}"))
        {
            return mistake(stateField, "The state must be two letters, for example SC.");
        }
        Set<String> names = new HashSet<>();
        for (Tee tee : notLoaded())
        {
            names.add(tee.teeName().trim().toLowerCase(Locale.ROOT));
        }
        boolean anyRated = false;
        for (int tee = 0; tee < TEES; tee = tee + 1)
        {
            if (!hasRating(tee) && loadedTees[tee] != null)
            {
                return mistake(ratingFields[tee], loadedTees[tee].teeName() + " is already in the program, so it needs its rating.");
            }
            if (!hasRating(tee))
            {
                continue;
            }
            anyRated = true;
            String problem = findTeeMistake(tee, names);
            if (problem != null)
            {
                return problem;
            }
        }
        if (!anyRated)
        {
            return mistake(ratingFields[0], noTeeYet());
        }
        return null;
    }

    /*
     * noTeeYet(): the footer message before any tee has a rating: which tees a course already has, or that it is a new course.
     * Passed in:  nothing
     * Passed out: the message
     */
    private String noTeeYet()
    {
        if (existingTees.isEmpty())
        {
            return "A new course. Fill in the city and state, then each tee you want, starting with its rating.";
        }
        List<String> names = new ArrayList<>();
        for (Tee tee : existingTees)
        {
            names.add(tee.teeName());
        }
        return existingTees.get(0).courseName() + " is already in the program with the tees " + String.join(", ", names)
                + ". The tees you fill in here are added to it.";
    }

    /*
     +--------------------------------------------------------------------------+
     | findTeeMistake(tee, names)                                               |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The checks of findMistake for one tee that has a rating.                 |
     |                                                                          |
     | Passed in:  tee   - 0 to 3                                               |
     |             names - tee names already used on this course, in lower      |
     |                     case; this tee's name is added to it                 |
     | Passed out: the message, or null when the tee is fine                    |
     +--------------------------------------------------------------------------+
     */
    private String findTeeMistake(int tee, Set<String> names)
    {
        String name = teeNameFields[tee].getText().trim();
        if (name.isEmpty())
        {
            return mistake(teeNameFields[tee], "Tee " + (tee + 1) + " has a rating but no name.");
        }
        if (!names.add(name.toLowerCase(Locale.ROOT)))
        {
            return mistake(teeNameFields[tee], "There is already a tee called " + name + " on this course.");
        }
        String label = teeLabel(tee);
        String problem = ratingMistake(ratingFields[tee], slopeFields[tee], label, "rating", "slope", true);
        if (problem == null && !nineHoleCheck.isSelected())
        {
            problem = ratingMistake(frontRatingFields[tee], frontSlopeFields[tee], label, "front-nine rating", "front-nine slope", false);
        }
        if (problem == null && !nineHoleCheck.isSelected())
        {
            problem = ratingMistake(backRatingFields[tee], backSlopeFields[tee], label, "back-nine rating", "back-nine slope", false);
        }
        if (problem != null)
        {
            return problem;
        }
        int[] usedOn = new int[HOLES + 1];
        for (int hole = 0; hole < playedHoles(); hole = hole + 1)
        {
            String where = label + ", hole " + (hole + 1) + ": ";
            String par = parFields[tee][hole].getText();
            if (par.isEmpty())
            {
                return mistake(parFields[tee][hole], where + "the par is missing.");
            }
            if (!par.matches("[3-6]"))
            {
                return mistake(parFields[tee][hole], where + "the par must be 3, 4, 5 or 6.");
            }
        }
        for (int hole = 0; hole < playedHoles(); hole = hole + 1)
        {
            String where = label + ", hole " + (hole + 1) + ": ";
            String text = indexFields[tee][hole].getText();
            if (text.isEmpty())
            {
                return mistake(indexFields[tee][hole], where + "the handicap number is missing.");
            }
            int rank = Integer.parseInt(text);
            if (rank < 1 || rank > playedHoles())
            {
                return mistake(indexFields[tee][hole], where + "the handicap number must be 1 to " + playedHoles() + ".");
            }
            if (usedOn[rank] > 0)
            {
                return mistake(indexFields[tee][hole], where + "handicap number " + rank + " is already used on hole " + usedOn[rank] + ".");
            }
            usedOn[rank] = hole + 1;
        }
        return null;
    }

    /*
     +--------------------------------------------------------------------------+
     | ratingMistake(ratingBox, slopeBox, label, ratingName, slopeName, needed) |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Checks a rating and its slope: a rating like 70.4 and a slope of 55 to   |
     | 155. The tee's own pair is needed; a front or back pair is optional      |
     | but must be both or neither.                                             |
     |                                                                          |
     | Passed in:  ratingBox, slopeBox      - the two boxes                     |
     |             label                    - the tee, for the message          |
     |             ratingName, slopeName    - what to call them in the message  |
     |             needed                   - true if the pair must be filled   |
     | Passed out: the message, or null when they are fine                      |
     +--------------------------------------------------------------------------+
     */
    private String ratingMistake(JTextField ratingBox, JTextField slopeBox, String label, String ratingName,
                                 String slopeName, boolean needed)
    {
        String rating = ratingBox.getText();
        String slope = slopeBox.getText();
        if (!needed && rating.isEmpty() && slope.isEmpty())
        {
            return null;
        }
        if (!needed && (rating.isEmpty() || slope.isEmpty()))
        {
            return mistake(rating.isEmpty() ? ratingBox : slopeBox, label + ": the " + ratingName + " and " + slopeName
                    + " go together. Fill in both or neither.");
        }
        if (!rating.matches("\\d{1,2}(\\.\\d?)?") || Double.parseDouble(rating) <= 0)
        {
            return mistake(ratingBox, label + ": the " + ratingName + " must be a number like 70.4.");
        }
        if (slope.isEmpty())
        {
            return mistake(slopeBox, label + ": the " + slopeName + " is missing.");
        }
        int value = Integer.parseInt(slope);
        if (value < MIN_SLOPE || value > MAX_SLOPE)
        {
            return mistake(slopeBox, label + ": the " + slopeName + " must be " + MIN_SLOPE + " to " + MAX_SLOPE + ".");
        }
        return null;
    }

    // -------------------------------------------------------------------- saving

    /*
     * numbers(fields): the numbers typed in the boxes of the holes the course has.
     * Passed in:  fields - one tee's par or stroke index boxes
     * Passed out: one number per hole
     */
    private int[] numbers(JTextField[] fields)
    {
        int[] values = new int[playedHoles()];
        for (int hole = 0; hole < values.length; hole = hole + 1)
        {
            values[hole] = Integer.parseInt(fields[hole].getText());
        }
        return values;
    }

    /*
     * decimal(field): a rating box as a number, 0 when it is blank.
     */
    private static double decimal(JTextField field)
    {
        return field.getText().isEmpty() ? 0 : Double.parseDouble(field.getText());
    }

    /*
     * whole(field): a slope or yardage box as a number, 0 when it is blank.
     */
    private static int whole(JTextField field)
    {
        return field.getText().isEmpty() ? 0 : Integer.parseInt(field.getText());
    }

    /*
     +--------------------------------------------------------------------------+
     | buildTees(firstId)                                                       |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Turns every NEW tee that has a rating (one not loaded from the file)     |
     | into a Tee, numbered from firstId. For a course already in the program   |
     | its own spelling of the name and its zip are used, so the new tees join  |
     | it exactly. Call it only after findMistake found nothing.                |
     |                                                                          |
     | Passed in:  firstId - the TeeID for the first new tee                    |
     | Passed out: the new tees                                                 |
     +--------------------------------------------------------------------------+
     */
    private List<Tee> buildTees(int firstId)
    {
        List<Tee> tees = new ArrayList<>();
        for (int tee = 0; tee < TEES; tee = tee + 1)
        {
            if (hasRating(tee) && loadedTees[tee] == null)
            {
                tees.add(teeFromFrame(tee, firstId + tees.size()));
            }
        }
        return tees;
    }

    /*
     * teeFromFrame(tee, id): a Tee made from what is typed in one frame and the course bar, with the given TeeID.
     * Passed in:  tee - the frame, 0 to 3; id - its TeeID
     * Passed out: the Tee
     */
    private Tee teeFromFrame(int tee, int id)
    {
        boolean nine = nineHoleCheck.isSelected();
        Tee model = existingTees.isEmpty() ? null : existingTees.get(0);
        String course = ((model == null || correcting) ? courseField.getText().trim() : model.courseName());
        String zip = ((model == null || correcting) ? zipField.getText().trim() : model.zip());
        return new Tee(id, course, cityField.getText().trim(), stateField.getText().trim(), zip,
                teeNameFields[tee].getText().trim(), playedHoles(), decimal(ratingFields[tee]), whole(slopeFields[tee]),
                whole(yardageFields[tee]), numbers(parFields[tee]), numbers(indexFields[tee]),
                nine ? 0 : decimal(frontRatingFields[tee]), nine ? 0 : whole(frontSlopeFields[tee]),
                nine ? 0 : decimal(backRatingFields[tee]), nine ? 0 : whole(backSlopeFields[tee]));
    }

    /*
     +--------------------------------------------------------------------------+
     | changedTees()                                                            |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The saved tees that need their line rewritten: every loaded tee whose    |
     | frame was changed, keeping its TeeID; and, when the city or state was    |
     | changed, every other tee of the course too (with only the city and       |
     | state changed), since they all share them. Tees not changed are left     |
     | out, so their lines are not touched. (A corrected name or zip is saved   |
     | for every tee separately, see save.) Call it only when findMistake       |
     | found nothing.                                                           |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: the changed tees                                             |
     +--------------------------------------------------------------------------+
     */
    private List<Tee> changedTees()
    {
        List<Tee> changed = new ArrayList<>();
        boolean place = placeChanged() && !courseCorrected();
        for (int tee = 0; tee < TEES; tee = tee + 1)
        {
            if (frameChanged(tee) || (place && loadedTees[tee] != null))
            {
                changed.add(teeFromFrame(tee, loadedTees[tee].id()));
            }
        }
        if (place)
        {
            String city = cityField.getText().trim();
            String state = stateField.getText().trim();
            for (Tee saved : notLoaded())
            {
                changed.add(new Tee(saved.id(), saved.courseName(), city, state, saved.zip(), saved.teeName(), saved.holes(),
                        saved.rating(), saved.slope(), saved.yardage(), saved.pars(), saved.strokeIndex(),
                        saved.frontRating(), saved.frontSlope(), saved.backRating(), saved.backSlope()));
            }
        }
        return changed;
    }

    /*
     * skippedTees(): the tees left out because they have no rating although a slope, front or back nine or yardage was typed in them, so the message after saving can say so. Pars and stroke index do not count: the copy arrow may have filled them.
     * Passed in:  nothing
     * Passed out: their names
     */
    private List<String> skippedTees()
    {
        List<String> skipped = new ArrayList<>();
        for (int tee = 0; tee < TEES; tee = tee + 1)
        {
            if (hasRating(tee))
            {
                continue;
            }
            for (JTextField field : List.of(slopeFields[tee], frontRatingFields[tee], frontSlopeFields[tee],
                    backRatingFields[tee], backSlopeFields[tee], yardageFields[tee]))
            {
                if (!field.getText().isEmpty())
                {
                    String name = teeNameFields[tee].getText().trim();
                    skipped.add(name.isEmpty() ? "Tee " + (tee + 1) : name);
                    break;
                }
            }
        }
        return skipped;
    }

    /*
     +--------------------------------------------------------------------------+
     | save()                                                                   |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Save is only on when everything checks out, but the names are tidied     |
     | and everything checked once more first, in case tidying changed          |
     | something. Then, after the window's sample-data question, the changed    |
     | tees of a course already in the program get their lines replaced (the    |
     | file as it was is copied to the backups folder), new tees are added with |
     | TeeIDs, everything is reloaded and the window goes Home with a message   |
     | such as "Updated 1 tee and added 1 tee for Sample Links." A corrected    |
     | name or zip is written into every tee of the course first (only the      |
     | Course, Zip, City and State cells change; a backup copy first). When the |
     | state is not the zip's, it first asks "... Save anyway?" (Go back is     |
     | the default). If the file                                                |
     | cannot be written the footer says why and nothing changes.               |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    private void save()
    {
        tidyBox(courseField);
        tidyBox(cityField);
        for (JTextField field : teeNameFields)
        {
            tidyBox(field);
        }
        refresh();
        if (!saveButton.isEnabled())
        {
            return;
        }
        if (zipCheck.verdict() == ZipCodes.Verdict.STATE_DIFFERS && !shell.confirmOnce(SAVE_ANYWAY_TITLE, zipCodes().saveQuestion(zipField.getText(),
                cityField.getText(), stateField.getText()), "Go back", "Save anyway"))
        {
            return;
        }
        if (!shell.readyToSave())
        {
            return;
        }
        DataStore store = shell.store();
        boolean corrected = courseCorrected();
        int teeCount = existingTees.size();
        if (corrected)
        {
            try
            {
                store.correctCourse(existingTees, courseField.getText().trim(), zipField.getText().trim(), cityField.getText().trim(),
                        stateField.getText().trim());
            }
            catch (IOException failure)
            {
                shell.setStatus("The course could not be corrected: " + failure.getMessage());
                return;
            }
        }
        List<Tee> tees = buildTees(store.nextTeeId());
        List<Tee> changed = changedTees();
        List<String> skipped = skippedTees();
        String course = (editing() ? teeFromFrame(0, 0).courseName() : tees.get(0).courseName());
        try
        {
            if (!changed.isEmpty() || !tees.isEmpty())
            {
                store.updateTees(changed, tees);
            }
        }
        catch (IOException failure)
        {
            shell.setStatus((corrected ? "The course was corrected, but its tees could not be saved: " : "The course could not be saved: ")
                    + failure.getMessage());
            return;
        }
        if (corrected)
        {
            correctedMessage(teeCount, changed.size(), tees.size());
            return;
        }
        List<String> done = new ArrayList<>();
        if (!changed.isEmpty())
        {
            done.add("updated " + changed.size() + (changed.size() == 1 ? " tee" : " tees"));
        }
        if (!tees.isEmpty())
        {
            done.add("added " + tees.size() + (tees.size() == 1 ? " tee" : " tees"));
        }
        String summary = String.join(" and ", done);
        String message = Character.toUpperCase(summary.charAt(0)) + summary.substring(1) + " for " + course + ".";
        if (!changed.isEmpty())
        {
            message = message + " A copy of the file as it was is in the backups folder.";
        }
        if (!skipped.isEmpty())
        {
            message = message + "  " + String.join(", ", skipped) + (skipped.size() == 1 ? " was skipped because it has" : " were skipped because they have")
                    + " no rating.";
        }
        shell.goHome();
        shell.setStatus(message);
    }

    /*
     * correctedMessage(teeCount, changedCount, addedCount): goes Home and says the course was corrected: "Corrected the course. Its 3 sets of tees and all their rounds are unchanged.", plus any tees changed or added at the same time.
     */
    private void correctedMessage(int teeCount, int changedCount, int addedCount)
    {
        String message = "Corrected the course. Its " + teeCount + (teeCount == 1 ? " set of tees and all its rounds are unchanged."
                : " sets of tees and all their rounds are unchanged.");
        if (changedCount > 0 || addedCount > 0)
        {
            message = message + " Also " + (changedCount > 0 ? "updated " + changedCount + (changedCount == 1 ? " tee" : " tees") : "")
                    + (changedCount > 0 && addedCount > 0 ? " and " : "")
                    + (addedCount > 0 ? "added " + addedCount + (addedCount == 1 ? " tee" : " tees") : "") + ".";
        }
        shell.goHome();
        shell.setStatus(message + " A copy of the file as it was is in the backups folder.");
    }

    /*
     * startCorrecting(): what Correct... does: unlocks the Course and Zip boxes of the course on the screen and says what correcting does. The course is not looked up again while they change.
     */
    private void startCorrecting()
    {
        if (!editing())
        {
            return;
        }
        correcting = true;
        applyState();
        refresh();
        shell.setStatus(CORRECT_NOTE);
        courseField.requestFocusInWindow();
    }

    /*
     * isCorrecting(): whether Correct... was pressed for the course on the screen (for the self-test).
     */
    boolean isCorrecting()
    {
        return correcting;
    }

    /*
     * courseCorrected(): whether, while correcting, the name or zip typed differs from the course's own (a change of capitals counts).
     */
    private boolean courseCorrected()
    {
        if (!correcting || existingTees.isEmpty())
        {
            return false;
        }
        Tee original = existingTees.get(0);
        return !courseField.getText().trim().equals(original.courseName()) || !zipField.getText().trim().equals(original.zip());
    }

    /*
     * courseClash(): whether, while correcting, the typed name and zip (capitals and extra spaces ignored) belong to another course already in the program.
     */
    private boolean courseClash()
    {
        if (!courseReady())
        {
            return false;
        }
        Set<Integer> ours = new HashSet<>();
        for (Tee tee : existingTees)
        {
            ours.add(tee.id());
        }
        for (Tee tee : shell.store().teesForCourse(courseField.getText(), zipField.getText()))
        {
            if (!ours.contains(tee.id()))
            {
                return true;
            }
        }
        return false;
    }

    /*
     * zipCodes(): the zip list, read the first time it is needed (never while the page is being built).
     */
    private ZipCodes zipCodes()
    {
        if (zipCodes == null)
        {
            zipCodes = ZipCodes.standard();
        }
        return zipCodes;
    }

    /*
     * useZipCodes(list): uses another zip list (for the self-test).
     */
    void useZipCodes(ZipCodes list)
    {
        zipCodes = list;
    }

    /*
     +--------------------------------------------------------------------------+
     | zipChanged()                                                             |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Runs when the zip changes (after the course lookup). When the 5-digit    |
     | zip is in the zip list and the City and State are BOTH empty, fills      |
     | them in (they stay editable; nothing typed is ever replaced). The        |
     | warnings and hints (a different state or city, an unknown zip) are       |
     | worked out by refresh each time, so they follow City and State too.      |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    private void zipChanged()
    {
        if (updating)
        {
            return;
        }
        ZipCodes.Check check = zipCodes().check(zipField.getText(), cityField.getText(), stateField.getText());
        if (check.verdict() == ZipCodes.Verdict.FILL)
        {
            cityField.setText(check.place().city());
            stateField.setText(check.place().state());
        }
    }

    // ------------------------------------------------------------ small helpers

    /*
     +--------------------------------------------------------------------------+
     | ChangeWatcher                                                            |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Runs an action whenever the text in a box changes.                       |
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
     | BoxFilter                                                                |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Decides what can be typed in a box and tells the screen when the box     |
     | is complete so the cursor can move on. Each kind of box has two          |
     | patterns: what it may hold while being typed, and what counts as         |
     | complete.                                                                |
     |   PAR          - 3, 4, 5 or 6; moves on at once                          |
     |   STROKE_INDEX - 2-9 move on at once; a lone 1 waits, and after it only  |
     |                  0-8 (10 to 18); nothing above 18. On a nine-hole        |
     |                  course 1-9 move on at once. Never starts with 0.        |
     |   SLOPE        - a first digit of 5-9 moves on after two digits (55-99), |
     |                  a 1 after three (100-155); 0, 2, 3 and 4 cannot start   |
     |   RATING       - one or two digits, a point, one digit; moves on after   |
     |                  the digit after the point                               |
     |   YARDAGE      - up to four digits; moves on after the fourth            |
     |   ZIP          - five digits; moves on after the fifth                   |
     |   STATE        - two letters, turned into capitals; moves on after two   |
     | Typing into a box that is already complete starts it over with what      |
     | was typed. Deleting is always allowed. While the screen fills a box      |
     | itself ("loading"), the text goes in as it is and nothing moves on.      |
     |                                                                          |
     | Fields (passed in when the filter is made):                              |
     | kind      - which kind of box                                            |
     | nineHoles - says whether it is a nine-hole course (stroke index)         |
     | loading   - says whether the screen is filling the box itself            |
     | advance   - what to do when the box is complete                          |
     +--------------------------------------------------------------------------+
     */
    static final class BoxFilter extends DocumentFilter
    {
        /*
         * Kind: the kinds of box, each with the pattern of what it may hold while typing and of what is complete.
         */
        enum Kind
        {
            PAR("[3-6]?", "[3-6]"),
            STROKE_INDEX("|[1-9]|1[0-8]", "[2-9]|1[0-8]"),
            SLOPE("|1|1[0-5]|1[0-4]\\d|15[0-5]|[5-9]|5[5-9]|[6-9]\\d", "5[5-9]|[6-9]\\d|1[0-4]\\d|15[0-5]"),
            RATING("|\\d{1,2}|\\d{1,2}\\.\\d?", "\\d{1,2}\\.\\d"),
            YARDAGE("|[1-9]\\d{0,3}", "[1-9]\\d{3}"),
            ZIP("\\d{0,5}", "\\d{5}"),
            STATE("[A-Z]{0,2}", "[A-Z]{2}");

            private final String partial;
            private final String complete;

            /*
             * Kind(partial, complete): remembers the two patterns.
             */
            Kind(String partial, String complete)
            {
                this.partial = partial;
                this.complete = complete;
            }
        }

        private static final String NINE_INDEX = "[1-9]";
        private final Kind kind;
        private final BooleanSupplier nineHoles;
        private final BooleanSupplier loading;
        private final Runnable advance;

        /*
         * BoxFilter(kind, nineHoles, loading, advance): remembers the kind of box, how to tell a nine-hole course, whether the screen is loading it, and what to do when it is complete.
         */
        BoxFilter(Kind kind, BooleanSupplier nineHoles, BooleanSupplier loading, Runnable advance)
        {
            this.kind = kind;
            this.nineHoles = nineHoles;
            this.loading = loading;
            this.advance = advance;
        }

        /*
         * mayHold(text): whether the box may hold this text while it is being typed.
         */
        private boolean mayHold(String text)
        {
            if (kind == Kind.STROKE_INDEX && nineHoles.getAsBoolean())
            {
                return text.isEmpty() || text.matches(NINE_INDEX);
            }
            return text.matches(kind.partial);
        }

        /*
         * isComplete(text): whether the text fills the box, so the cursor moves on.
         */
        private boolean isComplete(String text)
        {
            if (kind == Kind.STROKE_INDEX && nineHoles.getAsBoolean())
            {
                return text.matches(NINE_INDEX);
            }
            return text.matches(kind.complete);
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
            if (kind == Kind.STATE)
            {
                incoming = incoming.toUpperCase(Locale.ROOT);
            }
            if (incoming.isEmpty() || loading.getAsBoolean())
            {
                super.replace(bypass, offset, length, incoming, attributes);
                return;
            }
            String current = bypass.getDocument().getText(0, bypass.getDocument().getLength());
            String result = current.substring(0, offset) + incoming + current.substring(offset + length);
            if (!mayHold(result))
            {
                if (incoming.length() == 1 && isComplete(current) && mayHold(incoming))
                {
                    result = incoming;
                }
                else
                {
                    return;
                }
            }
            bypass.replace(0, bypass.getDocument().getLength(), result, attributes);
            if (isComplete(result))
            {
                advance.run();
            }
        }
    }
}

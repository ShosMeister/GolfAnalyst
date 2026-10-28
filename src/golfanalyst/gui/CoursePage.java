package golfanalyst.gui;

import golfanalyst.DataStore;
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
 | city, state and whether it is a nine-hole course. The tees stay greyed   |
 | until the name and zip are in. If that name (whatever the capitals) and  |
 | zip are already in the program, the city, state and nine-hole box are    |
 | filled in from it and locked, the footer lists the tees it has, and the  |
 | new tees are added to it.                                                |
 |                                                                          |
 | Below, in a scroll pane, are four tee columns, named Ladies, Beginner,   |
 | Intermediate and Expert to start with (they can be renamed). Each has    |
 | its name, rating and slope, the front-nine and back-nine rating and      |
 | slope (optional), the yardage (optional), the total par worked out as    |
 | you type, then the par and stroke index of every hole. Nothing is ever   |
 | copied from one tee to another: a rating invented that way would spoil   |
 | the handicap. A tee whose rating is left blank is skipped.               |
 |                                                                          |
 | Save and Cancel sit under the scroll pane so they are always in view.    |
 | Save checks everything first and the footer names the first mistake by   |
 | tee and hole; the box turns pink and the cursor goes to it. When all is  |
 | well the tees are added to courses.tsv (new TeeIDs, the file's own       |
 | column order) and the window goes back Home saying what was added.       |
 |                                                                          |
 | Sizes (default window 960 x 770, header 76, footer 30): the page gets    |
 | about 625 px. Margins 14, the course bar 32 + 6, the button bar 32 + 6   |
 | and the scroll pane's border 2 leave 533 px to see the tees in. The tee  |
 | grid is 24 rows of 22 px plus 2 = 530, so it fits; on a smaller window   |
 | the scroll bar appears.                                                  |
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
    private static final int ROW_HEIGHT = 22;
    private static final int BOX_HEIGHT = ROW_HEIGHT - 2;
    private static final int BAR_BOX_HEIGHT = 24;
    private static final int TEE_GAP = 14;
    private static final int MIN_SLOPE = 55;
    private static final int MAX_SLOPE = 155;
    private static final String READY_PROMPT = "Type the course name and its 5-digit zip first.";

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
    final ValueBox[] parTotalBoxes = new ValueBox[TEES];
    final JTextField[][] parFields = new JTextField[TEES][HOLES];
    final JTextField[][] indexFields = new JTextField[TEES][HOLES];
    final WoodButton saveButton = new WoodButton("Save");
    final WoodButton cancelButton = new WoodButton("Cancel");
    final JScrollPane scroller;
    final JPanel teeGrid = new JPanel(new GridBagLayout());

    private final List<JTextField> allBoxes = new ArrayList<>();
    private final List<JComponent> frontRow = new ArrayList<>();
    private final List<JComponent> backRow = new ArrayList<>();
    private final List<List<JComponent>> holeRows = new ArrayList<>();
    private final transient EntryTraversal focusPolicy = new EntryTraversal();
    private transient List<Tee> existingTees = new ArrayList<>();
    private JTextField badField = null;
    private boolean updating = false;

    /*
     +--------------------------------------------------------------------------+
     | CoursePage(shell)                                                        |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Builds the screen: the course bar, the tee grid in its scroll pane       |
     | and the Save / Cancel bar, then empties it. Like every page it must      |
     | not call the window while it is being built.                             |
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

        buildTeeGrid();
        JPanel gridTop = new JPanel(new BorderLayout());
        gridTop.setBackground(Color.WHITE);
        gridTop.add(teeGrid, BorderLayout.NORTH);
        scroller = new JScrollPane(gridTop, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroller.setBorder(BorderFactory.createLineBorder(Theme.FRAME_DARK, 1));
        scroller.getVerticalScrollBar().setUnitIncrement(ROW_HEIGHT);
        add(scroller, BorderLayout.CENTER);

        add(buildButtonBar(), BorderLayout.SOUTH);

        saveButton.addActionListener(event -> save());
        cancelButton.addActionListener(event -> shell.goHome());
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
     | The light bar across the top: course name, zip, city, state and the      |
     | nine-hole box. Changing the name or zip looks the course up.             |
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
        nineHoleCheck.setToolTipText("Tick for a nine-hole course (stroke index 1 to 9)");
        DocumentListener lookup = new ChangeWatcher(() -> courseChanged());
        courseField.getDocument().addDocumentListener(lookup);
        zipField.getDocument().addDocumentListener(lookup);
        bar.add(new JLabel("Course:"));
        bar.add(courseField);
        bar.add(new JLabel("Zip:"));
        bar.add(zipField);
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
     | buildTeeGrid()                                                           |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Builds the white grid of tees. Column 0 is the hole number; each tee     |
     | then has four columns (caption, box, caption, box). Rows:                |
     |   0      tee name                                                        |
     |   1      Rating and Slope                                                |
     |   2      Front 9 rating and slope (hidden on a nine-hole course)         |
     |   3      Back 9 rating and slope (hidden on a nine-hole course)          |
     |   4      Yards, and the total par worked out as you type                 |
     |   5      the Hole / Par / SI headings                                    |
     |   6-23   holes 1 to 18 (10 to 18 hidden on a nine-hole course)           |
     | Every row is 22 px: a 20 px box with 1 px above and below.               |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing (fills teeGrid)                                      |
     +--------------------------------------------------------------------------+
     */
    private void buildTeeGrid()
    {
        teeGrid.setBackground(Color.WHITE);
        teeGrid.setBorder(BorderFactory.createEmptyBorder(1, 4, 1, 4));
        for (int hole = 0; hole < HOLES; hole++)
        {
            holeRows.add(new ArrayList<>());
        }
        place(caption("Tee"), 0, 0, 1, 0);
        place(heading("Hole"), 0, 5, 1, 0);
        for (int hole = 0; hole < HOLES; hole++)
        {
            JLabel number = new JLabel(Integer.toString(hole + 1), SwingConstants.CENTER);
            number.setFont(Theme.font(Font.BOLD, 13f));
            number.setPreferredSize(new Dimension(30, BOX_HEIGHT));
            place(number, 0, 6 + hole, 1, 0);
            holeRows.get(hole).add(number);
        }
        for (int tee = 0; tee < TEES; tee++)
        {
            buildTeeColumn(tee, 1 + 4 * tee);
        }
    }

    /*
     +--------------------------------------------------------------------------+
     | buildTeeColumn(tee, base)                                                |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Makes and places every box of one tee (see buildTeeGrid for the rows).   |
     |                                                                          |
     | Passed in:  tee  - which tee, 0 to 3                                     |
     |             base - its first grid column                                 |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    private void buildTeeColumn(int tee, int base)
    {
        teeNameFields[tee] = nameBox(new JTextField(), 176, BOX_HEIGHT);
        teeNameFields[tee].setHorizontalAlignment(SwingConstants.CENTER);
        place(teeNameFields[tee], base, 0, 4, TEE_GAP);

        ratingFields[tee] = numberBox(new JTextField(), BoxFilter.Kind.RATING, 48, BOX_HEIGHT);
        slopeFields[tee] = numberBox(new JTextField(), BoxFilter.Kind.SLOPE, 44, BOX_HEIGHT);
        placePair(base, 1, "Rating", ratingFields[tee], "Slope", slopeFields[tee], null);

        frontRatingFields[tee] = numberBox(new JTextField(), BoxFilter.Kind.RATING, 48, BOX_HEIGHT);
        frontSlopeFields[tee] = numberBox(new JTextField(), BoxFilter.Kind.SLOPE, 44, BOX_HEIGHT);
        placePair(base, 2, "Front 9", frontRatingFields[tee], "Slope", frontSlopeFields[tee], frontRow);

        backRatingFields[tee] = numberBox(new JTextField(), BoxFilter.Kind.RATING, 48, BOX_HEIGHT);
        backSlopeFields[tee] = numberBox(new JTextField(), BoxFilter.Kind.SLOPE, 44, BOX_HEIGHT);
        placePair(base, 3, "Back 9", backRatingFields[tee], "Slope", backSlopeFields[tee], backRow);

        yardageFields[tee] = numberBox(new JTextField(), BoxFilter.Kind.YARDAGE, 48, BOX_HEIGHT);
        yardageFields[tee].setToolTipText("Total yardage (you can leave it blank)");
        parTotalBoxes[tee] = new ValueBox();
        parTotalBoxes[tee].setHorizontalAlignment(SwingConstants.CENTER);
        parTotalBoxes[tee].setPreferredSize(new Dimension(44, BOX_HEIGHT));
        parTotalBoxes[tee].setMinimumSize(new Dimension(44, BOX_HEIGHT));
        parTotalBoxes[tee].setToolTipText("The total par, worked out as you type");
        placePair(base, 4, "Yards", yardageFields[tee], "Par", parTotalBoxes[tee], null);

        place(heading("Par"), base, 5, 2, TEE_GAP);
        place(heading("SI"), base + 2, 5, 2, 0);
        for (int hole = 0; hole < HOLES; hole++)
        {
            parFields[tee][hole] = numberBox(new JTextField(), BoxFilter.Kind.PAR, 36, BOX_HEIGHT);
            parFields[tee][hole].getDocument().addDocumentListener(new ChangeWatcher(() -> updateTotals()));
            indexFields[tee][hole] = numberBox(new JTextField(), BoxFilter.Kind.STROKE_INDEX, 40, BOX_HEIGHT);
            place(parFields[tee][hole], base, 6 + hole, 2, TEE_GAP);
            place(indexFields[tee][hole], base + 2, 6 + hole, 2, 0);
            holeRows.get(hole).add(parFields[tee][hole]);
            holeRows.get(hole).add(indexFields[tee][hole]);
        }
    }

    /*
     * placePair(base, row, firstText, firstBox, secondText, secondBox, rowParts): puts "caption box caption box" across one tee's four columns.
     * Passed in:  base - the tee's first column, row - the grid row, the two captions and boxes,
     *             rowParts - a list to remember them in (so the row can be hidden), or null
     * Passed out: nothing
     */
    private void placePair(int base, int row, String firstText, JComponent firstBox, String secondText,
                           JComponent secondBox, List<JComponent> rowParts)
    {
        JComponent[] parts = {caption(firstText), firstBox, caption(secondText), secondBox};
        for (int col = 0; col < parts.length; col++)
        {
            place(parts[col], base + col, row, 1, col == 0 ? TEE_GAP : 0);
            if (rowParts != null)
            {
                rowParts.add(parts[col]);
            }
        }
    }

    /*
     * place(part, col, row, width, leftGap): adds one part to the tee grid at a column and row.
     * Passed in:  part - the component, col/row - where, width - columns it spans, leftGap - extra space on its left
     * Passed out: nothing
     */
    private void place(JComponent part, int col, int row, int width, int leftGap)
    {
        GridBagConstraints at = new GridBagConstraints();
        at.gridx = col;
        at.gridy = row;
        at.gridwidth = width;
        at.insets = new Insets(1, 2 + leftGap, 1, 2);
        at.anchor = GridBagConstraints.CENTER;
        teeGrid.add(part, at);
    }

    /*
     * caption(text): a small bold caption for a box in the tee grid.
     */
    private static JLabel caption(String text)
    {
        JLabel label = new JLabel(text, SwingConstants.RIGHT);
        label.setFont(Theme.font(Font.BOLD, 12f));
        return label;
    }

    /*
     * heading(text): a grey column heading (Hole, Par, SI), like the round tables.
     */
    private static JLabel heading(String text)
    {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(Theme.font(Font.BOLD, 13f));
        label.setOpaque(true);
        label.setBackground(Theme.FIELD_FACE);
        label.setPreferredSize(new Dimension(text.equals("Hole") ? 34 : 76, BOX_HEIGHT));
        return label;
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
     * commonBehaviour(field): what every box does: Enter moves on, the cursor arriving selects the text and scrolls the box into view, and typing clears the pink mistake mark.
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
     * courseReady(): whether the course name and a 5-digit zip have been typed, which opens the tees.
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
     | already in the program, fills in and locks its city, state and           |
     | nine-hole box and lists its tees in the footer. If it was found a        |
     | moment ago but no longer matches, those boxes are emptied again.         |
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
        boolean ready = courseReady();
        List<Tee> found = ready ? shell.store().teesForCourse(courseField.getText(), zipField.getText()) : new ArrayList<>();
        boolean wasExisting = !existingTees.isEmpty();
        existingTees = found;
        updating = true;
        if (!found.isEmpty())
        {
            cityField.setText(found.get(0).city());
            stateField.setText(found.get(0).state());
            nineHoleCheck.setSelected(found.get(0).holes() == NINE);
        }
        else if (wasExisting)
        {
            cityField.setText("");
            stateField.setText("");
            nineHoleCheck.setSelected(false);
        }
        updating = false;
        applyState();
        if (!ready)
        {
            shell.setStatus(READY_PROMPT);
        }
        else if (found.isEmpty())
        {
            shell.setStatus("A new course. Fill in the city and state, then its tees.");
        }
        else
        {
            List<String> names = new ArrayList<>();
            for (Tee tee : found)
            {
                names.add(tee.teeName());
            }
            shell.setStatus(found.get(0).courseName() + " is already in the program with the tees " + String.join(", ", names)
                    + ". The tees you fill in here are added to it.");
        }
    }

    /*
     * setNineHoles(nine): ticks or unticks the nine-hole box and shows or hides the holes and rows that go with it.
     * Passed in:  nine - true for a nine-hole course
     * Passed out: nothing
     */
    void setNineHoles(boolean nine)
    {
        nineHoleCheck.setSelected(nine);
        applyState();
    }

    /*
     +--------------------------------------------------------------------------+
     | applyState()                                                             |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Brings the screen up to date: the tees are open only once the course     |
     | name and zip are in; the city, state and nine-hole box are locked for    |
     | a course already in the program; a nine-hole course hides the front /    |
     | back rows and holes 10 to 18. Then the colours and par totals.           |
     | It never calls the window, so it is safe while the page is being built.  |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    private void applyState()
    {
        boolean open = courseReady();
        boolean locked = !existingTees.isEmpty();
        boolean nine = nineHoleCheck.isSelected();
        cityField.setEditable(!locked);
        stateField.setEditable(!locked);
        nineHoleCheck.setEnabled(!locked);
        for (JComponent part : frontRow)
        {
            part.setVisible(!nine);
        }
        for (JComponent part : backRow)
        {
            part.setVisible(!nine);
        }
        for (int hole = NINE; hole < HOLES; hole++)
        {
            for (JComponent part : holeRows.get(hole))
            {
                part.setVisible(!nine);
            }
        }
        for (int tee = 0; tee < TEES; tee++)
        {
            for (JTextField field : teeBoxes(tee))
            {
                field.setEnabled(open);
            }
        }
        teeGrid.revalidate();
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
     * typed(): runs after any box changes: the pink mistake mark goes once you start fixing things.
     */
    private void typed()
    {
        if (updating || badField == null)
        {
            return;
        }
        badField = null;
        colourBoxes();
    }

    /*
     * colourBoxes(): white for a box you can type in, grey for one you cannot, pink for the mistake Save found.
     */
    private void colourBoxes()
    {
        for (JTextField field : allBoxes)
        {
            boolean typing = field.isEnabled() && field.isEditable();
            field.setBackground(field == badField ? Theme.FIELD_ERROR : (typing ? Color.WHITE : Theme.FIELD_FACE));
        }
    }

    /*
     * updateTotals(): shows each tee's total par: the pars typed so far, over the holes the course has (blank when none are typed).
     */
    private void updateTotals()
    {
        for (int tee = 0; tee < TEES; tee++)
        {
            int total = 0;
            boolean any = false;
            for (int hole = 0; hole < playedHoles(); hole++)
            {
                String text = parFields[tee][hole].getText();
                if (text.matches("[3-6]"))
                {
                    total += Integer.parseInt(text);
                    any = true;
                }
            }
            parTotalBoxes[tee].setValue(any ? Integer.toString(total) : "");
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
            field.setText(tidy);
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
        for (int tee = 0; tee < TEES; tee++)
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
     * title(): the header text for this screen.
     */
    @Override
    public String title()
    {
        return "New Course";
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
        for (int tee = 0; tee < TEES; tee++)
        {
            teeNameFields[tee].setText(DEFAULT_TEE_NAMES[tee]);
        }
        nineHoleCheck.setSelected(false);
        existingTees = new ArrayList<>();
        badField = null;
        updating = false;
        applyState();
        scroller.getVerticalScrollBar().setValue(0);
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
     | findMistake()                                                            |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Checks everything before saving and returns the first mistake, naming    |
     | the tee and hole: the course name and a 5-digit zip are needed; a        |
     | state must be two letters; at least one tee must have a rating; each     |
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
            return mistake(courseField, "Type the name of the course.");
        }
        if (!zipField.getText().matches("\\d{5}"))
        {
            return mistake(zipField, "The zip must be 5 digits.");
        }
        if (!stateField.getText().isEmpty() && !stateField.getText().matches("[A-Z]{2}"))
        {
            return mistake(stateField, "The state must be two letters, for example SC.");
        }
        Set<String> names = new HashSet<>();
        for (Tee tee : existingTees)
        {
            names.add(tee.teeName().trim().toLowerCase(Locale.ROOT));
        }
        boolean anyRated = false;
        for (int tee = 0; tee < TEES; tee++)
        {
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
            return mistake(ratingFields[0], "Fill in the rating of at least one tee. A tee with no rating is skipped.");
        }
        return null;
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
        for (int hole = 0; hole < playedHoles(); hole++)
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
        for (int hole = 0; hole < playedHoles(); hole++)
        {
            String where = label + ", hole " + (hole + 1) + ": ";
            String text = indexFields[tee][hole].getText();
            if (text.isEmpty())
            {
                return mistake(indexFields[tee][hole], where + "the stroke index is missing.");
            }
            int rank = Integer.parseInt(text);
            if (rank < 1 || rank > playedHoles())
            {
                return mistake(indexFields[tee][hole], where + "the stroke index must be 1 to " + playedHoles() + ".");
            }
            if (usedOn[rank] > 0)
            {
                return mistake(indexFields[tee][hole], where + "stroke index " + rank + " is already used on hole " + usedOn[rank] + ".");
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
        for (int hole = 0; hole < values.length; hole++)
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
     | Turns every tee that has a rating into a Tee, numbered from firstId.     |
     | For a course already in the program its own spelling of the name and     |
     | its city, state and zip are used, so the new tees join it exactly.       |
     | Call it only after findMistake found nothing.                            |
     |                                                                          |
     | Passed in:  firstId - the TeeID for the first new tee                    |
     | Passed out: the new tees                                                 |
     +--------------------------------------------------------------------------+
     */
    private List<Tee> buildTees(int firstId)
    {
        boolean nine = nineHoleCheck.isSelected();
        Tee model = existingTees.isEmpty() ? null : existingTees.get(0);
        String course = model == null ? courseField.getText().trim() : model.courseName();
        String city = model == null ? cityField.getText().trim() : model.city();
        String state = model == null ? stateField.getText().trim() : model.state();
        String zip = model == null ? zipField.getText() : model.zip();
        List<Tee> tees = new ArrayList<>();
        for (int tee = 0; tee < TEES; tee++)
        {
            if (!hasRating(tee))
            {
                continue;
            }
            tees.add(new Tee(firstId + tees.size(), course, city, state, zip, teeNameFields[tee].getText().trim(),
                    playedHoles(), decimal(ratingFields[tee]), whole(slopeFields[tee]), whole(yardageFields[tee]),
                    numbers(parFields[tee]), numbers(indexFields[tee]),
                    nine ? 0 : decimal(frontRatingFields[tee]), nine ? 0 : whole(frontSlopeFields[tee]),
                    nine ? 0 : decimal(backRatingFields[tee]), nine ? 0 : whole(backSlopeFields[tee])));
        }
        return tees;
    }

    /*
     * skippedTees(): the tees left out because they have no rating although something else was typed in them, so the message after saving can say so.
     * Passed in:  nothing
     * Passed out: their names
     */
    private List<String> skippedTees()
    {
        List<String> skipped = new ArrayList<>();
        for (int tee = 0; tee < TEES; tee++)
        {
            if (hasRating(tee))
            {
                continue;
            }
            for (JTextField field : teeBoxes(tee))
            {
                if (field != teeNameFields[tee] && !field.getText().isEmpty())
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
     | Tidies the names, checks everything (findMistake) and, if something is   |
     | wrong, names it in the footer, marks the box pink and puts the cursor    |
     | there. Otherwise, after the window's sample-data question, adds the      |
     | tees to courses.tsv with new TeeIDs and goes Home with a message such    |
     | as "Added 3 tees for Sample Links." If the file cannot be written the    |
     | footer says why and nothing changes.                                     |
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
        String problem = findMistake();
        if (problem != null)
        {
            shell.setStatus(problem);
            colourBoxes();
            if (badField != null)
            {
                JTextField fix = badField;
                fix.scrollRectToVisible(new Rectangle(0, 0, fix.getWidth(), fix.getHeight()));
                SwingUtilities.invokeLater(() -> fix.requestFocusInWindow());
            }
            return;
        }
        if (!shell.readyToSave())
        {
            return;
        }
        DataStore store = shell.store();
        List<Tee> tees = buildTees(store.nextTeeId());
        List<String> skipped = skippedTees();
        try
        {
            store.addTees(tees);
        }
        catch (IOException failure)
        {
            shell.setStatus("The course could not be saved: " + failure.getMessage());
            return;
        }
        String message = "Added " + tees.size() + (tees.size() == 1 ? " tee" : " tees") + " for " + tees.get(0).courseName() + ".";
        if (!skipped.isEmpty())
        {
            message += "  " + String.join(", ", skipped) + (skipped.size() == 1 ? " was skipped because it has" : " were skipped because they have")
                    + " no rating.";
        }
        shell.goHome();
        shell.setStatus(message);
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

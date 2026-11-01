package golfanalyst.gui;

import golfanalyst.DataLoader;
import golfanalyst.Dataset;
import golfanalyst.Exporter;
import golfanalyst.Round;
import golfanalyst.Tee;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/*
 +--------------------------------------------------------------------------+
 | ExportForm                                                               |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | The Export window's content, dressed like every other dialog             |
 | (ConfirmForm.dress) and shown with ConfirmForm.showPanel, so it can be   |
 | tried without a screen. From the top: a few words on what Export does;   |
 | three choices (Everything, which is ticked at first; One course and its  |
 | rounds, with a list of the courses; Rounds from a date, with a From box  |
 | and an optional To box, blank meaning today, both days included);        |
 | "Include historical rounds" (ticked like Home's box); "Save into", a     |
 | folder box with Browse...; a line that always says what would be         |
 | written; then Cancel and Export.                                         |
 |                                                                          |
 | Export stays off until a folder is chosen and something would be         |
 | written. Picking a course in the list or typing a date ticks that        |
 | choice. Nothing is written here: the program window writes the export    |
 | when Export is pressed.                                                  |
 |                                                                          |
 | Public Methods:                                                          |
 | the boxes and buttons (for the self-test), choice(), plan(), folder(),   |
 | answer(), summaryArea(), introArea(), buttonRow(), showOn(owner)         |
 +--------------------------------------------------------------------------+
 */
@SuppressWarnings({"serial", "this-escape"})
final class ExportForm extends JPanel
{

    static final String TITLE = "Export";
    static final String INTRO = "Make a copy of your courses and rounds in the same layout the program uses, so another copy of "
            + "Golf Analyst can Import it. Nothing in your own files is changed.";
    static final String EVERYTHING_TEXT = "Everything (all my courses and rounds)";
    static final String COURSE_TEXT = "One course and its rounds";
    static final String DATES_TEXT = "Rounds from a date";
    static final String HISTORICAL_TEXT = "Include historical rounds";
    static final String TYPE_FROM = "Type the From date, for example 2026-10-22 or 10/22/2026.";
    static final String BAD_TO = "The To date is not a date. Type it like 2026-10-22 or 10/22/2026, or leave it blank for today.";
    static final String FROM_AFTER_TO = "The From date is after the To date.";
    static final String PICK_FOLDER = " Choose the folder to save into.";
    static final String NO_FOLDER = " That folder does not exist.";
    private static final int CONTENT_WIDTH = 520;
    private static final int INDENT = 26;
    private static final int FOLDER_WIDTH = 300;
    private static final int DATE_WIDTH = 110;
    private static final int COURSE_WIDTH = 420;
    final JRadioButton everythingChoice = choiceButton(EVERYTHING_TEXT);
    final JRadioButton courseChoice = choiceButton(COURSE_TEXT);
    final JRadioButton datesChoice = choiceButton(DATES_TEXT);
    final JComboBox<Exporter.CourseItem> courseList = new JComboBox<>();
    final JTextField fromBox = ConfirmForm.textBox("");
    final JTextField toBox = ConfirmForm.textBox("");
    final ImageCheckBox historicalBox = new ImageCheckBox();
    final JTextField folderBox = ConfirmForm.textBox("");
    final WoodButton browseButton = ConfirmForm.dialogButton("Browse...");
    final WoodButton exportButton = ConfirmForm.dialogButton("Export");
    final WoodButton cancelButton = ConfirmForm.dialogButton("Cancel");
    private final JTextArea introArea;
    private final JTextArea summaryArea;
    private final JPanel buttonRow;
    private final transient Dataset data;
    private final transient SettingsForm.Tools tools;
    private final transient Path startFolder;
    private transient Exporter.Plan plan = null;
    private int answer = ConfirmForm.CANCELLED;

    /*
     +--------------------------------------------------------------------------+
     | ExportForm(data, includeHistorical, startFolder, tools)                  |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Lays out the window (see the class comment), wires it and fills the      |
     | line that says what would be written. The course list starts at the      |
     | course of the most recent round. Reads the data only.                    |
     |                                                                          |
     | Passed in:  data              - the golfer's data                        |
     |             includeHistorical - how the historical box starts (Home's)   |
     |             startFolder       - where Browse... opens (LastExportFolder  |
     |                                 or the Documents folder)                 |
     |             tools             - the folder picker (the Settings tools)   |
     | Passed out: the form                                                     |
     +--------------------------------------------------------------------------+
     */
    ExportForm(Dataset data, boolean includeHistorical, Path startFolder, SettingsForm.Tools tools)
    {
        this.data = data;
        this.tools = tools;
        this.startFolder = startFolder;
        List<Exporter.CourseItem> courses = Exporter.courses(data);
        for (Exporter.CourseItem course : courses)
        {
            courseList.addItem(course);
        }
        Exporter.CourseItem latest = latestCourse(courses);
        if (latest != null)
        {
            courseList.setSelectedItem(latest);
        }
        courseList.setFont(Theme.font(java.awt.Font.PLAIN, 13f));
        courseList.setPreferredSize(new Dimension(COURSE_WIDTH, courseList.getPreferredSize().height));
        ButtonGroup group = new ButtonGroup();
        group.add(everythingChoice);
        group.add(courseChoice);
        group.add(datesChoice);
        everythingChoice.setSelected(true);
        courseChoice.setEnabled(!courses.isEmpty());
        courseList.setEnabled(!courses.isEmpty());
        fixWidth(fromBox, DATE_WIDTH);
        fixWidth(toBox, DATE_WIDTH);
        fixWidth(folderBox, FOLDER_WIDTH);
        fromBox.setToolTipText("The first day, for example 2026-10-22 or 10/22/2026");
        toBox.setToolTipText("The last day; leave it blank for today");
        folderBox.setToolTipText("A new folder named GolfAnalyst-export_ and the date is made in here");
        browseButton.setFont(Theme.font(java.awt.Font.BOLD, 13f));
        Dimension browseSize = new Dimension(96, 28);
        browseButton.setPreferredSize(browseSize);
        browseButton.setMinimumSize(browseSize);
        historicalBox.setPictureSize(18);
        historicalBox.setPreferredSize(new Dimension(22, 22));
        historicalBox.setSelected(includeHistorical);

        JPanel content = ConfirmForm.clear(new JPanel(new BorderLayout(0, ConfirmForm.GAP)));
        introArea = ConfirmForm.wrappedText(INTRO, CONTENT_WIDTH);
        content.add(introArea, BorderLayout.NORTH);
        JPanel rows = ConfirmForm.clear(new JPanel(new GridBagLayout()));
        addRow(rows, 0, 0, everythingChoice);
        addRow(rows, 1, 4, courseChoice);
        addRow(rows, 2, 2, line(INDENT, courseList));
        addRow(rows, 3, 4, datesChoice);
        addRow(rows, 4, 2, line(INDENT, dateCaption("From"), fromBox, dateCaption("To"), toBox, hint("(blank means today)")));
        JLabel historicalWords = ConfirmForm.checkLabel(HISTORICAL_TEXT);
        historicalWords.addMouseListener(new java.awt.event.MouseAdapter()
        {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent event)
            {
                historicalBox.toggle();
            }
        });
        addRow(rows, 5, ConfirmForm.GAP, line(0, historicalBox, historicalWords));
        addRow(rows, 6, ConfirmForm.GAP, line(0, ConfirmForm.caption("Save into"), folderBox, browseButton));
        content.add(rows, BorderLayout.CENTER);
        summaryArea = ConfirmForm.wrappedText(" \n ", CONTENT_WIDTH);
        content.add(summaryArea, BorderLayout.SOUTH);

        buttonRow = ConfirmForm.clear(new JPanel(new FlowLayout(FlowLayout.RIGHT, ConfirmForm.BUTTON_GAP, 0)));
        buttonRow.add(cancelButton);
        buttonRow.add(exportButton);
        exportButton.setDefaultRing(true);
        ConfirmForm.dress(this, TITLE, content, buttonRow);

        everythingChoice.addActionListener(event -> refresh());
        courseChoice.addActionListener(event -> refresh());
        datesChoice.addActionListener(event -> refresh());
        courseList.addActionListener(event ->
        {
            courseChoice.setSelected(true);
            refresh();
        });
        DocumentListener dateTyped = whenTyped(() ->
        {
            datesChoice.setSelected(true);
            refresh();
        });
        fromBox.getDocument().addDocumentListener(dateTyped);
        toBox.getDocument().addDocumentListener(dateTyped);
        folderBox.getDocument().addDocumentListener(whenTyped(() -> refresh()));
        historicalBox.setOnChange(() -> refresh());
        browseButton.addActionListener(event -> browse());
        cancelButton.addActionListener(event -> finish(ConfirmForm.CANCELLED));
        exportButton.addActionListener(event -> finish(ConfirmForm.GO));
        refresh();
    }

    /*
     * choiceButton(text): one of the three choices: a see-through radio button in the message font.
     */
    private static JRadioButton choiceButton(String text)
    {
        JRadioButton button = new JRadioButton(text);
        button.setOpaque(false);
        button.setFont(ConfirmForm.messageFont());
        button.setForeground(ConfirmForm.MESSAGE_TEXT);
        return button;
    }

    /*
     * fixWidth(box, width): keeps a box at one width however long its text is (a box with a column count would ignore the width).
     */
    private static void fixWidth(JTextField box, int width)
    {
        box.setColumns(0);
        Dimension size = new Dimension(width, box.getPreferredSize().height);
        box.setPreferredSize(size);
        box.setMinimumSize(size);
    }

    /*
     * dateCaption(text): a 56 x 24 WoodTag for the From and To boxes.
     */
    private static WoodTag dateCaption(String text)
    {
        WoodTag tag = ConfirmForm.caption(text);
        Dimension size = new Dimension(56, 24);
        tag.setPreferredSize(size);
        tag.setMinimumSize(size);
        return tag;
    }

    /*
     * hint(text): a few grey-brown words after a box.
     */
    private static JLabel hint(String text)
    {
        JLabel label = ConfirmForm.checkLabel(text);
        label.setFont(Theme.font(java.awt.Font.ITALIC, 12f));
        return label;
    }

    /*
     * line(indent, parts): the parts side by side on the left, 6 px apart, after an indent.
     */
    private static JPanel line(int indent, JComponent... parts)
    {
        JPanel row = ConfirmForm.clear(new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)));
        if (indent > 0)
        {
            row.add(javax.swing.Box.createHorizontalStrut(indent));
        }
        for (int lp = 0; lp < parts.length; lp = lp + 1)
        {
            if (lp > 0)
            {
                row.add(javax.swing.Box.createHorizontalStrut(6));
            }
            row.add(parts[lp]);
        }
        return row;
    }

    /*
     * addRow(rows, row, gapAbove, part): one full-width row of the choices.
     */
    private static void addRow(JPanel rows, int row, int gapAbove, JComponent part)
    {
        GridBagConstraints at = new GridBagConstraints();
        at.gridx = 0;
        at.gridy = row;
        at.anchor = GridBagConstraints.WEST;
        at.fill = GridBagConstraints.HORIZONTAL;
        at.weightx = 1.0;
        at.insets = new Insets(gapAbove, 0, 0, 0);
        rows.add(part, at);
    }

    /*
     * whenTyped(action): a listener that runs action whenever a box's text changes.
     */
    private static DocumentListener whenTyped(Runnable action)
    {
        return new DocumentListener()
        {
            @Override
            public void insertUpdate(DocumentEvent event)
            {
                action.run();
            }

            @Override
            public void removeUpdate(DocumentEvent event)
            {
                action.run();
            }

            @Override
            public void changedUpdate(DocumentEvent event)
            {
                action.run();
            }
        };
    }

    /*
     * latestCourse(courses): the course of the most recently played round, or the first course, or null.
     */
    private Exporter.CourseItem latestCourse(List<Exporter.CourseItem> courses)
    {
        Round newest = null;
        for (Round round : data.rounds())
        {
            newest = (newest == null || round.date().isAfter(newest.date()) ? round : newest);
        }
        for (Tee tee : data.tees())
        {
            if (newest != null && tee.id() == newest.teeId())
            {
                for (Exporter.CourseItem course : courses)
                {
                    if (course.name().equalsIgnoreCase(tee.courseName().trim()) && course.zip().equals(tee.zip().trim()))
                    {
                        return course;
                    }
                }
            }
        }
        return (courses.isEmpty() ? null : courses.get(0));
    }

    /*
     +--------------------------------------------------------------------------+
     | choice()                                                                 |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | What the window is set to, as an Exporter.Choice, or null when the       |
     | dates cannot be read (then the summary line says what to fix).           |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: the choice, or null                                          |
     +--------------------------------------------------------------------------+
     */
    Exporter.Choice choice()
    {
        boolean historical = historicalBox.isSelected();
        if (courseChoice.isSelected() && courseList.getSelectedItem() != null)
        {
            Exporter.CourseItem course = (Exporter.CourseItem) courseList.getSelectedItem();
            return new Exporter.Choice(Exporter.Kind.COURSE, course.name(), course.zip(), null, null, historical);
        }
        if (datesChoice.isSelected())
        {
            LocalDate from = DataLoader.parseDateOrNull(fromBox.getText());
            LocalDate to = (toBox.getText().isBlank() ? null : DataLoader.parseDateOrNull(toBox.getText()));
            if (from == null || (!toBox.getText().isBlank() && to == null) || from.isAfter((to == null ? LocalDate.now() : to)))
            {
                return null;
            }
            return new Exporter.Choice(Exporter.Kind.DATES, null, null, from, to, historical);
        }
        return new Exporter.Choice(Exporter.Kind.EVERYTHING, null, null, null, null, historical);
    }

    /*
     +--------------------------------------------------------------------------+
     | refresh()                                                                |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Works out what the choices would write and says so in the summary line   |
     | ("This will write 12 sets of tees and 37 rounds (4 of them               |
     | historical)."), or says what to fix in a date, or that a folder is       |
     | still needed. Export is on only when a folder is chosen and something    |
     | would be written.                                                        |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    void refresh()
    {
        Exporter.Choice choice = choice();
        String words;
        if (choice == null)
        {
            plan = null;
            LocalDate from = DataLoader.parseDateOrNull(fromBox.getText());
            boolean badTo = !toBox.getText().isBlank() && DataLoader.parseDateOrNull(toBox.getText()) == null;
            words = (from == null ? TYPE_FROM : (badTo ? BAD_TO : FROM_AFTER_TO));
        }
        else
        {
            plan = Exporter.plan(data, choice);
            words = plan.words();
        }
        Path into = folder();
        if (plan != null && plan.hasWork())
        {
            words = words + (folderBox.getText().isBlank() ? PICK_FOLDER : (into == null ? NO_FOLDER : ""));
        }
        summaryArea.setText(words);
        exportButton.setEnabled(plan != null && plan.hasWork() && into != null);
    }

    /*
     * browse(): picks the folder to save into, starting at the last one used (or the Documents folder).
     */
    private void browse()
    {
        Path picked = tools.chooseFolder(this, "Pick the folder to save the export into", (folder() != null ? folder() : startFolder));
        if (picked != null)
        {
            folderBox.setText(picked.toString());
        }
    }

    /*
     * folder(): the folder in the Save into box, or null when it is empty or not a folder.
     */
    Path folder()
    {
        String text = folderBox.getText().trim();
        if (text.isEmpty())
        {
            return null;
        }
        try
        {
            Path folder = Path.of(text);
            return (Files.isDirectory(folder) ? folder : null);
        }
        catch (java.nio.file.InvalidPathException notPath)
        {
            return null;
        }
    }

    /*
     * plan(): what the window's choices would write, or null when a date cannot be read.
     */
    Exporter.Plan plan()
    {
        return plan;
    }

    /*
     * finish(value): keeps the answer and closes the window the form is in (if it is in one).
     */
    private void finish(int value)
    {
        answer = value;
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window != null)
        {
            window.dispose();
        }
    }

    /*
     * answer(): GO once Export is pressed, CANCELLED otherwise.
     */
    int answer()
    {
        return answer;
    }

    /*
     * summaryArea(): the line that says what would be written.
     */
    JTextArea summaryArea()
    {
        return summaryArea;
    }

    /*
     * introArea(): the words at the top (for the self-test's layout check).
     */
    JTextArea introArea()
    {
        return introArea;
    }

    /*
     * buttonRow(): the row with Cancel and Export (for the self-test's layout check).
     */
    JPanel buttonRow()
    {
        return buttonRow;
    }

    /*
     * showOn(owner): shows the window over the program (Export is the default, Everything has the focus) and waits.
     * Passed in:  owner - something in the program window
     * Passed out: the answer
     */
    int showOn(Component owner)
    {
        answer = ConfirmForm.CANCELLED;
        ConfirmForm.showPanel(this, owner, exportButton, everythingChoice);
        return answer;
    }
}

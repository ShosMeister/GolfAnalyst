package golfanalyst.gui;

import golfanalyst.DataLoader;
import golfanalyst.Importer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;

/*
 +--------------------------------------------------------------------------+
 | ImportForm                                                               |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | The Import window's content. It is built like the other dialogs          |
 | (ConfirmForm's wrapping text, 16 px padding, 12 px gaps, shown with      |
 | ConfirmForm.showPanel, so it is sized with pack()) and can be tried      |
 | without a screen.                                                        |
 |                                                                          |
 | From the top: a few words on what to pick; a Courses file row and a      |
 | Rounds file row, each a box for the path and a Browse... button;         |
 | "Choose a whole data folder..." (fills both rows from a folder holding   |
 | courses.tsv and/or rounds.tsv); "Mark the imported rounds as             |
 | historical"; Check files; a read-only scrolling preview; then Cancel     |
 | and Import. Check files asks the checker what an import would do and     |
 | shows it in the preview; nothing is written. Import stays off until a    |
 | check finds something to add, and goes off again as soon as a path or    |
 | the historical box changes.                                              |
 |                                                                          |
 | Nested types:                                                            |
 | Checker - works out what an import would do (DataStore.checkImport)      |
 |                                                                          |
 | Public Methods:                                                          |
 | check(), fillFromFolder(folder) - what Check files and the folder do     |
 | plan(), answer()                - the checked plan and the answer        |
 | showOn(owner)                   - shows it in a window and waits         |
 +--------------------------------------------------------------------------+
 */
@SuppressWarnings({"serial", "this-escape"})
final class ImportForm extends JPanel
{

    static final String TITLE = "Import";
    static final String INTRO = "Pick a courses file, a rounds file or both, in the same layout as courses.tsv and rounds.tsv "
            + "(tab-separated text with a header row), or a whole data folder. Then press Check files to see what would be added. "
            + "Nothing is changed until you press Import.";
    static final String NOTHING_PICKED = "Pick a courses file, a rounds file or both first.";
    static final String CHECK_AGAIN = "Something was changed. Press Check files again.";
    private static final int PATH_WIDTH = 260;
    private static final int PREVIEW_HEIGHT = 170;
    final JTextField coursesPath = new JTextField();
    final JTextField roundsPath = new JTextField();
    final JButton browseCourses = new JButton("Browse...");
    final JButton browseRounds = new JButton("Browse...");
    final JButton folderButton = new JButton("Choose a whole data folder...");
    final JCheckBox historicalBox = new JCheckBox("Mark the imported rounds as historical");
    final JButton checkButton = new JButton("Check files");
    final JTextArea preview = new JTextArea();
    final JButton importButton = new JButton("Import");
    final JButton cancelButton = new JButton("Cancel");
    private final JTextArea introArea;
    private final JPanel buttonRow;
    private final transient Checker checker;
    private transient Importer.Plan plan = null;
    private int answer = ConfirmForm.CANCELLED;

    /*
     * Checker: works out what importing the files would do; either path may be null.
     */
    interface Checker
    {
        /*
         * check(coursesFile, roundsFile, markHistorical): the plan (nothing is written).
         */
        Importer.Plan check(Path coursesFile, Path roundsFile, boolean markHistorical) throws IOException;
    }

    /*
     +--------------------------------------------------------------------------+
     | ImportForm(checker)                                                      |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Lays out the window's parts (see the class comment) and wires them.      |
     |                                                                          |
     | Passed in:  checker - what Check files asks                              |
     | Passed out: the form                                                     |
     +--------------------------------------------------------------------------+
     */
    ImportForm(Checker checker)
    {
        super(new BorderLayout(0, ConfirmForm.GAP));
        this.checker = checker;
        int pad = ConfirmForm.PADDING;
        setBorder(BorderFactory.createEmptyBorder(pad, pad, pad, pad));
        introArea = ConfirmForm.wrappedText(INTRO);
        add(introArea, BorderLayout.NORTH);

        JPanel rows = new JPanel(new GridBagLayout());
        for (JTextField path : new JTextField[] {coursesPath, roundsPath})
        {
            Dimension size = new Dimension(PATH_WIDTH, path.getPreferredSize().height);
            path.setPreferredSize(size);
            path.setMinimumSize(size);
        }
        coursesPath.setToolTipText("A file laid out like courses.tsv; leave it empty to import rounds only");
        roundsPath.setToolTipText("A file laid out like rounds.tsv; leave it empty to import courses only");
        addRow(rows, 0, new JLabel("Courses file:"), coursesPath, browseCourses);
        addRow(rows, 1, new JLabel("Rounds file:"), roundsPath, browseRounds);
        GridBagConstraints at = new GridBagConstraints();
        at.gridx = 0;
        at.gridy = 2;
        at.gridwidth = 3;
        at.anchor = GridBagConstraints.WEST;
        at.insets = new Insets(4, 0, 0, 0);
        rows.add(folderButton, at);
        JPanel checkRow = new JPanel(new BorderLayout(8, 0));
        checkRow.add(historicalBox, BorderLayout.WEST);
        checkRow.add(checkButton, BorderLayout.EAST);
        at.gridy = 3;
        at.fill = GridBagConstraints.HORIZONTAL;
        at.insets = new Insets(ConfirmForm.GAP, 0, 0, 0);
        rows.add(checkRow, at);
        preview.setEditable(false);
        preview.setLineWrap(true);
        preview.setWrapStyleWord(true);
        preview.setFont(ConfirmForm.messageFont());
        preview.setText("Nothing checked yet.");
        JScrollPane scroller = new JScrollPane(preview, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroller.setPreferredSize(new Dimension(ConfirmForm.MESSAGE_WIDTH, PREVIEW_HEIGHT));
        at.gridy = 4;
        at.fill = GridBagConstraints.BOTH;
        at.weightx = 1.0;
        at.weighty = 1.0;
        rows.add(scroller, at);
        add(rows, BorderLayout.CENTER);

        buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonRow.add(cancelButton);
        buttonRow.add(importButton);
        add(buttonRow, BorderLayout.SOUTH);
        importButton.setEnabled(false);

        DocumentListener changed = new DocumentListener()
        {
            @Override
            public void insertUpdate(DocumentEvent event)
            {
                forgetCheck();
            }

            @Override
            public void removeUpdate(DocumentEvent event)
            {
                forgetCheck();
            }

            @Override
            public void changedUpdate(DocumentEvent event)
            {
                forgetCheck();
            }
        };
        coursesPath.getDocument().addDocumentListener(changed);
        roundsPath.getDocument().addDocumentListener(changed);
        historicalBox.addActionListener(event -> forgetCheck());
        browseCourses.addActionListener(event -> browse(coursesPath, "Pick the courses file"));
        browseRounds.addActionListener(event -> browse(roundsPath, "Pick the rounds file"));
        folderButton.addActionListener(event -> browseFolder());
        checkButton.addActionListener(event -> check());
        cancelButton.addActionListener(event -> finish(ConfirmForm.CANCELLED));
        importButton.addActionListener(event -> finish(ConfirmForm.GO));
    }

    /*
     * addRow(rows, row, label, path, browse): one file row: the label, the path box and its Browse... button.
     */
    private static void addRow(JPanel rows, int row, JLabel label, JTextField path, JButton browse)
    {
        GridBagConstraints at = new GridBagConstraints();
        at.gridy = row;
        at.insets = new Insets((row == 0 ? 0 : 4), 0, 0, 8);
        at.anchor = GridBagConstraints.WEST;
        at.gridx = 0;
        rows.add(label, at);
        at.gridx = 1;
        at.fill = GridBagConstraints.HORIZONTAL;
        at.weightx = 1.0;
        rows.add(path, at);
        at.gridx = 2;
        at.fill = GridBagConstraints.NONE;
        at.weightx = 0.0;
        at.insets = new Insets((row == 0 ? 0 : 4), 0, 0, 0);
        rows.add(browse, at);
    }

    /*
     * forgetCheck(): a path or the historical box changed, so the last check no longer holds: Import goes off until Check files is pressed again.
     */
    private void forgetCheck()
    {
        if (plan != null)
        {
            preview.setText(CHECK_AGAIN);
        }
        plan = null;
        importButton.setEnabled(false);
    }

    /*
     * pathIn(box): the path typed in a box, or null when it is empty.
     */
    private static Path pathIn(JTextField box)
    {
        String text = box.getText().trim();
        return (text.isEmpty() ? null : Path.of(text));
    }

    /*
     +--------------------------------------------------------------------------+
     | check()                                                                  |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | What Check files does: asks the checker what importing the two files     |
     | would do and puts the answer in the preview, in plain words. Import      |
     | comes on only when something would be added. Nothing is written.         |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    void check()
    {
        Path courses = pathIn(coursesPath);
        Path rounds = pathIn(roundsPath);
        plan = null;
        importButton.setEnabled(false);
        if (courses == null && rounds == null)
        {
            preview.setText(NOTHING_PICKED);
            return;
        }
        try
        {
            plan = checker.check(courses, rounds, historicalBox.isSelected());
            preview.setText(plan.summary());
            importButton.setEnabled(plan.hasWork());
        }
        catch (IOException | RuntimeException failure)
        {
            plan = null;
            preview.setText("The files could not be read: " + failure.getMessage());
        }
        preview.setCaretPosition(0);
    }

    /*
     * fillFromFolder(folder): fills both rows from a folder: its courses.tsv and rounds.tsv where they exist, empty where not (the preview says if it has neither).
     * Passed in:  folder - the folder picked
     * Passed out: nothing
     */
    void fillFromFolder(Path folder)
    {
        Path courses = folder.resolve(DataLoader.COURSES_FILE);
        Path rounds = folder.resolve(DataLoader.ROUNDS_FILE);
        coursesPath.setText(Files.isRegularFile(courses) ? courses.toString() : "");
        roundsPath.setText(Files.isRegularFile(rounds) ? rounds.toString() : "");
        if (coursesPath.getText().isEmpty() && roundsPath.getText().isEmpty())
        {
            preview.setText("That folder has no " + DataLoader.COURSES_FILE + " or " + DataLoader.ROUNDS_FILE + ".");
        }
    }

    /*
     * browse(box, title): lets the golfer pick a file and puts its path in the box.
     */
    private void browse(JTextField box, String title)
    {
        JFileChooser chooser = new JFileChooser(startFolder(box));
        chooser.setDialogTitle(title);
        chooser.setFileFilter(new FileNameExtensionFilter("Tab-separated text (*.tsv, *.txt)", "tsv", "txt"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
        {
            box.setText(chooser.getSelectedFile().getPath());
        }
    }

    /*
     * browseFolder(): lets the golfer pick a data folder and fills both rows from it.
     */
    private void browseFolder()
    {
        JFileChooser chooser = new JFileChooser(startFolder(coursesPath));
        chooser.setDialogTitle("Pick a folder with courses.tsv and/or rounds.tsv");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
        {
            fillFromFolder(chooser.getSelectedFile().toPath());
        }
    }

    /*
     * startFolder(box): where a file picker opens: the folder of the path in the box, or the user's own folder.
     */
    private static java.io.File startFolder(JTextField box)
    {
        Path path = pathIn(box);
        Path folder = (path == null ? null : path.toAbsolutePath().getParent());
        return (folder != null && Files.isDirectory(folder) ? folder.toFile() : null);
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
     * plan(): what the last check found, or null if there has been no check since the last change.
     */
    Importer.Plan plan()
    {
        return plan;
    }

    /*
     * answer(): GO once Import is pressed, CANCELLED otherwise.
     */
    int answer()
    {
        return answer;
    }

    /*
     * introArea(): the words at the top (for the self-test's layout check).
     */
    JTextArea introArea()
    {
        return introArea;
    }

    /*
     * buttonRow(): the row with Cancel and Import (for the self-test's layout check).
     */
    JPanel buttonRow()
    {
        return buttonRow;
    }

    /*
     * showOn(owner): shows the form in a window over the program (Check files is the default button, the courses box has the focus) and waits.
     * Passed in:  owner - something in the program window
     * Passed out: the answer
     */
    int showOn(Component owner)
    {
        answer = ConfirmForm.CANCELLED;
        ConfirmForm.showPanel(this, owner, TITLE, checkButton, coursesPath);
        return answer;
    }
}

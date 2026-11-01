package golfanalyst.gui;

import golfanalyst.DataLocation;
import golfanalyst.DataStore;
import golfanalyst.Settings;
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
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/*
 +--------------------------------------------------------------------------+
 | SettingsForm                                                             |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | The Settings window's content, dressed like every other dialog           |
 | (ConfirmForm.dress) and shown with ConfirmForm.showPanel, so it can be   |
 | tried without a screen. Rows: Your name; Backups to keep (10, 20, 50,    |
 | 100, All); Also copy every backup to (the folder or "not set", Browse... |
 | and Clear); Data folder (its path, Change... and Open data folder); Open |
 | backups folder; a note line; Cancel and OK.                              |
 |                                                                          |
 | Nothing is saved here: the window only collects the choices, and the     |
 | program window saves them when OK is pressed (Cancel changes nothing).   |
 | Change... picks a folder, says what is in it and asks whether to use it  |
 | as it is or copy the data there; that too only happens on OK, and only   |
 | from the next time the program is opened.                                |
 |                                                                          |
 | On the sample data only the name is saved, so the backup rows are off,   |
 | and the data folder row says it is the sample data, with "Create my own  |
 | data folder" instead of Change....                                       |
 |                                                                          |
 | Nested types:                                                            |
 | Tools - what the window asks of the program (pick a folder, open one,    |
 |         ask a question, a footer message); the self-test gives its own   |
 |                                                                          |
 | Public Methods:                                                          |
 | the boxes and buttons (for the self-test), answer(), name(),             |
 | backupsToKeep(), copyFolder(), newDataFolder(), copyData(), showOn(owner)|
 +--------------------------------------------------------------------------+
 */
@SuppressWarnings({"serial", "this-escape"})
final class SettingsForm extends JPanel
{

    static final String TITLE = "Settings";
    static final String INTRO = "Your name and these settings are kept in settings.txt in your data folder. Nothing is changed "
            + "until you press OK.";
    static final String NOT_SET = "not set";
    static final String SAMPLE_NOTE = "This is the sample data, so only your name is kept here.";
    static final String SHOWING_SAMPLE = "The program is showing the sample data";
    static final String COPY_DATA = "Copy my data there and use it";
    static final String USE_AS_IT_IS = "Use this folder as it is";
    static final int CHOICE_CANCELLED = ConfirmForm.CANCELLED;
    static final int CHOICE_USE = ConfirmForm.GO;
    static final int CHOICE_COPY = ConfirmForm.NO;
    private static final int CAPTION_WIDTH = 190;
    private static final int BOX_WIDTH = 300;
    final JTextField nameBox;
    final JComboBox<String> keepChoice = new JComboBox<>();
    final JTextField copyFolderBox = ConfirmForm.textBox(NOT_SET);
    final WoodButton browseCopy = smallButton("Browse...");
    final WoodButton clearCopy = smallButton("Clear");
    final JTextField dataFolderBox = ConfirmForm.textBox("");
    final WoodButton changeData = smallButton("Change...");
    final WoodButton openData = smallButton("Open data folder");
    final WoodButton createOwn = smallButton("Create my own data folder");
    final WoodButton openBackups = smallButton("Open backups folder");
    final JTextArea noteArea;
    final WoodButton okButton = ConfirmForm.dialogButton("OK");
    final WoodButton cancelButton = ConfirmForm.dialogButton("Cancel");
    private final JTextArea introArea;
    private final JPanel buttonRow;
    private final transient Tools tools;
    private final Path dataDir;
    private final boolean sample;
    private transient Path copyFolder;
    private transient Path newDataFolder = null;
    private boolean copyData = false;
    private int answer = ConfirmForm.CANCELLED;

    /*
     +--------------------------------------------------------------------------+
     | Tools                                                                    |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | What the Settings window asks of the program: pick a folder, open one in |
     | the system's file manager, ask the "use this folder" question, make the  |
     | golfer's own data folder (the sample-data safeguard) and put a message   |
     | in the footer. The program window gives the real ones; the self-test     |
     | gives its own.                                                           |
     +--------------------------------------------------------------------------+
     */
    interface Tools
    {
        /*
         * chooseFolder(parent, title, start): the folder picked, or null if the picker was cancelled.
         */
        Path chooseFolder(Component parent, String title, Path start);

        /*
         * open(folder): opens a folder in the system's file manager; false if this computer cannot.
         */
        boolean open(Path folder);

        /*
         * ask(form): shows a question and gives its answer (ConfirmForm.GO, NO or CANCELLED).
         */
        int ask(ConfirmForm form);

        /*
         * createOwnDataFolder(): the sample-data safeguard: asks whether to make the golfer's own data folder, and makes it on Yes.
         */
        void createOwnDataFolder();

        /*
         * say(text): puts a message in the program window's footer.
         */
        void say(String text);
    }

    /*
     +--------------------------------------------------------------------------+
     | SettingsForm(store, tools)                                               |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Lays out the window with the settings as they are now and wires the      |
     | buttons. Reads the data store only; writes nothing.                      |
     |                                                                          |
     | Passed in:  store - the data in use, tools - see Tools                   |
     | Passed out: the form                                                     |
     +--------------------------------------------------------------------------+
     */
    SettingsForm(DataStore store, Tools tools)
    {
        this.tools = tools;
        this.dataDir = store.dir();
        this.sample = store.isSampleData();
        Settings settings = Settings.load(dataDir);
        nameBox = ConfirmForm.textBox(store.golferName());
        for (int count : Settings.KEEP_CHOICES)
        {
            keepChoice.addItem(Settings.keepText(count));
        }
        keepChoice.setSelectedItem(Settings.keepText(settings.backupsToKeep()));
        keepChoice.setFont(golfanalyst.gui.Theme.font(java.awt.Font.PLAIN, 13f));
        copyFolder = settings.backupCopyFolder();
        copyFolderBox.setText(copyFolder == null ? NOT_SET : copyFolder.toString());
        copyFolderBox.setEditable(false);
        dataFolderBox.setText(sample ? SHOWING_SAMPLE : dataDir.toAbsolutePath().normalize().toString());
        dataFolderBox.setEditable(false);
        for (JTextField box : new JTextField[] {nameBox, copyFolderBox, dataFolderBox})
        {
            box.setColumns(0);
            Dimension size = new Dimension(BOX_WIDTH, box.getPreferredSize().height);
            box.setPreferredSize(size);
            box.setMinimumSize(size);
        }

        JPanel content = ConfirmForm.clear(new JPanel(new BorderLayout(0, ConfirmForm.GAP)));
        introArea = ConfirmForm.wrappedText(INTRO, CAPTION_WIDTH + BOX_WIDTH + 8);
        content.add(introArea, BorderLayout.NORTH);
        JPanel rows = ConfirmForm.clear(new JPanel(new GridBagLayout()));
        addRow(rows, 0, "Your name", nameBox);
        addRow(rows, 1, "Backups to keep", leftAligned(keepChoice));
        addRow(rows, 2, "Also copy every backup to", copyFolderBox);
        addRow(rows, 3, null, leftAligned(browseCopy, clearCopy));
        addRow(rows, 4, "Data folder", dataFolderBox);
        addRow(rows, 5, null, (sample ? leftAligned(createOwn) : leftAligned(changeData, openData)));
        addRow(rows, 6, null, leftAligned(openBackups));
        content.add(rows, BorderLayout.CENTER);
        noteArea = ConfirmForm.wrappedText(sample ? SAMPLE_NOTE + "\n \n " : " \n \n ", CAPTION_WIDTH + BOX_WIDTH + 8);
        content.add(noteArea, BorderLayout.SOUTH);
        if (sample)
        {
            keepChoice.setEnabled(false);
            browseCopy.setEnabled(false);
            clearCopy.setEnabled(false);
        }

        buttonRow = ConfirmForm.clear(new JPanel(new FlowLayout(FlowLayout.RIGHT, ConfirmForm.BUTTON_GAP, 0)));
        buttonRow.add(cancelButton);
        buttonRow.add(okButton);
        okButton.setDefaultRing(true);
        ConfirmForm.dress(this, TITLE, content, buttonRow);

        browseCopy.addActionListener(event -> browseCopyFolder());
        clearCopy.addActionListener(event ->
        {
            copyFolder = null;
            copyFolderBox.setText(NOT_SET);
        });
        changeData.addActionListener(event -> changeDataFolder());
        openData.addActionListener(event -> openFolder(dataDir, "The data folder"));
        createOwn.addActionListener(event -> tools.createOwnDataFolder());
        openBackups.addActionListener(event -> openBackupsFolder());
        okButton.addActionListener(event -> finish(ConfirmForm.GO));
        cancelButton.addActionListener(event -> finish(ConfirmForm.CANCELLED));
    }

    /*
     * smallButton(text): a 28 px tall WoodButton for a row of this window, wide enough for its words.
     */
    private static WoodButton smallButton(String text)
    {
        WoodButton button = ConfirmForm.dialogButton(text);
        button.setFont(golfanalyst.gui.Theme.font(java.awt.Font.BOLD, 13f));
        int width = Math.max(96, button.getFontMetrics(button.getFont()).stringWidth(text) + 28);
        Dimension size = new Dimension(width, 28);
        button.setPreferredSize(size);
        button.setMinimumSize(size);
        return button;
    }

    /*
     * leftAligned(parts): the parts side by side on the left, 8 px apart.
     */
    private static JPanel leftAligned(JComponent... parts)
    {
        JPanel row = ConfirmForm.clear(new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)));
        for (int lp = 0; lp < parts.length; lp = lp + 1)
        {
            if (lp > 0)
            {
                row.add(javax.swing.Box.createHorizontalStrut(8));
            }
            row.add(parts[lp]);
        }
        return row;
    }

    /*
     * addRow(rows, row, caption, value): one row: a wooden caption (or none) and its box or buttons.
     */
    private static void addRow(JPanel rows, int row, String caption, JComponent value)
    {
        GridBagConstraints at = new GridBagConstraints();
        at.gridy = row;
        at.anchor = GridBagConstraints.WEST;
        at.insets = new Insets((row == 0 ? 0 : 6), 0, 0, 8);
        at.gridx = 0;
        if (caption != null)
        {
            WoodTag tag = ConfirmForm.caption(caption);
            Dimension size = new Dimension(CAPTION_WIDTH, 24);
            tag.setPreferredSize(size);
            tag.setMinimumSize(size);
            rows.add(tag, at);
        }
        else
        {
            rows.add(javax.swing.Box.createRigidArea(new Dimension(CAPTION_WIDTH, 1)), at);
        }
        at.gridx = 1;
        at.insets = new Insets((row == 0 ? 0 : 6), 0, 0, 0);
        at.fill = GridBagConstraints.HORIZONTAL;
        at.weightx = 1.0;
        rows.add(value, at);
    }

    /*
     * browseCopyFolder(): picks the folder every backup is also copied to (a USB stick or a cloud folder, say).
     */
    private void browseCopyFolder()
    {
        Path picked = tools.chooseFolder(this, "Pick a folder to copy every backup to", copyFolder);
        if (picked != null)
        {
            copyFolder = picked;
            copyFolderBox.setText(picked.toString());
        }
    }

    /*
     +--------------------------------------------------------------------------+
     | changeDataFolder()                                                       |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | What Change... does: picks a folder, says what is in it ("This folder    |
     | has 37 rounds and 12 courses." or "This folder is empty.") and asks:     |
     | "Use this folder as it is", "Copy my data there and use it" or Cancel.   |
     | If a data file is already there only "Use this folder as it is" is       |
     | offered, so nothing can be written over. The choice is kept for OK; it   |
     | takes effect when the program is next opened. The old folder is never    |
     | touched.                                                                 |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    void changeDataFolder()
    {
        Path picked = tools.chooseFolder(this, "Pick the data folder to use", dataDir.toAbsolutePath());
        if (picked == null)
        {
            return;
        }
        if (picked.toAbsolutePath().normalize().equals(dataDir.toAbsolutePath().normalize()))
        {
            noteArea.setText("That is the data folder already in use.");
            return;
        }
        DataLocation.Contents contents = DataLocation.describe(picked);
        int choice = tools.ask(folderQuestion(picked, contents));
        if (choice == CHOICE_CANCELLED)
        {
            return;
        }
        newDataFolder = picked;
        copyData = choice == CHOICE_COPY;
        dataFolderBox.setText(picked.toAbsolutePath().normalize().toString());
        noteArea.setText("When you press OK, " + (copyData ? "your data is copied to this folder and " : "")
                + "the program uses this folder the next time it is opened. Your old folder is not changed.");
    }

    /*
     * folderQuestion(folder, contents): the question Change... asks about a folder; its answers are CHOICE_USE, CHOICE_COPY or CHOICE_CANCELLED.
     * Passed in:  folder - the folder picked, contents - what is in it
     * Passed out: the question
     */
    static ConfirmForm folderQuestion(Path folder, DataLocation.Contents contents)
    {
        String message = folder.toAbsolutePath().normalize() + "\n" + contents.words() + " The change takes effect when the program "
                + "is next opened, and your old folder is not changed.";
        if (contents.hasFiles())
        {
            return ConfirmForm.choices(message + " A data file is already there, so your data cannot be copied into it.",
                    USE_AS_IT_IS, null).withTitle("Data folder");
        }
        return ConfirmForm.choices(message, USE_AS_IT_IS, COPY_DATA).withTitle("Data folder");
    }

    /*
     * openFolder(folder, what): opens a folder in the system's file manager, or says in the footer that this computer cannot.
     */
    private void openFolder(Path folder, String what)
    {
        if (!tools.open(folder))
        {
            tools.say("This computer cannot open folders from Golf Analyst. " + what + " is " + folder.toAbsolutePath().normalize() + ".");
        }
    }

    /*
     * openBackupsFolder(): opens the backups folder, or says there are no backups yet.
     */
    private void openBackupsFolder()
    {
        Path backups = dataDir.resolve(golfanalyst.Backups.FOLDER);
        if (!Files.isDirectory(backups))
        {
            tools.say("There are no backups yet.");
            return;
        }
        openFolder(backups, "The backups folder");
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
     * answer(): GO once OK is pressed, CANCELLED otherwise.
     */
    int answer()
    {
        return answer;
    }

    /*
     * name(): the name typed, trimmed.
     */
    String name()
    {
        return nameBox.getText().trim();
    }

    /*
     * backupsToKeep(): the Backups to keep choice as it is written ("10" ... "All").
     */
    String backupsToKeep()
    {
        return String.valueOf(keepChoice.getSelectedItem());
    }

    /*
     * copyFolder(): the folder every backup is also copied to, or null for none.
     */
    Path copyFolder()
    {
        return copyFolder;
    }

    /*
     * newDataFolder(): the data folder chosen with Change..., or null when it was not changed.
     */
    Path newDataFolder()
    {
        return newDataFolder;
    }

    /*
     * copyData(): whether the data is to be copied into the new data folder.
     */
    boolean copyData()
    {
        return copyData;
    }

    /*
     * isSample(): whether the window was opened on the sample data (then only the name is saved).
     */
    boolean isSample()
    {
        return sample;
    }

    /*
     * introArea(), buttonRow(): the words at the top and the row with Cancel and OK (for the self-test's layout check).
     */
    JTextArea introArea()
    {
        return introArea;
    }

    /*
     * buttonRow(): the row with Cancel and OK (for the self-test's layout check).
     */
    JPanel buttonRow()
    {
        return buttonRow;
    }

    /*
     * showOn(owner): shows the window over the program (OK is the default, the name box has the focus) and waits.
     * Passed in:  owner - something in the program window
     * Passed out: the answer
     */
    int showOn(Component owner)
    {
        answer = ConfirmForm.CANCELLED;
        ConfirmForm.showPanel(this, owner, okButton, nameBox);
        return answer;
    }
}

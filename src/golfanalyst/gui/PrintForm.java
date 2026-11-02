package golfanalyst.gui;

import golfanalyst.BestRound;
import golfanalyst.CourseAnalysis;
import golfanalyst.DataLoader;
import golfanalyst.Handicap;
import golfanalyst.Period;
import golfanalyst.Round;
import golfanalyst.StatsBoard;
import golfanalyst.Tee;
import golfanalyst.print.CourseNotesReport;
import golfanalyst.print.HandicapReport;
import golfanalyst.print.MyScorecardReport;
import golfanalyst.print.Report;
import golfanalyst.print.ReportData;
import golfanalyst.print.RoundLogReport;
import golfanalyst.print.ScorecardReport;
import golfanalyst.print.StatsBoardReport;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.print.PrinterException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
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
 | PrintForm                                                                |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | The Print window's content, dressed like every other dialog              |
 | (ConfirmForm.dress, title "Print") and shown with ConfirmForm.showPanel, |
 | so it can be tried without a screen.                                     |
 |                                                                          |
 | Left: "What would you like to print?" and seven choices, each with a     |
 | grey hint under it. Right: a sunken panel (the value boxes' one pixel    |
 | edge: dark grey top and left, white bottom and right, face               |
 | (251,247,238)) with the chosen report's name in dark brown bold 15, its  |
 | options, and at its top right the small picture of page 1 (the real      |
 | report, redrawn whenever a choice changes). Under them "Send it to:"     |
 | with A printer or PDF and A spreadsheet file (.csv), a grey sunken box   |
 | with a note sized to show all of its words, then Cancel and Print...     |
 | (Save... for a spreadsheet), with the header's i and ? buttons (32 px)   |
 | at the left of that row, opening print-info.txt and print-help.txt over  |
 | the window.                                                              |
 |                                                                          |
 | Options: Handicap Report none; Round Log course and tees ("All courses"  |
 | first), Period, From and To (open only for Custom dates...) and Include  |
 | historical rounds; A round's scorecard the round, newest first; Best     |
 | Round course and tees and Single / Composite; Stats Board Include        |
 | historical rounds; My scorecard for a course the tees and the Handicap   |
 | Index used; Course notes the Analyzer's rows. Every pulldown lists only  |
 | what exists.                                                             |
 |                                                                          |
 | Print... checks there is something to print (otherwise a footer          |
 | message and the window stays), then hands the report to the printer      |
 | tool; when it has gone the window closes. Nothing is asked of the        |
 | program window while the form is built.                                  |
 |                                                                          |
 | Public Methods:                                                          |
 | the controls (for the self-test), chosen(), report(), refresh(),         |
 | noteArea(), answer(), doneMessage(), showOn(owner)                       |
 +--------------------------------------------------------------------------+
 */
@SuppressWarnings({"serial", "this-escape"})
final class PrintForm extends JPanel
{

    static final String TITLE = "Print";
    static final String QUESTION = "What would you like to print?";
    static final String SEND_TO = "Send it to:";
    static final String PRINTER_TEXT = "A printer or PDF";
    static final String CSV_TEXT = "A spreadsheet file (.csv)";
    static final String PRINTER_NOTE = " In the next window you pick the printer. On Windows, choose \"Microsoft Print to PDF\" "
            + "to make a PDF file.";
    static final String NOTHING_NOTE = "There is nothing to print for this choice yet.";
    static final String NOTHING_SAID = "There is nothing to print for that choice.";
    static final String SENT = "Sent to the printer.";
    static final String SAVE_NOTE = " in a spreadsheet file. You choose where in the next window.";
    static final String TABLES_ONLY = "Only for the reports that are tables";
    static final String HANDICAP_LINE = "Your last 20 rounds and how your index is worked out.";
    static final String HISTORICAL_TEXT = "Include historical rounds";
    static final String BAD_FROM = "Type the From date, for example 2026-10-07 or 10/7/2026.";
    static final String BAD_TO = "Type the To date, for example 2026-10-07 or 10/7/2026.";
    static final String FROM_AFTER_TO = "The From date is after the To date.";
    static final Color PANEL_FACE = new Color(251, 247, 238);
    static final Color EDGE_DARK = new Color(110, 110, 110);
    static final Color REPORT_TITLE = new Color(92, 45, 8);
    static final Color HINT_TEXT = new Color(120, 112, 100);
    static final int LEFT_WIDTH = 236;
    static final int RIGHT_WIDTH = 474;
    static final int TOP_HEIGHT = 296;
    static final int NOTE_WIDTH = 700;
    static final int COMBO_WIDTH = 224;
    static final int DATE_WIDTH = 84;
    static final int HELP_BUTTON_SIZE = 32;
    private static final int PANEL_PAD = 14;

    /*
     +--------------------------------------------------------------------------+
     | Kind                                                                     |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The seven reports, in the order of the choices, with their words and     |
     | the grey hint under each.                                                |
     +--------------------------------------------------------------------------+
     */
    enum Kind
    {
        HANDICAP("Handicap Report", "Your index and the rounds behind it"),
        ROUND_LOG("Round Log", "A list of rounds with the figures"),
        SCORECARD("A round's scorecard", "One saved round, hole by hole"),
        BEST("Best Round", "Your best round or composite"),
        STATS("Stats Board", "Your statistics, course by course"),
        MY_CARD("My scorecard for a course", "Your strokes and pick-up numbers"),
        NOTES("Course notes", "Hole-by-hole, from the Analyzer");

        final String label;
        final String hint;

        /*
         * Kind(label, hint): a choice's words and its hint.
         */
        Kind(String label, String hint)
        {
            this.label = label;
            this.hint = hint;
        }
    }

    /*
     +--------------------------------------------------------------------------+
     | Tools                                                                    |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | What the window asks of the program: send a report to the printer (the   |
     | system print dialog first), pick the spreadsheet file to save (the       |
     | system's save window), ask before replacing a file that is already       |
     | there, and put a message in the footer. The program                      |
     | window gives the real ones; the self-test gives its own.                 |
     +--------------------------------------------------------------------------+
     */
    interface Tools
    {
        /*
         * print(report): shows the system print dialog and prints; false if it was cancelled there.
         */
        boolean print(Report report) throws PrinterException;

        /*
         * chooseSaveFile(parent, folder, name): the system's save window, starting in folder with name filled in; null if cancelled.
         */
        java.nio.file.Path chooseSaveFile(Component parent, java.nio.file.Path folder, String name);

        /*
         * askReplace(file): asks whether to replace a file that is already there (No is the default); true to replace it.
         */
        boolean askReplace(java.nio.file.Path file);

        /*
         * showHelp(parent, fileKey): opens the help file ("print-info" or "print-help") over the Print window.
         */
        void showHelp(Component parent, String fileKey);

        /*
         * say(text): puts a message in the program window's footer.
         */
        void say(String text);
    }

    /*
     * TeeItem: a line of a tees pulldown: the TeeID (null for "All courses") and its words.
     */
    record TeeItem(Integer teeId, String label)
    {
        /*
         * toString(): the words in the pulldown.
         */
        @Override
        public String toString()
        {
            return label;
        }
    }

    /*
     * RoundItem: a line of the Round pulldown: the round and "2026-10-07  Sample Links (Blue)  88".
     */
    record RoundItem(Round round, String label)
    {
        /*
         * toString(): the words in the pulldown.
         */
        @Override
        public String toString()
        {
            return label;
        }
    }

    final Map<Kind, JRadioButton> choices = new EnumMap<>(Kind.class);
    final JLabel reportTitle = new JLabel();
    final PagePreview preview = new PagePreview();
    final JComboBox<TeeItem> logTees = new JComboBox<>();
    final JComboBox<Period> logPeriod = new JComboBox<>(Period.values());
    final JTextField fromBox = ConfirmForm.textBox("");
    final JTextField toBox = ConfirmForm.textBox("");
    final ImageCheckBox logHistorical = new ImageCheckBox();
    final JComboBox<RoundItem> roundList = new JComboBox<>();
    final JComboBox<BestRound.Choice> bestRows = new JComboBox<>();
    final JRadioButton singleButton = optionRadio("Single");
    final JRadioButton compositeButton = optionRadio("Composite");
    final ImageCheckBox statsHistorical = new ImageCheckBox();
    final JComboBox<TeeItem> cardTees = new JComboBox<>();
    final JLabel cardIndexLine = greyLine("");
    final JComboBox<BestRound.Choice> notesRows = new JComboBox<>();
    final JRadioButton printerChoice = optionRadio(PRINTER_TEXT);
    final JRadioButton csvChoice = optionRadio(CSV_TEXT);
    final WoodButton cancelButton = ConfirmForm.dialogButton("Cancel");
    final WoodButton printButton = ConfirmForm.dialogButton("Print...");
    final IconButton infoButton = new IconButton("Info.png", "Information about the reports", HELP_BUTTON_SIZE);
    final IconButton helpButton = new IconButton("Help.png", "Help with printing", HELP_BUTTON_SIZE);
    final Map<Kind, JPanel> optionPanels = new EnumMap<>(Kind.class);
    private final CardLayout optionCards = new CardLayout();
    private final JPanel optionDeck = ConfirmForm.clear(new JPanel(optionCards));
    private final JTextArea noteArea = new JTextArea();
    private final JPanel noteBox = new JPanel(new BorderLayout());
    private final JPanel buttonRow;
    private final transient ReportData data;
    private final transient Tools tools;
    private final transient java.nio.file.Path startFolder;
    private transient Report report = null;
    private int answer = ConfirmForm.CANCELLED;
    private String doneMessage = "";
    private boolean building = true;

    /*
     +--------------------------------------------------------------------------+
     | PrintForm(data, startFolder, tools)                                      |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Lays out the window (see the class comment), fills the pulldowns from    |
     | the data, ticks the Handicap Report and A printer or PDF, and draws      |
     | the first small picture. Reads the data only.                            |
     |                                                                          |
     | Passed in:  data        - what the reports are drawn from                |
     |             startFolder - where the save window starts (LastExportFolder |
     |                           or the Documents folder)                       |
     |             tools       - the printer, the save window and the footer    |
     | Passed out: the form                                                     |
     +--------------------------------------------------------------------------+
     */
    PrintForm(ReportData data, java.nio.file.Path startFolder, Tools tools)
    {
        this.data = data;
        this.startFolder = startFolder;
        this.tools = tools;
        fillPulldowns();

        JPanel content = ConfirmForm.clear(new JPanel(new GridBagLayout()));
        GridBagConstraints at = new GridBagConstraints();
        at.gridx = 0;
        at.gridy = 0;
        at.anchor = GridBagConstraints.NORTHWEST;
        content.add(choiceColumn(), at);
        at.gridx = 1;
        at.insets = new Insets(0, ConfirmForm.GAP, 0, 0);
        content.add(optionPanel(), at);
        at.gridx = 0;
        at.gridy = 1;
        at.gridwidth = 2;
        at.insets = new Insets(10, 0, 0, 0);
        at.fill = GridBagConstraints.HORIZONTAL;
        content.add(sendRow(), at);
        at.gridy = 2;
        at.insets = new Insets(8, 0, 0, 0);
        content.add(noteBox(), at);

        buttonRow = ConfirmForm.clear(new JPanel(new BorderLayout()));
        JPanel helpButtons = ConfirmForm.clear(new JPanel(new FlowLayout(FlowLayout.LEFT, ConfirmForm.BUTTON_GAP, 0)));
        helpButtons.add(infoButton);
        helpButtons.add(helpButton);
        JPanel mainButtons = ConfirmForm.clear(new JPanel(new FlowLayout(FlowLayout.RIGHT, ConfirmForm.BUTTON_GAP, 0)));
        mainButtons.add(cancelButton);
        mainButtons.add(printButton);
        buttonRow.add(helpButtons, BorderLayout.WEST);
        buttonRow.add(mainButtons, BorderLayout.EAST);
        printButton.setDefaultRing(true);
        ConfirmForm.dress(this, TITLE, content, buttonRow);

        cancelButton.addActionListener(event -> finish(ConfirmForm.CANCELLED));
        infoButton.addActionListener(event -> tools.showHelp(this, "print-info"));
        helpButton.addActionListener(event -> tools.showHelp(this, "print-help"));
        printButton.addActionListener(event -> printPressed());
        choices.get(Kind.HANDICAP).setSelected(true);
        printerChoice.setSelected(true);
        building = false;
        refresh();
    }

    // ------------------------------------------------------------ pulldowns

    /*
     +--------------------------------------------------------------------------+
     | fillPulldowns()                                                          |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Fills every pulldown with what exists: the tees with rounds ("All        |
     | courses" first) for the Round Log, every round newest first, Best        |
     | Round's rows, every set of tees for My scorecard and the Analyzer's      |
     | rows; each starts at the most recent round's choice.                     |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    private void fillPulldowns()
    {
        logTees.addItem(new TeeItem(null, RoundLogReport.ALL_COURSES));
        List<Tee> played = new ArrayList<>();
        for (Round round : data.rounds())
        {
            Tee tee = data.tees().get(round.teeId());
            if (tee != null && !played.contains(tee))
            {
                played.add(tee);
            }
        }
        for (Tee tee : sortedTees(played))
        {
            logTees.addItem(new TeeItem(tee.id(), teeWords(tee)));
        }

        Round newest = null;
        for (int lp = data.rounds().size() - 1; lp >= 0; lp = lp - 1)
        {
            Round round = data.rounds().get(lp);
            Tee tee = data.tees().get(round.teeId());
            if (tee != null)
            {
                roundList.addItem(new RoundItem(round, round.date() + "  " + tee.label() + "  " + round.totalScore()));
                if (newest == null)
                {
                    newest = round;
                }
            }
        }

        for (BestRound.Choice choice : BestRound.choices(data.rounds(), data.tees(), data.includeHistorical()))
        {
            bestRows.addItem(choice);
        }
        BestRound.Choice latest = BestRound.latestChoice(data.rounds(), data.tees(), data.includeHistorical());
        selectChoice(bestRows, latest);

        for (Tee tee : sortedTees(new ArrayList<>(data.tees().values())))
        {
            cardTees.addItem(new TeeItem(tee.id(), teeWords(tee)));
            if (newest != null && tee.id() == newest.teeId())
            {
                cardTees.setSelectedIndex(cardTees.getItemCount() - 1);
            }
        }

        for (BestRound.Choice choice : CourseAnalysis.rows(data.rounds(), data.tees(), data.includeHistorical()))
        {
            notesRows.addItem(choice);
        }
        selectChoice(notesRows, CourseAnalysis.latestRow(data.rounds(), data.tees(), data.includeHistorical()));
    }

    /*
     * sortedTees(tees): the tees by course name, then tee name, ignoring capitals.
     */
    private static List<Tee> sortedTees(List<Tee> tees)
    {
        List<Tee> sorted = new ArrayList<>(tees);
        sorted.sort(Comparator.comparing(Tee::courseName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Tee::teeName, String.CASE_INSENSITIVE_ORDER));
        return sorted;
    }

    /*
     * teeWords(tee): the tees' name as Best Round shows it, "Sample Links:White".
     */
    private static String teeWords(Tee tee)
    {
        StatsBoard.Part part = StatsBoard.Part.EIGHTEEN;
        if (tee.holes() == Tee.NINE)
        {
            part = StatsBoard.Part.NINE_COURSE;
        }
        return StatsBoard.labelOf(tee, part);
    }

    /*
     * selectChoice(list, wanted): selects the row with the same tees and kind as wanted (if any).
     */
    private static void selectChoice(JComboBox<BestRound.Choice> list, BestRound.Choice wanted)
    {
        if (wanted == null)
        {
            return;
        }
        for (int lp = 0; lp < list.getItemCount(); lp = lp + 1)
        {
            if (list.getItemAt(lp).key().equals(wanted.key()))
            {
                list.setSelectedIndex(lp);
                return;
            }
        }
    }

    // ------------------------------------------------------------ layout

    /*
     * choiceColumn(): the question and the seven choices, each with its grey hint.
     */
    private JPanel choiceColumn()
    {
        JPanel column = ConfirmForm.clear(new JPanel(new GridBagLayout()));
        Dimension size = new Dimension(LEFT_WIDTH, TOP_HEIGHT);
        column.setPreferredSize(size);
        column.setMinimumSize(size);
        JLabel question = new JLabel(QUESTION);
        question.setFont(ConfirmForm.messageFont().deriveFont(Font.BOLD, 15f));
        question.setForeground(ConfirmForm.MESSAGE_TEXT);
        GridBagConstraints at = new GridBagConstraints();
        at.gridx = 0;
        at.gridy = 0;
        at.anchor = GridBagConstraints.WEST;
        at.insets = new Insets(0, 0, 6, 0);
        column.add(question, at);
        ButtonGroup group = new ButtonGroup();
        int row = 1;
        for (Kind kind : Kind.values())
        {
            JRadioButton choice = optionRadio(kind.label);
            choice.setFont(ConfirmForm.messageFont().deriveFont(Font.BOLD, 14f));
            choice.setBorder(BorderFactory.createEmptyBorder(1, 0, 0, 0));
            choice.setMargin(new Insets(0, 0, 0, 0));
            group.add(choice);
            choice.addActionListener(event -> refresh());
            choices.put(kind, choice);
            at.gridy = row;
            at.insets = new Insets(0, 0, 0, 0);
            column.add(choice, at);
            JLabel hint = new JLabel(kind.hint);
            hint.setFont(ConfirmForm.messageFont().deriveFont(Font.PLAIN, 12f));
            hint.setForeground(HINT_TEXT);
            at.gridy = row + 1;
            at.insets = new Insets(0, 22, 3, 0);
            column.add(hint, at);
            row = row + 2;
        }
        GridBagConstraints filler = new GridBagConstraints();
        filler.gridy = row;
        filler.weighty = 1.0;
        column.add(ConfirmForm.clear(new JPanel()), filler);
        return column;
    }

    /*
     +--------------------------------------------------------------------------+
     | optionPanel()                                                            |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The sunken panel on the right: the report's name, its options (one card  |
     | per report) and the small picture at the top right.                      |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: the panel                                                    |
     +--------------------------------------------------------------------------+
     */
    private JPanel optionPanel()
    {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(PANEL_FACE);
        panel.setOpaque(true);
        panel.setBorder(BorderFactory.createCompoundBorder(sunkenEdge(), BorderFactory.createEmptyBorder(PANEL_PAD, PANEL_PAD,
                PANEL_PAD, PANEL_PAD)));
        Dimension size = new Dimension(RIGHT_WIDTH, TOP_HEIGHT);
        panel.setPreferredSize(size);
        panel.setMinimumSize(size);
        reportTitle.setFont(Theme.font(Font.BOLD, 15f));
        reportTitle.setForeground(REPORT_TITLE);

        optionPanels.put(Kind.HANDICAP, column(greyLine(HANDICAP_LINE)));
        optionPanels.put(Kind.ROUND_LOG, logOptions());
        optionPanels.put(Kind.SCORECARD, column(caption("Round"), sized(roundList)));
        optionPanels.put(Kind.BEST, bestOptions());
        optionPanels.put(Kind.STATS, column(checkRow(statsHistorical)));
        optionPanels.put(Kind.MY_CARD, column(caption("Course and tees"), sized(cardTees), cardIndexLine));
        optionPanels.put(Kind.NOTES, column(caption("Course and tees"), sized(notesRows)));
        for (Kind kind : Kind.values())
        {
            optionDeck.add(optionPanels.get(kind), kind.name());
        }

        JPanel left = ConfirmForm.clear(new JPanel(new BorderLayout(0, 10)));
        left.add(reportTitle, BorderLayout.NORTH);
        left.add(optionDeck, BorderLayout.CENTER);
        GridBagConstraints at = new GridBagConstraints();
        at.gridx = 0;
        at.gridy = 0;
        at.weightx = 1.0;
        at.weighty = 1.0;
        at.fill = GridBagConstraints.BOTH;
        at.anchor = GridBagConstraints.NORTHWEST;
        panel.add(left, at);
        at.gridx = 1;
        at.weightx = 0;
        at.fill = GridBagConstraints.NONE;
        at.anchor = GridBagConstraints.NORTHEAST;
        at.insets = new Insets(0, 10, 0, 0);
        panel.add(preview, at);

        ButtonGroup kinds = new ButtonGroup();
        kinds.add(singleButton);
        kinds.add(compositeButton);
        singleButton.setSelected(true);
        logTees.addActionListener(event -> refresh());
        logPeriod.addActionListener(event -> periodChanged());
        roundList.addActionListener(event -> refresh());
        bestRows.addActionListener(event -> refresh());
        singleButton.addActionListener(event -> refresh());
        compositeButton.addActionListener(event -> refresh());
        cardTees.addActionListener(event -> refresh());
        notesRows.addActionListener(event -> refresh());
        logHistorical.setSelected(data.includeHistorical());
        statsHistorical.setSelected(data.includeHistorical());
        logHistorical.setOnChange(() -> refresh());
        statsHistorical.setOnChange(() -> refresh());
        DocumentListener typed = whenTyped(() -> refresh());
        fromBox.getDocument().addDocumentListener(typed);
        toBox.getDocument().addDocumentListener(typed);
        showPeriodDates();
        return panel;
    }

    /*
     * sunkenEdge(): the value boxes' one pixel edge: dark grey on top and left, white on the bottom and right.
     */
    static javax.swing.border.Border sunkenEdge()
    {
        return BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 1, 0, 0, EDGE_DARK),
                BorderFactory.createMatteBorder(0, 0, 1, 1, Color.WHITE));
    }

    /*
     * logOptions(): the Round Log's options: course and tees, Period, From and To, Include historical rounds.
     */
    private JPanel logOptions()
    {
        fixWidth(fromBox, DATE_WIDTH);
        fixWidth(toBox, DATE_WIDTH);
        fromBox.setToolTipText("The first day, for example 2026-10-07 or 10/7/2026");
        toBox.setToolTipText("The last day, for example 2026-10-31 or 10/31/2026");
        JPanel dates = ConfirmForm.clear(new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)));
        dates.add(smallWords("From"));
        dates.add(javax.swing.Box.createHorizontalStrut(6));
        dates.add(fromBox);
        dates.add(javax.swing.Box.createHorizontalStrut(6));
        dates.add(smallWords("to"));
        dates.add(javax.swing.Box.createHorizontalStrut(6));
        dates.add(toBox);
        return column(caption("Course and tees"), sized(logTees), caption("Period"), sized(logPeriod), dates,
                checkRow(logHistorical));
    }

    /*
     * bestOptions(): Best Round's options: course and tees, then Single and Composite.
     */
    private JPanel bestOptions()
    {
        JPanel kinds = ConfirmForm.clear(new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)));
        kinds.add(singleButton);
        kinds.add(javax.swing.Box.createHorizontalStrut(14));
        kinds.add(compositeButton);
        return column(caption("Course and tees"), sized(bestRows), kinds);
    }

    /*
     * column(parts): the parts one under another at the top left, 4 px apart.
     */
    private static JPanel column(JComponent... parts)
    {
        JPanel column = ConfirmForm.clear(new JPanel(new GridBagLayout()));
        GridBagConstraints at = new GridBagConstraints();
        at.gridx = 0;
        at.anchor = GridBagConstraints.NORTHWEST;
        for (int lp = 0; lp < parts.length; lp = lp + 1)
        {
            at.gridy = lp;
            if (lp == 0)
            {
                at.insets = new Insets(0, 0, 0, 0);
            }
            else
            {
                at.insets = new Insets(6, 0, 0, 0);
            }
            column.add(parts[lp], at);
        }
        GridBagConstraints filler = new GridBagConstraints();
        filler.gridy = parts.length;
        filler.weighty = 1.0;
        filler.weightx = 1.0;
        column.add(ConfirmForm.clear(new JPanel()), filler);
        return column;
    }

    /*
     * caption(text): the bold dark words over a pulldown ("Course and tees").
     */
    private static JLabel caption(String text)
    {
        JLabel label = new JLabel(text);
        label.setFont(ConfirmForm.messageFont().deriveFont(Font.BOLD, 14f));
        label.setForeground(ConfirmForm.MESSAGE_TEXT);
        return label;
    }

    /*
     * smallWords(text): "From" and "to" beside the date boxes.
     */
    private static JLabel smallWords(String text)
    {
        JLabel label = new JLabel(text);
        label.setFont(ConfirmForm.messageFont().deriveFont(Font.PLAIN, 13f));
        label.setForeground(ConfirmForm.MESSAGE_TEXT);
        return label;
    }

    /*
     * greyLine(text): a grey line of words in the options (the Handicap Report's, the Handicap Index used).
     */
    private static JLabel greyLine(String text)
    {
        JLabel label = new JLabel(text);
        label.setFont(ConfirmForm.messageFont().deriveFont(Font.PLAIN, 13f));
        label.setForeground(HINT_TEXT);
        return label;
    }

    /*
     * optionRadio(text): a see-through radio button in the message font.
     */
    private static JRadioButton optionRadio(String text)
    {
        JRadioButton button = new JRadioButton(text);
        button.setOpaque(false);
        button.setFont(ConfirmForm.messageFont().deriveFont(Font.PLAIN, 13f));
        button.setForeground(ConfirmForm.MESSAGE_TEXT);
        return button;
    }

    /*
     * sized(list): a pulldown COMBO_WIDTH wide in the 13 pt font.
     */
    private static JComboBox<?> sized(JComboBox<?> list)
    {
        list.setFont(Theme.font(Font.PLAIN, 13f));
        Dimension size = new Dimension(COMBO_WIDTH, list.getPreferredSize().height);
        list.setPreferredSize(size);
        list.setMinimumSize(size);
        return list;
    }

    /*
     * fixWidth(box, width): keeps a box at one width (a box with a column count would ignore it).
     */
    private static void fixWidth(JTextField box, int width)
    {
        box.setColumns(0);
        Dimension size = new Dimension(width, box.getPreferredSize().height);
        box.setPreferredSize(size);
        box.setMinimumSize(size);
    }

    /*
     * checkRow(box): an "Include historical rounds" check box with its words (clicking the words ticks it).
     */
    private static JPanel checkRow(ImageCheckBox box)
    {
        box.setPictureSize(18);
        box.setPreferredSize(new Dimension(22, 22));
        JLabel words = ConfirmForm.checkLabel(HISTORICAL_TEXT);
        words.addMouseListener(new java.awt.event.MouseAdapter()
        {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent event)
            {
                box.toggle();
            }
        });
        JPanel row = ConfirmForm.clear(new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)));
        row.add(box);
        row.add(javax.swing.Box.createHorizontalStrut(6));
        row.add(words);
        return row;
    }

    /*
     * sendRow(): "Send it to:" and its two choices.
     */
    private JPanel sendRow()
    {
        JPanel row = ConfirmForm.clear(new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)));
        JLabel words = caption(SEND_TO);
        row.add(javax.swing.Box.createHorizontalStrut(8));
        row.add(words);
        row.add(javax.swing.Box.createHorizontalStrut(10));
        row.add(printerChoice);
        row.add(javax.swing.Box.createHorizontalStrut(10));
        row.add(csvChoice);
        ButtonGroup group = new ButtonGroup();
        group.add(printerChoice);
        group.add(csvChoice);
        printerChoice.setFont(ConfirmForm.messageFont().deriveFont(Font.PLAIN, 14f));
        csvChoice.setFont(ConfirmForm.messageFont().deriveFont(Font.PLAIN, 14f));
        printerChoice.addActionListener(event -> refresh());
        csvChoice.addActionListener(event -> refresh());
        return row;
    }

    /*
     * noteBox(): the grey sunken box holding the note, sized to its words.
     */
    private JPanel noteBox()
    {
        noteArea.setLineWrap(true);
        noteArea.setWrapStyleWord(true);
        noteArea.setEditable(false);
        noteArea.setFocusable(false);
        noteArea.setOpaque(false);
        noteArea.setForeground(ConfirmForm.MESSAGE_TEXT);
        noteArea.setFont(ConfirmForm.messageFont());
        noteBox.setBackground(Theme.FIELD_FACE);
        noteBox.setOpaque(true);
        noteBox.setBorder(BorderFactory.createCompoundBorder(sunkenEdge(), BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        noteBox.add(noteArea, BorderLayout.CENTER);
        return noteBox;
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

    // ------------------------------------------------------------ choices

    /*
     * chosen(): the report ticked on the left.
     */
    Kind chosen()
    {
        for (Kind kind : Kind.values())
        {
            if (choices.get(kind).isSelected())
            {
                return kind;
            }
        }
        return Kind.HANDICAP;
    }

    /*
     * periodChanged(): a new Period: the date boxes show its days (open to type only for Custom dates...).
     */
    private void periodChanged()
    {
        showPeriodDates();
        refresh();
    }

    /*
     +--------------------------------------------------------------------------+
     | showPeriodDates()                                                        |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Puts the chosen period's first and last day in the From and To boxes     |
     | (All time: the first round's day to today), grey and closed; for         |
     | Custom dates... the boxes open, keeping their days to start from.        |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    private void showPeriodDates()
    {
        Period period = (Period) logPeriod.getSelectedItem();
        boolean custom = period == Period.CUSTOM;
        fromBox.setEnabled(custom);
        toBox.setEnabled(custom);
        Color face = Theme.FIELD_FACE;
        if (custom)
        {
            face = Color.WHITE;
        }
        fromBox.setBackground(face);
        toBox.setBackground(face);
        if (custom)
        {
            return;
        }
        LocalDate first = period.from(data.today());
        LocalDate last = period.to(data.today());
        if (period == Period.ALL_TIME)
        {
            last = data.today();
            first = last;
            if (!data.rounds().isEmpty())
            {
                first = data.rounds().get(0).date();
            }
        }
        boolean wasBuilding = building;
        building = true;
        fromBox.setText(first.toString());
        toBox.setText(last.toString());
        building = wasBuilding;
    }

    /*
     +--------------------------------------------------------------------------+
     | report()                                                                 |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The report the window is set to, made from the choices, or null when a   |
     | custom date cannot be read (the note then says what to fix).             |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: the report, or null                                          |
     +--------------------------------------------------------------------------+
     */
    Report report()
    {
        switch (chosen())
        {
            case ROUND_LOG:
                return logReport();
            case SCORECARD:
                RoundItem item = (RoundItem) roundList.getSelectedItem();
                Round round = null;
                if (item != null)
                {
                    round = item.round();
                }
                return ScorecardReport.forRound(data, round);
            case BEST:
                return ScorecardReport.forBest(data, (BestRound.Choice) bestRows.getSelectedItem(), compositeButton.isSelected());
            case STATS:
                return new StatsBoardReport(data, statsHistorical.isSelected());
            case MY_CARD:
                TeeItem tees = (TeeItem) cardTees.getSelectedItem();
                Tee tee = null;
                if (tees != null)
                {
                    tee = data.tees().get(tees.teeId());
                }
                return new MyScorecardReport(data, tee);
            case NOTES:
                return new CourseNotesReport(data, (BestRound.Choice) notesRows.getSelectedItem());
            default:
                return new HandicapReport(data);
        }
    }

    /*
     * logReport(): the Round Log for the chosen tees and period, or null when a custom date cannot be read.
     */
    private RoundLogReport logReport()
    {
        TeeItem item = (TeeItem) logTees.getSelectedItem();
        Integer teeId = null;
        if (item != null)
        {
            teeId = item.teeId();
        }
        Period period = (Period) logPeriod.getSelectedItem();
        LocalDate first = period.from(data.today());
        LocalDate last = period.to(data.today());
        if (period == Period.CUSTOM)
        {
            first = DataLoader.parseDateOrNull(fromBox.getText());
            last = DataLoader.parseDateOrNull(toBox.getText());
            if (first == null || last == null || first.isAfter(last))
            {
                return null;
            }
        }
        return new RoundLogReport(data, teeId, first, last, logHistorical.isSelected());
    }

    /*
     * dateProblem(): what is wrong with the custom dates ("" when they are fine).
     */
    private String dateProblem()
    {
        LocalDate first = DataLoader.parseDateOrNull(fromBox.getText());
        LocalDate last = DataLoader.parseDateOrNull(toBox.getText());
        if (first == null)
        {
            return BAD_FROM;
        }
        if (last == null)
        {
            return BAD_TO;
        }
        if (first.isAfter(last))
        {
            return FROM_AFTER_TO;
        }
        return "";
    }

    /*
     +--------------------------------------------------------------------------+
     | refresh()                                                                |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Brings the window up to date with the choices: the report's name and     |
     | options, a new small picture, the note under "Send it to", and the       |
     | Print... button (off when there is nothing to print).                    |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    void refresh()
    {
        if (building)
        {
            return;
        }
        Kind kind = chosen();
        reportTitle.setText(kind.label);
        optionCards.show(optionDeck, kind.name());
        Handicap.Result handicap = data.handicap();
        if (handicap.currentIndex() == null)
        {
            cardIndexLine.setText("No Handicap Index yet: pick-up is par + 5.");
        }
        else
        {
            cardIndexLine.setText("Handicap Index " + Handicap.format(handicap.currentIndex()) + " will be used.");
        }
        boolean table = kind == Kind.HANDICAP || kind == Kind.ROUND_LOG || kind == Kind.STATS || kind == Kind.NOTES;
        csvChoice.setEnabled(table);
        if (table)
        {
            csvChoice.setToolTipText(null);
        }
        else
        {
            csvChoice.setToolTipText(TABLES_ONLY);
            printerChoice.setSelected(true);
        }
        if (csvChoice.isSelected())
        {
            printButton.setText("Save...");
        }
        else
        {
            printButton.setText("Print...");
        }
        report = report();
        preview.setReport(report);
        String note;
        if (report == null)
        {
            note = dateProblem();
        }
        else if (report.isEmpty())
        {
            note = NOTHING_NOTE;
        }
        else if (csvChoice.isSelected())
        {
            note = "This will save " + report.spreadsheetWords() + SAVE_NOTE;
        }
        else
        {
            note = "This will print " + report.describe() + "." + PRINTER_NOTE;
        }
        setNote(note);
        printButton.setEnabled(report != null && !report.isEmpty());
    }

    /*
     +--------------------------------------------------------------------------+
     | setNote(text)                                                            |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Puts the words in the note box and makes the box exactly tall enough     |
     | for all of them at its width; if the height changes while the window is  |
     | showing, the window is fitted again.                                     |
     |                                                                          |
     | Passed in:  text - the note                                              |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    private void setNote(String text)
    {
        noteArea.setText(text);
        noteArea.setSize(NOTE_WIDTH, Short.MAX_VALUE);
        Dimension wanted = new Dimension(NOTE_WIDTH, noteArea.getPreferredSize().height);
        Dimension before = noteArea.getPreferredSize();
        noteArea.setPreferredSize(wanted);
        noteArea.setMinimumSize(wanted);
        if (before.height != wanted.height)
        {
            revalidate();
            Window window = SwingUtilities.getWindowAncestor(this);
            if (window != null && window.isVisible())
            {
                window.pack();
            }
        }
    }

    /*
     * noteArea(): the note under "Send it to" (for the self-test).
     */
    JTextArea noteArea()
    {
        return noteArea;
    }

    /*
     * noteBoxPanel(): the grey box round the note (for the self-test's layout check).
     */
    JPanel noteBoxPanel()
    {
        return noteBox;
    }

    /*
     * buttonRow(): the row with Cancel and Print... (for the self-test's layout check).
     */
    JPanel buttonRow()
    {
        return buttonRow;
    }

    // ------------------------------------------------------------ the buttons

    /*
     +--------------------------------------------------------------------------+
     | printPressed()                                                           |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Print...: with nothing to print a footer message says so and the window  |
     | stays. Otherwise the report goes to the printer tool (the system print   |
     | dialog); if it was sent the window closes and the footer will say        |
     | "Sent to the printer."; if that dialog was cancelled the window stays.   |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    void printPressed()
    {
        Report chosenReport = report();
        if (chosenReport == null || chosenReport.isEmpty())
        {
            tools.say(NOTHING_SAID);
            return;
        }
        if (csvChoice.isSelected())
        {
            save(chosenReport);
            return;
        }
        try
        {
            if (tools.print(chosenReport))
            {
                doneMessage = SENT;
                finish(ConfirmForm.GO);
            }
        }
        catch (PrinterException failure)
        {
            tools.say("Nothing was printed: " + failure.getMessage());
        }
    }

    /*
     +--------------------------------------------------------------------------+
     | save(chosenReport)                                                       |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Save...: the save window starts in the start folder with a name like     |
     | RoundLog_2026-11-01.csv (".csv" is added if it was left off). A file     |
     | that is already there is replaced only if the golfer says so. Once saved |
     | the window closes and the footer will say "Saved 15 rows to ...". If     |
     | the save window is cancelled, or replacing is refused, the Print window  |
     | stays; if the file cannot be written the footer says why.                |
     |                                                                          |
     | Passed in:  chosenReport - the report to save                            |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    private void save(Report chosenReport)
    {
        java.nio.file.Path file = tools.chooseSaveFile(this, startFolder, golfanalyst.print.Csv.fileName(chosenReport, data.today()));
        if (file == null)
        {
            return;
        }
        if (!file.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".csv"))
        {
            file = file.resolveSibling(file.getFileName() + ".csv");
        }
        if (java.nio.file.Files.exists(file) && !tools.askReplace(file))
        {
            return;
        }
        List<List<String>> rows = chosenReport.spreadsheet();
        try
        {
            golfanalyst.print.Csv.write(file, rows);
        }
        catch (java.io.IOException failure)
        {
            tools.say("Nothing was saved: " + failure.getMessage());
            return;
        }
        doneMessage = "Saved " + (rows.size() - 1) + " rows to " + file.toAbsolutePath().normalize() + ".";
        finish(ConfirmForm.GO);
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
     * answer(): GO once something was sent, CANCELLED otherwise.
     */
    int answer()
    {
        return answer;
    }

    /*
     * doneMessage(): what the footer says after sending ("Sent to the printer."), or "".
     */
    String doneMessage()
    {
        return doneMessage;
    }

    /*
     * showOn(owner): shows the window over the program (Print... is the default, the first choice has the focus) and waits.
     */
    int showOn(Component owner)
    {
        answer = ConfirmForm.CANCELLED;
        ConfirmForm.showPanel(this, owner, printButton, choices.get(chosen()));
        return answer;
    }
}

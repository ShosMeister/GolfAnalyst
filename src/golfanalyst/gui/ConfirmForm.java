package golfanalyst.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/*
 +--------------------------------------------------------------------------+
 | ConfirmForm                                                              |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | The one helper behind every dialog in the program, so they all look and  |
 | size the same. Each is a panel with its own buttons, so the self-test    |
 | can lay it out and try it without a screen; showOn puts it in a modal    |
 | window titled "Golf Analyst", centred on the program, and waits.         |
 |                                                                          |
 | Look (dress): a 3 px dark brown border round everything; a 46 px band    |
 | painted from Wood_Light.gif with a light inner line, a 3 px dark brown   |
 | line along its bottom and the dialog's own title in 21 pt bold italic    |
 | white with a shadow; then the parchment body, 14 px padding top and      |
 | bottom and 16 px at the sides. The message is dark brown 14 pt text in a |
 | box that wraps at 420 px and is exactly as tall as its lines (worked     |
 | out before pack(), so no line can hide behind the buttons). Then a list, |
 | a typing box or choices (if any), then, 12 px below, WoodButtons on the  |
 | right, 10 px apart, at least 100 x 32. The default button has a gold     |
 | ring and the focus; Enter presses it and Esc cancels. The window is      |
 | sized with pack() and is at least 440 px wide.                           |
 |                                                                          |
 | Kinds (each with a title for the band, see withTitle):                   |
 | twoChoice     - a question, Keep (the default) and Go buttons            |
 | twoChoiceWithList - the same with a read-only scrolling list under it    |
 | typed         - "To confirm, type ..." with a box; OK stays off until    |
 |                 it holds the word (capitals and extra spaces ignored)    |
 | clearChoice   - Clear's two radio choices; Continue once one is chosen   |
 | yesNoCancel   - the sample-data question (Yes is the default)            |
 | yesNo         - a Yes / No question (No is the default)                  |
 | notice        - a message, maybe with a list, and OK                     |
 | textInput     - a question with one box to type in, OK and Cancel        |
 | Cancel, a Keep button, Esc and closing the window answer CANCELLED.      |
 |                                                                          |
 | Public Methods:                                                          |
 | the kinds above, withTitle(title), title()                               |
 | message(), messageArea(), buttonRow(), band(), defaultButton()           |
 | answer(), text()           - the answer given and the text typed         |
 | dress(panel, title, content, buttons) - gives any panel this look        |
 | showOn(owner), showPanel(...) - show a form or a dressed panel and wait  |
 +--------------------------------------------------------------------------+
 */
@SuppressWarnings({"serial", "this-escape"})
final class ConfirmForm extends JPanel
{

    static final int CANCELLED = 0;
    static final int GO = 1;
    static final int NO = 3;
    static final int ROUNDS_ONLY = 1;
    static final int EVERYTHING = 2;
    static final int MESSAGE_WIDTH = 420;
    static final int PADDING = 16;
    static final int PADDING_TOP = 14;
    static final int GAP = 12;
    static final int MIN_WIDTH = 440;
    static final int BAND_HEIGHT = 46;
    static final int BORDER = 3;
    static final int BUTTON_GAP = 10;
    static final int BUTTON_MIN_WIDTH = 100;
    static final int BUTTON_HEIGHT = 32;
    static final int LIST_ROWS = 8;
    static final String WINDOW_TITLE = "Golf Analyst";
    static final Color DARK_BROWN = new Color(70, 35, 5);
    static final Color MESSAGE_TEXT = new Color(60, 35, 10);
    static final Color BAND_LINE = new Color(255, 235, 190, 150);
    static final Color HAIRLINE = new Color(185, 180, 170);
    private static final int CHOICE_GAP = 4;
    final WoodButton goButton;
    final WoodButton cancelButton;
    final WoodButton noButton;
    final JTextField wordField;
    final JTextField textField;
    final JTextArea listArea;
    final JRadioButton roundsOnlyChoice;
    final JRadioButton everythingChoice;
    private final JTextArea messageArea;
    private final JPanel buttonRow;
    private final Band band;
    private final WoodButton defaultButton;
    private int answer = CANCELLED;

    /*
     +--------------------------------------------------------------------------+
     | Band                                                                     |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The 46 px wooden band across the top of a dialog with its title.         |
     +--------------------------------------------------------------------------+
     */
    static final class Band extends JComponent
    {
        private String title;

        /*
         * Band(title): a band showing the title.
         */
        Band(String title)
        {
            this.title = title;
            setPreferredSize(new Dimension(MIN_WIDTH, BAND_HEIGHT));
            setMinimumSize(new Dimension(0, BAND_HEIGHT));
        }

        /*
         * setTitle(newTitle), title(): the words in the band.
         */
        void setTitle(String newTitle)
        {
            title = newTitle;
            repaint();
        }

        /*
         * title(): the words in the band.
         */
        String title()
        {
            return title;
        }

        /*
         * paintComponent(graphics): the light wood stretched over the band, a light line 2 px in, a 3 px dark brown line along the bottom, and the title centred in white with a shadow.
         */
        @Override
        protected void paintComponent(Graphics graphics)
        {
            Graphics2D canvas = (Graphics2D) graphics.create();
            Theme.smooth(canvas);
            Theme.paintStretched(canvas, Theme.image("Wood_Light.gif"), getWidth(), getHeight(), Theme.WOOD_FALLBACK);
            canvas.setColor(BAND_LINE);
            canvas.drawRect(2, 2, getWidth() - 5, getHeight() - BORDER - 5);
            canvas.setColor(DARK_BROWN);
            canvas.fillRect(0, getHeight() - BORDER, getWidth(), BORDER);
            canvas.setFont(Theme.font(Font.BOLD | Font.ITALIC, 21f));
            Theme.drawCentered(canvas, title, 0, 0, getWidth(), getHeight() - BORDER, Theme.TEXT_LIGHT, true);
            canvas.dispose();
        }
    }

    /*
     +--------------------------------------------------------------------------+
     | ConfirmForm(message, cancelLabel, noLabel, goLabel, goFirst, word, ...)  |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Lays out the message, then the list, typing box or choices (when         |
     | given), then the buttons, dresses the panel (see dress) and wires it.    |
     | Use the makers below.                                                    |
     |                                                                          |
     | Passed in:  message        - the words at the top                        |
     |             cancelLabel    - the keep / cancel button's words, or null   |
     |             noLabel        - a No button's words, or null for none       |
     |             goLabel        - the go button's words                       |
     |             goFirst        - true to put the go button first (Yes, No)   |
     |             word           - the word to type, or null for no word box   |
     |             roundsText     - Clear's first choice, or null for none      |
     |             everythingText - Clear's second choice                       |
     |             list           - the read-only list, or null for none        |
     |             initialText    - the text of a typing box, or null for none  |
     |             goIsDefault    - true if the go button is the default        |
     | Passed out: the form                                                     |
     +--------------------------------------------------------------------------+
     */
    private ConfirmForm(String message, String cancelLabel, String noLabel, String goLabel, boolean goFirst, String word,
            String roundsText, String everythingText, String list, String initialText, boolean goIsDefault)
    {
        messageArea = wrappedText(message);
        JPanel top = clear(new JPanel(new BorderLayout(0, GAP)));
        top.add(messageArea, BorderLayout.NORTH);

        goButton = dialogButton(goLabel);
        cancelButton = (cancelLabel == null ? null : dialogButton(cancelLabel));
        noButton = (noLabel == null ? null : dialogButton(noLabel));
        wordField = (word == null ? null : textBox(""));
        textField = (initialText == null ? null : textBox(initialText));
        listArea = (list == null ? null : readOnlyList(list));
        roundsOnlyChoice = (roundsText == null ? null : new JRadioButton(roundsText));
        everythingChoice = (roundsText == null ? null : new JRadioButton(everythingText));
        if (listArea != null)
        {
            JScrollPane scroller = new JScrollPane(listArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                    JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            scroller.setBorder(BorderFactory.createLineBorder(HAIRLINE, 1));
            int rowHeight = listArea.getFontMetrics(listArea.getFont()).getHeight();
            scroller.setPreferredSize(new Dimension(MESSAGE_WIDTH, LIST_ROWS * rowHeight + 2 * 5 + 2));
            top.add(scroller, BorderLayout.CENTER);
        }
        JTextField box = (wordField != null ? wordField : textField);
        if (box != null)
        {
            JPanel holder = clear(new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)));
            holder.add(box);
            top.add(holder, BorderLayout.CENTER);
        }
        if (wordField != null)
        {
            goButton.setEnabled(false);
            wordField.getDocument().addDocumentListener(new DocumentListener()
            {
                @Override
                public void insertUpdate(DocumentEvent event)
                {
                    goButton.setEnabled(sameWords(wordField.getText(), word));
                }

                @Override
                public void removeUpdate(DocumentEvent event)
                {
                    goButton.setEnabled(sameWords(wordField.getText(), word));
                }

                @Override
                public void changedUpdate(DocumentEvent event)
                {
                    goButton.setEnabled(sameWords(wordField.getText(), word));
                }
            });
        }
        if (roundsOnlyChoice != null)
        {
            JPanel choices = clear(new JPanel(new GridLayout(0, 1, 0, 4)));
            ButtonGroup group = new ButtonGroup();
            for (JRadioButton choice : new JRadioButton[] {roundsOnlyChoice, everythingChoice})
            {
                group.add(choice);
                choices.add(wrappedChoice(choice));
                choice.addActionListener(event -> goButton.setEnabled(true));
            }
            top.add(choices, BorderLayout.CENTER);
            goButton.setEnabled(false);
        }

        buttonRow = clear(new JPanel(new FlowLayout(FlowLayout.RIGHT, BUTTON_GAP, 0)));
        if (goFirst)
        {
            buttonRow.add(goButton);
        }
        if (noButton != null)
        {
            buttonRow.add(noButton);
            noButton.addActionListener(event -> finish(NO));
        }
        if (cancelButton != null)
        {
            buttonRow.add(cancelButton);
            cancelButton.addActionListener(event -> finish(CANCELLED));
        }
        if (!goFirst)
        {
            buttonRow.add(goButton);
        }
        goButton.addActionListener(event -> finish(goAnswer()));
        if (goIsDefault)
        {
            defaultButton = goButton;
        }
        else
        {
            defaultButton = (cancelButton != null ? cancelButton : (noButton != null ? noButton : goButton));
        }
        defaultButton.setDefaultRing(true);
        band = dress(this, "Confirm", top, buttonRow);
    }

    // ------------------------------------------------------------ the look

    /*
     +--------------------------------------------------------------------------+
     | dress(panel, title, content, buttons)                                    |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Gives a panel the dialog look (see the class comment): the dark brown    |
     | border, the wooden band with the title, the parchment body holding the   |
     | content and, 12 px below it, the row of buttons. Esc on the panel        |
     | presses Cancel (or closes the window). The Import window uses it too.    |
     |                                                                          |
     | Passed in:  panel   - the dialog's own panel (its layout is replaced)    |
     |             title   - the words for the band                             |
     |             content - everything above the buttons                       |
     |             buttons - the row of buttons                                 |
     | Passed out: the band, so its title can be changed                        |
     +--------------------------------------------------------------------------+
     */
    static Band dress(JPanel panel, String title, JComponent content, JPanel buttons)
    {
        panel.removeAll();
        panel.setLayout(new BorderLayout());
        panel.setOpaque(true);
        panel.setBackground(Theme.HELP_PAPER);
        panel.setBorder(BorderFactory.createMatteBorder(BORDER, BORDER, BORDER, BORDER, DARK_BROWN));
        Band band = new Band(title);
        panel.add(band, BorderLayout.NORTH);
        JPanel body = clear(new JPanel(new BorderLayout(0, GAP)));
        body.setBorder(BorderFactory.createEmptyBorder(PADDING_TOP, PADDING, PADDING_TOP, PADDING));
        body.add(content, BorderLayout.CENTER);
        body.add(buttons, BorderLayout.SOUTH);
        panel.add(body, BorderLayout.CENTER);
        panel.registerKeyboardAction(event ->
        {
            Window window = SwingUtilities.getWindowAncestor(panel);
            if (window != null)
            {
                window.dispose();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        return band;
    }

    /*
     * clear(panel): makes a layout panel see-through, so the parchment shows behind it.
     */
    static JPanel clear(JPanel panel)
    {
        panel.setOpaque(false);
        return panel;
    }

    /*
     * dialogButton(text): a WoodButton for a dialog: at least 100 x 32, wide enough for its words.
     * Passed in:  text - the words
     * Passed out: the button
     */
    static WoodButton dialogButton(String text)
    {
        WoodButton button = new WoodButton(text);
        button.setFont(Theme.font(Font.BOLD, 15f));
        int width = Math.max(BUTTON_MIN_WIDTH, button.getFontMetrics(button.getFont()).stringWidth(text) + 32);
        Dimension size = new Dimension(width, BUTTON_HEIGHT);
        button.setPreferredSize(size);
        button.setMinimumSize(size);
        return button;
    }

    /*
     * textBox(text): a box to type in: white, a 1 px hairline border, 13 pt, 5 px inside.
     * Passed in:  text - what it starts with
     * Passed out: the box
     */
    static JTextField textBox(String text)
    {
        JTextField box = new JTextField(text, 16);
        box.setBackground(Color.WHITE);
        box.setForeground(MESSAGE_TEXT);
        box.setFont(Theme.font(Font.PLAIN, 13f));
        box.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(HAIRLINE, 1),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        return box;
    }

    /*
     * readOnlyList(text): a white read-only text area with 5 px inside, for a list that scrolls (the scroll pane gets the hairline).
     * Passed in:  text - the lines
     * Passed out: the area
     */
    static JTextArea readOnlyList(String text)
    {
        JTextArea area = new JTextArea(text);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBackground(Color.WHITE);
        area.setForeground(MESSAGE_TEXT);
        area.setFont(Theme.font(Font.PLAIN, 13f));
        area.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        area.setCaretPosition(0);
        return area;
    }

    /*
     * messageFont(): the message font: the system's own message-box font (Segoe UI on Windows) at 14 pt, or the label font where the look has none.
     */
    static Font messageFont()
    {
        Font font = UIManager.getFont("OptionPane.messageFont");
        return (font != null ? font : UIManager.getFont("Label.font")).deriveFont(Font.PLAIN, 14f);
    }

    /*
     * wrappedText(text): a read-only dark brown 14 pt text box that wraps at MESSAGE_WIDTH and is exactly as tall as its lines.
     */
    static JTextArea wrappedText(String text)
    {
        return wrappedText(text, MESSAGE_WIDTH);
    }

    /*
     +--------------------------------------------------------------------------+
     | wrappedText(text, width)                                                 |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | A read-only text box that wraps at the given width. Its height is        |
     | worked out for that width here, before any window is packed: a           |
     | wrapping text box asked for its size before it has a width reports too   |
     | few lines, which is how the last line once ended up behind the buttons.  |
     |                                                                          |
     | Passed in:  text  - the words                                            |
     |             width - where the lines wrap, in pixels                      |
     | Passed out: the text box, that wide and as tall as its lines             |
     +--------------------------------------------------------------------------+
     */
    static JTextArea wrappedText(String text, int width)
    {
        JTextArea area = new JTextArea(text);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setEditable(false);
        area.setFocusable(false);
        area.setOpaque(false);
        area.setForeground(MESSAGE_TEXT);
        area.setBorder(BorderFactory.createEmptyBorder());
        area.setFont(messageFont());
        area.setSize(width, Short.MAX_VALUE);
        Dimension wrapped = new Dimension(width, area.getPreferredSize().height);
        area.setPreferredSize(wrapped);
        area.setMinimumSize(wrapped);
        return area;
    }

    /*
     * wrappedChoice(choice): a radio button whose long words wrap: the words move into a wrapped text box beside the bare button (clicking them ticks it), and stay as the button's accessible name and tool tip.
     * Passed in:  choice - the radio button
     * Passed out: the row
     */
    private static JPanel wrappedChoice(JRadioButton choice)
    {
        String text = choice.getText();
        choice.setText("");
        choice.setOpaque(false);
        choice.getAccessibleContext().setAccessibleName(text);
        choice.setToolTipText(text);
        JPanel row = clear(new JPanel(new BorderLayout(CHOICE_GAP, 0)));
        JTextArea words = wrappedText(text, MESSAGE_WIDTH - choice.getPreferredSize().width - CHOICE_GAP);
        words.addMouseListener(new java.awt.event.MouseAdapter()
        {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent event)
            {
                choice.doClick();
            }
        });
        JPanel buttonHolder = clear(new JPanel(new BorderLayout()));
        buttonHolder.add(choice, BorderLayout.NORTH);
        row.add(buttonHolder, BorderLayout.WEST);
        row.add(words, BorderLayout.CENTER);
        return row;
    }

    /*
     * sameWords(typed, word): whether what was typed is the word, ignoring capitals and extra spaces.
     */
    static boolean sameWords(String typed, String word)
    {
        return typed.trim().replaceAll("\\s+", " ").equalsIgnoreCase(word.trim().replaceAll("\\s+", " "));
    }

    // ------------------------------------------------------------ the kinds

    /*
     * twoChoice(message, keepLabel, goLabel): a question with a Keep button (the default, so Enter keeps) and a Go button.
     */
    static ConfirmForm twoChoice(String message, String keepLabel, String goLabel)
    {
        return new ConfirmForm(message, keepLabel, null, goLabel, false, null, null, null, null, null, false);
    }

    /*
     * twoChoiceWithList(message, list, keepLabel, goLabel): the same with a read-only list under the question, about 8 lines high, that scrolls.
     */
    static ConfirmForm twoChoiceWithList(String message, String list, String keepLabel, String goLabel)
    {
        return new ConfirmForm(message, keepLabel, null, goLabel, false, null, null, null, list, null, false);
    }

    /*
     * typed(prompt, word): "To confirm, type DELETE." with a box; OK comes on only while the box holds the word (capitals and extra spaces ignored).
     */
    static ConfirmForm typed(String prompt, String word)
    {
        return new ConfirmForm(prompt, "Cancel", null, "OK", false, word, null, null, null, null, true);
    }

    /*
     * clearChoice(message, roundsText, everythingText): Clear's two choices, neither chosen; Continue comes on once one is.
     */
    static ConfirmForm clearChoice(String message, String roundsText, String everythingText)
    {
        return new ConfirmForm(message, "Cancel", null, "Continue", false, null, roundsText, everythingText, null, null, false);
    }

    /*
     * yesNoCancel(question, withCancel): the sample-data question: Yes (the default), No and, if wanted, Cancel; the answers are GO, NO and CANCELLED.
     */
    static ConfirmForm yesNoCancel(String question, boolean withCancel)
    {
        return new ConfirmForm(question, (withCancel ? "Cancel" : null), "No", "Yes", true, null, null, null, null, null, true);
    }

    /*
     * yesNo(question): a Yes / No question such as "Discard your changes?", with No (the safe answer) as the default; Yes answers GO.
     */
    static ConfirmForm yesNo(String question)
    {
        return new ConfirmForm(question, "No", null, "Yes", true, null, null, null, null, null, false);
    }

    /*
     * notice(message, list): a message with OK, and a scrolling list under it when list is not null.
     */
    static ConfirmForm notice(String message, String list)
    {
        return new ConfirmForm(message, null, null, "OK", false, null, null, null, list, null, true);
    }

    /*
     * textInput(question, initial): a question with one box to type in (holding initial), OK (the default) and Cancel; see text().
     */
    static ConfirmForm textInput(String question, String initial)
    {
        return new ConfirmForm(question, "Cancel", null, "OK", false, null, null, null, null, initial, true);
    }

    /*
     * withTitle(title): sets the words in the band (the window itself is always titled "Golf Analyst").
     * Passed in:  title - e.g. "Delete round"
     * Passed out: this form
     */
    ConfirmForm withTitle(String title)
    {
        band.setTitle(title);
        return this;
    }

    // ------------------------------------------------------------ answers and parts

    /*
     * title(): the words in the band.
     */
    String title()
    {
        return band.title();
    }

    /*
     * band(): the wooden band (for the self-test's layout check).
     */
    Band band()
    {
        return band;
    }

    /*
     * choiceText(choice): the words of one of Clear's choices (they sit in a text box beside the button).
     */
    static String choiceText(JRadioButton choice)
    {
        return choice.getAccessibleContext().getAccessibleName();
    }

    /*
     * message(): the words at the top of the form.
     */
    String message()
    {
        return messageArea.getText();
    }

    /*
     * messageArea(): the text box holding the message (for the self-test's layout check).
     */
    JTextArea messageArea()
    {
        return messageArea;
    }

    /*
     * buttonRow(): the row holding the buttons (for the self-test's layout check).
     */
    JPanel buttonRow()
    {
        return buttonRow;
    }

    /*
     * defaultButton(): the button Enter presses, with the gold ring and the focus.
     */
    WoodButton defaultButton()
    {
        return defaultButton;
    }

    /*
     * answer(): the answer given so far: CANCELLED until another button is pressed.
     */
    int answer()
    {
        return answer;
    }

    /*
     * text(): what is in the typing box of a textInput form ("" for other kinds).
     */
    String text()
    {
        return (textField == null ? "" : textField.getText());
    }

    /*
     * goAnswer(): what the go button means: GO, or for Clear the choice that is ticked.
     */
    private int goAnswer()
    {
        if (everythingChoice != null)
        {
            return (everythingChoice.isSelected() ? EVERYTHING : ROUNDS_ONLY);
        }
        return GO;
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

    // ------------------------------------------------------------ showing

    /*
     * showOn(owner): shows the form in a modal window over the program and waits (see showPanel); the focus starts on the typing box, if any, or the default button.
     * Passed in:  owner - something in the program window (null at start-up)
     * Passed out: the answer
     */
    int showOn(Component owner)
    {
        answer = CANCELLED;
        JTextField box = (wordField != null ? wordField : textField);
        showPanel(this, owner, defaultButton, (box != null ? box : defaultButton));
        return answer;
    }

    /*
     +--------------------------------------------------------------------------+
     | showPanel(panel, owner, defaultButton, firstFocus)                       |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The one way the program's dialogs are shown: a modal window titled       |
     | "Golf Analyst", sized to the panel with pack() (never a fixed height),   |
     | at least MIN_WIDTH wide, centred on the program window, with the         |
     | default button set (Enter presses it) and the focus on firstFocus. It    |
     | returns when the window is closed.                                       |
     |                                                                          |
     | Passed in:  panel         - the dialog's content (a dressed panel)       |
     |             owner         - something in the program window, or null     |
     |             defaultButton - the button Enter presses                     |
     |             firstFocus    - where the focus starts                       |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    static void showPanel(JPanel panel, Component owner, JButton defaultButton, Component firstFocus)
    {
        Window parent = (owner == null ? null : SwingUtilities.getWindowAncestor(owner));
        JDialog dialog = new JDialog(parent, WINDOW_TITLE, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setContentPane(panel);
        dialog.getRootPane().setDefaultButton(defaultButton);
        dialog.pack();
        dialog.setMinimumSize(new Dimension(Math.max(MIN_WIDTH, dialog.getWidth()), dialog.getHeight()));
        dialog.setSize(dialog.getMinimumSize());
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(parent);
        dialog.addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowOpened(WindowEvent event)
            {
                firstFocus.requestFocusInWindow();
            }
        });
        dialog.setVisible(true);
    }

    /*
     * caption(text): a 96 x 24 WoodTag caption for a box in a dialog (the Import window's rows).
     */
    static WoodTag caption(String text)
    {
        WoodTag tag = new WoodTag(text);
        Dimension size = new Dimension(96, 24);
        tag.setPreferredSize(size);
        tag.setMinimumSize(size);
        return tag;
    }

    /*
     * checkLabel(text): the 13 pt dark brown words beside an ImageCheckBox in a dialog.
     */
    static JLabel checkLabel(String text)
    {
        JLabel label = new JLabel(text);
        label.setFont(Theme.font(Font.PLAIN, 13f));
        label.setForeground(MESSAGE_TEXT);
        return label;
    }
}

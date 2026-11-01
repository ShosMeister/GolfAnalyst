package golfanalyst.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FlowLayout;
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
import javax.swing.JPanel;
import javax.swing.JRadioButton;
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
 | The one helper that builds the careful questions (Delete round, Clear,   |
 | the sample-data questions), so they all look and size the same. Each is  |
 | a panel with its own buttons, so the self-test can lay it out and try it |
 | without a screen; showOn puts it in a small window over the program and  |
 | waits for an answer.                                                     |
 |                                                                          |
 | Layout: 16 px of padding all round; the message at the top, in a text    |
 | box that wraps at 420 px and is exactly as tall as its lines, so the     |
 | window can never cut it off; then the typing box or the choices (if      |
 | any); then, 12 px below, the buttons on the right. The window is sized   |
 | to its content with pack() and is at least 440 px wide. The words are in |
 | the system's own message font (Segoe UI 12 on Windows), the same as the  |
 | program's other questions such as "Discard your changes?".               |
 |                                                                          |
 | Four kinds:                                                              |
 | twoChoice   - a question with a Keep button (the default) and a Go       |
 |               button, e.g. "Keep it" / "Delete...";                      |
 | typed       - "To confirm, type DELETE." with a box; OK stays off until  |
 |               the box holds the word (any capitals);                     |
 | clearChoice - Clear's two choices as radio buttons, neither chosen at    |
 |               first; Continue comes on once one is chosen;               |
 | yesNoCancel - the sample-data question: Yes (the default), No, Cancel    |
 |               (Cancel left out when it is given as null).                |
 | Cancel, the Keep button, Escape and closing the window all answer        |
 | CANCELLED.                                                               |
 |                                                                          |
 | Constants:                                                               |
 | CANCELLED, GO, NO          - the answers of a question                   |
 | ROUNDS_ONLY, EVERYTHING    - the answers of clearChoice                  |
 | MESSAGE_WIDTH, PADDING, GAP, MIN_WIDTH - the sizes above                 |
 |                                                                          |
 | Public Methods:                                                          |
 | twoChoice, typed, clearChoice, yesNoCancel - make a form                 |
 | message(), messageArea()   - the words at the top and their text box     |
 | buttonRow(), defaultButton() - the row of buttons and the default one    |
 | answer()                   - the answer given so far                     |
 | showOn(owner, title)       - shows it in a window and waits              |
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
    static final int GAP = 12;
    static final int MIN_WIDTH = 440;
    private static final int CHOICE_GAP = 4;
    final JButton goButton;
    final JButton cancelButton;
    final JButton noButton;
    final JTextField wordField;
    final JRadioButton roundsOnlyChoice;
    final JRadioButton everythingChoice;
    private final JTextArea messageArea;
    private final JPanel buttonRow;
    private final JButton defaultButton;
    private int answer = CANCELLED;

    /*
     +--------------------------------------------------------------------------+
     | ConfirmForm(message, buttons, word, roundsText, everythingText, ...)     |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Lays out the message, the box or the choices (when given) and the        |
     | buttons, and wires them. Use the four makers below.                      |
     |                                                                          |
     | Passed in:  message        - the words at the top                        |
     |             cancelLabel    - the keep / cancel button's words, or null   |
     |             noLabel        - a No button's words, or null for none       |
     |             goLabel        - the go button's words                       |
     |             goFirst        - true to put the go button first (Yes, No)   |
     |             word           - the word to type, or null for no box        |
     |             roundsText     - Clear's first choice, or null for none      |
     |             everythingText - Clear's second choice                       |
     |             goIsDefault    - true if the go button is the default        |
     | Passed out: the form                                                     |
     +--------------------------------------------------------------------------+
     */
    private ConfirmForm(String message, String cancelLabel, String noLabel, String goLabel, boolean goFirst, String word,
            String roundsText, String everythingText, boolean goIsDefault)
    {
        super(new BorderLayout(0, GAP));
        setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));
        messageArea = wrappedText(message);
        JPanel top = new JPanel(new BorderLayout(0, GAP));
        top.add(messageArea, BorderLayout.NORTH);
        add(top, BorderLayout.CENTER);

        goButton = new JButton(goLabel);
        cancelButton = (cancelLabel == null ? null : new JButton(cancelLabel));
        noButton = (noLabel == null ? null : new JButton(noLabel));
        wordField = (word == null ? null : new JTextField(12));
        roundsOnlyChoice = (roundsText == null ? null : new JRadioButton(roundsText));
        everythingChoice = (roundsText == null ? null : new JRadioButton(everythingText));
        if (wordField != null)
        {
            JPanel box = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            box.add(wordField);
            top.add(box, BorderLayout.CENTER);
            goButton.setEnabled(false);
            wordField.getDocument().addDocumentListener(new DocumentListener()
            {
                @Override
                public void insertUpdate(DocumentEvent event)
                {
                    goButton.setEnabled(wordField.getText().trim().equalsIgnoreCase(word));
                }

                @Override
                public void removeUpdate(DocumentEvent event)
                {
                    goButton.setEnabled(wordField.getText().trim().equalsIgnoreCase(word));
                }

                @Override
                public void changedUpdate(DocumentEvent event)
                {
                    goButton.setEnabled(wordField.getText().trim().equalsIgnoreCase(word));
                }
            });
        }
        if (roundsOnlyChoice != null)
        {
            JPanel choices = new JPanel(new GridLayout(0, 1, 0, 4));
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

        buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
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
        add(buttonRow, BorderLayout.SOUTH);
        goButton.addActionListener(event -> finish(goAnswer()));
        defaultButton = ((goIsDefault || cancelButton == null) ? goButton : cancelButton);
        registerKeyboardAction(event -> finish(CANCELLED), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    /*
     +--------------------------------------------------------------------------+
     | wrappedText(text)                                                        |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | A read-only text box that wraps at MESSAGE_WIDTH. Its height is worked   |
     | out for that width here, before any window is packed: a wrapping text    |
     | box asked for its size before it has a width reports too few lines,      |
     | which is how the last line once ended up behind the buttons.             |
     |                                                                          |
     | Passed in:  text - the words                                             |
     | Passed out: the text box, MESSAGE_WIDTH wide and as tall as its lines    |
     +--------------------------------------------------------------------------+
     */
    private static JTextArea wrappedText(String text)
    {
        return wrappedText(text, MESSAGE_WIDTH);
    }

    /*
     * wrappedText(text, width): the same text box wrapping at the given width, as tall as its lines at that width.
     * Passed in:  text - the words, width - where the lines wrap, in pixels
     * Passed out: the text box
     */
    private static JTextArea wrappedText(String text, int width)
    {
        JTextArea area = new JTextArea(text);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setEditable(false);
        area.setFocusable(false);
        area.setOpaque(false);
        area.setBorder(BorderFactory.createEmptyBorder());
        area.setFont(messageFont());
        area.setSize(width, Short.MAX_VALUE);
        Dimension wrapped = new Dimension(width, area.getPreferredSize().height);
        area.setPreferredSize(wrapped);
        area.setMinimumSize(wrapped);
        return area;
    }

    /*
     * messageFont(): the system's own font for the words of a message box (Segoe UI 12 on Windows, as "Discard your changes?" uses), or the label font where the look has none.
     * Passed in:  nothing
     * Passed out: the font
     */
    static Font messageFont()
    {
        Font font = UIManager.getFont("OptionPane.messageFont");
        return (font != null ? font : UIManager.getFont("Label.font"));
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
        choice.getAccessibleContext().setAccessibleName(text);
        choice.setToolTipText(text);
        JPanel row = new JPanel(new BorderLayout(CHOICE_GAP, 0));
        JTextArea words = wrappedText(text, MESSAGE_WIDTH - choice.getPreferredSize().width - CHOICE_GAP);
        words.addMouseListener(new java.awt.event.MouseAdapter()
        {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent event)
            {
                choice.doClick();
            }
        });
        JPanel buttonHolder = new JPanel(new BorderLayout());
        buttonHolder.add(choice, BorderLayout.NORTH);
        row.add(buttonHolder, BorderLayout.WEST);
        row.add(words, BorderLayout.CENTER);
        return row;
    }

    /*
     * twoChoice(message, keepLabel, goLabel): a question with a Keep button (the default, so Enter keeps) and a Go button.
     * Passed in:  message - the question, keepLabel - e.g. "Keep it", goLabel - e.g. "Delete..."
     * Passed out: the form
     */
    static ConfirmForm twoChoice(String message, String keepLabel, String goLabel)
    {
        return new ConfirmForm(message, keepLabel, null, goLabel, false, null, null, null, false);
    }

    /*
     * typed(prompt, word): "To confirm, type DELETE." with a box; OK comes on only while the box holds the word, any capitals.
     * Passed in:  prompt - the words above the box, word - the word to type
     * Passed out: the form
     */
    static ConfirmForm typed(String prompt, String word)
    {
        return new ConfirmForm(prompt, "Cancel", null, "OK", false, word, null, null, true);
    }

    /*
     * clearChoice(message, roundsText, everythingText): Clear's two choices, neither chosen; Continue comes on once one is.
     * Passed in:  message - the counts, roundsText / everythingText - the two choices
     * Passed out: the form
     */
    static ConfirmForm clearChoice(String message, String roundsText, String everythingText)
    {
        return new ConfirmForm(message, "Cancel", null, "Continue", false, null, roundsText, everythingText, false);
    }

    /*
     * yesNoCancel(question, withCancel): the sample-data question: Yes (the default), No and, if wanted, Cancel.
     * Passed in:  question - the words, withCancel - true to have a Cancel button
     * Passed out: the form; its answers are GO (Yes), NO and CANCELLED
     */
    static ConfirmForm yesNoCancel(String question, boolean withCancel)
    {
        return new ConfirmForm(question, (withCancel ? "Cancel" : null), "No", "Yes", true, null, null, null, true);
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
     * defaultButton(): the button Enter presses and the window's focus starts on (Keep it, Keep my data, Cancel, Yes or OK).
     */
    JButton defaultButton()
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
     * Passed in:  value - the answer
     * Passed out: nothing
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
     +--------------------------------------------------------------------------+
     | showOn(owner, title)                                                     |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Shows the form in a small window over the program and waits until it is  |
     | answered. The window is sized to the form with pack() (never a fixed     |
     | height) and is at least MIN_WIDTH wide. The default button has the       |
     | focus, so Enter presses it; on the typed form the box has the focus and  |
     | Enter presses OK once it is on. Closing the window counts as Cancel.     |
     |                                                                          |
     | Passed in:  owner - something in the program window                      |
     |             title - the window's title                                   |
     | Passed out: the answer                                                   |
     +--------------------------------------------------------------------------+
     */
    int showOn(Component owner, String title)
    {
        answer = CANCELLED;
        Window parent = (owner == null ? null : SwingUtilities.getWindowAncestor(owner));
        JDialog dialog = new JDialog(parent, title, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setContentPane(this);
        dialog.getRootPane().setDefaultButton(defaultButton);
        dialog.pack();
        dialog.setMinimumSize(new Dimension(Math.max(MIN_WIDTH, dialog.getWidth()), dialog.getHeight()));
        dialog.setSize(dialog.getMinimumSize());
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(parent);
        Component firstFocus = (wordField != null ? wordField : defaultButton);
        dialog.addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowOpened(WindowEvent event)
            {
                firstFocus.requestFocusInWindow();
            }
        });
        dialog.setVisible(true);
        return answer;
    }
}

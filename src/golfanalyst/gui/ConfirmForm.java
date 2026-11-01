package golfanalyst.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.KeyEvent;
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
 | The careful questions asked before anything is removed (Delete round,    |
 | Clear). Each is a panel with its own buttons, so the self-test can try   |
 | it without a screen; showOn puts it in a small window over the program   |
 | and waits for an answer, like the "Discard your changes?" questions.     |
 |                                                                          |
 | Three kinds:                                                             |
 | twoChoice   - a question with a Keep button (the default) and a Go       |
 |               button, e.g. "Keep it" / "Delete...";                      |
 | typed       - "To confirm, type DELETE." with a box; OK stays off until  |
 |               the box holds the word (any capitals);                     |
 | clearChoice - Clear's two choices as radio buttons, neither chosen at    |
 |               first; Continue comes on once one is chosen.               |
 | Cancel, the Keep button, Escape and closing the window all answer        |
 | CANCELLED.                                                               |
 |                                                                          |
 | Constants:                                                               |
 | CANCELLED, GO              - the answers of twoChoice and typed          |
 | ROUNDS_ONLY, EVERYTHING    - the answers of clearChoice                  |
 |                                                                          |
 | Public Methods:                                                          |
 | twoChoice(...), typed(...), clearChoice(...) - make a form               |
 | answer()                   - the answer given so far                     |
 | showOn(owner, title)       - shows it in a window and waits              |
 +--------------------------------------------------------------------------+
 */
@SuppressWarnings({"serial", "this-escape"})
final class ConfirmForm extends JPanel
{

    static final int CANCELLED = 0;
    static final int GO = 1;
    static final int ROUNDS_ONLY = 1;
    static final int EVERYTHING = 2;
    private static final int TEXT_COLUMNS = 42;
    final JButton goButton;
    final JButton cancelButton;
    final JTextField wordField;
    final JRadioButton roundsOnlyChoice;
    final JRadioButton everythingChoice;
    private final JTextArea messageArea;
    private int answer = CANCELLED;

    /*
     +--------------------------------------------------------------------------+
     | ConfirmForm(message, cancelLabel, goLabel, word, roundsText, ...)        |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Lays out the message, the box or the choices (when given) and the        |
     | buttons, and wires them. Use the three makers below.                     |
     |                                                                          |
     | Passed in:  message        - the words at the top                        |
     |             cancelLabel    - the keep / cancel button's words            |
     |             goLabel        - the go button's words                       |
     |             word           - the word to type, or null for no box        |
     |             roundsText     - Clear's first choice, or null for none      |
     |             everythingText - Clear's second choice                       |
     | Passed out: the form                                                     |
     +--------------------------------------------------------------------------+
     */
    private ConfirmForm(String message, String cancelLabel, String goLabel, String word, String roundsText, String everythingText)
    {
        super(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(14, 16, 12, 16));
        messageArea = new JTextArea(message, 0, TEXT_COLUMNS);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setEditable(false);
        messageArea.setFocusable(false);
        messageArea.setOpaque(false);
        messageArea.setFont(UIManager.getFont("Label.font"));
        add(messageArea, BorderLayout.NORTH);

        goButton = new JButton(goLabel);
        cancelButton = new JButton(cancelLabel);
        wordField = (word == null ? null : new JTextField(12));
        roundsOnlyChoice = (roundsText == null ? null : new JRadioButton(roundsText));
        everythingChoice = (roundsText == null ? null : new JRadioButton(everythingText));
        if (wordField != null)
        {
            JPanel box = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            box.add(wordField);
            add(box, BorderLayout.CENTER);
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
                choices.add(choice);
                choice.addActionListener(event -> goButton.setEnabled(true));
            }
            add(choices, BorderLayout.CENTER);
            goButton.setEnabled(false);
        }

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.add(cancelButton);
        buttons.add(goButton);
        add(buttons, BorderLayout.SOUTH);
        cancelButton.addActionListener(event -> finish(CANCELLED));
        goButton.addActionListener(event -> finish(goAnswer()));
        registerKeyboardAction(event -> finish(CANCELLED), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    /*
     * twoChoice(message, keepLabel, goLabel): a question with a Keep button (the default, so Enter keeps) and a Go button.
     * Passed in:  message - the question, keepLabel - e.g. "Keep it", goLabel - e.g. "Delete..."
     * Passed out: the form
     */
    static ConfirmForm twoChoice(String message, String keepLabel, String goLabel)
    {
        return new ConfirmForm(message, keepLabel, goLabel, null, null, null);
    }

    /*
     * typed(prompt, word): "To confirm, type DELETE." with a box; OK comes on only while the box holds the word, any capitals.
     * Passed in:  prompt - the words above the box, word - the word to type
     * Passed out: the form
     */
    static ConfirmForm typed(String prompt, String word)
    {
        return new ConfirmForm(prompt, "Cancel", "OK", word, null, null);
    }

    /*
     * clearChoice(message, roundsText, everythingText): Clear's two choices, neither chosen; Continue comes on once one is.
     * Passed in:  message - the counts, roundsText / everythingText - the two choices
     * Passed out: the form
     */
    static ConfirmForm clearChoice(String message, String roundsText, String everythingText)
    {
        return new ConfirmForm(message, "Cancel", "Continue", null, roundsText, everythingText);
    }

    /*
     * message(): the words at the top of the form (for the self-test).
     */
    String message()
    {
        return messageArea.getText();
    }

    /*
     * answer(): the answer given so far: CANCELLED until a go button is pressed.
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
     | answered. The Keep / Cancel button is the default for a question (so     |
     | Enter keeps everything); for the typed form Enter presses OK once it is  |
     | on. Closing the window counts as Cancel.                                 |
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
        dialog.getRootPane().setDefaultButton((wordField != null ? goButton : cancelButton));
        dialog.pack();
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(parent);
        if (wordField != null)
        {
            SwingUtilities.invokeLater(() -> wordField.requestFocusInWindow());
        }
        dialog.setVisible(true);
        return answer;
    }
}

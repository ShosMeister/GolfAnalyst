package golfanalyst.gui;

import golfanalyst.Tee;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

/*
 +--------------------------------------------------------------------------+
 | CopyCoursesForm                                                          |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | The window shown after New Round has made the golfer's own data folder   |
 | from the sample data and the folder has no courses yet: every course of  |
 | the sample data with a tick box ("Edgewater, Lancaster SC: Senior,       |
 | Beginner, Intermediate, Expert"), none ticked; Copy brings the ticked    |
 | courses, with all their tees exactly as they are, into the new folder;   |
 | None copies nothing. Dressed like every other dialog (ConfirmForm.dress) |
 | and shown with ConfirmForm.showPanel; AppPanel's CopyAsker lets the      |
 | self-test answer it.                                                     |
 |                                                                          |
 | Public Methods:                                                          |
 | CopyCoursesForm(tees) - the window for these sample tees                 |
 | courseNames(), tick(course), chosenTees(), answer(), introArea(),        |
 | buttonRow(), showOn(owner)                                               |
 +--------------------------------------------------------------------------+
 */
final class CopyCoursesForm extends JPanel
{

    static final String TITLE = "Copy courses";
    static final String INTRO = "Your own data folder has no courses yet. Tick any course from the sample data you would like to "
            + "use: it is copied into your folder with all its tees, exactly as it is. Choose None to add your own course with New "
            + "Course first.";
    static final int CONTENT_WIDTH = 460;

    final WoodButton noneButton = ConfirmForm.dialogButton("None");
    final WoodButton copyButton = ConfirmForm.dialogButton("Copy");
    private final Map<String, List<Tee>> courses = new LinkedHashMap<>();
    private final Map<String, ImageCheckBox> boxes = new LinkedHashMap<>();
    private final JTextArea introArea;
    private final JPanel buttonRow;
    private int answer = ConfirmForm.CANCELLED;

    /*
     +--------------------------------------------------------------------------+
     | CopyCoursesForm(tees)                                                    |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Groups the tees into courses (name ignoring capitals and spaces, and     |
     | zip; sorted by name, the tees in TeeID order) and lays out the window:   |
     | the words, one tick box row per course, then None and Copy (the          |
     | default).                                                                |
     |                                                                          |
     | Passed in:  tees - the sample data's tees                                |
     | Passed out: the window's content                                         |
     +--------------------------------------------------------------------------+
     */
    CopyCoursesForm(List<Tee> tees)
    {
        List<Tee> sorted = new ArrayList<>(tees);
        sorted.sort(Comparator.comparing((Tee tee) -> tee.courseName(), String.CASE_INSENSITIVE_ORDER).thenComparing(Tee::id));
        for (Tee tee : sorted)
        {
            String key = tee.courseName().trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT) + "|" + tee.zip();
            courses.computeIfAbsent(key, ignored -> new ArrayList<>()).add(tee);
        }
        JPanel content = ConfirmForm.clear(new JPanel(new BorderLayout(0, ConfirmForm.GAP)));
        introArea = ConfirmForm.wrappedText(INTRO, CONTENT_WIDTH);
        content.add(introArea, BorderLayout.NORTH);
        JPanel rows = ConfirmForm.clear(new JPanel());
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        for (Map.Entry<String, List<Tee>> course : courses.entrySet())
        {
            ImageCheckBox box = new ImageCheckBox();
            JLabel words = ConfirmForm.checkLabel(describe(course.getValue()));
            words.addMouseListener(new java.awt.event.MouseAdapter()
            {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent event)
                {
                    box.toggle();
                }
            });
            JPanel line = ConfirmForm.clear(new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2)));
            line.add(box);
            line.add(words);
            line.setAlignmentX(Component.LEFT_ALIGNMENT);
            rows.add(line);
            boxes.put(course.getKey(), box);
        }
        content.add(rows, BorderLayout.CENTER);
        buttonRow = ConfirmForm.clear(new JPanel(new FlowLayout(FlowLayout.RIGHT, ConfirmForm.BUTTON_GAP, 0)));
        buttonRow.add(noneButton);
        buttonRow.add(copyButton);
        copyButton.setDefaultRing(true);
        ConfirmForm.dress(this, TITLE, content, buttonRow);
        noneButton.addActionListener(event -> finish(ConfirmForm.CANCELLED));
        copyButton.addActionListener(event -> finish(ConfirmForm.GO));
    }

    /*
     * describe(tees): one course's line: "Edgewater, Lancaster SC: Senior, Beginner, Intermediate, Expert".
     */
    static String describe(List<Tee> tees)
    {
        Tee first = tees.get(0);
        List<String> names = new ArrayList<>();
        for (Tee tee : tees)
        {
            names.add(tee.teeName());
        }
        String place = "";
        if (!first.city().isBlank())
        {
            place = ", " + first.city().trim() + " " + first.state().trim();
        }
        return first.courseName() + place + ": " + String.join(", ", names);
    }

    /*
     * courseNames(): each course's line, in the order shown.
     */
    List<String> courseNames()
    {
        List<String> names = new ArrayList<>();
        for (List<Tee> tees : courses.values())
        {
            names.add(describe(tees));
        }
        return names;
    }

    /*
     * tick(course): ticks the course whose line starts with these words (for the self-test); false if there is none.
     */
    boolean tick(String course)
    {
        for (Map.Entry<String, List<Tee>> entry : courses.entrySet())
        {
            if (describe(entry.getValue()).startsWith(course))
            {
                boxes.get(entry.getKey()).setSelected(true);
                return true;
            }
        }
        return false;
    }

    /*
     * chosenTees(): every tee of the ticked courses (empty when None was chosen or nothing is ticked).
     */
    List<Tee> chosenTees()
    {
        List<Tee> chosen = new ArrayList<>();
        if (answer != ConfirmForm.GO)
        {
            return chosen;
        }
        for (Map.Entry<String, List<Tee>> entry : courses.entrySet())
        {
            if (boxes.get(entry.getKey()).isSelected())
            {
                chosen.addAll(entry.getValue());
            }
        }
        return chosen;
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
     * answer(): GO once Copy is pressed, CANCELLED for None or the window closed.
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
     * buttonRow(): the row with None and Copy (for the self-test's layout check).
     */
    JPanel buttonRow()
    {
        return buttonRow;
    }

    /*
     * showOn(owner): shows the window over the program (Copy is the default) and waits.
     * Passed in:  owner - something in the program window
     * Passed out: the answer
     */
    int showOn(Component owner)
    {
        answer = ConfirmForm.CANCELLED;
        ConfirmForm.showPanel(this, owner, copyButton, copyButton);
        return answer;
    }
}

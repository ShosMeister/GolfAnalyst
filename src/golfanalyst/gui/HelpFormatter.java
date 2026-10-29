package golfanalyst.gui;

import java.util.ArrayList;
import java.util.List;

/*
 +--------------------------------------------------------------------------+
 | HelpFormatter                                                            |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | Turns the text of a help file into pieces of text, each with a style, so |
 | the Help and Info windows can show headings and bold labels while the    |
 | files stay plain text that any editor can change. It has no window code, |
 | so it can be tested on a computer with no display.                       |
 |                                                                          |
 | The first line of the file is the window title and is not repeated in    |
 | the body. Then, line by line:                                            |
 |   a blank line           - skipped                                       |
 |   "# " at the start      - a section heading (the "# " is not shown)     |
 |   "12. " at the start    - a numbered step: its first sentence (up to    |
 |                            the first period after the number) in bold,   |
 |                            the rest plain                                |
 |   anything else          - a paragraph. If it has a colon within the     |
 |                            first 45 characters and no period before it,  |
 |                            the words up to and including the colon are   |
 |                            bold (a label, like "Low Index:"); the rest   |
 |                            is plain.                                     |
 | Every line that is shown is its own paragraph.                           |
 |                                                                          |
 | Constants:                                                               |
 | HEADING_MARK - the "# " that marks a section heading                     |
 |                                                                          |
 | Public Methods:                                                          |
 | title(fileText) - the first line, or "" if there is none                 |
 | pieces(fileText) - the body as styled pieces                             |
 +--------------------------------------------------------------------------+
 */
public final class HelpFormatter
{

    public static final String HEADING_MARK = "# ";
    private static final int LABEL_REACH = 45;

    /*
     * Style: how a piece of text looks: a section heading, bold, or plain.
     */
    public enum Style
    {
        HEADING, BOLD, PLAIN
    }

    /*
     * Piece: some text with its style; lastInParagraph is true for the final piece of a paragraph.
     */
    public record Piece(String text, Style style, boolean lastInParagraph)
    {
    }

    private HelpFormatter()
    {
    }

    /*
     * title(fileText): the first line of a help file, which is the window title.
     * Passed in:  fileText - the whole file
     * Passed out: the first line without spaces at its ends, or "" for an empty file
     */
    public static String title(String fileText)
    {
        String[] lines = fileText.split("\\R", 2);
        return lines.length == 0 ? "" : lines[0].strip();
    }

    /*
     +--------------------------------------------------------------------------+
     | pieces(fileText)                                                         |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Turns everything after the first line into styled pieces, following      |
     | the rules in the class comment.                                          |
     |                                                                          |
     | Passed in:  fileText - the whole file                                    |
     | Passed out: the pieces, in order                                         |
     +--------------------------------------------------------------------------+
     */
    public static List<Piece> pieces(String fileText)
    {
        List<Piece> pieces = new ArrayList<>();
        String[] lines = fileText.split("\\R", -1);
        for (int lp = 1; lp < lines.length; lp = lp + 1)
        {
            String line = lines[lp].strip();
            if (line.isEmpty())
            {
                continue;
            }
            if (line.startsWith(HEADING_MARK))
            {
                pieces.add(new Piece(line.substring(HEADING_MARK.length()).strip(), Style.HEADING, true));
                continue;
            }
            int boldEnd = boldEnd(line);
            if (boldEnd >= line.length())
            {
                pieces.add(new Piece(line, Style.BOLD, true));
            }
            else if (boldEnd > 0)
            {
                pieces.add(new Piece(line.substring(0, boldEnd), Style.BOLD, false));
                pieces.add(new Piece(line.substring(boldEnd), Style.PLAIN, true));
            }
            else
            {
                pieces.add(new Piece(line, Style.PLAIN, true));
            }
        }
        return pieces;
    }

    /*
     +--------------------------------------------------------------------------+
     | boldEnd(line)                                                            |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Works out how much of the start of a line is bold: for a numbered step   |
     | up to and including the first period after the number; for a label up    |
     | to and including its colon (within the first 45 characters, with no      |
     | period before it); otherwise nothing.                                    |
     |                                                                          |
     | Passed in:  line - one line of the file, spaces at the ends removed      |
     | Passed out: the number of characters to show in bold (0 for none)        |
     +--------------------------------------------------------------------------+
     */
    private static int boldEnd(String line)
    {
        int digits = 0;
        while (digits < line.length() && Character.isDigit(line.charAt(digits)))
        {
            digits = digits + 1;
        }
        if (digits > 0 && line.startsWith(". ", digits))
        {
            int period = line.indexOf('.', digits + 2);
            return period < 0 ? line.length() : period + 1;
        }
        int colon = line.indexOf(':');
        if (colon >= 0 && colon < LABEL_REACH && line.lastIndexOf('.', colon) < 0)
        {
            return colon + 1;
        }
        return 0;
    }
}

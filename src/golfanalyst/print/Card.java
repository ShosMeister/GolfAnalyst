package golfanalyst.print;

import java.awt.Color;
import java.awt.Font;
import java.util.List;
import java.util.Map;

/*
 +--------------------------------------------------------------------------+
 | Card                                                                     |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | Draws a table in the scorecard style, shared by the scorecard, Best      |
 | Round, My scorecard for a course and Course notes: a label column 96     |
 | wide with dark brown bold 13 labels, then a 42 wide column per heading;  |
 | a dark brown header row 34 high starting with "Hole"; body rows with the |
 | zebra colours and light (215,208,195) cell outlines; the total columns   |
 | (Out, In, Total or All) shaded (240,236,228) with bold words. A heading  |
 | may be tinted (Course notes' hardest and easiest holes), and a marker    |
 | may draw on a cell (the scorecard's circles and squares). It also draws  |
 | the rounded figures box under a card.                                    |
 |                                                                          |
 | Public Methods:                                                          |
 | width(headings)       - the card's width                                 |
 | paint(...)            - draws the card; gives the y under it             |
 | paintFigures(...)     - the rounded box of captions and values           |
 +--------------------------------------------------------------------------+
 */
final class Card
{

    static final double LABEL_WIDTH = 96;
    static final double HOLE_WIDTH = 42;
    static final double HEADER_HEIGHT = 34;
    static final double FIGURE_STEP = 340;

    /*
     * Marker: draws something more on a body cell (the score marks).
     */
    interface Marker
    {
        /*
         * mark(sheet, row, col, centreX, centreY): draws on the cell of that body row and column, centred there.
         */
        void mark(Sheet sheet, int row, int col, double centreX, double centreY);
    }

    private Card()
    {
    }

    /*
     * width(headings): the label column and a 42 wide column for each heading.
     */
    static double width(int headings)
    {
        return LABEL_WIDTH + headings * HOLE_WIDTH;
    }

    /*
     +--------------------------------------------------------------------------+
     | paint(sheet, top, labels, headings, totals, rows, rowHeight, bigRow,     |
     |       headingFaces, marker)                                              |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Draws the card (see the class comment) at the left margin.               |
     |                                                                          |
     | Passed in:  sheet        - the page                                      |
     |             top          - where the header row starts                   |
     |             labels       - the body rows' labels, top to bottom          |
     |             headings     - the header row's words after "Hole"           |
     |             totals       - for each heading, true for a total column     |
     |             rows         - each body row's words, one per heading        |
     |             rowHeight    - the body rows' height                         |
     |             bigRow       - a body row written bold 15 (the Score row),   |
     |                            or -1                                         |
     |             headingFaces - heading columns tinted, by column (may be     |
     |                            empty)                                        |
     |             marker       - draws on body cells, or null                  |
     | Passed out: the y under the last row                                     |
     +--------------------------------------------------------------------------+
     */
    static double paint(Sheet sheet, double top, List<String> labels, List<String> headings, List<Boolean> totals,
            List<List<String>> rows, double rowHeight, int bigRow, Map<Integer, Color> headingFaces, Marker marker)
    {
        double x = Sheet.MARGIN;
        double cardWidth = width(headings.size());
        Font headFont = Sheet.sans(Font.BOLD, 13);
        sheet.fill(x, top, cardWidth, HEADER_HEIGHT, Sheet.BROWN);
        sheet.textIn("Hole", x, top, LABEL_WIDTH, HEADER_HEIGHT, headFont, Color.WHITE, Table.Align.CENTRE, 0);
        for (int col = 0; col < headings.size(); col = col + 1)
        {
            double left = x + LABEL_WIDTH + col * HOLE_WIDTH;
            Color ink = Color.WHITE;
            if (headingFaces.containsKey(col))
            {
                sheet.fill(left, top, HOLE_WIDTH, HEADER_HEIGHT, headingFaces.get(col));
                ink = Sheet.INK;
            }
            sheet.textIn(headings.get(col), left, top, HOLE_WIDTH, HEADER_HEIGHT, headFont, ink, Table.Align.CENTRE, 0);
        }
        double bodyTop = top + HEADER_HEIGHT;
        double rowTop = bodyTop;
        for (int row = 0; row < rows.size(); row = row + 1)
        {
            Color face = Color.WHITE;
            if (row % 2 == 1)
            {
                face = Sheet.ZEBRA;
            }
            sheet.fill(x, rowTop, cardWidth, rowHeight, face);
            sheet.textIn(labels.get(row), x, rowTop, LABEL_WIDTH, rowHeight, Sheet.sans(Font.BOLD, 13), Sheet.BROWN, Table.Align.CENTRE, 0);
            for (int col = 0; col < headings.size(); col = col + 1)
            {
                double left = x + LABEL_WIDTH + col * HOLE_WIDTH;
                boolean total = totals.get(col);
                if (total)
                {
                    sheet.fill(left, rowTop, HOLE_WIDTH, rowHeight, Sheet.BOX);
                }
                double size = 13;
                int style = Font.PLAIN;
                if (row == bigRow)
                {
                    size = 15;
                    style = Font.BOLD;
                }
                if (total)
                {
                    style = Font.BOLD;
                }
                cellWords(sheet, rows.get(row).get(col), left, rowTop, rowHeight, Sheet.sans(style, size));
                if (marker != null)
                {
                    marker.mark(sheet, row, col, left + HOLE_WIDTH / 2, rowTop + rowHeight / 2);
                }
            }
            rowTop = rowTop + rowHeight;
        }
        for (int row = 0; row <= rows.size(); row = row + 1)
        {
            sheet.line(x, bodyTop + row * rowHeight, x + cardWidth, bodyTop + row * rowHeight, Sheet.CELL_LINE, 1);
        }
        sheet.line(x, bodyTop, x, rowTop, Sheet.CELL_LINE, 1);
        for (int col = 0; col <= headings.size(); col = col + 1)
        {
            double lineX = x + LABEL_WIDTH + col * HOLE_WIDTH;
            sheet.line(lineX, bodyTop, lineX, rowTop, Sheet.CELL_LINE, 1);
        }
        return rowTop;
    }

    /*
     * cellWords(sheet, words, left, top, rowHeight, font): the words centred in a hole cell; a check mark the font cannot draw is drawn as a tick.
     */
    private static void cellWords(Sheet sheet, String words, double left, double top, double rowHeight, Font font)
    {
        if (words.equals(Sheet.CHECK) && font.canDisplayUpTo(words) != -1)
        {
            double centreX = left + HOLE_WIDTH / 2;
            double centreY = top + rowHeight / 2;
            sheet.line(centreX - 6, centreY, centreX - 2, centreY + 5, Sheet.INK, 2.5);
            sheet.line(centreX - 2, centreY + 5, centreX + 6, centreY - 6, Sheet.INK, 2.5);
            return;
        }
        sheet.textIn(words, left, top, HOLE_WIDTH, rowHeight, font, Sheet.INK, Table.Align.CENTRE, 0);
    }

    /*
     +--------------------------------------------------------------------------+
     | paintFigures(sheet, pairs, boxTop, boxHeight, columns, rowStep, tints)   |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The rounded figures box across the margins (radius 10, (240,236,228)     |
     | with a dark brown 2 unit edge): a grey 12 caption and a bold 21 value    |
     | for each pair, in columns 340 apart (closer for more than three on a     |
 | row), filled row by row. A value may sit                                 |
     | on a tint (Course notes' hardest and easiest holes).                     |
     |                                                                          |
     | Passed in:  sheet     - the page                                         |
     |             pairs     - captions and values, in reading order            |
     |             boxTop    - where the box starts                             |
     |             boxHeight - its height                                       |
     |             columns   - pairs on a row                                   |
     |             rowStep   - from one row of pairs to the next                |
     |             tints     - a tint for each pair, or null for none (may be   |
     |                         null altogether)                                 |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    static void paintFigures(Sheet sheet, List<String[]> pairs, double boxTop, double boxHeight, int columns, double rowStep,
            List<Color> tints)
    {
        double across = sheet.width() - Sheet.MARGIN - Sheet.MARGIN;
        sheet.roundBox(Sheet.MARGIN, boxTop, across, boxHeight, 10, Sheet.BOX, Sheet.BROWN, 2);
        Font valueFont = Sheet.sans(Font.BOLD, 21);
        double step = FIGURE_STEP;
        if (columns > 3)
        {
            step = (across - 28) / columns;
        }
        for (int lp = 0; lp < pairs.size(); lp = lp + 1)
        {
            int col = lp % columns;
            int row = lp / columns;
            double x = Sheet.MARGIN + 28 + col * step;
            double captionTop = boxTop + 26 + row * rowStep;
            sheet.text(pairs.get(lp)[0], x, captionTop, Sheet.sans(Font.PLAIN, 12), Sheet.GREY);
            if (tints != null && tints.get(lp) != null)
            {
                double tintWidth = Math.min(step - 40, Sheet.MARGIN + across - 16 - (x - 6));
                sheet.fill(x - 6, captionTop + 15, tintWidth, Sheet.lineHeight(valueFont) + 6, tints.get(lp));
            }
            sheet.text(Sheet.fit(pairs.get(lp)[1], valueFont, step - 52), x, captionTop + 18, valueFont, Sheet.INK);
        }
    }
}

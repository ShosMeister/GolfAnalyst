package golfanalyst.print;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/*
 +--------------------------------------------------------------------------+
 | Sheet                                                                    |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | One printed page being drawn, and the drawing helpers every report       |
 | uses. Everything is in design units of 0.01 inch: a Letter page is 850   |
 | x 1100 units upright and 1100 x 850 on its side, with 60 units of        |
 | margin on every side. Font sizes are in units too (12 units is about     |
 | 8.6 pt). The printer, the small picture in the Print window and the      |
 | self-test all draw through here, scaled as they need.                    |
 |                                                                          |
 | Text is placed by the top of its line: text(..., y) puts the font's      |
 | ascent at y, so "GOLF ANALYST" at y 44 sits where the design pictures    |
 | show it. Text that is too wide for its space is cut short with "...".    |
 |                                                                          |
 | It uses java.awt only (no Swing windows).                                |
 |                                                                          |
 | Public Methods:                                                          |
 | width(), height(), graphics()       - the page and the drawing surface   |
 | sans(style, size), serif(style, size) - the two fonts                    |
 | text, textRight, textCentre, textIn - one line of text                   |
 | fit(text, font, width)              - text cut short with "..." to fit   |
 | wrap(text, font, width)             - text broken into lines             |
 | textWidth(text, font), ascent(font), lineHeight(font)                    |
 | fill, outline, rule, roundBox, circle, square - shapes                   |
 +--------------------------------------------------------------------------+
 */
public final class Sheet
{

    public static final double MARGIN = 60;
    public static final double LETTER_SHORT = 850;
    public static final double LETTER_LONG = 1100;
    public static final Color BROWN = new Color(92, 45, 8);
    public static final Color INK = new Color(30, 30, 30);
    public static final Color GREY = new Color(120, 120, 120);
    public static final Color FOOT_RULE = new Color(190, 180, 165);
    public static final Color ZEBRA = new Color(248, 245, 240);
    public static final Color SHADE = new Color(255, 240, 190);
    public static final Color BOX = new Color(240, 236, 228);
    public static final Color CELL_LINE = new Color(215, 208, 195);
    public static final Color HARD = new Color(255, 214, 208);
    public static final Color EASY = new Color(255, 236, 150);
    public static final String ELLIPSIS = "...";
    public static final String DAGGER = "†";
    public static final String CHECK = "✔";
    private final Graphics2D graphics;
    private final double width;
    private final double height;

    /*
     +--------------------------------------------------------------------------+
     | Sheet(graphics, width, height)                                           |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Starts drawing a page. The graphics must already be in design units      |
     | (the caller scales it). Smooth edges and exact text widths are switched  |
     | on so the page looks the same at every size.                             |
     |                                                                          |
     | Passed in:  graphics - where to draw, in design units                    |
     |             width    - the page width (850 or 1100)                      |
     |             height   - the page height (1100 or 850 on Letter, more on   |
     |                        A4; the printer decides)                          |
     | Passed out: the sheet                                                    |
     +--------------------------------------------------------------------------+
     */
    public Sheet(Graphics2D graphics, double width, double height)
    {
        this.graphics = graphics;
        this.width = width;
        this.height = height;
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    /*
     * width(): the page width in design units.
     */
    public double width()
    {
        return width;
    }

    /*
     * height(): the page height in design units.
     */
    public double height()
    {
        return height;
    }

    /*
     * graphics(): the drawing surface, in design units.
     */
    public Graphics2D graphics()
    {
        return graphics;
    }

    /*
     * sans(style, size): the SansSerif font (everything but titles) at a size in design units.
     */
    public static Font sans(int style, double size)
    {
        return new Font(Font.SANS_SERIF, style, 1).deriveFont((float) size);
    }

    /*
     * serif(style, size): the Serif font (titles) at a size in design units.
     */
    public static Font serif(int style, double size)
    {
        return new Font(Font.SERIF, style, 1).deriveFont((float) size);
    }

    /*
     * context(): how text is measured: exact widths, smooth edges (the same for every page size).
     */
    private static FontRenderContext context()
    {
        return new FontRenderContext(null, true, true);
    }

    /*
     * textWidth(text, font): how wide a line of text is, in design units.
     */
    public static double textWidth(String text, Font font)
    {
        if (text.isEmpty())
        {
            return 0;
        }
        return font.getStringBounds(text, context()).getWidth();
    }

    /*
     * ascent(font): how far the tallest letters reach above the line the text sits on.
     */
    public static double ascent(Font font)
    {
        return font.getLineMetrics("Hg", context()).getAscent();
    }

    /*
     * lineHeight(font): the height of one line of text (ascent and descent).
     */
    public static double lineHeight(Font font)
    {
        java.awt.font.LineMetrics metrics = font.getLineMetrics("Hg", context());
        return metrics.getAscent() + metrics.getDescent();
    }

    /*
     +--------------------------------------------------------------------------+
     | fit(text, font, room)                                                    |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Cuts a text short so it fits in the room it has, ending it with "...";   |
     | a text that fits is left as it is.                                       |
     |                                                                          |
     | Passed in:  text - the words                                             |
     |             font - the font they are drawn in                            |
     |             room - the width there is, in design units                   |
     | Passed out: the words, maybe shortened                                   |
     +--------------------------------------------------------------------------+
     */
    public static String fit(String text, Font font, double room)
    {
        if (textWidth(text, font) <= room)
        {
            return text;
        }
        String shorter = text;
        while (!shorter.isEmpty())
        {
            shorter = shorter.substring(0, shorter.length() - 1);
            String tried = shorter.stripTrailing() + ELLIPSIS;
            if (textWidth(tried, font) <= room)
            {
                return tried;
            }
        }
        return "";
    }

    /*
     +--------------------------------------------------------------------------+
     | wrap(text, font, room)                                                   |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Breaks a paragraph into lines no wider than the room, between words.     |
     | A single word wider than the room is cut short with "...".               |
     |                                                                          |
     | Passed in:  text - the paragraph                                         |
     |             font - its font                                              |
     |             room - the width of a line, in design units                  |
     | Passed out: the lines (at least one)                                     |
     +--------------------------------------------------------------------------+
     */
    public static List<String> wrap(String text, Font font, double room)
    {
        List<String> lines = new ArrayList<>();
        String line = "";
        for (String word : text.split(" "))
        {
            String longer;
            if (line.isEmpty())
            {
                longer = word;
            }
            else
            {
                longer = line + " " + word;
            }
            if (textWidth(longer, font) <= room || line.isEmpty())
            {
                line = longer;
            }
            else
            {
                lines.add(line);
                line = word;
            }
        }
        lines.add(fit(line, font, room));
        for (int lp = 0; lp < lines.size(); lp = lp + 1)
        {
            lines.set(lp, fit(lines.get(lp), font, room));
        }
        return lines;
    }

    /*
     * text(words, x, top, font, colour): one line of text from x, its letters starting at top.
     */
    public void text(String words, double x, double top, Font font, Color colour)
    {
        graphics.setFont(font);
        graphics.setColor(colour);
        graphics.drawString(words, (float) x, (float) (top + ascent(font)));
    }

    /*
     * textRight(words, right, top, font, colour): one line of text ending at right.
     */
    public void textRight(String words, double right, double top, Font font, Color colour)
    {
        text(words, right - textWidth(words, font), top, font, colour);
    }

    /*
     * textCentre(words, centre, top, font, colour): one line of text centred on centre.
     */
    public void textCentre(String words, double centre, double top, Font font, Color colour)
    {
        text(words, centre - textWidth(words, font) / 2, top, font, colour);
    }

    /*
     +--------------------------------------------------------------------------+
     | textIn(words, x, y, boxWidth, boxHeight, font, colour, align, pad)       |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | One line of text in a box (a table cell): centred top to bottom, placed  |
     | left, centre or right with pad units inside for left and right, and      |
     | cut short with "..." if it does not fit.                                 |
     |                                                                          |
     | Passed in:  words              - the text                                |
     |             x, y               - the box's top left corner               |
     |             boxWidth, boxHeight - its size                               |
     |             font, colour       - how the text looks                      |
     |             align              - Table.Align.LEFT, CENTRE or RIGHT       |
     |             pad                - the inner space for LEFT and RIGHT      |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    public void textIn(String words, double x, double y, double boxWidth, double boxHeight, Font font, Color colour,
            Table.Align align, double pad)
    {
        String shown;
        double top = y + (boxHeight - lineHeight(font)) / 2;
        switch (align)
        {
            case LEFT:
                shown = fit(words, font, boxWidth - pad - pad);
                text(shown, x + pad, top, font, colour);
                break;
            case RIGHT:
                shown = fit(words, font, boxWidth - pad - pad);
                textRight(shown, x + boxWidth - pad, top, font, colour);
                break;
            default:
                shown = fit(words, font, boxWidth - 4);
                textCentre(shown, x + boxWidth / 2, top, font, colour);
                break;
        }
    }

    /*
     * fill(x, y, boxWidth, boxHeight, colour): a filled rectangle.
     */
    public void fill(double x, double y, double boxWidth, double boxHeight, Color colour)
    {
        graphics.setColor(colour);
        graphics.fill(new Rectangle2D.Double(x, y, boxWidth, boxHeight));
    }

    /*
     * outline(x, y, boxWidth, boxHeight, colour, thickness): the edge of a rectangle, drawn inside it.
     */
    public void outline(double x, double y, double boxWidth, double boxHeight, Color colour, double thickness)
    {
        graphics.setColor(colour);
        graphics.setStroke(new BasicStroke((float) thickness));
        double half = thickness / 2;
        graphics.draw(new Rectangle2D.Double(x + half, y + half, boxWidth - thickness, boxHeight - thickness));
    }

    /*
     * rule(x, y, length, thickness, colour): a straight rule across the page, its top at y.
     */
    public void rule(double x, double y, double length, double thickness, Color colour)
    {
        fill(x, y, length, thickness, colour);
    }

    /*
     * line(x1, y1, x2, y2, colour, thickness): a thin line between two points (cell outlines).
     */
    public void line(double x1, double y1, double x2, double y2, Color colour, double thickness)
    {
        graphics.setColor(colour);
        graphics.setStroke(new BasicStroke((float) thickness));
        graphics.draw(new Line2D.Double(x1, y1, x2, y2));
    }

    /*
     +--------------------------------------------------------------------------+
     | roundBox(x, y, boxWidth, boxHeight, radius, face, edge, thickness)       |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | A filled box with rounded corners and an outline (the figures boxes).    |
     |                                                                          |
     | Passed in:  x, y, boxWidth, boxHeight - where and how big                |
     |             radius    - how round the corners are                        |
     |             face      - the fill colour                                  |
     |             edge      - the outline colour                               |
     |             thickness - the outline's thickness                          |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    public void roundBox(double x, double y, double boxWidth, double boxHeight, double radius, Color face, Color edge,
            double thickness)
    {
        double half = thickness / 2;
        RoundRectangle2D.Double shape = new RoundRectangle2D.Double(x + half, y + half, boxWidth - thickness,
                boxHeight - thickness, radius + radius, radius + radius);
        graphics.setColor(face);
        graphics.fill(shape);
        graphics.setColor(edge);
        graphics.setStroke(new BasicStroke((float) thickness));
        graphics.draw(shape);
    }

    /*
     * circle(centreX, centreY, across, colour, thickness): an empty circle (the birdie mark).
     */
    public void circle(double centreX, double centreY, double across, Color colour, double thickness)
    {
        graphics.setColor(colour);
        graphics.setStroke(new BasicStroke((float) thickness));
        graphics.draw(new Ellipse2D.Double(centreX - across / 2, centreY - across / 2, across, across));
    }

    /*
     * square(centreX, centreY, across, colour, thickness): an empty square (the bogey mark).
     */
    public void square(double centreX, double centreY, double across, Color colour, double thickness)
    {
        graphics.setColor(colour);
        graphics.setStroke(new BasicStroke((float) thickness));
        graphics.draw(new Rectangle2D.Double(centreX - across / 2, centreY - across / 2, across, across));
    }
}

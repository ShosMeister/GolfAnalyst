package golfanalyst.print;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

/*
 +--------------------------------------------------------------------------+
 | Thumbnail                                                                |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | The small picture of a page in the Print window: page 1 of the real      |
 | report, drawn by the report's own code on Letter paper, scaled down to   |
 | fit a box and centred in it, with a thin grey outline. With nothing to   |
 | print it is a blank page saying "Nothing to print yet". No window is     |
 | needed, so the self-test draws it into a picture.                        |
 |                                                                          |
 | Public Methods:                                                          |
 | draw(graphics, report, x, y, boxWidth, boxHeight) - the picture          |
 | pageArea(report, boxWidth, boxHeight) - where the page sits in the box   |
 +--------------------------------------------------------------------------+
 */
public final class Thumbnail
{

    public static final String NOTHING = "Nothing to print yet";
    public static final Color OUTLINE = new Color(160, 160, 160);

    private Thumbnail()
    {
    }

    /*
     +--------------------------------------------------------------------------+
     | pageArea(report, boxWidth, boxHeight)                                    |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Where the shrunken page sits in a box: as big as fits, centred. An       |
     | empty report (or none) is shown as an upright page.                      |
     |                                                                          |
     | Passed in:  report              - the report, or null                    |
     |             boxWidth, boxHeight - the box                                |
     | Passed out: the page's rectangle inside the box                          |
     +--------------------------------------------------------------------------+
     */
    public static Rectangle2D.Double pageArea(Report report, double boxWidth, double boxHeight)
    {
        double pageWidth = Sheet.LETTER_SHORT;
        double pageHeight = Sheet.LETTER_LONG;
        if (report != null && report.landscape())
        {
            pageWidth = Sheet.LETTER_LONG;
            pageHeight = Sheet.LETTER_SHORT;
        }
        double scale = Math.min((boxWidth - 2) / pageWidth, (boxHeight - 2) / pageHeight);
        double shownWidth = pageWidth * scale;
        double shownHeight = pageHeight * scale;
        return new Rectangle2D.Double((boxWidth - shownWidth) / 2, (boxHeight - shownHeight) / 2, shownWidth, shownHeight);
    }

    /*
     +--------------------------------------------------------------------------+
     | draw(graphics, report, x, y, boxWidth, boxHeight)                        |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Draws the small picture of page 1 in the box at (x, y).                  |
     |                                                                          |
     | Passed in:  graphics            - where to draw (left as it was)         |
     |             report              - the report, or null for nothing        |
     |             x, y                - the box's top left corner              |
     |             boxWidth, boxHeight - its size (188 x 150 in the window)     |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    public static void draw(Graphics2D graphics, Report report, double x, double y, double boxWidth, double boxHeight)
    {
        Graphics2D drawing = (Graphics2D) graphics.create();
        try
        {
            drawing.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            drawing.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            drawing.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            Rectangle2D.Double area = pageArea(report, boxWidth, boxHeight);
            area.x = Math.round(area.x + x);
            area.y = Math.round(area.y + y);
            area.width = Math.round(area.width);
            area.height = Math.round(area.height);
            drawing.setColor(Color.WHITE);
            drawing.fill(area);
            if (report == null || report.isEmpty())
            {
                Font font = new Font(Font.SANS_SERIF, Font.PLAIN, 11);
                drawing.setFont(font);
                drawing.setColor(Sheet.GREY);
                int across = drawing.getFontMetrics().stringWidth(NOTHING);
                drawing.drawString(NOTHING, (float) (area.x + (area.width - across) / 2), (float) (area.y + area.height / 2));
            }
            else
            {
                Shape before = drawing.getClip();
                AffineTransform kept = drawing.getTransform();
                drawing.clip(area);
                drawing.translate(area.x, area.y);
                drawing.scale(area.width / report.width(), area.height / report.letterHeight());
                report.paint(drawing, 0, report.letterHeight());
                drawing.setTransform(kept);
                drawing.setClip(before);
            }
            drawing.setColor(OUTLINE);
            drawing.setStroke(new java.awt.BasicStroke(1f));
            drawing.draw(new Rectangle2D.Double(area.x + 0.5, area.y + 0.5, area.width - 1, area.height - 1));
        }
        finally
        {
            drawing.dispose();
        }
    }
}

package golfanalyst.gui;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JComponent;

/*
 +--------------------------------------------------------------------------+
 | WoodTag                                                                  |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | A small caption on light wood, used above or beside a value box (Rating, |
 | Eagles, Birdies, Fairway, and so on). The text is bold. Its colour (and  |
 | whether it has a shadow) is set by TAG_TEXT and TAG_TEXT_SHADOW in       |
 | Theme, so it can be changed in one place. A tag does nothing when        |
 | clicked and does not change under the mouse.                             |
 +--------------------------------------------------------------------------+
 */
@SuppressWarnings({"serial", "this-escape"})
public class WoodTag extends JComponent
{

    private final String text;

    /*
     * WoodTag(text): makes a tag with the given caption.
     * Passed in:  text - the caption
     * Passed out: the tag
     */
    public WoodTag(String text)
    {
        this.text = text;
        setPreferredSize(new Dimension(122, 22));
        setFont(Theme.font(Font.BOLD, 14f));
    }

    /*
     * paintComponent(graphics): draws the light wood, a raised edge, and the caption.
     * Passed in:  graphics - what to draw on
     * Passed out: nothing
     */
    @Override
    protected void paintComponent(Graphics graphics)
    {
        Graphics2D canvas = (Graphics2D) graphics.create();
        Theme.smooth(canvas);
        Theme.paintStretched(canvas, Theme.image("Wood_Light.gif"), getWidth(), getHeight(), Theme.WOOD_FALLBACK);
        Theme.paintBevel(canvas, getWidth(), getHeight(), true);
        canvas.setFont(getFont());
        float size = getFont().getSize2D();
        while (size > 10f && canvas.getFontMetrics().stringWidth(text) > getWidth() - 8)
        {
            size = size - 1f;
            canvas.setFont(getFont().deriveFont(size));
        }
        Theme.drawCentered(canvas, text, 0, 0, getWidth(), getHeight(), Theme.TAG_TEXT, Theme.TAG_TEXT_SHADOW);
        canvas.dispose();
    }
}

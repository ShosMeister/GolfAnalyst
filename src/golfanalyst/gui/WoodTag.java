package golfanalyst.gui;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;

/*
 +--------------------------------------------------------------------------+
 | WoodTag                                                                  |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | A small caption on light wood, used above or beside a value box (Rating, |
 | Eagles, Birdies, Fairway, and so on). The text is bold. Its colour (and  |
 | whether it has a shadow) is set by TAG_TEXT and TAG_TEXT_SHADOW in       |
 | Theme, so it can be changed in one place. Normally a tag does nothing    |
 | when clicked and does not change under the mouse, and a caption too long |
 | for it is drawn smaller.                                                 |
 |                                                                          |
 | Public Methods:                                                          |
 | setText(text)       - changes the caption                                |
 | setFixedFont(flag)  - keeps the font as set, never shrinking it          |
 | setSortArrow(way)   - a small up (1) or down (-1) triangle in the top    |
 |                       right corner, above the text; 0 for none           |
 | setOnClick(action)  - makes the tag a button: a hand over it, the text   |
 |                       red under the mouse like the wood buttons, and     |
 |                       the action when clicked                            |
 +--------------------------------------------------------------------------+
 */
@SuppressWarnings({"serial", "this-escape"})
public class WoodTag extends JComponent
{

    private String text;
    private boolean fixedFont = false;
    private boolean hover = false;
    private Runnable onClick = null;
    private int sortArrow = 0;
    private static final int ARROW_WIDTH = 7;
    private static final int ARROW_HEIGHT = 4;
    private static final int ARROW_MARGIN = 3;

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
     * text(): the caption.
     */
    public String text()
    {
        return text;
    }

    /*
     * setText(newText): changes the caption.
     * Passed in:  newText - the new caption
     * Passed out: nothing
     */
    public void setText(String newText)
    {
        text = newText;
        repaint();
    }

    /*
     * setFixedFont(flag): when true the caption is always drawn in the tag's own font, even if it is too long, so a row of tags can share one size.
     * Passed in:  flag - true to keep the font as set
     * Passed out: nothing
     */
    public void setFixedFont(boolean flag)
    {
        fixedFont = flag;
        repaint();
    }

    /*
     +--------------------------------------------------------------------------+
     | setOnClick(action)                                                       |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Makes the tag work as a button: the hand cursor over it, its text red    |
     | while the mouse is over it (like the wood buttons), and the action run   |
     | when it is clicked. Call it once.                                        |
     |                                                                          |
     | Passed in:  action - what a click does                                   |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    public void setOnClick(Runnable action)
    {
        onClick = action;
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseEntered(MouseEvent event)
            {
                hover = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent event)
            {
                hover = false;
                repaint();
            }

            @Override
            public void mouseClicked(MouseEvent event)
            {
                onClick.run();
            }
        });
    }

    /*
     * setSortArrow(way): shows a small triangle in the top right corner, above the text so it never covers it: 1 points up, -1 down, 0 none.
     * Passed in:  way - 1, -1 or 0
     * Passed out: nothing
     */
    public void setSortArrow(int way)
    {
        sortArrow = way;
        repaint();
    }

    /*
     * sortArrow(): 1 for an up triangle, -1 for a down one, 0 for none.
     */
    public int sortArrow()
    {
        return sortArrow;
    }

    /*
     * isHover(): whether the mouse is over a clickable tag, so its text is red.
     */
    boolean isHover()
    {
        return hover;
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
        while (!fixedFont && size > 10f && canvas.getFontMetrics().stringWidth(text) > getWidth() - 8)
        {
            size = size - 1f;
            canvas.setFont(getFont().deriveFont(size));
        }
        Theme.drawCentered(canvas, text, 0, 0, getWidth(), getHeight(), (hover ? Theme.TEXT_HOVER : Theme.TAG_TEXT), Theme.TAG_TEXT_SHADOW);
        if (sortArrow != 0)
        {
            int right = getWidth() - ARROW_MARGIN;
            int left = right - ARROW_WIDTH;
            int top = ARROW_MARGIN;
            int bottom = top + ARROW_HEIGHT;
            int[] xs = {left, right, left + ARROW_WIDTH / 2};
            int[] ys = (sortArrow > 0 ? new int[] {bottom, bottom, top} : new int[] {top, top, bottom});
            canvas.setColor(hover ? Theme.TEXT_HOVER : Theme.TAG_TEXT);
            canvas.fillPolygon(xs, ys, 3);
        }
        canvas.dispose();
    }
}

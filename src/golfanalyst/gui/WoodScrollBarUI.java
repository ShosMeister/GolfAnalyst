package golfanalyst.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.SwingConstants;
import javax.swing.plaf.basic.BasicScrollBarUI;

/*
 +--------------------------------------------------------------------------+
 | WoodScrollBarUI                                                          |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | Paints a vertical scroll bar in the wood style, the same on every        |
 | look-and-feel. It behaves like any scroll bar (the thumb's height is the |
 | part of the list that can be seen), but some look-and-feels draw an      |
 | empty track when the whole list fits, so here the thumb then fills the   |
 | whole track instead.                                                     |
 |                                                                          |
 | Track: the WoodFrame's own wood at its place in the frame (see           |
 | WoodFrame.paintFrameWood), so the grain runs on from the frame's border, |
 | with only a thin dark line down its left edge against the white list.    |
 | Thumb: light wood turned so its grain runs along the bar, scaled         |
 | smoothly (never tiled, which shows seams), set in 2 px at the sides and  |
 | 1 px at the ends, with a raised edge and, on a thumb taller than 30 px,  |
 | three grip lines. Arrow buttons: 17 x 17 light wood with a raised edge   |
 | and a dark brown triangle.                                               |
 |                                                                          |
 | Public Methods:                                                          |
 | thumbArea() - where the thumb is drawn (the whole track when it fits)    |
 +--------------------------------------------------------------------------+
 */
public final class WoodScrollBarUI extends BasicScrollBarUI
{

    static final int BAR_WIDTH = 17;
    private static final int THUMB_SIDE_INSET = 2;
    private static final int THUMB_END_INSET = 1;
    private static final int GRIP_MIN_HEIGHT = 30;
    private static final int GRIP_GAP = 4;
    private static final int GRIP_SIDE_INSET = 3;
    private static final Color TRACK_EDGE = new Color(40, 15, 0, 200);
    private static final Color GRIP_DARK = new Color(90, 45, 5, 170);
    private static final Color GRIP_LIGHT = new Color(255, 235, 200, 140);
    private static final Color ARROW = new Color(70, 35, 5);
    private static final String LIGHT_WOOD = "Wood_Light.gif";
    private static BufferedImage turnedWood = null;

    /*
     * turnedWood(): the light wood picture turned 90 degrees, made once, so its grain runs along the bar (null if the picture is missing).
     */
    private static BufferedImage turnedWood()
    {
        BufferedImage wood = Theme.image(LIGHT_WOOD);
        if (turnedWood == null && wood != null)
        {
            BufferedImage turned = new BufferedImage(wood.getHeight(), wood.getWidth(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D canvas = turned.createGraphics();
            canvas.translate(wood.getHeight(), 0);
            canvas.rotate(Math.PI / 2);
            canvas.drawImage(wood, 0, 0, null);
            canvas.dispose();
            turnedWood = turned;
        }
        return turnedWood;
    }

    /*
     * paintLightWood(canvas, area, picture): light wood scaled smoothly over a rectangle, with a raised edge.
     * Passed in:  canvas - what to draw on, area - where, picture - the wood (or null for a plain colour)
     * Passed out: nothing
     */
    private static void paintLightWood(Graphics2D canvas, Rectangle area, BufferedImage picture)
    {
        Graphics2D wood = (Graphics2D) canvas.create();
        wood.translate(area.x, area.y);
        wood.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        Theme.paintStretched(wood, picture, area.width, area.height, Theme.WOOD_FALLBACK);
        Theme.paintBevel(wood, area.width, area.height, true);
        wood.dispose();
    }

    /*
     * getPreferredSize(component): always 17 px wide.
     */
    @Override
    public Dimension getPreferredSize(JComponent component)
    {
        return new Dimension(BAR_WIDTH, super.getPreferredSize(component).height);
    }

    /*
     * paint(graphics, component): the track, then always the thumb (the plain version skips an empty thumb, which happens when the whole list fits).
     */
    @Override
    public void paint(Graphics graphics, JComponent component)
    {
        paintTrack(graphics, component, getTrackBounds());
        paintThumb(graphics, component, getThumbBounds());
    }

    /*
     * paintTrack(graphics, component, trackBounds): the frame's own wood under the whole bar, and a dark line down its left edge.
     */
    @Override
    protected void paintTrack(Graphics graphics, JComponent component, Rectangle trackBounds)
    {
        Graphics2D canvas = (Graphics2D) graphics.create();
        WoodFrame.paintFrameWood(canvas, component);
        canvas.setColor(TRACK_EDGE);
        canvas.drawLine(0, 0, 0, component.getHeight() - 1);
        canvas.dispose();
    }

    /*
     * thumbArea(): where the thumb is drawn: its own bounds, or the whole track when the list fits and there is no thumb.
     * Passed in:  nothing
     * Passed out: the rectangle, before the 2 px and 1 px insets
     */
    Rectangle thumbArea()
    {
        Rectangle thumb = getThumbBounds();
        return (thumb.isEmpty() ? getTrackBounds() : thumb);
    }

    /*
     +--------------------------------------------------------------------------+
     | paintThumb(graphics, component, thumbBounds)                             |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Draws the thumb (over the whole track if it is empty): the turned light  |
     | wood scaled smoothly, set in 2 px at the sides and 1 px at the ends,     |
     | with a raised edge, and three grip lines across the middle when it is    |
     | taller than 30 px, each dark line with a light line just below it.       |
     |                                                                          |
     | Passed in:  graphics    - what to draw on                                |
     |             component   - the scroll bar                                 |
     |             thumbBounds - the thumb's bounds (may be empty)              |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    @Override
    protected void paintThumb(Graphics graphics, JComponent component, Rectangle thumbBounds)
    {
        Rectangle area = (thumbBounds.isEmpty() ? getTrackBounds() : thumbBounds);
        Rectangle inset = new Rectangle(area.x + THUMB_SIDE_INSET, area.y + THUMB_END_INSET,
                area.width - 2 * THUMB_SIDE_INSET, area.height - 2 * THUMB_END_INSET);
        if (inset.width <= 0 || inset.height <= 0)
        {
            return;
        }
        Graphics2D canvas = (Graphics2D) graphics.create();
        paintLightWood(canvas, inset, turnedWood());
        if (inset.height > GRIP_MIN_HEIGHT)
        {
            int middle = inset.y + inset.height / 2;
            for (int lp = -1; lp <= 1; lp = lp + 1)
            {
                int lineY = middle + lp * GRIP_GAP;
                canvas.setColor(GRIP_DARK);
                canvas.drawLine(inset.x + GRIP_SIDE_INSET, lineY, inset.x + inset.width - 1 - GRIP_SIDE_INSET, lineY);
                canvas.setColor(GRIP_LIGHT);
                canvas.drawLine(inset.x + GRIP_SIDE_INSET, lineY + 1, inset.x + inset.width - 1 - GRIP_SIDE_INSET, lineY + 1);
            }
        }
        canvas.dispose();
    }

    /*
     * createDecreaseButton(orientation): the wood arrow at the top.
     */
    @Override
    protected JButton createDecreaseButton(int orientation)
    {
        return new ArrowButton(SwingConstants.NORTH);
    }

    /*
     * createIncreaseButton(orientation): the wood arrow at the bottom.
     */
    @Override
    protected JButton createIncreaseButton(int orientation)
    {
        return new ArrowButton(SwingConstants.SOUTH);
    }

    /*
     +--------------------------------------------------------------------------+
     | ArrowButton                                                              |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | A 17 x 17 scroll bar arrow: light wood with a raised edge and a dark     |
     | brown triangle pointing up or down. The scroll bar does the scrolling.   |
     +--------------------------------------------------------------------------+
     */
    @SuppressWarnings("serial")
    private static final class ArrowButton extends JButton
    {
        private final int direction;

        /*
         * ArrowButton(direction): an arrow pointing SwingConstants.NORTH or SOUTH.
         */
        ArrowButton(int direction)
        {
            this.direction = direction;
            setFocusable(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setPreferredSize(new Dimension(BAR_WIDTH, BAR_WIDTH));
        }

        /*
         * paintComponent(graphics): the light wood, its raised edge and the triangle.
         */
        @Override
        protected void paintComponent(Graphics graphics)
        {
            Graphics2D canvas = (Graphics2D) graphics.create();
            Theme.smooth(canvas);
            paintLightWood(canvas, new Rectangle(0, 0, getWidth(), getHeight()), Theme.image(LIGHT_WOOD));
            int middle = getWidth() / 2;
            int top = getHeight() / 2 - 2;
            int bottom = getHeight() / 2 + 2;
            int[] xs = {middle - 4, middle + 4, middle};
            int[] ys = (direction == SwingConstants.NORTH ? new int[] {bottom, bottom, top - 1} : new int[] {top, top, bottom + 1});
            canvas.setColor(ARROW);
            canvas.fillPolygon(xs, ys, 3);
            canvas.dispose();
        }
    }
}

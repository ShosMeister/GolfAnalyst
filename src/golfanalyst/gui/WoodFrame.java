package golfanalyst.gui;

import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.BorderFactory;
import javax.swing.JPanel;

/*
 +--------------------------------------------------------------------------+
 | WoodFrame                                                                |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | A panel with a wooden frame around its contents, like the frames around  |
 | the hole-by-hole tables on the round screen. The wood is one stretched   |
 | picture, so there are no joins. Put the contents in the middle with      |
 | add(component, BorderLayout.CENTER).                                     |
 +--------------------------------------------------------------------------+
 */
@SuppressWarnings({"serial", "this-escape"})
public class WoodFrame extends JPanel
{

    private static final int FRAME_WIDTH = 9;

    /*
     * WoodFrame(): makes a frame with a border wide enough to show the wood.
     */
    public WoodFrame()
    {
        super(new BorderLayout());
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(FRAME_WIDTH, FRAME_WIDTH, FRAME_WIDTH, FRAME_WIDTH));
    }

    /*
     * paintComponent(graphics): draws the wood over the whole area, a raised outer edge and a sunken inner edge.
     * Passed in:  graphics - what to draw on
     * Passed out: nothing
     */
    @Override
    protected void paintComponent(Graphics graphics)
    {
        Graphics2D canvas = (Graphics2D) graphics.create();
        Theme.smooth(canvas);
        Theme.paintStretched(canvas, Theme.image("TitleBkg_Teak.jpg"), getWidth(), getHeight(), Theme.WOOD_FALLBACK);
        Theme.paintBevel(canvas, getWidth(), getHeight(), true);
        canvas.translate(FRAME_WIDTH - 2, FRAME_WIDTH - 2);
        Theme.paintBevel(canvas, getWidth() - 2 * (FRAME_WIDTH - 2), getHeight() - 2 * (FRAME_WIDTH - 2), false);
        canvas.dispose();
    }
}

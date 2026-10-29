package golfanalyst.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/*
 +--------------------------------------------------------------------------+
 | WoodFrame                                                                |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | A panel with a wooden frame around its contents, like the frames around  |
 | the hole-by-hole tables on the round screen. The wood is one stretched   |
 | picture, so there are no joins. Put the contents in the middle with      |
 | add(component, BorderLayout.CENTER).                                     |
 |                                                                          |
 | Public Methods:                                                          |
 | paintFrameWood(canvas, part) - paints the frame's own wood under a part  |
 |                                inside it, so the grain runs on unbroken  |
 +--------------------------------------------------------------------------+
 */
@SuppressWarnings({"serial", "this-escape"})
public class WoodFrame extends JPanel
{

    private static final int FRAME_WIDTH = 9;
    static final String WOOD_PICTURE = "TitleBkg_Teak.jpg";

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
        Theme.paintStretched(canvas, Theme.image(WOOD_PICTURE), getWidth(), getHeight(), Theme.WOOD_FALLBACK);
        Theme.paintBevel(canvas, getWidth(), getHeight(), true);
        canvas.translate(FRAME_WIDTH - 2, FRAME_WIDTH - 2);
        Theme.paintBevel(canvas, getWidth() - 2 * (FRAME_WIDTH - 2), getHeight() - 2 * (FRAME_WIDTH - 2), false);
        canvas.dispose();
    }
    /*
     +--------------------------------------------------------------------------+
     | paintFrameWood(canvas, part)                                             |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Paints the wood of the WoodFrame a part sits in, exactly where that      |
     | part is in the frame, instead of a fresh stretched copy, so the grain    |
     | flows unbroken from the frame's border across the part. It finds the     |
     | WoodFrame ancestor, works out the part's position in the frame, clips    |
     | to the part, moves back by that position and draws the same stretched    |
     | picture WoodFrame draws, at the frame's full size. A part that is not    |
     | in a WoodFrame gets the picture stretched over itself.                   |
     |                                                                          |
     | Passed in:  canvas - the part's graphics                                 |
     |             part   - the component being painted                         |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    public static void paintFrameWood(Graphics2D canvas, Component part)
    {
        Graphics2D wood = (Graphics2D) canvas.create();
        wood.clipRect(0, 0, part.getWidth(), part.getHeight());
        Theme.smooth(wood);
        WoodFrame frame = (WoodFrame) SwingUtilities.getAncestorOfClass(WoodFrame.class, part);
        if (frame == null)
        {
            Theme.paintStretched(wood, Theme.image(WOOD_PICTURE), part.getWidth(), part.getHeight(), Theme.WOOD_FALLBACK);
        }
        else
        {
            Point at = SwingUtilities.convertPoint(part, 0, 0, frame);
            wood.translate(-at.x, -at.y);
            Theme.paintStretched(wood, Theme.image(WOOD_PICTURE), frame.getWidth(), frame.getHeight(), Theme.WOOD_FALLBACK);
        }
        wood.dispose();
    }
}

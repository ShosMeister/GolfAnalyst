package golfanalyst.gui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.swing.JButton;

/*
 +--------------------------------------------------------------------------+
 | IconButton                                                               |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | The round wood buttons with a picture on them: Home, Help, Info and the  |
 | First / Previous / Next / Last arrows. When the mouse is over one it     |
 | lights up with a red ring (the same red the text buttons use); when it is|
 | pressed it darkens and moves down one pixel; a disabled one fades out.   |
 |                                                                          |
 | The picture comes from the images folder (for example "Home.png") and is |
 | scaled smoothly to the button's size. It behaves as an ordinary Swing    |
 | button.                                                                  |
 +--------------------------------------------------------------------------+
 */
@SuppressWarnings({"serial", "this-escape"})
public class IconButton extends JButton
{

    private final String imageName;
    private boolean hover = false;
    private boolean ring = false;

    /*
     +--------------------------------------------------------------------------+
     | IconButton(imageName, toolTip, size)                                     |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Makes a round picture button of the given size.                          |
     |                                                                          |
     | Passed in:  imageName - picture file in the images folder, "Home.png"    |
     |             toolTip   - the words shown when the mouse rests on it       |
     |             size      - width and height in pixels                       |
     | Passed out: the button                                                   |
     +--------------------------------------------------------------------------+
     */
    public IconButton(String imageName, String toolTip, int size)
    {
        this.imageName = imageName;
        setToolTipText(toolTip);
        getAccessibleContext().setAccessibleName(toolTip);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setPreferredSize(new Dimension(size, size));
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
        });
    }

    /*
     * setRing(flag): turns the always-on gold ring around the button on or off (the red hover ring replaces it while the mouse is over the button).
     * Passed in:  flag - true to show the gold ring all the time
     * Passed out: nothing
     */
    public void setRing(boolean flag)
    {
        ring = flag;
        repaint();
    }

    /*
     * paintComponent(graphics): draws the picture, with a gold ring if asked for, a red ring when the mouse is over it, a white dashed ring when it has keyboard focus, and a fade when disabled.
     * Passed in:  graphics - what to draw on
     * Passed out: nothing
     */
    @Override
    protected void paintComponent(Graphics graphics)
    {
        Graphics2D canvas = (Graphics2D) graphics.create();
        Theme.smooth(canvas);
        int size = Math.min(getWidth(), getHeight()) - 4;
        boolean pressed = getModel().isPressed() && isEnabled();
        int x = (getWidth() - size) / 2 + (pressed ? 1 : 0);
        int y = (getHeight() - size) / 2 + (pressed ? 1 : 0);
        if (!isEnabled())
        {
            canvas.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.40f));
        }
        BufferedImage picture = Theme.image(imageName);
        if (picture != null)
        {
            canvas.drawImage(picture, x, y, size, size, null);
        }
        else
        {
            canvas.setColor(Theme.WOOD_FALLBACK);
            canvas.fillOval(x, y, size, size);
        }
        if (hover && isEnabled())
        {
            canvas.setColor(new Color(255, 255, 255, 45));
            canvas.fillOval(x, y, size, size);
            canvas.setColor(Theme.TEXT_HOVER);
            canvas.setStroke(new BasicStroke(3f));
            canvas.drawOval(x + 1, y + 1, size - 3, size - 3);
        }
        else if (ring)
        {
            canvas.setColor(Theme.GOLD);
            canvas.setStroke(new BasicStroke(3f));
            canvas.drawOval(x + 1, y + 1, size - 3, size - 3);
        }
        if (pressed)
        {
            canvas.setColor(new Color(0, 0, 0, 80));
            canvas.fillOval(x, y, size, size);
        }
        if (isFocusOwner())
        {
            canvas.setColor(Color.WHITE);
            canvas.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[] {3f, 3f}, 0f));
            canvas.drawOval(x - 1, y - 1, size + 1, size + 1);
        }
        canvas.dispose();
    }
}

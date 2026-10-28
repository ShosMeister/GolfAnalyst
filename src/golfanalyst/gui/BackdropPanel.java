package golfanalyst.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;

/*
 +--------------------------------------------------------------------------+
 | BackdropPanel                                                            |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | A panel with a photograph behind it. The photo is scaled to cover the    |
 | whole panel (the excess is cropped, the picture is never squashed) and   |
 | the scaled copy is kept until the panel's size changes. The program name |
 | and version are drawn in yellow at the bottom left, as in the earlier    |
 | versions. Every screen of the program is a subclass of this, each with   |
 | its own photo.                                                           |
 |                                                                          |
 | Public Methods:                                                          |
 | setBackdrop(picture) - sets the photo (null gives a plain green)         |
 +--------------------------------------------------------------------------+
 */
@SuppressWarnings({"serial", "this-escape"})
public class BackdropPanel extends JPanel
{

    private BufferedImage backdrop;
    private BufferedImage scaled;
    private int scaledWidth = -1;
    private int scaledHeight = -1;

    /*
     * BackdropPanel(): makes an opaque panel; the photo is set with setBackdrop.
     */
    public BackdropPanel()
    {
        setOpaque(true);
    }

    /*
     * setBackdrop(picture): sets the photo behind the panel.
     * Passed in:  picture - the photo, or null for a plain colour
     * Passed out: nothing
     */
    public void setBackdrop(BufferedImage picture)
    {
        backdrop = picture;
        scaled = null;
        repaint();
    }

    /*
     +--------------------------------------------------------------------------+
     | coverScaled()                                                            |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Makes (or reuses) a copy of the photo scaled just large enough to        |
     | cover the panel, centred, so the panel is completely filled.             |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: a picture exactly the size of the panel                      |
     +--------------------------------------------------------------------------+
     */
    private BufferedImage coverScaled()
    {
        if (scaled != null && scaledWidth == getWidth() && scaledHeight == getHeight())
        {
            return scaled;
        }
        BufferedImage fitted = new BufferedImage(Math.max(1, getWidth()), Math.max(1, getHeight()), BufferedImage.TYPE_INT_RGB);
        Graphics2D canvas = fitted.createGraphics();
        Theme.smooth(canvas);
        double factor = Math.max(getWidth() / (double) backdrop.getWidth(), getHeight() / (double) backdrop.getHeight());
        int drawWidth = (int) Math.ceil(backdrop.getWidth() * factor);
        int drawHeight = (int) Math.ceil(backdrop.getHeight() * factor);
        canvas.drawImage(backdrop, (getWidth() - drawWidth) / 2, (getHeight() - drawHeight) / 2, drawWidth, drawHeight, null);
        canvas.dispose();
        scaled = fitted;
        scaledWidth = getWidth();
        scaledHeight = getHeight();
        return scaled;
    }

    /*
     * paintComponent(graphics): draws the photo (or plain green) and the version text.
     * Passed in:  graphics - what to draw on
     * Passed out: nothing
     */
    @Override
    protected void paintComponent(Graphics graphics)
    {
        Graphics2D canvas = (Graphics2D) graphics.create();
        if (backdrop == null)
        {
            canvas.setColor(Theme.BACKDROP_FALLBACK);
            canvas.fillRect(0, 0, getWidth(), getHeight());
        }
        else
        {
            canvas.drawImage(coverScaled(), 0, 0, null);
        }
        Theme.smooth(canvas);
        canvas.setFont(Theme.font(Font.BOLD | Font.ITALIC, 22f));
        canvas.setColor(Theme.TEXT_SHADOW);
        canvas.drawString(Theme.VERSION_TEXT, 11, getHeight() - 11);
        canvas.setColor(new Color(255, 255, 0));
        canvas.drawString(Theme.VERSION_TEXT, 10, getHeight() - 12);
        canvas.dispose();
    }
}

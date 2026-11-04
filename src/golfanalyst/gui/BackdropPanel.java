package golfanalyst.gui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;

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
 | The version text is also a "peek" button: while the left mouse button    |
 | is held down on it (the text plus 6 px round it), only the photo and     |
 | the text are drawn and every control of the page is hidden, so the       |
 | whole picture can be seen. Letting go of the button, anywhere, brings    |
 | everything back. While peeking the keyboard is ignored, so nothing       |
 | hidden can be typed into. The mouse shows a hand over the text, with a   |
 | tool tip saying what it does.                                            |
 |                                                                          |
 | Public Methods:                                                          |
 | setBackdrop(picture) - sets the photo (null gives a plain green)         |
 | isPeeking()          - whether the controls are hidden right now         |
 | versionLabelBounds() - where the version text is drawn                   |
 +--------------------------------------------------------------------------+
 */
@SuppressWarnings({"serial", "this-escape"})
public class BackdropPanel extends JPanel
{

    static final String PEEK_TIP = "Press and hold to see the whole picture.";
    private static final float VERSION_FONT = 22f;
    private static final int VERSION_LEFT = 10;
    private static final int VERSION_BOTTOM = 12;
    private static final int PEEK_MARGIN = 6;

    private BufferedImage backdrop;
    private BufferedImage scaled;
    private int scaledWidth = -1;
    private int scaledHeight = -1;
    private boolean peeking = false;
    private final transient KeyEventDispatcher keyBlocker = event -> true;

    /*
     +--------------------------------------------------------------------------+
     | BackdropPanel()                                                          |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Makes an opaque panel (the photo is set with setBackdrop) and listens    |
     | to the mouse for the peek: pressing the left button on the version       |
     | text starts it, releasing the button ends it, and moving over the text   |
     | shows the hand.                                                          |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: the panel                                                    |
     +--------------------------------------------------------------------------+
     */
    public BackdropPanel()
    {
        setOpaque(true);
        ToolTipManager.sharedInstance().registerComponent(this);
        MouseAdapter peekMouse = new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent event)
            {
                if (SwingUtilities.isLeftMouseButton(event) && peekArea().contains(event.getPoint()))
                {
                    startPeek();
                }
            }

            @Override
            public void mouseReleased(MouseEvent event)
            {
                if (SwingUtilities.isLeftMouseButton(event))
                {
                    stopPeek();
                }
            }

            @Override
            public void mouseMoved(MouseEvent event)
            {
                // a move with no button down means the release was missed: end the peek anyway
                stopPeek();
                showHand(peekArea().contains(event.getPoint()));
            }

            @Override
            public void mouseExited(MouseEvent event)
            {
                if (!peeking)
                {
                    showHand(false);
                }
            }
        };
        addMouseListener(peekMouse);
        addMouseMotionListener(peekMouse);
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
     * hasBackdrop(): whether the panel has a photo (false means the plain colour is shown).
     */
    public boolean hasBackdrop()
    {
        return backdrop != null;
    }

    /*
     * isPeeking(): whether the left button is being held on the version text, so the page's controls are hidden.
     */
    public boolean isPeeking()
    {
        return peeking;
    }

    /*
     +--------------------------------------------------------------------------+
     | versionLabelBounds()                                                     |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Works out the rectangle the version text is drawn in at the panel's      |
     | present size: from its left edge to the end of the text (plus the 1 px   |
     | shadow), and from the top of the letters to the bottom of the lowest     |
     | ones.                                                                    |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: the rectangle, in this panel's coordinates                   |
     +--------------------------------------------------------------------------+
     */
    public Rectangle versionLabelBounds()
    {
        FontMetrics metrics = getFontMetrics(versionFont());
        int baseline = getHeight() - VERSION_BOTTOM;
        return new Rectangle(VERSION_LEFT, baseline - metrics.getAscent(), metrics.stringWidth(Theme.VERSION_TEXT) + 1,
                metrics.getAscent() + metrics.getDescent() + 1);
    }

    /*
     * peekArea(): where a press starts the peek: the version text and 6 px all round it.
     */
    private Rectangle peekArea()
    {
        Rectangle area = versionLabelBounds();
        area.grow(PEEK_MARGIN, PEEK_MARGIN);
        return area;
    }

    /*
     * versionFont(): the bold italic 22 pt font of the version text.
     */
    private static Font versionFont()
    {
        return Theme.font(Font.BOLD | Font.ITALIC, VERSION_FONT);
    }

    /*
     * showHand(over): the hand cursor while over the version text, the ordinary arrow elsewhere.
     * Passed in:  over - true when the mouse is over the text
     * Passed out: nothing
     */
    private void showHand(boolean over)
    {
        setCursor(over ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : null);
    }

    /*
     * startPeek(): hides the page's controls and ignores the keyboard until the button is let go.
     */
    private void startPeek()
    {
        if (!peeking)
        {
            peeking = true;
            KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(keyBlocker);
            repaint();
        }
    }

    /*
     * stopPeek(): shows the page's controls again and gives the keyboard back.
     */
    private void stopPeek()
    {
        if (peeking)
        {
            peeking = false;
            KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(keyBlocker);
            repaint();
        }
    }

    /*
     * getToolTipText(event): the peek tool tip over the version text, and none anywhere else on the panel.
     * Passed in:  event - where the mouse is
     * Passed out: the tip, or null
     */
    @Override
    public String getToolTipText(MouseEvent event)
    {
        return (peekArea().contains(event.getPoint()) ? PEEK_TIP : null);
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
        canvas.setFont(versionFont());
        canvas.setColor(Theme.TEXT_SHADOW);
        canvas.drawString(Theme.VERSION_TEXT, VERSION_LEFT + 1, getHeight() - VERSION_BOTTOM + 1);
        canvas.setColor(new Color(255, 255, 0));
        canvas.drawString(Theme.VERSION_TEXT, VERSION_LEFT, getHeight() - VERSION_BOTTOM);
        canvas.dispose();
    }

    /*
     * isPaintingOrigin(): while peeking, a control that repaints itself (a blinking cursor, a button lighting up) is painted through this panel, so it stays hidden too.
     */
    @Override
    protected boolean isPaintingOrigin()
    {
        return peeking;
    }

    /*
     * paintChildren(graphics): draws the page's controls, except while peeking.
     * Passed in:  graphics - what to draw on
     * Passed out: nothing
     */
    @Override
    protected void paintChildren(Graphics graphics)
    {
        if (!peeking)
        {
            super.paintChildren(graphics);
        }
    }
}

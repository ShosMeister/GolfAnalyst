package golfanalyst.gui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.swing.JButton;

/*
 +--------------------------------------------------------------------------+
 | WoodButton                                                               |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | The dark wood button with the gold edge, used for every action on every  |
 | screen. The text is large, bold and white with a dark shadow so it can be|
 | read on the wood. When the mouse is over the button the text turns red,  |
 | as in the earlier versions; when the button is pressed the face darkens  |
 | and the text moves down one pixel; a disabled button has grey text.      |
 |                                                                          |
 | It behaves as an ordinary Swing button (action listeners, enabling,      |
 | keyboard use); only the painting is its own.                             |
 +--------------------------------------------------------------------------+
 */
@SuppressWarnings({"serial", "this-escape"})
public class WoodButton extends JButton
{

    private boolean hover = false;
    private boolean defaultRing = false;

    /*
     +--------------------------------------------------------------------------+
     | WoodButton(text)                                                         |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Makes the button and sets it up to repaint when the mouse enters or      |
     | leaves it.                                                               |
     |                                                                          |
     | Passed in:  text - the words on the button                               |
     | Passed out: the button                                                   |
     +--------------------------------------------------------------------------+
     */
    public WoodButton(String text)
    {
        super(text);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setPreferredSize(new Dimension(Theme.WOOD_BUTTON_WIDTH, Theme.WOOD_BUTTON_HEIGHT));
        setFont(Theme.font(Font.BOLD, 16f));
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
     * setDefaultRing(flag): marks a dialog's default button, which keeps its gold ring even while the focus is elsewhere (in a text box, say).
     * Passed in:  flag - true for the default button
     * Passed out: nothing
     */
    public void setDefaultRing(boolean flag)
    {
        defaultRing = flag;
        repaint();
    }

    /*
     * hasDefaultRing(): whether this is a dialog's default button (for the self-test).
     */
    public boolean hasDefaultRing()
    {
        return defaultRing;
    }

    /*
     * paintComponent(graphics): draws the wood face, then the text in white, red (mouse over) or grey (disabled), and the gold ring when it has the focus or is a dialog's default button.
     * Passed in:  graphics - what to draw on
     * Passed out: nothing
     */
    @Override
    protected void paintComponent(Graphics graphics)
    {
        Graphics2D canvas = (Graphics2D) graphics.create();
        Theme.smooth(canvas);
        BufferedImage wood = Theme.image("ButtonSquareWood.jpg");
        Theme.paintStretched(canvas, wood, getWidth(), getHeight(), Theme.WOOD_FALLBACK);
        boolean pressed = getModel().isPressed() && isEnabled();
        if (pressed)
        {
            canvas.setColor(new Color(0, 0, 0, 80));
            canvas.fillRect(0, 0, getWidth(), getHeight());
        }
        canvas.setFont(getFont());
        float size = getFont().getSize2D();
        while (size > 11f && canvas.getFontMetrics().stringWidth(getText()) > getWidth() - 16)
        {
            size = size - 1f;
            canvas.setFont(getFont().deriveFont(size));
        }
        Color color = !isEnabled() ? Theme.TEXT_DISABLED : (hover ? Theme.TEXT_HOVER : Theme.TEXT_LIGHT);
        int shift = pressed ? 1 : 0;
        Theme.drawCentered(canvas, getText(), shift, shift, getWidth(), getHeight(), color, true);
        if (isFocusOwner() || defaultRing)
        {
            canvas.setColor(new Color(255, 214, 0, 220));
            canvas.drawRect(3, 3, getWidth() - 7, getHeight() - 7);
        }
        canvas.dispose();
    }
}

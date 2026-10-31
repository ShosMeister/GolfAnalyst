package golfanalyst.gui;

import java.awt.AlphaComposite;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;

/*
 +--------------------------------------------------------------------------+
 | ImageCheckBox                                                            |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | The check box used in the hole-by-hole tables, drawn with the checked    |
 | and unchecked pictures from the earlier versions. Click it to turn it    |
 | on or off, or press Space when the cursor is on it. When it is disabled  |
 | (a fairway on a par 3, or a hole that is not being played) it is drawn   |
 | faded and ignores clicks and keys.                                       |
 |                                                                          |
 | Public Methods:                                                          |
 | isSelected(), setSelected(flag) - the on/off state                       |
 | setOnChange(action)             - what to do when the user clicks it     |
 | toggle()                        - flips it as a click would              |
 | setPictureSize(size)            - draws the picture smaller, e.g. 18     |
 |                                                                          |
 | If it is made opaque it fills its background colour first, so it can be  |
 | a cell of a table.                                                       |
 +--------------------------------------------------------------------------+
 */
@SuppressWarnings({"serial", "this-escape"})
public class ImageCheckBox extends JComponent
{

    private boolean selected = false;
    private Runnable onChange = null;
    private int pictureSize = 0;

    /*
     * ImageCheckBox(): makes an unchecked box that toggles when clicked, or when Space is pressed on it.
     */
    public ImageCheckBox()
    {
        setPreferredSize(new Dimension(22, 22));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent event)
            {
                toggle();
            }
        });
        addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyPressed(KeyEvent event)
            {
                if (event.getKeyCode() == KeyEvent.VK_SPACE)
                {
                    toggle();
                    event.consume();
                }
            }
        });
    }

    /*
     * toggle(): what a click or Space does: if the box is switched on, flips it and runs the change action.
     * Passed in:  nothing
     * Passed out: nothing
     */
    public void toggle()
    {
        if (isEnabled())
        {
            selected = !selected;
            repaint();
            if (onChange != null)
            {
                onChange.run();
            }
        }
    }

    /*
     * isSelected(): whether the box is checked.
     */
    public boolean isSelected()
    {
        return selected;
    }

    /*
     * setSelected(flag): checks or unchecks the box without calling the change action.
     * Passed in:  flag - true to check it
     * Passed out: nothing
     */
    public void setSelected(boolean flag)
    {
        selected = flag;
        repaint();
    }

    /*
     * setOnChange(action): sets what runs when the user clicks the box.
     * Passed in:  action - code to run, or null for nothing
     * Passed out: nothing
     */
    public void setOnChange(Runnable action)
    {
        onChange = action;
    }

    /*
     * setPictureSize(size): draws the picture at this width and height instead of its own size.
     * Passed in:  size - pixels, or 0 for the picture's own size
     * Passed out: nothing
     */
    public void setPictureSize(int size)
    {
        pictureSize = size;
        repaint();
    }

    /*
     * paintComponent(graphics): fills the background if opaque, then draws the checked or unchecked picture in the middle, faded if disabled.
     * Passed in:  graphics - what to draw on
     * Passed out: nothing
     */
    @Override
    protected void paintComponent(Graphics graphics)
    {
        Graphics2D canvas = (Graphics2D) graphics.create();
        Theme.smooth(canvas);
        if (isOpaque())
        {
            canvas.setColor(getBackground());
            canvas.fillRect(0, 0, getWidth(), getHeight());
        }
        if (!isEnabled())
        {
            canvas.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
        }
        BufferedImage picture = Theme.image(selected ? "checkbox_selOSX.gif" : "checkbox_OSX.gif");
        if (picture != null)
        {
            int width = (pictureSize > 0 ? pictureSize : picture.getWidth());
            int height = (pictureSize > 0 ? pictureSize : picture.getHeight());
            canvas.drawImage(picture, (getWidth() - width) / 2, (getHeight() - height) / 2, width, height, null);
        }
        else
        {
            canvas.drawRect(4, 4, getWidth() - 9, getHeight() - 9);
            if (selected)
            {
                canvas.drawLine(6, 6, getWidth() - 7, getHeight() - 7);
                canvas.drawLine(6, getHeight() - 7, getWidth() - 7, 6);
            }
        }
        canvas.dispose();
    }
}

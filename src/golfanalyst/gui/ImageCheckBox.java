package golfanalyst.gui;

import java.awt.AlphaComposite;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
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
 | on or off. When it is disabled (a fairway on a par 3, or a hole that is  |
 | not being played) it is drawn faded and ignores clicks.                  |
 |                                                                          |
 | Public Methods:                                                          |
 | isSelected(), setSelected(flag) - the on/off state                       |
 | setOnChange(action)             - what to do when the user clicks it     |
 +--------------------------------------------------------------------------+
 */
@SuppressWarnings({"serial", "this-escape"})
public class ImageCheckBox extends JComponent
{

    private boolean selected = false;
    private Runnable onChange = null;

    /*
     * ImageCheckBox(): makes an unchecked box that toggles when clicked.
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
        });
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
     * paintComponent(graphics): draws the checked or unchecked picture in the middle, faded if disabled.
     * Passed in:  graphics - what to draw on
     * Passed out: nothing
     */
    @Override
    protected void paintComponent(Graphics graphics)
    {
        Graphics2D canvas = (Graphics2D) graphics.create();
        Theme.smooth(canvas);
        if (!isEnabled())
        {
            canvas.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
        }
        BufferedImage picture = Theme.image(selected ? "checkbox_selOSX.gif" : "checkbox_OSX.gif");
        if (picture != null)
        {
            canvas.drawImage(picture, (getWidth() - picture.getWidth()) / 2, (getHeight() - picture.getHeight()) / 2, null);
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

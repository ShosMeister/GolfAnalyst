package golfanalyst.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

/*
 +--------------------------------------------------------------------------+
 | ValueBox                                                                 |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | A small sunken box that shows one calculated value (a handicap, a        |
 | percentage, a total). The grey face tells the user it cannot be typed    |
 | into. It can be flagged as an error, which turns the face pink.          |
 |                                                                          |
 | Public Methods:                                                          |
 | setValue(text) - shows the text                                          |
 | setError(flag) - pink face when true                                     |
 +--------------------------------------------------------------------------+
 */
@SuppressWarnings({"serial", "this-escape"})
public class ValueBox extends JLabel
{

    private boolean error = false;

    /*
     * ValueBox(): makes an empty, right-aligned, bold box.
     */
    public ValueBox()
    {
        super("", SwingConstants.RIGHT);
        setFont(Theme.font(Font.BOLD, 14f));
        setForeground(Theme.FIELD_TEXT);
        setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        setPreferredSize(new Dimension(96, 26));
        setMinimumSize(new Dimension(96, 26));
    }

    /*
     * setValue(text): shows a value in the box.
     * Passed in:  text - what to show
     * Passed out: nothing
     */
    public void setValue(String text)
    {
        setText(text);
    }

    /*
     * setError(flag): turns the pink "something is wrong here" face on or off.
     * Passed in:  flag - true for pink
     * Passed out: nothing
     */
    public void setError(boolean flag)
    {
        error = flag;
        repaint();
    }

    /*
     * paintComponent(graphics): draws the grey face with a sunken edge, then the text.
     * Passed in:  graphics - what to draw on
     * Passed out: nothing
     */
    @Override
    protected void paintComponent(Graphics graphics)
    {
        Graphics2D canvas = (Graphics2D) graphics.create();
        canvas.setColor(error ? Theme.FIELD_ERROR : Theme.FIELD_FACE);
        canvas.fillRect(0, 0, getWidth(), getHeight());
        canvas.setColor(new Color(110, 110, 110));
        canvas.drawLine(0, 0, getWidth() - 1, 0);
        canvas.drawLine(0, 0, 0, getHeight() - 1);
        canvas.setColor(Color.WHITE);
        canvas.drawLine(0, getHeight() - 1, getWidth() - 1, getHeight() - 1);
        canvas.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight() - 1);
        canvas.dispose();
        super.paintComponent(graphics);
    }
}

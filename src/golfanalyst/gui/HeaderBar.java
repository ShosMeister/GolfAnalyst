package golfanalyst.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GradientPaint;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;

/*
 +--------------------------------------------------------------------------+
 | HeaderBar                                                                |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | The top of the window, the same on every screen: one piece of wood with  |
 | the Home button on the left, the screen's title in a recessed field in   |
 | the middle, and the Help and Info buttons on the right. The wood is a    |
 | single stretched picture, so there are no joins in the grain.            |
 |                                                                          |
 | Public Methods:                                                          |
 | setTitle(text)                         - the words in the title field    |
 | homeButton(), helpButton(), infoButton() - the buttons, so the window    |
 |                                          can attach what they do         |
 +--------------------------------------------------------------------------+
 */
@SuppressWarnings({"serial", "this-escape"})
public final class HeaderBar extends JPanel
{

    private static final Color FRAME_BROWN = new Color(70, 35, 5);
    private final TitleField titleField = new TitleField();
    private final IconButton homeButton = new IconButton("Home.png", "Home", Theme.ICON_SIZE);
    private final IconButton helpButton = new IconButton("Help.png", "Help for this screen", Theme.ICON_SIZE);
    private final IconButton infoButton = new IconButton("Info.png", "Information about this screen", Theme.ICON_SIZE);

    /*
     * HeaderBar(): lays out the Home button, the title field and the Help and Info buttons.
     */
    public HeaderBar()
    {
        super(new BorderLayout(12, 0));
        setPreferredSize(new Dimension(100, Theme.HEADER_HEIGHT));
        setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        homeButton.setRing(true);
        helpButton.setRing(true);
        infoButton.setRing(true);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        right.setOpaque(false);
        right.add(helpButton);
        right.add(infoButton);
        add(homeButton, BorderLayout.WEST);
        add(titleField, BorderLayout.CENTER);
        add(right, BorderLayout.EAST);
    }

    /*
     * setTitle(text): shows the screen's title in the middle field.
     * Passed in:  text - the title
     * Passed out: nothing
     */
    public void setTitle(String text)
    {
        titleField.setText(text);
    }

    /*
     * title(): the words currently shown in the title field.
     */
    public String title()
    {
        return titleField.text();
    }

    /*
     * homeButton(): the Home button, so the window can attach its action.
     */
    public IconButton homeButton()
    {
        return homeButton;
    }

    /*
     * helpButton(): the Help button, so the window can attach its action.
     */
    public IconButton helpButton()
    {
        return helpButton;
    }

    /*
     * infoButton(): the Info button, so the window can attach its action.
     */
    public IconButton infoButton()
    {
        return infoButton;
    }

    /*
     * paintComponent(graphics): draws the one-piece wood banner with a raised edge and a shadow underneath.
     * Passed in:  graphics - what to draw on
     * Passed out: nothing
     */
    @Override
    protected void paintComponent(Graphics graphics)
    {
        Graphics2D canvas = (Graphics2D) graphics.create();
        Theme.smooth(canvas);
        Theme.paintStretched(canvas, Theme.image(Theme.HEADER_WOOD), getWidth(), getHeight(), Theme.WOOD_FALLBACK);
        canvas.setColor(new Color(255, 235, 190, 150));
        canvas.drawRect(2, 2, getWidth() - 5, getHeight() - 5);
        canvas.setColor(FRAME_BROWN);
        canvas.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
        canvas.drawRect(1, 1, getWidth() - 3, getHeight() - 3);
        canvas.dispose();
    }

    /*
     +--------------------------------------------------------------------------+
     | TitleField                                                               |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The recessed field in the middle of the header. It shows the title in    |
     | large bold italic white with a dark shadow, and makes the type smaller   |
     | if the title is too long to fit.                                         |
     +--------------------------------------------------------------------------+
     */
    @SuppressWarnings({"serial", "this-escape"})
    private static final class TitleField extends JComponent
    {
        private static final int MARGIN = 5;
        private static final int PADDING = 24;
        private String text = "";

        /*
         * text(): the title being shown.
         */
        String text()
        {
            return text;
        }

        /*
         * setText(newText): sets the title and redraws.
         */
        void setText(String newText)
        {
            text = newText;
            repaint();
        }

        /*
         * paintComponent(graphics): draws the recessed field and the title in it.
         */
        @Override
        protected void paintComponent(Graphics graphics)
        {
            Graphics2D canvas = (Graphics2D) graphics.create();
            Theme.smooth(canvas);
            int fieldHeight = getHeight() - 2 * MARGIN;
            paintRecess(canvas, getWidth(), fieldHeight);
            float size = 26f;
            canvas.setFont(Theme.font(Font.BOLD | Font.ITALIC, size));
            while (size > 14f && canvas.getFontMetrics().stringWidth(text) > getWidth() - PADDING)
            {
                size -= 1f;
                canvas.setFont(Theme.font(Font.BOLD | Font.ITALIC, size));
            }
            Theme.drawCentered(canvas, text, 0, MARGIN, getWidth(), fieldHeight, Color.WHITE, true);
            canvas.dispose();
        }

        /*
         +--------------------------------------------------------------------------+
         | paintRecess(canvas, width, height)                                       |
         +--------------------------------------------------------------------------+
         | Purpose:                                                                 |
         | Draws the field so it looks cut into the wood: a darker floor, a         |
         | soft shadow that falls from the top and left edges, a dark two-pixel     |
         | edge along the top and left (the lip casting the shadow), a light        |
         | two-pixel edge along the bottom and right (the far lip catching the      |
         | light), and a thin white line just inside as in the earlier versions.    |
         |                                                                          |
         | Passed in:  canvas - what to draw on                                     |
         |             width, height - the size of the field                        |
         | Passed out: nothing                                                      |
         +--------------------------------------------------------------------------+
         */
        private void paintRecess(Graphics2D canvas, int width, int height)
        {
            canvas.translate(0, MARGIN);
            canvas.setColor(new Color(70, 30, 0, 85));
            canvas.fillRect(0, 0, width, height);
            canvas.setPaint(new GradientPaint(0, 0, new Color(0, 0, 0, 140), 0, 14, new Color(0, 0, 0, 0)));
            canvas.fillRect(0, 0, width, 14);
            canvas.setPaint(new GradientPaint(0, 0, new Color(0, 0, 0, 110), 12, 0, new Color(0, 0, 0, 0)));
            canvas.fillRect(0, 0, 12, height);
            canvas.setColor(new Color(40, 15, 0, 235));
            canvas.fillRect(0, 0, width, 2);
            canvas.fillRect(0, 0, 2, height);
            canvas.setColor(new Color(255, 232, 185, 215));
            canvas.fillRect(0, height - 2, width, 2);
            canvas.fillRect(width - 2, 0, 2, height);
            canvas.setColor(new Color(255, 255, 255, 215));
            canvas.drawRect(4, 4, width - 9, height - 9);
            canvas.translate(0, -MARGIN);
        }
    }
}

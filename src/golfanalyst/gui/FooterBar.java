package golfanalyst.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/*
 +--------------------------------------------------------------------------+
 | FooterBar                                                                |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | The bottom of the window, the same on every screen: a strip of dark wood |
 | with the status message on the left and a gold progress bar on the right.|
 | The status line is where the program says what it is doing, what is wrong|
 | with something typed, or that a round was saved. If it is clicked, the   |
 | window can show a list of problems found in the data files.              |
 |                                                                          |
 | Public Methods:                                                          |
 | setStatus(text)             - the message on the left                    |
 | setProgress(fraction)       - fills the gold bar, 0.0 to 1.0             |
 | setStatusClickAction(action) - what happens when the message is clicked  |
 +--------------------------------------------------------------------------+
 */
@SuppressWarnings({"serial", "this-escape"})
public final class FooterBar extends JPanel
{

    private final JLabel statusLabel = new JLabel(" ");
    private final ProgressTrack progressTrack = new ProgressTrack();
    private transient Runnable statusClickAction = null;

    /*
     * FooterBar(): lays out the status message and the progress bar.
     */
    public FooterBar()
    {
        super(new BorderLayout(10, 0));
        setPreferredSize(new Dimension(100, Theme.FOOTER_HEIGHT));
        setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setFont(Theme.font(Font.PLAIN, 14f));
        statusLabel.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent event)
            {
                if (statusClickAction != null)
                {
                    statusClickAction.run();
                }
            }
        });
        add(statusLabel, BorderLayout.CENTER);
        progressTrack.setVisible(false);
        add(progressTrack, BorderLayout.EAST);
    }

    /*
     * setStatus(text): shows a message on the left of the footer.
     * Passed in:  text - the message
     * Passed out: nothing
     */
    public void setStatus(String text)
    {
        statusLabel.setText(text.isEmpty() ? " " : text);
    }

    /*
     * statusCursor(): the mouse pointer over the message (for the self-test).
     */
    Cursor statusCursor()
    {
        return statusLabel.getCursor();
    }

    /*
     * status(): the message currently shown on the left of the footer.
     */
    public String status()
    {
        return statusLabel.getText();
    }

    /*
     * setProgress(fraction): fills the gold bar.
     * Passed in:  fraction - 0.0 (empty) to 1.0 (full)
     * Passed out: nothing
     */
    public void setProgress(double fraction)
    {
        progressTrack.setFraction(fraction);
        progressTrack.setVisible(fraction >= 0.0 && fraction < 1.0);
    }

    /*
     * setStatusClickAction(action): sets what happens when the message is clicked; the pointer turns to a hand while there is an action.
     * Passed in:  action - code to run, or null for nothing
     * Passed out: nothing
     */
    public void setStatusClickAction(Runnable action)
    {
        statusClickAction = action;
        statusLabel.setCursor(Cursor.getPredefinedCursor(action == null ? Cursor.DEFAULT_CURSOR : Cursor.HAND_CURSOR));
    }

    /*
     * paintComponent(graphics): draws the dark wood strip in one piece.
     * Passed in:  graphics - what to draw on
     * Passed out: nothing
     */
    @Override
    protected void paintComponent(Graphics graphics)
    {
        Graphics2D canvas = (Graphics2D) graphics.create();
        Theme.smooth(canvas);
        Theme.paintStretched(canvas, Theme.image("TitleBkg_Teak.jpg"), getWidth(), getHeight(), Theme.WOOD_FALLBACK);
        canvas.setColor(new Color(0, 0, 0, 120));
        canvas.fillRect(0, 0, getWidth(), getHeight());
        canvas.setColor(new Color(255, 255, 255, 70));
        canvas.drawLine(0, 0, getWidth(), 0);
        canvas.dispose();
    }

    /*
     +--------------------------------------------------------------------------+
     | ProgressTrack                                                            |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The gold progress bar: a dark groove with the gold picture filling       |
     | from the left as far as the progress fraction.                           |
     +--------------------------------------------------------------------------+
     */
    @SuppressWarnings({"serial", "this-escape"})
    private static final class ProgressTrack extends JComponent
    {
        private static final int BAR_HEIGHT = 14;
        private double fraction = 0.0;

        /*
         * ProgressTrack(): sets the size of the groove.
         */
        ProgressTrack()
        {
            setPreferredSize(new Dimension(190, Theme.FOOTER_HEIGHT));
        }

        /*
         * setFraction(newFraction): sets how full the bar is (kept between 0 and 1) and redraws.
         */
        void setFraction(double newFraction)
        {
            fraction = Math.max(0.0, Math.min(1.0, newFraction));
            repaint();
        }

        /*
         * paintComponent(graphics): draws the groove and the gold fill.
         */
        @Override
        protected void paintComponent(Graphics graphics)
        {
            Graphics2D canvas = (Graphics2D) graphics.create();
            Theme.smooth(canvas);
            int top = (getHeight() - BAR_HEIGHT) / 2;
            canvas.setColor(new Color(0, 0, 0, 170));
            canvas.fillRect(0, top, getWidth(), BAR_HEIGHT);
            int filled = (int) (getWidth() * fraction);
            BufferedImage gold = Theme.image("ProgressGold.gif");
            if (filled > 0 && gold != null)
            {
                canvas.drawImage(gold, 0, top, filled, top + BAR_HEIGHT, 0, 0,
                        Math.max(1, (int) (gold.getWidth() * fraction)), gold.getHeight(), null);
            }
            else if (filled > 0)
            {
                canvas.setColor(Theme.GOLD);
                canvas.fillRect(0, top, filled, BAR_HEIGHT);
            }
            canvas.setColor(new Color(255, 255, 255, 80));
            canvas.drawRect(0, top, getWidth() - 1, BAR_HEIGHT - 1);
            canvas.dispose();
        }
    }
}

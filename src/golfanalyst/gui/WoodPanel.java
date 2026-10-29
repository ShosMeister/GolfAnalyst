package golfanalyst.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/*
 +--------------------------------------------------------------------------+
 | WoodPanel                                                                |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | A titled panel in the style of the Home screen: a light wood title bar   |
 | with the title in gold italic, over a light grey body holding rows of    |
 | "label ... value box". Used for the Handicap and Golfer's Stats panels.  |
 |                                                                          |
 | Public Methods:                                                          |
 | addRow(labelText, field) - adds a label and its value box as the next row|
 +--------------------------------------------------------------------------+
 */
@SuppressWarnings({"serial", "this-escape"})
public class WoodPanel extends JPanel
{

    private static final int TITLE_HEIGHT = 32;
    private final JPanel body = new JPanel(new GridBagLayout());
    private int nextRow = 0;

    /*
     * WoodPanel(title): makes the panel with its title bar and an empty body.
     * Passed in:  title - the words in the title bar
     * Passed out: the panel
     */
    public WoodPanel(String title)
    {
        super(new BorderLayout());
        setOpaque(false);
        add(new TitleBar(title), BorderLayout.NORTH);
        body.setBackground(Theme.PANEL_BODY);
        body.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.FRAME_DARK, 1),
                BorderFactory.createEmptyBorder(8, 10, 10, 10)));
        add(body, BorderLayout.CENTER);
    }

    /*
     * addRow(labelText, field): adds one row: a right-aligned label and the box that shows its value.
     * Passed in:  labelText - the words, field - the box or control
     * Passed out: nothing
     */
    public void addRow(String labelText, JComponent field)
    {
        GridBagConstraints at = new GridBagConstraints();
        at.gridy = nextRow;
        nextRow = nextRow + 1;
        at.insets = new Insets(4, 2, 4, 2);
        at.weighty = 1.0;
        at.gridx = 0;
        at.weightx = 1.0;
        at.anchor = GridBagConstraints.EAST;
        JLabel label = new JLabel(labelText, SwingConstants.RIGHT);
        label.setFont(Theme.font(Font.PLAIN, 14f));
        body.add(label, at);
        at.gridx = 1;
        at.weightx = 0.0;
        at.anchor = GridBagConstraints.WEST;
        body.add(field, at);
    }

    /*
     +--------------------------------------------------------------------------+
     | TitleBar                                                                 |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The light wood strip across the top of a WoodPanel, with the title       |
     | in gold bold italic centred on it.                                       |
     +--------------------------------------------------------------------------+
     */
    @SuppressWarnings({"serial", "this-escape"})
    private static final class TitleBar extends JComponent
    {
        private final String title;

        TitleBar(String title)
        {
            this.title = title;
            setPreferredSize(new Dimension(100, TITLE_HEIGHT));
        }

        /*
         * paintComponent(graphics): draws the wood strip, a raised edge, and the title.
         */
        @Override
        protected void paintComponent(Graphics graphics)
        {
            Graphics2D canvas = (Graphics2D) graphics.create();
            Theme.smooth(canvas);
            Theme.paintStretched(canvas, Theme.image("TitleBkg_Teak.jpg"), getWidth(), getHeight(), Theme.WOOD_FALLBACK);
            Theme.paintBevel(canvas, getWidth(), getHeight(), true);
            canvas.setFont(Theme.font(Font.BOLD | Font.ITALIC, 18f));
            Theme.drawCentered(canvas, title, 0, 0, getWidth(), getHeight(), Theme.GOLD, true);
            canvas.dispose();
        }
    }
}

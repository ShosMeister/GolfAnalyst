package golfanalyst.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/*
 +--------------------------------------------------------------------------+
 | HelpWindow                                                               |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | Shows an Info or Help text in its own small window. The text comes from  |
 | a plain text file in the help folder, so it can be edited with any text  |
 | editor and nothing has to be rebuilt. The first line of the file is the  |
 | title, shown in a band of light wood across the top; the rest is the     |
 | body, on parchment, with section headings, bold labels and bold step     |
 | titles worked out by HelpFormatter from the plain text ("# " in front of |
 | a line makes it a heading). The text wraps to the window's width and     |
 | cannot be changed.                                                       |
 |                                                                          |
 | The window is modal: while it is open the main window takes no clicks    |
 | or typing, so the help always belongs to the screen it was opened from.  |
 | It can still be moved and scrolled; Close (or the X) gives the main      |
 | window back.                                                             |
 |                                                                          |
 | File names are the screen's help key plus "-info" or "-help", for        |
 | example home-info.txt and home-help.txt.                                 |
 |                                                                          |
 | Public Methods:                                                          |
 | show(owner, helpDir, fileKey, fallbackTitle) - opens the window          |
 +--------------------------------------------------------------------------+
 */
@SuppressWarnings({"serial", "this-escape"})
public final class HelpWindow extends JDialog
{

    private static final int WINDOW_WIDTH = 680;
    private static final int WINDOW_HEIGHT = 580;
    private static final int BAND_HEIGHT = 46;
    private static final int BAND_LINE = 3;
    private static final int SIDE_PADDING = 20;
    private static final float TEXT_SIZE = 15f;
    private static final float HEADING_SIZE = 18f;
    private static final float TITLE_SIZE = 22f;
    private static final float HEADING_SPACE_ABOVE = 16f;
    private static final float HEADING_SPACE_BELOW = 4f;
    private static final float PARAGRAPH_SPACE_BELOW = 9f;

    /*
     +--------------------------------------------------------------------------+
     | HelpWindow(owner, title, pieces)                                         |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Builds the modal window: the wood title band, the styled text in a       |
     | scrolling, read-only pane on parchment, and a Close button underneath.   |
     |                                                                          |
     | Passed in:  owner  - the main window it opens over                       |
     |             title  - the words in the band and the title bar             |
     |             pieces - the styled text (see HelpFormatter)                 |
     | Passed out: the window (not yet visible)                                 |
     +--------------------------------------------------------------------------+
     */
    private HelpWindow(Frame owner, String title, List<HelpFormatter.Piece> pieces)
    {
        super(owner, title, true);
        JTextPane text = new JTextPane();
        text.setEditable(false);
        text.setBackground(Theme.HELP_PAPER);
        text.setBorder(BorderFactory.createEmptyBorder(12, SIDE_PADDING, 12, SIDE_PADDING));
        fill(text.getStyledDocument(), pieces);
        text.setCaretPosition(0);
        JScrollPane scroll = new JScrollPane(text);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Theme.HELP_PAPER);
        WoodButton close = new WoodButton("Close");
        close.addActionListener(event -> dispose());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 8));
        bottom.setBackground(Theme.HELP_PAPER);
        bottom.add(close);
        getContentPane().setBackground(Theme.HELP_PAPER);
        add(new TitleBand(title), BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
        setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
        pack();
        setLocationRelativeTo(owner);
    }

    /*
     * characterStyle(style): the font, size and colour of a piece: headings 18 pt bold, underlined, dark brown; the rest 15 pt black, bold or plain.
     * Passed in:  style - the piece's style
     * Passed out: the attributes
     */
    static SimpleAttributeSet characterStyle(HelpFormatter.Style style)
    {
        SimpleAttributeSet attributes = new SimpleAttributeSet();
        boolean heading = style == HelpFormatter.Style.HEADING;
        StyleConstants.setFontFamily(attributes, Theme.font(Font.PLAIN, TEXT_SIZE).getFamily());
        StyleConstants.setFontSize(attributes, Math.round(heading ? HEADING_SIZE : TEXT_SIZE));
        StyleConstants.setBold(attributes, style != HelpFormatter.Style.PLAIN);
        StyleConstants.setUnderline(attributes, heading);
        StyleConstants.setForeground(attributes, heading ? Theme.HELP_HEADING : Color.BLACK);
        return attributes;
    }

    /*
     * paragraphStyle(style): the space round a paragraph: 16 px above and 4 below a heading, 9 px below anything else.
     * Passed in:  style - the style of the paragraph's last piece
     * Passed out: the attributes
     */
    static SimpleAttributeSet paragraphStyle(HelpFormatter.Style style)
    {
        SimpleAttributeSet attributes = new SimpleAttributeSet();
        boolean heading = style == HelpFormatter.Style.HEADING;
        StyleConstants.setSpaceAbove(attributes, heading ? HEADING_SPACE_ABOVE : 0f);
        StyleConstants.setSpaceBelow(attributes, heading ? HEADING_SPACE_BELOW : PARAGRAPH_SPACE_BELOW);
        return attributes;
    }

    /*
     +--------------------------------------------------------------------------+
     | fill(document, pieces)                                                   |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Puts the styled pieces into a text document, one paragraph after         |
     | another, with the spacing for each kind of paragraph. It needs no        |
     | window, so the self-test can check it.                                   |
     |                                                                          |
     | Passed in:  document - an empty styled document                          |
     |             pieces   - the styled text (see HelpFormatter)               |
     | Passed out: nothing (the document is filled)                             |
     +--------------------------------------------------------------------------+
     */
    static void fill(StyledDocument document, List<HelpFormatter.Piece> pieces)
    {
        int paragraphStart = document.getLength();
        try
        {
            for (HelpFormatter.Piece piece : pieces)
            {
                document.insertString(document.getLength(), piece.text(), characterStyle(piece.style()));
                if (piece.lastInParagraph())
                {
                    document.insertString(document.getLength(), "\n", characterStyle(piece.style()));
                    document.setParagraphAttributes(paragraphStart, document.getLength() - paragraphStart,
                            paragraphStyle(piece.style()), false);
                    paragraphStart = document.getLength();
                }
            }
        }
        catch (BadLocationException impossible)
        {
            // text is only ever added at the end, which is always a valid place
        }
    }

    /*
     +--------------------------------------------------------------------------+
     | TitleBand                                                                |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The band across the top of the window: the header's light wood           |
     | stretched in one piece, the title in 22 pt bold italic white with a      |
     | dark shadow, and a 3 px dark brown line underneath.                      |
     +--------------------------------------------------------------------------+
     */
    private static final class TitleBand extends JComponent
    {
        private final String title;

        /*
         * TitleBand(title): remembers the title and sets the band's height.
         */
        TitleBand(String title)
        {
            this.title = title;
            setPreferredSize(new Dimension(WINDOW_WIDTH, BAND_HEIGHT));
        }

        /*
         * paintComponent(graphics): draws the wood, the title and the line under it.
         */
        @Override
        protected void paintComponent(Graphics graphics)
        {
            Graphics2D canvas = (Graphics2D) graphics.create();
            Theme.smooth(canvas);
            Theme.paintStretched(canvas, Theme.image(Theme.HEADER_WOOD), getWidth(), getHeight(), Theme.WOOD_FALLBACK);
            canvas.setFont(Theme.font(Font.BOLD | Font.ITALIC, TITLE_SIZE));
            Theme.drawCentered(canvas, title, 0, 0, getWidth(), getHeight() - BAND_LINE, Color.WHITE, true);
            canvas.setColor(Theme.FRAME_DARK);
            canvas.fillRect(0, getHeight() - BAND_LINE, getWidth(), BAND_LINE);
            canvas.dispose();
        }
    }

    /*
     +--------------------------------------------------------------------------+
     | show(owner, helpDir, fileKey, fallbackTitle)                             |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Reads a help file and opens a window showing it. If the file is          |
     | missing or cannot be read the window says so, and says which file it     |
     | was looking for, instead of failing. The window is modal, so this only   |
     | returns once it has been closed.                                         |
     |                                                                          |
     | Passed in:  owner         - the main window                              |
     |             helpDir       - the folder holding the help files            |
     |             fileKey       - the file name without ".txt", "home-info"    |
     |             fallbackTitle - the window title to use if the file has none |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    public static void show(Frame owner, Path helpDir, String fileKey, String fallbackTitle)
    {
        Path file = helpDir.resolve(fileKey + ".txt");
        String title = fallbackTitle;
        List<HelpFormatter.Piece> pieces;
        try
        {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            title = HelpFormatter.title(content).isEmpty() ? fallbackTitle : HelpFormatter.title(content);
            pieces = HelpFormatter.pieces(content);
        }
        catch (IOException missing)
        {
            pieces = List.of(new HelpFormatter.Piece("The help file " + file.toAbsolutePath() + " could not be read.",
                    HelpFormatter.Style.PLAIN, true));
        }
        new HelpWindow(owner, title, pieces).setVisible(true);
    }
}

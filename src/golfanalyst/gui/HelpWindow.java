package golfanalyst.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/*
 +--------------------------------------------------------------------------+
 | HelpWindow                                                               |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | Shows an Info or Help text in its own small window. The text comes from  |
 | a plain text file in the help folder, so it can be edited with any text  |
 | editor and nothing has to be rebuilt. The first line of the file is the  |
 | window's title and the rest is the body; paragraphs are separated by a   |
 | blank line and wrap to the window's width.                               |
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

    /*
     +--------------------------------------------------------------------------+
     | HelpWindow(owner, title, body)                                           |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Builds the window: the body text in a scrolling, read-only area and a    |
     | Close button underneath.                                                 |
     |                                                                          |
     | Passed in:  owner - the main window it opens over                        |
     |             title - the words in the title bar                           |
     |             body  - the text to show                                     |
     | Passed out: the window (not yet visible)                                 |
     +--------------------------------------------------------------------------+
     */
    private HelpWindow(Frame owner, String title, String body)
    {
        super(owner, title, false);
        JTextArea text = new JTextArea(body);
        text.setEditable(false);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setFont(Theme.font(Font.PLAIN, 15f));
        text.setBackground(Theme.PANEL_BODY);
        text.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        text.setCaretPosition(0);
        WoodButton close = new WoodButton("Close");
        close.addActionListener(event -> dispose());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 8));
        bottom.setBackground(Theme.PANEL_BODY);
        bottom.add(close);
        add(new JScrollPane(text), BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
        setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
        pack();
        setLocationRelativeTo(owner);
    }

    /*
     +--------------------------------------------------------------------------+
     | show(owner, helpDir, fileKey, fallbackTitle)                             |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Reads a help file and opens a window showing it. If the file is          |
     | missing or cannot be read the window says so, and says which file it     |
     | was looking for, instead of failing.                                     |
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
        String body;
        try
        {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            String[] lines = content.split("\\R", 2);
            title = lines[0].isBlank() ? fallbackTitle : lines[0].trim();
            body = lines.length > 1 ? lines[1].strip() : "";
        }
        catch (IOException missing)
        {
            body = "The help file " + file.toAbsolutePath() + " could not be read.";
        }
        new HelpWindow(owner, title, body).setVisible(true);
    }
}

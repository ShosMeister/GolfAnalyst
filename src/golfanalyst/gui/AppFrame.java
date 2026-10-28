package golfanalyst.gui;

import golfanalyst.DataStore;
import java.awt.Dimension;
import java.awt.Image;
import java.nio.file.Path;
import javax.swing.JFrame;

/*
 +--------------------------------------------------------------------------+
 | AppFrame                                                                 |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | The program's one window. It does nothing but be a window: it holds an   |
 | AppPanel (which has all the pages, the fixed header and footer, and the  |
 | navigation) and sets the window's title, icon, size and where it opens   |
 | on the screen. Closing the window ends the program.                      |
 +--------------------------------------------------------------------------+
 */
@SuppressWarnings({"serial", "this-escape"})
public final class AppFrame extends JFrame
{

    private static final int WINDOW_WIDTH = 960;
    private static final int WINDOW_HEIGHT = 770;
    private static final int MIN_WIDTH = 940;
    private static final int MIN_HEIGHT = 730;

    /*
     +--------------------------------------------------------------------------+
     | AppFrame(store, helpDir, startNote)                                      |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Builds the window around a new AppPanel.                                 |
     |                                                                          |
     | Passed in:  store     - the loaded data                                  |
     |             helpDir   - the folder with the help text files              |
     |             startNote - a note for the footer at start-up (or "")        |
     | Passed out: the window (not yet visible)                                 |
     +--------------------------------------------------------------------------+
     */
    public AppFrame(DataStore store, Path helpDir, String startNote)
    {
        super(Theme.VERSION_TEXT);
        AppPanel panel = new AppPanel(store, helpDir, startNote);
        setContentPane(panel);
        setTitle(panel.windowTitle());
        Image icon = Theme.image("Home.png");
        if (icon != null)
        {
            setIconImage(icon);
        }
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setMinimumSize(new Dimension(MIN_WIDTH, MIN_HEIGHT));
        setLocationRelativeTo(null);
    }
}

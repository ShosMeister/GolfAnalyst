package golfanalyst.gui;

import golfanalyst.DataStore;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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

    static final int WINDOW_WIDTH = 960;
    static final int WINDOW_HEIGHT = 770;
    private static final int MIN_WIDTH = 940;
    private static final int MIN_HEIGHT = 730;

    /*
     +--------------------------------------------------------------------------+
     | AppFrame(store, helpDir, startNote)                                      |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Builds the window around a new AppPanel. The close box quits only if     |
     | the page on show agrees (AppPanel.canQuit asks "Discard your changes?"   |
     | when something typed is not saved).                                      |
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
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent event)
            {
                if (panel.canQuit())
                {
                    dispose();
                    System.exit(0);
                }
            }
        });
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setMinimumSize(new Dimension(MIN_WIDTH, MIN_HEIGHT));
        setLocationRelativeTo(null);
    }
}

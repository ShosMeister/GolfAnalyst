package golfanalyst.gui;

import golfanalyst.DataStore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/*
 +--------------------------------------------------------------------------+
 | App                                                                      |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | Starts the program with its window. In NetBeans set this as the main     |
 | class (golfanalyst.gui.App) and press Run.                               |
 |                                                                          |
 | Where its folders are (ProgramFolders): from the project, all in the     |
 | working folder; installed (-Dgolfanalyst.home=the program's folder,      |
 | set by the installer's launcher and by run.bat/run.sh), the program's    |
 | own files in that folder, the golfer's data in Documents\Golf Analyst    |
 | and golfanalyst.location in the user's settings folder.                  |
 |                                                                          |
 | Which data folder it opens:                                              |
 |   1. the folder given as the first argument, if there is one;            |
 |   2. otherwise the folder named in golfanalyst.location (set by          |
 |      Settings, Change..., and by Create my own data folder when          |
 |      installed), if it is there;                                         |
 |   3. otherwise the golfer's own data folder ("data" next to the          |
 |      program, or Documents\Golf Analyst when installed);                 |
 |   4. otherwise the "sample-data" folder (the title and footer say so),   |
 |      so the first run has something to show;                             |
 |   5. otherwise a new empty own data folder with just the file headers.   |
 |                                                                          |
 | Other folders it looks for (in the program's folder, or set with         |
 | -Dgolfanalyst.images=... and -Dgolfanalyst.help=...):                    |
 |   images - the wood buttons, textures and background photos              |
 |   help   - the Info and Help text files                                  |
 |                                                                          |
 | Run with the argument --selftest to check the screens without opening    |
 | a window.                                                                |
 |                                                                          |
 | Public Methods:                                                          |
 | main(args) - program entry point                                         |
 +--------------------------------------------------------------------------+
 */
public final class App
{

    private static final String DATA_FOLDER = DataStore.DATA_FOLDER;
    private static final String SAMPLE_FOLDER = DataStore.SAMPLE_FOLDER;

    private App()
    {
    }

    /*
     +--------------------------------------------------------------------------+
     | main(args)                                                               |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Opens the window, or runs the screen self-test if asked.                 |
     |                                                                          |
     | Passed in:  args - optional: a data folder, or --selftest                |
     | Passed out: nothing (the exit code is 1 if --selftest fails)             |
     +--------------------------------------------------------------------------+
     */
    public static void main(String[] args)
    {
        if (args.length > 0 && args[0].equals("--selftest"))
        {
            System.exit(GuiSelfTest.run() ? 0 : 1);
        }
        SwingUtilities.invokeLater(() -> start(args));
    }

    /*
     +--------------------------------------------------------------------------+
     | start(args)                                                              |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Chooses the data folder, loads it and opens the window. If the data      |
     | cannot be read a message says why and the program closes.                |
     |                                                                          |
     | Passed in:  args - the command-line arguments                            |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    private static void start(String[] args)
    {
        try
        {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (Exception unavailable)
        {
            // the standard Java look is used instead
        }
        golfanalyst.ProgramFolders folders = golfanalyst.ProgramFolders.fromSystem();
        golfanalyst.ProgramFolders.setActive(folders);
        Theme.load(Path.of(System.getProperty("golfanalyst.images", folders.programFile("images").toString())));
        Path helpDir = Path.of(System.getProperty("golfanalyst.help", folders.programFile("help").toString()));
        String note = "";
        Path dataDir;
        if (args.length > 0)
        {
            dataDir = Path.of(args[0]);
        }
        else
        {
            dataDir = folders.startFolder();
        }
        if (dataDir == null)
        {
            dataDir = folders.ownDataFolder();
            createEmptyData(dataDir);
            note = "A new empty data folder was created.";
        }
        try
        {
            DataStore store = new DataStore(dataDir);
            new AppFrame(store, helpDir, note).setVisible(true);
        }
        catch (IOException failure)
        {
            ConfirmForm.notice("The data folder could not be read:\n" + dataDir.toAbsolutePath() + "\n\n" + failure.getMessage(), null)
                    .withTitle(AppPanel.PROBLEMS_TITLE).showOn(null);
            System.exit(1);
        }
    }

    /*
     * createEmptyData(dir): makes a data folder holding courses.tsv and rounds.tsv with just their header rows.
     * Passed in:  dir - the folder to create
     * Passed out: nothing (files are written; a failure is left for the loader to report)
     */
    private static void createEmptyData(Path dir)
    {
        try
        {
            DataStore.createEmptyFolder(dir);
        }
        catch (IOException failure)
        {
            // the loader reports the missing files when it opens the folder
        }
    }
}

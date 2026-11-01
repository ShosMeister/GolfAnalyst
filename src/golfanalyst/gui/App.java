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
 | Which data folder it opens:                                              |
 |   1. the folder given as the first argument, if there is one;            |
 |   2. otherwise the folder named in golfanalyst.location next to the      |
 |      program (set by Settings, Change...), if it is there;               |
 |   3. otherwise the folder named "data" next to the program;              |
 |   4. otherwise, if there is no "data" folder, the "sample-data" folder   |
 |      (the title and footer say so), so the first run has something to    |
 |      show;                                                               |
 |   5. otherwise a new empty "data" folder with just the file headers.     |
 |                                                                          |
 | Other folders it looks for (next to the program, or set with             |
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
        Theme.load(Path.of(System.getProperty("golfanalyst.images", "images")));
        Path helpDir = Path.of(System.getProperty("golfanalyst.help", "help"));
        String note = "";
        Path dataDir;
        Path chosen = golfanalyst.DataLocation.read(Path.of(""));
        if (args.length > 0)
        {
            dataDir = Path.of(args[0]);
        }
        else if (chosen != null)
        {
            dataDir = chosen;
        }
        else if (Files.isDirectory(Path.of(DATA_FOLDER)))
        {
            dataDir = Path.of(DATA_FOLDER);
        }
        else if (Files.isDirectory(Path.of(SAMPLE_FOLDER)))
        {
            dataDir = Path.of(SAMPLE_FOLDER);
        }
        else
        {
            dataDir = Path.of(DATA_FOLDER);
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

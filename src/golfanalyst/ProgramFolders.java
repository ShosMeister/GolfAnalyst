package golfanalyst;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/*
 +--------------------------------------------------------------------------+
 | ProgramFolders                                                           |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | Where the program's own files are and where it may write, in its two     |
 | ways of running:                                                         |
 |                                                                          |
 | From the project (NetBeans, app.bat): everything is in the working       |
 | folder as it always was: images, help, sample-data, zipcodes.tsv,        |
 | golfanalyst.location and the golfer's own data folder, data.             |
 |                                                                          |
 | Installed (the installer's launcher, or run.bat/run.sh from the zip,     |
 | sets -Dgolfanalyst.home to the program's folder): images, help,          |
 | sample-data and zipcodes.tsv are read from that folder, which ordinary   |
 | users may not be able to write to, so NOTHING is ever written there.     |
 | The golfer's own data folder is Documents\Golf Analyst (the real         |
 | Documents folder, wherever Windows keeps it, OneDrive too);              |
 | golfanalyst.location and the sample data's settings.txt live in the      |
 | user's own settings folder: %APPDATA%\Golf Analyst on Windows,           |
 | ~/Library/Application Support/Golf Analyst on a Mac and                  |
 | ~/.config/Golf Analyst elsewhere.                                        |
 |                                                                          |
 | The folders in use are set once at start-up (setActive); every other     |
 | part of the program asks active(). Tests make their own with project()   |
 | and installed() and fake folders.                                        |
 |                                                                          |
 | Public Methods:                                                          |
 | project(programDir), installed(programDir, userDir, documents),          |
 | fromSystem() - the three ways to make one                                |
 | active(), setActive(folders) - the folders the program is using          |
 | isInstalled(), programDir(), programFile(name), locationFolder(),        |
 | sampleFolder(), ownDataFolder(), documentsFolder(),                      |
 | settingsFolder(dataDir), startFolder()                                   |
 +--------------------------------------------------------------------------+
 */
public final class ProgramFolders
{

    public static final String HOME_PROPERTY = "golfanalyst.home";
    public static final String FOLDER_NAME = "Golf Analyst";
    static final long LOOKUP_SECONDS = 10;
    private static ProgramFolders active = project(Path.of(""));

    private final boolean installed;
    private final Path programDir;
    private final Path userDir;
    private final Supplier<Path> documentsLookup;
    private Path documents = null;

    /*
     * ProgramFolders(installed, programDir, userDir, documentsLookup): see the factory methods.
     */
    private ProgramFolders(boolean installed, Path programDir, Path userDir, Supplier<Path> documentsLookup)
    {
        this.installed = installed;
        this.programDir = programDir;
        this.userDir = userDir;
        this.documentsLookup = documentsLookup;
    }

    /*
     * project(programDir): running from the project: everything in programDir, as before (Documents is the usual guess).
     */
    public static ProgramFolders project(Path programDir)
    {
        return new ProgramFolders(false, programDir, programDir, () -> Path.of(System.getProperty("user.home"), "Documents"));
    }

    /*
     +--------------------------------------------------------------------------+
     | installed(programDir, userDir, documents)                                |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Running installed: the program's files in programDir (never written      |
     | to), the location file and the sample settings in userDir, the own data  |
     | folder in Documents.                                                     |
     |                                                                          |
     | Passed in:  programDir - the program's own folder                        |
     |             userDir    - the user's settings folder for Golf Analyst     |
     |             documents  - finds the Documents folder (asked only when     |
     |                          needed, once)                                   |
     | Passed out: the folders                                                  |
     +--------------------------------------------------------------------------+
     */
    public static ProgramFolders installed(Path programDir, Path userDir, Supplier<Path> documents)
    {
        return new ProgramFolders(true, programDir, userDir, documents);
    }

    /*
     * fromSystem(): installed when -Dgolfanalyst.home is set (the launcher sets it), with the system's real folders; otherwise the project's working folder.
     */
    public static ProgramFolders fromSystem()
    {
        String home = System.getProperty(HOME_PROPERTY, "").trim();
        if (home.isEmpty())
        {
            return project(Path.of(""));
        }
        return installed(Path.of(home), systemUserFolder(), () -> systemDocuments());
    }

    /*
     * active(): the folders the program is using (the project's working folder until setActive is called).
     */
    public static synchronized ProgramFolders active()
    {
        return active;
    }

    /*
     * setActive(folders): the folders to use from now on (App at start-up; the self-test, which puts the old ones back).
     */
    public static synchronized void setActive(ProgramFolders folders)
    {
        active = folders;
    }

    /*
     * isInstalled(): whether the program runs installed (then it never writes inside its own folder).
     */
    public boolean isInstalled()
    {
        return installed;
    }

    /*
     * programDir(): the program's own folder (images, help, sample-data, zipcodes.tsv).
     */
    public Path programDir()
    {
        return programDir;
    }

    /*
     * programFile(name): one of the program's own files or folders, such as "images" or "zipcodes.tsv".
     */
    public Path programFile(String name)
    {
        return programDir.resolve(name);
    }

    /*
     * locationFolder(): where golfanalyst.location is kept: the program's folder from the project, the user's settings folder when installed.
     */
    public Path locationFolder()
    {
        return userDir;
    }

    /*
     * sampleFolder(): the sample data that comes with the program.
     */
    public Path sampleFolder()
    {
        return programFile(DataStore.SAMPLE_FOLDER);
    }

    /*
     * ownDataFolder(): where the golfer's own data folder belongs: data in the program's folder from the project, Documents\Golf Analyst when installed.
     */
    public Path ownDataFolder()
    {
        if (installed)
        {
            return documentsFolder().resolve(FOLDER_NAME);
        }
        return programFile(DataStore.DATA_FOLDER);
    }

    /*
     * documentsFolder(): the Documents folder, looked up the first time it is asked for and then remembered.
     */
    public synchronized Path documentsFolder()
    {
        if (documents == null)
        {
            documents = documentsLookup.get();
        }
        return documents;
    }

    /*
     +--------------------------------------------------------------------------+
     | settingsFolder(dataDir)                                                  |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Where a data folder's settings.txt is kept: in the data folder itself,   |
     | except for the installed program's own sample data, whose settings (the  |
     | name and Include historical rounds) go into sample-data in the user's    |
     | settings folder so that nothing is written in the program's folder.      |
     |                                                                          |
     | Passed in:  dataDir - the data folder                                    |
     | Passed out: the folder for its settings.txt                              |
     +--------------------------------------------------------------------------+
     */
    public Path settingsFolder(Path dataDir)
    {
        if (installed && sameFolder(dataDir, sampleFolder()))
        {
            return userDir.resolve(DataStore.SAMPLE_FOLDER);
        }
        return dataDir;
    }

    /*
     +--------------------------------------------------------------------------+
     | startFolder()                                                            |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Which data folder to open when none was given: the one named in          |
     | golfanalyst.location if it is there, else the golfer's own data folder   |
     | if it is there, else the sample data if it is there.                     |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: the folder, or null when there is none of them (the caller   |
     |             then makes an empty own data folder)                         |
     +--------------------------------------------------------------------------+
     */
    public Path startFolder()
    {
        Path chosen = DataLocation.read(locationFolder());
        if (chosen != null)
        {
            return chosen;
        }
        if (Files.isDirectory(ownDataFolder()))
        {
            return ownDataFolder();
        }
        if (Files.isDirectory(sampleFolder()))
        {
            return sampleFolder();
        }
        return null;
    }

    /*
     * sameFolder(first, second): whether two paths name the same folder (compared as full paths).
     */
    static boolean sameFolder(Path first, Path second)
    {
        return first.toAbsolutePath().normalize().equals(second.toAbsolutePath().normalize());
    }

    /*
     * isWindows(): whether the program runs on Windows.
     */
    static boolean isWindows()
    {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("windows");
    }

    /*
     +--------------------------------------------------------------------------+
     | systemUserFolder()                                                       |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The user's own settings folder for Golf Analyst: %APPDATA%\Golf Analyst  |
     | on Windows, ~/Library/Application Support/Golf Analyst on a Mac,         |
     | $XDG_CONFIG_HOME/Golf Analyst (or ~/.config/Golf Analyst) elsewhere.     |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: the folder (it may not exist yet)                            |
     +--------------------------------------------------------------------------+
     */
    static Path systemUserFolder()
    {
        String home = System.getProperty("user.home");
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (isWindows())
        {
            String appData = System.getenv("APPDATA");
            if (appData != null && !appData.isBlank())
            {
                return Path.of(appData, FOLDER_NAME);
            }
            return Path.of(home, "AppData", "Roaming", FOLDER_NAME);
        }
        if (os.contains("mac"))
        {
            return Path.of(home, "Library", "Application Support", FOLDER_NAME);
        }
        String config = System.getenv("XDG_CONFIG_HOME");
        if (config != null && !config.isBlank())
        {
            return Path.of(config, FOLDER_NAME);
        }
        return Path.of(home, ".config", FOLDER_NAME);
    }

    /*
     +--------------------------------------------------------------------------+
     | systemDocuments()                                                        |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The real Documents folder. On Windows it asks Windows itself (the        |
     | known-folder lookup behind [Environment]::GetFolderPath, run through     |
     | PowerShell, since Java 17 cannot call Windows directly), so a Documents  |
     | folder moved to OneDrive or another drive is found. If that does not     |
     | answer, and on other systems, it is Documents in the home folder.        |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: the folder                                                   |
     +--------------------------------------------------------------------------+
     */
    static Path systemDocuments()
    {
        Path guess = Path.of(System.getProperty("user.home"), "Documents");
        if (!isWindows())
        {
            return guess;
        }
        try
        {
            Process lookup = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-Command",
                    "[Console]::OutputEncoding = [Text.Encoding]::UTF8; [Environment]::GetFolderPath('MyDocuments')")
                    .redirectErrorStream(true).start();
            lookup.getOutputStream().close();
            String answer;
            try (InputStream output = lookup.getInputStream())
            {
                answer = new String(output.readAllBytes(), StandardCharsets.UTF_8).trim();
            }
            if (!lookup.waitFor(LOOKUP_SECONDS, TimeUnit.SECONDS))
            {
                lookup.destroy();
                return guess;
            }
            return documentsFrom(answer, guess);
        }
        catch (IOException | RuntimeException failed)
        {
            return guess;
        }
        catch (InterruptedException stopped)
        {
            Thread.currentThread().interrupt();
            return guess;
        }
    }

    /*
     * documentsFrom(answer, guess): the folder in the lookup's answer (its last line, a byte order mark dropped) if it is a folder that exists, otherwise the guess.
     */
    static Path documentsFrom(String answer, Path guess)
    {
        String text = answer.replace(String.valueOf(LineReplacer.BYTE_ORDER_MARK), "").trim();
        String[] lines = text.split("\\R");
        String last = lines[lines.length - 1].trim();
        if (last.isEmpty())
        {
            return guess;
        }
        try
        {
            Path folder = Path.of(last);
            if (folder.isAbsolute() && Files.isDirectory(folder))
            {
                return folder;
            }
        }
        catch (RuntimeException notAPath)
        {
            // the guess is used
        }
        return guess;
    }
}

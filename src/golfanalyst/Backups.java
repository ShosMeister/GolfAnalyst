package golfanalyst;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/*
 +--------------------------------------------------------------------------+
 | Backups                                                                  |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | The one backup routine. Before any save that changes or removes          |
 | existing lines, the files about to change are copied into a folder of    |
 | their own in the data folder's backups folder, named with the date, the  |
 | time and the reason, for example backups\2026-11-01_143012_edit-round.   |
 | The copies keep their REAL names (rounds.tsv, courses.tsv, settings.txt) |
 | so putting one back is just copying it over the file in the data folder. |
 | Two backups in the same second get -2, -3 ... after the time. Adding a   |
 | round or new tees only adds lines, so it makes no backup.                |
 |                                                                          |
 | Reasons: edit-round, delete-round and edit-course are the "everyday"     |
 | kinds: only the newest BackupsToKeep folders of EACH kind are kept (a    |
 | setting; All keeps everything). import, clear and delete-course folders  |
 | are never deleted by the program. Backups made before this layout (the   |
 | dated rounds-... and courses-... files, the clear-..., import-... and    |
 | delete-course-... folders) do not match the new names, so they are       |
 | never counted and never touched.                                         |
 |                                                                          |
 | If the BackupCopyFolder setting holds a folder, each new backup folder   |
 | is also copied there under the same name. That is an extra: if it fails  |
 | (a USB stick not plugged in, say) the save still goes ahead and          |
 | takeWarning() gives a one-line warning for the footer. If the normal     |
 | backup cannot be made, Failed is thrown before anything is written, so   |
 | nothing changes.                                                         |
 |                                                                          |
 | Constants:                                                               |
 | FOLDER - the backups folder's name ("backups")                           |
 | EDIT_ROUND ... CLEAR - the reasons                                       |
 |                                                                          |
 | Public Methods:                                                          |
 | backupFolder(dataDir, reason, fileNames) - makes a backup now            |
 | backupFolder(dataDir, reason, fileNames, when) - the same at a time      |
 | backupsOf(dataDir, reason) - the backups of one kind, oldest first       |
 | takeWarning()              - the extra copy's warning, or null           |
 +--------------------------------------------------------------------------+
 */
public final class Backups
{

    public static final String FOLDER = "backups";
    public static final String EDIT_ROUND = "edit-round";
    public static final String DELETE_ROUND = "delete-round";
    public static final String EDIT_COURSE = "edit-course";
    public static final String DELETE_COURSE = "delete-course";
    public static final String IMPORT = "import";
    public static final String CLEAR = "clear";
    static final Set<String> EVERYDAY = Set.of(EDIT_ROUND, DELETE_ROUND, EDIT_COURSE);
    static final String WARNING_START = "The extra backup copy could not be made: ";
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss");
    private static final Pattern NAME = Pattern.compile("(\\d{4}-\\d{2}-\\d{2}_\\d{6})(?:-(\\d+))?_([a-z-]+)");
    private static String warning = null;

    /*
     +--------------------------------------------------------------------------+
     | Failed                                                                   |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Thrown when the backup cannot be made. Its message says so in words for  |
     | the footer, and that nothing was changed.                                |
     +--------------------------------------------------------------------------+
     */
    @SuppressWarnings("serial")
    public static final class Failed extends IOException
    {
        /*
         * Failed(fileName, cause): the message for a backup of fileName that could not be made.
         */
        Failed(String fileName, IOException cause)
        {
            super("No backup copy of " + fileName + " could be made in the " + FOLDER + " folder, so nothing was changed ("
                    + cause.getMessage() + ").", cause);
        }
    }

    private Backups()
    {
    }

    /*
     * backupFolder(dataDir, reason, fileNames): makes a backup now (see the next one).
     */
    public static Path backupFolder(Path dataDir, String reason, List<String> fileNames) throws IOException
    {
        return backupFolder(dataDir, reason, fileNames, LocalDateTime.now());
    }

    /*
     +--------------------------------------------------------------------------+
     | backupFolder(dataDir, reason, fileNames, when)                           |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Makes backups\YYYY-MM-DD_HHMMSS_reason (with -2, -3 ... after the time   |
     | if that name is taken) and copies into it, under their real names, the   |
     | named files of the data folder that exist. Then the everyday kinds are   |
     | pruned to the BackupsToKeep setting and, if BackupCopyFolder is set,     |
     | the new folder is copied there too (a failure there only leaves a        |
     | warning, see takeWarning). The new folder itself is never pruned.        |
     |                                                                          |
     | Passed in:  dataDir   - the data folder                                  |
     |             reason    - EDIT_ROUND, DELETE_ROUND, EDIT_COURSE,           |
     |                         DELETE_COURSE, IMPORT or CLEAR                   |
     |             fileNames - the files to copy, e.g. rounds.tsv               |
     |             when      - the date and time for the name                   |
     | Passed out: the folder made; throws Failed if it cannot be made, so the  |
     |             caller changes nothing                                       |
     +--------------------------------------------------------------------------+
     */
    public static Path backupFolder(Path dataDir, String reason, List<String> fileNames, LocalDateTime when) throws IOException
    {
        warning = null;
        Path parent = dataDir.toAbsolutePath().resolve(FOLDER);
        Path folder;
        try
        {
            Files.createDirectories(parent);
            String stamp = STAMP.format(when);
            folder = parent.resolve(stamp + "_" + reason);
            int count = 1;
            while (Files.exists(folder))
            {
                count = count + 1;
                folder = parent.resolve(stamp + "-" + count + "_" + reason);
            }
            Files.createDirectories(folder);
            for (String fileName : fileNames)
            {
                Path file = dataDir.resolve(fileName);
                if (Files.exists(file))
                {
                    Files.copy(file, folder.resolve(fileName));
                }
            }
        }
        catch (IOException failure)
        {
            throw new Failed((fileNames.size() == 1 ? fileNames.get(0) : "your data files"), failure);
        }
        Settings settings = Settings.load(dataDir);
        extraCopy(folder, settings.backupCopyFolder());
        prune(dataDir, settings.backupsToKeep(), folder);
        return folder;
    }

    /*
     * backupsOf(dataDir, reason): the backup folders of one kind in the new layout, oldest first (old-style backups are never listed).
     * Passed in:  dataDir - the data folder, reason - the kind
     * Passed out: the folders; empty if there are none
     */
    public static List<Path> backupsOf(Path dataDir, String reason) throws IOException
    {
        Path parent = dataDir.toAbsolutePath().resolve(FOLDER);
        List<Path> found = new ArrayList<>();
        if (!Files.isDirectory(parent))
        {
            return found;
        }
        try (Stream<Path> list = Files.list(parent))
        {
            list.filter(Files::isDirectory).forEach(path ->
            {
                Matcher parts = NAME.matcher(path.getFileName().toString());
                if (parts.matches() && parts.group(3).equals(reason))
                {
                    found.add(path);
                }
            });
        }
        found.sort(Comparator.comparing(Backups::sortKey));
        return found;
    }

    /*
     * sortKey(folder): the date and time in a backup folder's name with its -2, -3 count padded, so the names sort oldest first.
     */
    private static String sortKey(Path folder)
    {
        Matcher parts = NAME.matcher(folder.getFileName().toString());
        parts.matches();
        String count = (parts.group(2) == null ? "1" : parts.group(2));
        return parts.group(1) + "-" + String.format("%06d", Integer.parseInt(count));
    }

    /*
     * prune(dataDir, keep, justMade): deletes the oldest everyday backups beyond the newest keep of each kind (KEEP_ALL keeps all); the backup just made always counts as one of those kept and is never deleted (even if the clock says it is older); a folder that will not delete is left for next time.
     * Passed in:  dataDir - the data folder, keep - how many of each kind to keep, justMade - the new backup folder
     * Passed out: nothing
     */
    static void prune(Path dataDir, int keep, Path justMade) throws IOException
    {
        if (keep == Settings.KEEP_ALL)
        {
            return;
        }
        for (String reason : EVERYDAY)
        {
            List<Path> folders = backupsOf(dataDir, reason);
            folders.remove(justMade);
            int room = (justMade.getFileName().toString().endsWith("_" + reason) ? keep - 1 : keep);
            for (int lp = 0; lp < folders.size() - room; lp = lp + 1)
            {
                try
                {
                    deleteTree(folders.get(lp));
                }
                catch (IOException stuck)
                {
                    // left for next time; the new backup is already made
                }
            }
        }
    }

    /*
     * extraCopy(folder, target): copies a new backup folder into the BackupCopyFolder under the same name; a failure only leaves a warning for takeWarning.
     * Passed in:  folder - the new backup folder, target - the extra folder, or null when it is not set
     * Passed out: nothing
     */
    private static void extraCopy(Path folder, Path target)
    {
        if (target == null)
        {
            return;
        }
        try
        {
            if (!Files.isDirectory(target))
            {
                throw new IOException(target + " is not there");
            }
            Path copy = target.resolve(folder.getFileName().toString());
            Files.createDirectories(copy);
            try (Stream<Path> files = Files.list(folder))
            {
                for (Path file : files.toList())
                {
                    Files.copy(file, copy.resolve(file.getFileName().toString()));
                }
            }
        }
        catch (IOException | RuntimeException failure)
        {
            warning = WARNING_START + failure.getMessage();
        }
    }

    /*
     * takeWarning(): the warning from the last backup's extra copy, or null if there was none; it is given only once.
     */
    public static String takeWarning()
    {
        String given = warning;
        warning = null;
        return given;
    }

    /*
     * deleteTree(folder): deletes a folder and everything in it.
     */
    private static void deleteTree(Path folder) throws IOException
    {
        List<Path> paths;
        try (Stream<Path> walk = Files.walk(folder))
        {
            paths = new ArrayList<>(walk.toList());
        }
        for (int lp = paths.size() - 1; lp >= 0; lp = lp - 1)
        {
            Files.deleteIfExists(paths.get(lp));
        }
    }
}

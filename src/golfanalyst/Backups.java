package golfanalyst;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/*
 +--------------------------------------------------------------------------+
 | Backups                                                                  |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | The one backup routine. Before any save that changes or removes an       |
 | existing line of a data file (an edited round, edited tees), a dated     |
 | copy of the whole file is put in the backups folder inside the data      |
 | folder, for example backups\rounds-2026-10-31-142530.tsv. Adding a new   |
 | round or new tees only adds lines, so it makes no copy.                  |
 |                                                                          |
 | Only the 20 newest copies of each file are kept; older ones are deleted. |
 | Anything else in the backups folder (such as Clear's own                 |
 | clear-YYYY-MM-DD-HHMMSS folders) is never touched. If the copy cannot    |
 | be made, Failed is thrown before anything is written, so nothing         |
 | changes.                                                                 |
 |                                                                          |
 | Constants:                                                               |
 | FOLDER - the backups folder's name ("backups")                           |
 | KEEP   - how many copies of each file are kept (20)                      |
 |                                                                          |
 | Public Methods:                                                          |
 | copyBeforeChange(file)       - makes the dated copy, now                 |
 | copyBeforeChange(file, when) - the same at a given time (for the tests)  |
 | folderFor(file)              - the backups folder beside a data file     |
 | copiesOf(file)               - the copies kept of a file, oldest first   |
 +--------------------------------------------------------------------------+
 */
public final class Backups
{

    public static final String FOLDER = "backups";
    public static final int KEEP = 20;
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss");

    /*
     +--------------------------------------------------------------------------+
     | Failed                                                                   |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Thrown when the backup copy cannot be made. Its message says so in       |
     | words for the footer, and that nothing was changed.                      |
     +--------------------------------------------------------------------------+
     */
    @SuppressWarnings("serial")
    public static final class Failed extends IOException
    {
        /*
         * Failed(fileName, cause): the message for a copy of fileName that could not be made.
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
     * folderFor(file): the backups folder in the data folder that holds a data file.
     * Passed in:  file - rounds.tsv or courses.tsv
     * Passed out: the folder (it may not exist yet)
     */
    public static Path folderFor(Path file)
    {
        return file.toAbsolutePath().resolveSibling(FOLDER);
    }

    /*
     * copyBeforeChange(file): makes the dated backup copy of a data file now (see the next one).
     * Passed in:  file - the data file about to be changed
     * Passed out: the copy made; throws Failed if it cannot be made
     */
    public static Path copyBeforeChange(Path file) throws IOException
    {
        return copyBeforeChange(file, LocalDateTime.now());
    }

    /*
     +--------------------------------------------------------------------------+
     | copyBeforeChange(file, when)                                             |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Copies the whole data file into the backups folder (made if needed) as   |
     | <name>-YYYY-MM-DD-HHMMSS.tsv, for example rounds-2026-10-31-142530.tsv.  |
     | Two copies in the same second get -2, -3 ... after the time. Then the    |
     | copies of that file beyond the newest 20 are deleted (a copy that will   |
     | not delete is left; the save still goes ahead).                          |
     |                                                                          |
     | Passed in:  file - the data file about to be changed                     |
     |             when - the time to put in the name                           |
     | Passed out: the copy made; throws Failed if it cannot be made, so the    |
     |             caller changes nothing                                       |
     +--------------------------------------------------------------------------+
     */
    public static Path copyBeforeChange(Path file, LocalDateTime when) throws IOException
    {
        String fileName = file.getFileName().toString();
        try
        {
            Path folder = folderFor(file);
            Files.createDirectories(folder);
            String base = stemOf(fileName) + "-" + STAMP.format(when);
            Path copy = folder.resolve(base + extensionOf(fileName));
            int count = 1;
            while (Files.exists(copy))
            {
                count = count + 1;
                copy = folder.resolve(base + "-" + count + extensionOf(fileName));
            }
            Files.copy(file, copy);
            prune(file);
            return copy;
        }
        catch (IOException failure)
        {
            throw new Failed(fileName, failure);
        }
    }

    /*
     * copiesOf(file): the backup copies kept of a data file, oldest first (by the time in their names).
     * Passed in:  file - the data file
     * Passed out: the copies; empty if there are none
     */
    public static List<Path> copiesOf(Path file) throws IOException
    {
        Path folder = folderFor(file);
        List<Path> copies = new ArrayList<>();
        if (!Files.isDirectory(folder))
        {
            return copies;
        }
        Pattern name = copyName(file.getFileName().toString());
        try (Stream<Path> files = Files.list(folder))
        {
            files.filter(Files::isRegularFile).filter(path -> name.matcher(path.getFileName().toString()).matches())
                    .forEach(copies::add);
        }
        copies.sort(Comparator.comparing((Path path) -> sortKey(name, path)));
        return copies;
    }

    /*
     * prune(file): deletes the copies of a data file beyond the newest KEEP; one that will not delete is left alone.
     * Passed in:  file - the data file
     * Passed out: nothing
     */
    private static void prune(Path file) throws IOException
    {
        List<Path> copies = copiesOf(file);
        for (int lp = 0; lp < copies.size() - KEEP; lp = lp + 1)
        {
            try
            {
                Files.deleteIfExists(copies.get(lp));
            }
            catch (IOException stuck)
            {
                // left for next time; the new copy is already made
            }
        }
    }

    /*
     * copyName(fileName): the pattern a backup copy's name follows, e.g. rounds-2026-10-31-142530.tsv or ...-142530-2.tsv.
     */
    private static Pattern copyName(String fileName)
    {
        return Pattern.compile(Pattern.quote(stemOf(fileName)) + "-(\\d{4}-\\d{2}-\\d{2}-\\d{6})(?:-(\\d+))?"
                + Pattern.quote(extensionOf(fileName)));
    }

    /*
     * sortKey(name, path): the time in a copy's name with its -2, -3 count padded, so the names sort oldest first.
     */
    private static String sortKey(Pattern name, Path path)
    {
        Matcher parts = name.matcher(path.getFileName().toString());
        parts.matches();
        String count = (parts.group(2) == null ? "1" : parts.group(2));
        return parts.group(1) + "-" + String.format("%06d", Integer.parseInt(count));
    }

    /*
     * stemOf(fileName): the name without its extension ("rounds" for rounds.tsv).
     */
    private static String stemOf(String fileName)
    {
        int dot = fileName.lastIndexOf('.');
        return (dot > 0 ? fileName.substring(0, dot) : fileName);
    }

    /*
     * extensionOf(fileName): the extension with its dot (".tsv"), or "" if there is none.
     */
    private static String extensionOf(String fileName)
    {
        int dot = fileName.lastIndexOf('.');
        return (dot > 0 ? fileName.substring(dot) : "");
    }
}

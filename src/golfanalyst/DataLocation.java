package golfanalyst;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/*
 +--------------------------------------------------------------------------+
 | DataLocation                                                             |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | Where the data folder is. It cannot be kept in settings.txt (that file   |
 | is inside the data folder), so it is kept in a tiny file,                |
 | golfanalyst.location: one line, the folder's path. It is next to the     |
 | program when run from the project and in the user's settings folder      |
 | when installed (ProgramFolders.locationFolder). Without it the program   |
 | uses its usual folder (its own data folder, or the sample data).         |
 |                                                                          |
 | Also what the Settings window needs to move to another folder: what is   |
 | in a folder (describe), and copying the data there without ever writing  |
 | over a file (copyData). The old folder is never touched.                 |
 |                                                                          |
 | Nested types:                                                            |
 | Contents - what a folder holds: rounds, courses, and which of the three  |
 |            data files are already there                                  |
 |                                                                          |
 | Public Methods:                                                          |
 | read(programDir), write(programDir, folder) - the location file          |
 | describe(folder)          - what a folder holds                          |
 | copyData(from, to)        - copies the three files, never overwriting    |
 +--------------------------------------------------------------------------+
 */
public final class DataLocation
{

    public static final String FILE = "golfanalyst.location";
    public static final List<String> DATA_FILES = List.of(DataLoader.COURSES_FILE, DataLoader.ROUNDS_FILE, Settings.FILE);

    /*
     +--------------------------------------------------------------------------+
     | Contents                                                                 |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | What a folder holds: how many rounds and courses (a course is a name     |
     | and zip, whatever its sets of tees), and which of courses.tsv,           |
     | rounds.tsv and settings.txt are already there.                           |
     +--------------------------------------------------------------------------+
     */
    public record Contents(int rounds, int courses, List<String> filesThere)
    {
        /*
         * hasFiles(): whether any of the three data files is already there (then nothing may be copied in).
         */
        public boolean hasFiles()
        {
            return !filesThere.isEmpty();
        }

        /*
         * words(): "This folder has 37 rounds and 12 courses.", "This folder is empty." or, with files but nothing in them, "This folder has the data files but no rounds or courses yet."
         */
        public String words()
        {
            if (rounds == 0 && courses == 0)
            {
                return (hasFiles() ? "This folder has data files (" + String.join(", ", filesThere) + ") but no rounds or courses yet."
                        : "This folder is empty.");
            }
            return "This folder has " + Importer.count(rounds, "round", "rounds") + " and " + Importer.count(courses, "course", "courses")
                    + ".";
        }
    }

    private DataLocation()
    {
    }

    /*
     * read(programDir): the data folder named in golfanalyst.location in that folder, or null when the file is missing, empty, unreadable or names a folder that is not there.
     * Passed in:  programDir - the folder holding the location file (ProgramFolders.locationFolder)
     * Passed out: the folder, or null
     */
    public static Path read(Path programDir)
    {
        Path file = programDir.resolve(FILE);
        try
        {
            if (!Files.isRegularFile(file))
            {
                return null;
            }
            String text = Files.readString(file, StandardCharsets.UTF_8).trim();
            if (text.startsWith(String.valueOf(LineReplacer.BYTE_ORDER_MARK)))
            {
                text = text.substring(1).trim();
            }
            Path folder = (text.isEmpty() ? null : Path.of(text.lines().findFirst().orElse("").trim()));
            return (folder != null && Files.isDirectory(folder) ? folder : null);
        }
        catch (IOException | RuntimeException unreadable)
        {
            return null;
        }
    }

    /*
     * write(programDir, folder): writes golfanalyst.location in that folder (made if needed; one line, the data folder's full path), through a temporary file.
     * Passed in:  programDir - the folder holding the location file (ProgramFolders.locationFolder), folder - the data folder to use from now on
     * Passed out: nothing (throws IOException if it cannot be written)
     */
    public static void write(Path programDir, Path folder) throws IOException
    {
        Files.createDirectories(programDir.toAbsolutePath());
        LineReplacer.writeSafely(programDir.resolve(FILE), folder.toAbsolutePath().normalize() + System.lineSeparator());
    }

    /*
     +--------------------------------------------------------------------------+
     | describe(folder)                                                         |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Says what a folder holds, reading its files with the program's loader    |
     | when they are there (a file that cannot be read counts as nothing).      |
     |                                                                          |
     | Passed in:  folder - the folder                                          |
     | Passed out: the Contents                                                 |
     +--------------------------------------------------------------------------+
     */
    public static Contents describe(Path folder)
    {
        List<String> there = new ArrayList<>();
        for (String name : DATA_FILES)
        {
            if (Files.exists(folder.resolve(name)))
            {
                there.add(name);
            }
        }
        int rounds = 0;
        int courses = 0;
        if (there.contains(DataLoader.COURSES_FILE) || there.contains(DataLoader.ROUNDS_FILE))
        {
            try
            {
                Dataset data = DataLoader.load(folder);
                rounds = data.rounds().size();
                Set<String> names = new HashSet<>();
                for (Tee tee : data.tees())
                {
                    names.add(tee.courseName().trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT) + "|" + tee.zip().trim());
                }
                courses = names.size();
            }
            catch (IOException | RuntimeException unreadable)
            {
                rounds = 0;
                courses = 0;
            }
        }
        return new Contents(rounds, courses, there);
    }

    /*
     +--------------------------------------------------------------------------+
     | copyData(from, to)                                                       |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Copies courses.tsv, rounds.tsv and settings.txt (those that exist) from  |
     | the data folder into another folder. It never writes over a file: if     |
     | any of the three is already in the new folder, nothing is copied. The    |
     | old folder is not touched.                                               |
     |                                                                          |
     | Passed in:  from - the data folder now, to - the new folder              |
     | Passed out: nothing (throws IOException if a file is already there or a  |
     |             copy fails; a copy that failed part way is removed)          |
     +--------------------------------------------------------------------------+
     */
    public static void copyData(Path from, Path to) throws IOException
    {
        Contents there = describe(to);
        if (there.hasFiles())
        {
            throw new IOException(String.join(", ", there.filesThere()) + " is already in " + to + ", so nothing was copied");
        }
        Files.createDirectories(to);
        List<Path> copied = new ArrayList<>();
        try
        {
            for (String name : DATA_FILES)
            {
                if (Files.exists(from.resolve(name)))
                {
                    Files.copy(from.resolve(name), to.resolve(name));
                    copied.add(to.resolve(name));
                }
            }
        }
        catch (IOException failure)
        {
            for (Path made : copied)
            {
                Files.deleteIfExists(made);
            }
            throw failure;
        }
    }
}

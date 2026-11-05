package golfanalyst;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/*
 +--------------------------------------------------------------------------+
 | TestFolders                                                              |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | The self-tests' temporary folders. Every test that needs a folder of its |
 | own makes it here (make) instead of with Files.createTempDirectory, so   |
 | the folder is remembered; when a test run ends, passed or failed, its    |
 | runner calls deleteAll and every folder it made is removed with all that |
 | is in it. Before this the tests left about 20 folders in the system's    |
 | temporary folder on every run. Only the tests use it; the program never  |
 | does.                                                                    |
 |                                                                          |
 | Public Methods:                                                          |
 | make(prefix)  - a new empty temporary folder, remembered                 |
 | deleteAll()   - removes every remembered folder; gives how many could    |
 |                 not be removed                                           |
 +--------------------------------------------------------------------------+
 */
public final class TestFolders
{

    private static final List<Path> MADE = new ArrayList<>();

    private TestFolders()
    {
    }

    /*
     * make(prefix): a new empty folder in the system's temporary folder, named prefix plus a number, remembered for deleteAll.
     * Passed in:  prefix - the start of the folder's name, for example "golfanalyst-import"
     * Passed out: the folder (throws IOException if it cannot be made)
     */
    public static synchronized Path make(String prefix) throws IOException
    {
        Path folder = Files.createTempDirectory(prefix);
        MADE.add(folder);
        return folder;
    }

    /*
     +--------------------------------------------------------------------------+
     | deleteAll()                                                              |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Removes every folder made with make, with everything in it (files        |
     | first, the deepest folders before the ones holding them). A folder a     |
     | test has already removed is passed over; one that cannot be removed      |
     | (a file still open, say) is left and counted. The list is emptied.       |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: how many of the folders could not be removed (0 when all     |
     |             were)                                                        |
     +--------------------------------------------------------------------------+
     */
    public static synchronized int deleteAll()
    {
        int left = 0;
        for (Path folder : MADE)
        {
            if (!Files.exists(folder))
            {
                continue;
            }
            List<Path> inside = new ArrayList<>();
            try (Stream<Path> walk = Files.walk(folder))
            {
                walk.forEach(path -> inside.add(path));
            }
            catch (IOException | RuntimeException unreadable)
            {
                left = left + 1;
                continue;
            }
            inside.sort(Comparator.comparingInt((Path path) -> path.getNameCount()).reversed());
            for (Path path : inside)
            {
                try
                {
                    Files.deleteIfExists(path);
                }
                catch (IOException stillOpen)
                {
                    // left in place; the folder is counted below
                }
            }
            if (Files.exists(folder))
            {
                left = left + 1;
            }
        }
        MADE.clear();
        return left;
    }
}

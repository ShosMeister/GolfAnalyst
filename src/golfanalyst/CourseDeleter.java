package golfanalyst;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*
 +--------------------------------------------------------------------------+
 | CourseDeleter                                                            |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | Deletes a course: ALL of its sets of tees and EVERY round played on      |
 | them (historical ones too), since a round cannot exist without its tees. |
 |                                                                          |
 | summarize counts what would go and words the question and the list of    |
 | rounds for the dialog; nothing is written.                               |
 |                                                                          |
 | delete first copies courses.tsv and rounds.tsv into                      |
 | backups\delete-course-YYYY-MM-DD-HHMMSS (never pruned; if that fails     |
 | nothing is deleted). Then only the course's tee lines and only the       |
 | rounds on those TeeIDs are taken out (each line with its own line        |
 | break): comments, blank lines, every other line and the files' line      |
 | endings stay exactly as they were, and nothing is renumbered. It is all  |
 | or nothing: both new files are written to temporary files first, then    |
 | moved into place; if the second move fails, the first file is put back   |
 | from the backup and the problem is reported.                             |
 |                                                                          |
 | Nested types:                                                            |
 | Summary - what deleting a course would remove, in words                  |
 | Mover   - moves a finished temporary file into place (the self-test      |
 |           can make it fail)                                              |
 |                                                                          |
 | Public Methods:                                                          |
 | summarize(tees, rounds) - what would go                                  |
 | delete(dataDir, tees, rounds) - does it                                  |
 +--------------------------------------------------------------------------+
 */
public final class CourseDeleter
{

    static final String PREFIX = "delete-course-";
    public static final String UNSAVED_IGNORED = " Your unsaved changes on the screen will be ignored.";

    /*
     * Mover: moves a finished temporary file over the real one.
     */
    interface Mover
    {
        /*
         * move(from, to): replaces to with from.
         */
        void move(Path from, Path to) throws IOException;
    }

    /*
     +--------------------------------------------------------------------------+
     | Summary                                                                  |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | What deleting a course would remove: its name and zip, how many sets of  |
     | tees, rounds and historical rounds, and one line per round (date, tees,  |
     | score; historical ones marked), oldest first.                            |
     +--------------------------------------------------------------------------+
     */
    public record Summary(String courseName, String zip, int tees, int rounds, int historical, String list)
    {
        /*
         +--------------------------------------------------------------------------+
         | question(unsaved)                                                        |
         +--------------------------------------------------------------------------+
         | Purpose:                                                                 |
         | The first question: "Delete Edgewater (29708)? This removes the course,  |
         | its 3 sets of tees and all 12 rounds played on them (4 of them           |
         | historical). Your handicap and statistics will be worked out again. A    |
         | copy of your files is kept first." With no rounds it says "No rounds     |
         | have been played on it." instead. With unsaved changes on the screen     |
         | it adds that they will be ignored.                                       |
         |                                                                          |
         | Passed in:  unsaved - true if anything on the screen is not saved        |
         | Passed out: the words                                                    |
         +--------------------------------------------------------------------------+
         */
        public String question(boolean unsaved)
        {
            String teeWords = "its " + tees + (tees == 1 ? " set of tees" : " sets of tees");
            String what;
            if (rounds == 0)
            {
                what = "This removes the course and " + teeWords + ". No rounds have been played on it.";
            }
            else
            {
                String roundWords = (rounds == 1 ? "the 1 round played on them" : "all " + rounds + " rounds played on them");
                String old = "";
                if (historical > 0)
                {
                    old = (rounds == 1 ? " (it is historical)" : " (" + historical + " of them historical)");
                }
                what = "This removes the course, " + teeWords + " and " + roundWords + old + ".";
            }
            return "Delete " + courseName + " (" + zip + ")? " + what + " Your handicap and statistics will be worked out again. "
                    + "A copy of your files is kept first." + (unsaved ? UNSAVED_IGNORED : "");
        }

        /*
         * prompt(): the second question: "To confirm, type the name of the course exactly: Edgewater".
         */
        public String prompt()
        {
            return "To confirm, type the name of the course exactly: " + courseName;
        }

        /*
         * done(folderName): the footer afterwards: "Deleted Edgewater: 3 sets of tees and 12 rounds. A copy of your files as they were is in backups\delete-course-...."
         */
        public String done(String folderName)
        {
            return "Deleted " + courseName + ": " + tees + (tees == 1 ? " set of tees and " : " sets of tees and ") + rounds
                    + (rounds == 1 ? " round" : " rounds") + ". A copy of your files as they were is in " + Backups.FOLDER
                    + java.io.File.separator + folderName + ".";
        }
    }

    private CourseDeleter()
    {
    }

    /*
     +--------------------------------------------------------------------------+
     | summarize(tees, rounds)                                                  |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Counts what deleting a course would remove and lists its rounds, one     |
     | line each, oldest first: "2026-10-22   Senior   89", with "(9 holes)"    |
     | for a nine and "(historical)" for a historical round.                    |
     |                                                                          |
     | Passed in:  tees   - every set of tees of the course                     |
     |             rounds - every round in the program                          |
     | Passed out: the Summary                                                  |
     +--------------------------------------------------------------------------+
     */
    public static Summary summarize(List<Tee> tees, List<Round> rounds)
    {
        Map<Integer, Tee> byId = new HashMap<>();
        for (Tee tee : tees)
        {
            byId.put(tee.id(), tee);
        }
        List<Round> going = new ArrayList<>();
        for (Round round : rounds)
        {
            if (byId.containsKey(round.teeId()))
            {
                going.add(round);
            }
        }
        going.sort(Comparator.comparing(Round::date).thenComparingInt(Round::id));
        StringBuilder list = new StringBuilder();
        int historical = 0;
        for (Round round : going)
        {
            historical = (round.historical() ? historical + 1 : historical);
            list.append(round.date()).append("   ").append(byId.get(round.teeId()).teeName()).append("   ").append(round.totalScore())
                    .append(round.isNineHole() ? " (9 holes)" : "").append(round.historical() ? "   (historical)" : "").append('\n');
        }
        Tee first = tees.get(0);
        return new Summary(first.courseName(), first.zip(), tees.size(), going.size(), historical, list.toString().trim());
    }

    /*
     * delete(dataDir, tees, rounds): deletes the course with the normal file moves (see the class comment).
     * Passed in:  dataDir - the data folder, tees - every set of tees of the course, rounds - every round in the program
     * Passed out: the backup folder made (throws Backups.Failed or IOException; see the next one)
     */
    public static Path delete(Path dataDir, List<Tee> tees, List<Round> rounds) throws IOException
    {
        return delete(dataDir, tees, rounds, (from, to) ->
        {
            try
            {
                Files.move(from, to, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            }
            catch (IOException notAtomic)
            {
                Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
            }
        });
    }

    /*
     +--------------------------------------------------------------------------+
     | delete(dataDir, tees, rounds, mover)                                     |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Deletes the course (see the class comment): works out both new files,    |
     | copies both old ones into the backup folder, writes both new ones to     |
     | temporary files, then moves courses.tsv and rounds.tsv into place. If    |
     | anything fails before the first move nothing has changed; if the second  |
     | move fails, courses.tsv is put back from the backup. Temporary files     |
     | are always removed.                                                      |
     |                                                                          |
     | Passed in:  dataDir - the data folder                                    |
     |             tees    - every set of tees of the course                    |
     |             rounds  - every round in the program                         |
     |             mover   - what moves a temporary file into place             |
     | Passed out: the backup folder made; throws Backups.Failed if the copy    |
     |             cannot be made, or IOException if a line cannot be found or  |
     |             a file cannot be written (saying what was put back)          |
     +--------------------------------------------------------------------------+
     */
    static Path delete(Path dataDir, List<Tee> tees, List<Round> rounds, Mover mover) throws IOException
    {
        Path coursesFile = dataDir.resolve(DataLoader.COURSES_FILE);
        Path roundsFile = dataDir.resolve(DataLoader.ROUNDS_FILE);
        String courses = Files.readString(coursesFile, StandardCharsets.UTF_8);
        String roundsText = (Files.exists(roundsFile) ? Files.readString(roundsFile, StandardCharsets.UTF_8) : "");
        Set<Integer> teeIds = new HashSet<>();
        Map<Integer, LineReplacer.LineMaker> teeMakers = new HashMap<>();
        for (Tee tee : tees)
        {
            teeIds.add(tee.id());
            teeMakers.put(tee.id(), (headerCells, oldCells) -> null);
        }
        Map<Integer, LineReplacer.LineMaker> roundMakers = new HashMap<>();
        for (Round round : rounds)
        {
            if (teeIds.contains(round.teeId()))
            {
                roundMakers.put(round.id(), (headerCells, oldCells) -> null);
            }
        }
        String newCourses = LineReplacer.splice(courses, "teeid", teeMakers, DataLoader.COURSES_FILE).text();
        String newRounds = (roundMakers.isEmpty() ? roundsText
                : LineReplacer.splice(roundsText, "roundid", roundMakers, DataLoader.ROUNDS_FILE).text());

        Path folder = Backups.copyIntoFolder(dataDir, PREFIX, List.of(DataLoader.COURSES_FILE, DataLoader.ROUNDS_FILE),
                LocalDateTime.now());
        Path coursesTemp = coursesFile.resolveSibling(DataLoader.COURSES_FILE + ".new");
        Path roundsTemp = roundsFile.resolveSibling(DataLoader.ROUNDS_FILE + ".new");
        try
        {
            Files.writeString(coursesTemp, newCourses, StandardCharsets.UTF_8);
            if (!roundMakers.isEmpty())
            {
                Files.writeString(roundsTemp, newRounds, StandardCharsets.UTF_8);
            }
            mover.move(coursesTemp, coursesFile);
            if (!roundMakers.isEmpty())
            {
                try
                {
                    mover.move(roundsTemp, roundsFile);
                }
                catch (IOException roundsFailed)
                {
                    Files.copy(folder.resolve(DataLoader.COURSES_FILE), coursesFile, StandardCopyOption.REPLACE_EXISTING);
                    throw new IOException("rounds.tsv could not be written (" + roundsFailed.getMessage()
                            + "), so courses.tsv was put back as it was. Nothing was deleted.", roundsFailed);
                }
            }
        }
        finally
        {
            Files.deleteIfExists(coursesTemp);
            Files.deleteIfExists(roundsTemp);
        }
        return folder;
    }
}

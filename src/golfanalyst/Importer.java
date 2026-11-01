package golfanalyst;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/*
 +--------------------------------------------------------------------------+
 | Importer                                                                 |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | Adds rounds and courses from other files in the program's own layout     |
 | (another copy's data folder, or a spreadsheet saved as tab-delimited     |
 | text). It never changes a tee or round that is already there.            |
 |                                                                          |
 | check reads the files with the program's own loader, one row at a time,  |
 | so a bad row is rejected with its line number and the loader's reason    |
 | instead of stopping everything, and works out a Plan; nothing is         |
 | written. Tees: a tee whose course name and tee name match one of yours   |
 | (ignoring capitals and extra spaces; the zips must agree when both have  |
 | one) is skipped as "already here"; any other tee is added with a new     |
 | TeeID (your highest plus 1, in file order). Rounds: each round's TeeID   |
 | is changed to the TeeID it has in your data (with a courses file it      |
 | refers to that file's tees; without one it must already be one of        |
 | yours); a round with the same tee, date, holes, scores and putts as one  |
 | you have (or one earlier in the file) is skipped; the rest get new       |
 | RoundIDs (your highest plus 1, in file order).                           |
 |                                                                          |
 | apply first copies courses.tsv and rounds.tsv into the backups folder    |
 | (backups\import-YYYY-MM-DD-HHMMSS; if that fails nothing is written),    |
 | checks the files as they would become with the loader, then adds the     |
 | new lines at the end of each file in its own column order and line       |
 | endings, through a temporary file. Existing lines and comments are never |
 | touched (only a Historical column may be added to the header, when       |
 | historical rounds go into a file that has none).                         |
 |                                                                          |
 | Nested types:                                                            |
 | Note - one skipped or rejected row: file, line and reason                |
 | Plan - what an import would do                                           |
 |                                                                          |
 | Public Methods:                                                          |
 | check(coursesFile, roundsFile, mine, markHistorical) - works out a Plan  |
 | apply(dataDir, plan)                                  - does it          |
 +--------------------------------------------------------------------------+
 */
public final class Importer
{

    static final String IMPORT_PREFIX = "import-";

    /*
     * Note: one row that was skipped or rejected: the file's name, its line number (0 for the whole file) and why.
     */
    public record Note(String file, int line, String reason)
    {
        /*
         * toString(): "rounds.tsv, line 12: no tee with that number (TeeID 9)".
         */
        @Override
        public String toString()
        {
            return file + (line > 0 ? ", line " + line : "") + ": " + reason;
        }
    }

    /*
     +--------------------------------------------------------------------------+
     | Plan                                                                     |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | What an import would do: the new tees and rounds (with their new         |
     | TeeIDs and RoundIDs), how many were skipped and rejected, the notes for  |
     | them, and whether a courses file and a rounds file were given.           |
     +--------------------------------------------------------------------------+
     */
    public record Plan(List<Tee> newTees, List<Round> newRounds, int teesSkipped, int teesRejected, int roundsSkipped,
                       int roundsRejected, List<Note> rejected, List<Note> skipped, boolean hadCourses, boolean hadRounds)
    {
        /*
         * hasWork(): whether there is anything to add.
         */
        public boolean hasWork()
        {
            return !newTees.isEmpty() || !newRounds.isEmpty();
        }

        /*
         +--------------------------------------------------------------------------+
         | summary()                                                                |
         +--------------------------------------------------------------------------+
         | Purpose:                                                                 |
         | The preview in plain words: "3 new tees will be added (2 already here,   |
         | skipped)." and "35 rounds will be added (4 already here, skipped; 2      |
         | rejected).", then every rejected row and every skipped row with its      |
         | file, line number and reason.                                            |
         |                                                                          |
         | Passed in:  nothing                                                      |
         | Passed out: the text                                                     |
         +--------------------------------------------------------------------------+
         */
        public String summary()
        {
            StringBuilder text = new StringBuilder();
            if (hadCourses)
            {
                text.append(count(newTees.size(), "new tee", "new tees")).append(" will be added")
                       .append(details(teesSkipped, teesRejected)).append(".\n");
            }
            if (hadRounds)
            {
                text.append(count(newRounds.size(), "round", "rounds")).append(" will be added")
                        .append(details(roundsSkipped, roundsRejected)).append(".\n");
            }
            if (!hasWork())
            {
                text.append("There is nothing new to import.\n");
            }
            if (!rejected.isEmpty())
            {
                text.append("\nRejected:\n");
                for (Note note : rejected)
                {
                    text.append("  ").append(note).append('\n');
                }
            }
            if (!skipped.isEmpty())
            {
                text.append("\nSkipped:\n");
                for (Note note : skipped)
                {
                    text.append("  ").append(note).append('\n');
                }
            }
            return text.toString();
        }

        /*
         * details(skippedCount, rejectedCount): " (4 already here, skipped; 2 rejected)", or "" when there are none.
         */
        private static String details(int skippedCount, int rejectedCount)
        {
            List<String> parts = new ArrayList<>();
            if (skippedCount > 0)
            {
                parts.add(skippedCount + " already here, skipped");
            }
            if (rejectedCount > 0)
            {
                parts.add(rejectedCount + " rejected");
            }
            return (parts.isEmpty() ? "" : " (" + String.join("; ", parts) + ")");
        }
    }

    private Importer()
    {
    }

    /*
     * count(number, one, many): "1 round" or "35 rounds".
     */
    public static String count(int number, String one, String many)
    {
        return number + " " + (number == 1 ? one : many);
    }

    /*
     +--------------------------------------------------------------------------+
     | check(coursesFile, roundsFile, mine, markHistorical)                     |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Works out what importing the files would do (see the class comment).     |
     | Either file may be null. Nothing is written.                             |
     |                                                                          |
     | Passed in:  coursesFile    - the courses file to import, or null         |
     |             roundsFile     - the rounds file to import, or null          |
     |             mine           - the data already in the program             |
     |             markHistorical - true to mark every imported round           |
     |                              historical (Y); false keeps the file's own  |
     | Passed out: the Plan (throws IOException if a file cannot be read)       |
     +--------------------------------------------------------------------------+
     */
    public static Plan check(Path coursesFile, Path roundsFile, Dataset mine, boolean markHistorical) throws IOException
    {
        List<Note> rejected = new ArrayList<>();
        List<Note> skipped = new ArrayList<>();
        List<Tee> newTees = new ArrayList<>();
        Map<Integer, Tee> byImportId = new HashMap<>();
        int nextTeeId = 1;
        for (Tee tee : mine.tees())
        {
            nextTeeId = Math.max(nextTeeId, tee.id() + 1);
        }
        int teesSkipped = 0;
        int teesRejected = 0;
        if (coursesFile != null)
        {
            String name = coursesFile.getFileName().toString();
            List<String> lines = readFile(coursesFile, name, rejected);
            for (int lineNo : dataLines(lines, name, rejected, true))
            {
                List<Problem> problems = new ArrayList<>();
                List<Tee> parsed = DataLoader.parseCourses(onlyLine(lines, lineNo), problems);
                String reason = errors(problems);
                if (parsed.isEmpty() || reason != null)
                {
                    rejected.add(new Note(name, lineNo, (reason == null ? "this row could not be read" : reason)));
                    teesRejected = teesRejected + 1;
                    continue;
                }
                Tee tee = parsed.get(0);
                if (byImportId.containsKey(tee.id()))
                {
                    rejected.add(new Note(name, lineNo, "TeeID " + tee.id() + " is used on an earlier line of this file"));
                    teesRejected = teesRejected + 1;
                    continue;
                }
                Tee match = matchingTee(tee, mine.tees());
                Tee earlier = matchingTee(tee, newTees);
                if (match != null || earlier != null)
                {
                    Tee found = (match != null ? match : earlier);
                    byImportId.put(tee.id(), found);
                    skipped.add(new Note(name, lineNo, "already here (" + found.courseName() + ":" + found.teeName()
                            + (match != null ? ")" : ", earlier in this file)")));
                    teesSkipped = teesSkipped + 1;
                    continue;
                }
                List<Tee> known = new ArrayList<>(mine.tees());
                known.addAll(newTees);
                Tee added = renumbered(tee, nextTeeId, courseSpelling(tee, known));
                nextTeeId = nextTeeId + 1;
                newTees.add(added);
                byImportId.put(tee.id(), added);
            }
        }

        List<Round> newRounds = new ArrayList<>();
        int roundsSkipped = 0;
        int roundsRejected = 0;
        if (roundsFile != null)
        {
            String name = roundsFile.getFileName().toString();
            Map<Integer, Tee> lookup = new HashMap<>();
            if (coursesFile != null)
            {
                lookup.putAll(byImportId);
            }
            else
            {
                for (Tee tee : mine.tees())
                {
                    lookup.put(tee.id(), tee);
                }
            }
            Set<String> known = new HashSet<>();
            for (Round round : mine.rounds())
            {
                known.add(sameRoundKey(round, round.teeId()));
            }
            int nextRoundId = 1;
            for (Round round : mine.rounds())
            {
                nextRoundId = Math.max(nextRoundId, round.id() + 1);
            }
            List<String> lines = readFile(roundsFile, name, rejected);
            for (int lineNo : dataLines(lines, name, rejected, false))
            {
                List<Problem> problems = new ArrayList<>();
                List<Round> parsed = DataLoader.parseRounds(onlyLine(lines, lineNo), lookup, problems);
                String reason = errors(problems);
                if (parsed.isEmpty() || reason != null)
                {
                    rejected.add(new Note(name, lineNo, (reason == null ? "this row could not be read" : reason)));
                    roundsRejected = roundsRejected + 1;
                    continue;
                }
                Round round = parsed.get(0);
                int teeId = lookup.get(round.teeId()).id();
                String key = sameRoundKey(round, teeId);
                if (!known.add(key))
                {
                    skipped.add(new Note(name, lineNo, "already here (the same tee, date, holes, scores and putts)"));
                    roundsSkipped = roundsSkipped + 1;
                    continue;
                }
                newRounds.add(new Round(nextRoundId, teeId, round.date(), round.pcc(), round.startHole(), round.holes(),
                        round.scores(), round.putts(), round.fairway(), round.sand(), (markHistorical || round.historical())));
                nextRoundId = nextRoundId + 1;
            }
        }
        return new Plan(newTees, newRounds, teesSkipped, teesRejected, roundsSkipped, roundsRejected, rejected, skipped,
                coursesFile != null, roundsFile != null);
    }

    /*
     * readFile(file, name, rejected): the file's lines (byte-order mark removed), or none with a note if it is missing.
     */
    private static List<String> readFile(Path file, String name, List<Note> rejected) throws IOException
    {
        if (!Files.isRegularFile(file))
        {
            rejected.add(new Note(name, 0, "the file was not found: " + file.toAbsolutePath()));
            return new ArrayList<>();
        }
        return DataLoader.readLines(file, name, new ArrayList<>());
    }

    /*
     +--------------------------------------------------------------------------+
     | dataLines(lines, name, rejected, courses)                                |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Finds the rows to read: every line after the header row that is not      |
     | blank or a # comment. If the file as a whole cannot be read (empty, no   |
     | header row, a needed column missing) the loader's reason is noted for    |
     | the file and there are no rows.                                          |
     |                                                                          |
     | Passed in:  lines    - the file's lines                                  |
     |             name     - its name, for notes                               |
     |             rejected - receives a note for a file that cannot be read    |
     |             courses  - true for a courses file, false for rounds         |
     | Passed out: the line numbers (1 = first line) of the rows                |
     +--------------------------------------------------------------------------+
     */
    private static List<Integer> dataLines(List<String> lines, String name, List<Note> rejected, boolean courses)
    {
        List<Integer> rows = new ArrayList<>();
        if (lines.isEmpty())
        {
            if (rejected.stream().noneMatch(note -> note.file().equals(name)))
            {
                rejected.add(new Note(name, 0, "the file is empty"));
            }
            return rows;
        }
        int header = -1;
        for (int lp = 0; lp < lines.size(); lp = lp + 1)
        {
            String line = lines.get(lp);
            boolean content = !line.isBlank() && !line.trim().startsWith("#");
            if (content && header >= 0)
            {
                rows.add(lp + 1);
            }
            if (content && header < 0)
            {
                header = lp;
            }
        }
        List<Problem> problems = new ArrayList<>();
        List<String> headerOnly = (header < 0 ? lines : lines.subList(0, header + 1));
        if (courses)
        {
            DataLoader.parseCourses(headerOnly, problems);
        }
        else
        {
            DataLoader.parseRounds(headerOnly, Map.of(), problems);
        }
        String reason = errors(problems);
        if (reason != null)
        {
            rejected.add(new Note(name, 0, "the file cannot be read: " + reason));
            return new ArrayList<>();
        }
        return rows;
    }

    /*
     * onlyLine(lines, lineNo): the file with every row but one blanked out (the header and comments stay), so the loader reads just that row and its problems keep the real line number.
     */
    private static List<String> onlyLine(List<String> lines, int lineNo)
    {
        List<String> single = new ArrayList<>(lines.size());
        boolean headerSeen = false;
        for (int lp = 0; lp < lines.size(); lp = lp + 1)
        {
            String line = lines.get(lp);
            boolean content = !line.isBlank() && !line.trim().startsWith("#");
            single.add((content && headerSeen && lp + 1 != lineNo) ? "" : line);
            headerSeen = headerSeen || content;
        }
        return single;
    }

    /*
     * errors(problems): the loader's error messages joined with "; ", with "TeeID 9 is not in courses.tsv" said plainly as "no tee with that number (TeeID 9)", or null when there are none (warnings do not reject a row).
     */
    private static String errors(List<Problem> problems)
    {
        List<String> messages = new ArrayList<>();
        for (Problem problem : problems)
        {
            if (problem.severity() == Problem.Severity.ERROR)
            {
                String message = problem.message();
                if (message.startsWith("TeeID ") && message.contains(" is not in "))
                {
                    message = "no tee with that number (" + message.substring(0, message.indexOf(" is not in ")) + ")";
                }
                messages.add(message);
            }
        }
        return (messages.isEmpty() ? null : String.join("; ", messages));
    }

    /*
     * sameName(text): a name with capitals and extra spaces ignored, for matching.
     */
    static String sameName(String text)
    {
        return text.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    /*
     * matchingTee(tee, tees): the tee among tees with the same course name and tee name (capitals and extra spaces ignored) whose zip agrees when both have one, or null.
     */
    static Tee matchingTee(Tee tee, List<Tee> tees)
    {
        for (Tee other : tees)
        {
            boolean zipsAgree = tee.zip().isBlank() || other.zip().isBlank() || tee.zip().trim().equals(other.zip().trim());
            if (sameName(other.courseName()).equals(sameName(tee.courseName())) && sameName(other.teeName()).equals(sameName(tee.teeName()))
                    && zipsAgree)
            {
                return other;
            }
        }
        return null;
    }

    /*
     * courseSpelling(tee, tees): the spelling of the course name already used for the same course (name and agreeing zip), in the program or earlier in the file, so its new tees match it, or the tee's own.
     */
    private static String courseSpelling(Tee tee, List<Tee> tees)
    {
        for (Tee other : tees)
        {
            boolean zipsAgree = tee.zip().isBlank() || other.zip().isBlank() || tee.zip().trim().equals(other.zip().trim());
            if (sameName(other.courseName()).equals(sameName(tee.courseName())) && zipsAgree)
            {
                return other.courseName();
            }
        }
        return tee.courseName();
    }

    /*
     * renumbered(tee, id, courseName): the same tee with a new TeeID and course name, every other value kept.
     */
    private static Tee renumbered(Tee tee, int id, String courseName)
    {
        return new Tee(id, courseName, tee.city(), tee.state(), tee.zip(), tee.teeName(), tee.holes(), tee.rating(), tee.slope(),
                tee.yardage(), tee.pars(), tee.strokeIndex(), tee.frontRating(), tee.frontSlope(), tee.backRating(), tee.backSlope());
    }

    /*
     * sameRoundKey(round, teeId): what makes two rounds the same: the tee, date, holes played, scores and putts.
     */
    private static String sameRoundKey(Round round, int teeId)
    {
        return teeId + "|" + round.date() + "|" + round.startHole() + "|" + round.holes() + "|" + Arrays.toString(round.scores())
                + "|" + Arrays.toString(round.putts());
    }

    /*
     +--------------------------------------------------------------------------+
     | apply(dataDir, plan)                                                     |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Does what check worked out. First courses.tsv and rounds.tsv are copied  |
     | into backups\import-YYYY-MM-DD-HHMMSS (if that fails nothing is          |
     | written). Then both files are worked out as they would become and read   |
     | back with the loader's checks; a new line that would not load stops      |
     | everything before anything is written. Then the new tees and rounds are  |
     | added at the end of each file (its own column order and line endings),   |
     | each file written through a temporary file. Existing lines and comments  |
     | are never touched.                                                       |
     |                                                                          |
     | Passed in:  dataDir - the data folder                                    |
     |             plan    - from check, on this data                           |
     | Passed out: the backup folder made; throws Backups.Failed if the copy    |
     |             cannot be made, or IOException if there is nothing to add,   |
     |             a line would not load or a file cannot be written            |
     +--------------------------------------------------------------------------+
     */
    public static Path apply(Path dataDir, Plan plan) throws IOException
    {
        if (!plan.hasWork())
        {
            throw new IOException("there is nothing to import");
        }
        Path coursesFile = dataDir.resolve(DataLoader.COURSES_FILE);
        Path roundsFile = dataDir.resolve(DataLoader.ROUNDS_FILE);
        String courses = (Files.exists(coursesFile) ? Files.readString(coursesFile, StandardCharsets.UTF_8) : "");
        String rounds = (Files.exists(roundsFile) ? Files.readString(roundsFile, StandardCharsets.UTF_8) : "");
        Path folder = Backups.copyIntoFolder(dataDir, IMPORT_PREFIX, List.of(DataLoader.COURSES_FILE, DataLoader.ROUNDS_FILE),
                LocalDateTime.now());

        String newCourses = courses + CourseWriter.additionFor(courses, plan.newTees());
        CourseWriter.checkLoads(courses, newCourses, Set.of());
        Map<Integer, Tee> allTees = new LinkedHashMap<>();
        for (Tee tee : DataLoader.parseCourses(LineReplacer.lines(newCourses), new ArrayList<>()))
        {
            allTees.put(tee.id(), tee);
        }
        boolean anyHistorical = plan.newRounds().stream().anyMatch(Round::historical);
        String startRounds = rounds;
        String[] header = RoundWriter.findHeader(rounds);
        if (anyHistorical && header != null && !RoundWriter.hasHistoricalColumn(header))
        {
            startRounds = RoundWriter.withHistoricalColumn(rounds);
        }
        String newRounds = startRounds + RoundWriter.additionFor(startRounds, plan.newRounds(), allTees);
        RoundWriter.checkLoads(rounds, newRounds, allTees);

        if (!plan.newTees().isEmpty())
        {
            LineReplacer.writeSafely(coursesFile, newCourses);
        }
        if (!plan.newRounds().isEmpty())
        {
            LineReplacer.writeSafely(roundsFile, newRounds);
        }
        return folder;
    }
}

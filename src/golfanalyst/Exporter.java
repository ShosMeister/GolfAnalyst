package golfanalyst;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/*
 +--------------------------------------------------------------------------+
 | Exporter                                                                 |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | Makes a copy of some or all of the golfer's courses and rounds in the    |
 | program's own layout, so another copy of Golf Analyst can Import it. It  |
 | only READS the data files: nothing in them changes and no backup is      |
 | needed.                                                                  |
 |                                                                          |
 | plan works out what a choice would write: everything; one course (all    |
 | its sets of tees and the rounds on them); or rounds from one date to     |
 | another, both days included (and the sets of tees those rounds use).     |
 | Historical rounds are left out unless includeHistorical is true.         |
 |                                                                          |
 | write makes a NEW folder GolfAnalyst-export_YYYY-MM-DD_HHMMSS inside the |
 | chosen folder (with -2, -3 ... if that name is taken; nothing is ever    |
 | written over) and puts in it courses.tsv and rounds.tsv made from the    |
 | golfer's own files: the same header, the same column order, the same     |
 | line endings and the very same lines for the chosen rows, so TeeIDs and  |
 | RoundIDs are unchanged (Import gives new ones). The Historical column is |
 | left out when no exported round is historical. A short ReadMe.txt says   |
 | what the folder holds and how to Import it. Files are written under      |
 | temporary names and then moved into place; on any failure the half-made  |
 | folder is removed.                                                       |
 |                                                                          |
 | Nested types:                                                            |
 | Kind, Choice - what to export                                            |
 | CourseItem   - one course for the choice list                            |
 | Plan         - the sets of tees and rounds a choice would write          |
 |                                                                          |
 | Public Methods:                                                          |
 | courses(data)             - the courses to choose from                   |
 | plan(data, choice)        - what would be written                        |
 | write(dataDir, into, plan, when) - writes the export folder              |
 +--------------------------------------------------------------------------+
 */
public final class Exporter
{

    public static final String FOLDER_PREFIX = "GolfAnalyst-export_";
    public static final String README = "ReadMe.txt";
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss");

    /*
     * Kind: EVERYTHING, one COURSE and its rounds, or the rounds between two DATES.
     */
    public enum Kind
    {
        EVERYTHING, COURSE, DATES
    }

    /*
     * Choice: what to export: the kind, the course (name and zip, for COURSE), the dates (for DATES; a null to means today) and whether historical rounds go too.
     */
    public record Choice(Kind kind, String courseName, String zip, LocalDate from, LocalDate to, boolean includeHistorical)
    {
    }

    /*
     * CourseItem: one course in the choice list: its name, zip and how many sets of tees; shown as "Edgewater (29720), 4 sets of tees".
     */
    public record CourseItem(String name, String zip, int tees)
    {
        /*
         * toString(): how it reads in the list.
         */
        @Override
        public String toString()
        {
            return name + " (" + zip + "), " + tees + (tees == 1 ? " set of tees" : " sets of tees");
        }
    }

    /*
     * Plan: the sets of tees and the rounds a choice would write, in file order, and how many of the rounds are historical.
     */
    public record Plan(List<Tee> tees, List<Round> rounds, int historical)
    {
        /*
         * hasWork(): whether there is anything to write.
         */
        public boolean hasWork()
        {
            return !tees.isEmpty() || !rounds.isEmpty();
        }

        /*
         * words(): "This will write 12 sets of tees and 37 rounds (4 of them historical)." or, with nothing, "There is nothing to write for this choice."
         */
        public String words()
        {
            if (!hasWork())
            {
                return "There is nothing to write for this choice.";
            }
            return "This will write " + counts() + (historical > 0 ? " (" + historical + " of them historical)" : "") + ".";
        }

        /*
         * counts(): "12 sets of tees and 37 rounds".
         */
        public String counts()
        {
            return Importer.count(tees.size(), "set of tees", "sets of tees") + " and " + Importer.count(rounds.size(), "round", "rounds");
        }
    }

    /*
     * Mover: moves a finished temporary file into place (the self-test can make it fail).
     */
    interface Mover
    {
        /*
         * move(from, to): puts from in place of to.
         */
        void move(Path from, Path to) throws IOException;
    }

    private Exporter()
    {
    }

    /*
     * sameName(text): a name with capitals and extra spaces ignored.
     */
    private static String sameName(String text)
    {
        return text.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    /*
     * courses(data): every course (name and zip) with how many sets of tees it has, sorted by name.
     * Passed in:  data - the golfer's data
     * Passed out: the list
     */
    public static List<CourseItem> courses(Dataset data)
    {
        Map<String, CourseItem> found = new LinkedHashMap<>();
        for (Tee tee : data.tees())
        {
            String key = sameName(tee.courseName()) + "|" + tee.zip().trim();
            CourseItem before = found.get(key);
            found.put(key, new CourseItem((before == null ? tee.courseName() : before.name()), tee.zip().trim(),
                    (before == null ? 1 : before.tees() + 1)));
        }
        List<CourseItem> sorted = new ArrayList<>(found.values());
        sorted.sort((first, second) -> first.name().compareToIgnoreCase(second.name()));
        return sorted;
    }

    /*
     +--------------------------------------------------------------------------+
     | plan(data, choice)                                                       |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Works out what a choice would write (see the class comment). Rounds are  |
     | taken in date order; sets of tees in file order. For DATES, both dates   |
     | are included and a missing end date means today.                         |
     |                                                                          |
     | Passed in:  data   - the golfer's data                                   |
     |             choice - what to export                                      |
     | Passed out: the Plan                                                     |
     +--------------------------------------------------------------------------+
     */
    public static Plan plan(Dataset data, Choice choice)
    {
        List<Round> rounds = new ArrayList<>();
        List<Tee> tees = new ArrayList<>();
        Set<Integer> teeIds = new HashSet<>();
        if (choice.kind() == Kind.COURSE)
        {
            for (Tee tee : data.tees())
            {
                if (sameName(tee.courseName()).equals(sameName(choice.courseName())) && tee.zip().trim().equals(choice.zip().trim()))
                {
                    teeIds.add(tee.id());
                }
            }
        }
        LocalDate last = (choice.to() == null ? LocalDate.now() : choice.to());
        for (Round round : data.rounds())
        {
            boolean wanted = choice.includeHistorical() || !round.historical();
            if (choice.kind() == Kind.COURSE)
            {
                wanted = wanted && teeIds.contains(round.teeId());
            }
            if (choice.kind() == Kind.DATES)
            {
                wanted = wanted && choice.from() != null && !round.date().isBefore(choice.from()) && !round.date().isAfter(last);
            }
            if (wanted)
            {
                rounds.add(round);
            }
        }
        rounds.sort((first, second) -> (first.date().equals(second.date()) ? Integer.compare(first.id(), second.id())
                : first.date().compareTo(second.date())));
        Set<Integer> used = new HashSet<>();
        for (Round round : rounds)
        {
            used.add(round.teeId());
        }
        for (Tee tee : data.tees())
        {
            boolean wanted = choice.kind() == Kind.EVERYTHING || (choice.kind() == Kind.COURSE ? teeIds.contains(tee.id()) : used.contains(tee.id()));
            if (wanted)
            {
                tees.add(tee);
            }
        }
        int historical = (int) rounds.stream().filter(Round::historical).count();
        return new Plan(tees, rounds, historical);
    }

    /*
     * write(dataDir, into, plan, when): writes the export folder with the normal file moves (see the next one).
     */
    public static Path write(Path dataDir, Path into, Plan plan, LocalDateTime when) throws IOException
    {
        return write(dataDir, into, plan, when, (from, to) -> Files.move(from, to, StandardCopyOption.ATOMIC_MOVE));
    }

    /*
     +--------------------------------------------------------------------------+
     | write(dataDir, into, plan, when, mover)                                  |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Makes the new folder GolfAnalyst-export_YYYY-MM-DD_HHMMSS (a number      |
     | after it if the name is taken) inside the chosen folder and writes       |
     | courses.tsv, rounds.tsv and ReadMe.txt there (see the class comment).    |
     | Each file goes under a temporary name first and is then moved into       |
     | place; if anything fails, the half-made folder is removed and the        |
     | failure is passed on. The golfer's own files are only read.              |
     |                                                                          |
     | Passed in:  dataDir - the golfer's data folder                           |
     |             into    - the folder to make the export folder in            |
     |             plan    - what to write (from plan)                          |
     |             when    - the date and time for the folder's name            |
     |             mover   - what moves a finished file into place              |
     | Passed out: the export folder made (throws IOException if there is       |
     |             nothing to write or it cannot be written)                    |
     +--------------------------------------------------------------------------+
     */
    static Path write(Path dataDir, Path into, Plan plan, LocalDateTime when, Mover mover) throws IOException
    {
        if (!plan.hasWork())
        {
            throw new IOException("there is nothing to write");
        }
        if (!Files.isDirectory(into))
        {
            throw new IOException(into + " is not a folder");
        }
        Set<Integer> teeIds = new HashSet<>();
        plan.tees().forEach(tee -> teeIds.add(tee.id()));
        Set<Integer> roundIds = new HashSet<>();
        plan.rounds().forEach(round -> roundIds.add(round.id()));
        String courses = chosenLines(dataDir.resolve(DataLoader.COURSES_FILE), CourseWriter.STANDARD_HEADER, "teeid", teeIds, false);
        String rounds = chosenLines(dataDir.resolve(DataLoader.ROUNDS_FILE), RoundWriter.STANDARD_HEADER, "roundid", roundIds,
                plan.historical() == 0);
        String readMe = "Golf Analyst export, made " + when.toLocalDate() + ".\r\n\r\nThis folder holds " + plan.counts()
                + (plan.historical() > 0 ? " (" + plan.historical() + " of them historical)" : "")
                + " in courses.tsv and rounds.tsv, in the same layout Golf Analyst uses.\r\n\r\n"
                + "To bring this into another copy: press Import, then Choose a whole data folder, and pick this folder.\r\n";

        String base = FOLDER_PREFIX + STAMP.format(when);
        Path folder = into.resolve(base);
        int count = 1;
        while (Files.exists(folder))
        {
            count = count + 1;
            folder = into.resolve(base + "-" + count);
        }
        Files.createDirectory(folder);
        try
        {
            Map<String, String> files = new LinkedHashMap<>();
            files.put(DataLoader.COURSES_FILE, courses);
            files.put(DataLoader.ROUNDS_FILE, rounds);
            files.put(README, readMe);
            for (Map.Entry<String, String> file : files.entrySet())
            {
                Path temporary = folder.resolve(file.getKey() + ".new");
                Files.writeString(temporary, file.getValue(), StandardCharsets.UTF_8);
                mover.move(temporary, folder.resolve(file.getKey()));
            }
        }
        catch (IOException | RuntimeException failure)
        {
            deleteTree(folder);
            throw (failure instanceof IOException ? (IOException) failure : new IOException(failure.getMessage(), failure));
        }
        return folder;
    }

    /*
     +--------------------------------------------------------------------------+
     | chosenLines(file, standardHeader, idColumn, ids, dropHistorical)         |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The text of one exported file: the file's own header row, then its own   |
     | lines for the chosen IDs, in file order, with the file's own line        |
     | endings. Comments and other lines are left out. When dropHistorical is   |
     | true the Historical column is taken out of the header and every line.    |
     | A file that is missing or has no header gives the standard header.       |
     |                                                                          |
     | Passed in:  file           - courses.tsv or rounds.tsv                   |
     |             standardHeader - the header to use when there is none        |
     |             idColumn       - "teeid" or "roundid"                        |
     |             ids            - the IDs to keep                             |
     |             dropHistorical - true to leave the Historical column out     |
     | Passed out: the text                                                     |
     +--------------------------------------------------------------------------+
     */
    private static String chosenLines(Path file, String standardHeader, String idColumn, Set<Integer> ids, boolean dropHistorical)
            throws IOException
    {
        String text = (Files.exists(file) ? Files.readString(file, StandardCharsets.UTF_8) : "");
        String newline = (text.contains("\r\n") ? "\r\n" : "\n");
        List<String> lines = LineReplacer.lines(text);
        int header = -1;
        for (int lp = 0; lp < lines.size() && header < 0; lp = lp + 1)
        {
            String line = lines.get(lp);
            header = (!line.isBlank() && !line.trim().startsWith("#") ? lp : header);
        }
        if (header < 0)
        {
            return standardHeader + newline;
        }
        String[] headerCells = lines.get(header).split("\t", -1);
        int idCol = -1;
        int historicalCol = -1;
        for (int col = 0; col < headerCells.length; col = col + 1)
        {
            String name = DataLoader.normalize(headerCells[col]);
            idCol = (name.equals(idColumn) && idCol < 0 ? col : idCol);
            historicalCol = (name.equals("historical") ? col : historicalCol);
        }
        int drop = (dropHistorical ? historicalCol : -1);
        StringBuilder result = new StringBuilder(withoutCell(lines.get(header), drop)).append(newline);
        for (int lp = header + 1; lp < lines.size(); lp = lp + 1)
        {
            String line = lines.get(lp);
            if (line.isBlank() || line.trim().startsWith("#"))
            {
                continue;
            }
            String[] cells = line.split("\t", -1);
            try
            {
                if (idCol >= 0 && idCol < cells.length && ids.contains(Integer.valueOf(cells[idCol].trim())))
                {
                    result.append(withoutCell(line, drop)).append(newline);
                }
            }
            catch (NumberFormatException notNumber)
            {
                // a line the loader could not read is never exported
            }
        }
        return result.toString();
    }

    /*
     * withoutCell(line, col): a tab-separated line with one cell taken out (col -1 leaves it as it is).
     */
    private static String withoutCell(String line, int col)
    {
        if (col < 0)
        {
            return line;
        }
        List<String> cells = new ArrayList<>(List.of(line.split("\t", -1)));
        if (col < cells.size())
        {
            cells.remove(col);
        }
        return String.join("\t", cells);
    }

    /*
     * deleteTree(folder): deletes a half-made export folder and everything in it.
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

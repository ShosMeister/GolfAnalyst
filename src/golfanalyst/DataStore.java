package golfanalyst;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/*
 +--------------------------------------------------------------------------+
 | DataStore                                                                |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | The one place a screen goes to for data. It loads the data folder,       |
 | keeps the handicap and the overall statistics calculated, and adds new   |
 | rounds. After a round is added everything is loaded and calculated       |
 | again from the files, so what is on screen is always what is in the      |
 | files. It knows nothing about windows, so it can be tested without       |
 | one.                                                                     |
 |                                                                          |
 | Also keeps the golfer's name and the "Include historical rounds" choice  |
 | in settings.txt in the data folder, and knows whether it is showing the  |
 | sample data rather than the golfer's own. That choice decides which      |
 | rounds overall() counts; the handicap never counts historical rounds.    |
 |                                                                          |
 | Constants:                                                               |
 | DATA_FOLDER   - the name of the golfer's own data folder ("data")        |
 | SAMPLE_FOLDER - the name of the sample data folder ("sample-data")       |
 |                                                                          |
 | Public Methods:                                                          |
 | reload()                  - reads the files again and recalculates       |
 | dataset(), handicap(), overall(), roundsByDate() - the loaded data       |
 | teeMap()                  - course/tees keyed by id                      |
 | nextRoundId()             - an unused round id                           |
 | addRound(round)           - adds a round to rounds.tsv and reloads       |
 | replaceRound(round)       - saves an edited round (backup first), reloads|
 | deleteRound(roundId)      - removes one round (backup first), reloads    |
 | clear(everything)         - Clear: backup folder, then header rows only  |
 | checkImport(...), applyImport(plan) - Import: check, then add            |
 | correctCourse(tees, name, zip, city, state) - Correct... a course        |
 | nextTeeId()               - an unused tee id                             |
 | teesForCourse(name, zip)  - the tees a course already has                |
 | addTees(tees)             - adds tees to courses.tsv and reloads         |
 | updateTees(changed, added) - saves edited and new tees, reloads          |
 | roundsOnTee(teeId)        - how many rounds were played on a tee         |
 | project(draft)            - the handicap line a round would get          |
 | golferName(), setGolferName(name)                                        |
 | includeHistorical(), setIncludeHistorical(flag) - the Home screen's box  |
 | isSampleData()            - whether this is the sample data folder       |
 | dataFolderBeside()        - where the golfer's own data folder goes      |
 | createEmptyFolder(dir)    - makes a data folder with header rows only    |
 +--------------------------------------------------------------------------+
 */
public final class DataStore
{

    public static final String DATA_FOLDER = "data";
    public static final String SAMPLE_FOLDER = "sample-data";
    private static final String SETTINGS_FILE = Settings.FILE;
    private final Path dir;
    private Dataset dataset;
    private Handicap.Result handicap;
    private Stats.Summary overall;
    private List<Round> roundsByDate = new ArrayList<>();
    private boolean includeHistorical = true;

    /*
     * DataStore(dir): opens a data folder and loads it.
     * Passed in:  dir - the folder holding courses.tsv and rounds.tsv
     * Passed out: the store (throws IOException if a file cannot be read)
     */
    public DataStore(Path dir) throws IOException
    {
        this.dir = dir;
        reload();
    }

    /*
     +--------------------------------------------------------------------------+
     | reload()                                                                 |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Reads courses.tsv, rounds.tsv and the "Include historical rounds"        |
     | setting again and recalculates the handicap and statistics. Called at    |
     | start-up and after every change.                                         |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing (throws IOException if a file cannot be read)        |
     +--------------------------------------------------------------------------+
     */
    public void reload() throws IOException
    {
        dataset = DataLoader.load(dir);
        Map<Integer, Tee> tees = dataset.teeMap();
        handicap = Handicap.compute(dataset.rounds(), tees);
        includeHistorical = Settings.load(dir).includeHistorical();
        overall = Stats.summarize(Stats.counted(dataset.rounds(), includeHistorical), tees);
        List<Round> sorted = new ArrayList<>(dataset.rounds());
        sorted.sort(Comparator.comparing(Round::date).thenComparingInt(Round::id));
        roundsByDate = sorted;
    }

    /*
     * dir(): the data folder this store reads and writes.
     */
    public Path dir()
    {
        return dir;
    }

    /*
     * dataset(): everything loaded from the files, with any problems found.
     */
    public Dataset dataset()
    {
        return dataset;
    }

    /*
     * handicap(): the replayed handicap history and current index.
     */
    public Handicap.Result handicap()
    {
        return handicap;
    }

    /*
     * overall(): statistics over the rounds counted: every round, or only those that are not historical when the Home screen's box is unticked.
     */
    public Stats.Summary overall()
    {
        return overall;
    }

    /*
     * roundsByDate(): every round, historical ones included, oldest first (ties broken by round id).
     */
    public List<Round> roundsByDate()
    {
        return roundsByDate;
    }

    /*
     * teeMap(): the course/tees keyed by id.
     */
    public Map<Integer, Tee> teeMap()
    {
        return dataset.teeMap();
    }

    /*
     * nextRoundId(): an id no round uses yet, one more than the highest.
     * Passed in:  nothing
     * Passed out: the new id (1 if there are no rounds)
     */
    public int nextRoundId()
    {
        int highest = 0;
        for (Round round : dataset.rounds())
        {
            highest = Math.max(highest, round.id());
        }
        return highest + 1;
    }

    /*
     +--------------------------------------------------------------------------+
     | addRound(round)                                                          |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Adds a round to rounds.tsv and then reloads and recalculates             |
     | everything.                                                              |
     |                                                                          |
     | Passed in:  round - the round to add; its TeeID must be a known tee      |
     | Passed out: nothing (throws IOException if the file cannot be written)   |
     +--------------------------------------------------------------------------+
     */
    public void addRound(Round round) throws IOException
    {
        Tee tee = teeMap().get(round.teeId());
        if (tee == null)
        {
            throw new IOException("TeeID " + round.teeId() + " is not in " + DataLoader.COURSES_FILE);
        }
        RoundWriter.append(dir.resolve(DataLoader.ROUNDS_FILE), round, tee);
        reload();
    }

    /*
     +--------------------------------------------------------------------------+
     | replaceRound(round)                                                      |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Saves an edited round: its line in rounds.tsv (the one with the same     |
     | RoundID) is replaced and nothing else in the file changes; the file as   |
     | it was is copied to the backups folder (see RoundWriter.replace). Then   |
     | everything is loaded and calculated again.                               |
     |                                                                          |
     | Passed in:  round - the edited round, with the id of the saved one       |
     | Passed out: nothing (throws IOException if the tee is unknown, the       |
     |             round is not in the file, or the file cannot be written)     |
     +--------------------------------------------------------------------------+
     */
    public void replaceRound(Round round) throws IOException
    {
        Tee tee = teeMap().get(round.teeId());
        if (tee == null)
        {
            throw new IOException("TeeID " + round.teeId() + " is not in " + DataLoader.COURSES_FILE);
        }
        RoundWriter.replace(dir.resolve(DataLoader.ROUNDS_FILE), round, tee);
        reload();
    }

    /*
     +--------------------------------------------------------------------------+
     | deleteRound(roundId)                                                     |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Removes one saved round: only its line goes from rounds.tsv (see         |
     | RoundWriter.delete), after a dated copy of the file is put in the        |
     | backups folder. Then everything is loaded and calculated again.          |
     |                                                                          |
     | Passed in:  roundId - the round to remove                                |
     | Passed out: the backup copy made; throws Backups.Failed if the copy      |
     |             cannot be made (nothing is removed), or IOException if the   |
     |             round is not in the file or the file cannot be written       |
     +--------------------------------------------------------------------------+
     */
    public Path deleteRound(int roundId) throws IOException
    {
        Path copy = RoundWriter.delete(dir.resolve(DataLoader.ROUNDS_FILE), roundId);
        reload();
        return copy;
    }

    /*
     +--------------------------------------------------------------------------+
     | clear(everything)                                                        |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | What Clear does. First courses.tsv, rounds.tsv and settings.txt are      |
     | copied into backups\clear-YYYY-MM-DD-HHMMSS (if that fails nothing is    |
     | cleared). Then rounds.tsv keeps only its header row; with everything,    |
     | courses.tsv keeps only its header row too and settings.txt is reset (no  |
     | name, Include historical rounds ticked again). Each file is written      |
     | through a temporary file, so none is ever left half written. Then        |
     | everything is loaded again. The sample data is never cleared.            |
     |                                                                          |
     | Passed in:  everything - false for rounds only (courses and the name     |
     |                          are kept), true for everything                  |
     | Passed out: the backup folder made; throws Backups.Failed if the copy    |
     |             cannot be made, or IOException for the sample data or if a   |
     |             file cannot be written                                       |
     +--------------------------------------------------------------------------+
     */
    public Path clear(boolean everything) throws IOException
    {
        if (isSampleData())
        {
            throw new IOException("This is the sample data, so it is never cleared.");
        }
        Path folder = Backups.backupFolder(dir, Backups.CLEAR, List.of(DataLoader.COURSES_FILE, DataLoader.ROUNDS_FILE, SETTINGS_FILE),
                java.time.LocalDateTime.now());
        Path rounds = dir.resolve(DataLoader.ROUNDS_FILE);
        LineReplacer.writeSafely(rounds, headerOnly(rounds, RoundWriter.STANDARD_HEADER));
        if (everything)
        {
            Path courses = dir.resolve(DataLoader.COURSES_FILE);
            LineReplacer.writeSafely(courses, headerOnly(courses, CourseWriter.STANDARD_HEADER));
            Path settings = dir.resolve(SETTINGS_FILE);
            if (Files.exists(settings))
            {
                LineReplacer.writeSafely(settings, "#Golf Analyst settings\n");
            }
        }
        reload();
        return folder;
    }


    /*
     * deleteCourse(tees): deletes a course, all its tees and every round on them (see CourseDeleter: a backup folder first, all or nothing), then reloads.
     * Passed in:  tees - every set of tees of the course
     * Passed out: the backup folder made (throws Backups.Failed or IOException; then nothing is deleted)
     */
    public Path deleteCourse(List<Tee> tees) throws IOException
    {
        Path folder = CourseDeleter.delete(dir, tees, dataset.rounds());
        reload();
        return folder;
    }

    /*
     +--------------------------------------------------------------------------+
     | correctCourse(tees, name, zip, city, state)                              |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Corrects a course's name and zip, with its city and state, on every one  |
     | of its tees (see CourseWriter.correctCourse: only those four cells       |
     | change, TeeIDs stay, a backup copy first), then reloads, so the Stats    |
     | Board, Best Round and the rounds show the new name.                      |
     |                                                                          |
     | Passed in:  tees - every tee of the course                               |
     |             name, zip, city, state - the corrected values                |
     | Passed out: the backup copy made (throws Backups.Failed or IOException;  |
     |             then nothing is written)                                     |
     +--------------------------------------------------------------------------+
     */
    public Path correctCourse(List<Tee> tees, String name, String zip, String city, String state) throws IOException
    {
        List<Integer> ids = new ArrayList<>();
        for (Tee tee : tees)
        {
            ids.add(tee.id());
        }
        Path copy = CourseWriter.correctCourse(dir.resolve(DataLoader.COURSES_FILE), ids, name, zip, city, state);
        reload();
        return copy;
    }

    /*
     * checkImport(coursesFile, roundsFile, markHistorical): what importing these files would do (see Importer.check); nothing is written.
     * Passed in:  coursesFile, roundsFile - the files, either may be null; markHistorical - true to mark the rounds historical
     * Passed out: the plan (throws IOException if a file cannot be read)
     */
    public Importer.Plan checkImport(Path coursesFile, Path roundsFile, boolean markHistorical) throws IOException
    {
        return Importer.check(coursesFile, roundsFile, dataset, markHistorical);
    }

    /*
     * applyImport(plan): does an import worked out by checkImport (backup folder first, then the new lines added at the end), then reloads everything.
     * Passed in:  plan - from checkImport
     * Passed out: the backup folder made (throws Backups.Failed or IOException; then nothing is written)
     */
    public Path applyImport(Importer.Plan plan) throws IOException
    {
        Path folder = Importer.apply(dir, plan);
        reload();
        return folder;
    }

    /*
     * headerOnly(file, standardHeader): the file's own header row (or the standard one if it has none) with its own line ending, and nothing else.
     * Passed in:  file - the data file, standardHeader - the header to use when the file has none
     * Passed out: the new text
     */
    private static String headerOnly(Path file, String standardHeader) throws IOException
    {
        String text = (Files.exists(file) ? Files.readString(file, StandardCharsets.UTF_8) : "");
        String[] header = RoundWriter.findHeader(text);
        String newline = (text.contains("\r\n") ? "\r\n" : "\n");
        return (header == null ? standardHeader : String.join("\t", header)) + newline;
    }

    /*
     * nextTeeId(): an id no course/tee uses yet, one more than the highest.
     * Passed in:  nothing
     * Passed out: the new id (1 if there are no tees)
     */
    public int nextTeeId()
    {
        int highest = 0;
        for (Tee tee : dataset.tees())
        {
            highest = Math.max(highest, tee.id());
        }
        return highest + 1;
    }

    /*
     * sameName(text): a course name made easy to compare: no extra spaces, no capitals.
     * Passed in:  text - the name
     * Passed out: for example "sample links" for "  Sample   LINKS "
     */
    private static String sameName(String text)
    {
        return text.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    /*
     +--------------------------------------------------------------------------+
     | teesForCourse(name, zip)                                                 |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Finds the tees a course already has. The name matches whatever the       |
     | capitals and extra spaces, and the zip must be the same, so two          |
     | courses with the same name in different towns are kept apart.            |
     |                                                                          |
     | Passed in:  name - the course name                                       |
     |             zip  - the 5-digit zip                                       |
     | Passed out: the tees, in file order (empty if the course is new)         |
     +--------------------------------------------------------------------------+
     */
    public List<Tee> teesForCourse(String name, String zip)
    {
        List<Tee> found = new ArrayList<>();
        String wanted = sameName(name);
        for (Tee tee : dataset.tees())
        {
            if (sameName(tee.courseName()).equals(wanted) && tee.zip().trim().equals(zip.trim()))
            {
                found.add(tee);
            }
        }
        return found;
    }

    /*
     +--------------------------------------------------------------------------+
     | addTees(tees)                                                            |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Adds tees to courses.tsv (see CourseWriter, which refuses a line that    |
     | would not load) and then reloads and recalculates everything.            |
     |                                                                          |
     | Passed in:  tees - the new tees, with ids from nextTeeId() upwards       |
     | Passed out: nothing (throws IOException if a tee is refused or the       |
     |             file cannot be written)                                      |
     +--------------------------------------------------------------------------+
     */
    public void addTees(List<Tee> tees) throws IOException
    {
        CourseWriter.append(dir.resolve(DataLoader.COURSES_FILE), tees);
        reload();
    }

    /*
     +--------------------------------------------------------------------------+
     | updateTees(changed, added)                                               |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Saves edited tees (their lines are replaced in place, the file as it     |
     | was is copied to the backups folder) and any new tees (added at the end),|
     | then reloads, so the handicap of rounds on an edited tee is worked out   |
     | again with its new numbers. With nothing changed it is simply addTees.   |
     |                                                                          |
     | Passed in:  changed - edited tees, with the TeeIDs they already have     |
     |             added   - new tees, with ids from nextTeeId() upwards        |
     | Passed out: nothing (throws IOException if a tee is refused or the       |
     |             file cannot be written)                                      |
     +--------------------------------------------------------------------------+
     */
    public void updateTees(List<Tee> changed, List<Tee> added) throws IOException
    {
        if (changed.isEmpty())
        {
            addTees(added);
            return;
        }
        CourseWriter.update(dir.resolve(DataLoader.COURSES_FILE), changed, added);
        reload();
    }

    /*
     * roundsOnTee(teeId): how many saved rounds were played on a tee.
     * Passed in:  teeId - the tee
     * Passed out: the count
     */
    public int roundsOnTee(int teeId)
    {
        int count = 0;
        for (Round round : dataset.rounds())
        {
            if (round.teeId() == teeId)
            {
                count = count + 1;
            }
        }
        return count;
    }

    /*
     +--------------------------------------------------------------------------+
     | project(draft)                                                           |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Works out what the handicap would show for a round that has not          |
     | been saved yet, by replaying the history with the round added. If a      |
     | saved round has the draft's id (a round being edited), the draft takes   |
     | its place instead of being counted twice.                                |
     |                                                                          |
     | Passed in:  draft - the round being entered                              |
     | Passed out: the history line for that round, or null if it cannot be     |
     |             used for the handicap (for example a nine with no rating)    |
     +--------------------------------------------------------------------------+
     */
    public Handicap.Entry project(Round draft)
    {
        List<Round> all = new ArrayList<>(dataset.rounds());
        all.removeIf(saved -> saved.id() == draft.id());
        all.add(draft);
        Handicap.Result result = Handicap.compute(all, teeMap());
        for (Handicap.Entry entry : result.entries())
        {
            if (entry.round().id() == draft.id())
            {
                return entry;
            }
        }
        return null;
    }

    /*
     * backupWarningNote(): after a save that made a backup, "  The extra backup copy could not be made: ..." when the copy to the BackupCopyFolder failed, otherwise "" (it is given only once).
     */
    public String backupWarningNote()
    {
        String warning = Backups.takeWarning();
        return (warning == null ? "" : "  " + warning);
    }

    /*
     * golferName(): reads the golfer's name from settings.txt.
     * Passed in:  nothing
     * Passed out: the name, or "" if none has been entered
     */
    public String golferName()
    {
        return Settings.load(dir).golfer();
    }

    /*
     * setGolferName(name): saves the golfer's name in settings.txt, keeping the other settings.
     * Passed in:  name - the name to keep
     * Passed out: nothing (throws IOException if the file cannot be written)
     */
    public void setGolferName(String name) throws IOException
    {
        Settings.load(dir).with(Settings.GOLFER, name).save(dir);
    }

    /*
     * includeHistorical(): the Home screen's "Include historical rounds" box, as read from settings.txt (ticked when the line is missing).
     */
    public boolean includeHistorical()
    {
        return includeHistorical;
    }

    /*
     +--------------------------------------------------------------------------+
     | setIncludeHistorical(flag)                                               |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Saves the "Include historical rounds" choice in settings.txt             |
     | (IncludeHistorical=true or false) and works the statistics out again     |
     | with it. The handicap does not change: it never counts historical        |
     | rounds.                                                                  |
     |                                                                          |
     | Passed in:  flag - true to count historical rounds in the statistics     |
     | Passed out: nothing (throws IOException if the file cannot be written)   |
     +--------------------------------------------------------------------------+
     */
    public void setIncludeHistorical(boolean flag) throws IOException
    {
        Settings.load(dir).with(Settings.INCLUDE_HISTORICAL, Boolean.toString(flag)).save(dir);
        includeHistorical = flag;
        overall = Stats.summarize(Stats.counted(dataset.rounds(), includeHistorical), dataset.teeMap());
    }

    /*
     * isSampleData(): whether this store is reading the sample data folder (it is named sample-data) rather than the golfer's own.
     * Passed in:  nothing
     * Passed out: true for the sample data
     */
    public boolean isSampleData()
    {
        Path name = dir.toAbsolutePath().normalize().getFileName();
        return name != null && name.toString().equalsIgnoreCase(SAMPLE_FOLDER);
    }

    /*
     * dataFolderBeside(): where the golfer's own data folder belongs: a folder named data next to this one.
     * Passed in:  nothing
     * Passed out: the path (it may not exist yet)
     */
    public Path dataFolderBeside()
    {
        return dir.toAbsolutePath().normalize().resolveSibling(DATA_FOLDER);
    }

    /*
     +--------------------------------------------------------------------------+
     | createEmptyFolder(folder)                                                |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Makes a data folder holding courses.tsv and rounds.tsv with just their   |
     | header rows. Nothing is copied into it. A file that is already there is  |
     | never written over.                                                      |
     |                                                                          |
     | Passed in:  folder - the folder to create                                |
     | Passed out: nothing (throws IOException if it cannot be made)            |
     +--------------------------------------------------------------------------+
     */
    public static void createEmptyFolder(Path folder) throws IOException
    {
        Files.createDirectories(folder);
        Path courses = folder.resolve(DataLoader.COURSES_FILE);
        if (!Files.exists(courses))
        {
            Files.writeString(courses, CourseWriter.STANDARD_HEADER + "\n", StandardCharsets.UTF_8);
        }
        Path rounds = folder.resolve(DataLoader.ROUNDS_FILE);
        if (!Files.exists(rounds))
        {
            Files.writeString(rounds, RoundWriter.STANDARD_HEADER + "\n", StandardCharsets.UTF_8);
        }
    }
}

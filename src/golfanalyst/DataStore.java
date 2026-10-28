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
 | Also keeps the golfer's name in settings.txt in the data folder.         |
 |                                                                          |
 | Public Methods:                                                          |
 | reload()                  - reads the files again and recalculates       |
 | dataset(), handicap(), overall(), roundsByDate() - the loaded data       |
 | teeMap()                  - course/tees keyed by id                      |
 | nextRoundId()             - an unused round id                           |
 | addRound(round)           - adds a round to rounds.tsv and reloads       |
 | project(draft)            - the handicap line a round would get          |
 | golferName(), setGolferName(name)                                        |
 +--------------------------------------------------------------------------+
 */
public final class DataStore
{

    private static final String SETTINGS_FILE = "settings.txt";
    private static final String GOLFER_KEY = "golfer";
    private final Path dir;
    private Dataset dataset;
    private Handicap.Result handicap;
    private Stats.Summary overall;
    private List<Round> roundsByDate = new ArrayList<>();

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
     | Reads courses.tsv and rounds.tsv again and recalculates the              |
     | handicap and statistics. Called at start-up and after every change.      |
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
        overall = Stats.summarize(dataset.rounds(), tees);
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
     * overall(): statistics over every round.
     */
    public Stats.Summary overall()
    {
        return overall;
    }

    /*
     * roundsByDate(): every round, oldest first (ties broken by round id).
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
     | project(draft)                                                           |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Works out what the handicap would show for a round that has not          |
     | been saved yet, by replaying the history with the round added. The       |
     | draft's id must not be used by any saved round (see nextRoundId).        |
     |                                                                          |
     | Passed in:  draft - the round being entered                              |
     | Passed out: the history line for that round, or null if it cannot be     |
     |             used for the handicap (for example a nine with no rating)    |
     +--------------------------------------------------------------------------+
     */
    public Handicap.Entry project(Round draft)
    {
        List<Round> all = new ArrayList<>(dataset.rounds());
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
     * golferName(): reads the golfer's name from settings.txt.
     * Passed in:  nothing
     * Passed out: the name, or "" if none has been entered
     */
    public String golferName()
    {
        Path file = dir.resolve(SETTINGS_FILE);
        if (!Files.exists(file))
        {
            return "";
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8))
        {
            Properties settings = new Properties();
            settings.load(reader);
            return settings.getProperty(GOLFER_KEY, "");
        }
        catch (IOException unreadable)
        {
            return "";
        }
    }

    /*
     * setGolferName(name): saves the golfer's name in settings.txt.
     * Passed in:  name - the name to keep
     * Passed out: nothing (throws IOException if the file cannot be written)
     */
    public void setGolferName(String name) throws IOException
    {
        Properties settings = new Properties();
        settings.setProperty(GOLFER_KEY, name);
        Files.createDirectories(dir);
        try (Writer writer = Files.newBufferedWriter(dir.resolve(SETTINGS_FILE), StandardCharsets.UTF_8))
        {
            settings.store(writer, "Golf Analyst settings");
        }
    }
}

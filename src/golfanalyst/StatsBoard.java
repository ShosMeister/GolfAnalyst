package golfanalyst;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/*
 +--------------------------------------------------------------------------+
 | StatsBoard                                                               |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | Works out the Stats Board: one row of statistics for every course and    |
 | tees played, kept apart for 18-hole and nine-hole rounds. 18-hole        |
 | rounds give one row per tee. Nine-hole rounds give one row per tee and   |
 | nine: "Course:Tee (Front 9)" and "Course:Tee (Back 9)" on an 18-hole     |
 | tee, and just "Course:Tee" on a nine-hole course. 18-hole and nine-hole  |
 | rounds are never mixed in one row. Two total rows give the same figures  |
 | over all 18-hole and all nine-hole rounds.                               |
 |                                                                          |
 | Each row uses Stats.summarize, so the definitions are exactly the Home   |
 | screen's: fairways over par 4s and 5s only, greens in regulation and     |
 | sand saves calculated from the score, putts and par, and every           |
 | percentage over every hole played in that row. Historical rounds are     |
 | left out, as they are on Home. A figure with nothing to divide by is     |
 | NaN, shown as "-".                                                       |
 |                                                                          |
 | There is no Swing code here, so all of it can be tested on its own.      |
 |                                                                          |
 | Nested types:                                                            |
 | Part   - what a row holds: 18 holes, a front or back nine, or the nine   |
 |          holes of a nine-hole course                                     |
 | Column - the ten columns, their headings and which way is better         |
 | Row    - one line of the board                                           |
 | Board  - the 18-hole rows, the nine-hole rows and the two total rows     |
 |                                                                          |
 | Public Methods:                                                          |
 | build(rounds, tees)       - works out the whole board                    |
 | sort(rows, column, up)    - the rows in a column's order, "-" last       |
 | best(rows)                - the cells to mark gold in one section        |
 | number(value)             - one decimal, a trailing ".0" dropped, or "-" |
 +--------------------------------------------------------------------------+
 */
public final class StatsBoard
{

    public static final String ALL_EIGHTEEN = "All 18-hole rounds";
    public static final String ALL_NINE = "All nine-hole rounds";
    public static final int MIN_ROUNDS_FOR_BEST = 2;
    public static final int MIN_ROWS_FOR_BEST = 2;
    private static final String MISSING = "-";

    /*
     * Part: what a row holds.
     */
    public enum Part
    {
        EIGHTEEN, FRONT, BACK, NINE_COURSE, TOTAL
    }

    /*
     +--------------------------------------------------------------------------+
     | Column                                                                   |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The board's columns, left to right, with their heading text. For the     |
     | eight number columns, lowerIsBetter says which end of the column the     |
     | gold best figure is at (rounds have no best).                            |
     +--------------------------------------------------------------------------+
     */
    public enum Column
    {
        COURSE("Course : Tee Box", false, false),
        RATING("Rating\\Slope", false, false),
        ROUNDS("Rnds", false, false),
        AVERAGE("Avg", true, true),
        BEST("Best", true, true),
        FAIRWAY("Fwy %", true, false),
        GIR("GIR %", true, false),
        TWO_PUTT("2-Putt %", true, false),
        PUTTS("Putts/Rnd", true, true),
        SAND("Sand %", true, false);

        private final String heading;
        private final boolean hasBest;
        private final boolean lowerIsBetter;

        /*
         * Column(heading, hasBest, lowerIsBetter): a column with its heading and, if it has a best figure, which way is better.
         */
        Column(String heading, boolean hasBest, boolean lowerIsBetter)
        {
            this.heading = heading;
            this.hasBest = hasBest;
            this.lowerIsBetter = lowerIsBetter;
        }

        /*
         * heading(): the text in the column's heading tag.
         */
        public String heading()
        {
            return heading;
        }

        /*
         * hasBest(): whether the column has a gold best figure.
         */
        public boolean hasBest()
        {
            return hasBest;
        }

        /*
         * lowerIsBetter(): true where the lowest figure is the best (Avg, Best, Putts/Rnd).
         */
        public boolean lowerIsBetter()
        {
            return lowerIsBetter;
        }
    }

    /*
     +--------------------------------------------------------------------------+
     | Row                                                                      |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | One line of the board. Every figure is NaN when there is nothing to      |
     | work it out from.                                                        |
     |                                                                          |
     | Fields:                                                                  |
     | label         - "Edgewater:Senior", "Edgewater:Senior (Front 9)", or     |
     |                 a total row's name                                       |
     | teeId, part   - the tees and which holes (teeId -1 for a total row)      |
     | rating, slope - the rating and slope of those holes (NaN and 0 if none)  |
     | rounds        - how many rounds are in the row                           |
     | average, best - average and lowest score                                 |
     | fairwayPct, girPct, twoPuttPct, sandPct - the percentages, 0 to 100      |
     | puttsPerRound - average putts per round                                  |
     +--------------------------------------------------------------------------+
     */
    public record Row(String label, int teeId, Part part, double rating, int slope, int rounds,
                      double average, double best, double fairwayPct, double girPct, double twoPuttPct,
                      double puttsPerRound, double sandPct)
    {

        /*
         * key(): what identifies the row whatever order it is in, for example "5:EIGHTEEN".
         */
        public String key()
        {
            return teeId + ":" + part;
        }

        /*
         +--------------------------------------------------------------------------+
         | value(column)                                                            |
         +--------------------------------------------------------------------------+
         | Purpose:                                                                 |
         | The row's figure in a number column (the rating for Rating\Slope), as    |
         | used for sorting and for the gold best figure.                           |
         |                                                                          |
         | Passed in:  column - any column but COURSE                               |
         | Passed out: the figure, or NaN for "-"                                   |
         +--------------------------------------------------------------------------+
         */
        public double value(Column column)
        {
            return switch (column)
            {
                case RATING -> rating;
                case ROUNDS -> rounds;
                case AVERAGE -> average;
                case BEST -> best;
                case FAIRWAY -> fairwayPct;
                case GIR -> girPct;
                case TWO_PUTT -> twoPuttPct;
                case PUTTS -> puttsPerRound;
                case SAND -> sandPct;
                default -> Double.NaN;
            };
        }

        /*
         +--------------------------------------------------------------------------+
         | text(column)                                                             |
         +--------------------------------------------------------------------------+
         | Purpose:                                                                 |
         | What the row shows in a column: the label, "70.4 / 123" (or "-" for a    |
         | nine with no rating, and nothing on a total row), the number of          |
         | rounds, or a figure to one decimal with a trailing ".0" dropped.         |
         |                                                                          |
         | Passed in:  column - the column                                          |
         | Passed out: the text                                                     |
         +--------------------------------------------------------------------------+
         */
        public String text(Column column)
        {
            if (column == Column.COURSE)
            {
                return label;
            }
            if (column == Column.RATING)
            {
                if (part == Part.TOTAL)
                {
                    return "";
                }
                return (Double.isNaN(rating) ? MISSING : String.format(Locale.ROOT, "%.1f / %d", rating, slope));
            }
            if (column == Column.ROUNDS)
            {
                return Integer.toString(rounds);
            }
            return number(value(column));
        }
    }

    /*
     * Board: the 18-hole rows, the nine-hole rows (each in course order) and the two total rows (allNine is null when there are no nine-hole rounds).
     */
    public record Board(List<Row> eighteen, List<Row> nine, Row allEighteen, Row allNine)
    {
    }

    /*
     * StatsBoard(): not used; everything is static.
     */
    private StatsBoard()
    {
    }

    /*
     +--------------------------------------------------------------------------+
     | build(rounds, tees)                                                      |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Groups the rounds that are not historical by tees and holes played,      |
     | works out each row and the two total rows, and returns the board with    |
     | each section in course order (Course : Tee Box ascending).               |
     |                                                                          |
     | Passed in:  rounds - every round (historical ones are left out here)     |
     |             tees   - course/tees keyed by id                             |
     | Passed out: the Board                                                    |
     +--------------------------------------------------------------------------+
     */
    public static Board build(List<Round> rounds, Map<Integer, Tee> tees)
    {
        Map<String, List<Round>> groups = new LinkedHashMap<>();
        Map<String, Part> groupPart = new LinkedHashMap<>();
        Map<String, Tee> groupTee = new LinkedHashMap<>();
        List<Round> allEighteen = new ArrayList<>();
        List<Round> allNine = new ArrayList<>();
        for (Round round : Stats.current(rounds))
        {
            Tee tee = tees.get(round.teeId());
            if (tee == null)
            {
                continue;
            }
            Part part = partOf(round, tee);
            String key = tee.id() + ":" + part;
            groups.computeIfAbsent(key, unused -> new ArrayList<>()).add(round);
            groupPart.put(key, part);
            groupTee.put(key, tee);
            if (part == Part.EIGHTEEN)
            {
                allEighteen.add(round);
            }
            else
            {
                allNine.add(round);
            }
        }
        List<Row> eighteenRows = new ArrayList<>();
        List<Row> nineRows = new ArrayList<>();
        for (Map.Entry<String, List<Round>> group : groups.entrySet())
        {
            Part part = groupPart.get(group.getKey());
            Tee tee = groupTee.get(group.getKey());
            Row row = makeRow(labelOf(tee, part), tee, part, group.getValue(), tees);
            if (part == Part.EIGHTEEN)
            {
                eighteenRows.add(row);
            }
            else
            {
                nineRows.add(row);
            }
        }
        Row eighteenTotal = makeRow(ALL_EIGHTEEN, null, Part.TOTAL, allEighteen, tees);
        Row nineTotal = (allNine.isEmpty() ? null : makeRow(ALL_NINE, null, Part.TOTAL, allNine, tees));
        return new Board(sort(eighteenRows, Column.COURSE, true), sort(nineRows, Column.COURSE, true), eighteenTotal, nineTotal);
    }

    /*
     * partOf(round, tee): which kind of row a round belongs in: 18 holes, the front or back nine of an 18-hole tee, or a nine-hole course.
     */
    private static Part partOf(Round round, Tee tee)
    {
        if (tee.holes() == Tee.NINE)
        {
            return Part.NINE_COURSE;
        }
        if (!round.isNineHole())
        {
            return Part.EIGHTEEN;
        }
        return (round.startHole() == 0 ? Part.FRONT : Part.BACK);
    }

    /*
     * labelOf(tee, part): the row's name, "Course:Tee", with " (Front 9)" or " (Back 9)" for a nine of an 18-hole tee.
     */
    private static String labelOf(Tee tee, Part part)
    {
        String name = tee.courseName() + ":" + tee.teeName();
        if (part == Part.FRONT)
        {
            return name + " (Front 9)";
        }
        if (part == Part.BACK)
        {
            return name + " (Back 9)";
        }
        return name;
    }

    /*
     +--------------------------------------------------------------------------+
     | makeRow(label, tee, part, rounds, tees)                                  |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Works out one row from its rounds with Stats.summarize. The average,     |
     | best and putts come from the 18-hole or the nine-hole side of the        |
     | summary, whichever the row holds. The rating and slope are the tee's     |
     | own for 18 holes or a nine-hole course, the nine's for a front or back   |
     | nine (NaN if that nine has none), and none for a total row.              |
     |                                                                          |
     | Passed in:  label  - the row's name                                      |
     |             tee    - the tees (null for a total row)                     |
     |             part   - which holes                                         |
     |             rounds - the rounds in the row (may be empty)                |
     |             tees   - course/tees keyed by id                             |
     | Passed out: the Row                                                      |
     +--------------------------------------------------------------------------+
     */
    private static Row makeRow(String label, Tee tee, Part part, List<Round> rounds, Map<Integer, Tee> tees)
    {
        Stats.Summary summary = Stats.summarize(rounds, tees);
        boolean nineHoles = part == Part.FRONT || part == Part.BACK || part == Part.NINE_COURSE
                || (part == Part.TOTAL && !rounds.isEmpty() && rounds.get(0).isNineHole());
        double rating = Double.NaN;
        int slope = 0;
        if (tee != null)
        {
            int startHole = (part == Part.BACK ? Tee.NINE : 0);
            int holesPlayed = (part == Part.FRONT || part == Part.BACK ? Tee.NINE : tee.holes());
            if (tee.isRated(startHole, holesPlayed))
            {
                rating = tee.ratingFor(startHole, holesPlayed);
                slope = tee.slopeFor(startHole, holesPlayed);
            }
        }
        int count = (nineHoles ? summary.rounds9() : summary.rounds18());
        double average = (nineHoles ? summary.avgScore9() : summary.avgScore18());
        int bestScore = (nineHoles ? summary.bestScore9() : summary.bestScore18());
        double putts = (nineHoles ? summary.avgPutts9() : summary.avgPutts18());
        return new Row(label, (tee == null ? -1 : tee.id()), part, rating, slope, count,
                (count == 0 ? Double.NaN : average), (count == 0 ? Double.NaN : bestScore),
                summary.fairwayPct(), summary.girPct(), summary.twoPuttPct(),
                (count == 0 ? Double.NaN : putts), summary.sandSavePct());
    }

    /*
     +--------------------------------------------------------------------------+
     | sort(rows, column, ascending)                                            |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Puts rows in a column's order. Course : Tee Box sorts by name (any       |
     | capitals), Rating\Slope by rating then slope, the rest by number. A      |
     | "-" always goes last, whichever way the column is sorted. Rows that      |
     | tie keep to course order.                                                |
     |                                                                          |
     | Passed in:  rows      - the rows of one section                          |
     |             column    - the column to sort by                            |
     |             ascending - true for smallest (or A) first                   |
     | Passed out: a new list in that order                                     |
     +--------------------------------------------------------------------------+
     */
    public static List<Row> sort(List<Row> rows, Column column, boolean ascending)
    {
        Comparator<Row> byName = Comparator.comparing(Row::label, String.CASE_INSENSITIVE_ORDER).thenComparing(Row::key);
        Comparator<Row> order = (first, second) ->
        {
            int result;
            if (column == Column.COURSE)
            {
                result = byName.compare(first, second);
                return (ascending ? result : -result);
            }
            double firstValue = first.value(column);
            double secondValue = second.value(column);
            boolean firstMissing = Double.isNaN(firstValue);
            boolean secondMissing = Double.isNaN(secondValue);
            if (firstMissing || secondMissing)
            {
                if (firstMissing && secondMissing)
                {
                    return byName.compare(first, second);
                }
                return (firstMissing ? 1 : -1);
            }
            result = Double.compare(firstValue, secondValue);
            if (result == 0 && column == Column.RATING)
            {
                result = Integer.compare(first.slope(), second.slope());
            }
            if (result == 0)
            {
                return byName.compare(first, second);
            }
            return (ascending ? result : -result);
        };
        List<Row> sorted = new ArrayList<>(rows);
        sorted.sort(order);
        return sorted;
    }

    /*
     +--------------------------------------------------------------------------+
     | best(rows)                                                               |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Finds the gold best figure of each number column in one section: the     |
     | lowest Avg, Best and Putts/Rnd, the highest Fwy %, GIR %, 2-Putt % and   |
     | Sand %. Only rows with at least 2 rounds count, and only when at least   |
     | 2 rows in the section have that many; otherwise nothing is gold. A "-"   |
     | is never the best. Figures that tie as shown (to one decimal) are all    |
     | gold.                                                                    |
     |                                                                          |
     | Passed in:  rows - the rows of one section                               |
     | Passed out: for each column, the keys of the rows whose cell is gold     |
     +--------------------------------------------------------------------------+
     */
    public static Map<Column, Set<String>> best(List<Row> rows)
    {
        Map<Column, Set<String>> gold = new EnumMap<>(Column.class);
        List<Row> qualifying = new ArrayList<>();
        for (Row row : rows)
        {
            if (row.rounds() >= MIN_ROUNDS_FOR_BEST)
            {
                qualifying.add(row);
            }
        }
        if (qualifying.size() < MIN_ROWS_FOR_BEST)
        {
            return gold;
        }
        for (Column column : Column.values())
        {
            if (!column.hasBest())
            {
                continue;
            }
            long target = 0;
            boolean found = false;
            for (Row row : qualifying)
            {
                double value = row.value(column);
                if (Double.isNaN(value))
                {
                    continue;
                }
                long shown = Math.round(value * 10);
                if (!found || (column.lowerIsBetter() ? shown < target : shown > target))
                {
                    target = shown;
                    found = true;
                }
            }
            if (!found)
            {
                continue;
            }
            Set<String> keys = new HashSet<>();
            for (Row row : qualifying)
            {
                double value = row.value(column);
                if (!Double.isNaN(value) && Math.round(value * 10) == target)
                {
                    keys.add(row.key());
                }
            }
            gold.put(column, keys);
        }
        return gold;
    }

    /*
     * number(value): a figure to one decimal with a trailing ".0" dropped ("43.8", "44"), or "-" for NaN.
     * Passed in:  value - the figure
     * Passed out: the text
     */
    public static String number(double value)
    {
        if (Double.isNaN(value))
        {
            return MISSING;
        }
        String text = String.format(Locale.ROOT, "%.1f", value);
        return (text.endsWith(".0") ? text.substring(0, text.length() - 2) : text);
    }
}

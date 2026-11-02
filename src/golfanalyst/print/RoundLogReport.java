package golfanalyst.print;

import golfanalyst.Handicap;
import golfanalyst.Round;
import golfanalyst.Stats;
import golfanalyst.StatsBoard;
import golfanalyst.Tee;
import java.awt.Font;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/*
 +--------------------------------------------------------------------------+
 | RoundLogReport                                                           |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | The Round Log (on its side): a list of rounds, oldest first, with their  |
 | figures, for one set of tees or all courses and for a period, then a     |
 | summary of each kind of round on the last page.                          |
 |                                                                          |
 | Columns for one set of tees: Date 120, Holes 90 (18, Front nine, Back    |
 | nine or 9), Score 80, Putts 80, Fairways 130 ("5 / 14", hit of the par   |
 | 4s and 5s), Greens 120 ("4 / 18"), 2-putts 110 ("16 / 18"), Sand saves   |
 | 110 ("0 / 2", or "-" with no bunker), Differential 100 (one decimal, "-" |
 | when the round has none) and 26 for a dagger (nine-hole round) or H      |
 | (historical round). For All courses a Course (tee) column 190 wide comes |
 | after Date and the others narrow (Date 110, Holes 70, Score 60, Putts    |
 | 60, Fairways 100, Greens 90, 2-putts 90, Sand saves 90, Differential     |
 | 90). The table starts at y 170 with rows 24 high.                        |
 |                                                                          |
 | The summary, for each kind of round present (18 holes, Front nine, Back  |
 | nine, Nine-hole course): a tan bold row "18 holes (13 rounds)", then     |
 | Average (score and putts to one decimal; fairways, greens, 2-putts and   |
 | sand saves as a percentage of all the chances of that kind; the          |
 | differentials of the rounds that have one) and Best (lowest score, putts |
 | and differential; highest percentages, sand saves among the rounds that  |
 | had a bunker). It comes 6 units under the rounds after a dark brown      |
 | rule, then a grey note explains the marks used.                          |
 |                                                                          |
 | Pages: as many rounds as fit on each page, the header row on every page, |
 | " (continued)" in the subtitle from page 2; the summary only on the last |
 | page and never split: if it does not fit under the last rounds it has a  |
 | page of its own.                                                         |
 |                                                                          |
 | Public Methods:                                                          |
 | the Report methods; rounds(); table(); summary(); note();                |
 | rowsPerPage(height); summaryAlone(height)                                |
 +--------------------------------------------------------------------------+
 */
public final class RoundLogReport extends Report
{

    public static final String NAME = "Round Log";
    public static final String ALL_COURSES = "All courses";
    public static final String DAGGER_PART = Sheet.DAGGER + " = a nine-hole round (its differential includes an expected score for "
            + "the other nine).";
    public static final String HISTORICAL_PART = "H = a historical round, not used for your handicap.";
    public static final String PERCENT_PART = "Percentages use every hole played.";
    static final double TABLE_TOP = 170;
    static final double ROW_HEIGHT = 24;
    static final double SUMMARY_GAP = 6;
    static final double SUMMARY_RULE = 2;
    static final double NOTE_GAP = 12;
    static final double NOTE_SIZE = 11;
    private static final StatsBoard.Part[] KINDS = {StatsBoard.Part.EIGHTEEN, StatsBoard.Part.FRONT, StatsBoard.Part.BACK,
        StatsBoard.Part.NINE_COURSE};
    private static final String[] KIND_WORDS = {"18 holes", "Front nine", "Back nine", "Nine-hole course"};
    private final Integer teeId;
    private final LocalDate from;
    private final LocalDate to;
    private final List<Round> rounds = new ArrayList<>();
    private final Map<Integer, Double> differentials = new HashMap<>();

    /*
     +--------------------------------------------------------------------------+
     | RoundLogReport(data, teeId, from, to, includeHistorical)                 |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Picks the rounds to list: on the tees (or every course), from the first  |
     | day to the last (both included; a missing end lets everything through    |
     | on that side), with historical rounds only if asked for.                 |
     |                                                                          |
     | Passed in:  data              - the golfer's data                        |
     |             teeId             - the tees, or null for all courses        |
     |             from, to          - the first and last day, or null          |
     |             includeHistorical - true to list historical rounds too       |
     | Passed out: the report                                                   |
     +--------------------------------------------------------------------------+
     */
    public RoundLogReport(ReportData data, Integer teeId, LocalDate from, LocalDate to, boolean includeHistorical)
    {
        super(data);
        this.teeId = teeId;
        this.from = from;
        this.to = to;
        for (Round round : Stats.counted(data.rounds(), includeHistorical))
        {
            boolean inside = data.tees().containsKey(round.teeId());
            if (teeId != null && round.teeId() != teeId)
            {
                inside = false;
            }
            if (from != null && round.date().isBefore(from))
            {
                inside = false;
            }
            if (to != null && round.date().isAfter(to))
            {
                inside = false;
            }
            if (inside)
            {
                rounds.add(round);
            }
        }
        for (Handicap.Entry entry : data.handicap().entries())
        {
            differentials.put(entry.round().id(), entry.differential());
        }
    }

    /*
     * rounds(): the rounds listed, oldest first.
     */
    public List<Round> rounds()
    {
        return rounds;
    }

    /*
     * differential(round): the round's differential, or NaN when it has none (historical, unrated, or a nine waiting for an index).
     */
    double differential(Round round)
    {
        Double value = differentials.get(round.id());
        if (value == null)
        {
            return Double.NaN;
        }
        return value;
    }

    /*
     * name(): "Round Log".
     */
    @Override
    public String name()
    {
        return NAME;
    }

    /*
     * landscape(): the Round Log is printed on its side.
     */
    @Override
    public boolean landscape()
    {
        return true;
    }

    /*
     * isEmpty(): true when no round matches the choices.
     */
    @Override
    public boolean isEmpty()
    {
        return rounds.isEmpty();
    }

    /*
     * describe(): "15 rounds" or "1 round".
     */
    @Override
    public String describe()
    {
        return countWords(rounds.size());
    }

    /*
     * countWords(count): "15 rounds" or "1 round".
     */
    static String countWords(int count)
    {
        if (count == 1)
        {
            return "1 round";
        }
        return count + " rounds";
    }

    /*
     * heading(): "Round Log".
     */
    @Override
    protected String heading()
    {
        return NAME;
    }

    /*
     * courseWords(): the tees ("Sample Links (White)") or "All courses".
     */
    private String courseWords()
    {
        if (teeId == null)
        {
            return ALL_COURSES;
        }
        Tee tee = data.tees().get(teeId);
        if (tee == null)
        {
            return ALL_COURSES;
        }
        return tee.label();
    }

    /*
     +--------------------------------------------------------------------------+
     | subtitle(page)                                                           |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | "Angelo   -   Sample Links (White): all 15 rounds", or "...: 12 rounds   |
     | from 2025-11-01 to 2026-10-31", or "...: 1 round"; from the second page  |
     | it ends with " (continued)".                                             |
     |                                                                          |
     | Passed in:  page - which page (0 is the first)                           |
     | Passed out: the line                                                     |
     +--------------------------------------------------------------------------+
     */
    @Override
    protected String subtitle(int page)
    {
        String words;
        if (rounds.size() == 1)
        {
            words = countWords(1);
        }
        else if (from == null && to == null)
        {
            words = "all " + countWords(rounds.size());
        }
        else
        {
            words = countWords(rounds.size());
        }
        if (from != null && to != null)
        {
            words = words + " from " + from + " to " + to;
        }
        else if (from != null)
        {
            words = words + " from " + from;
        }
        else if (to != null)
        {
            words = words + " up to " + to;
        }
        String line = named(courseWords() + ": " + words);
        if (page > 0)
        {
            line = line + " (continued)";
        }
        return line;
    }

    // ------------------------------------------------------------ the tables

    /*
     * columns(): the columns for one set of tees, or with Course (tee) for All courses (see the class comment).
     */
    List<Table.Column> columns()
    {
        List<Table.Column> columns = new ArrayList<>();
        if (teeId == null)
        {
            columns.add(new Table.Column("Date", 110, Table.Align.LEFT));
            columns.add(new Table.Column("Course (tee)", 190, Table.Align.LEFT));
            columns.add(new Table.Column("Holes", 70, Table.Align.CENTRE));
            columns.add(new Table.Column("Score", 60, Table.Align.CENTRE));
            columns.add(new Table.Column("Putts", 60, Table.Align.CENTRE));
            columns.add(new Table.Column("Fairways", 100, Table.Align.CENTRE));
            columns.add(new Table.Column("Greens", 90, Table.Align.CENTRE));
            columns.add(new Table.Column("2-putts", 90, Table.Align.CENTRE));
            columns.add(new Table.Column("Sand saves", 90, Table.Align.CENTRE));
            columns.add(new Table.Column("Differential", 90, Table.Align.RIGHT));
        }
        else
        {
            columns.add(new Table.Column("Date", 120, Table.Align.LEFT));
            columns.add(new Table.Column("Holes", 90, Table.Align.CENTRE));
            columns.add(new Table.Column("Score", 80, Table.Align.CENTRE));
            columns.add(new Table.Column("Putts", 80, Table.Align.CENTRE));
            columns.add(new Table.Column("Fairways", 130, Table.Align.CENTRE));
            columns.add(new Table.Column("Greens", 120, Table.Align.CENTRE));
            columns.add(new Table.Column("2-putts", 110, Table.Align.CENTRE));
            columns.add(new Table.Column("Sand saves", 110, Table.Align.CENTRE));
            columns.add(new Table.Column("Differential", 100, Table.Align.RIGHT));
        }
        columns.add(new Table.Column("", 26, Table.Align.CENTRE));
        return columns;
    }

    /*
     * cells(values): one row's cells in the columns' order, from words keyed by column heading ("" for a heading not given).
     */
    private List<String> cells(Map<String, String> values)
    {
        List<String> cells = new ArrayList<>();
        for (Table.Column column : columns())
        {
            cells.add(values.getOrDefault(column.title(), ""));
        }
        return cells;
    }

    /*
     * ofChances(part, whole): "5 / 14".
     */
    private static String ofChances(int part, int whole)
    {
        return part + " / " + whole;
    }

    /*
     +--------------------------------------------------------------------------+
     | table()                                                                  |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The rounds as a table, one row each, oldest first.                       |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: the table                                                    |
     +--------------------------------------------------------------------------+
     */
    public Table table()
    {
        Table table = new Table(columns(), ROW_HEIGHT);
        for (Round round : rounds)
        {
            Tee tee = data.tees().get(round.teeId());
            RoundFigures figures = RoundFigures.of(round, tee);
            Map<String, String> values = new HashMap<>();
            values.put("Date", round.date().toString());
            values.put("Course (tee)", tee.label());
            values.put("Holes", holesLong(round, tee));
            values.put("Score", Integer.toString(figures.score()));
            values.put("Putts", Integer.toString(figures.putts()));
            values.put("Fairways", ofChances(figures.fairways(), figures.fairwayChances()));
            values.put("Greens", ofChances(figures.greens(), figures.holes()));
            values.put("2-putts", ofChances(figures.twoPutts(), figures.holes()));
            if (figures.bunkers() == 0)
            {
                values.put("Sand saves", "-");
            }
            else
            {
                values.put("Sand saves", ofChances(figures.sandSaves(), figures.bunkers()));
            }
            values.put("Differential", oneDecimal(differential(round)));
            if (round.historical())
            {
                values.put("", "H");
            }
            else if (round.isNineHole())
            {
                values.put("", Sheet.DAGGER);
            }
            table.add(new Table.Row(cells(values), Table.Kind.NORMAL));
        }
        return table;
    }

    /*
     * percent(part, whole): "46.7 %", or "-" with nothing to divide by.
     */
    private static String percent(int part, int whole)
    {
        if (whole == 0)
        {
            return "-";
        }
        return String.format(Locale.ROOT, "%.1f %%", 100.0 * part / whole);
    }

    /*
     * bestPercent(value): the highest percentage found ("71.4 %"), or "-" when no round had a chance (value below 0).
     */
    private static String bestPercent(double value)
    {
        if (value < 0)
        {
            return "-";
        }
        return String.format(Locale.ROOT, "%.1f %%", value);
    }

    /*
     +--------------------------------------------------------------------------+
     | summary()                                                                |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The summary rows: for each kind of round present, in the order 18        |
     | holes, Front nine, Back nine, Nine-hole course, a group row "18 holes    |
     | (13 rounds)", an Average row and a Best row (see the class comment).     |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: the table (same columns as the rounds)                       |
     +--------------------------------------------------------------------------+
     */
    public Table summary()
    {
        Table table = new Table(columns(), ROW_HEIGHT);
        for (int kind = 0; kind < KINDS.length; kind = kind + 1)
        {
            List<Round> ofKind = new ArrayList<>();
            for (Round round : rounds)
            {
                if (StatsBoard.partOf(round, data.tees().get(round.teeId())) == KINDS[kind])
                {
                    ofKind.add(round);
                }
            }
            if (ofKind.isEmpty())
            {
                continue;
            }
            table.add(new Table.Row(List.of(KIND_WORDS[kind] + " (" + countWords(ofKind.size()) + ")"), Table.Kind.GROUP));
            addKindRows(table, ofKind);
        }
        return table;
    }

    /*
     +--------------------------------------------------------------------------+
     | addKindRows(table, ofKind)                                               |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Adds the Average and Best rows for the rounds of one kind.               |
     |                                                                          |
     | Passed in:  table  - the summary being built                             |
     |             ofKind - the rounds of that kind (at least one)              |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    private void addKindRows(Table table, List<Round> ofKind)
    {
        int score = 0;
        int putts = 0;
        int fairways = 0;
        int chances = 0;
        int greens = 0;
        int holes = 0;
        int twoPutts = 0;
        int bunkers = 0;
        int saves = 0;
        double differentialTotal = 0;
        int withDifferential = 0;
        int bestScore = Integer.MAX_VALUE;
        int bestPutts = Integer.MAX_VALUE;
        double bestFairways = -1;
        double bestGreens = -1;
        double bestTwoPutts = -1;
        double bestSand = -1;
        double bestDifferential = Double.NaN;
        for (Round round : ofKind)
        {
            RoundFigures figures = RoundFigures.of(round, data.tees().get(round.teeId()));
            score = score + figures.score();
            putts = putts + figures.putts();
            fairways = fairways + figures.fairways();
            chances = chances + figures.fairwayChances();
            greens = greens + figures.greens();
            holes = holes + figures.holes();
            twoPutts = twoPutts + figures.twoPutts();
            bunkers = bunkers + figures.bunkers();
            saves = saves + figures.sandSaves();
            bestScore = Math.min(bestScore, figures.score());
            bestPutts = Math.min(bestPutts, figures.putts());
            if (figures.fairwayChances() > 0)
            {
                bestFairways = Math.max(bestFairways, 100.0 * figures.fairways() / figures.fairwayChances());
            }
            bestGreens = Math.max(bestGreens, 100.0 * figures.greens() / figures.holes());
            bestTwoPutts = Math.max(bestTwoPutts, 100.0 * figures.twoPutts() / figures.holes());
            if (figures.bunkers() > 0)
            {
                bestSand = Math.max(bestSand, 100.0 * figures.sandSaves() / figures.bunkers());
            }
            double differential = differential(round);
            if (!Double.isNaN(differential))
            {
                differentialTotal = differentialTotal + differential;
                withDifferential = withDifferential + 1;
                if (Double.isNaN(bestDifferential) || differential < bestDifferential)
                {
                    bestDifferential = differential;
                }
            }
        }
        double averageDifferential = Double.NaN;
        if (withDifferential > 0)
        {
            averageDifferential = differentialTotal / withDifferential;
        }
        Map<String, String> average = new HashMap<>();
        average.put("Date", "Average");
        average.put("Score", oneDecimal((double) score / ofKind.size()));
        average.put("Putts", oneDecimal((double) putts / ofKind.size()));
        average.put("Fairways", percent(fairways, chances));
        average.put("Greens", percent(greens, holes));
        average.put("2-putts", percent(twoPutts, holes));
        average.put("Sand saves", percent(saves, bunkers));
        average.put("Differential", oneDecimal(averageDifferential));
        table.add(new Table.Row(cells(average), Table.Kind.STRIPE));
        Map<String, String> best = new HashMap<>();
        best.put("Date", "Best");
        best.put("Score", Integer.toString(bestScore));
        best.put("Putts", Integer.toString(bestPutts));
        best.put("Fairways", bestPercent(bestFairways));
        best.put("Greens", bestPercent(bestGreens));
        best.put("2-putts", bestPercent(bestTwoPutts));
        best.put("Sand saves", bestPercent(bestSand));
        best.put("Differential", oneDecimal(bestDifferential));
        table.add(new Table.Row(cells(best), Table.Kind.WHITE));
    }

    /*
     * note(): the grey note under the summary, with only the parts that apply (the dagger, H, and how percentages are worked out).
     */
    public String note()
    {
        boolean nine = false;
        boolean historical = false;
        for (Round round : rounds)
        {
            historical = historical || round.historical();
            nine = nine || (round.isNineHole() && !round.historical());
        }
        List<String> parts = new ArrayList<>();
        if (nine)
        {
            parts.add(DAGGER_PART);
        }
        if (historical)
        {
            parts.add(HISTORICAL_PART);
        }
        parts.add(PERCENT_PART);
        return String.join("   ", parts);
    }

    // ------------------------------------------------------------ pages

    /*
     * rowsPerPage(height): how many rounds fit on a page this high, under the header row and above the end rule (at least 1).
     */
    public int rowsPerPage(double height)
    {
        int fit = new Table(columns(), ROW_HEIGHT).rowsThatFit(bodyBottom(height) - TABLE_TOP);
        return Math.max(1, fit);
    }

    /*
     * summaryHeight(): the height of the summary block: the gap, the rule, its rows, its end rule and the note.
     */
    double summaryHeight()
    {
        return SUMMARY_GAP + SUMMARY_RULE + summary().rows().size() * ROW_HEIGHT + Table.END_RULE + NOTE_GAP
                + Sheet.lineHeight(Sheet.sans(Font.PLAIN, NOTE_SIZE));
    }

    /*
     * roundPages(height): how many pages the rounds take (at least one).
     */
    private int roundPages(double height)
    {
        int perPage = rowsPerPage(height);
        return Math.max(1, (rounds.size() + perPage - 1) / perPage);
    }

    /*
     +--------------------------------------------------------------------------+
     | summaryAlone(height)                                                     |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Whether the summary needs a page of its own: it does when it does not    |
     | fit under the rounds on their last page.                                 |
     |                                                                          |
     | Passed in:  height - the page height                                     |
     | Passed out: true for a page with only the summary                        |
     +--------------------------------------------------------------------------+
     */
    public boolean summaryAlone(double height)
    {
        int perPage = rowsPerPage(height);
        int onLast = rounds.size() - (roundPages(height) - 1) * perPage;
        double under = TABLE_TOP + Table.HEADER_HEIGHT + onLast * ROW_HEIGHT + Table.END_RULE;
        return under + summaryHeight() > bodyBottom(height);
    }

    /*
     * pageCount(height): the pages of rounds, and one more when the summary has a page of its own.
     */
    @Override
    public int pageCount(double height)
    {
        if (summaryAlone(height))
        {
            return roundPages(height) + 1;
        }
        return roundPages(height);
    }

    /*
     +--------------------------------------------------------------------------+
     | paintBody(sheet, page, pages)                                            |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Draws one page: the header row and this page's rounds, and on the last   |
     | page the summary and the note (after the rounds, or on its own page).    |
     |                                                                          |
     | Passed in:  sheet - the page                                             |
     |             page  - which page (0 is the first)                          |
     |             pages - how many pages there are                             |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    @Override
    protected void paintBody(Sheet sheet, int page, int pages)
    {
        Table table = table();
        int perPage = rowsPerPage(sheet.height());
        int first = Math.min(rounds.size(), page * perPage);
        int last = Math.min(rounds.size(), first + perPage);
        double below = table.drawHeader(sheet, Sheet.MARGIN, TABLE_TOP);
        if (last > first)
        {
            below = table.drawRows(sheet, Sheet.MARGIN, below, first, last);
            below = table.drawEndRule(sheet, Sheet.MARGIN, below);
        }
        if (page != pages - 1)
        {
            return;
        }
        Table summary = summary();
        if (last > first)
        {
            below = below + SUMMARY_GAP;
            sheet.rule(Sheet.MARGIN, below, summary.width(), SUMMARY_RULE, Sheet.BROWN);
            below = below + SUMMARY_RULE;
        }
        below = summary.drawRows(sheet, Sheet.MARGIN, below, 0, summary.rows().size());
        below = summary.drawEndRule(sheet, Sheet.MARGIN, below);
        Font noteFont = Sheet.sans(Font.PLAIN, NOTE_SIZE);
        sheet.text(Sheet.fit(note(), noteFont, sheet.width() - Sheet.MARGIN - Sheet.MARGIN), Sheet.MARGIN, below + NOTE_GAP, noteFont,
                Sheet.GREY);
    }
}

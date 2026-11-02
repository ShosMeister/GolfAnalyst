package golfanalyst.print;

import golfanalyst.BestRound;
import golfanalyst.CourseAnalysis;
import golfanalyst.Tee;
import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/*
 +--------------------------------------------------------------------------+
 | CourseNotesReport                                                        |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | Course notes (on its side): the Analyzer's figures (CourseAnalysis) for  |
 | one course and tees, using Home's Include historical rounds box. A card  |
 | in the scorecard style (label column 96, holes 42, Out, In and All;      |
 | rows 28 high so everything fits on A4 too): Hole (the three hardest      |
 | holes' numbers on (255,214,208), the three easiest on (255,236,150),     |
 | only with 2 or more rounds, never both, as on the Analyzer, and their    |
 | Avg vs par cells in the same tints), Par, Hdcp,                          |
 | Avg score, Avg vs par (a sign and one decimal; a whole number in the     |
 | totals, as on the Analyzer), Best, Worst, Avg putts, Fairway % and       |
 | Greens % ("-" for nothing to show). Under it a box like the Analyzer's   |
 | summary (Rounds, Avg score, Best score, Putts / round, Fairways, Greens, |
 | Sand saves, Hardest holes on red and Easiest holes on gold, each list in |
 | hole-number order) and a grey legend.                                    |
 |                                                                          |
 | Public Methods:                                                          |
 | the Report methods; analysis(), hardest(), easiest(), headings(),        |
 | rowTexts(), figures()                                                    |
 +--------------------------------------------------------------------------+
 */
public final class CourseNotesReport extends Report
{

    public static final String NAME = "Course Notes";
    public static final List<String> LABELS = List.of("Par", "Hdcp", "Avg score", "Avg vs par", "Best", "Worst", "Avg putts",
            "Fairway %", "Greens %");
    public static final String LEGEND = "Red = your hardest holes, where you lose the most strokes. Gold = your easiest.";
    static final int TINTED = 3;
    static final int AVG_VS_PAR_ROW = 3;
    static final int MIN_ROUNDS_TO_TINT = 2;
    static final double CARD_TOP = 172;
    static final double ROW_HEIGHT = 28;
    static final double BOX_GAP = 20;
    static final double BOX_HEIGHT = 200;
    static final double LEGEND_GAP = 10;
    private final BestRound.Choice row;
    private final CourseAnalysis.Analysis analysis;
    private final List<Integer> hardest = new ArrayList<>();
    private final List<Integer> easiest = new ArrayList<>();

    /*
     +--------------------------------------------------------------------------+
     | CourseNotesReport(data, row)                                             |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Works out the Analyzer's figures for the row and, with 2 or more         |
     | rounds, the hardest holes (hardestHoles(3)) and the easiest (the first   |
     | three of easiestHoles that are not among the hardest).                   |
     |                                                                          |
     | Passed in:  data - the golfer's data                                     |
     |             row  - an Analyzer row (null for none)                       |
     | Passed out: the report                                                   |
     +--------------------------------------------------------------------------+
     */
    public CourseNotesReport(ReportData data, BestRound.Choice row)
    {
        super(data);
        this.row = row;
        if (row == null)
        {
            this.analysis = null;
            return;
        }
        this.analysis = CourseAnalysis.analyse(row, data.rounds(), data.tees(), data.includeHistorical());
        if (analysis.rounds().size() >= MIN_ROUNDS_TO_TINT)
        {
            hardest.addAll(analysis.hardestHoles(TINTED));
            for (int hole : analysis.easiestHoles(analysis.holes().size()))
            {
                if (easiest.size() < TINTED && !hardest.contains(hole))
                {
                    easiest.add(hole);
                }
            }
        }
    }

    /*
     * analysis(): the Analyzer's figures, or null.
     */
    public CourseAnalysis.Analysis analysis()
    {
        return analysis;
    }

    /*
     * hardest(): the hardest holes, in hole-number order (none with fewer than 2 rounds).
     */
    public List<Integer> hardest()
    {
        List<Integer> sorted = new ArrayList<>(hardest);
        Collections.sort(sorted);
        return sorted;
    }

    /*
     * easiest(): the easiest holes, in hole-number order (none with fewer than 2 rounds).
     */
    public List<Integer> easiest()
    {
        List<Integer> sorted = new ArrayList<>(easiest);
        Collections.sort(sorted);
        return sorted;
    }

    /*
     * place(): "Sample Links (White)", with " Front 9" or " Back 9" for a nine of an 18-hole tee.
     */
    String place()
    {
        Tee tee = data.tees().get(row.teeId());
        String words = tee.label();
        if (row.part() == golfanalyst.StatsBoard.Part.FRONT)
        {
            words = words + " Front 9";
        }
        else if (row.part() == golfanalyst.StatsBoard.Part.BACK)
        {
            words = words + " Back 9";
        }
        return words;
    }

    /*
     * name(): "Course Notes".
     */
    @Override
    public String name()
    {
        return NAME;
    }

    /*
     * landscape(): printed on its side.
     */
    @Override
    public boolean landscape()
    {
        return true;
    }

    /*
     * isEmpty(): true with no row or no rounds on it.
     */
    @Override
    public boolean isEmpty()
    {
        return analysis == null || analysis.rounds().isEmpty() || !data.tees().containsKey(row.teeId());
    }

    /*
     * describe(): "one page: course notes for Sample Links (White) from 6 rounds".
     */
    @Override
    public String describe()
    {
        if (isEmpty())
        {
            return "nothing";
        }
        return "one page: course notes for " + place() + " from " + RoundLogReport.countWords(analysis.rounds().size());
    }

    /*
     * heading(): "Course Notes   -   Sample Links (White)".
     */
    @Override
    protected String heading()
    {
        if (isEmpty())
        {
            return NAME;
        }
        return NAME + SEPARATOR + place();
    }

    /*
     * subtitle(page): "Angelo   -   6 rounds".
     */
    @Override
    protected String subtitle(int page)
    {
        if (isEmpty())
        {
            return named(RoundLogReport.countWords(0));
        }
        return named(RoundLogReport.countWords(analysis.rounds().size()));
    }

    /*
     * pageCount(height): always one page.
     */
    @Override
    public int pageCount(double height)
    {
        return 1;
    }

    // ------------------------------------------------------------ the figures

    /*
     * totals(): Out and In (18-hole rows only), then All.
     */
    private List<CourseAnalysis.Totals> totals()
    {
        List<CourseAnalysis.Totals> totals = new ArrayList<>();
        if (analysis.out() != null)
        {
            totals.add(analysis.out());
            totals.add(analysis.in());
        }
        totals.add(analysis.all());
        return totals;
    }

    /*
     * headings(): the hole numbers, then Out, In and All.
     */
    public List<String> headings()
    {
        List<String> headings = new ArrayList<>();
        for (CourseAnalysis.HoleStats hole : analysis.holes())
        {
            headings.add(Integer.toString(hole.hole()));
        }
        for (CourseAnalysis.Totals total : totals())
        {
            headings.add(total.label());
        }
        return headings;
    }

    /*
     * decimal(value): one decimal, or "-" for nothing to show.
     */
    private static String decimal(double value)
    {
        return oneDecimal(value);
    }

    /*
     * whole(value): a whole number, or "-" for nothing to show (NaN) or none (0 for best and worst).
     */
    private static String whole(double value)
    {
        if (Double.isNaN(value) || value == 0)
        {
            return "-";
        }
        return Long.toString(Math.round(value));
    }

    /*
     * percent(value): a percentage as a whole number, or "-" for nothing to show.
     */
    private static String percent(double value)
    {
        if (Double.isNaN(value))
        {
            return "-";
        }
        return Long.toString(Math.round(value));
    }

    /*
     +--------------------------------------------------------------------------+
     | rowTexts()                                                               |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The words of the nine body rows for every hole, then Out, In and All     |
     | (no Hdcp for the totals).                                                |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: the rows, top to bottom                                      |
     +--------------------------------------------------------------------------+
     */
    public List<List<String>> rowTexts()
    {
        Tee tee = data.tees().get(row.teeId());
        List<List<String>> rows = new ArrayList<>();
        for (int line = 0; line < LABELS.size(); line = line + 1)
        {
            rows.add(new ArrayList<>());
        }
        for (CourseAnalysis.HoleStats hole : analysis.holes())
        {
            String overPar = "-";
            if (!Double.isNaN(hole.avgOverPar()))
            {
                overPar = String.format(Locale.ROOT, "%+.1f", hole.avgOverPar());
            }
            String[] words = {Integer.toString(hole.par()), Integer.toString(tee.strokeIndex()[hole.hole() - 1]), decimal(hole.avgScore()),
                overPar, whole(hole.best()), whole(hole.worst()), decimal(hole.avgPutts()), percent(hole.fairwayPct()),
                percent(hole.girPct())};
            for (int line = 0; line < words.length; line = line + 1)
            {
                rows.get(line).add(words[line]);
            }
        }
        for (CourseAnalysis.Totals total : totals())
        {
            String overPar = "-";
            if (!Double.isNaN(total.avgOverPar()))
            {
                overPar = String.format(Locale.ROOT, "%+d", Math.round(total.avgOverPar()));
            }
            String[] words = {Integer.toString(total.par()), "", decimal(total.avgScore()), overPar, whole(total.best()),
                whole(total.worst()), decimal(total.avgPutts()), percent(total.fairwayPct()), percent(total.girPct())};
            for (int line = 0; line < words.length; line = line + 1)
            {
                rows.get(line).add(words[line]);
            }
        }
        return rows;
    }

    /*
     +--------------------------------------------------------------------------+
     | spreadsheet()                                                            |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | One row per hole for a spreadsheet file: Hole, Par, Hdcp, Rounds, the    |
     | seven counts (Dbl Eagle to Other), Avg score, Avg vs par, Best, Worst,   |
     | Avg putts (one decimal, empty for nothing to show), Fairway % and        |
     | Greens % (one decimal).                                                  |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: the header row and one row per hole                          |
     +--------------------------------------------------------------------------+
     */
    @Override
    public List<List<String>> spreadsheet()
    {
        Tee tee = data.tees().get(row.teeId());
        List<List<String>> rows = new ArrayList<>();
        List<String> header = new ArrayList<>(List.of("Hole", "Par", "Hdcp", "Rounds"));
        header.addAll(CourseAnalysis.CATEGORIES);
        header.addAll(List.of("Avg score", "Avg vs par", "Best", "Worst", "Avg putts", "Fairway %", "Greens %"));
        rows.add(header);
        for (CourseAnalysis.HoleStats hole : analysis.holes())
        {
            List<String> cells = new ArrayList<>(List.of(Integer.toString(hole.hole()), Integer.toString(hole.par()),
                    Integer.toString(tee.strokeIndex()[hole.hole() - 1]), Integer.toString(hole.rounds())));
            for (int count : hole.counts())
            {
                cells.add(Integer.toString(count));
            }
            String best = "";
            String worst = "";
            if (hole.rounds() > 0)
            {
                best = Integer.toString(hole.best());
                worst = Integer.toString(hole.worst());
            }
            cells.addAll(List.of(Csv.number(hole.avgScore(), 1), Csv.number(hole.avgOverPar(), 1), best, worst,
                    Csv.number(hole.avgPutts(), 1), Csv.number(hole.fairwayPct(), 1), Csv.number(hole.girPct(), 1)));
            rows.add(cells);
        }
        return rows;
    }

    /*
     * spreadsheetWords(): "18 holes" or "9 holes".
     */
    @Override
    public String spreadsheetWords()
    {
        return analysis.holes().size() + " holes";
    }

    /*
     * holeList(holes): "5, 6, 11", or "-" for none.
     */
    static String holeList(List<Integer> holes)
    {
        if (holes.isEmpty())
        {
            return "-";
        }
        List<String> numbers = new ArrayList<>();
        for (int hole : holes)
        {
            numbers.add(Integer.toString(hole));
        }
        return String.join(", ", numbers);
    }

    /*
     * withPercent(value): "40%", or "-" for nothing to show.
     */
    private static String withPercent(double value)
    {
        if (Double.isNaN(value))
        {
            return "-";
        }
        return Math.round(value) + "%";
    }

    /*
     * figures(): the summary box's captions and values, as the Analyzer's summary frame shows them.
     */
    public List<String[]> figures()
    {
        CourseAnalysis.Summary summary = analysis.summary();
        List<String[]> pairs = new ArrayList<>();
        pairs.add(new String[] {"Rounds", Integer.toString(summary.rounds())});
        pairs.add(new String[] {"Avg score", decimal(summary.avgScore())});
        pairs.add(new String[] {"Best score", whole(summary.bestScore())});
        pairs.add(new String[] {"Putts / round", decimal(summary.puttsPerRound())});
        pairs.add(new String[] {"Fairways", withPercent(summary.fairwayPct())});
        pairs.add(new String[] {"Greens", withPercent(summary.girPct())});
        pairs.add(new String[] {"Sand saves", withPercent(summary.sandPct())});
        pairs.add(new String[] {"Hardest holes", holeList(hardest())});
        pairs.add(new String[] {"Easiest holes", holeList(easiest())});
        return pairs;
    }

    /*
     +--------------------------------------------------------------------------+
     | paintBody(sheet, page, pages)                                            |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The card with the tinted hole numbers, the summary box with the hole     |
     | lists on their tints, and the legend.                                    |
     |                                                                          |
     | Passed in:  sheet - the page; page, pages - always 0 and 1               |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    @Override
    protected void paintBody(Sheet sheet, int page, int pages)
    {
        if (isEmpty())
        {
            return;
        }
        List<String> headings = headings();
        List<Boolean> totals = new ArrayList<>();
        Map<Integer, Color> faces = new HashMap<>();
        for (int col = 0; col < headings.size(); col = col + 1)
        {
            boolean total = col >= analysis.holes().size();
            totals.add(total);
            if (!total)
            {
                int hole = analysis.holes().get(col).hole();
                if (hardest.contains(hole))
                {
                    faces.put(col, Sheet.HARD);
                }
                else if (easiest.contains(hole))
                {
                    faces.put(col, Sheet.EASY);
                }
            }
        }
        double below = Card.paint(sheet, CARD_TOP, LABELS, headings, totals, rowTexts(), ROW_HEIGHT, -1, faces, AVG_VS_PAR_ROW, null);
        List<Color> tints = new ArrayList<>();
        for (int lp = 0; lp < 7; lp = lp + 1)
        {
            tints.add(null);
        }
        tints.add(Sheet.HARD);
        tints.add(Sheet.EASY);
        double boxTop = below + BOX_GAP;
        Card.paintFigures(sheet, figures(), boxTop, BOX_HEIGHT, 3, 58, tints);
        Font legendFont = Sheet.sans(Font.PLAIN, 11);
        sheet.text(LEGEND, Sheet.MARGIN, boxTop + BOX_HEIGHT + LEGEND_GAP, legendFont, Sheet.GREY);
    }
}

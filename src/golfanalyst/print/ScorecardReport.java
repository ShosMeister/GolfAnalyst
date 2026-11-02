package golfanalyst.print;

import golfanalyst.BestRound;
import golfanalyst.Handicap;
import golfanalyst.Round;
import golfanalyst.Tee;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/*
 +--------------------------------------------------------------------------+
 | ScorecardReport                                                          |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | A scorecard on its side: one round hole by hole, with the score marks    |
 | and a box of the round's figures. Two reports use it: A round's          |
 | scorecard (a saved round) and Best Round (the best single round on a     |
 | course and tees, or the composite of the best score on every hole).      |
 |                                                                          |
 | The card at (60,172): a label column 96 wide (Hole, Par, Hdcp, Score,    |
 | Putts, Fairway, Sand in dark brown bold 13), then 42 wide columns for    |
 | the holes played, "Out" after hole 9 and "In" and "Total" at the end     |
 | (a nine has only "Total"). The header row is 34 high, the six body rows  |
 | 38 high with the zebra colours and light cell outlines; Out, In and      |
 | Total are shaded (240,236,228) and bold. Par and Putts have totals; the  |
 | Score row is 15 bold with totals; Hdcp, Fairway and Sand have none.      |
 | Score marks: a circle 30 across for a birdie or better, a square 28 for  |
 | a bogey, a double square (28 and 22) for a double bogey or worse.        |
 | Fairway: a check for a hit, "-" on a par 3. Sand: a                      |
 | check where the ball was in a bunker. A grey legend under it; 40 units   |
 | lower a rounded box 230 high with nine figures in three columns.         |
 |                                                                          |
 | Public Methods:                                                          |
 | forRound(data, round)              - a saved round's scorecard           |
 | forBest(data, choice, composite)   - Best Round                          |
 | the Report methods; round(), best(), composite(), markOf(score, par),    |
 | headings(), rowTexts(), figures()                                        |
 +--------------------------------------------------------------------------+
 */
public final class ScorecardReport extends Report
{

    public static final String ROUND_NAME = "Scorecard";
    public static final String BEST_NAME = "Best Round";
    public static final String OUT = "Out";
    public static final String IN = "In";
    public static final String TOTAL = "Total";
    public static final List<String> LABELS = List.of("Par", "Hdcp", "Score", "Putts", "Fairway", "Sand");
    public static final String LEGEND = "Circle = birdie or better.   Square = bogey.   Double square = double bogey or worse.   "
            + Sheet.CHECK + " in the Fairway row = fairway hit.   " + Sheet.CHECK + " in the Sand row = in a bunker.   - = par 3, no fairway.";
    static final double CARD_TOP = 172;
    static final double LABEL_WIDTH = 96;
    static final double HOLE_WIDTH = 42;
    static final double HEADER_HEIGHT = 34;
    static final double ROW_HEIGHT = 38;
    static final double LEGEND_GAP = 12;
    static final double BOX_GAP = 40;
    static final double BOX_HEIGHT = 230;
    static final double FIGURE_STEP = 340;
    static final java.awt.Color OUTLINE = Sheet.CELL_LINE;
    private static final int SCORE_ROW = 2;
    private final Round round;
    private final BestRound.Choice best;
    private final boolean composite;
    private final List<Round> roundsUsed;

    /*
     * Mark: the shape drawn round a score: none (par), a circle (birdie or better), a square (bogey), a double square (worse).
     */
    public enum Mark
    {
        NONE, CIRCLE, SQUARE, DOUBLE_SQUARE
    }

    /*
     +--------------------------------------------------------------------------+
     | ScorecardReport(data, round, best, composite, roundsUsed)                |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Keeps what to print; forRound and forBest make it.                       |
     |                                                                          |
     | Passed in:  data       - the golfer's data                               |
     |             round      - the round to show, or null for none             |
     |             best       - the Best Round choice, or null for a saved round|
     |             composite  - true for the composite                          |
     |             roundsUsed - the rounds Best Round chose from                |
     | Passed out: the report                                                   |
     +--------------------------------------------------------------------------+
     */
    private ScorecardReport(ReportData data, Round round, BestRound.Choice best, boolean composite, List<Round> roundsUsed)
    {
        super(data);
        this.round = round;
        this.best = best;
        this.composite = composite;
        this.roundsUsed = roundsUsed;
    }

    /*
     * forRound(data, round): the scorecard of one saved round (null for none).
     */
    public static ScorecardReport forRound(ReportData data, Round round)
    {
        return new ScorecardReport(data, round, null, false, List.of());
    }

    /*
     +--------------------------------------------------------------------------+
     | forBest(data, choice, composite)                                         |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Best Round for a course and tees, as the Best Round screen works it      |
     | out: the lowest round, or the composite of the best score on every       |
     | hole. Historical rounds follow Home's box.                               |
     |                                                                          |
     | Passed in:  data      - the golfer's data                                |
     |             choice    - the course and tees (a Best Round choice), or    |
     |                         null                                             |
     |             composite - true for the composite                           |
     | Passed out: the report                                                   |
     +--------------------------------------------------------------------------+
     */
    public static ScorecardReport forBest(ReportData data, BestRound.Choice choice, boolean composite)
    {
        if (choice == null)
        {
            return new ScorecardReport(data, null, null, composite, List.of());
        }
        List<Round> used = BestRound.roundsOf(choice, data.rounds(), data.tees(), data.includeHistorical());
        Round shown;
        if (used.isEmpty())
        {
            shown = null;
        }
        else if (composite)
        {
            shown = BestRound.compositeRound(choice, used);
        }
        else
        {
            shown = BestRound.bestSingle(used);
        }
        return new ScorecardReport(data, shown, choice, composite, used);
    }

    /*
     * round(): the round shown (the composite is an unsaved round), or null.
     */
    public Round round()
    {
        return round;
    }

    /*
     * best(): the Best Round choice, or null for a saved round's scorecard.
     */
    public BestRound.Choice best()
    {
        return best;
    }

    /*
     * composite(): true for Best Round's composite.
     */
    public boolean composite()
    {
        return composite;
    }

    /*
     * tee(): the tees of the round shown, or null.
     */
    Tee tee()
    {
        if (round == null)
        {
            return null;
        }
        return data.tees().get(round.teeId());
    }

    /*
     * name(): "Scorecard" or "Best Round".
     */
    @Override
    public String name()
    {
        if (best != null)
        {
            return BEST_NAME;
        }
        return ROUND_NAME;
    }

    /*
     * landscape(): scorecards are printed on their side.
     */
    @Override
    public boolean landscape()
    {
        return true;
    }

    /*
     * isEmpty(): true when there is no round to show.
     */
    @Override
    public boolean isEmpty()
    {
        return round == null || tee() == null;
    }

    /*
     * describe(): "one page: the scorecard of your round on 2026-10-07" or "one page: your best round on ...".
     */
    @Override
    public String describe()
    {
        if (isEmpty())
        {
            return "nothing";
        }
        if (best == null)
        {
            return "one page: the scorecard of your round on " + round.date();
        }
        if (composite)
        {
            return "one page: your best score on every hole of " + tee().label();
        }
        return "one page: your best round on " + tee().label();
    }

    /*
     * heading(): "Scorecard   -   Sample Links (Blue)" or "Best Round   -   Sample Links (Blue)" (with " (composite)").
     */
    @Override
    protected String heading()
    {
        if (isEmpty())
        {
            return name();
        }
        String words = name() + SEPARATOR + tee().label();
        if (composite)
        {
            words = words + " (composite)";
        }
        return words;
    }

    /*
     +--------------------------------------------------------------------------+
     | subtitle(page)                                                           |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | "Angelo   -   2026-10-07   -   round 00026" (and "   -   historical"),   |
     | or for a composite "Angelo   -   built from your 5 rounds".              |
     |                                                                          |
     | Passed in:  page - which page (always 0)                                 |
     | Passed out: the line                                                     |
     +--------------------------------------------------------------------------+
     */
    @Override
    protected String subtitle(int page)
    {
        if (isEmpty())
        {
            return named("");
        }
        if (composite)
        {
            return named("built from your " + RoundLogReport.countWords(roundsUsed.size()));
        }
        String words = round.date() + SEPARATOR + "round " + String.format("%05d", round.id());
        if (round.historical())
        {
            words = words + SEPARATOR + "historical";
        }
        return named(words);
    }

    /*
     * pageCount(height): a scorecard is always one page.
     */
    @Override
    public int pageCount(double height)
    {
        return 1;
    }

    // ------------------------------------------------------------ the card

    /*
     * markOf(score, par): the mark round a score: a circle for a birdie or better, a square for a bogey, a double square for worse.
     */
    public static Mark markOf(int score, int par)
    {
        int overPar = score - par;
        if (overPar <= -1)
        {
            return Mark.CIRCLE;
        }
        if (overPar == 1)
        {
            return Mark.SQUARE;
        }
        if (overPar >= 2)
        {
            return Mark.DOUBLE_SQUARE;
        }
        return Mark.NONE;
    }

    /*
     +--------------------------------------------------------------------------+
     | slots()                                                                  |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The card's columns after the label column: the index of each hole        |
     | played (0 is the first hole played), then -1 for Out, -2 for In and -3   |
     | for Total. 18 holes: 1-9, Out, 10-18, In, Total; nine holes: the nine    |
     | and Total.                                                               |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: the columns, left to right                                   |
     +--------------------------------------------------------------------------+
     */
    private List<Integer> slots()
    {
        List<Integer> slots = new ArrayList<>();
        for (int hole = 0; hole < round.holes(); hole = hole + 1)
        {
            slots.add(hole);
            if (round.holes() == Tee.HOLES && hole == Tee.NINE - 1)
            {
                slots.add(-1);
            }
        }
        if (round.holes() == Tee.HOLES)
        {
            slots.add(-2);
        }
        slots.add(-3);
        return slots;
    }

    /*
     * headings(): the header row after "Hole": the hole numbers played, Out, In and Total.
     */
    public List<String> headings()
    {
        List<String> headings = new ArrayList<>();
        for (int slot : slots())
        {
            headings.add(slotHeading(slot));
        }
        return headings;
    }

    /*
     * slotHeading(slot): a column's heading: its hole number, Out, In or Total.
     */
    private String slotHeading(int slot)
    {
        if (slot == -1)
        {
            return OUT;
        }
        if (slot == -2)
        {
            return IN;
        }
        if (slot == -3)
        {
            return TOTAL;
        }
        return Integer.toString(round.startHole() + slot + 1);
    }

    /*
     * sum(values, slot): the total of the values for Out (holes 1-9), In (10-18) or Total.
     */
    private int sum(int[] values, int slot)
    {
        int first = 0;
        int last = values.length;
        if (slot == -1)
        {
            last = Tee.NINE;
        }
        else if (slot == -2)
        {
            first = Tee.NINE;
        }
        int total = 0;
        for (int hole = first; hole < last; hole = hole + 1)
        {
            total = total + values[hole];
        }
        return total;
    }

    /*
     * parsPlayed(): the par of each hole played.
     */
    private int[] parsPlayed()
    {
        int[] pars = new int[round.holes()];
        for (int hole = 0; hole < round.holes(); hole = hole + 1)
        {
            pars[hole] = tee().pars()[round.startHole() + hole];
        }
        return pars;
    }

    /*
     +--------------------------------------------------------------------------+
     | rowTexts()                                                               |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The words in the six body rows (Par, Hdcp, Score, Putts, Fairway,        |
     | Sand), one per column of headings(): totals for Par, Score and Putts     |
     | in Out, In and Total, nothing there for the others; a check for a        |
     | fairway hit and "-" on a par 3; a check for a bunker.                    |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: the rows, top to bottom                                      |
     +--------------------------------------------------------------------------+
     */
    public List<List<String>> rowTexts()
    {
        int[] pars = parsPlayed();
        List<List<String>> rows = new ArrayList<>();
        for (int row = 0; row < LABELS.size(); row = row + 1)
        {
            List<String> texts = new ArrayList<>();
            for (int slot : slots())
            {
                texts.add(cellText(row, slot, pars));
            }
            rows.add(texts);
        }
        return rows;
    }

    /*
     * cellText(row, slot, pars): the words in one cell of the card (see rowTexts).
     */
    private String cellText(int row, int slot, int[] pars)
    {
        boolean total = slot < 0;
        switch (row)
        {
            case 0:
                if (total)
                {
                    return Integer.toString(sum(pars, slot));
                }
                return Integer.toString(pars[slot]);
            case 1:
                if (total)
                {
                    return "";
                }
                return Integer.toString(tee().strokeIndex()[round.startHole() + slot]);
            case 2:
                if (total)
                {
                    return Integer.toString(sum(round.scores(), slot));
                }
                return Integer.toString(round.scores()[slot]);
            case 3:
                if (total)
                {
                    return Integer.toString(sum(round.putts(), slot));
                }
                return Integer.toString(round.putts()[slot]);
            case 4:
                if (total)
                {
                    return "";
                }
                if (pars[slot] == 3)
                {
                    return "-";
                }
                if (round.fairway()[slot])
                {
                    return Sheet.CHECK;
                }
                return "";
            default:
                if (!total && round.sand()[slot])
                {
                    return Sheet.CHECK;
                }
                return "";
        }
    }

    /*
     * ofHoles(part, whole): "3 of 14".
     */
    private static String ofHoles(int part, int whole)
    {
        return part + " of " + whole;
    }

    /*
     +--------------------------------------------------------------------------+
     | figures()                                                                |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The nine captions and values of the figures box, in reading order:       |
     | Score ("88   (+16)"), Putts, Fairways ("3 of 14"), Greens in             |
     | regulation, Two putts or fewer, Sand saves ("1 of 3" or "-"), Rating /   |
     | slope, Differential and Index after this round ("-" for anything the     |
     | round does not have: a historical round or the composite has no          |
     | differential and no index after it).                                     |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: pairs of caption and value                                   |
     +--------------------------------------------------------------------------+
     */
    public List<String[]> figures()
    {
        RoundFigures figures = RoundFigures.of(round, tee());
        int overPar = figures.score() - figures.par();
        String toPar = "E";
        if (overPar > 0)
        {
            toPar = "+" + overPar;
        }
        else if (overPar < 0)
        {
            toPar = Integer.toString(overPar);
        }
        String sand = "-";
        if (figures.bunkers() > 0)
        {
            sand = ofHoles(figures.sandSaves(), figures.bunkers());
        }
        String rating = "-";
        int slope = tee().slopeFor(round.startHole(), round.holes());
        if (slope > 0)
        {
            rating = String.format(Locale.ROOT, "%.1f / %d", tee().ratingFor(round.startHole(), round.holes()), slope);
        }
        String differential = "-";
        String indexAfter = "-";
        for (Handicap.Entry entry : data.handicap().entries())
        {
            if (entry.round().id() == round.id() && round.id() != BestRound.COMPOSITE_ID)
            {
                differential = oneDecimal(entry.differential());
                if (entry.indexAfter() != null)
                {
                    indexAfter = Handicap.format(entry.indexAfter());
                }
            }
        }
        List<String[]> pairs = new ArrayList<>();
        pairs.add(new String[] {"Score", figures.score() + "   (" + toPar + ")"});
        pairs.add(new String[] {"Putts", Integer.toString(figures.putts())});
        pairs.add(new String[] {"Fairways", ofHoles(figures.fairways(), figures.fairwayChances())});
        pairs.add(new String[] {"Greens in regulation", ofHoles(figures.greens(), figures.holes())});
        pairs.add(new String[] {"Two putts or fewer", ofHoles(figures.twoPutts(), figures.holes())});
        pairs.add(new String[] {"Sand saves", sand});
        pairs.add(new String[] {"Rating / slope", rating});
        pairs.add(new String[] {"Differential", differential});
        pairs.add(new String[] {"Index after this round", indexAfter});
        return pairs;
    }

    /*
     * cardWidth(): the width of the card: the label column and a column for each heading.
     */
    double cardWidth()
    {
        return LABEL_WIDTH + headings().size() * HOLE_WIDTH;
    }

    /*
     * cardBottom(): the y under the card's last row.
     */
    static double cardBottom()
    {
        return CARD_TOP + HEADER_HEIGHT + LABELS.size() * ROW_HEIGHT;
    }

    /*
     +--------------------------------------------------------------------------+
     | paintBody(sheet, page, pages)                                            |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The card, the legend under it and the figures box.                       |
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
        List<Boolean> totals = new ArrayList<>();
        for (int slot : slots())
        {
            totals.add(slot < 0);
        }
        int[] pars = parsPlayed();
        List<Integer> slots = slots();
        Card.paint(sheet, CARD_TOP, LABELS, headings(), totals, rowTexts(), ROW_HEIGHT, SCORE_ROW, java.util.Map.of(),
                (onSheet, row, col, centreX, centreY) ->
                {
                    int hole = slots.get(col);
                    if (row == SCORE_ROW && hole >= 0)
                    {
                        paintMark(onSheet, markOf(round.scores()[hole], pars[hole]), centreX, centreY);
                    }
                });
        Font legendFont = Sheet.sans(Font.PLAIN, 11);
        double across = sheet.width() - Sheet.MARGIN - Sheet.MARGIN;
        double legendTop = cardBottom() + LEGEND_GAP;
        sheet.text(Sheet.fit(LEGEND, legendFont, across), Sheet.MARGIN, legendTop, legendFont, Sheet.GREY);
        double boxTop = Math.min(legendTop + Sheet.lineHeight(legendFont) + BOX_GAP, bodyBottom(sheet.height()) + 10 - BOX_HEIGHT);
        Card.paintFigures(sheet, figures(), boxTop, BOX_HEIGHT, 3, 66, null);
    }

    /*
     * paintMark(sheet, mark, centreX, centreY): the shape round a score, dark brown, 2 units thick.
     */
    static void paintMark(Sheet sheet, Mark mark, double centreX, double centreY)
    {
        if (mark == Mark.CIRCLE)
        {
            sheet.circle(centreX, centreY, 30, Sheet.BROWN, 2);
        }
        else if (mark == Mark.SQUARE)
        {
            sheet.square(centreX, centreY, 28, Sheet.BROWN, 2);
        }
        else if (mark == Mark.DOUBLE_SQUARE)
        {
            sheet.square(centreX, centreY, 28, Sheet.BROWN, 2);
            sheet.square(centreX, centreY, 22, Sheet.BROWN, 2);
        }
    }
}

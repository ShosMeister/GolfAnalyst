package golfanalyst;

import golfanalyst.StatsBoard.Part;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/*
 +--------------------------------------------------------------------------+
 | CourseAnalysis                                                           |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | The figures for the Analyzer (the screen is not built yet): hole by hole |
 | results for one course and tees. A row is of the Stats Board kind and    |
 | the same as Best Round's choices: an 18-hole tee, the front or back nine |
 | of an 18-hole tee, or a nine-hole course; kinds of round are never       |
 | mixed. The default row is the kind of the most recent round. Historical  |
 | rounds count only when includeHistorical is true (Home's box).           |
 |                                                                          |
 | For each hole (1-18, 1-9, or 10-18 for a back nine): the par; how many   |
 | rounds have a score there; the count in each of seven results against    |
 | par (CATEGORIES: Dbl Eagle is par minus 3 or better, Eagle minus 2,      |
 | Birdie minus 1, Par, Bogey plus 1, Dbl Bogey plus 2, Other plus 3 or     |
 | worse) and the same as exact percentages of the rounds on the hole; the  |
 | average score, the average over par, the best and worst score; the       |
 | average putts; fairway % (par 4 and 5 only, none on a par 3); greens in  |
 | regulation % (score minus putts no more than par minus 2); sand saves %  |
 | (none if never in the sand). These are Home's definitions (Stats).       |
 |                                                                          |
 | Totals for Out and In (18-hole rows only) and All: the counts added up,  |
 | the par and the same figures for those holes taken as one score per      |
 | round (average, best and worst totals, putts per round, percentages over |
 | the holes). A summary: rounds, putts per round, fairway, greens and sand |
 | saves % over every hole, average score and best score. hardestHoles(n)   |
 | and bestHoles(n) rank the holes by average strokes over par (ties go to  |
 | the lower hole number).                                                  |
 |                                                                          |
 | There is no Swing code here, so all of it can be tested on its own.      |
 | Nothing is stored: everything is worked out from score, putts, par and   |
 | the fairway and sand ticks.                                              |
 |                                                                          |
 | Nested types:                                                            |
 | HoleStats - one hole's figures                                           |
 | Totals    - Out, In or All                                               |
 | Summary   - the course summary                                           |
 | Analysis  - all of it for one row                                        |
 |                                                                          |
 | Public Methods:                                                          |
 | rows(rounds, tees, includeHistorical)        - the rows with a round     |
 | latestRow(rounds, tees, includeHistorical)   - the default row           |
 | analyse(row, rounds, tees, includeHistorical) - the figures for a row    |
 | category(overPar)                            - which of the seven results|
 +--------------------------------------------------------------------------+
 */
public final class CourseAnalysis
{

    public static final List<String> CATEGORIES = List.of("Dbl Eagle", "Eagle", "Birdie", "Par", "Bogey", "Dbl Bogey", "Other");
    public static final int DBL_EAGLE = 0;
    public static final int EAGLE = 1;
    public static final int BIRDIE = 2;
    public static final int PAR = 3;
    public static final int BOGEY = 4;
    public static final int DBL_BOGEY = 5;
    public static final int OTHER = 6;
    public static final String OUT = "Out";
    public static final String IN = "In";
    public static final String ALL = "All";

    /*
     +--------------------------------------------------------------------------+
     | HoleStats                                                                |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | One hole: its number and par, how many rounds have a score on it, the    |
     | counts and exact percentages of the seven results (in CATEGORIES         |
     | order), the average score and over par, the best and worst score (0      |
     | when there are none), the average putts and the fairway, greens and      |
     | sand saves percentages. A figure with nothing to divide by is NaN        |
     | (fairway on a par 3, sand saves when never in the sand, everything on a  |
     | hole with no rounds).                                                    |
     +--------------------------------------------------------------------------+
     */
    public record HoleStats(int hole, int par, int rounds, int[] counts, double[] percents, double avgScore, double avgOverPar,
                            int best, int worst, double avgPutts, double fairwayPct, double girPct, double sandPct)
    {
    }

    /*
     +--------------------------------------------------------------------------+
     | Totals                                                                   |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Out, In or All: the label, the first and last hole, the par, the number  |
     | of rounds, the seven counts added across the holes and their exact       |
     | percentages of every score counted, and for the holes taken as one       |
     | score per round: the average, the average over par, the best and worst   |
     | (0 when there are none) and the putts per round; then fairway, greens    |
     | and sand saves % over the holes (NaN with nothing to divide by).         |
     +--------------------------------------------------------------------------+
     */
    public record Totals(String label, int firstHole, int lastHole, int par, int rounds, int[] counts, double[] percents, double avgScore,
                         double avgOverPar, int best, int worst, double avgPutts, double fairwayPct, double girPct, double sandPct)
    {
    }

    /*
     +--------------------------------------------------------------------------+
     | Summary                                                                  |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The course summary: rounds, average putts per round, fairway, greens in  |
     | regulation and sand saves % over every hole in the row, the average      |
     | score and the best score (0 when there are no rounds; NaN for a figure   |
     | with nothing to divide by).                                              |
     +--------------------------------------------------------------------------+
     */
    public record Summary(int rounds, double puttsPerRound, double fairwayPct, double girPct, double sandPct, double avgScore,
                          int bestScore)
    {
    }

    /*
     +--------------------------------------------------------------------------+
     | Analysis                                                                 |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Everything for one row: the row, the rounds used, one HoleStats per      |
     | hole in hole order, Out and In (null unless the row is 18 holes), All    |
     | and the summary.                                                         |
     |                                                                          |
     | Public Methods:                                                          |
     | hardestHoles(n), bestHoles(n) - hole numbers ranked by average over par  |
     +--------------------------------------------------------------------------+
     */
    public record Analysis(BestRound.Choice row, List<Round> rounds, List<HoleStats> holes, Totals out, Totals in, Totals all,
                           Summary summary)
    {
        /*
         * hardestHoles(n): up to n hole numbers with the most strokes over par on average (ties: the lower hole number first).
         */
        public List<Integer> hardestHoles(int count)
        {
            return ranked(count, true);
        }

        /*
         * bestHoles(n): up to n hole numbers with the fewest strokes over par on average (ties: the lower hole number first).
         */
        public List<Integer> bestHoles(int count)
        {
            return ranked(count, false);
        }

        /*
         +--------------------------------------------------------------------------+
         | ranked(count, hardestFirst)                                              |
         +--------------------------------------------------------------------------+
         | Purpose:                                                                 |
         | Ranks the holes that have a score by average strokes over par, worst     |
         | or best first; a tie goes to the lower hole number.                      |
         |                                                                          |
         | Passed in:  count        - how many to give at most                      |
         |             hardestFirst - true for the hardest, false for the best      |
         | Passed out: the hole numbers                                             |
         +--------------------------------------------------------------------------+
         */
        private List<Integer> ranked(int count, boolean hardestFirst)
        {
            List<HoleStats> played = new ArrayList<>();
            for (HoleStats hole : holes)
            {
                if (hole.rounds() > 0)
                {
                    played.add(hole);
                }
            }
            played.sort((first, second) ->
            {
                int byScore = (hardestFirst ? Double.compare(second.avgOverPar(), first.avgOverPar())
                        : Double.compare(first.avgOverPar(), second.avgOverPar()));
                return (byScore != 0 ? byScore : Integer.compare(first.hole(), second.hole()));
            });
            List<Integer> numbers = new ArrayList<>();
            for (int lp = 0; lp < played.size() && lp < count; lp = lp + 1)
            {
                numbers.add(played.get(lp).hole());
            }
            return numbers;
        }
    }

    private CourseAnalysis()
    {
    }

    /*
     * rows(rounds, tees, includeHistorical): the rows that can be analysed (courses and tees with at least one round), in Best Round's order.
     */
    public static List<BestRound.Choice> rows(List<Round> rounds, Map<Integer, Tee> tees, boolean includeHistorical)
    {
        return BestRound.choices(rounds, tees, includeHistorical);
    }

    /*
     * latestRow(rounds, tees, includeHistorical): the default row, the kind of the most recently played round (null when there are no rounds).
     */
    public static BestRound.Choice latestRow(List<Round> rounds, Map<Integer, Tee> tees, boolean includeHistorical)
    {
        return BestRound.latestChoice(rounds, tees, includeHistorical);
    }

    /*
     * category(overPar): which of the seven results a score is, from its strokes over par (-1 is a birdie): DBL_EAGLE to OTHER.
     */
    public static int category(int overPar)
    {
        if (overPar <= -3)
        {
            return DBL_EAGLE;
        }
        if (overPar >= 3)
        {
            return OTHER;
        }
        return overPar + PAR;
    }

    /*
     +--------------------------------------------------------------------------+
     | analyse(row, rounds, tees, includeHistorical)                            |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Works out every figure for one row (see the class comment) from the      |
     | rounds of that kind on those tees.                                       |
     |                                                                          |
     | Passed in:  row               - the course, tees and kind (from rows)    |
     |             rounds            - all the golfer's rounds                  |
     |             tees              - every set of tees by TeeID               |
     |             includeHistorical - Home's Include historical rounds box     |
     | Passed out: the Analysis (with no rounds, every count is 0 and every     |
     |             average NaN)                                                 |
     +--------------------------------------------------------------------------+
     */
    public static Analysis analyse(BestRound.Choice row, List<Round> rounds, Map<Integer, Tee> tees, boolean includeHistorical)
    {
        Tee tee = tees.get(row.teeId());
        List<Round> used = BestRound.roundsOf(row, rounds, tees, includeHistorical);
        int firstIndex = (row.part() == Part.BACK ? Tee.NINE : 0);
        int holeCount = (row.part() == Part.EIGHTEEN ? Tee.HOLES : Tee.NINE);
        List<HoleStats> holes = new ArrayList<>();
        for (int place = 0; place < holeCount; place = place + 1)
        {
            holes.add(holeStats(firstIndex + place + 1, tee.pars()[firstIndex + place], place, used));
        }
        int firstHole = firstIndex + 1;
        int lastHole = firstIndex + holeCount;
        Totals out = null;
        Totals in = null;
        if (row.part() == Part.EIGHTEEN)
        {
            out = totals(OUT, 1, Tee.NINE, holes, used, firstHole);
            in = totals(IN, Tee.NINE + 1, Tee.HOLES, holes, used, firstHole);
        }
        Totals all = totals(ALL, firstHole, lastHole, holes, used, firstHole);
        Summary summary = new Summary(used.size(), all.avgPutts(), all.fairwayPct(), all.girPct(), all.sandPct(), all.avgScore(), all.best());
        return new Analysis(row, used, holes, out, in, all, summary);
    }

    /*
     +--------------------------------------------------------------------------+
     | holeStats(hole, par, place, rounds)                                      |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The figures for one hole from the rounds that have a score on it (a      |
     | score of 0 or less, or a round too short, is not counted).               |
     |                                                                          |
     | Passed in:  hole   - the hole number (1-18)                              |
     |             par    - its par                                             |
     |             place  - where the hole is in each round's lists (0 is the   |
     |                      first hole the round played)                        |
     |             rounds - the rounds of the row                               |
     | Passed out: the HoleStats                                                |
     +--------------------------------------------------------------------------+
     */
    private static HoleStats holeStats(int hole, int par, int place, List<Round> rounds)
    {
        int[] counts = new int[CATEGORIES.size()];
        int played = 0;
        int strokes = 0;
        int putts = 0;
        int best = 0;
        int worst = 0;
        int fairways = 0;
        int greens = 0;
        int sandShots = 0;
        int sandSaves = 0;
        for (Round round : rounds)
        {
            if (place >= round.scores().length || round.scores()[place] <= 0)
            {
                continue;
            }
            int score = round.scores()[place];
            int holePutts = round.putts()[place];
            played = played + 1;
            strokes = strokes + score;
            putts = putts + holePutts;
            best = (best == 0 || score < best ? score : best);
            worst = Math.max(worst, score);
            int category = category(score - par);
            counts[category] = counts[category] + 1;
            fairways = fairways + (round.fairway()[place] ? 1 : 0);
            greens = greens + (Stats.isGreenInRegulation(par, score, holePutts) ? 1 : 0);
            if (round.sand()[place])
            {
                sandShots = sandShots + 1;
                sandSaves = sandSaves + (Stats.isSandSave(par, score) ? 1 : 0);
            }
        }
        double[] percents = new double[counts.length];
        for (int lp = 0; lp < counts.length; lp = lp + 1)
        {
            percents[lp] = Stats.pct(counts[lp], played);
        }
        double avgScore = average(strokes, played);
        return new HoleStats(hole, par, played, counts, percents, avgScore, avgScore - par, best, worst, average(putts, played),
                (par >= 4 ? Stats.pct(fairways, played) : Double.NaN), Stats.pct(greens, played), Stats.pct(sandSaves, sandShots));
    }

    /*
     +--------------------------------------------------------------------------+
     | totals(label, from, to, holes, rounds, firstHole)                        |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Out, In or All for holes from to to: the hole counts added up, the par,  |
     | and each round's score and putts over those holes (only rounds with a    |
     | score there count toward the average, best and worst), then fairway      |
     | (par 4 and 5), greens and sand saves % over those holes.                 |
     |                                                                          |
     | Passed in:  label     - "Out", "In" or "All"                             |
     |             from, to  - the first and last hole number                   |
     |             holes     - the row's HoleStats                              |
     |             rounds    - the rounds of the row                            |
     |             firstHole - the hole number of each round's first place      |
     | Passed out: the Totals                                                   |
     +--------------------------------------------------------------------------+
     */
    private static Totals totals(String label, int from, int to, List<HoleStats> holes, List<Round> rounds, int firstHole)
    {
        int[] counts = new int[CATEGORIES.size()];
        int par = 0;
        int scored = 0;
        int fairwayChances = 0;
        int fairways = 0;
        int greens = 0;
        int sandShots = 0;
        int sandSaves = 0;
        for (HoleStats hole : holes)
        {
            if (hole.hole() < from || hole.hole() > to)
            {
                continue;
            }
            par = par + hole.par();
            for (int lp = 0; lp < counts.length; lp = lp + 1)
            {
                counts[lp] = counts[lp] + hole.counts()[lp];
                scored = scored + hole.counts()[lp];
            }
        }
        int roundCount = 0;
        int strokes = 0;
        int putts = 0;
        int best = 0;
        int worst = 0;
        for (Round round : rounds)
        {
            int roundStrokes = 0;
            int roundPutts = 0;
            for (HoleStats hole : holes)
            {
                int place = hole.hole() - firstHole;
                if (hole.hole() < from || hole.hole() > to || place >= round.scores().length || round.scores()[place] <= 0)
                {
                    continue;
                }
                int score = round.scores()[place];
                int holePutts = round.putts()[place];
                roundStrokes = roundStrokes + score;
                roundPutts = roundPutts + holePutts;
                if (hole.par() >= 4)
                {
                    fairwayChances = fairwayChances + 1;
                    fairways = fairways + (round.fairway()[place] ? 1 : 0);
                }
                greens = greens + (Stats.isGreenInRegulation(hole.par(), score, holePutts) ? 1 : 0);
                if (round.sand()[place])
                {
                    sandShots = sandShots + 1;
                    sandSaves = sandSaves + (Stats.isSandSave(hole.par(), score) ? 1 : 0);
                }
            }
            if (roundStrokes > 0)
            {
                roundCount = roundCount + 1;
                strokes = strokes + roundStrokes;
                putts = putts + roundPutts;
                best = (best == 0 || roundStrokes < best ? roundStrokes : best);
                worst = Math.max(worst, roundStrokes);
            }
        }
        double[] percents = new double[counts.length];
        for (int lp = 0; lp < counts.length; lp = lp + 1)
        {
            percents[lp] = Stats.pct(counts[lp], scored);
        }
        double avgScore = average(strokes, roundCount);
        return new Totals(label, from, to, par, roundCount, counts, percents, avgScore, avgScore - par, best, worst,
                average(putts, roundCount), Stats.pct(fairways, fairwayChances), Stats.pct(greens, scored), Stats.pct(sandSaves, sandShots));
    }

    /*
     * average(total, count): total / count, or NaN when count is 0.
     */
    private static double average(int total, int count)
    {
        return (count == 0 ? Double.NaN : (double) total / count);
    }
}

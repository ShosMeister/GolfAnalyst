package golfanalyst;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/*
 +--------------------------------------------------------------------------+
 | Stats                                                                    |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | Calculates the performance statistics shown on the Home, Stats and       |
 | Analyzer views, for any list of rounds (all rounds, one course/tee,      |
 | or the last five). 18-hole and 9-hole rounds are both counted.           |
 |                                                                          |
 | Definitions (percentages are 0-100; NaN means there was no data):        |
 | Fairways hit   - fairways hit divided by the par 4 and par 5 holes       |
 |                  played. Par 3s are not counted, so the number of        |
 |                  chances varies from course to course.                   |
 | Greens in reg. - greens in regulation divided by all holes played.       |
 |                  A green is in regulation when the strokes taken         |
 |                  before the first putt are no more than par minus 2      |
 |                  (score minus putts). It is calculated, never entered.   |
 | 2 putts or fewer - holes holed in two putts or fewer divided by all      |
 |                  holes played. Whatever is left over was 3+ putts.       |
 | Sand saves     - sand saves divided by sand shots. A hole with a         |
 |                  bunker shot is a save when it was finished in par or    |
 |                  better. It is calculated from the score.                |
 |                                                                          |
 | 9-hole and 18-hole rounds are kept apart wherever a total would be       |
 | misleading: average score, best score and average putts are given        |
 | separately for 18-hole rounds and for 9-hole rounds. Everything that     |
 | is measured per hole (the percentages, average score by par, and the     |
 | results against par) simply pools all the holes played.                  |
 |                                                                          |
 | Constants:                                                               |
 | DIST_LABELS - names for the result buckets, in order: eagle or better,   |
 |               birdie, par, bogey, double bogey, triple or worse          |
 |                                                                          |
 | Nested types:                                                            |
 | Summary - all of the statistics for one list of rounds                   |
 |                                                                          |
 | Public Methods:                                                          |
 | summarize(rounds, tees) - calculates a Summary                           |
 | current(rounds)         - the rounds that are not historical             |
 | pct(part, whole)        - safe percentage (NaN if whole is 0)            |
 | isGreenInRegulation(...), isSandSave(...) - the calculated rules         |
 +--------------------------------------------------------------------------+
 */
public final class Stats
{

    public static final String[] DIST_LABELS = {"Eagle or better", "Birdie", "Par", "Bogey", "Double bogey", "Triple or worse"};
    private static final int RESULT_BUCKETS = 6;
    private static final int PAR_TYPES = 3;
    private static final int FIRST_PAR_TYPE = 3;
    private static final int MIN_PAR_FOR_FAIRWAY = 4;
    private static final int MAX_PUTTS_FOR_TWO_PUTT = 2;

    private Stats()
    {
    }

    /*
     +--------------------------------------------------------------------------+
     | Summary                                                                  |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Every statistic for one list of rounds.                                  |
     |                                                                          |
     | Fields:                                                                  |
     | rounds18, rounds9      - number of 18-hole and 9-hole rounds counted     |
     | avgScore18, bestScore18, avgPutts18 - averages over 18-hole rounds       |
     | avgScore9, bestScore9, avgPutts9    - averages over 9-hole rounds        |
     |                          (best score is 0 when there were none)          |
     | fairwayHits, fairwayChances, fairwayPct - par 4/5 fairways               |
     | girPct         - greens in regulation %                                  |
     | twoPuttPct     - % of holes holed in two putts or fewer                  |
     | sandShots, sandSaves, sandSavePct - bunker play                          |
     | parAverage     - average score on par 3, par 4 and par 5 holes           |
     | distribution   - holes by result against par, in DIST_LABELS order       |
     |                                                                          |
     | Public Methods:                                                          |
     | rounds() - total rounds counted, 18-hole plus 9-hole                     |
     +--------------------------------------------------------------------------+
     */
    public record Summary(int rounds18, int rounds9,
                          double avgScore18, int bestScore18, double avgPutts18,
                          double avgScore9, int bestScore9, double avgPutts9,
                          int fairwayHits, int fairwayChances, double fairwayPct,
                          double girPct, double twoPuttPct,
                          int sandShots, int sandSaves, double sandSavePct,
                          double[] parAverage, int[] distribution)
    {

        /*
         * rounds(): total rounds counted.
         * Passed in:  nothing
         * Passed out: 18-hole rounds plus 9-hole rounds
         */
        public int rounds()
        {
            return rounds18 + rounds9;
        }
    }

    /*
     +--------------------------------------------------------------------------+
     | isGreenInRegulation(par, score, putts)                                   |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Decides whether a hole was a green in regulation, from the score and     |
     | the putts alone. The strokes before the first putt (score minus          |
     | putts) got the ball on the green, so it is in regulation when that       |
     | is no more than par minus 2. Because putts count only strokes taken      |
     | while the ball is on the green, a chip that finishes on the green        |
     | and a putt from the fringe are not mixed up. A hole holed from off       |
     | the green (0 putts) is a green in regulation only if it was holed        |
     | in par minus 2 strokes or fewer, which is how a hole in one is           |
     | treated; a chip-in for birdie is not.                                    |
     |                                                                          |
     | Passed in:  par   - par of the hole                                      |
     |             score - strokes taken on the hole                            |
     |             putts - putts taken on the hole (0 if holed from off         |
     |                     the green)                                           |
     | Passed out: true if the green was hit in regulation                      |
     +--------------------------------------------------------------------------+
     */
    public static boolean isGreenInRegulation(int par, int score, int putts)
    {
        return score - putts <= par - 2;
    }

    /*
     * isSandSave(par, score): decides whether a hole with a bunker shot was saved.
     * Passed in:  par   - par of the hole
     *             score - strokes taken on the hole
     * Passed out: true if the hole was finished in par or better
     */
    public static boolean isSandSave(int par, int score)
    {
        return score <= par;
    }

    /*
     * pct(part, whole): a percentage that does not divide by zero.
     * Passed in:  part  - the count of things that happened
     *             whole - the count of chances
     * Passed out: 100 x part / whole, or NaN when whole is 0
     */
    public static double pct(int part, int whole)
    {
        return whole == 0 ? Double.NaN : 100.0 * part / whole;
    }

    /*
     * average(total, count): an average that does not divide by zero.
     * Passed in:  total - the sum, count - how many were added
     * Passed out: total / count, or NaN when count is 0
     */
    private static double average(int total, int count)
    {
        return count == 0 ? Double.NaN : (double) total / count;
    }

    /*
     +--------------------------------------------------------------------------+
     | resultBucket(overPar)                                                    |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Sorts a hole result into one of the six DIST_LABELS buckets.             |
     |                                                                          |
     | Passed in:  overPar - score minus par (-1 is a birdie, 2 a double)       |
     | Passed out: bucket number 0-5                                            |
     +--------------------------------------------------------------------------+
     */
    public static int resultBucket(int overPar)
    {
        if (overPar <= -2)
        {
            return 0;
        }
        if (overPar >= 3)
        {
            return 5;
        }
        return overPar + 2;
    }

    /*
     * current(rounds): the rounds that are not historical, which are the ones the statistics are about.
     * Passed in:  rounds - any rounds
     * Passed out: a new list without the historical ones, in the same order
     */
    public static List<Round> current(List<Round> rounds)
    {
        List<Round> kept = new ArrayList<>();
        for (Round round : rounds)
        {
            if (!round.historical())
            {
                kept.add(round);
            }
        }
        return kept;
    }

    /*
     +--------------------------------------------------------------------------+
     | summarize(rounds, tees)                                                  |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Walks every hole of every round and totals up the statistics.            |
     | Rounds whose tee is unknown are skipped.                                 |
     |                                                                          |
     | Passed in:  rounds - the rounds to include                               |
     |             tees   - course/tees keyed by id (for each hole's par)       |
     | Passed out: a Summary of the statistics                                  |
     +--------------------------------------------------------------------------+
     */
    public static Summary summarize(List<Round> rounds, Map<Integer, Tee> tees)
    {
        int rounds18 = 0;
        int rounds9 = 0;
        int scoreSum18 = 0;
        int scoreSum9 = 0;
        int puttSum18 = 0;
        int puttSum9 = 0;
        int bestScore18 = Integer.MAX_VALUE;
        int bestScore9 = Integer.MAX_VALUE;
        int fairwayHits = 0;
        int fairwayChances = 0;
        int girHits = 0;
        int holesPlayed = 0;
        int twoPuttOrFewer = 0;
        int sandShots = 0;
        int sandSaves = 0;
        double[] parScoreSum = new double[PAR_TYPES];
        int[] parHoleCount = new int[PAR_TYPES];
        int[] resultCounts = new int[RESULT_BUCKETS];

        for (Round round : rounds)
        {
            Tee tee = tees.get(round.teeId());
            if (tee == null)
            {
                continue;
            }
            if (round.isNineHole())
            {
                rounds9 = rounds9 + 1;
                scoreSum9 = scoreSum9 + round.totalScore();
                puttSum9 = puttSum9 + round.totalPutts();
                bestScore9 = Math.min(bestScore9, round.totalScore());
            }
            else
            {
                rounds18 = rounds18 + 1;
                scoreSum18 = scoreSum18 + round.totalScore();
                puttSum18 = puttSum18 + round.totalPutts();
                bestScore18 = Math.min(bestScore18, round.totalScore());
            }
            for (int hole = 0; hole < round.holes(); hole = hole + 1)
            {
                int par = tee.pars()[round.startHole() + hole];
                int score = round.scores()[hole];
                holesPlayed = holesPlayed + 1;
                if (par >= MIN_PAR_FOR_FAIRWAY)
                {
                    fairwayChances = fairwayChances + 1;
                    if (round.fairway()[hole])
                    {
                        fairwayHits = fairwayHits + 1;
                    }
                }
                if (isGreenInRegulation(par, score, round.putts()[hole]))
                {
                    girHits = girHits + 1;
                }
                if (round.putts()[hole] <= MAX_PUTTS_FOR_TWO_PUTT)
                {
                    twoPuttOrFewer = twoPuttOrFewer + 1;
                }
                if (round.sand()[hole])
                {
                    sandShots = sandShots + 1;
                    if (isSandSave(par, score))
                    {
                        sandSaves = sandSaves + 1;
                    }
                }
                if (par < FIRST_PAR_TYPE + PAR_TYPES)
                {
                    parScoreSum[par - FIRST_PAR_TYPE] = parScoreSum[par - FIRST_PAR_TYPE] + score;
                    parHoleCount[par - FIRST_PAR_TYPE] = parHoleCount[par - FIRST_PAR_TYPE] + 1;
                }
                resultCounts[resultBucket(score - par)] = resultCounts[resultBucket(score - par)] + 1;
            }
        }
        double[] parAverage = new double[PAR_TYPES];
        for (int lp = 0; lp < PAR_TYPES; lp = lp + 1)
        {
            parAverage[lp] = parHoleCount[lp] == 0 ? Double.NaN : parScoreSum[lp] / parHoleCount[lp];
        }
        return new Summary(rounds18, rounds9,
                average(scoreSum18, rounds18), rounds18 == 0 ? 0 : bestScore18, average(puttSum18, rounds18),
                average(scoreSum9, rounds9), rounds9 == 0 ? 0 : bestScore9, average(puttSum9, rounds9),
                fairwayHits, fairwayChances, pct(fairwayHits, fairwayChances),
                pct(girHits, holesPlayed), pct(twoPuttOrFewer, holesPlayed),
                sandShots, sandSaves, pct(sandSaves, sandShots),
                parAverage, resultCounts);
    }
}

package golfanalyst;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/*
 +--------------------------------------------------------------------------+
 | Handicap                                                                 |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | Calculates a Handicap Index the way the USGA / World Handicap System     |
 | Rules of Handicapping do, replaying every round in date order exactly    |
 | as a handicap service would have posted them. Both 18-hole and 9-hole    |
 | rounds are handled.                                                      |
 |                                                                          |
 | Rules implemented (rule numbers are from the Rules of Handicapping):     |
 | 3.1  Maximum hole score: net double bogey (par + 2 + handicap strokes    |
 |      on that hole). Before an index exists the limit is par + 5.         |
 | 5.1a Score Differential = (113 / Slope) x (Adjusted Gross Score -        |
 |      Course Rating - PCC), rounded to the nearest tenth.                 |
 | 5.1b A 9-hole round gets a 9-hole Score Differential from the 9-hole     |
 |      Course Rating and Slope Rating (PCC counts half). It is turned      |
 |      into an 18-hole differential by adding the expected 9-hole          |
 |      differential for the Handicap Index on the day:                     |
 |      0.52 x index + 1.2.                                                 |
 | 5.2  Index = average of the lowest differentials among the most recent   |
 |      20 (8 of 20 when there are 20; a table with an adjustment when      |
 |      there are fewer). Maximum 54.0.                                     |
 | 4.5  An initial index needs 54 holes played, in any mix of 9-hole and    |
 |      18-hole rounds.                                                     |
 | 5.7  Low Handicap Index: the lowest index issued in the 365 days up to   |
 |      the latest round. It exists only once 20 scores are on record.      |
 | 5.8  Soft cap: any rise above the Low Index beyond 3.0 is halved.        |
 |      Hard cap: the index may not exceed the Low Index by more than 5.0.  |
 | 5.9  Exceptional Score Reduction: a differential 7.0 to 9.9 better than  |
 |      the index in effect takes 1.0 off the last 20 differentials; 10.0   |
 |      or better takes 2.0 off.                                            |
 | 6.1b A 9-hole Course Handicap, used for the net double bogey limit on a  |
 |      9-hole round: (index / 2, to a tenth) x (9-hole slope / 113) +      |
 |      (9-hole rating - 9-hole par). Handicap strokes go to the nine       |
 |      holes in order of their stroke index.                               |
 |                                                                          |
 | Before the first index exists (fewer than 54 holes posted) a 9-hole      |
 | round cannot be given an expected score. Such rounds are held as         |
 | "pending". When the 54th hole is posted, a provisional index is worked   |
 | out by adding pending nine-hole differentials together in pairs (the     |
 | method used before 2024), and that index then supplies the expected      |
 | score for every pending round. The USGA text describes the result but    |
 | not the exact steps, so treat early all-nine-hole records with care.     |
 |                                                                          |
 | Not covered: Playing Conditions Calculation values (enter the day's PCC  |
 | in rounds.tsv if you know it), 10-17 hole rounds, and manual Handicap    |
 | Committee adjustments.                                                   |
 |                                                                          |
 | Nested types:                                                            |
 | Cap     - which cap, if any, changed an index (NONE, SOFT, HARD)         |
 | Capped  - an index together with the cap that produced it                |
 | Entry   - the state of the handicap after one round was posted           |
 | Result  - the whole history plus the current scoring record              |
 | Posted  - (private) one differential in the scoring record               |
 |                                                                          |
 | Public Methods:                                                          |
 | compute(rounds, tees)              - replays all rounds (main entry)     |
 | indexFromDifferentials(recent)     - the 3-to-20 score table             |
 | differentialsUsed(count)           - how many lowest differentials count |
 | differential(...), nineHoleDifferential(...),                            |
 | expectedNineHoleDifferential(...)  - Rules 5.1a and 5.1b                 |
 | courseHandicap(...), nineHoleCourseHandicap(...),                        |
 | strokesReceived(...), maxHoleScores(...),                                |
 | adjustedGross(...)                 - net double bogey pieces             |
 | esrAdjustment(...), applyCaps(...) - Rules 5.9 and 5.8                   |
 | round1(value), format(index)       - number helpers                      |
 +--------------------------------------------------------------------------+
 */
public final class Handicap
{

    public static final double MAX_INDEX = 54.0;
    /** Rule 3.1: before an index exists the most a hole can count is par + 5. */
    public static final int NO_INDEX_OVER_PAR = 5;
    private static final int NET_DOUBLE_BOGEY_OVER_PAR = 2;
    private static final double STANDARD_SLOPE = 113.0;
    private static final int RECORD_SIZE = 20;
    private static final int INITIAL_HOLES = 54;
    private static final double EXPECTED_PER_INDEX_POINT = 0.52;
    private static final double EXPECTED_BASE = 1.2;
    private static final double ESR_SMALL_GAP = 7.0;
    private static final double ESR_LARGE_GAP = 10.0;
    private static final double SOFT_CAP_TRIGGER = 3.0;
    private static final double SOFT_CAP_KEEP_FRACTION = 0.5;
    private static final double HARD_CAP_LIMIT = 5.0;
    private static final int LOW_INDEX_DAYS = 365;
    private static final int MIN_SCORES = 3;

    private Handicap()
    {
    }

    /*
     +--------------------------------------------------------------------------+
     | Cap                                                                      |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Says which safeguard, if any, held an index back.                        |
     | NONE - index is the plain calculated value                               |
     | SOFT - rise above the Low Index was halved beyond 3.0                    |
     | HARD - index was stopped at 5.0 above the Low Index                      |
     +--------------------------------------------------------------------------+
     */
    public enum Cap
    {
        NONE, SOFT, HARD
    }

    /*
     +--------------------------------------------------------------------------+
     | Capped                                                                   |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The result of applying the soft and hard caps.                           |
     | Fields: index - the index after capping; cap - which cap applied         |
     +--------------------------------------------------------------------------+
     */
    public record Capped(double index, Cap cap)
    {
    }

    /*
     +--------------------------------------------------------------------------+
     | Entry                                                                    |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | One line of the handicap history: everything that happened when a        |
     | round was posted.                                                        |
     |                                                                          |
     | Fields:                                                                  |
     | round               - the round that was posted                          |
     | tee                 - the course/tee it was played on                    |
     | indexBefore         - index in effect before this round (null if none)   |
     | adjustedGross       - score after net double bogey limits (for the       |
     |                       holes played, so 9 holes gives a 9-hole score)     |
     | nineHoleDifferential - the 9-hole Score Differential (NaN for an         |
     |                       18-hole round)                                     |
     | differential        - the 18-hole Score Differential before any ESR      |
     |                       adjustment (NaN while a 9-hole round is pending)   |
     | esrAdjustment       - 0, -1.0 or -2.0 applied to the last 20             |
     | calculatedIndex     - index before caps (null if no index yet)           |
     | cap                 - cap that changed the index, if any                 |
     | indexAfter          - the index issued after this round (null if none)   |
     | scoresInRecord      - how many scores were on record after posting       |
     +--------------------------------------------------------------------------+
     */
    public record Entry(Round round, Tee tee, Double indexBefore, int adjustedGross,
                        double nineHoleDifferential, double differential, double esrAdjustment,
                        Double calculatedIndex, Cap cap, Double indexAfter, int scoresInRecord)
    {
    }

    /*
     +--------------------------------------------------------------------------+
     | Result                                                                   |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The outcome of replaying every round.                                    |
     |                                                                          |
     | Fields:                                                                  |
     | entries              - one Entry per round used, oldest first            |
     | currentDifferentials - the (up to) 20 differentials now on record,       |
     |                        oldest first, with ESR adjustments included       |
     | currentIndex         - the Handicap Index today (null if none yet)       |
     | lowIndex             - the Low Handicap Index (null until 20 scores)     |
     +--------------------------------------------------------------------------+
     */
    public record Result(List<Entry> entries, List<Double> currentDifferentials, Double currentIndex, Double lowIndex)
    {
    }

    /*
     +--------------------------------------------------------------------------+
     | Posted                                                                   |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | One round's place in the scoring record while the history is being       |
     | replayed. Its value changes if an Exceptional Score Reduction is         |
     | applied later, or when a pending 9-hole round is resolved.               |
     |                                                                          |
     | Fields:                                                                  |
     | entryIndex           - position of this round's Entry in the history     |
     | nineHoleDifferential - 9-hole differential (NaN for an 18-hole round)    |
     | value                - the 18-hole Score Differential (NaN if pending)   |
     | pending              - true while a 9-hole round waits for the           |
     |                        first Handicap Index to exist                     |
     +--------------------------------------------------------------------------+
     */
    private static final class Posted
    {
        final int entryIndex;
        final double nineHoleDifferential;
        double value;
        boolean pending;

        Posted(int entryIndex, double nineHoleDifferential, double value, boolean pending)
        {
            this.entryIndex = entryIndex;
            this.nineHoleDifferential = nineHoleDifferential;
            this.value = value;
            this.pending = pending;
        }
    }

    /*
     * round1(value): rounds to the nearest tenth, exact halves going up, as the Rules of Handicapping do.
     * Passed in:  value - any number
     * Passed out: the value to one decimal place
     */
    public static double round1(double value)
    {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    /*
     +--------------------------------------------------------------------------+
     | courseHandicap(index, rating, slope, par)                                |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Converts a Handicap Index into the number of strokes a player gets       |
     | over 18 holes on a particular set of tees:                               |
     |   index x (slope / 113) + (rating - par), rounded to a whole number.     |
     |                                                                          |
     | Passed in:  index  - the Handicap Index                                  |
     |             rating - Course Rating of the tees                           |
     |             slope  - Slope Rating of the tees                            |
     |             par    - total par of the course                             |
     | Passed out: the Course Handicap (negative for a plus handicap)           |
     +--------------------------------------------------------------------------+
     */
    public static int courseHandicap(double index, double rating, int slope, int par)
    {
        return (int) Math.floor(index * slope / STANDARD_SLOPE + (rating - par) + 0.5);
    }

    /*
     +--------------------------------------------------------------------------+
     | nineHoleCourseHandicap(index, rating, slope, par)                        |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Rule 6.1b: the number of strokes a player gets over nine holes.          |
     | Half the index (to the nearest tenth) is used, together with the         |
     | nine-hole rating, slope and par:                                         |
     |   (index / 2) x (slope / 113) + (rating - par), rounded to a whole       |
     |   number.                                                                |
     |                                                                          |
     | Passed in:  index  - the Handicap Index                                  |
     |             rating, slope - the 9-hole Course and Slope Ratings          |
     |             par    - par of the nine holes                               |
     | Passed out: the 9-hole Course Handicap                                   |
     +--------------------------------------------------------------------------+
     */
    public static int nineHoleCourseHandicap(double index, double rating, int slope, int par)
    {
        double halfIndex = round1(index / 2.0);
        return (int) Math.floor(halfIndex * slope / STANDARD_SLOPE + (rating - par) + 0.5);
    }

    /*
     +--------------------------------------------------------------------------+
     | strokesReceived(courseHandicap, rank, holeCount)                         |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Works out how many handicap strokes fall on one hole. Over 18 holes      |
     | a Course Handicap of 20 gives one stroke on every hole plus a            |
     | second on the two hardest (ranks 1 and 2). Over nine holes the same      |
     | idea is applied to nine holes. A plus handicap gives strokes back        |
     | on the easiest holes.                                                    |
     |                                                                          |
     | Passed in:  courseHandicap - the player's Course Handicap for the        |
     |                              number of holes being played                |
     |             rank           - the hole's difficulty rank among the        |
     |                              holes played, 1 = hardest                   |
     |             holeCount      - holes being played, 18 or 9                 |
     | Passed out: strokes received on the hole (negative = strokes given)      |
     +--------------------------------------------------------------------------+
     */
    public static int strokesReceived(int courseHandicap, int rank, int holeCount)
    {
        if (courseHandicap >= 0)
        {
            return courseHandicap / holeCount + (rank <= courseHandicap % holeCount ? 1 : 0);
        }
        int strokesGiven = -courseHandicap;
        return -(strokesGiven / holeCount + (rank > holeCount - strokesGiven % holeCount ? 1 : 0));
    }

    /*
     +--------------------------------------------------------------------------+
     | maxHoleScores(tee, startHole, holesPlayed, indexBefore)                  |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Finds the most a player may count on each hole for handicap              |
     | purposes: net double bogey, or par + 5 if no index exists yet.           |
     |                                                                          |
     | Passed in:  tee          - the course/tee being played                   |
     |             startHole    - first hole played, 0-based (0 or 9)           |
     |             holesPlayed  - 18 or 9                                       |
     |             indexBefore  - Handicap Index in effect, or null if none     |
     | Passed out: array with the maximum score for each hole played            |
     +--------------------------------------------------------------------------+
     */
    public static int[] maxHoleScores(Tee tee, int startHole, int holesPlayed, Double indexBefore)
    {
        int[] maxima = new int[holesPlayed];
        if (indexBefore == null)
        {
            for (int hole = 0; hole < holesPlayed; hole = hole + 1)
            {
                maxima[hole] = tee.pars()[startHole + hole] + NO_INDEX_OVER_PAR;
            }
            return maxima;
        }

        int courseHandicap;
        if (holesPlayed == Tee.HOLES)
        {
            courseHandicap = courseHandicap(indexBefore, tee.rating(), tee.slope(), tee.par());
        }
        else
        {
            courseHandicap = nineHoleCourseHandicap(indexBefore, tee.ratingFor(startHole, holesPlayed),
                    tee.slopeFor(startHole, holesPlayed), tee.parFor(startHole, holesPlayed));
        }
        int[] ranks = tee.strokeRanksFor(startHole, holesPlayed);
        for (int hole = 0; hole < holesPlayed; hole = hole + 1)
        {
            maxima[hole] = tee.pars()[startHole + hole] + NET_DOUBLE_BOGEY_OVER_PAR
                    + strokesReceived(courseHandicap, ranks[hole], holesPlayed);
        }
        return maxima;
    }

    /*
     +--------------------------------------------------------------------------+
     | adjustedGross(round, tee, indexBefore)                                   |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Adds up the round with every hole limited to its maximum hole            |
     | score. This is the Adjusted Gross Score used for handicapping.           |
     |                                                                          |
     | Passed in:  round        - the round played                              |
     |             tee          - the course/tee it was played on               |
     |             indexBefore  - index in effect when it was played, or null   |
     | Passed out: the Adjusted Gross Score for the holes played                |
     +--------------------------------------------------------------------------+
     */
    public static int adjustedGross(Round round, Tee tee, Double indexBefore)
    {
        int[] maxima = maxHoleScores(tee, round.startHole(), round.holes(), indexBefore);
        int total = 0;
        for (int hole = 0; hole < round.holes(); hole = hole + 1)
        {
            total = total + Math.min(round.scores()[hole], maxima[hole]);
        }
        return total;
    }

    /*
     +--------------------------------------------------------------------------+
     | differential(adjustedGross, rating, slope, pcc)                          |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Rule 5.1a: turns an 18-hole score into a Score Differential so           |
     | scores from different tees can be compared:                              |
     |   (113 / slope) x (adjusted gross - rating - pcc)                        |
     |                                                                          |
     | Passed in:  adjustedGross - Adjusted Gross Score                         |
     |             rating        - Course Rating                                |
     |             slope         - Slope Rating                                 |
     |             pcc           - Playing Conditions Calculation, normally 0   |
     | Passed out: the differential, to one decimal place                       |
     +--------------------------------------------------------------------------+
     */
    public static double differential(int adjustedGross, double rating, int slope, int pcc)
    {
        return round1(STANDARD_SLOPE / slope * (adjustedGross - rating - pcc));
    }

    /*
     +--------------------------------------------------------------------------+
     | nineHoleDifferential(adjustedGross, rating, slope, pcc)                  |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Rule 5.1b: the Score Differential for nine holes, using the 9-hole       |
     | Course Rating and Slope Rating. Only half of the Playing Conditions      |
     | Calculation applies:                                                     |
     |   (113 / slope) x (adjusted gross - rating - 0.5 x pcc)                  |
     |                                                                          |
     | Passed in:  adjustedGross - 9-hole Adjusted Gross Score                  |
     |             rating, slope - the 9-hole Course and Slope Ratings          |
     |             pcc           - Playing Conditions Calculation               |
     | Passed out: the 9-hole differential, to one decimal place                |
     +--------------------------------------------------------------------------+
     */
    public static double nineHoleDifferential(int adjustedGross, double rating, int slope, int pcc)
    {
        return round1(STANDARD_SLOPE / slope * (adjustedGross - rating - 0.5 * pcc));
    }

    /*
     +--------------------------------------------------------------------------+
     | expectedNineHoleDifferential(index)                                      |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Rule 5.1b: the Score Differential a player with this Handicap Index      |
     | is expected to produce over nine holes: 0.52 x index + 1.2. It is        |
     | added to a real 9-hole differential to make an 18-hole one. The          |
     | formula reproduces the USGA's published example (index 14.0 gives        |
     | 8.5, which is how 7.2 became 15.7).                                      |
     |                                                                          |
     | Passed in:  index - the Handicap Index on the day of the round           |
     | Passed out: the expected 9-hole differential, to one decimal place       |
     +--------------------------------------------------------------------------+
     */
    public static double expectedNineHoleDifferential(double index)
    {
        return round1(EXPECTED_PER_INDEX_POINT * index + EXPECTED_BASE);
    }

    /*
     +--------------------------------------------------------------------------+
     | esrAdjustment(indexBefore, differential)                                 |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Rule 5.9, Exceptional Score Reduction. Decides how much to take off      |
     | the last 20 differentials after an unusually good round.                 |
     |                                                                          |
     | Passed in:  indexBefore  - index in effect when the round was played     |
     |             differential - the round's 18-hole differential              |
     | Passed out: -2.0 if the differential beat the index by 10.0 or more,     |
     |             -1.0 if by 7.0 to 9.9, otherwise 0.0                         |
     +--------------------------------------------------------------------------+
     */
    public static double esrAdjustment(double indexBefore, double differential)
    {
        double betterBy = round1(indexBefore - differential);
        if (betterBy >= ESR_LARGE_GAP)
        {
            return -2.0;
        }
        if (betterBy >= ESR_SMALL_GAP)
        {
            return -1.0;
        }
        return 0.0;
    }

    /*
     +--------------------------------------------------------------------------+
     | differentialsUsed(scoreCount)                                            |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Says how many of the lowest differentials count toward the index         |
     | for a given number of scores on record (Rule 5.2 table).                 |
     |                                                                          |
     | Passed in:  scoreCount - scores on record, up to 20                      |
     | Passed out: 0 for fewer than 3 scores, then 1 (3-5 scores),              |
     |             2 (6-8), 3 (9-11), 4 (12-14), 5 (15-16), 6 (17-18),          |
     |             7 (19) and 8 (20)                                            |
     +--------------------------------------------------------------------------+
     */
    public static int differentialsUsed(int scoreCount)
    {
        if (scoreCount < MIN_SCORES)
        {
            return 0;
        }
        if (scoreCount <= 5)
        {
            return 1;
        }
        if (scoreCount <= 8)
        {
            return 2;
        }
        if (scoreCount <= 11)
        {
            return 3;
        }
        if (scoreCount <= 14)
        {
            return 4;
        }
        if (scoreCount <= 16)
        {
            return 5;
        }
        if (scoreCount <= 18)
        {
            return 6;
        }
        if (scoreCount == 19)
        {
            return 7;
        }
        return 8;
    }

    /*
     +--------------------------------------------------------------------------+
     | indexFromDifferentials(recent)                                           |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Rules 5.2a and 5.2b: averages the lowest differentials of the most       |
     | recent 20 scores. With 3, 4 or 6 scores a small adjustment of            |
     | -2.0, -1.0 or -1.0 is added. The result is limited to 54.0.              |
     | Caps are applied separately by applyCaps.                                |
     |                                                                          |
     | Passed in:  recent - differentials on record, oldest first               |
     | Passed out: the calculated index to one decimal, or null when there      |
     |             are fewer than 3 scores                                      |
     +--------------------------------------------------------------------------+
     */
    public static Double indexFromDifferentials(List<Double> recent)
    {
        int scoreCount = Math.min(recent.size(), RECORD_SIZE);
        if (scoreCount < MIN_SCORES)
        {
            return null;
        }
        List<Double> ranked = new ArrayList<>(recent.subList(recent.size() - scoreCount, recent.size()));
        ranked.sort(Comparator.naturalOrder());
        int countUsed = differentialsUsed(scoreCount);
        double adjustment = switch (scoreCount)
        {
            case 3 -> -2.0;
            case 4, 6 -> -1.0;
            default -> 0.0;
        };
        double total = 0;
        for (int lp = 0; lp < countUsed; lp = lp + 1)
        {
            total = total + ranked.get(lp);
        }
        return Math.min(round1(total / countUsed + adjustment), MAX_INDEX);
    }

    /*
     +--------------------------------------------------------------------------+
     | applyCaps(calculated, lowIndex)                                          |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Rule 5.8: limits how fast an index can rise. If the calculated           |
     | index is more than 3.0 above the Low Index, only half of the rise        |
     | beyond 3.0 is allowed (soft cap). The result may never be more than      |
     | 5.0 above the Low Index (hard cap). Falling indexes are never            |
     | capped.                                                                  |
     |                                                                          |
     | Passed in:  calculated - the index before caps                           |
     |             lowIndex   - the Low Handicap Index                          |
     | Passed out: a Capped holding the final index and which cap applied       |
     +--------------------------------------------------------------------------+
     */
    public static Capped applyCaps(double calculated, double lowIndex)
    {
        if (calculated - lowIndex <= SOFT_CAP_TRIGGER)
        {
            return new Capped(calculated, Cap.NONE);
        }
        double softCapped = round1(lowIndex + SOFT_CAP_TRIGGER
                + SOFT_CAP_KEEP_FRACTION * (calculated - lowIndex - SOFT_CAP_TRIGGER));
        if (softCapped - lowIndex > HARD_CAP_LIMIT)
        {
            return new Capped(round1(lowIndex + HARD_CAP_LIMIT), Cap.HARD);
        }
        return new Capped(softCapped, Cap.SOFT);
    }

    /*
     * format(index): prepares an index for display; plus handicaps (better than scratch) get a leading +.
     * Passed in:  index - the index, negative for a plus handicap, or null
     * Passed out: text like "14.3", "+2.1" or "n/a"
     */
    public static String format(Double index)
    {
        if (index == null)
        {
            return "n/a";
        }
        return index < 0 ? String.format("+%.1f", -index) : String.format("%.1f", index);
    }

    /*
     +--------------------------------------------------------------------------+
     | compute(allRounds, tees)                                                 |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Replays every round oldest to newest. For each round it finds the        |
     | adjusted gross score using the index in effect at the time, turns        |
     | it into an 18-hole differential (adding the expected score for a         |
     | 9-hole round), applies any Exceptional Score Reduction to the last       |
     | 20 differentials, recalculates the index, and applies the soft and       |
     | hard caps once a Low Index exists. Nine-hole rounds played before        |
     | the first index exists are held back and resolved when it does.          |
     | Rounds on a nine that has no rating are skipped.                         |
     |                                                                          |
     | Passed in:  allRounds - every round, in any order                        |
     |             tees      - course/tees keyed by id                          |
     | Passed out: a Result holding the full history and the current index      |
     +--------------------------------------------------------------------------+
     */
    public static Result compute(List<Round> allRounds, Map<Integer, Tee> tees)
    {
        List<Round> rounds = new ArrayList<>(allRounds);
        rounds.sort(Comparator.comparing(Round::date).thenComparingInt(Round::id));

        List<Entry> entries = new ArrayList<>();
        List<Posted> record = new ArrayList<>();
        List<LocalDate> lowIndexDates = new ArrayList<>();
        List<Double> lowIndexValues = new ArrayList<>();
        Double indexBefore = null;
        int holesPosted = 0;

        for (Round round : rounds)
        {
            Tee tee = tees.get(round.teeId());
            if (tee == null || !tee.isRated(round.startHole(), round.holes()))
            {
                continue;
            }

            int adjustedGross = adjustedGross(round, tee, indexBefore);
            double nineDifferential = Double.NaN;
            double scoreDifferential;
            boolean pending = false;
            if (round.isNineHole())
            {
                nineDifferential = nineHoleDifferential(adjustedGross,
                        tee.ratingFor(round.startHole(), round.holes()),
                        tee.slopeFor(round.startHole(), round.holes()), round.pcc());
                if (indexBefore == null)
                {
                    scoreDifferential = Double.NaN;
                    pending = true;
                }
                else
                {
                    scoreDifferential = round1(nineDifferential + expectedNineHoleDifferential(indexBefore));
                }
            }
            else
            {
                scoreDifferential = differential(adjustedGross, tee.rating(), tee.slope(), round.pcc());
            }
            holesPosted = holesPosted + round.holes();

            Posted posted = new Posted(entries.size(), nineDifferential, scoreDifferential, pending);
            record.add(posted);

            double esr = 0.0;
            if (!pending && indexBefore != null)
            {
                esr = esrAdjustment(indexBefore, scoreDifferential);
                if (esr != 0.0)
                {
                    applyEsr(record, esr);
                }
            }

            if (indexBefore == null && holesPosted >= INITIAL_HOLES)
            {
                resolvePending(record, entries);
                scoreDifferential = posted.value;
            }

            List<Double> recent = recentValues(record);
            int scoresInRecord = countPosted(record);
            Double calculated = holesPosted >= INITIAL_HOLES ? indexFromDifferentials(recent) : null;
            Double indexAfter = calculated;
            Cap cap = Cap.NONE;
            if (calculated != null && scoresInRecord >= RECORD_SIZE)
            {
                Double lowIndex = lowIndexAsOf(round.date(), lowIndexDates, lowIndexValues);
                if (lowIndex != null)
                {
                    Capped capped = applyCaps(calculated, lowIndex);
                    indexAfter = capped.index();
                    cap = capped.cap();
                }
            }
            if (indexAfter != null && scoresInRecord >= RECORD_SIZE)
            {
                lowIndexDates.add(round.date());
                lowIndexValues.add(indexAfter);
            }
            entries.add(new Entry(round, tee, indexBefore, adjustedGross, nineDifferential, scoreDifferential,
                    esr, calculated, cap, indexAfter, scoresInRecord));
            if (indexAfter != null)
            {
                indexBefore = indexAfter;
            }
        }

        Double currentIndex = entries.isEmpty() ? null : entries.get(entries.size() - 1).indexAfter();
        LocalDate lastDate = rounds.isEmpty() ? null : rounds.get(rounds.size() - 1).date();
        Double currentLow = lastDate == null ? null : lowIndexAsOf(lastDate, lowIndexDates, lowIndexValues);
        return new Result(entries, recentValues(record), currentIndex, currentLow);
    }

    /*
     +--------------------------------------------------------------------------+
     | recentValues(record)                                                     |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Collects the 18-hole differentials now on record, skipping rounds        |
     | still pending, and keeps only the most recent 20.                        |
     |                                                                          |
     | Passed in:  record - every round posted so far, oldest first             |
     | Passed out: up to 20 differentials, oldest first                         |
     +--------------------------------------------------------------------------+
     */
    private static List<Double> recentValues(List<Posted> record)
    {
        List<Double> values = new ArrayList<>();
        for (Posted item : record)
        {
            if (!item.pending)
            {
                values.add(item.value);
            }
        }
        return new ArrayList<>(values.subList(Math.max(0, values.size() - RECORD_SIZE), values.size()));
    }

    /*
     * countPosted(record): counts the scores on record, not counting pending 9-hole rounds.
     * Passed in:  record - every round posted so far
     * Passed out: number of scores with a differential
     */
    private static int countPosted(List<Posted> record)
    {
        int count = 0;
        for (Posted item : record)
        {
            if (!item.pending)
            {
                count = count + 1;
            }
        }
        return count;
    }

    /*
     +--------------------------------------------------------------------------+
     | applyEsr(record, esr)                                                    |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Rule 5.9: adds the (negative) Exceptional Score Reduction to the         |
     | most recent 20 differentials on record, including the round that         |
     | just earned it.                                                          |
     |                                                                          |
     | Passed in:  record - every round posted so far, oldest first             |
     |             esr    - the adjustment, -1.0 or -2.0                        |
     | Passed out: nothing (the values in record are changed)                   |
     +--------------------------------------------------------------------------+
     */
    private static void applyEsr(List<Posted> record, double esr)
    {
        int adjusted = 0;
        for (int lp = record.size() - 1; lp >= 0 && adjusted < RECORD_SIZE; lp = lp - 1)
        {
            Posted item = record.get(lp);
            if (!item.pending)
            {
                item.value = round1(item.value + esr);
                adjusted = adjusted + 1;
            }
        }
    }

    /*
     +--------------------------------------------------------------------------+
     | resolvePending(record, entries)                                          |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Called when the 54th hole has been posted and no index existed yet.      |
     | Builds a provisional index by adding pending 9-hole differentials        |
     | together in pairs (oldest first) alongside the 18-hole                   |
     | differentials, then uses that index's expected 9-hole differential       |
     | to give every pending round its 18-hole differential. The history        |
     | lines already written for those rounds are updated to match.             |
     |                                                                          |
     | Passed in:  record  - every round posted so far, oldest first            |
     |             entries - the history lines written so far                   |
     | Passed out: nothing (record and entries are changed)                     |
     +--------------------------------------------------------------------------+
     */
    private static void resolvePending(List<Posted> record, List<Entry> entries)
    {
        List<Double> provisional = new ArrayList<>();
        Double waiting = null;
        for (Posted item : record)
        {
            if (!item.pending)
            {
                provisional.add(item.value);
            }
            else if (waiting == null)
            {
                waiting = item.nineHoleDifferential;
            }
            else
            {
                provisional.add(round1(waiting + item.nineHoleDifferential));
                waiting = null;
            }
        }
        Double provisionalIndex = indexFromDifferentials(provisional);
        if (provisionalIndex == null)
        {
            return;
        }
        double expected = expectedNineHoleDifferential(provisionalIndex);
        for (Posted item : record)
        {
            if (!item.pending)
            {
                continue;
            }
            item.value = round1(item.nineHoleDifferential + expected);
            item.pending = false;
            if (item.entryIndex < entries.size())
            {
                Entry old = entries.get(item.entryIndex);
                entries.set(item.entryIndex, new Entry(old.round(), old.tee(), old.indexBefore(),
                        old.adjustedGross(), old.nineHoleDifferential(), item.value, old.esrAdjustment(),
                        old.calculatedIndex(), old.cap(), old.indexAfter(), old.scoresInRecord()));
            }
        }
    }

    /*
     +--------------------------------------------------------------------------+
     | lowIndexAsOf(date, dates, values)                                        |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Finds the Low Handicap Index: the lowest index issued, with 20 or        |
     | more scores on record, during the 365 days up to a given date.           |
     |                                                                          |
     | Passed in:  date   - the day the latest round was played                 |
     |             dates  - the days indexes were issued                        |
     |             values - the indexes issued (same order as dates)            |
     | Passed out: the lowest qualifying index, or null if there is none        |
     +--------------------------------------------------------------------------+
     */
    private static Double lowIndexAsOf(LocalDate date, List<LocalDate> dates, List<Double> values)
    {
        Double lowest = null;
        LocalDate windowStart = date.minusDays(LOW_INDEX_DAYS);
        for (int lp = 0; lp < dates.size(); lp = lp + 1)
        {
            if (dates.get(lp).isBefore(windowStart) || dates.get(lp).isAfter(date))
            {
                continue;
            }
            if (lowest == null || values.get(lp) < lowest)
            {
                lowest = values.get(lp);
            }
        }
        return lowest;
    }
}

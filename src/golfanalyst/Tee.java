package golfanalyst;

/*
 +--------------------------------------------------------------------------+
 | Tee                                                                      |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | Holds everything known about one course/tee combination (one row of      |
 | courses.tsv). Every combination has its own unique id, so "Jacksons      |
 | Chase / Intermediate" and "Jacksons Chase / Expert" are two Tee objects. |
 |                                                                          |
 | A tee is either an 18-hole tee or a 9-hole tee (a nine-hole course).     |
 | An 18-hole tee can also carry separate ratings for its front nine        |
 | (holes 1-9) and back nine (holes 10-18), which are needed to post a      |
 | nine-hole round played on it for handicap purposes.                      |
 |                                                                          |
 | Fields (passed in when a Tee is created):                                |
 | id           - unique number for this course/tee combination             |
 | courseName   - name of the course                                        |
 | city, state, zip - location text (optional, display only)                |
 | teeName      - name of the tees (Ladies, Beginner, Intermediate...)      |
 | holes        - 18 for an 18-hole tee, 9 for a nine-hole course           |
 | rating       - USGA Course Rating over all of the tee's holes            |
 | slope        - USGA Slope Rating over all of the tee's holes             |
 | yardage      - total yardage (optional, display only, 0 if unknown)      |
 | pars         - par for each hole (array position 0 is hole 1)            |
 | strokeIndex  - the scorecard's hole handicap ranking, 1 = hardest.       |
 |                An 18-hole tee uses each of 1-18 once, a nine-hole        |
 |                tee uses each of 1-9 once. It decides which holes get     |
 |                handicap strokes for the net double bogey limit.          |
 | frontRating, frontSlope - ratings for holes 1-9 only (0 if not rated)    |
 | backRating, backSlope   - ratings for holes 10-18 only (0 if not rated)  |
 |                                                                          |
 | Constants:                                                               |
 | HOLES - holes in a full round (18)                                       |
 | NINE  - holes in a nine-hole round (9)                                   |
 |                                                                          |
 | Public Methods:                                                          |
 | par(), parFor(...)         - total par for all holes, or for the holes   |
 |                              of a round                                  |
 | isRated(...)               - whether ratings exist for the holes played  |
 | ratingFor(...), slopeFor(...) - the ratings that apply to the holes      |
 |                              played                                      |
 | strokeRanksFor(...)        - stroke index ranks within the holes played  |
 | label()                    - short name for reports                      |
 +--------------------------------------------------------------------------+
 */
public record Tee(int id, String courseName, String city, String state, String zip,
                  String teeName, int holes, double rating, int slope, int yardage,
                  int[] pars, int[] strokeIndex,
                  double frontRating, int frontSlope, double backRating, int backSlope)
{

    public static final int HOLES = 18;
    public static final int NINE = 9;

    /*
     * par(): adds up the par of every hole on the tee.
     * Passed in:  nothing
     * Passed out: total par, for example 72
     */
    public int par()
    {
        return parFor(0, holes);
    }

    /*
     +--------------------------------------------------------------------------+
     | parFor(startHole, holesPlayed)                                           |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Adds up the par of the holes in one round, so a front-nine round         |
     | gets the par of holes 1-9 only.                                          |
     |                                                                          |
     | Passed in:  startHole   - first hole played, as a 0-based position       |
     |                           (0 = hole 1, 9 = hole 10)                      |
     |             holesPlayed - how many holes were played (9 or 18)           |
     | Passed out: total par of those holes, for example 36                     |
     +--------------------------------------------------------------------------+
     */
    public int parFor(int startHole, int holesPlayed)
    {
        int total = 0;
        for (int hole = startHole; hole < startHole + holesPlayed; hole = hole + 1)
        {
            total = total + pars[hole];
        }
        return total;
    }

    /*
     +--------------------------------------------------------------------------+
     | isRated(startHole, holesPlayed)                                          |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Says whether the Course Rating and Slope Rating needed to post this      |
     | round for handicap purposes exist. Playing every hole of the tee         |
     | uses the tee's own ratings; playing one nine of an 18-hole tee           |
     | needs that nine's front or back ratings.                                 |
     |                                                                          |
     | Passed in:  startHole, holesPlayed - as for parFor                       |
     | Passed out: true if the ratings exist                                    |
     +--------------------------------------------------------------------------+
     */
    public boolean isRated(int startHole, int holesPlayed)
    {
        return slopeFor(startHole, holesPlayed) > 0;
    }

    /*
     * ratingFor(startHole, holesPlayed): the Course Rating for the holes played.
     * Passed in:  startHole, holesPlayed - as for parFor
     * Passed out: the rating (0 if that nine has no rating)
     */
    public double ratingFor(int startHole, int holesPlayed)
    {
        if (holesPlayed == holes)
        {
            return rating;
        }
        return startHole == 0 ? frontRating : backRating;
    }

    /*
     * slopeFor(startHole, holesPlayed): the Slope Rating for the holes played.
     * Passed in:  startHole, holesPlayed - as for parFor
     * Passed out: the slope (0 if that nine has no rating)
     */
    public int slopeFor(int startHole, int holesPlayed)
    {
        if (holesPlayed == holes)
        {
            return slope;
        }
        return startHole == 0 ? frontSlope : backSlope;
    }

    /*
     +--------------------------------------------------------------------------+
     | strokeRanksFor(startHole, holesPlayed)                                   |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Ranks the holes played from hardest (1) to easiest, using the            |
     | scorecard's stroke index. For a full 18-hole round the ranks are the     |
     | stroke index itself. For a nine-hole round on one half of an             |
     | 18-hole tee the nine stroke index values are put in order, so            |
     | values 10, 12, 11 become ranks 1, 3, 2. This decides which of the        |
     | nine holes receive handicap strokes.                                     |
     |                                                                          |
     | Passed in:  startHole, holesPlayed - as for parFor                       |
     | Passed out: array with one rank per hole played, 1 to holesPlayed        |
     +--------------------------------------------------------------------------+
     */
    public int[] strokeRanksFor(int startHole, int holesPlayed)
    {
        int[] ranks = new int[holesPlayed];
        for (int hole = 0; hole < holesPlayed; hole = hole + 1)
        {
            int rank = 1;
            for (int otherHole = 0; otherHole < holesPlayed; otherHole = otherHole + 1)
            {
                if (strokeIndex[startHole + otherHole] < strokeIndex[startHole + hole])
                {
                    rank = rank + 1;
                }
            }
            ranks[hole] = rank;
        }
        return ranks;
    }

    /*
     * label(): the short name used in reports.
     * Passed in:  nothing
     * Passed out: text like "Edgewater (Beginner)"
     */
    public String label()
    {
        return courseName + " (" + teeName + ")";
    }
}

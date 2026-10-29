package golfanalyst;

import java.time.LocalDate;

/*
 +--------------------------------------------------------------------------+
 | Round                                                                    |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | Holds one round, either 18 holes or 9 holes (one row of rounds.tsv).     |
 | Only the hole-by-hole entries are stored. Totals, percentages and        |
 | adjusted scores are always calculated from them and never trusted from   |
 | a file, so a hand-edited file can never disagree with itself.            |
 |                                                                          |
 | Fields (passed in when a Round is created):                              |
 | id         - unique number for this round                                |
 | teeId      - id of the Tee (course/tee combination) that was played      |
 | date       - the day the round was played                                |
 | pcc        - Playing Conditions Calculation for the day, -1 to +3        |
 |              (normally 0)                                                |
 | startHole  - first hole played as a 0-based position: 0 for a full       |
 |              round or the front nine, 9 for the back nine                |
 | holes      - how many holes were played, 18 or 9                         |
 | scores     - strokes on each hole played                                 |
 | putts      - putts on each hole played                                   |
 | fairway    - true where the tee shot finished in the fairway             |
 | sand       - true where the player played a shot from a bunker           |
 | (all per-hole arrays have one entry for each hole played)                |
 |                                                                          |
 | Greens in regulation and sand saves are deliberately not stored. They    |
 | are worked out from the score, the putts, the par and the sand flag      |
 | (see Stats), so nobody can enter a better number than what happened.     |
 |                                                                          |
 | Putts are counted only when the ball is on the putting green, with any   |
 | club. A hole holed from off the green (a chip-in, a holed bunker shot)   |
 | is 0 putts.                                                              |
 |                                                                          |
 | Public Methods:                                                          |
 | totalScore(), totalPutts() - sums over the holes played                  |
 | isNineHole()               - true for a nine-hole round                  |
 | holesLabel(tee)            - "18", "9", "9F" (front) or "9B" (back)      |
 +--------------------------------------------------------------------------+
 */
public record Round(int id, int teeId, LocalDate date, int pcc, int startHole, int holes,
                    int[] scores, int[] putts,
                    boolean[] fairway, boolean[] sand)
{

    /*
     * totalScore(): adds up the strokes on every hole played (the gross score).
     * Passed in:  nothing
     * Passed out: the gross score, for example 93 (or 46 for nine holes)
     */
    public int totalScore()
    {
        int total = 0;
        for (int holeScore : scores)
        {
            total = total + holeScore;
        }
        return total;
    }

    /*
     * totalPutts(): adds up the putts on every hole played.
     * Passed in:  nothing
     * Passed out: the putt total, for example 34
     */
    public int totalPutts()
    {
        int total = 0;
        for (int holePutts : putts)
        {
            total = total + holePutts;
        }
        return total;
    }

    /*
     * isNineHole(): says whether this was a nine-hole round.
     * Passed in:  nothing
     * Passed out: true for 9 holes, false for 18
     */
    public boolean isNineHole()
    {
        return holes < Tee.HOLES;
    }

    /*
     * holesLabel(tee): short description of which holes were played.
     * Passed in:  tee - the course/tee played
     * Passed out: "18" for a full round, "9" on a nine-hole course,
     *             "9F" for the front nine and "9B" for the back nine
     */
    public String holesLabel(Tee tee)
    {
        if (!isNineHole())
        {
            return "18";
        }
        if (tee.holes() == Tee.NINE)
        {
            return "9";
        }
        return startHole == 0 ? "9F" : "9B";
    }
}

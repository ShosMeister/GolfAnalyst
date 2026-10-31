package golfanalyst;

import golfanalyst.StatsBoard.Part;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/*
 +--------------------------------------------------------------------------+
 | BestRound                                                                |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | Works out the Best Round screen for one course and tees. It never mixes  |
 | kinds of round: the choices are the same as the Stats Board's rows (an   |
 | 18-hole tee, the front or back nine of an 18-hole tee, or a nine-hole    |
 | course), and only the rounds of the chosen kind are used. Historical     |
 | rounds are left out, as on the Stats Board and Home.                     |
 |                                                                          |
 | Single: the real round with the lowest total score (a tie goes to the    |
 | most recent round).                                                      |
 | Composite: a made-up round from the best score on every hole. For each   |
 | hole it takes the lowest score on that hole in any of the rounds; if     |
 | several rounds tie, the one with the fewest putts there, then the most   |
 | recent. The putts, fairway and sand of that hole come from the same      |
 | round as the score. Totals and statistics are then worked out from these |
 | holes in the usual way (by whoever shows them).                          |
 |                                                                          |
 | There is no Swing code here, so all of it can be tested on its own.      |
 |                                                                          |
 | Nested types:                                                            |
 | Choice     - one entry of the course and tees pulldown                   |
 | HoleResult - one hole of the composite and the round it came from        |
 |                                                                          |
 | Public Methods:                                                          |
 | choices(rounds, tees)             - every course and tees with a round   |
 | (each of the three also takes includeHistorical, the Home screen's box;  |
 |  without it historical rounds are left out)                              |
 | latestChoice(rounds, tees)        - the kind of the most recent round    |
 | roundsOf(choice, rounds, tees)    - the rounds of that kind              |
 | bestSingle(rounds)                - the lowest round (newest on a tie)   |
 | composite(rounds)                 - the best result on every hole        |
 | compositeRound(choice, rounds)    - the composite as an unsaved Round    |
 +--------------------------------------------------------------------------+
 */
public final class BestRound
{

    public static final int COMPOSITE_ID = -1;

    /*
     +--------------------------------------------------------------------------+
     | Choice                                                                   |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | One course and tees, and which holes: teeId, part (EIGHTEEN, FRONT,      |
     | BACK or NINE_COURSE) and the name shown, the same as the Stats Board's   |
     | ("Edgewater:Senior", "Edgewater:Senior (Front 9)").                      |
     +--------------------------------------------------------------------------+
     */
    public record Choice(int teeId, Part part, String label)
    {

        /*
         * key(): what identifies the choice, for example "5:EIGHTEEN" (the same as a Stats Board row's key).
         */
        public String key()
        {
            return teeId + ":" + part;
        }

        /*
         * toString(): the name shown in the pulldown.
         */
        @Override
        public String toString()
        {
            return label;
        }
    }

    /*
     * HoleResult: one hole of the composite: the hole number (1 to 18), what was scored there and the round it came from.
     */
    public record HoleResult(int hole, int score, int putts, boolean fairway, boolean sand, Round from)
    {
    }

    /*
     * BestRound(): not used; everything is static.
     */
    private BestRound()
    {
    }

    /*
     +--------------------------------------------------------------------------+
     | choices(rounds, tees)                                                    |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Lists every course and tees (and kind of round) that has at least one    |
     | round counted, sorted by course name, then tee name, then 18 holes       |
     | before the front nine before the back nine. Historical rounds count      |
     | only when includeHistorical is true.                                     |
     |                                                                          |
     | Passed in:  rounds            - every round                              |
     |             tees              - course/tees keyed by id                  |
     |             includeHistorical - true to count historical rounds too      |
     | Passed out: the choices                                                  |
     +--------------------------------------------------------------------------+
     */
    public static List<Choice> choices(List<Round> rounds, Map<Integer, Tee> tees, boolean includeHistorical)
    {
        Map<String, Choice> found = new LinkedHashMap<>();
        for (Round round : Stats.counted(rounds, includeHistorical))
        {
            Tee tee = tees.get(round.teeId());
            if (tee == null)
            {
                continue;
            }
            Part part = StatsBoard.partOf(round, tee);
            Choice choice = new Choice(tee.id(), part, StatsBoard.labelOf(tee, part));
            found.putIfAbsent(choice.key(), choice);
        }
        List<Choice> sorted = new ArrayList<>(found.values());
        sorted.sort(Comparator.comparing((Choice choice) -> tees.get(choice.teeId()).courseName(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(choice -> tees.get(choice.teeId()).teeName(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Choice::part));
        return sorted;
    }

    /*
     * choices(rounds, tees): the choices without historical rounds.
     */
    public static List<Choice> choices(List<Round> rounds, Map<Integer, Tee> tees)
    {
        return choices(rounds, tees, false);
    }

    /*
     * latestChoice(rounds, tees): the choice of the most recently played round that is not historical, or null if there are none.
     */
    public static Choice latestChoice(List<Round> rounds, Map<Integer, Tee> tees)
    {
        return latestChoice(rounds, tees, false);
    }

    /*
     * latestChoice(rounds, tees, includeHistorical): the choice of the most recently played round counted (latest date, then highest id), or null if there are none.
     */
    public static Choice latestChoice(List<Round> rounds, Map<Integer, Tee> tees, boolean includeHistorical)
    {
        Round latest = null;
        for (Round round : Stats.counted(rounds, includeHistorical))
        {
            Tee tee = tees.get(round.teeId());
            if (tee != null && (latest == null || newer(round, latest)))
            {
                latest = round;
            }
        }
        if (latest == null)
        {
            return null;
        }
        Tee tee = tees.get(latest.teeId());
        Part part = StatsBoard.partOf(latest, tee);
        return new Choice(tee.id(), part, StatsBoard.labelOf(tee, part));
    }

    /*
     * newer(first, second): whether the first round was played after the second (by date, then by round id).
     */
    private static boolean newer(Round first, Round second)
    {
        int byDate = first.date().compareTo(second.date());
        return byDate > 0 || (byDate == 0 && first.id() > second.id());
    }

    /*
     * roundsOf(choice, rounds, tees): the rounds that are not historical, on the chosen tees and of the chosen kind.
     */
    public static List<Round> roundsOf(Choice choice, List<Round> rounds, Map<Integer, Tee> tees)
    {
        return roundsOf(choice, rounds, tees, false);
    }

    /*
     * roundsOf(choice, rounds, tees, includeHistorical): the rounds counted on the chosen tees and of the chosen kind (historical ones only when includeHistorical is true).
     */
    public static List<Round> roundsOf(Choice choice, List<Round> rounds, Map<Integer, Tee> tees, boolean includeHistorical)
    {
        List<Round> kept = new ArrayList<>();
        for (Round round : Stats.counted(rounds, includeHistorical))
        {
            Tee tee = tees.get(round.teeId());
            if (tee != null && tee.id() == choice.teeId() && StatsBoard.partOf(round, tee) == choice.part())
            {
                kept.add(round);
            }
        }
        return kept;
    }

    /*
     * bestSingle(rounds): the round with the lowest total score; a tie goes to the most recent. Null if there are none.
     */
    public static Round bestSingle(List<Round> rounds)
    {
        Round best = null;
        for (Round round : rounds)
        {
            if (best == null || round.totalScore() < best.totalScore()
                    || (round.totalScore() == best.totalScore() && newer(round, best)))
            {
                best = round;
            }
        }
        return best;
    }

    /*
     +--------------------------------------------------------------------------+
     | composite(rounds)                                                        |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Picks the best result on every hole. All the rounds must be of one kind  |
     | (see roundsOf), so they all cover the same holes. On each hole the       |
     | lowest score wins; if several rounds tie, the fewest putts; if they      |
     | still tie, the most recent round. The putts, fairway and sand come from  |
     | the winning round.                                                       |
     |                                                                          |
     | Passed in:  rounds - the rounds of one kind on one set of tees           |
     | Passed out: one HoleResult per hole played, in hole order (empty if      |
     |             there are no rounds)                                         |
     +--------------------------------------------------------------------------+
     */
    public static List<HoleResult> composite(List<Round> rounds)
    {
        List<HoleResult> holes = new ArrayList<>();
        if (rounds.isEmpty())
        {
            return holes;
        }
        Round first = rounds.get(0);
        for (int lp = 0; lp < first.holes(); lp = lp + 1)
        {
            Round winner = null;
            for (Round round : rounds)
            {
                if (winner == null || better(round, winner, lp))
                {
                    winner = round;
                }
            }
            holes.add(new HoleResult(first.startHole() + lp + 1, winner.scores()[lp], winner.putts()[lp],
                    winner.fairway()[lp], winner.sand()[lp], winner));
        }
        return holes;
    }

    /*
     * better(round, winner, place): whether a round's result on one hole beats the best so far: a lower score, then fewer putts, then more recent.
     */
    private static boolean better(Round round, Round winner, int place)
    {
        if (round.scores()[place] != winner.scores()[place])
        {
            return round.scores()[place] < winner.scores()[place];
        }
        if (round.putts()[place] != winner.putts()[place])
        {
            return round.putts()[place] < winner.putts()[place];
        }
        return newer(round, winner);
    }

    /*
     +--------------------------------------------------------------------------+
     | compositeRound(choice, rounds)                                           |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Puts the composite holes into an unsaved Round, so it can be shown and   |
     | counted exactly like a real one. Its id is COMPOSITE_ID (no saved round  |
     | uses it), its date is the newest of the rounds, and its PCC is 0.        |
     |                                                                          |
     | Passed in:  choice - the course and tees                                 |
     |             rounds - the rounds of that kind                             |
     | Passed out: the composite round, or null if there are no rounds          |
     +--------------------------------------------------------------------------+
     */
    public static Round compositeRound(Choice choice, List<Round> rounds)
    {
        List<HoleResult> holes = composite(rounds);
        if (holes.isEmpty())
        {
            return null;
        }
        int count = holes.size();
        int[] scores = new int[count];
        int[] putts = new int[count];
        boolean[] fairway = new boolean[count];
        boolean[] sand = new boolean[count];
        LocalDate newest = rounds.get(0).date();
        for (int lp = 0; lp < count; lp = lp + 1)
        {
            HoleResult hole = holes.get(lp);
            scores[lp] = hole.score();
            putts[lp] = hole.putts();
            fairway[lp] = hole.fairway();
            sand[lp] = hole.sand();
        }
        for (Round round : rounds)
        {
            if (round.date().isAfter(newest))
            {
                newest = round.date();
            }
        }
        Round first = rounds.get(0);
        return new Round(COMPOSITE_ID, choice.teeId(), newest, 0, first.startHole(), first.holes(), scores, putts, fairway, sand);
    }
}

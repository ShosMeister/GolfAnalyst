package golfanalyst;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/*
 +--------------------------------------------------------------------------+
 | SelfTest                                                                 |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | Checks the engine against answers worked out by hand, so a change to     |
 | the rules or the loader cannot quietly give different numbers. Each      |
 | check prints FAIL with a name if it is wrong; the last line reports      |
 | how many passed and failed.                                              |
 |                                                                          |
 | What is covered:                                                         |
 | - Score Differential, the 3-to-20 score table, 54.0 maximum              |
 | - Course Handicap, strokes per hole, net double bogey limits, for both   |
 |   18 holes and 9 holes                                                   |
 | - 9-hole differential, expected score, and the USGA's published example  |
 | - Exceptional Score Reduction thresholds, soft and hard caps             |
 | - Replays of several rounds: an ESR that changes earlier differentials,  |
 |   a blow-up hole cut back to par + 5, a 9-hole round after an index      |
 |   exists, and 9-hole rounds waiting for the first index                  |
 | - Fairway % ignoring par 3s, with 18-hole and 9-hole rounds mixed        |
 | - Greens in regulation and sand saves, which are calculated, including   |
 |   chip-ins and holes holed from off the green                            |
 | - Loader errors and warnings, including 9-hole tees and rounds           |
 |                                                                          |
 | How to run:                                                              |
 |   java -cp out golfanalyst.Main --selftest                               |
 |                                                                          |
 | Public Methods:                                                          |
 | run() - runs every check, returns true if all passed                     |
 +--------------------------------------------------------------------------+
 */
public final class SelfTest
{

    private static final double TOLERANCE = 1e-9;
    private static final int PLAIN_PUTTS = 2;
    private static int passedCount = 0;
    private static int failedCount = 0;

    private SelfTest()
    {
    }

    /*
     * check(name, ok): records one test result, printing FAIL and the name if it is wrong.
     * Passed in:  name - what is being checked, ok - true if it passed
     * Passed out: nothing
     */
    private static void check(String name, boolean ok)
    {
        if (ok)
        {
            passedCount++;
        }
        else
        {
            failedCount++;
            System.out.println("FAIL  " + name);
        }
    }

    /*
     * checkEquals(name, actual, expected): checks a number against the answer worked out by hand.
     * Passed in:  name - what is being checked, actual - what the program produced,
     *             expected - the value worked out by hand
     * Passed out: nothing
     */
    private static void checkEquals(String name, double actual, double expected)
    {
        boolean ok = Math.abs(actual - expected) < TOLERANCE;
        if (!ok)
        {
            System.out.println("      expected " + expected + " but got " + actual);
        }
        check(name, ok);
    }

    /*
     * listOf(values): builds a list of differentials for the table tests.
     * Passed in:  values - the numbers to put in the list
     * Passed out: the list
     */
    private static List<Double> listOf(double... values)
    {
        List<Double> list = new ArrayList<>();
        for (double value : values)
        {
            list.add(value);
        }
        return list;
    }

    /*
     * oneToN(count): builds the list 1.0, 2.0, 3.0 ... count for the table tests.
     * Passed in:  count - how many values to make
     * Passed out: the list
     */
    private static List<Double> oneToN(int count)
    {
        List<Double> list = new ArrayList<>();
        for (int lp = 1; lp <= count; lp++)
        {
            list.add((double) lp);
        }
        return list;
    }

    /*
     +--------------------------------------------------------------------------+
     | testTee(backRating, backSlope)                                           |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Builds a par 72 course for the tests: 18-hole rating 70.0 and slope      |
     | 113 (so the differential equals the score minus 70), stroke index        |
     | equal to the hole number, four par 3s, and a front nine rated 35.0       |
     | over par 36. The back nine is par 36 with the rating given.              |
     |                                                                          |
     | Passed in:  backRating, backSlope - the back nine's ratings (0 and 0     |
     |             for a back nine with no rating)                              |
     | Passed out: the test Tee, with id 1                                      |
     +--------------------------------------------------------------------------+
     */
    private static Tee testTee(double backRating, int backSlope)
    {
        int[] pars = {4, 3, 5, 4, 4, 3, 5, 4, 4, 4, 3, 5, 4, 4, 3, 5, 4, 4};
        int[] strokeIndex = new int[Tee.HOLES];
        for (int hole = 0; hole < Tee.HOLES; hole++)
        {
            strokeIndex[hole] = hole + 1;
        }
        return new Tee(1, "Test Links", "", "", "", "White", Tee.HOLES, 70.0, 113, 6000,
                pars, strokeIndex, 35.0, 113, backRating, backSlope);
    }

    /*
     +--------------------------------------------------------------------------+
     | roundOver(id, holesOverPar, date, tee)                                   |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Builds an 18-hole round that scores par + 1 on the first few holes       |
     | and par on the rest, with two putts on every hole. Its gross score       |
     | is 72 + holesOverPar, so on the test tee its differential is             |
     | 2.0 + holesOverPar.                                                      |
     |                                                                          |
     | Passed in:  id           - round id                                      |
     |             holesOverPar - how many holes (from hole 1) score par + 1    |
     |             date         - date played, as 2026-01-01                    |
     |             tee          - the course/tee played                         |
     | Passed out: the Round                                                    |
     +--------------------------------------------------------------------------+
     */
    private static Round roundOver(int id, int holesOverPar, String date, Tee tee)
    {
        int[] scores = new int[Tee.HOLES];
        int[] putts = new int[Tee.HOLES];
        for (int hole = 0; hole < Tee.HOLES; hole++)
        {
            scores[hole] = tee.pars()[hole] + (hole < holesOverPar ? 1 : 0);
            putts[hole] = PLAIN_PUTTS;
        }
        boolean[] noFlags = new boolean[Tee.HOLES];
        return new Round(id, tee.id(), LocalDate.parse(date), 0, 0, Tee.HOLES, scores, putts,
                noFlags.clone(), noFlags.clone());
    }

    /*
     +--------------------------------------------------------------------------+
     | nineRound(id, startHole, holesOverPar, date, tee)                        |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Builds a 9-hole round that scores par + 1 on the first few holes         |
     | played and par on the rest, with two putts on every hole. On the         |
     | test tee's front nine its gross score is 36 + holesOverPar, so its       |
     | 9-hole differential is 1.0 + holesOverPar.                               |
     |                                                                          |
     | Passed in:  id           - round id                                      |
     |             startHole    - 0 for the front nine, 9 for the back nine     |
     |             holesOverPar - how many holes score par + 1                  |
     |             date         - date played, as 2026-01-01                    |
     |             tee          - the course/tee played                         |
     | Passed out: the Round                                                    |
     +--------------------------------------------------------------------------+
     */
    private static Round nineRound(int id, int startHole, int holesOverPar, String date, Tee tee)
    {
        int[] scores = new int[Tee.NINE];
        int[] putts = new int[Tee.NINE];
        for (int hole = 0; hole < Tee.NINE; hole++)
        {
            scores[hole] = tee.pars()[startHole + hole] + (hole < holesOverPar ? 1 : 0);
            putts[hole] = PLAIN_PUTTS;
        }
        boolean[] noFlags = new boolean[Tee.NINE];
        return new Round(id, tee.id(), LocalDate.parse(date), 0, startHole, Tee.NINE, scores, putts,
                noFlags.clone(), noFlags.clone());
    }

    /*
     +--------------------------------------------------------------------------+
     | roundRow(id, teeId, date, holes, scores, putts, fairways, sand)          |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Builds one tab-separated line of rounds.tsv from its parts, so the       |
     | loader tests stay readable.                                              |
     |                                                                          |
     | Passed in:  id, teeId, date, holes - the first four columns              |
     |             scores, putts, fairways, sand - per-hole lists as text       |
     | Passed out: the line, with the columns in the order of the test          |
     |             header (RoundID, TeeID, Date, Holes, Scores, Putts,          |
     |             Fairways, Sand)                                              |
     +--------------------------------------------------------------------------+
     */
    private static String roundRow(int id, int teeId, String date, String holes,
                                   String scores, String putts, String fairways, String sand)
    {
        return id + "\t" + teeId + "\t" + date + "\t" + holes + "\t" + scores + "\t" + putts + "\t" + fairways + "\t" + sand;
    }

    /*
     +--------------------------------------------------------------------------+
     | run()                                                                    |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Runs every check and prints the totals.                                  |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: true if every check passed                                   |
     +--------------------------------------------------------------------------+
     */
    public static boolean run()
    {
        // Rule 5.1a differential
        checkEquals("differential 90 vs 70.0/120", Handicap.differential(90, 70.0, 120, 0), 18.8);
        checkEquals("differential with PCC +1", Handicap.differential(90, 70.0, 120, 1), 17.9);

        // Rule 5.2 table
        checkEquals("3 scores: lowest 1 - 2.0", Handicap.indexFromDifferentials(listOf(10, 12, 14)), 8.0);
        checkEquals("4 scores: lowest 1 - 1.0", Handicap.indexFromDifferentials(listOf(10, 12, 14, 16)), 9.0);
        checkEquals("5 scores: lowest 1", Handicap.indexFromDifferentials(listOf(10, 12, 14, 16, 18)), 10.0);
        checkEquals("6 scores: avg lowest 2 - 1.0", Handicap.indexFromDifferentials(listOf(10, 12, 14, 16, 18, 20)), 10.0);
        checkEquals("7 scores: avg lowest 2", Handicap.indexFromDifferentials(oneToN(7)), 1.5);
        checkEquals("9 scores: avg lowest 3", Handicap.indexFromDifferentials(oneToN(9)), 2.0);
        checkEquals("12 scores: avg lowest 4", Handicap.indexFromDifferentials(oneToN(12)), 2.5);
        checkEquals("15 scores: avg lowest 5", Handicap.indexFromDifferentials(oneToN(15)), 3.0);
        checkEquals("17 scores: avg lowest 6", Handicap.indexFromDifferentials(oneToN(17)), 3.5);
        checkEquals("19 scores: avg lowest 7", Handicap.indexFromDifferentials(oneToN(19)), 4.0);
        checkEquals("20 scores: avg lowest 8", Handicap.indexFromDifferentials(oneToN(20)), 4.5);
        check("fewer than 3 scores: no index", Handicap.indexFromDifferentials(listOf(10, 12)) == null);
        checkEquals("index never above 54.0", Handicap.indexFromDifferentials(listOf(70, 71, 72, 73, 74)), 54.0);

        // Course Handicap (worked examples from a state golf association handbook: 18 and 15)
        check("course handicap 17.2 @ 70.5/127/72 = 18", Handicap.courseHandicap(17.2, 70.5, 127, 72) == 18);
        check("course handicap 17.2 @ 68.9/118/72 = 15", Handicap.courseHandicap(17.2, 68.9, 118, 72) == 15);

        // Strokes per hole over 18 holes
        check("CH 20: rank 2 gets 2", Handicap.strokesReceived(20, 2, 18) == 2);
        check("CH 20: rank 3 gets 1", Handicap.strokesReceived(20, 3, 18) == 1);
        check("CH 18: every hole gets 1", Handicap.strokesReceived(18, 18, 18) == 1 && Handicap.strokesReceived(18, 1, 18) == 1);
        check("CH 0: none", Handicap.strokesReceived(0, 1, 18) == 0);
        check("CH 36: two each", Handicap.strokesReceived(36, 9, 18) == 2);
        check("CH -2 (plus): gives back on rank 17 and 18 only",
                Handicap.strokesReceived(-2, 17, 18) == -1 && Handicap.strokesReceived(-2, 18, 18) == -1
                        && Handicap.strokesReceived(-2, 16, 18) == 0);

        // Maximum hole score over 18 holes
        Tee tee = testTee(35.0, 113);
        check("no index: par + 5", Handicap.maxHoleScores(tee, 0, Tee.HOLES, null)[0] == 9);
        check("no index: par 3 is limited to 8", Handicap.maxHoleScores(tee, 0, Tee.HOLES, null)[1] == 8);
        check("index 18.0 (CH 16): hardest par 4 -> 7", Handicap.maxHoleScores(tee, 0, Tee.HOLES, 18.0)[0] == 4 + 2 + 1);
        check("index 18.0 (CH 16): hole 17 (stroke index 17) par 4 -> 6",
                Handicap.maxHoleScores(tee, 0, Tee.HOLES, 18.0)[16] == 4 + 2);

        // Exceptional Score Reduction
        checkEquals("ESR 8.0 better = -1", Handicap.esrAdjustment(20.0, 12.0), -1.0);
        checkEquals("ESR exactly 7.0 better = -1", Handicap.esrAdjustment(20.0, 13.0), -1.0);
        checkEquals("ESR 10.5 better = -2", Handicap.esrAdjustment(20.0, 9.5), -2.0);
        checkEquals("ESR exactly 10.0 better = -2", Handicap.esrAdjustment(20.0, 10.0), -2.0);
        checkEquals("ESR 6.0 better = none", Handicap.esrAdjustment(20.0, 14.0), 0.0);

        // Soft and hard caps
        checkEquals("no cap within 3.0", Handicap.applyCaps(13.0, 10.0).index(), 13.0);
        checkEquals("soft cap: 10 + 3 + half of 2", Handicap.applyCaps(15.0, 10.0).index(), 14.0);
        check("soft cap flagged", Handicap.applyCaps(15.0, 10.0).cap() == Handicap.Cap.SOFT);
        checkEquals("hard cap at low + 5", Handicap.applyCaps(20.0, 10.0).index(), 15.0);
        check("hard cap flagged", Handicap.applyCaps(20.0, 10.0).cap() == Handicap.Cap.HARD);
        checkEquals("falling index is not capped", Handicap.applyCaps(8.0, 10.0).index(), 8.0);

        // Nine-hole rules: differential, expected score, Course Handicap, strokes
        checkEquals("9-hole differential 41 vs 35.0/113", Handicap.nineHoleDifferential(41, 35.0, 113, 0), 6.0);
        checkEquals("9-hole differential: PCC counts half", Handicap.nineHoleDifferential(41, 35.0, 113, 2), 5.0);
        checkEquals("expected 9-hole differential for index 14.0 (USGA example gives 8.5)",
                Handicap.expectedNineHoleDifferential(14.0), 8.5);
        checkEquals("expected 9-hole differential for index 10.0", Handicap.expectedNineHoleDifferential(10.0), 6.4);
        checkEquals("expected 9-hole differential for index 0.0", Handicap.expectedNineHoleDifferential(0.0), 1.2);
        checkEquals("USGA example: 9-hole 7.2 + expected 8.5 = 15.7", Handicap.round1(7.2 + Handicap.expectedNineHoleDifferential(14.0)), 15.7);
        check("9-hole Course Handicap (USGA example: 8.7, 35.3, 121, par 36 = 4)",
                Handicap.nineHoleCourseHandicap(8.7, 35.3, 121, 36) == 4);
        check("9 holes, CH 3: ranks 1-3 get a stroke, rank 4 does not",
                Handicap.strokesReceived(3, 1, 9) == 1 && Handicap.strokesReceived(3, 3, 9) == 1
                        && Handicap.strokesReceived(3, 4, 9) == 0);
        check("9 holes, CH 10: rank 1 gets 2, rank 2 gets 1",
                Handicap.strokesReceived(10, 1, 9) == 2 && Handicap.strokesReceived(10, 2, 9) == 1);
        check("9 holes, CH -2 (plus): gives back on rank 8 and 9 only",
                Handicap.strokesReceived(-2, 9, 9) == -1 && Handicap.strokesReceived(-2, 8, 9) == -1
                        && Handicap.strokesReceived(-2, 7, 9) == 0);

        // Stroke ranks within a nine
        int[] scrambledBack = {12, 16, 10, 18, 14, 11, 17, 13, 15};
        int[] scrambledIndex = new int[Tee.HOLES];
        for (int hole = 0; hole < Tee.NINE; hole++)
        {
            scrambledIndex[hole] = hole + 1;
            scrambledIndex[Tee.NINE + hole] = scrambledBack[hole];
        }
        Tee scrambledTee = new Tee(7, "Scrambled", "", "", "", "Blue", Tee.HOLES, 70.0, 113, 6000,
                tee.pars(), scrambledIndex, 35.0, 113, 35.0, 113);
        check("front nine ranks follow the stroke index",
                Arrays.equals(scrambledTee.strokeRanksFor(0, Tee.NINE), new int[] {1, 2, 3, 4, 5, 6, 7, 8, 9}));
        check("back nine stroke index values 10-18 become ranks 1-9",
                Arrays.equals(scrambledTee.strokeRanksFor(Tee.NINE, Tee.NINE), new int[] {3, 7, 1, 9, 5, 2, 8, 4, 6}));

        // Maximum hole score over nine holes: index 18.0, front nine rated 35.0/113, par 36
        check("front nine, index 18.0 (9-hole CH 8): hardest par 4 -> 7",
                Handicap.maxHoleScores(tee, 0, Tee.NINE, 18.0)[0] == 4 + 2 + 1);
        check("front nine, index 18.0 (9-hole CH 8): easiest hole (rank 9) gets no stroke -> 6",
                Handicap.maxHoleScores(tee, 0, Tee.NINE, 18.0)[8] == 4 + 2);
        check("front nine, no index: par 3 limited to 8", Handicap.maxHoleScores(tee, 0, Tee.NINE, null)[1] == 8);
        check("tee par for the nine holes of a round", tee.parFor(0, Tee.NINE) == 36 && tee.parFor(Tee.NINE, Tee.NINE) == 36);
        check("holes label", roundOver(1, 0, "2026-01-01", tee).holesLabel(tee).equals("18")
                && nineRound(2, 0, 0, "2026-01-01", tee).holesLabel(tee).equals("9F")
                && nineRound(3, 9, 0, "2026-01-01", tee).holesLabel(tee).equals("9B"));

        // Full replay: differentials 15.0, 20.0, 10.0 give an index of 8.0 after three rounds
        Map<Integer, Tee> tees = Map.of(1, tee);
        List<Round> rounds = new ArrayList<>();
        rounds.add(roundOver(1, 13, "2026-01-01", tee));
        rounds.add(roundOver(2, 18, "2026-01-08", tee));
        rounds.add(roundOver(3, 8, "2026-01-15", tee));
        rounds.add(roundOver(4, 18, "2026-01-22", tee));
        Handicap.Result replay = Handicap.compute(rounds, tees);
        check("no index after 2 rounds", replay.entries().get(1).indexAfter() == null);
        checkEquals("index after 3 rounds", replay.entries().get(2).indexAfter(), 8.0);
        checkEquals("index after 4 rounds", replay.entries().get(3).indexAfter(), 9.0);

        // ESR in a replay: a 72 (differential 2.0) against an index of 9.0 is 7.0 better,
        // so every differential on record drops by 1.0
        rounds.add(roundOver(5, 0, "2026-01-29", tee));
        replay = Handicap.compute(rounds, tees);
        checkEquals("ESR applied to the new differential", replay.currentDifferentials().get(4), 1.0);
        checkEquals("ESR applied to an earlier differential", replay.currentDifferentials().get(0), 14.0);
        checkEquals("index after 5 rounds with ESR", replay.entries().get(4).indexAfter(), 1.0);
        checkEquals("ESR amount recorded", replay.entries().get(4).esrAdjustment(), -1.0);

        // A 9-hole round once an index exists: 9-hole 1.0 + expected for index 8.0 (5.4) = 6.4
        List<Round> withNine = new ArrayList<>();
        withNine.add(roundOver(1, 13, "2026-01-01", tee));
        withNine.add(roundOver(2, 18, "2026-01-08", tee));
        withNine.add(roundOver(3, 8, "2026-01-15", tee));
        withNine.add(nineRound(4, 0, 0, "2026-01-22", tee));
        Handicap.Result nineResult = Handicap.compute(withNine, tees);
        checkEquals("9-hole differential recorded", nineResult.entries().get(3).nineHoleDifferential(), 1.0);
        checkEquals("18-hole differential from a 9-hole round", nineResult.entries().get(3).differential(), 6.4);
        checkEquals("index after the 9-hole round (4 scores: lowest 6.4 - 1.0)", nineResult.entries().get(3).indexAfter(), 5.4);

        // Nine-hole rounds before any index exists: held until 54 holes are posted
        List<Round> nines = new ArrayList<>();
        int[] holesOver = {4, 3, 5, 2, 6, 3};
        for (int lp = 0; lp < holesOver.length; lp++)
        {
            nines.add(nineRound(lp + 1, 0, holesOver[lp], "2026-02-0" + (lp + 1), tee));
        }
        Handicap.Result pendingResult = Handicap.compute(nines.subList(0, 5), tees);
        check("45 holes of 9-hole rounds: no index yet", pendingResult.currentIndex() == null);
        check("a 9-hole round with no index is pending", Double.isNaN(pendingResult.entries().get(4).differential()));
        pendingResult = Handicap.compute(nines, tees);
        checkEquals("54 holes of 9-hole rounds: initial index", pendingResult.currentIndex(), 7.3);
        checkEquals("first 9-hole round gets its 18-hole differential", pendingResult.entries().get(0).differential(), 9.8);
        checkEquals("fourth 9-hole round gets its 18-hole differential", pendingResult.entries().get(3).differential(), 7.8);

        // A nine with no rating cannot be used for the handicap
        Tee unratedBack = testTee(0, 0);
        check("front nine is rated, back nine is not", unratedBack.isRated(0, Tee.NINE) && !unratedBack.isRated(Tee.NINE, Tee.NINE));
        Handicap.Result unratedResult = Handicap.compute(List.of(nineRound(1, Tee.NINE, 0, "2026-03-01", unratedBack)),
                Map.of(1, unratedBack));
        check("unrated nine is left out of the handicap", unratedResult.entries().isEmpty());

        // Net double bogey with no index yet: one hole 8 over par is cut back to par + 5
        int[] blowupScores = new int[Tee.HOLES];
        int[] blowupPutts = new int[Tee.HOLES];
        for (int hole = 0; hole < Tee.HOLES; hole++)
        {
            blowupScores[hole] = tee.pars()[hole];
            blowupPutts[hole] = PLAIN_PUTTS;
        }
        blowupScores[0] += 8;
        boolean[] noFlags = new boolean[Tee.HOLES];
        Round blowup = new Round(9, 1, LocalDate.parse("2026-02-01"), 0, 0, Tee.HOLES, blowupScores, blowupPutts,
                noFlags, noFlags);
        Handicap.Result blowupResult = Handicap.compute(List.of(blowup), tees);
        check("adjusted gross cuts the blow-up hole (80 -> 77)", blowupResult.entries().get(0).adjustedGross() == 77);
        checkEquals("differential comes from the adjusted score", blowupResult.entries().get(0).differential(), 7.0);

        // Statistics: fairway % only counts par 4s and 5s, 18-hole and 9-hole rounds counted apart
        boolean[] allFlags = new boolean[Tee.HOLES];
        Arrays.fill(allFlags, true);
        Round allFairways = new Round(10, 1, LocalDate.parse("2026-02-02"), 0, 0, Tee.HOLES, tee.pars().clone(), blowupPutts,
                allFlags, noFlags);
        Stats.Summary summary = Stats.summarize(List.of(allFairways), tees);
        check("fairway chances exclude par 3s", summary.fairwayChances() == 14);
        checkEquals("fairway % is 100 even with par 3 flags set", summary.fairwayPct(), 100.0);
        checkEquals("2-putts-or-fewer % with two putts everywhere", summary.twoPuttPct(), 100.0);
        checkEquals("GIR % when every hole is par with two putts", summary.girPct(), 100.0);
        check("sand save % is n/a with no sand shots", Double.isNaN(summary.sandSavePct()));
        Stats.Summary mixed = Stats.summarize(List.of(allFairways, nineRound(11, 0, 0, "2026-02-03", tee)), tees);
        check("one 18-hole and one 9-hole round counted apart", mixed.rounds18() == 1 && mixed.rounds9() == 1);
        checkEquals("average 9-hole score", mixed.avgScore9(), 36.0);
        checkEquals("average 18-hole score is not pulled down by the 9-hole round", mixed.avgScore18(), 72.0);
        check("fairway chances: 14 over 18 holes + 7 on the front nine", mixed.fairwayChances() == 21);

        // Calculated statistics: greens in regulation, sand saves, chip-ins
        check("GIR: par 4 in 4 with 2 putts", Stats.isGreenInRegulation(4, 4, 2));
        check("GIR: par 3 in 3 with 2 putts", Stats.isGreenInRegulation(3, 3, 2));
        check("GIR: par 5 in 5 with 2 putts", Stats.isGreenInRegulation(5, 5, 2));
        check("GIR: par 4 in 3 with 1 putt (birdie)", Stats.isGreenInRegulation(4, 3, 1));
        check("not GIR: par 4 in 5 with 2 putts (bogey)", !Stats.isGreenInRegulation(4, 5, 2));
        check("not GIR: par 4 chipped on and one-putted for par", !Stats.isGreenInRegulation(4, 4, 1));
        check("not GIR: par 4 chip-in for birdie (3 strokes, 0 putts)", !Stats.isGreenInRegulation(4, 3, 0));
        check("GIR: hole in one on a par 3 (1 stroke, 0 putts)", Stats.isGreenInRegulation(3, 1, 0));
        check("sand save: par from a bunker", Stats.isSandSave(4, 4));
        check("sand save: birdie from a bunker", Stats.isSandSave(4, 3));
        check("no sand save: bogey from a bunker", !Stats.isSandSave(4, 5));

        // Hole 1 is a chip-in birdie (0 putts) from off the green. Hole 2 is a par 3 where the tee shot found
        // the bunker, the bunker shot reached the green and one putt saved par. Every other hole is a bogey.
        int[] chipScores = new int[Tee.HOLES];
        int[] chipPutts = new int[Tee.HOLES];
        boolean[] chipSand = new boolean[Tee.HOLES];
        for (int hole = 0; hole < Tee.HOLES; hole++)
        {
            chipScores[hole] = tee.pars()[hole] + 1;
            chipPutts[hole] = PLAIN_PUTTS;
        }
        chipScores[0] = tee.pars()[0] - 1;
        chipPutts[0] = 0;
        chipSand[0] = true;
        chipSand[1] = true;
        chipScores[1] = tee.pars()[1];
        chipPutts[1] = 1;
        Round chipRound = new Round(12, 1, LocalDate.parse("2026-02-04"), 0, 0, Tee.HOLES, chipScores, chipPutts,
                noFlags, chipSand);
        Stats.Summary chipStats = Stats.summarize(List.of(chipRound), tees);
        checkEquals("a chip-in birdie and 17 bogeys hit no greens", chipStats.girPct(), 0.0);
        checkEquals("putts total counts the chip-in as 0 putts and the bunker save as 1", chipStats.avgPutts18(), 33.0);
        checkEquals("2-putts-or-fewer includes the chip-in", chipStats.twoPuttPct(), 100.0);
        check("sand saves are worked out from the score: 2 bunker shots, 2 saves",
                chipStats.sandShots() == 2 && chipStats.sandSaves() == 2);
        Round bogeyRound = roundOver(13, 18, "2026-02-05", tee);
        checkEquals("bogey on every hole with two putts: no greens in regulation",
                Stats.summarize(List.of(bogeyRound), tees).girPct(), 0.0);

        // Loader validation: courses, including a 9-hole tee and front/back ratings
        String parsText = "4,3,5,4,4,3,5,4,4,4,3,5,4,4,3,5,4,4";
        String indexText = "1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18";
        String nineParsText = "4,3,5,4,4,3,5,4,4";
        String nineIndexText = "1,2,3,4,5,6,7,8,9";
        String courseHeader = "TeeID\tCourse\tTee\tHoles\tRating\tSlope\tPars\tStrokeIndex\tFrontRating\tFrontSlope\tBackRating\tBackSlope";
        List<Problem> courseProblems = new ArrayList<>();
        List<Tee> loadedTees = DataLoader.parseCourses(List.of(
                "# comment", courseHeader,
                "1\tLinks\tWhite\t\t70.0\t113\t" + parsText + "\t" + indexText + "\t35.1\t121\t35.3\t125",
                "2\tPark\tWhite\t9\t34.6\t116\t" + nineParsText + "\t" + nineIndexText + "\t\t\t\t",
                "1\tOther\tBlue\t\t72.0\t120\t" + parsText + "\t" + indexText + "\t\t\t\t",
                "3\tLinks\tRed\t\t68.0\t110\t" + parsText + "\t1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,17\t\t\t\t",
                "4\tHalf\tRed\t\t68.0\t110\t" + parsText + "\t" + indexText + "\t35.0\t\t\t",
                "5\tPark\tRed\t9\t34.0\t110\t" + parsText + "\t" + nineIndexText + "\t\t\t\t",
                "6\tHalf\tBlue\t\t68.0\t110\t" + parsText + "\t" + indexText + "\t35.0\t110\t\t"),
                courseProblems);
        check("loader keeps the 18-hole tee, the 9-hole tee and the front-rated tee", loadedTees.size() == 3);
        check("duplicate TeeID, bad StrokeIndex, half a rating pair and a 9-hole tee with 18 pars are errors",
                courseProblems.stream().filter(problem -> problem.severity() == Problem.Severity.ERROR).count() == 4);
        check("loaded 9-hole tee has 9 holes and its own rating",
                loadedTees.get(1).holes() == Tee.NINE && loadedTees.get(1).par() == 36 && loadedTees.get(1).isRated(0, Tee.NINE));
        check("loaded 18-hole tee has front and back nine ratings",
                loadedTees.get(0).ratingFor(Tee.NINE, Tee.NINE) == 35.3 && loadedTees.get(0).slopeFor(0, Tee.NINE) == 121);

        // Loader validation: rounds, including 9-hole rounds and chip-ins
        String roundHeader = "RoundID\tTeeID\tDate\tHoles\tScores\tPutts\tFairways\tSand";
        String scoresText = "5,4,6,5,5,4,6,5,5,5,4,6,5,5,4,6,5,5";
        String puttsText = "2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2";
        String chipInPuttsText = "0,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2";
        String zerosText = "0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0";
        String par3FairwayText = "0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0";   // hole 2 is a par 3
        String nineScoresText = "5,4,6,5,5,4,6,5,5";
        String ninePuttsText = "2,2,2,2,2,2,2,2,2";
        String nineZerosText = "0,0,0,0,0,0,0,0,0";
        Map<Integer, Tee> loadedLookup = Map.of(1, loadedTees.get(0), 2, loadedTees.get(1), 6, loadedTees.get(2));
        List<Problem> roundProblems = new ArrayList<>();
        List<Round> loadedRounds = DataLoader.parseRounds(List.of(roundHeader,
                roundRow(1, 1, "2026-03-01", "", scoresText, puttsText, par3FairwayText, zerosText),
                roundRow(2, 9, "2026-03-02", "", scoresText, puttsText, zerosText, zerosText),
                roundRow(3, 1, "3/9/2026", "", scoresText, scoresText, zerosText, zerosText),
                roundRow(4, 1, "2026-13-45", "", scoresText, puttsText, zerosText, zerosText),
                roundRow(5, 1, "2026-03-05", "F", nineScoresText, ninePuttsText, nineZerosText, nineZerosText),
                roundRow(6, 1, "2026-03-06", "B", nineScoresText, ninePuttsText, nineZerosText, nineZerosText),
                roundRow(7, 2, "2026-03-07", "", nineScoresText, ninePuttsText, nineZerosText, nineZerosText),
                roundRow(8, 1, "2026-03-08", "", nineScoresText, ninePuttsText, nineZerosText, nineZerosText),
                roundRow(9, 2, "2026-03-09", "F", nineScoresText, ninePuttsText, nineZerosText, nineZerosText),
                roundRow(10, 1, "2026-03-10", "9", nineScoresText, ninePuttsText, nineZerosText, nineZerosText),
                roundRow(11, 6, "2026-03-11", "B", nineScoresText, ninePuttsText, nineZerosText, nineZerosText),
                roundRow(12, 1, "2026-03-12", "", scoresText, chipInPuttsText, zerosText, zerosText)),
                loadedLookup, roundProblems);
        check("loader keeps the good 18-hole, front, back, 9-hole-tee, unrated-nine and chip-in rounds",
                loadedRounds.size() == 6);
        check("unknown TeeID, putts >= score, a bad date, 9 values for 18 holes, F on a 9-hole tee and Holes=9 are errors",
                roundProblems.stream().filter(problem -> problem.severity() == Problem.Severity.ERROR).count() == 6);
        check("a fairway on a par 3 is a warning",
                roundProblems.stream().anyMatch(problem -> problem.severity() == Problem.Severity.WARNING
                        && problem.message().contains("par 3")));
        check("a nine with no rating is a warning",
                roundProblems.stream().anyMatch(problem -> problem.severity() == Problem.Severity.WARNING
                        && problem.message().contains("not toward the handicap")));
        check("loaded rounds know which holes were played",
                loadedRounds.get(1).startHole() == 0 && loadedRounds.get(1).holes() == Tee.NINE
                        && loadedRounds.get(2).startHole() == Tee.NINE && loadedRounds.get(3).holes() == Tee.NINE);
        check("a chip-in (0 putts on a hole) is accepted", loadedRounds.get(5).putts()[0] == 0);

        // Old files that still have Saves and GIR columns: loaded, columns ignored, with a warning
        List<Problem> oldColumnProblems = new ArrayList<>();
        List<Round> oldColumnRounds = DataLoader.parseRounds(List.of(roundHeader + "\tSaves\tGIR",
                roundRow(1, 1, "2026-03-01", "", scoresText, puttsText, zerosText, zerosText)
                        + "\t" + zerosText + "\t" + zerosText),
                loadedLookup, oldColumnProblems);
        check("old Saves and GIR columns are ignored with a warning",
                oldColumnRounds.size() == 1 && oldColumnProblems.stream().anyMatch(problem ->
                        problem.severity() == Problem.Severity.WARNING && problem.message().contains("calculated")));

        System.out.println(passedCount + " passed, " + failedCount + " failed");
        return failedCount == 0;
    }
}

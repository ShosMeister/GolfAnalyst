package golfanalyst;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

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
 | - Adding tees to courses.tsv (CourseWriter), the next TeeID, finding a   |
 |   course, the sample folder and the empty data folder                    |
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
            passedCount = passedCount + 1;
        }
        else
        {
            failedCount = failedCount + 1;
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
        for (int lp = 1; lp <= count; lp = lp + 1)
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
        for (int hole = 0; hole < Tee.HOLES; hole = hole + 1)
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
        for (int hole = 0; hole < Tee.HOLES; hole = hole + 1)
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
        for (int hole = 0; hole < Tee.NINE; hole = hole + 1)
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
        for (int hole = 0; hole < Tee.NINE; hole = hole + 1)
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
        for (int lp = 0; lp < holesOver.length; lp = lp + 1)
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
        for (int hole = 0; hole < Tee.HOLES; hole = hole + 1)
        {
            blowupScores[hole] = tee.pars()[hole];
            blowupPutts[hole] = PLAIN_PUTTS;
        }
        blowupScores[0] = blowupScores[0] + 8;
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
        for (int hole = 0; hole < Tee.HOLES; hole = hole + 1)
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

        checkStatsBoard();
        checkBestRound();

        try
        {
            checkCourseFiles();
            checkRoundReplace();
            checkHistorical();
        }
        catch (IOException failure)
        {
            check("the file checks ran without error: " + failure, false);
        }

        System.out.println(passedCount + " passed, " + failedCount + " failed");
        return failedCount == 0;
    }

    /*
     +--------------------------------------------------------------------------+
     | checkBestRound()                                                         |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Tests the BestRound calculations on rounds whose answers were worked     |
     | out by hand.                                                             |
     |                                                                          |
     | It checks that:                                                          |
     | - the pulldown lists each course, tees and kind of round once, sorted    |
     |   by course and tees, and starts on the kind of the latest round;        |
     | - the best single round is the lowest, and a tie goes to the newer one;  |
     | - the composite takes the lowest score on each hole, a tie going to the  |
     |   fewer putts and then the newer round;                                  |
     | - the putts, fairway and sand of each hole come from that same round;    |
     | - the composite's totals are right and it has no saved round's id;       |
     | - front nine, back nine and nine-hole course rounds are kept apart and   |
     |   numbered by their own holes;                                           |
     | - historical rounds are left out;                                        |
     | - one round, and no rounds at all, are handled.                          |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    private static void checkBestRound()
    {
        Tee links = testTee(0, 0);
        int[] pars = links.pars();
        int[] ninePars = {4, 3, 5, 4, 4, 3, 5, 4, 4};
        int[] nineIndex = {1, 2, 3, 4, 5, 6, 7, 8, 9};
        Tee park = new Tee(2, "Nine Park", "", "", "", "Red", Tee.NINE, 34.0, 110, 3000,
                ninePars, nineIndex, 0, 0, 0, 0);
        Map<Integer, Tee> tees = Map.of(1, links, 2, park);
        int[] twoPutts = new int[Tee.HOLES];
        Arrays.fill(twoPutts, PLAIN_PUTTS);
        boolean[] none = new boolean[Tee.HOLES];

        // Round 2: a birdie 3 on hole 1 with 1 putt and the fairway hit, bogeys everywhere else: 88
        int[] scores2 = new int[Tee.HOLES];
        for (int hole = 0; hole < Tee.HOLES; hole = hole + 1)
        {
            scores2[hole] = pars[hole] + 1;
        }
        scores2[0] = 3;
        int[] putts2 = twoPutts.clone();
        putts2[0] = 1;
        boolean[] fairway2 = new boolean[Tee.HOLES];
        fairway2[0] = true;
        Round round2 = new Round(2, 1, LocalDate.parse("2026-02-01"), 0, 0, Tee.HOLES, scores2, putts2, fairway2, none.clone());
        // Round 3: also 3 on hole 1 but 2 putts, a birdie 2 on hole 2 from the sand, pars elsewhere: 70
        int[] scores3 = pars.clone();
        scores3[0] = 3;
        scores3[1] = 2;
        int[] putts3 = twoPutts.clone();
        putts3[1] = 1;
        boolean[] sand3 = new boolean[Tee.HOLES];
        sand3[1] = true;
        Round round3 = new Round(3, 1, LocalDate.parse("2026-03-01"), 0, 0, Tee.HOLES, scores3, putts3, none.clone(), sand3);
        // Round 4: birdies on holes 5 and 6, pars elsewhere: also 70, and newer
        int[] scores4 = pars.clone();
        scores4[4] = 3;
        scores4[5] = 2;
        int[] putts4 = twoPutts.clone();
        putts4[5] = 1;
        Round round4 = new Round(4, 1, LocalDate.parse("2026-04-01"), 0, 0, Tee.HOLES, scores4, putts4, none.clone(), none.clone());
        Round oldLow = new Round(5, 1, LocalDate.parse("2005-06-19"), 0, 0, Tee.HOLES, pars.clone(), new int[Tee.HOLES],
                none.clone(), none.clone(), true);
        Arrays.fill(oldLow.scores(), 3);
        Arrays.fill(oldLow.putts(), 1);
        int[] parkScores = {4, 3, 5, 4, 4, 3, 5, 4, 5};
        int[] parkPutts = {2, 2, 2, 2, 2, 2, 2, 2, 2};
        boolean[] noNine = new boolean[Tee.NINE];
        Round parkRound = new Round(9, 2, LocalDate.parse("2026-06-01"), 0, 0, Tee.NINE, parkScores, parkPutts, noNine, noNine);
        List<Round> rounds = List.of(roundOver(1, Tee.HOLES, "2026-01-01", links), round2, round3, round4, oldLow,
                nineRound(10, 0, 9, "2026-05-01", links), nineRound(11, 0, 0, "2026-05-02", links),
                nineRound(12, 9, 0, "2026-05-03", links), parkRound);

        List<BestRound.Choice> choices = BestRound.choices(rounds, tees);
        check("best round: one choice per course, tees and kind, sorted by course then tees", choices.size() == 4
                && choices.get(0).label().equals("Nine Park:Red")
                && choices.get(1).label().equals("Test Links:White")
                && choices.get(2).label().equals("Test Links:White (Front 9)")
                && choices.get(3).label().equals("Test Links:White (Back 9)"));
        check("best round: it starts on the kind of the latest round", BestRound.latestChoice(rounds, tees).key().equals("2:NINE_COURSE"));
        BestRound.Choice eighteen = choices.get(1);
        List<Round> eighteenRounds = BestRound.roundsOf(eighteen, rounds, tees);
        check("best round: only that kind's rounds are used, historical ones left out", eighteenRounds.size() == 4
                && eighteenRounds.stream().noneMatch(Round::historical) && eighteenRounds.stream().noneMatch(Round::isNineHole));
        check("best single: the lowest total, and a tie goes to the newer round", BestRound.bestSingle(eighteenRounds) == round4);

        List<BestRound.HoleResult> holes = BestRound.composite(eighteenRounds);
        check("composite: one result per hole, numbered 1 to 18", holes.size() == 18 && holes.get(0).hole() == 1 && holes.get(17).hole() == 18);
        check("composite: the lowest score on a hole wins", holes.get(1).score() == 2 && holes.get(1).from() == round3
                && holes.get(4).score() == 3 && holes.get(4).from() == round4);
        check("composite: a tied score goes to the fewer putts", holes.get(0).score() == 3 && holes.get(0).putts() == 1
                && holes.get(0).from() == round2);
        check("composite: a tie in score and putts goes to the newer round", holes.get(17).from() == round4);
        check("composite: the fairway comes from the round that made the score", holes.get(0).fairway());
        check("composite: so does the sand", holes.get(1).sand() && !holes.get(4).sand());
        Round composite = BestRound.compositeRound(eighteen, eighteenRounds);
        check("composite: the best of every hole adds up to 68 with 33 putts", composite.totalScore() == 68 && composite.totalPutts() == 33);
        check("composite: it is not a saved round and is dated by the newest round", composite.id() == BestRound.COMPOSITE_ID
                && composite.date().equals(LocalDate.parse("2026-04-01")) && composite.holes() == Tee.HOLES);

        BestRound.Choice front = choices.get(2);
        List<Round> frontRounds = BestRound.roundsOf(front, rounds, tees);
        List<BestRound.HoleResult> frontHoles = BestRound.composite(frontRounds);
        check("composite: a front nine uses only front-nine rounds, holes 1 to 9", frontRounds.size() == 2
                && frontHoles.size() == 9 && frontHoles.get(8).hole() == 9 && BestRound.compositeRound(front, frontRounds).totalScore() == 36);
        BestRound.Choice back = choices.get(3);
        List<BestRound.HoleResult> backHoles = BestRound.composite(BestRound.roundsOf(back, rounds, tees));
        check("composite: a back nine is holes 10 to 18", backHoles.size() == 9 && backHoles.get(0).hole() == 10
                && backHoles.get(8).hole() == 18);
        List<Round> parkRounds = BestRound.roundsOf(choices.get(0), rounds, tees);
        Round parkComposite = BestRound.compositeRound(choices.get(0), parkRounds);
        check("composite: with one round it is that round", parkRounds.size() == 1 && parkComposite.totalScore() == 37
                && BestRound.bestSingle(parkRounds) == parkRound && parkComposite.startHole() == 0 && parkComposite.holes() == Tee.NINE);
        check("best round: no rounds give no choices, no best and no composite", BestRound.choices(List.of(), tees).isEmpty()
                && BestRound.latestChoice(List.of(), tees) == null && BestRound.bestSingle(List.of()) == null
                && BestRound.composite(List.of()).isEmpty() && BestRound.compositeRound(eighteen, List.of()) == null);
    }

    /*
     +--------------------------------------------------------------------------+
     | checkStatsBoard()                                                        |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Tests the StatsBoard calculations on a small set of rounds whose         |
     | answers were worked out by hand.                                         |
     |                                                                          |
     | It checks that:                                                          |
     | - each tee gets one row for its 18-hole rounds;                          |
     | - nine-hole rounds get their own rows: one for a front nine, one for a   |
     |   back nine, and one for a nine-hole course;                             |
     | - the average, best score, putts and each percentage come out right;     |
     | - a figure with nothing to divide by shows "-";                          |
     | - each kind of row shows the right rating and slope;                     |
     | - the two total rows add up all 18-hole and all nine-hole rounds, and    |
     |   are there even when there is nothing to add up;                        |
     | - historical rounds are left out;                                        |
     | - sorting works both ways, with "-" always at the bottom;                |
     | - gold marks the best figure only among rows with two or more rounds,    |
     |   only when at least two rows have that many, and every tie is gold.     |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    private static void checkStatsBoard()
    {
        Tee links = testTee(0, 0);
        int[] pars = links.pars();
        int[] ninePars = {4, 3, 5, 4, 4, 3, 5, 4, 4};
        int[] nineIndex = {1, 2, 3, 4, 5, 6, 7, 8, 9};
        Tee park = new Tee(2, "Nine Park", "", "", "", "Red", Tee.NINE, 34.0, 110, 3000,
                ninePars, nineIndex, 0, 0, 0, 0);
        Tee other = new Tee(3, "other course", "", "", "", "Blue", Tee.HOLES, 71.0, 125, 6400,
                pars, links.strokeIndex(), 0, 0, 0, 0);
        Tee single = new Tee(4, "Single Round", "", "", "", "Gold", Tee.HOLES, 69.0, 118, 6100,
                pars, links.strokeIndex(), 0, 0, 0, 0);
        Map<Integer, Tee> tees = Map.of(1, links, 2, park, 3, other, 4, single);

        // Round A on Test Links: par everywhere but hole 1 (5, 3 putts) and hole 2 (3, 1 putt);
        // fairways on holes 1 and 3; sand on hole 1 (bogey, no save) and hole 2 (par, a save)
        int[] scoresA = pars.clone();
        int[] puttsA = new int[Tee.HOLES];
        Arrays.fill(puttsA, PLAIN_PUTTS);
        scoresA[0] = 5;
        puttsA[0] = 3;
        puttsA[1] = 1;
        boolean[] fairwayA = new boolean[Tee.HOLES];
        fairwayA[0] = true;
        fairwayA[2] = true;
        boolean[] sandA = new boolean[Tee.HOLES];
        sandA[0] = true;
        sandA[1] = true;
        Round roundA = new Round(10, 1, LocalDate.parse("2026-05-01"), 0, 0, Tee.HOLES, scoresA, puttsA, fairwayA, sandA);
        Round roundB = roundOver(11, Tee.HOLES, "2026-05-02", links);
        Round oldRound = new Round(12, 1, LocalDate.parse("2005-06-19"), 0, 0, Tee.HOLES, pars.clone(), puttsA.clone(),
                new boolean[Tee.HOLES], new boolean[Tee.HOLES], true);
        int[] birdies = pars.clone();
        for (int hole = 0; hole < 5; hole = hole + 1)
        {
            birdies[hole] = birdies[hole] - 1;
        }
        int[] onePutts = new int[Tee.HOLES];
        Arrays.fill(onePutts, 1);
        Round singleRound = new Round(13, 4, LocalDate.parse("2026-05-03"), 0, 0, Tee.HOLES, birdies, onePutts,
                new boolean[Tee.HOLES], new boolean[Tee.HOLES]);
        List<Round> rounds = new ArrayList<>(List.of(roundA, roundB, oldRound, singleRound,
                roundOver(14, 0, "2026-05-04", other), roundOver(15, 0, "2026-05-05", other),
                nineRound(16, 0, 9, "2026-05-06", links), nineRound(17, 9, 0, "2026-05-07", links)));
        int[] parkScores = {4, 3, 5, 4, 4, 3, 5, 4, 5};
        int[] parkPutts = {2, 2, 2, 2, 2, 2, 2, 2, 2};
        boolean[] noNine = new boolean[Tee.NINE];
        rounds.add(new Round(18, 2, LocalDate.parse("2026-05-08"), 0, 0, Tee.NINE, parkScores, parkPutts, noNine, noNine));
        rounds.add(new Round(19, 2, LocalDate.parse("2026-05-09"), 0, 0, Tee.NINE, parkScores, parkPutts, noNine, noNine));

        StatsBoard.Board board = StatsBoard.build(rounds, tees);
        check("one 18-hole row per tee played, in course order (any capitals)", board.eighteen().size() == 3
                && board.eighteen().get(0).label().equals("other course:Blue")
                && board.eighteen().get(1).label().equals("Single Round:Gold")
                && board.eighteen().get(2).label().equals("Test Links:White"));
        StatsBoard.Row links18 = board.eighteen().get(2);
        check("historical rounds are left out of the board", links18.rounds() == 2 && links18.best() == 73);
        check("the average of 73 and 90 is 81.5", links18.text(StatsBoard.Column.AVERAGE).equals("81.5"));
        check("the best is the lowest score", links18.text(StatsBoard.Column.BEST).equals("73"));
        check("putts per round: 36 and 36 make 36", links18.text(StatsBoard.Column.PUTTS).equals("36"));
        check("fairways: 2 of the 28 par 4s and 5s is 7.1%", links18.text(StatsBoard.Column.FAIRWAY).equals("7.1"));
        check("greens in regulation: 17 of 36 holes is 47.2%", links18.text(StatsBoard.Column.GIR).equals("47.2"));
        check("two putts or fewer: 35 of 36 holes is 97.2%", links18.text(StatsBoard.Column.TWO_PUTT).equals("97.2"));
        check("sand: 1 save from 2 sand holes is 50% (no .0)", links18.text(StatsBoard.Column.SAND).equals("50"));
        check("an 18-hole row shows the tee's rating and slope", links18.text(StatsBoard.Column.RATING).equals("70.0 / 113"));
        check("a row with no sand holes shows - for Sand %", board.eighteen().get(0).text(StatsBoard.Column.SAND).equals("-"));

        check("nine-hole rows: front, back and the nine-hole course, never mixed with 18", board.nine().size() == 3
                && board.nine().get(0).label().equals("Nine Park:Red")
                && board.nine().get(1).label().equals("Test Links:White (Back 9)")
                && board.nine().get(2).label().equals("Test Links:White (Front 9)"));
        check("a front nine row shows that nine's rating", board.nine().get(2).text(StatsBoard.Column.RATING).equals("35.0 / 113"));
        check("a nine with no rating shows -", board.nine().get(1).text(StatsBoard.Column.RATING).equals("-"));
        check("a nine-hole course shows its own rating", board.nine().get(0).text(StatsBoard.Column.RATING).equals("34.0 / 110"));
        check("a nine-hole row's average is over nine holes", board.nine().get(2).text(StatsBoard.Column.AVERAGE).equals("45")
                && board.nine().get(0).text(StatsBoard.Column.AVERAGE).equals("37"));
        check("a nine-hole row's putts are per nine-hole round", board.nine().get(0).text(StatsBoard.Column.PUTTS).equals("18"));

        StatsBoard.Row total18 = board.allEighteen();
        check("the 18-hole total covers every 18-hole round but the historical one", total18.rounds() == 5
                && total18.text(StatsBoard.Column.BEST).equals("67") && total18.label().equals("All 18-hole rounds"));
        check("the 18-hole total's average is (73 + 90 + 67 + 72 + 72) / 5", total18.text(StatsBoard.Column.AVERAGE).equals("74.8"));
        check("the totals leave Rating\\Slope empty", total18.text(StatsBoard.Column.RATING).isEmpty());
        check("the nine-hole total covers the four nine-hole rounds", board.allNine() != null && board.allNine().rounds() == 4
                && board.allNine().text(StatsBoard.Column.BEST).equals("36"));
        StatsBoard.Board eighteensOnly = StatsBoard.build(List.of(roundA, roundB), tees);
        check("the nine-hole total is there even with no nine-hole rounds: 0 rounds and dashes", eighteensOnly.nine().isEmpty()
                && eighteensOnly.allNine().rounds() == 0 && eighteensOnly.allNine().text(StatsBoard.Column.ROUNDS).equals("0")
                && eighteensOnly.allNine().text(StatsBoard.Column.AVERAGE).equals("-")
                && eighteensOnly.allNine().label().equals("All nine-hole rounds"));
        StatsBoard.Board empty = StatsBoard.build(List.of(), tees);
        check("with no rounds the board is empty and the total has nothing to show", empty.eighteen().isEmpty()
                && empty.allEighteen().rounds() == 0 && empty.allEighteen().text(StatsBoard.Column.AVERAGE).equals("-"));

        List<StatsBoard.Row> byAverage = StatsBoard.sort(board.eighteen(), StatsBoard.Column.AVERAGE, true);
        check("sorting by Avg puts the lowest first", byAverage.get(0).teeId() == 4 && byAverage.get(1).teeId() == 3
                && byAverage.get(2).teeId() == 1);
        List<StatsBoard.Row> byAverageDown = StatsBoard.sort(board.eighteen(), StatsBoard.Column.AVERAGE, false);
        check("sorting again reverses it", byAverageDown.get(0).teeId() == 1 && byAverageDown.get(2).teeId() == 4);
        List<StatsBoard.Row> bySand = StatsBoard.sort(board.eighteen(), StatsBoard.Column.SAND, true);
        List<StatsBoard.Row> bySandDown = StatsBoard.sort(board.eighteen(), StatsBoard.Column.SAND, false);
        check("a - sorts last going up", bySand.get(0).teeId() == 1);
        check("a - sorts last going down too", bySandDown.get(0).teeId() == 1
                && Double.isNaN(bySandDown.get(1).sandPct()) && Double.isNaN(bySandDown.get(2).sandPct()));
        List<StatsBoard.Row> byName = StatsBoard.sort(board.eighteen(), StatsBoard.Column.COURSE, false);
        check("sorting by name backwards", byName.get(0).teeId() == 1 && byName.get(2).teeId() == 3);
        List<StatsBoard.Row> byRating = StatsBoard.sort(board.nine(), StatsBoard.Column.RATING, true);
        check("sorting by rating puts a nine with no rating last", byRating.get(0).teeId() == 2
                && byRating.get(2).part() == StatsBoard.Part.BACK);

        Map<StatsBoard.Column, Set<String>> gold = StatsBoard.best(board.eighteen());
        check("gold: the lowest average among rows with 2 or more rounds", gold.get(StatsBoard.Column.AVERAGE).equals(Set.of("3:EIGHTEEN")));
        check("gold: a row with one round never counts, however good", !gold.get(StatsBoard.Column.BEST).contains("4:EIGHTEEN")
                && gold.get(StatsBoard.Column.BEST).equals(Set.of("3:EIGHTEEN")));
        check("gold: the highest fairway percentage", gold.get(StatsBoard.Column.FAIRWAY).equals(Set.of("1:EIGHTEEN")));
        check("gold: ties are all gold", gold.get(StatsBoard.Column.PUTTS).equals(Set.of("1:EIGHTEEN", "3:EIGHTEEN")));
        check("gold: a - is never gold, the figure that exists is", gold.get(StatsBoard.Column.SAND).equals(Set.of("1:EIGHTEEN")));
        check("gold: Rnds and Rating have no gold", !gold.containsKey(StatsBoard.Column.ROUNDS) && !gold.containsKey(StatsBoard.Column.RATING));
        check("gold: none when fewer than 2 rows have 2 or more rounds", StatsBoard.best(board.nine()).isEmpty());

        check("numbers: one decimal", StatsBoard.number(14.2857).equals("14.3"));
        check("numbers: a trailing .0 is dropped", StatsBoard.number(43.0).equals("43"));
        check("numbers: nothing to show is -", StatsBoard.number(Double.NaN).equals("-"));
    }

    /*
     +--------------------------------------------------------------------------+
     | checkHistorical()                                                        |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Checks historical rounds (old rounds kept only to look back at): the     |
     | optional Historical column loads Y, YES, N and blank and refuses         |
     | anything else; a historical round is left out of the handicap and the    |
     | statistics but stays among the loaded rounds; an unrated nine marked     |
     | historical gives no warning; and editing a historical round keeps its    |
     | Y in the file.                                                           |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing (throws IOException if a temporary file fails)       |
     +--------------------------------------------------------------------------+
     */
    private static void checkHistorical() throws IOException
    {
        Tee tee = testTee(0, 0);
        Map<Integer, Tee> tees = Map.of(1, tee);
        String header = "RoundID\tTeeID\tDate\tHoles\tScores\tPutts\tFairways\tSand\tHistorical";
        String twos = "2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2";
        String zeros = "0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0";
        String eighty = "4,4,4,4,4,4,4,4,4,5,5,5,5,5,5,5,5,4";
        String nineties = "5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,6";
        String nine = "5,5,5,5,5,5,5,5,5";
        String nineTwos = "2,2,2,2,2,2,2,2,2";
        String nineZeros = "0,0,0,0,0,0,0,0,0";
        List<String> lines = List.of(header,
                roundRow(1, 1, "2005-06-19", "", eighty, twos, zeros, zeros) + "\tY",
                roundRow(2, 1, "2005-06-26", "", eighty, twos, zeros, zeros) + "\tyes",
                roundRow(3, 1, "2026-03-01", "", nineties, twos, zeros, zeros) + "\t",
                roundRow(4, 1, "2026-03-02", "", nineties, twos, zeros, zeros) + "\tN",
                roundRow(5, 1, "2026-03-03", "", nineties, twos, zeros, zeros) + "\tX",
                roundRow(6, 1, "2005-07-04", "B", nine, nineTwos, nineZeros, nineZeros) + "\tY");
        List<Problem> problems = new ArrayList<>();
        List<Round> rounds = DataLoader.parseRounds(lines, tees, problems);
        check("Historical Y and yes load as historical", rounds.get(0).historical() && rounds.get(1).historical());
        check("Historical blank and N load as ordinary rounds", !rounds.get(2).historical() && !rounds.get(3).historical());
        check("any other Historical value skips the row with an error", rounds.size() == 5
                && problems.stream().anyMatch(problem -> problem.severity() == Problem.Severity.ERROR
                        && problem.message().contains("Historical must be blank, Y or N")));
        check("a historical nine with no rating gives no warning", rounds.get(4).historical()
                && problems.stream().noneMatch(problem -> problem.message().contains("no back nine rating")));
        check("rounds made without the flag are not historical", !roundOver(9, 0, "2026-01-01", tee).historical());

        List<Round> current = Stats.current(rounds);
        check("Stats.current leaves out the historical rounds", current.size() == 2
                && current.get(0).id() == 3 && current.get(1).id() == 4);
        Stats.Summary statistics = Stats.summarize(current, tees);
        check("the statistics count only the ordinary rounds", statistics.rounds() == 2 && statistics.bestScore18() == 91);

        Handicap.Result withHistory = Handicap.compute(rounds, tees);
        Handicap.Result without = Handicap.compute(current, tees);
        check("historical rounds are left out of the handicap", withHistory.entries().size() == 2
                && withHistory.entries().stream().noneMatch(entry -> entry.round().historical()));
        check("the index is the same with or without the historical rounds",
                Objects.equals(withHistory.currentIndex(), without.currentIndex())
                        && withHistory.currentDifferentials().equals(without.currentDifferentials()));

        Path temp = Files.createTempDirectory("golfanalyst-historical-test");
        Path roundsFile = temp.resolve(DataLoader.ROUNDS_FILE);
        String original = header + "\r\n" + lines.get(1) + "\r\n" + lines.get(3) + "\r\n";
        Files.writeString(roundsFile, original, StandardCharsets.UTF_8);
        Round old = rounds.get(0);
        int[] fixedScores = old.scores().clone();
        fixedScores[0] = 3;
        Round fixed = new Round(old.id(), old.teeId(), old.date(), old.pcc(), old.startHole(), old.holes(),
                fixedScores, old.putts(), old.fairway(), old.sand(), true);
        RoundWriter.replace(roundsFile, fixed, tee);
        List<String> after = Files.readAllLines(roundsFile, StandardCharsets.UTF_8);
        check("editing a historical round keeps its Y", after.get(1).startsWith("1\t1\t2005-06-19\t") && after.get(1).endsWith("\tY")
                && after.get(1).contains("\t3,4,4,"));
        check("editing it leaves the other round alone", after.get(2).equals(lines.get(3)));

        // A file from before the Historical column: marking a round adds the column to the header only
        String oldHeader = "RoundID\tTeeID\tDate\tHoles\tScores\tPutts\tFairways\tSand";
        String oldLine = "1\t1\t2005-06-19\t\t" + eighty + "\t" + twos + "\t" + zeros + "\t" + zeros;
        String oldFile = "# my rounds\r\n" + oldHeader + "\r\n" + oldLine + "\r\n";
        Files.writeString(roundsFile, oldFile, StandardCharsets.UTF_8);
        RoundWriter.replace(roundsFile, fixed, tee);
        String marked = Files.readString(roundsFile, StandardCharsets.UTF_8);
        check("marking a round historical in an older file adds the column to the header",
                marked.startsWith("# my rounds\r\n" + oldHeader + "\tHistorical\r\n1\t1\t2005-06-19\t") && marked.endsWith("\tY\r\n"));
        Files.writeString(roundsFile, oldFile, StandardCharsets.UTF_8);
        RoundWriter.append(roundsFile, rounds.get(2), tee);
        check("an ordinary round added to an older file leaves the header alone",
                Files.readString(roundsFile, StandardCharsets.UTF_8).startsWith("# my rounds\r\n" + oldHeader + "\r\n" + oldLine + "\r\n"));
        RoundWriter.append(roundsFile, fixed, tee);
        List<String> appended = Files.readAllLines(roundsFile, StandardCharsets.UTF_8);
        check("a historical round added to an older file adds the column and its Y", appended.get(1).equals(oldHeader + "\tHistorical")
                && appended.get(2).equals(oldLine) && appended.get(appended.size() - 1).endsWith("\tY"));
        Files.deleteIfExists(temp.resolve(DataLoader.ROUNDS_FILE + RoundWriter.BACKUP_SUFFIX));
        Files.deleteIfExists(roundsFile);
        Files.deleteIfExists(temp);
    }

    /*
     +--------------------------------------------------------------------------+
     | checkRoundReplace()                                                      |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Checks RoundWriter.replace on a file in a temporary folder that has      |
     | comments, Windows line endings, an extra column of your own and three    |
     | rounds: only the edited round's line changes (same RoundID, the extra    |
     | column kept), every other character of the file stays as it was, the     |
     | backup holds the file as it was, and a RoundID that is missing or on     |
     | two lines is refused with the file left alone.                           |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing (throws IOException if a temporary file fails)       |
     +--------------------------------------------------------------------------+
     */
    private static void checkRoundReplace() throws IOException
    {
        Path temp = Files.createTempDirectory("golfanalyst-replace-test");
        Path rounds = temp.resolve(DataLoader.ROUNDS_FILE);
        Path backup = temp.resolve(DataLoader.ROUNDS_FILE + RoundWriter.BACKUP_SUFFIX);
        String nine = "5,5,5,5,5,5,5,5,5";
        String twos = "2,2,2,2,2,2,2,2,2";
        String zeros = "0,0,0,0,0,0,0,0,0";
        String first = "1\t1\t2026-03-01\tF\t\t" + nine + "\t" + twos + "\t" + zeros + "\t" + zeros + "\tfirst note";
        String second = "2\t1\t2026-03-02\tF\t\t" + nine + "\t" + twos + "\t" + zeros + "\t" + zeros + "\tkeep this note";
        String third = "3\t1\t2026-03-03\tF\t\t" + nine + "\t" + twos + "\t" + zeros + "\t" + zeros + "\t";
        String original = "# my rounds\r\n" + RoundWriter.STANDARD_HEADER + "\tNotes\r\n" + first + "\r\n# a comment between\r\n\r\n"
                + second + "\r\n" + third + "\r\n";
        Files.writeString(rounds, original, StandardCharsets.UTF_8);

        Tee tee = testTee(35.0, 113);
        int[] scores = {4, 5, 5, 5, 5, 5, 5, 5, 6};
        int[] putts = {2, 2, 2, 2, 2, 2, 2, 2, 2};
        boolean[] fairway = new boolean[Tee.NINE];
        boolean[] sand = new boolean[Tee.NINE];
        fairway[0] = true;
        Round edited = new Round(2, 1, LocalDate.of(2026, 3, 9), 1, 0, Tee.NINE, scores, putts, fairway, sand);
        RoundWriter.replace(rounds, edited, tee);
        String after = Files.readString(rounds, StandardCharsets.UTF_8);
        String newLine = "2\t1\t2026-03-09\tF\t1\t4,5,5,5,5,5,5,5,6\t" + twos + "\t1,0,0,0,0,0,0,0,0\t" + zeros + "\tkeep this note";
        check("replacing a round changes only its line", after.equals(original.replace(second, newLine)));
        check("the edited line keeps its RoundID and the column of your own", after.contains("\r\n" + newLine + "\r\n"));
        check("comments, the other rounds and the Windows line endings are untouched", after.startsWith("# my rounds\r\n")
                && after.contains("\r\n" + first + "\r\n# a comment between\r\n\r\n") && after.endsWith("\r\n" + third + "\r\n"));
        check("the file as it was is kept in rounds.tsv.bak", Files.readString(backup, StandardCharsets.UTF_8).equals(original));
        List<Problem> problems = new ArrayList<>();
        Map<Integer, Tee> tees = Map.of(1, tee);
        List<Round> reloaded = DataLoader.parseRounds(Files.readAllLines(rounds), tees, problems);
        check("the edited file loads with no errors", reloaded.size() == 3
                && problems.stream().noneMatch(problem -> problem.severity() == Problem.Severity.ERROR));

        Round missing = new Round(99, 1, LocalDate.of(2026, 3, 9), 0, 0, Tee.NINE, scores, putts, fairway, sand);
        boolean refused = false;
        try
        {
            RoundWriter.replace(rounds, missing, tee);
        }
        catch (IOException expected)
        {
            refused = expected.getMessage().contains("not in");
        }
        check("a RoundID that is not in the file is refused", refused);
        check("a refused edit leaves the file alone", Files.readString(rounds, StandardCharsets.UTF_8).equals(after));

        Files.writeString(rounds, after + third + "\r\n", StandardCharsets.UTF_8);
        Round twice = new Round(3, 1, LocalDate.of(2026, 3, 9), 0, 0, Tee.NINE, scores, putts, fairway, sand);
        refused = false;
        try
        {
            RoundWriter.replace(rounds, twice, tee);
        }
        catch (IOException expected)
        {
            refused = expected.getMessage().contains("more than one");
        }
        check("a RoundID on two lines is refused", refused);

        for (Path file : List.of(rounds, backup))
        {
            Files.deleteIfExists(file);
        }
        Files.deleteIfExists(temp);
    }

    /*
     +--------------------------------------------------------------------------+
     | checkCourseFiles()                                                       |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Checks CourseWriter and the course parts of DataStore on files in a      |
     | temporary folder: the file's own column order (and a column of your      |
     | own) is kept, Windows line endings and comments are kept, a nine-hole    |
     | tee is written with 9 holes, what is written loads back the same, a      |
     | duplicate TeeID is refused with the file left as it was, the next        |
     | TeeID, finding a course whatever the capitals, the sample folder is      |
     | known by its name, and an empty data folder gets header rows only and    |
     | never writes over a file.                                                |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing (throws IOException if a temporary file fails)       |
     +--------------------------------------------------------------------------+
     */
    private static void checkCourseFiles() throws IOException
    {
        Path temp = Files.createTempDirectory("golfanalyst-course-test");
        Path courses = temp.resolve(DataLoader.COURSES_FILE);
        String parsText = "4,3,5,4,4,3,5,4,4,4,3,5,4,4,3,5,4,4";
        String indexText = "1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18";
        String header = "Course\tTeeID\tTee\tMyNotes\tRating\tSlope\tPars\tStrokeIndex\tZip\tCity\tState\tHoles\tYardage"
                + "\tFrontRating\tFrontSlope\tBackRating\tBackSlope";
        String first = "Old Links\t1\tWhite\tkeep me\t70.0\t113\t" + parsText + "\t" + indexText + "\t29000\tAnytown\tSC\t18\t6000\t\t\t\t";
        Files.writeString(courses, "# my comment\r\n" + header + "\r\n" + first + "\r\n", StandardCharsets.UTF_8);
        Files.writeString(temp.resolve(DataLoader.ROUNDS_FILE), RoundWriter.STANDARD_HEADER + "\n", StandardCharsets.UTF_8);

        DataStore store = new DataStore(temp);
        check("next TeeID is one more than the highest", store.nextTeeId() == 2);
        check("a course is found whatever the capitals and spaces", store.teesForCourse("  old   LINKS ", "29000").size() == 1);
        check("the same name with another zip is another course", store.teesForCourse("Old Links", "29001").isEmpty());

        int[] pars = {4, 3, 5, 4, 4, 3, 5, 4, 4, 4, 3, 5, 4, 4, 3, 5, 4, 4};
        int[] index = new int[Tee.HOLES];
        for (int hole = 0; hole < Tee.HOLES; hole = hole + 1)
        {
            index[hole] = Tee.HOLES - hole;
        }
        Tee blue = new Tee(2, "Old Links", "Anytown", "SC", "29000", "Blue", Tee.HOLES, 72.3, 131, 0,
                pars, index, 36.0, 128, 36.3, 134);
        Tee nine = new Tee(3, "Old Links", "Anytown", "SC", "29000", "Short", Tee.NINE, 33.8, 112, 2800,
                Arrays.copyOf(pars, Tee.NINE), new int[] {5, 9, 1, 7, 3, 8, 2, 6, 4}, 0, 0, 0, 0);
        String[] cells = header.split("\t");
        String line = CourseWriter.formatLine(blue, cells);
        check("a course line follows the file's own column order, with a blank for a column of your own",
                line.startsWith("Old Links\t2\tBlue\t\t72.3\t131\t" + parsText + "\t18,17,"));
        check("a blank yardage is written as blank", line.split("\t", -1)[12].isEmpty());

        store.addTees(List.of(blue, nine));
        String after = Files.readString(courses, StandardCharsets.UTF_8);
        check("adding tees keeps the comment and the first line", after.startsWith("# my comment\r\n" + header + "\r\n" + first + "\r\n"));
        check("adding tees keeps the Windows line endings", !after.replace("\r\n", "").contains("\n"));
        check("the store reloads with the new tees", store.dataset().tees().size() == 3 && store.dataset().errorCount() == 0);
        Tee readBlue = store.teeMap().get(2);
        Tee readNine = store.teeMap().get(3);
        check("a written tee loads back the same", readBlue != null && readBlue.rating() == 72.3 && readBlue.slope() == 131
                && readBlue.strokeIndex()[0] == 18 && readBlue.frontRating() == 36.0 && readBlue.backSlope() == 134
                && readBlue.city().equals("Anytown") && readBlue.yardage() == 0);
        check("a nine-hole tee is written with 9 holes and loads back", readNine != null && readNine.holes() == Tee.NINE
                && readNine.pars().length == Tee.NINE && readNine.yardage() == 2800);
        check("next TeeID moves on after adding", store.nextTeeId() == 4);

        Tee clash = new Tee(1, "New Place", "", "", "29500", "Red", Tee.HOLES, 70.0, 113, 0, pars, index, 0, 0, 0, 0);
        boolean refused = false;
        try
        {
            store.addTees(List.of(clash));
        }
        catch (IOException expected)
        {
            refused = expected.getMessage().contains("duplicate TeeID");
        }
        check("a TeeID already used is refused", refused);
        check("a refused tee leaves the file as it was", Files.readString(courses, StandardCharsets.UTF_8).equals(after));

        Tee whiteFixed = new Tee(1, "Old Links", "Anytown", "SC", "29000", "White", Tee.HOLES, 70.2, 114, 6000,
                pars, index, 0, 0, 0, 0);
        Tee green = new Tee(4, "Old Links", "Anytown", "SC", "29000", "Green", Tee.HOLES, 66.0, 110, 0, pars, index, 0, 0, 0, 0);
        store.updateTees(List.of(whiteFixed), List.of(green));
        String updated = Files.readString(courses, StandardCharsets.UTF_8);
        String[] beforeLines = after.split("\r\n", -1);
        String[] updatedLines = updated.split("\r\n", -1);
        boolean onlyWhite = updatedLines.length == beforeLines.length + 1;
        for (int lp = 0; onlyWhite && lp < beforeLines.length - 1; lp = lp + 1)
        {
            onlyWhite = beforeLines[lp].equals(updatedLines[lp]) || beforeLines[lp].startsWith("Old Links\t1\t");
        }
        check("editing a tee replaces only its line, adds the new tee at the end, keeps the line endings", onlyWhite
                && updated.startsWith("# my comment\r\n" + header + "\r\n") && updated.endsWith("\r\n")
                && updatedLines[updatedLines.length - 2].startsWith("Old Links\t4\tGreen\t"));
        check("the edited line has the new numbers and keeps the column of your own",
                updated.contains("Old Links\t1\tWhite\tkeep me\t70.2\t114\t"));
        check("the file as it was is kept in courses.tsv.bak",
                Files.readString(temp.resolve("courses.tsv.bak"), StandardCharsets.UTF_8).equals(after));
        check("the store reloads with the edited and the new tee", store.teeMap().get(1).rating() == 70.2
                && store.teeMap().get(4) != null && store.dataset().errorCount() == 0);
        int[] badPars = pars.clone();
        badPars[0] = 7;
        Tee badFix = new Tee(2, "Old Links", "Anytown", "SC", "29000", "Blue", Tee.HOLES, 72.3, 131, 0,
                badPars, index, 0, 0, 0, 0);
        refused = false;
        try
        {
            store.updateTees(List.of(badFix), List.of());
        }
        catch (IOException expected)
        {
            refused = expected.getMessage().contains("would not load");
        }
        check("an edit that would not load is refused", refused);
        check("a refused edit leaves the file as it was", Files.readString(courses, StandardCharsets.UTF_8).equals(updated));
        after = updated;

        Path fresh = temp.resolve("fresh");
        CourseWriter.append(fresh.resolve(DataLoader.COURSES_FILE), List.of(clash));
        List<String> freshLines = Files.readAllLines(fresh.resolve(DataLoader.COURSES_FILE));
        check("a new courses file gets the standard header first", freshLines.size() == 2
                && freshLines.get(0).equals(CourseWriter.STANDARD_HEADER) && freshLines.get(1).startsWith("1\tNew Place\t"));

        check("a folder not named sample-data is not the sample data", !store.isSampleData());
        Path sample = temp.resolve(DataStore.SAMPLE_FOLDER);
        Files.createDirectories(sample);
        Files.writeString(sample.resolve(DataLoader.COURSES_FILE), CourseWriter.STANDARD_HEADER + "\n", StandardCharsets.UTF_8);
        Files.writeString(sample.resolve(DataLoader.ROUNDS_FILE), RoundWriter.STANDARD_HEADER + "\n", StandardCharsets.UTF_8);
        DataStore sampleStore = new DataStore(sample);
        check("a folder named sample-data is the sample data", sampleStore.isSampleData());
        check("the golfer's own data folder goes beside it", sampleStore.dataFolderBeside().equals(temp.toAbsolutePath().normalize().resolve("data")));

        Path empty = temp.resolve("empty");
        DataStore.createEmptyFolder(empty);
        check("an empty data folder has the header rows only",
                Files.readAllLines(empty.resolve(DataLoader.COURSES_FILE)).equals(List.of(CourseWriter.STANDARD_HEADER))
                        && Files.readAllLines(empty.resolve(DataLoader.ROUNDS_FILE)).equals(List.of(RoundWriter.STANDARD_HEADER)));
        Files.writeString(empty.resolve(DataLoader.COURSES_FILE), "# mine\n", StandardOpenOption.APPEND);
        String mine = Files.readString(empty.resolve(DataLoader.COURSES_FILE));
        DataStore.createEmptyFolder(empty);
        check("making the data folder again never writes over a file", Files.readString(empty.resolve(DataLoader.COURSES_FILE)).equals(mine));

        List<Path> paths = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(temp))
        {
            walk.forEach(paths::add);
        }
        for (int lp = paths.size() - 1; lp >= 0; lp = lp - 1)
        {
            Files.deleteIfExists(paths.get(lp));
        }
    }
}

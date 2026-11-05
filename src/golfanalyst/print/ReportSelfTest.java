package golfanalyst.print;

import golfanalyst.Handicap;
import golfanalyst.Round;
import golfanalyst.Tee;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 +--------------------------------------------------------------------------+
 | ReportSelfTest                                                           |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | The checks of the reports themselves, on rounds made up here and worked  |
 | out by hand: the Handicap Report, the Round Log and the scorecard. They  |
 | report through PrintSelfTest.check.                                      |
 |                                                                          |
 | Public Methods:                                                          |
 | run() - runs every report check                                          |
 +--------------------------------------------------------------------------+
 */
final class ReportSelfTest
{

    static final int[] PARS = {4, 4, 3, 5, 4, 4, 3, 5, 4, 4, 4, 3, 5, 4, 4, 3, 5, 4};
    static final int[] INDEXES = {7, 15, 1, 9, 11, 17, 3, 13, 5, 8, 16, 2, 10, 12, 18, 4, 14, 6};
    static final int[] NINE_PARS = {4, 3, 5, 4, 4, 3, 5, 4, 4};
    static final int[] NINE_INDEXES = {1, 9, 3, 5, 7, 8, 2, 4, 6};
    static final Tee LINKS = new Tee(1, "Test Links", "Fort Mill", "SC", "29708", "White", Tee.HOLES, 72.0, 113, 6200, PARS,
            INDEXES, 36.0, 113, 36.0, 113);
    static final Tee NINE = new Tee(2, "Little Nine", "Fort Mill", "SC", "29708", "Red", Tee.NINE, 35.0, 113, 2900, NINE_PARS,
            NINE_INDEXES, 0.0, 0, 0.0, 0);

    private ReportSelfTest()
    {
    }

    /*
     * check(name, ok): records one result.
     */
    private static void check(String name, boolean ok)
    {
        PrintSelfTest.check(name, ok);
    }

    /*
     * run(): every report check.
     */
    static void run()
    {
        checkHandicapReport();
        checkRoundLog();
        checkRoundLogPages();
        checkScorecard();
        checkBestRound();
        checkStatsBoard();
        checkMyScorecard();
        checkCourseNotes();
        checkPageNotes();
        checkBoldDaggers();
        checkHistoricalSample();
        try
        {
            checkNames();
        }
        catch (java.io.IOException failure)
        {
            check("names: the sample data could be read (" + failure.getMessage() + ")", false);
        }
        try
        {
            checkSpreadsheets();
        }
        catch (java.io.IOException failure)
        {
            check("spreadsheet: a temporary file could be written (" + failure.getMessage() + ")", false);
        }
    }

    // ------------------------------------------------------------ made-up rounds

    /*
     * tees(): the two made-up sets of tees by TeeID.
     */
    static Map<Integer, Tee> tees()
    {
        Map<Integer, Tee> tees = new HashMap<>();
        tees.put(LINKS.id(), LINKS);
        tees.put(NINE.id(), NINE);
        return tees;
    }

    /*
     +--------------------------------------------------------------------------+
     | round(id, tee, date, start, holes, overPar, putts, fairways, sand, old)  |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | A made-up round: every hole played overPar over its par with the same    |
     | putts; fairway hit on every par 4 and 5 or none; sand on the holes       |
     | listed.                                                                  |
     |                                                                          |
     | Passed in:  id, tee, date - the round's number, tees and day             |
     |             start, holes  - first hole (0 or 9) and how many             |
     |             overPar       - strokes over par on each hole                |
     |             putts         - putts on each hole                           |
     |             fairways      - true to hit every fairway                    |
     |             sand          - holes (0 is the first played) in a bunker    |
     |             old           - true for a historical round                  |
     | Passed out: the round                                                    |
     +--------------------------------------------------------------------------+
     */
    static Round round(int id, Tee tee, String date, int start, int holes, int overPar, int putts, boolean fairways, int[] sand,
            boolean old)
    {
        int[] scores = new int[holes];
        int[] puttCounts = new int[holes];
        boolean[] hit = new boolean[holes];
        boolean[] bunker = new boolean[holes];
        for (int hole = 0; hole < holes; hole = hole + 1)
        {
            scores[hole] = tee.pars()[start + hole] + overPar;
            puttCounts[hole] = putts;
            hit[hole] = fairways && tee.pars()[start + hole] > 3;
        }
        for (int hole : sand)
        {
            bunker[hole] = true;
        }
        return new Round(id, tee.id(), LocalDate.parse(date), 0, start, holes, scores, puttCounts, hit, bunker, old);
    }

    /*
     * data(rounds): report data for made-up rounds (golfer Pat, historical rounds counted, not the sample).
     */
    static ReportData data(List<Round> rounds)
    {
        List<Round> sorted = new ArrayList<>(rounds);
        sorted.sort((one, two) ->
        {
            int byDate = one.date().compareTo(two.date());
            if (byDate != 0)
            {
                return byDate;
            }
            return Integer.compare(one.id(), two.id());
        });
        return new ReportData(sorted, tees(), Handicap.compute(sorted, tees()), "Pat", true, false, PrintSelfTest.TODAY, "4.00");
    }

    /*
     * steady(count): count 18-hole rounds a day apart from January 1, 2026, scores going 0 to 12 over par in a mixed order.
     */
    static List<Round> steady(int count)
    {
        List<Round> rounds = new ArrayList<>();
        LocalDate day = LocalDate.of(2026, 1, 1);
        for (int lp = 0; lp < count; lp = lp + 1)
        {
            int extra = (lp * 7) % 13;
            Round even = round(lp + 1, LINKS, day.plusDays(lp).toString(), 0, Tee.HOLES, 0, 2, true, new int[0], false);
            int[] scores = even.scores().clone();
            for (int hole = 0; hole < extra; hole = hole + 1)
            {
                scores[hole] = scores[hole] + 1;
            }
            rounds.add(new Round(even.id(), even.teeId(), even.date(), 0, 0, Tee.HOLES, scores, even.putts(), even.fairway(), even.sand()));
        }
        return rounds;
    }

    // ------------------------------------------------------------ Handicap Report

    /*
     +--------------------------------------------------------------------------+
     | shadedRight(report)                                                      |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Whether the shaded rows are exactly the rounds that make the index: as   |
     | many as the rules use for the rounds on record, each no higher than any  |
     | unshaded listed round, their average giving today's index, and the       |
     | table's shaded rows the same rounds.                                     |
     |                                                                          |
     | Passed in:  report - the Handicap Report                                 |
     | Passed out: true if all of that holds                                    |
     +--------------------------------------------------------------------------+
     */
    private static boolean shadedRight(HandicapReport report)
    {
        Handicap.Result result = report.data().handicap();
        int used = Handicap.differentialsUsed(result.currentDifferentials().size());
        double highestShaded = -1000;
        double lowestOther = 1000;
        double total = 0;
        int shaded = 0;
        Table table = report.table();
        boolean rowsMatch = true;
        for (int lp = 0; lp < report.entries().size(); lp = lp + 1)
        {
            Handicap.Entry entry = report.entries().get(lp);
            boolean counts = report.counts(entry);
            rowsMatch = rowsMatch && (table.rows().get(lp).kind() == Table.Kind.SHADED) == counts;
            if (counts)
            {
                shaded = shaded + 1;
                total = total + entry.differential();
                highestShaded = Math.max(highestShaded, entry.differential());
            }
            else
            {
                lowestOther = Math.min(lowestOther, entry.differential());
            }
        }
        double index = Handicap.round1(total / shaded);
        return shaded == used && highestShaded <= lowestOther && rowsMatch && Math.abs(index - result.currentIndex()) < 1e-9;
    }

    /*
     +--------------------------------------------------------------------------+
     | checkHandicapReport()                                                    |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The Handicap Report with 25, 20, 12 and 2 rounds: the last 20 listed     |
     | newest first, "8 of 20" (or 4 of 12), exactly the counting rounds        |
     | shaded and the first note naming them; with 2 rounds no index, so "No    |
     | index yet", nothing shaded and no index note; nine-hole rounds with      |
     | their dagger, "9" and "Front 9", and the dagger note only when one is    |
     | listed. One page always.                                                 |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    private static void checkHandicapReport()
    {
        HandicapReport many = new HandicapReport(data(steady(25)));
        check("handicap report: 25 rounds list the last 20, newest first", many.entries().size() == 20
                && many.entries().get(0).round().id() == 25 && many.entries().get(19).round().id() == 6
                && many.subtitle(0).equals("Pat   -   your last 20 rounds, as of November 1, 2026") && many.pageCount(1100) == 1);
        check("handicap report: 25 rounds shade exactly the 8 that count", shadedRight(many) && many.countedWords().equals("8 of 20"));
        String note = many.notes().get(0);
        check("handicap report: the first note names the shaded rounds and the index", note.startsWith("Your 8 lowest differentials "
                + "(the shaded rounds, most recent first): ") && note.endsWith(", so your Handicap Index is "
                + Handicap.format(many.data().handicap().currentIndex()) + ".")
                && many.notes().contains(HandicapReport.ADJ_NOTE) && many.notes().contains(HandicapReport.FORMULA_NOTE)
                && !many.notes().contains(HandicapReport.DAGGER_NOTE));
        HandicapReport twenty = new HandicapReport(data(steady(20)));
        check("handicap report: 20 rounds shade 8 of 20", twenty.entries().size() == 20 && shadedRight(twenty)
                && twenty.countedWords().equals("8 of 20"));
        HandicapReport twelve = new HandicapReport(data(steady(12)));
        check("handicap report: 12 rounds list 12 and shade the 4 that count", twelve.entries().size() == 12 && shadedRight(twelve)
                && twelve.countedWords().equals("4 of 12") && twelve.notes().get(0).startsWith("Your 4 lowest differentials")
                && twelve.subtitle(0).endsWith("your last 12 rounds, as of November 1, 2026"));
        HandicapReport two = new HandicapReport(data(steady(2)));
        boolean noneShaded = true;
        for (Table.Row row : two.table().rows())
        {
            noneShaded = noneShaded && row.kind() != Table.Kind.SHADED;
        }
        BufferedImage page = PrintSelfTest.picture(two, 0, 1100);
        check("handicap report: with no index yet the rounds are still listed and nothing is shaded", two.entries().size() == 2
                && two.data().handicap().currentIndex() == null && noneShaded && !two.notes().get(0).startsWith("Your")
                && two.notes().get(0).equals(HandicapReport.ADJ_NOTE) && PrintSelfTest.near(page, 90, 205, 320, 250, Sheet.BROWN) > 100
                && PrintSelfTest.near(page, 330, 195, 780, 220, Sheet.INK) > 50);

        List<Round> mixed = steady(6);
        mixed.add(round(40, NINE, "2026-03-01", 0, Tee.NINE, 1, 2, true, new int[0], false));
        mixed.add(round(41, LINKS, "2026-03-02", 0, Tee.NINE, 1, 2, true, new int[0], false));
        HandicapReport nines = new HandicapReport(data(mixed));
        Table table = nines.table();
        check("handicap report: nine-hole rounds show 9 or Front 9 and a dagger, and the dagger note", nines.entries().size() == 8
                && table.rows().get(0).cells().get(2).equals("Front 9") && table.rows().get(0).cells().get(6).equals(Sheet.DAGGER)
                && table.rows().get(1).cells().get(2).equals("9") && table.rows().get(1).cells().get(6).equals(Sheet.DAGGER)
                && table.rows().get(2).cells().get(2).equals("18") && table.rows().get(2).cells().get(6).isEmpty()
                && nines.notes().contains(HandicapReport.DAGGER_NOTE) && table.columns().get(5).title().equals("Differential")
                && table.width() == 730);
    }

    // ------------------------------------------------------------ Round Log

    /*
     * logRounds(): the small set worked out by hand (see checkRoundLog).
     */
    static List<Round> logRounds()
    {
        List<Round> rounds = new ArrayList<>();
        rounds.add(round(1, LINKS, "2026-01-10", 0, Tee.HOLES, 0, 2, true, new int[0], false));
        rounds.add(round(2, LINKS, "2026-02-10", 0, Tee.HOLES, 1, 2, false, new int[] {0, 1}, false));
        Round front = round(3, LINKS, "2026-03-10", 0, Tee.NINE, 0, 2, true, new int[] {0}, false);
        boolean[] someFairways = new boolean[Tee.NINE];
        someFairways[0] = true;
        someFairways[1] = true;
        someFairways[3] = true;
        rounds.add(new Round(3, LINKS.id(), front.date(), 0, 0, Tee.NINE, front.scores(), front.putts(), someFairways, front.sand()));
        rounds.add(round(4, LINKS, "2025-12-01", 0, Tee.HOLES, 0, 2, true, new int[0], true));
        return rounds;
    }

    /*
     * cell(table, row, heading): the words in a row under a heading.
     */
    static String cell(Table table, int row, String heading)
    {
        for (int col = 0; col < table.columns().size(); col = col + 1)
        {
            if (table.columns().get(col).title().equals(heading))
            {
                return table.rows().get(row).cells().get(col);
            }
        }
        return "no such column";
    }

    /*
     +--------------------------------------------------------------------------+
     | checkRoundLog()                                                          |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The Round Log on a small set worked out by hand (Test Links, par 72,     |
     | rating 72.0 / 113, 14 par 4s and 5s): round 1 all pars, two putts and    |
     | every fairway (differential 0.0); round 2 a bogey on every hole, no      |
     | fairways, in a bunker on holes 1 and 2 (differential 18.0); round 3 the  |
     | front nine at par with 3 of 7 fairways and a sand save on hole 1 (no     |
     | index yet, so no differential); round 4 historical. Checks the rows,     |
     | the 18-hole and front-nine summaries, the note, historical on and off,   |
     | a period, all courses and the order of the kinds.                        |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    private static void checkRoundLog()
    {
        ReportData data = data(logRounds());
        RoundLogReport log = new RoundLogReport(data, LINKS.id(), null, null, false);
        Table table = log.table();
        check("round log: one set of tees, oldest first, historical left out", log.rounds().size() == 3
                && log.rounds().get(0).id() == 1 && log.subtitle(0).equals("Pat   -   Test Links (White): all 3 rounds")
                && table.columns().get(1).title().equals("Holes") && table.columns().get(0).width() == 120 && table.width() == 966);
        check("round log: a round's figures", cell(table, 0, "Fairways").equals("14 / 14") && cell(table, 0, "Greens").equals("18 / 18")
                && cell(table, 0, "2-putts").equals("18 / 18") && cell(table, 0, "Sand saves").equals("-")
                && cell(table, 0, "Differential").equals("0.0") && cell(table, 1, "Fairways").equals("0 / 14")
                && cell(table, 1, "Greens").equals("0 / 18") && cell(table, 1, "Sand saves").equals("0 / 2")
                && cell(table, 1, "Score").equals("90") && cell(table, 1, "Putts").equals("36") && cell(table, 1, "Differential").equals("18.0"));
        check("round log: a front nine shows Front nine, its figures and a dagger", cell(table, 2, "Holes").equals("Front nine")
                && cell(table, 2, "Fairways").equals("3 / 7") && cell(table, 2, "Greens").equals("9 / 9") && cell(table, 2, "Sand saves").equals("1 / 1")
                && cell(table, 2, "Differential").equals("-") && cell(table, 2, "").equals(Sheet.DAGGER));
        Table summary = log.summary();
        check("round log: the 18-hole summary worked out by hand", summary.rows().size() == 6
                && summary.rows().get(0).cells().get(0).equals("18 holes (2 rounds)") && summary.rows().get(0).kind() == Table.Kind.GROUP
                && cell(summary, 1, "Date").equals("Average") && cell(summary, 1, "Score").equals("81.0") && cell(summary, 1, "Putts").equals("36.0")
                && cell(summary, 1, "Fairways").equals("50.0 %") && cell(summary, 1, "Greens").equals("50.0 %")
                && cell(summary, 1, "2-putts").equals("100.0 %") && cell(summary, 1, "Sand saves").equals("0.0 %")
                && cell(summary, 1, "Differential").equals("9.0") && cell(summary, 2, "Date").equals("Best") && cell(summary, 2, "Score").equals("72")
                && cell(summary, 2, "Fairways").equals("100.0 %") && cell(summary, 2, "Greens").equals("100.0 %")
                && cell(summary, 2, "Sand saves").equals("0.0 %") && cell(summary, 2, "Differential").equals("0.0"));
        check("round log: the front-nine summary", summary.rows().get(3).cells().get(0).equals("Front nine (1 round)")
                && cell(summary, 4, "Score").equals("36.0") && cell(summary, 4, "Fairways").equals("42.9 %")
                && cell(summary, 4, "Sand saves").equals("100.0 %") && cell(summary, 4, "Differential").equals("-")
                && cell(summary, 5, "Differential").equals("-") && cell(summary, 5, "Putts").equals("18"));
        check("round log: the note has only the parts that apply", log.note().equals(RoundLogReport.DAGGER_PART + "   "
                + RoundLogReport.PERCENT_PART));

        RoundLogReport withOld = new RoundLogReport(data, LINKS.id(), null, null, true);
        check("round log: historical rounds on: listed with H and noted", withOld.rounds().size() == 4
                && cell(withOld.table(), 0, "").equals("H") && withOld.note().contains(RoundLogReport.HISTORICAL_PART)
                && withOld.summary().rows().get(0).cells().get(0).equals("18 holes (3 rounds)"));
        RoundLogReport february = new RoundLogReport(data, LINKS.id(), LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28), false);
        check("round log: a period keeps only its rounds and names its dates", february.rounds().size() == 1
                && february.rounds().get(0).id() == 2 && february.subtitle(0).equals("Pat   -   Test Links (White): 1 round from 2026-02-01 to 2026-02-28"));
        RoundLogReport nothing = new RoundLogReport(data, LINKS.id(), LocalDate.of(2027, 1, 1), LocalDate.of(2027, 1, 31), false);
        check("round log: a period with no rounds has nothing to print", nothing.isEmpty());

        List<Round> kinds = new ArrayList<>(logRounds());
        kinds.add(round(5, NINE, "2026-04-01", 0, Tee.NINE, 0, 2, true, new int[0], false));
        kinds.add(round(6, LINKS, "2026-04-02", Tee.NINE, Tee.NINE, 1, 2, true, new int[0], false));
        RoundLogReport all = new RoundLogReport(data(kinds), null, null, null, false);
        Table allTable = all.table();
        Table allSummary = all.summary();
        List<String> groups = new ArrayList<>();
        for (Table.Row row : allSummary.rows())
        {
            if (row.kind() == Table.Kind.GROUP)
            {
                groups.add(row.cells().get(0));
            }
        }
        check("round log: all courses has Course (tee) after Date and the narrower widths", all.subtitle(0).equals("Pat   -   All courses: all 5 rounds")
                && allTable.columns().get(1).title().equals("Course (tee)") && allTable.columns().get(1).width() == 190
                && allTable.columns().get(0).width() == 110 && allTable.columns().get(9).width() == 90
                && cell(allTable, 3, "Course (tee)").equals("Little Nine (Red)") && cell(allTable, 3, "Holes").equals("9")
                && cell(allTable, 4, "Holes").equals("Back nine"));
        check("round log: the kinds of round in order: 18 holes, Front nine, Back nine, Nine-hole course", groups.equals(List.of(
                "18 holes (2 rounds)", "Front nine (1 round)", "Back nine (1 round)", "Nine-hole course (1 round)")));
    }

    /*
     +--------------------------------------------------------------------------+
     | checkRoundLogPages()                                                     |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Page breaks on Letter (850 high on its side: 22 rounds a page) and A4    |
     | (778 high: fewer): one page; 50 rounds on three pages with the summary   |
     | under the last six; 44 rounds filling two pages so the summary goes on   |
     | a third page alone; "(continued)" from page 2; and the summary drawn     |
     | only on the last page.                                                   |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    private static void checkRoundLogPages()
    {
        double letter = Sheet.LETTER_SHORT;
        double a4 = 1100.0 * 210.0 / 297.0;
        RoundLogReport small = new RoundLogReport(data(steady(10)), LINKS.id(), null, null, false);
        check("round log pages: 10 rounds and the summary fit on one page", small.pageCount(letter) == 1 && !small.summaryAlone(letter)
                && small.rowsPerPage(letter) == 22 && small.rowsPerPage(a4) < 22 && small.subtitle(0).endsWith("all 10 rounds"));
        RoundLogReport fifty = new RoundLogReport(data(steady(50)), LINKS.id(), null, null, false);
        check("round log pages: 50 rounds take three pages with the summary under the last rounds", fifty.pageCount(letter) == 3
                && !fifty.summaryAlone(letter) && fifty.subtitle(1).endsWith("all 50 rounds (continued)")
                && !fifty.subtitle(0).endsWith("(continued)"));
        RoundLogReport full = new RoundLogReport(data(steady(44)), LINKS.id(), null, null, false);
        check("round log pages: when the summary does not fit it gets a page of its own", full.pageCount(letter) == 3
                && full.summaryAlone(letter));
        BufferedImage first = PrintSelfTest.picture(fifty, 0, letter);
        BufferedImage last = PrintSelfTest.picture(fifty, 2, letter);
        int groupTop = (int) (RoundLogReport.TABLE_TOP + Table.HEADER_HEIGHT + 6 * RoundLogReport.ROW_HEIGHT + Table.END_RULE
                + RoundLogReport.SUMMARY_GAP + RoundLogReport.SUMMARY_RULE + 4);
        check("round log pages: the summary is drawn only on the last page", PrintSelfTest.count(last, 70, groupTop, 1000, groupTop + 10,
                Sheet.BOX) > 1000 && PrintSelfTest.count(first, 70, 170, 1000, 780, Sheet.BOX) == 0);
        BufferedImage alone = PrintSelfTest.picture(full, 2, letter);
        int aloneGroup = (int) (RoundLogReport.TABLE_TOP + Table.HEADER_HEIGHT + 4);
        check("round log pages: a summary alone sits under the header row", PrintSelfTest.count(alone, 70, aloneGroup, 1000, aloneGroup + 10,
                Sheet.BOX) > 1000 && PrintSelfTest.count(alone, 70, 175, 1000, 195, Sheet.BROWN) > 1000);
        RoundLogReport onA4 = new RoundLogReport(data(steady(50)), LINKS.id(), null, null, false);
        BufferedImage a4Page = PrintSelfTest.picture(onA4, 0, a4);
        int footRule = (int) Math.floor(a4 - Report.FOOT_RULE_UP) + 1;
        check("round log pages: A4 fits fewer rows and still keeps the rows above the foot rule", onA4.pageCount(a4) >= onA4.pageCount(letter)
                && PrintSelfTest.near(a4Page, 60, footRule - 1, 1040, footRule + 1, Sheet.FOOT_RULE) > 500
                && PrintSelfTest.count(a4Page, 60, footRule - 18, 1040, footRule - 1, Sheet.ZEBRA) == 0);
    }

    // ------------------------------------------------------------ scorecard

    /*
     +--------------------------------------------------------------------------+
     | checkScorecard()                                                         |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The scorecard for 18 holes (Out after 9, In and Total), a front nine     |
     | and a back nine (their nine and Total), and a nine-hole course; the      |
     | rows' words and totals; the mark for every kind of score; a historical   |
     | round's subtitle; the figures box; a long course name cut short in the   |
     | title.                                                                   |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    private static void checkScorecard()
    {
        List<Round> rounds = logRounds();
        rounds.add(round(5, NINE, "2026-04-01", 0, Tee.NINE, 0, 2, true, new int[0], false));
        rounds.add(round(6, LINKS, "2026-04-02", Tee.NINE, Tee.NINE, 1, 2, true, new int[0], false));
        ReportData data = data(rounds);
        ScorecardReport full = ScorecardReport.forRound(data, rounds.get(1));
        List<String> headings = full.headings();
        List<List<String>> rows = full.rowTexts();
        check("scorecard: 18 holes have Out after 9, then In and Total", headings.size() == 21 && headings.get(8).equals("9")
                && headings.get(9).equals("Out") && headings.get(10).equals("10") && headings.get(19).equals("In")
                && headings.get(20).equals("Total") && full.cardWidth() == 96 + 21 * 42);
        check("scorecard: pars, scores and putts have totals; Hdcp, Fairway and Sand do not", rows.get(0).get(9).equals("36")
                && rows.get(0).get(20).equals("72") && rows.get(2).get(9).equals("45") && rows.get(2).get(20).equals("90")
                && rows.get(3).get(19).equals("18") && rows.get(1).get(0).equals("7") && rows.get(1).get(9).isEmpty()
                && rows.get(4).get(9).isEmpty() && rows.get(5).get(20).isEmpty());
        check("scorecard: fairway a dash on a par 3, empty when missed; a check in Sand for a bunker", rows.get(4).get(2).equals("-")
                && rows.get(4).get(0).isEmpty() && rows.get(5).get(0).equals(Sheet.CHECK) && rows.get(5).get(2).isEmpty());
        ScorecardReport hit = ScorecardReport.forRound(data, rounds.get(0));
        check("scorecard: a check for a fairway hit", hit.rowTexts().get(4).get(0).equals(Sheet.CHECK));
        check("scorecard: the subtitle, title and figures", full.subtitle(0).equals("Pat   -   2026-02-10   -   round 00002")
                && full.heading().equals("Scorecard   -   Test Links (White)") && full.figures().get(0)[1].equals("90   (+18)")
                && full.figures().get(2)[1].equals("0 of 14") && full.figures().get(5)[1].equals("0 of 2")
                && full.figures().get(6)[1].equals("72.0 / 113") && full.figures().get(7)[1].equals("18.0")
                && full.figures().get(8)[1].equals("-") && hit.figures().get(0)[1].equals("72   (E)") && hit.figures().get(5)[1].equals("-"));

        ScorecardReport front = ScorecardReport.forRound(data, rounds.get(2));
        ScorecardReport back = ScorecardReport.forRound(data, rounds.get(5));
        ScorecardReport nine = ScorecardReport.forRound(data, rounds.get(4));
        check("scorecard: a front nine is holes 1-9 and Total", front.headings().size() == 10 && front.headings().get(0).equals("1")
                && front.headings().get(9).equals("Total") && front.rowTexts().get(0).get(9).equals("36") && front.cardWidth() == 96 + 10 * 42
                && front.figures().get(6)[1].equals("36.0 / 113"));
        check("scorecard: a back nine is holes 10-18 and Total", back.headings().get(0).equals("10") && back.headings().get(8).equals("18")
                && back.headings().get(9).equals("Total") && back.rowTexts().get(1).get(0).equals("8")
                && back.rowTexts().get(2).get(9).equals("45"));
        check("scorecard: a nine-hole course is holes 1-9 and Total", nine.headings().size() == 10 && nine.rowTexts().get(0).get(9).equals("36")
                && nine.heading().equals("Scorecard   -   Little Nine (Red)"));
        check("scorecard: the marks: circle for birdie or better, square for bogey, double square for worse",
                ScorecardReport.markOf(2, 4) == ScorecardReport.Mark.CIRCLE && ScorecardReport.markOf(3, 4) == ScorecardReport.Mark.CIRCLE
                && ScorecardReport.markOf(1, 3) == ScorecardReport.Mark.CIRCLE && ScorecardReport.markOf(4, 4) == ScorecardReport.Mark.NONE
                && ScorecardReport.markOf(5, 4) == ScorecardReport.Mark.SQUARE && ScorecardReport.markOf(6, 4) == ScorecardReport.Mark.DOUBLE_SQUARE
                && ScorecardReport.markOf(11, 4) == ScorecardReport.Mark.DOUBLE_SQUARE);
        ScorecardReport old = ScorecardReport.forRound(data, rounds.get(3));
        check("scorecard: a historical round says so and has no differential", old.subtitle(0).equals("Pat   -   2025-12-01   -   round 00004"
                + "   -   historical") && old.figures().get(7)[1].equals("-"));

        BufferedImage card = PrintSelfTest.picture(full, 0, Sheet.LETTER_SHORT);
        int scoreTop = (int) (ScorecardReport.CARD_TOP + ScorecardReport.HEADER_HEIGHT + 2 * ScorecardReport.ROW_HEIGHT);
        int holeLeft = (int) (Sheet.MARGIN + ScorecardReport.LABEL_WIDTH);
        check("scorecard: a bogey has its square and Out its shaded cell", PrintSelfTest.near(card, holeLeft + 6, scoreTop + 4, holeLeft + 36,
                scoreTop + 34, Sheet.BROWN) > 60 && PrintSelfTest.count(card, holeLeft + 9 * 42 + 3, scoreTop + 2, holeLeft + 9 * 42 + 8,
                scoreTop + 8, Sheet.BOX) > 10);

        Tee longTee = new Tee(1, "The Remarkably Long Named Golf and Country Club of the Southern Carolina Lowlands", "Fort Mill", "SC",
                "29708", "Championship Gold", Tee.HOLES, 72.0, 113, 6200, PARS, INDEXES, 36.0, 113, 36.0, 113);
        Map<Integer, Tee> longTees = new HashMap<>();
        longTees.put(1, longTee);
        ReportData longData = new ReportData(List.of(rounds.get(1)), longTees, Handicap.compute(List.of(rounds.get(1)), longTees), "Pat",
                true, false, PrintSelfTest.TODAY, "4.00");
        BufferedImage longCard = PrintSelfTest.picture(ScorecardReport.forRound(longData, rounds.get(1)), 0, Sheet.LETTER_SHORT);
        BufferedImage longLog = PrintSelfTest.picture(new RoundLogReport(longData, null, null, null, true), 0, Sheet.LETTER_SHORT);
        check("scorecard and round log: a long course name is cut short inside the margins", PrintSelfTest.inked(longCard, 1041, 60, 1100, 110) == 0
                && PrintSelfTest.inked(longCard, 900, 66, 1040, 110) > 50 && Sheet.fit(longTee.label(), Sheet.sans(java.awt.Font.PLAIN, 13), 174)
                .endsWith(Sheet.ELLIPSIS) && PrintSelfTest.near(longLog, 320, 202, 360, 222, Sheet.INK) > 5);
    }

    // ------------------------------------------------------------ part 3

    /*
     * withIndex(data, index): the same data with today's Handicap Index set by hand (null for none), for the card's strokes.
     */
    static ReportData withIndex(ReportData data, Double index)
    {
        Handicap.Result result = new Handicap.Result(data.handicap().entries(), data.handicap().currentDifferentials(), index, null);
        return new ReportData(data.rounds(), data.tees(), result, data.golfer(), data.includeHistorical(), data.sample(), data.today(),
                data.version());
    }

    /*
     +--------------------------------------------------------------------------+
     | checkBestRound()                                                         |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Best Round on Test Links (round 1 all pars, round 2 all bogeys): the     |
     | single best is round 1 with its date and number; the composite takes     |
     | the best score on every hole, says how many rounds it was built from     |
     | and has no differential or index, as on the Best Round screen.           |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    private static void checkBestRound()
    {
        List<Round> rounds = new ArrayList<>();
        rounds.add(round(1, LINKS, "2026-01-10", 0, Tee.HOLES, 0, 2, true, new int[0], false));
        Round bogeys = round(2, LINKS, "2026-02-10", 0, Tee.HOLES, 1, 2, false, new int[0], false);
        int[] mixed = bogeys.scores().clone();
        mixed[0] = PARS[0] - 1;
        rounds.add(new Round(2, LINKS.id(), bogeys.date(), 0, 0, Tee.HOLES, mixed, bogeys.putts(), bogeys.fairway(), bogeys.sand()));
        ReportData data = data(rounds);
        golfanalyst.BestRound.Choice choice = golfanalyst.BestRound.latestChoice(data.rounds(), data.tees(), true);
        ScorecardReport single = ScorecardReport.forBest(data, choice, false);
        check("best round: the single best round with its date and number", single.round().id() == 1
                && single.heading().equals("Best Round   -   Test Links (White)") && single.subtitle(0).equals("Pat   -   2026-01-10   -   round 00001")
                && single.name().equals("Best Round") && single.figures().get(0)[1].equals("72   (E)"));
        ScorecardReport composite = ScorecardReport.forBest(data, choice, true);
        List<String> scores = composite.rowTexts().get(2);
        check("best round: the composite is the best score on every hole and has no differential or index", composite.composite()
                && composite.heading().equals("Best Round   -   Test Links (White) (composite)")
                && composite.subtitle(0).equals("Pat   -   built from your 2 rounds") && scores.get(0).equals("3") && scores.get(1).equals("4")
                && scores.get(20).equals("71") && composite.figures().get(7)[1].equals("-") && composite.figures().get(8)[1].equals("-"));
        check("best round: no course chosen has nothing to print", ScorecardReport.forBest(data, null, false).isEmpty());
    }

    /*
     +--------------------------------------------------------------------------+
     | checkStatsBoard()                                                        |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The Stats Board report: the two total rows first, the 18-hole rows, the  |
     | "Nine-hole rounds" band and the nine-hole rows; the screen's columns and |
     | words; the best figures shaded where StatsBoard.best says; the           |
     | historical words; and 30 sets of tees breaking onto a second page with   |
     | the header row again.                                                    |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    private static void checkStatsBoard()
    {
        List<Round> rounds = logRounds();
        rounds.add(round(5, NINE, "2026-04-01", 0, Tee.NINE, 0, 2, true, new int[0], false));
        rounds.add(round(6, NINE, "2026-04-08", 0, Tee.NINE, 1, 2, true, new int[0], false));
        ReportData data = data(rounds);
        StatsBoardReport board = new StatsBoardReport(data, false);
        Table table = board.table();
        List<Table.Row> rows = table.rows();
        check("stats board: totals first, then the 18-hole rows, the nine-hole band and the nine-hole rows", rows.size() == 6
                && rows.get(0).kind() == Table.Kind.TOTAL && rows.get(0).cells().get(0).equals("All 18-hole rounds")
                && rows.get(1).cells().get(0).equals("All nine-hole rounds") && rows.get(2).cells().get(0).equals("Test Links (White)")
                && rows.get(3).kind() == Table.Kind.GROUP && rows.get(3).cells().get(0).equals(StatsBoardReport.NINE_BAND)
                && rows.get(4).cells().get(0).equals("Little Nine (Red)") && rows.get(5).cells().get(0).equals("Test Links (White), front nine"));
        List<Round> backRounds = logRounds();
        backRounds.add(round(7, LINKS, "2026-04-15", 9, Tee.NINE, 0, 2, true, new int[0], false));
        StatsBoardReport back = new StatsBoardReport(data(backRounds), false);
        boolean backNamed = false;
        for (Table.Row backRow : back.table().rows())
        {
            backNamed = backNamed || backRow.cells().get(0).equals("Test Links (White), back nine");
        }
        check("stats board: the printed names, the headings Course (tee) and Rating / slope, the screen's own names unchanged",
                backNamed && table.columns().get(0).title().equals("Course (tee)") && table.columns().get(1).title().equals("Rating / slope")
                && golfanalyst.StatsBoard.Column.COURSE.heading().equals("Course : Tee Box")
                && golfanalyst.StatsBoard.Column.RATING.heading().equals("Rating\\Slope")
                && board.board().nine().get(1).label().equals("Test Links:White (Front 9)")
                && board.board().eighteen().get(0).label().equals("Test Links:White"));
        check("stats board: the screen's columns and figures, widened to fill the page", table.columns().size() == 10
                && Math.abs(table.columns().get(0).width() - 266 * 980.0 / 858) < 1e-9
                && Math.abs(table.columns().get(1).width() - 96 * 980.0 / 858) < 1e-9 && table.columns().get(9).title().equals("Sand %")
                && Math.abs(table.width() - 980) < 1e-9
                && rows.get(2).cells().get(2).equals("2") && rows.get(2).cells().get(3).equals("81")
                && rows.get(2).cells().get(1).equals("72.0 / 113") && board.subtitle(0).equals("Pat   -   historical rounds not included")
                && new StatsBoardReport(data, true).subtitle(0).endsWith("historical rounds included"));
        java.util.Map<golfanalyst.StatsBoard.Column, java.util.Set<String>> best = golfanalyst.StatsBoard.best(board.board().nine());
        boolean shadedRight = true;
        for (int col = 0; col < golfanalyst.StatsBoard.Column.values().length; col = col + 1)
        {
            java.util.Set<String> keys = best.get(golfanalyst.StatsBoard.Column.values()[col]);
            boolean wanted = keys != null && keys.contains(board.board().nine().get(0).key());
            shadedRight = shadedRight && rows.get(4).shadedCells().contains(col) == wanted;
        }
        check("stats board: the best figures are shaded as the screen shows gold", shadedRight && rows.get(0).shadedCells().isEmpty());

        java.util.Map<Integer, Tee> many = new HashMap<>();
        List<Round> manyRounds = new ArrayList<>();
        for (int lp = 0; lp < 30; lp = lp + 1)
        {
            Tee tee = new Tee(10 + lp, "Course " + (10 + lp), "Fort Mill", "SC", "29708", "White", Tee.HOLES, 72.0, 113, 6000, PARS, INDEXES,
                    36.0, 113, 36.0, 113);
            many.put(tee.id(), tee);
            manyRounds.add(round(100 + lp, tee, LocalDate.of(2026, 1, 1).plusDays(lp).toString(), 0, Tee.HOLES, 0, 2, true, new int[0],
                    false));
        }
        ReportData manyData = new ReportData(manyRounds, many, Handicap.compute(manyRounds, many), "Pat", true, false, PrintSelfTest.TODAY,
                "4.00");
        StatsBoardReport long1 = new StatsBoardReport(manyData, true);
        BufferedImage second = PrintSelfTest.picture(long1, 1, Sheet.LETTER_SHORT);
        check("stats board: many rows break onto a second page with the header row again", long1.pageCount(Sheet.LETTER_SHORT) == 2
                && long1.rowsPerPage(Sheet.LETTER_SHORT) == 23 && PrintSelfTest.count(second, 70, 175, 900, 195, Sheet.BROWN) > 1000
                && PrintSelfTest.near(second, 70, 205, 300, 225, Sheet.INK) > 20);
    }

    /*
     +--------------------------------------------------------------------------+
     | checkMyScorecard()                                                       |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | My scorecard on Test Links (rating 72.0, slope 113, par 72, so the       |
     | course handicap is the index rounded): 15.3 gives 15, a stroke on        |
     | handicap numbers 1-15 and pick-up par + 3 there; 20.0 gives 20, two      |
     | strokes on numbers 1 and 2; 0.0 gives none and pick-up par + 2; a plus   |
     | 2.0 gives a stroke back ("-1") on numbers 17 and 18 and shows "+2"; a    |
     | nine-hole course uses half the index (7.7) with the nine's rating 35.0   |
     | and par 36, so 7; with no index the Strokes row is empty and pick-up is  |
     | par + 5.                                                                 |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    private static void checkMyScorecard()
    {
        ReportData data = data(logRounds());
        MyScorecardReport fifteen = new MyScorecardReport(withIndex(data, 15.3), LINKS);
        int[] strokes = fifteen.strokes();
        int[] pickUps = fifteen.pickUps();
        check("my scorecard: index 15.3 gives 15 strokes, one on handicap numbers 1 to 15", fifteen.courseHandicap() == 15
                && strokes[2] == 1 && strokes[1] == 1 && strokes[5] == 0 && strokes[14] == 0 && pickUps[2] == 3 + 3 && pickUps[5] == 4 + 2
                && fifteen.subtitle(0).equals("Pat   -   Handicap Index 15.3") && fifteen.heading().equals("My Scorecard   -   Test Links (White)"));
        List<List<String>> rows = fifteen.rowTexts();
        check("my scorecard: strokes blank where a hole gives none, totals for strokes but not pick-up, four empty rows", fifteen.headings().size() == 21
                && rows.get(2).get(20).equals("15") && rows.get(0).get(20).equals("72") && rows.get(3).get(20).isEmpty() && rows.get(3).get(9).isEmpty()
                && rows.get(3).get(19).isEmpty() && rows.get(3).get(0).equals("7") && rows.get(2).get(0).equals("1") && rows.get(2).get(5).isEmpty()
                && rows.get(2).get(9).equals("8")
                && rows.get(1).get(9).isEmpty() && rows.get(4).get(0).isEmpty() && rows.get(7).get(20).isEmpty()
                && fifteen.figures().get(0)[1].equals("15") && fifteen.figures().get(1)[1].equals("72.0 / 113")
                && fifteen.figures().get(2)[1].equals("6200") && fifteen.figures().get(3)[1].equals("72"));
        MyScorecardReport twenty = new MyScorecardReport(withIndex(data, 20.0), LINKS);
        check("my scorecard: index 20.0 gives two strokes on the two hardest holes", twenty.courseHandicap() == 20 && twenty.strokes()[2] == 2
                && twenty.rowTexts().get(2).get(2).equals("2") && twenty.rowTexts().get(2).get(0).equals("1")
                && twenty.strokes()[11] == 2 && twenty.strokes()[0] == 1 && twenty.pickUps()[2] == 3 + 4);
        MyScorecardReport zero = new MyScorecardReport(withIndex(data, 0.0), LINKS);
        boolean allZero = true;
        for (int hole = 0; hole < Tee.HOLES; hole = hole + 1)
        {
            allZero = allZero && zero.strokes()[hole] == 0 && zero.pickUps()[hole] == PARS[hole] + 2;
        }
        check("my scorecard: a zero handicap gives no strokes and pick-up par + 2", zero.courseHandicap() == 0 && allZero);
        MyScorecardReport plus = new MyScorecardReport(withIndex(data, -2.0), LINKS);
        check("my scorecard: a plus handicap gives a stroke back on the two easiest holes", plus.courseHandicap() == -2
                && plus.strokes()[14] == -1 && plus.strokes()[5] == -1 && plus.strokes()[0] == 0 && plus.rowTexts().get(2).get(15).equals("-1")
                && plus.pickUps()[14] == PARS[14] + 1 && plus.figures().get(0)[1].equals("+2") && plus.subtitle(0).endsWith("Handicap Index +2.0"));
        MyScorecardReport nine = new MyScorecardReport(withIndex(data, 15.3), NINE);
        int ninth = 0;
        for (int stroke : nine.strokes())
        {
            ninth = ninth + stroke;
        }
        check("my scorecard: a nine-hole course uses half the index with the nine's rating", nine.courseHandicap() == 7 && ninth == 7
                && nine.strokes()[0] == 1 && nine.strokes()[1] == 0 && nine.headings().size() == 10
                && nine.headings().get(9).equals("Total") && nine.rowTexts().get(0).get(9).equals("36"));
        MyScorecardReport none = new MyScorecardReport(withIndex(data, null), LINKS);
        check("my scorecard: with no index the Strokes row is empty and pick-up is par + 5", none.strokes() == null
                && none.rowTexts().get(2).get(0).isEmpty() && none.rowTexts().get(2).get(20).isEmpty() && none.pickUps()[0] == PARS[0] + 5
                && none.figures().get(0)[1].equals("-") && none.subtitle(0).equals("Pat   -   no Handicap Index yet"));
        BufferedImage page = PrintSelfTest.picture(none, 0, Sheet.LETTER_SHORT);
        check("my scorecard: the card, the notes and the box fit above the foot rule", PrintSelfTest.count(page, 60, 700, 1040, 780, Sheet.BOX) == 0
                && PrintSelfTest.count(page, 100, 580, 1000, 650, Sheet.BOX) > 1000);
    }

    /*
     +--------------------------------------------------------------------------+
     | checkCourseNotes()                                                       |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Course notes on two rounds made to be worst on holes 11, 5 and 6 (in     |
     | that ranking) and best on 13, 8 and 9: the lists in hole-number order,   |
     | the hole numbers tinted red and gold on the page, the rows and totals,   |
     | and no tint with one round.                                              |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    private static void checkCourseNotes()
    {
        int[] extra = {0, 0, 0, 0, 3, 2, 0, -1, -1, 0, 4, 0, -2, 0, 0, 0, 0, 0};
        List<Round> rounds = new ArrayList<>();
        for (int lp = 0; lp < 2; lp = lp + 1)
        {
            Round even = round(lp + 1, LINKS, LocalDate.of(2026, 3, 1).plusDays(lp).toString(), 0, Tee.HOLES, 0, 2, true, new int[0], false);
            int[] scores = even.scores().clone();
            for (int hole = 0; hole < Tee.HOLES; hole = hole + 1)
            {
                scores[hole] = scores[hole] + extra[hole];
            }
            rounds.add(new Round(even.id(), LINKS.id(), even.date(), 0, 0, Tee.HOLES, scores, even.putts(), even.fairway(), even.sand()));
        }
        ReportData data = data(rounds);
        golfanalyst.BestRound.Choice row = golfanalyst.CourseAnalysis.latestRow(data.rounds(), data.tees(), true);
        CourseNotesReport notes = new CourseNotesReport(data, row);
        check("course notes: the hardest and easiest holes in hole-number order", notes.analysis().hardestHoles(3).equals(List.of(11, 5, 6))
                && notes.hardest().equals(List.of(5, 6, 11)) && notes.easiest().equals(List.of(8, 9, 13))
                && notes.figures().get(7)[1].equals("5, 6, 11") && notes.figures().get(8)[1].equals("8, 9, 13")
                && notes.heading().equals("Course Notes   -   Test Links (White)") && notes.subtitle(0).equals("Pat   -   2 rounds"));
        List<List<String>> rows = notes.rowTexts();
        check("course notes: the rows, Out, In and All", notes.headings().size() == 21 && notes.headings().get(18).equals("Out")
                && notes.headings().get(20).equals("All") && rows.get(0).get(20).equals("72") && rows.get(3).get(10).equals("+4.0")
                && rows.get(3).get(12).equals("-2.0") && rows.get(2).get(4).equals("7.0") && rows.get(1).get(0).equals("7")
                && rows.get(1).get(19).isEmpty() && rows.get(7).get(2).equals("-") && rows.get(3).get(20).equals("+5"));
        BufferedImage page = PrintSelfTest.picture(notes, 0, Sheet.LETTER_SHORT);
        int left = (int) (Sheet.MARGIN + Card.LABEL_WIDTH);
        int top = (int) CourseNotesReport.CARD_TOP + 3;
        check("course notes: the hole numbers are tinted red and gold on the page", PrintSelfTest.count(page, left + 4 * 42 + 3, top, left + 4 * 42 + 8,
                top + 5, Sheet.HARD) > 10 && PrintSelfTest.count(page, left + 12 * 42 + 3, top, left + 12 * 42 + 8, top + 5, Sheet.EASY) > 10
                && PrintSelfTest.count(page, left + 3, top, left + 8, top + 5, Sheet.BROWN) > 10);
        int vsPar = (int) (CourseNotesReport.CARD_TOP + Card.HEADER_HEIGHT + CourseNotesReport.AVG_VS_PAR_ROW * CourseNotesReport.ROW_HEIGHT) + 3;
        int score = (int) (CourseNotesReport.CARD_TOP + Card.HEADER_HEIGHT + 2 * CourseNotesReport.ROW_HEIGHT) + 3;
        check("course notes: the Avg vs par cells of the hardest and easiest holes take the same tints", PrintSelfTest.count(page,
                left + 4 * 42 + 3, vsPar, left + 4 * 42 + 8, vsPar + 5, Sheet.HARD) > 10 && PrintSelfTest.count(page, left + 12 * 42 + 3, vsPar,
                left + 12 * 42 + 8, vsPar + 5, Sheet.EASY) > 10 && PrintSelfTest.count(page, left + 4 * 42 + 3, score, left + 4 * 42 + 8,
                score + 5, Sheet.HARD) == 0 && PrintSelfTest.count(page, left + 3, vsPar, left + 8, vsPar + 5, Sheet.HARD) == 0);
        CourseNotesReport one = new CourseNotesReport(data(List.of(rounds.get(0))), row);
        check("course notes: one round is not tinted", one.hardest().isEmpty() && one.figures().get(7)[1].equals("-")
                && one.figures().get(8)[1].equals("-"));
    }

    // ------------------------------------------------------------ part 4

    /*
     +--------------------------------------------------------------------------+
     | checkSpreadsheets()                                                      |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The spreadsheet files: a field with a comma, a quote or a line break is  |
     | quoted with its quotes doubled; the file has a byte order mark and       |
     | Windows line breaks; the offered names; and each report's columns and    |
     | rows, on a course named Bud's "Best", Links so the quoting is used:      |
     | the Round Log (per-hole scores and putts, empty for holes not played),   |
     | the Handicap Report (Counts as the shading), the Stats Board (the        |
     | screen's columns) and Course notes (one row per hole). The scorecards    |
     | have none.                                                               |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing (throws IOException if the temporary file fails)     |
     +--------------------------------------------------------------------------+
     */
    private static void checkSpreadsheets() throws java.io.IOException
    {
        check("spreadsheet: a field with a comma or a quote is quoted, quotes doubled", Csv.field("Bud's \"Best\", Links")
                .equals("\"Bud's \"\"Best\"\", Links\"") && Csv.field("Plain").equals("Plain") && Csv.field("two\nlines").equals("\"two\nlines\"")
                && Csv.number(13.94, 1).equals("13.9") && Csv.number(Double.NaN, 1).isEmpty());

        Tee odd = new Tee(1, "Bud's \"Best\", Links", "Fort Mill", "SC", "29708", "White", Tee.HOLES, 72.0, 113, 6200, PARS, INDEXES, 36.0,
                113, 36.0, 113);
        Map<Integer, Tee> tees = new HashMap<>();
        tees.put(1, odd);
        List<Round> rounds = new ArrayList<>(logRounds());
        rounds.sort((one, two) -> one.date().compareTo(two.date()));
        ReportData data = new ReportData(rounds, tees, Handicap.compute(rounds, tees), "Pat", true, false, PrintSelfTest.TODAY, "4.00");
        RoundLogReport log = new RoundLogReport(data, null, null, null, true);
        List<List<String>> logRows = log.spreadsheet();
        List<String> header = logRows.get(0);
        check("spreadsheet, round log: the columns", header.size() == 15 + 36 && header.get(0).equals("Date") && header.get(1).equals("Course")
                && header.get(2).equals("Tee") && header.get(13).equals("Differential") && header.get(14).equals("Historical")
                && header.get(15).equals("Score 1") && header.get(32).equals("Score 18") && header.get(33).equals("Putts 1")
                && header.get(50).equals("Putts 18") && logRows.size() == 5);
        List<String> old = logRows.get(1);
        List<String> second = logRows.get(3);
        List<String> front = logRows.get(4);
        check("spreadsheet, round log: a round's figures, oldest first, no summary rows", old.get(0).equals("2025-12-01")
                && old.get(14).equals("Yes") && second.get(0).equals("2026-02-10") && second.get(1).equals(odd.courseName())
                && second.get(3).equals("18") && second.get(4).equals("90") && second.get(5).equals("36") && second.get(6).equals("0")
                && second.get(7).equals("14") && second.get(8).equals("0") && second.get(9).equals("18") && second.get(10).equals("18")
                && second.get(11).equals("2") && second.get(12).equals("0") && second.get(13).equals("18.0") && second.get(14).equals("No")
                && second.get(15).equals("5") && second.get(50).equals("2"));
        check("spreadsheet, round log: a front nine leaves holes 10-18 empty and has no differential", front.get(3).equals("Front nine")
                && front.get(13).isEmpty() && front.get(15).equals("4") && front.get(24).isEmpty() && front.get(41).equals("2")
                && front.get(42).isEmpty());
        String text = Csv.text(logRows);
        check("spreadsheet, round log: the course name is quoted in the file", text.contains(",\"Bud's \"\"Best\"\", Links\",White,")
                && text.contains("\r\n") && !text.replace("\r\n", "").contains("\n") && log.spreadsheetWords().equals("4 rounds"));

        java.nio.file.Path folder = golfanalyst.TestFolders.make("golfanalyst-csv");
        java.nio.file.Path file = folder.resolve("log.csv");
        Csv.write(file, logRows);
        byte[] bytes = java.nio.file.Files.readAllBytes(file);
        String back = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
        boolean onlyIt = true;
        try (java.util.stream.Stream<java.nio.file.Path> list = java.nio.file.Files.list(folder))
        {
            onlyIt = list.count() == 1;
        }
        check("spreadsheet: the file is UTF-8 with a byte order mark and nothing left behind", (bytes[0] & 0xFF) == 0xEF
                && (bytes[1] & 0xFF) == 0xBB && (bytes[2] & 0xFF) == 0xBF && back.substring(1).equals(text) && onlyIt
                && Csv.fileName(log, PrintSelfTest.TODAY).equals("RoundLog_2026-11-01.csv"));

        HandicapReport handicap = new HandicapReport(data(steady(25)));
        List<List<String>> handicapRows = handicap.spreadsheet();
        int yes = 0;
        for (int lp = 1; lp < handicapRows.size(); lp = lp + 1)
        {
            if (handicapRows.get(lp).get(7).equals("Yes"))
            {
                yes = yes + 1;
            }
        }
        check("spreadsheet, handicap report: the columns, newest first, Counts as the shading", handicapRows.get(0).equals(List.of("Date",
                "Course", "Tee", "Holes", "Score", "Adjusted", "Differential", "Counts")) && handicapRows.size() == 21 && yes == 8
                && handicapRows.get(1).get(0).equals(handicap.entries().get(0).round().date().toString())
                && handicapRows.get(1).get(1).equals("Test Links") && handicapRows.get(1).get(2).equals("White")
                && Csv.fileName(handicap, PrintSelfTest.TODAY).equals("HandicapReport_2026-11-01.csv")
                && handicap.spreadsheetWords().equals("20 rounds"));

        List<Round> withNine = new ArrayList<>(logRounds());
        withNine.add(round(5, NINE, "2026-04-01", 0, Tee.NINE, 0, 2, true, new int[0], false));
        StatsBoardReport board = new StatsBoardReport(data(withNine), false);
        List<List<String>> boardRows = board.spreadsheet();
        check("spreadsheet, stats board: the printed names, the totals and every row", boardRows.get(0).size() == 10
                && boardRows.get(0).get(0).equals("Course (tee)") && boardRows.get(0).get(1).equals("Rating / slope") && boardRows.get(0).get(9).equals("Sand %") && boardRows.size() == 1 + 2 + 1 + 2
                && boardRows.get(1).get(0).equals("All 18-hole rounds") && boardRows.get(3).get(0).equals("Test Links (White)")
                && boardRows.get(4).get(0).equals("Little Nine (Red)") && boardRows.get(4).get(9).isEmpty() && boardRows.get(3).get(9).equals("0")
                && board.spreadsheetWords().equals("5 rows"));

        golfanalyst.BestRound.Choice row = new golfanalyst.BestRound.Choice(1, golfanalyst.StatsBoard.Part.EIGHTEEN, "Odd");
        CourseNotesReport notes = new CourseNotesReport(data, row);
        List<List<String>> noteRows = notes.spreadsheet();
        check("spreadsheet, course notes: one row per hole with the counts and averages", noteRows.size() == 19 && noteRows.get(0).size() == 18
                && noteRows.get(0).get(4).equals("Dbl Eagle") && noteRows.get(0).get(10).equals("Other") && noteRows.get(0).get(17).equals("Greens %")
                && noteRows.get(1).get(0).equals("1") && noteRows.get(1).get(2).equals("7") && noteRows.get(1).get(3).equals("3")
                && noteRows.get(3).get(16).isEmpty() && noteRows.get(3).get(15).equals("2.0") && notes.spreadsheetWords().equals("18 holes"));
        check("spreadsheet: the scorecards have none", ScorecardReport.forRound(data, rounds.get(0)).spreadsheet() == null
                && new MyScorecardReport(data, odd).spreadsheet() == null);
    }

    // ------------------------------------------------------------ second look

    /*
     +--------------------------------------------------------------------------+
     | checkNames()                                                             |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Every report's subtitle starts with the golfer's name and "   -   " when |
     | a name is set (the sample data with the test name Angelo), and leaves    |
     | both out when the name is blank.                                         |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing (throws IOException if the sample cannot be read)    |
     +--------------------------------------------------------------------------+
     */
    private static void checkNames() throws java.io.IOException
    {
        ReportData read = PrintSelfTest.sample();
        String[] names = {"Angelo", ""};
        for (String name : names)
        {
            ReportData data = new ReportData(read.rounds(), read.tees(), read.handicap(), name, true, true, read.today(), read.version());
            Round newest = data.rounds().get(data.rounds().size() - 1);
            golfanalyst.BestRound.Choice latest = golfanalyst.BestRound.latestChoice(data.rounds(), data.tees(), true);
            List<Report> reports = List.of(new HandicapReport(data), new RoundLogReport(data, null, null, null, true),
                    ScorecardReport.forRound(data, newest), ScorecardReport.forBest(data, latest, false),
                    ScorecardReport.forBest(data, latest, true), new StatsBoardReport(data, true),
                    new MyScorecardReport(data, data.tees().get(newest.teeId())), new CourseNotesReport(data, latest));
            boolean right = true;
            for (Report report : reports)
            {
                String line = report.subtitle(0);
                if (name.isEmpty())
                {
                    right = right && !line.startsWith(" ") && !line.startsWith("-") && !line.startsWith(Report.SEPARATOR.trim());
                }
                else
                {
                    right = right && line.startsWith("Angelo" + Report.SEPARATOR) && !line.equals("Angelo" + Report.SEPARATOR);
                }
            }
            String what = "a blank name leaves out the name and its dash";
            if (!name.isEmpty())
            {
                what = "every report's subtitle starts with the name and its dash";
            }
            check("names: " + what, right && reports.size() == 8);
        }
    }

    /*
     +--------------------------------------------------------------------------+
     | checkPageNotes()                                                         |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The Round Log's note on every page with only that page's parts: 50       |
     | rounds (22 a page on Letter) with a historical round on page 1 and a     |
     | front nine on page 2: page 1 says only H, page 2 only the dagger, page   |
     | 3 (with the summary) only how percentages are worked out; the notes are  |
     | drawn on pages 1 and 2 under their tables.                               |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    private static void checkPageNotes()
    {
        List<Round> rounds = steady(50);
        Round third = rounds.get(3);
        rounds.set(3, new Round(third.id(), third.teeId(), third.date(), 0, 0, Tee.HOLES, third.scores(), third.putts(), third.fairway(),
                third.sand(), true));
        Round later = rounds.get(30);
        rounds.set(30, round(later.id(), LINKS, later.date().toString(), 0, Tee.NINE, 1, 2, true, new int[0], false));
        RoundLogReport log = new RoundLogReport(data(rounds), LINKS.id(), null, null, true);
        double letter = Sheet.LETTER_SHORT;
        check("page notes: 22 rounds a page with room for the note, three pages", log.rowsPerPage(letter) == 22
                && log.pageCount(letter) == 3 && !log.summaryAlone(letter));
        check("page notes: each page's note has only the parts for that page", log.noteOn(0, letter).equals(RoundLogReport.HISTORICAL_PART)
                && log.noteOn(1, letter).equals(RoundLogReport.DAGGER_PART) && log.noteOn(2, letter).equals(RoundLogReport.PERCENT_PART));
        int noteTop = (int) (RoundLogReport.TABLE_TOP + Table.HEADER_HEIGHT + 22 * RoundLogReport.ROW_HEIGHT + Table.END_RULE
                + RoundLogReport.NOTE_GAP);
        BufferedImage first = PrintSelfTest.picture(log, 0, letter);
        BufferedImage second = PrintSelfTest.picture(log, 1, letter);
        check("page notes: the note is drawn under the table on pages 1 and 2, above the foot rule", PrintSelfTest.near(first, 60, noteTop,
                400, noteTop + 14, Sheet.GREY) > 20 && PrintSelfTest.near(second, 60, noteTop, 400, noteTop + 14, Sheet.GREY) > 20
                && noteTop + 14 < letter - Report.FOOT_RULE_UP);
        RoundLogReport plain = new RoundLogReport(data(steady(50)), LINKS.id(), null, null, true);
        BufferedImage none = PrintSelfTest.picture(plain, 0, letter);
        check("page notes: a page with no dagger or H has no note", plain.noteOn(0, letter).isEmpty()
                && PrintSelfTest.near(none, 60, noteTop, 400, noteTop + 14, Sheet.GREY) == 0);
    }

    /*
     +--------------------------------------------------------------------------+
     | checkBoldDaggers()                                                       |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The notes' dagger is bold at the note's own size: drawn by               |
     | textWithBoldDagger it has more ink than a plain one, the words after it  |
     | are the same, and on a Round Log page whose note starts with the dagger  |
     | the dagger there has the bold one's ink.                                 |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    private static void checkBoldDaggers()
    {
        java.awt.Font font = Sheet.sans(java.awt.Font.PLAIN, RoundLogReport.NOTE_SIZE);
        BufferedImage plain = blank(200, 40);
        BufferedImage bold = blank(200, 40);
        java.awt.Graphics2D plainPen = plain.createGraphics();
        java.awt.Graphics2D boldPen = bold.createGraphics();
        new Sheet(plainPen, 200, 40).text(Sheet.DAGGER, 10, 10, font, Sheet.GREY);
        new Sheet(boldPen, 200, 40).textWithBoldDagger(Sheet.DAGGER, 10, 10, font, Sheet.GREY);
        plainPen.dispose();
        boldPen.dispose();
        int plainInk = PrintSelfTest.inked(plain, 0, 0, 200, 40);
        int boldInk = PrintSelfTest.inked(bold, 0, 0, 200, 40);
        double daggerWide = Sheet.textWidth(Sheet.DAGGER, font.deriveFont(java.awt.Font.BOLD));
        check("bold daggers: the notes' dagger is bold, the words round it as before", boldInk > plainInk
                && Sheet.boldDaggerRoom(500, "a " + Sheet.DAGGER + " b " + Sheet.DAGGER, font) <= 500
                && Sheet.boldDaggerRoom(500, "no dagger", font) == 500);

        List<Round> rounds = steady(50);
        Round later = rounds.get(30);
        rounds.set(30, round(later.id(), LINKS, later.date().toString(), 0, Tee.NINE, 1, 2, true, new int[0], false));
        RoundLogReport log = new RoundLogReport(data(rounds), LINKS.id(), null, null, true);
        double letter = Sheet.LETTER_SHORT;
        int noteTop = (int) (RoundLogReport.TABLE_TOP + Table.HEADER_HEIGHT + 22 * RoundLogReport.ROW_HEIGHT + Table.END_RULE
                + RoundLogReport.NOTE_GAP);
        BufferedImage page = PrintSelfTest.picture(log, 1, letter);
        int pageInk = PrintSelfTest.inked(page, (int) Sheet.MARGIN, noteTop - 2, (int) (Sheet.MARGIN + daggerWide), noteTop + 16);
        check("bold daggers: the Round Log note's dagger is drawn bold (" + pageInk + " pixels, plain " + plainInk + ")",
                log.noteOn(1, letter).startsWith(Sheet.DAGGER) && pageInk >= boldInk - 2 && pageInk > plainInk);
    }

    /*
     * blank(width, height): a white picture to draw a test on.
     */
    private static BufferedImage blank(int width, int height)
    {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D pen = image.createGraphics();
        pen.setColor(java.awt.Color.WHITE);
        pen.fillRect(0, 0, width, height);
        pen.dispose();
        return image;
    }

    /*
     +--------------------------------------------------------------------------+
     | checkHistoricalSample()                                                  |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The historical sample picture's data: Samples.withHistorical marks only  |
     | the two oldest rounds on the tees, leaves the data it was given as it    |
     | was, and the Round Log drawn from it has H marks and the H legend.       |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    private static void checkHistoricalSample()
    {
        ReportData data = data(steady(6));
        ReportData copy = Samples.withHistorical(data, LINKS.id(), 2);
        int marked = 0;
        for (Round round : copy.rounds())
        {
            if (round.historical())
            {
                marked = marked + 1;
            }
        }
        boolean untouched = true;
        for (Round round : data.rounds())
        {
            untouched = untouched && !round.historical();
        }
        RoundLogReport log = new RoundLogReport(copy, LINKS.id(), null, null, true);
        check("historical sample: two oldest rounds marked in a copy only, the H and its legend printed", marked == 2
                && copy.rounds().get(0).historical() && copy.rounds().get(1).historical() && !copy.rounds().get(2).historical()
                && untouched && copy.includeHistorical() && log.noteOn(0, Sheet.LETTER_SHORT).contains(RoundLogReport.HISTORICAL_PART));
    }
}

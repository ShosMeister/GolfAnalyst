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
     | Page breaks on Letter (850 high on its side: 23 rounds a page) and A4    |
     | (778 high: fewer): one page; 50 rounds on three pages with the summary   |
     | under the last four; 46 rounds filling two pages so the summary goes on  |
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
                && small.rowsPerPage(letter) == 23 && small.rowsPerPage(a4) < 23 && small.subtitle(0).endsWith("all 10 rounds"));
        RoundLogReport fifty = new RoundLogReport(data(steady(50)), LINKS.id(), null, null, false);
        check("round log pages: 50 rounds take three pages with the summary under the last rounds", fifty.pageCount(letter) == 3
                && !fifty.summaryAlone(letter) && fifty.subtitle(1).endsWith("all 50 rounds (continued)")
                && !fifty.subtitle(0).endsWith("(continued)"));
        RoundLogReport full = new RoundLogReport(data(steady(46)), LINKS.id(), null, null, false);
        check("round log pages: when the summary does not fit it gets a page of its own", full.pageCount(letter) == 3
                && full.summaryAlone(letter));
        BufferedImage first = PrintSelfTest.picture(fifty, 0, letter);
        BufferedImage last = PrintSelfTest.picture(fifty, 2, letter);
        int groupTop = (int) (RoundLogReport.TABLE_TOP + Table.HEADER_HEIGHT + 4 * RoundLogReport.ROW_HEIGHT + Table.END_RULE
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
}

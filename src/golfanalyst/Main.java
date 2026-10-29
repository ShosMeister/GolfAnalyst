package golfanalyst;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/*
 +--------------------------------------------------------------------------+
 | Main                                                                     |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | Command-line front end for the Golf Analyst engine. It loads the data    |
 | folder and prints the same information the old Home, Stats and           |
 | Analyzer screens showed, plus the full handicap history. The engine      |
 | classes (DataLoader, Handicap, Stats) know nothing about printing, so    |
 | a graphical screen can be added later without changing them.             |
 |                                                                          |
 | How to run:                                                              |
 |   java -cp out golfanalyst.Main [data folder]     (default: data)        |
 |   java -cp out golfanalyst.Main --selftest        (runs the tests)       |
 |                                                                          |
 | Public Methods:                                                          |
 | main(args) - program entry point                                         |
 +--------------------------------------------------------------------------+
 */
public final class Main
{

    private static final int RECENT_ROUNDS = 5;

    private Main()
    {
    }

    /*
     +--------------------------------------------------------------------------+
     | main(args)                                                               |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Loads the data folder, calculates the handicap and statistics, and       |
     | prints every report.                                                     |
     |                                                                          |
     | Passed in:  args - optional: the data folder, or --selftest              |
     | Passed out: nothing (the exit code is 1 if --selftest fails)             |
     +--------------------------------------------------------------------------+
     */
    public static void main(String[] args) throws IOException
    {
        if (args.length > 0 && args[0].equals("--selftest"))
        {
            System.exit(SelfTest.run() ? 0 : 1);
        }
        Path dir = Path.of(args.length > 0 ? args[0] : "data");
        Dataset data = DataLoader.load(dir);
        Map<Integer, Tee> tees = data.teeMap();

        System.out.println("GOLF ANALYST   data folder: " + dir.toAbsolutePath());
        System.out.println(data.tees().size() + " course/tee rows, " + data.rounds().size() + " rounds loaded, "
                + data.errorCount() + " error(s), " + data.warningCount() + " warning(s)");
        for (Problem problem : data.problems())
        {
            System.out.println("  " + problem);
        }
        System.out.println();
        if (data.rounds().isEmpty())
        {
            return;
        }

        List<Round> byDate = new ArrayList<>(data.rounds());
        byDate.sort((first, second) -> first.date().compareTo(second.date()) != 0
                ? first.date().compareTo(second.date()) : Integer.compare(first.id(), second.id()));

        Handicap.Result handicap = Handicap.compute(data.rounds(), tees);
        printHome(handicap, Stats.summarize(byDate, tees), byDate, tees);
        printCourseStats(byDate, tees);
        printHistory(handicap);
        printRecord(handicap);
    }

    /*
     +--------------------------------------------------------------------------+
     | printHome(handicap, all, byDate, tees)                                   |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Prints the Home view: Handicap Index, overall statistics and a line      |
     | for the most recent rounds. Averages are shown separately for            |
     | 18-hole and 9-hole rounds, since a 9-hole score cannot be compared       |
     | with an 18-hole one.                                                     |
     |                                                                          |
     | Passed in:  handicap - the handicap Result                               |
     |             all      - statistics over every round                       |
     |             byDate   - all rounds, oldest first                          |
     |             tees     - course/tees keyed by id                           |
     | Passed out: nothing (prints)                                             |
     +--------------------------------------------------------------------------+
     */
    private static void printHome(Handicap.Result handicap, Stats.Summary all, List<Round> byDate, Map<Integer, Tee> tees)
    {
        System.out.println("HOME");
        System.out.println("  Handicap Index     " + Handicap.format(handicap.currentIndex())
                + (handicap.currentIndex() == null ? "   (needs 54 holes played)" : "")
                + (handicap.lowIndex() != null ? "   Low Index (365 days): " + Handicap.format(handicap.lowIndex()) : ""));
        System.out.println("  Rounds             " + all.rounds() + "   (" + all.rounds18() + " of 18 holes, "
                + all.rounds9() + " of 9 holes)");
        System.out.println("  Average score      18 holes: " + oneDecimal(all.avgScore18()) + " (best "
                + bestText(all.bestScore18()) + ")     9 holes: " + oneDecimal(all.avgScore9()) + " (best "
                + bestText(all.bestScore9()) + ")");
        System.out.println("  Average putts      18 holes: " + oneDecimal(all.avgPutts18())
                + "     9 holes: " + oneDecimal(all.avgPutts9()));
        System.out.println("  Fairways hit       " + oneDecimal(all.fairwayPct()) + "%   (" + all.fairwayHits()
                + " of " + all.fairwayChances() + " par 4/5 holes)");
        System.out.println("  Greens in reg.     " + oneDecimal(all.girPct()) + "%");
        System.out.println("  2 putts or fewer   " + oneDecimal(all.twoPuttPct()) + "%   (the rest were 3+ putts)");
        System.out.println("  Sand saves         " + oneDecimal(all.sandSavePct()) + "%   (" + all.sandSaves()
                + " of " + all.sandShots() + ")");
        System.out.println("  Avg score by par   par 3: " + twoDecimals(all.parAverage()[0])
                + "   par 4: " + twoDecimals(all.parAverage()[1]) + "   par 5: " + twoDecimals(all.parAverage()[2]));
        StringBuilder results = new StringBuilder("  Results            ");
        for (int lp = 0; lp < all.distribution().length; lp = lp + 1)
        {
            results.append(Stats.DIST_LABELS[lp]).append(' ').append(all.distribution()[lp])
                    .append(lp < all.distribution().length - 1 ? " | " : "");
        }
        System.out.println(results);

        List<Round> recent = byDate.subList(Math.max(0, byDate.size() - RECENT_ROUNDS), byDate.size());
        Stats.Summary recentStats = Stats.summarize(recent, tees);
        System.out.println("  Last " + recentStats.rounds() + " rounds: fairways " + oneDecimal(recentStats.fairwayPct())
                + "%, GIR " + oneDecimal(recentStats.girPct()) + "%, 2 putts or fewer "
                + oneDecimal(recentStats.twoPuttPct()) + "%");
        System.out.println();
    }

    /*
     +--------------------------------------------------------------------------+
     | printCourseStats(byDate, tees)                                           |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Prints the Stats view: one line of statistics for every course/tee       |
     | that has been played, with 18-hole and 9-hole results side by side.      |
     |                                                                          |
     | Passed in:  byDate - all rounds, oldest first                            |
     |             tees   - course/tees keyed by id                             |
     | Passed out: nothing (prints)                                             |
     +--------------------------------------------------------------------------+
     */
    private static void printCourseStats(List<Round> byDate, Map<Integer, Tee> tees)
    {
        System.out.println("STATS BY COURSE / TEE");
        System.out.printf(Locale.ROOT, "  %-34s %4s %6s %5s %4s %6s %5s %6s %6s %9s%n",
                "Course (tee)", "R18", "Avg18", "Best", "R9", "Avg9", "Best", "Fwy%", "GIR%", "<=2putt%");
        Map<Integer, List<Round>> roundsByTee = new TreeMap<>();
        for (Round round : byDate)
        {
            roundsByTee.computeIfAbsent(round.teeId(), key -> new ArrayList<>()).add(round);
        }
        for (Map.Entry<Integer, List<Round>> group : roundsByTee.entrySet())
        {
            Tee tee = tees.get(group.getKey());
            Stats.Summary stats = Stats.summarize(group.getValue(), tees);
            System.out.printf(Locale.ROOT, "  %-34s %4d %6s %5s %4d %6s %5s %6s %6s %9s%n",
                    tee.label(), stats.rounds18(), oneDecimal(stats.avgScore18()), bestText(stats.bestScore18()),
                    stats.rounds9(), oneDecimal(stats.avgScore9()), bestText(stats.bestScore9()),
                    oneDecimal(stats.fairwayPct()), oneDecimal(stats.girPct()), oneDecimal(stats.twoPuttPct()));
        }
        System.out.println();
    }

    /*
     +--------------------------------------------------------------------------+
     | printHistory(handicap)                                                   |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Prints every round used for the handicap, newest first, with its         |
     | adjusted score, 18-hole differential, any Exceptional Score              |
     | Reduction, the index after it was posted, and a note. For a 9-hole       |
     | round the note shows the 9-hole differential and the expected            |
     | differential that were added together; for a round waiting for the       |
     | first index it says so; when a cap held the index back it says that.     |
     |                                                                          |
     | Passed in:  handicap - the handicap Result                               |
     | Passed out: nothing (prints)                                             |
     +--------------------------------------------------------------------------+
     */
    private static void printHistory(Handicap.Result handicap)
    {
        System.out.println("HANDICAP HISTORY (most recent first)");
        System.out.printf(Locale.ROOT, "  %-10s %-34s %5s %5s %5s %6s %5s %7s %s%n",
                "Date", "Course (tee)", "Holes", "Score", "Adj", "Diff", "ESR", "Index", "Note");
        List<Handicap.Entry> newestFirst = new ArrayList<>(handicap.entries());
        Collections.reverse(newestFirst);
        for (Handicap.Entry entry : newestFirst)
        {
            String note = "";
            if (Double.isNaN(entry.differential()))
            {
                note = "9-hole score waiting for the first index";
            }
            else if (!Double.isNaN(entry.nineHoleDifferential()))
            {
                note = String.format(Locale.ROOT, "9-hole diff %.1f + expected %.1f",
                        entry.nineHoleDifferential(), Handicap.round1(entry.differential() - entry.nineHoleDifferential()));
            }
            if (entry.cap() != Handicap.Cap.NONE)
            {
                note = note + (note.isEmpty() ? "" : "; ") + entry.cap() + " cap (uncapped "
                        + Handicap.format(entry.calculatedIndex()) + ")";
            }
            String differentialText = Double.isNaN(entry.differential()) ? "-"
                    : String.format(Locale.ROOT, "%.1f", entry.differential());
            String esr = entry.esrAdjustment() == 0 ? "" : String.format(Locale.ROOT, "%.1f", entry.esrAdjustment());
            System.out.printf(Locale.ROOT, "  %-10s %-34s %5s %5d %5d %6s %5s %7s %s%n",
                    entry.round().date(), entry.tee().label(), entry.round().holesLabel(entry.tee()),
                    entry.round().totalScore(), entry.adjustedGross(), differentialText, esr,
                    Handicap.format(entry.indexAfter()), note);
        }
        System.out.println();
    }

    /*
     +--------------------------------------------------------------------------+
     | printRecord(handicap)                                                    |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Prints the current scoring record: the (up to) 20 differentials in       |
     | date order, with a * on the lowest ones that count toward the index.     |
     |                                                                          |
     | Passed in:  handicap - the handicap Result                               |
     | Passed out: nothing (prints)                                             |
     +--------------------------------------------------------------------------+
     */
    private static void printRecord(Handicap.Result handicap)
    {
        List<Double> current = handicap.currentDifferentials();
        int countUsed = Handicap.differentialsUsed(current.size());
        Integer[] lowestFirst = new Integer[current.size()];
        for (int lp = 0; lp < lowestFirst.length; lp = lp + 1)
        {
            lowestFirst[lp] = lp;
        }
        Arrays.sort(lowestFirst, (first, second) -> Double.compare(current.get(first), current.get(second)));
        boolean[] counts = new boolean[current.size()];
        for (int lp = 0; lp < countUsed; lp = lp + 1)
        {
            counts[lowestFirst[lp]] = true;
        }

        System.out.println("CURRENT SCORING RECORD: " + current.size() + " differential(s); lowest " + countUsed + " count");
        StringBuilder line = new StringBuilder("  ");
        for (int lp = 0; lp < current.size(); lp = lp + 1)
        {
            line.append(String.format(Locale.ROOT, "%.1f%s ", current.get(lp), counts[lp] ? "*" : ""));
        }
        System.out.println(line);
        System.out.println("  (oldest to newest; * = counts toward the index)");
    }

    /*
     * oneDecimal(value): formats a number for a report with one decimal place.
     * Passed in:  value - any number, NaN meaning "no data"
     * Passed out: text like "94.3", or "n/a"
     */
    private static String oneDecimal(double value)
    {
        return Double.isNaN(value) ? "n/a" : String.format(Locale.ROOT, "%.1f", value);
    }

    /*
     * twoDecimals(value): formats a number for a report with two decimal places.
     * Passed in:  value - any number, NaN meaning "no data"
     * Passed out: text like "4.27", or "n/a"
     */
    private static String twoDecimals(double value)
    {
        return Double.isNaN(value) ? "n/a" : String.format(Locale.ROOT, "%.2f", value);
    }

    /*
     * bestText(score): formats a best score, where 0 means there were no rounds.
     * Passed in:  score - the best score, or 0 for none
     * Passed out: the score as text, or "-"
     */
    private static String bestText(int score)
    {
        return score == 0 ? "-" : Integer.toString(score);
    }
}

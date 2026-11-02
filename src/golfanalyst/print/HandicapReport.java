package golfanalyst.print;

import golfanalyst.Handicap;
import golfanalyst.Round;
import golfanalyst.Tee;
import java.awt.Font;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/*
 +--------------------------------------------------------------------------+
 | HandicapReport                                                           |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | The Handicap Report (upright, always one page): the Handicap Index and   |
 | the last 20 rounds that count for the handicap, most recent first, with  |
 | the rounds that make up the index shaded, and a few lines saying how     |
 | the index is worked out. Historical rounds are never in it.              |
 |                                                                          |
 | The box from (60,170) to (790,262): "Handicap Index" and the index in    |
 | Serif bold 44, then Low Index (365 days), Rounds counted ("8 of 20") and |
 | Latest round. With no index yet it says "No index yet" and that an index |
 | needs scores for at least 54 holes. The table at y 284: Date, Course     |
 | (tee), Holes (18, 9, Front 9 or Back 9), Score, Adj., Differential and a |
 | dagger for a nine-hole round, rows 26 high. Under it the notes.          |
 |                                                                          |
 | Which rounds count is the engine's own choice: the lowest of the         |
 | differentials on record (Handicap.differentialsUsed of them), as         |
 | Handicap.indexFromDifferentials picks them (a tie goes to the older      |
 | round). Each round's Differential column is its own differential, as     |
 | the Round screen shows it; when an exceptional score reduction, a cap    |
 | or the rules' adjustment for a short record makes the index differ from  |
 | their average, the first note says so.                                   |
 |                                                                          |
 | Public Methods:                                                          |
 | the Report methods; entries() - the rounds listed, most recent first;    |
 | counts(entry) - whether a round is shaded; table(); notes()              |
 +--------------------------------------------------------------------------+
 */
public final class HandicapReport extends Report
{

    public static final String NAME = "Handicap Report";
    public static final int LISTED = 20;
    public static final String NO_INDEX = "No index yet";
    public static final String NEEDS_54 = "A Handicap Index needs scores for at least 54 holes.";
    public static final String ADJ_NOTE = "Adj. = your score after limiting each hole to a net double bogey, the most a hole can count "
            + "towards your handicap.";
    public static final String DAGGER_NOTE = Sheet.DAGGER + " = a nine-hole round. Its differential is the nine-hole differential "
            + "plus an expected score for the other nine holes, so it can sit beside 18-hole rounds.";
    public static final String FORMULA_NOTE = "Differential = (113 / slope) x (adjusted score - course rating - playing conditions "
            + "adjustment). Historical rounds are not used.";
    static final double BOX_TOP = 170;
    static final double BOX_HEIGHT = 92;
    static final double TABLE_TOP = 284;
    static final double ROW_HEIGHT = 26;
    static final double NOTES_GAP = 16;
    private final List<Handicap.Entry> entries = new ArrayList<>();
    private final Set<Integer> countingIds = new HashSet<>();
    private final List<Handicap.Entry> counting = new ArrayList<>();
    private boolean reduced = false;

    /*
     +--------------------------------------------------------------------------+
     | HandicapReport(data)                                                     |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Takes the last 20 rounds the handicap used, newest first, and finds the  |
     | ones that make up today's index (see the class comment).                 |
     |                                                                          |
     | Passed in:  data - the golfer's data                                     |
     | Passed out: the report                                                   |
     +--------------------------------------------------------------------------+
     */
    public HandicapReport(ReportData data)
    {
        super(data);
        List<Handicap.Entry> all = data.handicap().entries();
        for (int lp = all.size() - 1; lp >= 0 && entries.size() < LISTED; lp = lp - 1)
        {
            entries.add(all.get(lp));
        }
        if (data.handicap().currentIndex() == null)
        {
            return;
        }
        List<Double> values = data.handicap().currentDifferentials();
        int first = all.size() - values.size();
        List<Integer> order = new ArrayList<>();
        for (int lp = 0; lp < values.size(); lp = lp + 1)
        {
            order.add(lp);
        }
        order.sort((one, two) -> Double.compare(values.get(one), values.get(two)));
        int used = Handicap.differentialsUsed(values.size());
        for (int lp = 0; lp < used && lp < order.size(); lp = lp + 1)
        {
            int place = order.get(lp);
            Handicap.Entry entry = all.get(first + place);
            countingIds.add(entry.round().id());
            if (Math.abs(values.get(place) - entry.differential()) > 1e-9)
            {
                reduced = true;
            }
        }
        for (Handicap.Entry entry : entries)
        {
            if (countingIds.contains(entry.round().id()))
            {
                counting.add(entry);
            }
        }
    }

    /*
     * entries(): the rounds listed, most recent first.
     */
    public List<Handicap.Entry> entries()
    {
        return entries;
    }

    /*
     * counts(entry): whether the round is one of those that make up today's index (its row is shaded).
     */
    public boolean counts(Handicap.Entry entry)
    {
        return countingIds.contains(entry.round().id());
    }

    /*
     * name(): "Handicap Report".
     */
    @Override
    public String name()
    {
        return NAME;
    }

    /*
     * landscape(): the Handicap Report is upright.
     */
    @Override
    public boolean landscape()
    {
        return false;
    }

    /*
     * isEmpty(): true when the handicap has used no rounds.
     */
    @Override
    public boolean isEmpty()
    {
        return entries.isEmpty();
    }

    /*
     * roundsWords(): "your last 20 rounds", "your last 12 rounds", or "your one round".
     */
    private String roundsWords()
    {
        if (entries.size() == 1)
        {
            return "your one round";
        }
        return "your last " + entries.size() + " rounds";
    }

    /*
     * describe(): "one page: the Handicap Report for your last 20 rounds".
     */
    @Override
    public String describe()
    {
        return "one page: the Handicap Report for " + roundsWords();
    }

    /*
     * heading(): "Handicap Report".
     */
    @Override
    protected String heading()
    {
        return NAME;
    }

    /*
     * subtitle(page): "Angelo   -   your last 20 rounds, as of November 1, 2026".
     */
    @Override
    protected String subtitle(int page)
    {
        return named(roundsWords() + ", as of " + longDate(data.today()));
    }

    /*
     * pageCount(height): always one page.
     */
    @Override
    public int pageCount(double height)
    {
        return 1;
    }

    /*
     * hasNineHole(): whether a nine-hole round is listed (then the dagger note is printed).
     */
    private boolean hasNineHole()
    {
        for (Handicap.Entry entry : entries)
        {
            if (entry.round().isNineHole())
            {
                return true;
            }
        }
        return false;
    }

    /*
     +--------------------------------------------------------------------------+
     | table()                                                                  |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The rounds as a table: Date 110, Course (tee) 260, Holes 80, Score 70,   |
     | Adj. 70, Differential 110 (one decimal, "-" while a nine waits for the   |
     | first index) and 30 for the dagger; the counting rounds shaded.          |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: the table                                                    |
     +--------------------------------------------------------------------------+
     */
    public Table table()
    {
        Table table = new Table(List.of(new Table.Column("Date", 110, Table.Align.LEFT),
                new Table.Column("Course (tee)", 260, Table.Align.LEFT), new Table.Column("Holes", 80, Table.Align.CENTRE),
                new Table.Column("Score", 70, Table.Align.CENTRE), new Table.Column("Adj.", 70, Table.Align.CENTRE),
                new Table.Column("Differential", 110, Table.Align.RIGHT), new Table.Column("", 30, Table.Align.CENTRE)), ROW_HEIGHT);
        for (Handicap.Entry entry : entries)
        {
            Round round = entry.round();
            Tee tee = entry.tee();
            String mark = "";
            if (round.isNineHole())
            {
                mark = Sheet.DAGGER;
            }
            Table.Kind kind = Table.Kind.NORMAL;
            if (counts(entry))
            {
                kind = Table.Kind.SHADED;
            }
            table.add(new Table.Row(List.of(round.date().toString(), tee.label(), holesShort(round, tee),
                    Integer.toString(round.totalScore()), Integer.toString(entry.adjustedGross()), oneDecimal(entry.differential()),
                    mark), kind));
        }
        return table;
    }

    /*
     +--------------------------------------------------------------------------+
     | notes()                                                                  |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The paragraphs under the table: how the index comes from the shaded      |
     | rounds (only once there is an index), what Adj. means, what the dagger   |
     | means (only if one is printed) and how a differential is worked out.     |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: the paragraphs, in order                                     |
     +--------------------------------------------------------------------------+
     */
    public List<String> notes()
    {
        List<String> notes = new ArrayList<>();
        Double index = data.handicap().currentIndex();
        if (index != null && !counting.isEmpty())
        {
            notes.add(indexNote(index));
        }
        notes.add(ADJ_NOTE);
        if (hasNineHole())
        {
            notes.add(DAGGER_NOTE);
        }
        notes.add(FORMULA_NOTE);
        return notes;
    }

    /*
     +--------------------------------------------------------------------------+
     | indexNote(index)                                                         |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | "Your 8 lowest differentials (the shaded rounds, most recent first):     |
     | 13.9, 17.1, .... Their average is 15.26, so your Handicap Index is       |
     | 15.3." When something besides the average changed the index, the last    |
     | sentence names it: "Their average is 15.26; after the cap on how fast    |
     | an index can rise your Handicap Index is 15.0."                          |
     |                                                                          |
     | Passed in:  index - today's Handicap Index                               |
     | Passed out: the paragraph                                                |
     +--------------------------------------------------------------------------+
     */
    String indexNote(double index)
    {
        List<String> numbers = new ArrayList<>();
        double total = 0;
        for (Handicap.Entry entry : counting)
        {
            numbers.add(oneDecimal(entry.differential()));
            total = total + entry.differential();
        }
        double average = total / counting.size();
        String lowest = "Your lowest differential";
        if (counting.size() > 1)
        {
            lowest = "Your " + counting.size() + " lowest differentials";
        }
        String paragraph = lowest + " (the shaded rounds, most recent first): " + String.join(", ", numbers) + ". ";
        String averageWords = String.format(Locale.ROOT, "%.2f", average);
        if (counting.size() == 1)
        {
            paragraph = lowest + " (the shaded round): " + String.join(", ", numbers) + ". ";
        }
        List<String> reasons = new ArrayList<>();
        int onRecord = data.handicap().currentDifferentials().size();
        if (reduced)
        {
            reasons.add("the exceptional score reduction");
        }
        if (onRecord == 3 || onRecord == 4 || onRecord == 6)
        {
            reasons.add("the rules' adjustment for having only " + onRecord + " rounds");
        }
        List<Handicap.Entry> all = data.handicap().entries();
        if (!all.isEmpty() && all.get(all.size() - 1).cap() != Handicap.Cap.NONE)
        {
            reasons.add("the cap on how fast an index can rise");
        }
        if (reasons.isEmpty())
        {
            return paragraph + "Their average is " + averageWords + ", so your Handicap Index is " + Handicap.format(index) + ".";
        }
        return paragraph + "Their average is " + averageWords + "; after " + String.join(" and ", reasons)
                + " your Handicap Index is " + Handicap.format(index) + ".";
    }

    /*
     * countedWords(): "8 of 20": the rounds that count, of the rounds listed.
     */
    public String countedWords()
    {
        return counting.size() + " of " + entries.size();
    }

    /*
     +--------------------------------------------------------------------------+
     | paintBody(sheet, page, pages)                                            |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The index box, the table of rounds and the notes under it.               |
     |                                                                          |
     | Passed in:  sheet - the page; page, pages - always 0 and 1               |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    @Override
    protected void paintBody(Sheet sheet, int page, int pages)
    {
        double across = sheet.width() - Sheet.MARGIN - Sheet.MARGIN;
        sheet.roundBox(Sheet.MARGIN, BOX_TOP, across, BOX_HEIGHT, 10, Sheet.BOX, Sheet.BROWN, 2);
        sheet.text("Handicap Index", 90, 188, Sheet.sans(Font.PLAIN, 13), Sheet.GREY);
        Double index = data.handicap().currentIndex();
        if (index == null)
        {
            double size = 44;
            while (size > 20 && Sheet.textWidth(NO_INDEX, Sheet.serif(Font.BOLD, size)) > 230)
            {
                size = size - 2;
            }
            sheet.text(NO_INDEX, 90, 205, Sheet.serif(Font.BOLD, size), Sheet.BROWN);
            paragraphs(sheet, List.of(NEEDS_54), 330, 200, 430, Sheet.sans(Font.PLAIN, 13), Sheet.INK, 0);
        }
        else
        {
            sheet.text(Handicap.format(index), 90, 205, Sheet.serif(Font.BOLD, 44), Sheet.BROWN);
            String low = "-";
            if (data.handicap().lowIndex() != null)
            {
                low = Handicap.format(data.handicap().lowIndex());
            }
            figure(sheet, 330, "Low Index (365 days)", low);
            figure(sheet, 480, "Rounds counted", countedWords());
            figure(sheet, 630, "Latest round", entries.get(0).round().date().toString());
        }
        Table table = table();
        double below = table.draw(sheet, Sheet.MARGIN, TABLE_TOP, 0, table.rows().size());
        paragraphs(sheet, notes(), Sheet.MARGIN, below + NOTES_GAP, across, Sheet.sans(Font.PLAIN, 12), Sheet.INK, 4);
    }

    /*
     * figure(sheet, x, caption, value): a grey 12 caption at y 190 and its bold 20 value at y 212.
     */
    private static void figure(Sheet sheet, double x, String caption, String value)
    {
        sheet.text(caption, x, 190, Sheet.sans(Font.PLAIN, 12), Sheet.GREY);
        sheet.text(value, x, 212, Sheet.sans(Font.BOLD, 20), Sheet.INK);
    }
}

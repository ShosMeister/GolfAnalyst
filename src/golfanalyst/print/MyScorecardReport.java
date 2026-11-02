package golfanalyst.print;

import golfanalyst.Handicap;
import golfanalyst.Tee;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/*
 +--------------------------------------------------------------------------+
 | MyScorecardReport                                                        |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | My scorecard for a course (on its side): a card to take out on the       |
 | course. Rows Par, Hdcp, Strokes and Pick-up, then four empty rows to     |
 | write in (Score, Putts, Fairway, Sand), each 38 high with cell outlines, |
 | in the scorecard style with Out, In and Total (a nine-hole course has    |
 | only Total).                                                             |
 |                                                                          |
 | Course handicap: round(index x slope / 113 + (rating - par)) for 18      |
 | holes; for a nine-hole course half the index with the nine's rating,     |
 | slope and par (Handicap.courseHandicap and nineHoleCourseHandicap).      |
 | Strokes on a hole: the course handicap divided by the holes (the whole   |
 | part), plus one where the hole's handicap number is no more than the     |
 | remainder; a plus handicap gives a stroke back ("-1") on the easiest     |
 | holes (Handicap.strokesReceived). Pick-up = par + 2 + strokes (the net   |
 | double bogey, the most a hole can count). With no index the Strokes row  |
 | is empty, pick-up is par + 5 and a grey line says so. A box under the    |
 | card shows Course handicap, Rating / slope, Yards and Par.               |
 |                                                                          |
 | Public Methods:                                                          |
 | the Report methods; tee(), index(), courseHandicap(), strokes(),         |
 | pickUps(), headings(), rowTexts(), figures()                             |
 +--------------------------------------------------------------------------+
 */
public final class MyScorecardReport extends Report
{

    public static final String NAME = "My Scorecard";
    public static final List<String> LABELS = List.of("Par", "Hdcp", "Strokes", "Pick-up", "Score", "Putts", "Fairway", "Sand");
    public static final String PICK_UP_NOTE = "Pick-up = the most a hole can count towards your handicap (net double bogey). "
            + "When you reach it, pick up and move on.";
    public static final String NO_INDEX_NOTE = "You have no Handicap Index yet, so there are no strokes and the pick-up number is "
            + "par + 5.";
    static final double CARD_TOP = 172;
    static final double ROW_HEIGHT = 38;
    static final double NOTE_GAP = 12;
    static final double BOX_GAP = 30;
    static final double BOX_HEIGHT = 90;
    private final Tee tee;

    /*
     * MyScorecardReport(data, tee): the card for these tees (null for none), with the golfer's Handicap Index today.
     */
    public MyScorecardReport(ReportData data, Tee tee)
    {
        super(data);
        this.tee = tee;
    }

    /*
     * tee(): the tees on the card, or null.
     */
    public Tee tee()
    {
        return tee;
    }

    /*
     * index(): the Handicap Index used, or null when there is none yet.
     */
    public Double index()
    {
        return data.handicap().currentIndex();
    }

    /*
     * indexWords(): "Handicap Index 15.3" or "no Handicap Index yet".
     */
    public String indexWords()
    {
        if (index() == null)
        {
            return "no Handicap Index yet";
        }
        return "Handicap Index " + Handicap.format(index());
    }

    /*
     * name(): "My Scorecard".
     */
    @Override
    public String name()
    {
        return NAME;
    }

    /*
     * landscape(): printed on its side.
     */
    @Override
    public boolean landscape()
    {
        return true;
    }

    /*
     * isEmpty(): true when there are no tees to print.
     */
    @Override
    public boolean isEmpty()
    {
        return tee == null;
    }

    /*
     * describe(): "one page: your scorecard for Sample Links (White)".
     */
    @Override
    public String describe()
    {
        if (tee == null)
        {
            return "nothing";
        }
        return "one page: your scorecard for " + tee.label();
    }

    /*
     * heading(): "My Scorecard   -   Sample Links (White)".
     */
    @Override
    protected String heading()
    {
        if (tee == null)
        {
            return NAME;
        }
        return NAME + SEPARATOR + tee.label();
    }

    /*
     * subtitle(page): "Angelo   -   Handicap Index 15.3" (or "no Handicap Index yet").
     */
    @Override
    protected String subtitle(int page)
    {
        return named(indexWords());
    }

    /*
     * pageCount(height): always one page.
     */
    @Override
    public int pageCount(double height)
    {
        return 1;
    }

    // ------------------------------------------------------------ the figures

    /*
     * courseHandicap(): the course handicap for these tees (negative for a plus handicap), or null with no index.
     */
    public Integer courseHandicap()
    {
        if (index() == null)
        {
            return null;
        }
        if (tee.holes() == Tee.NINE)
        {
            return Handicap.nineHoleCourseHandicap(index(), tee.rating(), tee.slope(), tee.par());
        }
        return Handicap.courseHandicap(index(), tee.rating(), tee.slope(), tee.par());
    }

    /*
     * strokes(): the strokes on each hole (negative where a plus handicap gives one back), or null with no index.
     */
    public int[] strokes()
    {
        Integer handicap = courseHandicap();
        if (handicap == null)
        {
            return null;
        }
        int[] ranks = tee.strokeRanksFor(0, tee.holes());
        int[] strokes = new int[tee.holes()];
        for (int hole = 0; hole < tee.holes(); hole = hole + 1)
        {
            strokes[hole] = Handicap.strokesReceived(handicap, ranks[hole], tee.holes());
        }
        return strokes;
    }

    /*
     * pickUps(): the pick-up number of each hole: par + 2 + strokes, or par + 5 with no index.
     */
    public int[] pickUps()
    {
        int[] strokes = strokes();
        int[] pickUps = new int[tee.holes()];
        for (int hole = 0; hole < tee.holes(); hole = hole + 1)
        {
            if (strokes == null)
            {
                pickUps[hole] = tee.pars()[hole] + Handicap.NO_INDEX_OVER_PAR;
            }
            else
            {
                pickUps[hole] = tee.pars()[hole] + 2 + strokes[hole];
            }
        }
        return pickUps;
    }

    /*
     * slots(): the card's columns: each hole (0 is hole 1), -1 for Out after hole 9, -2 for In and -3 for Total (a nine has only Total).
     */
    private List<Integer> slots()
    {
        List<Integer> slots = new ArrayList<>();
        for (int hole = 0; hole < tee.holes(); hole = hole + 1)
        {
            slots.add(hole);
            if (tee.holes() == Tee.HOLES && hole == Tee.NINE - 1)
            {
                slots.add(-1);
            }
        }
        if (tee.holes() == Tee.HOLES)
        {
            slots.add(-2);
        }
        slots.add(-3);
        return slots;
    }

    /*
     * headings(): the hole numbers, Out, In and Total.
     */
    public List<String> headings()
    {
        List<String> headings = new ArrayList<>();
        for (int slot : slots())
        {
            if (slot == -1)
            {
                headings.add(ScorecardReport.OUT);
            }
            else if (slot == -2)
            {
                headings.add(ScorecardReport.IN);
            }
            else if (slot == -3)
            {
                headings.add(ScorecardReport.TOTAL);
            }
            else
            {
                headings.add(Integer.toString(slot + 1));
            }
        }
        return headings;
    }

    /*
     * sum(values, slot): the total for Out (holes 1-9), In (10-18) or Total.
     */
    private static int sum(int[] values, int slot)
    {
        int first = 0;
        int last = values.length;
        if (slot == -1)
        {
            last = Tee.NINE;
        }
        else if (slot == -2)
        {
            first = Tee.NINE;
        }
        int total = 0;
        for (int hole = first; hole < last; hole = hole + 1)
        {
            total = total + values[hole];
        }
        return total;
    }

    /*
     +--------------------------------------------------------------------------+
     | rowTexts()                                                               |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The words of the eight body rows: Par (with totals), Hdcp, Strokes (the  |
     | strokes a hole gives, blank for none, with totals; empty with no index), |
     | Pick-up (no totals), then the four empty rows to write in.               |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: the rows, top to bottom                                      |
     +--------------------------------------------------------------------------+
     */
    public List<List<String>> rowTexts()
    {
        int[] strokes = strokes();
        int[] pickUps = pickUps();
        List<List<String>> rows = new ArrayList<>();
        for (int row = 0; row < LABELS.size(); row = row + 1)
        {
            List<String> texts = new ArrayList<>();
            for (int slot : slots())
            {
                String words = "";
                if (row == 0)
                {
                    words = numberOrTotal(tee.pars(), slot);
                }
                else if (row == 1 && slot >= 0)
                {
                    words = Integer.toString(tee.strokeIndex()[slot]);
                }
                else if (row == 2 && strokes != null && (slot < 0 || strokes[slot] != 0))
                {
                    words = numberOrTotal(strokes, slot);
                }
                else if (row == 3 && slot >= 0)
                {
                    words = numberOrTotal(pickUps, slot);
                }
                texts.add(words);
            }
            rows.add(texts);
        }
        return rows;
    }

    /*
     * numberOrTotal(values, slot): a hole's number, or the total for Out, In or Total.
     */
    private static String numberOrTotal(int[] values, int slot)
    {
        if (slot < 0)
        {
            return Integer.toString(sum(values, slot));
        }
        return Integer.toString(values[slot]);
    }

    /*
     * handicapWords(handicap): a course handicap as printed: "18", "0", or "+2" for a plus handicap.
     */
    static String handicapWords(int handicap)
    {
        if (handicap < 0)
        {
            return "+" + (0 - handicap);
        }
        return Integer.toString(handicap);
    }

    /*
     * figures(): Course handicap ("-" with no index), Rating / slope, Yards ("-" when not known) and Par.
     */
    public List<String[]> figures()
    {
        String handicap = "-";
        if (courseHandicap() != null)
        {
            handicap = handicapWords(courseHandicap());
        }
        String yards = "-";
        if (tee.yardage() > 0)
        {
            yards = Integer.toString(tee.yardage());
        }
        List<String[]> pairs = new ArrayList<>();
        pairs.add(new String[] {"Course handicap", handicap});
        pairs.add(new String[] {"Rating / slope", String.format(Locale.ROOT, "%.1f / %d", tee.rating(), tee.slope())});
        pairs.add(new String[] {"Yards", yards});
        pairs.add(new String[] {"Par", Integer.toString(tee.par())});
        return pairs;
    }

    /*
     +--------------------------------------------------------------------------+
     | paintBody(sheet, page, pages)                                            |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The card, the pick-up line (and the no-index line) under it and the box  |
     | of the course's figures.                                                 |
     |                                                                          |
     | Passed in:  sheet - the page; page, pages - always 0 and 1               |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    @Override
    protected void paintBody(Sheet sheet, int page, int pages)
    {
        if (tee == null)
        {
            return;
        }
        List<Boolean> totals = new ArrayList<>();
        for (int slot : slots())
        {
            totals.add(slot < 0);
        }
        double below = Card.paint(sheet, CARD_TOP, LABELS, headings(), totals, rowTexts(), ROW_HEIGHT, -1, Map.of(), -1, null);
        Font noteFont = Sheet.sans(Font.PLAIN, 11);
        double across = sheet.width() - Sheet.MARGIN - Sheet.MARGIN;
        double noteTop = below + NOTE_GAP;
        sheet.text(Sheet.fit(PICK_UP_NOTE, noteFont, across), Sheet.MARGIN, noteTop, noteFont, Sheet.GREY);
        noteTop = noteTop + Sheet.lineHeight(noteFont) + 2;
        if (index() == null)
        {
            sheet.text(Sheet.fit(NO_INDEX_NOTE, noteFont, across), Sheet.MARGIN, noteTop, noteFont, Sheet.GREY);
            noteTop = noteTop + Sheet.lineHeight(noteFont) + 2;
        }
        Card.paintFigures(sheet, figures(), noteTop + BOX_GAP, BOX_HEIGHT, 4, 0, null);
    }
}

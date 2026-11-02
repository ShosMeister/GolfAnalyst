package golfanalyst.print;

import golfanalyst.Round;
import golfanalyst.Stats;
import golfanalyst.Tee;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/*
 +--------------------------------------------------------------------------+
 | RoundLogReport                                                           |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | The Round Log (on its side): a list of rounds, oldest first, with their  |
 | figures, for one set of tees or all courses and for a period, then a     |
 | summary of each kind of round on the last page.                          |
 |                                                                          |
 | Public Methods:                                                          |
 | the Report methods; rounds() - the rounds listed, oldest first           |
 +--------------------------------------------------------------------------+
 */
public final class RoundLogReport extends Report
{

    public static final String NAME = "Round Log";
    public static final String ALL_COURSES = "All courses";
    private final Integer teeId;
    private final LocalDate from;
    private final LocalDate to;
    private final List<Round> rounds = new ArrayList<>();

    /*
     +--------------------------------------------------------------------------+
     | RoundLogReport(data, teeId, from, to, includeHistorical)                 |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Picks the rounds to list: on the tees (or every course), from the first  |
     | day to the last (both included; a missing end lets everything through    |
     | on that side), with historical rounds only if asked for.                 |
     |                                                                          |
     | Passed in:  data              - the golfer's data                        |
     |             teeId             - the tees, or null for all courses        |
     |             from, to          - the first and last day, or null          |
     |             includeHistorical - true to list historical rounds too       |
     | Passed out: the report                                                   |
     +--------------------------------------------------------------------------+
     */
    public RoundLogReport(ReportData data, Integer teeId, LocalDate from, LocalDate to, boolean includeHistorical)
    {
        super(data);
        this.teeId = teeId;
        this.from = from;
        this.to = to;
        for (Round round : Stats.counted(data.rounds(), includeHistorical))
        {
            boolean inside = data.tees().containsKey(round.teeId());
            if (teeId != null && round.teeId() != teeId)
            {
                inside = false;
            }
            if (from != null && round.date().isBefore(from))
            {
                inside = false;
            }
            if (to != null && round.date().isAfter(to))
            {
                inside = false;
            }
            if (inside)
            {
                rounds.add(round);
            }
        }
    }

    /*
     * rounds(): the rounds listed, oldest first.
     */
    public List<Round> rounds()
    {
        return rounds;
    }

    /*
     * name(): "Round Log".
     */
    @Override
    public String name()
    {
        return NAME;
    }

    /*
     * landscape(): the Round Log is printed on its side.
     */
    @Override
    public boolean landscape()
    {
        return true;
    }

    /*
     * isEmpty(): true when no round matches the choices.
     */
    @Override
    public boolean isEmpty()
    {
        return rounds.isEmpty();
    }

    /*
     * describe(): "15 rounds" or "1 round".
     */
    @Override
    public String describe()
    {
        return countWords(rounds.size());
    }

    /*
     * countWords(count): "15 rounds" or "1 round".
     */
    static String countWords(int count)
    {
        if (count == 1)
        {
            return "1 round";
        }
        return count + " rounds";
    }

    /*
     * heading(): "Round Log".
     */
    @Override
    protected String heading()
    {
        return NAME;
    }

    /*
     * courseWords(): the tees ("Sample Links (White)") or "All courses".
     */
    private String courseWords()
    {
        if (teeId == null)
        {
            return ALL_COURSES;
        }
        Tee tee = data.tees().get(teeId);
        if (tee == null)
        {
            return ALL_COURSES;
        }
        return tee.label();
    }

    /*
     +--------------------------------------------------------------------------+
     | subtitle(page)                                                           |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | "Angelo   -   Sample Links (White): all 15 rounds", or "...: 12 rounds   |
     | from 2025-11-01 to 2026-10-31", or "...: 1 round"; from the second page  |
     | it ends with " (continued)".                                             |
     |                                                                          |
     | Passed in:  page - which page (0 is the first)                           |
     | Passed out: the line                                                     |
     +--------------------------------------------------------------------------+
     */
    @Override
    protected String subtitle(int page)
    {
        String words;
        if (rounds.size() == 1)
        {
            words = countWords(1);
        }
        else if (from == null && to == null)
        {
            words = "all " + countWords(rounds.size());
        }
        else
        {
            words = countWords(rounds.size());
        }
        if (from != null && to != null)
        {
            words = words + " from " + from + " to " + to;
        }
        else if (from != null)
        {
            words = words + " from " + from;
        }
        else if (to != null)
        {
            words = words + " up to " + to;
        }
        String line = named(courseWords() + ": " + words);
        if (page > 0)
        {
            line = line + " (continued)";
        }
        return line;
    }

    /*
     * pageCount(height): how many pages the rounds and the summary take.
     */
    @Override
    public int pageCount(double height)
    {
        return 1;
    }

    /*
     * paintBody(sheet, page, pages): the rounds on this page, and the summary on the last page.
     */
    @Override
    protected void paintBody(Sheet sheet, int page, int pages)
    {
        // the rounds and the summary
    }
}

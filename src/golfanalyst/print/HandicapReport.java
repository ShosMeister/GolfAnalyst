package golfanalyst.print;

import golfanalyst.Handicap;
import java.util.ArrayList;
import java.util.List;

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
 | Public Methods:                                                          |
 | the Report methods; entries() - the rounds listed, most recent first     |
 +--------------------------------------------------------------------------+
 */
public final class HandicapReport extends Report
{

    public static final String NAME = "Handicap Report";
    public static final int LISTED = 20;
    private final List<Handicap.Entry> entries = new ArrayList<>();

    /*
     * HandicapReport(data): the report for the handicap in the data: the last 20 rounds it used, newest first.
     */
    public HandicapReport(ReportData data)
    {
        super(data);
        List<Handicap.Entry> all = data.handicap().entries();
        for (int lp = all.size() - 1; lp >= 0 && entries.size() < LISTED; lp = lp - 1)
        {
            entries.add(all.get(lp));
        }
    }

    /*
     * entries(): the rounds listed, most recent first.
     */
    public List<Handicap.Entry> entries()
    {
        return entries;
    }

    @Override
    public String name()
    {
        return NAME;
    }

    @Override
    public boolean landscape()
    {
        return false;
    }

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

    @Override
    public String describe()
    {
        return "one page: the Handicap Report for " + roundsWords();
    }

    @Override
    protected String heading()
    {
        return NAME;
    }

    @Override
    protected String subtitle(int page)
    {
        return named(roundsWords() + ", as of " + longDate(data.today()));
    }

    @Override
    public int pageCount(double height)
    {
        return 1;
    }

    @Override
    protected void paintBody(Sheet sheet, int page, int pages)
    {
        // the index box, the table and the notes
    }
}

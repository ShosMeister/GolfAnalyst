package golfanalyst.print;

import golfanalyst.Handicap;
import golfanalyst.Tee;

/*
 +--------------------------------------------------------------------------+
 | MyScorecardReport                                                        |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | My scorecard for a course (on its side): a card to take out on the       |
 | course, with the holes, pars and handicap numbers, the strokes the       |
 | golfer gets on each hole and the pick-up number (the most a hole can     |
 | count towards the handicap), and empty rows to write the round in.       |
 |                                                                          |
 | Public Methods:                                                          |
 | the Report methods; tee(), index()                                       |
 +--------------------------------------------------------------------------+
 */
public final class MyScorecardReport extends Report
{

    public static final String NAME = "My Scorecard";
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

    /*
     * paintBody(sheet, page, pages): the card and the course box.
     */
    @Override
    protected void paintBody(Sheet sheet, int page, int pages)
    {
        // the card and the course box
    }
}

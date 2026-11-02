package golfanalyst.print;

import golfanalyst.BestRound;
import golfanalyst.Round;
import golfanalyst.Tee;
import java.util.List;

/*
 +--------------------------------------------------------------------------+
 | ScorecardReport                                                          |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | A scorecard on its side: one round hole by hole, with the score marks    |
 | and a box of the round's figures. Two reports use it: A round's          |
 | scorecard (a saved round) and Best Round (the best single round on a     |
 | course and tees, or the composite of the best score on every hole).      |
 |                                                                          |
 | Public Methods:                                                          |
 | forRound(data, round)              - a saved round's scorecard           |
 | forBest(data, choice, composite)   - Best Round                          |
 | the Report methods; round(), best(), composite()                         |
 +--------------------------------------------------------------------------+
 */
public final class ScorecardReport extends Report
{

    public static final String ROUND_NAME = "Scorecard";
    public static final String BEST_NAME = "Best Round";
    private final Round round;
    private final BestRound.Choice best;
    private final boolean composite;
    private final List<Round> roundsUsed;

    /*
     +--------------------------------------------------------------------------+
     | ScorecardReport(data, round, best, composite, roundsUsed)                |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Keeps what to print; forRound and forBest make it.                       |
     |                                                                          |
     | Passed in:  data       - the golfer's data                               |
     |             round      - the round to show, or null for none             |
     |             best       - the Best Round choice, or null for a saved round|
     |             composite  - true for the composite                          |
     |             roundsUsed - the rounds Best Round chose from                |
     | Passed out: the report                                                   |
     +--------------------------------------------------------------------------+
     */
    private ScorecardReport(ReportData data, Round round, BestRound.Choice best, boolean composite, List<Round> roundsUsed)
    {
        super(data);
        this.round = round;
        this.best = best;
        this.composite = composite;
        this.roundsUsed = roundsUsed;
    }

    /*
     * forRound(data, round): the scorecard of one saved round (null for none).
     */
    public static ScorecardReport forRound(ReportData data, Round round)
    {
        return new ScorecardReport(data, round, null, false, List.of());
    }

    /*
     +--------------------------------------------------------------------------+
     | forBest(data, choice, composite)                                         |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Best Round for a course and tees, as the Best Round screen works it      |
     | out: the lowest round, or the composite of the best score on every       |
     | hole. Historical rounds follow Home's box.                               |
     |                                                                          |
     | Passed in:  data      - the golfer's data                                |
     |             choice    - the course and tees (a Best Round choice), or    |
     |                         null                                             |
     |             composite - true for the composite                           |
     | Passed out: the report                                                   |
     +--------------------------------------------------------------------------+
     */
    public static ScorecardReport forBest(ReportData data, BestRound.Choice choice, boolean composite)
    {
        if (choice == null)
        {
            return new ScorecardReport(data, null, null, composite, List.of());
        }
        List<Round> used = BestRound.roundsOf(choice, data.rounds(), data.tees(), data.includeHistorical());
        Round shown;
        if (used.isEmpty())
        {
            shown = null;
        }
        else if (composite)
        {
            shown = BestRound.compositeRound(choice, used);
        }
        else
        {
            shown = BestRound.bestSingle(used);
        }
        return new ScorecardReport(data, shown, choice, composite, used);
    }

    /*
     * round(): the round shown (the composite is an unsaved round), or null.
     */
    public Round round()
    {
        return round;
    }

    /*
     * best(): the Best Round choice, or null for a saved round's scorecard.
     */
    public BestRound.Choice best()
    {
        return best;
    }

    /*
     * composite(): true for Best Round's composite.
     */
    public boolean composite()
    {
        return composite;
    }

    /*
     * tee(): the tees of the round shown, or null.
     */
    Tee tee()
    {
        if (round == null)
        {
            return null;
        }
        return data.tees().get(round.teeId());
    }

    /*
     * name(): "Scorecard" or "Best Round".
     */
    @Override
    public String name()
    {
        if (best != null)
        {
            return BEST_NAME;
        }
        return ROUND_NAME;
    }

    /*
     * landscape(): scorecards are printed on their side.
     */
    @Override
    public boolean landscape()
    {
        return true;
    }

    /*
     * isEmpty(): true when there is no round to show.
     */
    @Override
    public boolean isEmpty()
    {
        return round == null || tee() == null;
    }

    /*
     * describe(): "one page: the scorecard of your round on 2026-10-07" or "one page: your best round on ...".
     */
    @Override
    public String describe()
    {
        if (isEmpty())
        {
            return "nothing";
        }
        if (best == null)
        {
            return "one page: the scorecard of your round on " + round.date();
        }
        if (composite)
        {
            return "one page: your best score on every hole of " + tee().label();
        }
        return "one page: your best round on " + tee().label();
    }

    /*
     * heading(): "Scorecard   -   Sample Links (Blue)" or "Best Round   -   Sample Links (Blue)" (with " (composite)").
     */
    @Override
    protected String heading()
    {
        if (isEmpty())
        {
            return name();
        }
        String words = name() + SEPARATOR + tee().label();
        if (composite)
        {
            words = words + " (composite)";
        }
        return words;
    }

    /*
     +--------------------------------------------------------------------------+
     | subtitle(page)                                                           |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | "Angelo   -   2026-10-07   -   round 00026" (and "   -   historical"),   |
     | or for a composite "Angelo   -   built from your 5 rounds".              |
     |                                                                          |
     | Passed in:  page - which page (always 0)                                 |
     | Passed out: the line                                                     |
     +--------------------------------------------------------------------------+
     */
    @Override
    protected String subtitle(int page)
    {
        if (isEmpty())
        {
            return named("");
        }
        if (composite)
        {
            return named("built from your " + RoundLogReport.countWords(roundsUsed.size()));
        }
        String words = round.date() + SEPARATOR + "round " + String.format("%05d", round.id());
        if (round.historical())
        {
            words = words + SEPARATOR + "historical";
        }
        return named(words);
    }

    /*
     * pageCount(height): a scorecard is always one page.
     */
    @Override
    public int pageCount(double height)
    {
        return 1;
    }

    /*
     * paintBody(sheet, page, pages): the card and the figures box.
     */
    @Override
    protected void paintBody(Sheet sheet, int page, int pages)
    {
        // the card and the figures box
    }
}

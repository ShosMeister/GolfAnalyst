package golfanalyst.print;

import golfanalyst.StatsBoard;

/*
 +--------------------------------------------------------------------------+
 | StatsBoardReport                                                         |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | The Stats Board on paper (on its side): the same columns, rows and       |
 | rounding as the screen, the nine-hole rows in their own section after    |
 | the 18-hole rows, and the best figure of each column shaded where the    |
 | screen shows gold.                                                       |
 |                                                                          |
 | Public Methods:                                                          |
 | the Report methods; board() - the figures                                |
 +--------------------------------------------------------------------------+
 */
public final class StatsBoardReport extends Report
{

    public static final String NAME = "Stats Board";
    private final boolean includeHistorical;
    private final StatsBoard.Board board;

    /*
     * StatsBoardReport(data, includeHistorical): the board of the data, with or without historical rounds.
     */
    public StatsBoardReport(ReportData data, boolean includeHistorical)
    {
        super(data);
        this.includeHistorical = includeHistorical;
        this.board = StatsBoard.build(data.rounds(), data.tees(), includeHistorical);
    }

    /*
     * board(): the figures printed.
     */
    public StatsBoard.Board board()
    {
        return board;
    }

    /*
     * name(): "Stats Board".
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
     * isEmpty(): true when there are no rows (no rounds).
     */
    @Override
    public boolean isEmpty()
    {
        return board.eighteen().isEmpty() && board.nine().isEmpty();
    }

    /*
     * describe(): "the Stats Board: 12 rows".
     */
    @Override
    public String describe()
    {
        int rows = board.eighteen().size() + board.nine().size();
        if (rows == 1)
        {
            return "the Stats Board: 1 row";
        }
        return "the Stats Board: " + rows + " rows";
    }

    /*
     * heading(): "Stats Board".
     */
    @Override
    protected String heading()
    {
        return NAME;
    }

    /*
     * subtitle(page): the name and whether historical rounds are included.
     */
    @Override
    protected String subtitle(int page)
    {
        if (includeHistorical)
        {
            return named("historical rounds included");
        }
        return named("historical rounds not included");
    }

    /*
     * pageCount(height): how many pages the rows take.
     */
    @Override
    public int pageCount(double height)
    {
        return 1;
    }

    /*
     * paintBody(sheet, page, pages): the rows on this page.
     */
    @Override
    protected void paintBody(Sheet sheet, int page, int pages)
    {
        // the table
    }
}

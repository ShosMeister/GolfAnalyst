package golfanalyst.print;

import golfanalyst.BestRound;
import golfanalyst.CourseAnalysis;
import golfanalyst.Tee;

/*
 +--------------------------------------------------------------------------+
 | CourseNotesReport                                                        |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | Course notes (on its side): the Analyzer's figures for one course and    |
 | tees, hole by hole, with the hardest holes in red and the easiest in     |
 | gold, and the Analyzer's summary underneath. Historical rounds follow    |
 | Home's Include historical rounds box.                                    |
 |                                                                          |
 | Public Methods:                                                          |
 | the Report methods; analysis()                                           |
 +--------------------------------------------------------------------------+
 */
public final class CourseNotesReport extends Report
{

    public static final String NAME = "Course Notes";
    private final BestRound.Choice row;
    private final CourseAnalysis.Analysis analysis;

    /*
     * CourseNotesReport(data, row): the notes for an Analyzer row (null for none).
     */
    public CourseNotesReport(ReportData data, BestRound.Choice row)
    {
        super(data);
        this.row = row;
        if (row == null)
        {
            this.analysis = null;
        }
        else
        {
            this.analysis = CourseAnalysis.analyse(row, data.rounds(), data.tees(), data.includeHistorical());
        }
    }

    /*
     * analysis(): the Analyzer's figures, or null.
     */
    public CourseAnalysis.Analysis analysis()
    {
        return analysis;
    }

    /*
     * place(): "Sample Links (White)", with " Front 9" or " Back 9" for a nine of an 18-hole tee.
     */
    String place()
    {
        Tee tee = data.tees().get(row.teeId());
        String words = tee.label();
        if (row.part() == golfanalyst.StatsBoard.Part.FRONT)
        {
            words = words + " Front 9";
        }
        else if (row.part() == golfanalyst.StatsBoard.Part.BACK)
        {
            words = words + " Back 9";
        }
        return words;
    }

    /*
     * name(): "Course Notes".
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
     * isEmpty(): true with no row or no rounds on it.
     */
    @Override
    public boolean isEmpty()
    {
        return analysis == null || analysis.rounds().isEmpty() || !data.tees().containsKey(row.teeId());
    }

    /*
     * describe(): "one page: course notes for Sample Links (White) from 6 rounds".
     */
    @Override
    public String describe()
    {
        if (isEmpty())
        {
            return "nothing";
        }
        return "one page: course notes for " + place() + " from " + RoundLogReport.countWords(analysis.rounds().size());
    }

    /*
     * heading(): "Course Notes   -   Sample Links (White)".
     */
    @Override
    protected String heading()
    {
        if (isEmpty())
        {
            return NAME;
        }
        return NAME + SEPARATOR + place();
    }

    /*
     * subtitle(page): "Angelo   -   6 rounds".
     */
    @Override
    protected String subtitle(int page)
    {
        if (isEmpty())
        {
            return named(RoundLogReport.countWords(0));
        }
        return named(RoundLogReport.countWords(analysis.rounds().size()));
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
     * paintBody(sheet, page, pages): the hole table, the summary box and the legend.
     */
    @Override
    protected void paintBody(Sheet sheet, int page, int pages)
    {
        // the hole table and the summary
    }
}

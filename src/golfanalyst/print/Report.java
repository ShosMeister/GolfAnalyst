package golfanalyst.print;

import java.awt.Font;
import java.awt.Graphics2D;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/*
 +--------------------------------------------------------------------------+
 | Report                                                                   |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | What every printed report has in common, and the page furniture they     |
 | all share. A report is drawn by code into a Graphics2D in design units   |
 | (see Sheet), never through a Swing window, so the printer, the small     |
 | picture in the Print window and the self-test draw exactly the same      |
 | page. The width is fixed (850 upright, 1100 on its side); the height     |
 | comes from the paper, and decides how many table rows fit on a page.     |
 |                                                                          |
 | The furniture on every page: "GOLF ANALYST" bold 13 dark brown at        |
 | (60,44); "Printed November 1, 2026" grey 13 ending 60 from the right;    |
 | the title in Serif bold 32 dark brown at y 66; the subtitle 16 in ink    |
 | at y 112; a dark brown rule 4 high across the margins at y 142. At the   |
 | foot a (190,180,165) rule 2 high at height - 66; under it, grey 11, the  |
 | note on the left ("SAMPLE DATA - not real rounds" or nothing) and        |
 | "Golf Analyst v4.00   Page 1 of 2" on the right.                         |
 |                                                                          |
 | A report fills in heading(), subtitle(page) and paintBody(...); the      |
 | rest is done here.                                                       |
 |                                                                          |
 | Public Methods:                                                          |
 | name()         - the report's name ("Round Log"), for the print job      |
 | landscape()    - true for a page on its side                             |
 | width()        - 1100 on its side, 850 upright                           |
 | isEmpty()      - true when there is nothing to print                     |
 | describe()     - what will be printed, for the Print window's note       |
 | pageCount(height) - pages at this page height                            |
 | paint(graphics, page, height) - draws one page (0 is the first)          |
 | longDate(day)  - "November 1, 2026"                                      |
 +--------------------------------------------------------------------------+
 */
public abstract class Report
{

    public static final double TOP_LINE = 44;
    public static final double TITLE_TOP = 66;
    public static final double SUBTITLE_TOP = 112;
    public static final double HEAD_RULE_TOP = 142;
    public static final double HEAD_RULE = 4;
    public static final double FOOT_RULE_UP = 66;
    public static final double FOOT_RULE = 2;
    public static final double FOOT_TEXT_GAP = 10;
    public static final String SEPARATOR = "   -   ";
    private static final DateTimeFormatter LONG_DATE = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.US);
    protected final ReportData data;

    /*
     * Report(data): a report drawn from this data.
     */
    protected Report(ReportData data)
    {
        this.data = data;
    }

    /*
     * name(): the report's name, for example "Round Log" (the print job and the Print window use it).
     */
    public abstract String name();

    /*
     * landscape(): true for a page on its side (every report but the Handicap Report).
     */
    public abstract boolean landscape();

    /*
     * isEmpty(): true when there is nothing to print (the Print button is then off).
     */
    public abstract boolean isEmpty();

    /*
     * describe(): what will be printed, after "This will print " in the Print window: "15 rounds", "one page: ...".
     */
    public abstract String describe();

    /*
     * heading(): the big title on the page, for example "Scorecard   -   Sample Links (Blue)".
     */
    protected abstract String heading();

    /*
     * subtitle(page): the line under the title on that page (0 is the first).
     */
    protected abstract String subtitle(int page);

    /*
     * pageCount(height): how many pages the report takes on pages this high (at least 1).
     */
    public abstract int pageCount(double height);

    /*
     * paintBody(sheet, page, pages): draws everything between the head rule and the foot rule of one page.
     */
    protected abstract void paintBody(Sheet sheet, int page, int pages);

    /*
     * width(): the page width in design units: 1100 on its side, 850 upright.
     */
    public double width()
    {
        if (landscape())
        {
            return Sheet.LETTER_LONG;
        }
        return Sheet.LETTER_SHORT;
    }

    /*
     * letterHeight(): the page height on Letter paper with no printer margins: 850 on its side, 1100 upright.
     */
    public double letterHeight()
    {
        if (landscape())
        {
            return Sheet.LETTER_SHORT;
        }
        return Sheet.LETTER_LONG;
    }

    /*
     * data(): what the report is drawn from.
     */
    public ReportData data()
    {
        return data;
    }

    /*
     +--------------------------------------------------------------------------+
     | paint(graphics, page, height)                                            |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Draws one page: the white paper, the furniture and the report's body.    |
     |                                                                          |
     | Passed in:  graphics - where to draw, already in design units            |
     |             page     - which page (0 is the first)                       |
     |             height   - the page height in design units                   |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    public void paint(Graphics2D graphics, int page, double height)
    {
        Sheet sheet = new Sheet(graphics, width(), height);
        sheet.fill(0, 0, width(), height, java.awt.Color.WHITE);
        int pages = pageCount(height);
        paintFurniture(sheet, page, pages);
        paintBody(sheet, page, pages);
    }

    /*
     * paintFurniture(sheet, page, pages): the head and foot of the page (see the class comment).
     */
    private void paintFurniture(Sheet sheet, int page, int pages)
    {
        double right = sheet.width() - Sheet.MARGIN;
        double across = right - Sheet.MARGIN;
        sheet.text("GOLF ANALYST", Sheet.MARGIN, TOP_LINE, Sheet.sans(Font.BOLD, 13), Sheet.BROWN);
        sheet.textRight("Printed " + longDate(data.today()), right, TOP_LINE, Sheet.sans(Font.PLAIN, 13), Sheet.GREY);
        Font titleFont = Sheet.serif(Font.BOLD, 32);
        sheet.text(Sheet.fit(heading(), titleFont, across), Sheet.MARGIN, TITLE_TOP, titleFont, Sheet.BROWN);
        Font subtitleFont = Sheet.sans(Font.PLAIN, 16);
        sheet.text(Sheet.fit(subtitle(page), subtitleFont, across), Sheet.MARGIN, SUBTITLE_TOP, subtitleFont, Sheet.INK);
        sheet.rule(Sheet.MARGIN, HEAD_RULE_TOP, across, HEAD_RULE, Sheet.BROWN);

        double footRule = sheet.height() - FOOT_RULE_UP;
        sheet.rule(Sheet.MARGIN, footRule, across, FOOT_RULE, Sheet.FOOT_RULE);
        Font footFont = Sheet.sans(Font.PLAIN, 11);
        double footTop = footRule + FOOT_RULE + FOOT_TEXT_GAP;
        sheet.text(data.note(), Sheet.MARGIN, footTop, footFont, Sheet.GREY);
        sheet.textRight("Golf Analyst v" + data.version() + "   Page " + (page + 1) + " of " + pages, right, footTop, footFont,
                Sheet.GREY);
    }

    /*
     * bodyTop(): where the body starts: 28 units under the head rule.
     */
    protected static double bodyTop()
    {
        return HEAD_RULE_TOP + HEAD_RULE + 24;
    }

    /*
     * bodyBottom(height): the lowest the body may reach: 20 units above the foot rule.
     */
    protected static double bodyBottom(double height)
    {
        return height - FOOT_RULE_UP - 20;
    }

    /*
     * named(rest): the golfer's name, the separator and the rest ("Angelo   -   Sample Links (White): ..."); just the rest with no name.
     */
    protected String named(String rest)
    {
        if (data.golfer().isBlank())
        {
            return rest;
        }
        return data.golfer().trim() + SEPARATOR + rest;
    }

    /*
     * longDate(day): a day written out, "November 1, 2026".
     */
    public static String longDate(LocalDate day)
    {
        return day.format(LONG_DATE);
    }
}

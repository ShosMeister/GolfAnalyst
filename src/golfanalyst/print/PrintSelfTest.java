package golfanalyst.print;

import golfanalyst.DataStore;
import golfanalyst.Period;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.function.BiConsumer;

/*
 +--------------------------------------------------------------------------+
 | PrintSelfTest                                                            |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | The printing checks, run with the engine's self-test (Main --selftest),  |
 | which passes in its own check(name, ok). Everything here draws into      |
 | pictures in memory: no window and no printer are needed.                 |
 |                                                                          |
 | Public Methods:                                                          |
 | run(check) - runs every printing check                                   |
 +--------------------------------------------------------------------------+
 */
public final class PrintSelfTest
{

    static final LocalDate TODAY = LocalDate.of(2026, 11, 1);
    static final double A4_HEIGHT = 1202;
    private static BiConsumer<String, Boolean> check;

    private PrintSelfTest()
    {
    }

    /*
     * run(checker): runs every printing check, reporting each through checker (throws IOException if the sample data cannot be read).
     */
    public static void run(BiConsumer<String, Boolean> checker) throws IOException
    {
        check = checker;
        checkPeriods();
        checkTextFitting();
        checkFurniture();
        checkThumbnail();
        ReportSelfTest.run();
    }

    /*
     * check(name, ok): records one result through the engine's self-test.
     */
    static void check(String name, boolean ok)
    {
        check.accept(name, ok);
    }

    /*
     * sample(): the report data of the sample folder, printed on November 1, 2026.
     */
    static ReportData sample() throws IOException
    {
        return ReportData.of(new DataStore(Path.of(DataStore.SAMPLE_FOLDER)), TODAY, "4.00");
    }

    /*
     * days(period, today, from, to): whether a period runs from..to on that day (null for no end).
     */
    private static boolean days(Period period, LocalDate today, String from, String to)
    {
        LocalDate first = period.from(today);
        LocalDate last = period.to(today);
        boolean fromRight = (from == null && first == null) || (from != null && LocalDate.parse(from).equals(first));
        boolean toRight = (to == null && last == null) || (to != null && LocalDate.parse(to).equals(last));
        return fromRight && toRight;
    }

    /*
     +--------------------------------------------------------------------------+
     | checkPeriods()                                                           |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Every Period choice with a fixed today, worked out by hand: an ordinary  |
     | day, the end of a long month (3 months before May 31 is February 28,     |
     | or 29 in a leap year), March 1 in a leap year, February 29 itself,       |
     | January (last month and last year fall in the year before), both ends    |
     | included, and no "Year to date".                                         |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    private static void checkPeriods()
    {
        LocalDate today = LocalDate.of(2026, 11, 1);
        check("period: All time and Custom dates have no ends", days(Period.ALL_TIME, today, null, null)
                && days(Period.CUSTOM, today, null, null));
        check("period: Last 30 days is the 30 days ending today", days(Period.LAST_30_DAYS, today, "2026-10-03", "2026-11-01"));
        check("period: This month is the 1st to today", days(Period.THIS_MONTH, today, "2026-11-01", "2026-11-01"));
        check("period: Last month is the whole previous month", days(Period.LAST_MONTH, today, "2026-10-01", "2026-10-31"));
        check("period: Last 3, 6 and 12 months start on the same date", days(Period.LAST_3_MONTHS, today, "2026-08-01", "2026-11-01")
                && days(Period.LAST_6_MONTHS, today, "2026-05-01", "2026-11-01")
                && days(Period.LAST_12_MONTHS, today, "2025-11-01", "2026-11-01"));
        check("period: This year and Last year", days(Period.THIS_YEAR, today, "2026-01-01", "2026-11-01")
                && days(Period.LAST_YEAR, today, "2025-01-01", "2025-12-31"));

        LocalDate mayEnd = LocalDate.of(2026, 5, 31);
        check("period: at a month end, 3 months back is the last day of February", days(Period.LAST_3_MONTHS, mayEnd, "2026-02-28", "2026-05-31")
                && days(Period.LAST_MONTH, mayEnd, "2026-04-01", "2026-04-30") && days(Period.LAST_30_DAYS, mayEnd, "2026-05-02", "2026-05-31")
                && days(Period.LAST_6_MONTHS, mayEnd, "2025-11-30", "2026-05-31"));
        check("period: in a leap year February has 29 days", days(Period.LAST_3_MONTHS, LocalDate.of(2028, 5, 31), "2028-02-29", "2028-05-31")
                && days(Period.LAST_MONTH, LocalDate.of(2028, 3, 1), "2028-02-01", "2028-02-29")
                && days(Period.LAST_30_DAYS, LocalDate.of(2028, 3, 1), "2028-02-01", "2028-03-01"));
        check("period: from February 29 a year back is February 28", days(Period.LAST_12_MONTHS, LocalDate.of(2028, 2, 29), "2027-02-28", "2028-02-29")
                && days(Period.THIS_MONTH, LocalDate.of(2028, 2, 29), "2028-02-01", "2028-02-29"));
        LocalDate january = LocalDate.of(2027, 1, 15);
        check("period: in January last month and last year are in the year before", days(Period.LAST_MONTH, january, "2026-12-01", "2026-12-31")
                && days(Period.LAST_YEAR, january, "2026-01-01", "2026-12-31") && days(Period.THIS_YEAR, january, "2027-01-01", "2027-01-15"));
        check("period: both ends are included", Period.LAST_MONTH.contains(LocalDate.of(2026, 10, 1), today)
                && Period.LAST_MONTH.contains(LocalDate.of(2026, 10, 31), today) && !Period.LAST_MONTH.contains(LocalDate.of(2026, 11, 1), today)
                && !Period.LAST_MONTH.contains(LocalDate.of(2026, 9, 30), today) && Period.ALL_TIME.contains(LocalDate.of(1991, 6, 1), today));
        boolean noYearToDate = true;
        for (Period period : Period.values())
        {
            noYearToDate = noYearToDate && !period.label().toLowerCase().contains("year to date");
        }
        check("period: ten choices, in order, with no Year to date", Period.values().length == 10 && noYearToDate
                && Period.values()[0] == Period.ALL_TIME && Period.values()[9] == Period.CUSTOM
                && Period.CUSTOM.label().equals("Custom dates..."));
    }

    /*
     * checkTextFitting(): text cut short with "..." to fit, and paragraphs wrapped within their width.
     */
    private static void checkTextFitting()
    {
        Font font = Sheet.sans(Font.PLAIN, 13);
        String longName = "The Very Long Name Golf and Country Club (Championship)";
        String cut = Sheet.fit(longName, font, 150);
        check("printing: a text too wide for its column is cut short with ...", cut.endsWith(Sheet.ELLIPSIS)
                && Sheet.textWidth(cut, font) <= 150 && cut.length() < longName.length()
                && Sheet.fit("Short", font, 150).equals("Short"));
        java.util.List<String> lines = Sheet.wrap("Differential = (113 / slope) x (adjusted score - course rating - playing conditions "
                + "adjustment). Historical rounds are not used.", Sheet.sans(Font.PLAIN, 12), 300);
        boolean narrow = lines.size() > 1;
        for (String line : lines)
        {
            narrow = narrow && Sheet.textWidth(line, Sheet.sans(Font.PLAIN, 12)) <= 300;
        }
        check("printing: a paragraph wraps within its width", narrow);
    }

    /*
     +--------------------------------------------------------------------------+
     | Furniture                                                                |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | A report with nothing but the page furniture, for checking it.           |
     +--------------------------------------------------------------------------+
     */
    private static final class Furniture extends Report
    {
        private final boolean sideways;

        /*
         * Furniture(data, sideways): a page with only the head and foot, upright or on its side.
         */
        Furniture(ReportData data, boolean sideways)
        {
            super(data);
            this.sideways = sideways;
        }

        /*
         * name(), landscape(), isEmpty(), describe(), heading(), subtitle(page), pageCount(height): a two-page test report.
         */
        @Override
        public String name()
        {
            return "Furniture";
        }

        @Override
        public boolean landscape()
        {
            return sideways;
        }

        @Override
        public boolean isEmpty()
        {
            return false;
        }

        @Override
        public String describe()
        {
            return "a test page";
        }

        @Override
        protected String heading()
        {
            return "Test";
        }

        @Override
        protected String subtitle(int page)
        {
            return named("a page");
        }

        @Override
        public int pageCount(double height)
        {
            return 2;
        }

        /*
         * paintBody(sheet, page, pages): nothing; only the furniture is drawn.
         */
        @Override
        protected void paintBody(Sheet sheet, int page, int pages)
        {
            // nothing but the furniture
        }
    }

    /*
     * picture(report, page, height): one page drawn at full size (one pixel per design unit).
     */
    static BufferedImage picture(Report report, int page, double height)
    {
        BufferedImage image = new BufferedImage((int) report.width(), (int) Math.ceil(height), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try
        {
            report.paint(graphics, page, height);
        }
        finally
        {
            graphics.dispose();
        }
        return image;
    }

    /*
     * count(image, x1, y1, x2, y2, colour): how many pixels in the area are exactly that colour.
     */
    static int count(BufferedImage image, int x1, int y1, int x2, int y2, Color colour)
    {
        int found = 0;
        for (int y = Math.max(0, y1); y < Math.min(image.getHeight(), y2); y = y + 1)
        {
            for (int x = Math.max(0, x1); x < Math.min(image.getWidth(), x2); x = x + 1)
            {
                if ((image.getRGB(x, y) & 0xFFFFFF) == (colour.getRGB() & 0xFFFFFF))
                {
                    found = found + 1;
                }
            }
        }
        return found;
    }

    /*
     * inked(image, x1, y1, x2, y2): how many pixels in the area are drawn on (darker than the paper, not the magenta background).
     */
    static int inked(BufferedImage image, int x1, int y1, int x2, int y2)
    {
        int found = 0;
        for (int y = Math.max(0, y1); y < Math.min(image.getHeight(), y2); y = y + 1)
        {
            for (int x = Math.max(0, x1); x < Math.min(image.getWidth(), x2); x = x + 1)
            {
                Color pixel = new Color(image.getRGB(x, y));
                boolean magenta = pixel.getRed() > 200 && pixel.getBlue() > 200 && pixel.getGreen() < 60;
                if (!magenta && pixel.getRed() + pixel.getGreen() + pixel.getBlue() < 650)
                {
                    found = found + 1;
                }
            }
        }
        return found;
    }

    /*
     * near(image, x1, y1, x2, y2, colour): how many pixels in the area are close to that colour (smoothed edges allowed).
     */
    static int near(BufferedImage image, int x1, int y1, int x2, int y2, Color colour)
    {
        int found = 0;
        for (int y = Math.max(0, y1); y < Math.min(image.getHeight(), y2); y = y + 1)
        {
            for (int x = Math.max(0, x1); x < Math.min(image.getWidth(), x2); x = x + 1)
            {
                Color pixel = new Color(image.getRGB(x, y));
                int distance = Math.abs(pixel.getRed() - colour.getRed()) + Math.abs(pixel.getGreen() - colour.getGreen())
                        + Math.abs(pixel.getBlue() - colour.getBlue());
                if (distance < 60)
                {
                    found = found + 1;
                }
            }
        }
        return found;
    }

    /*
     +--------------------------------------------------------------------------+
     | checkFurniture()                                                         |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The page furniture drawn into a picture on Letter (upright and on its    |
     | side) and A4 (taller, so the foot moves down): the dark brown head rule  |
     | at y 142 only between the margins, the light foot rule at height - 66,   |
     | the dark brown "GOLF ANALYST", the grey printed date ending at the       |
     | right margin, the title, and the sample note only on the sample data.    |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing (throws IOException if the sample cannot be read)    |
     +--------------------------------------------------------------------------+
     */
    private static void checkFurniture() throws IOException
    {
        ReportData sampleData = sample();
        double[] heights = {Sheet.LETTER_LONG, A4_HEIGHT};
        String[] papers = {"Letter", "A4"};
        for (int lp = 0; lp < heights.length; lp = lp + 1)
        {
            Furniture upright = new Furniture(sampleData, false);
            BufferedImage page = picture(upright, 0, heights[lp]);
            int bottom = (int) heights[lp];
            int footRule = (int) (heights[lp] - Report.FOOT_RULE_UP);
            check("printing, " + papers[lp] + ": the head rule is dark brown, 4 high, between the margins",
                    count(page, 60, 142, 790, 146, Sheet.BROWN) == 730 * 4 && count(page, 0, 142, 60, 146, Sheet.BROWN) == 0
                    && count(page, 790, 142, 850, 146, Sheet.BROWN) == 0 && count(page, 60, 147, 790, 150, Sheet.BROWN) == 0);
            check("printing, " + papers[lp] + ": the foot rule is 2 high at the page height less 66",
                    count(page, 60, footRule, 790, footRule + 2, Sheet.FOOT_RULE) == 730 * 2
                    && count(page, 60, footRule - 3, 790, footRule, Sheet.FOOT_RULE) == 0);
            check("printing, " + papers[lp] + ": GOLF ANALYST, the title and the printed date are at the top",
                    near(page, 60, 44, 180, 60, Sheet.BROWN) > 40 && near(page, 60, 66, 160, 105, Sheet.BROWN) > 100
                    && near(page, 640, 44, 790, 62, Sheet.GREY) > 30 && near(page, 790, 40, 850, 62, Sheet.GREY) == 0);
            check("printing, " + papers[lp] + ": the sample note and the page number are under the foot rule",
                    near(page, 60, footRule + 8, 260, bottom - 30, Sheet.GREY) > 30
                    && near(page, 640, footRule + 8, 790, bottom - 30, Sheet.GREY) > 30);
        }
        ReportData ownData = new ReportData(sampleData.rounds(), sampleData.tees(), sampleData.handicap(), "Pat", false, false, TODAY, "4.00");
        BufferedImage own = picture(new Furniture(ownData, true), 1, Sheet.LETTER_SHORT);
        int footRule = (int) (Sheet.LETTER_SHORT - Report.FOOT_RULE_UP);
        check("printing: no sample note on the golfer's own data, and a page on its side is 1100 wide", own.getWidth() == 1100
                && near(own, 60, footRule + 8, 400, 830, Sheet.GREY) == 0 && near(own, 880, footRule + 8, 1040, 830, Sheet.GREY) > 30
                && count(own, 60, 142, 1040, 146, Sheet.BROWN) == 980 * 4);
        check("printing: the printed date is written out", Report.longDate(TODAY).equals("November 1, 2026")
                && ReportData.SAMPLE_NOTE.equals(sampleData.note()) && ownData.note().isEmpty());
    }

    /*
     +--------------------------------------------------------------------------+
     | checkThumbnail()                                                         |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The small picture of the page drawn into a 188 x 150 picture with no     |
     | window: a page on its side fills the width and is centred top to bottom, |
     | with its grey outline and the dark brown head rule showing; an upright   |
     | page fills the height; nothing to print gives a blank page with grey     |
     | words and no brown.                                                      |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: nothing (throws IOException if the sample cannot be read)    |
     +--------------------------------------------------------------------------+
     */
    private static void checkThumbnail() throws IOException
    {
        ReportData sampleData = sample();
        BufferedImage box = new BufferedImage(188, 150, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = box.createGraphics();
        graphics.setColor(Color.MAGENTA);
        graphics.fillRect(0, 0, 188, 150);
        Thumbnail.draw(graphics, new Furniture(sampleData, true), 0, 0, 188, 150);
        graphics.dispose();
        java.awt.geom.Rectangle2D.Double area = Thumbnail.pageArea(new Furniture(sampleData, true), 188, 150);
        check("thumbnail: a page on its side fills the width, centred top to bottom", Math.abs(area.width - 186) < 0.01
                && Math.abs(area.y - (150 - area.height) / 2) < 0.01 && Math.abs(area.height - 186 * 850.0 / 1100) < 0.01);
        int top = (int) Math.round(area.y);
        check("thumbnail: the page is drawn white with its grey outline and the brown head rule", count(box, 0, 0, 188, top - 1, Color.MAGENTA) > 0
                && inked(box, 2, top + 2, 186, top + 30) > 20 && count(box, 1, top, 187, top + 1, Thumbnail.OUTLINE) > 100
                && count(box, 20, top + 60, 160, top + 100, Color.WHITE) > 1000);

        java.awt.geom.Rectangle2D.Double upright = Thumbnail.pageArea(new Furniture(sampleData, false), 188, 150);
        check("thumbnail: an upright page fills the height", Math.abs(upright.height - 148) < 0.01 && upright.width < 120);

        BufferedImage blank = new BufferedImage(188, 150, BufferedImage.TYPE_INT_RGB);
        Graphics2D blankGraphics = blank.createGraphics();
        Thumbnail.draw(blankGraphics, null, 0, 0, 188, 150);
        blankGraphics.dispose();
        check("thumbnail: nothing to print is a blank page with grey words", near(blank, 0, 0, 188, 150, Sheet.BROWN) == 0
                && near(blank, 30, 60, 160, 90, Sheet.GREY) > 10 && Thumbnail.NOTHING.equals("Nothing to print yet"));
    }
}

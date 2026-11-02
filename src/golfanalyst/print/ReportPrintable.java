package golfanalyst.print;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.DialogTypeSelection;
import javax.print.attribute.standard.JobName;
import javax.print.attribute.standard.OrientationRequested;

/*
 +--------------------------------------------------------------------------+
 | ReportPrintable                                                          |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | Hands a report to Java's standard printing (PrinterJob). Each page is    |
 | scaled the same way across and down so the report's design width just    |
 | fills the printer's printable width; the printable height, in design     |
 | units at that scale, is the page height, which decides how many rows     |
 | fit. So Letter and A4 both work, and so does "Microsoft Print to PDF".   |
 |                                                                          |
 | Public Methods:                                                          |
 | print(graphics, format, page) - draws one page for the printer           |
 | pageHeight(report, format)    - the page height in design units          |
 | send(report)                  - the system print dialog, then printing   |
 +--------------------------------------------------------------------------+
 */
public final class ReportPrintable implements Printable
{

    private final Report report;

    /*
     * ReportPrintable(report): the report to print.
     */
    public ReportPrintable(Report report)
    {
        this.report = report;
    }

    /*
     * scale(report, format): design units to printer units: the design width over the printable width.
     */
    private static double scale(Report report, PageFormat format)
    {
        return format.getImageableWidth() / report.width();
    }

    /*
     * pageHeight(report, format): the page height in design units on this paper (850 on Letter on its side with no margins).
     */
    public static double pageHeight(Report report, PageFormat format)
    {
        return format.getImageableHeight() / scale(report, format);
    }

    /*
     +--------------------------------------------------------------------------+
     | print(graphics, format, page)                                            |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Called by the printing system for each page in turn: moves to the        |
     | printable area, scales to design units and draws the page.               |
     |                                                                          |
     | Passed in:  graphics - the printer's drawing surface                     |
     |             format   - the paper and its printable area                  |
     |             page     - which page (0 is the first)                       |
     | Passed out: PAGE_EXISTS, or NO_SUCH_PAGE after the last page             |
     +--------------------------------------------------------------------------+
     */
    @Override
    public int print(Graphics graphics, PageFormat format, int page)
    {
        double height = pageHeight(report, format);
        if (page >= report.pageCount(height))
        {
            return NO_SUCH_PAGE;
        }
        Graphics2D drawing = (Graphics2D) graphics.create();
        try
        {
            drawing.translate(format.getImageableX(), format.getImageableY());
            double scale = scale(report, format);
            drawing.scale(scale, scale);
            drawing.clipRect(0, 0, (int) Math.ceil(report.width()), (int) Math.ceil(height));
            report.paint(drawing, page, height);
        }
        finally
        {
            drawing.dispose();
        }
        return PAGE_EXISTS;
    }

    /*
     +--------------------------------------------------------------------------+
     | send(report)                                                             |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Shows the system's own print dialog, set to the report's way round       |
     | (upright for the Handicap Report, on its side for the others), and       |
     | prints if the golfer presses Print there.                                |
     |                                                                          |
     | Passed in:  report - what to print                                       |
     | Passed out: true if it was sent, false if the dialog was cancelled       |
     |             (throws PrinterException if the printer refuses it)          |
     +--------------------------------------------------------------------------+
     */
    public static boolean send(Report report) throws PrinterException
    {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("Golf Analyst - " + report.name());
        job.setPrintable(new ReportPrintable(report));
        PrintRequestAttributeSet settings = new HashPrintRequestAttributeSet();
        settings.add(DialogTypeSelection.NATIVE);
        settings.add(new JobName("Golf Analyst - " + report.name(), null));
        if (report.landscape())
        {
            settings.add(OrientationRequested.LANDSCAPE);
        }
        else
        {
            settings.add(OrientationRequested.PORTRAIT);
        }
        if (!job.printDialog(settings))
        {
            return false;
        }
        job.print(settings);
        return true;
    }
}

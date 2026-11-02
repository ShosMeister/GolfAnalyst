package golfanalyst.print;

import golfanalyst.DataStore;
import golfanalyst.Round;
import golfanalyst.Tee;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import javax.imageio.ImageIO;

/*
 +--------------------------------------------------------------------------+
 | Samples                                                                  |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | A tool, not part of the program: draws each report from the sample data  |
 | with the reports' own drawing code into PNG pictures (one pixel per      |
 | design unit, Letter paper), so they can be put beside the design         |
 | pictures in docs\mockups. Run from the project folder after build.bat:   |
 |   java -cp out golfanalyst.print.Samples docs\mockups                    |
 |                                                                          |
 | Public Methods:                                                          |
 | main(args) - args[0] is the folder to write into                         |
 +--------------------------------------------------------------------------+
 */
public final class Samples
{

    private Samples()
    {
    }

    /*
     * main(args): draws the pictures into the folder named in args[0] (docs\mockups if none is given).
     */
    public static void main(String[] args) throws IOException
    {
        Path folder = Path.of("docs", "mockups");
        if (args.length > 0)
        {
            folder = Path.of(args[0]);
        }
        Files.createDirectories(folder);
        ReportData data = ReportData.of(new DataStore(Path.of(DataStore.SAMPLE_FOLDER)), LocalDate.now(), "4.00");
        write(new HandicapReport(data), 0, folder.resolve("print_actual_handicap_report.png"));

        Round newest = data.rounds().get(data.rounds().size() - 1);
        Integer whiteTees = null;
        for (Tee tee : data.tees().values())
        {
            if (tee.courseName().equals("Sample Links") && tee.teeName().equals("White"))
            {
                whiteTees = tee.id();
            }
        }
        write(new RoundLogReport(data, whiteTees, null, null, data.includeHistorical()), 0, folder.resolve("print_actual_round_log.png"));
        RoundLogReport everything = new RoundLogReport(data, null, null, null, true);
        for (int page = 0; page < everything.pageCount(everything.letterHeight()); page = page + 1)
        {
            write(everything, page, folder.resolve("print_actual_round_log_multipage_" + (page + 1) + ".png"));
        }

        Round shown = newest;
        for (Round round : data.rounds())
        {
            if (round.id() == 26)
            {
                shown = round;
            }
        }
        write(ScorecardReport.forRound(data, shown), 0, folder.resolve("print_actual_scorecard.png"));
        System.out.println("Pictures written to " + folder.toAbsolutePath());
    }

    /*
     * write(report, page, file): draws one page on Letter paper into a PNG, one pixel per design unit.
     */
    static void write(Report report, int page, Path file) throws IOException
    {
        double height = report.letterHeight();
        if (page >= report.pageCount(height))
        {
            System.out.println("No page " + (page + 1) + " for " + file.getFileName());
            return;
        }
        BufferedImage image = new BufferedImage((int) report.width(), (int) height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try
        {
            report.paint(graphics, page, height);
        }
        finally
        {
            graphics.dispose();
        }
        ImageIO.write(image, "png", file.toFile());
    }
}

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
 | pictures in docs\mockups. The sample data has no golfer's name, so the   |
 | test name "Angelo" is used, in memory only: no settings file is written. |
 | print_actual_round_log_historical.png treats the two oldest White rounds |
 | at Sample Links as historical, again only in memory, so the H shows.     |
 | Run from the project folder after build.bat:                             |
 |   java -cp out golfanalyst.print.Samples docs\mockups                    |
 |                                                                          |
 | Public Methods:                                                          |
 | main(args) - args[0] is the folder to write into                         |
 +--------------------------------------------------------------------------+
 */
public final class Samples
{

    static final String TEST_NAME = "Angelo";

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
        ReportData read = ReportData.of(new DataStore(Path.of(DataStore.SAMPLE_FOLDER)), LocalDate.now(), "4.00");
        ReportData data = new ReportData(read.rounds(), read.tees(), read.handicap(), TEST_NAME, read.includeHistorical(), read.sample(),
                read.today(), read.version());
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
        ReportData historical = withHistorical(data, whiteTees, 2);
        write(new RoundLogReport(historical, whiteTees, null, null, true), 0, folder.resolve("print_actual_round_log_historical.png"));
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
        golfanalyst.BestRound.Choice latest = golfanalyst.BestRound.latestChoice(data.rounds(), data.tees(), data.includeHistorical());
        write(ScorecardReport.forBest(data, latest, false), 0, folder.resolve("print_actual_best_round.png"));
        write(new StatsBoardReport(data, data.includeHistorical()), 0, folder.resolve("print_actual_stats_board.png"));
        write(new MyScorecardReport(data, data.tees().get(newest.teeId())), 0, folder.resolve("print_actual_my_scorecard.png"));
        golfanalyst.BestRound.Choice busiest = null;
        int most = 0;
        for (golfanalyst.BestRound.Choice row : golfanalyst.CourseAnalysis.rows(data.rounds(), data.tees(), data.includeHistorical()))
        {
            int count = golfanalyst.BestRound.roundsOf(row, data.rounds(), data.tees(), data.includeHistorical()).size();
            if (count > most)
            {
                most = count;
                busiest = row;
            }
        }
        write(new CourseNotesReport(data, busiest), 0, folder.resolve("print_actual_course_notes.png"));
        System.out.println("Pictures written to " + folder.toAbsolutePath());
    }

    /*
     +--------------------------------------------------------------------------+
     | withHistorical(data, teeId, count)                                       |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | A copy of the report data in memory with the oldest rounds on one set    |
     | of tees marked historical and the handicap worked out again without      |
     | them. Nothing is read from or written to a file.                         |
     |                                                                          |
     | Passed in:  data  - the sample's report data                             |
     |             teeId - the tees                                             |
     |             count - how many of their oldest rounds to mark              |
     | Passed out: the copy                                                     |
     +--------------------------------------------------------------------------+
     */
    static ReportData withHistorical(ReportData data, int teeId, int count)
    {
        java.util.List<Round> rounds = new java.util.ArrayList<>();
        int marked = 0;
        for (Round round : data.rounds())
        {
            if (round.teeId() == teeId && marked < count)
            {
                round = new Round(round.id(), round.teeId(), round.date(), round.pcc(), round.startHole(), round.holes(), round.scores(),
                        round.putts(), round.fairway(), round.sand(), true);
                marked = marked + 1;
            }
            rounds.add(round);
        }
        return new ReportData(rounds, data.tees(), golfanalyst.Handicap.compute(rounds, data.tees()), data.golfer(), true, data.sample(),
                data.today(), data.version());
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

package golfanalyst;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/*
 +--------------------------------------------------------------------------+
 | CourseWriter                                                             |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | Adds new tees to courses.tsv, one line per tee. Like RoundWriter, each   |
 | line is written in the column order of the file's own header row, so a   |
 | file whose columns you have rearranged, or that has extra columns of     |
 | your own, stays intact. A column the program does not know is left       |
 | blank. If the file does not exist, or has no header row yet, the         |
 | standard header is written first. Existing lines and comments are never  |
 | touched; the new lines are only added at the end, with the file's own    |
 | line ending style.                                                       |
 |                                                                          |
 | Before anything is written, the whole file as it would become is read    |
 | back with DataLoader's own checks. If a new line would be refused (a     |
 | TeeID that is already used, a par of 7, ...) nothing is written and an   |
 | IOException says why. So a line this class writes always loads.          |
 |                                                                          |
 | Constants:                                                               |
 | STANDARD_HEADER - the header used when a file has none                   |
 |                                                                          |
 | Public Methods:                                                          |
 | append(file, tees)            - adds the tees to the file                |
 | formatLine(tee, headerCells)  - builds one tab-separated line            |
 +--------------------------------------------------------------------------+
 */
public final class CourseWriter
{

    public static final String STANDARD_HEADER =
            "TeeID\tCourse\tCity\tState\tZip\tTee\tHoles\tRating\tSlope\tYardage\tPars\tStrokeIndex"
            + "\tFrontRating\tFrontSlope\tBackRating\tBackSlope";

    private CourseWriter()
    {
    }

    /*
     * joinNumbers(values): turns per-hole numbers into one comma-separated cell.
     * Passed in:  values - one number per hole
     * Passed out: text like "4,3,5,..."
     */
    private static String joinNumbers(int[] values)
    {
        StringBuilder text = new StringBuilder();
        for (int lp = 0; lp < values.length; lp = lp + 1)
        {
            text.append(lp == 0 ? "" : ",").append(values[lp]);
        }
        return text.toString();
    }

    /*
     * ratingText(rating): a rating as written in the file, or "" when there is none (0).
     * Passed in:  rating - the rating, for example 70.4
     * Passed out: text like "70.4"
     */
    private static String ratingText(double rating)
    {
        return rating > 0 ? Double.toString(rating) : "";
    }

    /*
     * wholeText(value): a whole number as written in the file, or "" when there is none (0).
     * Passed in:  value - the number, for example a slope or a yardage
     * Passed out: text like "123"
     */
    private static String wholeText(int value)
    {
        return value > 0 ? Integer.toString(value) : "";
    }

    /*
     * cellText(tee, column): the text of one column of a tee's line.
     * Passed in:  tee - what is being written; column - normalized column name, e.g. "teeid"
     * Passed out: the text for that column, or "" for a column this program does not know
     */
    private static String cellText(Tee tee, String column)
    {
        return switch (column)
        {
            case "teeid" -> Integer.toString(tee.id());
            case "course" -> tee.courseName();
            case "city" -> tee.city();
            case "state" -> tee.state();
            case "zip" -> tee.zip();
            case "tee" -> tee.teeName();
            case "holes" -> Integer.toString(tee.holes());
            case "rating" -> ratingText(tee.rating());
            case "slope" -> wholeText(tee.slope());
            case "yardage" -> wholeText(tee.yardage());
            case "pars" -> joinNumbers(tee.pars());
            case "strokeindex" -> joinNumbers(tee.strokeIndex());
            case "frontrating" -> ratingText(tee.frontRating());
            case "frontslope" -> wholeText(tee.frontSlope());
            case "backrating" -> ratingText(tee.backRating());
            case "backslope" -> wholeText(tee.backSlope());
            default -> "";
        };
    }

    /*
     +--------------------------------------------------------------------------+
     | formatLine(tee, headerCells)                                             |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Builds one line of courses.tsv for a tee, with the columns in the        |
     | order of the header row.                                                 |
     |                                                                          |
     | Passed in:  tee         - the tee to write                               |
     |             headerCells - the header row, split into its cells           |
     | Passed out: the tab-separated line (no line ending)                      |
     +--------------------------------------------------------------------------+
     */
    public static String formatLine(Tee tee, String[] headerCells)
    {
        StringBuilder line = new StringBuilder();
        for (int col = 0; col < headerCells.length; col = col + 1)
        {
            if (col > 0)
            {
                line.append('\t');
            }
            line.append(cellText(tee, DataLoader.normalize(headerCells[col])));
        }
        return line.toString();
    }

    /*
     +--------------------------------------------------------------------------+
     | append(file, tees)                                                       |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Adds tees to the end of courses.tsv, creating the file (with the         |
     | standard header) if it does not exist or has no header. The whole file   |
     | as it would become is checked first with DataLoader.parseCourses; if     |
     | any new line has an error, nothing is written.                           |
     |                                                                          |
     | Passed in:  file - the courses.tsv path                                  |
     |             tees - the new tees, each with its own unused TeeID          |
     | Passed out: nothing (the file is changed); throws IOException if a new   |
     |             line would be refused or the file cannot be written          |
     +--------------------------------------------------------------------------+
     */
    public static void append(Path file, List<Tee> tees) throws IOException
    {
        String existing = Files.exists(file) ? Files.readString(file, StandardCharsets.UTF_8) : "";
        String[] headerCells = RoundWriter.findHeader(existing);
        String newline = existing.contains("\r\n") ? "\r\n" : "\n";
        StringBuilder addition = new StringBuilder();
        if (!existing.isEmpty() && !existing.endsWith("\n"))
        {
            addition.append(newline);
        }
        if (headerCells == null)
        {
            addition.append(STANDARD_HEADER).append(newline);
            headerCells = STANDARD_HEADER.split("\t");
        }
        for (Tee tee : tees)
        {
            addition.append(formatLine(tee, headerCells)).append(newline);
        }

        String clean = existing.startsWith("﻿") ? existing.substring(1) : existing;
        int oldLineCount = clean.isEmpty() ? 0 : clean.split("\\R", -1).length - (clean.endsWith("\n") ? 1 : 0);
        List<String> whole = new ArrayList<>(Arrays.asList((clean + addition).split("\\R")));
        List<Problem> problems = new ArrayList<>();
        DataLoader.parseCourses(whole, problems);
        for (Problem problem : problems)
        {
            if (problem.severity() == Problem.Severity.ERROR && (problem.line() > oldLineCount || problem.line() == 0))
            {
                throw new IOException("the new tee would not load: " + problem.message());
            }
        }

        if (file.getParent() != null)
        {
            Files.createDirectories(file.getParent());
        }
        Files.writeString(file, addition.toString(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
}

package golfanalyst;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/*
 +--------------------------------------------------------------------------+
 | DataLoader                                                               |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | Reads courses.tsv and rounds.tsv from the data folder and turns them     |
 | into Tee and Round objects. The files are plain tab-separated text so    |
 | they can be edited by hand or in a spreadsheet, for example to add a     |
 | season of scores in one go.                                              |
 |                                                                          |
 | File rules:                                                              |
 | - Blank lines and lines starting with # are ignored.                     |
 | - The first other line is the header row. Columns are found by name,     |
 |   in any order, ignoring capitals, spaces and underscores.               |
 | - Per-hole values are comma-separated numbers in a single cell: 18 of    |
 |   them for a full round, 9 for a nine-hole round.                        |
 | - Dates may be written 2026-10-22 or 10/22/2026.                         |
 | - A row that cannot be understood is skipped and reported as an ERROR    |
 |   with its line number. A row that is understandable but looks wrong     |
 |   is kept and reported as a WARNING.                                     |
 |                                                                          |
 | courses.tsv columns:                                                     |
 | TeeID, Course, Tee, Rating, Slope, Pars, StrokeIndex (required)          |
 | City, State, Zip, Yardage (optional)                                     |
 | Holes (optional: 18 or 9, default 18; 9 means a nine-hole course, and    |
 |   then Pars and StrokeIndex have 9 values and Rating and Slope are       |
 |   the nine-hole ratings)                                                 |
 | FrontRating, FrontSlope, BackRating, BackSlope (optional: the nine-hole  |
 |   ratings of an 18-hole tee, needed to post a nine-hole round played     |
 |   on it for handicap purposes)                                           |
 |                                                                          |
 | rounds.tsv columns:                                                      |
 | RoundID, TeeID, Date, Scores, Putts, Fairways, Sand (required)           |
 | PCC (optional, defaults to 0)                                            |
 | Holes (optional: blank for the whole tee; F for the front nine or B for  |
 |   the back nine of an 18-hole tee)                                       |
 | Putts count only strokes taken with the ball on the putting green, so a  |
 | hole holed from off the green is 0. Greens in regulation and sand saves  |
 | are never entered; they are calculated from the score and putts. A file  |
 | that still has old Saves or GIR columns loads, with those columns        |
 | ignored and a warning.                                                   |
 |                                                                          |
 | Public Methods:                                                          |
 | load(dir)   - reads both files from a folder                             |
 | parseCourses, parseRounds - turn lines of text into objects (these are   |
 |               separate so the self-test can feed them text directly)     |
 +--------------------------------------------------------------------------+
 */
public final class DataLoader
{

    public static final String COURSES_FILE = "courses.tsv";
    public static final String ROUNDS_FILE = "rounds.tsv";
    private static final DateTimeFormatter SLASH_DATE =
            DateTimeFormatter.ofPattern("M/d/uuuu").withResolverStyle(ResolverStyle.STRICT);
    private static final double MIN_USUAL_RATING_18 = 50.0;
    private static final double MAX_USUAL_RATING_18 = 85.0;
    private static final double MIN_USUAL_RATING_9 = 25.0;
    private static final double MAX_USUAL_RATING_9 = 45.0;
    private static final int MIN_SLOPE = 55;
    private static final int MAX_SLOPE = 155;

    private DataLoader()
    {
    }

    /*
     +--------------------------------------------------------------------------+
     | load(dir)                                                                |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Reads courses.tsv and then rounds.tsv from the data folder. Courses      |
     | are read first because every round must refer to a known course/tee.     |
     |                                                                          |
     | Passed in:  dir - the data folder                                        |
     | Passed out: a Dataset holding the tees, rounds and any problems          |
     +--------------------------------------------------------------------------+
     */
    public static Dataset load(Path dir) throws IOException
    {
        List<Problem> problems = new ArrayList<>();
        List<Tee> tees = parseCourses(readLines(dir.resolve(COURSES_FILE), COURSES_FILE, problems), problems);
        Map<Integer, Tee> teeLookup = new HashMap<>();
        for (Tee tee : tees)
        {
            teeLookup.put(tee.id(), tee);
        }
        List<Round> rounds = parseRounds(readLines(dir.resolve(ROUNDS_FILE), ROUNDS_FILE, problems), teeLookup, problems);
        return new Dataset(tees, rounds, problems);
    }

    /*
     +--------------------------------------------------------------------------+
     | readLines(path, name, problems)                                          |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Reads a text file into a list of lines, removing a byte-order mark       |
     | if a spreadsheet program added one.                                      |
     |                                                                          |
     | Passed in:  path     - the file to read                                  |
     |             name     - short file name used in problem messages          |
     |             problems - list that receives an ERROR if the file is        |
     |                        missing                                           |
     | Passed out: the lines of the file (empty if it was not found)            |
     +--------------------------------------------------------------------------+
     */
    static List<String> readLines(Path path, String name, List<Problem> problems) throws IOException
    {
        if (!Files.exists(path))
        {
            problems.add(error(name, 0, "file not found: " + path.toAbsolutePath()));
            return new ArrayList<>();
        }
        List<String> lines = new ArrayList<>(Files.readAllLines(path, StandardCharsets.UTF_8));
        if (!lines.isEmpty() && lines.get(0).startsWith("\uFEFF"))
        {
            lines.set(0, lines.get(0).substring(1));
        }
        return lines;
    }

    /*
     +--------------------------------------------------------------------------+
     | parseCourses(lines, problems)                                            |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Converts the lines of courses.tsv into Tee objects, checking each        |
     | row as it goes: unique positive TeeID, rating and slope present,         |
     | one par (3 to 6) and one stroke index per hole, a stroke index that      |
     | uses each number from 1 to the number of holes exactly once, and         |
     | front/back nine ratings given in pairs.                                  |
     |                                                                          |
     | Passed in:  lines    - text lines of the file, header included           |
     |             problems - list that receives errors and warnings            |
     | Passed out: the Tee objects for every row that was valid                 |
     +--------------------------------------------------------------------------+
     */
    static List<Tee> parseCourses(List<String> lines, List<Problem> problems)
    {
        List<Tee> loaded = new ArrayList<>();
        Table table = parseTable(lines, COURSES_FILE,
                List.of("teeid", "course", "tee", "rating", "slope", "pars", "strokeindex"), problems);
        if (table == null)
        {
            return loaded;
        }

        Map<Integer, Integer> idLine = new HashMap<>();
        Set<String> courseTeeNames = new HashSet<>();
        for (int row = 0; row < table.rows.size(); row++)
        {
            String[] cells = table.rows.get(row);
            int lineNo = table.lineNos.get(row);
            try
            {
                int id = intCell(table, cells, "teeid");
                if (id <= 0)
                {
                    throw new RowError("TeeID must be a positive number");
                }
                if (idLine.containsKey(id))
                {
                    throw new RowError("duplicate TeeID " + id + " (already used on line " + idLine.get(id)
                            + "); every course/tee combination needs its own ID");
                }
                String course = table.get(cells, "course");
                String teeName = table.get(cells, "tee");
                if (course.isEmpty())
                {
                    throw new RowError("Course name is empty");
                }
                if (teeName.isEmpty())
                {
                    throw new RowError("Tee name is empty");
                }
                int holes = table.get(cells, "holes").isEmpty() ? Tee.HOLES : intCell(table, cells, "holes");
                if (holes != Tee.NINE && holes != Tee.HOLES)
                {
                    throw new RowError("Holes must be 9 or 18, found " + holes);
                }
                double rating = doubleCell(table, cells, "rating");
                int slope = intCell(table, cells, "slope");
                if (rating <= 0)
                {
                    throw new RowError("Rating must be greater than 0");
                }
                int yardage = table.get(cells, "yardage").isEmpty() ? 0 : intCell(table, cells, "yardage");
                int[] pars = intList(table.get(cells, "pars"), "Pars", holes, 3, 6);
                int[] strokeIndex = intList(table.get(cells, "strokeindex"), "StrokeIndex", holes, 1, holes);
                boolean[] indexUsed = new boolean[holes + 1];
                for (int rank : strokeIndex)
                {
                    if (indexUsed[rank])
                    {
                        throw new RowError("StrokeIndex must use each number 1-" + holes
                                + " exactly once (" + rank + " is repeated)");
                    }
                    indexUsed[rank] = true;
                }
                checkRatingRange(problems, lineNo, "Rating", rating, slope, holes);

                double frontRating = 0;
                int frontSlope = 0;
                double backRating = 0;
                int backSlope = 0;
                if (holes == Tee.HOLES)
                {
                    frontRating = optionalDouble(table, cells, "frontrating");
                    frontSlope = optionalInt(table, cells, "frontslope");
                    backRating = optionalDouble(table, cells, "backrating");
                    backSlope = optionalInt(table, cells, "backslope");
                    if ((frontRating > 0) != (frontSlope > 0))
                    {
                        throw new RowError("FrontRating and FrontSlope must be given together");
                    }
                    if ((backRating > 0) != (backSlope > 0))
                    {
                        throw new RowError("BackRating and BackSlope must be given together");
                    }
                    if (frontSlope > 0)
                    {
                        checkRatingRange(problems, lineNo, "FrontRating", frontRating, frontSlope, Tee.NINE);
                    }
                    if (backSlope > 0)
                    {
                        checkRatingRange(problems, lineNo, "BackRating", backRating, backSlope, Tee.NINE);
                    }
                }
                else if (!table.get(cells, "frontrating").isEmpty() || !table.get(cells, "backrating").isEmpty())
                {
                    problems.add(warning(COURSES_FILE, lineNo,
                            "front/back ratings are ignored on a 9-hole tee; its Rating and Slope are the nine-hole ratings"));
                }
                if (!courseTeeNames.add((course + "|" + teeName).toLowerCase(Locale.ROOT)))
                {
                    problems.add(warning(COURSES_FILE, lineNo, "course/tee '" + course + " / " + teeName
                            + "' appears more than once"));
                }
                idLine.put(id, lineNo);
                loaded.add(new Tee(id, course, table.get(cells, "city"), table.get(cells, "state"),
                        table.get(cells, "zip"), teeName, holes, rating, slope, yardage, pars, strokeIndex,
                        frontRating, frontSlope, backRating, backSlope));
            }
            catch (RowError problem)
            {
                problems.add(error(COURSES_FILE, lineNo, problem.getMessage()));
            }
        }
        return loaded;
    }

    /*
     +--------------------------------------------------------------------------+
     | parseRounds(lines, tees, problems)                                       |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Converts the lines of rounds.tsv into Round objects. Structural          |
     | mistakes skip the row (duplicate RoundID, unknown TeeID, bad date,       |
     | a Holes value that does not suit the tee, the wrong number of            |
     | holes in a list, putts not less than the score; a hole holed from off    |
     | the green has 0 putts, which is allowed). Suspicious entries only        |
     | produce warnings: a fairway marked on a par 3, old Saves/GIR columns     |
     | that are being ignored, or a nine-hole round on a tee whose nine has     |
     | no rating (the round still counts in the statistics, but not toward      |
     | the handicap).                                                           |
     |                                                                          |
     | Passed in:  lines    - text lines of the file, header included           |
     |             tees     - known course/tees, keyed by TeeID                 |
     |             problems - list that receives errors and warnings            |
     | Passed out: the Round objects for every row that was valid               |
     +--------------------------------------------------------------------------+
     */
    static List<Round> parseRounds(List<String> lines, Map<Integer, Tee> tees, List<Problem> problems)
    {
        List<Round> loaded = new ArrayList<>();
        Table table = parseTable(lines, ROUNDS_FILE,
                List.of("roundid", "teeid", "date", "scores", "putts", "fairways", "sand"), problems);
        if (table == null)
        {
            return loaded;
        }
        if (table.columns.containsKey("saves") || table.columns.containsKey("gir"))
        {
            problems.add(warning(ROUNDS_FILE, 0, "the Saves and GIR columns are ignored: sand saves and greens in "
                    + "regulation are calculated from the score and putts"));
        }

        Map<Integer, Integer> idLine = new HashMap<>();
        for (int row = 0; row < table.rows.size(); row++)
        {
            String[] cells = table.rows.get(row);
            int lineNo = table.lineNos.get(row);
            try
            {
                int id = intCell(table, cells, "roundid");
                if (id <= 0)
                {
                    throw new RowError("RoundID must be a positive number");
                }
                if (idLine.containsKey(id))
                {
                    throw new RowError("duplicate RoundID " + id + " (already used on line " + idLine.get(id) + ")");
                }
                int teeId = intCell(table, cells, "teeid");
                Tee tee = tees.get(teeId);
                if (tee == null)
                {
                    throw new RowError("TeeID " + teeId + " is not in " + COURSES_FILE);
                }
                LocalDate date = parseDate(table.get(cells, "date"));
                int pcc = table.get(cells, "pcc").isEmpty() ? 0 : intCell(table, cells, "pcc");
                if (pcc < -1 || pcc > 3)
                {
                    throw new RowError("PCC must be between -1 and 3 (usually 0)");
                }

                String holesText = table.get(cells, "holes").toUpperCase(Locale.ROOT);
                int startHole;
                int holesPlayed;
                if (tee.holes() == Tee.NINE)
                {
                    if (!holesText.isEmpty() && !holesText.equals("9"))
                    {
                        throw new RowError("Holes must be blank or 9 on a 9-hole tee, found '" + holesText + "'");
                    }
                    startHole = 0;
                    holesPlayed = Tee.NINE;
                }
                else if (holesText.isEmpty() || holesText.equals("18"))
                {
                    startHole = 0;
                    holesPlayed = Tee.HOLES;
                }
                else if (holesText.equals("F") || holesText.equals("FRONT"))
                {
                    startHole = 0;
                    holesPlayed = Tee.NINE;
                }
                else if (holesText.equals("B") || holesText.equals("BACK"))
                {
                    startHole = Tee.NINE;
                    holesPlayed = Tee.NINE;
                }
                else
                {
                    throw new RowError("Holes must be blank, 18, F (front nine) or B (back nine), found '" + holesText + "'");
                }

                int[] scores = intList(table.get(cells, "scores"), "Scores", holesPlayed, 1, 30);
                int[] putts = intList(table.get(cells, "putts"), "Putts", holesPlayed, 0, 15);
                for (int hole = 0; hole < holesPlayed; hole++)
                {
                    if (putts[hole] >= scores[hole])
                    {
                        throw new RowError("hole " + (startHole + hole + 1) + ": putts (" + putts[hole]
                                + ") must be fewer than the score (" + scores[hole] + ")");
                    }
                }
                boolean[] fairway = flagList(table.get(cells, "fairways"), "Fairways", holesPlayed);
                boolean[] sand = flagList(table.get(cells, "sand"), "Sand", holesPlayed);

                List<Integer> par3Fairways = new ArrayList<>();
                for (int hole = 0; hole < holesPlayed; hole++)
                {
                    int par = tee.pars()[startHole + hole];
                    int holeNumber = startHole + hole + 1;
                    if (fairway[hole] && par == 3)
                    {
                        par3Fairways.add(holeNumber);
                    }
                }
                if (!par3Fairways.isEmpty())
                {
                    problems.add(warning(ROUNDS_FILE, lineNo, "fairway marked on par 3 hole(s) " + par3Fairways
                            + " - ignored, fairway % only counts par 4s and 5s"));
                }
                if (!tee.isRated(startHole, holesPlayed))
                {
                    problems.add(warning(ROUNDS_FILE, lineNo, "tee " + teeId + " has no "
                            + (startHole == 0 ? "front" : "back")
                            + " nine rating, so this round counts in the statistics but not toward the handicap"));
                }
                idLine.put(id, lineNo);
                loaded.add(new Round(id, teeId, date, pcc, startHole, holesPlayed, scores, putts,
                        fairway, sand));
            }
            catch (RowError problem)
            {
                problems.add(error(ROUNDS_FILE, lineNo, problem.getMessage()));
            }
        }
        return loaded;
    }

    /*
     +--------------------------------------------------------------------------+
     | RowError                                                                 |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Internal signal used to abandon one bad row. It carries the message      |
     | that ends up in the Problem list. It never leaves this class.            |
     +--------------------------------------------------------------------------+
     */
    private static final class RowError extends Exception
    {
        private static final long serialVersionUID = 1L;

        RowError(String message)
        {
            super(message);
        }
    }

    /*
     +--------------------------------------------------------------------------+
     | Table                                                                    |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | A parsed tab-separated file: which column each header name sits in,      |
     | the data rows, and the original file line number of each row.            |
     |                                                                          |
     | Public Methods:                                                          |
     | get(cells, name) - returns the trimmed text of the named column in a     |
     |                    row, or "" if the column or cell is absent            |
     +--------------------------------------------------------------------------+
     */
    private static final class Table
    {
        final Map<String, Integer> columns = new HashMap<>();
        final List<String[]> rows = new ArrayList<>();
        final List<Integer> lineNos = new ArrayList<>();

        /*
         * get(cells, name): reads one cell of a row by column name.
         * Passed in:  cells - one data row, split into cells
         *             name  - normalized column name, e.g. "teeid"
         * Passed out: trimmed cell text, or "" when not present
         */
        String get(String[] cells, String name)
        {
            Integer col = columns.get(name);
            return (col == null || col >= cells.length) ? "" : cells[col].trim();
        }
    }

    /*
     +--------------------------------------------------------------------------+
     | parseTable(lines, file, required, problems)                              |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Splits file lines into a header and data rows, skipping blank lines      |
     | and # comments, and checks that every required column is present.        |
     |                                                                          |
     | Passed in:  lines    - text lines of the file                            |
     |             file     - short file name used in problem messages          |
     |             required - normalized names of the columns that must exist   |
     |             problems - list that receives an ERROR when the header       |
     |                        is missing or incomplete                          |
     | Passed out: the parsed Table, or null if the file cannot be used         |
     +--------------------------------------------------------------------------+
     */
    private static Table parseTable(List<String> lines, String file, List<String> required, List<Problem> problems)
    {
        Table table = null;
        for (int lp = 0; lp < lines.size(); lp++)
        {
            String rawLine = lines.get(lp);
            if (rawLine.isBlank() || rawLine.trim().startsWith("#"))
            {
                continue;
            }
            String[] cells = rawLine.split("\t", -1);
            if (table == null)
            {
                table = new Table();
                for (int col = 0; col < cells.length; col++)
                {
                    table.columns.putIfAbsent(normalize(cells[col]), col);
                }
                List<String> missing = new ArrayList<>();
                for (String name : required)
                {
                    if (!table.columns.containsKey(name))
                    {
                        missing.add(name);
                    }
                }
                if (!missing.isEmpty())
                {
                    problems.add(error(file, lp + 1, "header row is missing column(s): " + missing));
                    return null;
                }
            }
            else
            {
                table.rows.add(cells);
                table.lineNos.add(lp + 1);
            }
        }
        if (table == null)
        {
            problems.add(error(file, 0, "no header row found"));
        }
        return table;
    }

    /*
     * normalize(text): makes header names easy to match, so "Stroke Index" and "stroke_index" both become "strokeindex".
     * Passed in:  text - a header cell
     * Passed out: lower case with no spaces or underscores
     */
    static String normalize(String text)
    {
        return text.trim().toLowerCase(Locale.ROOT).replace(" ", "").replace("_", "");
    }

    /*
     * intCell(table, cells, name): reads a whole-number cell from a row.
     * Passed in:  table, cells - the parsed table and one of its rows
     *             name         - normalized column name
     * Passed out: the number; throws RowError if it is not a whole number
     */
    private static int intCell(Table table, String[] cells, String name) throws RowError
    {
        String text = table.get(cells, name);
        try
        {
            return Integer.parseInt(text);
        }
        catch (NumberFormatException problem)
        {
            throw new RowError(name + " must be a whole number, found '" + text + "'");
        }
    }

    /*
     * doubleCell(table, cells, name): reads a decimal-number cell, such as a course rating.
     * Passed in:  table, cells, name - as for intCell
     * Passed out: the number; throws RowError if it is not a number
     */
    private static double doubleCell(Table table, String[] cells, String name) throws RowError
    {
        String text = table.get(cells, name);
        try
        {
            return Double.parseDouble(text);
        }
        catch (NumberFormatException problem)
        {
            throw new RowError(name + " must be a number, found '" + text + "'");
        }
    }

    /*
     * optionalInt(table, cells, name): like intCell, but a blank cell or missing column gives 0.
     * Passed in:  table, cells, name - as for intCell
     * Passed out: the number, or 0 when blank
     */
    private static int optionalInt(Table table, String[] cells, String name) throws RowError
    {
        return table.get(cells, name).isEmpty() ? 0 : intCell(table, cells, name);
    }

    /*
     * optionalDouble(table, cells, name): like doubleCell, but a blank cell or missing column gives 0.
     * Passed in:  table, cells, name - as for doubleCell
     * Passed out: the number, or 0 when blank
     */
    private static double optionalDouble(Table table, String[] cells, String name) throws RowError
    {
        return table.get(cells, name).isEmpty() ? 0 : doubleCell(table, cells, name);
    }

    /*
     +--------------------------------------------------------------------------+
     | checkRatingRange(problems, lineNo, label, rating, slope, holes)          |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Adds a WARNING when a Course Rating or Slope Rating is outside the       |
     | range normally seen, which usually means a typing slip. 18-hole          |
     | ratings are expected between 50 and 85, nine-hole ones between 25        |
     | and 45, and slope between 55 and 155.                                    |
     |                                                                          |
     | Passed in:  problems - list that receives the warnings                   |
     |             lineNo   - line number for the message                       |
     |             label    - which column is being checked, e.g. "Rating"      |
     |             rating, slope - the values to check                          |
     |             holes    - 18 or 9, the number of holes the rating covers    |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    private static void checkRatingRange(List<Problem> problems, int lineNo, String label,
                                         double rating, int slope, int holes)
    {
        double minRating = holes == Tee.HOLES ? MIN_USUAL_RATING_18 : MIN_USUAL_RATING_9;
        double maxRating = holes == Tee.HOLES ? MAX_USUAL_RATING_18 : MAX_USUAL_RATING_9;
        if (rating < minRating || rating > maxRating)
        {
            problems.add(warning(COURSES_FILE, lineNo, label + " " + rating + " looks unusual for a "
                    + holes + "-hole rating"));
        }
        if (slope < MIN_SLOPE || slope > MAX_SLOPE)
        {
            problems.add(warning(COURSES_FILE, lineNo, label + " has a Slope of " + slope
                    + ", outside the usual " + MIN_SLOPE + "-" + MAX_SLOPE + " range"));
        }
    }

    /*
     +--------------------------------------------------------------------------+
     | intList(text, name, count, min, max)                                     |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Reads a cell holding one whole number per hole, such as                  |
     | "4,3,5,4,...", and checks the count and range.                           |
     |                                                                          |
     | Passed in:  text  - the cell text                                        |
     |             name  - column name for error messages                       |
     |             count - how many values are required (9 or 18)               |
     |             min, max - allowed range for every value                     |
     | Passed out: array of values (position 0 is the first hole played);       |
     |             throws RowError if the count, a value, or its range is wrong |
     +--------------------------------------------------------------------------+
     */
    private static int[] intList(String text, String name, int count, int min, int max) throws RowError
    {
        String[] parts = text.split(",", -1);
        int found = text.isBlank() ? 0 : parts.length;
        if (found != count)
        {
            throw new RowError(name + " needs " + count + " comma-separated values, found " + found);
        }
        int[] values = new int[count];
        for (int hole = 0; hole < count; hole++)
        {
            String part = parts[hole].trim();
            try
            {
                values[hole] = Integer.parseInt(part);
            }
            catch (NumberFormatException problem)
            {
                throw new RowError(name + " value " + (hole + 1) + " is not a whole number: '" + part + "'");
            }
            if (values[hole] < min || values[hole] > max)
            {
                throw new RowError(name + " value " + (hole + 1) + " = " + values[hole]
                        + " is outside " + min + ".." + max);
            }
        }
        return values;
    }

    /*
     * flagList(text, name, count): reads a cell of yes/no flags written as 1 (yes) and 0 (no).
     * Passed in:  text  - the cell text, such as "0,1,1,0,..."
     *             name  - column name for error messages
     *             count - how many flags are required (9 or 18)
     * Passed out: array of booleans; throws RowError for a bad cell
     */
    private static boolean[] flagList(String text, String name, int count) throws RowError
    {
        int[] values = intList(text, name, count, 0, 1);
        boolean[] flags = new boolean[count];
        for (int hole = 0; hole < count; hole++)
        {
            flags[hole] = values[hole] == 1;
        }
        return flags;
    }

    /*
     * parseDate(text): reads a date written as 2026-10-22 or as 10/22/2026.
     * Passed in:  text - the date cell
     * Passed out: the date; throws RowError if neither format fits
     */
    private static LocalDate parseDate(String text) throws RowError
    {
        try
        {
            return LocalDate.parse(text);
        }
        catch (DateTimeParseException notIsoFormat)
        {
            // not year-month-day; try month/day/year below
        }
        try
        {
            return LocalDate.parse(text, SLASH_DATE);
        }
        catch (DateTimeParseException notSlashFormat)
        {
            throw new RowError("Date '" + text + "' must look like 2026-10-22 or 10/22/2026");
        }
    }

    /*
     * parseDateOrNull(text): reads a date typed as 2026-10-22 or 10/22/2026, for screens that take a typed date.
     * Passed in:  text - what the user typed
     * Passed out: the date, or null if it is not a valid date in either format
     */
    public static LocalDate parseDateOrNull(String text)
    {
        try
        {
            return parseDate(text.trim());
        }
        catch (RowError notADate)
        {
            return null;
        }
    }

    /*
     * error(file, line, message): builds an ERROR Problem (line 0 = not tied to a line).
     */
    private static Problem error(String file, int line, String message)
    {
        return new Problem(Problem.Severity.ERROR, file, line, message);
    }

    /*
     * warning(file, line, message): builds a WARNING Problem (line 0 = not tied to a line).
     */
    private static Problem warning(String file, int line, String message)
    {
        return new Problem(Problem.Severity.WARNING, file, line, message);
    }
}

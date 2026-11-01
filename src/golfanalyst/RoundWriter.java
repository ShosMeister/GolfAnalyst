package golfanalyst;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.Set;

/*
 +--------------------------------------------------------------------------+
 | RoundWriter                                                              |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | Adds a new round to rounds.tsv, or replaces the line of a round that     |
 | was edited. The line is written in the column order of the file's own    |
 | header row, so a file whose columns you have rearranged, or that has     |
 | extra columns of your own, stays intact. A column the program does not   |
 | know is left blank on a new line, and keeps what it had on a replaced    |
 | line. If the file does not exist, or has no header row yet, the          |
 | standard header is written first. Other lines and comments are never     |
 | touched: a new line is only added at the end, and a replaced line is     |
 | swapped in place with every other character of the file left as it       |
 | was. Before a line is replaced, a dated copy of the file as it was goes  |
 | in the backups folder (see Backups), so a bad edit can be undone by      |
 | hand.                                                                    |
 |                                                                          |
 | Constants:                                                               |
 | STANDARD_HEADER - the header used when a file has none                   |
 | HISTORICAL_HEADING - the Historical column's heading                     |
 |                                                                          |
 | Public Methods:                                                          |
 | append(file, round, tee)            - adds one round to the file         |
 | replace(file, round, tee)           - replaces the line of one round     |
 | formatLine(round, tee, headerCells) - builds the tab-separated line      |
 | holesText(round, tee)               - the Holes column text              |
 +--------------------------------------------------------------------------+
 */
public final class RoundWriter
{

    public static final String STANDARD_HEADER = "RoundID\tTeeID\tDate\tHoles\tPCC\tScores\tPutts\tFairways\tSand";
    public static final String HISTORICAL_HEADING = "Historical";
    private static final Set<String> KNOWN_COLUMNS =
            Set.of("roundid", "teeid", "date", "holes", "pcc", "scores", "putts", "fairways", "sand", "historical");

    private RoundWriter()
    {
    }

    /*
     * holesText(round, tee): works out what goes in the Holes column.
     * Passed in:  round - the round, tee - the course/tee it was played on
     * Passed out: "" for a whole tee, "F" for the front nine or "B" for the back nine of an 18-hole tee
     */
    public static String holesText(Round round, Tee tee)
    {
        if (!round.isNineHole() || tee.holes() == Tee.NINE)
        {
            return "";
        }
        return round.startHole() == 0 ? "F" : "B";
    }

    /*
     * joinNumbers(values): turns per-hole numbers into one comma-separated cell.
     * Passed in:  values - one number per hole played
     * Passed out: text like "5,4,6,..."
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
     * joinFlags(flags): turns per-hole yes/no flags into one comma-separated cell of 1s and 0s.
     * Passed in:  flags - one flag per hole played
     * Passed out: text like "1,0,0,1,..."
     */
    private static String joinFlags(boolean[] flags)
    {
        StringBuilder text = new StringBuilder();
        for (int lp = 0; lp < flags.length; lp = lp + 1)
        {
            text.append(lp == 0 ? "" : ",").append(flags[lp] ? 1 : 0);
        }
        return text.toString();
    }

    /*
     * cellText(round, tee, column): the text of one column of a round's line.
     * Passed in:  round, tee - what is being written; column - normalized column name, e.g. "roundid"
     * Passed out: the text for that column, or "" for a column this program does not know
     */
    private static String cellText(Round round, Tee tee, String column)
    {
        return switch (column)
        {
            case "roundid" -> Integer.toString(round.id());
            case "teeid" -> Integer.toString(round.teeId());
            case "date" -> round.date().toString();
            case "holes" -> holesText(round, tee);
            case "pcc" -> round.pcc() == 0 ? "" : Integer.toString(round.pcc());
            case "scores" -> joinNumbers(round.scores());
            case "putts" -> joinNumbers(round.putts());
            case "fairways" -> joinFlags(round.fairway());
            case "sand" -> joinFlags(round.sand());
            case "historical" -> (round.historical() ? "Y" : "");
            default -> "";
        };
    }

    /*
     +--------------------------------------------------------------------------+
     | formatLine(round, tee, headerCells)                                      |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Builds one line of rounds.tsv for a round, with the columns in the       |
     | order of the header row.                                                 |
     |                                                                          |
     | Passed in:  round       - the round to write                             |
     |             tee         - the course/tee it was played on                |
     |             headerCells - the header row, split into its cells           |
     | Passed out: the tab-separated line (no line ending)                      |
     +--------------------------------------------------------------------------+
     */
    public static String formatLine(Round round, Tee tee, String[] headerCells)
    {
        StringBuilder line = new StringBuilder();
        for (int col = 0; col < headerCells.length; col = col + 1)
        {
            if (col > 0)
            {
                line.append('\t');
            }
            line.append(cellText(round, tee, DataLoader.normalize(headerCells[col])));
        }
        return line.toString();
    }

    /*
     +--------------------------------------------------------------------------+
     | findHeader(text)                                                         |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Finds the header row of a data file: the first line that is neither      |
     | blank nor a # comment.                                                   |
     |                                                                          |
     | Passed in:  text - the whole file                                        |
     | Passed out: the header split into cells, or null if there is none        |
     |             (CourseWriter uses it too)                                   |
     +--------------------------------------------------------------------------+
     */
    static String[] findHeader(String text)
    {
        String clean = text.startsWith("\uFEFF") ? text.substring(1) : text;
        for (String line : clean.split("\\R"))
        {
            if (!line.isBlank() && !line.trim().startsWith("#"))
            {
                return line.split("\t", -1);
            }
        }
        return null;
    }

    /*
     * hasHistoricalColumn(headerCells): whether the header row has a Historical column (any capitals or spaces).
     * Passed in:  headerCells - the header row, split into its cells
     * Passed out: true if it has one
     */
    static boolean hasHistoricalColumn(String[] headerCells)
    {
        for (String cell : headerCells)
        {
            if (DataLoader.normalize(cell).equals("historical"))
            {
                return true;
            }
        }
        return false;
    }

    /*
     +--------------------------------------------------------------------------+
     | withHistoricalColumn(text)                                               |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Adds a Historical column to the end of the header row, so a ticked       |
     | Historical box has somewhere to go in a file made before v4.30 (the      |
     | sample data has no such column). Only the header line changes: rows      |
     | without the column read as not historical, so every other character of   |
     | the file stays as it was.                                                |
     |                                                                          |
     | Passed in:  text - the whole file, with a header row                     |
     | Passed out: the same text with "<tab>Historical" after the header        |
     +--------------------------------------------------------------------------+
     */
    static String withHistoricalColumn(String text)
    {
        int lineStart = (!text.isEmpty() && text.charAt(0) == LineReplacer.BYTE_ORDER_MARK ? 1 : 0);
        while (lineStart < text.length())
        {
            int lineEnd = lineStart;
            while (lineEnd < text.length() && text.charAt(lineEnd) != '\n' && text.charAt(lineEnd) != '\r')
            {
                lineEnd = lineEnd + 1;
            }
            String line = text.substring(lineStart, lineEnd);
            if (!line.isBlank() && !line.trim().startsWith("#"))
            {
                return text.substring(0, lineEnd) + "\t" + HISTORICAL_HEADING + text.substring(lineEnd);
            }
            lineStart = lineEnd + 1;
        }
        return text;
    }

    /*
     +--------------------------------------------------------------------------+
     | append(file, round, tee)                                                 |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Adds a round to the end of rounds.tsv, creating the file (with the       |
     | standard header) if it does not exist or has no header. The file's       |
     | own line ending style is kept. A historical round going into a file      |
     | with no Historical column first adds one to the header row (see          |
     | withHistoricalColumn), with a backup copy first (it changes a line).     |
     |                                                                          |
     | Passed in:  file  - the rounds.tsv path                                  |
     |             round - the round to add                                     |
     |             tee   - the course/tee it was played on                      |
     | Passed out: nothing (the file is changed); throws IOException if the     |
     |             file cannot be written                                       |
     +--------------------------------------------------------------------------+
     */
    public static void append(Path file, Round round, Tee tee) throws IOException
    {
        String existing = Files.exists(file) ? Files.readString(file, StandardCharsets.UTF_8) : "";
        if (round.historical() && findHeader(existing) != null && !hasHistoricalColumn(findHeader(existing)))
        {
            existing = withHistoricalColumn(existing);
            LineReplacer.write(file, existing);
        }
        String[] headerCells = findHeader(existing);
        String newline = existing.contains("\r\n") ? "\r\n" : "\n";
        StringBuilder addition = new StringBuilder();
        if (!existing.isEmpty() && !existing.endsWith("\n"))
        {
            addition.append(newline);
        }
        if (headerCells == null)
        {
            String header = STANDARD_HEADER + (round.historical() ? "\t" + HISTORICAL_HEADING : "");
            addition.append(header).append(newline);
            headerCells = header.split("\t");
        }
        addition.append(formatLine(round, tee, headerCells)).append(newline);
        if (file.getParent() != null)
        {
            Files.createDirectories(file.getParent());
        }
        Files.writeString(file, addition.toString(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    /*
     +--------------------------------------------------------------------------+
     | replace(file, round, tee)                                                |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Replaces the line of an edited round: the one line whose RoundID is the  |
     | round's id. The new line follows the header's column order; a column     |
     | the program does not know keeps what was in it. Everything else in the   |
     | file (other lines, comments, blank lines, line endings) is left exactly  |
     | as it was. First a dated copy goes in the backups folder; the new file   |
     | is then written to a temporary file next to it and swapped in, so a      |
     | failure part way cannot leave a half-written rounds.tsv. A historical    |
     | round in a file with no Historical column also adds one to the header    |
     | row.                                                                     |
     |                                                                          |
     | Passed in:  file  - the rounds.tsv path                                  |
     |             round - the edited round, with the id of the saved one       |
     |             tee   - the course/tee it was played on                      |
     | Passed out: nothing (the file is changed); throws IOException if the     |
     |             file has no header or no RoundID column, if no line (or      |
     |             more than one) has that RoundID, or if it cannot be written  |
     +--------------------------------------------------------------------------+
     */
    public static void replace(Path file, Round round, Tee tee) throws IOException
    {
        String text = Files.readString(file, StandardCharsets.UTF_8);
        if (round.historical() && findHeader(text) != null && !hasHistoricalColumn(findHeader(text)))
        {
            text = withHistoricalColumn(text);
        }
        LineReplacer.LineMaker maker = (headerCells, oldCells) ->
        {
            StringBuilder line = new StringBuilder();
            for (int col = 0; col < headerCells.length; col = col + 1)
            {
                String column = DataLoader.normalize(headerCells[col]);
                String cell = KNOWN_COLUMNS.contains(column) ? cellText(round, tee, column) : (col < oldCells.length ? oldCells[col] : "");
                line.append(col > 0 ? "\t" : "").append(cell);
            }
            return line.toString();
        };
        LineReplacer.Spliced spliced = LineReplacer.splice(text, "roundid", Map.of(round.id(), maker), file.getFileName().toString());
        LineReplacer.write(file, spliced.text());
    }
}

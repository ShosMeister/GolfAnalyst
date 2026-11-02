package golfanalyst.print;

import golfanalyst.LineReplacer;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/*
 +--------------------------------------------------------------------------+
 | Csv                                                                      |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | Writes a report's table as a spreadsheet file (.csv) that Excel, Numbers |
 | and LibreOffice open as they are: UTF-8 with a byte order mark (so       |
 | Excel reads accents and the dagger right), commas between fields, a      |
 | field holding a comma, a double quote or a line break put in double      |
 | quotes with its quotes doubled, one header row, dates as 2026-10-07,     |
 | numbers with a dot for the decimal point, and Windows line breaks. The   |
 | file is written to a temporary file beside it and then moved into        |
 | place, so a failed save never leaves half a file. Whether an existing    |
 | file may be replaced is asked before this is called.                     |
 |                                                                          |
 | Public Methods:                                                          |
 | field(words)        - one field, quoted if it needs to be                |
 | text(rows)          - the whole file's text (without the byte order mark)|
 | write(file, rows)   - writes the file                                    |
 | fileName(report, day) - "RoundLog_2026-11-01.csv"                        |
 | number(value, places) - "13.9", or "" for nothing to show                |
 +--------------------------------------------------------------------------+
 */
public final class Csv
{

    public static final String LINE_BREAK = "\r\n";

    private Csv()
    {
    }

    /*
     * field(words): one field: as it is, or in double quotes with its quotes doubled if it holds a comma, a quote or a line break.
     */
    public static String field(String words)
    {
        if (words.contains(",") || words.contains("\"") || words.contains("\n") || words.contains("\r"))
        {
            return "\"" + words.replace("\"", "\"\"") + "\"";
        }
        return words;
    }

    /*
     * text(rows): every row as a line of fields separated by commas, each line ended with a Windows line break.
     */
    public static String text(List<List<String>> rows)
    {
        StringBuilder text = new StringBuilder();
        for (List<String> row : rows)
        {
            List<String> fields = new ArrayList<>();
            for (String words : row)
            {
                fields.add(field(words));
            }
            text.append(String.join(",", fields)).append(LINE_BREAK);
        }
        return text.toString();
    }

    /*
     +--------------------------------------------------------------------------+
     | write(file, rows)                                                        |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Writes the rows to the file (see the class comment): first to a          |
     | temporary file in the same folder, then moved over the file's name.      |
     |                                                                          |
     | Passed in:  file - where to save                                         |
     |             rows - the header row and the data rows                      |
     | Passed out: nothing (throws IOException if it cannot be written; the     |
     |             temporary file is then removed)                              |
     +--------------------------------------------------------------------------+
     */
    public static void write(Path file, List<List<String>> rows) throws IOException
    {
        Path folder = file.toAbsolutePath().getParent();
        Path temporary = Files.createTempFile(folder, "golfanalyst-", ".csv.new");
        try
        {
            Files.writeString(temporary, LineReplacer.BYTE_ORDER_MARK + text(rows), StandardCharsets.UTF_8);
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
        }
        finally
        {
            Files.deleteIfExists(temporary);
        }
    }

    /*
     * fileName(report, day): the name the save window offers, the report's name without spaces and the day: "RoundLog_2026-11-01.csv".
     */
    public static String fileName(Report report, LocalDate day)
    {
        return report.name().replace(" ", "") + "_" + day + ".csv";
    }

    /*
     * number(value, places): a number with that many decimals and a dot, or "" for nothing to show (NaN).
     */
    public static String number(double value, int places)
    {
        if (Double.isNaN(value))
        {
            return "";
        }
        return String.format(Locale.ROOT, "%." + places + "f", value);
    }
}

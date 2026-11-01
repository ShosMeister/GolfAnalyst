package golfanalyst;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 +--------------------------------------------------------------------------+
 | LineReplacer                                                             |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | Changes or removes existing lines of a data file safely. It is shared    |
 | by rounds (an edited or deleted round) and courses (edited tees).        |
 |                                                                          |
 | splice finds the lines whose ID column holds the given IDs and swaps in  |
 | new lines for them, or removes them (a line and its own line break);     |
 | every other character of the file (other lines, comments, blank lines,   |
 | line endings) stays exactly as it was. It works on text only, so nothing |
 | is written until write is called.                                        |
 |                                                                          |
 | write first puts a dated copy of the file in the backups folder (see     |
 | Backups; if that fails nothing is written), then writes the new text to  |
 | a temporary file next to it and swaps it in, so a failure part way       |
 | cannot leave a half-written file.                                        |
 |                                                                          |
 | Constants:                                                               |
 | BYTE_ORDER_MARK - the mark some spreadsheets put at the start of a file  |
 |                                                                          |
 | Public Methods:                                                          |
 | splice(text, idColumn, makers, fileName) - swaps in new lines            |
 | write(file, newText)                     - backup, then safe write       |
 | writeSafely(file, newText)               - safe write, no backup         |
 +--------------------------------------------------------------------------+
 */
public final class LineReplacer
{

    public static final char BYTE_ORDER_MARK = (char) 0xFEFF;
    private static final Pattern LINE_BREAK = Pattern.compile("\\R");

    /*
     * LineMaker: builds the new line for an ID from the header row and the line as it was (so columns the program does not know can keep their value).
     */
    public interface LineMaker
    {
        /*
         * make(headerCells, oldCells): the new line, tab separated, with no line ending, or null to remove the line with its line break.
         */
        String make(String[] headerCells, String[] oldCells);
    }

    /*
     * Spliced: the file's new text, and the line numbers (1 = first line) that were replaced.
     */
    public record Spliced(String text, Set<Integer> lineNumbers)
    {
    }

    private LineReplacer()
    {
    }

    /*
     +--------------------------------------------------------------------------+
     | splice(text, idColumn, makers, fileName)                                 |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Finds, below the header row, the line holding each ID in makers and      |
     | puts the new line in its place, or removes the line and its own line     |
     | break when the maker gives null. Lines that are blank or start with #    |
     | are never touched, and nothing else in the text changes.                 |
     |                                                                          |
     | Passed in:  text     - the whole file                                    |
     |             idColumn - the ID column's name, normalized ("roundid")      |
     |             makers   - for each ID, what builds its new line             |
     |             fileName - the file's name, for messages                     |
     | Passed out: the new text and the replaced line numbers; throws           |
     |             IOException if there is no header or no ID column, or if an  |
     |             ID is on no line or on more than one                         |
     +--------------------------------------------------------------------------+
     */
    public static Spliced splice(String text, String idColumn, Map<Integer, LineMaker> makers, String fileName)
            throws IOException
    {
        String[] headerCells = RoundWriter.findHeader(text);
        if (headerCells == null)
        {
            throw new IOException(fileName + " has no header row");
        }
        int idCol = -1;
        for (int col = 0; col < headerCells.length; col = col + 1)
        {
            if (DataLoader.normalize(headerCells[col]).equals(idColumn) && idCol < 0)
            {
                idCol = col;
            }
        }
        if (idCol < 0)
        {
            throw new IOException(fileName + " has no " + idColumn + " column");
        }

        TreeMap<Integer, int[]> found = new TreeMap<>();
        Map<Integer, Integer> foundId = new TreeMap<>();
        int lineStart = !text.isEmpty() && text.charAt(0) == BYTE_ORDER_MARK ? 1 : 0;
        int lineNumber = 1;
        boolean headerSeen = false;
        Matcher breaks = LINE_BREAK.matcher(text);
        while (lineStart <= text.length())
        {
            int lineEnd = breaks.find(lineStart) ? breaks.start() : text.length();
            String line = text.substring(lineStart, lineEnd);
            if (!line.isBlank() && !line.trim().startsWith("#"))
            {
                Integer id = headerSeen ? idOf(line, idCol) : null;
                if (id != null && makers.containsKey(id))
                {
                    if (foundId.containsValue(id))
                    {
                        throw new IOException("ID " + id + " is on more than one line of " + fileName);
                    }
                    int nextStart = (lineEnd < text.length() ? breaks.end() : lineEnd);
                    found.put(lineStart, new int[] {lineEnd, lineNumber, nextStart});
                    foundId.put(lineStart, id);
                }
                headerSeen = true;
            }
            if (lineEnd >= text.length())
            {
                break;
            }
            lineStart = breaks.end();
            lineNumber = lineNumber + 1;
        }
        for (int id : makers.keySet())
        {
            if (!foundId.containsValue(id))
            {
                throw new IOException("ID " + id + " is not in " + fileName);
            }
        }

        StringBuilder result = new StringBuilder();
        Set<Integer> lineNumbers = new HashSet<>();
        int copiedTo = 0;
        for (Map.Entry<Integer, int[]> place : found.entrySet())
        {
            int start = place.getKey();
            int end = place.getValue()[0];
            String[] oldCells = text.substring(start, end).split("\t", -1);
            String made = makers.get(foundId.get(start)).make(headerCells, oldCells);
            result.append(text, copiedTo, start);
            if (made == null)
            {
                copiedTo = place.getValue()[2];
            }
            else
            {
                result.append(made);
                copiedTo = end;
            }
            lineNumbers.add(place.getValue()[1]);
        }
        result.append(text.substring(copiedTo));
        return new Spliced(result.toString(), lineNumbers);
    }

    /*
     * idOf(line, idCol): the ID in a data line's ID column.
     * Passed in:  line - one line of the file, idCol - where the ID is
     * Passed out: the ID, or null when the cell is missing or not a whole number
     */
    private static Integer idOf(String line, int idCol)
    {
        String[] cells = line.split("\t", -1);
        if (idCol >= cells.length)
        {
            return null;
        }
        try
        {
            return Integer.valueOf(cells[idCol].trim());
        }
        catch (NumberFormatException notNumber)
        {
            return null;
        }
    }

    /*
     +--------------------------------------------------------------------------+
     | write(file, newText)                                                     |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Puts a dated copy of the file as it is in the backups folder             |
     | (Backups.copyBeforeChange), then writes the new text to a temporary      |
     | file next to it and swaps it in, so the real file is never left half     |
     | written. If the copy cannot be made, nothing is written.                 |
     |                                                                          |
     | Passed in:  file    - the data file                                      |
     |             newText - its whole new text                                 |
     | Passed out: the backup copy made; throws Backups.Failed if the copy      |
     |             cannot be made, or IOException if the file cannot be written |
     +--------------------------------------------------------------------------+
     */
    public static Path write(Path file, String newText) throws IOException
    {
        Path copy = Backups.copyBeforeChange(file);
        writeSafely(file, newText);
        return copy;
    }

    /*
     * writeSafely(file, newText): writes the new text to a temporary file next to the real one and swaps it in, so the real file is never left half written (no backup; Clear makes its own first).
     * Passed in:  file - the data file, newText - its whole new text
     * Passed out: nothing (throws IOException if it cannot be written)
     */
    public static void writeSafely(Path file, String newText) throws IOException
    {
        Path temporary = file.resolveSibling(file.getFileName() + ".new");
        Files.writeString(temporary, newText, StandardCharsets.UTF_8);
        try
        {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        }
        catch (IOException notAtomic)
        {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /*
     * lines(text): splits a file's text into lines the same way the loader reads it (the byte-order mark removed), so line numbers agree.
     * Passed in:  text - the whole file
     * Passed out: its lines
     */
    public static List<String> lines(String text)
    {
        String clean = !text.isEmpty() && text.charAt(0) == BYTE_ORDER_MARK ? text.substring(1) : text;
        List<String> lines = new ArrayList<>(List.of(clean.split("\\R", -1)));
        if (!lines.isEmpty() && lines.get(lines.size() - 1).isEmpty() && (clean.endsWith("\n") || clean.endsWith("\r")))
        {
            lines.remove(lines.size() - 1);
        }
        return lines;
    }
}

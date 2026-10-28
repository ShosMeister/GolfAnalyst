package golfanalyst;

/*
 +--------------------------------------------------------------------------+
 | Problem                                                                  |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | Describes one thing found wrong, or merely suspicious, while loading a   |
 | data file, with the file name and line number so it can be fixed.        |
 |                                                                          |
 | Severity:                                                                |
 | ERROR   - the row could not be understood and was skipped                |
 | WARNING - the row was kept, but something about it looks odd             |
 |                                                                          |
 | Fields (passed in when a Problem is created):                            |
 | severity - ERROR or WARNING                                              |
 | file     - name of the data file, for example "rounds.tsv"               |
 | line     - line number in that file (0 if the problem is not tied to     |
 |            one line, such as a missing file)                             |
 | message  - plain-English description of the problem                      |
 |                                                                          |
 | Public Methods:                                                          |
 | toString() - returns one line of text suitable for printing              |
 +--------------------------------------------------------------------------+
 */
public record Problem(Severity severity, String file, int line, String message)
{

    public enum Severity
    {
        ERROR, WARNING
    }

    /*
     * toString(): formats the problem for display.
     * Passed in:  nothing
     * Passed out: text like "ERROR  rounds.tsv line 12: TeeID 9 is not in courses.tsv"
     */
    @Override
    public String toString()
    {
        return severity + "  " + file + (line > 0 ? " line " + line : "") + ": " + message;
    }
}

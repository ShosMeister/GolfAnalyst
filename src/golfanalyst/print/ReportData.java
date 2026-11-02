package golfanalyst.print;

import golfanalyst.DataStore;
import golfanalyst.Handicap;
import golfanalyst.Round;
import golfanalyst.Tee;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/*
 +--------------------------------------------------------------------------+
 | ReportData                                                               |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | Everything a report is drawn from, read once from the data: the rounds   |
 | (oldest first), the tees by TeeID, the handicap history, the golfer's    |
 | name, Home's Include historical rounds box, whether this is the sample   |
 | data, the day the report is printed and the program's version. Nothing   |
 | here changes the data.                                                   |
 |                                                                          |
 | Fields:                                                                  |
 | rounds            - every round, oldest first                            |
 | tees              - the courses and tees by TeeID                        |
 | handicap          - Handicap.compute of the rounds                       |
 | golfer            - the golfer's name ("" if none)                       |
 | includeHistorical - Home's box                                           |
 | sample            - true when the sample data is showing                 |
 | today             - the printing day                                     |
 | version           - "4.00", for the foot of every page                   |
 |                                                                          |
 | Public Methods:                                                          |
 | of(store, today, version) - reads it from the program's data             |
 | note()                    - the words at the bottom left of every page   |
 +--------------------------------------------------------------------------+
 */
public record ReportData(List<Round> rounds, Map<Integer, Tee> tees, Handicap.Result handicap, String golfer,
                         boolean includeHistorical, boolean sample, LocalDate today, String version)
{

    public static final String SAMPLE_NOTE = "SAMPLE DATA - not real rounds";

    /*
     * of(store, today, version): the data a report needs, read from the program's store.
     */
    public static ReportData of(DataStore store, LocalDate today, String version)
    {
        return new ReportData(store.roundsByDate(), store.teeMap(), store.handicap(), store.golferName(),
                store.includeHistorical(), store.isSampleData(), today, version);
    }

    /*
     * note(): "SAMPLE DATA - not real rounds" when the sample data is showing, otherwise nothing.
     */
    public String note()
    {
        if (sample)
        {
            return SAMPLE_NOTE;
        }
        return "";
    }
}

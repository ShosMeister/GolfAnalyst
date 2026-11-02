package golfanalyst;

import java.time.LocalDate;

/*
 +--------------------------------------------------------------------------+
 | Period                                                                   |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | The choices of the Print window's Period pulldown and the days each one  |
 | covers. Both ends are included. Today is the end of every period except  |
 | Last month and Last year:                                                |
 |   All time       - every round (no ends)                                 |
 |   Last 30 days   - the 30 days ending today                              |
 |   This month     - the 1st of this month to today                        |
 |   Last month     - the whole previous calendar month                     |
 |   Last 3, 6 and 12 months - the same date that many months ago to today  |
 |                    (a day the month does not have becomes its last day:  |
 |                    3 months before May 31 is February 28 or 29)          |
 |   This year      - January 1 to today                                    |
 |   Last year      - all of the previous calendar year                     |
 |   Custom dates   - the From and To boxes decide (no ends here)           |
 | "Year to date" is not offered: it is the same as This year.              |
 |                                                                          |
 | Today is always passed in, so the dates can be tested with a fixed day.  |
 | There is no Swing code here.                                             |
 |                                                                          |
 | Public Methods:                                                          |
 | label()            - the words in the pulldown                           |
 | from(today)        - the first day, or null for no first day             |
 | to(today)          - the last day, or null for no last day               |
 | contains(date, today) - whether a day is inside the period               |
 +--------------------------------------------------------------------------+
 */
public enum Period
{
    ALL_TIME("All time"),
    LAST_30_DAYS("Last 30 days"),
    THIS_MONTH("This month"),
    LAST_MONTH("Last month"),
    LAST_3_MONTHS("Last 3 months"),
    LAST_6_MONTHS("Last 6 months"),
    LAST_12_MONTHS("Last 12 months"),
    THIS_YEAR("This year"),
    LAST_YEAR("Last year"),
    CUSTOM("Custom dates...");

    private static final int DAYS_IN_LAST_30 = 30;
    private final String label;

    /*
     * Period(label): a choice and the words shown for it.
     */
    Period(String label)
    {
        this.label = label;
    }

    /*
     * label(): the words in the pulldown, for example "Last 3 months".
     */
    public String label()
    {
        return label;
    }

    /*
     * toString(): the label, so the pulldown shows it.
     */
    @Override
    public String toString()
    {
        return label;
    }

    /*
     +--------------------------------------------------------------------------+
     | from(today)                                                              |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The first day of the period (included).                                  |
     |                                                                          |
     | Passed in:  today - the day to count from                                |
     | Passed out: the first day, or null for All time and Custom dates         |
     +--------------------------------------------------------------------------+
     */
    public LocalDate from(LocalDate today)
    {
        switch (this)
        {
            case LAST_30_DAYS:
                return today.minusDays(DAYS_IN_LAST_30 - 1);
            case THIS_MONTH:
                return today.withDayOfMonth(1);
            case LAST_MONTH:
                return today.minusMonths(1).withDayOfMonth(1);
            case LAST_3_MONTHS:
                return today.minusMonths(3);
            case LAST_6_MONTHS:
                return today.minusMonths(6);
            case LAST_12_MONTHS:
                return today.minusMonths(12);
            case THIS_YEAR:
                return today.withDayOfYear(1);
            case LAST_YEAR:
                return today.minusYears(1).withDayOfYear(1);
            default:
                return null;
        }
    }

    /*
     +--------------------------------------------------------------------------+
     | to(today)                                                                |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The last day of the period (included).                                   |
     |                                                                          |
     | Passed in:  today - the day to count from                                |
     | Passed out: the last day of the previous month for Last month, December  |
     |             31 of last year for Last year, null for All time and Custom  |
     |             dates, otherwise today                                       |
     +--------------------------------------------------------------------------+
     */
    public LocalDate to(LocalDate today)
    {
        switch (this)
        {
            case ALL_TIME:
            case CUSTOM:
                return null;
            case LAST_MONTH:
                return today.withDayOfMonth(1).minusDays(1);
            case LAST_YEAR:
                return today.withDayOfYear(1).minusDays(1);
            default:
                return today;
        }
    }

    /*
     * contains(date, today): whether the day falls inside the period (a missing end lets every day through on that side).
     */
    public boolean contains(LocalDate date, LocalDate today)
    {
        LocalDate first = from(today);
        LocalDate last = to(today);
        if (first != null && date.isBefore(first))
        {
            return false;
        }
        if (last != null && date.isAfter(last))
        {
            return false;
        }
        return true;
    }
}

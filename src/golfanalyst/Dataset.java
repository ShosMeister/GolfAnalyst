package golfanalyst;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 +--------------------------------------------------------------------------+
 | Dataset                                                                  |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | Bundles everything loaded from the data folder: every course/tee, every  |
 | round, and the list of problems found while loading them.                |
 |                                                                          |
 | Fields (passed in when a Dataset is created):                            |
 | tees     - all course/tee combinations that loaded successfully          |
 | rounds   - all rounds that loaded successfully                           |
 | problems - every error and warning found while loading                   |
 |                                                                          |
 | Public Methods:                                                          |
 | teeMap()       - returns the tees keyed by id, for quick lookup          |
 | errorCount()   - returns how many ERROR problems were found              |
 | warningCount() - returns how many WARNING problems were found            |
 +--------------------------------------------------------------------------+
 */
public record Dataset(List<Tee> tees, List<Round> rounds, List<Problem> problems)
{

    /*
     * teeMap(): builds a lookup so a round's TeeID can be turned into its Tee.
     * Passed in:  nothing
     * Passed out: map of tee id to Tee
     */
    public Map<Integer, Tee> teeMap()
    {
        Map<Integer, Tee> lookup = new HashMap<>();
        for (Tee tee : tees)
        {
            lookup.put(tee.id(), tee);
        }
        return lookup;
    }

    /*
     * errorCount(): counts the problems that caused a row to be skipped.
     * Passed in:  nothing
     * Passed out: number of ERROR problems
     */
    public long errorCount()
    {
        return problems.stream().filter(problem -> problem.severity() == Problem.Severity.ERROR).count();
    }

    /*
     * warningCount(): counts the problems that were noted but did not stop a row loading.
     * Passed in:  nothing
     * Passed out: number of WARNING problems
     */
    public long warningCount()
    {
        return problems.stream().filter(problem -> problem.severity() == Problem.Severity.WARNING).count();
    }
}

package golfanalyst.print;

import golfanalyst.Round;
import golfanalyst.Stats;
import golfanalyst.Tee;

/*
 +--------------------------------------------------------------------------+
 | RoundFigures                                                             |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | The figures of one round that the reports print, worked out from the     |
 | scores, putts, pars and ticks with Home's definitions (Stats): fairways  |
 | hit of the par 4s and 5s played, greens in regulation of the holes       |
 | played, holes with two putts or fewer, holes in a bunker and the sand    |
 | saves among them. Nothing is stored.                                     |
 |                                                                          |
 | Fields:                                                                  |
 | holes        - holes played (18 or 9)                                    |
 | par          - par of the holes played                                   |
 | score, putts - the totals                                                |
 | fairways, fairwayChances - fairways hit, and par 4s and 5s played        |
 | greens       - greens in regulation                                      |
 | twoPutts     - holes with two putts or fewer                             |
 | bunkers, sandSaves - holes in a bunker, and how many were saved          |
 |                                                                          |
 | Public Methods:                                                          |
 | of(round, tee) - works the figures out                                   |
 +--------------------------------------------------------------------------+
 */
public record RoundFigures(int holes, int par, int score, int putts, int fairways, int fairwayChances, int greens,
                           int twoPutts, int bunkers, int sandSaves)
{

    /*
     +--------------------------------------------------------------------------+
     | of(round, tee)                                                           |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Works out the round's figures hole by hole.                              |
     |                                                                          |
     | Passed in:  round - the round                                            |
     |             tee   - the tees it was played on                            |
     | Passed out: the figures                                                  |
     +--------------------------------------------------------------------------+
     */
    public static RoundFigures of(Round round, Tee tee)
    {
        int par = 0;
        int fairways = 0;
        int chances = 0;
        int greens = 0;
        int twoPutts = 0;
        int bunkers = 0;
        int saves = 0;
        for (int hole = 0; hole < round.holes(); hole = hole + 1)
        {
            int holePar = tee.pars()[round.startHole() + hole];
            int score = round.scores()[hole];
            int putts = round.putts()[hole];
            par = par + holePar;
            if (holePar > 3)
            {
                chances = chances + 1;
                if (round.fairway()[hole])
                {
                    fairways = fairways + 1;
                }
            }
            if (Stats.isGreenInRegulation(holePar, score, putts))
            {
                greens = greens + 1;
            }
            if (putts <= 2)
            {
                twoPutts = twoPutts + 1;
            }
            if (round.sand()[hole])
            {
                bunkers = bunkers + 1;
                if (Stats.isSandSave(holePar, score))
                {
                    saves = saves + 1;
                }
            }
        }
        return new RoundFigures(round.holes(), par, round.totalScore(), round.totalPutts(), fairways, chances, greens, twoPutts,
                bunkers, saves);
    }
}

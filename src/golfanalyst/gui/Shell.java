package golfanalyst.gui;

import golfanalyst.DataStore;

/*
 +--------------------------------------------------------------------------+
 | Shell                                                                    |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | What a screen is allowed to ask of the window it sits in. Screens talk   |
 | only to this interface, never to the window itself, so a screen can be   |
 | built and tested without opening a window (the window is a JFrame, which |
 | cannot exist on a computer with no display).                             |
 |                                                                          |
 | Public Methods:                                                          |
 | store()          - the loaded data                                       |
 | setStatus(text)  - shows a message in the footer                         |
 | refreshTitle()   - asks the header to show the current screen's title    |
 | goHome()         - switches to the Home screen                           |
 | newRound()       - opens the round screen ready to enter a new round     |
 | browseRounds()   - opens the round screen at the latest saved round      |
 | editRound()      - opens the round viewer ready to pick a round to edit  |
 | newCourse()      - opens the screen for adding a course and its tees     |
 | askYesNo(question) - asks a Yes / No question                            |
 | readyToSave()    - asks, on the sample data, whether to save there       |
 +--------------------------------------------------------------------------+
 */
public interface Shell
{

    /*
     * store(): the loaded data folder, handicap and statistics.
     */
    DataStore store();

    /*
     * setStatus(text): shows a message in the footer.
     * Passed in:  text - the message
     * Passed out: nothing
     */
    void setStatus(String text);

    /*
     * refreshTitle(): asks the header to show the current screen's title again, after it changed.
     */
    void refreshTitle();

    /*
     * goHome(): switches to the Home screen.
     */
    void goHome();

    /*
     * newRound(): opens the round screen ready to enter a new round.
     */
    void newRound();

    /*
     * browseRounds(): opens the round screen showing the latest saved round.
     */
    void browseRounds();

    /*
     * editRound(): opens the round viewer at the latest round, with its Edit button, so a round can be picked and edited.
     */
    void editRound();

    /*
     * newCourse(): opens the New Course screen, empty and ready to type in.
     */
    void newCourse();

    /*
     * askYesNo(question): asks a Yes / No question, for example "Discard your changes?".
     * Passed in:  question - the words to ask
     * Passed out: true for Yes
     */
    boolean askYesNo(String question);

    /*
     +--------------------------------------------------------------------------+
     | readyToSave()                                                            |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Called by a screen just before it writes to the data files. On the       |
     | golfer's own data it simply says yes. On the sample data it asks         |
     | whether to make the golfer's own data folder first.                      |
     |                                                                          |
     | Passed in:  nothing                                                      |
     | Passed out: true to go ahead and save, false to save nothing             |
     +--------------------------------------------------------------------------+
     */
    boolean readyToSave();
}

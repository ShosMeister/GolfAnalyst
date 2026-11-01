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
 | confirmTwice(question, keepLabel, goLabel, word) - the two careful       |
 |                    questions before something is removed                 |
 | confirmOnce(question, keepLabel, goLabel) - one careful question         |
 | clearData()      - Clear: asks what to clear, twice, then clears it      |
 | importData()     - Import: checks the files, then adds what is new       |
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
     * browseRoundsOn(teeId): opens the round screen on the newest round played on those tees, with the Show box set to them.
     */
    void browseRoundsOn(int teeId);

    /*
     * statsBoard(): opens the Stats Board.
     */
    void statsBoard();

    /*
     * bestRound(): opens the Best Round screen on the course and tees of the latest round.
     */
    void bestRound();

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

    /*
     +--------------------------------------------------------------------------+
     | confirmTwice(question, keepLabel, goLabel, word)                         |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The two careful questions before something is removed: first the         |
     | question with a Keep button (the default) and a Go button, then "To      |
     | confirm, type WORD." with a box whose OK stays off until it holds the    |
     | word.                                                                    |
     |                                                                          |
     | Passed in:  question  - the first question                               |
     |             keepLabel - e.g. "Keep it", goLabel - e.g. "Delete..."       |
     |             word      - the word to type, e.g. "DELETE"                  |
     | Passed out: true only if both were answered yes                          |
     +--------------------------------------------------------------------------+
     */
    boolean confirmTwice(String question, String keepLabel, String goLabel, String word);

    /*
     * confirmOnce(question, keepLabel, goLabel): one careful question with a Keep button (the default) and a Go button, e.g. "Save anyway?" with "Go back".
     * Passed in:  question, keepLabel, goLabel - the words
     * Passed out: true only if the Go button was pressed
     */
    boolean confirmOnce(String question, String keepLabel, String goLabel);

    /*
     * clearData(): what Home's Clear button does: asks what to clear and confirms it twice, makes the backup folder, clears, and goes Home.
     */
    void clearData();

    /*
     * importData(): what Home's Import button does: the Import window, a check of the files, then (after a backup) the new rounds and courses added.
     */
    void importData();
}

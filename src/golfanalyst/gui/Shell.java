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
 | confirmTwice(...), confirmOnce(...) - the careful questions              |
 | showNotice(title, message, list), askText(title, question, initial)      |
 | clearData()      - Clear: asks what to clear, twice, then clears it      |
 | importData()     - Import: checks the files, then adds what is new       |
 | openSettings(), exportData() - the Settings and Export windows           |
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
     * analyzer(): opens the Analyzer on the course and tees of the latest round, showing Counts.
     */
    void analyzer();

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
     | confirmTwice(title, question, list, keepLabel, goLabel, prompt, word)    |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The two careful questions before something is removed: first the         |
     | question (with a scrolling list under it when list is not null), a Keep  |
     | button (the default) and a Go button; then the prompt with a box whose   |
     | OK stays off until it holds the word (capitals and extra spaces          |
     | ignored).                                                                |
     |                                                                          |
     | Passed in:  title     - the words in the first dialog's band             |
     |             question  - the first question                               |
     |             list      - lines to show under it, or null                  |
     |             keepLabel - e.g. "Keep it", goLabel - e.g. "Delete..."       |
     |             prompt    - e.g. "To confirm, type DELETE."                  |
     |             word      - the word to type, e.g. "DELETE"                  |
     | Passed out: true only if both were answered yes                          |
     +--------------------------------------------------------------------------+
     */
    boolean confirmTwice(String title, String question, String list, String keepLabel, String goLabel, String prompt, String word);

    /*
     * confirmOnce(title, question, keepLabel, goLabel): one careful question with a Keep button (the default) and a Go button, e.g. "Save anyway?" with "Go back".
     * Passed in:  title - the words in the band; question, keepLabel, goLabel - the words
     * Passed out: true only if the Go button was pressed
     */
    boolean confirmOnce(String title, String question, String keepLabel, String goLabel);

    /*
     * showNotice(title, message, list): a message with OK (and a scrolling list when list is not null), e.g. "Save failed".
     * Passed in:  title - the words in the band, message - the words, list - lines under it or null
     * Passed out: nothing
     */
    void showNotice(String title, String message, String list);

    /*
     * askText(title, question, initial): a question with one box to type in, e.g. the golfer's name.
     * Passed in:  title - the words in the band, question - the words, initial - what the box starts with
     * Passed out: what was typed, or null if Cancel was pressed
     */
    String askText(String title, String question, String initial);

    /*
     * clearData(): what Home's Clear button does: asks what to clear and confirms it twice, makes the backup folder, clears, and goes Home.
     */
    void clearData();

    /*
     * importData(): what Home's Import button does: the Import window, a check of the files, then (after a backup) the new rounds and courses added.
     */
    void importData();

    /*
     * openSettings(): what Home's Settings button does: the Settings window, saved on OK.
     */
    void openSettings();

    /*
     * exportData(): what Home's Export button does: the Export window, then a copy of the chosen courses and rounds in a new folder.
     */
    void exportData();
}

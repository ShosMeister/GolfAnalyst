package golfanalyst.gui;

/*
 +--------------------------------------------------------------------------+
 | Page                                                                     |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | The base of every screen. A page is the middle section of the window: the|
 | header above it and the footer below it never change, only the page.     |
 | Each page has its own background photo, a title for the header, and a    |
 | name that picks its Info and Help text files.                            |
 |                                                                          |
 | What a subclass provides:                                                |
 | title()    - the words for the header                                    |
 | helpKey()  - the start of its help file names, e.g. "home" for           |
 |              home-info.txt and home-help.txt                             |
 | onShow()   - called each time the page is shown, to refresh its values   |
 +--------------------------------------------------------------------------+
 */
@SuppressWarnings({"serial", "this-escape"})
public abstract class Page extends BackdropPanel
{

    protected final transient Shell shell;

    /*
     * Page(shell, backdropName): sets up the page with its background photo.
     * Passed in:  shell - the window it sits in, backdropName - "home", "round", ...
     * Passed out: the page
     */
    protected Page(Shell shell, String backdropName)
    {
        this.shell = shell;
        setBackdrop(Theme.backdrop(backdropName));
    }

    /*
     * title(): the words to show in the header while this page is showing.
     */
    public abstract String title();

    /*
     * helpKey(): the start of this page's help file names.
     */
    public abstract String helpKey();

    /*
     * onShow(): refreshes the page each time it is shown.
     */
    public abstract void onShow();
}

package golfanalyst.gui;

import golfanalyst.print.Report;
import golfanalyst.print.Thumbnail;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JComponent;

/*
 +--------------------------------------------------------------------------+
 | PagePreview                                                              |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | The small picture of the page in the Print window, 188 x 150: page 1 of  |
 | the real report drawn by Thumbnail (the report's own drawing code,       |
 | shrunk), or a blank page saying "Nothing to print yet". The window sets  |
 | a new report whenever a choice changes.                                  |
 |                                                                          |
 | Public Methods:                                                          |
 | setReport(report) - shows another report (null for nothing)              |
 | report()          - the report shown                                     |
 +--------------------------------------------------------------------------+
 */
@SuppressWarnings("serial")
final class PagePreview extends JComponent
{

    static final int PREVIEW_WIDTH = 188;
    static final int PREVIEW_HEIGHT = 150;
    private transient Report report = null;

    /*
     * PagePreview(): an empty picture, 188 x 150.
     */
    PagePreview()
    {
        Dimension size = new Dimension(PREVIEW_WIDTH, PREVIEW_HEIGHT);
        setPreferredSize(size);
        setMinimumSize(size);
        setMaximumSize(size);
        setOpaque(false);
    }

    /*
     * setReport(newReport): shows page 1 of another report (null for nothing) and redraws.
     */
    void setReport(Report newReport)
    {
        report = newReport;
        repaint();
    }

    /*
     * report(): the report shown, or null.
     */
    Report report()
    {
        return report;
    }

    /*
     * paintComponent(graphics): the shrunken page, centred, with its thin grey outline.
     */
    @Override
    protected void paintComponent(Graphics graphics)
    {
        Thumbnail.draw((Graphics2D) graphics, report, 0, 0, getWidth(), getHeight());
    }
}

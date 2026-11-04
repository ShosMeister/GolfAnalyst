package golfanalyst.gui;

import golfanalyst.DataStore;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/*
 +--------------------------------------------------------------------------+
 | PictureCheck                                                             |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | Checks that every page starts and finds its own background picture: the  |
 | window is built (without opening it) on a data folder with the pictures  |
 | of one images folder, every page is shown and drawn, and each must have  |
 | its photo (home, round, course, stats, bestround, analyzer). The screen  |
 | self-test runs it on the project's images; tools\make-installer.bat runs |
 | it with the built program's own Java, jar and files                      |
 | (golfanalyst.gui.App --check-pictures), so a build that lost a picture   |
 | fails.                                                                   |
 |                                                                          |
 | Public Methods:                                                          |
 | PAGE_PICTURES - each page and the picture it must find                   |
 | problems(images, data, help) - what is wrong, one line each (empty when  |
 |                                every page has its picture)               |
 +--------------------------------------------------------------------------+
 */
final class PictureCheck
{

    static final Map<String, String> PAGE_PICTURES = pagePictures();

    private PictureCheck()
    {
    }

    /*
     * pagePictures(): each page (its card) and the background picture it must find, in the order they are shown.
     */
    private static Map<String, String> pagePictures()
    {
        Map<String, String> pictures = new LinkedHashMap<>();
        pictures.put(AppPanel.HOME, "home");
        pictures.put(AppPanel.ROUND, "round");
        pictures.put(AppPanel.COURSE, "course");
        pictures.put(AppPanel.STATS, "stats");
        pictures.put(AppPanel.BEST, "bestround");
        pictures.put(AppPanel.ANALYZER, "analyzer");
        return pictures;
    }

    /*
     +--------------------------------------------------------------------------+
     | problems(images, data, help)                                             |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Builds the window with the pictures in images on the data folder, shows  |
     | and draws every page, and lists each page that failed to start or has    |
     | no photo of its own. The images folder in use before is put back.        |
     |                                                                          |
     | Passed in:  images - the images folder to check                          |
     |             data   - a data folder to open (the sample data)             |
     |             help   - the help folder                                     |
     | Passed out: the problems, one line each (empty when every page is right) |
     +--------------------------------------------------------------------------+
     */
    static List<String> problems(Path images, Path data, Path help)
    {
        List<String> found = new ArrayList<>();
        Path before = Theme.imagesFolder();
        try
        {
            Theme.load(images);
            AppPanel panel = new AppPanel(new DataStore(data), help, "");
            panel.setSize(AppFrame.WINDOW_WIDTH, AppFrame.WINDOW_HEIGHT);
            for (Map.Entry<String, String> page : PAGE_PICTURES.entrySet())
            {
                try
                {
                    panel.navigate(page.getKey());
                    panel.doLayout();
                    BufferedImage drawn = new BufferedImage(AppFrame.WINDOW_WIDTH, AppFrame.WINDOW_HEIGHT, BufferedImage.TYPE_INT_RGB);
                    Graphics2D pen = drawn.createGraphics();
                    panel.paint(pen);
                    pen.dispose();
                    if (Theme.backdrop(page.getValue()) == null || !panel.page(page.getKey()).hasBackdrop())
                    {
                        found.add("The " + page.getKey() + " page has no background picture (" + page.getValue() + " in "
                                + images.resolve("backgrounds") + ").");
                    }
                }
                catch (RuntimeException failure)
                {
                    found.add("The " + page.getKey() + " page did not start: " + failure);
                }
            }
        }
        catch (IOException | RuntimeException failure)
        {
            found.add("The window could not be built: " + failure);
        }
        finally
        {
            Theme.load(before);
        }
        return found;
    }
}

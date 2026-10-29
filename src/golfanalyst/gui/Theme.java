package golfanalyst.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

/*
 +--------------------------------------------------------------------------+
 | Theme                                                                    |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | The look of the program in one place: the colours, fonts and sizes, the  |
 | pictures (wood textures, buttons, backgrounds) and the small drawing     |
 | helpers every custom control uses. To change the look, change it here.   |
 |                                                                          |
 | Pictures are read from the images folder the first time they are asked   |
 | for and then remembered. A picture that is missing is not an error: the  |
 | controls fall back to plain colours, so the program still opens.         |
 |                                                                          |
 | Backgrounds are looked up by page name in images/backgrounds, for        |
 | example home.gif or round.jpg. Replace a file to change a page's photo.  |
 |                                                                          |
 | Public Methods:                                                          |
 | load(dir), image(name), backdrop(page) - pictures                        |
 | font(style, size)                      - the program's font              |
 | smooth(graphics)                       - turns on smooth drawing         |
 | drawCentered(...), paintStretched(...), paintBevel(...) - drawing helpers|
 +--------------------------------------------------------------------------+
 */
public final class Theme
{

    public static final Color TEXT_LIGHT = Color.WHITE;
    public static final Color TEXT_HOVER = new Color(255, 60, 60);
    public static final Color TEXT_DISABLED = new Color(165, 155, 145);
    public static final Color TEXT_SHADOW = new Color(0, 0, 0, 200);
    public static final Color TEXT_ON_LIGHT_WOOD = new Color(60, 30, 5);
    public static final Color GOLD = new Color(255, 214, 0);
    /** Colour of the caption text on the light wood tags (Date, Score, Rating, Eagles ...). To try white captions set this to Color.WHITE and TAG_TEXT_SHADOW to true. */
    public static final Color TAG_TEXT = TEXT_ON_LIGHT_WOOD;
    /** Whether caption text gets a dark shadow behind it. Helps if TAG_TEXT is a light colour. */
    public static final boolean TAG_TEXT_SHADOW = false;
    public static final Color PANEL_BODY = new Color(240, 240, 240);
    public static final Color FIELD_FACE = new Color(228, 228, 228);
    public static final Color FIELD_TEXT = Color.BLACK;
    public static final Color FIELD_ERROR = new Color(255, 200, 200);
    /** The pale cream fill of the spreadsheet cell that has the cursor (New Course). */
    public static final Color FIELD_FOCUS = new Color(255, 248, 220);
    /** The parchment behind the text of the Help and Info windows. */
    public static final Color HELP_PAPER = new Color(247, 241, 227);
    /** The dark brown of the section headings in the Help and Info windows. */
    public static final Color HELP_HEADING = new Color(100, 50, 8);
    /** The thin lines between spreadsheet cells (New Course). */
    public static final Color GRID_LINE = new Color(185, 180, 170);
    public static final Color FRAME_DARK = new Color(70, 40, 10);
    public static final Color WOOD_FALLBACK = new Color(120, 70, 25);
    public static final Color BACKDROP_FALLBACK = new Color(58, 96, 52);
    public static final int HEADER_HEIGHT = 76;
    public static final int FOOTER_HEIGHT = 30;
    public static final int WOOD_BUTTON_WIDTH = 140;
    public static final int WOOD_BUTTON_HEIGHT = 40;
    public static final int ICON_SIZE = 56;
    public static final int NAV_ICON_SIZE = 46;
    /** The version. The second pair of digits: .0x is a bug fix, .x0 is a new feature. */
    public static final String VERSION = "4.40";
    public static final String VERSION_TEXT = "Golf Analyst v" + VERSION;
    /** The picture stretched across the header. Change this name to use a different wood. */
    public static final String HEADER_WOOD = "Wood_Light.gif";

    private static final Map<String, BufferedImage> IMAGES = new HashMap<>();
    private static final String[] BACKDROP_EXTENSIONS = {"jpg", "jpeg", "png", "gif"};
    private static Path imagesDir = Path.of("images");

    private Theme()
    {
    }

    /*
     * load(dir): sets the folder pictures are read from and forgets any pictures already read.
     * Passed in:  dir - the images folder
     * Passed out: nothing
     */
    public static void load(Path dir)
    {
        imagesDir = dir;
        IMAGES.clear();
    }

    /*
     +--------------------------------------------------------------------------+
     | image(fileName)                                                          |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Gets a picture from the images folder, reading it the first time.        |
     |                                                                          |
     | Passed in:  fileName - the picture's name inside the images folder,      |
     |                        for example "Home.png" or "backgrounds/home.gif"  |
     | Passed out: the picture, or null if there is no such file or it          |
     |             cannot be read                                               |
     +--------------------------------------------------------------------------+
     */
    public static BufferedImage image(String fileName)
    {
        if (!IMAGES.containsKey(fileName))
        {
            BufferedImage loaded = null;
            Path file = imagesDir.resolve(fileName);
            if (Files.exists(file))
            {
                try
                {
                    loaded = ImageIO.read(file.toFile());
                }
                catch (IOException unreadable)
                {
                    loaded = null;
                }
            }
            IMAGES.put(fileName, loaded);
        }
        return IMAGES.get(fileName);
    }

    /*
     * backdrop(pageName): finds the background photo for a page.
     * Passed in:  pageName - "home", "round", "stats" ...
     * Passed out: the picture from images/backgrounds, or null if there is none
     */
    public static BufferedImage backdrop(String pageName)
    {
        for (String extension : BACKDROP_EXTENSIONS)
        {
            BufferedImage found = image("backgrounds/" + pageName + "." + extension);
            if (found != null)
            {
                return found;
            }
        }
        return null;
    }

    /*
     * font(style, size): the font used everywhere, so the look can be changed in one place.
     * Passed in:  style - Font.PLAIN, Font.BOLD, Font.ITALIC (or both), size - points
     * Passed out: the font
     */
    public static Font font(int style, float size)
    {
        return new Font(Font.SANS_SERIF, style, 12).deriveFont(size);
    }

    /*
     * smooth(graphics): turns on smooth edges for shapes, text and scaled pictures.
     * Passed in:  graphics - what is being drawn on
     * Passed out: nothing
     */
    public static void smooth(Graphics2D graphics)
    {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    /*
     +--------------------------------------------------------------------------+
     | drawCentered(graphics, text, x, y, width, height, color, shadow)         |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | Draws text in the middle of a rectangle, using the font already set      |
     | on the graphics. A dark shadow one pixel down and to the right makes     |
     | light text readable on any wood or photo.                                |
     |                                                                          |
     | Passed in:  graphics     - what is being drawn on (font already set)     |
     |             text         - the words to draw                             |
     |             x, y         - top left of the rectangle                     |
     |             width, height - size of the rectangle                        |
     |             color        - text colour                                   |
     |             shadow       - true to draw the shadow                       |
     | Passed out: nothing                                                      |
     +--------------------------------------------------------------------------+
     */
    public static void drawCentered(Graphics2D graphics, String text, int x, int y, int width, int height,
                                    Color color, boolean shadow)
    {
        FontMetrics metrics = graphics.getFontMetrics();
        int textX = x + (width - metrics.stringWidth(text)) / 2;
        int textY = y + (height - metrics.getHeight()) / 2 + metrics.getAscent();
        if (shadow)
        {
            graphics.setColor(TEXT_SHADOW);
            graphics.drawString(text, textX + 1, textY + 1);
        }
        graphics.setColor(color);
        graphics.drawString(text, textX, textY);
    }

    /*
     * paintStretched(graphics, picture, width, height, fallback): draws a picture stretched over a whole area, in one piece (no tiling, so no seams).
     * Passed in:  graphics - what is being drawn on, picture - the picture (may be null),
     *             width, height - the area, fallback - colour used if the picture is null
     * Passed out: nothing
     */
    public static void paintStretched(Graphics2D graphics, BufferedImage picture, int width, int height, Color fallback)
    {
        if (picture == null)
        {
            graphics.setColor(fallback);
            graphics.fillRect(0, 0, width, height);
            return;
        }
        graphics.drawImage(picture, 0, 0, width, height, null);
    }

    /*
     * paintBevel(graphics, width, height, raised): draws a two-pixel 3D edge so an area looks raised or sunken.
     * Passed in:  graphics - what is being drawn on, width, height - the area, raised - true for raised, false for sunken
     * Passed out: nothing
     */
    public static void paintBevel(Graphics2D graphics, int width, int height, boolean raised)
    {
        Color light = new Color(255, 255, 255, 120);
        Color dark = new Color(0, 0, 0, 140);
        graphics.setStroke(new BasicStroke(1f));
        for (int edge = 0; edge < 2; edge = edge + 1)
        {
            graphics.setColor(raised ? light : dark);
            graphics.drawLine(edge, edge, width - 1 - edge, edge);
            graphics.drawLine(edge, edge, edge, height - 1 - edge);
            graphics.setColor(raised ? dark : light);
            graphics.drawLine(edge, height - 1 - edge, width - 1 - edge, height - 1 - edge);
            graphics.drawLine(width - 1 - edge, edge, width - 1 - edge, height - 1 - edge);
        }
    }
}

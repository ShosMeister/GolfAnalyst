import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/*
 +--------------------------------------------------------------------------+
 | MakeIcon                                                                 |
 +--------------------------------------------------------------------------+
 | Purpose:                                                                 |
 | A tool, not part of the program: makes the installer's Windows icon      |
 | (a multi-size .ico) from the picture the window's title bar uses,        |
 | images\Home.png. Each size is stored as a PNG inside the .ico, which     |
 | Windows Vista and later read. The sizes smaller than the picture are     |
 | shrunk in halving steps so they stay sharp; the picture is only 80 x 80, |
 | so 256 is enlarged and a little soft.                                    |
 | Run from the project folder (Java 17 runs a single source file):         |
 |   java tools\MakeIcon.java images\Home.png installer\GolfAnalyst.ico     |
 |                                                                          |
 | Public Methods:                                                          |
 | main(args) - args[0] the picture, args[1] the .ico to write              |
 +--------------------------------------------------------------------------+
 */
public final class MakeIcon
{

    static final int[] SIZES = {16, 20, 24, 32, 40, 48, 64, 80, 256};
    static final int HEADER_BYTES = 6;
    static final int ENTRY_BYTES = 16;

    private MakeIcon()
    {
    }

    /*
     * main(args): reads the picture, makes every size and writes the .ico.
     */
    public static void main(String[] args) throws IOException
    {
        if (args.length < 2)
        {
            System.out.println("Use: java tools\\MakeIcon.java images\\Home.png installer\\GolfAnalyst.ico");
            return;
        }
        BufferedImage source = ImageIO.read(Path.of(args[0]).toFile());
        List<byte[]> pictures = new ArrayList<>();
        for (int size : SIZES)
        {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            ImageIO.write(scaled(source, size), "png", bytes);
            pictures.add(bytes.toByteArray());
        }
        Files.write(Path.of(args[1]), icoFile(pictures));
        System.out.println("Wrote " + args[1] + " with sizes " + java.util.Arrays.toString(SIZES) + " from a " + source.getWidth() + " x "
                + source.getHeight() + " picture.");
    }

    /*
     +--------------------------------------------------------------------------+
     | scaled(source, size)                                                     |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The picture at size x size: halved step by step while it is more than   |
     | twice as big (so no detail is skipped), then drawn at the size with      |
     | smooth (bicubic) scaling.                                                |
     |                                                                          |
     | Passed in:  source - the picture, size - the width and height wanted     |
     | Passed out: the new picture, with its see-through parts kept             |
     +--------------------------------------------------------------------------+
     */
    static BufferedImage scaled(BufferedImage source, int size)
    {
        BufferedImage current = source;
        while (current.getWidth() / 2 >= size)
        {
            current = draw(current, current.getWidth() / 2);
        }
        return draw(current, size);
    }

    /*
     * draw(picture, size): the picture drawn into a new see-through square of that size, smoothly.
     */
    static BufferedImage draw(BufferedImage picture, int size)
    {
        BufferedImage result = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D pen = result.createGraphics();
        pen.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        pen.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        pen.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        pen.drawImage(picture, 0, 0, size, size, null);
        pen.dispose();
        return result;
    }

    /*
     +--------------------------------------------------------------------------+
     | icoFile(pictures)                                                        |
     +--------------------------------------------------------------------------+
     | Purpose:                                                                 |
     | The bytes of an .ico file: a 6-byte header (type 1 = icon, the count),   |
     | one 16-byte entry per picture (width and height, 0 meaning 256, 32       |
     | bits a pixel, the size and where its bytes start), then the pictures.    |
     |                                                                          |
     | Passed in:  pictures - the PNG bytes, in the order of SIZES              |
     | Passed out: the file's bytes                                             |
     +--------------------------------------------------------------------------+
     */
    static byte[] icoFile(List<byte[]> pictures)
    {
        int total = HEADER_BYTES + ENTRY_BYTES * pictures.size();
        for (byte[] picture : pictures)
        {
            total = total + picture.length;
        }
        ByteBuffer file = ByteBuffer.allocate(total).order(ByteOrder.LITTLE_ENDIAN);
        file.putShort((short) 0);
        file.putShort((short) 1);
        file.putShort((short) pictures.size());
        int offset = HEADER_BYTES + ENTRY_BYTES * pictures.size();
        for (int lp = 0; lp < pictures.size(); lp = lp + 1)
        {
            int side = SIZES[lp];
            if (side >= 256)
            {
                side = 0;
            }
            file.put((byte) side);
            file.put((byte) side);
            file.put((byte) 0);
            file.put((byte) 0);
            file.putShort((short) 1);
            file.putShort((short) 32);
            file.putInt(pictures.get(lp).length);
            file.putInt(offset);
            offset = offset + pictures.get(lp).length;
        }
        for (byte[] picture : pictures)
        {
            file.put(picture);
        }
        return file.array();
    }
}

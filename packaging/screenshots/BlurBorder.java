import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** Blur the outer 5 pixels. Those pixels are the desktop behind the window, so the frame is filled from the interior before the blur. */
public final class BlurBorder {
    static final int FRAME = 5;
    static final int RADIUS = 2;

    public static void main(String[] args) throws IOException {
        if (args.length == 1 && "--self-check".equals(args[0])) {
            selfCheck();
            return;
        }
        if (args.length == 0) {
            System.err.println("usage: BlurBorder.java --self-check | <png>...");
            System.exit(2);
        }
        for (String arg : args) {
            Path path = Path.of(arg);
            BufferedImage image = ImageIO.read(path.toFile());
            if (image == null) {
                throw new IOException("not a png: " + arg);
            }
            ImageIO.write(blurFrame(image), "png", path.toFile());
            System.out.println("blurred " + arg);
        }
    }

    static BufferedImage blurFrame(BufferedImage source) {
        int w = source.getWidth();
        int h = source.getHeight();
        int x0 = Math.min(FRAME, w / 2);
        int y0 = Math.min(FRAME, h / 2);
        int x1 = Math.max(x0, w - 1 - FRAME);
        int y1 = Math.max(y0, h - 1 - FRAME);
        BufferedImage filled = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            int sy = Math.min(Math.max(y, y0), y1);
            for (int x = 0; x < w; x++) {
                int sx = Math.min(Math.max(x, x0), x1);
                filled.setRGB(x, y, source.getRGB(sx, sy));
            }
        }
        BufferedImage blurred = box(filled, RADIUS);
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                boolean frame = x < FRAME || y < FRAME || x >= w - FRAME || y >= h - FRAME;
                out.setRGB(x, y, frame ? blurred.getRGB(x, y) : source.getRGB(x, y));
            }
        }
        return out;
    }

    static BufferedImage box(BufferedImage src, int radius) {
        int w = src.getWidth();
        int h = src.getHeight();
        int[] pix = src.getRGB(0, 0, w, h, null, 0, w);
        int[] tmp = new int[pix.length];
        blur(pix, tmp, w, h, radius, true);
        blur(tmp, pix, w, h, radius, false);
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        out.setRGB(0, 0, w, h, pix, 0, w);
        return out;
    }

    static void blur(int[] src, int[] dst, int w, int h, int radius, boolean horizontal) {
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int a = 0;
                int r = 0;
                int g = 0;
                int b = 0;
                int n = 0;
                for (int k = -radius; k <= radius; k++) {
                    int sx = horizontal ? x + k : x;
                    int sy = horizontal ? y : y + k;
                    if (sx < 0 || sy < 0 || sx >= w || sy >= h) {
                        continue;
                    }
                    int p = src[sy * w + sx];
                    a += (p >>> 24) & 255;
                    r += (p >>> 16) & 255;
                    g += (p >>> 8) & 255;
                    b += p & 255;
                    n++;
                }
                dst[y * w + x] = (a / n << 24) | (r / n << 16) | (g / n << 8) | (b / n);
            }
        }
    }

    static void selfCheck() {
        int w = 40;
        int h = 30;
        BufferedImage src = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        int red = 0xffff0000;
        int blue = 0xff0000ff;
        int white = 0xffffffff;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                src.setRGB(x, y, red);
            }
        }
        for (int y = FRAME; y < h - FRAME; y++) {
            for (int x = FRAME; x < w - FRAME; x++) {
                src.setRGB(x, y, blue);
            }
        }
        src.setRGB(20, 15, white);
        BufferedImage once = blurFrame(src);
        BufferedImage twice = blurFrame(once);
        if (once.getRGB(20, 15) != white) {
            fail("interior changed");
        }
        if (once.getRGB(5, 15) != blue) {
            fail("inner edge changed");
        }
        if ((once.getRGB(0, 0) & 0xffffff) == 0xff0000) {
            fail("desktop leak stayed in the corner");
        }
        if ((once.getRGB(4, 15) & 0xffffff) == 0xff0000) {
            fail("desktop leak stayed on the frame");
        }
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (once.getRGB(x, y) != twice.getRGB(x, y)) {
                    fail("second blur changed the frame");
                }
            }
        }
        System.out.println("ok");
    }

    static void fail(String message) {
        System.err.println(message);
        System.exit(1);
    }
}

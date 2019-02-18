import Catalano.Imaging.FastBitmap;
import Catalano.Imaging.Tools.IntegralImage;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Tests {

    private String path = "./test-res/";

    @Test
    public void testImageRead() {
        FastBitmap[] bitmaps = {};
        try {
            bitmaps = FaceRecognition.readImagesFromDataBase(path); // Read images
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Correctly reads all images
        assertEquals(bitmaps.length, new File(path).listFiles().length);
    }

    @Test
    public void testIntegralImages() {
        FastBitmap blackFB = readImage(path + "black-25px.png");
        FastBitmap whiteFB = readImage(path + "black-25px.png");
        FastBitmap faceFB = readImage(path + "000.png");
        FastBitmap[] fbs = new FastBitmap[]{blackFB, whiteFB, faceFB};
        IntegralImage[] iis = FaceRecognition.convertToIntegralImages(fbs);

        IntegralImage black = iis[0];
        IntegralImage white = iis[1];
        IntegralImage face = iis[2];

        // Test black
        System.out.println("== BLACK ==");
        System.out.println(Arrays.deepToString(blackFB.toMatrixGrayAsInt())); // Expecting this to be all 0. Is 1...
        System.out.println(Arrays.deepToString(black.getInternalData())); // Consistent with above.


        //Test white
        System.out.println("== WHITE ==");
        System.out.println(Arrays.deepToString(whiteFB.toMatrixGrayAsInt())); // Expecting this to be all 255. Is 1..
        System.out.println(Arrays.deepToString(white.getInternalData())); // Consistent with above.

        //Test face
        System.out.println("== FACE ==");
        System.out.println(Arrays.deepToString(faceFB.toMatrixGrayAsInt())); // Seems reasonable.
        System.out.println(Arrays.deepToString(face.getInternalData()));
    }

    public FastBitmap readImage(String path) {
        File file = new File(path);
        FastBitmap fb = new FastBitmap();
        try {
            fb = new FastBitmap(ImageIO.read(file));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return fb;
    }
}

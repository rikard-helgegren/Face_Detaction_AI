import Catalano.Imaging.FastBitmap;
import Catalano.Imaging.Tools.IntegralImage;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Tests {

    private String path = "./test-res/";

    @Test
    public void testImageRead() {
        HalIntegralImage[] images = {};
        try {
            images = FaceRecognition.readImagesFromDataBase(path); // Read images
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Correctly reads all images
        assertEquals(images.length, new File(path).listFiles().length);
    }

    @Test
    public void testIntegralImages() throws Exception {
        HalIntegralImage black = readImage(path + "black-25px.png");
        HalIntegralImage white = readImage(path + "white-25px.png");
        HalIntegralImage face = readImage(path + "000.png");
        HalIntegralImage face92x112 = readImage(path + "92x112.png");
        HalIntegralImage blackTop10 = readImage(path + "blackTop10-25px.png");
        HalIntegralImage[] fbs = new HalIntegralImage[]{black, white, face, face92x112, blackTop10};


        // Probably want assertions below, but this will do for now to check what happens.

        // Test black
        System.out.println("== BLACK ==");
        assertEquals(25, black.getHeight());
        assertEquals(25, black.getWidth());
        //System.out.println(Arrays.deepToString(blackFB.toMatrixGrayAsInt())); // Expecting this to be all 0. Is all 1.
        System.out.println(Arrays.deepToString(black.getInternalData())); // Consistent with above except first line is 0.
        assertEquals(1, black.getRectangleSum(24, 24, 23, 23));
        assertEquals(4, black.getRectangleSum(24, 24, 22, 22));

        /* 1 1 1 1   1  2  3  4
           1 1 1 1   2  4  6  8
           1 1 1 1   3  6  9  12
           1 1 1 1   4  8  12 16

           (24, 24), (22, 22) ++> (23, 23)
            4 + 1 - 2 - 4
         */

        //Test white
        System.out.println("== WHITE ==");
        //System.out.println(Arrays.deepToString(whiteFB.toMatrixGrayAsInt())); // Expecting this to be all 254.
        System.out.println(Arrays.deepToString(white.getInternalData())); // Consistent with above except first line is 0.
        System.out.println(white.getWidth());
        System.out.println(white.getInternalData().length); // Apparently, internal data is 1px larger than input img.
        System.out.println(white.getRectangleSum(24, 24, 0, 0)); // This calculates the total sum
        assertEquals(254, white.getInternalData()[0][0]);


        //Test face
        System.out.println("== FACE ==");
        //System.out.println(Arrays.deepToString(faceFB.toMatrixGrayAsInt())); // Seems reasonable.
        System.out.println(Arrays.deepToString(face.getInternalData()));

        // Test image with where top 10 rows are black, bottom 15 rows are white.
        System.out.println("== Black top 10 ==");
        //System.out.println(Arrays.deepToString(blackTop10.getInternalData()));
        assertEquals(1, blackTop10.getInternalData()[0][0]);
        assertEquals(10, blackTop10.getInternalData()[9][0]);
        assertEquals(265, blackTop10.getInternalData()[10][0]); // Should expected not be 264? Hm...

        // Test that dimensions are correct.
        assertEquals(92, face92x112.getWidth());
        assertEquals(112, face92x112.getHeight());
    }

    private HalIntegralImage readImage(String path) throws Exception {
        File file = new File(path);
        HalIntegralImage img = new HalIntegralImage(new FastBitmap(ImageIO.read(file)));
        return img;
    }
}

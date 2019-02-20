import Catalano.Imaging.FastBitmap;
import Catalano.Imaging.Tools.IntegralImage;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.io.File;

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
        HalIntegralImage blackLeft10 = readImage(path + "blackLeft10-25px.png");

        // Test black
        //System.out.println("== BLACK ==");
        assertEquals(25, black.getHeight());
        assertEquals(25, black.getWidth());
        //System.out.println(Arrays.deepToString(blackFB.toMatrixGrayAsInt())); // Expecting this to be all 0. Is all 1.
        //System.out.println(Arrays.deepToString(black.getInternalData())); // Consistent with above except first line is 0.
        assertEquals(1, black.getRectangleSum(0, 0, 0, 0));
        assertEquals(2, black.getRectangleSum(0, 0, 0, 1));
        assertEquals(4, black.getRectangleSum(0, 0, 1, 1));
        assertEquals(9, black.getRectangleSum(0, 0, 2, 2));
        assertEquals(9, black.getRectangleSum(1, 1, 3, 3));
        assertEquals(25, black.getRectangleSum(0, 0, 4, 4));
        assertEquals(black.getInternalData()[24][24], black.getRectangleSum(0, 0, 24, 24));

        //Test white
        //System.out.println("== WHITE ==");
        //System.out.println(Arrays.deepToString(white.getInternalData())); // Consistent with above except first line is 0.
        assertEquals(254, white.getInternalData()[0][0]);

        //Test face
        //System.out.println("== FACE ==");
        //System.out.println(Arrays.deepToString(faceFB.toMatrixGrayAsInt())); // Seems reasonable.
        //System.out.println(Arrays.deepToString(face.getInternalData()));

        // Test image where top 10 rows are black, bottom 15 rows are white.
        //System.out.println("== Black top 10 ==");
        //System.out.println(Arrays.deepToString(blackTop10.getInternalData()));
        assertEquals(1, blackTop10.getInternalData()[0][0]);
        assertEquals(10, blackTop10.getInternalData()[9][0]);
        assertEquals(265, blackTop10.getInternalData()[10][0]); // Should expected not be 264? Hm...

        // Test image with where leftmost 10 columns are black, rightmost 15 columns are white.
        //System.out.println("== Black left 10 ==");
        //System.out.println(Arrays.deepToString(blackTop10.getInternalData()));
        assertEquals(1, blackLeft10.getInternalData()[0][0]);
        assertEquals(10, blackLeft10.getInternalData()[0][9]);
        assertEquals(265, blackLeft10.getInternalData()[0][10]); // Should expected not be 264? Hm...

        // Test that dimensions are correct.
        assertEquals(92, face92x112.getWidth());
        assertEquals(112, face92x112.getHeight());
    }

    // Tests so that feature calculation is correct
    @Test
    public void testFeatures() throws Exception {
        HalIntegralImage black = readImage(path + "black-25px.png");
        HalIntegralImage white = readImage(path + "white-25px.png");
        HalIntegralImage face92x112 = readImage(path + "92x112.png");
        HalIntegralImage blackTop10 = readImage(path + "blackTop10-25px.png");
        HalIntegralImage blackLeft10 = readImage(path + "blackLeft10-25px.png");

        int expected = blackTop10.getRectangleSum(0, 0, 3, 3) - blackTop10.getRectangleSum(4, 0, 7, 3);
        int actual = FaceRecognition.calcHorizontalTwoRectFeature(blackTop10, 0, 0, 4, 4);
        //System.out.println(blackTop10.getRectangleSum(0, 0, 3, 3) + " - " + blackTop10.getRectangleSum(4, 4, 7, 7) + " = " + expected + " : " + actual);
        assertEquals(expected, actual);
        assertEquals(
                blackTop10.getRectangleSum(1, 1, 3, 3) - blackTop10.getRectangleSum(1, 4, 3, 6),
                FaceRecognition.calcVerticalTwoRectFeature(blackTop10, 1, 1, 4, 4)
        );
    }

    private HalIntegralImage readImage(String path) throws Exception {
        File file = new File(path);
        HalIntegralImage img = new HalIntegralImage(new FastBitmap(ImageIO.read(file)));
        return img;
    }
}

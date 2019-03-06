import Catalano.Imaging.FastBitmap;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Tests {

    private String path = "./test-res/testing/";

    //The coordinates for all references to an integral images and their expected values have been increased by one
    //to correct for the now larger integral image.

    @Test
    public void testImageRead() {
        ArrayList<HalIntegralImage> images = new ArrayList<>();
        try {
            images = Data.readImagesFromDataBase(path); // Read images
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Correctly reads all images
        assertEquals(images.size(), new File(path).listFiles().length);
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
        assertEquals(black.getInternalData()[25][25], black.getRectangleSum(0, 0, 24, 24));

        //Test white
        //System.out.println("== WHITE ==");
        //System.out.println(Arrays.deepToString(white.getInternalData())); // Consistent with above except first line is 0.
        assertEquals(254, white.getInternalData()[1][1]);

        //Test face
        //System.out.println("== FACE ==");
        //System.out.println(Arrays.deepToString(faceFB.toMatrixGrayAsInt())); // Seems reasonable.
        //System.out.println(Arrays.deepToString(face.getInternalData()));

        // Test image where top 10 rows are black, bottom 15 rows are white.
        //System.out.println("== Black top 10 ==");
        //System.out.println(Arrays.deepToString(blackTop10.getInternalData()));
        //printImageValues(blackTop10.fastBitmap);
        //assertEquals(1, blackTop10.fastBitmap.getGray(9, 0)); // FastBitmap uses coordinates in wrong order
        //assertEquals(255, blackTop10.fastBitmap.getGray(10, 0));
        //System.out.println("---------------");
        //printIntegralImage(blackTop10.getInternalData());
        assertEquals(1, blackTop10.getInternalData()[1][1]);
        assertEquals(10, blackTop10.getInternalData()[10][1]);
        assertEquals(265, blackTop10.getInternalData()[11][1]);
        assertEquals(11, blackTop10.getInternalData()[1][11]);

        // Test image with where leftmost 10 columns are black, rightmost 15 columns are white.
        //System.out.println("== Black left 10 ==");
        //System.out.println(Arrays.deepToString(blackTop10.getInternalData()));
        assertEquals(1, blackLeft10.getInternalData()[1][1]);
        assertEquals(10, blackLeft10.getInternalData()[10][1]);
        assertEquals(11, blackLeft10.getInternalData()[11][1]);
        assertEquals(265, blackLeft10.getInternalData()[1][11]);

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

        // Test horizontal feature calculation
        int expectedFace = face92x112.getRectangleSum(0, 0, 1, 3) - face92x112.getRectangleSum(2, 0, 3, 3);
        int actualFace = Feature.calcHorizontalTwoRectFeature(face92x112, 0, 0, 4, 4);
        assertEquals(expectedFace, actualFace);

        // Test vertical feature calculation
        assertEquals(
                face92x112.getRectangleSum(1, 1, 4, 2) -
                         face92x112.getRectangleSum(1, 3, 4, 4),
                Feature.calcVerticalTwoRectFeature(face92x112, 1, 1, 4, 4)
        );
        // Test type three feature calculation
        assertEquals(
                face92x112.getRectangleSum(2, 0, 3, 3) -
                         face92x112.getRectangleSum(0, 0, 1, 3) -
                         face92x112.getRectangleSum(4, 0, 5, 3),
                Feature.calcThreeRectFeature(face92x112, 0, 0, 6, 4)
        );

        // Test type four feature calculation
        assertEquals(
                face92x112.getRectangleSum(2, 0, 3, 1) +
                         face92x112.getRectangleSum(0, 2, 1, 3) -
                         face92x112.getRectangleSum(0, 0, 1, 1) -
                         face92x112.getRectangleSum(2, 2, 3, 3),
                Feature.calcFourRectFeature(face92x112, 0, 0, 4, 4)
        );
    }

    // Tests saving and loading of classifiers.
    @Test
    public void testClassifierSaveLoad() throws Exception {

        Classifier a1 = null;
        Classifier b1 = null;

        try {
            a1 = new Classifier(new Feature(Feature.Type.HORIZONTAL, 1, 2, 4, 4), 10, 10);
            b1 = new Classifier(new Feature(Feature.Type.VERTICAL, 2, 8, 6, 4), 5, 5);
        } catch (Exception e) {
            e.printStackTrace();
        }

        StrongClassifier a = new StrongClassifier();
        a.addClassifier(a1);
        StrongClassifier b = new StrongClassifier();
        b.addClassifier(b1);


        CascadeClassifier cascadeClassifier = new CascadeClassifier();
        cascadeClassifier.addStrongClassifier(a);
        cascadeClassifier.addStrongClassifier(b);

        cascadeClassifier.save("test-res/test.cascade");

        CascadeClassifier loaded = new CascadeClassifier();
        try {
            loaded = new CascadeClassifier("test-res/test.cascade");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        assertEquals(cascadeClassifier, loaded);

        new File("test.classifiers").delete();
    }

    private HalIntegralImage readImage(String path) throws Exception {
        File file = new File(path);
        HalIntegralImage img = new HalIntegralImage(new FastBitmap(ImageIO.read(file)), "asdf");
        return img;
    }


    @Test
    public void testBestThresholdAndParity() throws Exception {
        HalIntegralImage black = readImage(path + "black-25px.png");
        LabeledIntegralImage black1 = new LabeledIntegralImage(black, false, 1);
        HalIntegralImage white = readImage(path + "white-25px.png");
        LabeledIntegralImage white1 = new LabeledIntegralImage(white, false, 1);
        HalIntegralImage face = readImage(path + "000.png");
        LabeledIntegralImage face1 = new LabeledIntegralImage(face, true, 1);
        HalIntegralImage blackTop10 = readImage(path + "blackTop10-25px.png");
        LabeledIntegralImage blackTop101 = new LabeledIntegralImage(black, false, 1);
        HalIntegralImage blackLeft10 = readImage(path + "blackLeft10-25px.png");
        LabeledIntegralImage blackLeft101 = new LabeledIntegralImage(black, true, 1);

        ArrayList<LabeledIntegralImage> trainingData = new ArrayList();
        trainingData.add(black1);
        //trainingData.add(white1);
        trainingData.add(face1);
        //trainingData.add(blackTop101);
        // trainingData.add(blackLeft101);


        Feature rect = new Feature(Feature.Type.HORIZONTAL, 6,8,2,2);
        ThresholdParity rectTP = Classifier.calcBestThresholdAndParity(trainingData, rect);
        //System.out.println("threshold " + rectTP.threshold + "; Parity " + rectTP.parity);
        assertTrue(0 <= rectTP.threshold && rectTP.threshold <= 65, "Expected in [0, 65]. Was: " + rectTP.threshold);

        Feature rect2 = new Feature(Feature.Type.VERTICAL, 6,8,2,2);
        ThresholdParity rect2TP = Classifier.calcBestThresholdAndParity(trainingData, rect2);
        //System.out.println("threshold " + rect2TP.threshold + "; Parity " + rect2TP.parity);
        assertTrue(-69 <= rectTP.threshold && rectTP.threshold <= 0, "Expected in [-69, 0]. Was: " + rect2TP.threshold);

        Feature rect3 = new Feature(Feature.Type.THREE, 6,8,3,2);
        ThresholdParity rect3TP = Classifier.calcBestThresholdAndParity(trainingData, rect3);
        //System.out.println("threshold " + rect3TP.threshold + "; Parity " + rect3TP.parity);
        assertTrue(-338 <= rect3TP.threshold && rect3TP.threshold <= -2, "Expected in [-338, -2]. Was: " + rect3TP.threshold);

    }

    public static void printIntegralImage(int[][] img) {

        for (int h = 0; h < img.length; h++) {
            for (int w = 0; w < img[0].length; w++) {
                System.out.print(img[h][w] + ", ");
            }
            System.out.println();
        }
    }

    public static void printImageValues(FastBitmap fb){
        int width = fb.getWidth();
        int height = fb.getHeight();

        for(int h=0;h<height;h++) {
            for (int w = 0; w < width; w++) {
                System.out.print(fb.getGray(h, w) + ", ");
            }
            System.out.println();
        }
    }

    @Test
    public void testGetRectangleSum() throws Exception {
        File file = new File(path + "B20_03379.png");
        HalIntegralImage img = new HalIntegralImage(new FastBitmap(ImageIO.read(file)), file.getName());


        System.out.println("GRAY:");
        printImageValues(new FastBitmap(ImageIO.read(file)));

        System.out.println();
        System.out.println();
        System.out.println("II:");
        printIntegralImage(img.getInternalData());


        assertTrue(img.getRectangleSum(0,0,18,4)>img.getRectangleSum(0,0,4,18),
                "The test failed on the image test-res/B20_03379.png. " +
                        "The top should be brighter than the left side but isn't");
        assertEquals(img.getRectangleSum(0,0,18,4),14525,
                "Rectangle sum for the top 4 pixels of the image test-res/B20_03379.png is wrong.");
        assertEquals(img.getRectangleSum(0,0,4,18), 1415,
                "Rectangle sum for the leftmost 4 pixels of the image test-res/B20_03379.png is wrong.");
    }


    @Test
    public void testConvert() throws Exception {
        BufferedImage bi = ImageIO.read(new File("./test-res/examples/many_faces.png"));

        BufferedImage image = new BufferedImage(bi.getWidth(), bi.getHeight(),
                BufferedImage.TYPE_BYTE_GRAY);
        Graphics g = image.getGraphics();
        g.drawImage(bi, 0, 0, null);
        g.dispose();

        FastBitmap fb = new FastBitmap(image);

        //printImageValues(fb);

        //assertTrue(fb.isGrayscale());
        assertTrue(fb.isGrayscale());
    }
}

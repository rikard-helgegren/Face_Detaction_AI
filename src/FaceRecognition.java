import Catalano.Imaging.FastBitmap;
import Catalano.Imaging.Tools.IntegralImage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;

/**
 * This file should be run with the project root as working directory.
 * Make sure images exist before running.
 */
public class FaceRecognition {

    private static class LabeledIntegralImage {
        public int isFace; // 1 for true, 0 for false
        public HalIntegralImage img;
        public double weight;

        public LabeledIntegralImage(HalIntegralImage img, int isFace, double weight) {
            this.isFace = isFace;
            this.img = img;
            this.weight = weight;
        }
    }

    public static void main(String[] args) throws Exception {
        // Read images from file system amd calculate integralImages.
        // This now uses our own HalIntegralImage. It seems to work, but there could be bugs.
        HalIntegralImage[] trainFaces = {};
        HalIntegralImage[] trainNoFaces = {};
        HalIntegralImage[] testFaces = {};
        HalIntegralImage[] testNoFaces = {};
        try {
            // Read images for training set
            trainFaces = readImagesFromDataBase("./res/source/data/train/face"); // Read face images
            trainNoFaces = readImagesFromDataBase("./res/source/data/train/non-face"); // Read no-face images

            // Read images for test set
            testFaces = readImagesFromDataBase("./res/source/data/test/face");
            testNoFaces = readImagesFromDataBase("./res/source/data/test/non-face");
            //System.out.println("Read faces (and the corresponding non faces) from " + faceImagesFolder[i]);
        } catch (IOException e) {
            System.err.println("Data folder not found. Have you extracted data.zip correctly?");
        } catch (Exception e) {
            System.err.println("There was an error reading images.");
            e.printStackTrace();
        }

        // Calculate initial weights. TODO Verify that this is correct. I'm not sure.
        double weightFace = 1.0 / (2 * trainFaces.length);
        double weightNoFace = 1.0 / (2 * trainNoFaces.length);

        // Re-store arrays of training data as a list and add face label.
        ArrayList<LabeledIntegralImage> trainingData = new ArrayList<>(5000);
        for (HalIntegralImage img : trainFaces) trainingData.add(new LabeledIntegralImage(img, 1, weightFace));
        for (HalIntegralImage img : trainNoFaces) trainingData.add(new LabeledIntegralImage(img, 0, weightNoFace));
        Collections.shuffle(trainingData);

        // Re-store arrays of test data as list and add face label. Test data weights will not be used.
        ArrayList<LabeledIntegralImage> testData = new ArrayList<>(20000);
        for (HalIntegralImage img : testFaces) testData.add(new LabeledIntegralImage(img, 1, 0));
        for (HalIntegralImage img : testNoFaces) testData.add(new LabeledIntegralImage(img, 0, 0));
        Collections.shuffle(testData);

        // Generate all possible features
        ArrayList<Feature> allFeatures = Feature.generateAllFeatures(19, 19);
        Collections.shuffle(allFeatures);
        





        // Do pattern recognition things
        //searchForPatterns();
    }

    /**
     * Reads the images from
     * @param path a path to a folder containing only images. Images should have correct size and other properties.
     * @throws IOException
     */
    public static HalIntegralImage[] readImagesFromDataBase(String path) throws Exception {
        File imageFolder = new File(path);
        HalIntegralImage[] images = new HalIntegralImage[imageFolder.listFiles().length];

        File[] listFiles = imageFolder.listFiles();
        for (int i = 0; i < listFiles.length; i++) {
            File imgFile = listFiles[i];
            BufferedImage bi = ImageIO.read(imgFile);
            FastBitmap fb = new FastBitmap(bi);
            try {
                images[i] = new HalIntegralImage(fb);
            } catch (Exception e) {
                System.err.println("Could not read " + imgFile.getPath());
                e.printStackTrace();
                break;
            }
            //if ((i+1) % 1000 == 0) System.out.printf("%d/%d\n", i+1, imageFolder.listFiles().length);
        }
        return images;
    }



    /**
     * Uses features to determine if the image might be a face.
     * @param img
     * @return true if the image might be a face
     */
    public static boolean couldBeFace(HalIntegralImage img) {
        return false;
    }

}

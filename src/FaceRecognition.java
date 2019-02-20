import Catalano.Imaging.FastBitmap;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;

/**
 * This file should be run with the project root as working directory.
 * Make sure images exist before running.
 */
public class FaceRecognition {

    public static void main(String[] args) throws IOException {
        String[] faceImagesFolder = {"./res/source/data/train/face", "./res/generated/att-faces-scaled"};
        String[] noFaceImagesFolder = {"./res/source/data/train/non-face", "./res/source/no-faces-crawled"};

        String zipPath = "./res/source/data.zip";

        // Read images from file system amd calculate integralImages.
        // This now uses our own HalIntegralImage. It seems to work, but there could be bugs.
        HalIntegralImage[] faces = {};
        HalIntegralImage[] noFaces = {};
        for (int i = 0; i < faceImagesFolder.length; i++) {
            try {
                faces = readImagesFromDataBase(faceImagesFolder[i]); // Read face images
                noFaces = readImagesFromDataBase(noFaceImagesFolder[i]); // Read no-face images
                //System.out.println("Read faces (and the corresponding non faces) from " + faceImagesFolder[i]);
                break;
            } catch (IOException e) {
                System.err.println("Data folder (" + faceImagesFolder[i] + ") not found.");
                System.out.println("Trying next location...");
            } catch (Exception e) {
                System.err.println("There was an error reading images.");
                e.printStackTrace();
            }
        }


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
     * Generate a list of all possible feature types.
     * @param imageWidth
     * @param imageHeight
     * @return
     */
    public static ArrayList<Feature> generateAllFeatures(int imageWidth, int imageHeight) {
        ArrayList<Feature> allFeatures = new ArrayList<>(160000);
        for (int x = 0; x < imageWidth; x++){
            for (int y = 0; y < imageHeight; y++) {
                for (int w = 2; w < imageWidth - x; w+=1) {
                    for (int h = 2; h < imageHeight - y; h += 1){
                        if (w % 2 == 0) allFeatures.add(new Feature(Feature.Type.HORIZONTAL, x, y, w, h));
                        if (h % 2 == 0) allFeatures.add(new Feature(Feature.Type.VERTICAL, x, y, w, h));
                        if (w % 2 == 0 && h % 2 == 0) allFeatures.add(new Feature(Feature.Type.FOUR, x, y, w, h));
                        if (w % 3 == 0) allFeatures.add(new Feature(Feature.Type.THREE, x, y, w, h));
                    }
                }
            }
        }

        return allFeatures;
    }

    /**
     * Calculates the difference between the rectangle sums of two rectangles located next to each other horizontally.
     * Note that x+2*w may not be larger than the width of the image.
     * Note that y+h may not be larger than the height of the image.
     *
     * Left rectangle: upper left corner is (x, y), bottom right is (x+w, y+h).
     * Right rectangle: upper left corner is (x+w, y), bottom right is (x+2*w, y+h).
     *
     * @param img the integral image to operate on
     * @param x coordinate for the upper left corner of the left rectangle.
     * @param y coordinate for the upper left corner of the left rectangle.
     * @param w the width of total feature.
     * @param h the height of total feature.
     * @return
     * @throws Exception
     */
    public static int calcHorizontalTwoRectFeature(HalIntegralImage img, int x, int y, int w, int h) throws Exception {
        if (w%2 != 0) throw new Exception("Horizontal feature, width has to be divisible by 2. Was " + w);
        // -1 on end coordinates because getRectangleSum uses inclusive coordinates,
        // but width and height are generally thought of as exclusive.
        // That is rectangle at (0, 0) with w=3 and h=3 means a 3x3 rectangle.
        // However, if rectangleSum gets (0, 0) and (3, 3) it will use a 4x4 rectangle in top left corner.
        return img.getRectangleSum(x, y, x+w/2-1, y+h-1) - img.getRectangleSum(x+w/2, y, x+w-1, y+h-1);
    }

    /**
     * Calculates the difference between the rectangle sums of two rectangles located next to each other vertically.
     *
     * @param img the integral image to operate on
     * @param x coordinate for the upper left corner of the top rectangle.
     * @param y coordinate for the upper left corner of the top rectangle.
     * @param w the width of each rectangle.
     * @param h the height of each rectangle.
     * @return
     * @throws Exception
     */
    public static int calcVerticalTwoRectFeature(HalIntegralImage img, int x, int y, int w, int h) throws Exception {
        if (h%2 != 0) throw new Exception("Vertical feature, height has to be divisible by 2. Was " + h);
        return img.getRectangleSum(x, y, x+w-1, y+h/2-1) - img.getRectangleSum(x, y+h/2, x+w-1, y+h-1);
    }

    public static int calcThreeRectFeature(HalIntegralImage img, int x, int y, int w, int h) throws Exception {
        return 0; // TODO
    }

    public static int calcFourRectFeature(HalIntegralImage img, int x, int y, int w, int h) throws Exception {
        return 0; // TODO
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

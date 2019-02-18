import Catalano.Imaging.FastBitmap;
import Catalano.Imaging.Tools.IntegralImage;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Scanner;

/**
 * This file should be run with the project root as working directory.
 * Make sure images exist before running.
 */
public class FaceRecognition {

    public static void main(String[] args) {
        String faceImagesFolder = "./res/att-faces-scaled";
        // String noFaceImagesFolder = "./res/att-faces-scaled";

        // Read images from file system.
        // FastBitmap is part of the Catalano library.
        FastBitmap[] faceBitmaps = {};
        FastBitmap[] noFaceBitmaps = {};
        try {
            faceBitmaps = readImagesFromDataBase(faceImagesFolder); // Read face images
            // TODO Read no-face images
            // noFaceBitmaps = readImagesFromDataBase(imageFolder); // Read no-face images
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Calculate integral images of all face images.
        HalIntegralImage[] integralFaceImages = convertToIntegralImages(faceBitmaps);

        // TODO Calculate integral images of all non-face images.
        IntegralImage[] integralNoFaceImages;

        System.out.println(integralFaceImages[0]);

        // Do pattern recognition things
        //searchForPatterns();
    }

    /**
     * Reads the images from
     * @param path a path to a folder containing only images. Images should have correct size and other properties.
     * @throws IOException
     */
    public static FastBitmap[] readImagesFromDataBase(String path) throws IOException{
        File imageFolder = new File(path);
        FastBitmap[] bitmaps = new FastBitmap[imageFolder.listFiles().length];

        File[] listFiles = imageFolder.listFiles();
        for (int i = 0; i < listFiles.length; i++) {
            File imgFile = listFiles[i];
            BufferedImage bi = ImageIO.read(imgFile);
            FastBitmap fb = new FastBitmap(bi);
            bitmaps[i] = fb;
            System.out.printf("%d/%d\n", i+1, imageFolder.listFiles().length);
        }
        return bitmaps;
    }

    public static HalIntegralImage[] convertToIntegralImages(FastBitmap[] bitmaps) {
        HalIntegralImage[] integrals = new HalIntegralImage[bitmaps.length];
        for (int i = 0; i < bitmaps.length; i++) {
            integrals[i] = new HalIntegralImage(bitmaps[i]);
        }
        return integrals;
    }

    // ImageMagick is much more convenient for size and contrast changes than java,
    // therefore, those tasks have been moved to bash script. See `prepareImages.sh`
    /*public static void preProcessImages(FastBitmap[] bitmaps) {

    }*/
    /*


    public placeholder saveImages(placeholder images) {

    }

    public placeholder searchForPatterns(placeholder images) {
        //more methods needed
    }*/


}

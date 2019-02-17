import Catalano.Imaging.FastBitmap;

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
        String imageFolder = "./res/att-faces-scaled";

        FastBitmap[] bitmaps = {};
        try {
            bitmaps = readImagesFromDataBase(imageFolder); // Read images
        } catch (IOException e) {
            e.printStackTrace();
        }
        preProcessImages(bitmaps); //only done first time
        //saveImages(images);      //only done first time, don't override previous folder
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
            System.out.printf("%d/%d\n", i, imageFolder.listFiles().length);
        }
        return bitmaps;
    }

    // ImageMagick is much more convenient for size and contrast changes than java,
    // therefore, those tasks have been moved to bash script. See `prepareImages.sh`
    public static void preProcessImages(FastBitmap[] bitmaps) {
        
    }
    /*


    public placeholder saveImages(placeholder images) {

    }

    public placeholder searchForPatterns(placeholder images) {
        //more methods needed
    }*/


}

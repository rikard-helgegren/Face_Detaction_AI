import Catalano.Imaging.FastBitmap;
import Catalano.Imaging.Tools.IntegralImage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;

/**
 * This file should be run with the project root as working directory.
 * Make sure images exist before running.
 */
public class FaceRecognition {

    public static void main(String[] args) {
        String faceImagesFolder = "./res/generated/att-faces-scaled";
        String noFaceImagesFolder = "./res/source/no-faces-crawled";

        // Read images from file system amd calculate integralImages.
        // This now uses our own HalIntegralImage. It seems to work, but there could be bugs.
        HalIntegralImage[] faces = {};
        HalIntegralImage[] noFaces = {};
        try {
            faces = readImagesFromDataBase(faceImagesFolder); // Read face images
            noFaces = readImagesFromDataBase(noFaceImagesFolder); // Read no-face images
        } catch (Exception e) {
            e.printStackTrace();
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
            images[i] = new HalIntegralImage(fb);
            //System.out.printf("%7s: %d/%d\n", imgFile.getName(), i+1, imageFolder.listFiles().length);
        }
        return images;
    }

    /*
    public placeholder searchForPatterns(placeholder images) {
        //more methods needed
    }
    */


}

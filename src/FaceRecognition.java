import Catalano.Imaging.FastBitmap;
import Catalano.Imaging.Tools.IntegralImage;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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

    /*
    public placeholder searchForPatterns(placeholder images) {
        //more methods needed
    }
    */


}

import Catalano.Imaging.FastBitmap;

import java.io.File;

/**
 * This file should be run with the project root as working directory.
 */
public class FaceRecognition {
    FastBitmap b;
    public static void main(String[] args) {
        String imageFolder = "./res/att-faces";

        readImagesFromDatabase(imageFolder);
        //preProcessImages(images); //only done first time
        //saveImages(images);      //only done first time, don't override previous folder
        //searchForPatterns();
    }

    public static void readImagesFromDatabase(String folderName) {
        System.out.println("Reading images...");
        File folder = new File(folderName);
        for (File f : folder.listFiles()) {
            if (f.isDirectory()) {
                System.out.println(getContentString(f));
            }
        }
    }

    /**
     * Returns a newline-separated string of all file names in a folder or empty string if folder is not a folder.
     * @param folder a file handle representing a folder.
     * @return a newline-separated string of file names
     */
    private static String getContentString(File folder) {
        if (!folder.isDirectory()) return "";
        String s = "";
        for (File f : folder.listFiles()) {
            s += f.getName() + "\n";
        }
        return s;
    }

    /*
    public placeholder preProcessImages(placeholder images) {
        fixSize();
        fixContrast();
    }

    public placeholder fixSize(placeholder images) {

    }

    public placeholder fixContrast(placeholder images) {

    }

    public placeholder saveImages(placeholder images) {

    }

    public placeholder searchForPatterns(placeholder images) {
        //more methods needed
    }*/







}

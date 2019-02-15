import Catalano.Imaging.FastBitmap;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Scanner;

/**
 * This file should be run with the project root as working directory.
 */
public class FaceRecognition {

    public static void main(String[] args) {
        String imageFolder = "./res/att-faces";

        try {
            readImagesFromDatabase(imageFolder);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //preProcessImages(images); //only done first time
        //saveImages(images);      //only done first time, don't override previous folder
        //searchForPatterns();
    }

    /**
     * Reads .pgm images from our folder structure.
     *
     * .pgm reading code mostly from: https://stackoverflow.com/questions/3639198/how-to-read-pgm-images-in-java
     * @param folderName the name of our folder of folders containing .pgm images.
     * @throws IOException
     */
    public static void readImagesFromDatabase(String folderName) throws IOException {
        System.out.println("Reading images...");
        File folder = new File(folderName);
        for (File f : folder.listFiles()) {
            if (f.isDirectory()) {
                for (File g : f.listFiles()) {
                    String imgPath = g.getPath();
                    //System.out.println(imgPath);

                    // Read image metadata
                    FileInputStream fileInputStream = null;
                    fileInputStream = new FileInputStream(imgPath);
                    Scanner scan = new Scanner(fileInputStream);
                    scan.nextLine(); // Discard magic number
                    int picWidth = scan.nextInt(); // Image width
                    int picHeight = scan.nextInt(); // Image height
                    int maxValue = scan.nextInt(); // Maximum value of pixel.

                    fileInputStream.close();

                    // Read image as binary data
                    fileInputStream = new FileInputStream(imgPath);
                    DataInputStream dis = new DataInputStream(fileInputStream);

                    // Discard header lines (the ones we read previously)
                    int numnewlines = 3;
                    while (numnewlines > 0) {
                        char c;
                        do {
                            c = (char)(dis.readUnsignedByte());
                        } while (c != '\n');
                        numnewlines--;
                    }

                    // Read the image data
                    int[][] data2D = new int[picHeight][picWidth];
                    for (int row = 0; row < picHeight; row++) {
                        for (int col = 0; col < picWidth; col++) {
                            data2D[row][col] = dis.readUnsignedByte();
                            //System.out.print(data2D[row][col] + " ");
                        }
                        //System.out.println();
                    }

                    // Create a FastBitmap. Part of the Catalano library.
                    FastBitmap fb = new FastBitmap(data2D);
                    System.out.println(g.getPath() + " " + fb.getGraphics());

                    // Save FastBitmap as image
                    BufferedImage bufferedImage = new BufferedImage(picWidth, picHeight, BufferedImage.TYPE_BYTE_GRAY);
                    fb.getGraphics().drawImage(bufferedImage, 0, 0, null);

                    // TODO Only saves a black image, but of the right size.
                    ImageIO.write(bufferedImage, "PNG", new File("./processed/" + "a.png"));
                }
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

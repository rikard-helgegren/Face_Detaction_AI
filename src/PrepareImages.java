import Catalano.Imaging.FastBitmap;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Scanner;

// DO NOT RUN THIS FILE, NO LONGER NEEDED
/**
 * Helps prepare images. Currently, run this and then `prepareImages.sh` to prepare images.
 */
public class PrepareImages {

    public static void main(String[] args) {
        String imageFolder = "./res/source/att-faces";

        try {
            flattenConvertAttImages(imageFolder);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // Currently, this function just copies all images and places them in a new folder,
    // but without the previous folder structure.
    /**
     * Reads our ATT face images, converts them to PNG and saves in a flattened file structure.
     *
     * @param folderPath the name of our folder of folders containing .pgm images.
     * @throws IOException
     */
    public static void flattenConvertAttImages(String folderPath) throws IOException {
        System.out.println("Reading images...");
        File folder = new File(folderPath);
        int imageNr = 0;
        for (File f : folder.listFiles()) {
            String folderName = f.getName();
            if (f.isDirectory()) {
                for (File g : f.listFiles()) {
                    String fileName = g.getName().replaceAll("\\.pgm", "");
                    String imgPath = g.getPath();
                    //System.out.println("Read: " + fileName);

                    FastBitmap fb = readPGM(imgPath);

                    BufferedImage bufferedImage = fb.toBufferedImage();

                    // Save FastBitmap as image. Can't save as pgm for some reason. Might not want to.
                    ImageIO.write(bufferedImage, "png",
                            new File(String.format("./res/generated/att-faces-java/%03d.png",imageNr++))
                    );
                    //System.out.println("Saved image.");
                }
            }
            System.out.println("Done with " + folderName);
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

    /**
     * Reads a single .pgm image from a given path.
     *
     * Code mostly from: https://stackoverflow.com/questions/3639198/how-to-read-pgm-images-in-java
     *
     * @param imgPath
     * @return
     * @throws IOException if given image was not found or any other IO error occurred.
     */
    public static FastBitmap readPGM(String imgPath) throws IOException {
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
        return new FastBitmap(data2D);
    }

}

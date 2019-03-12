import Catalano.Imaging.FastBitmap;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;

public class Data {

    public String[] pathsToFaces;
    public String[] pathsToNonFaces;

    // Percentages and maximums for datasets.
    // Percentage sum is automatically normalized to 1.
    private double percentTrainFaces = 0.5;
    private int maxTrainFaces = 4000;
    private double percentTrainNonFaces = 0.5;
    private int maxTrainNonFaces = 10000;

    private double percentTestFaces = 0.5;
    private int maxTestFaces = 10000;
    private double percentTestNonFaces = 0.5;
    private int maxTestNonFaces = 50000;

    private double percentValidateFaces = 0.1;
    private int maxValidateFaces = 1000;
    private double percentValidateNonFaces = 0.1;
    private int maxValidateNonFaces = 2000;

    public List<LabeledIntegralImage> negativeSamples;
    public List<LabeledIntegralImage> positiveSamples;
    public List<LabeledIntegralImage> allSamples;
    public List<LabeledIntegralImage> testData;
    public List<LabeledIntegralImage> validationData;

    public Data() throws Exception {
        // Define paths to all datasets
        String originalTrainFaces = "./res/faces/original-train-face";
        String originalTestFaces = "./res/faces/original-test-face";
        String attFaces = "./res/faces/att";
        String lfw2Faces = "./res/faces/lfw2-19px"; // For testing only
        String fddbFaces = "./res/faces/fddb-flat"; // For testing only

        String originalTrainNonFaces = "./res/non-faces/original-test-non-face";
        String originalTestNonFaces = "./res/non-faces/original-train-non-face";
        String crawledNonFaces = "./res/non-faces/scraped";
        String manyFacesTest = "./test-res/examples";
        String smartestPictureNonFaces = "./res/non-faces/smartest-picture-non-face";
        String manyScrapedNonFaces = "./res/non-faces/many-scraped-non-face";

        // Select which datasets to use
        pathsToFaces = new String[]{originalTrainFaces, attFaces, lfw2Faces};
        pathsToNonFaces = new String[]{originalTrainNonFaces, originalTestNonFaces, crawledNonFaces, smartestPictureNonFaces, manyScrapedNonFaces};
        //pathsToFaces = new String[]{lfw2Faces};
        //pathsToNonFaces = new String[]{originalTestNonFaces};

        partitionData();


        // Pre-calculate feature values. OK to pre-calculate on only part of data.
        System.out.println("Pre-calculating feature values for training data...");
        Feature.calculateFeatureValues(allSamples);
        //System.out.println("Pre-calculating feature values for test data...");
        //Feature.calculateFeatureValues(testData);

    }

    private void partitionData() throws Exception {
        // Read images from file system and calculate integral images.
        ArrayList<HalIntegralImage> faces = new ArrayList<>();
        ArrayList<HalIntegralImage> nonFaces = new ArrayList<>();
        try {
            // Read images for faces
            for (String path : pathsToFaces) {
                System.out.println(path);
                faces.addAll(Data.readImagesFromDataBase(path));
            }
            // Read images for non-faces
            for (String path : pathsToNonFaces) {
                System.out.println(path);
                nonFaces.addAll(Data.readImagesFromDataBase(path));
            }

        } catch (Exception e) {
            System.err.println("Data folder not found. Have you extracted res.zip correctly?");
            System.exit(1);
        }

        // Label all integral images
        ArrayList<LabeledIntegralImage> allFaces = new ArrayList<>();
        ArrayList<LabeledIntegralImage> allNonFaces = new ArrayList<>();
        for (HalIntegralImage img : faces) allFaces.add(new LabeledIntegralImage(img, true, 0));
        for (HalIntegralImage img : nonFaces) allNonFaces.add(new LabeledIntegralImage(img, false, 0));

        // Shuffle so that final sets have contribution from each file-system set. Use seed to make shuffling deterministic.
        Collections.shuffle(allFaces, new Random(1));
        Collections.shuffle(allNonFaces, new Random(1));

        // Calculate indexes to split sets correctly based on percentage and max value
        double totalFacePercentage = percentTrainFaces + percentValidateFaces + percentTestFaces;
        double totalNonFacePercentage = percentTrainNonFaces + percentValidateNonFaces + percentTestNonFaces;

        int endTrainFacesIndex = (int) ((allFaces.size() - 1) * (percentTrainFaces / totalFacePercentage));
        int endTrainNonFacesIndex = (int) ((allNonFaces.size() - 1) * (percentTrainNonFaces / totalNonFacePercentage));

        int endValidateFacesIndex = (int) ((allFaces.size() - 1) * (percentTrainFaces + percentValidateFaces) / totalFacePercentage);
        int endValidateNonFacesIndex = (int) ((allNonFaces.size() - 1) * (percentTrainNonFaces + percentValidateNonFaces) / totalNonFacePercentage);

        int endTestFacesIndex = allFaces.size();
        int endTestNonFacesIndex = allNonFaces.size();

        positiveSamples = new ArrayList<>(allFaces.subList(0, Math.min(endTrainFacesIndex, maxTrainFaces)));
        negativeSamples = new ArrayList<>(allNonFaces.subList(0, Math.min(endTrainNonFacesIndex, maxTrainNonFaces)));

        // Set training data weights
        double weightFace = 1.0 / (2 * positiveSamples.size());
        double weightNoFace = 1.0 / (2 * negativeSamples.size());

        for (LabeledIntegralImage i : positiveSamples) {
            i.setWeight(weightFace);
        }
        for (LabeledIntegralImage i : negativeSamples) {
            i.setWeight(weightNoFace);
        }

        // Create final sets
        allSamples = new ArrayList<>();
        allSamples.addAll(negativeSamples);
        allSamples.addAll(positiveSamples);

        validationData = new ArrayList<>(
                allFaces.subList(endTrainFacesIndex,
                        Math.min(endValidateFacesIndex, endTrainFacesIndex + maxValidateFaces)));
        validationData.addAll(allNonFaces.subList(
                endTrainNonFacesIndex,
                Math.min(endValidateNonFacesIndex, endTrainNonFacesIndex + maxValidateNonFaces)));

        testData = new ArrayList<>(allFaces.subList(
                endValidateFacesIndex,
                Math.min(endTestFacesIndex, endValidateFacesIndex + maxTestFaces)));
        testData.addAll(allNonFaces.subList(
                endValidateNonFacesIndex,
                Math.min(endTestNonFacesIndex, endValidateNonFacesIndex + maxTestNonFaces)));

    }

    /**
     * Reads the images from
     *
     * @param path a path to a folder containing only images. Images should have correct size and other properties.
     * @throws IOException
     */
    public static ArrayList<HalIntegralImage> readImagesFromDataBase(String path) throws Exception {
        File imageFolder = new File(path);
        ArrayList<HalIntegralImage> images = new ArrayList<>(imageFolder.listFiles().length);

        File[] listFiles = imageFolder.listFiles();
        for (int i = 0; i < listFiles.length; i++) {
            File imgFile = listFiles[i];
            BufferedImage bi = loadImageAsGrayscale(imgFile);
            FastBitmap fb = new FastBitmap(bi);
            try {
                images.add(new HalIntegralImage(fb, imgFile.getName()));
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
     * Removes the data that does not pass the strong classifier.
     *
     * @param cascadeClassifier
     * @param data
     * @return an array of images that the strong classifier thinks could be faces
     */
    public static ArrayList<LabeledIntegralImage> filter(CascadeClassifier cascadeClassifier, List<LabeledIntegralImage> data) throws Exception {
        ArrayList<LabeledIntegralImage> maybeFaces = new ArrayList<>(data.size() / 2);
        for (LabeledIntegralImage d : data) {
            if (cascadeClassifier.isFace(d.img)) {
                maybeFaces.add(d);
            }
        }
        return maybeFaces;
    }

    private static BufferedImage loadImageAsGrayscale(File file) throws IOException {
        //Load the image
        BufferedImage loadedImage = ImageIO.read(file);

        //Make a new empty BufferedImage and set its type to grayscale
        BufferedImage grayImage = new BufferedImage(loadedImage.getWidth(), loadedImage.getHeight(),
                BufferedImage.TYPE_BYTE_GRAY);

        //Draw loadedImage in grayscale on grayImage
        Graphics g = grayImage.getGraphics();
        g.drawImage(loadedImage, 0, 0, null);
        g.dispose();


        return grayImage;
    }

    public static void save(Serializable s, String fileName) {
        try {
            FileOutputStream fileOut = new FileOutputStream(fileName);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(s);
            out.close();
            fileOut.close();
        } catch (IOException e) {
            System.err.println("Save file could not be opened.");
            e.printStackTrace();
        }
    }

    public static Serializable load(String fileName) throws IOException, ClassNotFoundException {
        Serializable s;
        FileInputStream fileIn = new FileInputStream(fileName);
        ObjectInputStream in = new ObjectInputStream(fileIn);
        s = (Serializable) in.readObject();
        in.close();
        fileIn.close();
        return s;
    }


}

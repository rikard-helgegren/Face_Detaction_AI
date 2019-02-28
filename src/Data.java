import Catalano.Imaging.FastBitmap;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class Data {

    public ArrayList<LabeledIntegralImage> negativeSamples;
    public ArrayList<LabeledIntegralImage> positiveSamples;
    public ArrayList<LabeledIntegralImage> allSamples;

    public ArrayList<LabeledIntegralImage> testData;

    public Data() throws Exception {
        System.out.println("1");
        // Read images from file system amd calculate integralImages.
        // This now uses our own HalIntegralImage. It seems to work, but there could be bugs.
        HalIntegralImage[] trainFaces = {};
        HalIntegralImage[] trainNoFaces = {};
        HalIntegralImage[] testFaces = {};
        HalIntegralImage[] testNoFaces = {};
        try {
            System.out.println("2");
            // Read images for training set
            trainFaces = Data.readImagesFromDataBase("./res/source/data/train/face"); // Read face images
            trainNoFaces = Data.readImagesFromDataBase("./res/source/data/train/non-face"); // Read no-face images
            System.out.println("3");
            // Read images for test set
            testFaces = Data.readImagesFromDataBase("./res/source/data/test/face");
            testNoFaces = Data.readImagesFromDataBase("./res/source/data/test/non-face");
            System.out.println("4");
            //System.out.println("Read faces (and the corresponding non faces) from " + faceImagesFolder[i]);
        } catch (Exception e) {
            System.err.println("Data folder not found. Have you extracted data.zip correctly?");
            System.exit(1);
        }
        System.out.println("5");

        // Re-store arrays of training data as a list and add face label.
        double weightFace = 1.0 / (2 * trainFaces.length);
        double weightNoFace = 1.0 / (2 * trainNoFaces.length);

        negativeSamples = new ArrayList<>();
        positiveSamples = new ArrayList<>();
        allSamples = new ArrayList<>();
        for (HalIntegralImage img : trainNoFaces) negativeSamples.add(new LabeledIntegralImage(img, 0, weightNoFace));//TODO change maybe
        for (HalIntegralImage img : trainFaces) positiveSamples.add(new LabeledIntegralImage(img, 1, weightFace));
        allSamples.addAll(negativeSamples);
        allSamples.addAll(positiveSamples);
        System.out.println("6");

        // Re-store arrays of test data as list and add face label. Test data weights will not be used.
        testData = new ArrayList<>(20000);
        for (HalIntegralImage img : testFaces) testData.add(new LabeledIntegralImage(img, 1, 0));
        for (HalIntegralImage img : testNoFaces) testData.add(new LabeledIntegralImage(img, 0, 0));
        System.out.println("7");

        // Pre-calculate all feature values
        Feature.calculateFeatureValues(allSamples);

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
     * Removes the data that does not pass the strong classifier.
     * @param cascadedClassifier
     * @param data
     * @return an array of images that the strong classifier thinks could be faces
     */
    public static ArrayList<LabeledIntegralImage> filter(ArrayList<StrongClassifier> cascadedClassifier, ArrayList<LabeledIntegralImage> data) throws Exception {
        ArrayList<LabeledIntegralImage> maybeFaces = new ArrayList<>(data.size()/2);
        for (LabeledIntegralImage d : data) {
            if (FaceRecognition.isFace(cascadedClassifier, d.img)) {
                maybeFaces.add(d);
            }
        }
        return maybeFaces;
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

    public static void saveCascade(ArrayList<StrongClassifier> classifiers, String fileName){
        save(classifiers, fileName);
    }

    public static ArrayList<StrongClassifier> loadCascade(String fileName) throws IOException, ClassNotFoundException {
        return (ArrayList<StrongClassifier>) load(fileName);
    }

    public static void saveStrong(StrongClassifier strongClassifier, String fileName){
        save(strongClassifier, fileName);
    }

    public static StrongClassifier loadStrong(String fileName) throws IOException, ClassNotFoundException {
        return (StrongClassifier) load(fileName);
    }
}

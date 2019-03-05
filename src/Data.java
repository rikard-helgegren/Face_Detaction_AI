import Catalano.Imaging.FastBitmap;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Data {

    public List<LabeledIntegralImage> negativeSamples;
    public List<LabeledIntegralImage> positiveSamples;
    public List<LabeledIntegralImage> allSamples;

    public List<LabeledIntegralImage> testData;
    public List<LabeledIntegralImage> validationData;

    public Data() throws Exception {
        // Read images from file system amd calculate integralImages.
        // This now uses our own HalIntegralImage. It seems to work, but there could be bugs.
        HalIntegralImage[] trainFaces = {};
        HalIntegralImage[] trainNonFaces = {};
        //HalIntegralImage[] testFaces = {};
        HalIntegralImage[] testNonFaces = {};
        try {
            // Read images for training set
            trainFaces = Data.readImagesFromDataBase("./res/source/data/train/face"); // Read face images
            trainNonFaces = Data.readImagesFromDataBase("./res/source/data/train/non-face"); // Read no-face images
            // Read images for test set
            //testFaces = Data.readImagesFromDataBase("./res/source/data/test/face");
            testNonFaces = Data.readImagesFromDataBase("./res/source/data/test/non-face");

            //System.out.println("Read faces (and the corresponding non faces) from " + faceImagesFolder[i]);
        } catch (Exception e) {
            System.err.println("Data folder not found. Have you extracted data.zip correctly?");
            System.exit(1);
        }
        // Re-store arrays of training data as a list and add face label.
        double weightFace = 1.0 / (2 * (trainFaces.length /*+ testFaces.length*/));
        double weightNoFace = 1.0 / (2 * (trainNonFaces.length + testNonFaces.length));

        List<LabeledIntegralImage> allFaces = new ArrayList<>();
        for (HalIntegralImage img : trainFaces) allFaces.add(new LabeledIntegralImage(img, true, weightFace));
        //for (HalIntegralImage img : testFaces) allFaces.add(new LabeledIntegralImage(img, true, weightFace));
        Collections.shuffle(allFaces);

        List<LabeledIntegralImage> allNonFaces = new ArrayList<>();
        for (HalIntegralImage img : trainNonFaces) allNonFaces.add(new LabeledIntegralImage(img, false, weightNoFace));
        for (HalIntegralImage img : testNonFaces) allNonFaces.add(new LabeledIntegralImage(img, false, weightNoFace));
        Collections.shuffle(allNonFaces);

        negativeSamples = new ArrayList<>(allNonFaces.subList(0, allNonFaces.size()/4));
        positiveSamples = new ArrayList<>(allFaces.subList(0, allFaces.size()/2));

        validationData = new ArrayList<>(allNonFaces.subList(allNonFaces.size()/2, allNonFaces.size()*3/4));
        validationData.addAll(allFaces.subList(allFaces.size()/2, allFaces.size()*3/4));

        testData = new ArrayList<>(allNonFaces.subList(allNonFaces.size()*3/4, allNonFaces.size()));
        testData.addAll(allFaces.subList(allFaces.size()*3/4, allFaces.size()));

        allSamples = new ArrayList<>();
        allSamples.addAll(negativeSamples);
        allSamples.addAll(positiveSamples);

        /*
        int faceSplitIndex = 5 * trainFaces.length / 6;
        int noFaceSplitIndex = 5 * trainNonFaces.length / 6;
        System.out.printf("Splitting faces at %d and non-faces at %d\n", faceSplitIndex, noFaceSplitIndex);

        negativeSamples = new ArrayList<>();
        positiveSamples = new ArrayList<>();
        allSamples = new ArrayList<>();
        for (int i = 0; i < noFaceSplitIndex; i++) {
            HalIntegralImage img = trainNonFaces[i];
            negativeSamples.add(new LabeledIntegralImage(img, false, weightNoFace));
        }
        for (int i = 0; i < faceSplitIndex; i++) {
            HalIntegralImage img = trainFaces[i];
            positiveSamples.add(new LabeledIntegralImage(img, true, weightFace));
        }

        // Re-store arrays of test data as list and add face label. Test data weights will not be used.
        testData = new ArrayList<>(20000);
        for (int i = noFaceSplitIndex; i < trainNonFaces.length; i++) {
            HalIntegralImage img = trainNonFaces[i];
            testData.add(new LabeledIntegralImage(img, false, 0));
        }
        for (int i = faceSplitIndex; i < trainFaces.length; i++) {
            HalIntegralImage img = trainFaces[i];
            testData.add(new LabeledIntegralImage(img, true, 0));
        }*/

        // Pre-calculate all feature values
        System.out.println("Pre-calculating feature values for training data...");
        Feature.calculateFeatureValues(allSamples);
        //System.out.println("Pre-calculating feature values for test data...");
        //Feature.calculateFeatureValues(testData);

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
                images[i] = new HalIntegralImage(fb, imgFile.getName());
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
     * @param cascadeClassifier
     * @param data
     * @return an array of images that the strong classifier thinks could be faces
     */
    public static ArrayList<LabeledIntegralImage> filter(CascadeClassifier cascadeClassifier, List<LabeledIntegralImage> data) throws Exception {
        ArrayList<LabeledIntegralImage> maybeFaces = new ArrayList<>(data.size()/2);
        for (LabeledIntegralImage d : data) {
            if (cascadeClassifier.isFace(d.img)) {
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

    public static void saveStrong(StrongClassifier strongClassifier, String fileName){
        save(strongClassifier, fileName);
    }

    public static StrongClassifier loadStrong(String fileName) throws IOException, ClassNotFoundException {
        return (StrongClassifier) load(fileName);
    }
}

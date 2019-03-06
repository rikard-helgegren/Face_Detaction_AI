import Catalano.Imaging.FastBitmap;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Data {

    public String[] pathsToFaces;
    public String[] pathsToNonFaces;

    public double percentTrainFaces = 0.8;
    public double percentTrainNonFaces = 0.3;
    public double percentTestFaces = 0.1;
    public double percentTestNonFaces = 0.1;
    public double percentValidateFaces = 0.1;
    public double percentValidateNonFaces = 0.1;

    String originalTrainFaces = "./res/faces/original-train-face";
    String originalTestFaces = "./res/faces/original-test-face";
    String attFaces = "./res/faces/att";
    String lfw2Faces = "./res/faces/lfw2-19px";

    String originalTrainNonFaces = "./res/non-faces/original-test-non-face";
    String originalTestNonFaces =  "./res/non-faces/original-train-non-face";
    String crawledNonFaces = "./res/non-faces/scraped";


    String manyFacesTest = "./test-res/examples";

    public List<LabeledIntegralImage> negativeSamples;
    public List<LabeledIntegralImage> positiveSamples;
    public List<LabeledIntegralImage> allSamples;

    public List<LabeledIntegralImage> testData;
    public List<LabeledIntegralImage> validationData;

    public Data() throws Exception {
        pathsToFaces = new String[]{originalTrainFaces, attFaces};
        pathsToNonFaces = new String[]{originalTrainNonFaces, originalTestNonFaces, crawledNonFaces};

        // Read images from file system amd calculate integralImages.
        // This now uses our own HalIntegralImage. It seems to work, but there could be bugs.
        ArrayList<HalIntegralImage> faces = new ArrayList<>();
        ArrayList<HalIntegralImage> nonFaces = new ArrayList<>();
        try {
            // Read images for faces
            for(String path : pathsToFaces) faces.addAll(Data.readImagesFromDataBase(path));
             // Read images for non-faces
            for(String path : pathsToNonFaces) nonFaces.addAll(Data.readImagesFromDataBase(path));

        } catch (Exception e) {
            System.err.println("Data folder not found. Have you extracted data.zip correctly?");
            System.exit(1);
        }
        // Re-store arrays of training data as a list and add face label.
        double weightFace = 1.0 / (2 * faces.size());
        double weightNoFace = 1.0 / (2 * nonFaces.size());

        ArrayList<LabeledIntegralImage> allFaces = new ArrayList<>();
        ArrayList<LabeledIntegralImage> allNonFaces = new ArrayList<>();

        for(HalIntegralImage img : faces) allFaces.add(new LabeledIntegralImage(img, true, weightFace));
        for(HalIntegralImage img : nonFaces) allNonFaces.add(new LabeledIntegralImage(img, false, weightNoFace));

        Collections.shuffle(allFaces);
        Collections.shuffle(allNonFaces);

        int endTrainFacesIndex = (int)((allFaces.size()-1)*percentTrainFaces);
        int endTrainNonFacesIndex = (int)((allNonFaces.size()-1)*percentTrainNonFaces);
        int endValidateFacesIndex = (int)((allFaces.size()-1)*(percentTrainFaces+percentValidateFaces));
        int endValidateNonFacesIndex = (int)((allNonFaces.size()-1)*(percentTrainNonFaces+percentValidateNonFaces));
        int endTestFacesIndex = (int)((allFaces.size()-1)*(percentTrainFaces+percentValidateFaces+percentTestFaces));
        int endTestNonFacesIndex = (int)((allNonFaces.size()-1)*(percentTrainNonFaces+percentValidateNonFaces+percentTestNonFaces));

        if(endTestFacesIndex >= allFaces.size() || endTestNonFacesIndex >= allNonFaces.size()) {
            throw new Exception("The final index is larger than allFaces or allNonFaces, perhaps the sum of percentages exceed 1.");
        }

        positiveSamples = new ArrayList<>(allFaces.subList(0, endTrainFacesIndex));
        negativeSamples = new ArrayList<>(allNonFaces.subList(0, endTrainNonFacesIndex));

        allSamples = new ArrayList<>();
        allSamples.addAll(negativeSamples);
        allSamples.addAll(positiveSamples);

        validationData = new ArrayList<>(allFaces.subList(endTrainFacesIndex+1, endValidateFacesIndex));
        validationData.addAll(allNonFaces.subList(endTrainNonFacesIndex+1, endValidateNonFacesIndex));

        testData = new ArrayList<>(allFaces.subList(endValidateFacesIndex+1, endTestFacesIndex));
        testData.addAll(allNonFaces.subList(endValidateNonFacesIndex+1, endTestNonFacesIndex));

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
    public static ArrayList<HalIntegralImage> readImagesFromDataBase(String path) throws Exception {
        File imageFolder = new File(path);
        ArrayList<HalIntegralImage> images = new ArrayList<>(imageFolder.listFiles().length);

        File[] listFiles = imageFolder.listFiles();
        for (int i = 0; i < listFiles.length; i++) {
            File imgFile = listFiles[i];
            BufferedImage bi = ImageIO.read(imgFile);
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

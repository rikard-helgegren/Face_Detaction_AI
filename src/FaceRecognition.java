import Catalano.Imaging.FastBitmap;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Array;
import java.sql.SQLOutput;
import java.util.*;

/**
 * This file should be run with the project root as working directory.
 * Make sure images exist before running.
 */
public class FaceRecognition {
    private static final int degenerateDecisionTreeSize = 2;
    private static final boolean loadFromFile = false; // Set this boolean to load or train.

    private static class LabeledIntegralImage {
        public int isFace; // 1 for true, 0 for false
        public HalIntegralImage img;
        public double weight;

        public LabeledIntegralImage(HalIntegralImage img, int isFace, double weight) {
            this.isFace = isFace;
            this.img = img;
            this.weight = weight;
        }
    }

    private static class ThresholdParity{
        public int threshold;
        public int parity;

        public ThresholdParity(int threshold, int  parity){
            this.threshold = threshold;
            this.parity = parity;
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("1");
        // Read images from file system amd calculate integralImages.
        // This now uses our own HalIntegralImage. It seems to work, but there could be bugs.
        HalIntegralImage[] trainFaces = {};
        HalIntegralImage[] trainNoFaces = {};
        HalIntegralImage[] testFaces = {};
        HalIntegralImage[] testNoFaces = {};
        try {
            System.out.println("1.25");
            // Read images for training set
            trainFaces = readImagesFromDataBase("./res/source/data/train/face"); // Read face images
            trainNoFaces = readImagesFromDataBase("./res/source/data/train/non-face"); // Read no-face images
            System.out.println("1.5");
            // Read images for test set
            testFaces = readImagesFromDataBase("./res/source/data/test/face");
            testNoFaces = readImagesFromDataBase("./res/source/data/test/non-face");
            System.out.println("1.75");
            //System.out.println("Read faces (and the corresponding non faces) from " + faceImagesFolder[i]);
        } catch (IOException e) {
            System.err.println("Data folder not found. Have you extracted data.zip correctly?");
        } catch (Exception e) {
            System.err.println("There was an error reading images.");
            e.printStackTrace();
        }
        System.out.println("2");
        // Calculate initial weights. TODO Verify that this is correct. I'm not sure.
        double weightFace = 1.0 / (2 * trainFaces.length);
        double weightNoFace = 1.0 / (2 * trainNoFaces.length);

        // Re-store arrays of training data as a list and add face label.
        ArrayList<LabeledIntegralImage> trainingData = new ArrayList<>(5000);
        for (HalIntegralImage img : trainFaces) trainingData.add(new LabeledIntegralImage(img, 1, weightFace));
        for (HalIntegralImage img : trainNoFaces) trainingData.add(new LabeledIntegralImage(img, 0, weightNoFace));
        System.out.println("3");

        // Re-store arrays of test data as list and add face label. Test data weights will not be used.
        ArrayList<LabeledIntegralImage> testData = new ArrayList<>(20000);
        for (HalIntegralImage img : testFaces) testData.add(new LabeledIntegralImage(img, 1, 0));
        for (HalIntegralImage img : testNoFaces) testData.add(new LabeledIntegralImage(img, 0, 0));
        Collections.shuffle(testData);
        System.out.println("4");

        ArrayList<Classifier> degenerateDecisionTree;

        if (loadFromFile) {
            // Load strong classifier from file
            degenerateDecisionTree = load("save.classifiers");
        } else {
            // Train strong classifier
            degenerateDecisionTree = train(trainingData);

            // Save strong classifier
            save(degenerateDecisionTree, "save.classifiers");
        }

        // Test strong classifier
        test(degenerateDecisionTree, testData);
        test(degenerateDecisionTree, trainingData);
    }

    /**
     * Trains a network using the AdaBoost algorithm as described in
     * http://www.vision.caltech.edu/html-files/EE148-2005-Spring/pprs/viola04ijcv.pdf
     *
     * @param trainingData the labeled training data
     * @return a degenerate decision tree representing the strong classifier.
     * @throws Exception if something goes wrong
     */
    public static ArrayList<Classifier> train(ArrayList<LabeledIntegralImage> trainingData) throws Exception {
        // Generate all possible features
        ArrayList<Feature> allFeatures = Feature.generateAllFeatures(19, 19);
        Collections.shuffle(allFeatures);

        ArrayList<Classifier> degenerateDecisionTree = new ArrayList<>(degenerateDecisionTreeSize);

        // This is the Adaboost training algorithm


        // For each t
        for(int t=1;t<=degenerateDecisionTreeSize;t++) {
            System.out.println("t = "+t);
            // 1. Normalize weights
            double weightSum = 0;
            for (LabeledIntegralImage img : trainingData) {
                weightSum += img.weight;
            }
            for (LabeledIntegralImage img : trainingData) {
                img.weight = img.weight / weightSum;
            }

            // 2. Train a classifier for every feature. Each is trained on all trainingData
            ArrayList<Classifier> classifiers = new ArrayList<>(allFeatures.size());
            for (int i = 0; i < allFeatures.size(); i++) {
                Feature j = allFeatures.get(i);
                ThresholdParity p = calcBestThresholdAndParity(trainingData, j);
                int threshold = p.threshold;
                int parity = p.parity;


                // Actual step 2
                double error = 0;
                Classifier h = new Classifier(j, threshold, parity); // TODO Calculate parity!! It should be 1 or -1.
                for (LabeledIntegralImage img : trainingData) {
                    error += Math.abs(h.canBeFace(img.img) - img.isFace); // Throws exception
                }
                h.setError(error * weightSum);
                classifiers.add(h);
                if (i % 100 == 0) System.out.printf("Feature %d/%d, t=%d\n", i, allFeatures.size(),t);
            }
            // 3. Choose the classifier with the lowest error
            Classifier bestClassifier = classifiers.get(0);
            for (Classifier c : classifiers) {
                if (c.getError() < bestClassifier.getError()) bestClassifier = c;
            }

            // 4. Update weights
            bestClassifier.setBeta(bestClassifier.getError() / (1 - bestClassifier.getError()));
            bestClassifier.setAlpha(Math.log(1/bestClassifier.getBeta()));
            for (LabeledIntegralImage img : trainingData) {
                // If classifier is right, multiply by beta
                if (bestClassifier.canBeFace(img.img) == img.isFace) img.weight = img.weight * bestClassifier.getBeta();
            }
            degenerateDecisionTree.add(bestClassifier);
            System.out.println("Best classifiers feature: ");
            System.out.println(bestClassifier);
        }
        return degenerateDecisionTree;
    }

    /**
     * Tests a decision tree against some testdata.
     * @param degenerateDecisionTree
     * @param testData
     * @throws Exception
     */
    public static void test(ArrayList<Classifier> degenerateDecisionTree, ArrayList<LabeledIntegralImage> testData) throws Exception {
        System.out.println("Testing decision tree");
        for(Classifier c : degenerateDecisionTree) {
            System.out.println("\t" + c);
        }

        int nrCorrectIsFace = 0;
        int nrWrongIsFace = 0;
        int nrCorrectIsNotFace = 0;
        int nrWrongIsNotFace = 0;
        for(LabeledIntegralImage i:testData){
            if(i.isFace==1){
                if(isFace(degenerateDecisionTree,i.img)){
                    nrCorrectIsFace++;
                }else{
                    nrWrongIsFace++;
                }
            }
            if(i.isFace==0){
                if(!isFace(degenerateDecisionTree,i.img)){
                    nrCorrectIsNotFace++;
                }else{
                    nrWrongIsNotFace++;
                }
            }
        }
        System.out.println("RESULTS");
        //System.out.println("nrCorrectIsFace: "+nrCorrectIsFace+" nrWrongIsFace: "+nrWrongIsFace+" nrCorrectIsNotFace: "+nrCorrectIsNotFace+" nrWrongIsNotFace: "+nrWrongIsNotFace);
        System.out.printf("When the image is     a face. Correct %d. Wrong: %d\n", nrCorrectIsFace, nrWrongIsFace);
        System.out.printf("When the image is not a face. Correct %d. Wrong: %d\n", nrCorrectIsNotFace, nrWrongIsNotFace);
    }



    public static boolean isFace(ArrayList<Classifier> degenerateDecisionTree, HalIntegralImage i) throws Exception{

        //How it looks like you should do accorging to the paper:

        double threshold = 0;
        for(Classifier c:degenerateDecisionTree){
            threshold+=c.getAlpha();
        }
        threshold/=2;

        double value = 0;
        for(Classifier c:degenerateDecisionTree){
            value+=c.getAlpha()*c.canBeFace(i);
        }

        return value>=threshold;

        //Test got 23550 corre475ct and 495 wrong


        //How it looks like you should do according to computerphile
        /*
        for(Classifier c:degenerateDecisionTree){
            if(c.canBeFace(i)!=1) return false;
        }

        return true;
        */
        //Test got 23570 correct and 475 wrong
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
     * Calculates the best threshold for a single weak classifier.
     * @param trainingData the training data used in adaboost.
     * @param j the current feature being evaluated in adaboost
     * @return
     * @throws Exception if calculateFeatureValue throws an exception
     */
    public static ThresholdParity calcBestThresholdAndParity(ArrayList<LabeledIntegralImage> trainingData, Feature j) throws Exception {
        // Sort training data based on features
        trainingData.sort((a, b) -> {
            try {
                // No attention given to order here. Might have to do that if things doesn't work.
                return j.calculateFeatureValue(a.img) - j.calculateFeatureValue(b.img);
            } catch (Exception e) {
                System.err.println("Features could not be sorted due to an error.");
                e.printStackTrace();
            }
            return 0;
        });

        //Go through the sorted training data and store the values from the feature j in featureValues.
        ArrayList<Integer> featureValues = new ArrayList<>(trainingData.size());
        for (LabeledIntegralImage img : trainingData) {
            featureValues.add(j.calculateFeatureValue(img.img));
        }

        int bestThreshold = 0;
        int bestThresholdParity = 0;
        double lowestError = Double.MAX_VALUE; // Corresponding error for the best threshold.
        // TODO In below for loop, i should be 1 to go through all thresholds.
        //  However, it should be fine to take big jumps in i. This SIGNIFICANTLY reduces running time.
        //  Maybe we could even instead of a for loop, basically linear search, use logarithmic search
        //  to find the best threshold much faster.
        for (int i = 0; i < featureValues.size(); i += 100) {
            Integer threshold = featureValues.get(i);
            double tPlus = 0;
            double tMinus = 0;
            double sPlus = 0;
            double sMinus = 0;
            for (LabeledIntegralImage img : trainingData) {
                if (img.isFace == 1) {
                    tPlus += img.weight;
                    if (img.weight < threshold) {
                        sPlus += img.weight;
                    }
                } else if (img.isFace == 0) {
                    tMinus += img.weight;
                    if (img.weight < threshold) {
                        sMinus += img.weight;
                    }
                }
            }
            double error = sMinus + tPlus - sPlus; //Generally: above negative, below positve.
            int parity = 1;
            if(sPlus + tMinus - sMinus < sMinus + tPlus - sPlus){
                error = sPlus + tMinus - sMinus; //Generally: above positive, below negative.
                parity = -1;
            }
            //double error = Math.min(sPlus + tMinus - sMinus, sMinus + tPlus - sPlus);


            if (error < lowestError) {
                lowestError = error;
                // Final best threshold would probably be a value in-between the best threshold and one of the
                // possible thresholds next to it. If we want we can implement that at some point.
                bestThreshold = threshold;
                bestThresholdParity = parity;
            }
        }
        return new ThresholdParity(bestThreshold, bestThresholdParity);
    }


    public static void save(ArrayList<Classifier> classifiers, String fileName){
        try {
            FileOutputStream fileOut = new FileOutputStream(fileName);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(classifiers);
            out.close();
            fileOut.close();
        } catch (IOException e) {
            System.err.println("Save file could not be opened.");
            e.printStackTrace();
        }
    }

    public static ArrayList<Classifier> load(String fileName) throws IOException, ClassNotFoundException {
        ArrayList<Classifier> classifiers;
        FileInputStream fileIn = new FileInputStream(fileName);
        ObjectInputStream in = new ObjectInputStream(fileIn);
        classifiers = (ArrayList<Classifier>) in.readObject();
        in.close();
        fileIn.close();
        return classifiers;
    }

}

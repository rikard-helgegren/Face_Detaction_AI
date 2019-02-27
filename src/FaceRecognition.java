import Catalano.Imaging.FastBitmap;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;

/**
 * This file should be run with the project root as working directory.
 * Make sure images exist before running.
 */
public class FaceRecognition {
    private static final boolean loadFromFile = false; // Set this boolean to load or train.
    private static final double overallFalsePositiveRate = 0.3;
    public static final double DELTA = 0.00001;
    public static PrintWriter writer;

    static {
        try {
            writer = new PrintWriter("the-file-name.txt", "UTF-8");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
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

    private static class PerformanceStats {
        public double truePositive;
        public double falsePositive;
        public double falseNegative;

        public PerformanceStats(double truePositive, double falsePositive, double falseNegative){
            this.truePositive = truePositive;
            this.falsePositive = falsePositive;
            this.falseNegative = falseNegative;
        }

        @Override
        public String toString(){
            return String.format("%.2f detection rate, %.2f falsePositive", truePositive, falsePositive);
        }
    }

    public static void main(String[] args) throws Exception {

        /*

        File imageFolder = new File("./res/source/data/train/face");
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

        Feature.calcHorizontalTwoRectFeature(images[49], 8, 0, 2, 6);

        System.exit(1);

         */




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
            trainFaces = readImagesFromDataBase("./res/source/data/train/face"); // Read face images
            trainNoFaces = readImagesFromDataBase("./res/source/data/train/non-face"); // Read no-face images
            System.out.println("3");
            // Read images for test set
            testFaces = readImagesFromDataBase("./res/source/data/test/face");
            testNoFaces = readImagesFromDataBase("./res/source/data/test/non-face");
            System.out.println("4");
            //System.out.println("Read faces (and the corresponding non faces) from " + faceImagesFolder[i]);
        } catch (Exception e) {
            System.err.println("Data folder not found. Have you extracted data.zip correctly?");
            System.exit(1);
        }
        System.out.println("5");

        // Calculate initial weights. TODO Verify that this is correct. I'm not sure.
        double weightFace = 1.0 / (2 * trainFaces.length);
        double weightNoFace = 1.0 / (2 * trainNoFaces.length);

        // Re-store arrays of training data as a list and add face label.
        ArrayList<LabeledIntegralImage> trainingData = new ArrayList<>(5000);
        for (HalIntegralImage img : trainFaces) trainingData.add(new LabeledIntegralImage(img, 1, weightFace));
        for (HalIntegralImage img : trainNoFaces) trainingData.add(new LabeledIntegralImage(img, 0, weightNoFace));
        System.out.println("6");

        // Re-store arrays of test data as list and add face label. Test data weights will not be used.
        ArrayList<LabeledIntegralImage> testData = new ArrayList<>(20000);
        for (HalIntegralImage img : testFaces) testData.add(new LabeledIntegralImage(img, 1, 0));
        for (HalIntegralImage img : testNoFaces) testData.add(new LabeledIntegralImage(img, 0, 0));
        Collections.shuffle(testData);
        System.out.println("7");

        ArrayList<StrongClassifier> cascadedClassifier = new ArrayList<>();

        if (loadFromFile) {
            // Load strong classifier from file
            cascadedClassifier = load("save.classifiers");
        } else {
            // Train strong classifier

            cascadedClassifier = new ArrayList<StrongClassifier>();


            double maxFalsePositiveRatePerLayer = 0.7;
            double minDetectionRatePerLayer = 0.85;
            double prevFalsePositiveRate = 1;
            double curFalsePositiveRate = 1;
            double prevDetectionRate = 1;
            double curDetectionRate = 1;

            ArrayList<LabeledIntegralImage> negativeSamples = new ArrayList<>();
            ArrayList<LabeledIntegralImage> positiveSamples = new ArrayList<>();
            for (HalIntegralImage img : trainNoFaces) negativeSamples.add(new LabeledIntegralImage(img, 0, weightNoFace));//TODO change maybe
            for (HalIntegralImage img : trainFaces) positiveSamples.add(new LabeledIntegralImage(img, 1, weightFace));


            //The training algorithm for building a cascaded detector
            while(curFalsePositiveRate>overallFalsePositiveRate) {
                System.out.printf("Cascaded classifier. Performance: %s\n", evalCascade(cascadedClassifier, testData));
                for(StrongClassifier c:cascadedClassifier){
                    System.out.println(c);
                }

                ArrayList<LabeledIntegralImage> allSamples = initAdaBoost(positiveSamples, negativeSamples);
                StrongClassifier strongClassifier = new StrongClassifier();
                cascadedClassifier.add(strongClassifier);

                prevDetectionRate = curDetectionRate;
                prevFalsePositiveRate = curFalsePositiveRate;

                while(curFalsePositiveRate > maxFalsePositiveRatePerLayer*prevFalsePositiveRate){
                    System.out.printf("Current false positive rate is %.2f\n", curFalsePositiveRate);
                    System.out.printf("Current detection rate rate is %.2f\n", curDetectionRate);
                    System.out.println(strongClassifier);
                    System.out.printf("Training strong classifier, now with %d weak.\n", strongClassifier.getSize() + 1);

                    strongClassifier.addClassifier(trainOneWeak(allSamples));
                    strongClassifier.setThresholdMultiplier(1);

                    while(true) {
                        System.out.printf("Evaluating threshold multiplier %.2f. With threshold: %.2f. ", cascadedClassifier.get(cascadedClassifier.size()-1).getThresholdMultiplier(), cascadedClassifier.get(cascadedClassifier.size()-1).getThreshold());
                        PerformanceStats stats = evalCascade(cascadedClassifier, testData);
                        System.out.printf("Performance: %s. ", stats);
                        curFalsePositiveRate = stats.falsePositive;
                        curDetectionRate = stats.truePositive;
                        if(curDetectionRate >= minDetectionRatePerLayer * prevDetectionRate) {
                            System.out.printf("GOOD! Using this one. \n");
                            break;
                        } else {
                            System.out.printf("\n");
                        }

                        strongClassifier.setThresholdMultiplier(Math.max(0, strongClassifier.getThresholdMultiplier() - 0.01));
                        if (strongClassifier.getThresholdMultiplier() < DELTA) System.err.println("WARNING, thresholdMultiplier was 0.");
                    }
                    writer.close();
                }

                if(curFalsePositiveRate > overallFalsePositiveRate){
                    negativeSamples = filterData(cascadedClassifier, negativeSamples);
                }


                //degenerateDecisionTree.addAll(train(data, 1));
                //data = filterData(degenerateDecisionTree, data);
            }


            // Save strong classifier
            save(cascadedClassifier, "save.classifiers");
        }

        // Test strong classifier
        test(cascadedClassifier, testData);
        //test(degenerateDecisionTree, trainingData);
    }


    

    public static ArrayList<LabeledIntegralImage> initAdaBoost(ArrayList<LabeledIntegralImage> positiveSamples, ArrayList<LabeledIntegralImage> negativeSamples) throws Exception {
        // Calculate initial weights.
        double weightFace = 1.0 / (2 * positiveSamples.size());
        double weightNoFace = 1.0 / (2 * negativeSamples.size());

        for (LabeledIntegralImage s : positiveSamples) {
            s.setWeight(weightFace);
        }
        for (LabeledIntegralImage s : negativeSamples) {
            s.setWeight(weightNoFace);
        }

        ArrayList<LabeledIntegralImage> allSamples = new ArrayList<>();
        allSamples.addAll(negativeSamples);
        allSamples.addAll(positiveSamples);

        return allSamples;
    }

    /**
     * Trains a network using the AdaBoost algorithm as described in
     * http://www.vision.caltech.edu/html-files/EE148-2005-Spring/pprs/viola04ijcv.pdf
     *
     * @return a degenerate decision tree representing the strong classifier.
     * @throws Exception if something goes wrong
     */
    public static Classifier trainOneWeak(ArrayList<LabeledIntegralImage> allSamples) throws Exception {
        System.out.println("Started training on weak classifier");
        // Generate all possible features
        ArrayList<Feature> allFeatures = Feature.generateAllFeatures(19, 19);
        //Collections.shuffle(allFeatures);

        int size = 1;
        //ArrayList<Classifier> degenerateDecisionTree = new ArrayList<>(size);

        // This is the Adaboost training algorithm

        // 1. Normalize weights
        double weightSum = 0;
        for (LabeledIntegralImage img : allSamples) {
            weightSum += img.getWeight();
        }
        System.out.println("ws: "+weightSum);
        for (LabeledIntegralImage img : allSamples) {
            img.setWeight(img.getWeight() / weightSum);
        }

        // 2. Train a classifier for every feature. Each is trained on all trainingData
        ArrayList<Classifier> classifiers = new ArrayList<>(allFeatures.size());
        for (int i = 0; i < allFeatures.size(); i++) {
            Feature j = allFeatures.get(i);
            ThresholdParity p = calcBestThresholdAndParity(allSamples, j);
            int threshold = p.threshold;
            int parity = p.parity;
            //System.out.println("T & P: "+threshold+", "+parity);
            // Actual step 2
            double error = 0;
            Classifier h = new Classifier(j, threshold, parity);
            for (LabeledIntegralImage img : allSamples) {
                error += img.getWeight() * Math.abs(h.canBeFace(img.img) - img.isFace); // Throws exception
            }
            //System.out.println("Error for this feature: "+error);

            //System.out.println("Parity for this feature: "+parity);
            h.setError(error);
            classifiers.add(h);
            if (i % 2000 == 0) System.out.printf("Feature %d/%d\n", i, allFeatures.size());
        }
        // 3. Choose the classifier with the lowest error
        Classifier bestClassifier = classifiers.get(0);
        for (Classifier c : classifiers) {
            if (c.getError() < bestClassifier.getError()) bestClassifier = c;
        }

        System.out.println("Best classifier choosen:");
        // 4. Update weights
        System.out.println("Error: " + bestClassifier.getError());
        System.out.println("Beta: " + bestClassifier.getError() / (1 - bestClassifier.getError()));
        bestClassifier.setBeta(bestClassifier.getError() / (1 - bestClassifier.getError()));
        //System.out.println("Beta is " + bestClassifier.getBeta());
        //System.out.println("Setting alpha to " + Math.log(1/bestClassifier.getBeta()));
        System.out.println("Alpha: " + Math.log(1.0/bestClassifier.getBeta()));
        System.out.println();
        bestClassifier.setAlpha(Math.log(1.0/bestClassifier.getBeta()));
        //System.out.println("Testing Alpha:");
        //System.out.println(bestClassifier.getBeta());
        //System.out.println(Math.log(1/bestClassifier.getBeta()));
        //System.out.println(bestClassifier.getAlpha());
        for (LabeledIntegralImage img : allSamples) {
            // If classifier is right, multiply by beta
            if (bestClassifier.canBeFace(img.img) == img.isFace) {
                img.setWeight(img.getWeight() * bestClassifier.getBeta());
            }
        }
        //degenerateDecisionTree.add(bestClassifier);
        //System.out.println("Best classifiers feature: ");
        //System.out.println(bestClassifier);

        return bestClassifier;
    }

    /**
     * Tests a decision tree against some testdata.
     * @param degenerateDecisionTree
     * @param testData
     * @throws Exception
     */
    public static void test(ArrayList<StrongClassifier> degenerateDecisionTree, ArrayList<LabeledIntegralImage> testData) throws Exception {
        System.out.println("Testing decision tree");
        for(StrongClassifier c : degenerateDecisionTree) {
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
        System.out.printf("When the image is     a face. Correct: %d. Wrong: %d.", nrCorrectIsFace, nrWrongIsFace);
        System.out.printf(" True positive: %.1f %%. False negative: %.1f %%\n",
                100*(double)nrCorrectIsFace/(nrCorrectIsFace+nrWrongIsFace),
                100* evalCascade(degenerateDecisionTree, testData).falseNegative);
        System.out.printf("When the image is not a face. Correct: %d. Wrong: %d.", nrCorrectIsNotFace, nrWrongIsNotFace);
        System.out.printf(" True negative: %.1f %%. False positive: %.1f %%\n",
                100*(double)nrCorrectIsNotFace/(nrCorrectIsNotFace+nrWrongIsNotFace),
                100* evalCascade(degenerateDecisionTree, testData).falsePositive);
        System.out.printf("Total number of correct guesses: %d. Wrong: %d\n", nrCorrectIsFace+nrCorrectIsNotFace,nrWrongIsFace+nrWrongIsNotFace);
    }

    /**
     * Removes the data that does not pass the strong classifier.
     * @param cascadedClassifier
     * @param data
     * @return an array of images that the strong classifier thinks could be faces
     */
    public static ArrayList<LabeledIntegralImage> filterData(ArrayList<StrongClassifier> cascadedClassifier, ArrayList<LabeledIntegralImage> data) throws Exception {
        ArrayList<LabeledIntegralImage> maybeFaces = new ArrayList<>(data.size()/2);
        for (LabeledIntegralImage d : data) {
            if (isFace(cascadedClassifier, d.img)) {
                maybeFaces.add(d);
            }
        }
        return maybeFaces;
    }

    public static PerformanceStats evalCascade(ArrayList<StrongClassifier> decisionTree, ArrayList<LabeledIntegralImage> testData) throws Exception {
        int nrCorrectIsFace = 0;
        int nrWrongIsFace = 0;
        int nrCorrectIsNotFace = 0;
        int nrWrongIsNotFace = 0;
        for(LabeledIntegralImage i:testData){
            if(i.isFace==1){
                if(isFace(decisionTree,i.img)){
                    nrCorrectIsFace++;
                }else{
                    nrWrongIsFace++;
                }
            }
            if(i.isFace==0){
                if(!isFace(decisionTree,i.img)){
                    nrCorrectIsNotFace++;
                }else{
                    nrWrongIsNotFace++;
                }
            }
        }
        double falsePositive = ((double)nrWrongIsNotFace) / (nrCorrectIsNotFace + nrWrongIsNotFace);
        double truePositive  = ((double)nrCorrectIsFace)  / (nrCorrectIsFace    + nrWrongIsFace);
        double falseNegative = ((double)nrWrongIsFace)    / (nrCorrectIsFace    + nrWrongIsFace);

        return new PerformanceStats(truePositive, falsePositive, falseNegative);
    }

    public static boolean isFace(ArrayList<StrongClassifier> strongClassifiers, HalIntegralImage i) throws Exception{
        //How it looks like you should do according to computerphile
        for(StrongClassifier c : strongClassifiers){
            if(!c.canBeFace(i)) return false;
        }

        return true;
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
        //TODO Sort this directly?
        //Go through the sorted training data and store the values from the feature j in featureValues.
        ArrayList<Integer> featureValues = new ArrayList<>(trainingData.size());
        for (LabeledIntegralImage img : trainingData) {
            featureValues.add(j.calculateFeatureValue(img.img));
        }

        //System.out.println("Sorted list from feture: "+j+": First value"+featureValues.get(0)+", Last: "+featureValues.get(featureValues.size()-1));

        //System.out.println("Testing feature: "+j);

        int bestThreshold = 0;
        int bestThresholdParity = 0;
        double lowestError = Double.MAX_VALUE; // Corresponding error for the best threshold.
        // TODO In below for loop, i should be 1 to go through all thresholds.
        //  However, it should be fine to take big jumps in i. This SIGNIFICANTLY reduces running time.
        //  Maybe we could even instead of a for loop, basically linear search, use logarithmic search
        //  to find the best threshold much faster.
        for (int i = 0; i < featureValues.size(); i += 100) {
            //Integer threshold = featureValues.get(i);
            Integer threshold = featureValues.get(i);
            //System.out.println("Threshold nr: "+i+" = "+threshold);
            double tPlus = 0;
            double tMinus = 0;
            double sPlus = 0;
            double sMinus = 0;
            //System.out.println("Looping through all trainingdata");
            for (int k=0; k<trainingData.size(); k++) {
                LabeledIntegralImage img = trainingData.get(k);
                if (img.isFace == 1) {
                    tPlus += img.getWeight();
                    //if (img.getWeight() < threshold) {
                    if (k < i) {
                        sPlus += img.getWeight();
                        //System.out.println("isFace: It ("+img.getWeight()+") is below threshold: "+threshold);
                    }else{
                        //System.out.println("isFace: It is above threshold: "+threshold);
                    }
                } else if (img.isFace == 0) {
                    tMinus += img.getWeight();
                    if (k < i) {
                        sMinus += img.getWeight();
                        //System.out.println("It is below threshold: "+threshold);
                    }else{
                        //System.out.println("It is above threshold: "+threshold);
                    }
                }
            }
            double error = sMinus + tPlus - sPlus; //Generally: above negative, below positve.
            int parity = 1;
            if(sPlus + tMinus - sMinus < sMinus + tPlus - sPlus){
                error = sPlus + tMinus - sMinus; //Generally: above positive, below negative.
                parity = -1;
                //System.out.println("sPlus + tMinus - sMinus is the smallest: "+(sPlus + tMinus - sMinus)+" instead of: "+(sMinus + tPlus - sPlus));
            }else{
                //System.out.println("sMinus + tPlus - sPlus is the smallest: "+(sMinus + tPlus - sPlus)+" instead of: "+(sPlus + tMinus - sMinus));
            }
            //double error = Math.min(sPlus + tMinus - sMinus, sMinus + tPlus - sPlus);
            //System.out.println("Error for this threshold: "+error);

            if (error < lowestError) {
                lowestError = error;
                // Final best threshold would probably be a value in-between the best threshold and one of the
                // possible thresholds next to it. If we want we can implement that at some point.
                bestThreshold = threshold;
                bestThresholdParity = parity;
            }
        }
        //System.out.println("Best threshold for this one: "+bestThreshold);
        return new ThresholdParity(bestThreshold, bestThresholdParity);
    }


    public static void save(ArrayList<StrongClassifier> classifiers, String fileName){
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

    public static ArrayList<StrongClassifier> load(String fileName) throws IOException, ClassNotFoundException {
        ArrayList<StrongClassifier> classifiers;
        FileInputStream fileIn = new FileInputStream(fileName);
        ObjectInputStream in = new ObjectInputStream(fileIn);
        classifiers = (ArrayList<StrongClassifier>) in.readObject();
        in.close();
        fileIn.close();
        return classifiers;
    }

}

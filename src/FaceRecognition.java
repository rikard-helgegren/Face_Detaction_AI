import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

// Target: 70% true positive, 105 false positive
/**
 * This file should be run with the project root as working directory.
 * Make sure images exist before running.
 */
public class FaceRecognition {
    private static final boolean trainFullCascade = false; // Should a cascade be trained? If not, a strong will be trained.
    private static final boolean loadFromFile = false; // Set this boolean to loadCascade or train.
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

    public static class ThresholdParity{
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

        Data data = new Data();
        
        if (trainFullCascade) {
            System.out.println("Starting training of cascaded classifier.");
            ArrayList<StrongClassifier> cascadedClassifier;

            if (loadFromFile) {
                // Load strong classifier from file
                cascadedClassifier = Data.loadCascade("cascade1.cascade");
            } else {
                cascadedClassifier = trainCascadedClassifier(data.positiveSamples, data.negativeSamples, data.testData);
                // Save cascaded classifier
                Data.saveCascade(cascadedClassifier, "save.cascade");
            }
            test(cascadedClassifier, data.testData);
        } else {
            System.out.println("Starting training of strong classifier.");
            StrongClassifier strongClassifier;
            if (loadFromFile) {
                // Load strong classifier from file
                strongClassifier = Data.loadStrong("save.strong");
            } else {
                strongClassifier = trainTestStrongClassifier(data.allSamples, 10, data.testData);
                // Save cascaded classifier
                Data.saveStrong(strongClassifier, "save.strong");
            }
            testStrong(strongClassifier, data.testData);
        }

    }

    /**
     * Trains a strong classifier and tests it, just for status purposes, between each weak classifier.
     * @param trainingData
     * @param size
     * @param testData
     * @return
     * @throws Exception
     */
    public static StrongClassifier trainTestStrongClassifier(ArrayList<LabeledIntegralImage> trainingData, int size,
                                                             ArrayList<LabeledIntegralImage> testData) throws Exception {

        StrongClassifier strongClassifier = new StrongClassifier(); // Init
        strongClassifier.setThresholdMultiplier(0.5); // Threshold is default 0.5 in AdaBoost

        for (int i = 0; i < size; i++) {
            System.out.printf("Training weak classifier %d/%d.\n", i + 1, size);
            strongClassifier = trainStrongClassifier(trainingData, strongClassifier, 1);
            testStrong(strongClassifier, testData);
        }

        return strongClassifier;
    }

    /**
     * Trains more weak classifiers into a strong classifier.
     * @param trainingData the training data. Usually, you want weights to not change between calls on same classifier.
     * @param strongClassifier the strong classifier to train into. Can be an empty one. Will be modified in-place.
     * @param extraSize how many weak classifiers to train now.
     * @return a strong classifier with extraSize more weak classifiers than the input one had.
     * @throws Exception
     */
    public static StrongClassifier trainStrongClassifier(
            ArrayList<LabeledIntegralImage> trainingData, StrongClassifier strongClassifier, int extraSize) throws Exception {
        for (int i = 0; i < extraSize; i++) {
            strongClassifier.addClassifier(trainOneWeak(trainingData));
        }
        return strongClassifier;
    }

    public static ArrayList<StrongClassifier> trainCascadedClassifier(
            ArrayList<LabeledIntegralImage> positiveSamples,
            ArrayList<LabeledIntegralImage> negativeSamples,
            ArrayList<LabeledIntegralImage> testData) throws Exception {

        // Train cascaded classifier
        ArrayList<StrongClassifier> cascadedClassifier = new ArrayList<StrongClassifier>();

        double maxFalsePositiveRatePerLayer = 0.7;
        double minDetectionRatePerLayer = 0.9;
        double prevFalsePositiveRate = 1;
        double curFalsePositiveRate = 1;
        double prevDetectionRate = 1;
        double curDetectionRate = 1;

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

                if (strongClassifier.getSize() == 0) {
                    strongClassifier.addClassifier(trainOneWeak(allSamples));
                    strongClassifier.addClassifier(trainOneWeak(allSamples));
                } else {
                    strongClassifier.addClassifier(trainOneWeak(allSamples));
                }
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
                negativeSamples = Data.filter(cascadedClassifier, negativeSamples);
            }
        }

        return cascadedClassifier;
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
    // Takes 16s without sorting and recalculation of featureValues in calcThreshold.
    // Takes 107s with sorting and recalculation.
    // Takes 59s with sorting but without recalculation.
    // Takes 13s with sorting but without recalculation and with 8 threads. (Current)
    public static Classifier trainOneWeak(ArrayList<LabeledIntegralImage> allSamples) throws Exception {
        long t0 = System.currentTimeMillis();
        //System.out.println("Started training on weak classifier");
        // Generate all possible features
        //ArrayList<Feature> allFeatures = Feature.generateAllFeatures(19, 19);
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
        //Queue<Classifier> classifiers = adaBoostStepTwo(allSamples); // Single thread
        Queue<Classifier> classifiers = adaBoostStepTwoThreaded(allSamples, 8); // Multi-thread

        // 3. Choose the classifier with the lowest error
        //Classifier bestClassifier = classifiers.get(0);
        Classifier bestClassifier = classifiers.poll();
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

        System.out.printf("Trained one weak classifier in %ds\n", (System.currentTimeMillis() - t0) / 1000);
        return bestClassifier;
    }

    public static Queue<Classifier> adaBoostStepTwo(List<LabeledIntegralImage> allSamples) throws Exception {
        Queue<Classifier> classifiers = new LinkedList<>();
        for (int i = 0; i < Feature.allFeatures.size(); i++) {
            Feature j = Feature.allFeatures.get(i);
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
            if (i % 2000 == 0) System.out.printf("Feature %d/%d\n", i, Feature.allFeatures.size());
        }
        return classifiers;
    }

    public static Queue<Classifier> adaBoostStepTwoThreaded(ArrayList<LabeledIntegralImage> allSamples, int cores) throws InterruptedException {
        // Multithreaded version of step 2
        ConcurrentLinkedQueue<Classifier> classifiers = new ConcurrentLinkedQueue<>(); // List of classifiers
        ArrayList<Thread> threads = new ArrayList<>(); // List of all threads
        int partitions = cores; // How many partitions to divide allFeatures into. This is the same as number of threads.

        // Partition data and create all but the last thread.
        int start = 0;
        for (int i = 0; i < partitions-1; i++) {
            int end = start + Feature.allFeatures.size() / partitions;
            threads.add(new AdaTwo(Feature.allFeatures.subList(start, end), classifiers, (List<LabeledIntegralImage>) allSamples.clone()));
            start = end;
        }
        // Create the last thread. Special case since it may have a slightly different length than the others.
        threads.add(new AdaTwo(Feature.allFeatures.subList(start, Feature.allFeatures.size()-1), classifiers, (List<LabeledIntegralImage>) allSamples.clone()));

        // Start all threads
        for (Thread t : threads) {
            t.start();
        }

        // Wait for all threads to finish.
        for (Thread t : threads) {
            t.join();
        }
        return classifiers;
    }

    public static void testStrong(StrongClassifier strongClassifier, ArrayList<LabeledIntegralImage> testData) throws Exception {
        System.out.println("Testing Strong Classifier");
        System.out.println(strongClassifier);

        //strongClassifier = new StrongClassifier(strongClassifier, 1);
        PerformanceStats stats = null;
        try {
            stats = evalStrong(strongClassifier, testData);
        } catch (Exception e) {
            System.out.println("The evaluation of the strong classifier failed.");
            e.printStackTrace();
        }

        System.out.printf("Performance was %s\n", stats);

    }

    /**
     * Tests a decision tree against some testdata.
     * @param degenerateDecisionTree
     * @param testData
     * @throws Exception
     */
    public static void test(ArrayList<StrongClassifier> degenerateDecisionTree, ArrayList<LabeledIntegralImage> testData) throws Exception {
        System.out.println("Testing Cascade Classifier");
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

    public static PerformanceStats evalStrong(StrongClassifier strongClassifier, ArrayList<LabeledIntegralImage> testData) throws Exception {
        int nrCorrectIsFace = 0;
        int nrWrongIsFace = 0;
        int nrCorrectIsNotFace = 0;
        int nrWrongIsNotFace = 0;
        for(LabeledIntegralImage i:testData){
            if(i.isFace==1){
                if(strongClassifier.canBeFace(i.img)){
                    nrCorrectIsFace++;
                }else{
                    nrWrongIsFace++;
                }
            }
            if(i.isFace==0){
                if(!strongClassifier.canBeFace(i.img)){
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

    public static ThresholdParity calcAvgThresholdAndParity(ArrayList<LabeledIntegralImage> trainingData, Feature j) throws Exception {
        trainingData.sort((a, b) -> {
            try {
                // The order here matters.
                return a.img.getFeatureValue(j) - b.img.getFeatureValue(j);
            } catch (Exception e) {
                System.err.println("Features could not be sorted due to an error.");
                e.printStackTrace();
            }
            return 0;
        });
        //TODO Sort this directly?
        //Go through the sorted training data and store the values from the feature j in featureValues.
        ArrayList<Integer> featureValues = new ArrayList<>(trainingData.size());
        int featureValueSum = 0;
        for (LabeledIntegralImage img : trainingData) {
            int fv = img.img.getFeatureValue(j);
            featureValues.add(fv);
            featureValueSum += fv;
        }

        double threshold = Math.round((double) featureValueSum / trainingData.size());
        int parity = (threshold > 0) ? -1 : 1; // If threshold > 0, parity = -1.
        return new ThresholdParity((int) threshold, parity);

    }

    /**
     * Calculates the best threshold for a single weak classifier.
     * @param trainingData the training data used in adaboost.
     * @param j the current feature being evaluated in adaboost
     * @return
     * @throws Exception if calculateFeatureValue throws an exception
     */
    public static ThresholdParity calcBestThresholdAndParity(List<LabeledIntegralImage> trainingData, Feature j) throws Exception {

        // DONE Feature values are no longer calculated every time.
        // TODO If possible, move sorting so it does not happen every time.
        // Sort training data based on features
        trainingData.sort((a, b) -> {
            try {
                // The order here matters.
                //return j.calculateFeatureValue(a.img) - j.calculateFeatureValue(b.img);
                return a.img.getFeatureValue(j) - b.img.getFeatureValue(j);
            } catch (Exception e) {
                System.err.println("Features could not be sorted due to an error.");
                e.printStackTrace();
            }
            return 0;
        });
        //Go through the sorted training data and store the values from the feature j in featureValues.
        ArrayList<Integer> featureValues = new ArrayList<>(trainingData.size());
        for (LabeledIntegralImage img : trainingData) {
            featureValues.add(img.img.getFeatureValue(j));
        }

        //System.out.println("Sorted list from feature: "+j+": First value"+featureValues.get(0)+", Last: "+featureValues.get(featureValues.size()-1));

        //System.out.println("Testing feature: "+j);

        int bestThreshold = 0;
        int bestThresholdParity = 0;
        double lowestError = Double.MAX_VALUE; // Corresponding error for the best threshold.
        // TODO In below for loop, i should be 1 to go through all thresholds.
        //  However, it should be fine to take big jumps in i. This SIGNIFICANTLY reduces running time.
        //  Maybe we could even instead of a for loop, basically linear search, use logarithmic search
        //  to find the best threshold much faster.
        for (int i = 0; i < featureValues.size(); i += 100) {
            Integer threshold = featureValues.get(i);
            //Integer threshold = trainingData.get(i).getFeatureValue(j);
            //System.out.println("Threshold nr: "+i+" = "+threshold);
            double tPlus = 0;
            double tMinus = 0;
            double sPlus = 0;
            double sMinus = 0;
            //System.out.println("Looping through all trainingdata");
            // TODO Check calculations here by hand
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


}

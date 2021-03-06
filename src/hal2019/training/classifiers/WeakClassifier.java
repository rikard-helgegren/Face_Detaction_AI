package hal2019.training.classifiers;

import hal2019.*;
import hal2019.training.Feature;
import hal2019.training.PerformanceStats;
import hal2019.training.ThresholdParity;
import hal2019.training.TrainClassifiers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class WeakClassifier extends FaceDetector implements Serializable {

    private static final long serialVersionUID = 0; // Increase when changing something in this class

    private Feature feature;
    private int threshold;
    private int parity; // should be +1 or -1
    private double error;
    private double beta;
    private double alpha;
    public PerformanceStats testPerformance;
    public PerformanceStats trainPerformance;

    public WeakClassifier(List<LabeledIntegralImage> trainingData) throws Exception {
        this(trainOneWeak(trainingData));
    }

    private WeakClassifier(WeakClassifier c) {
        feature = c.feature;
        threshold = c.threshold;
        parity = c.parity;
        error = c.error;
        beta = c.beta;
        alpha = c.alpha;
    }

    /**
     * Constructs a classifier that uses the the given type of feature with the given values.
     * @param feature the feature this classifier should use.
     */
    public WeakClassifier(Feature feature, int threshold, int parity) throws Exception {
        this.feature = feature;
        this.threshold = threshold;
        this.parity = parity;
        setError(0);
        setBeta(0);
        this.alpha = 0;
    }

    public double getError() {
        return error;
    }

    public void setError(double error) throws Exception {
        if (error < 0 || error > 1) throw new Exception("Error must be in [0, 1]. Was: " + error);
        this.error = error;
    }

    public double getBeta() {
        return beta;
    }

    public void setBeta(double beta) throws Exception {
        if (beta < -0.01) throw new Exception("Beta must be in [0, inf). Was: " + beta); // TODO maybe can be 0 instead
        if (beta < 0 && beta > 0 - TrainClassifiers.DELTA) beta = 0;
        this.beta = beta;
    }

    public double getAlpha() {
        return alpha;
    }

    public void setAlpha(double alpha) {
        this.alpha = alpha;
    }

    /**
     *
     * @param img the image to classify
     * @return 1 if this classifier thinks the image might be a face and 0 otherwise.
     * @throws Exception
     */
    public boolean canBeFace(HalIntegralImage img) throws Exception {
        if (parity != 1 && parity != -1) throw new Exception("Parity was not 1 or -1. It was: " + parity);

        return parity * img.getFeatureValue(feature) < parity * threshold;
    }

    /**
     * Sets weights on positive and negative samples, combines them, and returns the new list.
     *
     * Use this to prepare your data before training and after removing data.
     *
     * @param positiveSamples
     * @param negativeSamples
     * @return
     * @throws Exception
     */
    public static ArrayList<LabeledIntegralImage> initForAdaBoost(
            List<LabeledIntegralImage> positiveSamples, List<LabeledIntegralImage> negativeSamples) throws Exception {
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
     * Runs one iteration of the AdaBoost algorithm as described in
     * http://www.vision.caltech.edu/html-files/EE148-2005-Spring/pprs/viola04ijcv.pdf
     * that creates a single weak classifier.
     *
     * @return a single weak classifier trained from one iteration of AdaBoost
     * @throws Exception if something goes wrong
     */
    private static WeakClassifier trainOneWeak(List<LabeledIntegralImage> allSamples) throws Exception {
        long t0 = System.currentTimeMillis();

        // 1. Normalize weights
        double weightSum = 0;
        for (LabeledIntegralImage img : allSamples) {
            weightSum += img.getWeight();
        }
        for (LabeledIntegralImage img : allSamples) {
            img.setWeight(img.getWeight() / weightSum);
        }

        // 2. Train a classifier for every feature. Each is trained on all trainingData
        //Queue<WeakClassifier> classifiers = adaBoostStepTwo(allSamples); // Single thread
        Queue<WeakClassifier> classifiers = adaBoostStepTwoThreaded(allSamples, 8); // Multi-thread

        // 3. Choose the classifier with the lowest error
        WeakClassifier bestClassifier = classifiers.poll();
        for (WeakClassifier c : classifiers) {
            if (c.getError() < bestClassifier.getError()) bestClassifier = c;
        }

        // 4. Update weights
        bestClassifier.setBeta(bestClassifier.getError() / (1 - bestClassifier.getError()));
        bestClassifier.setAlpha(Math.log(1.0/bestClassifier.getBeta()));
        for (LabeledIntegralImage img : allSamples) {
            // If classifier is right, multiply by beta
            if (bestClassifier.canBeFace(img.img) == img.isFace) {
                img.setWeight(img.getWeight() * bestClassifier.getBeta());
            }
        }

        System.out.printf("Trained one weak classifier in %d seconds.\n", (System.currentTimeMillis() - t0) / 1000);
        return bestClassifier;
    }


    /**
     * Performs step 2 of adaboost using a single-threaded approach.
     * @param allSamples
     * @return
     * @throws Exception
     */
    public static Queue<WeakClassifier> adaBoostStepTwo(List<LabeledIntegralImage> allSamples) throws Exception {
        Queue<WeakClassifier> classifiers = new LinkedList<>();
        for (int i = 0; i < Feature.allFeatures.size(); i++) {
            Feature j = Feature.allFeatures.get(i);
            ThresholdParity p = calcBestThresholdAndParity(allSamples, j);
            int threshold = p.threshold;
            int parity = p.parity;

            // Actual step 2
            double error = 0;
            WeakClassifier h = new WeakClassifier(j, threshold, parity);
            for (LabeledIntegralImage img : allSamples) {
                int canBeFace = (h.canBeFace(img.img)) ? 1 : 0;
                int isFace = (img.isFace) ? 1 : 0;
                error += img.getWeight() * Math.abs(canBeFace - isFace); // Throws exception
            }

            h.setError(error);
            classifiers.add(h);
            if (i % 2000 == 0) System.out.printf("Feature %d/%d\n", i, Feature.allFeatures.size());
        }
        return classifiers;
    }

    /**
     * Performs step 2 of adaboost using a multithreaded approach.
     * @param allSamples
     * @param partitions how many partitions to divide all samples into. One thread will be started for each partition.
     * @return
     * @throws InterruptedException
     */
    private static Queue<WeakClassifier> adaBoostStepTwoThreaded(List<LabeledIntegralImage> allSamples, int partitions) throws InterruptedException {
        ConcurrentLinkedQueue<WeakClassifier> classifiers = new ConcurrentLinkedQueue<>(); // List of classifiers
        List<Thread> threads = new ArrayList<>(); // List of all threads

        // Partition data and create all but the last thread.
        int start = 0;
        for (int i = 0; i < partitions-1; i++) {
            int end = start + Feature.allFeatures.size() / partitions;
            threads.add(new AdaTwo(Feature.allFeatures.subList(start, end), classifiers, new ArrayList<>(allSamples)));
            start = end;
        }
        // Create the last thread. Special case since it may have a slightly different length than the others.
        threads.add(new AdaTwo(Feature.allFeatures.subList(start, Feature.allFeatures.size()-1), classifiers, new ArrayList<>(allSamples)));

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

    /**
     * Calculates the best threshold for a single weak classifier.
     * @param trainingData the training data used in adaboost.
     * @param j the current feature being evaluated in adaboost
     * @return
     * @throws Exception if calculateFeatureValue throws an exception
     */
    public static ThresholdParity calcBestThresholdAndParity(List<LabeledIntegralImage> trainingData, Feature j) throws Exception {
        // Sort training data based on feature value
        trainingData.sort((a, b) -> {
            try {
                // The order here seemingly matters.
                return a.img.getFeatureValue(j) - b.img.getFeatureValue(j);
            } catch (Exception e) {
                System.err.println("Features could not be sorted due to an error.");
                e.printStackTrace();
            }
            return 0;
        });

        // Create a list of all feature j's feature values in the same order as the sorted training data
        ArrayList<Integer> featureValues = new ArrayList<>(trainingData.size());
        for (LabeledIntegralImage img : trainingData) {
            featureValues.add(img.img.getFeatureValue(j));
        }

        // Find the best threshold and corresponding parity by calculating at which threshold the feature divides the data best
        int bestThreshold = 0;
        int bestThresholdParity = 0;
        double lowestError = Double.MAX_VALUE; // Corresponding error for the best threshold.
        // In below for loop, 1 should be added to i to go through all thresholds.
        // However, it should be fine to take big jumps in i. This SIGNIFICANTLY reduces running time.
        // Maybe we could even instead of a for loop, basically linear search, use logarithmic search
        // to find the best threshold much faster.
        for (int i = 0; i < featureValues.size(); i += 100) {
            Integer threshold = featureValues.get(i);
            double tPlus = 0;
            double tMinus = 0;
            double sPlus = 0;
            double sMinus = 0;
            for (int k=0; k<trainingData.size(); k++) {
                LabeledIntegralImage img = trainingData.get(k);
                if (img.isFace) {
                    tPlus += img.getWeight();
                    if (k < i) {
                        sPlus += img.getWeight();
                    }
                } else if (!img.isFace) {
                    tMinus += img.getWeight();
                    if (k < i) {
                        sMinus += img.getWeight();
                    }
                }
            }
            double error = sMinus + tPlus - sPlus; //Generally: above negative, below positve.
            int parity = 1;
            if(sPlus + tMinus - sMinus < sMinus + tPlus - sPlus){
                error = sPlus + tMinus - sMinus; //Generally: above positive, below negative.
                parity = -1;
            }

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


    public Feature getFeature() {
        return feature;
    }

    public String toString(){
        String s = String.format("[ %s, threshold %6d, parity %2d, error %.3f, beta %.3f, alpha %.3f. ",
                feature, threshold, parity, error, beta, alpha);
        if (trainPerformance != null) {
            s += String.format("Train: %s. ", trainPerformance);
        }
        if (testPerformance != null) {
            s += String.format("Test: %s. ", testPerformance);
        }
        return s + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof WeakClassifier)) return false;
        WeakClassifier c = (WeakClassifier) o;

        if (feature.equals(c.feature) && threshold == c.threshold && parity == c.parity &&
                error == c.error && beta == c.beta && alpha == c.alpha) {
            return true;
        }
        return false;
    }
}

package hal2019.training;

import hal2019.Data;
import hal2019.training.classifiers.CascadeClassifier;
import hal2019.training.classifiers.StrongClassifier;

/**
 * This file should be run with the project root as working directory.
 * Make sure images exist before running.
 */
public class TrainClassifiers {
    // True if a cascade classifier should be used. Otherwise a strong classifier.
    private static final boolean trainFullCascade = true;

    // True if a network should be loaded. Otherwise, one will be trained.
    private static final boolean loadFromFile = false;

    public static final double DELTA = 0.00001;
    public static final int trainingDataWidth = 19;
    public static final int trainingDataHeight = 19;

    public static void main(String[] args) throws Exception {
        
        Data data = new Data();
        
        if (trainFullCascade) {
            System.out.println("Starting training of cascaded classifier.");
            CascadeClassifier cascadedClassifier;

            if (loadFromFile) {
                // Load strong classifier from file
                cascadedClassifier = new CascadeClassifier("saves/save.cascade");
            } else {
                cascadedClassifier = new CascadeClassifier(
                        0.001,
                        0.4,
                        0.995,
                        data.positiveSamples, data.negativeSamples, data.validationData);
                // Save cascaded classifier
                cascadedClassifier.save("saves/save.cascade");
            }
            cascadedClassifier.test(data.testData);
        } else {
            System.out.println("Starting training of strong classifier.");
            StrongClassifier strongClassifier;
            if (loadFromFile) {
                // Load strong classifier from file
                strongClassifier = new StrongClassifier("saves/save.strong");
            } else {
                strongClassifier = new StrongClassifier(2, data.allSamples, data.testData);
                // Save cascaded classifier
                strongClassifier.save("saves/save.strong");
            }
            strongClassifier.test(data.testData);
        }

    }

}

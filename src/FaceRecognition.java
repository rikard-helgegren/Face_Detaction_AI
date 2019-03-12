// Target: 70% true positive, 105 false positive
/**
 * This file should be run with the project root as working directory.
 * Make sure images exist before running.
 */
public class FaceRecognition {
    // Should an entire cascade be trained? If not, a strong classifier will be trained.
    private static final boolean trainFullCascade = true;
    private static final boolean loadFromFile = false; // Set this boolean to loadCascade or train.
    //The overall false positive rate the cascaded classifier should reach.

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
                cascadedClassifier = new CascadeClassifier("save.cascade");
            } else {
                cascadedClassifier = new CascadeClassifier(
                        0.01,
                        0.5,
                        0.99,
                        data.positiveSamples, data.negativeSamples, data.validationData);
                // Save cascaded classifier
                cascadedClassifier.save("save.cascade");
            }
            cascadedClassifier.test(data.testData);
        } else {
            System.out.println("Starting training of strong classifier.");
            StrongClassifier strongClassifier;
            if (loadFromFile) {
                // Load strong classifier from file
                strongClassifier = new StrongClassifier("save.strong");
            } else {
                strongClassifier = new StrongClassifier(200, data.allSamples, data.testData);
                // Save cascaded classifier
                strongClassifier.save("save.strong");
            }
            strongClassifier.test(data.testData);
        }

    }

}

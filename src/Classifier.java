public class Classifier {

    private Feature feature;
    private double threshold;
    private int parity; // should be +1 or -1
    private double error;

    /**
     * Constructs a classifier that uses the the given type of feature with the given values.
     * @param feature the feature this classifier should use.
     */
    public Classifier(Feature feature, int threshold, int parity) {
        this.feature = feature;
        this.threshold = threshold;
        this.parity = parity;
        this.error = 0;
    }

    // TODO Takes some parameter to update the error value for this classifier.
    public void updateError() {

    }

    public double getError() {
        return error;
    }

    public void setError(double error) {
        this.error = error;
    }

    public int canBeFace(HalIntegralImage img) throws Exception {
        if (parity != 1 && parity != -1) throw new Exception("Parity was not 1 or -1. It was: " + parity);
        if (parity * feature.calculateFeatureValue(img) < parity * threshold) {
            return 1;
        }
        return 0;
    }


}

public class Classifier {

    private Feature feature;
    private double threshold;
    private int parity; // should be +1 or -1
    private double error;
    private double beta;
    private double alpha;

    /**
     * Constructs a classifier that uses the the given type of feature with the given values.
     * @param feature the feature this classifier should use.
     */
    public Classifier(Feature feature, int threshold, int parity) {
        this.feature = feature;
        this.threshold = threshold;
        this.parity = parity;
        this.error = 0;
        this.beta = 0;
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

    public double getBeta() {
        return beta;
    }

    public void setBeta(double beta) {
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
    public int canBeFace(HalIntegralImage img) throws Exception {
        if (parity != 1 && parity != -1) throw new Exception("Parity was not 1 or -1. It was: " + parity);
        if (parity * feature.calculateFeatureValue(img) < parity * threshold) {
            return 1;
        }
        return 0;
    }


}

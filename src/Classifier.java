public class Classifier {

    private Feature feature;
    private double error;

    /**
     * Constructs a classifier that uses the the given type of feature with the given values.
     * @param feature the feature this classifier should use.
     */
    public Classifier(Feature feature) {
        this.feature = feature;
        this.error = 0;
    }

    // TODO Takes some parameter to update the error value for this classifier.
    public void updateError() {

    }

    public double getError() {
        return error;
    }


}

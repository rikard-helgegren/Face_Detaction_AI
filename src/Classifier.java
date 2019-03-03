import java.io.Serializable;
import java.util.List;

public class Classifier extends FaceDetector implements Serializable {
    private static final long serialVersionUID = 0; // Increase when changing something in this class

    private Feature feature;
    private double threshold;
    private int parity; // should be +1 or -1
    private double error;
    private double beta;
    private double alpha;
    private PerformanceStats testPerformance;

    /**
     * Constructs a classifier that uses the the given type of feature with the given values.
     * @param feature the feature this classifier should use.
     */
    public Classifier(Feature feature, int threshold, int parity) throws Exception {
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
        if (beta < 0 && beta > 0 - FaceRecognition.DELTA) beta = 0;
        //if (beta > 1 && beta < 1 + FaceRecognition.DELTA) beta = 1;
        //if (beta < 0 || beta > 1) throw new Exception("Beta must be in [0, 1]. Was: " + beta);
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

        //System.out.println("My (weak classifier) threshold: "+threshold);
        //System.out.println("The feature value of the image: "+feature.calculateFeatureValue(img));
        //System.out.println("parity: "+parity);
        return parity * img.getFeatureValue(feature) < parity * threshold;
    }

    public Feature getFeature() {
        return feature;
    }

    @Override
    public PerformanceStats eval(List<LabeledIntegralImage> testData) throws Exception {
        PerformanceStats stats = super.eval(testData);
        testPerformance = stats;
        return stats;
    }

    public String toString(){
        String s = String.format("[ Feature %s, threshold %.3f, parity %d, error %.3f, beta %.3f, alpha %.3f. ",
                feature, threshold, parity, error, beta, alpha);
        if (testPerformance != null) {
            s += String.format("Test performance was %s ", testPerformance);
        }
        return s + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Classifier)) return false;
        Classifier c = (Classifier) o;

        if (feature.equals(c.feature) && threshold == c.threshold && parity == c.parity &&
                error == c.error && beta == c.beta && alpha == c.alpha) {
            return true;
        }
        return false;
    }
}

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class StrongClassifier extends FaceDetector implements Serializable {
    private static final long serialVersionUID = 0; // Increase when changing something in this class

    private List<Classifier> weakClassifiers;
    //private double threshold;
    private double thresholdMultiplier = 1;

    public StrongClassifier() {
        weakClassifiers = new ArrayList<>();
    }

    public StrongClassifier(List<Classifier> weakClassifiers) throws Exception {
        this.weakClassifiers = weakClassifiers;
        //calcThreshold();
    }

    public StrongClassifier(StrongClassifier strongClassifier, int size) throws Exception {
        this(strongClassifier.weakClassifiers.subList(0, size));
    }

    public double getThreshold() {
        double threshold = 0;
        for(Classifier c:weakClassifiers){
            threshold+=c.getAlpha();
            //FaceRecognition.writer.printf("Alpha: %.3f\n", c.getAlpha());
        }
        return threshold;
    }

    public void addClassifier(Classifier c) {
        weakClassifiers.add(c);
        //calcThreshold();
    }

    public void setThresholdMultiplier(double thresholdMultiplier) throws Exception {
        if (thresholdMultiplier < 0 || thresholdMultiplier > 1) throw new Exception("Threshold multiplier has to be in [0,1]. Was: " + thresholdMultiplier);
        this.thresholdMultiplier = thresholdMultiplier;
    }

    public double getThresholdMultiplier() {
        return thresholdMultiplier;
    }

    public boolean canBeFace(HalIntegralImage img) throws Exception {
        if (weakClassifiers.size() < 1) throw new Exception("This strong classifier has no weak classifiers.");
        //How it looks like you should do according to the paper:

        double value = 0;
        for(Classifier c:weakClassifiers){
            int isFace = (c.canBeFace(img)) ? 1 : 0;
            value+=c.getAlpha()*isFace;
        }

        //FaceRecognition.writer.printf("Value: %.3f, Mult: %.3f, Threshold: %.3f\n", value, thresholdMultiplier, getThreshold());
        return value>=getThreshold()*thresholdMultiplier;
    }

    public int getSize() {
        return weakClassifiers.size();
    }

    public Classifier getLastTrained() {
        return weakClassifiers.get(weakClassifiers.size()-1);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof StrongClassifier)) return false;
        StrongClassifier c = (StrongClassifier) o;

        if(weakClassifiers.size() != c.weakClassifiers.size()) return false;

        for (int i = 0; i < weakClassifiers.size(); i++) {
            Classifier weakClassifier = weakClassifiers.get(i);

            if(!weakClassifier.equals(c.weakClassifiers.get(i))){
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        String s = String.format("=== Strong Classifier. Size: %d. Threshold multiplier: %.2f.\n", weakClassifiers.size(), getThresholdMultiplier());
        for (Classifier c : weakClassifiers) {
            s += "====== " + c + "\n";
        }
        return s;
    }
}

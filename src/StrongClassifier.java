import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class StrongClassifier extends FaceDetector implements Serializable {
    private static final long serialVersionUID = 0; // Increase when changing something in this class

    private List<WeakClassifier> weakClassifiers = new ArrayList<>();
    //private double threshold;
    private double thresholdMultiplier = 0.5;

    public StrongClassifier() {
    }

    public StrongClassifier(int size, List<LabeledIntegralImage> trainingData) throws Exception {
        for (int i = 0; i < size; i++) {
            weakClassifiers.add(new WeakClassifier(trainingData));
        }
    }

    public StrongClassifier(String fileName) throws IOException, ClassNotFoundException {
        this((StrongClassifier) Data.load(fileName));
    }

    /**
     * Creates a new strong classifier but tests both weak and strong classifiers to print status messages.
     * @param size
     * @param trainingData
     * @param testData
     * @throws Exception
     */
    public StrongClassifier(int size, List<LabeledIntegralImage> trainingData, List<LabeledIntegralImage> testData) throws Exception {
        try {
            setThresholdMultiplier(0.5); // Threshold is default 0.5 in AdaBoost
        } catch (Exception e) {
            System.err.println("Precondition violation in strong classifier. Should never be able to happen.");
            e.printStackTrace();
        }

        for (int i = 0; i < size; i++) {
            System.out.printf("Training weak classifier %d/%d.\n", i + 1, size);
            WeakClassifier c = new WeakClassifier(trainingData);
            c.trainPerformance = c.eval(trainingData);
            c.testPerformance = c.eval(testData);
            addClassifier(c);

            test(testData);
        }
    }

    private StrongClassifier(StrongClassifier c){
        this.weakClassifiers = c.weakClassifiers;
        this.thresholdMultiplier = c.thresholdMultiplier;
    }

    public void test(List<LabeledIntegralImage> testData) throws Exception {
        System.out.println("Testing Strong WeakClassifier");
        System.out.println(toString());

        //strongClassifier = new StrongClassifier(strongClassifier, 1);
        PerformanceStats stats = null;
        try {
            stats = eval(testData);
        } catch (Exception e) {
            System.out.println("The evaluation of the strong classifier failed.");
            e.printStackTrace();
        }

        System.out.printf("Performance was %s\n", stats);
    }

    public double getThreshold() {
        double threshold = 0;
        for(WeakClassifier c:weakClassifiers){
            threshold+=c.getAlpha();
            //TrainClassifiers.writer.printf("Alpha: %.3f\n", c.getAlpha());
        }
        return threshold;
    }

    public void addClassifier(WeakClassifier c) {
        weakClassifiers.add(c);
        //calcThreshold();
    }

    public void setThresholdMultiplier(double thresholdMultiplier) throws Exception {
        if (thresholdMultiplier < 0 || thresholdMultiplier > 1) {
            throw new Exception("Threshold multiplier has to be in [0,1]. Was: " + thresholdMultiplier);
        }
        this.thresholdMultiplier = thresholdMultiplier;
    }

    public double getThresholdMultiplier() {
        return thresholdMultiplier;
    }

    public boolean canBeFace(HalIntegralImage img) throws Exception {
        if (weakClassifiers.size() < 1) throw new Exception("This strong classifier has no weak classifiers.");
        //How it looks like you should do according to the paper:

        double value = 0;
        for(WeakClassifier c:weakClassifiers){
            int isFace = (c.canBeFace(img)) ? 1 : 0;
            value+=c.getAlpha()*isFace;
        }

        //TrainClassifiers.writer.printf("Value: %.3f, Mult: %.3f, Threshold: %.3f\n", value, thresholdMultiplier, getThreshold());
        return value>=getThreshold()*thresholdMultiplier;
    }

    //TODO Using overloading to avoid extra calculations during training
    public boolean canBeFace(HalIntegralImage img, int receptiveFieldWidth,int receptiveFieldHeight) throws Exception {
        if (weakClassifiers.size() < 1) throw new Exception("This strong classifier has no weak classifiers.");
        //How it looks like you should do according to the paper:

        double value = 0;
        for(WeakClassifier c:weakClassifiers){
            int isFace = (c.canBeFace(img, receptiveFieldWidth, receptiveFieldHeight)) ? 1 : 0;
            value+=c.getAlpha()*isFace;
        }

        //TrainClassifiers.writer.printf("Value: %.3f, Mult: %.3f, Threshold: %.3f\n", value, thresholdMultiplier, getThreshold());
        return value>=getThreshold()*thresholdMultiplier;
    }

    public int getSize() {
        return weakClassifiers.size();
    }

    public WeakClassifier getLastTrained() {
        return weakClassifiers.get(weakClassifiers.size()-1);
    }

    public void save(String fileName) {
        Data.save(this, fileName);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof StrongClassifier)) return false;
        StrongClassifier c = (StrongClassifier) o;

        if(weakClassifiers.size() != c.weakClassifiers.size()) return false;

        for (int i = 0; i < weakClassifiers.size(); i++) {
            WeakClassifier weakClassifier = weakClassifiers.get(i);

            if(!weakClassifier.equals(c.weakClassifiers.get(i))){
                return false;
            }
        }
        return true;
    }

    public String toStringSummary() {
        String s = String.format("=== Strong WeakClassifier. Size: %d. Threshold multiplier: %.2f.\n", weakClassifiers.size(), getThresholdMultiplier());
        return s;
    }

    @Override
    public String toString() {
        String s = String.format("=== Strong WeakClassifier. Size: %d. Threshold multiplier: %.2f.\n", weakClassifiers.size(), getThresholdMultiplier());
        for (WeakClassifier c : weakClassifiers) {
            s += "====== " + c + "\n";
        }
        return s;
    }
}

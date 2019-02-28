import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class AdaTwo extends Thread {

    private List<Feature> features;
    private ConcurrentLinkedQueue<Classifier> classifiers;
    private List<LabeledIntegralImage> allSamples;

    public AdaTwo(List<Feature> features, ConcurrentLinkedQueue<Classifier> classifiers, List<LabeledIntegralImage> allSamples) {
        this.features = features;
        this.classifiers = classifiers;
        this.allSamples = allSamples;
    }

    @Override
    public void run() {
        try {
            for (int i = 0; i < features.size(); i++) {
                Feature j = features.get(i);
                FaceRecognition.ThresholdParity p = FaceRecognition.calcBestThresholdAndParity(allSamples, j);
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
                //if (i % 500 == 0) System.out.printf("Feature %d/%d\n", i, features.size());
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}

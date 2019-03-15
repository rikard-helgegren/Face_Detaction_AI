import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class AdaTwo extends Thread {

    private List<Feature> features;
    private ConcurrentLinkedQueue<WeakClassifier> classifiers;
    private List<LabeledIntegralImage> allSamples;

    public AdaTwo(List<Feature> features, ConcurrentLinkedQueue<WeakClassifier> classifiers, List<LabeledIntegralImage> allSamples) {
        this.features = features;
        this.classifiers = classifiers;
        this.allSamples = allSamples;
    }

    @Override
    public void run() {
        try {
            for (int i = 0; i < features.size(); i++) {
                Feature j = features.get(i);
                //if(j.getW()==18) System.out.println("Tried feature: "+j);

                ThresholdParity p = WeakClassifier.calcBestThresholdAndParity(allSamples, j);


                int threshold = p.threshold;
                int parity = p.parity;
                //System.out.println("T & P: "+threshold+", "+parity);
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
                //if (i % 500 == 0) System.out.printf("Feature %d/%d\n", i, features.size());



            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}

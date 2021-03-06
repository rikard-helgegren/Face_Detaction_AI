package hal2019.training.classifiers;

import hal2019.LabeledIntegralImage;
import hal2019.training.ThresholdParity;
import hal2019.training.Feature;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * This class handles step two of the adaboost algorithm: Make a classifier for
 * each feature. But it does so concurrently with other threads of this class.
 * Each with a subset of all the features.
 */
public class AdaTwo extends Thread {

    private List<Feature> features;
    private ConcurrentLinkedQueue<WeakClassifier> classifiers;
    private List<LabeledIntegralImage> allSamples;

    /**
     *
     * @param features The features this thread should handle.
     * @param classifiers The resulting classifiers from the features will
     *                    be placed here.
     * @param allSamples All the integral images of the data.
     */
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
                //if (i % 500 == 0) System.out.printf("hal2019.training.Feature %d/%d\n", i, features.size());



            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}

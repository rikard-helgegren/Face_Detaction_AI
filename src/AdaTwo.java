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
                //if(j.getW()==18) System.out.println("Tried feature: "+j);

                FaceRecognition.ThresholdParity p = FaceRecognition.calcBestThresholdAndParity(allSamples, j);

                boolean isSpec = false;
                if(FaceRecognition.isSpecial(j)){
                    isSpec = true;
                    System.out.println("Special feature: ");
                }
                boolean isSpec2 = false;
                if(FaceRecognition.isSpecial2(j)){
                    isSpec2 = true;
                    System.out.println("Special feature 2: ");
                }

                int threshold = p.threshold;
                int parity = p.parity;
                //System.out.println("T & P: "+threshold+", "+parity);
                // Actual step 2
                if(isSpec || isSpec2) System.out.printf("Total threshold: %d total parity: %d\n",threshold,parity);
                double error = 0;
                Classifier h = new Classifier(j, threshold, parity);
                for (LabeledIntegralImage img : allSamples) {
                    error += img.getWeight() * Math.abs(h.canBeFace(img.img) - img.isFace); // Throws exception

                    if(isSpec || isSpec2) System.out.printf("Feature value is: %d on image %s. IsFace: %d\n",img.img.getFeatureValue(j), img.img.getName(), img.isFace);
                }
                if(isSpec || isSpec2) System.out.println("Total error for feature: "+error);
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

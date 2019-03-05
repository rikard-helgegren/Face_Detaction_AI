import java.util.ArrayList;
import java.util.List;

public class CascadeClassifier {

	private final double overallFalsePositiveRate;
	private final double maxFalsePositiveRatePerLayer;
	private final double minDetectionRatePerLayer;

	private ArrayList<StrongClassifier> strongClassifiers;

	public CascadeClassifier(double overallFalsePositive, double maxFalsePositiveRatePerLayer, double minDetectionRatePerLayer) {
		this.overallFalsePositiveRate = overallFalsePositive;
		this.maxFalsePositiveRatePerLayer = maxFalsePositiveRatePerLayer;
		this.minDetectionRatePerLayer = minDetectionRatePerLayer;
	}

	public void train(
			  List<LabeledIntegralImage> positiveSamples,
			  List<LabeledIntegralImage> negativeSamples,
			  List<LabeledIntegralImage> testData) throws Exception {

		//double maxFalsePositiveRatePerLayer = 0.7;
		//double minDetectionRatePerLayer = 0.95;
		double prevFalsePositiveRate = 1;
		double curFalsePositiveRate = 1;
		double prevDetectionRate = 1;
		double curDetectionRate = 1;

		strongClassifiers = new ArrayList<>();

		//The training algorithm for building a cascaded detector
		while(curFalsePositiveRate>overallFalsePositiveRate) {
			System.out.println(toString());

			ArrayList<LabeledIntegralImage> allSamples = initAdaBoost(positiveSamples, negativeSamples);
			StrongClassifier strongClassifier = new StrongClassifier();
			strongClassifiers.add(strongClassifier);

			prevDetectionRate = curDetectionRate;
			prevFalsePositiveRate = curFalsePositiveRate;

			while(curFalsePositiveRate > maxFalsePositiveRatePerLayer*prevFalsePositiveRate){
				System.out.printf("Current false positive rate is %.2f\n", curFalsePositiveRate);
				System.out.printf("Current detection rate rate is %.2f\n", curDetectionRate);
				System.out.println(strongClassifier);
				System.out.printf("Training strong classifier, now with %d weak.\n", strongClassifier.getSize() + 1);

				if (strongClassifier.getSize() == 0) {
					strongClassifier.addClassifier(trainOneWeak(allSamples));
					strongClassifier.addClassifier(trainOneWeak(allSamples));
				} else {
					strongClassifier.addClassifier(trainOneWeak(allSamples));
				}
				strongClassifier.setThresholdMultiplier(1);

				while(true) {
					//System.out.printf("Evaluating threshold multiplier %.2f. With threshold: %.2f. ",
					//        cascadedClassifier.get(cascadedClassifier.size()-1).getThresholdMultiplier(),
					//        cascadedClassifier.get(cascadedClassifier.size()-1).getThreshold());
					PerformanceStats stats = evalCascade(strongClassifier, testData);
					System.out.printf("Performance: %s. ", stats);
					curFalsePositiveRate = stats.falsePositive;
					curDetectionRate = stats.truePositive;
					if(curDetectionRate >= minDetectionRatePerLayer * prevDetectionRate) {
						System.out.printf("GOOD! Using this one. \n");
						break;
					} else {
						System.out.printf("\n");
					}

					strongClassifier.setThresholdMultiplier(Math.max(0, strongClassifier.getThresholdMultiplier() - 0.01));
					if (strongClassifier.getThresholdMultiplier() < FaceRecognition.DELTA) System.err.println("WARNING, thresholdMultiplier was 0.");
				}
			}

			if(curFalsePositiveRate > overallFalsePositiveRate){
				negativeSamples = Data.filter(strongClassifiers, negativeSamples);
			}
		}
	}

	@Override
	public String toString() {
		String s = String.format("Cascaded classifier. %d %d");
		for(StrongClassifier c:strongClassifiers) {
			s += c.toString();
		}
		return s;
	}
}

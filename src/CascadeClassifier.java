import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CascadeClassifier implements Serializable {
	private static final long serialVersionUID = 0; // Increase when changing something in this class

	private final double targetMaxFalsePositive;
	private final double maxFalsePositiveRatePerLayer;
	private final double minDetectionRatePerLayer;

	private ArrayList<StrongClassifier> strongClassifiers;

	/**
	 *
	 * @param targetMaxFalsePositive the final classifier will have a false positive rate of less than this.
	 *                                 Should be in [0, 1].
	 * @param maxFalsePositiveRatePerLayer a decimal in [0, 1].
	 * @param minDetectionRatePerLayer a decimal in [0, 1].
	 * @param positiveSamples positive training data
	 * @param negativeSamples negative training data
	 * @param validationData data to use for validation during training
	 * @throws Exception
	 */
	public CascadeClassifier(double targetMaxFalsePositive,
									 double maxFalsePositiveRatePerLayer,
									 double minDetectionRatePerLayer,
									 List<LabeledIntegralImage> positiveSamples,
									 List<LabeledIntegralImage> negativeSamples,
									 List<LabeledIntegralImage> validationData) throws Exception {

		this.targetMaxFalsePositive = targetMaxFalsePositive;
		this.maxFalsePositiveRatePerLayer = maxFalsePositiveRatePerLayer;
		this.minDetectionRatePerLayer = minDetectionRatePerLayer;
		train(positiveSamples, negativeSamples, validationData);
	}

	public CascadeClassifier(String path) throws IOException, ClassNotFoundException {
		this((CascadeClassifier) Data.load(path));
	}

	public CascadeClassifier() {
		targetMaxFalsePositive = 0;
		maxFalsePositiveRatePerLayer = 0;
		minDetectionRatePerLayer = 0;
		strongClassifiers = new ArrayList<>();
	}

	private CascadeClassifier(CascadeClassifier c) {
		targetMaxFalsePositive = c.targetMaxFalsePositive;
		maxFalsePositiveRatePerLayer = c.maxFalsePositiveRatePerLayer;
		minDetectionRatePerLayer = c.minDetectionRatePerLayer;

		strongClassifiers = c.strongClassifiers;
	}

	public void train(
			  List<LabeledIntegralImage> positiveSamples,
			  List<LabeledIntegralImage> negativeSamples,
			  List<LabeledIntegralImage> validationData) throws Exception {

		//double maxFalsePositiveRatePerLayer = 0.7;
		//double minDetectionRatePerLayer = 0.95;
		double prevFalsePositiveRate = 1;
		double curFalsePositiveRate = 1;
		double prevDetectionRate = 1;
		double curDetectionRate = 1;

		strongClassifiers = new ArrayList<>();

		//The training algorithm for building a cascaded detector
		while(curFalsePositiveRate> targetMaxFalsePositive) {
			System.out.println(toString());

			ArrayList<LabeledIntegralImage> allSamples = Classifier.initForAdaBoost(positiveSamples, negativeSamples);
			StrongClassifier strongClassifier = new StrongClassifier();
			strongClassifiers.add(strongClassifier);

			prevDetectionRate = curDetectionRate;
			prevFalsePositiveRate = curFalsePositiveRate;

			while(curFalsePositiveRate > maxFalsePositiveRatePerLayer*prevFalsePositiveRate){
				System.out.printf("Current false positive rate is %.4f\n", curFalsePositiveRate);
				System.out.printf("Current detection rate rate is %.4f\n", curDetectionRate);
				System.out.printf("Data left: %d positive, %d negative.\n", positiveSamples.size(), negativeSamples.size());

				System.out.println(strongClassifier);
				System.out.printf("Training strong classifier, now with %d weak.\n", strongClassifier.getSize() + 1);

				if (strongClassifier.getSize() == 0) {
					strongClassifier.addClassifier(new Classifier(allSamples));
					strongClassifier.addClassifier(new Classifier(allSamples));
				} else {
					strongClassifier.addClassifier(new Classifier(allSamples));
				}
				strongClassifier.setThresholdMultiplier(1);

				while(true) {
					//System.out.printf("Evaluating threshold multiplier %.2f. With threshold: %.2f. ",
					//        cascadedClassifier.get(cascadedClassifier.size()-1).getThresholdMultiplier(),
					//        cascadedClassifier.get(cascadedClassifier.size()-1).getThreshold());
					PerformanceStats stats = eval(validationData);
					//System.out.printf("Performance: %s. ", stats);
					curFalsePositiveRate = stats.falsePositive;
					curDetectionRate = stats.truePositive;
					if(curDetectionRate >= minDetectionRatePerLayer * prevDetectionRate) {
						//System.out.printf("GOOD! Using this one. \n");
						break;
					} else {
						//System.out.printf("\n");
					}

					strongClassifier.setThresholdMultiplier(Math.max(0, strongClassifier.getThresholdMultiplier() - 0.01));
					//if (strongClassifier.getThresholdMultiplier() < FaceRecognition.DELTA) System.err.println("WARNING, thresholdMultiplier was 0.");
				}
			}

			if(curFalsePositiveRate > targetMaxFalsePositive){
				negativeSamples = Data.filter(this, negativeSamples);
			}

			if (negativeSamples.size() == 0) {
				System.err.println("Cascade training stopped since we ran out of negative data.");
				break;
			}
		}
	}

	private PerformanceStats eval(List<LabeledIntegralImage> testData) throws Exception {
		int nrCorrectIsFace = 0;
		int nrWrongIsFace = 0;
		int nrCorrectIsNotFace = 0;
		int nrWrongIsNotFace = 0;
		for(LabeledIntegralImage i:testData){
			if(i.isFace){
				if(isFace(i.img)){
					nrCorrectIsFace++;
				}else{
					nrWrongIsFace++;
				}
			}
			if(!i.isFace){
				if(!isFace(i.img)){
					nrCorrectIsNotFace++;
				}else{
					nrWrongIsNotFace++;
				}
			}
		}
		double falsePositive = ((double)nrWrongIsNotFace) / (nrCorrectIsNotFace + nrWrongIsNotFace);
		double truePositive  = ((double)nrCorrectIsFace)  / (nrCorrectIsFace    + nrWrongIsFace);
		double falseNegative = ((double)nrWrongIsFace)    / (nrCorrectIsFace    + nrWrongIsFace);

		return new PerformanceStats(truePositive, falsePositive, falseNegative);
	}

	public boolean isFace(HalIntegralImage i) throws Exception{
		//How it looks like you should do according to computerphile
		for(StrongClassifier c : strongClassifiers){
			if(!c.canBeFace(i)) return false;
		}
		return true;
	}

	public void addStrongClassifier(StrongClassifier c) {
		strongClassifiers.add(c);
	}

	public StrongClassifier getStrongClassifier(int index) {
		return strongClassifiers.get(index);
	}

	/**
	 * Tests this cascade classifier tree against some test data.
	 * @param testData
	 * @throws Exception
	 */
	public void test(List<LabeledIntegralImage> testData) throws Exception {
		PerformanceStats stats = eval(testData);
		System.out.println("Testing...");
		System.out.println(this.toString());

		System.out.println("RESULTS: " + stats);
	}

	public void save(String path) {
		Data.save(this, path);
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof CascadeClassifier)) return false;
		CascadeClassifier c = (CascadeClassifier) o;

		if(strongClassifiers.size() != c.strongClassifiers.size()) return false;

		for (int i = 0; i < strongClassifiers.size(); i++) {
			StrongClassifier strongClassifier = strongClassifiers.get(i);

			if(!strongClassifier.equals(c.strongClassifiers.get(i))){
				return false;
			}
		}
		return true;
	}

	@Override
	public String toString() {
		int totalWeak = 0;
		for (StrongClassifier s : strongClassifiers) {
			totalWeak += s.getSize();
		}
		String s = String.format("Cascade Classifier. %d strong, total %d weak.\n", strongClassifiers.size(), totalWeak);
		for(StrongClassifier c:strongClassifiers) {
			s += c.toString();
		}
		return s;
	}
}

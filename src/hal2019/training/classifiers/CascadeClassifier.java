package hal2019.training.classifiers;

import hal2019.*;
import hal2019.training.Feature;
import hal2019.training.PerformanceStats;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CascadeClassifier extends FaceDetector implements Serializable {
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

	private CascadeClassifier(ArrayList<StrongClassifier> strongClassifiers) {
		this.strongClassifiers = strongClassifiers;
		this.targetMaxFalsePositive = 0;
		this.maxFalsePositiveRatePerLayer = 0;
		this.minDetectionRatePerLayer = 0;
	}

	/**
	 * Train this cascade classifier.
	 * @param positiveSamples Set of positive samples.
	 * @param negativeSamples Set of initial negative samples.
	 * @param validationData Set of validation data.
	 * @throws Exception
	 */
	public void train(
			  List<LabeledIntegralImage> positiveSamples,
			  List<LabeledIntegralImage> negativeSamples,
			  List<LabeledIntegralImage> validationData) throws Exception {
		int posValidation = 0;
		for (LabeledIntegralImage l : validationData) {
			if (l.isFace) posValidation++;
		}

		double prevFalsePositiveRate = 1;
		double curFalsePositiveRate = 1;
		double prevDetectionRate = 1;
		double curDetectionRate = 1;

		int rejectedNegatives = 0;

		strongClassifiers = new ArrayList<>();

		//While the current false positive rate is too high
		while(curFalsePositiveRate > targetMaxFalsePositive) {
			long t0layer = System.currentTimeMillis();
			long tValidation = 0;

			// Create a new layer and init adaboost (reset image weights)
			ArrayList<LabeledIntegralImage> allSamples = WeakClassifier.initForAdaBoost(positiveSamples, negativeSamples);

			StrongClassifier strongClassifier = new StrongClassifier();
			strongClassifiers.add(strongClassifier);

			prevDetectionRate = curDetectionRate;
			prevFalsePositiveRate = curFalsePositiveRate;

			// Add more weak classifiers until false positive rate is low enough
			while(curFalsePositiveRate > maxFalsePositiveRatePerLayer*prevFalsePositiveRate){
				System.out.println();
				System.out.println(this.toStringSummary());
				System.out.println();
				System.out.printf("Validation in %.1fs. %5d positive, %5d negative. ",
						tValidation / 1000.0, posValidation, validationData.size() - posValidation);
				System.out.printf("Detection rate %.2f%%. ", curDetectionRate * 100);
				System.out.printf("False positive rate %.2f%%.\n", curFalsePositiveRate * 100);
				System.out.printf("Training  : %5d positive, %5d negative. Total %d rejected.\n",
						positiveSamples.size(), negativeSamples.size(), rejectedNegatives);
				System.out.println("--------------------------------------------------------------------------------");

				strongClassifier.addClassifier(new WeakClassifier(allSamples));


				strongClassifier.setThresholdMultiplier(1);

				// Test classifier performance
				long t0Validation = System.currentTimeMillis();
				while(true) {
					PerformanceStats stats = eval(validationData);
					curFalsePositiveRate = stats.falsePositive;
					curDetectionRate = stats.truePositive;
					// If detection rate is high enough, break
					if(curDetectionRate >= minDetectionRatePerLayer * prevDetectionRate) {
						break;
					}
					// Otherwise, decrease threshold
					strongClassifier.setThresholdMultiplier(Math.max(0, strongClassifier.getThresholdMultiplier() - 0.02));
				}
				tValidation = System.currentTimeMillis() - t0Validation;
			}
			// False positive rate is now low enough.

			// If false positive rate is not yet low enough
			if(curFalsePositiveRate > targetMaxFalsePositive){
				// Autosave. For if training has to be canceled.
				this.save(String.format("saves/autosave.cascade"));

				// Remove negative samples that were correctly classified
				int negativeBefore = negativeSamples.size();
				negativeSamples = Data.filter(this, negativeSamples);
				rejectedNegatives += negativeBefore - negativeSamples.size(); // Track nr of rejected negatives

				if (negativeSamples.size() < Data.maxTrainNonFaces) {

					// Add more negative samples that this classifier says are false positive
					List<LabeledIntegralImage> refills = Data.getRefills(this, 10000 - negativeSamples.size());
					Feature.calculateFeatureValues(refills);
					negativeSamples.addAll(refills);

					// Stop if we are out of data
					if (negativeSamples.size() < Data.maxTrainNonFaces / 5) {
						System.err.println("Cascade training stopped since we ran out of negative data.");
						break;
					}
				}
			}
		}
	}

	public boolean canBeFace(HalIntegralImage i) throws Exception{
		//Iterate through all strong hal2019.training.classifiers, if the image passes all of
		//them return true.
		for(StrongClassifier c : strongClassifiers){
			if(!c.canBeFace(i)) return false;
		}
		return true;
	}

	public void addStrongClassifier(StrongClassifier c) {
		strongClassifiers.add(c);
	}
	
	/**
	 * Tests this cascade classifier tree against some test data.
	 * @param testData
	 * @throws Exception
	 */
	public void test(List<LabeledIntegralImage> testData) throws Exception {
		PerformanceStats stats = eval(testData);
		System.out.println("Testing...");
		System.out.println(this.toString() + "\n");

		System.out.println("Test results of");
		System.out.println(this.toStringSummary());

		int testFaces = 0;
		for (LabeledIntegralImage l : testData) {
			if (l.isFace) testFaces++;
		}

		System.out.printf("Test done on %d test images. %d positive, %d negative. ", testData.size(), testFaces, testData.size()-testFaces);
		System.out.println("RESULTS: " + stats);

		for (int i = 0; i < strongClassifiers.size(); i++) {
			CascadeClassifier c = new CascadeClassifier(new ArrayList<>(strongClassifiers.subList(0, i+1)));
			PerformanceStats s = c.eval(testData);
			System.out.printf("Stage %d: %.3f reject rate.\n", i+1, 1 - s.falsePositive);
		}
	}

	public StrongClassifier getStrongClassifier(int index) {
		return strongClassifiers.get(index);
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

	public String toStringSummary() {
		int totalWeak = 0;
		for (StrongClassifier s : strongClassifiers) {
			totalWeak += s.getSize();
		}
		String s = String.format("= Cascade Classifier. %d strong, total %d weak.\n", strongClassifiers.size(), totalWeak);
		for (int i = 0; i < strongClassifiers.size(); i++) {
			StrongClassifier c = strongClassifiers.get(i);
			s += c.toStringSummary();
			if (i + 1 < strongClassifiers.size()) s += "\n";
		}
		return s;
	}

	@Override
	public String toString() {
		int totalWeak = 0;
		for (StrongClassifier s : strongClassifiers) {
			totalWeak += s.getSize();
		}
		String s = String.format("= Cascade Classifier. %d strong, total %d weak.\n", strongClassifiers.size(), totalWeak);
		for (int i = 0; i < strongClassifiers.size(); i++) {
			StrongClassifier c = strongClassifiers.get(i);
			s += c.toString();
			if (i + 1 < strongClassifiers.size()) s += "\n";
		}
		return s;
	}
}

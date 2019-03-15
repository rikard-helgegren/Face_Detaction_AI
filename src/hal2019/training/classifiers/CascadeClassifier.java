package hal2019.training.classifiers;

import hal2019.*;
import hal2019.training.Feature;
import hal2019.training.PerformanceStats;

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

	private CascadeClassifier(ArrayList<StrongClassifier> strongClassifiers) {
		this.strongClassifiers = strongClassifiers;
		this.targetMaxFalsePositive = 0;
		this.maxFalsePositiveRatePerLayer = 0;
		this.minDetectionRatePerLayer = 0;
	}

	public void train(
			  List<LabeledIntegralImage> positiveSamples,
			  List<LabeledIntegralImage> negativeSamples,
			  List<LabeledIntegralImage> validationData) throws Exception {
		int posValidation = 0;
		for (LabeledIntegralImage l : validationData) {
			if (l.isFace) posValidation++;
		}

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


			// Init adaboost (reset image weights etc) and create a new layer (strong classifier)
			ArrayList<LabeledIntegralImage> allSamples = WeakClassifier.initForAdaBoost(positiveSamples, negativeSamples);

			StrongClassifier strongClassifier = new StrongClassifier();
			strongClassifiers.add(strongClassifier);

			prevDetectionRate = curDetectionRate;
			prevFalsePositiveRate = curFalsePositiveRate;

			// Train weak hal2019.training.classifiers until restraints are fulfilled.
			while(curFalsePositiveRate > maxFalsePositiveRatePerLayer*prevFalsePositiveRate){
				System.out.println("Current Cascade:");
				System.out.println(this.toStringSummary());
				System.out.printf("Validation data %d positive, %d negative. ", posValidation, validationData.size() - posValidation);
				System.out.printf("Detection rate is %.4f. ", curDetectionRate);
				System.out.printf("False positive rate is %.4f.\n", curFalsePositiveRate);
				//System.out.println();
				System.out.printf("hal2019.Data left: %d positive, %d negative.\n", positiveSamples.size(), negativeSamples.size());

				//System.out.printf("Training strong classifier, now with %d weak.\n", strongClassifier.getSize() + 1);
				strongClassifier.addClassifier(new WeakClassifier(allSamples));

				strongClassifier.setThresholdMultiplier(1);

				while(true) {
					PerformanceStats stats = eval(validationData);
					curFalsePositiveRate = stats.falsePositive;
					curDetectionRate = stats.truePositive;
					if(curDetectionRate >= minDetectionRatePerLayer * prevDetectionRate) {
						break;
					}
					strongClassifier.setThresholdMultiplier(Math.max(0, strongClassifier.getThresholdMultiplier() - 0.01));
				}
			}

			if(curFalsePositiveRate > targetMaxFalsePositive){
				// Remove negative samples that were correctly classified
				negativeSamples = Data.filter(this, negativeSamples);
				if (negativeSamples.size() < 10000) {
					// Add more negative samples that this classifier says are false positive
					List<LabeledIntegralImage> refills = Data.getRefills(this, 10000 - negativeSamples.size());
					Feature.calculateFeatureValues(refills);
					negativeSamples.addAll(refills);
					this.save(String.format("saves/autosave.cascade"));
					// Stop if we are out of data
					if (negativeSamples.size() < 1000) {
						System.err.println("Cascade training stopped since we ran out of negative data.");
						break;
					}
				}
			}
		}
	}

	public boolean isFace(HalIntegralImage i) throws Exception{
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
		String s = String.format("Cascade hal2019.training.classifiers.WeakClassifier. %d strong, total %d weak.\n", strongClassifiers.size(), totalWeak);
		for(StrongClassifier c:strongClassifiers) {
			s += c.toStringSummary();
		}
		return s;
	}

	@Override
	public String toString() {
		int totalWeak = 0;
		for (StrongClassifier s : strongClassifiers) {
			totalWeak += s.getSize();
		}
		String s = String.format("Cascade hal2019.training.classifiers.WeakClassifier. %d strong, total %d weak.\n", strongClassifiers.size(), totalWeak);
		for(StrongClassifier c:strongClassifiers) {
			s += c.toString();
		}
		return s;
	}
}

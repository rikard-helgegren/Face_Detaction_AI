public class LabeledIntegralImage {
	public boolean isFace;
	public HalIntegralImage img;
	private double weight;

	public LabeledIntegralImage(HalIntegralImage img, boolean isFace, double weight) throws Exception {
		this.isFace = isFace;
		this.img = img;
		setWeight(weight);
	}

	public void setWeight(double weight) throws Exception {
		if (weight < 0) throw new Exception("Weight has to be >= 0. Was: " + weight);
		this.weight = weight;
	}

	public double getWeight() {
		return weight;
	}

}
public class LabeledIntegralImage {
	public int isFace; // 1 for true, 0 for false
	public HalIntegralImage img;
	private double weight;

	public LabeledIntegralImage(HalIntegralImage img, int isFace, double weight) throws Exception {
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
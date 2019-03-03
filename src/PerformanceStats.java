public class PerformanceStats {
    public double truePositive;
    public double falsePositive;
    public double falseNegative;

    public PerformanceStats(double truePositive, double falsePositive, double falseNegative){
        this.truePositive = truePositive;
        this.falsePositive = falsePositive;
        this.falseNegative = falseNegative;
    }

    @Override
    public String toString(){
        return String.format("%.2f detection rate, %.2f falsePositive", truePositive, falsePositive);
    }
}

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
        return String.format("DetRate %.2f, FalsePos %.2f", truePositive, falsePositive);
    }
}

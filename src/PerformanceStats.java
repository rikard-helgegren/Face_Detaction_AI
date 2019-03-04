import java.io.Serializable;

public class PerformanceStats implements Serializable {
    private static final long serialVersionUID = 0; // Increase when changing something in this class

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

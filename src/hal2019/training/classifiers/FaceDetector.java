package hal2019.training.classifiers;

import hal2019.HalIntegralImage;
import hal2019.LabeledIntegralImage;
import hal2019.training.PerformanceStats;

import java.util.List;

public abstract class FaceDetector {

    abstract boolean canBeFace(HalIntegralImage img) throws Exception;

    public PerformanceStats eval(List<LabeledIntegralImage> testData) throws Exception {
        int nrCorrectIsFace = 0;
        int nrWrongIsFace = 0;
        int nrCorrectIsNotFace = 0;
        int nrWrongIsNotFace = 0;
        for(LabeledIntegralImage i:testData){
            if(i.isFace){
                if(canBeFace(i.img)){
                    nrCorrectIsFace++;
                }else{
                    nrWrongIsFace++;
                }
            }
            if(!i.isFace){
                if(!canBeFace(i.img)){
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
}

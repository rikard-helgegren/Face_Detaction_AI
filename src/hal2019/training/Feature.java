package hal2019.training;

import hal2019.HalIntegralImage;
import hal2019.LabeledIntegralImage;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Feature implements Serializable {
    private static final long serialVersionUID = 0; // Increase when changing something in this class

    // Generate all features depending on data dimensions.
    public static ArrayList<Feature> allFeatures = generateAllFeatures(
            TrainClassifiers.trainingDataWidth, TrainClassifiers.trainingDataHeight);

    /**
     * Represents the 4 different types of feature.
     */
    public enum Type {HORIZONTAL, VERTICAL, THREE, FOUR};

    private Type type;
    private int x;
    private int y;
    private int w;
    private int h;

    private int id; // Matches the index in each of the hal2019.LabeledIntegralImage's arrays.
    private static int ID = 0;

    /**
     * Constructs a feature with the given values.
     * @param type the hal2019.training.Feature.Type representing the type of feature.
     * @param x coordinate of uppermost left corner
     * @param y coordinate of uppermost left corner
     * @param w width
     * @param h height
     */
    public Feature(Type type, int x, int y, int w, int h) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.id = ID++;
    }

    public static void calculateFeatureValues(List<LabeledIntegralImage> images) throws Exception {
        //System.out.printf("Pre-calculating feature values for %d samples.\n", images.size());
        for (LabeledIntegralImage img : images) {
            int[] featureValues = new int[Feature.allFeatures.size()];
            for (int i = 0; i < Feature.allFeatures.size(); i++) {
                Feature f = Feature.allFeatures.get(i);
                featureValues[i] = f.calculateFeatureValue(img.img);
            }
            img.img.setFeatureValues(featureValues);
        }
    }

    /**
     * Evaluates an image with this feature by selecting one of hal2019.training.Feature's static methods for calculating feature value.
     * @param img the image to evaluate using this features.
     * @return
     * @throws Exception if any calculation fails or the given feature was not a recognized type.
     */
    public int calculateFeatureValue(HalIntegralImage img) throws Exception {
        switch (type) {
            case HORIZONTAL: return calcHorizontalTwoRectFeature(img, x, y, w, h);
            case VERTICAL: return calcVerticalTwoRectFeature(img, x, y, w, h);
            case THREE: return calcThreeRectFeature(img, x, y, w, h);
            case FOUR: return calcFourRectFeature(img, x, y, w, h);
        }
        throw new Exception("The feature was not of a recognized type. Type was: " + type);
    }
    public int calculateFeatureValue(HalIntegralImage img, int receptiveFieldWidth,int receptiveFieldHeight) throws Exception {
        x=x/ TrainClassifiers.trainingDataWidth*receptiveFieldWidth;
        y=y/ TrainClassifiers.trainingDataHeight*receptiveFieldHeight;
        w=w/ TrainClassifiers.trainingDataWidth*receptiveFieldWidth;
        h=h/ TrainClassifiers.trainingDataHeight*receptiveFieldHeight;

        switch (type) {
            case HORIZONTAL: return calcHorizontalTwoRectFeature(img, x, y, w, h);
            case VERTICAL: return calcVerticalTwoRectFeature(img, x, y, w, h);
            case THREE: return calcThreeRectFeature(img, x, y, w, h);
            case FOUR: return calcFourRectFeature(img, x, y, w, h);
        }
        throw new Exception("The feature was not of a recognized type. Type was: " + type);
    }

    /**
     * Generate a list of all possible feature types.
     * @param imageWidth
     * @param imageHeight
     * @return
     */
    private static ArrayList<Feature> generateAllFeatures(int imageWidth, int imageHeight) {
        ID = 0; // Reset ID in case this method is called multiple times.
        ArrayList<Feature> allFeatures = new ArrayList<>(10000);
        for (int x = 0; x < imageWidth; x+=2){           ///------------------------------TODO increased x and y faster
            for (int y = 0; y < imageHeight; y+=2) {
                for (int w = 2; w < imageWidth - x; w+=1) {
                    for (int h = 2; h < imageHeight - y; h += 1){
                        if (w % 2 == 0) allFeatures.add(new Feature(Feature.Type.HORIZONTAL, x, y, w, h));
                        if (h % 2 == 0) allFeatures.add(new Feature(Feature.Type.VERTICAL, x, y, w, h));
                        if (w % 2 == 0 && h % 2 == 0) allFeatures.add(new Feature(Feature.Type.FOUR, x, y, w, h));
                        if (w % 3 == 0) allFeatures.add(new Feature(Feature.Type.THREE, x, y, w, h));
                    }
                }
            }
        }
        return allFeatures;
    }

    /**
     * Calculates the difference between the rectangle sums of two rectangles located next to each other horizontally.
     * Note that x+w may not be larger than the width of the image.
     * Note that y+h may not be larger than the height of the image.
     *
     * |‾‾‾‾‾|‾‾‾‾‾|
     * |     |     |
     * |  +  |  -  |
     * |     |     |
     * |_____|_____|
     *
     * @param img the integral image to operate on
     * @param x coordinate for the upper left corner of the feature area.
     * @param y coordinate for the upper left corner of the feature area.
     * @param w the width of the total feature area.
     * @param h the height of the total feature area.
     * @return
     * @throws Exception
     */
    public static int calcHorizontalTwoRectFeature(HalIntegralImage img, int x, int y, int w, int h) throws Exception {
        if (w%2 != 0) throw new Exception("Horizontal feature, width has to be divisible by 2. Was " + w);
        // -1 on end coordinates because getRectangleSum uses inclusive coordinates,
        // but width and height are generally thought of as exclusive.
        // That is rectangle at (0, 0) with w=3 and h=3 means a 3x3 rectangle.
        // However, if rectangleSum gets (0, 0) and (3, 3) it will use a 4x4 rectangle in top left corner.
        return img.getRectangleSum(x, y, x+w/2-1, y+h-1) - img.getRectangleSum(x+w/2, y, x+w-1, y+h-1);
    }

    /**
     * Calculates the difference between the rectangle sums of two rectangles located next to each other vertically.
     *
     * |‾‾‾‾‾‾‾‾‾‾‾|
     * |     +     |
     * |___________|
     * |           |
     * |     -     |
     * |___________|
     *
     *
     * @param img the integral image to operate on
     * @param x coordinate for the upper left corner of the feature area.
     * @param y coordinate for the upper left corner of the feature area.
     * @param w the width of the total feature area.
     * @param h the height of the total feature area.
     * @return
     * @throws Exception
     */
    public static int calcVerticalTwoRectFeature(HalIntegralImage img, int x, int y, int w, int h) throws Exception {
        if (h%2 != 0) throw new Exception("Vertical feature, height has to be divisible by 2. Was " + h);
        return img.getRectangleSum(x, y, x+w-1, y+h/2-1) - img.getRectangleSum(x, y+h/2, x+w-1, y+h-1);
    }
    
    /**
     * Calculates the difference between the rectangle sums of three rectangles located next to each other horizontally.
     *
     * |‾‾‾‾‾|‾‾‾‾‾|‾‾‾‾‾|
     * |     |     |     |
     * |  -  |  +  |  -  |
     * |     |     |     |
     * |_____|_____|_____|
     *
     *
     * @param img the integral image to operate on
     * @param x coordinate for the upper left corner of the feature area.
     * @param y coordinate for the upper left corner of the feature area.
     * @param w the width of the total feature area.
     * @param h the height of the total feature area.
     * @return
     * @throws Exception
     */
    public static int calcThreeRectFeature(HalIntegralImage img, int x, int y, int w, int h) throws Exception {
    	if (w%3 != 0) throw new Exception("Three Vertical feature, width has to be divisible by 3. Was " + w);
    	return - img.getRectangleSum(x, y, x+w/3-1, y+h-1) + img.getRectangleSum(x+w/3, y, x+2*w/3-1, y+h-1) - img.getRectangleSum(x+2*w/3, y, x+w-1, y+h-1);
    }

    
    /**
     * Calculates the difference between the rectangle sums of four rectangles located next to each other like a chess board.
     *
     * |‾‾‾‾‾|‾‾‾‾‾|
     * |  +  |  -  |
     * |_____|_____|
     * |     |     |
     * |  -  |  +  |
     * |_____|_____|
     *
     *
     * @param img the integral image to operate on
     * @param x coordinate for the upper left corner of the feature area.
     * @param y coordinate for the upper left corner of the feature area.
     * @param w the width of the total feature area.
     * @param h the height of the total feature area.
     * @return
     * @throws Exception
     */
    public static int calcFourRectFeature(HalIntegralImage img, int x, int y, int w, int h) throws Exception {
    	if (h%2 != 0) throw new Exception("Four Rect feature, height has to be divisible by 2. Was " + h);
    	if (w%2 != 0) throw new Exception("Four Rect feature, width has to be divisible by 2. Was " + w);
    	return  img.getRectangleSum(x, y+h/2, x+w/2-1, y+h-1) +
                img.getRectangleSum(x+w/2, y, x+w-1, y+h/2-1) -
                img.getRectangleSum(x, y, x+w/2-1, y+h/2-1) -
                img.getRectangleSum(x+w/2, y+h/2, x+w-1, y+h-1);
    }

    public int getH() {
        return h;
    }

    public int getW() {
        return w;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public Type getType() {
        return type;
    }

    public String toString(){
        return String.format("%10s, (x=%2d, y=%2d), (w=%2d, h=%2d)", getType(), getX(), getY(), getW(), getH());
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Feature)) return false;
        Feature f = (Feature) o;
        if (type.equals(f.type) && x == f.x && y == f.y && w == f.w && h == f.h) {
            return true;
        }
        return false;
    }

    public int getId() {
        return id;
    }
}

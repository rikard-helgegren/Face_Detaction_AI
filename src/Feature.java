import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;

public class Feature implements Serializable {

    /**
     * Represents the 4 different types of feature.
     */
    public enum Type {HORIZONTAL, VERTICAL, THREE, FOUR};

    private Type type;
    private int x;
    private int y;
    private int w;
    private int h;

    /**
     * Constructs a feature with the given values.
     * @param type the Feature.Type representing the type of feature.
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
    }

    /**
     * Evaluates an image with this feature by selecting one of Feature's static methods for calculating feature value.
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

    /**
     * Generate a list of all possible feature types.
     * @param imageWidth
     * @param imageHeight
     * @return
     */
    public static ArrayList<Feature> generateAllFeatures(int imageWidth, int imageHeight) {
        ArrayList<Feature> allFeatures = new ArrayList<>(160000);
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
     * |‾‾‾‾‾‾‾‾|
     * |    |    |
     * |        |
     * |________|
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
     * Calculates the difference between the rectangle sums of four rectangles located next to each other like a chess board..
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
        return "x: "+getX()+" y: "+getY()+" w: "+getW()+" h: "+getH()+ " type: "+getType();
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

}

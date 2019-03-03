import Catalano.Imaging.FastBitmap;

import java.awt.image.BufferedImage;

/**
 * Represents an integral image.
 *
 * In internal data, first coordinate is Y and second is X. This way each array contains one image row.
 */
public class HalIntegralImage {

    // First coordinate is Y, second is X.
    public int[][] data;
    public FastBitmap fastBitmap;
    private int[] featureValues;
    private String name;

    public HalIntegralImage(BufferedImage bi, String name) throws Exception {
        this(new FastBitmap(bi), name);
    }

    public HalIntegralImage(FastBitmap fb, String name) throws Exception {
        if (!fb.isGrayscale()) throw new Exception("Image must be grayscale.");
        this.name = name;
        data = toIntegralData(fb);
        fastBitmap = fb;
    }

    public HalIntegralImage(FastBitmap fb) throws Exception {
        this(fb, "");
    }

    public static int[][] toIntegralData(FastBitmap fb) {
        int[][] integral = new int[fb.getHeight()][fb.getWidth()];
        //System.out.printf("Dimension: %s, %s\n", fb.getWidth(), fb.getHeight());

        // TODO Seems to not always work if images are not square. Fix or find out what this depends on.
        // I have done some more testing since that might have fixed it. I am not entirely certain however. /Ecen
        /*if (fb.getWidth() != fb.getHeight()) {
            System.out.printf("Dimension: %s, %s\n", fb.getWidth(), fb.getHeight());
            return integral;
        }*/

        // Calculate sums.
        for (int y = 0; y < fb.getHeight(); y++) {
            int rowSum = 0;
            for (int x = 0; x < fb.getWidth(); x++) {
                //System.out.printf("\t%s, %s\n", x, y);
                rowSum += fb.getGray(y, x); //Shouldn't the coordinates be the other way around
                int aboveSum = 0; // Default to 0 for first row, when y is 0.
                if (y > 0) aboveSum = integral[y-1][x];
                integral[y][x] = rowSum + aboveSum;
                //System.out.println(rowSum + " + " + aboveSum);
            }
        }
        return integral;
    }

    public String getName() {
        return name;
    }

    /**
     * Calculate sum of pixels in the specified rectangle.
     * Both coordinate sets are inclusive.
     * Image coordinate system has origo (0, 0) in top left corner.
     * Ex: Rectangle sum of a 25x25 image is calculated as getRectangleSum(0, 0, 24, 24).
     *
     * @param x1 Coordinate of left-top rectangle's corner.
     * @param y1 Coordinate of left-top rectangle's corner.
     * @param x2 Coordinate of right-bottom rectangle's corner.
     * @param y2 Coordinate of right-bottom rectangle's corner.
     * @return Returns sum of pixels in the specified rectangle.
     */
    public int getRectangleSum(int x1, int y1, int x2, int y2) throws Exception {
        // check if requested rectangle is out of the image
        if (    ( x2 < 0 ) || ( y2 < 0 ) || ( x1 < 0 ) || ( y1 < 0 ) ||
                ( x1 >= this.getHeight() ) || ( y1 >= this.getWidth() ) ||
                ( x2 >= this.getHeight() ) || ( y2 >= this.getWidth() )) {
            throw new Exception("Coordinates are outside of image!");
        }
        if ( x2 < x1 || y2 < y1) throw new Exception("Coordinates are given in the wrong order.");

        int corner = (x1 == 0 || y1 == 0) ? 0 : data[y1-1][x1-1]; // If x1 or y1 is 0, there is no corner piece.
        int left = (x1 == 0) ? 0 : data[y2][x1-1]; // If x1 is 0, there is no left piece
        int top = (y1 == 0) ? 0: data[y1-1][x2]; // If y1 is 0, there is no top piece
        int main = data[y2][x2]; // There is always a main piece

        return main + corner - left - top;
    }

    public int[][] getInternalData() {
        return data;
    }

    public int getHeight(){
        return data.length;
    }

    public int getWidth() {
        return data[0].length;
    }

    public void setFeatureValues(int[] featureValues) {
        this.featureValues = featureValues;
    }

    /**
     * Gets the image feature value. This method will return a precalculated value if it exists.
     * If not, it will calculate the value from scratch.
     * To pre-calculate, run Feature.calculateFeatureValues on your dataset.
     * @param f
     * @return
     * @throws Exception
     */
    public int getFeatureValue(Feature f) throws Exception {
        if (featureValues != null) {
            return featureValues[f.getId()];
        }
        return f.calculateFeatureValue(this);
    }
}

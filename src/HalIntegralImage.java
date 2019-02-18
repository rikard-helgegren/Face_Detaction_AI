import Catalano.Imaging.FastBitmap;
import Catalano.Imaging.Tools.IntegralImage;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.util.Arrays;

/**
 * Represents an integral image.
 *
 * In internal data, first coordinate is Y and second is X. This way each array contains one image row.
 */
public class HalIntegralImage {

    // First coordinate is Y, second is X.
    public int[][] data;

    public HalIntegralImage(BufferedImage bi) {
        this(new FastBitmap(bi));
    }

    public HalIntegralImage(FastBitmap fb) {
        data = toIntegralImage(fb);
    }

    public int[][] toIntegralImage(FastBitmap fb) {
        int[][] integral = new int[fb.getWidth()][fb.getHeight()];

        // Calculate sums.
        for (int y = 0; y < integral[0].length; y++) {
            int rowSum = 0;
            for (int x = 0; x < integral.length; x++) {
                rowSum += fb.getGray(x, y);
                int aboveSum = 0; // Default to 0 for first row, when y is 0.
                if (y > 0) aboveSum = integral[x][y-1];
                integral[x][y] = rowSum + aboveSum;
                //System.out.println(rowSum + " + " + aboveSum);
            }
        }

        return integral;
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

        if ( x2 > this.getHeight() )  x2 = this.getHeight();
        if ( y2 > this.getWidth() ) y2 = this.getWidth();

        return data[x2][y2] + data[x1][y1] - data[x1][y2] - data[x2][y1];
    }

    public int[][] getInternalData() {
        return data;
    }

    public int getHeight(){
        return data[0].length;
    }

    public int getWidth() {
        return data.length;
    }
}

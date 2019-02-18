import Catalano.Imaging.FastBitmap;
import Catalano.Imaging.Tools.IntegralImage;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.util.Arrays;

public class HalIntegralImage {

    public int[][] data;

    public HalIntegralImage(BufferedImage bi) {
        this(new FastBitmap(bi));
    }

    public HalIntegralImage(FastBitmap fb) {
        data = toIntegralImage(fb);
    }

    public int[][] toIntegralImage(FastBitmap fb) {
        int[][] original = fb.toMatrixGrayAsInt();
        int[][] integral = new int[fb.getHeight()+1][fb.getWidth()];

        for (int y = 1; y < integral.length; y++) {
            int rowSum = 0;
            for (int x = 0; x < integral[0].length; x++) {
                rowSum += fb.getGray(x, y-1);
                integral[y][x] = rowSum + integral[y-1][x];
            }
        }

        return Arrays.copyOfRange(integral, 1, integral.length);
    }

    /**
     * Calculate sum of pixels in the specified rectangle.
     * Both coordinate sets are inclusive.
     * Ex: Rectangle sum of a 25x25 image is calculated as getRectangleSum(24, 24, 0, 0).
     *
     * @param x1 Coordinate of left-top rectangle's corner.
     * @param y1 Coordinate of left-top rectangle's corner.
     * @param x2 Coordinate of right-bottom rectangle's corner.
     * @param y2 Coordinate of right-bottom rectangle's corner.
     * @return Returns sum of pixels in the specified rectangle.
     */
    public int getRectangleSum(int x1, int y1, int x2, int y2) throws Exception {
        // check if requested rectangle is out of the image
        if ( ( x2 < 0 ) || ( y2 < 0 ) || ( x1 < 0 ) || ( y1 < 0 ) || ( x1 >= this.getHeight() ) || ( y1 >= this.getWidth() ) ){
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
        return data.length;
    }

    public int getWidth() {
        return data[0].length;
    }
}

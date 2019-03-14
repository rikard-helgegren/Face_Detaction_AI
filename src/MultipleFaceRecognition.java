import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.awt.Dimension;
import java.awt.Graphics;

public class MultipleFaceRecognition{
    //The image to be used
    private static final String path = "test-res/examples/many_faces.png";
    //The minimum size of the sliding window
    private static final int minFaceSize = 28;
    //The maximum size of the sliding window
    private static final int maxFaceSize = 28;
    //How much the slidning window increases every iteration.
    private static final int slidingWindowIncrease = 3;
    /*
    Should we allow the algorithm to find two or more overlapping faces?
    For example find a face in both squares below:

     |‾‾‾‾‾‾‾‾‾‾|
     | |‾‾‾‾‾‾‾‾|‾|
     | |        | |
     | |        | |
     |_|________| |
       |__________|

     */
    private static final boolean allowOverlapping = true;
    //Should the full image be scaled down or the features scaled up?
    private static final boolean scaleFeatures = true;
    //If sliding windows should move at a speed proportional to it's size.
    private static final boolean slidingWindowSpeedAsScale = true;

    //How fast to move the sliding window if it should move at a
    //speed proportional to it's size.
    private static final double slidingWindowMoveSpeedScale = 2d/19;
    //How fast to move the sliding window if it should move at a
    //constant rate.
    private static final int slidingWindowMoveSpeed = 2;

    public static void main(String[] args) throws Exception {
        //Take the image from the image path, convert it to grayscale and store it here
        BufferedImage img = loadImageAsGrayscale(path);
        //Load the cascaded classifier
        CascadeClassifier cascade = new CascadeClassifier("save.cascade");

        //Find all faces in the image using the cascade
        ArrayList<Rectangle> faces = findFaces(cascade, img);

        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    startGUI(img,faces);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Finds faces in an image using the method specified by the variable scaleFeatures.
     *
     * @param cascade The cascaded classifier to use
     * @param img The image to search for faces using the cascade
     * @return An arraylist of squares which surround the found faces.
     * @throws Exception
     */
    private static ArrayList<Rectangle> findFaces(CascadeClassifier cascade, BufferedImage img) throws Exception {
        ArrayList<Rectangle> faces = new ArrayList<>();

        if(scaleFeatures){
            faces = findFacesScaleFeatures(cascade,img);
        }else{
            faces = findFacesScaleImage(cascade, img);
        }

        return faces;
    }

    /**
     * Finds faces in an image using the method of scaling up the features to the appropriate size.
     *
     * @param cascade The cascaded classifier to use
     * @param img The image to search for faces using the cascade
     * @return An arraylist of squares which surround the found faces.
     * @throws Exception
     */
    private static ArrayList<Rectangle> findFacesScaleImage(CascadeClassifier cascade, BufferedImage img) throws Exception {
        ArrayList<Rectangle> faces = new ArrayList<>();

        double scaleImageToMaxFace = (double)FaceRecognition.trainingDataWidth/maxFaceSize;
        double scaleImageToMinFace = (double)FaceRecognition.trainingDataWidth/minFaceSize;
        double scalePerLayer = (double) FaceRecognition.trainingDataWidth/(FaceRecognition.trainingDataWidth + slidingWindowIncrease);
        BufferedImage scaled = scaleImage(img, scaleImageToMinFace);
        System.out.println(scaleImageToMinFace);

        while(scaled.getWidth()>=scaleImageToMaxFace*img.getWidth()){
            ArrayList<Rectangle> newFaces = findFaces(cascade, scaled, FaceRecognition.trainingDataWidth);
            for(Rectangle r:newFaces){
                r.scale((double)img.getWidth()/scaled.getWidth());
            }
            faces.addAll(newFaces);

            scaled = scaleImage(scaled, scalePerLayer);
        }

        return faces;
    }

    /**
     * Finds faces in an image using the method of scaling down the image to
     * make the features the appropriate size i relation to the image.
     *
     * @param cascade The cascaded classifier to use
     * @param img The image to search for faces using the cascade
     * @return An arraylist of squares which surround the found faces.
     * @throws Exception
     */
    private static ArrayList<Rectangle> findFacesScaleFeatures(CascadeClassifier cascade, BufferedImage img) throws Exception {
        ArrayList<Rectangle> faces = new ArrayList<>();

        for (int s = minFaceSize; s <= maxFaceSize; s+= slidingWindowIncrease) {
            faces.addAll(findFaces(cascade, img, s));
        }

        return faces;
    }

    /**
     * Finds faces in an image using a specified size of the sliding window.
     *
     * @param cascade The cascaded classifier to use
     * @param img The image to search for faces using the cascade
     * @param slidingWindowSize The size of the sliding window
     * @return An arraylist of squares which surround the found faces
     * @throws Exception
     */
    private static ArrayList<Rectangle> findFaces(CascadeClassifier cascade, BufferedImage img, int slidingWindowSize) throws Exception {
        ArrayList<Rectangle> faces = new ArrayList<>();

        //Set the speed of the sliding window.
        int slidingWindowSpeed;

        if(slidingWindowSpeedAsScale){
            slidingWindowSpeed = (int)(slidingWindowSize*slidingWindowMoveSpeedScale);
        }else{
            slidingWindowSize = slidingWindowMoveSpeed;
        }

        for (int x = 0; x < img.getWidth()-slidingWindowSize; x+=slidingWindowSpeed){
            for (int y = 0; y < img.getHeight()-slidingWindowSize; y+=slidingWindowSpeed) {

                //Use the cascaded classifier to check every window
                if(cascade.isFace(integralImageFromSubwindow(x,y,slidingWindowSize,img))){
                    Rectangle newFace = new Rectangle(x, y, slidingWindowSize, slidingWindowSize);

                    if(allowOverlapping) {
                        System.out.println("Face found: " + newFace);
                        faces.add(newFace);
                    }else{
                        boolean overlaps = false;
                        for(Rectangle sq:faces){
                            if(sq.overlaps(newFace)){
                                overlaps = true;
                            }
                        }
                        if(!overlaps){
                            System.out.println("Face found: "+newFace);
                            faces.add(newFace);
                        }
                    }

                    y+=slidingWindowSize;
                }
            }
        }

        return faces;
    }

    public static ArrayList<HalIntegralImage> findFaceIntegralImagesScaleImage(CascadeClassifier cascade, BufferedImage img, int resultingWidth) throws Exception {
        ArrayList<HalIntegralImage> faces = new ArrayList<>();
        int maxSlidingWindowSize = 100;
        int minSlidingWindowSize = 19;
        int slidingWindowIncrease = 1;

        double scaleImageToMaxFace = (double)resultingWidth/maxSlidingWindowSize;
        double scaleImageToMinFace = (double)resultingWidth/minSlidingWindowSize;
        double scalePerLayer = (double)resultingWidth/(resultingWidth + slidingWindowIncrease);
        BufferedImage scaled = scaleImage(img, scaleImageToMinFace);

        while(scaled.getWidth()>=scaleImageToMaxFace*img.getWidth()){
            ArrayList<HalIntegralImage> newFaces = findFaceIntegralImages(cascade, scaled, FaceRecognition.trainingDataWidth);
            faces.addAll(newFaces);

            scaled = scaleImage(scaled, scalePerLayer);
        }

        return faces;
    }

    private static ArrayList<HalIntegralImage> findFaceIntegralImages(CascadeClassifier cascade, BufferedImage img, int slidingWindowSize) throws Exception {
        ArrayList<HalIntegralImage> faces = new ArrayList<>();
        int xIncrease = 1;
        int yIncrease = 1;

        for (int x = 0; x < img.getWidth()-slidingWindowSize; x+=xIncrease){
            for (int y = 0; y < img.getHeight()-slidingWindowSize; y+=yIncrease) {

                HalIntegralImage integralImage = integralImageFromSubwindow(x,y,slidingWindowSize,img);
                if(cascade.isFace(integralImage)){
                    faces.add(integralImage);

                    y+=slidingWindowSize;
                }
            }
        }

        return faces;
    }

    /**
     * Scales an image with a factor.
     *
     * @param img The image which to scale
     * @param scale The scale factor
     * @return A scaled image
     */
    private static BufferedImage scaleImage(BufferedImage img, double scale){
        AffineTransform at = new AffineTransform();

        BufferedImage after = new BufferedImage((int)(img.getWidth() * scale), (int)(img.getHeight()*scale), BufferedImage.TYPE_BYTE_GRAY);

        at.scale(scale, scale);
        AffineTransformOp scaleOp =
                new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
        return scaleOp.filter(img, after);
    }

    /**
     * Get an integral image from a subwindow of an image.
     *
     * @param x The x coordinate for the subwindow
     * @param y The y coordinate for the subwindow
     * @param size The size of the subwindow (width and height)
     * @param img The image from where to take the subwindow
     * @return The integral image from the subwindow
     */
    private static HalIntegralImage integralImageFromSubwindow(int x, int y, int size, BufferedImage img) throws Exception {
        //Create a subimage from the original
        BufferedImage subimage = new BufferedImage(size, size, BufferedImage.TYPE_BYTE_GRAY);
        int yl;
        for(yl = y;yl<size+y;yl++){
            for(int xl = x; xl<size+x; xl++){
                subimage.setRGB(xl-x,yl-y, img.getRGB(xl, yl));
            }
        }
        //Convert the subimage to an integralImage and return it.
        return new HalIntegralImage(subimage);
    }

    /**
     * Start a GUI showing the image with the found faces.
     *
     * @param img The image to show
     * @param faces The faces on the image
     * @return
     */
    private static void startGUI(BufferedImage img, ArrayList<Rectangle> faces) {

        //Create the window.
        JFrame frame = new JFrame("Face recognition using the method proposed by Viola-Jones");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        //Set the size of the window to the size of the image.
        frame.setPreferredSize(new Dimension(img.getWidth(),img.getHeight()));
        frame.setMinimumSize(new Dimension(img.getWidth(),img.getHeight()));

        //Place the image at the center of the screen
        frame.setLocationRelativeTo(null);

        //Draw the buffered image with the recognized faces.
        frame.getContentPane().add(new FacesPainter(img, faces));

        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }

    /**
     * Load an image and convert it to grayscale.
     *
     * @param path The path to the image
     * @return An image in grayscale
     */
    public static BufferedImage loadImageAsGrayscale(String path) throws IOException {
        //Load the image
        BufferedImage loadedImage = ImageIO.read(new File(path));

        //Make a new empty BufferedImage and set its type to grayscale
        BufferedImage grayImage = new BufferedImage(loadedImage.getWidth(), loadedImage.getHeight(),
                BufferedImage.TYPE_BYTE_GRAY);

        //Draw loadedImage in grayscale on grayImage
        Graphics g = grayImage.getGraphics();
        g.drawImage(loadedImage, 0, 0, null);
        g.dispose();


        return grayImage;
    }
}

package hal2019.graphics;

import hal2019.Data;
import hal2019.HalIntegralImage;
import hal2019.training.TrainClassifiers;
import hal2019.training.classifiers.CascadeClassifier;

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

public class Detector {
    //The image to be used
    private static final String path = "test-res/examples/many_faces.png";
    //The minimum size of the sliding window
    private static final int minFaceSize = 28;
    //The maximum size of the sliding window
    private static final int maxFaceSize = 40;
    //How much the slidning window increases every iteration.
    private static final int slidingWindowIncrease = 10;
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
    private static final boolean allowOverlapping = false;
    //Should the full image be scaled down or the features scaled up? (Note
    //that if the image is scaled down the sliding window will always move
    //with slidingWindowMoveSpeed, thus will always move with a speed
    //proportional to it's size)
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
        BufferedImage img = Data.loadImageAsGrayscale(path);
        //Load the cascaded classifier
        CascadeClassifier cascade = new CascadeClassifier("saves/save.cascade");

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
     * Finds faces in an image using the method of scaling up the features.
     *
     * @param cascade The cascaded classifier to use
     * @param img The image to search for faces using the cascade
     * @return An arraylist of squares which surround the found faces.
     * @throws Exception
     */
    private static ArrayList<Rectangle> findFacesScaleImage(CascadeClassifier cascade, BufferedImage img) throws Exception {
        ArrayList<Rectangle> faces = new ArrayList<>();

        /*
         * scaleImageToMaxFace is the factor used to scale the image down
         * enough to make the sliding windows size (trainingDataWidth)
         * proportionally as big as maxFaceSize is to the original image.
         *
         * scaleImageToMinFace is the factor used to scale the image down
         * enough to make the sliding windows size (trainingDataWidth)
         * proportionally as big as minFaceSize is to the original image.
         *
         *
         * Instead of increasing the sliding window size with
         * slidingWindowIncrease scale down the full image with:
         *
         *                trainingDataWidth
         *     —————————————————————————————————————————
         *     trainingDataWidth + slidingWindowIncrease
         *
         *
         */
        double scaleImageToMaxFace = (double) TrainClassifiers.trainingDataWidth/maxFaceSize;
        double scaleImageToMinFace = (double) TrainClassifiers.trainingDataWidth/minFaceSize;
        double scalePerLayer = (double) TrainClassifiers.trainingDataWidth/ (TrainClassifiers.trainingDataWidth + slidingWindowIncrease);
        //Initially scale the image with scaleImageToMinFace.
        BufferedImage scaled = scaleImage(img, scaleImageToMinFace);

        while(scaled.getWidth()>=scaleImageToMaxFace*img.getWidth()){
            //Find all the faces in the scaled image with the size
            //trainingDataWidth.
            ArrayList<Rectangle> newFaces = findFaces(cascade, scaled, TrainClassifiers.trainingDataWidth);
            //Scale all the new faces to where they were found but on the
            // original image size.
            for(Rectangle r:newFaces){
                r.scale((double)img.getWidth()/scaled.getWidth());
            }

            if(allowOverlapping) {
                //Add all the new faces
                faces.addAll(newFaces);
            }
            else {
                //Add all the new faces without overlapping
                addWithoutOverlap(faces, newFaces);
            }

            //Scale the image with scalePerLayer every iteration.
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
            if(allowOverlapping) {
                //Add all the new faces
                faces.addAll(findFaces(cascade, img, s));
            }
            else {
                //Add all the new faces without overlapping
                addWithoutOverlap(faces, findFaces(cascade, img, s));
            }
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

        long t0 = System.currentTimeMillis();
        for (int x = 0; x < img.getWidth()-slidingWindowSize; x+=slidingWindowSpeed){
            for (int y = 0; y < img.getHeight()-slidingWindowSize; y+=slidingWindowSpeed) {

                //Use the cascaded classifier to check every window
                if(cascade.canBeFace(integralImageFromSubwindow(x,y,slidingWindowSize,img))){
                    Rectangle newFace = new Rectangle(x, y, slidingWindowSize, slidingWindowSize);

                    faces.add(newFace);

                    if(!allowOverlapping) {
                        //If a face is found, make a bigger jump to avoid
                        //looking at an overlapping squre.
                        y+=slidingWindowSize;
                    }
                }
            }
        }
        System.out.printf("Found %d faces in %.1f seconds.\n", faces.size(), (System.currentTimeMillis() - t0) / 1000.0);

        return faces;
    }

    /**
     * Calculates integral images of faces found in an image.
     * Uses the method of scaling up the features to find the faces.
     *
     * Mainly used to find false positives in images without any faces.
     *
     * @param cascade The cascaded classifier to use
     * @param img The image to search for faces using the cascade
     * @param resultingSize The size of the resulting images.
     * @return An arraylist of HalIntegralImages of found faces.
     * @throws Exception
     */
    public static ArrayList<HalIntegralImage> findFaceIntegralImagesScaleImage(CascadeClassifier cascade, BufferedImage img, int resultingSize) throws Exception {
        ArrayList<HalIntegralImage> faces = new ArrayList<>();

        //The maximum and the minimum size of the sliding window.
        int maxSlidingWindowSize = 100;
        int minSlidingWindowSize = 19;
        //How much the sliding window should increase every iteration.
        int slidingWindowIncrease = 1;

        /*
         * scaleImageToMaxFace is the factor used to scale the image down
         * enough to make the sliding windows size (resultingSize)
         * proportionally as big as maxFaceSize is to the original image.
         *
         * scaleImageToMinFace is the factor used to scale the image down
         * enough to make the sliding windows size (resultingSize)
         * proportionally as big as minFaceSize is to the original image.
         *
         *
         * Instead of increasing the sliding window size with
         * slidingWindowIncrease scale down the full image with:
         *
         *                  resultingSize
         *       —————————————————————————————————————
         *       resultingSize + slidingWindowIncrease
         *
         */
        double scaleImageToMaxFace = (double)resultingSize/maxSlidingWindowSize;
        double scaleImageToMinFace = (double)resultingSize/minSlidingWindowSize;
        double scalePerLayer = (double)resultingSize/(resultingSize + slidingWindowIncrease);
        //Initially scale the image with scaleImageToMinFace.
        BufferedImage scaled = scaleImage(img, scaleImageToMinFace);

        while(scaled.getWidth()>=scaleImageToMaxFace*img.getWidth()){
            ArrayList<HalIntegralImage> newFaces = findFaceIntegralImages(cascade, scaled, TrainClassifiers.trainingDataWidth);
            faces.addAll(newFaces);

            scaled = scaleImage(scaled, scalePerLayer);
        }

        return faces;
    }
    /**
     * Calculates integral images of faces, with a specified size, found in an
     * image.
     *
     * Mainly used to find false positives in images without any faces.
     *
     * @param cascade The cascaded classifier to use
     * @param img The image to search for faces using the cascade
     * @param slidingWindowSize The size of the sliding window
     * @return An arraylist of HalIntegralImages of found faces
     * @throws Exception
     */
    private static ArrayList<HalIntegralImage> findFaceIntegralImages(CascadeClassifier cascade, BufferedImage img, int slidingWindowSize) throws Exception {
        ArrayList<HalIntegralImage> faces = new ArrayList<>();

        /*
        * The sliding window will slow down when a face is found to search
        * more closely at that area since it will be more likely that another
        * face is found there.
        *
        * The IncreaseMax variables sets the max length the sliding window can
        * move every iteration.
        *
        * The Increase variables contains the length the sliding window
        * should move every iteration.
        */
        int xIncreaseMax = 10;
        int yIncreaseMax = 10;

        int xIncrease = 1;
        int yIncrease = 1;

        for (int x = 0; x < img.getWidth()-slidingWindowSize; x+=xIncrease){
            for (int y = 0; y < img.getHeight()-slidingWindowSize; y+=yIncrease) {

                HalIntegralImage integralImage = integralImageFromSubwindow(x,y,slidingWindowSize,img);

                //If a face was found
                if(cascade.canBeFace(integralImage)){
                    faces.add(integralImage);

                    //Reset the Increase variables.
                    xIncrease = 1;
                    yIncrease = 1;
                }else{
                    //Increase the Increase variables until max.
                    xIncrease = xIncrease==xIncreaseMax?xIncrease:xIncrease+1;
                    yIncrease = yIncrease==yIncreaseMax?yIncrease:yIncrease+1;
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

    private static void addWithoutOverlap(ArrayList<Rectangle> a, ArrayList<Rectangle> b){
        for(Rectangle rectangle:b){
            if(!rectangle.overlapsAny(a)){
                a.add(rectangle);
            }
        }
    }
}

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
    private static final String path = "test-res/examples/many_faces.png";
    private static final int minFaceSize = 25;
    private static final int maxFaceSize = 35;

    public static void main(String[] args) throws Exception {
        BufferedImage img = loadImageAsGrayScale(path);
        CascadeClassifier cascade = new CascadeClassifier("save.cascade");

        ArrayList<Square> faces = findFaces(cascade, img);

        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    createAndShowGUI(img,faces);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }


    private static ArrayList<Square> findFaces(CascadeClassifier cascade, BufferedImage img) throws Exception {
        ArrayList<Square> faces = new ArrayList<>();


        int w = img.getWidth();
        int h = img.getHeight();
        BufferedImage after = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
        AffineTransform at = new AffineTransform();
        at.scale(0.5, 0.5);
        AffineTransformOp scaleOp =
                new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
        after = scaleOp.filter(img, after);

        //img = after;


        int s = 19;
        for (int x = 0; x < after.getWidth()-s; x+=s/8){
            for (int y = 0; y < after.getHeight()-s; y+=s/8) {
                if(cascade.isFace(integralImageFromSubWindow(x,y,s,after))){
                    Square newFace = new Square(x*2, y*2, s*2, s*2);
                    System.out.println("Face found: "+newFace);
                    faces.add(newFace);

                    System.out.println("Face found: "+newFace);
                    faces.add(newFace);

                    /*
                    boolean overlaps = false;
                    for(Square sq:faces){
                        if(sq.overlaps(newFace)){
                            overlaps = true;
                        }
                    }
                    if(!overlaps){
                        System.out.println("Face found: "+newFace);
                        faces.add(newFace);
                    }*/

                    y+=s;
                }
            }
        }




        /*
        for (int s = minFaceSize; s <= maxFaceSize; s+=2) {
            for (int x = 0; x < img.getWidth()-s; x+=s/8){
                for (int y = 0; y < img.getHeight()-s; y+=s/8) {
                    if(cascade.isFace(integralImageFromSubWindow(x,y,s,img))){
                        Square newFace = new Square(x, y, s, s);
                        System.out.println("Face found: "+newFace);
                        faces.add(newFace);

                        boolean overlaps = false;

                        /*boolean overlaps = false;
                        for(Square sq:faces){
                            if(sq.overlaps(newFace)){
                                overlaps = true;
                            }
                        }
                        if(!overlaps){
                            System.out.println("Face found: "+newFace);
                            faces.add(newFace);
                        }

                        y+=s;
                    }
                }
            }
        }*/

        return faces;
    }

    private static HalIntegralImage integralImageFromSubWindow(int x, int y, int size, BufferedImage img) throws Exception {
        BufferedImage newBuff = new BufferedImage(size, size, BufferedImage.TYPE_BYTE_GRAY);
        int yl;
        for(yl = y;yl<size+y;yl++){
            for(int xl = x; xl<size+x; xl++){
                newBuff.setRGB(xl-x,yl-y, img.getRGB(xl, yl));
            }
        }

        return new HalIntegralImage(newBuff);
    }

    private static void createAndShowGUI(BufferedImage img, ArrayList<Square> faces) {

        //Create and set up the window.
        JFrame frame = new JFrame("Face recognition using the method proposed by Viola-Jones");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setPreferredSize(new Dimension(img.getWidth(),img.getHeight()));
        frame.setMinimumSize(new Dimension(img.getWidth(),img.getHeight()));
        frame.setLocationRelativeTo(null);

        //Draw the buffered image with the recognized faces.
        frame.getContentPane().add(new FacesPainter(img, faces));

        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }

    //TODO Not sure how well this works...
    private static BufferedImage loadImageAsGrayScale(String path) throws IOException {
        BufferedImage bi = ImageIO.read(new File(path));


        BufferedImage image = new BufferedImage(bi.getWidth(), bi.getHeight(),
                BufferedImage.TYPE_BYTE_GRAY);

        Graphics g = image.getGraphics();
        g.drawImage(bi, 0, 0, null);
        g.dispose();


        return image;
    }
}

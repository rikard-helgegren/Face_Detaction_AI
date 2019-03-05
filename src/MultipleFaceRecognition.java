import Catalano.Imaging.FastBitmap;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MultipleFaceRecognition extends JPanel{

    public static final int minFaceSize = 19;
    public static final int maxFaceSize = 60;
    private  static BufferedImage img;
    private static FastBitmap fb;


    private class Square{
        public int x;
        public int y;
        public int w;
        public int h;

        public Square(int x, int y, int w, int h){
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }
    }

    public static void main(String[] args) throws IOException {
        img = loadImageAsGrayScale("./code/test-res/example-res/many_faces.png");
        fb = new FastBitmap(img);

        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }


    private static ArrayList<Square> findFaces() throws IOException, ClassNotFoundException {
        List<StrongClassifier> cascade = Data.loadCascade("save.cascade");



        return new ArrayList<>();
    }

    private static BufferedImage integralImageFromSubWindow(int x, int y, int width, int height){
        BufferedImage newBuff = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

        for(int yl = y;yl<width;yl++){
            for(int xl = x; xl<height; xl++){
                System.out.println(x+" - "+y);
                newBuff.setRGB(xl-x,yl-y, img.getRGB(xl, yl));
            }
        }


        return newBuff;
    }

    //TODO: this is a copy from FaceRec
    private static boolean isFace(List<StrongClassifier> cascade, int x, int y, int width, int height) throws Exception{

        //for(StrongClassifier c : cascade){
        //    if(!c.canBeFace()) return false;
        //}

        return true;
    }

    public void paint(Graphics g) {
        g.drawImage(integralImageFromSubWindow(100,100,400,400), 0, 0, this);
    }

    private static void createAndShowGUI() {
        //Create and set up the window.
        JFrame frame = new JFrame("Image");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setPreferredSize(new Dimension(img.getWidth(),img.getHeight()));
        frame.setMinimumSize(new Dimension(img.getWidth(),img.getHeight()));
        frame.setLocationRelativeTo(null);

        //Add the ubiquitous "Hello World" label.
        frame.getContentPane().add(new MultipleFaceRecognition());

        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }

    //TODO Not sure how well this works...
    public static BufferedImage loadImageAsGrayScale(String path) throws IOException {
        BufferedImage bi = ImageIO.read(new File(path));


        BufferedImage image = new BufferedImage(bi.getWidth(), bi.getHeight(),
                BufferedImage.TYPE_BYTE_GRAY);

        Graphics g = image.getGraphics();
        g.drawImage(bi, 0, 0, null);
        g.dispose();


        return image;
    }
}

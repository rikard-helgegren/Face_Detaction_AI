import Catalano.Imaging.FastBitmap;
import Catalano.Statistics.Kernels.SquaredSinc;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.awt.Dimension;
import java.awt.Graphics;

public class MultipleFaceRecognition extends JPanel{

    public static final int minFaceSize = 25;
    public static final int maxFaceSize = 35;
    private  static BufferedImage img;
    private static FastBitmap fb;
    private static ArrayList<Square> facess;


    private static class Square{
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

        @Override
        public String toString() {
            return "x: "+x+" y: "+y+" w: "+w+" h: "+h;
        }

        public boolean overlaps(Square s){
            return x+w>s.x && x<s.x+s.w && y+h>s.y && y<s.y+s.h;
        }
    }

    public static void main(String[] args) throws Exception {
        img = loadImageAsGrayScale("test-res/examples/many_faces.png");
        fb = new FastBitmap(img);
        CascadeClassifier cascade = new CascadeClassifier("save.cascade");
        System.out.println(cascade.isFace(integralImageFromSubWindow(2,2,19,19)));

        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    createAndShowGUI();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }


    private static ArrayList<Square> findFaces() throws Exception {
        CascadeClassifier cascade = new CascadeClassifier("save.cascade");
        ArrayList<Square> faces = new ArrayList<>();
        System.out.println(img.getHeight());

        for (int s = minFaceSize; s <= maxFaceSize; s+=2) {
            for (int x = 0; x < img.getWidth()-s; x+=s/8){
                for (int y = 0; y < img.getHeight()-s; y+=s/8) {
                    if(cascade.isFace(integralImageFromSubWindow(x,y,s,s))){
                        Square newFace = new Square(x, y, s, s);
                        System.out.println("Face found: "+newFace);
                        faces.add(newFace);

                        /*boolean overlaps = false;
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
        }

        return faces;
    }

    private static HalIntegralImage integralImageFromSubWindow(int x, int y, int width, int height) throws Exception {
        BufferedImage newBuff = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        int yl = 0;
        for(yl = y;yl<height+y;yl++){
            for(int xl = x; xl<width+x; xl++){
                newBuff.setRGB(xl-x,yl-y, img.getRGB(xl, yl));
            }
        }

        return new HalIntegralImage(newBuff);
    }

    public void paint(Graphics g) {
        g.drawImage(this.img,0,0,this);

        Graphics2D newG = (Graphics2D)g;
        newG.setColor(Color.RED);
        newG.setStroke(new BasicStroke(1));
        for(int i=0;i<facess.size();i++){
            newG.drawRect(facess.get(i).x,facess.get(i).y,facess.get(i).w,facess.get(i).h);
        }
    }

    private static void createAndShowGUI() throws Exception {
        ArrayList<Square> faces = findFaces();
        facess = faces;


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

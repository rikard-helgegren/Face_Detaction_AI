package hal2019.graphics;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

//A class used to paint the found faces on an image.
class FacesPainter extends JLabel {
    //The color of the rectangle surrounding the face
    private Color rectangleColor = Color.red;
    //The thickness of the rectangle border
    private int rectangleBorderThickness = 1;
    private BufferedImage img;
    ArrayList<Rectangle> faces;
    /**
     * @param img The image on where to paint the faces
     * @param faces The list of rectangles where faces were found
     */
    FacesPainter(BufferedImage img, ArrayList<Rectangle> faces){
        this.img = img;
        this.faces = faces;
    }

    @Override
    public void paint(Graphics g) {
        g.drawImage(this.img,0,0,this);

        Graphics2D newG = (Graphics2D)g;
        newG.setColor(rectangleColor);
        newG.setStroke(new BasicStroke(rectangleBorderThickness));
        for(int i=0;i<faces.size();i++){
            newG.drawRect(faces.get(i).x,faces.get(i).y,faces.get(i).w,faces.get(i).h);
        }
    }
}

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

class FacesPainter extends JLabel {
    private BufferedImage img;
    ArrayList<Square> faces;
    FacesPainter(BufferedImage img, ArrayList<Square> faces){
        this.img = img;
        this.faces = faces;
    }

    @Override
    public void paint(Graphics g) {
        g.drawImage(this.img,0,0,this);

        Graphics2D newG = (Graphics2D)g;
        newG.setColor(Color.RED);
        newG.setStroke(new BasicStroke(1));
        for(int i=0;i<faces.size();i++){
            newG.drawRect(faces.get(i).x,faces.get(i).y,faces.get(i).w,faces.get(i).h);
        }
    }
}

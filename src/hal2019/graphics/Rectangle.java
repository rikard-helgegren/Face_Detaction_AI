package hal2019.graphics;

import java.util.ArrayList;

class Rectangle {
    public int x;
    public int y;
    public int w;
    public int h;

    /**
     * @param x The x coordinate of the top left corner of the rectangle
     * @param y The y coordinate of the top left corner of the rectangle
     * @param w The width of the rectangle
     * @param h The height of the rectangle
     */
    public Rectangle(int x, int y, int w, int h){
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    @Override
    public String toString() {
        return "x: "+x+" y: "+y+" w: "+w+" h: "+h;
    }

    /**
     * Check if another rectangle overlaps this rectangle
     *
     * @param rectangles The rectangle to compare to this rectangle
     * @return If rectangles overlaps this rectangle
     */
    public boolean overlaps(Rectangle rectangles){
        return x+w>rectangles.x &&
                x<rectangles.x+rectangles.w &&
                y+h>rectangles.y &&
                y<rectangles.y+rectangles.h;
    }

    /**
     * Check if any rectangle overlaps this rectangle
     *
     * @param rectangles The rectangles to compare to this rectangle
     * @return If any rectangle from rectangles overlaps this rectangle
     */
    public boolean overlapsAny(ArrayList<Rectangle> rectangles){
        for(Rectangle r:rectangles){
            if(this.overlaps(r)){
                return true;
            }
        }
        return false;
    }

    /**
     * Scale this rectangles position and size.
     *
     * @param s The factor to scale with
     */
    public void scale(double s){
        x*=s;
        y*=s;
        w*=s;
        h*=s;
    }
}

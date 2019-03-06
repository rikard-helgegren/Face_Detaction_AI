class Square {
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

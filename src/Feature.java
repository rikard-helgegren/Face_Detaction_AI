public class Feature {

    /**
     * Represents the 4 different types of feature.
     */
    public enum Type {HORIZONTAL, VERTICAL, THREE, FOUR};

    private Type type;
    private int x;
    private int y;
    private int w;
    private int h;

    /**
     * Constructs a feature with the given values.
     * @param type the Feature.Type representing the type of feature.
     * @param x coordinate of uppermost left corner
     * @param y coordinate of uppermost left corner
     * @param w width
     * @param h height
     */
    public Feature(Type type, int x, int y, int w, int h) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

}

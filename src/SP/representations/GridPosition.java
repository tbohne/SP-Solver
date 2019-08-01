package SP.representations;

/**
 * Represents positions for stacks and items in a grid layout.
 * The stacks are positioned in the storage area, while the items
 * are positioned on the vehicles they are delivered with.
 *
 * @author Tim Bohne
 */
public class GridPosition {

    private final double xCoord;
    private final double yCoord;

    /**
     * Constructor
     *
     * @param xCoord - x coordinate of position
     * @param yCoord - y coordinate of position
     */
    public GridPosition(double xCoord, double yCoord) {
        this.xCoord = xCoord;
        this.yCoord = yCoord;
    }

    /**
     * Copy-Constructor
     *
     * @param pos - position to be copied
     */
    public GridPosition(GridPosition pos) {
        this.xCoord = pos.getXCoord();
        this.yCoord = pos.getYCoord();
    }

    /**
     * Returns the position's x coordinate.
     *
     * @return x coordinate of position
     */
    public double getXCoord() {
        return this.xCoord;
    }

    /**
     * Returns the position's y coordinate
     *
     * @return y coordinate of position
     */
    public double getYCoord() {
        return this.yCoord;
    }

    /**
     * Provides an appropriate visualization for a position.
     *
     * @return string visualizing the position
     */
    @Override
    public String toString() {
        return "(" + String.format("%.2f", this.xCoord) + "," + String.format("%.2f", this.yCoord) + ")";
    }
}

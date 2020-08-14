package SP.representations;

/**
 * Represents a four dimensional item (length, width, addDimOne, addDimTwo).
 */
public class FourDimensionalItem extends Item {

    private final float addDimOne;
    private final float addDimTwo;

    /**
     * Constructor
     *
     * @param idx       - index that identifies the item
     * @param length    - length of the item
     * @param width     - width of the item
     * @param addDimOne - first additional dimension
     * @param addDimTwo - second additional dimension
     * @param pos       - position of the item on the vehicle with which it is delivered
     */
    public FourDimensionalItem(int idx, float length, float width, float addDimOne, float addDimTwo, GridPosition pos) {
        super(idx, length, width, pos);
        this.addDimOne = addDimOne;
        this.addDimTwo = addDimTwo;
    }

    /**
     * Copy-Constructor
     *
     * @param item - item to be copied
     */
    public FourDimensionalItem(FourDimensionalItem item) {
        super(item.getIdx(), item.getLength(), item.getWidth(), new GridPosition(item.getPosition()));
        this.addDimOne = item.getAddDimOne();
        this.addDimTwo = item.getAddDimTwo();
    }

    /**
     * Returns the item's first additional dimension.
     *
     * @return first additional dimension
     */
    public float getAddDimOne() {
        return this.addDimOne;
    }

    /**
     * Returns the item's second additional dimension.
     *
     * @return second additional dimension
     */
    public float getAddDimTwo() {
        return this.addDimTwo;
    }

    /**
     * Compares two items in terms of their dimensions.
     * This method is basically enabling the sorting of the items w.r.t. the stacking constraints.
     *
     * @param item - item to be compared to
     * @return whether this item is "smaller", "equally sized", or "taller" compared to the other item
     */
    @Override
    public int compareTo(Item item) {
        if (this.getWidth() <= item.getWidth() && this.getLength() <= item.getLength()
            && this.getAddDimOne() <= ((FourDimensionalItem)item).getAddDimOne()
            && this.getAddDimTwo() <= ((FourDimensionalItem)item).getAddDimTwo()) {
                return -1;
        } else if (this.getWidth() == item.getWidth() && this.getLength() == item.getLength()
            && this.getAddDimOne() == ((FourDimensionalItem)item).getAddDimOne()
            && this.getAddDimTwo() == ((FourDimensionalItem)item).getAddDimTwo()) {
                return 0;
        } else {
            return 1;
        }
    }

    /**
     * Returns a string representation of the four dimensional item.
     *
     * @return string representing the item
     */
    @Override
    public String toString() {
        return "idx: " + this.getIdx() + ", width: " + this.getWidth() + ", length: " + this.getLength()
            + ", addDimOne: " + this.getAddDimOne() + ", addDimTwo: " + this.getAddDimTwo();
    }
}

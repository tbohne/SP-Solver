package SP.representations;

/**
 * Represents an item that is going to be assigned to a stack position as part of a stacking problem.
 *
 * @author Tim Bohne
 */
public class Item implements Comparable<Item> {

    private final int idx;
    private final float length;
    private final float width;
    private final GridPosition pos;

    /**
     * Constructor
     *
     * @param idx    - index that identifies the item
     * @param length - length of the item
     * @param width  - width of the item
     * @param pos    - position of the item on the vehicle with which it is delivered
     */
    public Item(int idx, float length, float width, GridPosition pos) {
        this.idx = idx;
        this.length = length;
        this.width = width;
        this.pos = pos;
    }

    /**
     * Copy-Constructor
     *
     * @param item - item to be copied
     */
    public Item(Item item) {
        this.idx = item.getIdx();
        this.length = item.getLength();
        this.width = item.getWidth();
        this.pos = new GridPosition(item.getPosition());
    }

    /**
     * Returns the item's index.
     *
     * @return index of the item
     */
    public int getIdx() {
        return this.idx;
    }

    /**
     * Returns the item's length.
     *
     * @return length of the item
     */
    public float getLength() {
        return  this.length;
    }

    /**
     * Returns the item's width
     *
     * @return width of the item
     */
    public float getWidth() {
        return this.width;
    }

    /**
     * Returns the item's position on the vehicle with which it is delivered.
     *
     * @return position of the item
     */
    public GridPosition getPosition() {
        return this.pos;
    }

    /**
     * Compares two items in terms of their dimensions.
     * This method is basically enabling the sorting of the items w.r.t. the stacking constraints.
     *
     * @param item - item to be compared to
     * @return whether this item is smaller, equally sized, or taller compared to the other item
     */
    @Override
    public int compareTo(Item item) {
        if (this.getWidth() <= item.getWidth() && this.getLength() <= item.getLength()) {
            return -1;
        } else if (this.getWidth() == item.getWidth() && this.getLength() == item.getLength()) {
            return 0;
        } else {
            return 1;
        }
    }

    /**
     * Returns a string representing the item object.
     *
     * @return string representing the item
     */
    @Override
    public String toString() {
        return "idx: " + this.getIdx() + ", width: " + this.getWidth() + ", length: " + this.getLength();
    }
}

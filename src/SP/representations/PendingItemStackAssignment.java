package SP.representations;

/**
 * Represents a pending stack assignment that is yet to be performed.
 *
 * @author Tim Bohne
 */
public class PendingItemStackAssignment {

    private int item;
    private int stack;

    /**
     * Constructor
     *
     * @param item  - item to be assigned to the stack
     * @param stack - stack the item is going to be assigned to
     */
    public PendingItemStackAssignment(int item, int stack) {
        this.item = item;
        this.stack = stack;
    }

    /**
     * Returns the item to be assigned.
     *
     * @return item to be assigned
     */
    public int getItem() {
        return this.item;
    }

    /**
     * Returns the stack the item is going to be assigned to.
     *
     * @return stack the item gets assigned to
     */
    public int getStack() {
        return this.stack;
    }
}

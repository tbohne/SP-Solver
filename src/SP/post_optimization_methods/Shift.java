package SP.post_optimization_methods;

/**
 * Represents a shift operation in the tabu search.
 *
 * @author Tim Bohne
 */
public class Shift {

    private final int item;
    private final int stack;

    /**
     * Constructor
     *
     * @param item  - item to be shifted
     * @param stack - stack the item gets shifted to
     */
    public Shift(int item, int stack) {
        this.item = item;
        this.stack = stack;
    }

    /**
     * Returns the item to be shifted.
     *
     * @return item to be shifted
     */
    private int getItem() {
        return this.item;
    }

    /**
     * Returns the stack the item gets shifted to.
     *
     * @return stack the item gets shifted to
     */
    private int getStack() {
        return this.stack;
    }

    /**
     * Enables equality check between shift operations.
     *
     * @param object - shift to be compared with
     * @return whether or not the shift operations are equal
     */
    @Override
    public boolean equals(Object object) {
        return object != null && object instanceof Shift && (this.item == ((Shift) object).getItem() && this.stack == ((Shift) object).getStack());
    }

    /**
     * Returns the string representing the shift object.
     *
     * @return string representation of shift
     */
    @Override
    public String toString() {
        return this.item + " --- " + this.stack;
    }
}

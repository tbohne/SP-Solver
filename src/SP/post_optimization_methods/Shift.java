package SP.post_optimization_methods;

import SP.representations.StackPosition;

/**
 * Represents a shift operation in the tabu search.
 *
 * @author Tim Bohne
 */
public class Shift {

    private final int item;
    private final StackPosition shiftPos;

    /**
     * Constructor
     *
     * @param item     - item to be shifted
     * @param shiftPos - position the item gets shifted to
     */
    public Shift(int item, StackPosition shiftPos) {
        this.item = item;
        this.shiftPos = shiftPos;
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
     * Returns the position the item gets shifted to.
     *
     * @return position the item gets shifted to
     */
    private StackPosition getShiftPos() {
        return this.shiftPos;
    }

    /**
     * Enables equality check between shift operations.
     *
     * @param object - shift to be compared with
     * @return whether or not the shift operations are equal
     */
    @Override
    public boolean equals(Object object) {
        return object != null && object instanceof Shift && (this.item == ((Shift) object).getItem() && this.shiftPos.equals(((Shift) object).getShiftPos()));
    }

    /**
     * Returns the string representing the shift object.
     *
     * @return string representation of shift
     */
    @Override
    public String toString() {
        return this.item + " --- " + this.shiftPos.toString();
    }
}

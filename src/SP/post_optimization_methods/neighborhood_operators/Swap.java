package SP.post_optimization_methods.neighborhood_operators;

/**
 * Represents a swap operation in the tabu search.
 * A swap operation can be represented by two shift operations.
 *
 * @author Tim Bohne
 */
public class Swap {

    private final Shift shiftOne;
    private final Shift shiftTwo;

    /**
     * Constructor
     *
     * @param shiftOne - first shift of the swap operation
     * @param shiftTwo - second shift of the swap operation
     */
    public Swap(Shift shiftOne, Shift shiftTwo) {
        this.shiftOne = shiftOne;
        this.shiftTwo = shiftTwo;
    }

    /**
     * Returns the swap's first shift operation.
     *
     * @return 1st shift operation
     */
    public Shift getShiftOne() {
        return this.shiftOne;
    }

    /**
     * Returns the swap's second shift operation.
     *
     * @return 2nd shift operation
     */
    public Shift getShiftTwo() {
        return this.shiftTwo;
    }
}

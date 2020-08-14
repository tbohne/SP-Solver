package SP.representations;

/**
 * Represents a position inside a stack in the storage area an item can be assigned to.
 *
 * @author Tim Bohne
 */
public class StackPosition {

    private final int stackIdx;
    private final int level;

    /**
     * Constructor
     *
     * @param stackIdx - index of the stack
     * @param level    - level inside the stack
     */
    public StackPosition(int stackIdx, int level) {
        this.stackIdx = stackIdx;
        this.level = level;
    }

    /**
     * Returns the position's stack index which identifies the stack the position is contained in.
     *
     * @return stack the position is contained in
     */
    public int getStackIdx() {
        return this.stackIdx;
    }

    /**
     * Returns the position's level inside the stack.
     *
     * @return level of the position inside the stack
     */
    public int getLevel() {
        return this.level;
    }

    /**
     * Provides a string representation of the stack position.
     *
     * @return string representation of the stack position
     */
    @Override
    public String toString() {
        return "(stack: " + this.stackIdx + ", level: " + this.level + ")";
    }

    /**
     * Determines whether two stack positions are equal which is the case
     * when they have the same index and level.
     *
     * @param object - stack position to be checked for equality
     * @return whether or not the two stack positions are equal
     */
    @Override
    public boolean equals(Object object) {
        return object != null && object instanceof StackPosition && this.getLevel() == ((StackPosition) object).getLevel()
            && this.getStackIdx() == ((StackPosition) object).getStackIdx();
    }
}

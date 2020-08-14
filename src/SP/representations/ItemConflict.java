package SP.representations;

/**
 * Represents an item's conflicts with other items based on the stacking constraints.
 *
 * @author Tim Bohne
 */
public class ItemConflict implements Comparable<ItemConflict> {

    private final int itemIdx;
    private int numberOfConflicts;

    /**
     * Constructor
     *
     * @param idx       - index of the item
     * @param conflicts - number of conflicts
     */
    public ItemConflict(int idx, int conflicts) {
        this.itemIdx = idx;
        this.numberOfConflicts = conflicts;
    }

    /**
     * Returns the item's number of conflicts.
     *
     * @return number of conflicts
     */
    public int getNumberOfConflicts() {
        return this.numberOfConflicts;
    }

    /**
     * Returns the item's index.
     *
     * @return item index
     */
    public int getItemIdx() {
        return this.itemIdx;
    }

    /**
     * Sets the item's number of conflicts.
     *
     * @param conflicts - number of conflicts
     */
    public void setNumberOfConflicts(int conflicts) {
        this.numberOfConflicts = conflicts;
    }

    /**
     * Enables comparability between instances of item conflicts (based on number of conflicts).
     *
     * @param otherConflict - item conflict to be compared to
     * @return a negative integer, zero, or a positive integer as the first
     *         item conflict is less than, equal to, or greater than the second
     */
    @Override
    public int compareTo(ItemConflict otherConflict) {
        if (this.numberOfConflicts < otherConflict.numberOfConflicts) {
            return -1;
        } else if (otherConflict.numberOfConflicts < this.numberOfConflicts) {
            return 1;
        }
        return 0;
    }

    /**
     * Generates a string representation of an item conflict.
     *
     * @return string representation
     */
    @Override
    public String toString() {
        return "item: " + this.itemIdx + " --- conflicts: " + this.numberOfConflicts;
    }
}

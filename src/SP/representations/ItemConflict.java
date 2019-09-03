package SP.representations;

public class ItemConflict implements Comparable<ItemConflict> {

    private int itemIdx;
    private int numberOfConflicts;

    public ItemConflict(int idx, int conflicts) {
        this.itemIdx = idx;
        this.numberOfConflicts = conflicts;
    }

    public int getNumberOfConflicts() {
        return this.numberOfConflicts;
    }

    public int getItemIdx() {
        return this.itemIdx;
    }

    public void setNumberOfConflicts(int conflicts) {
        this.numberOfConflicts = conflicts;
    }

    @Override
    public int compareTo(ItemConflict otherConflict) {
        if (this.numberOfConflicts < otherConflict.numberOfConflicts) {
            return -1;
        } else if (otherConflict.numberOfConflicts < this.numberOfConflicts) {
            return 1;
        }
        return 0;
    }

    @Override
    public String toString() {
        return "item: " + this.itemIdx + " --- conflicts: " + this.numberOfConflicts;
    }
}

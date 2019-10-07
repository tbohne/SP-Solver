package SP.post_optimization_methods;

public class PartitionShift {

    private final int item;
    private final int partitionIdx;

    public PartitionShift(int item, int partitionIdx) {
        this.item = item;
        this.partitionIdx = partitionIdx;
    }

    private int getItem() {
        return this.item;
    }

    private int getPartitionIdx() {
        return this.partitionIdx;
    }

    public boolean equals(Object object) {
        return object != null && object instanceof PartitionShift && (this.item == ((PartitionShift) object).getItem() && this.partitionIdx == (((PartitionShift) object).getPartitionIdx()));
    }

    public String toString() {
        return this.item + " --- " + this.partitionIdx;
    }
}

package SP.post_optimization_methods;

public class PartitionSwap {

    private final PartitionShift shiftOne;
    private final PartitionShift shiftTwo;

    public PartitionSwap(PartitionShift shiftOne, PartitionShift shiftTwo) {
        this.shiftOne = shiftOne;
        this.shiftTwo = shiftTwo;
    }

    public PartitionShift getShiftOne() {
        return this.shiftOne;
    }

    public PartitionShift getShiftTwo() {
        return this.shiftTwo;
    }
}

package SP.post_optimization_methods;

public class PendingItemStackAssignment {

    private int item;
    private int stack;

    public PendingItemStackAssignment(int item, int stack) {
        this.item = item;
        this.stack = stack;
    }

    public int getItem() {
        return this.item;
    }

    public int getStack() {
        return this.stack;
    }
}

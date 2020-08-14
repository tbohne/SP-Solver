package SP.representations;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an instance of a stacking problem.
 *
 * @author Tim Bohne
 */
public class Instance {

    private final String name;
    private Item[] items;
    private int[][] stacks;
    private final int stackCapacity;
    private List<GridPosition> stackPositions;
    private int[][] stackingConstraints;
    private double[][] costs;

    /**
     * Constructor
     *
     * @param items               - items to be stored in the stacks
     * @param numberOfStacks      - number of available stacks
     * @param stackPositions      - positions of the stacks in the storage area
     * @param stackCapacity       - maximum number of items per stack
     * @param stackingConstraints - stacking constraints to be respected
     * @param costs               - matrix containing the costs of item-stack-assignments
     * @param name                - name of the instance
     */
    public Instance(Item[] items, int numberOfStacks, List<GridPosition> stackPositions, int stackCapacity,
        int[][] stackingConstraints, double[][] costs, String name) {

            this.items = items;
            this.initStacks(numberOfStacks, stackCapacity);
            this.initStackPositions(stackPositions);
            this.stackCapacity = stackCapacity;
            this.stackingConstraints = stackingConstraints;
            this.costs = costs;
            this.name = name;
    }

    /**
     * Copy-Constructor
     *
     * @param instance - instance to be copied
     */
    public Instance(Instance instance) {
        this.stackCapacity = instance.getStackCapacity();
        this.name = instance.getName();
        this.copyStacks(instance);
        this.copyItems(instance);
        this.copyStackPositions(instance);
        this.copyStackingConstraints(instance);
        this.copyCosts(instance);
    }

    /**
     * Resets the instance's stacks.
     */
    public void resetStacks() {
        int numberOfStacks = this.stacks.length;
        int stackCapacity = this.stacks[0].length;
        this.stacks = new int[numberOfStacks][stackCapacity];
        for (int stack = 0; stack < numberOfStacks; stack++) {
            for (int level = 0; level < stackCapacity; level++) {
                this.stacks[stack][level] = -1;
            }
        }
    }

    /**
     * Returns the ground level for the instance's stacks.
     *
     * @return ground level of stacks
     */
    public int getGroundLevel() {
        return this.stackCapacity - 1;
    }

    /**
     * Removes the specified list of items from the storage area.
     *
     * @param items - list of items to be removed
     */
    public void removeItemListFromStorageArea(List<Integer> items) {
        for (int stack = 0; stack < this.getStacks().length; stack++) {
            for (int level = 0; level < this.getStacks()[stack].length; level++) {
                if (items.contains(this.getStacks()[stack][level])) {
                    this.getStacks()[stack][level] = -1;
                }
            }
        }
    }

    /**
     * Returns the instance's items.
     *
     * @return items of the instance
     */
    public int[] getItems() {
        int[] itemIndices = new int[this.items.length];
        for (int i = 0; i < itemIndices.length; i++) {
            itemIndices[i] = i;
        }
        return itemIndices;
    }

    /**
     * Returns the instance's item objects.
     *
     * @return item objects of the instance
     */
    public Item[] getItemObjects() {
        return this.items;
    }

    /**
     * Returns the instance's stacks.
     *
     * @return stacks of the instance
     */
    public int[][] getStacks() {
        return this.stacks;
    }

    /**
     * Returns the instance's stack positions.
     *
     * @return stack positions of the instance
     */
    public List<GridPosition> getStackPositions() {
        return this.stackPositions;
    }

    /**
     * Returns the instance's matrix of stacking constraints.
     *
     * @return stacking constraints of the instance
     */
    public int[][] getStackingConstraints() {
        return this.stackingConstraints;
    }

    /**
     * Returns the instance's transport costs.
     *
     * @return transport costs of the instance
     */
    public double[][] getCosts() {
        return this.costs;
    }

    /**
     * Returns the instance's placement constraints.
     * Since the placement constraints are implemented via high cost entries,
     * they can be retrieved from the matrix of transport costs.
     *
     * @return placement constraints of the instance
     */
    public int[][] getPlacementConstraints() {

        int[][] placementConstraints = new int[this.items.length][this.stacks.length];

        for (int item = 0; item < this.items.length; item++) {
            for (int stack = 0; stack < this.stacks.length; stack++) {
                if (this.costs[item][stack] < Integer.MAX_VALUE / this.items.length) {
                    placementConstraints[item][stack] = 1;
                } else {
                    placementConstraints[item][stack] = 0;
                }
            }
        }
        return placementConstraints;
    }

    /**
     * Returns the instance's stack capacity.
     *
     * @return capacity of the instance's stacks
     */
    public int getStackCapacity() {
        return this.stackCapacity;
    }

    /**
     * Returns the instance's name.
     *
     * @return name of the instance
     */
    public String getName() { return this.name; }

    /**
     * Returns a string containing the instance's data.
     *
     * @return string of instance data
     */
    @Override
    public String toString() {

        StringBuilder str = new StringBuilder("********************************************************************\n");
        str.append("instance: ").append(this.getName()).append("\n");
        str.append("items: ");
        for (Item item : this.getItemObjects()) {
            str.append(item.getIdx()).append(" ");
        }
        str.append("\nnumber of stacks: ").append(this.getStacks().length).append("\n");
        str.append("stack capacity: ").append(this.getStackCapacity()).append("\n\n");

        str.append("item positions:\n");
        for (Item item : this.getItemObjects()) {
            str.append(item.getPosition()).append(" ");
        }
        str.append("\nstack position:\n");
        for (GridPosition pos : this.getStackPositions()) {
            str.append(pos).append(" ");
        }
        str.append("\n\n");

        str.append("stacking constraints:" + "\n");
        for (int i = 0; i < this.getItems().length; i++) {
            for (int j = 0; j < this.getItems().length; j++) {
                str.append(this.getStackingConstraints()[i][j]).append(" ");
            }
            str.append("\n");
        }

        str.append("\ntransport costs:\n");
        for (int i = 0; i < this.getCosts().length; i++) {
            for (int j = 0; j < this.getCosts()[i].length; j++) {
                str.append(this.getCosts()[i][j]).append(" ");
            }
            str.append("\n");
        }
        str.append("********************************************************************\n");

        return str.toString();
    }

    /**
     * Initializes the available stacks. Each position inside the stacks is initialized with -1
     * which means that it's free (has no item assigned to it).
     *
     * @param numberOfStacks - specifies the instance's number of stacks
     * @param stackCapacity  - specifies the capacity of the stacks
     */
    private void initStacks(int numberOfStacks, int stackCapacity) {
        this.stacks = new int[numberOfStacks][stackCapacity];
        for (int stack = 0; stack < numberOfStacks; stack++) {
            for (int level = 0; level < stackCapacity; level++) {
                this.stacks[stack][level] = -1;
            }
        }
    }

    /**
     * Initializes the stack positions inside the storage area.
     *
     * @param stackPositions - list of stack positions
     */
    private void initStackPositions(List<GridPosition> stackPositions) {
        this.stackPositions = new ArrayList<>();
        for (GridPosition pos : stackPositions) {
            this.stackPositions.add(new GridPosition(pos));
        }
    }

    /**
     * Copies the given instance's stacks.
     *
     * @param instance - instance the stacks get copied from
     */
    private void copyStacks(Instance instance) {
        this.stacks = new int[instance.getStacks().length][instance.getStackCapacity()];
        for (int stack = 0; stack < instance.getStacks().length; stack++) {
            for (int level = 0; level < instance.getStackCapacity(); level++) {
                this.stacks[stack][level] = instance.getStacks()[stack][level];
            }
        }
    }

    /**
     * Lowers all items that are stacked 'in the air'.
     */
    public void lowerItemsThatAreStackedInTheAir() {
        for (int stack = 0; stack < this.stacks.length; stack++) {
            boolean loweredItem = true;
            while (loweredItem) {
                loweredItem = false;
                for (int level = this.stacks[stack].length - 1; level > 0; level--) {
                    if (this.stacks[stack][level] == -1 && this.stacks[stack][level - 1] != -1) {
                        this.stacks[stack][level] = this.stacks[stack][level - 1];
                        this.stacks[stack][level - 1] = -1;
                        loweredItem = true;
                        break;
                    }
                }
            }
        }
    }

    /**
     * Copies the given instance's stack positions.
     *
     * @param instance - instance the stack positions get copied from
     */
    private void copyStackPositions(Instance instance) {
        this.stackPositions = new ArrayList<>();
        for (GridPosition pos : instance.getStackPositions()) {
            stackPositions.add(new GridPosition(pos));
        }
    }

    /**
     * Copies the given instance's items.
     *
     * @param instance - instance the items get copied from
     */
    private void copyItems(Instance instance) {
        this.items = new Item[instance.getItems().length];
        for (int i = 0; i < instance.getItems().length; i++) {
            this.items[i] = new Item(instance.getItemObjects()[i]);
        }
    }

    /**
     * Copies the given instance's stacking constraints.
     *
     * @param instance - instance the stacking constraints get copied from
     */
    private void copyStackingConstraints(Instance instance) {
        this.stackingConstraints = new int[instance.getStackingConstraints().length][];
        for (int i = 0; i < instance.getStackingConstraints().length; i++) {
            this.stackingConstraints[i] = instance.getStackingConstraints()[i].clone();
        }
    }

    /**
     * Copies the given instance's costs.
     *
     * @param instance - instance the costs get copied from
     */
    private void copyCosts(Instance instance) {
        this.costs = new double[instance.getCosts().length][];
        for (int i = 0; i < instance.getCosts().length; i++) {
            this.costs[i] = instance.getCosts()[i].clone();
        }
    }
}

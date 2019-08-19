package SP.representations;

import SP.util.RepresentationUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a solution of a stacking problem.
 *
 * @author Tim Bohne
 */
public class Solution implements Comparable<Solution> {

    private Instance solvedInstance;
    private double timeToSolve;
    private double timeLimit;
    private int[][] filledStacks;
    private final boolean empty;

    /**
     * Constructor for empty solutions.
     */
    public Solution() {
        this.empty = true;
        this.filledStacks = new int[0][];
    }

    /**
     * Constructor
     *
     * @param timeToSolve    - time it took to create the solution
     * @param timeLimit      - considered time limit
     * @param solvedInstance - solved instance
     */
    public Solution(double timeToSolve, double timeLimit, Instance solvedInstance) {
        this.timeToSolve = timeToSolve;
        this.empty = false;
        this.timeLimit = timeLimit;
        this.solvedInstance = solvedInstance;
        this.createFilledStacksFromInstance();
    }

    /**
     * Copy-Constructor
     *
     * @param sol - solution to be copied
     */
    public Solution(Solution sol) {
        this.timeToSolve = sol.getTimeToSolveAsDouble();
        this.empty = sol.isEmpty();
        this.solvedInstance = new Instance(sol.getSolvedInstance());
        this.timeLimit = sol.getTimeLimit();

        this.filledStacks = new int[sol.solvedInstance.getStacks().length][];
        for (int i = 0; i < sol.getFilledStacks().length; i++) {
            this.filledStacks[i] = sol.getFilledStacks()[i].clone();
        }
    }

    /**
     * Returns the solution's stacks filled with the assigned items.
     *
     * @return filled stacks of the solution
     */
    public int[][] getFilledStacks() {
        return this.filledStacks;
    }

    /**
     * Returns the instance the solution was generated for.
     *
     * @return solved instance
     */
    public Instance getSolvedInstance() {
        return this.solvedInstance;
    }

    /**
     * Returns the considered time limit for the solution generation.
     *
     * @return considered time limit
     */
    public double getTimeLimit() {
        return this.timeLimit;
    }

    /**
     * Returns the solution's number of items that are assigned to stacks.
     *
     * @return number of assigned items
     */
    public int getNumberOfAssignedItems() {
        return this.getAssignedItems().size();
    }

    /**
     * Returns the items of the solution that are assigned to stacks.
     *
     * @return list of assigned items
     */
    public List<Integer> getAssignedItems() {
        if (this.empty) {
            return new ArrayList<>();
        }
        ArrayList<Integer> assignedItems = new ArrayList<>();
        for (int[] filledStack : this.filledStacks) {
            for (int item : filledStack) {
                if (item != -1) {
                    assignedItems.add(item);
                }
            }
        }
        return assignedItems;
    }

    /**
     * Returns a string representation of the solutions time to solve the instance.
     *
     * @return time to solve the instance
     */
    public String getTimeToSolve() {
        return String.format("%.02f", this.timeToSolve).replace(",", ".");
    }

    /**
     * Returns the solution's time to solve the instance.
     *
     * @return time to solve the instance
     */
    public double getTimeToSolveAsDouble() {
        return this.timeToSolve;
    }

    /**
     * Returns the string representation of the solution's objective value (transport costs).
     *
     * @return objective value of the solution
     */
    public String getObjectiveValue() {
        return String.format("%.02f", this.computeCosts()).replace(",", ".");
    }

    /**
     * Returns the name of the solved instance.
     *
     * @return name of the solved instance
     */
    public String getNameOfSolvedInstance() {
        return this.solvedInstance.getName();
    }

    /**
     * Returns whether the solution is empty which means that the items are not yet assigned to stacks.
     *
     * @return whether the solution is empty
     */
    public boolean isEmpty() {
        return this.empty;
    }

    /**
     * Sets the solution's time to solve the instance.
     *
     * @param duration - the duration it took to solve the instance
     */
    public void setTimeToSolve(double duration) {
        this.timeToSolve = duration;
    }

    /**
     * Transforms the given, possibly unordered stack assignments into feasible ones by sorting the items
     * with regard to the transitive stacking constraints.
     */
    public void sortItemsInStacksBasedOnTransitiveStackingConstraints() {

        for (int stack = 0; stack < this.filledStacks.length; stack++) {

            // retrieve indices of items that are assigned to the stack and clear stack positions
            List<Integer> itemIndices = new ArrayList<>();
            for (int level = 0; level < this.solvedInstance.getStackCapacity(); level++) {
                if (this.filledStacks[stack][level] != -1) {
                    itemIndices.add(this.filledStacks[stack][level]);
                    this.filledStacks[stack][level] = -1;
                }
            }
            // retrieve item objects by indices
            List<Item> items = new ArrayList<>();
            for (int itemIdx : itemIndices) {
                // an item's idx should be the same as its position in the array
                if (itemIdx == this.solvedInstance.getItemObjects()[itemIdx].getIdx()) {
                    items.add(this.solvedInstance.getItemObjects()[itemIdx]);
                // if it's not, the item has to be retrieved manually
                } else {
                    for (Item item : this.solvedInstance.getItemObjects()) {
                        if (item.getIdx() == itemIdx) {
                            items.add(item);
                        }
                    }
                }
            }
            // sort items with regard to the transitive stacking constraints
            Collections.sort(items);

            // if the stack isn't completely filled, the offset is used to start at the correct top level
            int offset = this.solvedInstance.getStackCapacity() - items.size();

            int idx = 0;
            for (int level = offset; level < this.solvedInstance.getStackCapacity(); level++) {
                if (idx < items.size()) {
                    this.filledStacks[stack][level] = items.get(idx++).getIdx();
                }
            }
        }
    }

    /**
     * Lowers all items that are stacked 'in the air'.
     */
    public void lowerItemsThatAreStackedInTheAir() {
        for (int stack = 0; stack < this.filledStacks.length; stack++) {
            boolean loweredItem = true;
            while (loweredItem) {
                loweredItem = false;
                for (int level = this.filledStacks[stack].length - 1; level > 0; level--) {
                    if (this.filledStacks[stack][level] == -1 && this.filledStacks[stack][level - 1] != -1) {
                        this.filledStacks[stack][level] = this.filledStacks[stack][level - 1];
                        this.filledStacks[stack][level - 1] = -1;
                        loweredItem = true;
                        break;
                    }
                }
            }
        }
    }

    public boolean soFarFeasible() {

        this.lowerItemsThatAreStackedInTheAir();

        if (!this.stackingConstraintsRespected()) {
            System.out.println("infeasible solution - the stacking constraints are violated");
            return false;
        } else if (!this.placementConstraintsRespected()) {
            System.out.println("infeasible solution - the placement constraints are violated");
            return false;
        } else if (this.containsDuplicates()) {
            System.out.println("infeasible solution - there are items that are assigned more than once");
            return false;
        } else if (containsGaps()) {
            System.out.println("infeasible solution - there are gaps in the filled stacks");
            return false;
        }
        return true;
    }

    /**
     * Returns whether the solution is feasible which means that all items have been
     * assigned to a position in a stack in a way that respects the stacking- and
     * placement-constraints and the stack capacity. Additionally no item has been assigned
     * to more than one position and no stack contains gaps.
     *
     * @return whether the solution is feasible
     */
    public boolean isFeasible() {
        if (!this.allItemsAssigned()) {
            System.out.println("infeasible solution - not all items have been assigned");
            return false;
        } else if (!this.stackingConstraintsRespected()) {
            System.out.println("infeasible solution - the stacking constraints are violated");
            return false;
        } else if (!this.placementConstraintsRespected()) {
            System.out.println("infeasible solution - the placement constraints are violated");
            return false;
        } else if (this.containsDuplicates()) {
            System.out.println("infeasible solution - there are items that are assigned more than once");
            return false;
        } else if (this.containsGaps()) {
            System.out.println("infeasible solution - there are gaps in the filled stacks");
            return false;
        }
        return true;
    }

    /**
     * Prints the solution's filled stacks.
     */
    public void printFilledStacks() {
        for (int i = 0; i < this.getFilledStacks().length; i++) {
            for (int j = 0; j < this.getFilledStacks()[i].length; j++) {
                System.out.print(this.getFilledStacks()[i][j] + " ");
            }
            System.out.println();
        }
    }

    /**
     * Computes the solution's transport costs which result from the item-stack-assignments.
     *
     * @return transport costs of the solution
     */
    public double computeCosts() {
        if (this.empty) {
            return Double.MAX_VALUE;
        }
        double costs = 0.0;
        for (int stack = 0; stack < this.filledStacks.length; stack++) {
            for (int level = 0; level < this.filledStacks[stack].length; level++) {
                int item = this.filledStacks[stack][level];
                if (item != -1) {
                    costs += this.solvedInstance.getCosts()[item][stack];
                }
            }
        }
        return Math.round(costs * 100.0) / 100.0;
    }

    /**
     * Returns a string representation of the solution's data.
     *
     * @return string representation of the solution
     */
    @Override
    public String toString() {

        StringBuilder str = new StringBuilder();

        if (this.isFeasible()) {
            str.append("time limit: ").append(this.timeLimit).append(" s\n");
            str.append("time to solve: ").append(String.format("%.2f", this.timeToSolve)).append(" s");
            str.append(this.timeToSolve > this.timeLimit ? " (time limit exceeded)\n" : "\n");
            str.append("objective value: ").append(this.computeCosts());
            str.append(this.timeToSolve > this.timeLimit ? " (not optimal)\n\n" : "\n\n");
            str.append("stacks (top to bottom):\n");

            int maxStringOffset = RepresentationUtil.getMaximumStringOffset(this.filledStacks.length);

            for (int i = 0; i < this.filledStacks.length; i++) {
                String space = RepresentationUtil.getCurrentSpace(i, maxStringOffset);
                str.append("stack ").append(space).append(i).append(":    ");
                for (int j = 0; j < this.filledStacks[i].length; j++) {
                    if (this.filledStacks[i][j] != -1) {
                        str.append(this.filledStacks[i][j]).append(" ");
                    }
                }
                str.append("\n");
            }
        } else {
            str.append("Problem not solved.\n");
        }
        return str.toString();
    }

    /**
     * Compares two solutions with regard to their costs.
     *
     * @param sol - the solution to be compared to
     * @return whether this solution's costs are smaller, equal to, or greater than the other solution's costs
     */
    @Override
    public int compareTo(Solution sol) {
        if (this.computeCosts() < sol.computeCosts()) {
            return -1;
        } else if (sol.computeCosts() < this.computeCosts()) {
            return 1;
        }
        return 0;
    }

    /**
     * Returns whether all of the instance's items have been assigned to a stack.
     *
     * @return whether all items have been assigned to a stack
     */
    private boolean allItemsAssigned() {
        boolean[] allItemsAssigned = new boolean[this.solvedInstance.getItems().length];
        for (int[] filledStack : this.filledStacks) {
            for (int item : filledStack) {
                if (item != -1) {
                    allItemsAssigned[item] = true;
                }
            }
        }
        for (boolean itemAssigned : allItemsAssigned) {
            if (!itemAssigned) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the items that are not yet assigned to a stack position.
     *
     * @return list of unassigned items
     */
    public List<Integer> getUnassignedItems() {
        List<Integer> assigned = new ArrayList<>();
        for (int[] filledStack : this.filledStacks) {
            for (int item : filledStack) {
                if (item != -1) { assigned.add(item); }
            }
        }
        List<Integer> unassigned = new ArrayList<>();
        for (int item = 0; item < this.solvedInstance.getItems().length; item++) {
            if (!assigned.contains(item)) {
                unassigned.add(item);
            }
        }
        return unassigned;
    }

    /**
     * Returns whether the stacking constraints are respected by the solution's stack assignments.
     *
     * @return whether the solution is respecting the stacking constraints
     */
    private boolean stackingConstraintsRespected() {
        for (int[] filledStack : this.filledStacks) {
            for (int level = 1; level < filledStack.length; level++) {
                int itemBelow = filledStack[level];
                int itemAbove = filledStack[level - 1];
                if (itemAbove != -1 && itemBelow != -1) {
                    if (this.solvedInstance.getStackingConstraints()[itemAbove][itemBelow] != 1) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Returns whether the placement constraints are respected by the solution's stack assignments.
     *
     * @return whether the solution is respecting the placement constraints
     */
    private boolean placementConstraintsRespected() {
        for (int stack = 0; stack < this.filledStacks.length; stack++) {
            for (int level = 0; level < this.filledStacks[stack].length; level++) {
                int item = this.filledStacks[stack][level];
                if (item != -1) {
                    if (this.solvedInstance.getCosts()[item][stack] >= Integer.MAX_VALUE / this.solvedInstance.getItems().length) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Returns whether the solution contains duplicates (items occurring more than once).
     *
     * @return whether the solution contains duplicate items
     */
    private boolean containsDuplicates() {
        ArrayList<Integer> assignedItems = new ArrayList<>();
        for (int[] filledStack : this.filledStacks) {
            for (int item : filledStack) {
                if (item != -1) {
                    if (assignedItems.contains(item)) {
                        return true;
                    }
                    assignedItems.add(item);
                }
            }
        }
        return false;
    }

    /**
     * Returns whether the solution's filled stacks contain gaps.
     *
     * @return whether the filled stacks contain gaps
     */
    private boolean containsGaps() {
        for (int[] filledStack : this.filledStacks) {
            for (int level = filledStack.length - 1; level > 0; level--) {
                if (filledStack[level] == -1 && filledStack[level - 1] != -1) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Creates the filled stacks from the solved instance's stack assignments.
     */
    private void createFilledStacksFromInstance() {
        this.filledStacks = new int[this.solvedInstance.getStacks().length][];
        for (int i = 0; i < this.solvedInstance.getStacks().length; i++) {
            this.filledStacks[i] = this.solvedInstance.getStacks()[i].clone();
        }
    }
}

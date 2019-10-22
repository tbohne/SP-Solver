package SP.post_optimization_methods;

import SP.representations.Instance;
import SP.representations.Solution;
import SP.representations.StackPosition;
import SP.util.HeuristicUtil;

import java.util.List;

/**
 * Operator to be used in neighborhood structures for the local search.
 * A shift operation moves an item from one stack to a compatible free position in another stack.
 *
 * @author Tim Bohne
 */
public class ShiftOperator {

    private final int unsuccessfulNbrGenerationAttempts;

    /**
     * Constructor
     *
     * @param unsuccessfulNbrGenerationAttempts - number of failing attempts to generate a neighboring solution
     *                                            after which the search for a neighbor is stopped
     */
    public ShiftOperator(int unsuccessfulNbrGenerationAttempts) {
        this.unsuccessfulNbrGenerationAttempts = unsuccessfulNbrGenerationAttempts;
    }

    /**
     * Generates a neighbor for the specified solution by applying the shift operator.
     *
     * @param currSol         - current solution to generate neighbor for
     * @param performedShifts - list of performed shifts
     * @return generated neighboring solution
     */
    public Solution generateShiftNeighbor(Solution currSol, List<Shift> performedShifts) {
        Solution neighbor = new Solution(currSol);
        StackPosition srcPos = HeuristicUtil.getRandomStackPositionFilledWithItem(neighbor);
        int item = neighbor.getFilledStacks()[srcPos.getStackIdx()][srcPos.getLevel()];
        StackPosition shiftTarget = this.getFeasibleShiftTarget(neighbor, srcPos, item);
        if (shiftTarget == null) { return neighbor; }
        Shift shift = this.shiftItem(neighbor, item, srcPos, shiftTarget.getStackIdx());
        performedShifts.clear();
        performedShifts.add(shift);
        neighbor.lowerItemsThatAreStackedInTheAir();
        neighbor.sortItemsInStacksBasedOnTransitiveStackingConstraints();
        return neighbor;
    }

    /**
     * Shifts an item from its source position to a target position in the stacks of the specified solution.
     *
     * @param sol         - solution for which the item gets shifted
     * @param item        - item to be shifted
     * @param srcPos      - original position of the item to be shifted
     * @param targetStack - stack the item gets shifted to
     * @return performed shift operation
     */
    private Shift shiftItem(Solution sol, int item, StackPosition srcPos, int targetStack) {
        HeuristicUtil.assignItemToStack(item, sol.getFilledStacks()[targetStack], sol.getSolvedInstance().getItemObjects());
        sol.getFilledStacks()[srcPos.getStackIdx()][srcPos.getLevel()] = -1;
        return new Shift(item, targetStack);
    }

    /**
     * Searches a feasible shift target for the specified item.
     *
     * @param neighbor - neighboring solution to perform shift for
     * @param srcPos   - original position of the item to be shifted
     * @param item     - item to be shifted
     * @return stack position the item gets shifted to or null if search failed
     */
    private StackPosition getFeasibleShiftTarget(Solution neighbor, StackPosition srcPos, int item) {

        StackPosition shiftTarget = HeuristicUtil.getRandomFreeSlot(neighbor);
        shiftTarget = HeuristicUtil.ensureShiftTargetInDifferentStack(shiftTarget, srcPos, neighbor);
        int failCnt = 0;
        Instance instance = neighbor.getSolvedInstance();

        while (!HeuristicUtil.itemCompatibleWithStack(instance.getCosts(), item, shiftTarget.getStackIdx()) ||
            !HeuristicUtil.itemCompatibleWithAlreadyAssignedItems(
                item, neighbor.getFilledStacks()[shiftTarget.getStackIdx()],instance.getItemObjects(), instance.getStackingConstraints()
            )
        ) {
            if (failCnt == this.unsuccessfulNbrGenerationAttempts) {
                System.out.println("FAILED TO FIND A COMPATIBLE SHIFT TARGET");
                return null;
            }
            shiftTarget = HeuristicUtil.getRandomFreeSlot(neighbor);
            shiftTarget = HeuristicUtil.ensureShiftTargetInDifferentStack(shiftTarget, srcPos, neighbor);
            failCnt++;
        }
        return shiftTarget;
    }
}

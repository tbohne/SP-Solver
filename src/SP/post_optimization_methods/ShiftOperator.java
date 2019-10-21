package SP.post_optimization_methods;

import SP.representations.Instance;
import SP.representations.Solution;
import SP.representations.StackPosition;
import SP.util.HeuristicUtil;

import java.util.List;

public class ShiftOperator {

    private int unsuccessfulNbrGenerationAttempts;

    public ShiftOperator(int unsuccessfulNbrGenerationAttempts) {
        this.unsuccessfulNbrGenerationAttempts = unsuccessfulNbrGenerationAttempts;
    }

    private Shift shiftItem(Solution sol, int item, StackPosition srcPos, int targetStack) {
        HeuristicUtil.assignItemToStack(item, sol.getFilledStacks()[targetStack], sol.getSolvedInstance().getItemObjects());
        sol.getFilledStacks()[srcPos.getStackIdx()][srcPos.getLevel()] = -1;
        return new Shift(item, targetStack);
    }

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
}

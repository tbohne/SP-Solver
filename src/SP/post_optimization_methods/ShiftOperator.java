package SP.post_optimization_methods;

import SP.representations.Instance;
import SP.representations.Solution;
import SP.representations.StackPosition;
import SP.util.HeuristicUtil;

import java.util.List;

public class ShiftOperator {

    public ShiftOperator() {}


    public static Shift shiftItem(Solution sol, int item, StackPosition pos, StackPosition shiftTarget) {

        HeuristicUtil.assignItemToStack(item, sol.getFilledStacks()[shiftTarget.getStackIdx()], sol.getSolvedInstance().getItemObjects());

        sol.getFilledStacks()[pos.getStackIdx()][pos.getLevel()] = -1;

        return new Shift(item, shiftTarget.getStackIdx());
    }

    public Solution generateShiftNeighbor(Solution currSol, List<Shift> performedShifts, int unsuccessfulNeighborGenerationAttempts) {

        Solution neighbor = new Solution(currSol);
        StackPosition pos = HeuristicUtil.getRandomStackPositionFilledWithItem(neighbor);
        int item = neighbor.getFilledStacks()[pos.getStackIdx()][pos.getLevel()];
        StackPosition shiftTarget = HeuristicUtil.getRandomFreeSlot(neighbor);
        HeuristicUtil.ensureShiftTargetInDifferentStack(shiftTarget, pos, neighbor);

        int failCnt = 0;

        Instance instance = currSol.getSolvedInstance();

        while (!HeuristicUtil.itemCompatibleWithStack(instance.getCosts(), item, shiftTarget.getStackIdx())
                || !HeuristicUtil.itemCompatibleWithAlreadyAssignedItems(
                item, currSol.getFilledStacks()[shiftTarget.getStackIdx()],instance.getItemObjects(), instance.getStackingConstraints()
        )
                ) {
            if (failCnt == unsuccessfulNeighborGenerationAttempts) {
                System.out.println("FAILED TO FIND A COMPATIBLE SHIFT TARGET");
                return neighbor;
            }
            shiftTarget = HeuristicUtil.getRandomFreeSlot(neighbor);
            HeuristicUtil.ensureShiftTargetInDifferentStack(shiftTarget, pos, neighbor);
            failCnt++;
        }

        Shift shift = this.shiftItem(neighbor, item, pos, shiftTarget);
        performedShifts.clear();
        performedShifts.add(shift);
        neighbor.lowerItemsThatAreStackedInTheAir();
        neighbor.sortItemsInStacksBasedOnTransitiveStackingConstraints();
        return neighbor;
    }

}

package SP.post_optimization_methods;

import SP.representations.Instance;
import SP.representations.Solution;
import SP.representations.StackPosition;
import SP.util.HeuristicUtil;
import SP.util.NeighborhoodUtil;

import java.util.List;

@SuppressWarnings("Duplicates")
public class ShiftOperator {

    public ShiftOperator() {}

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

        Shift shift = HeuristicUtil.shiftItem(neighbor, item, pos, shiftTarget);
        performedShifts.clear();
        NeighborhoodUtil.blockShiftForWholeStack(currSol, item, shiftTarget.getStackIdx(), performedShifts);

        performedShifts.add(shift);
        neighbor.lowerItemsThatAreStackedInTheAir();
        neighbor.sortItemsInStacksBasedOnTransitiveStackingConstraints();
        return neighbor;
    }

}

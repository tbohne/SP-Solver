package SP.post_optimization_methods;

import SP.representations.Item;
import SP.representations.Solution;
import SP.representations.StackPosition;
import SP.util.HeuristicUtil;

import java.util.ArrayList;
import java.util.List;

public class SwapOperator {

    private int maxNumberOfSwaps;
    private int unsuccessfulNbrGenerationAttempts;

    public SwapOperator(int maxNumberOfSwaps, int unsuccessfulNbrGenerationAttempts) {
        this.maxNumberOfSwaps = maxNumberOfSwaps;
        this.unsuccessfulNbrGenerationAttempts = unsuccessfulNbrGenerationAttempts;
    }

    /**
     * Exchanges the items in the specified positions in the stacks of the given solution.
     *
     * @param sol    - solution to be altered
     * @param posOne - first position of the exchange
     * @param posTwo - second position of the exchange
     */
    private Swap swapItems(Solution sol, StackPosition posOne, StackPosition posTwo) {
        int itemOne = sol.getFilledStacks()[posOne.getStackIdx()][posOne.getLevel()];
        int itemTwo = sol.getFilledStacks()[posTwo.getStackIdx()][posTwo.getLevel()];
        // clear stack pos of swap items
        sol.getFilledStacks()[posOne.getStackIdx()][posOne.getLevel()] = -1;
        sol.getFilledStacks()[posTwo.getStackIdx()][posTwo.getLevel()] = -1;
        HeuristicUtil.assignItemToStack(itemOne, sol.getFilledStacks()[posTwo.getStackIdx()], sol.getSolvedInstance().getItemObjects());
        HeuristicUtil.assignItemToStack(itemTwo, sol.getFilledStacks()[posOne.getStackIdx()], sol.getSolvedInstance().getItemObjects());
        // the swap operations consists of two shift operations
        return new Swap(new Shift(itemOne, posTwo.getStackIdx()), new Shift(itemTwo, posOne.getStackIdx()));
    }

    private StackPosition getFeasibleSwapPartner(Solution neighbor, Solution currSol, StackPosition posOne, int swapItemOne) {
        int failCnt = 0;
        StackPosition posTwo = HeuristicUtil.getRandomStackPositionFilledWithItem(neighbor);
        int swapItemTwo = currSol.getFilledStacks()[posTwo.getStackIdx()][posTwo.getLevel()];
        // not swapping inside a stack
        HeuristicUtil.ensureShiftTargetInDifferentStack(posTwo, posOne, neighbor);

        double[][] costs = neighbor.getSolvedInstance().getCosts();
        int[][] stacks = neighbor.getFilledStacks();
        Item[] itemObjects = neighbor.getSolvedInstance().getItemObjects();
        int[][] stackingConstraints = neighbor.getSolvedInstance().getStackingConstraints();

        while (!HeuristicUtil.itemCompatibleWithStack(costs, swapItemOne, posTwo.getStackIdx())
            || !HeuristicUtil.itemCompatibleWithStack(costs, swapItemTwo, posOne.getStackIdx())
            || !HeuristicUtil.itemCompatibleWithAlreadyAssignedItems(swapItemOne, stacks[posTwo.getStackIdx()], itemObjects, stackingConstraints)
            || !HeuristicUtil.itemCompatibleWithAlreadyAssignedItems(swapItemTwo, stacks[posOne.getStackIdx()], itemObjects, stackingConstraints)
        ) {

            if (failCnt == this.unsuccessfulNbrGenerationAttempts) {
                System.out.println("FAILED TO FIND A COMPATIBLE SWAP");
                return null;
            }
            posTwo = HeuristicUtil.getRandomStackPosition(neighbor);
            HeuristicUtil.ensureShiftTargetInDifferentStack(posTwo, posOne, neighbor);
        }
        return posTwo;
    }

    /**
     * Performs the specified number of swap operations and stores them in the swap list.
     *
     * @param swapList      - list to store the performer swaps
     * @return generated neighbor solution
     */
    private Solution performSwaps(List<Swap> swapList, Solution currSol, int numberOfSwaps) {

        Solution neighbor = new Solution(currSol);
        int swapCnt = 0;

        while (swapCnt < numberOfSwaps) {

            StackPosition posOne = HeuristicUtil.getRandomStackPositionFilledWithItem(neighbor);
            int swapItemOne = neighbor.getFilledStacks()[posOne.getStackIdx()][posOne.getLevel()];

            StackPosition posTwo = this.getFeasibleSwapPartner(neighbor, currSol, posOne, swapItemOne);
            if (posTwo == null) { return neighbor; }

            Solution tmpSol = new Solution(neighbor);
            Swap swap = this.swapItems(tmpSol, posOne, posTwo);
            if (!swapList.contains(swap)) {
                swapList.add(swap);
                neighbor = tmpSol;
                swapCnt++;
                neighbor.lowerItemsThatAreStackedInTheAir();
                neighbor.sortItemsInStacksBasedOnTransitiveStackingConstraints();
            }
        }
        return neighbor;
    }


    /**
     * Generates a neighbor for the current solution using the "k-swap-neighborhood".
     *
     * @return a neighboring solution
     */
    @SuppressWarnings("Duplicates")
    public Solution generateSwapNeighbor(Solution currSol, List<Shift> performedShifts, int numberOfSwaps) {
        int numOfSwaps = numberOfSwaps > maxNumberOfSwaps ? maxNumberOfSwaps : numberOfSwaps;
        List<Swap> swapList = new ArrayList<>();
        Solution neighbor = this.performSwaps(swapList, currSol, numOfSwaps);
        for (Swap swap : swapList) {
            performedShifts.add(swap.getShiftOne());
            performedShifts.add(swap.getShiftTwo());
        }
        return neighbor;
    }
}

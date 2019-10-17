package SP.post_optimization_methods;

import SP.representations.Solution;
import SP.representations.StackPosition;
import SP.util.HeuristicUtil;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("Duplicates")
public class SwapOperator {

    private int maxNumberOfSwaps;

    public SwapOperator(int maxNumberOfSwaps) {
        this.maxNumberOfSwaps = maxNumberOfSwaps;
    }

    /**
     * Exchanges the items in the specified positions in the stacks of the given solution.
     *
     * @param sol    - solution to be altered
     * @param posOne - first position of the exchange
     * @param posTwo - second position of the exchange
     */
    public Swap swapItems(Solution sol, StackPosition posOne, StackPosition posTwo) {
        int itemOne = sol.getFilledStacks()[posOne.getStackIdx()][posOne.getLevel()];
        int itemTwo = sol.getFilledStacks()[posTwo.getStackIdx()][posTwo.getLevel()];
        sol.getFilledStacks()[posOne.getStackIdx()][posOne.getLevel()] = itemTwo;
        sol.getFilledStacks()[posTwo.getStackIdx()][posTwo.getLevel()] = itemOne;

        // the swap operations consists of two shift operations
        return new Swap(new Shift(itemOne, posTwo), new Shift(itemTwo, posOne));
    }

    /**
     * Performs the specified number of swap operations and stores them in the swap list.
     *
     * @param swapList      - list to store the performer swaps
     * @return generated neighbor solution
     */
    public Solution performSwaps(List<Swap> swapList, Solution currSol, int numberOfSwaps) {

        Solution neighbor = new Solution(currSol);
        int swapCnt = 0;

        while (swapCnt < numberOfSwaps) {
            StackPosition posOne = HeuristicUtil.getRandomStackPositionFilledWithItem(neighbor);
            int swapItemOne = neighbor.getFilledStacks()[posOne.getStackIdx()][posOne.getLevel()];
            StackPosition posTwo = HeuristicUtil.getRandomStackPositionFilledWithItem(neighbor);
            int swapItemTwo = currSol.getFilledStacks()[posTwo.getStackIdx()][posTwo.getLevel()];

            // the swapped items should differ
            while (swapItemTwo == swapItemOne) {
                posTwo = HeuristicUtil.getRandomStackPosition(neighbor);
                swapItemTwo = currSol.getFilledStacks()[posTwo.getStackIdx()][posTwo.getLevel()];
            }

            Solution tmpSol = new Solution(neighbor);
            Swap swap = this.swapItems(tmpSol, posOne, posTwo);
            if (!swapList.contains(swap)) {
                swapList.add(swap);
                neighbor = tmpSol;
                swapCnt++;
                neighbor.lowerItemsThatAreStackedInTheAir();
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
    public Solution generateSwapNeighbor(Solution currSol, List<Shift> performedShifts, int unsuccessfulNeighborGenerationAttempts, int numberOfSwaps) {

        int numOfSwaps = numberOfSwaps > maxNumberOfSwaps ? maxNumberOfSwaps : numberOfSwaps;

        List<Solution> nbrs = new ArrayList<>();
        int failCnt = 0;
        int unsuccessfulKSwapCounter = 0;

        List<Swap> swapList = new ArrayList<>();

        Solution neighbor = this.performSwaps(swapList, currSol, numOfSwaps);
        for (Swap swap : swapList) {
            performedShifts.add(swap.getShiftOne());
            performedShifts.add(swap.getShiftTwo());
        }
        return neighbor;
    }

}

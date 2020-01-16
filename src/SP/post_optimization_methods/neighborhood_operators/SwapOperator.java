package SP.post_optimization_methods.neighborhood_operators;

import SP.representations.Item;
import SP.representations.Solution;
import SP.representations.StackPosition;
import SP.util.HeuristicUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Operator to be used in neighborhood structures for the local search.
 * A swap operation swaps two compatible items which means that the items exchange positions in the stacks.
 *
 * @author Tim Bohne
 */
public class SwapOperator {

    private final int maxNumberOfSwaps;
    private final int unsuccessfulNbrGenerationAttempts;

    /**
     * Constructor
     *
     * @param maxNumberOfSwaps                  - maximum number of swaps performed in one operator application
     * @param unsuccessfulNbrGenerationAttempts - number of failing attempts to generate a neighboring solution
     *                                            after which the search for a neighbor is stopped
     */
    public SwapOperator(int maxNumberOfSwaps, int unsuccessfulNbrGenerationAttempts) {
        this.maxNumberOfSwaps = maxNumberOfSwaps;
        this.unsuccessfulNbrGenerationAttempts = unsuccessfulNbrGenerationAttempts;
    }

    /**
     * Generates a neighbor for the specified solution by applying the swap operator.
     *
     * @param currSol         - current solution to generate neighbor for
     * @param performedShifts - list of performed shifts (a swap consists of two shifts)
     * @param numberOfSwaps   - number of swaps to be performed
     * @return generated neighboring solution
     */
    public Solution generateSwapNeighbor(Solution currSol, List<Shift> performedShifts, int numberOfSwaps) {
        int numOfSwaps = numberOfSwaps > maxNumberOfSwaps ? maxNumberOfSwaps : numberOfSwaps;
        List<Swap> swapList = new ArrayList<>();
        Solution neighbor = this.performSwaps(swapList, currSol, numOfSwaps);
        for (Swap swap : swapList) {
            performedShifts.add(swap.getShiftOne());
            performedShifts.add(swap.getShiftTwo());
        }
        neighbor.lowerItemsThatAreStackedInTheAir();
        neighbor.sortItemsInStacksBasedOnTransitiveStackingConstraints();
        return neighbor;
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
        // assign items to new positions
        HeuristicUtil.assignItemToStack(
            itemOne, sol.getFilledStacks()[posTwo.getStackIdx()], sol.getSolvedInstance().getItemObjects()
        );
        HeuristicUtil.assignItemToStack(
            itemTwo, sol.getFilledStacks()[posOne.getStackIdx()], sol.getSolvedInstance().getItemObjects()
        );
        // the swap operations consists of two shift operations
        return new Swap(new Shift(itemOne, posTwo.getStackIdx()), new Shift(itemTwo, posOne.getStackIdx()));
    }

    /**
     * Retrieves a feasible swap partner for the specified item.
     *
     * @param neighbor    - neighboring solution to perform swap for
     * @param posOne      - position of the item to find a swap partner for
     * @param swapItemOne - item to find feasible swap partner for
     * @return stack position of the swap partner
     */
    private StackPosition getFeasibleSwapPartner(Solution neighbor, StackPosition posOne, int swapItemOne) {
        int failCnt = 0;
        StackPosition posTwo = HeuristicUtil.getRandomStackPositionFilledWithItem(neighbor);
        int swapItemTwo = neighbor.getFilledStacks()[posTwo.getStackIdx()][posTwo.getLevel()];

        double[][] costs = neighbor.getSolvedInstance().getCosts();
        int[][] stacks = neighbor.getFilledStacks();
        Item[] itemObjects = neighbor.getSolvedInstance().getItemObjects();
        int[][] stackingConstraints = neighbor.getSolvedInstance().getStackingConstraints();

        while (!HeuristicUtil.itemCompatibleWithStack(costs, swapItemOne, posTwo.getStackIdx())
            || !HeuristicUtil.itemCompatibleWithStack(costs, swapItemTwo, posOne.getStackIdx())
            || !HeuristicUtil.itemCompatibleWithAlreadyAssignedItems(swapItemOne, stacks[posTwo.getStackIdx()], itemObjects, stackingConstraints)
            || !HeuristicUtil.itemCompatibleWithAlreadyAssignedItems(swapItemTwo, stacks[posOne.getStackIdx()], itemObjects, stackingConstraints)
            || posOne.getStackIdx() == posTwo.getStackIdx()
        ) {
            if (failCnt == this.unsuccessfulNbrGenerationAttempts) {
                System.out.println("FAILED TO FIND A COMPATIBLE SWAP");
                return null;
            }
            posTwo = HeuristicUtil.getRandomStackPositionFilledWithItem(neighbor);
            swapItemTwo = neighbor.getFilledStacks()[posTwo.getStackIdx()][posTwo.getLevel()];
            failCnt++;
        }
        return posTwo;
    }

    /**
     * Performs the specified number of swap operations and stores them in the swap list.
     *
     * @param swapList      - list to store the performed swaps
     * @param currSol       - current solution
     * @param numberOfSwaps - number of swaps to be performed
     * @return generated neighboring solution
     */
    private Solution performSwaps(List<Swap> swapList, Solution currSol, int numberOfSwaps) {

        Solution neighbor = new Solution(currSol);
        int swapCnt = 0;

        while (swapCnt < numberOfSwaps) {
            StackPosition posOne = HeuristicUtil.getRandomStackPositionFilledWithItem(neighbor);
            int swapItemOne = neighbor.getFilledStacks()[posOne.getStackIdx()][posOne.getLevel()];
            StackPosition posTwo = this.getFeasibleSwapPartner(neighbor, posOne, swapItemOne);

            if (posTwo == null) { return neighbor; }

            Solution tmpSol = new Solution(neighbor);
            Swap swap = this.swapItems(tmpSol, posOne, posTwo);
            if (!swapList.contains(swap)) {
                swapList.add(swap);
                neighbor = tmpSol;
                swapCnt++;
            }
        }
        return neighbor;
    }
}

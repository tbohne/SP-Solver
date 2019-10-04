package SP.post_optimization_methods;

import SP.experiments.PostOptimization;
import SP.representations.Solution;
import SP.representations.StackPosition;
import SP.util.HeuristicUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class StackBasedNeighborhood implements SwapShiftNeighborhood {

    private int numberOfNeighbors;
    private PostOptimization.ShortTermStrategies shortTermStrategy;
    private int maxTabuListLength;
    private int unsuccessfulNeighborGenerationAttempts;
    private int unsuccessfulKSwapAttempts;

    private int tabuListClears;

    public StackBasedNeighborhood(
        int numberOfNeighbors, PostOptimization.ShortTermStrategies shortTermStrategy,
        int maxTabuListLength, int unsuccessfulNeighborGenerationAttempts, int unsuccessfulKSwapAttempts
    ) {
        this.numberOfNeighbors = numberOfNeighbors;
        this.shortTermStrategy = shortTermStrategy;
        this.maxTabuListLength = maxTabuListLength;
        this.unsuccessfulNeighborGenerationAttempts = unsuccessfulNeighborGenerationAttempts;
        this.tabuListClears = 0;
        this.unsuccessfulNeighborGenerationAttempts = unsuccessfulNeighborGenerationAttempts;
        this.unsuccessfulKSwapAttempts = unsuccessfulKSwapAttempts;
    }

    /**
     * Returns a random position in the stacks.
     *
     * @param sol - solution for which to retrieve a random stack position
     * @return random position in the stacks
     */
    public StackPosition getRandomStackPosition(Solution sol) {
        int stackIdx = HeuristicUtil.getRandomIntegerInBetween(0, sol.getFilledStacks().length - 1);
        int level = HeuristicUtil.getRandomIntegerInBetween(0, sol.getFilledStacks()[stackIdx].length - 1);
        return new StackPosition(stackIdx, level);
    }

    /**
     * Retrieves the free slots in the stacks.
     *
     * @param sol - solution to retrieve the free slots for
     * @return list of free slots in the stacks
     */
    public List<StackPosition> getFreeSlots(Solution sol) {
        List<StackPosition> freeSlots = new ArrayList<>();
        for (int stack = 0; stack < sol.getFilledStacks().length; stack++) {
            for (int level = 0; level < sol.getFilledStacks()[stack].length; level++) {
                if (sol.getFilledStacks()[stack][level] == -1) {
                    freeSlots.add(new StackPosition(stack, level));
                }
            }
        }
        return freeSlots;
    }

    /**
     * Returns a random free slot in the stacks of the specified solution.
     *
     * @param sol - specified solution to return a free slot for
     * @return random free slot in the stacks
     */
    public StackPosition getRandomFreeSlot(Solution sol) {
        List<StackPosition> freeSlots = getFreeSlots(sol);
        int freeSlotIdx = HeuristicUtil.getRandomIntegerInBetween(0, freeSlots.size() - 1);
        return freeSlots.get(freeSlotIdx);
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
     * Shifts the item stored in pos to the shift target.
     *
     * @param sol         - solution to be updated
     * @param item        - item to be shifted
     * @param shiftTarget - position the item is shifted to
     * @param pos         - the item's original position
     */
    public Shift shiftItem(Solution sol, int item, StackPosition pos, StackPosition shiftTarget) {

        sol.getFilledStacks()[shiftTarget.getStackIdx()][shiftTarget.getLevel()] =
                sol.getFilledStacks()[pos.getStackIdx()][pos.getLevel()];

        sol.getFilledStacks()[pos.getStackIdx()][pos.getLevel()] = -1;
        return new Shift(item, shiftTarget);
    }

    /**
     * Returns a random stack position that is occupied with an item.
     *
     * @param neighbor - neighbor to return an occupied stack position for
     * @return occupied stack position
     */
    public StackPosition getRandomStackPositionFilledWithItem(Solution neighbor) {
        StackPosition pos = getRandomStackPosition(neighbor);
        int item = neighbor.getFilledStacks()[pos.getStackIdx()][pos.getLevel()];
        while (item == -1) {
            pos = getRandomStackPosition(neighbor);
            item = neighbor.getFilledStacks()[pos.getStackIdx()][pos.getLevel()];
        }
        return pos;
    }

    /**
     * Ensures that the shift target is in a different stack.
     *
     * @param shiftTarget - stack position the item gets shifted to
     * @param pos         - current position of the item
     * @param neighbor    - considered solution
     */
    public void getRandomShiftTargetFromOtherStack(StackPosition shiftTarget, StackPosition pos, Solution neighbor) {
        while (shiftTarget.getStackIdx() == pos.getStackIdx()) {
            shiftTarget = getRandomFreeSlot(neighbor);
        }
    }

    /**
     * Adds the specified shift operation to the tabu list.
     * Replaces the oldest entry if the maximum length of the tabu list is reached.
     *
     * @param shift - shift operation to be added to the tabu list
     */
    public void forbidShift(Shift shift, Queue<Shift> tabuList) {
        if (tabuList.size() >= this.maxTabuListLength) {
            tabuList.poll();
        }
        tabuList.add(shift);
    }

    /**
     * Clears the entries in the shift tabu list and increments the clear counter.
     */
    public void clearTabuList(Queue<Shift> tabuList) {
        tabuList.clear();
//        tabuList = new LinkedList<>();
        this.tabuListClears++;
    }

    public int getTabuListClears() {
        return this.tabuListClears;
    }

    /**
     * Generates a neighbor for the current solution using the "shift-neighborhood".
     *
     * @return neighboring solution
     */
    @SuppressWarnings("Duplicates")
    public Solution getNeighborShift(Solution currSol, Queue<Shift> tabuList, Solution bestSol) {

        List<Solution> nbrs = new ArrayList<>();
        int failCnt = 0;

        while (nbrs.size() < this.numberOfNeighbors) {

            Solution neighbor = new Solution(currSol);
            StackPosition pos = getRandomStackPositionFilledWithItem(neighbor);
            int item = neighbor.getFilledStacks()[pos.getStackIdx()][pos.getLevel()];
            StackPosition shiftTarget = getRandomFreeSlot(neighbor);
            getRandomShiftTargetFromOtherStack(shiftTarget, pos, neighbor);
            Shift shift = shiftItem(neighbor, item, pos, shiftTarget);
            neighbor.lowerItemsThatAreStackedInTheAir();

            if (!neighbor.isFeasible()) { continue; }

            // FIRST-FIT
            if (this.shortTermStrategy == PostOptimization.ShortTermStrategies.FIRST_FIT && !tabuList.contains(shift)
                && neighbor.computeCosts() < currSol.computeCosts()) {

                forbidShift(shift, tabuList);
                return neighbor;

                // BEST-FIT
            } else if (!tabuList.contains(shift)) {
                nbrs.add(neighbor);
                forbidShift(shift, tabuList);
            } else {
                failCnt++;
                if (failCnt == this.unsuccessfulNeighborGenerationAttempts) {
                    failCnt = 0;
                    if (nbrs.size() == 0) {
                        System.out.println("CLEAR");
                        this.clearTabuList(tabuList);
                    } else {
                        return HeuristicUtil.getBestSolution(nbrs);
                    }
                }
            }
            // ASPIRATION CRITERION
            if (neighbor.computeCosts() < bestSol.computeCosts()) {
                if (this.shortTermStrategy == PostOptimization.ShortTermStrategies.FIRST_FIT) {
                    return neighbor;
                } else {
                    nbrs.add(neighbor);
                }
            }
        }
        return HeuristicUtil.getBestSolution(nbrs);
    }

    /**
     * Performs the specified number of swap operations and stores them in the swap list.
     *
     * @param numberOfSwaps - number of swaps to be performed
     * @param swapList      - list to store the performer swaps
     * @return generated neighbor solution
     */
    public Solution performSwaps(int numberOfSwaps, List<Swap> swapList, Solution currSol) {

        Solution neighbor = new Solution(currSol);
        int swapCnt = 0;

        while (swapCnt < numberOfSwaps) {
            StackPosition posOne = this.getRandomStackPositionFilledWithItem(neighbor);
            int swapItemOne = neighbor.getFilledStacks()[posOne.getStackIdx()][posOne.getLevel()];
            StackPosition posTwo = this.getRandomStackPositionFilledWithItem(neighbor);
            int swapItemTwo = currSol.getFilledStacks()[posTwo.getStackIdx()][posTwo.getLevel()];

            // the swapped items should differ
            while (swapItemTwo == swapItemOne) {
                posTwo = this.getRandomStackPosition(neighbor);
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
     * Checks whether the swap list contains a tabu swap operation.
     *
     * @param swapList - list of swaps to be checked for tabu operations
     * @return whether or not the swap list contains a tabu swap operation
     */
    public boolean containsTabuSwap(List<Swap> swapList, Queue<Shift> tabuList) {
        for (Swap swap : swapList) {
            // a swap consists of two shift operations
            if (tabuList.contains(swap.getShiftOne()) && tabuList.contains(swap.getShiftTwo())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Generates a neighbor for the current solution using the "k-swap-neighborhood".
     *
     * @param numberOfSwaps - number of swaps to be performed
     * @return a neighboring solution
     */
    @SuppressWarnings("Duplicates")
    public Solution getNeighborKSwap(int numberOfSwaps, Solution currSol, Queue<Shift> tabuList, Solution bestSol) {

        List<Solution> nbrs = new ArrayList<>();
        int failCnt = 0;
        int unsuccessfulKSwapCounter = 0;

        while (nbrs.size() < this.numberOfNeighbors) {

            if (numberOfSwaps > 1 && unsuccessfulKSwapCounter++ == this.unsuccessfulKSwapAttempts) {
                Solution best = HeuristicUtil.getBestSolution(nbrs);
                if (best.isEmpty()) {
                    return currSol;
                }
                return best;
            }
            List<Swap> swapList = new ArrayList<>();
            Solution neighbor = this.performSwaps(numberOfSwaps, swapList, currSol);

            if (!neighbor.isFeasible()) { continue; }

            // FIRST-FIT
            if (this.shortTermStrategy == PostOptimization.ShortTermStrategies.FIRST_FIT
                    && !this.containsTabuSwap(swapList, tabuList) && neighbor.computeCosts() < currSol.computeCosts()) {

                for (Swap swap : swapList) {
                    this.forbidShift(swap.getShiftOne(), tabuList);
                    this.forbidShift(swap.getShiftTwo(), tabuList);
                }
                return neighbor;

                // BEST-FIT
            } else if (!this.containsTabuSwap(swapList, tabuList)) {
                nbrs.add(neighbor);
                for (Swap swap : swapList) {
                    this.forbidShift(swap.getShiftOne(), tabuList);
                    this.forbidShift(swap.getShiftTwo(), tabuList);
                }
            } else {
                failCnt++;
                if (failCnt == this.unsuccessfulNeighborGenerationAttempts) {
                    failCnt = 0;
                    if (nbrs.size() == 0) {
                        System.out.println("CLEAR");
                        this.clearTabuList(tabuList);
                    } else {
                        return HeuristicUtil.getBestSolution(nbrs);
                    }
                }
            }
            // ASPIRATION CRITERION
            if (neighbor.computeCosts() < bestSol.computeCosts()) {
                if (this.shortTermStrategy == PostOptimization.ShortTermStrategies.FIRST_FIT) {
                    return neighbor;
                } else {
                    nbrs.add(neighbor);
                }
            }
        }
        return HeuristicUtil.getBestSolution(nbrs);
    }
}

package SP.post_optimization_methods;

import SP.experiments.PostOptimization;
import SP.representations.Solution;
import SP.representations.StackPosition;
import SP.util.HeuristicUtil;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class StackBasedSwapShiftNeighborhood implements Neighborhood {

    private int numberOfNeighbors;
    private PostOptimization.ShortTermStrategies shortTermStrategy;
    private int maxTabuListLength;
    private int unsuccessfulNeighborGenerationAttempts;
    private int unsuccessfulKSwapAttempts;
    private float kSwapProbability;
    private int kSwapIntervalUB;
    private float swapProbability;

    private int tabuListClears;
    private Queue<Shift> tabuList;

    public StackBasedSwapShiftNeighborhood(
        int numberOfNeighbors, PostOptimization.ShortTermStrategies shortTermStrategy,
        int maxTabuListLength, int unsuccessfulNeighborGenerationAttempts, int unsuccessfulKSwapAttempts,
        float kSwapProbability, int kSwapIntervalUB, float swapProbability
    ) {
        this.numberOfNeighbors = numberOfNeighbors;
        this.shortTermStrategy = shortTermStrategy;
        this.maxTabuListLength = maxTabuListLength;
        this.unsuccessfulNeighborGenerationAttempts = unsuccessfulNeighborGenerationAttempts;
        this.tabuListClears = 0;
        this.unsuccessfulNeighborGenerationAttempts = unsuccessfulNeighborGenerationAttempts;
        this.unsuccessfulKSwapAttempts = unsuccessfulKSwapAttempts;
        this.tabuList = new LinkedList<>();
        this.kSwapProbability = kSwapProbability;
        this.kSwapIntervalUB = kSwapIntervalUB;
        this.swapProbability = swapProbability;
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
     * Adds the specified shift operation to the tabu list.
     * Replaces the oldest entry if the maximum length of the tabu list is reached.
     *
     * @param shift - shift operation to be added to the tabu list
     */
    public void forbidShift(Shift shift) {
        if (this.tabuList.size() >= this.maxTabuListLength) {
            this.tabuList.poll();
        }
        this.tabuList.add(shift);
    }

    /**
     * Clears the entries in the shift tabu list and increments the clear counter.
     */
    public void clearTabuList() {
        this.tabuList.clear();
        this.tabuListClears++;
    }

    public int getTabuListClears() {
        return this.tabuListClears;
    }

    public Solution getNeighbor(Solution currSol, Solution bestSol) {
        double rand = Math.random();
        if (rand < this.kSwapProbability / 100.0) {
            return this.getNeighborKSwap(HeuristicUtil.getRandomIntegerInBetween(2, this.kSwapIntervalUB), currSol, bestSol);
        } else if (rand < (this.swapProbability + this.kSwapProbability) / 100.0) {
            return this.getNeighborKSwap(1, currSol, bestSol);
        } else {
            // shift is only possible if there are free slots
            if (currSol.getNumberOfAssignedItems() < currSol.getFilledStacks().length * currSol.getFilledStacks()[0].length) {
                return this.getNeighborShift(currSol, bestSol);
            } else {
                return this.getNeighborKSwap(1, currSol, bestSol);
            }
        }
    }

    /**
     * Generates a neighbor for the current solution using the "shift-neighborhood".
     *
     * @return neighboring solution
     */
    @SuppressWarnings("Duplicates")
    public Solution getNeighborShift(Solution currSol, Solution bestSol) {

        List<Solution> nbrs = new ArrayList<>();
        int failCnt = 0;

        while (nbrs.size() < this.numberOfNeighbors) {

            Solution neighbor = new Solution(currSol);
            StackPosition pos = HeuristicUtil.getRandomStackPositionFilledWithItem(neighbor);
            int item = neighbor.getFilledStacks()[pos.getStackIdx()][pos.getLevel()];
            StackPosition shiftTarget = HeuristicUtil.getRandomFreeSlot(neighbor);
            HeuristicUtil.ensureShiftTargetInDifferentStack(shiftTarget, pos, neighbor);
            Shift shift = HeuristicUtil.shiftItem(neighbor, item, pos, shiftTarget);
            neighbor.lowerItemsThatAreStackedInTheAir();

            if (!neighbor.isFeasible()) { continue; }

            // FIRST-FIT
            if (this.shortTermStrategy == PostOptimization.ShortTermStrategies.FIRST_FIT && !this.tabuList.contains(shift)
                && neighbor.computeCosts() < currSol.computeCosts()) {

                forbidShift(shift);
                return neighbor;

                // BEST-FIT
            } else if (!this.tabuList.contains(shift)) {
                nbrs.add(neighbor);
                forbidShift(shift);
            } else {
                failCnt++;
                if (failCnt == this.unsuccessfulNeighborGenerationAttempts) {
                    failCnt = 0;
                    if (nbrs.size() == 0) {
                        System.out.println("CLEAR");
                        this.clearTabuList();
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
     * Checks whether the swap list contains a tabu swap operation.
     *
     * @param swapList - list of swaps to be checked for tabu operations
     * @return whether or not the swap list contains a tabu swap operation
     */
    public boolean containsTabuSwap(List<Swap> swapList) {
        for (Swap swap : swapList) {
            // a swap consists of two shift operations
            if (this.tabuList.contains(swap.getShiftOne()) && this.tabuList.contains(swap.getShiftTwo())) {
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
    public Solution getNeighborKSwap(int numberOfSwaps, Solution currSol, Solution bestSol) {

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
                    && !this.containsTabuSwap(swapList) && neighbor.computeCosts() < currSol.computeCosts()) {

                for (Swap swap : swapList) {
                    this.forbidShift(swap.getShiftOne());
                    this.forbidShift(swap.getShiftTwo());
                }
                return neighbor;

                // BEST-FIT
            } else if (!this.containsTabuSwap(swapList)) {
                nbrs.add(neighbor);
                for (Swap swap : swapList) {
                    this.forbidShift(swap.getShiftOne());
                    this.forbidShift(swap.getShiftTwo());
                }
            } else {
                failCnt++;
                if (failCnt == this.unsuccessfulNeighborGenerationAttempts) {
                    failCnt = 0;
                    if (nbrs.size() == 0) {
                        System.out.println("CLEAR");
                        this.clearTabuList();
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

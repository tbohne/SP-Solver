package SP.post_optimization_methods.neighborhood_operators;

import SP.representations.Instance;
import SP.representations.Item;
import SP.representations.Solution;
import SP.representations.StackPosition;
import SP.util.HeuristicUtil;

/**
 * Operator to be used in neighborhood structures for the local search.
 * A swap operation swaps two compatible items which means that the items exchange positions in the stacks.
 *
 * @author Tim Bohne
 */
public class SwapOperator {

    private final int unsuccessfulNbrGenerationAttempts;

    /**
     * Constructor
     *
     * @param unsuccessfulNbrGenerationAttempts - number of failing attempts to generate a neighboring solution
     *                                            after which the search for a neighbor is stopped
     */
    public SwapOperator(int unsuccessfulNbrGenerationAttempts) {
        this.unsuccessfulNbrGenerationAttempts = unsuccessfulNbrGenerationAttempts;
    }

    /**
     * Generates a neighbor for the specified solution by applying the swap operator.
     * The swap operator searches the whole swap neighborhood and returns a best found solution.
     *
     * @param currSol - current solution to generate neighbor for
     * @return generated neighboring solution
     */
    public Solution generateSwapNeighbor(Solution currSol) {

        Solution bestNbr = new Solution(currSol);
        double currSolCosts = currSol.computeCosts();
        double bestNbrCosts = currSolCosts;

        for (int stack = 0; stack < currSol.getFilledStacks().length; stack++) {
            for (int level = 0; level < currSol.getFilledStacks()[stack].length; level++) {

                if (currSol.getFilledStacks()[stack][level] != -1) {

                    StackPosition posOne = new StackPosition(stack, level);
                    int swapItemOne = currSol.getFilledStacks()[stack][level];
                    Instance instance = currSol.getSolvedInstance();

                    for (int targetStack = 0; targetStack < currSol.getFilledStacks().length; targetStack++) {

                        if (posOne.getStackIdx() == targetStack) { continue; }

                        for (int targetLevel = 0; targetLevel < currSol.getFilledStacks()[targetStack].length; targetLevel++) {

                            if (currSol.getFilledStacks()[targetStack][targetLevel] != -1) {

                                int swapItemTwo = currSol.getFilledStacks()[targetStack][targetLevel];

                                if (HeuristicUtil.itemCompatibleWithStack(instance.getCosts(), swapItemOne, targetStack)
                                    && HeuristicUtil.itemCompatibleWithStack(instance.getCosts(), swapItemTwo, posOne.getStackIdx())
                                    && HeuristicUtil.itemCompatibleWithAlreadyAssignedItems(
                                        swapItemOne, currSol.getFilledStacks()[targetStack],
                                        instance.getItemObjects(), instance.getStackingConstraints()
                                    )
                                    && HeuristicUtil.itemCompatibleWithAlreadyAssignedItems(
                                        swapItemTwo, currSol.getFilledStacks()[posOne.getStackIdx()],
                                        instance.getItemObjects(), instance.getStackingConstraints()
                                    )
                                ) {
                                    double nbrCosts = currSolCosts - instance.getCosts()[swapItemOne][posOne.getStackIdx()]
                                        - instance.getCosts()[swapItemTwo][targetStack]
                                        + instance.getCosts()[swapItemOne][targetStack]
                                        + instance.getCosts()[swapItemTwo][posOne.getStackIdx()];

                                    if (nbrCosts < bestNbrCosts) {
                                        Solution nbr = new Solution(currSol);
                                        this.swapItems(nbr, posOne, new StackPosition(targetStack, targetLevel));
                                        bestNbr = nbr;
                                        bestNbrCosts = nbrCosts;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return bestNbr;
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

        sol.lowerItemsThatAreStackedInTheAirForSpecificStack(posOne.getStackIdx());
        sol.lowerItemsThatAreStackedInTheAirForSpecificStack(posTwo.getStackIdx());
        sol.sortItemsInStackBasedOnTransitiveStackingConstraints(posOne.getStackIdx());
        sol.sortItemsInStackBasedOnTransitiveStackingConstraints(posTwo.getStackIdx());

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
     * @param currSol - current solution
     * @return generated neighboring solution
     */
    public Solution generateRandomSwapNeighbor(Solution currSol) {

        Solution neighbor = new Solution(currSol);

        StackPosition posOne = HeuristicUtil.getRandomStackPositionFilledWithItem(neighbor);
        int swapItemOne = neighbor.getFilledStacks()[posOne.getStackIdx()][posOne.getLevel()];
        StackPosition posTwo = this.getFeasibleSwapPartner(neighbor, posOne, swapItemOne);

        if (posTwo == null) { return neighbor; }

        this.swapItems(neighbor, posOne, posTwo);

        return neighbor;
    }
}

package SP.exact_solvers;

import SP.representations.Item;
import SP.representations.ItemConflict;
import SP.representations.Solution;
import SP.util.HeuristicUtil;
import SP.util.LowerBoundsUtil;

import java.util.*;

/**
 * Exact branch-and-bound solver to generate feasible solutions to stacking problems.
 * The goal is to minimize the transport costs while respecting all given constraints.
 *
 * @author Tim Bohne
 */
public class BranchAndBound {

    private final Solution initialSolution;
    private final int numberOfItems;
    private final double[][] costs;
    private final int[][] stackingConstraints;
    private final Item[] itemObjects;
    private Solution bestSol;
    private Map<Solution, Double> computedLowerBounds;
    private Map<Solution, List<ItemConflict>> listOfConflicts;

    /**
     * Constructor
     *
     * @param initialSolution - solution to be used as reference in branch-and-bound procedure
     */
    public BranchAndBound(Solution initialSolution) {
        this.initialSolution = initialSolution;
        this.bestSol = new Solution(this.initialSolution);
        this.numberOfItems = initialSolution.getSolvedInstance().getItemObjects().length;
        this.costs = initialSolution.getSolvedInstance().getCosts();
        this.stackingConstraints = initialSolution.getSolvedInstance().getStackingConstraints();
        this.itemObjects = initialSolution.getSolvedInstance().getItemObjects();
        this.computedLowerBounds = new HashMap<>();
        this.listOfConflicts = new HashMap<>();
    }

    /**
     * Starts the branch-and-bound procedure for the given instance.
     */
    public void solve() {
        System.out.println("feasible: " + this.initialSolution.isFeasible());
        System.out.println("costs: " + this.initialSolution.computeCosts());
        Solution clearSol = new Solution(this.initialSolution);
        this.clearSolution(clearSol);
        this.branchAndBound(clearSol);
        this.bestSol.lowerItemsThatAreStackedInTheAir();
        System.out.println("feasible: " + this.bestSol.isFeasible());
        System.out.println("costs: " + this.bestSol.computeCosts());
    }

    private int getBranchingItem(Solution currSol) {

        List<Integer> unassignedItems = currSol.getUnassignedItems();
        int itemToBeAdded = unassignedItems.get(0);

        if (this.listOfConflicts.get(currSol) != null) {
            for (ItemConflict conf : this.listOfConflicts.get(currSol)) {
                if (unassignedItems.contains(conf.getItemIdx())) {
                    itemToBeAdded = conf.getItemIdx();
                    break;
                }
            }
        }
        return itemToBeAdded;

        // DEFAULT BRANCHING RULE:
        // int itemToBeAdded = currSol.getAssignedItems().size();
    }

    private void updateBestSolution(Solution tmpSol) {
        if (tmpSol.computeCosts() < this.bestSol.computeCosts()) {
            System.out.println("best sol updated..");
            this.bestSol = new Solution(tmpSol);
        }
    }

    private void saveConflictsForCurrentLB(Solution solutionLB, Solution currentSol) {
        List<ItemConflict> conflictList = solutionLB.getNumberOfConflictsForEachItem();
        this.listOfConflicts.put(currentSol, conflictList);
    }

    private void printStatistics(
        int cuts, int worseLBCuts, int feasibleLBCuts, int updatedBestSolCuts, int iterations, int visitedNodes, int unexploredNodes
    ) {

        String worseLBCutsPercentage = String.format("%.2f", (worseLBCuts * 1.0 / cuts * 100.0));
        String feasibleLBCutsPercentage = String.format("%.2f", (feasibleLBCuts * 1.0 / cuts * 100.0));
        String updatedBestSolCutsPercentage = String.format("%.2f", (updatedBestSolCuts * 1.0 / cuts * 100.0));

        System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        System.out.println("iterations: " + iterations);
        System.out.println("visited nodes: " + visitedNodes);
        System.out.println(
            "cuts: " + cuts
            + " (worseLBCuts: " + worseLBCutsPercentage + "%"
            + ", feasibleLBCuts: " + feasibleLBCutsPercentage + "%"
            + ", updateBestSolCuts: " + updatedBestSolCutsPercentage + "%)"
        );
        System.out.println("unexplored nodes (queue size): " + unexploredNodes);
        System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
    }

    /**
     * Branch-and-Bound procedure that generates exact solutions for stacking problems.
     *
     * @param sol - currently considered partial solution
     */
    private void branchAndBound(Solution sol) {

        PriorityQueue<Solution> unexploredNodes = new PriorityQueue<>(1, new CombinedComparator());
        unexploredNodes.add(new Solution(sol));

        int visitedNodes = 0;
        int cuts = 0;
        int updatedBestSolCuts = 0;
        int feasibleLBCuts = 0;
        int worseLBCuts = 0;
        int iterations = 0;

        while (!unexploredNodes.isEmpty()) {
            this.printStatistics(cuts, worseLBCuts, feasibleLBCuts, updatedBestSolCuts, iterations, visitedNodes, unexploredNodes.size());
            iterations++;
            Solution currSol = unexploredNodes.poll();

            // the best sol could have been updated since this solution was added - check again
            if ((this.computedLowerBounds.containsKey(currSol) && this.computedLowerBounds.get(currSol) >= this.bestSol.computeCosts())) {
                cuts++;
                updatedBestSolCuts++;
                continue;
            }
            visitedNodes++;

            for (int stack = 0; stack < currSol.getFilledStacks().length; stack++) {

                int itemToBeAdded = this.getBranchingItem(currSol);

                if (HeuristicUtil.stackHasFreePosition(currSol.getFilledStacks()[stack])
                    && HeuristicUtil.itemCompatibleWithStack(this.costs, itemToBeAdded, stack)
                    && HeuristicUtil.itemCompatibleWithAlreadyAssignedItems(
                        itemToBeAdded, currSol.getFilledStacks()[stack], this.itemObjects, this.stackingConstraints
                )) {

                    Solution tmpSol = new Solution(currSol);
                    HeuristicUtil.assignItemToStack(itemToBeAdded, tmpSol.getFilledStacks()[stack], this.itemObjects);

                    if (tmpSol.getAssignedItems().size() == this.numberOfItems) {
                        this.updateBestSolution(tmpSol);
                    } else {

                        Solution solutionLB = LowerBoundsUtil.computeLowerBound(tmpSol);
                        solutionLB.sortItemsInStacksBasedOnTransitiveStackingConstraints();

                        // rest can be cut off
                        if (solutionLB.isFeasible()) {
                            this.updateBestSolution(solutionLB);
                            cuts++;
                            feasibleLBCuts++;
                        } else {
                            if (solutionLB.computeCosts() < this.bestSol.computeCosts()) {
                                unexploredNodes.add(tmpSol);
                                this.computedLowerBounds.put(tmpSol, solutionLB.computeCosts());
                                this.saveConflictsForCurrentLB(solutionLB, tmpSol);
                            } else {
                                cuts++;
                                worseLBCuts++;
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Clears the stacks of the specified solution.
     *
     * @param sol - solution to clear the stacks for
     */
    private void clearSolution(Solution sol) {
        for (int i = 0; i < sol.getFilledStacks().length; i++) {
            for (int j = 0; j < sol.getFilledStacks()[0].length; j++) {
                sol.getFilledStacks()[i][j] = -1;
            }
        }
    }
}

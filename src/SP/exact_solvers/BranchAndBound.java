package SP.exact_solvers;

import SP.representations.Item;
import SP.representations.Solution;
import SP.util.HeuristicUtil;
import SP.util.LowerBoundsUtil;

import java.util.PriorityQueue;

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
    }

    /**
     * Starts the branch-and-bound procedure for the given instance.
     */
    public void solve() {
        Solution clearSol = new Solution(this.initialSolution);
        this.clearSolution(clearSol);
        this.branchAndBound(clearSol);
        this.bestSol.lowerItemsThatAreStackedInTheAir();
        System.out.println("feasible: " + this.bestSol.isFeasible());
        System.out.println("costs: " + this.bestSol.computeCosts());
    }

    /**
     * Branch-and-Bound procedure that generates exact solutions for stacking problems.
     *
     * @param sol - currently considered partial solution
     */
    private void branchAndBound(Solution sol) {

        PriorityQueue<Solution> unexploredNodes = new PriorityQueue<>();
        unexploredNodes.add(new Solution(sol));

        while (!unexploredNodes.isEmpty()) {

            Solution currSol = unexploredNodes.poll();

            for (int stack = 0; stack < currSol.getFilledStacks().length; stack++) {

                if (HeuristicUtil.stackHasFreePosition(currSol.getFilledStacks()[stack])
                    && HeuristicUtil.itemCompatibleWithStack(this.costs, currSol.getAssignedItems().size(), stack)
                    && HeuristicUtil.itemCompatibleWithAlreadyAssignedItems(
                    currSol.getAssignedItems().size(), currSol.getFilledStacks()[stack], this.itemObjects, this.stackingConstraints
                )) {
                    Solution tmpSol = new Solution(currSol);
                    HeuristicUtil.assignItemToStack(tmpSol.getAssignedItems().size(), tmpSol.getFilledStacks()[stack], this.itemObjects);

                    if (tmpSol.getAssignedItems().size() == this.numberOfItems) {
                        if (tmpSol.computeCosts() < this.bestSol.computeCosts()) {
                            this.bestSol = tmpSol;
                        }
                    } else {
                        double LB = LowerBoundsUtil.computeLowerBound(tmpSol, this.costs, this.itemObjects, this.stackingConstraints, this.numberOfItems);
                        if (LB < this.bestSol.computeCosts()) {
                            unexploredNodes.add(new Solution(tmpSol));
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

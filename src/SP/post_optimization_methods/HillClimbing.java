package SP.post_optimization_methods;

import SP.experiments.PostOptimization;
import SP.post_optimization_methods.neighborhood_operators.EjectionChainOperator;
import SP.post_optimization_methods.neighborhood_operators.ShiftOperator;
import SP.post_optimization_methods.neighborhood_operators.SwapOperator;
import SP.representations.Solution;
import SP.util.HeuristicUtil;
import org.jgrapht.GraphPath;

import java.util.*;

/**
 * Hill climbing approach to solve stacking problems.
 */
public class HillClimbing implements LocalSearchAlgorithm {

    private int numberOfNeighbors;
    private PostOptimization.ShortTermStrategies shortTermStrategy;

    // nbh operators
    private EjectionChainOperator ejectionChainOperator;
    private ShiftOperator shiftOperator;
    private SwapOperator swapOperator;

    /**
     * Constructor
     *
     * @param numberOfNeighbors     - number of neighboring solutions to be generated
     * @param shortTermStrategy     - short term strategy to be used during the neighborhood search
     * @param ejectionChainOperator - ejection chain operator to be used in neighbor generation
     * @param shiftOperator         - shift operator to be used in neighbor generation
     * @param swapOperator          - swap operator to be used in neighbor generation
     */
    public HillClimbing(
        int numberOfNeighbors, PostOptimization.ShortTermStrategies shortTermStrategy,
        EjectionChainOperator ejectionChainOperator, ShiftOperator shiftOperator, SwapOperator swapOperator
    ) {
        this.numberOfNeighbors = numberOfNeighbors;
        this.shortTermStrategy = shortTermStrategy;
        this.ejectionChainOperator = ejectionChainOperator;
        this.shiftOperator = shiftOperator;
        this.swapOperator = swapOperator;
    }

    /**
     * Logs the current state to the console (dbg).
     *
     * @param currSol  - current solution (before applying the ejection chain)
     * @param bestPath - best ejection chain
     * @param tmpSol   - solution generating by applying the ejection chain
     */
    private void logCurrentState(Solution currSol, GraphPath bestPath, Solution tmpSol) {
        System.out.println("costs before: " + currSol.computeCosts());
        System.out.println("costs of best path: " + bestPath.getWeight());
        System.out.println("costs after ejection chain: " + tmpSol.computeCosts());
        System.out.println("feasible: " + tmpSol.isFeasible());
        System.exit(0);
    }

    /**
     * Applies a variable neighborhood consisting of the shift, swap and ejection chain neighborhood.
     *
     * @param currSol - solution to generate a neighbor for
     * @return first successfully generated neighbor
     */
    private Solution applyVariableNeighborhood(Solution currSol) {

        Solution neighbor = new Solution();

        // shift is only possible if there are free slots
        if (currSol.getNumberOfAssignedItems() < currSol.getFilledStacks().length * currSol.getFilledStacks()[0].length) {
            neighbor = this.shiftOperator.generateShiftNeighbor(currSol, new ArrayList<>());
        }
        if (!neighbor.isFeasible() || neighbor.computeCosts() > currSol.computeCosts()) {
            // next operator
            int rand = HeuristicUtil.getRandomIntegerInBetween(1, 4);
            neighbor = this.swapOperator.generateSwapNeighbor(currSol, new ArrayList<>(), rand);
            if (!neighbor.isFeasible() || neighbor.computeCosts() > currSol.computeCosts()) {
                // next operator
                neighbor = this.ejectionChainOperator.generateEjectionChainNeighbor(currSol, new ArrayList<>());
                System.out.println("USING EJECTION CHAIN OPERATOR");
            } else {
                System.out.println("USING SWAP OPERATOR");
            }
        } else {
            System.out.println("USING SHIFT OPERATOR");
        }
        return neighbor;
    }

    /**
     * Returns a neighboring solution for the current one.
     *
     * @param currSol - current solution to retrieve a neighbor for
     * @param bestSol - so far best solution
     * @return best generated neighbor in terms of costs
     */
    public Solution getNeighbor(Solution currSol, Solution bestSol) {

        List<Solution> nbrs = new ArrayList<>();

        while (nbrs.size() < this.numberOfNeighbors) {

            Solution neighbor = this.applyVariableNeighborhood(currSol);
            if (!neighbor.isFeasible()) { continue; }

            // FIRST-FIT
            if (this.shortTermStrategy == PostOptimization.ShortTermStrategies.FIRST_FIT
                && neighbor.computeCosts() < currSol.computeCosts()
            ) {
                return neighbor;
            // BEST-FIT
            } else {
                nbrs.add(neighbor);
            }
        }
        return HeuristicUtil.getBestSolution(nbrs);
    }
}

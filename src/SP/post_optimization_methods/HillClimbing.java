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
 *
 * @author Tim Bohne
 */
public class HillClimbing implements LocalSearchAlgorithm {

    private final int numberOfNeighbors;
    private final PostOptimization.ShortTermStrategies shortTermStrategy;
    private final int unsuccessfulNeighborGenerationAttempts;

    // nbh operators
    private final EjectionChainOperator ejectionChainOperator;
    private final ShiftOperator shiftOperator;
    private final SwapOperator swapOperator;

    // used to only apply shift and swap once since they are deterministic
    private boolean generatedShiftNbr;
    private boolean generatedSwapNbr;

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
        int unsuccessfulNeighborGenerationAttempts, EjectionChainOperator ejectionChainOperator,
        ShiftOperator shiftOperator, SwapOperator swapOperator
    ) {
        this.numberOfNeighbors = numberOfNeighbors;
        this.shortTermStrategy = shortTermStrategy;
        this.unsuccessfulNeighborGenerationAttempts = unsuccessfulNeighborGenerationAttempts;
        this.ejectionChainOperator = ejectionChainOperator;
        this.shiftOperator = shiftOperator;
        this.swapOperator = swapOperator;
        this.generatedShiftNbr = false;
        this.generatedSwapNbr = false;
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
        double currSolCosts = currSol.computeCosts();

        // shift is only possible if there are free slots
        if (!this.generatedShiftNbr && currSol.getNumberOfAssignedItems() < currSol.getFilledStacks().length * currSol.getFilledStacks()[0].length) {
            neighbor = this.shiftOperator.generateShiftNeighbor(currSol);
        }
        if (!neighbor.isFeasible() || neighbor.computeCosts() >= currSolCosts) {
            // next operator
            if (!this.generatedSwapNbr) {
                neighbor = this.swapOperator.generateSwapNeighbor(currSol);
            }
            if (!neighbor.isFeasible() || neighbor.computeCosts() >= currSolCosts) {
                // next operator
                neighbor = this.ejectionChainOperator.generateEjectionChainNeighbor(currSol);
//                System.out.println("USING EJECTION CHAIN OPERATOR: " + neighbor.computeCosts());
            } else {
//                System.out.println("USING SWAP OPERATOR: " + neighbor.computeCosts());
                this.generatedSwapNbr = true;
            }
        } else {
//            System.out.println("USING SHIFT OPERATOR: " + neighbor.computeCosts());
            this.generatedShiftNbr = true;
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
        int nbrGenerationFails = 0;
        double currSolCosts = currSol.computeCosts();

        this.generatedShiftNbr = false;
        this.generatedSwapNbr = false;

        while (nbrs.size() < this.numberOfNeighbors) {

            if (nbrGenerationFails == this.unsuccessfulNeighborGenerationAttempts) {
                Solution sol = HeuristicUtil.getBestSolution(nbrs);
                return sol.isEmpty() ? currSol : sol;
            }
            Solution neighbor = this.applyVariableNeighborhood(currSol);

            if (!neighbor.isFeasible() || neighbor.computeCosts() >= currSolCosts) {
                nbrGenerationFails++;
                continue;
            }
            nbrGenerationFails = 0;

            // FIRST-FIT
            if (this.shortTermStrategy == PostOptimization.ShortTermStrategies.FIRST_FIT) {
                return neighbor;
            // BEST-FIT
            } else {
                nbrs.add(neighbor);
            }
        }
        return HeuristicUtil.getBestSolution(nbrs);
    }
}

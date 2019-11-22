package SP.post_optimization_methods;

import SP.experiments.PostOptimization;
import SP.post_optimization_methods.neighborhood_operators.EjectionChainOperator;
import SP.post_optimization_methods.neighborhood_operators.ShiftOperator;
import SP.post_optimization_methods.neighborhood_operators.SwapOperator;
import SP.representations.Solution;
import SP.util.HeuristicUtil;
import org.jgrapht.GraphPath;

import java.util.*;

public class HillClimbing implements LocalSearchAlgorithm {

    private int numberOfNeighbors;
    private PostOptimization.ShortTermStrategies shortTermStrategy;

    // nbh operators
    private EjectionChainOperator ejectionChainOperator;
    private ShiftOperator shiftOperator;
    private SwapOperator swapOperator;

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

    private void logCurrentState(Solution currSol, GraphPath bestPath, Solution tmpSol) {
        System.out.println("costs before: " + currSol.computeCosts());
        System.out.println("costs of best path: " + bestPath.getWeight());
        System.out.println("costs after ejection chain: " + tmpSol.computeCosts());
        System.out.println("feasible: " + tmpSol.isFeasible());
        System.exit(0);
    }

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
                double start = System.currentTimeMillis();
                neighbor = this.ejectionChainOperator.generateEjectionChainNeighbor(currSol, new ArrayList<>());
//                System.out.println("runtime for ejection chain nbr generation: " + (System.currentTimeMillis() - start) / 1000.0);
                System.out.println("USING EJECTION CHAIN OPERATOR");
            } else {
                System.out.println("USING SWAP OPERATOR");
            }
        } else {
            System.out.println("USING SHIFT OPERATOR");
        }
        return neighbor;
    }

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

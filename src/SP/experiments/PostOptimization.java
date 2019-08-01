package SP.experiments;

import SP.io.SolutionReader;
import SP.io.SolutionWriter;
import SP.post_optimization_methods.TabuSearch;
import SP.representations.OptimizableSolution;
import SP.representations.Solution;
import SP.util.RepresentationUtil;

import java.util.List;

/**
 * Applies a configurable post optimization method to previously generated solutions for stacking problems.
 *
 * @author Tim Bohne
 */
public class PostOptimization {

    /**
     * Enumeration containing the different stopping criteria for the tabu search.
     */
    public enum StoppingCriteria {
        ITERATIONS,
        TABU_LIST_CLEARS,
        NON_IMPROVING_ITERATIONS
    }

    /**
     * Enumeration containing the different short term strategies for the tabu search.
     */
    public enum ShortTermStrategies {
        FIRST_FIT,
        BEST_FIT
    }

    private static final String INSTANCE_PREFIX = "res/instances/";
    private static final String SOLUTION_PREFIX = "res/solutions/";

    private static final ShortTermStrategies SHORT_TERM_STRATEGY = ShortTermStrategies.BEST_FIT;
    private static final StoppingCriteria STOPPING_CRITERION = StoppingCriteria.NON_IMPROVING_ITERATIONS;

    private static final int NUMBER_OF_ITERATIONS = 50;
    private static final int NUMBER_OF_TABU_LIST_CLEARS = 10;
    private static final int NUMBER_OF_NON_IMPROVING_ITERATIONS = 50;

    private static final int NUMBER_OF_NEIGHBORS = 1000;
    private static final int MAX_TABU_LIST_LENGTH_FACTOR = 9;
    private static final int UNSUCCESSFUL_NEIGHBOR_GENERATION_ATTEMPTS = 5000;
    private static final int UNSUCCESSFUL_K_SWAP_ATTEMPTS = 1000;
    private static final int K_SWAP_INTERVAL_UB = 4;

    private static final float K_SWAP_PROBABILITY = 5.0F;
    private static final float SWAP_PROBABILITY = 45.0F;

    // 3Cap solutions otherwise
    private static final boolean OPTIMIZE_TWO_CAP_SOLUTIONS = true;

    public static void main(String[] args) {
        optimizeSolutions();
    }

    /**
     * Optimizes the solutions in the specified directory using a tabu search.
     */
    private static void optimizeSolutions() {

        List<OptimizableSolution> solutions;
        if (OPTIMIZE_TWO_CAP_SOLUTIONS) {
            solutions = SolutionReader.readSolutionsFromDir(
                SOLUTION_PREFIX, INSTANCE_PREFIX, RepresentationUtil.getNameOfSolver(CompareSolvers.Solver.CONSTRUCTIVE_TWO_CAP)
            );
        } else {
            solutions = SolutionReader.readSolutionsFromDir(
                SOLUTION_PREFIX, INSTANCE_PREFIX, RepresentationUtil.getNameOfSolver(CompareSolvers.Solver.CONSTRUCTIVE_THREE_CAP)
            );
        }

        if (solutions.size() == 0) {
            System.out.println("No solution read. Either the specified directory doesn't "
                + "contain any solutions, or the specified heuristic doesn't match the stack capacity of the solutions.");
        }

        for (OptimizableSolution sol : solutions) {

            double startTime = System.currentTimeMillis();

            TabuSearch ts = new TabuSearch(
                sol.getSol(), sol.getOptimalObjectiveValue(), NUMBER_OF_NEIGHBORS, MAX_TABU_LIST_LENGTH_FACTOR,
                SHORT_TERM_STRATEGY, UNSUCCESSFUL_NEIGHBOR_GENERATION_ATTEMPTS, UNSUCCESSFUL_K_SWAP_ATTEMPTS,
                NUMBER_OF_NON_IMPROVING_ITERATIONS, K_SWAP_INTERVAL_UB, NUMBER_OF_ITERATIONS, NUMBER_OF_TABU_LIST_CLEARS,
                STOPPING_CRITERION, K_SWAP_PROBABILITY, SWAP_PROBABILITY
            );
            Solution impSol = ts.solve();
            impSol.setTimeToSolve((System.currentTimeMillis() - startTime) / 1000.0);

            SolutionWriter.writeSolution(
                SOLUTION_PREFIX
                + impSol.getNameOfSolvedInstance().replace("instances/", "")
                + "_imp.txt", impSol, RepresentationUtil.getNameOfSolver(CompareSolvers.Solver.TABU_SEARCH)
            );

            CompareSolvers.Solver solver;
            if (sol.getSol().getFilledStacks()[0].length == 2) {
                solver = CompareSolvers.Solver.CONSTRUCTIVE_TWO_CAP;
            } else {
                solver = CompareSolvers.Solver.CONSTRUCTIVE_THREE_CAP;
            }
            SolutionWriter.writeSolutionAsCSV(
                SOLUTION_PREFIX + "solutions_imp.csv", sol.getSol(), RepresentationUtil.getAbbreviatedNameOfSolver(solver)
            );
            SolutionWriter.writeOptAndImpAsCSV(SOLUTION_PREFIX + "solutions_imp.csv", sol, impSol);
        }
    }
}

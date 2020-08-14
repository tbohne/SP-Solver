package SP.experiments;

import SP.io.SolutionWriter;
import SP.post_optimization_methods.*;
import SP.post_optimization_methods.neighborhood_operators.EjectionChainOperator;
import SP.post_optimization_methods.neighborhood_operators.ShiftOperator;
import SP.post_optimization_methods.neighborhood_operators.SwapOperator;
import SP.representations.ImprovedSolution;
import SP.representations.Solution;
import SP.util.HeuristicUtil;
import SP.util.RepresentationUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Applies a configurable post optimization method to previously generated solutions for stacking problems.
 *
 * @author Tim Bohne
 */
public class PostOptimization {

    /**
     * Enumeration containing the different stopping criteria.
     */
    public enum StoppingCriteria {
        ITERATIONS,
        NON_IMPROVING_ITERATIONS
    }

    /**
     * Enumeration containing the different short term strategies.
     */
    public enum ShortTermStrategies {
        FIRST_FIT,
        BEST_FIT
    }

    /************************************* CONFIG *************************************/
    private static final String INSTANCE_PREFIX = "res/instances/";
    private static final String SOLUTION_PREFIX = "res/solutions/";

    // GENERAL
    private static final CompareSolvers.Solver SOLVER_OF_INITIAL_SOLUTION = CompareSolvers.Solver.GENERAL_HEURISTIC;
    private static final ShortTermStrategies SHORT_TERM_STRATEGY = ShortTermStrategies.BEST_FIT;
    private static final StoppingCriteria STOPPING_CRITERION = StoppingCriteria.NON_IMPROVING_ITERATIONS;
    private static final int NUMBER_OF_NEIGHBORS = 5;
    private static final int UNSUCCESSFUL_NEIGHBOR_GENERATION_ATTEMPTS = 2;

    // STOPPING SPECIFIC
    private static final int NUMBER_OF_ITERATIONS = 500;
    private static final int NUMBER_OF_NON_IMPROVING_ITERATIONS = 50;
    private static final double TIME_LIMIT = 3600;
    /**********************************************************************************/

    public static void optimizeSolutionWithHillClimbing(List<Solution> initialSolutions) {

        double startTime = System.currentTimeMillis();

        // operators to be used in the variable neighborhood
        EjectionChainOperator ejectionChainOperator = new EjectionChainOperator();
        ShiftOperator shiftOperator = new ShiftOperator(UNSUCCESSFUL_NEIGHBOR_GENERATION_ATTEMPTS);
        SwapOperator swapOperator = new SwapOperator(UNSUCCESSFUL_NEIGHBOR_GENERATION_ATTEMPTS);

        int cores = Runtime.getRuntime().availableProcessors();
        List<Thread> threads = new ArrayList<>();
        List<ImprovedSolution> impSolutions = new ArrayList<>();

        for (int thread = 0; thread < cores; thread++) {
            LocalSearchAlgorithm hillClimbing = new HillClimbing(
                NUMBER_OF_NEIGHBORS, SHORT_TERM_STRATEGY, UNSUCCESSFUL_NEIGHBOR_GENERATION_ATTEMPTS,
                ejectionChainOperator, shiftOperator, swapOperator
            );
            impSolutions.add(new ImprovedSolution());
            threads.add(new Thread(new LocalSearch(
                initialSolutions.get(thread), TIME_LIMIT, Double.MIN_VALUE, NUMBER_OF_NON_IMPROVING_ITERATIONS,
                NUMBER_OF_ITERATIONS, STOPPING_CRITERION, hillClimbing, impSolutions.get(thread)
            )));
        }

        try {
            for (Thread t : threads) {
                t.start();
            }
            for (Thread t : threads) {
                t.join();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        ImprovedSolution impSol = HeuristicUtil.getBestImpSolution(impSolutions);
        Solution bestSol = impSol.getSol();
        bestSol.setTimeToSolve((System.currentTimeMillis() - startTime) / 1000.0);

        SolutionWriter.writeSolution(
            SOLUTION_PREFIX
            + bestSol.getNameOfSolvedInstance().replace("instances/", "")
            + "_imp.txt", bestSol, RepresentationUtil.getNameOfSolver(CompareSolvers.Solver.HILL_CLIMBING)
        );
        SolutionWriter.writeImpAsCSV(
            SOLUTION_PREFIX + "solutions.csv", bestSol,
            RepresentationUtil.getAbbreviatedNameOfSolver(CompareSolvers.Solver.HILL_CLIMBING), initialSolutions.get(0),
            impSol.getTimeToBestSolution(), impSol.getIterationOfLastImprovement(), impSol.getNumberOfPerformedIterations()
        );
        SolutionWriter.writePostOptimizationConfig(
            SOLUTION_PREFIX + "post_optimization_config.csv",
            SOLVER_OF_INITIAL_SOLUTION, SHORT_TERM_STRATEGY, STOPPING_CRITERION, NUMBER_OF_NEIGHBORS,
            UNSUCCESSFUL_NEIGHBOR_GENERATION_ATTEMPTS, NUMBER_OF_ITERATIONS, NUMBER_OF_NON_IMPROVING_ITERATIONS
        );
    }
}

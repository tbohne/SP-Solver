package SP.experiments;

import SP.io.SolutionReader;
import SP.io.SolutionWriter;
import SP.post_optimization_methods.*;
import SP.post_optimization_methods.neighborhood_operators.EjectionChainOperator;
import SP.post_optimization_methods.neighborhood_operators.ShiftOperator;
import SP.post_optimization_methods.neighborhood_operators.SwapOperator;
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

    /************************************* CONFIG *************************************/
    private static final String INSTANCE_PREFIX = "res/instances/";
    private static final String SOLUTION_PREFIX = "res/solutions/";

    // GENERAL
    private static final CompareSolvers.Solver SOLVER_OF_INITIAL_SOLUTION = CompareSolvers.Solver.GENERAL_HEURISTIC;
    private static final ShortTermStrategies SHORT_TERM_STRATEGY = ShortTermStrategies.BEST_FIT;
    private static final StoppingCriteria STOPPING_CRITERION = StoppingCriteria.NON_IMPROVING_ITERATIONS;
    private static final int NUMBER_OF_NEIGHBORS = 1;
    private static final int MAX_TABU_LIST_LENGTH_FACTOR = 1000;
    private static final int UNSUCCESSFUL_NEIGHBOR_GENERATION_ATTEMPTS = 300;

    // STOPPING SPECIFIC
    private static final int NUMBER_OF_ITERATIONS = 500;
    private static final int NUMBER_OF_TABU_LIST_CLEARS = 10;
    private static final int NUMBER_OF_NON_IMPROVING_ITERATIONS = 500;

    // maximum number of swaps to be performed in a single application of the swap operator
    private static final int MAX_NUMBER_OF_SWAPS = 4;
    /**********************************************************************************/

    public static void main(String[] args) {
        optimizeSolutions();
    }

    public static void optimizeSolutionWithTabuSearch(Solution sol) {

        EjectionChainOperator ejectionChainOperator = new EjectionChainOperator();
        ShiftOperator shiftOperator = new ShiftOperator(UNSUCCESSFUL_NEIGHBOR_GENERATION_ATTEMPTS);
        SwapOperator swapOperator = new SwapOperator(MAX_NUMBER_OF_SWAPS, UNSUCCESSFUL_NEIGHBOR_GENERATION_ATTEMPTS);

        LocalSearchAlgorithm tabuSearch = new TabuSearch(
            NUMBER_OF_NEIGHBORS, SHORT_TERM_STRATEGY,
            NUMBER_OF_NEIGHBORS * MAX_TABU_LIST_LENGTH_FACTOR,
            UNSUCCESSFUL_NEIGHBOR_GENERATION_ATTEMPTS, ejectionChainOperator, shiftOperator, swapOperator
        );

        LocalSearch localSearch = new LocalSearch(
            sol, 0, Double.MIN_VALUE,
            NUMBER_OF_NON_IMPROVING_ITERATIONS, NUMBER_OF_ITERATIONS, STOPPING_CRITERION, tabuSearch
        );

        double startTime = System.currentTimeMillis();
        Solution impSol = localSearch.solve();
        impSol.setTimeToSolve((System.currentTimeMillis() - startTime) / 1000.0);

        SolutionWriter.writeSolution(
            SOLUTION_PREFIX
            + impSol.getNameOfSolvedInstance().replace("instances/", "")
            + "_imp.txt", impSol, RepresentationUtil.getNameOfSolver(CompareSolvers.Solver.TABU_SEARCH)
        );

        SolutionWriter.writeImpAsCSV(
            SOLUTION_PREFIX + "solutions.csv", impSol, RepresentationUtil.getAbbreviatedNameOfSolver(CompareSolvers.Solver.TABU_SEARCH)
        );
    }

    public static void optimizeSolutionWithHillClimbing(Solution sol) {

        // operators to be used in the variable neighborhood
        EjectionChainOperator ejectionChainOperator = new EjectionChainOperator();
        ShiftOperator shiftOperator = new ShiftOperator(UNSUCCESSFUL_NEIGHBOR_GENERATION_ATTEMPTS);
        SwapOperator swapOperator = new SwapOperator(MAX_NUMBER_OF_SWAPS, UNSUCCESSFUL_NEIGHBOR_GENERATION_ATTEMPTS);

        LocalSearchAlgorithm hillClimbing = new HillClimbing(
            NUMBER_OF_NEIGHBORS, SHORT_TERM_STRATEGY, ejectionChainOperator, shiftOperator, swapOperator
        );

        LocalSearch localSearch = new LocalSearch(
            sol, 0, Double.MIN_VALUE,
            NUMBER_OF_NON_IMPROVING_ITERATIONS, NUMBER_OF_ITERATIONS, STOPPING_CRITERION, hillClimbing
        );

        double startTime = System.currentTimeMillis();
        Solution impSol = localSearch.solve();
        impSol.setTimeToSolve((System.currentTimeMillis() - startTime) / 1000.0);

        SolutionWriter.writeSolution(
            SOLUTION_PREFIX
            + impSol.getNameOfSolvedInstance().replace("instances/", "")
            + "_imp.txt", impSol, RepresentationUtil.getNameOfSolver(CompareSolvers.Solver.HILL_CLIMBING)
        );

        SolutionWriter.writeImpAsCSV(
            SOLUTION_PREFIX + "solutions.csv", impSol, RepresentationUtil.getAbbreviatedNameOfSolver(CompareSolvers.Solver.HILL_CLIMBING)
        );

        SolutionWriter.writePostOptimizationConfig(
            SOLUTION_PREFIX + "post_optimization_config.csv",
            SOLVER_OF_INITIAL_SOLUTION, SHORT_TERM_STRATEGY, STOPPING_CRITERION, NUMBER_OF_NEIGHBORS,
            MAX_TABU_LIST_LENGTH_FACTOR, UNSUCCESSFUL_NEIGHBOR_GENERATION_ATTEMPTS, NUMBER_OF_ITERATIONS,
            NUMBER_OF_TABU_LIST_CLEARS, NUMBER_OF_NON_IMPROVING_ITERATIONS, MAX_NUMBER_OF_SWAPS
        );
    }

    /**
     * Optimizes the solutions in the specified directory using a tabu search.
     */
    private static void optimizeSolutions() {

        List<OptimizableSolution> solutions;
        solutions = SolutionReader.readSolutionsFromDir(
            SOLUTION_PREFIX, INSTANCE_PREFIX, RepresentationUtil.getNameOfSolver(SOLVER_OF_INITIAL_SOLUTION)
        );
        if (solutions.size() == 0) {
            System.out.println("No solution read. Either the specified directory doesn't "
                + "contain any solutions, or the specified heuristic doesn't match the stack capacity of the solutions.");
        }

        for (OptimizableSolution sol : solutions) {

            double startTime = System.currentTimeMillis();

            // operators to be used in the variable neighborhood
            EjectionChainOperator ejectionChainOperator = new EjectionChainOperator();
            ShiftOperator shiftOperator = new ShiftOperator(UNSUCCESSFUL_NEIGHBOR_GENERATION_ATTEMPTS);
            SwapOperator swapOperator = new SwapOperator(MAX_NUMBER_OF_SWAPS, UNSUCCESSFUL_NEIGHBOR_GENERATION_ATTEMPTS);

            LocalSearchAlgorithm tabuSearch = new TabuSearch(
                NUMBER_OF_NEIGHBORS, SHORT_TERM_STRATEGY,
                NUMBER_OF_NEIGHBORS * MAX_TABU_LIST_LENGTH_FACTOR,
                UNSUCCESSFUL_NEIGHBOR_GENERATION_ATTEMPTS, ejectionChainOperator, shiftOperator, swapOperator
            );

            LocalSearchAlgorithm hillClimbing = new HillClimbing(
                NUMBER_OF_NEIGHBORS, SHORT_TERM_STRATEGY, ejectionChainOperator, shiftOperator, swapOperator
            );

            LocalSearch ts = new LocalSearch(
                sol.getSol(), 0, sol.getOptimalObjectiveValue(),
                NUMBER_OF_NON_IMPROVING_ITERATIONS, NUMBER_OF_ITERATIONS, STOPPING_CRITERION, hillClimbing
            );

            Solution impSol = ts.solve();
            impSol.setTimeToSolve((System.currentTimeMillis() - startTime) / 1000.0);

            SolutionWriter.writeSolution(
                SOLUTION_PREFIX
                + impSol.getNameOfSolvedInstance().replace("instances/", "")
                + "_imp.txt", impSol, RepresentationUtil.getNameOfSolver(CompareSolvers.Solver.TABU_SEARCH)
            );

            SolutionWriter.writeSolutionAsCSV(
                SOLUTION_PREFIX + "solutions_imp.csv", sol.getSol(),
                RepresentationUtil.getAbbreviatedNameOfSolver(SOLVER_OF_INITIAL_SOLUTION)
            );
            SolutionWriter.writeOptAndImpAsCSV(
                SOLUTION_PREFIX + "solutions_imp.csv", sol, impSol, RepresentationUtil.getAbbreviatedNameOfSolver(CompareSolvers.Solver.TABU_SEARCH)
            );

            SolutionWriter.writePostOptimizationConfig(
                SOLUTION_PREFIX + "post_optimization_config.csv",
                SOLVER_OF_INITIAL_SOLUTION, SHORT_TERM_STRATEGY, STOPPING_CRITERION, NUMBER_OF_NEIGHBORS,
                MAX_TABU_LIST_LENGTH_FACTOR, UNSUCCESSFUL_NEIGHBOR_GENERATION_ATTEMPTS, NUMBER_OF_ITERATIONS,
                NUMBER_OF_TABU_LIST_CLEARS, NUMBER_OF_NON_IMPROVING_ITERATIONS, MAX_NUMBER_OF_SWAPS
            );
        }
    }
}

package SP.experiments;

import com.google.common.collect.Lists;

import java.util.List;

/**
 * Used to configure / start the solver comparison.
 *
 * @author Tim Bohne
 */
public class CompareSolvers {

    /**
     * Enumeration containing the names of the different solvers.
     */
    public enum Solver {
        MIP_BINPACKING,
        MIP_THREEINDEX,
        CONSTRUCTIVE_TWO_CAP,
        CONSTRUCTIVE_THREE_CAP,
        TABU_SEARCH,
        GENERAL_HEURISTIC,
        HILL_CLIMBING
    }

    private static final String INSTANCE_PREFIX = "res/instances/";
    private static final String SOLUTION_PREFIX = "res/solutions/";

    // solvers to be used
    private static final List<Solver> SOLVERS = Lists.newArrayList(
        Solver.MIP_BINPACKING,
        Solver.MIP_THREEINDEX,
//        Solver.CONSTRUCTIVE_TWO_CAP
//        Solver.CONSTRUCTIVE_THREE_CAP
        Solver.GENERAL_HEURISTIC,
        Solver.HILL_CLIMBING,
        Solver.TABU_SEARCH
    );

    /********************** CPLEX CONFIG **********************/
    private static final boolean HIDE_CPLEX_OUTPUT = true;
    // 1 --> feasibility over optimality
    private static final int MIP_EMPHASIS = 1;
    // 0 --> only terminating with optimal solutions before time limit
    private static final double MIP_TOLERANCE = 0.0;
    /**********************************************************/

    // specifies the time limit for the solving procedure in seconds
    private static final double TIME_LIMIT = 1800;

    // 2Cap and 3Cap provide post processing procedures that can be enabled here
    private static final boolean POST_PROCESSING = true;

    /********************** 3CAP CONFIG **********************/
    // 3Cap has an option to prioritize the runtime which can be set here
    private static final boolean PRIORITIZE_RUNTIME = false;
    // rating system config
    private static final int THRESHOLD_LB = 20;
    private static final int THRESHOLD_UB = 75;
    private static final int STEP_SIZE = 5;
    private static final float SPLIT_PAIRS_DIVISOR = 3.4F;
    private static final float PENALTY_FACTOR = 5.0F;
    /*********************************************************/

    // determines whether only b=2 or b=3 instances are considered (general b otherwise)
    private static final boolean SOLVE_INSTANCES_WITH_SPECIFIC_STACK_CAPACITY = false;
    // in case of specific capacity: determines whether only b=2 instances are considered (b=3 otherwise)
    private static final boolean SOLVE_TWO_CAP = false;

    public static void main(String[] args) {

        if (SOLVE_INSTANCES_WITH_SPECIFIC_STACK_CAPACITY) {

            // solving instances with capacity 2
            if (SOLVE_TWO_CAP) {
                SolverComparison2Cap twoCap = new SolverComparison2Cap(
                    SOLUTION_PREFIX, INSTANCE_PREFIX, TIME_LIMIT, HIDE_CPLEX_OUTPUT,
                    MIP_EMPHASIS, MIP_TOLERANCE, POST_PROCESSING
                );
                twoCap.compareSolvers(SOLVERS);

            // solving instances with capacity 3
            } else {
                SolverComparison3Cap threeCap = new SolverComparison3Cap(
                    SOLUTION_PREFIX, INSTANCE_PREFIX, TIME_LIMIT, HIDE_CPLEX_OUTPUT,
                    MIP_EMPHASIS, MIP_TOLERANCE, POST_PROCESSING, PRIORITIZE_RUNTIME,
                    THRESHOLD_LB, THRESHOLD_UB, STEP_SIZE, SPLIT_PAIRS_DIVISOR, PENALTY_FACTOR
                );
                threeCap.compareSolvers(SOLVERS);
            }

        // only using solvers for general b
        } else {

            SolverComparison solverComp = new SolverComparison(
                SOLUTION_PREFIX, INSTANCE_PREFIX, TIME_LIMIT, HIDE_CPLEX_OUTPUT, MIP_EMPHASIS, MIP_TOLERANCE
            );
            solverComp.compareSolvers(SOLVERS);

        }
    }
}

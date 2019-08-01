package SP.representations;

/**
 * Represents a solutions that is going to be optimized by a post optimization method.
 *
 * @author Tim Bohne
 */
public class OptimizableSolution {

    private final Solution sol;
    private final double optimalObjectiveValue;
    private final double runtimeForOptimalSolution;

    /**
     * Constructor
     *
     * @param sol                       - solution to be optimized
     * @param optimalObjectiveValue     - optimal objective value for the solved instance
     * @param runtimeForOptimalSolution - runtime that was needed to generate the optimal solution with CPLEX
     */
    public OptimizableSolution(Solution sol, double optimalObjectiveValue, double runtimeForOptimalSolution) {
        this.sol = sol;
        this.optimalObjectiveValue = optimalObjectiveValue;
        this.runtimeForOptimalSolution = runtimeForOptimalSolution;
    }

    /**
     * Returns the solution to be optimized.
     *
     * @return solution to be optimized
     */
    public Solution getSol() {
        return this.sol;
    }

    /**
     * Returns the optimal objective value for the solved instance.
     *
     * @return optimal objective value
     */
    public double getOptimalObjectiveValue() {
        return this.optimalObjectiveValue;
    }

    /**
     * Returns the runtime that was needed to generate the optimal solution with CPLEX.
     *
     * @return runtime for optimal solution
     */
    public double getRuntimeForOptimalSolution() {
        return this.runtimeForOptimalSolution;
    }
}

package SP.representations;

/**
 * Represents the result of a post optimization technique.
 */
public class ImprovedSolution {

    private Solution sol;
    private double timeToBestSolution;
    private int performedIterations;
    private int iterationOfLastImprovement;

    public ImprovedSolution() {
        this.sol = null;
        this.timeToBestSolution = 0;
        this.performedIterations = 0;
        this.iterationOfLastImprovement = 0;
    }

    /**
     * Constructor
     *
     * @param sol                        - improved solution (result of post optimization)
     * @param timeToBest                 - runtime to find the best solution
     * @param performedIterations        - total number of performed iterations
     * @param iterationOfLastImprovement - number of iterations to find the best solution
     */
    public ImprovedSolution(Solution sol, double timeToBest, int performedIterations, int iterationOfLastImprovement) {
        this.sol = sol;
        this.timeToBestSolution = timeToBest;
        this.performedIterations = performedIterations;
        this.iterationOfLastImprovement = iterationOfLastImprovement;
    }

    /**
     * Returns the improved solution.
     *
     * @return improved solution
     */
    public Solution getSol() {
        return this.sol;
    }

    /**
     * Returns the runtime to find the best solution.
     *
     * @return runtime to best solution
     */
    public double getTimeToBestSolution() {
        return this.timeToBestSolution;
    }

    /**
     * Returns the total number of performed iterations.
     *
     * @return total number of performed iterations
     */
    public int getNumberOfPerformedIterations() {
        return this.performedIterations;
    }

    /**
     * Returns the number of iterations to find the best solution.
     *
     * @return number of iteration to best solution
     */
    public int getIterationOfLastImprovement() {
        return this.iterationOfLastImprovement;
    }

    public void setSol(Solution sol) {
        this.sol = sol;
    }

    public void setTimeToBestSolution(double timeToBestSolution) {
        this.timeToBestSolution = timeToBestSolution;
    }

    public void setPerformedIterations(int performedIterations) {
        this.performedIterations = performedIterations;
    }

    public void setIterationOfLastImprovement(int iterationOfLastImprovement) {
        this.iterationOfLastImprovement = iterationOfLastImprovement;
    }
}

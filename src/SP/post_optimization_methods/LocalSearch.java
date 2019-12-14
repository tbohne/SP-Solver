package SP.post_optimization_methods;

import SP.experiments.PostOptimization;
import SP.representations.PartitionList;
import SP.representations.Solution;

/**
 * Improvement heuristic that starts with an initial solution of a stacking-problem
 * and tries to improve its quality in terms of cost minimization by performing a local search.
 *
 * @author Tim Bohne
 */
public class LocalSearch {

    private Solution currSol;
    private Solution bestSol;
    private double timeLimit;
    private double startTime;
    private final double optimalObjectiveValue;

    private final PostOptimization.StoppingCriteria stoppingCriterion;
    private final LocalSearchAlgorithm localSearchAlgorithm;

    private int iterationOfLastImprovement;
    private double timeToBestSolution;
    private int totalNumOfPerformedIterations;
    private final int numberOfNonImprovingIterations;
    private final int numberOfIterations;

    /**
     * Constructor
     *
     * @param initialSolution                - initial solution to be improved
     * @param timeLimit                      - time limit for the improvement procedure
     * @param optimalObjectiveValue          - optimal objective value for the solution (generated by CPLEX)
     * @param numberOfNonImprovingIterations - number of non improving iterations before termination
     * @param numberOfIterations             - number of iterations before termination
     * @param stoppingCriterion              - stopping criterion to be used
     * @param localSearchAlgorithm           - neighborhood structure to be used in the local search
     */
    public LocalSearch(
        Solution initialSolution, double timeLimit, double optimalObjectiveValue,
        int numberOfNonImprovingIterations, int numberOfIterations,
        PostOptimization.StoppingCriteria stoppingCriterion, LocalSearchAlgorithm localSearchAlgorithm
    ) {
        this.currSol = new Solution(initialSolution);
        this.bestSol = new Solution(initialSolution);
        this.iterationOfLastImprovement = 0;
        this.stoppingCriterion = stoppingCriterion;
        this.numberOfNonImprovingIterations = numberOfNonImprovingIterations;
        this.numberOfIterations = numberOfIterations;
        this.startTime = System.currentTimeMillis();
        this.optimalObjectiveValue = optimalObjectiveValue;
        this.timeLimit = timeLimit;
        this.localSearchAlgorithm = localSearchAlgorithm;
        this.timeToBestSolution = 0.0;
        this.totalNumOfPerformedIterations = 0;
    }

    public double getTimeToBestSolution() {
        return this.timeToBestSolution;
    }

    public int getIterationsToBestSolution() {
        return this.iterationOfLastImprovement;
    }

    public int getTotalNumOfPerformedIterations() {
        return totalNumOfPerformedIterations;
    }

    /**
     * Ignores the best solution's current stack assignments and generates the cheapest stack assignments
     * for its given partitions (item tuples) by computing a min-cost-perfect-matching.
     */
    private void computeBestAssignmentForCurrentPartitions() {
        PartitionList partitionSol = new PartitionList(this.bestSol);
        System.out.println("best sol before: " + this.bestSol.computeCosts() + " feasible? " + this.bestSol.isFeasible());
        this.bestSol = partitionSol.generateSolutionFromPartitions();
        System.out.println("best sol after: " + this.bestSol.computeCosts() + " feasible? " + this.bestSol.isFeasible());
    }

    /**
     * Improves a given solution to a stacking problem using a local search.
     *
     * @return best solution generated in the local search procedure
     */
    public Solution solve() {

        this.startTime = System.currentTimeMillis();
        this.computeBestAssignmentForCurrentPartitions();

        switch (this.stoppingCriterion) {
            case ITERATIONS:
                this.solveIterations(this.localSearchAlgorithm);
                break;
            case NON_IMPROVING_ITERATIONS:
                this.solveIterationsSinceLastImprovement(this.localSearchAlgorithm);
                break;
            default:
                this.solveIterationsSinceLastImprovement(this.localSearchAlgorithm);
        }

        this.computeBestAssignmentForCurrentPartitions();
        return this.bestSol;
    }

    /**
     * Retrieves a neighboring solution by applying the operators of the specified neighborhood.
     *
     * @param localSearchAlgorithm - neighborhood structure used to generate neighboring solutions
     * @return neighboring solution
     */
    private Solution getNeighbor(LocalSearchAlgorithm localSearchAlgorithm) {
        Solution sol = localSearchAlgorithm.getNeighbor(this.currSol, this.bestSol);
        return sol == null ? this.currSol : sol;
    }

    /**
     * Updates the current solution with the best neighbor.
     * Additionally, the best solution gets updated if a new best solution is found.
     *
     * @param iteration            - current iteration
     * @param localSearchAlgorithm - neighborhood structure used to generate neighboring solutions
     */
    private void updateCurrentSolution(int iteration, LocalSearchAlgorithm localSearchAlgorithm) {
        this.currSol = this.getNeighbor(localSearchAlgorithm);
        if (this.currSol.computeCosts() < this.bestSol.computeCosts()) {
            this.bestSol = this.currSol;
            this.iterationOfLastImprovement = iteration;
            this.timeToBestSolution = (System.currentTimeMillis() - this.startTime) / 1000;
        }
    }

    /**
     * Performs the local search with a number of iterations as stop criterion.
     *
     * @param localSearchAlgorithm - neighborhood structure used to generate neighboring solutions
     */
    private void solveIterations(LocalSearchAlgorithm localSearchAlgorithm) {
        for (int i = 0; i < this.numberOfIterations; i++) {
            if (this.timeLimit != 0 && (System.currentTimeMillis() - this.startTime) / 1000 > this.timeLimit) { break; }
            if (this.bestSol.computeCosts() == this.optimalObjectiveValue) { break; }
            this.updateCurrentSolution(i, localSearchAlgorithm);
        }
    }

    /**
     * Performs the local search with a number of non-improving iterations as stop criterion.
     *
     * @param localSearchAlgorithm - neighborhood structure used to generate neighboring solutions
     */
    private void solveIterationsSinceLastImprovement(LocalSearchAlgorithm localSearchAlgorithm) {
        this.totalNumOfPerformedIterations = 0;
        while (Math.abs(this.iterationOfLastImprovement - this.totalNumOfPerformedIterations) < this.numberOfNonImprovingIterations) {
            System.out.println("non improving iterations: " + Math.abs(this.iterationOfLastImprovement - this.totalNumOfPerformedIterations));
            if (this.timeLimit != 0 && (System.currentTimeMillis() - this.startTime) / 1000 > this.timeLimit) { break; }
            if (this.bestSol.computeCosts() == this.optimalObjectiveValue) { break; }
            this.updateCurrentSolution(this.totalNumOfPerformedIterations++, localSearchAlgorithm);
        }
    }
}

package SP.post_optimization_methods;

import SP.experiments.PostOptimization;
import SP.representations.PartitionList;
import SP.representations.Solution;

/**
 * Improvement heuristic that starts with an initial solution of a stacking-problem
 * and tries to improve its quality in terms of cost minimization by performing a tabu-search.
 *
 * @author Tim Bohne
 */
public class TabuSearch {

    private Solution currSol;
    private Solution bestSol;
    private double timeLimit;
    private double startTime;
    private final double optimalObjectiveValue;

    private final PostOptimization.StoppingCriteria stoppingCriterion;
    private final TabuSearchNeighborhood neighborhood;

    private int iterationOfLastImprovement;
    private final int numberOfNonImprovingIterations;
    private final int numberOfIterations;
    private final int numberOfTabuListClears;

    /**
     * Constructor
     *
     * @param initialSolution                - initial solution to be improved
     * @param timeLimit                      - time limit for the improvement procedure
     * @param optimalObjectiveValue          - optimal objective value for the solution (generated by CPLEX)
     * @param numberOfNonImprovingIterations - number of non improving iterations before termination
     * @param numberOfIterations             - number of iterations before termination
     * @param numberOfTabuListClears         - number of tabu list clears before termination
     * @param stoppingCriterion              - stopping criterion to be used
     * @param neighborhood                   - neighborhood structure to be used in the local search
     */
    public TabuSearch(
        Solution initialSolution, double timeLimit, double optimalObjectiveValue,
        int numberOfNonImprovingIterations, int numberOfIterations, int numberOfTabuListClears,
        PostOptimization.StoppingCriteria stoppingCriterion, TabuSearchNeighborhood neighborhood
    ) {
        this.currSol = new Solution(initialSolution);
        this.bestSol = new Solution(initialSolution);
        this.iterationOfLastImprovement = 0;
        this.stoppingCriterion = stoppingCriterion;
        this.numberOfNonImprovingIterations = numberOfNonImprovingIterations;
        this.numberOfIterations = numberOfIterations;
        this.numberOfTabuListClears = numberOfTabuListClears;
        this.startTime = System.currentTimeMillis();
        this.timeLimit = 0;
        this.optimalObjectiveValue = optimalObjectiveValue;
        this.timeLimit = timeLimit;
        this.neighborhood = neighborhood;
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
     * Improves a given solution to a stacking problem using a tabu search.
     *
     * @return best solution generated in the tabu search procedure
     */
    public Solution solve() {

        this.startTime = System.currentTimeMillis();
        this.computeBestAssignmentForCurrentPartitions();

        switch (this.stoppingCriterion) {
            case ITERATIONS:
                this.solveIterations(this.neighborhood);
                break;
            case TABU_LIST_CLEARS:
                this.solveTabuListClears(this.neighborhood);
                break;
            case NON_IMPROVING_ITERATIONS:
                this.solveIterationsSinceLastImprovement(this.neighborhood);
                break;
            default:
                this.solveIterationsSinceLastImprovement(this.neighborhood);
        }

        this.computeBestAssignmentForCurrentPartitions();
        return this.bestSol;
    }

    /**
     * Retrieves a neighboring solution by applying the operators of the specified neighborhood.
     *
     * @param neighborhood - neighborhood structure used to generate neighboring solutions
     * @return neighboring solution
     */
    private Solution getNeighbor(TabuSearchNeighborhood neighborhood) {
        Solution sol = neighborhood.getNeighbor(this.currSol, this.bestSol);
        return sol == null ? this.currSol : sol;
    }

    /**
     * Updates the current solution with the best neighbor.
     * Additionally, the best solution gets updated if a new best solution is found.
     *
     * @param iteration    - current iteration
     * @param neighborhood - neighborhood structure used to generate neighboring solutions
     */
    private void updateCurrentSolution(int iteration, TabuSearchNeighborhood neighborhood) {
        this.currSol = this.getNeighbor(neighborhood);
        if (this.currSol.computeCosts() < this.bestSol.computeCosts()) {
            this.bestSol = this.currSol;
            this.iterationOfLastImprovement = iteration;
        }
    }

    /**
     * Performs the tabu search with a number of iterations as stop criterion.
     *
     * @param neighborhood - neighborhood structure used to generate neighboring solutions
     */
    private void solveIterations(TabuSearchNeighborhood neighborhood) {
        for (int i = 0; i < this.numberOfIterations; i++) {
            if (this.timeLimit != 0 && (System.currentTimeMillis() - this.startTime) / 1000 > this.timeLimit) { break; }
            if (this.bestSol.computeCosts() == this.optimalObjectiveValue) { break; }
            this.updateCurrentSolution(i, neighborhood);
        }
    }

    /**
     * Performs the tabu search with a number of tabu list clears as stop criterion.
     *
     * @param neighborhood - neighborhood structure used to generate neighboring solutions
     */
    private void solveTabuListClears(TabuSearchNeighborhood neighborhood) {
        int iteration = 0;
        while (neighborhood.getTabuListClears() < this.numberOfTabuListClears) {
            if (this.timeLimit != 0 && (System.currentTimeMillis() - this.startTime) / 1000 > this.timeLimit) { break; }
            if (this.bestSol.computeCosts() == this.optimalObjectiveValue) { break; }
            this.updateCurrentSolution(iteration++, neighborhood);
        }
    }

    /**
     * Performs the tabu search with a number of non-improving iterations as stop criterion.
     *
     * @param neighborhood - neighborhood structure used to generate neighboring solutions
     */
    private void solveIterationsSinceLastImprovement(TabuSearchNeighborhood neighborhood) {
        int iteration = 0;
        while (Math.abs(this.iterationOfLastImprovement - iteration) < this.numberOfNonImprovingIterations) {
//            System.out.println("non improving iterations: " + Math.abs(this.iterationOfLastImprovement - iteration));
            if (this.timeLimit != 0 && (System.currentTimeMillis() - this.startTime) / 1000 > this.timeLimit) { break; }
            if (this.bestSol.computeCosts() == this.optimalObjectiveValue) { break; }
            this.updateCurrentSolution(iteration++, neighborhood);
        }
    }
}

package SP.post_optimization_methods;

import SP.representations.Solution;

/**
 * Interface to be implemented by different local search algorithms.
 *
 * @author Tim Bohne
 */
public interface LocalSearchAlgorithm {

    /**
     * Returns a neighboring solution based on the neighborhood structure used in the local search algorithm.
     *
     * @param currSol - current solution to retrieve a neighbor for
     * @param bestSol - so far best solution
     * @return neighboring solution
     */
    Solution getNeighbor(Solution currSol, Solution bestSol);
}

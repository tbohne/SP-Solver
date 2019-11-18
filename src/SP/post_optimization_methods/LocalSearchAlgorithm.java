package SP.post_optimization_methods;

import SP.representations.Solution;

/**
 * Interface to be implemented by different neighborhood structures used in the local search.
 *
 * @author Tim Bohne
 */
public interface Neighborhood {

    /**
     * Returns a neighboring solution based on the neighborhood structure.
     *
     * @param currSol - current solution to retrieve a neighbor for
     * @param bestSol - so far best solution
     * @return neighboring solution
     */
    Solution getNeighbor(Solution currSol, Solution bestSol);
}

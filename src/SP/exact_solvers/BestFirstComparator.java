package SP.exact_solvers;

import SP.representations.Solution;
import SP.util.LowerBoundsUtil;

import java.util.Comparator;

/**
 * Comparator to be used in the branch-and-bound procedure (best-first approach).
 *
 * @author Tim Bohne
 */
public class BestFirstComparator implements Comparator<Solution> {

    /**
     * Compares the given solutions in terms of their lower bounds.
     *
     * @param solOne - first solution to be compared
     * @param solTwo - second solution to be compared
     * @return a negative integer, zero, or a positive integer as the first
     *         solution is less than, equal to, or greater than the second
     */
    @Override
    public int compare(Solution solOne, Solution solTwo) {

        double lowerBoundSolOne = LowerBoundsUtil.computeLowerBound(solOne).computeCosts();
        double lowerBoundSolTwo = LowerBoundsUtil.computeLowerBound(solTwo).computeCosts();

        if (lowerBoundSolOne < lowerBoundSolTwo) {
            return -1;
        } else if (lowerBoundSolTwo < lowerBoundSolOne) {
            return 1;
        }
        return 0;
    }
}

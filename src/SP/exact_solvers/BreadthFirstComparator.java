package SP.exact_solvers;

import SP.representations.Solution;

import java.util.Comparator;

/**
 * Comparator to be used in the branch-and-bound procedure (BFS approach).
 *
 * @author Tim Bohne
 */
public class BreadthFirstComparator implements Comparator<Solution> {

    /**
     * Compares the given solutions in terms of their number of assigned items.
     *
     * @param solOne - first solution to be compared
     * @param solTwo - second solution to be compared
     * @return a negative integer, zero, or a positive integer as the first
     *         solution is less than, equal to, or greater than the second
     */
    @Override
    public int compare(Solution solOne, Solution solTwo) {
        if (solOne.getNumberOfAssignedItems() < solTwo.getNumberOfAssignedItems()) {
            return -1;
        } else if (solTwo.getNumberOfAssignedItems() < solOne.getNumberOfAssignedItems()) {
            return 1;
        }
        return 0;
    }
}

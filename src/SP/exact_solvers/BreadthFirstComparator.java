package SP.exact_solvers;

import SP.representations.Solution;

import java.util.Comparator;

public class BreadthFirstComparator implements Comparator<Solution> {

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

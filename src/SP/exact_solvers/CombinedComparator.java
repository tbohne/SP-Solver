package SP.exact_solvers;

import SP.representations.Solution;
import SP.util.LowerBoundsUtil;

import java.util.Comparator;

public class CombinedComparator implements Comparator<Solution> {

    @Override
    public int compare(Solution solOne, Solution solTwo) {

        // sort based on DFS
        if (solOne.getNumberOfAssignedItems() < solTwo.getNumberOfAssignedItems()) {
            return 1;
        } else if (solTwo.getNumberOfAssignedItems() < solOne.getNumberOfAssignedItems()) {
            return -1;
        } else {
            // then sort with best-first (based on LBs)
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
}

package SP.exact_solvers;

import SP.representations.Solution;
import SP.util.LowerBoundsUtil;

import java.util.Comparator;

public class BestFirstComparator implements Comparator<Solution> {

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

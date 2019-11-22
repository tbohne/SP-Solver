package SP.util;

import SP.constructive_heuristics.GeneralHeuristic;
import SP.constructive_heuristics.ThreeCapHeuristic;
import SP.constructive_heuristics.TwoCapHeuristic;
import SP.experiments.CompareSolvers;
import SP.mip_formulations.BinPackingFormulation;
import SP.mip_formulations.ThreeIndexFormulation;
import SP.post_optimization_methods.LocalSearch;

/**
 * A collection of general utility methods used in the representations.
 *
 * @author Tim Bohne
 */
public class RepresentationUtil {

    /**
     * Helper method used in toString().
     *
     * @param numberOfStacks - number of available stacks
     * @return maximum string offset
     */
    public static int getMaximumStringOffset(int numberOfStacks) {
        int cnt = 0;
        while ((numberOfStacks / 10) > 0) {
            numberOfStacks /= 10;
            cnt++;
        }
        return cnt;
    }

    /**
     * Helper method used in toString().
     *
     * @param idx       - index of the stack
     * @param maxOffset - maximum space offset
     * @return space used for the current stack visualization
     */
    public static String getCurrentSpace(int idx, int maxOffset) {
        StringBuilder space = new StringBuilder();
        String idxStr = Integer.toString(idx);
        for (int i = 0; i < maxOffset - idxStr.length() - 1; i++) {
            space.append(" ");
        }
        return space.toString();
    }

    /**
     * Returns the corresponding integer matrix for the given double matrix.
     *
     * @param matrix - matrix whose entries are going to be casted
     * @return corresponding integer matrix
     */
    public static int[][] castFloatingPointMatrix(double[][] matrix) {
        int[][] integerMatrix = new int[matrix.length][matrix[0].length];
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                integerMatrix[i][j] = (int)matrix[i][j];
            }
        }
        return integerMatrix;
    }

    /**
     * Returns the name of the solver used to create the solution.
     *
     * @param solver - specifies the used solver
     * @return name of the used solver
     */
    public static String getNameOfSolver(CompareSolvers.Solver solver) {
        switch (solver) {
            case MIP_BINPACKING:
                return BinPackingFormulation.class.getName();
            case MIP_THREEINDEX:
                return ThreeIndexFormulation.class.getName();
            case CONSTRUCTIVE_TWO_CAP:
                return TwoCapHeuristic.class.getName();
            case CONSTRUCTIVE_THREE_CAP:
                return ThreeCapHeuristic.class.getName();
            case TABU_SEARCH:
                return LocalSearch.class.getName();
            case GENERAL_HEURISTIC:
                return GeneralHeuristic.class.getName();
            default:
                return "";
        }
    }

    /**
     * Returns the abbreviated name of the solver used to create the solution.
     *
     * @param solver - specifies the used solver
     * @return abbreviated name of the used solver
     */
    public static String getAbbreviatedNameOfSolver(CompareSolvers.Solver solver) {
        switch (solver) {
            case MIP_BINPACKING:
                return "BinP";
            case MIP_THREEINDEX:
                return "3Idx";
            case CONSTRUCTIVE_TWO_CAP:
                return "2Cap";
            case CONSTRUCTIVE_THREE_CAP:
                return "3Cap";
            case TABU_SEARCH:
                return "TS";
            case GENERAL_HEURISTIC:
                return "GH";
            default:
                return "";
        }
    }
}

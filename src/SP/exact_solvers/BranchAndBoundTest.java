package SP.exact_solvers;

import SP.experiments.CompareSolvers;
import SP.io.SolutionReader;
import SP.representations.OptimizableSolution;
import SP.representations.Solution;
import SP.util.RepresentationUtil;

import java.util.List;

public class BranchAndBoundTest {

    private static final String INSTANCE_PREFIX = "res/instances/";
    private static final String SOLUTION_PREFIX = "res/solutions/";

    public static void main(String[] args) {

        List<OptimizableSolution> solutions;
        solutions = SolutionReader.readSolutionsFromDir(
            SOLUTION_PREFIX, INSTANCE_PREFIX, RepresentationUtil.getNameOfSolver(CompareSolvers.Solver.GENERAL_HEURISTIC)
        );

        if (solutions.size() == 0) {
            System.out.println("No solution read. Either the specified directory doesn't "
                + "contain any solutions, or the specified heuristic doesn't match the stack capacity of the solutions.");
        }

        for (OptimizableSolution sol : solutions) {
            BranchAndBound solver = new BranchAndBound(sol.getSol());
            solver.solve();
            break;
        }
    }
}

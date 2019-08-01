package SP.experiments;

import SP.constructive_heuristics.TwoCapHeuristic;
import SP.io.InstanceReader;
import SP.io.SolutionWriter;
import SP.representations.Instance;
import SP.representations.Solution;
import SP.util.HeuristicUtil;
import SP.util.RepresentationUtil;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * Provides functionalities for comparisons of different solvers for stacking problems with a stack capacity of 2.
 *
 * @author Tim Bohne
 */
class SolverComparison2Cap extends SolverComparison {

    private final boolean postProcessing;

    /**
     * Constructor
     *
     * @param solutionPrefix  - prefix for solutions
     * @param instancePrefix  - prefix for instances
     * @param timeLimit       - time limit for solving procedure
     * @param hideCPLEXOutput - determines whether cplex output should be hidden
     * @param mipEmphasis     - control trade-offs between speed, feasibility and optimality
     * @param tolerance       - termination tolerance for cplex (MIPs)
     * @param postProcessing  - determines whether the post processing step is used
     */
    public SolverComparison2Cap(
        String solutionPrefix, String instancePrefix, double timeLimit, boolean hideCPLEXOutput,
        int mipEmphasis, double tolerance, boolean postProcessing
    ) {
        super(solutionPrefix, instancePrefix, timeLimit, hideCPLEXOutput, mipEmphasis, tolerance);
        this.postProcessing = postProcessing;
    }

    /**
     * Compares the different solvers for a stack capacity of 2.
     *
     * @param solversToBeCompared - determines the solvers that are supposed to be compared
     */
    public void compareSolvers(List<CompareSolvers.Solver> solversToBeCompared) {

        File dir = new File(this.instancePrefix);
        File[] directoryListing = dir.listFiles();
        assert directoryListing != null;
        Arrays.sort(directoryListing);
        String allSol = HeuristicUtil.createStringContainingAllSolutionNames(this.solutionPrefix);

        for (File file : directoryListing) {

            if (file.toString().contains("slp_instance_") && !allSol.contains(file.toString().replace("res/instances/", ""))) {
                String instanceName = file.toString().replace("res/instances/", "").replace(".txt", "");
                System.out.println("working on: " + instanceName);
                Instance instance = InstanceReader.readInstance(this.instancePrefix + instanceName + ".txt");
                String solutionName = instanceName.replace("instance", "sol");

                computeLowerBound(instance, solutionName);

                if (solversToBeCompared.contains(CompareSolvers.Solver.MIP_BINPACKING)) {
                    System.out.println("solving with BinP..");
                    solveWithBinPacking(instance, solutionName);
                    instance.resetStacks();
                }
                if (solversToBeCompared.contains(CompareSolvers.Solver.MIP_THREEINDEX)) {
                    System.out.println("solving with 3Idx..");
                    solveWithThreeIdx(instance, solutionName);
                    instance.resetStacks();
                }
                if (solversToBeCompared.contains(CompareSolvers.Solver.CONSTRUCTIVE_TWO_CAP)) {
                    System.out.println("solving with 2Cap..");
                    solveWithTwoCap(instance, solutionName);
                }
            }
        }
    }

    /**
     * Solves the given instance with the constructive heuristic for a stack capacity of 2.
     *
     * @param instance     - instance to be solved
     * @param solutionName - name of the generated solution
     */
    private void solveWithTwoCap(Instance instance, String solutionName) {
        TwoCapHeuristic twoCapHeuristic = new TwoCapHeuristic(instance, this.timeLimit);
        Solution sol = twoCapHeuristic.solve(this.postProcessing);
        if (!sol.isEmpty()) {
            SolutionWriter.writeSolution(
                this.solutionPrefix + solutionName + ".txt", sol, RepresentationUtil.getNameOfSolver(CompareSolvers.Solver.CONSTRUCTIVE_TWO_CAP)
            );
            SolutionWriter.writeSolutionAsCSV(
                this.solutionPrefix + "solutions.csv", sol, RepresentationUtil.getAbbreviatedNameOfSolver(CompareSolvers.Solver.CONSTRUCTIVE_TWO_CAP)
            );
        }
    }
}

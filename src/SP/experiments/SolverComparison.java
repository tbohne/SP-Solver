package SP.experiments;

import SP.constructive_heuristics.GeneralHeuristic;
import SP.io.InstanceReader;
import SP.io.SolutionWriter;
import SP.mip_formulations.BinPackingFormulation;
import SP.mip_formulations.ThreeIndexFormulation;
import SP.representations.Instance;
import SP.representations.Solution;
import SP.util.HeuristicUtil;
import SP.util.RepresentationUtil;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * Provides functionalities for comparisons of different solvers for stacking problems.
 *
 * @author Tim Bohne
 */
public class SolverComparison {

    protected final String solutionPrefix;
    protected final String instancePrefix;
    protected final double timeLimit;

    // cplex config
    private final boolean hideCPLEXOutput;
    private final int mipEmphasis;
    private final double mipTolerance;

    /**
     * Constructor
     *
     * @param solutionPrefix  - prefix for solutions
     * @param instancePrefix  - prefix for instances
     * @param timeLimit       - time limit for solving procedure
     * @param hideCPLEXOutput - determines whether cplex output should be hidden
     * @param mipEmphasis     - control trade-offs between speed, feasibility and optimality
     * @param mipTolerance    - termination tolerance for cplex (MIPs)
     */
    public SolverComparison(
        String solutionPrefix, String instancePrefix, double timeLimit,
        boolean hideCPLEXOutput, int mipEmphasis, double mipTolerance
    ) {
        this.solutionPrefix = solutionPrefix;
        this.instancePrefix = instancePrefix;
        this.timeLimit = timeLimit;
        this.hideCPLEXOutput = hideCPLEXOutput;
        this.mipEmphasis = mipEmphasis;
        this.mipTolerance = mipTolerance;
    }

    /**
     * Computes a lower bound for the specified instance using the LowerBoundsCalculator.
     *
     * @param instance     - instance to compute a LB for
     * @param solutionName - name of the generated solution
     */
    public void computeLowerBound(Instance instance, String solutionName) {
        LowerBoundsCalculator lbc = new LowerBoundsCalculator(instance);
        double lowerBound = lbc.computeLowerBound();
        SolutionWriter.writeLowerBound(this.solutionPrefix + solutionName + ".txt", lowerBound);
        SolutionWriter.writeLowerBoundAsCSV(this.solutionPrefix + "solutions.csv", lowerBound, instance.getName());
    }

    /**
     * Solves the given instance with the bin-packing solver.
     *
     * @param instance     - instance to be solved
     * @param solutionName - name of the generated solution
     */
    public void solveWithBinPacking(Instance instance, String solutionName) {
        BinPackingFormulation binPackingFormulation = new BinPackingFormulation(
            instance, this.timeLimit, this.hideCPLEXOutput, this.mipEmphasis, this.mipTolerance
        );
        Solution sol = binPackingFormulation.solve();
        SolutionWriter.writeSolution(
            this.solutionPrefix + solutionName + ".txt", sol, RepresentationUtil.getNameOfSolver(CompareSolvers.Solver.MIP_BINPACKING)
        );
        SolutionWriter.writeSolutionAsCSV(
            this.solutionPrefix + "solutions.csv", sol, RepresentationUtil.getAbbreviatedNameOfSolver(CompareSolvers.Solver.MIP_BINPACKING)
        );
    }

    /**
     * Solves the given instance with the general heuristic.
     *
     * @param instance     - instance to be solved
     * @param solutionName - name of the generated solution
     */
    public void solveWithGeneralHeuristic(Instance instance, String solutionName) {
        GeneralHeuristic heuristic = new GeneralHeuristic(instance, this.timeLimit);
        Solution sol = heuristic.solve();
        System.out.println("is feasible ? " + sol.isFeasible());
        SolutionWriter.writeSolution(
            this.solutionPrefix + solutionName + ".txt", sol, RepresentationUtil.getNameOfSolver(CompareSolvers.Solver.GENERAL_HEURISTIC)
        );
        SolutionWriter.writeSolutionAsCSV(
            this.solutionPrefix + "solutions.csv", sol, RepresentationUtil.getAbbreviatedNameOfSolver(CompareSolvers.Solver.GENERAL_HEURISTIC)
        );
    }

    /**
     * Solves the given instance with the three-index solver.
     *
     * @param instance     - instance to be solved
     * @param solutionName - name of the generated solution
     */
    public void solveWithThreeIdx(Instance instance, String solutionName) {
        ThreeIndexFormulation threeIndexFormulation = new ThreeIndexFormulation(
            instance, this.timeLimit, this.hideCPLEXOutput, this.mipEmphasis, this.mipTolerance
        );
        Solution sol = threeIndexFormulation.solve();
        SolutionWriter.writeSolution(
            this.solutionPrefix + solutionName + ".txt", sol, RepresentationUtil.getNameOfSolver(CompareSolvers.Solver.MIP_THREEINDEX)
        );
        SolutionWriter.writeSolutionAsCSV(
            this.solutionPrefix + "solutions.csv", sol, RepresentationUtil.getAbbreviatedNameOfSolver(CompareSolvers.Solver.MIP_THREEINDEX)
        );
    }

    /**
     * Workaround that triggers the ClassLoader to load all classes that are needed during the solving procedure.
     * Otherwise the first instance to be solved would have a longer runtime because of the initial class loading.
     *
     * @param instanceFile - file to load the instance from
     */
    public void prepareRuntimeMeasurementByPreLoadingAllClasses(File instanceFile) {
        String instanceName = instanceFile.toString().replace("res/instances/", "").replace(".txt", "");
        Instance instance = InstanceReader.readInstance(this.instancePrefix + instanceName + ".txt");
        String solutionName = instanceName.replace("instance", "sol");
        computeLowerBound(instance, solutionName);
    }

    /**
     * Computes lower bounds and compares the bin-packing and three-index formulation.
     * The results are written to the file system.
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
                if (solversToBeCompared.contains(CompareSolvers.Solver.GENERAL_HEURISTIC)) {
                    System.out.println("solving with general heuristic..");
                    solveWithGeneralHeuristic(instance, solutionName);
                    instance.resetStacks();
                }
            }
        }
    }
}

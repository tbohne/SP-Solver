package SP.io;

import SP.experiments.CompareSolvers;
import SP.experiments.PostOptimization;
import SP.representations.OptimizableSolution;
import SP.representations.Solution;

import java.io.*;

/**
 * Provides functionalities to write solutions of stacking problems to the file system.
 *
 * @author Tim Bohne
 */
public class SolutionWriter {

    /**
     * Writes the specified solution as CSV to the specified file.
     *
     * @param filename       - name of the file to write to
     * @param lowerBound     - LB to be written to the file
     * @param nameOfInstance - name of the instance the LB was computed for
     */
    public static void writeLowerBoundAsCSV(String filename, double lowerBound, String nameOfInstance) {
        try {
            File file = new File(filename);
            boolean newFile = false;

            if (!file.exists()) {
                file.createNewFile();
                newFile = true;
            }
            FileWriter fw = new FileWriter(file, true);
            BufferedWriter bw = new BufferedWriter(fw);

            if (newFile) {
                bw.write("instance,solver,time,val\n");
            }
            bw.write(nameOfInstance.replace("instances/slp_instance_", "")
                + "," + "LB" + "," + "-" + "," + lowerBound + "\n");

            bw.close();
            fw.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Writes the configuration of the post-optimization process to a csv file.
     *
     * @param filename                               - name of the file to store the config in
     * @param solverOfInitialSolution                - name of the constructive heuristic used to create the init solution
     * @param shortTermStrategy                      - used short-term strategy
     * @param stoppingCriterion                      - used stopping criterion
     * @param numOfNeighbors                         - number of neighbors considered in each iteration
     * @param maxTabuListLengthFactor                - factor that is multiplied with the number of nbrs to provide a max length for the tabu list
     * @param unsuccessfulNeighborGenerationAttempts - number of unsuccessful neighbor generation attempts before the nbh search is stopped
     * @param numOfIterations                        - number of iterations (only used when stopping criterion)
     * @param numOfTabuListClears                    - number of tabu list clears (only used when stopping criterion)
     * @param numOfNonImprovingIterations            - number of non-improving iterations (only used when stopping criterion)
     * @param maxNumOfSwaps                          - maximum number of swaps (only used in swap-operator)
     */
    public static void writePostOptimizationConfig(
        String filename, CompareSolvers.Solver solverOfInitialSolution, PostOptimization.ShortTermStrategies shortTermStrategy,
        PostOptimization.StoppingCriteria stoppingCriterion, int numOfNeighbors, int maxTabuListLengthFactor,
        int unsuccessfulNeighborGenerationAttempts, int numOfIterations, int numOfTabuListClears,
        int numOfNonImprovingIterations, int maxNumOfSwaps
    ) {

        File file = new File(filename);

        try {
            FileOutputStream fos = new FileOutputStream(file);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
            bw.write(
            "solverOfInitialSolution,shortTermStrategy,stoppingCriterion,numOfNeighbors,maxTabuListLengthFactor,"
                + "unsuccessfulNeighborGenerationAttempts,numOfIterations,numOfTabuListClears,numOfNonImprovingIterations,maxNumOfSwaps\n"
            );
            bw.write(
            solverOfInitialSolution + "," + shortTermStrategy + "," + stoppingCriterion + "," + numOfNeighbors
                + "," + maxTabuListLengthFactor + "," + unsuccessfulNeighborGenerationAttempts + ","
                + numOfIterations + "," + numOfTabuListClears + "," + numOfNonImprovingIterations + "," + maxNumOfSwaps
            );
            bw.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeImpAsCSV(String filename, Solution impSol, String postOptimizationMethod) {
        try {
            File file = new File(filename);
            boolean newFile = false;
            if (!file.exists()) {
                file.createNewFile();
                newFile = true;
            }
            FileWriter fw = new FileWriter(file, true);
            BufferedWriter bw = new BufferedWriter(fw);
            if (newFile) {
                bw.write("instance,solver,time,val\n");
            }

            String solver;
            if (impSol.getFilledStacks()[0].length == 2) {
                solver = "2Cap + " + postOptimizationMethod;
            } else if (impSol.getFilledStacks()[0].length == 3) {
                solver = "3Cap + " + postOptimizationMethod;
            } else {
                solver = "GH + " + postOptimizationMethod;
            }

            double totalRuntime = (double)Math.round((impSol.getTimeToSolveAsDouble() + impSol.getTimeToSolveAsDouble()) * 100) / 100;

            bw.write(impSol.getNameOfSolvedInstance().replace("instances/slp_instance_", "")
                + "," + solver + "," + totalRuntime + "," + impSol.computeCosts() + "\n");

            bw.close();
            fw.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Writes the optimal solution and the improved solution generated by the
     * post optimization method as CSV to the specified file.
     *
     * @param filename - name of the file to write to
     * @param sol      - original solution
     * @param impSol   - improved solution (post optimization result)
     */
    public static void writeOptAndImpAsCSV(String filename, OptimizableSolution sol, Solution impSol, String postOptimizationMethod) {
        try {
            File file = new File(filename);
            boolean newFile = false;
            if (!file.exists()) {
                file.createNewFile();
                newFile = true;
            }
            FileWriter fw = new FileWriter(file, true);
            BufferedWriter bw = new BufferedWriter(fw);
            if (newFile) {
                bw.write("instance,solver,time,val\n");
            }

            bw.write(impSol.getNameOfSolvedInstance().replace("instances/slp_instance_", "")
                + "," + "OPT" + "," + sol.getRuntimeForOptimalSolution() + "," + sol.getOptimalObjectiveValue() + "\n");

            writeImpAsCSV(filename, impSol, postOptimizationMethod);

            bw.close();
            fw.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Writes the specified solution as CSV to the specified file.
     *
     * @param filename - name of the file to write to
     * @param sol      - solution to be written to the file
     * @param solver   - solver used to create the solution
     */
    public static void writeSolutionAsCSV(String filename, Solution sol, String solver) {
        try {
            File file = new File(filename);
            boolean newFile = false;
            if (!file.exists()) {
                file.createNewFile();
                newFile = true;
            }
            FileWriter fw = new FileWriter(file, true);
            BufferedWriter bw = new BufferedWriter(fw);

            if (newFile) {
                bw.write("instance,solver,time,val\n");
            }
            if (sol.isFeasible()) {
                bw.write(
                    sol.getNameOfSolvedInstance().replace("instances/slp_instance_", "")
                    + "," + solver + "," + sol.getTimeToSolve() + "," + sol.computeCosts() + "\n"
                );
            }

            bw.close();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Writes the specified LB to the specified file.
     *
     * @param filename   - name of the file to write to
     * @param lowerBound - lower bound to be written to the file
     */
    public static void writeLowerBound(String filename, double lowerBound) {
        try {
            File file = new File(filename);
            if (!file.exists()) {
                file.createNewFile();
            }

            FileWriter fw = new FileWriter(file, true);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write("lower bound for relaxed s_ij: " + lowerBound + "\n");

            bw.close();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Writes the specified solution to the specified file.
     *
     * @param filename - name of the file to write to
     * @param sol      - solution to be written to the file
     * @param solver   - solver used to create the solution
     */
    public static void writeSolution(String filename, Solution sol, String solver) {
        try {
            File file = new File(filename);
            boolean appendNewLines = true;
            if (!file.exists()) {
                file.createNewFile();
                appendNewLines = false;
            }
            FileWriter fw = new FileWriter(file, true);
            BufferedWriter bw = new BufferedWriter(fw);

            if (appendNewLines) {
                bw.newLine();
                bw.write("#####################################################\n");
                bw.newLine();
            }
            bw.write("solved with: " + solver + "\n");
            bw.write(sol.toString());

            bw.close();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

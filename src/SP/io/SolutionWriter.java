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
    public static void writeLowerBoundAsCSV(String filename, double lowerBound, String nameOfInstance, double timeLimit) {
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
                bw.write("instance,solver,time_limit,time,val,imp(abs),imp(%),time_to_best(s),iterations_to_best,total_iterations\n");
            }
            bw.write(nameOfInstance.replace("instances/slp_instance_", "")
                + "," + "LB" + "," + timeLimit + ",-," + lowerBound + ",-,-,-,-,-" + "\n");

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
     * @param unsuccessfulNeighborGenerationAttempts - number of unsuccessful neighbor generation attempts before the nbh search is stopped
     * @param numOfIterations                        - number of iterations (only used when stopping criterion)
     * @param numOfNonImprovingIterations            - number of non-improving iterations (only used when stopping criterion)
     */
    public static void writePostOptimizationConfig(
        String filename, CompareSolvers.Solver solverOfInitialSolution, PostOptimization.ShortTermStrategies shortTermStrategy,
        PostOptimization.StoppingCriteria stoppingCriterion, int numOfNeighbors, int unsuccessfulNeighborGenerationAttempts,
        int numOfIterations, int numOfNonImprovingIterations
    ) {

        File file = new File(filename);

        try {
            FileOutputStream fos = new FileOutputStream(file);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
            bw.write(
            "solverOfInitialSolution,shortTermStrategy,stoppingCriterion,numOfNeighbors,"
                + "unsuccessfulNeighborGenerationAttempts,numOfIterations,numOfNonImprovingIterations\n"
            );
            bw.write(
            solverOfInitialSolution + "," + shortTermStrategy + "," + stoppingCriterion + "," + numOfNeighbors
                + "," + unsuccessfulNeighborGenerationAttempts + "," + numOfIterations + ","
                + numOfNonImprovingIterations
            );
            bw.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeImpAsCSV(
        String filename, Solution impSol, String postOptimizationMethod, Solution initialSol,
        double timeToBestSolution, int iterationsToBestSolution, int totalNumberOfPerformedIterations
    ) {

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
                bw.write("instance,solver,time_limit,time,val,imp(abs),imp(%),time_to_best(s),iterations_to_best,total_iterations\n");
            }

            String solver;
//            if (impSol.getFilledStacks()[0].length == 2) {
//                solver = "2Cap + " + postOptimizationMethod;
//            } else if (impSol.getFilledStacks()[0].length == 3) {
//                solver = "3Cap + " + postOptimizationMethod;
//            } else {
                solver = "GH + " + postOptimizationMethod;
//            }

            double totalRuntime = Math.round(impSol.getTimeToSolveAsDouble() * 100.0) / 100.0;
            double absoluteImprovement = Math.round(initialSol.computeCosts() - impSol.computeCosts());
            double relativeImprovement = Math.round((initialSol.computeCosts() - impSol.computeCosts()) / initialSol.computeCosts() * 100.0);

            bw.write(impSol.getNameOfSolvedInstance().replace("instances/slp_instance_", "")
                + "," + solver + "," + initialSol.getTimeLimit() + "," + totalRuntime + "," + impSol.computeCosts()
                + "," + absoluteImprovement + "," + relativeImprovement + "," + timeToBestSolution + ","
                + iterationsToBestSolution + "," + totalNumberOfPerformedIterations + "\n");

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
    public static void writeOptAndImpAsCSV(
        String filename, OptimizableSolution sol, Solution impSol, String postOptimizationMethod,
        double timeToBestSolution, int iterationsToBestSolution, int totalNumberOfPerformedIterations
    ) {

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
                bw.write("instance,solver,time_limit,time,val,imp(abs),imp(%),time_to_best(s),iterations_to_best,total_iterations\n");
            }

            bw.write(impSol.getNameOfSolvedInstance().replace("instances/slp_instance_", "")
                + "," + "OPT" + "," + sol.getSol().getTimeLimit() + "," + sol.getRuntimeForOptimalSolution() + "," + sol.getOptimalObjectiveValue() + "\n");

            writeImpAsCSV(filename, impSol, postOptimizationMethod, sol.getSol(), timeToBestSolution, iterationsToBestSolution, totalNumberOfPerformedIterations);

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
                bw.write("instance,solver,time_limit,time,val,imp(abs),imp(%),time_to_best(s),iterations_to_best,total_iterations\n");
            }
            if (sol.isFeasible()) {
                bw.write(
                    sol.getNameOfSolvedInstance().replace("instances/slp_instance_", "")
                    + "," + solver + "," + sol.getTimeLimit() + "," + sol.getTimeToSolve() + "," + sol.computeCosts()
                    + ",-,-,-,-,-" + "\n"
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

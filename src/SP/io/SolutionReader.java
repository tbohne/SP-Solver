package SP.io;

import SP.experiments.CompareSolvers;
import SP.representations.OptimizableSolution;
import SP.representations.Instance;
import SP.representations.Solution;
import SP.util.RepresentationUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Provides functionalities to read solutions for stacking problems from the file system.
 *
 * @author Tim Bohne
 */
public class SolutionReader {

    /**
     * Reads the solutions from the files contained in the specified directory.
     *
     * @param solutionDirName - name of the directory containing the solutions
     * @param instanceDirName - name of the directory containing the solved instances
     * @param solver          - specifies the solver for which the solution is to be read
     * @return list of the read solutions as optimizable solutions
     */
    public static List<OptimizableSolution> readSolutionsFromDir(String solutionDirName, String instanceDirName, String solver) {
        File dir = new File(solutionDirName);
        List<OptimizableSolution> solutions = new ArrayList<>();
        File[] directoryListing = dir.listFiles();
        assert directoryListing != null;
        Arrays.sort(directoryListing);
        for (File file : directoryListing) {
            if (!file.isDirectory() && file.getName().contains("slp_")) {

                if (file.getName().contains("_2_")
                    && solver.equals(RepresentationUtil.getNameOfSolver(CompareSolvers.Solver.CONSTRUCTIVE_THREE_CAP))
                    || file.getName().contains("_3_")
                    && solver.equals(RepresentationUtil.getNameOfSolver(CompareSolvers.Solver.CONSTRUCTIVE_TWO_CAP))
                ) {
                    return solutions;
                }

                solutions.add(readSolutionFile(file, instanceDirName, solver));
            }
        }
        return solutions;
    }

    /**
     * Reads the stack assignments from a solution file using the specified reader.
     *
     * @param br     - buffered reader pointing to the line to read from
     * @param stacks - stacks to be filled
     * @throws IOException for errors during reading procedure
     */
    private static void readStackAssignments(BufferedReader br, List<List<Integer>> stacks) throws IOException {
        for (String line; (line = br.readLine()) != null && !line.contains("#"); ) {
            List<Integer> stack = new ArrayList<>();
            String[] splitArr = line.split(":");
            if (splitArr.length > 1) {
                String first = splitArr[1].trim();
                if (first.length() > 0) {
                    int[] array = Arrays.stream(first.split("\\s")).mapToInt(Integer::parseInt).toArray();
                    for (int item : array) {
                        stack.add(item);
                    }
                }
                stacks.add(new ArrayList<>(stack));
            } else {
                stacks.add(new ArrayList<>());
            }
        }
    }

    /**
     * Copies the read stack assignments to the solution's filled stacks.
     *
     * @param stackAssignments - stack assignments to be copied
     * @param sol              - solution whose stacks get filled
     */
    private static void fillStacks(List<List<Integer>> stackAssignments, Solution sol) {
        for (int i = 0; i < stackAssignments.size(); i++) {
            for (int j = 0; j < stackAssignments.get(i).size(); j++) {
                sol.getFilledStacks()[i][j] = stackAssignments.get(i).get(j);
            }
        }
    }

    /**
     * Reads a solution to a stacking problem from the specified file.
     *
     * @param br              - buffered reader pointing to the file to read from
     * @param instanceDirName - name of the instance directory
     * @param file            - file to read from
     * @return read solution
     */
    private static Solution readSolution(BufferedReader br, String instanceDirName, File file) {

        Solution sol = new Solution();

        try {
            String line = br.readLine();
            if (line.contains("Problem not solved")) { return sol; }
            double timeLimit = Double.parseDouble(line.split(":")[1].replace("s", "").trim());
            line = br.readLine();
            if (line.contains("time limit exceeded")) { return sol; }

            double timeToSolveInSeconds = Double.parseDouble(line.split(":")[1].replace(",", ".").replace("s", "").trim());
            br.readLine();
            br.readLine();
            br.readLine();

            List<List<Integer>> stackAssignments = new ArrayList<>();
            readStackAssignments(br, stackAssignments);
            String instanceName = file.getName().replace("sol", "instance");
            Instance instance = InstanceReader.readInstance(instanceDirName + instanceName);
            sol = new Solution(timeToSolveInSeconds, timeLimit, instance);
            fillStacks(stackAssignments, sol);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return sol;
    }

    /**
     * Reads the solution to a stacking problem from the specified file.
     *
     * @param file            - file to read from
     * @param instanceDirName - name of the instance directory
     * @param solver          - specifies the solver for which the solution is to be read
     * @return generated optimizable solution from the read solution
     */
    private static OptimizableSolution readSolutionFile(File file, String instanceDirName, String solver) {

        Solution sol = new Solution();
        double optimalObjectiveValue = -1;
        double runtimeForOptimalSolution = -1;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {

            String line = br.readLine();

            while (line != null) {
                if (line.contains("solved with:")) {

                    if (!line.contains(solver)) {
                        Solution tmpSol = readSolution(br, instanceDirName, file);
                        if (tmpSol.getTimeToSolveAsDouble() < tmpSol.getTimeLimit()) {
                            optimalObjectiveValue = tmpSol.computeCosts();
                            runtimeForOptimalSolution = tmpSol.getTimeToSolveAsDouble();
                        }
                    } else {
                        sol = readSolution(br, instanceDirName, file);
                    }
                }
                line = br.readLine();
            }
            br.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        sol.lowerItemsThatAreStackedInTheAir();
        return new OptimizableSolution(sol, optimalObjectiveValue, runtimeForOptimalSolution);
    }
}

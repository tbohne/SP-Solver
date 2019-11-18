package SP.post_optimization_methods;

import SP.experiments.PostOptimization;
import SP.representations.Solution;
import SP.util.HeuristicUtil;
import org.jgrapht.GraphPath;

import java.util.*;

public class VariableTabuSearchTabuSearchNeighborhood implements Neighborhood {

    private int numberOfNeighbors;
    private PostOptimization.ShortTermStrategies shortTermStrategy;
    private int unsuccessfulNeighborGenerationAttempts;
    private int maxTabuListLength;
    private int tabuListClears;
    private Queue<Shift> tabuList;

    // nbh operators
    private EjectionChainOperator ejectionChainOperator;
    private ShiftOperator shiftOperator;
    private SwapOperator swapOperator;

    public VariableTabuSearchTabuSearchNeighborhood(
        int numberOfNeighbors, PostOptimization.ShortTermStrategies shortTermStrategy,
        int maxTabuListLength, int unsuccessfulNeighborGenerationAttempts,
        EjectionChainOperator ejectionChainOperator, ShiftOperator shiftOperator, SwapOperator swapOperator
    ) {
        this.numberOfNeighbors = numberOfNeighbors;
        this.shortTermStrategy = shortTermStrategy;
        this.maxTabuListLength = maxTabuListLength;
        this.unsuccessfulNeighborGenerationAttempts = unsuccessfulNeighborGenerationAttempts;
        this.tabuListClears = 0;
        this.unsuccessfulNeighborGenerationAttempts = unsuccessfulNeighborGenerationAttempts;
        this.tabuList = new LinkedList<>();

        this.ejectionChainOperator = ejectionChainOperator;
        this.shiftOperator = shiftOperator;
        this.swapOperator = swapOperator;
    }

    /**
     * Clears the entries in the shift tabu list and increments the clear counter.
     */
    public void clearTabuList() {
        this.tabuList.clear();
        this.tabuListClears++;
    }

    public int getTabuListClears() {
        return this.tabuListClears;
    }

    public boolean tabuListContainsAnyOfTheShifts(List<Shift> performedShifts) {
        for (Shift shift : performedShifts) {
            if (this.tabuList.contains(shift)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds the specified shift operation to the tabu list.
     * Replaces the oldest entry if the maximum length of the tabu list is reached.
     *
     */
    public void forbidShifts(List<Shift> performedShifts) {
        if (this.tabuList.size() >= this.maxTabuListLength) {
            while (this.tabuList.size() + performedShifts.size() >= this.maxTabuListLength) {
                this.tabuList.poll();
            }
        }
        for (Shift shift : performedShifts) {
            this.tabuList.add(shift);
        }
    }

    private void logCurrentState(Solution currSol, GraphPath bestPath, Solution tmpSol) {
        System.out.println("costs before: " + currSol.computeCosts());
        System.out.println("costs of best path: " + bestPath.getWeight());
        System.out.println("costs after ejection chain: " + tmpSol.computeCosts());
        System.out.println("feasible: " + tmpSol.isFeasible());
        System.exit(0);
    }

    private Solution applyVariableNeighborhood(Solution currSol, List<Shift> performedShifts) {

        Solution neighbor = new Solution();

//        // shift is only possible if there are free slots
//        if (currSol.getNumberOfAssignedItems() < currSol.getFilledStacks().length * currSol.getFilledStacks()[0].length) {
//            neighbor = this.shiftOperator.generateShiftNeighbor(currSol, performedShifts);
//        }
//        if (!neighbor.isFeasible() || neighbor.computeCosts() > currSol.computeCosts()) {
//            // next operator
//            int rand = HeuristicUtil.getRandomIntegerInBetween(1, 4);
//            performedShifts.clear();
//            neighbor = this.swapOperator.generateSwapNeighbor(currSol, performedShifts, rand);
//            if (!neighbor.isFeasible() || neighbor.computeCosts() > currSol.computeCosts()) {
//                // next operator
                performedShifts.clear();
                double start = System.currentTimeMillis();
                neighbor = this.ejectionChainOperator.generateEjectionChainNeighbor(currSol, performedShifts);
//                System.out.println("runtime for ejection chain nbr generation: " + (System.currentTimeMillis() - start) / 1000.0);
////                System.out.println("USING EJECTION CHAIN OPERATOR");
//            } else {
////                System.out.println("USING SWAP OPERATOR");
//            }
//        } else {
////            System.out.println("USING SHIFT OPERATOR");
//        }
        return neighbor;
    }

    public Solution getNeighbor(Solution currSol, Solution bestSol) {
        List<Solution> nbrs = new ArrayList<>();
        int failCnt = 0;
        Map<Solution, List<Shift>> shiftsForSolution = new HashMap<>();

        System.out.println("TL: " + this.tabuList.size());

        while (nbrs.size() < this.numberOfNeighbors) {

            List<Shift> performedShifts = new ArrayList<>();
            Solution neighbor = this.applyVariableNeighborhood(currSol, performedShifts);
            if (!neighbor.isFeasible()) { continue; }
            shiftsForSolution.put(neighbor, performedShifts);

            // FIRST-FIT
            if (this.shortTermStrategy == PostOptimization.ShortTermStrategies.FIRST_FIT
                && !this.tabuListContainsAnyOfTheShifts(performedShifts)
                && neighbor.computeCosts() < currSol.computeCosts()
            ) {
                this.forbidShifts(performedShifts);
//                System.out.println("FIRST-FIT RETURN");
                return neighbor;
            // BEST-FIT
            } else if (!this.tabuListContainsAnyOfTheShifts(performedShifts)) {
                nbrs.add(neighbor);
            } else {

                System.out.println("TABU");

                // TABU
                // ASPIRATION CRITERION
                if (neighbor.computeCosts() < bestSol.computeCosts()) {
//                    System.out.println("ASPIRATION!");
                    if (this.shortTermStrategy == PostOptimization.ShortTermStrategies.FIRST_FIT) {
                        return neighbor;
                    } else {
                        nbrs.add(neighbor);
                    }
                } else {
                    failCnt++;
                    if (failCnt == this.unsuccessfulNeighborGenerationAttempts) {
                        failCnt = 0;

                        if (nbrs.size() == 0) {
//                            System.out.println("CLEARING TL");
                            this.clearTabuList();
                        } else {
//                            System.out.println("FAIL RETURN");
                            return HeuristicUtil.getBestSolution(nbrs);
                        }
                    }
                }
            }
        }

//        System.out.println("BEST-FIT RETURN");
        Solution best = HeuristicUtil.getBestSolution(nbrs);
        this.forbidShifts(shiftsForSolution.get(best));
        return HeuristicUtil.getBestSolution(nbrs);
    }
}

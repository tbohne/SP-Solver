package SP.exact_solvers;

import SP.representations.Item;
import SP.representations.Solution;
import SP.util.HeuristicUtil;

import java.util.ArrayList;
import java.util.List;

public class BranchAndBound {

    private Solution initialSolution;
    private double startTime;
    private Solution bestSol;
    private double bestObjectiveValue;
    private int numberOfItems;
    private double[][] costs;
    private int[][] stackingConstraints;
    private Item[] itemObjects;

    public BranchAndBound(Solution initialSolution) {
        this.initialSolution = initialSolution;
        this.bestObjectiveValue = initialSolution.computeCosts();
        this.bestSol = new Solution(this.initialSolution);
        this.numberOfItems = initialSolution.getSolvedInstance().getItemObjects().length;
        this.costs = initialSolution.getSolvedInstance().getCosts();
        this.stackingConstraints = initialSolution.getSolvedInstance().getStackingConstraints();
        this.itemObjects = initialSolution.getSolvedInstance().getItemObjects();
    }

    private void clearSolution(Solution sol) {
        for (int i = 0; i < sol.getFilledStacks().length; i++) {
            for (int j = 0; j < sol.getFilledStacks()[0].length; j++) {
                sol.getFilledStacks()[i][j] = -1;
            }
        }
    }

    private void printRes(List<Solution> solutions) {
        for (Solution sol : solutions) {
            sol.lowerItemsThatAreStackedInTheAir();
            sol.printFilledStacks();
            System.out.println(sol.computeCosts());
            System.out.println("feasible: " + sol.isFeasible());
            System.out.println();
        }
    }

    public void solve() {
        Solution clearSol = new Solution(this.initialSolution);
        this.clearSolution(clearSol);
        List<Solution> solutionsForItemIteration = new ArrayList<>();

        // init solutions for item 0
        for (int stack = 0; stack < clearSol.getFilledStacks().length; stack++) {
            Solution tmpSol = new Solution(clearSol);

            if (HeuristicUtil.itemCompatibleWithStack(this.costs, 0, stack)
                    && HeuristicUtil.stackHasFreePosition(tmpSol.getFilledStacks()[stack])
                    && HeuristicUtil.itemCompatibleWithAlreadyAssignedItems(0, tmpSol.getFilledStacks()[stack], this.itemObjects, this.stackingConstraints)) {

                HeuristicUtil.assignItemToStack(0, tmpSol.getFilledStacks()[stack], this.itemObjects);
                solutionsForItemIteration.add(tmpSol);
            }
        }

        actuallySolve(solutionsForItemIteration, 1);
    }

    private void actuallySolve(List<Solution> solutions, int itemToBeAdded) {

        List<Solution> newSolutions = new ArrayList<>();

        for (Solution currSol : solutions) {
            for (int stack = 0; stack < currSol.getFilledStacks().length; stack++) {

                Solution tmpSol = new Solution(currSol);

                if (HeuristicUtil.itemCompatibleWithStack(this.costs, itemToBeAdded, stack)
                    && HeuristicUtil.stackHasFreePosition(tmpSol.getFilledStacks()[stack])
                    && HeuristicUtil.itemCompatibleWithAlreadyAssignedItems(itemToBeAdded, tmpSol.getFilledStacks()[stack], this.itemObjects, this.stackingConstraints)) {

                        if (!tmpSol.isItemAssigned(itemToBeAdded)) {
                            HeuristicUtil.assignItemToStack(itemToBeAdded, tmpSol.getFilledStacks()[stack], this.itemObjects);
                            newSolutions.add(tmpSol);
                        }
                }
            }
        }

        if (itemToBeAdded + 1 <  this.numberOfItems) {
            this.actuallySolve(newSolutions, ++itemToBeAdded);
        } else {
            this.printRes(newSolutions);
        }
    }
}

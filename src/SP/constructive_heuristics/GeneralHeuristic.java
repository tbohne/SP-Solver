package SP.constructive_heuristics;

import SP.representations.*;
import SP.util.HeuristicUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GeneralHeuristic {

    private final Instance instance;
    private double startTime;
    private final double timeLimit;

    /**
     * Constructor
     *
     * @param instance  - instance of the stacking problem to be solved
     * @param timeLimit - time limit for the solving procedure
     */
    public GeneralHeuristic(Instance instance, double timeLimit) {
        this.instance = instance;
        this.timeLimit = timeLimit;
    }

    private List<Integer> getItemList() {
        List<Integer> items = new ArrayList<>();
        for (int item : this.instance.getItems()) {
            items.add(item);
        }
        return items;
    }

    public void generateFirstFitSolution() {

        List<Integer> itemList = this.getItemList();
        Collections.shuffle(itemList);

        for (int item : itemList) {
            for (int stack = 0; stack < this.instance.getStacks().length; stack++) {
                if (HeuristicUtil.itemCompatibleWithStack(this.instance.getCosts(), item, stack)
                    && HeuristicUtil.stackHasFreePosition(this.instance.getStacks()[stack])
                    && HeuristicUtil.itemCompatibleWithAlreadyAssignedItems(
                        item, this.instance.getStacks()[stack], this.instance.getItemObjects(), this.instance.getStackingConstraints())
                ) {
                    HeuristicUtil.assignItemToStack(item, this.instance.getStacks()[stack], this.instance.getItemObjects());
                    break;
                }
            }
        }
    }

    public Solution solve() {

        Solution sol = new Solution();
        this.startTime = System.currentTimeMillis();

        while (!sol.isFeasible()) {
            this.instance.resetStacks();
            this.generateFirstFitSolution();
            this.instance.lowerItemsThatAreStackedInTheAir();
            sol = new Solution((System.currentTimeMillis() - this.startTime) / 1000.0, this.timeLimit, this.instance);
        }

        sol.sortItemsInStacksBasedOnTransitiveStackingConstraints();
        sol.setTimeToSolve((System.currentTimeMillis() - this.startTime) / 1000.0);
        return sol;
    }
}

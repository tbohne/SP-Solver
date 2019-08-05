package SP.exact_solvers;

import SP.representations.BipartiteGraph;
import SP.representations.Item;
import SP.representations.Solution;
import SP.representations.StackPosition;
import SP.util.GraphUtil;
import SP.util.HeuristicUtil;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultUndirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    /**
     * Adds edges to the bipartite graph that connect items with stack positions.
     *
     * @param bipartiteGraph - bipartite graph to add the edges to
     * @param items          - items to be connected to positions
     * @param positions      - positions the items get connected to
     */
    private void addEdgesBetweenItemsAndStackPositions(
            Graph<String, DefaultWeightedEdge> bipartiteGraph, List<Integer> items, List<StackPosition> positions
    ) {
        for (int item : items) {
            for (StackPosition pos : positions) {
                DefaultWeightedEdge edge = bipartiteGraph.addEdge("item" + item, "pos" + pos);
                double costs = this.costs[item][pos.getStackIdx()];
                bipartiteGraph.setEdgeWeight(edge, costs);
            }
        }
    }

    /**
     * Adds edges to the bipartite graph that connect dummy items with stack positions.
     *
     * @param bipartiteGraph - bipartite graph to add the edges to
     * @param dummyItems     - dummy items to be connected to positions
     * @param positions      - positions the dummy items get connected to
     */
    private void addEdgesBetweenDummyItemsAndStackPositions(
            Graph<String, DefaultWeightedEdge> bipartiteGraph, List<Integer> dummyItems, List<StackPosition> positions
    ) {
        for (int item : dummyItems) {
            for (StackPosition pos : positions) {
                DefaultWeightedEdge edge = bipartiteGraph.addEdge("dummy" + item, "pos" + pos);
                bipartiteGraph.setEdgeWeight(edge, 0);
            }
        }
    }

    private BipartiteGraph constructBipartiteGraph(Solution sol) {
        List<Integer> unassignedItems = sol.getUnassignedItems();
        List<StackPosition> freePositions = HeuristicUtil.retrieveEmptyPositions(sol);

        Graph<String, DefaultWeightedEdge> graph = new DefaultUndirectedWeightedGraph<>(DefaultWeightedEdge.class);
        Set<String> partitionOne = new HashSet<>();
        Set<String> partitionTwo = new HashSet<>();

        GraphUtil.addVerticesForUnmatchedItems(unassignedItems, graph, partitionOne);
        GraphUtil.addVerticesForEmptyPositions(freePositions, graph, partitionTwo);
        List<Integer> dummyItems = GraphUtil.introduceDummyVerticesToBipartiteGraph(graph, partitionOne, partitionTwo);
        this.addEdgesBetweenDummyItemsAndStackPositions(graph, dummyItems, freePositions);
        this.addEdgesBetweenItemsAndStackPositions(graph, unassignedItems, freePositions);

        return new BipartiteGraph(partitionOne, partitionTwo, graph);
    }

    private void actuallySolve(List<Solution> solutions, int itemToBeAdded) {

        List<Solution> newSolutions = new ArrayList<>();

        for (Solution currSol : solutions) {
            for (int stack = 0; stack < currSol.getFilledStacks().length; stack++) {

                Solution tmpSol = new Solution(currSol);

                // compute LB for solution
                // if LB < c*
                BipartiteGraph graph = this.constructBipartiteGraph(tmpSol);

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
//            this.printRes(newSolutions);
        }
    }
}

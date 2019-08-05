package SP.exact_solvers;

import SP.representations.BipartiteGraph;
import SP.representations.Item;
import SP.representations.Solution;
import SP.representations.StackPosition;
import SP.util.GraphUtil;
import SP.util.HeuristicUtil;
import org.jgrapht.Graph;
import org.jgrapht.alg.matching.KuhnMunkresMinimalWeightBipartitePerfectMatching;
import org.jgrapht.graph.DefaultUndirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

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

    public void solve() {
        Solution clearSol = new Solution(this.initialSolution);
        this.clearSolution(clearSol);
        branchAndBound(clearSol, 0);
        this.bestSol.lowerItemsThatAreStackedInTheAir();
        System.out.println("feasible: " + this.bestSol.isFeasible());
        System.out.println("costs: " + this.bestSol.computeCosts());
    }

    /**
     * Adds edges to the bipartite graph that connect items with stack positions.
     *
     * @param bipartiteGraph - bipartite graph to add the edges to
     * @param items          - items to be connected to positions
     * @param positions      - positions the items get connected to
     */
    private void addEdgesBetweenItemsAndStackPositions(
            Solution sol, Graph<String, DefaultWeightedEdge> bipartiteGraph, List<Integer> items, List<StackPosition> positions
    ) {
        for (int item : items) {
            for (StackPosition pos : positions) {

                // stack incompatibility is realized indirectly via high costs to keep the graph complete bipartite
                // the stacking constraints should also be respected, therefore they're realized indirectly as well
                if (HeuristicUtil.itemCompatibleWithAlreadyAssignedItems(item, sol.getFilledStacks()[pos.getStackIdx()], this.itemObjects, this.stackingConstraints)) {
                        DefaultWeightedEdge edge = bipartiteGraph.addEdge("item" + item, "pos" + pos);
                        double costs = this.costs[item][pos.getStackIdx()];
                        bipartiteGraph.setEdgeWeight(edge, costs);
                } else {
                    DefaultWeightedEdge edge = bipartiteGraph.addEdge("item" + item, "pos" + pos);
                    double costs = Integer.MAX_VALUE / this.numberOfItems;
                    bipartiteGraph.setEdgeWeight(edge, costs);
                }
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
        this.addEdgesBetweenItemsAndStackPositions(sol, graph, unassignedItems, freePositions);

        return new BipartiteGraph(partitionOne, partitionTwo, graph);
    }

    private double computeLB(Solution sol) {
        BipartiteGraph graph = this.constructBipartiteGraph(sol);

        KuhnMunkresMinimalWeightBipartitePerfectMatching<String, DefaultWeightedEdge> minCostPerfectMatching =
            new KuhnMunkresMinimalWeightBipartitePerfectMatching<>(
                graph.getGraph(), graph.getPartitionOne(), graph.getPartitionTwo()
            )
        ;
        return minCostPerfectMatching.getMatching().getWeight() + sol.computeCosts();
    }

    private void branchAndBound(Solution sol, int itemToBeAdded) {

        for (int stack = 0; stack < sol.getFilledStacks().length; stack++) {

            if (HeuristicUtil.stackHasFreePosition(sol.getFilledStacks()[stack])
                && HeuristicUtil.itemCompatibleWithStack(this.costs, itemToBeAdded, stack)
                && HeuristicUtil.itemCompatibleWithAlreadyAssignedItems(itemToBeAdded, sol.getFilledStacks()[stack], this.itemObjects, this.stackingConstraints)) {

                    Solution tmpSol = new Solution(sol);
                    HeuristicUtil.assignItemToStack(itemToBeAdded, tmpSol.getFilledStacks()[stack], this.itemObjects);

                    if (itemToBeAdded == this.numberOfItems - 1) {
                        if (tmpSol.computeCosts() < this.bestObjectiveValue) {
                            this.bestSol = tmpSol;
                            this.bestObjectiveValue = tmpSol.computeCosts();
                            System.out.println(this.bestObjectiveValue);
                        }
                    } else {
                        double LB = this.computeLB(tmpSol);
                        if (LB < this.bestObjectiveValue) {
                            this.branchAndBound(tmpSol, itemToBeAdded + 1);
                        }
                    }
            }
        }
    }
}

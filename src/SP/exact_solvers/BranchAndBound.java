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

/**
 * Exact branch-and-bound solver to generate feasible solutions to stacking problems.
 * The goal is to minimize the transport costs while respecting all given constraints.
 *
 * @author Tim Bohne
 */
public class BranchAndBound {

    private final Solution initialSolution;
    private final int numberOfItems;
    private final double[][] costs;
    private final int[][] stackingConstraints;
    private final Item[] itemObjects;
    private Solution bestSol;

    /**
     * Constructor
     *
     * @param initialSolution - solution to be used as reference in branch-and-bound procedure
     */
    public BranchAndBound(Solution initialSolution) {
        this.initialSolution = initialSolution;
        this.bestSol = new Solution(this.initialSolution);
        this.numberOfItems = initialSolution.getSolvedInstance().getItemObjects().length;
        this.costs = initialSolution.getSolvedInstance().getCosts();
        this.stackingConstraints = initialSolution.getSolvedInstance().getStackingConstraints();
        this.itemObjects = initialSolution.getSolvedInstance().getItemObjects();
    }

    /**
     * Starts the branch-and-bound procedure for the given instance.
     */
    public void solve() {
        Solution clearSol = new Solution(this.initialSolution);
        this.clearSolution(clearSol);
        this.branchAndBound(clearSol);
        this.bestSol.lowerItemsThatAreStackedInTheAir();
        System.out.println("feasible: " + this.bestSol.isFeasible());
        System.out.println("costs: " + this.bestSol.computeCosts());
    }

    /**
     * Branch-and-Bound procedure that generates exact solutions for stacking problems.
     *
     * @param sol           - currently considered partial solution
     */
    private void branchAndBound(Solution sol) {

        System.out.println(sol.getAssignedItems().size());

        for (int stack = 0; stack < sol.getFilledStacks().length; stack++) {

            if (HeuristicUtil.stackHasFreePosition(sol.getFilledStacks()[stack])
                && HeuristicUtil.itemCompatibleWithStack(this.costs, sol.getAssignedItems().size(), stack)
                && HeuristicUtil.itemCompatibleWithAlreadyAssignedItems(
                    sol.getAssignedItems().size(), sol.getFilledStacks()[stack], this.itemObjects, this.stackingConstraints
            )) {
                Solution tmpSol = new Solution(sol);
                HeuristicUtil.assignItemToStack(tmpSol.getAssignedItems().size(), tmpSol.getFilledStacks()[stack], this.itemObjects);

                if (tmpSol.getAssignedItems().size() == this.numberOfItems - 1) {
                    if (tmpSol.computeCosts() < this.bestSol.computeCosts()) {
                        this.bestSol = tmpSol;
                        System.out.println(this.bestSol.computeCosts());
                    }
                } else {
                    double LB = this.computeLowerBound(tmpSol);
                    if (LB < this.bestSol.computeCosts()) {
                        this.branchAndBound(tmpSol);
                    }
                }
            }
        }
    }

    /**
     * Clears the stacks of the specified solution.
     *
     * @param sol - solution to clear the stacks for
     */
    private void clearSolution(Solution sol) {
        for (int i = 0; i < sol.getFilledStacks().length; i++) {
            for (int j = 0; j < sol.getFilledStacks()[0].length; j++) {
                sol.getFilledStacks()[i][j] = -1;
            }
        }
    }

    /**
     * Adds edges to the bipartite graph that connect unassigned items with compatible free stack positions.
     *
     * @param bipartiteGraph - bipartite graph to add the edges to
     * @param items          - unassigned items to be connected to compatible free positions
     * @param positions      - free positions the items get connected to
     */
    private void addEdgesBetweenItemsAndStackPositions(
        Solution sol, Graph<String, DefaultWeightedEdge> bipartiteGraph, List<Integer> items, List<StackPosition> positions
    ) {
        for (int item : items) {
            for (StackPosition pos : positions) {

                DefaultWeightedEdge edge = bipartiteGraph.addEdge("item" + item, "pos" + pos);
                double costs = this.costs[item][pos.getStackIdx()];

                // Stack incompatibility is realized indirectly via high costs (in costs matrix) to keep the graph
                // complete bipartite. The stacking constraints should also be respected, therefore violated stacking
                // constraints are realized via high costs as well.
                if (!HeuristicUtil.itemCompatibleWithAlreadyAssignedItems(
                    item, sol.getFilledStacks()[pos.getStackIdx()], this.itemObjects, this.stackingConstraints)
                ) {
                    costs = Integer.MAX_VALUE / this.numberOfItems;
                }
                bipartiteGraph.setEdgeWeight(edge, costs);
            }
        }
    }

    /**
     * Constructs the bipartite graph between unassigned items and free stack positions to be
     * used in the lower bound computation.
     *
     * @param sol - partial solution to compute a lower bound for
     * @return bipartite graph between unassigned items and free stack positions
     */
    private BipartiteGraph constructBipartiteGraph(Solution sol) {
        List<Integer> unassignedItems = sol.getUnassignedItems();
        List<StackPosition> freePositions = HeuristicUtil.retrieveEmptyPositions(sol);

        Graph<String, DefaultWeightedEdge> graph = new DefaultUndirectedWeightedGraph<>(DefaultWeightedEdge.class);
        Set<String> partitionOne = new HashSet<>();
        Set<String> partitionTwo = new HashSet<>();

        GraphUtil.addVerticesForUnmatchedItems(unassignedItems, graph, partitionOne);
        GraphUtil.addVerticesForEmptyPositions(freePositions, graph, partitionTwo);
        List<Integer> dummyItems = GraphUtil.introduceDummyVerticesToBipartiteGraph(graph, partitionOne, partitionTwo);

        GraphUtil.addEdgesBetweenDummyItemsAndStackPositions(graph, dummyItems, freePositions);
        this.addEdgesBetweenItemsAndStackPositions(sol, graph, unassignedItems, freePositions);

        return new BipartiteGraph(partitionOne, partitionTwo, graph);
    }

    /**
     * Computes a lower bound for the solution to be used in the branch and bound procedure.
     * A minimum-weight-perfect-matching gets computed between unassigned items and compatible free stack positions.
     * Then a lower bound gets obtained by adding the matching costs to the total costs of all fixed assignments.
     *
     * @param sol - partial solution to compute a lower bound for
     * @return computed lower bound
     */
    private double computeLowerBound(Solution sol) {
        BipartiteGraph graph = this.constructBipartiteGraph(sol);
        KuhnMunkresMinimalWeightBipartitePerfectMatching<String, DefaultWeightedEdge> minCostPerfectMatching =
            new KuhnMunkresMinimalWeightBipartitePerfectMatching<>(graph.getGraph(), graph.getPartitionOne(), graph.getPartitionTwo());
        return minCostPerfectMatching.getMatching().getWeight() + sol.computeCosts();
    }
}

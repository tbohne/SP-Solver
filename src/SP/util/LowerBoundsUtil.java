package SP.util;

import SP.representations.BipartiteGraph;
import SP.representations.Solution;
import SP.representations.StackPosition;
import org.jgrapht.Graph;
import org.jgrapht.alg.matching.KuhnMunkresMinimalWeightBipartitePerfectMatching;
import org.jgrapht.graph.DefaultUndirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Provides lower bound computations for partially generated stacking solutions.
 *
 * @author Tim Bohne
 */
public class LowerBoundsUtil {

    /**
     * Computes a lower bound for the solution to be used in the branch and bound procedure.
     * A minimum-weight-perfect-matching gets computed between unassigned items and compatible free stack positions.
     * Then a lower bound gets obtained by adding the matching costs to the total costs of all fixed assignments.
     * It's a LB, because only stacking constraints between unassigned items and already assigned items are considered
     * and not stacking constraints between items that are going to be assigned.
     *
     * @param sol - partial solution to compute a lower bound for
     * @return computed lower bound
     */
    public static double computeLowerBound(Solution sol) {
        BipartiteGraph graph = constructBipartiteGraph(sol);
        KuhnMunkresMinimalWeightBipartitePerfectMatching<String, DefaultWeightedEdge> minCostPerfectMatching =
            new KuhnMunkresMinimalWeightBipartitePerfectMatching<>(graph.getGraph(), graph.getPartitionOne(), graph.getPartitionTwo());
        return minCostPerfectMatching.getMatching().getWeight() + sol.computeCosts();
    }

    /**
     * Adds edges to the bipartite graph that connect unassigned items with compatible free stack positions.
     *
     * @param sol            - partial solution to compute a lower bound for
     * @param bipartiteGraph - bipartite graph to add edges to
     * @param items          - items to be assigned to stacks
     * @param positions      - positions in stacks the items could be assigned to
     */
    private static void addEdgesBetweenItemsAndStackPositions(
        Solution sol, Graph<String, DefaultWeightedEdge> bipartiteGraph, List<Integer> items, List<StackPosition> positions
    ) {
        for (int item : items) {
            for (StackPosition pos : positions) {

                DefaultWeightedEdge edge = bipartiteGraph.addEdge("item" + item, "pos" + pos);
                double costs = sol.getSolvedInstance().getCosts()[item][pos.getStackIdx()];

                // Stack incompatibility is realized indirectly via high costs (in costs matrix) to keep the graph
                // complete bipartite. The stacking constraints should also be respected, therefore violated stacking
                // constraints are realized via high costs as well.
                if (!HeuristicUtil.itemCompatibleWithAlreadyAssignedItems(
                        item, sol.getFilledStacks()[pos.getStackIdx()], sol.getSolvedInstance().getItemObjects(), sol.getSolvedInstance().getStackingConstraints())
                        ) {
                    costs = Integer.MAX_VALUE / sol.getSolvedInstance().getItems().length;
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
    private static BipartiteGraph constructBipartiteGraph(Solution sol) {
        List<Integer> unassignedItems = sol.getUnassignedItems();
        List<StackPosition> freePositions = HeuristicUtil.retrieveEmptyPositions(sol);

        Graph<String, DefaultWeightedEdge> graph = new DefaultUndirectedWeightedGraph<>(DefaultWeightedEdge.class);
        Set<String> partitionOne = new HashSet<>();
        Set<String> partitionTwo = new HashSet<>();

        GraphUtil.addVerticesForUnmatchedItems(unassignedItems, graph, partitionOne);
        GraphUtil.addVerticesForEmptyPositions(freePositions, graph, partitionTwo);
        List<Integer> dummyItems = GraphUtil.introduceDummyVerticesToBipartiteGraph(graph, partitionOne, partitionTwo);

        GraphUtil.addEdgesBetweenDummyItemsAndStackPositions(graph, dummyItems, freePositions);
        addEdgesBetweenItemsAndStackPositions(sol, graph, unassignedItems, freePositions);

        return new BipartiteGraph(partitionOne, partitionTwo, graph);
    }
}

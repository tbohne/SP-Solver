package SP.experiments;

import SP.representations.BipartiteGraph;
import SP.representations.Instance;
import SP.representations.StackPosition;
import SP.util.GraphUtil;
import org.jgrapht.Graph;
import org.jgrapht.alg.matching.KuhnMunkresMinimalWeightBipartitePerfectMatching;
import org.jgrapht.graph.DefaultUndirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Computes lower bounds for the total costs of instances of stacking problems
 * by relaxing the stacking constraints s_ij.
 *
 * @author Tim Bohne
 */
public class LowerBoundsCalculator {

    private final Instance instance;

    /**
     * Constructor
     *
     * @param instance  - instance of the stacking problem to compute a LB for
     */
    public LowerBoundsCalculator(Instance instance) {
        this.instance = instance;
    }

    /**
     * Computes the LB for the instance of the stacking problem by creating a bipartite graph
     * between items and stack positions. The weight of an edge connecting an item with a position in
     * a stack corresponds to the transport costs for the assignment.
     * A minimum-weight-perfect-matching gets computed on this graph and the weight of the matching is
     * a lower bound on the total costs of the instance.
     *
     * @return computed lower bound
     */
    public double computeLowerBound() {
        BipartiteGraph bipartiteGraph = this.generateBipartiteGraph();
        KuhnMunkresMinimalWeightBipartitePerfectMatching<String, DefaultWeightedEdge> minCostPerfectMatching =
            new KuhnMunkresMinimalWeightBipartitePerfectMatching<>(
                bipartiteGraph.getGraph(), bipartiteGraph.getPartitionOne(), bipartiteGraph.getPartitionTwo()
            )
        ;
        double lowerBound = Math.round(minCostPerfectMatching.getMatching().getWeight() * 100.0) / 100.0;
        System.out.println("LB (with relaxed s_ij): " + lowerBound);
        return lowerBound;
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
                double costs = this.instance.getCosts()[item][pos.getStackIdx()];
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

    /**
     * Generates the bipartite graph between items and stack positions.
     *
     * @return generated bipartite graph
     */
    private BipartiteGraph generateBipartiteGraph() {

        Graph<String, DefaultWeightedEdge> graph = new DefaultUndirectedWeightedGraph<>(DefaultWeightedEdge.class);
        Set<String> partitionOne = new HashSet<>();
        Set<String> partitionTwo = new HashSet<>();

        List<StackPosition> positions = new ArrayList<>();
        for (int stack = 0; stack < this.instance.getStacks().length; stack++) {
            for (int level = 0; level < this.instance.getStacks()[stack].length; level++) {
                positions.add(new StackPosition(stack, level));
            }
        }
        List<Integer> itemList = new ArrayList<>();
        for (int item : this.instance.getItems()) {
            itemList.add(item);
        }

        GraphUtil.addVerticesForUnmatchedItems(itemList, graph, partitionOne);
        GraphUtil.addVerticesForEmptyPositions(positions, graph, partitionTwo);
        List<Integer> dummyItems = GraphUtil.introduceDummyVerticesToBipartiteGraph(graph, partitionOne, partitionTwo);
        this.addEdgesBetweenDummyItemsAndStackPositions(graph, dummyItems, positions);
        this.addEdgesBetweenItemsAndStackPositions(graph, itemList, positions);

        return new BipartiteGraph(partitionOne, partitionTwo, graph);
    }
}

package SP.representations;

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
 * Second solution representation based on item partitions.
 * The item partitions in this representation are not yet associated to stacks.
 *
 * @author Tim Bohne
 */
public class PartitionList {

    private final Solution sol;
    private final List<List<Integer>> partitions;

    /**
     * Constructor
     *
     * @param sol - input solution to get partitions from
     */
    public PartitionList(Solution sol) {
        this.sol = new Solution(sol);
        this.partitions = this.generatePartitions();
    }

    /**
     * Generates the item partition list.
     *
     * @return list of item partitions
     */
    private List<List<Integer>> generatePartitions() {
        List<List<Integer>> partitions = new ArrayList<>();
        for (int[] stack : this.sol.getFilledStacks()) {
            List<Integer> partition = new ArrayList<>();
            for (int item : stack) {
                if (item != -1) {
                    partition.add(item);
                }
            }
            if (!partition.isEmpty()) {
                partitions.add(partition);
            }
        }
        return partitions;
    }

    /**
     * Adds the item partitions as vertices to the specified graph.
     *
     * @param bipartiteGraph - graph the partitions are added to
     * @param partitionOne   - set for first partition of bipartite graph
     */
    private void addVerticesForPartitions(Graph<String, DefaultWeightedEdge> bipartiteGraph, Set<String> partitionOne) {
        for (List<Integer> partition : partitions) {
            bipartiteGraph.addVertex("partition" + partition);
            partitionOne.add("partition" + partition);
        }
    }

    /**
     * Adds the stacks as vertices to the specified graph.
     *
     * @param bipartiteGraph - graph the stacks are added to
     * @param partitionTwo   - set of second partition of bipartite graph
     */
    private void addVerticesForStacks(Graph<String, DefaultWeightedEdge> bipartiteGraph, Set<String> partitionTwo) {
        for (int stack = 0; stack < this.sol.getFilledStacks().length; stack++) {
            bipartiteGraph.addVertex("stack" + stack);
            partitionTwo.add("stack" + stack);
        }
    }

    /**
     * Adds edges between dummy partitions and stacks in the specified graph.
     *
     * @param bipartiteGraph  - graph the edges are added to
     * @param dummyPartitions - list of dummy partitions
     */
    private void addEdgesBetweenDummyPartitionsAndStacks(
        Graph<String, DefaultWeightedEdge> bipartiteGraph, List<Integer> dummyPartitions
    ) {
        for (int dummyPartition : dummyPartitions) {
            for (int stack = 0; stack < this.sol.getFilledStacks().length; stack++) {
                DefaultWeightedEdge edge = bipartiteGraph.addEdge("dummy" + dummyPartition, "stack" + stack);
                bipartiteGraph.setEdgeWeight(edge, 0);
            }
        }
    }

    /**
     * Adds edges between items and stack positions in the specified graph.
     *
     * @param bipartiteGraph - graph the edges are added to
     */
    private void addEdgesBetweenItemsAndStackPositions(Graph<String, DefaultWeightedEdge> bipartiteGraph) {
        // assumed at this point: items inside a partition are compatible
        for (List<Integer> partition : partitions) {
            for (int stack = 0; stack < this.sol.getFilledStacks().length; stack++) {
                DefaultWeightedEdge edge = bipartiteGraph.addEdge("partition" + partition, "stack" + stack);
                double costs = 0;
                for (int item : partition) {
                    costs += sol.getSolvedInstance().getCosts()[item][stack];
                }
                bipartiteGraph.setEdgeWeight(edge, costs);
            }
        }
    }

    /**
     * Constructs the bipartite graph between item partitions and stacks.
     *
     * @return constructed bipartite graph
     */
    private BipartiteGraph constructBipartiteGraph() {
        Graph<String, DefaultWeightedEdge> graph = new DefaultUndirectedWeightedGraph<>(DefaultWeightedEdge.class);
        Set<String> partitionOne = new HashSet<>();
        Set<String> partitionTwo = new HashSet<>();

        this.addVerticesForPartitions(graph, partitionOne);
        this.addVerticesForStacks(graph, partitionTwo);
        // there could be more stacks than partitions
        List<Integer> dummyPartitions = GraphUtil.introduceDummyVerticesToBipartiteGraph(graph, partitionOne, partitionTwo);
        this.addEdgesBetweenDummyPartitionsAndStacks(graph, dummyPartitions);
        this.addEdgesBetweenItemsAndStackPositions(graph);
        return new BipartiteGraph(partitionOne, partitionTwo, graph);
    }

    /**
     * Generates an actual solution from the list of item partitions
     * by assigning the partitions to stacks using a min-cost-perfect-matching.
     *
     * @return generated solution
     */
    private Solution generateSolutionFromPartitions() {

        BipartiteGraph graph = this.constructBipartiteGraph();

        KuhnMunkresMinimalWeightBipartitePerfectMatching<String, DefaultWeightedEdge> minCostPerfectMatching =
            new KuhnMunkresMinimalWeightBipartitePerfectMatching<>(
                    graph.getGraph(), graph.getPartitionOne(), graph.getPartitionTwo()
            );

        Solution newSol = new Solution(this.sol);
        newSol.clearFilledStacks();

        for (Object o : minCostPerfectMatching.getMatching()) {
            if (!o.toString().contains("dummy")) {
                List<Integer> itemList = new ArrayList<>();
                String listOfItems = o.toString().split(":")[0].replace("(partition", "").replace("[", "").replace("]", "").trim();
                // empty partition
                if (listOfItems.isEmpty()) { continue; }
                for (String item : listOfItems.split(",")) {
                    itemList.add(Integer.parseInt(item.trim()));
                }
                int stack = Integer.parseInt(o.toString().split(":")[1].replace("stack", "").replace(")", "").trim());
                int level = 0;
                for (int item : itemList) {
                    newSol.getFilledStacks()[stack][level++] = item;
                }
            }
        }
        return newSol;
    }
}

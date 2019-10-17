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

public class PartitionList {

    private Solution sol;
    private List<List<Integer>> partitions;

    public PartitionList(Solution sol) {
        this.sol = new Solution(sol);
        this.partitions = this.generatePartitions();
    }

    public List<List<Integer>> getPartitions() {
        return this.partitions;
    }

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

    private void addVerticesForPartitions(Graph<String, DefaultWeightedEdge> bipartiteGraph, Set<String> partitionOne) {
        for (List<Integer> partition : partitions) {
            bipartiteGraph.addVertex("partition" + partition);
            partitionOne.add("partition" + partition);
        }
    }

    private void addVerticesForStacks(Graph<String, DefaultWeightedEdge> bipartiteGraph, Set<String> partitionTwo) {
        for (int stack = 0; stack < this.sol.getFilledStacks().length; stack++) {
            bipartiteGraph.addVertex("stack" + stack);
            partitionTwo.add("stack" + stack);
        }
    }

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

    private void addEdgesBetweenItemsAndStackPositions(
            Graph<String, DefaultWeightedEdge> bipartiteGraph
    ) {

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

    private Solution generateSolutionFromPartitions() {

        // generate nbr solution (minCostBipartitePM)
        // construct bipartite graph between partitions and stacks

        BipartiteGraph graph = this.constructBipartiteGraph();

        KuhnMunkresMinimalWeightBipartitePerfectMatching<String, DefaultWeightedEdge> minCostPerfectMatching =
                new KuhnMunkresMinimalWeightBipartitePerfectMatching<>(graph.getGraph(), graph.getPartitionOne(), graph.getPartitionTwo());

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

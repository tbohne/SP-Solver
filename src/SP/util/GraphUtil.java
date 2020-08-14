package SP.util;

import SP.representations.*;
import org.jgrapht.Graph;
import org.jgrapht.alg.matching.EdmondsMaximumCardinalityMatching;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultUndirectedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.util.*;

/**
 * A collection of graph related utility methods used in the heuristics.
 * There are three main categories of methods: graph modification, graph generation and parsing.
 *
 * @author Tim Bohne
 */
public class GraphUtil {

    /**********************************************************************************/
    /*************************** GRAPH MODIFICATION METHODS ***************************/
    /**********************************************************************************/

    /**
     * Adds edges between item pairs and stacks in the given bipartite graph consisting of the items in
     * one partition and stacks in the other one. Such an edge indicates that the item pair is assignable
     * to the stack. Each pair is assignable to every stack initially. Forbidden assignments are indirectly
     * avoided by the minimum weight perfect matching through high edge costs.
     *
     * @param bipartiteGraph - bipartite graph consisting of stacks and items
     * @param itemPairs      - pairs of items to be connected to the stacks
     * @param stacks         - stacks the item pairs are going to be connected to
     * @param costMatrix     - matrix containing the costs for item-stack-assignments
     */
    public static void addEdgesBetweenItemPairsAndStacks(
        Graph<String, DefaultWeightedEdge> bipartiteGraph, List<MCMEdge> itemPairs, int[][] stacks, double[][] costMatrix
    ) {
        for (MCMEdge itemPair : itemPairs) {
            for (int stack = 0; stack < stacks.length; stack++) {
                DefaultWeightedEdge edge = bipartiteGraph.addEdge("pair" + itemPair, "stack" + stack);
                double costs = costMatrix[itemPair.getVertexOne()][stack] + costMatrix[itemPair.getVertexTwo()][stack];
                bipartiteGraph.setEdgeWeight(edge, costs);
            }
        }
    }

    /**
     * Adds edges between item triples and stacks in the given bipartite graph consisting of the items in
     * one partition and stacks in the other one. Such an edge indicates that the item triple is assignable
     * to the stack. Each triple is assignable to every stack initially. Forbidden assignments are indirectly
     * avoided by the minimum weight perfect matching through high edge costs.
     *
     * @param bipartiteGraph - bipartite graph consisting of stacks and items
     * @param itemTriples    - triples of items to be connected to the stacks
     * @param stacks         - stacks the item triples are going to be connected to
     * @param costMatrix     - matrix containing the costs for item-stack-assignments
     */
    public static void addEdgesBetweenItemTriplesAndStacks(
        Graph<String, DefaultWeightedEdge> bipartiteGraph, List<List<Integer>> itemTriples, int[][] stacks, double[][] costMatrix
    ) {
        for (List<Integer> itemTriple : itemTriples) {
            for (int stack = 0; stack < stacks.length; stack++) {
                DefaultWeightedEdge edge = bipartiteGraph.addEdge("triple" + itemTriple, "stack" + stack);
                double costs = costMatrix[itemTriple.get(0)][stack]
                    + costMatrix[itemTriple.get(1)][stack]
                    + costMatrix[itemTriple.get(2)][stack];
                bipartiteGraph.setEdgeWeight(edge, costs);
            }
        }
    }

    /**
     * Adds edges between dummy items and stacks in the given bipartite graph consisting of the items in
     * one partition and stacks in the other one. Such an edge indicates that the dummy item is assignable
     * to the stack. Each dummy is assignable to every stack at costs 0.
     *
     * @param bipartiteGraph - bipartite graph consisting of stacks and items
     * @param dummyItems     - dummy items to be connected to the stacks
     * @param stacks         - stacks the dummy items are going to be connected to
     */
    public static void addEdgesBetweenDummyItemsAndStacks(
        Graph<String, DefaultWeightedEdge> bipartiteGraph, List<Integer> dummyItems, int[][] stacks
    ) {
        for (int item : dummyItems) {
            for (int stack = 0; stack < stacks.length; stack++) {
                DefaultWeightedEdge edge = bipartiteGraph.addEdge("dummy" + item, "stack" + stack);
                bipartiteGraph.setEdgeWeight(edge, 0);
            }
        }
    }

    /**
     * Adds edges between unmatched items and stacks in the given bipartite graph consisting of the items in
     * one partition and stacks in the other one. Such an edge indicates that the item is assignable
     * to the stack. Each item is assignable to every stack initially. Forbidden assignments are indirectly
     * avoided by the minimum weight perfect matching through high edge costs.
     *
     * @param bipartiteGraph - bipartite graph consisting of stacks and items
     * @param unmatchedItems - unmatched items to be connected to the stacks
     * @param stacks         - stacks the items are going to be connected to
     * @param costMatrix     - matrix containing the costs for item-stack-assignments
     */
    public static void addEdgesBetweenUnmatchedItemsAndStacks(
        Graph<String, DefaultWeightedEdge> bipartiteGraph, List<Integer> unmatchedItems, int[][] stacks, double[][] costMatrix
    ) {
        for (int item : unmatchedItems) {
            for (int stack = 0; stack < stacks.length; stack++) {
                DefaultWeightedEdge edge = bipartiteGraph.addEdge("item" + item, "stack" + stack);
                double costs = costMatrix[item][stack];
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
    public static void addEdgesBetweenDummyItemsAndStackPositions(
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
     * Introduces dummy vertices to the given bipartite graph.
     *
     * @param bipartiteGraph - given bipartite graph consisting of items and stacks
     * @param partitionOne   - bipartite graph's first partition
     * @param partitionTwo   - bipartite graph's second partition
     * @return created list of dummy items corresponding to the dummy vertices
     */
    public static List<Integer> introduceDummyVerticesToBipartiteGraph(
        Graph<String, DefaultWeightedEdge> bipartiteGraph, Set<String> partitionOne, Set<String> partitionTwo
    ) {
        int cnt = 0;
        List<Integer> dummyItems = new ArrayList<>();
        while (partitionOne.size() < partitionTwo.size()) {
            bipartiteGraph.addVertex("dummy" + cnt);
            partitionOne.add("dummy" + cnt);
            dummyItems.add(cnt++);
        }
        return dummyItems;
    }

    /**
     * Adds an edge between the specified pair and unmatched item to the given graph.
     *
     * @param graph         - graph the edge is added to
     * @param unmatchedItem - item to be connected to pair
     * @param itemPair      - pair the item gets connected to
     */
    private static void addEdgeBetweenPairAndUnmatchedItem(
        Graph<String, DefaultEdge> graph, int unmatchedItem, MCMEdge itemPair
    ) {
        if (!graph.containsEdge("v" + unmatchedItem, "edge" + itemPair)) {
            graph.addEdge("edge" + itemPair, "v" + unmatchedItem);
        }
    }

    /**
     * Adds an edge to the graph if the given unmatched item can be assigned
     * to the given pair to build up a valid item triple.
     *
     * @param graph               - graph to be extended
     * @param lowerItemOfPair     - lower item of the pair
     * @param unmatchedItem       - unmatched item to be assigned to the pair
     * @param upperItemOfPair     - upper item of the pair
     * @param itemPair            - edge that represents the pair
     * @param stackingConstraints - stacking constraints to be respected
     */
    private static void addEdgeForCompatibleItemTriple(
        Graph<String, DefaultEdge> graph, int lowerItemOfPair, int unmatchedItem, int upperItemOfPair,
        MCMEdge itemPair, int[][] stackingConstraints, boolean pairStackableInBothDirections
    ) {
        // all permutations to be tested
        if (pairStackableInBothDirections) {
            if (HeuristicUtil.itemAssignableToPairStackableInBothDirections(
                stackingConstraints, lowerItemOfPair, upperItemOfPair, unmatchedItem
            )) {
                GraphUtil.addEdgeBetweenPairAndUnmatchedItem(graph, unmatchedItem, itemPair);
            }
        } else {
            if (HeuristicUtil.itemAssignableToPairStackableInOneDirection(
                stackingConstraints, lowerItemOfPair, upperItemOfPair, unmatchedItem
            )) {
                GraphUtil.addEdgeBetweenPairAndUnmatchedItem(graph, unmatchedItem, itemPair);
            }
        }
    }

    /**
     * Adds an edge to the post processing graph connecting an item with a compatible empty position.
     *
     * @param postProcessingGraph - graph the edges are added to
     * @param item                - item to be connected to compatible empty position
     * @param emptyPos            - empty position the item gets connected to
     * @param originalCosts       - current costs for each item assignment
     * @param instance            - instance of the stacking problem
     */
    public static void addEdgeToPostProcessingGraph(
        Graph<String, DefaultWeightedEdge> postProcessingGraph, int item, StackPosition emptyPos,
        Map<Integer, Double> originalCosts, Instance instance
    ) {
        DefaultWeightedEdge edge = postProcessingGraph.addEdge("item" + item, "pos" + emptyPos);
        double savings = HeuristicUtil.getSavingsForItem(emptyPos.getStackIdx(), originalCosts, item, instance.getCosts());
        postProcessingGraph.setEdgeWeight(edge, savings);
    }

    /**
     * Adds item triples as vertices to the given bipartite graph.
     *
     * @param itemTriples    - item triples to be added as vertices
     * @param bipartiteGraph - bipartite graph to be extended
     * @param partitionOne   - partition the vertices are added to
     */
    public static void addVerticesForItemTriples(
        List<List<Integer>> itemTriples, Graph<String, DefaultWeightedEdge> bipartiteGraph, Set<String> partitionOne
    ) {
        for (List<Integer> triple : itemTriples) {
            bipartiteGraph.addVertex("triple" + triple);
            partitionOne.add("triple" + triple);
        }
    }

    /**
     * Adds item pairs as vertices to the given bipartite graph.
     *
     * @param itemPairs      - item pairs to be added as vertices
     * @param bipartiteGraph - bipartite graph to be extended
     * @param partitionOne   - partition the vertices are added to
     */
    public static void addVerticesForItemPairs(
        List<MCMEdge> itemPairs, Graph<String, DefaultWeightedEdge> bipartiteGraph, Set<String> partitionOne
    ) {
        for (MCMEdge pair : itemPairs) {
            bipartiteGraph.addVertex("pair" + pair);
            partitionOne.add("pair" + pair);
        }
    }

    /**
     * Adds unmatched items as vertices to the given bipartite graph.
     *
     * @param unmatchedItems - unmatched items to be added as vertices
     * @param bipartiteGraph - bipartite graph to be extended
     * @param partitionOne   - partition the vertices are added to
     */
    public static void addVerticesForUnmatchedItems(
        List<Integer> unmatchedItems, Graph<String, DefaultWeightedEdge> bipartiteGraph, Set<String> partitionOne
    ) {
        for (int item : unmatchedItems) {
            bipartiteGraph.addVertex("item" + item);
            partitionOne.add("item" + item);
        }
    }

    /**
     * Adds the stacks as vertices to the given bipartite graph.
     *
     * @param stacks         - stacks to be added as vertices
     * @param bipartiteGraph - bipartite graph to be extended
     * @param partitionTwo   - partition the vertices are added to
     */
    public static void addVerticesForStacks(
        int[][] stacks, Graph<String, DefaultWeightedEdge> bipartiteGraph, Set<String> partitionTwo
    ) {
        for (int stack = 0; stack < stacks.length; stack++) {
            bipartiteGraph.addVertex("stack" + stack);
            partitionTwo.add("stack" + stack);
        }
    }

    /**
     * Adds the empty positions as vertices to the given bipartite graph.
     *
     * @param emptyPositions - empty positions to be added as vertices
     * @param graph          - graph the vertices are added to
     * @param partitionTwo   - partition of the bipartite graph the vertices are added to
     */
    public static void addVerticesForEmptyPositions(
        List<StackPosition> emptyPositions, Graph<String, DefaultWeightedEdge> graph, Set<String> partitionTwo
    ) {
        for (StackPosition pos : emptyPositions) {
            graph.addVertex("pos" + pos);
            partitionTwo.add("pos" + pos);
        }
    }

    /********************************************************************************/
    /*************************** GRAPH GENERATION METHODS ***************************/
    /********************************************************************************/

    /**
     * Generates the bipartite graph containing pairs of items in
     * one partition and unmatched items in the other one.
     *
     * @param itemPairs           - list of item pairs
     * @param unmatchedItems      - list of unmatched items
     * @param stackingConstraints - stacking constraints to be respected
     * @return generated bipartite graph
     */
    public static Graph<String, DefaultEdge> generateBipartiteGraphBetweenPairsOfItemsAndUnmatchedItems(
        List<MCMEdge> itemPairs, List<Integer> unmatchedItems, int[][] stackingConstraints
    ) {
        Graph<String, DefaultEdge> bipartiteGraph = new DefaultUndirectedGraph<>(DefaultEdge.class);

        // adding the item pairs as nodes to the graph
        for (MCMEdge itemPair : itemPairs) {
            bipartiteGraph.addVertex("edge" + itemPair);
        }
        // adding the unmatched items as nodes to the graph
        for (int i : unmatchedItems) {
            bipartiteGraph.addVertex("v" + i);
        }

        for (MCMEdge itemPair : itemPairs) {
            for (Integer unmatchedItem : unmatchedItems) {

                int itemOne = itemPair.getVertexOne();
                int itemTwo = itemPair.getVertexTwo();

                // if it is possible to complete the stack assignment with the unmatched item, it is done

                if (HeuristicUtil.pairStackableInBothDirections(itemOne, itemTwo, stackingConstraints)) {
                    GraphUtil.addEdgeForCompatibleItemTriple(
                        bipartiteGraph, itemTwo, unmatchedItem, itemOne, itemPair, stackingConstraints, true
                    );
                // one on top of two
                } else if (stackingConstraints[itemOne][itemTwo] == 1) {
                    GraphUtil.addEdgeForCompatibleItemTriple(
                        bipartiteGraph, itemTwo, unmatchedItem, itemOne, itemPair, stackingConstraints, false
                    );
                // two on top of one
                } else if (stackingConstraints[itemTwo][itemOne] == 1) {
                    GraphUtil.addEdgeForCompatibleItemTriple(
                        bipartiteGraph, itemOne, unmatchedItem, itemTwo, itemPair, stackingConstraints, false
                    );
                }
            }
        }
        return bipartiteGraph;
    }

    /**
     * Generates the stacking constraint graph for the items.
     * There exists an edge between two items if the items are stackable in
     * at least one direction and if they can be assigned to at least one stack together.
     *
     * @param items               - list of items (vertices of the graph)
     * @param stackingConstraints - stacking constraints to be respected
     * @param costMatrix          - matrix containing the costs for item-stack-assignments
     * @param invalidEdgeCosts    - cost value used to implement the placement constraints
     * @param stacks              - given stacks in the storage area
     * @return generated stacking constraint graph
     */
    public static Graph<String, DefaultEdge> generateStackingConstraintGraph(
        int[] items, int[][] stackingConstraints, double[][] costMatrix, int invalidEdgeCosts, int[][] stacks
    ) {
        Graph<String, DefaultEdge> stackingConstraintGraph = new DefaultUndirectedGraph<>(DefaultEdge.class);
        List<Integer> itemList = new ArrayList<>();
        for (int item : items) {
            itemList.add(item);
            stackingConstraintGraph.addVertex("v" + item);
        }

        for (int i = 0; i < stackingConstraints.length; i++) {
            for (int j = 0; j < stackingConstraints[0].length; j++) {

                // For all incoming items i and j, there is an edge if s_ij + s_ji >= 1
                // and if they have at least one compatible stack.
                if (i != j && stackingConstraints[i][j] == 1 || stackingConstraints[j][i] == 1) {
                    int numberOfCompatibleStacks = 0;
                    for (int stackIdx = 0; stackIdx < stacks.length; stackIdx++) {
                        if (costMatrix[i][stackIdx] < invalidEdgeCosts && costMatrix[j][stackIdx] < invalidEdgeCosts) {
                            numberOfCompatibleStacks++;
                        }
                    }
                    if (numberOfCompatibleStacks > 0) {
                        if (itemList.contains(i) && itemList.contains(j)) {
                            if (!stackingConstraintGraph.containsEdge("v" + j, "v" + i)) {
                                stackingConstraintGraph.addEdge("v" + i, "v" + j);
                            }
                        }
                    }
                }
            }
        }
        return stackingConstraintGraph;
    }

    /***********************************************************************/
    /*************************** PARSING METHODS ***************************/
    /***********************************************************************/

    /**
     * Parses item pairs from the given maximum cardinality matching.
     *
     * @param mcm - the given maximum cardinality matching
     * @return the list of matched items (edges)
     */
    public static ArrayList<MCMEdge> parseItemPairsFromMCM(EdmondsMaximumCardinalityMatching mcm) {
        ArrayList<MCMEdge> matchedItems = new ArrayList<>();
        for (Object edge : mcm.getMatching()) {
            int vertexOne = Integer.parseInt(edge.toString().split(":")[0].replace("(v", "").trim());
            int vertexTwo = Integer.parseInt(edge.toString().split(":")[1].replace("v", "").replace(")", "").trim());
            MCMEdge e = new MCMEdge(vertexOne, vertexTwo, 0);
            matchedItems.add(e);
        }
        return matchedItems;
    }

    /**
     * Parses item triples from the given maximum cardinality matching.
     *
     * @param mcm - maximum cardinality matching to be parsed
     * @return parsed item triples
     */
    public static List<List<Integer>> parseItemTriplesFromMCM(EdmondsMaximumCardinalityMatching mcm) {
        List<List<Integer>> itemTriples = new ArrayList<>();
        for (Object edge : mcm.getMatching().getEdges()) {
            String parsedEdge = edge.toString().replace("(edge(", "").replace(") : v", ", ").replace(")", "").trim();
            int first = Integer.parseInt(parsedEdge.split(",")[0].trim());
            int second = Integer.parseInt(parsedEdge.split(",")[1].trim());
            int third = Integer.parseInt(parsedEdge.split(",")[2].trim());

            List<Integer> currAssignment = new ArrayList<>();
            currAssignment.add(first);
            currAssignment.add(second);
            currAssignment.add(third);
            itemTriples.add(new ArrayList<>(currAssignment));
        }
        return itemTriples;
    }

    /**
     * Parses the stacks from the given edge.
     *
     * @param edge - edge to parse stack from
     * @return parsed stack
     */
    public static int parseStack(DefaultWeightedEdge edge) {
        return Integer.parseInt(edge.toString().split(":")[1].replace("stack", "").replace(")", "").trim());
    }

    /**
     * Parses the item from the given edge.
     *
     * @param edge - edge to parse item from
     * @return parsed item
     */
    public static int parseItem(DefaultWeightedEdge edge) {
        return Integer.parseInt(edge.toString().split(":")[0].replace("(item", "").trim());
    }

    /**
     * Parses first item of triple from edge.
     *
     * @param edge - edge to parse first item from
     * @return parsed item
     */
    public static int parseItemOneOfTriple(DefaultWeightedEdge edge) {
        return Integer.parseInt(edge.toString().split(":")[0].split(",")[0].replace("(triple[", "").trim());
    }

    /**
     * Parses second item of triple from edge.
     *
     * @param edge - edge to parse second item from
     * @return parsed item
     */
    public static int parseItemTwoOfTriple(DefaultWeightedEdge edge) {
        return Integer.parseInt(edge.toString().split(":")[0].split(",")[1].trim());
    }

    /**
     * Parses third item of triple from edge.
     *
     * @param edge - edge to parse third item from
     * @return parsed item
     */
    public static int parseItemThreeOfTriple(DefaultWeightedEdge edge) {
        return Integer.parseInt(edge.toString().split(":")[0].split(",")[2].replace("]", "").trim());
    }

    /**
     * Parses first item of pair from edge.
     *
     * @param edge - edge to parse first item from
     * @return parsed item
     */
    public static int parseItemOneOfPair(DefaultWeightedEdge edge) {
        return Integer.parseInt(edge.toString().split(":")[0].replace("(pair", "").split(",")[0].replace("(", "").trim());
    }

    /**
     * Parses second item of pair from edge.
     *
     * @param edge - edge to parse second item from
     * @return parsed item
     */
    public static int parseItemTwoOfPair(DefaultWeightedEdge edge) {
        return Integer.parseInt(edge.toString().split(":")[0].replace("(pair", "").split(",")[1].replace(")", "").trim());
    }

    /**
     * Parses the stack the triple gets assigned to from the given edge.
     *
     * @param edge - edge to parse the stack from
     * @return parsed stack
     */
    public static int parseStackForTriple(DefaultWeightedEdge edge) {
        return Integer.parseInt(edge.toString().split(":")[1].replace("stack", "").replace(")", "").trim());
    }

    /**
     * Parses the stack the pair gets assigned to from the given edge.
     *
     * @param edge - edge to parse the stack from
     * @return parsed stack
     */
    public static int parseStackForPair(DefaultWeightedEdge edge) {
        return Integer.parseInt(edge.toString().split(":")[1].replace("stack", "").replace(")", "").trim());
    }

    /**
     * Parses the item from the edge of the post processing graph.
     *
     * @param edge - edge to be parsed
     * @return parsed item
     */
    public static int parseItemFromPostProcessingMatching(DefaultWeightedEdge edge) {
        return Integer.parseInt(edge.toString().split(":")[0].replace("(", "").replace("item", "").trim());
    }

    /**
     * Parses the stack from the edge of the post processing graph.
     *
     * @param edge - edge to be parsed
     * @return parsed stack
     */
    public static int parseStackFromPostProcessingMatching(DefaultWeightedEdge edge) {
        return Integer.parseInt(edge.toString().split(":")[2].split(",")[0].trim());
    }

    /**
     * Parses the level from the edge of the post processing graph.
     *
     * @param edge - edge to be parsed
     * @return parsed level
     */
    public static int parseLevelFromPostProcessingMatching(DefaultWeightedEdge edge) {
        return Integer.parseInt(edge.toString().split(":")[3].replace(")", "").trim());
    }
}

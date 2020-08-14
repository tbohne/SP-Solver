package SP.post_optimization_methods.neighborhood_operators;

import SP.representations.PendingItemStackAssignment;
import SP.representations.EjectionChainPath;
import SP.representations.Solution;
import SP.util.HeuristicUtil;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.BellmanFordShortestPath;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.traverse.TopologicalOrderIterator;

import java.util.*;

/**
 * Operator to be used in neighborhood structures for the local search.
 * An ejection chain is generally a list of combined shift operations.
 *
 * @author Tim Bohne
 */
public class EjectionChainOperator {

    /**
     * Constructor
     */
    public EjectionChainOperator() {}

    /**
     * Generates a neighbor for the specified solution by applying the ejection chain operator.
     *
     * @param currSol - current solution to generate neighbor for
     * @return generated neighboring solution
     */
    public Solution generateEjectionChainNeighbor(Solution currSol) {
        GraphPath bestPath = constructEjectionChain(currSol);
        Solution neighbor = new Solution(currSol);
        if (bestPath == null) { return neighbor; }
        applyEjectionChain(neighbor, bestPath);
        neighbor.lowerItemsThatAreStackedInTheAir();
        neighbor.sortItemsInStacksBasedOnTransitiveStackingConstraints();
        return neighbor;
    }

    /**
     * Returns a random order for the stacks of the specified solution.
     *
     * @param currSol - solution to retrieve a random stack order for
     * @return random stack order
     */
    private List<Integer> getRandomStackOrder(Solution currSol) {
        List<Integer> stackOrder = new ArrayList<>();
        for (int stack = 0; stack < currSol.getFilledStacks().length; stack++) {
            stackOrder.add(stack);
        }
        Collections.shuffle(stackOrder);
        return stackOrder;
    }

    /**
     * Adds the specified number of items as vertices to the graph.
     *
     * @param graph         - graph the vertices are added to
     * @param numberOfItems - number of items to be added
     */
    private void addVerticesForItems(Graph<String, DefaultWeightedEdge> graph, int numberOfItems) {
        for (int item = 0; item < numberOfItems; item++) {
            graph.addVertex("item" + item);
        }
    }

    /**
     * Adds not completely filled stacks from the specified solution as vertices to the graph.
     *
     * @param graph          - graph the vertices are added to
     * @param numberOfStacks - overall number of stacks
     * @param currSol        - current solution
     */
    private void addVerticesForStacks(Graph<String, DefaultWeightedEdge> graph, int numberOfStacks, Solution currSol) {
        for (int stack = 0; stack < numberOfStacks; stack++) {
            if (!HeuristicUtil.completelyFilledStack(currSol.getFilledStacks()[stack])) {
                graph.addVertex("stack" + stack);
            }
        }
    }

    /**
     * Retrieves the current total costs of the item assignments for the specified stack.
     *
     * @param currSol - current solution
     * @param stack   - stack to compute costs for
     * @return computed costs for given stack
     */
    private double getCostsForStack(Solution currSol, int stack) {
        double costs = 0.0;
        for (int item : currSol.getFilledStacks()[stack]) {
            if (item != -1) {
                costs += currSol.getSolvedInstance().getCosts()[item][stack];
            }
        }
        return costs;
    }

    /**
     * Adds an edge between two items to the given graph.
     *
     * @param graph   - graph to add the edge to
     * @param currSol - current solution
     * @param itemOne - first item to be connected
     * @param itemTwo - second item to be connected
     */
    private void addEdgeBetweenItems(
        DefaultDirectedWeightedGraph<String, DefaultWeightedEdge> graph, Solution currSol, int itemOne, int itemTwo
    ) {

        if (!graph.containsEdge("item" + itemOne, "item" + itemTwo)) {

            int stackIdxItemTwo = currSol.getStackIdxForAssignedItem(itemTwo);

            if (HeuristicUtil.itemCompatibleWithAlreadyAssignedItemsWithException(
                itemOne, currSol.getFilledStacks()[stackIdxItemTwo], currSol.getSolvedInstance().getItemObjects(),
                currSol.getSolvedInstance().getStackingConstraints(), itemTwo
            )) {

                if (HeuristicUtil.itemCompatibleWithStack(currSol.getSolvedInstance().getCosts(), itemOne, stackIdxItemTwo)) {
                    DefaultWeightedEdge edge = graph.addEdge("item" + itemOne, "item" + itemTwo);
                    double currCosts = getCostsForStack(currSol, stackIdxItemTwo);
                    double costsItemTwo = currSol.getSolvedInstance().getCosts()[itemTwo][stackIdxItemTwo];
                    double costsItemOne = currSol.getSolvedInstance().getCosts()[itemOne][stackIdxItemTwo];
                    double costsAfter = currCosts - costsItemTwo + costsItemOne;
                    // cost reduction when positive
                    double costDiff = currCosts - costsAfter;
                    // negating edge costs to obtain longest path using Bellman-Ford
                    costDiff *= -1;
                    graph.setEdgeWeight(edge, costDiff);
                }
            }
        }
    }

    /**
     * Adds an edge from an item to a stack to the given graph.
     *
     * @param graph   - graph to add the edge to
     * @param currSol - current solution
     * @param item    - item to be connected with the stack
     * @param stack   - stack the item gets connected to
     */
    private void addEdgeFromItemToStack(
        DefaultDirectedWeightedGraph<String, DefaultWeightedEdge> graph, Solution currSol, int item, int stack
    ) {

        if (graph.containsVertex("stack" + stack) && graph.containsVertex("item" + item)) {

            if (HeuristicUtil.itemCompatibleWithStack(currSol.getSolvedInstance().getCosts(), item, stack)) {

                if (HeuristicUtil.itemCompatibleWithAlreadyAssignedItems(item, currSol.getFilledStacks()[stack],
                    currSol.getSolvedInstance().getItemObjects(), currSol.getSolvedInstance().getStackingConstraints())
                ) {
                    DefaultWeightedEdge edge = graph.addEdge("item" + item, "stack" + stack);
                    double currCosts = getCostsForStack(currSol, stack);
                    double afterInsertion = currCosts + currSol.getSolvedInstance().getCosts()[item][stack];
                    // cost reduction when positive
                    double costDiff = currCosts - afterInsertion;
                    // negating edge costs to obtain longest path using Bellman-Ford
                    costDiff *= -1;
                    graph.setEdgeWeight(edge, costDiff);
                }
            }
        }
    }

    /**
     * Adds an edge from a stack to an item to the given graph.
     *
     * @param graph   - graph to add the edge to
     * @param currSol - current solution
     * @param stack   - stack the item gets connected to
     * @param item    - item to be connected with the stack
     */
    private void addEdgeFromStackToItem(
        DefaultDirectedWeightedGraph<String, DefaultWeightedEdge> graph, Solution currSol, int stack, int item
    ) {
        if (graph.containsVertex("stack" + stack) && graph.containsVertex("item" + item)) {
            DefaultWeightedEdge edge = graph.addEdge("stack" + stack, "item" + item);
            int itemStack = currSol.getStackIdxForAssignedItem(item);
            double currCosts = getCostsForStack(currSol, itemStack);
            double afterReduction = currCosts - currSol.getSolvedInstance().getCosts()[item][itemStack];
            // cost reduction when positive
            double costDiff = currCosts - afterReduction;
            // negating edge costs to obtain longest path using Bellman-Ford
            costDiff *= -1;
            graph.setEdgeWeight(edge, costDiff);
        }
    }

    /**
     * Adds edges to the specified graph building up the auxiliary graph to be used
     * to generate ejection chains.
     *
     * @param graph         - graph to add the edges to
     * @param stackOrder    - stack order to be used (only shifts from left tor right)
     * @param numberOfItems - number of items of the instance to be solved
     * @param currSol       - current solution
     */
    private void addEdges(
        DefaultDirectedWeightedGraph<String, DefaultWeightedEdge> graph, List<Integer> stackOrder, int numberOfItems, Solution currSol
    ) {

        for (int i = 0; i < numberOfItems; i++) {
            for (int j = 0; j < numberOfItems; j++) {

                if (i == j || !graph.containsVertex("item" + i) || !graph.containsVertex("item" + j)) { continue; }

                int stackIdxItemOne = currSol.getStackIdxForAssignedItem(i);
                int stackIdxItemTwo = currSol.getStackIdxForAssignedItem(j);
                // don't shift in same stack
                if (stackIdxItemOne == stackIdxItemTwo) { continue; }

                if (stackOrder.indexOf(stackIdxItemOne) < stackOrder.indexOf(stackIdxItemTwo)) {
                    addEdgeBetweenItems(graph, currSol, i, j);
                } else {
                    addEdgeBetweenItems(graph, currSol, j, i);
                }
            }
            for (int stack = 0; stack < stackOrder.size(); stack++) {

                int stackIdxItem = currSol.getStackIdxForAssignedItem(i);
                if (stackIdxItem == stack) { continue; }

                if (stackOrder.indexOf(stackIdxItem) < stackOrder.indexOf(stack)) {
                    addEdgeFromItemToStack(graph, currSol, i, stack);
                } else {
                    addEdgeFromStackToItem(graph, currSol, stack, i);
                }
            }
        }
        for (int item = 0; item < numberOfItems; item++) {
            DefaultWeightedEdge edge = graph.addEdge("source", "item" + item);
            int itemStack = currSol.getStackIdxForAssignedItem(item);
            double currCosts = getCostsForStack(currSol, itemStack);
            double afterReduction = currCosts - currSol.getSolvedInstance().getCosts()[item][itemStack];
            // cost reduction when positive
            double costDiff = currCosts - afterReduction;
            // negating edge costs to obtain longest path using Bellman-Ford
            costDiff *= -1;
            graph.setEdgeWeight(edge, costDiff);
        }
    }

    /**
     * Applies source to item edge which means that the item gets removed from its original stack.
     *
     * @param rhs     - right hand side of the edge string
     * @param currSol - current solution
     */
    private void applySourceToItemEdge(String rhs, Solution currSol) {
        int item = Integer.parseInt(rhs.replace("item", "").replace(")", "").trim());
        int stackOfItem = currSol.getStackIdxForAssignedItem(item);
        HeuristicUtil.removeItemFromStack(item, currSol.getFilledStacks()[stackOfItem]);
    }

    /**
     * Applies item to item edge which means that both items are removed from their original stacks
     * and item one gets assigned to the stack of item two.
     *
     * @param lhs     - left hand side of the edge string
     * @param rhs     - right hand side of the edge string
     * @param currSol - current solution
     */
    private void applyItemToItemEdge(String lhs, String rhs, Solution currSol) {
        int itemOne = Integer.parseInt(lhs.replace("(item", "").trim());
        int itemTwo = Integer.parseInt(rhs.replace("item", "").replace(")", "").trim());
        int stackOfItemOne = currSol.getStackIdxForAssignedItem(itemOne);
        int stackOfItemTwo = currSol.getStackIdxForAssignedItem(itemTwo);
        if (stackOfItemOne != -1) {
            HeuristicUtil.removeItemFromStack(itemOne, currSol.getFilledStacks()[stackOfItemOne]);
        }
        if (stackOfItemTwo != -1) {
            HeuristicUtil.removeItemFromStack(itemTwo, currSol.getFilledStacks()[stackOfItemTwo]);
        }
        HeuristicUtil.assignItemToStack(itemOne, currSol.getFilledStacks()[stackOfItemTwo], currSol.getSolvedInstance().getItemObjects());
    }

    /**
     * Applies item to stack edge which means that the item gets shifted to the stack.
     *
     * @param lhs     - left hand side of the edge string
     * @param rhs     - right hand side of the edge string
     * @param currSol - current solution
     * @param pending - pending stack assignment
     * @return pending stack assignment (may be same as before)
     */
    private PendingItemStackAssignment applyItemToStackEdge(String lhs, String rhs, Solution currSol, PendingItemStackAssignment pending) {
        int item = Integer.parseInt(lhs.replace("(item", "").trim());
        int stack = Integer.parseInt(rhs.replace("stack", "").replace(")", "").trim());
        int stackOfItem = currSol.getStackIdxForAssignedItem(item);
        if (stackOfItem != -1) {
            HeuristicUtil.removeItemFromStack(item, currSol.getFilledStacks()[stackOfItem]);
        }
        if (HeuristicUtil.completelyFilledStack(currSol.getFilledStacks()[stack])) {
            return new PendingItemStackAssignment(item, stack);
        } else {
            HeuristicUtil.assignItemToStack(item, currSol.getFilledStacks()[stack], currSol.getSolvedInstance().getItemObjects());
        }
        return pending;
    }

    /**
     * Applies stack to item edge which means that the item gets removed from the stack.
     *
     * @param lhs     - left hand side of edge string
     * @param rhs     - right hand side of edge string
     * @param currSol - current solution
     * @param pending - pending stack assignment
     * @return pending stack assignment (may be same as before)
     */
    private PendingItemStackAssignment applyStackToItemEdge(
        String lhs, String rhs, Solution currSol, PendingItemStackAssignment pending
    ) {
        int stack = Integer.parseInt(lhs.replace("(stack", "").trim());
        int item = Integer.parseInt(rhs.replace("item", "").replace(")", "").trim());
        HeuristicUtil.removeItemFromStack(item, currSol.getFilledStacks()[stack]);

        if (pending != null && stack == pending.getStack()) {
            HeuristicUtil.assignItemToStack(pending.getItem(), currSol.getFilledStacks()[stack], currSol.getSolvedInstance().getItemObjects());
            return null;
        }
        return pending;
    }

    /**
     * Applies the generated ejection chain (combination of item insertions and removals).
     *
     * @param currSol -  current solution
     * @param path    - ejection chain
     */
    private void applyEjectionChain(Solution currSol, GraphPath path) {

        PendingItemStackAssignment pending = null;

        for (Object o : path.getEdgeList()) {

            String lhs = o.toString().split(":")[0];
            String rhs = o.toString().split(":")[1];

            if (lhs.contains("source") && rhs.contains("item")) {
                applySourceToItemEdge(rhs, currSol);
            } else if (lhs.contains("item") && rhs.contains("item")) {
                applyItemToItemEdge(lhs, rhs, currSol);
            } else if (lhs.contains("item") && rhs.contains("stack")) {
                pending = applyItemToStackEdge(lhs, rhs, currSol, pending);
            } else if (lhs.contains("stack") && rhs.contains("item")) {
                pending = applyStackToItemEdge(lhs, rhs, currSol, pending);
            }
        }
        if (pending != null) {
            HeuristicUtil.assignItemToStack(
                pending.getItem(), currSol.getFilledStacks()[pending.getStack()], currSol.getSolvedInstance().getItemObjects()
            );
        }
    }

    /**
     * Retrieves a topological order for the vertices of the given graph.
     *
     * @param graph - graph to determine a topological order for
     * @return topological order for the graph's vertices
     */
    private List<String> getTopologicalOrder(DefaultDirectedWeightedGraph<String, DefaultWeightedEdge> graph) {
        List<String> topologicalOrderList = new ArrayList<>();
        TopologicalOrderIterator topologicalOrder = new TopologicalOrderIterator<>(graph);
        while (topologicalOrder.hasNext()) {
            topologicalOrderList.add(topologicalOrder.next().toString());
        }
        return topologicalOrderList;
    }

    /**
     * Computes distances and predecessors for each node.
     *
     * @param topologicalOrder - topological order to loop through
     * @param distances        - map to store the distances in
     * @param graph            - graph the distances are based on
     * @param pred             - map to store the predecessors in
     */
    private void computeDistAndPred(
        List<String> topologicalOrder, Map<String, Double> distances,
        DefaultDirectedWeightedGraph<String, DefaultWeightedEdge> graph, Map<String, String> pred
    ) {
        for (String node : topologicalOrder) {
            if (node.contains("source")) {
                distances.put(node, 0.0);
            } else {
                double costs = Double.MAX_VALUE;
                String predecessor = "";
                for (String predCandidate : topologicalOrder) {
                    if (!predCandidate.equals(node)) {
                        if (graph.containsEdge(predCandidate, node)) {
                            if (distances.get(predCandidate) + graph.getEdgeWeight(graph.getEdge(predCandidate, node)) < costs) {
                                costs = distances.get(predCandidate) + graph.getEdgeWeight(graph.getEdge(predCandidate, node));
                                predecessor = predCandidate;
                            }
                        }
                    }
                }
                if (costs < Double.MAX_VALUE) {
                    distances.put(node, costs);
                    pred.put(node, predecessor);
                }
            }
        }
    }

    /**
     * Retrieves the reversed path.
     *
     * @param lastNodeOfPath - specifies the last node of the path
     * @param pred           - map containing each node's predecessor
     * @return reversed path
     */
    private List<String> getReversedPath(String lastNodeOfPath, Map<String, String> pred) {
        String curr = lastNodeOfPath;
        List<String> reversedPath = new ArrayList<>();
        reversedPath.add(curr);
        while (!curr.equals("source")) {
            reversedPath.add(pred.get(curr));
            curr = pred.get(curr);
        }
        return reversedPath;
    }

    /**
     * Retrieves the best ejection chain (shortest path from source to any stack).
     *
     * @param graph - graph containing the paths
     * @return best ejection chain (biggest cost reduction)
     */
    private GraphPath getBestPath(DefaultDirectedWeightedGraph<String, DefaultWeightedEdge> graph) {

        List<String> topologicalOrder = this.getTopologicalOrder(graph);
        if (!topologicalOrder.get(0).equals("source")) {
            System.out.println("PROBLEM - NO SOURCE VERTEX!");
            System.exit(0);
        }

        Map<String, Double> distances = new HashMap<>();
        // init distances
        for (String node : topologicalOrder) { distances.put(node, Double.MAX_VALUE); }
        Map<String, String> pred = new HashMap<>();
        this.computeDistAndPred(topologicalOrder, distances, graph, pred);

        double minCosts = Double.MAX_VALUE;
        String lastNodeOfPath = "";
        for (String node : distances.keySet()) {
            if (node.contains("stack")) {
                if (distances.get(node) < minCosts) {
                    minCosts = distances.get(node);
                    lastNodeOfPath = node;
                }
            }
        }

        if (lastNodeOfPath.isEmpty()) { return new EjectionChainPath(graph); }
        List<String> reversedPath = this.getReversedPath(lastNodeOfPath, pred);
        Collections.reverse(reversedPath);
        EjectionChainPath bestPath = new EjectionChainPath(graph);
        bestPath.setPath(reversedPath, graph);
        return bestPath;
    }

    /**
     * Retrieves the best ejection chain (shortest path from source to any stack).
     *
     * @param graph          - graph containing the paths
     * @param shortestPath   - all pairs shortest path (Bellman-Ford result)
     * @param numberOfStacks - number of stacks available in the storage area
     * @return best ejection chain (biggest cost reduction)
     */
    private GraphPath getBestPathUsingBellmanFord(
        DefaultDirectedWeightedGraph<String, DefaultWeightedEdge> graph,
        BellmanFordShortestPath shortestPath, int numberOfStacks
    ) {
        GraphPath bestPath = null;

        // compute shortest path from source to any stack
        // --> sequence of combined item insertions and removals
        // --> each shortest path from source to a stack corresponds to one ejection chain
        for (int stack = 0; stack < numberOfStacks; stack++) {
            if (graph.containsVertex("stack" + stack)) {

                // get a shortest path from source vertex to the specified stack
                GraphPath path = shortestPath.getPath("source", "stack" + stack);

                if (path != null) {
                    if (bestPath != null) {
                        // looking for the shortest path --> biggest reduction
                        if (bestPath.getWeight() > path.getWeight()) {
                            bestPath = path;
                        }
                    } else {
                        bestPath = path;
                    }
                }
            }
        }
        return bestPath;
    }

    /**
     * Constructs an ejection chain by building up an auxiliary graph and then
     * searching for the shortest path (biggest cost reduction) in that graph.
     *
     * @param currSol - current solution
     * @return ejection chain
     */
    private GraphPath constructEjectionChain(Solution currSol) {

        List<Integer> stackOrder = getRandomStackOrder(currSol);
        // construct auxiliary graph
        DefaultDirectedWeightedGraph<String, DefaultWeightedEdge> graph = new DefaultDirectedWeightedGraph<>(DefaultWeightedEdge.class);
        graph.addVertex("source");
        addVerticesForItems(graph, currSol.getSolvedInstance().getItems().length);
        addVerticesForStacks(graph, currSol.getFilledStacks().length, currSol);
        addEdges(graph, stackOrder, currSol.getSolvedInstance().getItems().length, currSol);

        // creating bellman-ford instance
//        BellmanFordShortestPath shortestPath = new BellmanFordShortestPath(graph);
//        GraphPath bellManBestPath = getBestPathUsingBellmanFord(graph, shortestPath, stackOrder.size());

        // return best ejection chain:
        GraphPath bestPath = this.getBestPath(graph);

        return bestPath;
    }
}

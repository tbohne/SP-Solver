package SP.post_optimization_methods;

import SP.representations.OwnPath;
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
     * @param currSol         - current solution to generate neighbor for
     * @param performedShifts - list that keeps track of the performed shifts
     * @return generated neighboring solution
     */
    public Solution generateEjectionChainNeighbor(Solution currSol, List<Shift> performedShifts) {
        GraphPath bestPath = constructEjectionChain(currSol);
        Solution neighbor = new Solution(currSol);
        if (bestPath == null) { return neighbor; }
        applyEjectionChain(neighbor, bestPath, performedShifts);
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
     * @param lhs             - left hand side of the edge string
     * @param rhs             - right hand side of the edge string
     * @param currSol         - current solution
     * @param performedShifts - list that keeps track of performed shifts
     */
    private void applyItemToItemEdge(String lhs, String rhs, Solution currSol, List<Shift> performedShifts) {
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
        performedShifts.add(new Shift(itemOne, stackOfItemTwo));
        HeuristicUtil.assignItemToStack(itemOne, currSol.getFilledStacks()[stackOfItemTwo], currSol.getSolvedInstance().getItemObjects());
    }

    /**
     * Applies item to stack edge which means that the item gets shifted to the stack.
     *
     * @param lhs             - left hand side of the edge string
     * @param rhs             - right hand side of the edge string
     * @param currSol         - current solution
     * @param performedShifts - list that keeps track of the performed shifts
     * @param pending         - pending stack assignment
     * @return pending stack assignment (may be same as before)
     */
    private PendingItemStackAssignment applyItemToStackEdge(String lhs, String rhs, Solution currSol, List<Shift> performedShifts, PendingItemStackAssignment pending) {
        int item = Integer.parseInt(lhs.replace("(item", "").trim());
        int stack = Integer.parseInt(rhs.replace("stack", "").replace(")", "").trim());
        int stackOfItem = currSol.getStackIdxForAssignedItem(item);
        if (stackOfItem != -1) {
            HeuristicUtil.removeItemFromStack(item, currSol.getFilledStacks()[stackOfItem]);
        }
        if (HeuristicUtil.completelyFilledStack(currSol.getFilledStacks()[stack])) {
            return new PendingItemStackAssignment(item, stack);
        } else {
            performedShifts.add(new Shift(item, stack));
            HeuristicUtil.assignItemToStack(item, currSol.getFilledStacks()[stack], currSol.getSolvedInstance().getItemObjects());
        }
        return pending;
    }

    /**
     * Applies stack to item edge which means that the item gets removed from the stack.
     *
     * @param lhs             - left hand side of edge string
     * @param rhs             - right hand side of edge string
     * @param currSol         - current solution
     * @param pending         - pending stack assignment
     * @param performedShifts - list that keeps track of the performed shifts
     * @return pensing stack assignment (may be same as before)
     */
    private PendingItemStackAssignment applyStackToItemEdge(
        String lhs, String rhs, Solution currSol, PendingItemStackAssignment pending, List<Shift> performedShifts
    ) {
        int stack = Integer.parseInt(lhs.replace("(stack", "").trim());
        int item = Integer.parseInt(rhs.replace("item", "").replace(")", "").trim());
        HeuristicUtil.removeItemFromStack(item, currSol.getFilledStacks()[stack]);

        if (pending != null && stack == pending.getStack()) {
            performedShifts.add(new Shift(pending.getItem(), stack));
            HeuristicUtil.assignItemToStack(pending.getItem(), currSol.getFilledStacks()[stack], currSol.getSolvedInstance().getItemObjects());
            return null;
        }
        return pending;
    }

    /**
     * Applies the generated ejection chain (combination of item insertions and removals).
     *
     * @param currSol         -  current solution
     * @param path            - ejection chain
     * @param performedShifts - list that keeps track of the performed shifts
     */
    private void applyEjectionChain(Solution currSol, GraphPath path, List<Shift> performedShifts) {

        PendingItemStackAssignment pending = null;
        performedShifts.clear();

        for (Object o : path.getEdgeList()) {

            String lhs = o.toString().split(":")[0];
            String rhs = o.toString().split(":")[1];

            if (lhs.contains("source") && rhs.contains("item")) {
                applySourceToItemEdge(rhs, currSol);
            } else if (lhs.contains("item") && rhs.contains("item")) {
                applyItemToItemEdge(lhs, rhs, currSol, performedShifts);
            } else if (lhs.contains("item") && rhs.contains("stack")) {
                pending = applyItemToStackEdge(lhs, rhs, currSol, performedShifts, pending);
            } else if (lhs.contains("stack") && rhs.contains("item")) {
                pending = applyStackToItemEdge(lhs, rhs, currSol, pending, performedShifts);
            }
        }
        if (pending != null) {
            performedShifts.add(new Shift(pending.getItem(), pending.getStack()));
            HeuristicUtil.assignItemToStack(
                pending.getItem(), currSol.getFilledStacks()[pending.getStack()], currSol.getSolvedInstance().getItemObjects()
            );
        }
    }

    private List<String> getTopologicalOrder(DefaultDirectedWeightedGraph<String, DefaultWeightedEdge> graph) {
        List<String> topologicalOrderList = new ArrayList<>();
        TopologicalOrderIterator topologicalOrder = new TopologicalOrderIterator<>(graph);
        while (topologicalOrder.hasNext()) {
            topologicalOrderList.add(topologicalOrder.next().toString());
        }
        return topologicalOrderList;
    }

    private GraphPath getBestPathNewWay(DefaultDirectedWeightedGraph<String, DefaultWeightedEdge> graph) {

        List<String> topologicalOrder = this.getTopologicalOrder(graph);

        // TODO: dbg
        if (!topologicalOrder.get(0).equals("source")) {
            System.out.println("PROBLEM - NO SOURCE VERTEX!");
            System.exit(0);
        }

        Map<String, Double> distances = new HashMap<>();
        // init distances
        for (String node : topologicalOrder) { distances.put(node, Double.MAX_VALUE); }

        Map<String, String> pred = new HashMap<>();

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

        // TODO: dbg
        if (lastNodeOfPath.isEmpty()) { return new OwnPath(graph); }

        String curr = lastNodeOfPath;
        List<String> reversedPath = new ArrayList<>();
        reversedPath.add(curr);
        while (!curr.equals("source")) {
            reversedPath.add(pred.get(curr));
            curr = pred.get(curr);
            // TODO: dbg
            if (curr == null) { System.exit(0); }
        }
        Collections.reverse(reversedPath);
        OwnPath bestPath = new OwnPath(graph);
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
    private GraphPath getBestPath(
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

        double start = System.currentTimeMillis();
        // construct auxiliary graph
        DefaultDirectedWeightedGraph<String, DefaultWeightedEdge> graph = new DefaultDirectedWeightedGraph<>(DefaultWeightedEdge.class);
        graph.addVertex("source");
        addVerticesForItems(graph, currSol.getSolvedInstance().getItems().length);
        addVerticesForStacks(graph, currSol.getFilledStacks().length, currSol);
        addEdges(graph, stackOrder, currSol.getSolvedInstance().getItems().length, currSol);
        System.out.println("----------> graph const: " + (System.currentTimeMillis() - start) / 1000.0);

        // creating bellman-ford instance
        BellmanFordShortestPath shortestPath = new BellmanFordShortestPath(graph);

        start = System.currentTimeMillis();

        // return best ejection chain:
        GraphPath bestPath = this.getBestPathNewWay(graph);

        // TODO: further check whether both results always the same
        GraphPath bellManBestPath = getBestPath(graph, shortestPath, stackOrder.size());
        if (Math.abs(bestPath.getWeight() - bellManBestPath.getWeight()) > 0.0005) {
            System.out.println("PROBLEM:");
            System.out.println("MY: " + bestPath.getWeight());
            System.out.println("BELLMAN-FORD: " + bellManBestPath.getWeight());
            System.exit(0);
        }

        System.out.println("----------> Bellman-Ford: " + (System.currentTimeMillis() - start) / 1000.0);
        return bestPath;
    }
}

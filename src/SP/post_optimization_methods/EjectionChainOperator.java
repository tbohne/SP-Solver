package SP.post_optimization_methods;

import SP.representations.Solution;
import SP.util.HeuristicUtil;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.BellmanFordShortestPath;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EjectionChainOperator {

    public EjectionChainOperator() {}

    public List<Integer> getRandomStackOrder(Solution currSol) {
        // choose random order of stacks in current solution
        List<Integer> stackOrder = new ArrayList<>();
        for (int stack = 0; stack < currSol.getFilledStacks().length; stack++) {
            stackOrder.add(stack);
        }
        Collections.shuffle(stackOrder);
        return stackOrder;
    }

    private void addVerticesForItems(Graph<String, DefaultWeightedEdge> graph, int numberOfItems) {
        for (int item = 0; item < numberOfItems; item++) {
            graph.addVertex("item" + item);
        }
    }

    private void addVerticesForStacks(Graph<String, DefaultWeightedEdge> graph, int numberOfStacks, Solution currSol) {
        for (int stack = 0; stack < numberOfStacks; stack++) {
            if (!HeuristicUtil.completelyFilledStack(currSol.getFilledStacks()[stack])) {
                graph.addVertex("stack" + stack);
            }
        }
    }

    private double getCostsForStack(Solution currSol, int stack) {
        double costs = 0.0;
        for (int item : currSol.getFilledStacks()[stack]) {
            if (item != -1) {
                costs += currSol.getSolvedInstance().getCosts()[item][stack];
            }
        }
        return costs;
    }

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

    private void applySourceToItemEdge(String lhs, String rhs, Solution currSol, List<Shift> performedShifts) {
        int item = Integer.parseInt(rhs.replace("item", "").replace(")", "").trim());
        int stackOfItem = currSol.getStackIdxForAssignedItem(item);
        HeuristicUtil.removeItemFromStack(item, currSol.getFilledStacks()[stackOfItem]);
    }

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

    public void applyEjectionChain(Solution currSol, GraphPath path, List<Shift> performedShifts) {

        PendingItemStackAssignment pending = null;
        performedShifts.clear();

        for (Object o : path.getEdgeList()) {
            String lhs = o.toString().split(":")[0];
            String rhs = o.toString().split(":")[1];

            if (lhs.contains("source") && rhs.contains("item")) {
                applySourceToItemEdge(lhs, rhs, currSol, performedShifts);
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

    public GraphPath getBestPath(
        DefaultDirectedWeightedGraph<String, DefaultWeightedEdge> graph,
        BellmanFordShortestPath shortestPath, int numberOfStacks
    ) {

        GraphPath bestPath = null;

        for (int stack = 0; stack < numberOfStacks; stack++) {
            if (graph.containsVertex("stack" + stack)) {
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

    public GraphPath constructEjectionChain(Solution currSol) {

        List<Integer> stackOrder = getRandomStackOrder(currSol);

        // construct auxiliary graph:
        DefaultDirectedWeightedGraph<String, DefaultWeightedEdge> graph = new DefaultDirectedWeightedGraph<>(DefaultWeightedEdge.class);
        graph.addVertex("source");
        addVerticesForItems(graph, currSol.getSolvedInstance().getItems().length);
        addVerticesForStacks(graph, currSol.getFilledStacks().length, currSol);
        addEdges(graph, stackOrder, currSol.getSolvedInstance().getItems().length, currSol);

        // compute shortest path from source to any node in V^ZERO (stacks)
        // --> sequence of combined item insertions and removals
        // each shortest path from source to a node in V^ZERO corresponds to one ejection chain --> choose best
        BellmanFordShortestPath shortestPath = new BellmanFordShortestPath(graph);

        return getBestPath(graph, shortestPath, stackOrder.size());
    }

    public Solution generateEjectionChainNeighbor(Solution currSol, List<Shift> performedShifts) {
        GraphPath bestPath = constructEjectionChain(currSol);
        Solution neighbor = new Solution(currSol);
        if (bestPath == null) { return neighbor; }
        applyEjectionChain(neighbor, bestPath, performedShifts);
        neighbor.lowerItemsThatAreStackedInTheAir();
        neighbor.sortItemsInStacksBasedOnTransitiveStackingConstraints();
        return neighbor;
    }
}

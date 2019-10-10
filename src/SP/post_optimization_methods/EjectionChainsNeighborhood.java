package SP.post_optimization_methods;

import SP.experiments.PostOptimization;
import SP.representations.Solution;
import SP.representations.StackPosition;
import SP.util.HeuristicUtil;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.BellmanFordShortestPath;
import org.jgrapht.graph.*;

import java.util.*;


public class EjectionChainsNeighborhood {

    private int numberOfNeighbors;
    private PostOptimization.ShortTermStrategies shortTermStrategy;
    private int unsuccessfulNeighborGenerationAttempts;
    private int maxTabuListLength;
    private int tabuListClears;
    private Queue<Shift> tabuList;

    public EjectionChainsNeighborhood(
        int numberOfNeighbors, PostOptimization.ShortTermStrategies shortTermStrategy,
        int maxTabuListLength, int unsuccessfulNeighborGenerationAttempts
    ) {
        this.numberOfNeighbors = numberOfNeighbors;
        this.shortTermStrategy = shortTermStrategy;
        this.maxTabuListLength = maxTabuListLength;
        this.unsuccessfulNeighborGenerationAttempts = unsuccessfulNeighborGenerationAttempts;
        this.tabuListClears = 0;
        this.unsuccessfulNeighborGenerationAttempts = unsuccessfulNeighborGenerationAttempts;
        this.tabuList = new LinkedList<>();
    }

    /**
     * Clears the entries in the shift tabu list and increments the clear counter.
     */
    public void clearTabuList() {
        this.tabuList.clear();
        this.tabuListClears++;
    }

    public int getTabuListClears() {
        return this.tabuListClears;
    }

    public List<Integer> getRandomStackOrder(Solution currSol) {
        // choose random order of stacks in current solution
        List<Integer> stackOrder = new ArrayList<>();
        for (int stack = 0; stack < currSol.getFilledStacks().length; stack++) {
            stackOrder.add(stack);
        }
        Collections.shuffle(stackOrder);
        return stackOrder;
    }

    private void addVerticesForItems(Graph<String, DefaultWeightedEdge> graph, int numberOfItems, int source) {
        for (int item = 0; item < numberOfItems; item++) {
            if (item != source) {
                graph.addVertex("item" + item);
            }
        }
    }

    private void addVerticesForStacks(Graph<String, DefaultWeightedEdge> graph, int numberOfStacks, Solution currSol) {
        for (int stack = 0; stack < numberOfStacks; stack++) {
            if (!HeuristicUtil.completelyFilledStack(currSol.getFilledStacks()[stack])) {
                graph.addVertex("stack" + stack);
            }
        }
    }

    private int addRandomSourceVertex(Graph<String, DefaultWeightedEdge> graph, int numberOfItems) {
        int randomItem = HeuristicUtil.getRandomIntegerInBetween(0, numberOfItems - 1);
        graph.addVertex("source" + randomItem);
        return randomItem;
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
                    double currCosts = this.getCostsForStack(currSol, stackIdxItemTwo);
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
                    double currCosts = this.getCostsForStack(currSol, stack);
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
            double currCosts = this.getCostsForStack(currSol, itemStack);
            double afterReduction = currCosts - currSol.getSolvedInstance().getCosts()[item][itemStack];

            // cost reduction when positive
            double costDiff = currCosts - afterReduction;
            // negating edge costs to obtain longest path using Bellman-Ford
            costDiff *= -1;
            graph.setEdgeWeight(edge, costDiff);
        }
    }

    private void addEdgeBetweenSourceAndItem(
        DefaultDirectedWeightedGraph<String, DefaultWeightedEdge> graph, Solution currSol, int source, int item
    ) {

        if (HeuristicUtil.itemCompatibleWithAlreadyAssignedItemsWithException(
            source, currSol.getFilledStacks()[currSol.getStackIdxForAssignedItem(item)],
            currSol.getSolvedInstance().getItemObjects(), currSol.getSolvedInstance().getStackingConstraints(), item
        )) {

            if (HeuristicUtil.itemCompatibleWithStack(currSol.getSolvedInstance().getCosts(), source, currSol.getStackIdxForAssignedItem(item))) {

                if (graph.containsVertex("item" + item)) {

                    DefaultWeightedEdge edge = graph.addEdge("source" + source, "item" + item);
                    int stack = currSol.getStackIdxForAssignedItem(item);
                    double currCosts = this.getCostsForStack(currSol, stack);
                    double afterReduction = currCosts - currSol.getSolvedInstance().getCosts()[item][stack];

                    double sourceCostBefore = currSol.getSolvedInstance().getCosts()[source][currSol.getStackIdxForAssignedItem(source)];
                    double sourceCostAfter = currSol.getSolvedInstance().getCosts()[source][stack];
                    double sourceDiff = sourceCostBefore - sourceCostAfter;

                    // cost reduction when positive
                    double costDiff = currCosts - afterReduction + sourceDiff;
                    // negating edge costs to obtain longest path using Bellman-Ford
                    costDiff *= -1;
                    graph.setEdgeWeight(edge, costDiff);
                }
            }
        }
    }

    private void addEdges(
        DefaultDirectedWeightedGraph<String, DefaultWeightedEdge> graph, List<Integer> stackOrder,
        int numberOfItems, Solution currSol, int source
    ) {

        for (int i = 0; i < numberOfItems; i++) {
            for (int j = 0; j < numberOfItems; j++) {

                if (i == j || !graph.containsVertex("item" + i) || !graph.containsVertex("item" + j)) { continue; }

                int stackIdxItemOne = currSol.getStackIdxForAssignedItem(i);
                int stackIdxItemTwo = currSol.getStackIdxForAssignedItem(j);
                // don't shift in same stack
                if (stackIdxItemOne == stackIdxItemTwo) { continue; }

                if (stackOrder.indexOf(stackIdxItemOne) < stackOrder.indexOf(stackIdxItemTwo)) {
                    this.addEdgeBetweenItems(graph, currSol, i, j);
                } else {
                    this.addEdgeBetweenItems(graph, currSol, j, i);
                }
            }

            for (int stack = 0; stack < stackOrder.size(); stack++) {

                int stackIdxItem = currSol.getStackIdxForAssignedItem(i);
                if (stackIdxItem == stack) { continue; }

                if (stackOrder.indexOf(stackIdxItem) < stackOrder.indexOf(stack)) {
                    this.addEdgeFromItemToStack(graph, currSol, i, stack);
                } else {
                    this.addEdgeFromStackToItem(graph, currSol, stack, i);
                }
            }
        }

        for (int item = 0; item < numberOfItems; item++) {
            if (currSol.getStackIdxForAssignedItem(item) == currSol.getStackIdxForAssignedItem(source)) { continue; }
            this.addEdgeBetweenSourceAndItem(graph, currSol, source, item);
        }
    }

    public void blockShiftForWholeStack(Solution currSol, int item, int stack, List<Shift> performedShifts) {
        for (int level = 0; level < currSol.getSolvedInstance().getStackCapacity(); level++) {
            Shift shift = new Shift(item, new StackPosition(stack, level));
            performedShifts.add(shift);
        }
    }

    private void applySourceToItemEdge(String lhs, String rhs, Solution currSol, List<Shift> performedShifts) {
        int source = Integer.parseInt(lhs.replace("(source", "").trim());
        int stackOfSource = currSol.getStackIdxForAssignedItem(source);
        HeuristicUtil.removeItemFromStack(source, currSol.getFilledStacks()[stackOfSource]);
        int item = Integer.parseInt(rhs.replace("item", "").replace(")", "").trim());
        int stackOfItem = currSol.getStackIdxForAssignedItem(item);
        HeuristicUtil.removeItemFromStack(item, currSol.getFilledStacks()[stackOfItem]);
        this.blockShiftForWholeStack(currSol, source, stackOfItem, performedShifts);
        HeuristicUtil.assignItemToStack(source, currSol.getFilledStacks()[stackOfItem], currSol.getSolvedInstance().getItemObjects());
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
        this.blockShiftForWholeStack(currSol, itemOne, stackOfItemOne, performedShifts);
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
            this.blockShiftForWholeStack(currSol, item, stack, performedShifts);
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
            this.blockShiftForWholeStack(currSol, pending.getItem(), stack, performedShifts);
            HeuristicUtil.assignItemToStack(pending.getItem(), currSol.getFilledStacks()[stack], currSol.getSolvedInstance().getItemObjects());
            return null;
        }
        return pending;
    }

    public List<Shift> applyEjectionChain(Solution currSol, GraphPath path) {

        PendingItemStackAssignment pending = null;
        List<Shift> performedShifts = new ArrayList<>();

        for (Object o : path.getEdgeList()) {
            String lhs = o.toString().split(":")[0];
            String rhs = o.toString().split(":")[1];

            if (lhs.contains("source") && rhs.contains("item")) {
                this.applySourceToItemEdge(lhs, rhs, currSol, performedShifts);
            } else if (lhs.contains("item") && rhs.contains("item")) {
                this.applyItemToItemEdge(lhs, rhs, currSol, performedShifts);
            } else if (lhs.contains("item") && rhs.contains("stack")) {
                pending = this.applyItemToStackEdge(lhs, rhs, currSol, performedShifts, pending);
            } else if (lhs.contains("stack") && rhs.contains("item")) {
                pending = this.applyStackToItemEdge(lhs, rhs, currSol, pending, performedShifts);
            }
        }
        if (pending != null) {
            this.blockShiftForWholeStack(currSol, pending.getItem(), pending.getStack(), performedShifts);
            HeuristicUtil.assignItemToStack(
                pending.getItem(), currSol.getFilledStacks()[pending.getStack()], currSol.getSolvedInstance().getItemObjects()
            );
        }
        return performedShifts;
    }

    public GraphPath getBestPath(
        DefaultDirectedWeightedGraph<String, DefaultWeightedEdge> graph,
        BellmanFordShortestPath shortestPath, int numberOfStacks, int source
    ) {

        GraphPath bestPath = null;

        for (int stack = 0; stack < numberOfStacks; stack++) {
            if (graph.containsVertex("stack" + stack)) {
                GraphPath path = shortestPath.getPath("source" + source, "stack" + stack);
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

        List<Integer> stackOrder = this.getRandomStackOrder(currSol);

        // construct auxiliary graph:
        DefaultDirectedWeightedGraph<String, DefaultWeightedEdge> graph = new DefaultDirectedWeightedGraph<>(DefaultWeightedEdge.class);
        int source = this.addRandomSourceVertex(graph, currSol.getSolvedInstance().getItems().length);
        this.addVerticesForItems(graph, currSol.getSolvedInstance().getItems().length, source);
        this.addVerticesForStacks(graph, currSol.getFilledStacks().length, currSol);
        this.addEdges(graph, stackOrder, currSol.getSolvedInstance().getItems().length, currSol, source);

        // compute shortest path from source to any node in V^ZERO (stacks)
        // --> sequence of combined item insertions and removals
        // each shortest path from source to a node in V^ZERO corresponds to one ejection chain --> choose best
        BellmanFordShortestPath shortestPath = new BellmanFordShortestPath(graph);

        return this.getBestPath(graph, shortestPath, stackOrder.size(), source);
    }

    public boolean tabuListContainsAnyOfTheShifts(List<Shift> performedShifts) {
        for (Shift shift : performedShifts) {
            if (this.tabuList.contains(shift)) {
                return true;
            }
        }
        return false;
    }

    public void forbidShifts(List<Shift> performedShifts) {
        if (this.tabuList.size() >= this.maxTabuListLength) {
            while (this.tabuList.size() + performedShifts.size() >= this.maxTabuListLength) {
                this.tabuList.poll();
            }
        }
        for (Shift shift : performedShifts) {
            this.tabuList.add(shift);
        }
    }

    private void logCurrentState(Solution currSol, GraphPath bestPath, Solution tmpSol) {
        System.out.println("costs before: " + currSol.computeCosts());
        System.out.println("costs of best path: " + bestPath.getWeight());
        System.out.println("costs after ejection chain: " + tmpSol.computeCosts());
        System.out.println("feasible: " + tmpSol.isFeasible());
        System.exit(0);
    }

    public Solution getNeighbor(Solution currSol, Solution bestSol) {

        List<Solution> nbrs = new ArrayList<>();
        int failCnt = 0;

        while (nbrs.size() < this.numberOfNeighbors) {

            GraphPath bestPath = this.constructEjectionChain(currSol);

            Solution tmpSol = new Solution(currSol);
            if (bestPath == null) { return tmpSol; }
            List<Shift> performedShifts = this.applyEjectionChain(tmpSol, bestPath);
            tmpSol.lowerItemsThatAreStackedInTheAir();
            tmpSol.sortItemsInStacksBasedOnTransitiveStackingConstraints();

            this.logCurrentState(currSol, bestPath, tmpSol);

            Solution neighbor = tmpSol;

            if (!neighbor.isFeasible()) { continue; }

            // FIRST-FIT
            if (this.shortTermStrategy == PostOptimization.ShortTermStrategies.FIRST_FIT && !this.tabuListContainsAnyOfTheShifts(performedShifts)
                && neighbor.computeCosts() < currSol.computeCosts()) {

                this.forbidShifts(performedShifts);
                return neighbor;

            // BEST-FIT
            } else if (!this.tabuListContainsAnyOfTheShifts(performedShifts)) {
                nbrs.add(neighbor);
                this.forbidShifts(performedShifts);
            } else {
                failCnt++;
                if (failCnt == this.unsuccessfulNeighborGenerationAttempts) {
                    failCnt = 0;
                    if (nbrs.size() == 0) {
                        System.out.println("CLEAR");
                        this.clearTabuList();
                    } else {
                        return HeuristicUtil.getBestSolution(nbrs);
                    }
                }
            }
            // ASPIRATION CRITERION
            if (neighbor.computeCosts() < bestSol.computeCosts()) {
                if (this.shortTermStrategy == PostOptimization.ShortTermStrategies.FIRST_FIT) {
                    return neighbor;
                } else {
                    nbrs.add(neighbor);
                }
            }
        }
        return HeuristicUtil.getBestSolution(nbrs);
    }
}

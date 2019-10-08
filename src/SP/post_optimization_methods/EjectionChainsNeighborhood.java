package SP.post_optimization_methods;

import SP.experiments.PostOptimization;
import SP.representations.Solution;
import SP.util.HeuristicUtil;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.BellmanFordShortestPath;
import org.jgrapht.graph.*;

import java.util.*;

public class EjectionChainsNeighborhood {

    private int numberOfNeighbors;
    private PostOptimization.ShortTermStrategies shortTermStrategy;
    private int maxTabuListLength;
    private int unsuccessfulNeighborGenerationAttempts;

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

    private void addEdgesBetweenItems(
        DefaultDirectedWeightedGraph<String, DefaultWeightedEdge> graph, List<Integer> stackOrder,
        int numberOfItems, Solution currSol, int source
    ) {

        // for items
        for (int i = 0; i < numberOfItems; i++) {
            for (int j = 0; j < numberOfItems; j++) {

                if (i == j || !graph.containsVertex("item" + i) || !graph.containsVertex("item" + j)) { continue; }

                int stackIdxItemOne = currSol.getStackIdxForAssignedItem(i);
                int stackIdxItemTwo = currSol.getStackIdxForAssignedItem(j);

                // don't shift in same stack
                if (stackIdxItemOne == stackIdxItemTwo) { continue; }

                if (stackOrder.indexOf(stackIdxItemOne) < stackOrder.indexOf(stackIdxItemTwo)) {

                    if (!graph.containsEdge("item" + i, "item" + j)) {

                        // place i, remove j
                        if (HeuristicUtil.itemCompatibleWithAlreadyAssignedItemsWithException(
                            i, currSol.getFilledStacks()[stackIdxItemTwo], currSol.getSolvedInstance().getItemObjects(),
                            currSol.getSolvedInstance().getStackingConstraints(), j
                        )) {

                            if (HeuristicUtil.itemCompatibleWithStack(currSol.getSolvedInstance().getCosts(), i, stackIdxItemTwo)) {
                                DefaultWeightedEdge edge = graph.addEdge("item" + i, "item" + j);
                                // the cost difference of bin B(j) when replacing item j by item i
                                double currCosts = this.getCostsForStack(currSol, stackIdxItemTwo);
                                double costsItemJ = currSol.getSolvedInstance().getCosts()[j][stackIdxItemTwo];
                                double costsItemI = currSol.getSolvedInstance().getCosts()[i][stackIdxItemTwo];
                                double costsAfter = currCosts - costsItemJ + costsItemI;
                                // cost reduction when positive
                                double costDiff = currCosts - costsAfter;
                                graph.setEdgeWeight(edge, costDiff);
                            }
                        }
                    }
                } else {

                    if (!graph.containsEdge("item" + j, "item" + i)) {


                        if (HeuristicUtil.itemCompatibleWithAlreadyAssignedItemsWithException(
                            j, currSol.getFilledStacks()[stackIdxItemOne], currSol.getSolvedInstance().getItemObjects(),
                            currSol.getSolvedInstance().getStackingConstraints(), i
                        )) {

                            if (HeuristicUtil.itemCompatibleWithStack(currSol.getSolvedInstance().getCosts(), j, stackIdxItemOne)) {
                                DefaultWeightedEdge edge = graph.addEdge("item" + j, "item" + i);
                                // the cost difference of bin B(i) when replacing item i by item j
                                double currCosts = this.getCostsForStack(currSol, stackIdxItemOne);
                                double costsItemI = currSol.getSolvedInstance().getCosts()[i][stackIdxItemOne];
                                double costsItemJ = currSol.getSolvedInstance().getCosts()[j][stackIdxItemOne];
                                double costsAfter = currCosts - costsItemI + costsItemJ;
                                // cost reduction when positive
                                double costDiff = currCosts - costsAfter;
                                graph.setEdgeWeight(edge, costDiff);
                            }
                        }
                    }
                }
            }

            for (int stack = 0; stack < stackOrder.size(); stack++) {
                int stackIdxItem = currSol.getStackIdxForAssignedItem(i);
                if (stackOrder.indexOf(stackIdxItem) < stackOrder.indexOf(stack)) {
                    if (graph.containsVertex("stack" + stack) && graph.containsVertex("item" + i)) {

                        if (stackIdxItem == stack) { continue; }

                        if (HeuristicUtil.itemCompatibleWithStack(currSol.getSolvedInstance().getCosts(), i, stack)) {

                            if (HeuristicUtil.itemCompatibleWithAlreadyAssignedItems(i, currSol.getFilledStacks()[stack], currSol.getSolvedInstance().getItemObjects(),
                                    currSol.getSolvedInstance().getStackingConstraints())) {

                                DefaultWeightedEdge edge = graph.addEdge("item" + i, "stack" + stack);
                                // the cost difference of bin B(j) when inserting item i
                                double currCosts = this.getCostsForStack(currSol, stack);
                                double afterInsertion = currCosts + currSol.getSolvedInstance().getCosts()[i][stack];
                                // cost reduction when positive
                                double costDiff = currCosts - afterInsertion;
                                graph.setEdgeWeight(edge, costDiff);
                            }
                        }
                    }
                } else {
                    if (graph.containsVertex("stack" + stack) && graph.containsVertex("item" + i)) {
                        DefaultWeightedEdge edge = graph.addEdge("stack" + stack, "item" + i);
                        // the cost difference of bin B(j) when removing item i
                        double currCosts = this.getCostsForStack(currSol, stack);
                        double afterReduction = currCosts - currSol.getSolvedInstance().getCosts()[i][stack];
                        // cost reduction when positive
                        double costDiff = currCosts - afterReduction;
                        graph.setEdgeWeight(edge, costDiff);
                    }
                }
            }
        }

        for (int item = 0; item < numberOfItems; item++) {

            if (HeuristicUtil.itemCompatibleWithAlreadyAssignedItemsWithException(
                source, currSol.getFilledStacks()[currSol.getStackIdxForAssignedItem(item)], currSol.getSolvedInstance().getItemObjects(),
                currSol.getSolvedInstance().getStackingConstraints(), item
            )) {

                if (HeuristicUtil.itemCompatibleWithStack(currSol.getSolvedInstance().getCosts(), source, currSol.getStackIdxForAssignedItem(item))) {

                    if (currSol.getStackIdxForAssignedItem(item) == currSol.getStackIdxForAssignedItem(source)) { continue; }

                    if (graph.containsVertex("item" + item)) {
                        DefaultWeightedEdge edge = graph.addEdge("source" + source, "item" + item);
                        // the cost difference of bin B(j) when removing item
                        int stack = currSol.getStackIdxForAssignedItem(item);
                        double currCosts = this.getCostsForStack(currSol, stack);
                        double afterReduction = currCosts - currSol.getSolvedInstance().getCosts()[item][stack];
                        // cost reduction when positive
                        double costDiff = currCosts - afterReduction;
                        graph.setEdgeWeight(edge, costDiff);
                    }
                }
            }
        }
    }

    public void applyEjectionChain(Solution currSol, GraphPath path) {

        // (source, item, ..., item, stack)

        System.out.println("path: " + path);

        PendingItemStackAssignment pending = null;

        for (Object o : path.getEdgeList()) {

            String lhs = o.toString().split(":")[0];
            String rhs = o.toString().split(":")[1];

            // case 1: (source -> item)
            if (lhs.contains("source") && rhs.contains("item")) {

                int source = Integer.parseInt(lhs.replace("(source", "").trim());
                int stackOfSource = currSol.getStackIdxForAssignedItem(source);
                HeuristicUtil.removeItemFromStack(source, currSol.getFilledStacks()[stackOfSource]);
                int item = Integer.parseInt(rhs.replace("item", "").replace(")", "").trim());
                int stackOfItem = currSol.getStackIdxForAssignedItem(item);
                HeuristicUtil.removeItemFromStack(item, currSol.getFilledStacks()[stackOfItem]);
                HeuristicUtil.assignItemToStack(source, currSol.getFilledStacks()[stackOfItem], currSol.getSolvedInstance().getItemObjects());

            } else if (lhs.contains("item") && rhs.contains("item")) {

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

            } else if (lhs.contains("item") && rhs.contains("stack")) {

                int item = Integer.parseInt(lhs.replace("(item", "").trim());
                int stack = Integer.parseInt(rhs.replace("stack", "").replace(")", "").trim());
                int stackOfItem = currSol.getStackIdxForAssignedItem(item);

                if (stackOfItem != -1) {
                    HeuristicUtil.removeItemFromStack(item, currSol.getFilledStacks()[stackOfItem]);
                }

                if (HeuristicUtil.completelyFilledStack(currSol.getFilledStacks()[stack])) {
                    pending = new PendingItemStackAssignment(item, stack);
                } else {
                    HeuristicUtil.assignItemToStack(item, currSol.getFilledStacks()[stack], currSol.getSolvedInstance().getItemObjects());
                }


            } else if (lhs.contains("stack") && rhs.contains("item")) {

                int stack = Integer.parseInt(lhs.replace("(stack", "").trim());
                int item = Integer.parseInt(rhs.replace("item", "").replace(")", "").trim());

                HeuristicUtil.removeItemFromStack(item, currSol.getFilledStacks()[stack]);

                if (pending != null && stack == pending.getStack()) {
                    HeuristicUtil.assignItemToStack(pending.getItem(), currSol.getFilledStacks()[stack], currSol.getSolvedInstance().getItemObjects());
                    pending = null;
                }
            }
        }

        if (pending != null) {
            HeuristicUtil.assignItemToStack(pending.getItem(), currSol.getFilledStacks()[pending.getStack()], currSol.getSolvedInstance().getItemObjects());
        }
    }

    public Solution getNeighbor(Solution currSol, Solution bestSol) {
        List<Integer> stackOrder = this.getRandomStackOrder(currSol);

        // construct auxiliary graph:
        // V: items + stacks + source
        // A: (i, j) forall i, j for which B(i) < B(j) in the order
        //           and one arc from (source, j) for any j from the set of items
        // c(i, j): check paper

        DefaultDirectedWeightedGraph<String, DefaultWeightedEdge> graph = new DefaultDirectedWeightedGraph<>(DefaultWeightedEdge.class);
        int source = this.addRandomSourceVertex(graph, currSol.getSolvedInstance().getItems().length);
        this.addVerticesForItems(graph, currSol.getSolvedInstance().getItems().length, source);
        this.addVerticesForStacks(graph, currSol.getFilledStacks().length, currSol);
        this.addEdgesBetweenItems(graph, stackOrder, currSol.getSolvedInstance().getItems().length, currSol, source);

        // compute shortest path from source to any node in V^ZERO (stacks)
        // --> sequence of combined item insertions and removals
        // update solution in case of improvement
        // each shortest path from source to a node in V^ZERO corresponds to one ejection chain?
        // --> choose best
        BellmanFordShortestPath shortestPath = new BellmanFordShortestPath(graph);
        // each negative path should correspond to a cost reduction


        System.out.println("costs before ejection chain: " + currSol.computeCosts());

        GraphPath bestPath = null;

        for (int stack = 0; stack < stackOrder.size(); stack++) {

            if (graph.containsVertex("stack" + stack)) {

                GraphPath path = shortestPath.getPath("source" + source, "stack" + stack);

                if (path != null) {
                    if (bestPath != null) {
                        if (bestPath.getWeight() > path.getWeight()) {
                            bestPath = path;
                        }
                    } else {
                        bestPath = path;
                    }
//                System.out.println("path: " + path);
//                System.out.println("costs: " + path.getLength());
                } else {
//                System.out.println("no path from " + source + " to " + stack);
//                System.exit(0);
                }
            }
        }

        Solution tmpSol = new Solution(currSol);
        if (bestPath == null) {
            return tmpSol;
        }
        this.applyEjectionChain(tmpSol, bestPath);
        System.out.println("costs of best path: " + bestPath.getWeight());
        System.out.println("costs after ejection chain: " + tmpSol.computeCosts());
//        tmpSol.printFilledStacks();

        tmpSol.lowerItemsThatAreStackedInTheAir();
        tmpSol.sortItemsInStacksBasedOnTransitiveStackingConstraints();

        System.out.println("feasible: " + tmpSol.isFeasible());
        System.exit(0);

        return tmpSol;
    }
}

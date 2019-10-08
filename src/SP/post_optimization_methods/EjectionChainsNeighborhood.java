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

    private void addVerticesForItems(Graph<String, DefaultWeightedEdge> graph, int numberOfItems) {
        for (int item = 0; item < numberOfItems; item++) {
            graph.addVertex("item" + item);
        }
    }

    private void addVerticesForStacks(Graph<String, DefaultWeightedEdge> graph, int numberOfStacks) {
        for (int stack = 0; stack < numberOfStacks; stack++) {
            graph.addVertex("stack" + stack);
        }
    }

    private int addRandomSourceVertex(Graph<String, DefaultWeightedEdge> graph, int numberOfItems) {
        int randomItem = HeuristicUtil.getRandomIntegerInBetween(0, numberOfItems);
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
        DefaultDirectedWeightedGraph<String, DefaultWeightedEdge> graph, List<Integer> stackOrder, int numberOfItems, Solution currSol, int source
    ) {

        // for items
        for (int i = 0; i < numberOfItems; i++) {
            for (int j = 0; j < numberOfItems; j++) {

                if (i == j) {continue; }

                int stackIdxItemOne = currSol.getStackIdxForAssignedItem(i);
                int stackIdxItemTwo = currSol.getStackIdxForAssignedItem(j);
                if (stackOrder.indexOf(stackIdxItemOne) < stackOrder.indexOf(stackIdxItemTwo)) {

                    if (!graph.containsEdge("item" + i, "item" + j)) {
                        DefaultWeightedEdge edge = graph.addEdge("item" + i, "item" + j);
//                        System.out.println("EDGE: " + i + "   " + j);
                        // the cost difference of bin B(j) when replacing item j by item i
                        double currCosts = this.getCostsForStack(currSol, stackIdxItemTwo);
                        double costsItemJ = currSol.getSolvedInstance().getCosts()[j][stackIdxItemTwo];
                        double costsItemI = currSol.getSolvedInstance().getCosts()[i][stackIdxItemTwo];
                        double costsAfter = currCosts - costsItemJ + costsItemI;
                        // cost reduction when positive
                        double costDiff = currCosts - costsAfter;
                        graph.setEdgeWeight(edge, costDiff);
                    }
                } else {

                    if (!graph.containsEdge("item" + j, "item" + i)) {
                        DefaultWeightedEdge edge = graph.addEdge("item" + j, "item" + i);
                        // the cost difference of bin B(i) when replacing item i by item j
                        double currCosts = this.getCostsForStack(currSol, stackIdxItemOne);
                        double costsItemI = currSol.getSolvedInstance().getCosts()[i][stackIdxItemOne];
                        double costsItemJ = currSol.getSolvedInstance().getCosts()[j][stackIdxItemOne];
                        double costsAfter = currCosts - costsItemI + costsItemJ;
                        // cost reduction when positive
                        double costDiff = currCosts - costsAfter;
//                        System.out.println("costs: " + costDiff);
                        graph.setEdgeWeight(edge, costDiff);
                    }
                }
            }

            for (int stack = 0; stack < stackOrder.size(); stack++) {
                int stackIdxItem = currSol.getStackIdxForAssignedItem(i);
                if (stackOrder.indexOf(stackIdxItem) < stackOrder.indexOf(stack)) {
                    DefaultWeightedEdge edge = graph.addEdge("item" + i, "stack" + stack);
                    // the cost difference of bin B(j) when inserting item i
                    double currCosts = this.getCostsForStack(currSol, stack);
                    double afterInsertion = currCosts + currSol.getSolvedInstance().getCosts()[i][stack];
                    // cost reduction when positive
                    double costDiff = currCosts - afterInsertion;
                    graph.setEdgeWeight(edge, costDiff);
                } else {
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

        for (int item = 0; item < numberOfItems; item++) {
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

    public Solution getNeighbor(Solution currSol, Solution bestSol) {
        List<Integer> stackOrder = this.getRandomStackOrder(currSol);

        // construct auxiliary graph:
        // V: items + stacks + source
        // A: (i, j) forall i, j for which B(i) < B(j) in the order
        //           and one arc from (source, j) for any j from the set of items
        // c(i, j): check paper

        DefaultDirectedWeightedGraph<String, DefaultWeightedEdge> graph = new DefaultDirectedWeightedGraph<>(DefaultWeightedEdge.class);
        this.addVerticesForItems(graph, currSol.getSolvedInstance().getItems().length);
        this.addVerticesForStacks(graph, currSol.getFilledStacks().length);
        int source = this.addRandomSourceVertex(graph, currSol.getSolvedInstance().getItems().length);
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
            GraphPath path = shortestPath.getPath("source" + source, "stack" + stack);
            if (path != null) {
                if (bestPath != null) {
                    if (bestPath.getLength() > path.getLength()) {
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

        System.out.println(bestPath);
        System.out.println("costs of best path: " + bestPath.getWeight());

        return currSol;
    }
}

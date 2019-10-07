package SP.post_optimization_methods;

import SP.experiments.PostOptimization;
import SP.representations.BipartiteGraph;
import SP.representations.Solution;
import SP.util.GraphUtil;
import SP.util.HeuristicUtil;
import org.jgrapht.Graph;
import org.jgrapht.alg.matching.KuhnMunkresMinimalWeightBipartitePerfectMatching;
import org.jgrapht.graph.DefaultUndirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.util.*;

public class PartitionBasedSwapShiftNeighborhood implements SwapShiftNeighborhood {

    private int numberOfNeighbors;
    private PostOptimization.ShortTermStrategies shortTermStrategy;
    private int maxTabuListLength;
    private int unsuccessfulNeighborGenerationAttempts;
    private int unsuccessfulKSwapAttempts;

    private Queue<PartitionShift> tabuList;
    private int tabuListClears;

    public PartitionBasedSwapShiftNeighborhood(
            int numberOfNeighbors, PostOptimization.ShortTermStrategies shortTermStrategy,
            int maxTabuListLength, int unsuccessfulNeighborGenerationAttempts, int unsuccessfulKSwapAttempts
    ) {
        this.numberOfNeighbors = numberOfNeighbors;
        this.shortTermStrategy = shortTermStrategy;
        this.maxTabuListLength = maxTabuListLength;
        this.unsuccessfulNeighborGenerationAttempts = unsuccessfulNeighborGenerationAttempts;
        this.tabuListClears = 0;
        this.unsuccessfulNeighborGenerationAttempts = unsuccessfulNeighborGenerationAttempts;
        this.unsuccessfulKSwapAttempts = unsuccessfulKSwapAttempts;
        this.tabuList = new LinkedList<>();
    }

    /**
     * Adds the specified shift operation to the tabu list.
     * Replaces the oldest entry if the maximum length of the tabu list is reached.
     *
     * @param shift - shift operation to be added to the tabu list
     */
    public void forbidShift(PartitionShift shift) {
        if (this.tabuList.size() >= this.maxTabuListLength) {
            this.tabuList.poll();
        }
        this.tabuList.add(shift);
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

    public List<List<Integer>> getPartitionsFromSolution(Solution sol) {

        List<List<Integer>> partitions = new ArrayList<>();

        for (int[] stack : sol.getFilledStacks()) {
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

    private PartitionShift shiftItem(List<List<Integer>> partitions, Solution currSol) {

        // compute random item to be shifted
        int partitionIdx = HeuristicUtil.getRandomIntegerInBetween(0, partitions.size() - 1);
        List<Integer> partition = partitions.get(partitionIdx);
        int itemIdx = HeuristicUtil.getRandomIntegerInBetween(0, partition.size() - 1);
        int shiftItem = partition.remove(itemIdx);

        // allow empty partitions - otherwise the indices change --> problem for tabu list
        // if (partition.isEmpty()) { partitions.remove(partition); }

        int shiftTargetPartitionIdx = HeuristicUtil.getRandomIntegerInBetween(0, partitions.size() - 1);

        // create array with containing items of target partition
        int[] itemsOfTargetPartition = new int[partitions.get(shiftTargetPartitionIdx).size()];
        for (int i = 0; i < partitions.get(shiftTargetPartitionIdx).size(); i++) {
            itemsOfTargetPartition[i] = partitions.get(shiftTargetPartitionIdx).get(i);
        }

        // TODO: reasonable here? or just ignore feasibility?
//        // find new shift target until compatible partition is found
        while (partitions.get(shiftTargetPartitionIdx).size() == currSol.getSolvedInstance().getStackCapacity()
                /*|| !HeuristicUtil.itemCompatibleWithAlreadyAssignedItems(
                shiftItem, itemsOfTargetPartition, currSol.getSolvedInstance().getItemObjects(), currSol.getSolvedInstance().getStackingConstraints())*/
                )
        {
            shiftTargetPartitionIdx = HeuristicUtil.getRandomIntegerInBetween(0, partitions.size() - 1);
        }

        partitions.get(shiftTargetPartitionIdx).add(shiftItem);

        return new PartitionShift(shiftItem, shiftTargetPartitionIdx);
    }

    private void addVerticesForPartitions(List<List<Integer>> partitions, Graph<String, DefaultWeightedEdge> bipartiteGraph, Set<String> partitionOne) {
        for (List<Integer> partition : partitions) {
            bipartiteGraph.addVertex("partition" + partition);
            partitionOne.add("partition" + partition);
        }
    }

    private void addVerticesForStacks(int numberOfStacks, Graph<String, DefaultWeightedEdge> bipartiteGraph, Set<String> partitionTwo) {
        for (int stack = 0; stack < numberOfStacks; stack++) {
            bipartiteGraph.addVertex("stack" + stack);
            partitionTwo.add("stack" + stack);
        }
    }

    private void addEdgesBetweenDummyPartitionsAndStacks(
        Graph<String, DefaultWeightedEdge> bipartiteGraph, List<Integer> dummyPartitions, int numberOfStacks
    ) {
        for (int dummyPartition : dummyPartitions) {
            for (int stack = 0; stack < numberOfStacks; stack++) {
                DefaultWeightedEdge edge = bipartiteGraph.addEdge("dummy" + dummyPartition, "stack" + stack);
                bipartiteGraph.setEdgeWeight(edge, 0);
            }
        }
    }

    private void addEdgesBetweenItemsAndStackPositions(
        Solution sol, Graph<String, DefaultWeightedEdge> bipartiteGraph, List<List<Integer>> partitions, int numberOfStacks
    ) {

        // assumed at this point: items inside a partition are compatible

        for (List<Integer> partition : partitions) {
            for (int stack = 0; stack < numberOfStacks; stack++) {
                DefaultWeightedEdge edge = bipartiteGraph.addEdge("partition" + partition, "stack" + stack);
                double costs = 0;
                for (int item : partition) {
                    costs += sol.getSolvedInstance().getCosts()[item][stack];
                }
                bipartiteGraph.setEdgeWeight(edge, costs);
            }
        }
    }

    private BipartiteGraph constructBipartiteGraph(List<List<Integer>> partitions, Solution sol) {

        Graph<String, DefaultWeightedEdge> graph = new DefaultUndirectedWeightedGraph<>(DefaultWeightedEdge.class);

        Set<String> partitionOne = new HashSet<>();
        Set<String> partitionTwo = new HashSet<>();

        this.addVerticesForPartitions(partitions, graph, partitionOne);
        this.addVerticesForStacks(sol.getFilledStacks().length, graph, partitionTwo);
        // there could be more stacks than partitions
        List<Integer> dummyPartitions = GraphUtil.introduceDummyVerticesToBipartiteGraph(graph, partitionOne, partitionTwo);

        addEdgesBetweenDummyPartitionsAndStacks(graph, dummyPartitions, sol.getFilledStacks().length);
        addEdgesBetweenItemsAndStackPositions(sol, graph, partitions, sol.getFilledStacks().length);

        return new BipartiteGraph(partitionOne, partitionTwo, graph);
    }

    private Solution generateSolutionFromPartitions(List<List<Integer>> partitions, Solution currSol) {

        // generate nbr solution (minCostBipartitePM)

        // construct bipartite graph between partitions and stacks

        BipartiteGraph graph = this.constructBipartiteGraph(partitions, currSol);

        KuhnMunkresMinimalWeightBipartitePerfectMatching<String, DefaultWeightedEdge> minCostPerfectMatching =
            new KuhnMunkresMinimalWeightBipartitePerfectMatching<>(graph.getGraph(), graph.getPartitionOne(), graph.getPartitionTwo());

        Solution newSol = new Solution(currSol);
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

    /**
     * Generates a neighbor for the current solution using the "shift-neighborhood".
     *
     * @return neighboring solution
     */
    @SuppressWarnings("Duplicates")
    public Solution getNeighborShift(Solution currSol, Solution bestSol) {

        List<Solution> nbrs = new ArrayList<>();
        int failCnt = 0;

        while (nbrs.size() < this.numberOfNeighbors) {

            List<List<Integer>> partitions = this.getPartitionsFromSolution(currSol);
            PartitionShift shift = this.shiftItem(partitions, currSol);

            Solution neighbor = this.generateSolutionFromPartitions(partitions, currSol);
            neighbor.sortItemsInStacksBasedOnTransitiveStackingConstraints();
            neighbor.lowerItemsThatAreStackedInTheAir();

            if (!neighbor.isFeasible()) { continue; }

            // FIRST-FIT
            if (this.shortTermStrategy == PostOptimization.ShortTermStrategies.FIRST_FIT && !tabuList.contains(shift)
                    && neighbor.computeCosts() < currSol.computeCosts()) {

                forbidShift(shift);
                return neighbor;

            // BEST-FIT
            } else if (!tabuList.contains(shift)) {
                nbrs.add(neighbor);
                forbidShift(shift);
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

    /**
     * Performs the specified number of swap operations and stores them in the swap list.
     *
     * @param numberOfSwaps - number of swaps to be performed
     * @param swapList      - list to store the performer swaps
     * @return generated neighbor solution
     */
    public Solution performSwaps(int numberOfSwaps, List<PartitionSwap> swapList, Solution currSol) {

        List<List<Integer>> partitions = this.getPartitionsFromSolution(currSol);
        int swapCnt = 0;

        while (swapCnt < numberOfSwaps) {

            int partitionIdxOne = HeuristicUtil.getRandomIntegerInBetween(0, partitions.size() - 1);
            int itemIdxOne = HeuristicUtil.getRandomIntegerInBetween(0, partitions.get(partitionIdxOne).size() - 1);
            int partitionIdxTwo = HeuristicUtil.getRandomIntegerInBetween(0, partitions.size() - 1);
            int itemIdxTwo = HeuristicUtil.getRandomIntegerInBetween(0, partitions.get(partitionIdxTwo).size() - 1);

            // TODO: check whether both items the same

            int itemOne = partitions.get(partitionIdxOne).get(itemIdxOne);
            int itemTwo = partitions.get(partitionIdxTwo).get(itemIdxTwo);
            partitions.get(partitionIdxOne).set(itemIdxOne, itemTwo);
            partitions.get(partitionIdxTwo).set(itemIdxTwo, itemOne);

            PartitionSwap swap = new PartitionSwap(new PartitionShift(itemOne, partitionIdxTwo), new PartitionShift(itemTwo, partitionIdxOne));

            if (!swapList.contains(swap)) {
                swapList.add(swap);
                swapCnt++;
            }
        }

        Solution neighbor = this.generateSolutionFromPartitions(partitions, currSol);
        neighbor.sortItemsInStacksBasedOnTransitiveStackingConstraints();
        neighbor.lowerItemsThatAreStackedInTheAir();
        return neighbor;
    }

    /**
     * Checks whether the swap list contains a tabu swap operation.
     *
     * @param swapList - list of swaps to be checked for tabu operations
     * @return whether or not the swap list contains a tabu swap operation
     */
    public boolean containsTabuSwap(List<PartitionSwap> swapList) {
        for (PartitionSwap swap : swapList) {
            // a swap consists of two shift operations
            if (this.tabuList.contains(swap.getShiftOne()) && this.tabuList.contains(swap.getShiftTwo())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Generates a neighbor for the current solution using the "k-swap-neighborhood".
     *
     * @param numberOfSwaps - number of swaps to be performed
     * @return a neighboring solution
     */
    @SuppressWarnings("Duplicates")
    public Solution getNeighborKSwap(int numberOfSwaps, Solution currSol, Solution bestSol) {

        List<Solution> nbrs = new ArrayList<>();
        int failCnt = 0;
        int unsuccessfulKSwapCounter = 0;

        while (nbrs.size() < this.numberOfNeighbors) {

            if (numberOfSwaps > 1 && unsuccessfulKSwapCounter++ == this.unsuccessfulKSwapAttempts) {
                Solution best = HeuristicUtil.getBestSolution(nbrs);
                if (best.isEmpty()) {
                    return currSol;
                }
                return best;
            }
            List<PartitionSwap> swapList = new ArrayList<>();
            Solution neighbor = this.performSwaps(numberOfSwaps, swapList, currSol);

            if (!neighbor.isFeasible()) { continue; }

            // FIRST-FIT
            if (this.shortTermStrategy == PostOptimization.ShortTermStrategies.FIRST_FIT
                    && !this.containsTabuSwap(swapList) && neighbor.computeCosts() < currSol.computeCosts()) {

                for (PartitionSwap swap : swapList) {
                    this.forbidShift(swap.getShiftOne());
                    this.forbidShift(swap.getShiftTwo());
                }
                return neighbor;

                // BEST-FIT
            } else if (!this.containsTabuSwap(swapList)) {
                nbrs.add(neighbor);
                for (PartitionSwap swap : swapList) {
                    this.forbidShift(swap.getShiftOne());
                    this.forbidShift(swap.getShiftTwo());
                }
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

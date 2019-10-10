package SP.util;

import SP.representations.*;
import org.jgrapht.alg.matching.MaximumWeightBipartiteMatching;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.io.File;
import java.util.*;

/**
 * A collection of general utility methods used in the heuristics.
 *
 * @author Tim Bohne
 */
public class HeuristicUtil {

    /**
     * Returns the list of unmatched items based on the matched items from the triples and pairs.
     * The matched items and the list containing every item are used to determine the unmatched items.
     *
     * @param itemTriples - list of item triples
     * @param itemPairs   - list of item pairs
     * @param items       - list of all items
     * @return list containing the unmatched items
     */
    public static List<Integer> getUnmatchedItemsFromTriplesAndPairs(
        List<List<Integer>> itemTriples, List<MCMEdge> itemPairs, int[] items
    ) {
        List<Integer> matchedItems = new ArrayList<>();
        matchedItems.addAll(getMatchedItemsFromTriples(itemTriples));
        matchedItems.addAll(getMatchedItemsFromPairs(itemPairs));
        return getUnmatchedItemsFromMatchedItems(matchedItems, items);
    }

    /**
     * Returns a list of unmatched items based on the list of matched triples.
     *
     * @param itemTriples - list of item triples
     * @return list containing the unmatched items
     */
    public static List<Integer> getUnmatchedItemsFromTriples(List<List<Integer>> itemTriples, int[] items) {
        return HeuristicUtil.getUnmatchedItemsFromMatchedItems(HeuristicUtil.getMatchedItemsFromTriples(itemTriples), items);
    }

    /**
     * Returns the list of unmatched items based on the given list of item pairs.
     *
     * @param itemPairs - list of matched items
     * @param items     - list containing every item
     * @return list of unmatched items
     */
    public static List<Integer> getUnmatchedItemsFromPairs(List<MCMEdge> itemPairs, int[] items) {
        return HeuristicUtil.getUnmatchedItemsFromMatchedItems(HeuristicUtil.getMatchedItemsFromPairs(itemPairs), items);
    }

    /**
     * Updates the stack assignments in the post processing step with the specified matching.
     * Only one item can be assigned to each stack, because otherwise incompatibilities in terms
     * of the stacking constraints could arise. Therefore each stack is blocked for additional assignments
     * after an item has been assigned to it.
     *
     * @param maxSavingsMatching - matching the stack assignments are updated with
     */
    public static void updateStackAssignmentsInPostProcessing(
        MaximumWeightBipartiteMatching<String, DefaultWeightedEdge> maxSavingsMatching,
        BipartiteGraph postProcessingGraph, Instance instance
    ) {
        List<Integer> blockedStacks = new ArrayList<>();
        Map<Integer, List<Integer>> stackAssignments = new HashMap<>();
        Map<Integer, Double> itemSavings = new HashMap<>();

        HeuristicUtil.prepareUpdateOfStackAssignmentsInPostProcessing(maxSavingsMatching, itemSavings, postProcessingGraph, stackAssignments);

        for (DefaultWeightedEdge edge : maxSavingsMatching.getMatching().getEdges()) {
            int item = GraphUtil.parseItemFromPostProcessingMatching(edge);
            int stack = GraphUtil.parseStackFromPostProcessingMatching(edge);
            int level = GraphUtil.parseLevelFromPostProcessingMatching(edge);

            if (blockedStacks.contains(stack)) { continue; }

            if (stackAssignments.get(stack).size() > 1) {
                int bestItem = stackAssignments.get(stack).get(0);
                for (int i = 1; i < stackAssignments.get(stack).size(); i++) {
                    if (itemSavings.get(stackAssignments.get(stack).get(i)) > itemSavings.get(bestItem)) {
                        bestItem = stackAssignments.get(stack).get(i);
                    }
                }
                HeuristicUtil.updateItemPositionInPostProcessing(bestItem, stack, level, blockedStacks, instance);
            } else {
                HeuristicUtil.updateItemPositionInPostProcessing(item, stack, level, blockedStacks, instance);
            }
        }
    }

    /**
     * Computes the average for the specified list of integers.
     *
     * @param values - list of integer values to compute the average for
     * @return average value
     */
    public static int getAvg(List<Integer> values) {
        int avg = 0;
        for (int val : values) {
            avg += val;
        }
        return avg / values.size();
    }

    /**
     * Computes the manhattan distance between the specified item and stack.
     *
     * @param item           - the item used in the dist computation
     * @param stack          - the stack used in the dist computation
     * @param stackPositions - list of stack positions
     * @return manhattan distance between original item position and stack
     */
    public static double computeManhattanDist(int item, int stack, Item[] items, List<GridPosition> stackPositions) {
        GridPosition itemPosition = items[item].getPosition();
        GridPosition stackPosition = stackPositions.get(stack);
        return Math.abs(itemPosition.getXCoord() - stackPosition.getXCoord()) + Math.abs(itemPosition.getYCoord() - stackPosition.getYCoord());
    }

    /**
     * Returns the percentage of one-entries in the stacking constraint matrix.
     *
     * @param stackingConstraintMatrix - matrix to be considered
     * @return percentage of one-entries
     */
    public static float getPercentageOfOneEntriesInStackingConstraintMatrix(int[][] stackingConstraintMatrix) {
        int numOfEntries = stackingConstraintMatrix.length * stackingConstraintMatrix.length;
        int numOfOnes = 0;
        for (int i = 0; i < stackingConstraintMatrix.length; i++) {
            for (int j = 0; j < stackingConstraintMatrix[i].length; j++) {
                numOfOnes += stackingConstraintMatrix[i][j];
            }
        }
        return ((float)numOfOnes / (float)numOfEntries) * 100;
    }

    /**
     * Returns whether the given item is validly assignable to the given pair.
     *
     * @param item        - the item to be checked
     * @param pairItemOne - the first item of the pair
     * @param pairItemTwo - the second item of the pair
     * @return whether or not the item is assignable to the pair
     */
    public static boolean itemAssignableToPair(int item, int pairItemOne, int pairItemTwo, int[][] stackingConstraints) {
        if (HeuristicUtil.pairStackableInBothDirections(pairItemOne, pairItemTwo, stackingConstraints)) {
            return HeuristicUtil.itemAssignableToPairStackableInBothDirections(stackingConstraints, pairItemOne, pairItemTwo, item);
        } else if (stackingConstraints[pairItemOne][pairItemTwo] == 1) {
            return HeuristicUtil.itemAssignableToPairStackableInOneDirection(stackingConstraints, pairItemTwo, pairItemOne, item);
        } else {
            return HeuristicUtil.itemAssignableToPairStackableInOneDirection(stackingConstraints, pairItemOne, pairItemTwo, item);
        }
    }

    /**
     * Checks whether the specified item can be assigned to the specified pair
     * that is stackable in both directions in a way that respects the stacking constraints.
     *
     * @param stackingConstraints - stacking constraints to be respected
     * @param lowerItemOfPair     - lower item of the pair
     * @param upperItemOfPair     - upper item of the pair
     * @param item                - item to be checked for compatibility with the pair
     * @return whether or not the item is compatible with the pair
     */
    public static boolean itemAssignableToPairStackableInBothDirections(
        int[][] stackingConstraints, int lowerItemOfPair, int upperItemOfPair, int item
    ) {
        return stackingConstraints[lowerItemOfPair][item] == 1
            || stackingConstraints[item][upperItemOfPair] == 1
            || (stackingConstraints[upperItemOfPair][item] == 1 && stackingConstraints[item][lowerItemOfPair] == 1)
            || stackingConstraints[upperItemOfPair][item] == 1
            || (stackingConstraints[lowerItemOfPair][item] == 1 && stackingConstraints[item][upperItemOfPair] == 1)
            || stackingConstraints[item][lowerItemOfPair] == 1;
    }

    /**
     * Checks whether the specified item can be assigned to the specified pair
     * that is stackable in only one direction in a way that respects the stacking constraints.
     *
     * @param stackingConstraints - stacking constraints to be respected
     * @param lowerItemOfPair     - lower item of the pair
     * @param upperItemOfPair     - upper item of the pair
     * @param item                - item to be checked for compatibility with the pair
     * @return whether or not the item is compatible with the pair
     */
    public static boolean itemAssignableToPairStackableInOneDirection(
        int[][] stackingConstraints, int lowerItemOfPair, int upperItemOfPair, int item
    ) {
        return stackingConstraints[lowerItemOfPair][item] == 1
            || stackingConstraints[item][upperItemOfPair] == 1
            || (stackingConstraints[upperItemOfPair][item] == 1 && stackingConstraints[item][lowerItemOfPair] == 1);
    }

    /**
     * Checks whether the specified items are stackable in at least one direction.
     *
     * @param stackingConstraints - stacking constraints to be respected
     * @param itemOne             - first item of the pair to be checked
     * @param itemTwo             - second item of the pair to be checked
     * @return whether or not the pair of items is stackable
     */
    public static boolean pairStackableInAtLeastOneDirection(int[][] stackingConstraints, int itemOne, int itemTwo) {
        return stackingConstraints[itemOne][itemTwo] == 1 || stackingConstraints[itemTwo][itemOne] == 1;
    }

    /**
     * Checks whether the specified items are stackable in both directions.
     *
     * @param itemOne             - first item of the pair to be checked
     * @param itemTwo             - second item of the pair to be checked
     * @param stackingConstraints - stacking constraints to be respected
     * @return whether or not the given items are stackable in both directions
     */
    public static boolean pairStackableInBothDirections(int itemOne, int itemTwo, int[][] stackingConstraints) {
        return stackingConstraints[itemTwo][itemOne] == 1 && stackingConstraints[itemOne][itemTwo] == 1;
    }

    /**
     * Checks whether the specified item is compatible with the specified stack
     * in terms of the placement constraints.
     *
     * @param costs - matrix of transport costs indirectly encoding placement constraints
     * @param item  - item to check compatibility for
     * @param stack - stack to check compatibility for
     * @return whether or not the item is compatible with the stack
     */
    public static boolean itemCompatibleWithStack(double[][] costs, int item, int stack) {
        return costs[item][stack] < Integer.MAX_VALUE / costs.length;
    }

    public static boolean stackHasFreePosition(int[] stack) {
        for (int item : stack) {
            if (item == -1) {
                return true;
            }
        }
        return false;
    }

    public static boolean stackIsEmpty(int[] stack) {
        for (int item : stack) {
            if (item != -1) {
                return false;
            }
        }
        return true;
    }

    // Returns the item objects that are stored in the stack
    public static List<Item> getItemObjectsFromIndices(int[] stack, Item[] itemObjects) {
        List<Item> items = new ArrayList<>();
        for (int itemIdx : stack) {
            if (itemIdx == -1) { continue; }
            items.add(itemObjects[itemIdx]);
        }
        return items;
    }

    public static boolean itemCompatibleWithAlreadyAssignedItemsWithException(
        int item, int[] stack, Item[] itemObjects, int[][] stackingConstraints, int exception
    ) {

        // no assigned items
        if (stackIsEmpty(stack)) { return true; }

        List<Item> items = getItemObjectsFromIndices(stack, itemObjects);
        items.add(itemObjects[item]);
        List<Item> tmpItems = getItemObjectsFromIndices(stack, itemObjects);
        for (Item i : tmpItems) {
            if (i.getIdx() == exception) {
                items.remove(i);
            }
        }

        // sort items with regard to the transitive stacking constraints
        Collections.sort(items);
        for (int i = 0; i < items.size() - 1; i++) {
            if (stackingConstraints[items.get(i).getIdx()][items.get(i + 1).getIdx()] != 1) {
                return false;
            }
        }
        return true;
    }

    public static boolean itemCompatibleWithAlreadyAssignedItems(int item, int[] stack, Item[] itemObjects, int[][] stackingConstraints) {

        // no assigned items
        if (stackIsEmpty(stack)) { return true; }

        List<Item> items = getItemObjectsFromIndices(stack, itemObjects);
        items.add(itemObjects[item]);
        // sort items with regard to the transitive stacking constraints
        Collections.sort(items);

        for (int i = 0; i < items.size() - 1; i++) {
            if (stackingConstraints[items.get(i).getIdx()][items.get(i + 1).getIdx()] != 1) {
                return false;
            }
        }

        return true;
    }

    public static void assignItemToStack(int item, int[] stack, Item[] itemObjects) {
        List<Item> items = getItemObjectsFromIndices(stack, itemObjects);
        items.add(itemObjects[item]);
        // sort items with regard to the transitive stacking constraints
        Collections.sort(items);
        // clear previous assignments
        for (int i = 0; i < stack.length; i++) { stack[i] = -1; }
        // assign items in new order
        for (int i = 0; i < items.size(); i++) {
            stack[i] = items.get(i).getIdx();
        }
    }

    public static boolean completelyFilledStack(int[] stack) {
        for (int i = 0; i < stack.length; i++) {
            if (stack[i] == -1) {
                return false;
            }
        }
        return true;
    }

    public static void removeItemFromStack(int item, int[] stack) {
        for (int i = 0; i < stack.length; i++) {
            if (stack[i] == item) {
                stack[i] = -1;
            }
        }
    }

    /**
     * Creates an item array from the given list of items.
     *
     * @param items - list of items
     * @return array of the given items
     */
    public static int[] getItemArrayFromItemList(List<Integer> items) {
        int[] itemArr = new int[items.size()];
        for (int i = 0; i < items.size(); i++) {
            itemArr[i] = items.get(i);
        }
        return itemArr;
    }

    /**
     * Returns the savings for the specified item.
     *
     * @param stackIdx    - index of the considered stack
     * @param costsBefore - original costs for each item assignment
     * @param item        - item the savings are computed for
     * @return savings for the specified item
     */
    public static double getSavingsForItem(int stackIdx, Map<Integer, Double> costsBefore, int item, double[][] costMatrix) {
        double costs = costMatrix[item][stackIdx];
        return costsBefore.get(item) - costs;
    }

    /**
     * Returns the maximum savings for the specified pair.
     *
     * @param itemOne     - first item of the pair
     * @param itemTwo     - second item of the pair
     * @param stackIdx    - target stack
     * @param costsBefore - original costs for each item assignment
     * @param costMatrix  - costs for each item-stack assignment
     * @return savings for the specified pair
     */
    public static double getSavingsForPair(int itemOne, int itemTwo, int stackIdx, Map<Integer, Double> costsBefore, double[][] costMatrix) {
        double costsItemOne = costMatrix[itemOne][stackIdx];
        double costsItemTwo = costMatrix[itemTwo][stackIdx];
        double savingsItemOne = costsBefore.get(itemOne) - costsItemOne;
        double savingsItemTwo = costsBefore.get(itemTwo) - costsItemTwo;
        return savingsItemOne > savingsItemTwo ? savingsItemOne : savingsItemTwo;
    }

    /**
     * Returns the maximum savings for the specified triple.
     *
     * @param itemOne     - first item of triple
     * @param itemTwo     - second item of triple
     * @param itemThree   - third item of triple
     * @param stackIdx    - index of considered stack
     * @param costsBefore - original costs for each item assignment
     * @param costMatrix  - costs for each item-stack assignment
     * @return savings for the specified triple
     */
    public static double getSavingsForTriple(
        int itemOne, int itemTwo, int itemThree, int stackIdx, Map<Integer, Double> costsBefore, double[][] costMatrix
    ) {
        double costsItemOne = costMatrix[itemOne][stackIdx];
        double costsItemTwo = costMatrix[itemTwo][stackIdx];
        double costsItemThree = costMatrix[itemThree][stackIdx];

        double savingsItemOne = costsBefore.get(itemOne) - costsItemOne;
        double savingsItemTwo = costsBefore.get(itemTwo) - costsItemTwo;
        double savingsItemThree = costsBefore.get(itemThree) - costsItemThree;

        List<Double> savings = new ArrayList<>(Arrays.asList(savingsItemOne, savingsItemTwo, savingsItemThree));
        return Collections.max(savings);
    }

    /**
     * Updates the stack assignments for the compatible triple.
     *
     * @param itemOne     - first item of the triple
     * @param itemTwo     - second item of the triple
     * @param itemThree   - third item of the triple
     * @param stack       - index of the considered stack
     * @param costsBefore - original costs for each item-stack assignment
     * @param instance    - considered instance of the stacking problem
     */
    public static void updateAssignmentsForCompatibleTriple(
        int itemOne, int itemTwo, int itemThree, int stack, Map<Integer, Double> costsBefore, Instance instance
    ) {
        int itemWithMaxSavings = HeuristicUtil.getItemWithMaxSavingsForTriple(
            itemOne, itemTwo, itemThree, stack, costsBefore, instance.getCosts()
        );
        HeuristicUtil.removeItemFromStacks(itemWithMaxSavings, instance.getStacks());
        instance.getStacks()[stack][instance.getGroundLevel()] = itemWithMaxSavings;
    }

    /**
     * Updates the stack assignments for the specified pair.
     *
     * @param itemOne     - first item of the pair
     * @param itemTwo     - second item of the pair
     * @param stack       - target stack
     * @param costsBefore - costs for the original item assignments
     * @param instance    - considered instance of the stacking problem
     */
    public static void updateStackAssignmentsForPairs(
        int itemOne, int itemTwo, int stack, Map<Integer, Double> costsBefore, Instance instance
    ) {
        // both items compatible
        if (HeuristicUtil.itemCompatibleWithStack(instance.getCosts(), itemOne, stack)
            && HeuristicUtil.itemCompatibleWithStack(instance.getCosts(), itemTwo, stack)) {

                HeuristicUtil.updateAssignmentsForCompatiblePair(itemOne, stack, itemTwo, costsBefore, instance);

        // item one compatible
        } else if (HeuristicUtil.itemCompatibleWithStack(instance.getCosts(), itemOne, stack)) {
            HeuristicUtil.removeItemFromStacks(itemOne, instance.getStacks());
            instance.getStacks()[stack][instance.getGroundLevel()] = itemOne;
        // item two compatible
        } else if (HeuristicUtil.itemCompatibleWithStack(instance.getCosts(), itemTwo, stack)) {
            HeuristicUtil.removeItemFromStacks(itemTwo, instance.getStacks());
            instance.getStacks()[stack][instance.getGroundLevel()] = itemTwo;
        }
    }

    /**
     * Retrieves all the stacks that remain empty in the solution.
     *
     * @param sol - solution to be checked for empty stacks
     * @return list of remaining empty stacks
     */
    public static List<String> retrieveEmptyStacks(Solution sol) {

        int stackCapacity = sol.getFilledStacks()[0].length;

        List<String> emptyStacks = new ArrayList<>();
        for (int stack = 0; stack < sol.getFilledStacks().length; stack++) {
            int sumOfEntries = 0;
            for (int entry : sol.getFilledStacks()[stack]) {
                sumOfEntries += entry;
            }
            if (sumOfEntries == -stackCapacity) {
                emptyStacks.add("stack" + stack);
            }
        }
        return emptyStacks;
    }

    /**
     * Retrieves the empty positions from the stacks.
     *
     * @param sol - solution to retrieve the empty positions for
     * @return list of empty positions in the stacks
     */
    public static List<StackPosition> retrieveEmptyPositions(Solution sol) {
        List<StackPosition> emptyPositions = new ArrayList<>();
        for (int stack = 0; stack < sol.getFilledStacks().length; stack++) {
            for (int level = 0; level < sol.getFilledStacks()[stack].length; level++) {
                if (sol.getFilledStacks()[stack][level] == -1) {
                    emptyPositions.add(new StackPosition(stack, level));
                }
            }
        }
        return emptyPositions;
    }

    /**
     * Returns the costs for each item's assignment in the solution.
     *
     * @param sol - solution to get the costs for
     * @return original costs for each item assignment
     */
    public static Map<Integer, Double> getOriginalCosts(Solution sol, double[][] costMatrix) {
        Map<Integer, Double> originalCosts = new HashMap<>();
        for (int stack = 0; stack < sol.getFilledStacks().length; stack++) {
            for (int entry : sol.getFilledStacks()[stack]) {
                if (entry != -1) {
                    originalCosts.put(entry, costMatrix[entry][stack]);
                }
            }
        }
        return originalCosts;
    }

    /**
     * Returns the best solution from the list of generated solutions based on the transport costs.
     *
     * @param solutions - list of generated solutions
     * @return best solution based on the costs
     */
    public static Solution getBestSolution(List<Solution> solutions) {
        Solution bestSol = new Solution();
        for (Solution sol : solutions) {
            if (sol.computeCosts() < bestSol.computeCosts()) {
                bestSol = sol;
            }
        }
        return bestSol;
    }

    /**
     * Returns the percentage deviation between the expected and the actual value.
     *
     * @param expected - expected value
     * @param actual   - actual value
     * @return percentage deviation between expected and actual value
     */
    public static int getPercentageDeviation(float expected, float actual) {
        float diff = Math.abs(expected - actual);
        return Math.round(diff / expected * 100);
    }

    /**
     * Returns a random value in between the specified limits.
     *
     * @param min - lower bound of the value (inclusive)
     * @param max - upper bound of the value (inclusive)
     * @return random value in between the specified limits
     */
    public static float getRandomValueInBetween(float min, float max) {
        Random r = new Random();
        return (float)(Math.round(min + r.nextFloat() * (max - min) * 10.0) / 10.0);
    }

    /**
     * Rounds the given value to the next half.
     *
     * @param val - value to be rounded
     * @return rounded value
     */
    public static float roundToHalf(float val) {
        return (float)(Math.round(val * 2) / 2.0);
    }

    /**
     * Returns a random value in between the specified limits in .5 steps.
     *
     * @param min - lower bound of the value
     * @param max - upper bound of the value
     * @return random value in between the specified limits
     */
    public static float getRandomValueInBetweenHalfSteps(float min, float max) {
        Random r = new Random();
        float val = roundToHalf((float)(Math.round(min + r.nextFloat() * (max - min) * 10.0) / 10.0));
        if (val > max) { val = max; }
        return val;
    }

    /**
     * Returns a random integer in between the specified limits.
     *
     * @param min - lower bound of the value
     * @param max - upper bound of the value
     * @return random value in between the specified limits (inclusive limits)
     */
    public static int getRandomIntegerInBetween(int min, int max) {
        Random r = new Random();
        return r.nextInt((max - min) + 1) + min;
    }

    /**
     * Returns a random integer in between the specified limits except the specified value.
     *
     * @param min       - lower bound of the value
     * @param max       - upper bound of the value
     * @param exception - value not to be returned
     * @return random value in between the specified limits (inclusive limits)
     */
    public static int getRandomIntegerInBetweenWithException(int min, int max, int exception) {
        Random r = new Random();
        int val = r.nextInt((max - min) + 1) + min;
        while (val == exception) {
            val = r.nextInt((max - min) + 1) + min;
        }
        return val;
    }

    /**
     * Returns whether the specified list of items contains duplicates.
     *
     * @param items - list of items to be checked
     * @return whether or not the list contains duplicates
     */
    public static boolean itemListContainsDuplicates(List<Integer> items) {
        Set<Integer> set = new HashSet<>(items);
        return set.size() < items.size();
    }

    /**
     * Returns a copy of the specified edge list.
     *
     * @param edges - list of edges to be copied
     * @return copy of edge list
     */
    public static List<MCMEdge> getCopyOfEdgeList(List<MCMEdge> edges) {
        List<MCMEdge> copy = new ArrayList<>();
        for (MCMEdge edge : edges) {
            copy.add(new MCMEdge(edge));
        }
        return copy;
    }

    /**
     * Returns a reversed copy of the specified edge list.
     *
     * @param edges - list of edges to be reversed
     * @return reversed edge list
     */
    public static List<MCMEdge> getReversedCopyOfEdgeList(List<MCMEdge> edges) {
        List<MCMEdge> edgesRev = new ArrayList<>(edges);
        Collections.reverse(edgesRev);
        return edgesRev;
    }

    /**
     * Sorts the given map by value.
     *
     * @param map - map to be sorted
     * @return sorted map
     */
    public static <K, V extends Comparable<? super V>> Map<K, V> sortMapByValue(Map<K, V> map) {
        List<Map.Entry<K, V>> list = new ArrayList<>(map.entrySet());
        list.sort(Map.Entry.comparingByValue());
        Map<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    /**
     * Creates a string containing the names of all solutions.
     *
     * @return string containing the names of all solutions
     */
    public static String createStringContainingAllSolutionNames(String prefix) {
        File dir = new File(prefix);
        File[] dirListing = dir.listFiles();
        StringBuilder str = new StringBuilder();
        assert dirListing != null;
        for (File f : dirListing) {
            str.append(f.toString()).append(" ");
        }
        return str.toString();
    }

    /**
     * Returns a list containing all the matched items that are part of an item pair.
     *
     * @param itemPairs - pairs of items
     * @return matched items from pairs
     */
    private static List<Integer> getMatchedItemsFromPairs(List<MCMEdge> itemPairs) {
        List<Integer> matchedItems = new ArrayList<>();
        for (MCMEdge pair : itemPairs) {
            matchedItems.add(pair.getVertexOne());
            matchedItems.add(pair.getVertexTwo());
        }
        return matchedItems;
    }

    /**
     * Returns a list containing all the matched items that are part of an item triple.
     *
     * @param itemTriples - triples of items
     * @return matched items from triples
     */
    private static List<Integer> getMatchedItemsFromTriples(List<List<Integer>> itemTriples) {
        List<Integer> matchedItems = new ArrayList<>();
        for (List<Integer> triple : itemTriples) {
            matchedItems.addAll(triple);
        }
        return matchedItems;
    }

    /**
     * Returns the list of unmatched items based on the list of matched items.
     *
     * @param matchedItems - list of matched items
     * @param items        - list containing every item
     * @return list of unmatched items
     */
    private static List<Integer> getUnmatchedItemsFromMatchedItems(List<Integer> matchedItems, int[] items) {
        List<Integer> unmatchedItems = new ArrayList<>();
        for (int item : items) {
            if (!matchedItems.contains(item)) {
                unmatchedItems.add(item);
            }
        }
        return unmatchedItems;
    }

    /**
     * Prepares the update of stack assignments in the post processing step by creating lists of items that
     * are assigned to the same stack and storing the savings for each item.
     *
     * @param maxSavingsMatching  - matching to be processed
     * @param itemSavings         - map to be filled with each item's saving
     * @param postProcessingGraph - post-processing graph containing the items and empty positions
     * @param stackAssignments    - map to be filled with the stacks and the items assigned to each stack
     */
    private static void prepareUpdateOfStackAssignmentsInPostProcessing(
        MaximumWeightBipartiteMatching<String, DefaultWeightedEdge> maxSavingsMatching, Map<Integer, Double> itemSavings,
        BipartiteGraph postProcessingGraph, Map<Integer, List<Integer>> stackAssignments
    ) {
        for (DefaultWeightedEdge edge : maxSavingsMatching.getMatching().getEdges()) {
            int item = GraphUtil.parseItemFromPostProcessingMatching(edge);
            int stack = GraphUtil.parseStackFromPostProcessingMatching(edge);
            itemSavings.put(item, postProcessingGraph.getGraph().getEdgeWeight(edge));
            if (stackAssignments.containsKey(stack)) {
                stackAssignments.get(stack).add(item);
            } else {
                stackAssignments.put(stack, new ArrayList<>());
            }
        }
    }

    /**
     * Updates the item position in the storage area in the post processing step and adds
     * the used stack to the list of blocked stacks.
     *
     * @param item          - item to be assigned to new position
     * @param stack         - stack the item gets assigned to
     * @param level         - level the item gets assigned to
     * @param blockedStacks - list of blocked stacks (only one item per stack in post-processing)
     */
    private static void updateItemPositionInPostProcessing(int item, int stack, int level, List<Integer> blockedStacks, Instance instance) {
        HeuristicUtil.removeItemFromStacks(item, instance.getStacks());
        instance.getStacks()[stack][level] = item;
        blockedStacks.add(stack);
    }

    /**
     * Removes the specified item from the stack it is assigned to.
     *
     * @param item   - item to be removed
     * @param stacks - containing the stack the item gets removed from
     */
    private static void removeItemFromStacks(int item, int[][] stacks) {
        for (int stack = 0; stack < stacks.length; stack++) {
            for (int level = 0; level < stacks[stack].length; level++) {
                if (stacks[stack][level] == item) {
                    stacks[stack][level] = -1;
                }
            }
        }
    }

    /**
     * Returns the item of the triple that leads to the maximum savings.
     *
     * @param itemOne     - first item of the triple
     * @param itemTwo     - second item of the triple
     * @param itemThree   - third item of the triple
     * @param stackIdx    - index of the considered stack
     * @param costsBefore - original costs for each item-stack assignment
     * @param costMatrix  - costs for each item-stack assignment
     * @return item with the maximum savings
     */
    private static int getItemWithMaxSavingsForTriple(
        int itemOne, int itemTwo, int itemThree, int stackIdx, Map<Integer, Double> costsBefore, double[][] costMatrix
    ) {
        double costsItemOne = costMatrix[itemOne][stackIdx];
        double costsItemTwo = costMatrix[itemTwo][stackIdx];
        double costsItemThree = costMatrix[itemThree][stackIdx];

        double savingsItemOne = costsBefore.get(itemOne) - costsItemOne;
        double savingsItemTwo = costsBefore.get(itemTwo) - costsItemTwo;
        double savingsItemThree = costsBefore.get(itemThree) - costsItemThree;

        List<Double> savings = new ArrayList<>(Arrays.asList(savingsItemOne, savingsItemTwo, savingsItemThree));
        double maxSavings = Collections.max(savings);

        if (maxSavings == savingsItemOne) {
            return itemOne;
        } else if (maxSavings == savingsItemTwo) {
            return itemTwo;
        } else {
            return itemThree;
        }
    }

    /**
     * Updates the stack assignments for the compatible pair.
     *
     * @param itemOne     - first item of the pair
     * @param stack       - considered stack
     * @param itemTwo     - second item of the pair
     * @param costsBefore - original costs for each item-stack assignment
     * @param instance    - considered instance of the stacking problem
     */
    private static void updateAssignmentsForCompatiblePair(
        int itemOne, int stack, int itemTwo, Map<Integer, Double> costsBefore, Instance instance
    ) {
        double costsItemOne = instance.getCosts()[itemOne][stack];
        double costsItemTwo = instance.getCosts()[itemTwo][stack];
        double savingsItemOne = costsBefore.get(itemOne) - costsItemOne;
        double savingsItemTwo = costsBefore.get(itemTwo) - costsItemTwo;

        if (savingsItemOne > savingsItemTwo) {
            HeuristicUtil.removeItemFromStacks(itemOne, instance.getStacks());
            instance.getStacks()[stack][instance.getGroundLevel()] = itemOne;
        } else {
            HeuristicUtil.removeItemFromStacks(itemTwo, instance.getStacks());
            instance.getStacks()[stack][instance.getGroundLevel()] = itemTwo;
        }
    }
}

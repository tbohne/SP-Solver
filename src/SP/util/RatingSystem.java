package SP.util;

import SP.representations.MCMEdge;

import java.util.*;

/**
 * A collection of utility methods providing a rating system
 * for items / edges used in some of the heuristics.
 *
 * @author Tim Bohne
 */
public class RatingSystem {

    /**
     * Computes the row rating for the given item.
     * The row rating is the number of items the given item can be stacked upon.
     *
     * @param item                - item for which the row rating gets computed
     * @param stackingConstraints - stacking constraints the rating is based on
     * @return row rating of the item
     */
    public static int computeRowRatingForItem(int item, int[][] stackingConstraints) {
        int rating = 0;
        for (int entry : stackingConstraints[item]) {
            rating += entry;
        }
        return rating;
    }

    /**
     * Computes the column rating for the given item.
     * The column rating is the number of items that can be stacked upon the given item.
     *
     * @param item                - item for which the col rating gets computed
     * @param stackingConstraints - stacking constraints the rating is based on
     * @return col rating of the item
     */
    public static int computeColRatingForItem(int item, int[][] stackingConstraints) {
        int rating = 0;
        for (int[] stackingConstraint : stackingConstraints) {
            rating += stackingConstraint[item];
        }
        return rating;
    }

    /**
     * Computes the worst compatibility for the given pair, which is the number of compatible
     * items of the less compatible item of the pair.
     *
     * @param edge                - edge in the MCM (item pair)
     * @param stackingConstraints - stacking constraints the compatibility is based on
     * @param matchedItems        - edges in the MCM (pairs of items)
     * @return computed worst compatibility
     */
    public static int computeWorstCompatibilityForPair(MCMEdge edge, int[][] stackingConstraints, List<MCMEdge> matchedItems) {
        int itemOne = edge.getVertexOne();
        int itemTwo = edge.getVertexTwo();
        int pairCompatibilityItemOne = computePairCompatibility(matchedItems, itemOne, stackingConstraints, edge);
        int pairCompatibilityItemTwo = computePairCompatibility(matchedItems, itemTwo, stackingConstraints, edge);
        return pairCompatibilityItemOne > pairCompatibilityItemTwo ? pairCompatibilityItemTwo : pairCompatibilityItemOne;
    }

    /**
     * Returns a rating for a pair of items based on its average worst compatibility
     * and its average costs for a stack assignment.
     *
     * @param listOfPairValues      - list containing the values (comp / costs) for the item pairs
     * @param avgWorstCompatibility - average worst compatibility of an item pair
     * @param avgCosts              - average costs for a stack assignment of an item pair
     * @param pairIdx               - index of the considered pair
     * @param deviationThreshold    - deviation threshold for avg costs and avg worst compatibility
     * @return computed rating
     */
    public static int getRatingBasedOnCompatibilityAndCosts(
        List<List<Integer>> listOfPairValues, int avgWorstCompatibility, int avgCosts, int pairIdx, int deviationThreshold
    ) {
        int rating = 0;
        if (listOfPairValues.get(pairIdx).get(0) > avgWorstCompatibility && listOfPairValues.get(pairIdx).get(1) > avgCosts) {
            int compatibilityDeviation = HeuristicUtil.getPercentageDeviation(avgWorstCompatibility, listOfPairValues.get(pairIdx).get(0));
            int costDeviation = HeuristicUtil.getPercentageDeviation(avgCosts, listOfPairValues.get(pairIdx).get(1));
            if (compatibilityDeviation + costDeviation > deviationThreshold) {
                rating = compatibilityDeviation + costDeviation;
            }
        }
        return rating;
    }

    /**
     * A rating system developed for the 3Cap heuristic (can be used for other capacities as well).
     * It assigns ratings to pairs of items considering their compatibility to other
     * items and their costs for being assigned to a stack together.
     *
     * @param matchedItems        - edges (pairs) to be rated
     * @param stackingConstraints - stacking constraints to be respected
     * @param costs               - matrix containing transport costs
     * @param stacks              - available stacks
     * @param deviationThreshold  - determines whether an edge is rated higher then 0
     */
    public static void assignPairRating(
        List<MCMEdge> matchedItems, int[][] stackingConstraints, double[][] costs, int[][] stacks, int deviationThreshold, float penaltyFactor
    ) {
        List<List<Integer>> listOfPairValues = new ArrayList<>();
        List<Integer> worstCompatibilities = new ArrayList<>();
        List<Integer> avgCostValues = new ArrayList<>();

        for (MCMEdge edge : matchedItems) {
            int worstCompatibility = computeWorstCompatibilityForPair(edge, stackingConstraints, matchedItems);
            int forbiddenVal = Integer.MAX_VALUE / stackingConstraints.length;
            double avgCosts = computeAvgCosts(stacks, costs, edge.getVertexOne(), edge.getVertexTwo(), forbiddenVal, penaltyFactor);

            List<Integer> pairValues = new ArrayList<>();
            pairValues.add(worstCompatibility);
            pairValues.add((int)avgCosts);
            listOfPairValues.add(pairValues);
            worstCompatibilities.add(worstCompatibility);
            avgCostValues.add((int)avgCosts);
        }
        int avgWorstCompatibility = HeuristicUtil.getAvg(worstCompatibilities);
        int avgCosts = HeuristicUtil.getAvg(avgCostValues);

        for (int i = 0; i < matchedItems.size(); i++) {
            int rating = getRatingBasedOnCompatibilityAndCosts(listOfPairValues, avgWorstCompatibility, avgCosts, i, deviationThreshold);
            matchedItems.get(i).setRating(rating);
        }
    }

    /**
     * Assigns the row rating to the specified edges.
     *
     * @param matchedItems        - matched items (edges) to be rated
     * @param stackingConstraints - stacking constraints the ratings are based on
     */
    public static void assignRowRatingToEdges(List<MCMEdge> matchedItems, int[][] stackingConstraints) {
        for (MCMEdge edge : matchedItems) {
            edge.setRating(computeRowRatingForItem(edge.getVertexOne(), stackingConstraints)
                + computeRowRatingForItem(edge.getVertexTwo(), stackingConstraints));
        }
    }

    /**
     * Assigns the col rating to the specified edges.
     *
     * @param matchedItems        - matched items (edges) to be rated
     * @param stackingConstraints - stacking constraints the ratings are based on
     */
    public static void assignColRatingToEdges(List<MCMEdge> matchedItems, int[][] stackingConstraints) {
        for (MCMEdge edge : matchedItems) {
            edge.setRating(computeColRatingForItem(edge.getVertexOne(), stackingConstraints)
                + computeColRatingForItem(edge.getVertexTwo(), stackingConstraints));
        }
    }

    /**
     * Provides a map containing the ratings of the item pair.
     *
     * @param itemOne             - first item of the pair
     * @param itemTwo             - second item of the pair
     * @param stackingConstraints - stacking constraints the ratings are based on
     * @return map containing the ratings
     */
    public static Map<Integer, String> getRatingsMapForItemPair(int itemOne, int itemTwo, int[][] stackingConstraints) {
        Map<Integer, String> itemRatings = new HashMap<>();
        itemRatings.put(RatingSystem.computeRowRatingForItem(itemOne, stackingConstraints), "itemOneRow");
        itemRatings.put(RatingSystem.computeColRatingForItem(itemOne, stackingConstraints), "itemOneCol");
        itemRatings.put(RatingSystem.computeRowRatingForItem(itemTwo, stackingConstraints), "itemTwoRow");
        itemRatings.put(RatingSystem.computeColRatingForItem(itemTwo, stackingConstraints), "itemTwoCol");
        return itemRatings;
    }

    /**
     * Provides a list of the ratings of the item pair.
     *
     * @param itemOne             - first item of the pair
     * @param itemTwo             - second item of the pair
     * @param stackingConstraints - stacking constraints the ratings are based on
     * @return list containing the ratings
     */
    public static List<Integer> getItemRatingsForItemPair(int itemOne, int itemTwo, int[][] stackingConstraints) {
        Map<Integer, String> itemRatings = getRatingsMapForItemPair(itemOne, itemTwo, stackingConstraints);
        List<Integer> ratings = new ArrayList<>();
        ratings.addAll(itemRatings.keySet());
        return ratings;
    }

    /**
     * Returns the sum of the relevant ratings for the item pair.
     *
     * @param itemOne             - first item of the pair
     * @param itemTwo             - second item of the pair
     * @param stackingConstraints - stacking constraints the ratings are based on
     * @return sum of the relevant item ratings
     */
    public static int getSumOfRelevantRatingsForItemPair(int itemOne, int itemTwo, int[][] stackingConstraints) {
        if (HeuristicUtil.pairStackableInBothDirections(itemOne, itemTwo, stackingConstraints)) {
            return computeRowRatingForItem(itemOne, stackingConstraints)
                + computeColRatingForItem(itemOne, stackingConstraints)
                + computeRowRatingForItem(itemTwo, stackingConstraints)
                + computeColRatingForItem(itemTwo, stackingConstraints);

        } else if (stackingConstraints[itemOne][itemTwo] == 1) {
            // item one upon item two
            int colItemOne = computeColRatingForItem(itemOne, stackingConstraints);
            int rowItemTwo = computeRowRatingForItem(itemTwo, stackingConstraints);
            return colItemOne + rowItemTwo;
        } else {
            // item two upon item one
            int colItemTwo = computeColRatingForItem(itemTwo, stackingConstraints);
            int rowItemOne = computeRowRatingForItem(itemOne, stackingConstraints);
            return colItemTwo + rowItemOne;
        }
    }

    /**
     * Returns the extreme value from the relevant ratings of the pair.
     *
     * @param itemOne             - first item of the pair
     * @param itemTwo             - second item of the pair
     * @param stackingConstraints - stacking constraints the ratings are based on
     * @param min                 - specifies whether the min value should be returned (max otherwise)
     * @return extreme rating value
     */
    public static int getExtremeOfRelevantRatingsForItemPair(int itemOne, int itemTwo, int[][] stackingConstraints, boolean min) {
        if (HeuristicUtil.pairStackableInBothDirections(itemOne, itemTwo, stackingConstraints)) {
            if (min) {
                return Collections.min(getItemRatingsForItemPair(itemOne, itemTwo, stackingConstraints));
            } else {
                return Collections.max(getItemRatingsForItemPair(itemOne, itemTwo, stackingConstraints));
            }
        } else if (stackingConstraints[itemOne][itemTwo] == 1) {
            // item one upon item two
            // min(col rating item one, row rating item two)
            int colItemOne = computeColRatingForItem(itemOne, stackingConstraints);
            int rowItemTwo = computeRowRatingForItem(itemTwo, stackingConstraints);
            if (min) {
                return colItemOne < rowItemTwo ? colItemOne : rowItemTwo;
            } else {
                return colItemOne > rowItemTwo ? colItemOne : rowItemTwo;
            }
        } else {
            // item two upon item one
            // min(col rating item two, row rating item one)
            int colItemTwo = computeColRatingForItem(itemTwo, stackingConstraints);
            int rowItemOne = computeRowRatingForItem(itemOne, stackingConstraints);
            if (min) {
                return colItemTwo < rowItemOne ? colItemTwo : rowItemOne;
            } else {
                return colItemTwo > rowItemOne ? colItemTwo : rowItemOne;
            }
        }
    }

    /**
     * Rates the given edges based on their items' compatibility to other edges.
     *
     * @param matchedItems        - edges (pairs) to be rated
     * @param stackingConstraints - stacking constraints to be respected
     */
    public static void assignCompatibilityRatingToEdges(List<MCMEdge> matchedItems, int[][] stackingConstraints) {
        for (MCMEdge edge : matchedItems) {
            int itemOne = edge.getVertexOne();
            int itemTwo = edge.getVertexTwo();
            int compatibility = 0;

            for (MCMEdge e : matchedItems) {
                if (e != edge) {
                    if (HeuristicUtil.itemAssignableToPair(itemOne, e.getVertexOne(), e.getVertexTwo(), stackingConstraints)
                        || HeuristicUtil.itemAssignableToPair(itemTwo, e.getVertexOne(), e.getVertexTwo(), stackingConstraints)) {
                            compatibility++;
                    }
                }
            }
            edge.setRating(compatibility);
        }
    }

    /**
     * Rates the given edges based on their extreme cost values.
     *
     * @param matchedItems        - edges (pairs) to be rated
     * @param stackingConstraints - stacking constraints to be respected
     * @param costs               - cost matrix
     * @param stacks              - available stacks
     * @param max                 - determines whether the max value is used (min otherwise)
     */
    public static void assignExtremeCostRatingToEdges(
        List<MCMEdge> matchedItems, int[][] stackingConstraints, int[][] costs, int[][] stacks, boolean max
    ) {
        for (MCMEdge edge : matchedItems) {
            List<Integer> costEntries = new ArrayList<>();
            int itemOne = edge.getVertexOne();
            int itemTwo = edge.getVertexTwo();
            int forbiddenVal = Integer.MAX_VALUE / stackingConstraints.length;

            for (int stack = 0; stack < stacks.length; stack++) {
                if (costs[itemOne][stack] < forbiddenVal && costs[itemTwo][stack] < forbiddenVal) {
                    int currCost = costs[itemOne][stack] + costs[itemTwo][stack];
                    costEntries.add(currCost);
                }
            }
            if (max) {
                edge.setRating(Collections.max(costEntries));
            } else {
                edge.setRating(Collections.min(costEntries));
            }
        }
    }

    /**
     * Computes a value that is used as penalty value for incompatible stacks.
     * The penalty value is penaltyFactor times the avg costs of a feasible item-stack assignment.
     *
     * @param costs        - matrix containing the transport costs
     * @param forbiddenVal - cost value representing incompatible stacks
     * @return computed penalty value
     */
    public static double computePenaltyValue(double[][] costs, int forbiddenVal, float penaltyFactor) {
        double avgFeasibleCosts = 0;
        int cnt = 0;
        for (double[] cost : costs) {
            for (double aCost : cost) {
                if (aCost < forbiddenVal) {
                    avgFeasibleCosts += aCost;
                    cnt++;
                }
            }
        }
        return avgFeasibleCosts / cnt * penaltyFactor;
    }

    /**
     * Computes the average costs to assign the given pair.
     * An incompatible stack is penalized with a high cost value.
     *
     * @param stacks       - available stacks
     * @param costs        - matrix of transport costs
     * @param itemOne      - first item of the pair
     * @param itemTwo      - second item of the pair
     * @param forbiddenVal - value indicating incompatible stacks
     * @return average costs (with penalties)
     */
    public static double computeAvgCosts(int[][] stacks, double[][] costs, int itemOne, int itemTwo, int forbiddenVal, float penaltyFactor) {

        double penaltyVal = computePenaltyValue(costs, forbiddenVal, penaltyFactor);

        double avgCosts = 0;
        for (int stack = 0; stack < stacks.length; stack++) {
            if (costs[itemOne][stack] < forbiddenVal && costs[itemTwo][stack] < forbiddenVal) {
                double currCost = costs[itemOne][stack] + costs[itemTwo][stack];
                avgCosts += currCost;
            } else {
                avgCosts += penaltyVal;
            }
        }
        return avgCosts / stacks.length;
    }

    /**
     * Computes an item's compatibility to other pairs which is the number of other pairs it is assignable to.
     *
     * @param pairs               - list of item pairs to be checked
     * @param item                - item to compute the compatibility for
     * @param stackingConstraints - stacking constraints to compatibility is based on
     * @param itemEdge            - edge the item to be checked is part of
     * @return pair compatibility
     */
    public static int computePairCompatibility(List<MCMEdge> pairs, int item, int[][] stackingConstraints, MCMEdge itemEdge) {
        int pairCompatibility = 0;
        for (MCMEdge pair : pairs) {
            if (itemEdge != pair) {
                if (HeuristicUtil.itemAssignableToPair(item, pair.getVertexOne(), pair.getVertexTwo(), stackingConstraints)) {
                    pairCompatibility++;
                }
            }
        }
        return pairCompatibility;
    }

    /**
     * Assigns the sum of the ratings to the specified edges.
     *
     * @param matchedItems        - matched items (edges) to be rated
     * @param stackingConstraints - stacking constraints the ratings are based on
     */
    public static void assignSumRatingToEdges(List<MCMEdge> matchedItems, int[][] stackingConstraints) {
        for (MCMEdge edge : matchedItems) {
            int itemOne = edge.getVertexOne();
            int itemTwo = edge.getVertexTwo();
            edge.setRating(getSumOfRelevantRatingsForItemPair(itemOne, itemTwo, stackingConstraints));
        }
    }

    /**
     * Assigns the minimum rating to the specified edges.
     *
     * @param matchedItems        - matched items (edges) to be rated
     * @param stackingConstraints - tacking constraints the ratings are based on
     */
    public static void assignMinRatingToEdges(List<MCMEdge> matchedItems, int[][] stackingConstraints) {
        for (MCMEdge edge : matchedItems) {
            int itemOne = edge.getVertexOne();
            int itemTwo = edge.getVertexTwo();
            edge.setRating(getExtremeOfRelevantRatingsForItemPair(itemOne, itemTwo, stackingConstraints, true));
        }
    }

    /**
     * Assigns the maximum rating to the specified edges.
     *
     * @param matchedItems        - matched items (edges) to be rated
     * @param stackingConstraints - stacking constraints the ratings are based on
     */
    public static void assignMaxRatingToEdges(List<MCMEdge> matchedItems, int[][] stackingConstraints) {
        for (MCMEdge edge : matchedItems) {
            int itemOne = edge.getVertexOne();
            int itemTwo = edge.getVertexTwo();
            edge.setRating(getExtremeOfRelevantRatingsForItemPair(itemOne, itemTwo, stackingConstraints, false));
        }
    }

    /**
     * Returns the list of unmatched items increasingly sorted by col rating.
     *
     * @param unmatchedItems - unsorted list of unmatched items
     * @return sorted list of unmatched items
     */
    public static List<Integer> getUnmatchedItemsSortedByColRating(List<Integer> unmatchedItems, int[][] stackingConstraints) {
        Map<Integer, Integer> unmatchedItemColRatings = new HashMap<>();
        for (int item : unmatchedItems) {
            unmatchedItemColRatings.put(item, RatingSystem.computeColRatingForItem(item, stackingConstraints));
        }
        Map<Integer, Integer> sortedItemColRatings = HeuristicUtil.sortMapByValue(unmatchedItemColRatings);
        List<Integer> unmatchedItemsSortedByColRating = new ArrayList<>();
        unmatchedItemsSortedByColRating.addAll(sortedItemColRatings.keySet());
        return unmatchedItemsSortedByColRating;
    }

    /**
     * Returns the list of unmatched items increasingly sorted by row rating.
     *
     * @param unmatchedItems - unsorted list of unmatched items
     * @return sorted list of unmatched items
     */
    public static List<Integer> getUnmatchedItemsSortedByRowRating(List<Integer> unmatchedItems, int[][] stackingConstraints) {
        Map<Integer, Integer> unmatchedItemRowRatings = new HashMap<>();
        for (int item : unmatchedItems) {
            int rating = RatingSystem.computeRowRatingForItem(item, stackingConstraints);
            unmatchedItemRowRatings.put(item, rating);
        }
        List<Integer> unmatchedItemsSortedByRowRating = new ArrayList<>();
        Map<Integer, Integer> sortedItemRowRatings = HeuristicUtil.sortMapByValue(unmatchedItemRowRatings);
        unmatchedItemsSortedByRowRating.addAll(sortedItemRowRatings.keySet());
        return unmatchedItemsSortedByRowRating;
    }
}

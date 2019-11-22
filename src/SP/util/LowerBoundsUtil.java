package SP.util;

import SP.post_optimization_methods.neighborhood_operators.Shift;
import SP.representations.BipartiteGraph;
import SP.representations.ItemConflict;
import SP.representations.Solution;
import SP.representations.StackPosition;
import org.jgrapht.Graph;
import org.jgrapht.alg.matching.KuhnMunkresMinimalWeightBipartitePerfectMatching;
import org.jgrapht.graph.DefaultUndirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Provides lower bound computations for partially generated stacking solutions.
 *
 * @author Tim Bohne
 */
public class LowerBoundsUtil {

    /**
     * Computes a lower bound for the solution to be used in the branch and bound procedure.
     * A minimum-weight-perfect-matching gets computed between unassigned items and compatible free stack positions.
     * Then a lower bound gets obtained by adding the matching costs to the total costs of all fixed assignments.
     * It's a LB, because only stacking constraints between unassigned items and already assigned items are considered
     * and not stacking constraints between items that are going to be assigned.
     *
     * @param sol - partial solution to compute a lower bound for
     * @return computed lower bound
     */
    public static Solution computeLowerBound(Solution sol) {

        List<Integer> fixedItems = sol.getAssignedItems();

        BipartiteGraph graph = constructBipartiteGraph(sol);
        KuhnMunkresMinimalWeightBipartitePerfectMatching<String, DefaultWeightedEdge> minCostPerfectMatching =
            new KuhnMunkresMinimalWeightBipartitePerfectMatching<>(graph.getGraph(), graph.getPartitionOne(), graph.getPartitionTwo());

        Solution tmpSol = new Solution(sol);

        for (Object o : minCostPerfectMatching.getMatching()) {
            if (!o.toString().contains("dummy")) {
                int item = Integer.parseInt(o.toString().split("pos")[0].replace(" :", "").replace("(item", "").trim());
                int stack = Integer.parseInt(o.toString().split("pos")[1].split(",")[0].replace("(stack: ", "").trim());
                int level = Integer.parseInt(o.toString().split("pos")[1].split(",")[1].replace("level: ", "").replace("))", "").trim());

                if (tmpSol.getFilledStacks()[stack][level] == -1) {
                    tmpSol.getFilledStacks()[stack][level] = item;
                } else {
                    System.out.println("PROBLEM: position already allocated");
                }
            }
        }
        if (tmpSol.isFeasible()) {
            return tmpSol;
        } else {
            // improve LB
            boolean changes = true;
            // <item, stack>
            List<Shift> itemShifts = new ArrayList<>();
            while (changes) {
                changes = postProcessing(tmpSol, fixedItems, itemShifts);
                tmpSol.lowerItemsThatAreStackedInTheAir();
            }
            return tmpSol;
        }
    }

    /**
     * Post-processing procedure to improve the lower bound.
     * TODO: check and refactor post-processing
     *
     * @param tmpSol     - lower bound solution
     * @param fixedItems - items already assigned to stacks in the partial solution
     * @param itemShifts - to be filled with the performed item shifts
     * @return whether or not the lower bound solution was changed during the post-processing
     */
    private static boolean postProcessing(Solution tmpSol, List<Integer> fixedItems, List<Shift> itemShifts) {

        List<StackPosition> freePositions = HeuristicUtil.retrieveEmptyPositions(tmpSol);
        boolean change = false;
        List<Integer> conflictingItems = new ArrayList<>();

        // iteratively move conflicting items
        for (ItemConflict conflict : tmpSol.getNumberOfConflictsForEachItem()) {
            if (fixedItems.contains(conflict.getItemIdx())) { continue; }
            if (conflictingItems.size() < freePositions.size()) {
                conflictingItems.add(conflict.getItemIdx());
            } else {
                break;
            }
        }
        Graph<String, DefaultWeightedEdge> graph = new DefaultUndirectedWeightedGraph<>(DefaultWeightedEdge.class);
        Set<String> partitionOne = new HashSet<>();
        Set<String> partitionTwo = new HashSet<>();
        GraphUtil.addVerticesForUnmatchedItems(conflictingItems, graph, partitionOne);
        GraphUtil.addVerticesForEmptyPositions(freePositions, graph, partitionTwo);
        List<Integer> dummyItems = GraphUtil.introduceDummyVerticesToBipartiteGraph(graph, partitionOne, partitionTwo);
        GraphUtil.addEdgesBetweenDummyItemsAndStackPositions(graph, dummyItems, freePositions);
        addEdgesBetweenItemsAndStackPositions(tmpSol, graph, conflictingItems, freePositions);
        BipartiteGraph g = new BipartiteGraph(partitionOne, partitionTwo, graph);
        KuhnMunkresMinimalWeightBipartitePerfectMatching<String, DefaultWeightedEdge> minCostPerfectMatching =
                new KuhnMunkresMinimalWeightBipartitePerfectMatching<>(g.getGraph(), g.getPartitionOne(), g.getPartitionTwo());

        for (Object o : minCostPerfectMatching.getMatching()) {
            if (!o.toString().contains("dummy")) {
                int item = Integer.parseInt(o.toString().split("pos")[0].replace(" :", "").replace("(item", "").trim());
                int stack = Integer.parseInt(o.toString().split("pos")[1].split(",")[0].replace("(stack: ", "").trim());
                int level = Integer.parseInt(o.toString().split("pos")[1].split(",")[1].replace("level: ", "").replace("))", "").trim());

                if (tmpSol.getFilledStacks()[stack][level] == -1) {
                    Shift tmpShift = new Shift(item, stack);
                    if (itemShifts.contains(tmpShift)) { continue; }
                    if (!HeuristicUtil.itemCompatibleWithStack(tmpSol.getSolvedInstance().getCosts(), item, stack)) { continue; }
                    for (int[] s : tmpSol.getFilledStacks()) {
                        for (int i = 0; i < s.length; i++) {
                            if (s[i] == item) {
                                s[i] = -1;
                            }
                        }
                    }
                    change = true;
                    itemShifts.add(tmpShift);
                    tmpSol.getFilledStacks()[stack][level] = item;
                } else {
                    System.out.println("PROBLEM: position already allocated");
                }
            }
        }
        return change;
    }

    /**
     * Adds edges to the bipartite graph that connect unassigned items with compatible free stack positions.
     *
     * @param sol            - partial solution to compute a lower bound for
     * @param bipartiteGraph - bipartite graph to add edges to
     * @param items          - items to be assigned to stacks
     * @param positions      - positions in stacks the items could be assigned to
     */
    private static void addEdgesBetweenItemsAndStackPositions(
        Solution sol, Graph<String, DefaultWeightedEdge> bipartiteGraph, List<Integer> items, List<StackPosition> positions
    ) {
        for (int item : items) {
            for (StackPosition pos : positions) {

                DefaultWeightedEdge edge = bipartiteGraph.addEdge("item" + item, "pos" + pos);
                double costs = sol.getSolvedInstance().getCosts()[item][pos.getStackIdx()];

                // Stack incompatibility is realized indirectly via high costs (in costs matrix) to keep the graph
                // complete bipartite. The stacking constraints should also be respected, therefore violated stacking
                // constraints are realized via high costs as well.
                if (!HeuristicUtil.itemCompatibleWithAlreadyAssignedItems(
                        item, sol.getFilledStacks()[pos.getStackIdx()], sol.getSolvedInstance().getItemObjects(), sol.getSolvedInstance().getStackingConstraints())
                        ) {
                    costs = Integer.MAX_VALUE / sol.getSolvedInstance().getItems().length;
                }
                bipartiteGraph.setEdgeWeight(edge, costs);
            }
        }
    }

    /**
     * Constructs the bipartite graph between unassigned items and free stack positions to be
     * used in the lower bound computation.
     *
     * @param sol - partial solution to compute a lower bound for
     * @return bipartite graph between unassigned items and free stack positions
     */
    private static BipartiteGraph constructBipartiteGraph(Solution sol) {
        List<Integer> unassignedItems = sol.getUnassignedItems();
        List<StackPosition> freePositions = HeuristicUtil.retrieveEmptyPositions(sol);

        Graph<String, DefaultWeightedEdge> graph = new DefaultUndirectedWeightedGraph<>(DefaultWeightedEdge.class);
        Set<String> partitionOne = new HashSet<>();
        Set<String> partitionTwo = new HashSet<>();

        GraphUtil.addVerticesForUnmatchedItems(unassignedItems, graph, partitionOne);
        GraphUtil.addVerticesForEmptyPositions(freePositions, graph, partitionTwo);
        List<Integer> dummyItems = GraphUtil.introduceDummyVerticesToBipartiteGraph(graph, partitionOne, partitionTwo);

        GraphUtil.addEdgesBetweenDummyItemsAndStackPositions(graph, dummyItems, freePositions);
        addEdgesBetweenItemsAndStackPositions(sol, graph, unassignedItems, freePositions);

        return new BipartiteGraph(partitionOne, partitionTwo, graph);
    }
}

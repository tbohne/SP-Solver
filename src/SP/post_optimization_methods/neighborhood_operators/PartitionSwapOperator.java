package SP.post_optimization_methods.neighborhood_operators;

import SP.representations.Solution;
import SP.util.HeuristicUtil;

import java.util.List;

// TODO: not yet tested nor completed
public class PartitionSwapOperator {

    public PartitionSwapOperator() {}

    public void performSwaps(
            List<List<Integer>> partitions, int numberOfSwaps, Solution currSol, List<Shift> performedShifts, int unsuccessfulNeighborGenerationAttempts
    ) {

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

            // TODO: add performed shifts
        }
    }

}

package SP.post_optimization_methods.neighborhood_operators;

import SP.representations.Solution;
import SP.util.HeuristicUtil;

import java.util.List;

// TODO: not yet tested nor completed
public class PartitionShiftOperator {

    public PartitionShiftOperator() {}

    private void generateShiftNeighbor(
            List<List<Integer>> partitions, Solution currSol, List<Shift> performedShifts, int unsuccessfulNeighborGenerationAttempts
    ) {

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

        // TODO: add shift to performed shifts
    }

}

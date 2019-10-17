package SP.post_optimization_methods;

import SP.representations.PartitionList;
import SP.representations.Solution;
import SP.util.HeuristicUtil;

import java.util.List;

public class PartitionShiftOperator {

    public PartitionShiftOperator() {}

    private PartitionShift generateShiftNeighbor( Solution currSol, List<Shift> performedShifts, int unsuccessfulNeighborGenerationAttempts) {

        PartitionList partitions = new PartitionList(currSol);

        // compute random item to be shifted
        int partitionIdx = HeuristicUtil.getRandomIntegerInBetween(0, partitions.getPartitions().size() - 1);
        List<Integer> partition = partitions.getPartitions().get(partitionIdx);
        int itemIdx = HeuristicUtil.getRandomIntegerInBetween(0, partition.size() - 1);
        int shiftItem = partition.remove(itemIdx);

        // allow empty partitions - otherwise the indices change --> problem for tabu list
        // if (partition.isEmpty()) { partitions.remove(partition); }

        int shiftTargetPartitionIdx = HeuristicUtil.getRandomIntegerInBetween(0, partitions.getPartitions().size() - 1);

        // create array with containing items of target partition
        int[] itemsOfTargetPartition = new int[partitions.getPartitions().get(shiftTargetPartitionIdx).size()];
        for (int i = 0; i < partitions.getPartitions().get(shiftTargetPartitionIdx).size(); i++) {
            itemsOfTargetPartition[i] = partitions.getPartitions().get(shiftTargetPartitionIdx).get(i);
        }

        // TODO: reasonable here? or just ignore feasibility?
//        // find new shift target until compatible partition is found
        while (partitions.getPartitions().get(shiftTargetPartitionIdx).size() == currSol.getSolvedInstance().getStackCapacity()
                /*|| !HeuristicUtil.itemCompatibleWithAlreadyAssignedItems(
                shiftItem, itemsOfTargetPartition, currSol.getSolvedInstance().getItemObjects(), currSol.getSolvedInstance().getStackingConstraints())*/
                )
        {
            shiftTargetPartitionIdx = HeuristicUtil.getRandomIntegerInBetween(0, partitions.getPartitions().size() - 1);
        }

        partitions.getPartitions().get(shiftTargetPartitionIdx).add(shiftItem);

        // TODO: add shift to performed shifts

        return new PartitionShift(shiftItem, shiftTargetPartitionIdx);
    }

}

package SP.post_optimization_methods;

import SP.representations.Solution;

import java.util.List;
import java.util.Queue;

public interface SwapShiftNeighborhood {

    Solution getNeighborKSwap(int numberOfSwaps, Solution currSol, Queue<Shift> tabuList, Solution bestSol);

    Solution getNeighborShift(Solution currSol, Queue<Shift> tabuList, Solution bestSol);

    void forbidShift(Shift shift, Queue<Shift> tabuList);

    void clearTabuList(Queue<Shift> tabuList);

    int getTabuListClears();

    Solution performSwaps(int numberOfSwaps, List<Swap> swapList, Solution currSol);

    boolean containsTabuSwap(List<Swap> swapList, Queue<Shift> tabuList);
}

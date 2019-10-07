package SP.post_optimization_methods;

import SP.representations.Solution;

public interface SwapShiftNeighborhood {

    Solution getNeighborKSwap(int numberOfSwaps, Solution currSol, Solution bestSol);

    Solution getNeighborShift(Solution currSol, Solution bestSol);

    int getTabuListClears();
}

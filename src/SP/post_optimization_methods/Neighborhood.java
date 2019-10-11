package SP.post_optimization_methods;

import SP.representations.Solution;

public interface Neighborhood {

    Solution getNeighbor(Solution currSol, Solution bestSol);

    int getTabuListClears();
}

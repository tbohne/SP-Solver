package SP.exact_solvers;

import SP.representations.Solution;

public class BranchAndBound {


    private Solution initialSolution;
    private double startTime;

    public BranchAndBound(Solution initialSolution) {
        this.initialSolution = initialSolution;
    }

    public void solve() {

        System.out.println(this.initialSolution);

    }
}

package SP.mip_formulations;

import SP.representations.Instance;
import SP.representations.Solution;
import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearIntExpr;
import ilog.concert.IloLinearNumExpr;
import ilog.cplex.IloCplex;

/**
 * Represents the three-index MIP formulation of stacking problems that gets solved by CPLEX.
 *
 * @author Tim Bohne
 */
public class ThreeIndexFormulation {

    private final Instance instance;
    private final double timeLimit;
    private final boolean hideCPLEXOutput;
    private final int mipEmphasis;
    private final double tolerance;

    /**
     * Constructor
     *
     * @param instance        - instance to be solved
     * @param timeLimit       - time limit for the solving procedure
     * @param hideCPLEXOutput - determines whether the CPLEX output gets hidden
     * @param mipEmphasis     - controls trade-offs between speed, feasibility and optimality
     * @param tolerance       - termination tolerance
     */
    public ThreeIndexFormulation(Instance instance, double timeLimit, boolean hideCPLEXOutput, int mipEmphasis, double tolerance) {
        this.instance = instance;
        this.timeLimit = timeLimit;
        this.hideCPLEXOutput = hideCPLEXOutput;
        this.mipEmphasis = mipEmphasis;
        this.tolerance = tolerance;
    }

    /**
     * Solves the instance of a stacking problem using the three-index formulation.
     *
     * @return generated solution to the stacking problem
     */
    public Solution solve() {

        Solution sol = new Solution();

        try {
            // define new model
           IloCplex cplex = new IloCplex();

            IloIntVar[][][] x = new IloIntVar[this.instance.getItems().length][][];
            this.initVariables(cplex, x);
            IloLinearNumExpr objective = cplex.linearNumExpr();
            this.defineObjective(cplex, objective, x);
            this.addConstraints(cplex, x);
            this.setCPLEXConfig(cplex);
            double startTime = cplex.getCplexTime();

            if (cplex.solve()) {
                double timeToSolve = cplex.getCplexTime() - startTime;
                this.setStacks(cplex, x);
                this.instance.lowerItemsThatAreStackedInTheAir();
                sol = new Solution(timeToSolve, timeLimit, this.instance);
            }
            cplex.end();
        } catch (IloException e) {
            e.printStackTrace();
        }

        return sol;
    }

    /**
     * Defines the objective function which is to minimize the transport costs.
     *
     * @param cplex     - CPLEX model
     * @param objective - objective to be defined
     * @param x         - variables x_iql
     * @throws ilog.concert.IloException for CPLEX errors
     */
    private void defineObjective(IloCplex cplex, IloLinearNumExpr objective, IloIntVar[][][] x) throws ilog.concert.IloException {
        for (int i = 0; i < this.instance.getItems().length; i++) {
            for (int q = 0; q < this.instance.getStacks().length; q++) {
                for (int l = 0; l < this.instance.getStackCapacity(); l++) {
                    objective.addTerm(this.instance.getCosts()[i][q], x[i][q][l]);
                }
            }
        }
        cplex.addMinimize(objective);
    }

    /**
     * Initializes the variables x_iql (x_iql = 1 --> item i gets placed in stack q at level l).
     *
     * @param cplex - CPLEX model
     * @param x     - variables x_iql
     * @throws ilog.concert.IloException for CPLEX errors
     */
    private void initVariables(IloCplex cplex, IloIntVar[][][] x) throws ilog.concert.IloException {

        for (int i = 0; i < this.instance.getItems().length; i++) {
            x[i] = new IloIntVar[this.instance.getStacks().length][];
        }
        for (int i = 0; i < this.instance.getItems().length; i++) {
            for (int j = 0; j < this.instance.getStacks().length; j++) {
                x[i][j] = cplex.intVarArray(this.instance.getStackCapacity(), 0, 1);
            }
        }
    }

    /**
     * Adds the three-index formulation's constraints to the CPLEX model.
     *
     * @param cplex - CPLEX model
     * @param x     - variables x_iql
     * @throws ilog.concert.IloException for CPLEX errors
     */
    private void addConstraints(IloCplex cplex, IloIntVar[][][] x) throws ilog.concert.IloException {

        // --- Constraint (7) ---
        for (int i = 0; i < this.instance.getItems().length; i++) {
            IloLinearIntExpr expr = cplex.linearIntExpr();
            for (int q = 0; q < this.instance.getStacks().length; q++) {
                if (this.instance.getPlacementConstraints()[i][q] == 1) {
                    for (int l = 0; l < this.instance.getStackCapacity(); l++) {
                        expr.addTerm(1, x[i][q][l]);
                    }
                }
            }
            cplex.addEq(expr, 1);
        }

        // --- Constraint (8) ---
        for (int q = 0; q < this.instance.getStacks().length; q++) {
            for (int l = 0; l < this.instance.getStackCapacity(); l++) {
                IloLinearIntExpr expr = cplex.linearIntExpr();
                for (int i = 0; i < this.instance.getItems().length; i++) {
                    expr.addTerm(1, x[i][q][l]);
                }
                cplex.addLe(expr, 1);
            }
        }

        // --- Constraint (9) ---
        for (int i = 0; i < this.instance.getItems().length; i++) {
            for (int q = 0; q < this.instance.getStacks().length; q++) {
                for (int l = 1; l < this.instance.getStackCapacity(); l++) {
                    IloLinearIntExpr expr = cplex.linearIntExpr();
                    for (int j = 0; j < this.instance.getItems().length; j++) {
                        if (i != j && this.instance.getStackingConstraints()[i][j] == 1) {
                            expr.addTerm(1, x[j][q][l - 1]);
                        }
                    }
                    expr.addTerm(-1, x[i][q][l]);
                    cplex.addGe(expr, 0);
                }
            }
        }
    }

    /**
     * Sets the CPLEX configuration.
     *
     * @param cplex - CPLEX model
     * @throws ilog.concert.IloException for CPLEX errors
     */
    @SuppressWarnings("Duplicates")
    private void setCPLEXConfig(IloCplex cplex) throws ilog.concert.IloException {

        if (this.hideCPLEXOutput) {
            cplex.setOut(null);
        }

        // set time limit
        cplex.setParam(IloCplex.Param.TimeLimit, this.timeLimit);

        // deactivate pre solving procedure (takes too long for large instances)
        // time limit is not considered during pre solving
//        cplex.setParam(IloCplex.Param.Preprocessing.Presolve, false);

        // if you don't want to eliminate the presolve totally, you can
        // limit the number of passes the presolving performs
//        cplex.setParam(IloCplex.Param.Preprocessing.NumPass, 100);

        // avoid out-of-memory problems
//        cplex.setParam(IloCplex.DoubleParam.WorkMem, 16384);
//        cplex.setParam(IloCplex.Param.WorkMem, 16384);
//        cplex.setParam(IloCplex.Param.MIP.Limits.TreeMemory, 16384);
//        cplex.setParam(IloCplex.Param.MIP.Strategy.File, 3);
//        cplex.setParam(IloCplex.Param.WorkDir, "/tmp");

        // control trade-offs between speed, feasibility and optimality
        cplex.setParam(IloCplex.IntParam.MIPEmphasis, this.mipEmphasis);

        // set termination tolerance
        cplex.setParam(IloCplex.DoubleParam.EpAGap, this.tolerance);
        cplex.setParam(IloCplex.DoubleParam.EpGap, this.tolerance);
    }

    /**
     * Reverses the item order for each stack to be consistent with the other solvers.
     */
    private void reverseItemOrderForEachStack() {
        for (int i = 0; i < this.instance.getStacks().length; i++) {
            for (int j = 0; j < this.instance.getStacks()[i].length / 2; j++) {
                int tmp = this.instance.getStacks()[i][j];
                this.instance.getStacks()[i][j] = this.instance.getStacks()[i][this.instance.getStacks()[i].length - j - 1];
                this.instance.getStacks()[i][this.instance.getStacks()[i].length - j - 1] = tmp;
            }
        }
    }

    /**
     * Sets the stacks based on the CPLEX variables.
     *
     * @param cplex - the CPLEX model
     * @param x     - the variable x_iq
     */
    private void setStacks(IloCplex cplex, IloIntVar[][][] x) {

        for (int i = 0; i < this.instance.getItems().length; i++) {
            for (int q = 0; q < this.instance.getStacks().length; q++) {
                for (int l = 0; l < this.instance.getStackCapacity(); l++) {
                    try {
                        if (Math.round(cplex.getValue(x[i][q][l])) == 1) {
                            this.instance.getStacks()[q][l] = i;
                        }
                    } catch (IloException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        // used to end up with a top to bottom order
        this.reverseItemOrderForEachStack();
    }
}

package SP.mip_formulations;

import SP.representations.Instance;
import SP.representations.Solution;
import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearIntExpr;
import ilog.concert.IloLinearNumExpr;
import ilog.cplex.IloCplex;

/**
 * Represents the bin-packing MIP formulation of stacking problems that gets solved by CPLEX.
 *
 * @author Tim Bohne
 */
public class BinPackingFormulation {

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
    public BinPackingFormulation(Instance instance, double timeLimit, boolean hideCPLEXOutput, int mipEmphasis, double tolerance) {
        this.instance = instance;
        this.timeLimit = timeLimit;
        this.hideCPLEXOutput = hideCPLEXOutput;
        this.mipEmphasis = mipEmphasis;
        this.tolerance = tolerance;
    }

    /**
     * Solves the instance of a stacking problem using the bin-packing formulation.
     *
     * @return generated solution to the stacking problem
     */
    public Solution solve() {

        Solution sol = new Solution();

        try {
            // define new model
            IloCplex cplex = new IloCplex();

            IloIntVar[][] x = new IloIntVar[this.instance.getItems().length][];
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
                sol.sortItemsInStacksBasedOnTransitiveStackingConstraints();
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
     * @throws ilog.concert.IloException for CPLEX errors
     */
    private void defineObjective(IloCplex cplex, IloLinearNumExpr objective, IloIntVar[][] x) throws ilog.concert.IloException {
        for (int i = 0; i < this.instance.getItems().length; i++) {
            for (int q = 0; q < this.instance.getStacks().length; q++) {
                objective.addTerm(this.instance.getCosts()[i][q], x[i][q]);
            }
        }
        cplex.addMinimize(objective);
    }

    /**
     * Initializes the variables x_iq (x_iq = 1 --> item i gets placed in stack q).
     *
     * @param cplex - CPLEX model
     * @param x     - variables x_iq
     * @throws ilog.concert.IloException for CPLEX errors
     */
    private void initVariables(IloCplex cplex, IloIntVar[][] x) throws ilog.concert.IloException {
        for (int i = 0; i < this.instance.getItems().length; i++) {
            x[i] = cplex.intVarArray(this.instance.getStacks().length, 0, 1);
        }
    }

    /**
     * Adds the bin-packing formulation's constraints to the CPLEX model.
     *
     * @param cplex - CPLEX model
     * @param x     - variables x_iq
     * @throws ilog.concert.IloException for CPLEX errors
     */
    private void addConstraints(IloCplex cplex, IloIntVar[][] x) throws ilog.concert.IloException {

        // --- Constraint (2) ---
        for (int i = 0; i < this.instance.getItems().length; i++) {
            IloLinearIntExpr expr = cplex.linearIntExpr();
            for (int q = 0; q < this.instance.getStacks().length; q++) {
                if (this.instance.getPlacementConstraints()[i][q] == 1) {
                    expr.addTerm(1, x[i][q]);
                }
            }
            cplex.addEq(expr, 1);
        }

        // --- Constraint (3) ---
        for (int q = 0; q < this.instance.getStacks().length; q++) {
            IloLinearIntExpr expr = cplex.linearIntExpr();
            for (int i = 0; i < this.instance.getItems().length; i++) {
                expr.addTerm(1, x[i][q]);
            }
            cplex.addLe(expr, this.instance.getStackCapacity());
        }

        // --- Constraint (4) ---
        for (int i = 0; i < this.instance.getItems().length; i++) {
            for (int j = 0; j < this.instance.getItems().length; j++) {

                if (i != j && this.instance.getStackingConstraints()[i][j] == 0 && this.instance.getStackingConstraints()[j][i] == 0) {
                    for (int q = 0; q < this.instance.getStacks().length; q++) {
                        IloLinearIntExpr expr = cplex.linearIntExpr();
                        expr.addTerm(1, x[i][q]);
                        expr.addTerm(1, x[j][q]);
                        cplex.addLe(expr, 1);
                    }
                }
            }
        }
    }

    /**
     * Sets the stacks based on the CPLEX variables.
     *
     * @param cplex - CPLEX model
     * @param x     - variables x_iq
     * @throws ilog.concert.IloException for CPLEX errors
     */
    private void setStacks(IloCplex cplex, IloIntVar[][] x) throws ilog.concert.IloException {
        for (int i = 0; i < x.length; i++) {
            for (int q = 0; q < x[0].length; q++) {
                if (Math.round(cplex.getValue(x[i][q])) == 1) {
                    int idx = 0;
                    while (this.instance.getStacks()[q][idx] != -1) { idx++; }
                    this.instance.getStacks()[q][idx] = i;
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
    private void setCPLEXConfig(IloCplex cplex) throws ilog.concert.IloException {

        if (this.hideCPLEXOutput) {
            cplex.setOut(null);
        }

        // set time limit
        cplex.setParam(IloCplex.Param.TimeLimit, this.timeLimit);

        // control trade-offs between speed, feasibility and optimality
        cplex.setParam(IloCplex.IntParam.MIPEmphasis, this.mipEmphasis);

        // set termination tolerance
        cplex.setParam(IloCplex.DoubleParam.EpAGap, this.tolerance);
        cplex.setParam(IloCplex.DoubleParam.EpGap, this.tolerance);
    }
}


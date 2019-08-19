package SP.experiments;

import SP.representations.GridPosition;
import SP.representations.Instance;
import SP.io.InstanceWriter;
import SP.representations.Item;
import SP.util.HeuristicUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides functionalities to configure and generate test instances for stacking problems.
 *
 * @author Tim Bohne
 */
public class TestDataGenerator {

    private static final String INSTANCE_PREFIX = "res/instances/";

    /******************************* CONFIGURATION *************************************/
    private static final int NUMBER_OF_INSTANCES = 20;
    private static final int NUMBER_OF_ITEMS = 500;
    private static final int STACK_CAPACITY = 2;

    // The number of stacks m is initially m = n / b,
    // this number specifies the percentage by which the initial m gets increased.
    private static final int ADDITIONAL_STACK_PERCENTAGE = 20;

    private static final float CHANCE_FOR_ONE_IN_STACKING_CONSTRAINTS = 0.0159F;
    private static final float CHANCE_FOR_ONE_IN_PLACEMENT_CONSTRAINTS = 0.7F;

    private static final boolean USING_STACKING_CONSTRAINT_GENERATION_APPROACH_ONE = false;
    private static final boolean TRANSITIVE_STACKING_CONSTRAINTS = true;

    private static final float ITEM_LENGTH_LB = 1.0F;
    private static final float ITEM_LENGTH_UB = 6.0F;
    private static final float ITEM_WIDTH_LB = 1.0F;
    private static final float ITEM_WIDTH_UB = 2.4F;

    private static final float STORAGE_AREA_SLOT_LENGTH = 6.06F;
    private static final float STORAGE_AREA_SLOT_WIDTH = 2.44F;
    private static final float STORAGE_AREA_VEHICLE_DISTANCE_FACTOR = 5.0F;
    private static final int NUMBER_OF_ROWS_IN_STORAGE_AREA = 2;
    /***********************************************************************************/

    public static void main(String[] args) {

        int numOfStacks = computeNumberOfStacks();

        for (int idx = 0; idx < NUMBER_OF_INSTANCES; idx++) {

            List<GridPosition> stackPositions = generateStackPositionsOnSpecifiedNumberOfRows(numOfStacks);
            Item[] items = generateItems(stackPositions);

            int[][] stackingConstraintMatrix;
            if (USING_STACKING_CONSTRAINT_GENERATION_APPROACH_ONE) {
                stackingConstraintMatrix = generateStackingConstraintMatrixApproachOne();
            } else {
                stackingConstraintMatrix = generateStackingConstraintMatrixApproachTwo(items);
            }

            double[][] costs = new double[NUMBER_OF_ITEMS][numOfStacks];
            generateCosts(costs, numOfStacks, items, stackPositions);
            generateInstance(idx, numOfStacks, items, stackPositions, stackingConstraintMatrix, costs);
        }
    }

    /**
     * Generates the positions of the fixed stacks in the storage area in a square.
     *
     * @param numOfStacks - number of stacks available in the instances
     * @return list of generated stack positions
     */
    private static List<GridPosition> generateStackPositionsSquare(int numOfStacks) {
        List<GridPosition> stackPositions = new ArrayList<>();
        int stacksPerRow = (int)Math.ceil(Math.sqrt(numOfStacks));
        float xCoord = 0;
        float yCoord = 0;

        for (int i = 0; i < numOfStacks; i++) {
            if (i != 0 && i % stacksPerRow == 0) {
                yCoord += STORAGE_AREA_SLOT_WIDTH;
                xCoord = 0;
            }
            stackPositions.add(new GridPosition(xCoord, yCoord));
            xCoord += STORAGE_AREA_SLOT_LENGTH;
        }
        return stackPositions;
    }

    /**
     * Generates the positions of the fixed stacks in the storage area
     * on the specified number of rows.
     *
     * @param numOfStacks - number of stacks available in the instances
     * @return list of generated stack positions
     */
    private static List<GridPosition> generateStackPositionsOnSpecifiedNumberOfRows(int numOfStacks) {

        List<GridPosition> stackPositions = new ArrayList<>();
        float xCoord = 0;
        float yCoord = 0;
        int perRow = numOfStacks / NUMBER_OF_ROWS_IN_STORAGE_AREA;
        int remainders = numOfStacks % NUMBER_OF_ROWS_IN_STORAGE_AREA;
        boolean remainderAttached = false;
        int rowCnt = 0;

        for (int i = 0; i < numOfStacks; i++) {
            if (rowCnt == perRow || remainderAttached) {
                if (remainders > 0 && !remainderAttached) {
                    remainderAttached = true;
                    remainders--;
                } else {
                    xCoord = 0;
                    yCoord += STORAGE_AREA_SLOT_WIDTH;
                    remainderAttached = false;
                    rowCnt = 0;
                }
            }
            stackPositions.add(new GridPosition(xCoord, yCoord));
            xCoord += STORAGE_AREA_SLOT_LENGTH;
            rowCnt++;
        }
        return stackPositions;
    }

    /**
     * Generates the original positions of the items on the vehicles they are arriving on.
     * In practical settings, there are often two tracks for arriving vehicles.
     * Therefore the items are positioned on two rows.
     *
     * @return list of generated item positions
     */
    private static List<GridPosition> generateItemPositions(List<GridPosition> stackPositions) {

        List<GridPosition> itemPositions = new ArrayList<>();

        float xCoord = 0;
        float yCoord = (float)stackPositions.get(stackPositions.size() - 1).getYCoord()
            + STORAGE_AREA_SLOT_WIDTH + STORAGE_AREA_VEHICLE_DISTANCE_FACTOR * STORAGE_AREA_SLOT_WIDTH;

        for (int i = 0; i < NUMBER_OF_ITEMS; i++) {
            if (i == NUMBER_OF_ITEMS / 2) {
                xCoord = 0;
                yCoord += STORAGE_AREA_SLOT_WIDTH;
            }
            itemPositions.add(new GridPosition(xCoord, yCoord));
            xCoord += STORAGE_AREA_SLOT_LENGTH;
        }
        return itemPositions;
    }

    /**
     * Generates n items with a random length and width from a given range.
     *
     * @return generated items
     */
    private static Item[] generateItems(List<GridPosition> stackPositions) {
        List<GridPosition> itemPositions = generateItemPositions(stackPositions);
        Item[] items = new Item[NUMBER_OF_ITEMS];
        for (int i = 0; i < NUMBER_OF_ITEMS; i++) {
            float length = HeuristicUtil.getRandomValueInBetweenHalfSteps(ITEM_LENGTH_LB, ITEM_LENGTH_UB);
            float width = HeuristicUtil.getRandomValueInBetweenHalfSteps(ITEM_WIDTH_LB, ITEM_WIDTH_UB);
            items[i] = new Item(i, length, width, itemPositions.get(i));
        }
        return items;
    }

    /**
     * Generates the stacking constraint matrix. In this approach, an item is stackable on top of another item
     * if its length and width values are less than or equal to the length and width of the other item.
     * The length and width per item is a random value from a given range. Two items are stackable in both
     * directions if they have the same width and length. The transitivity is already given in this approach.
     *
     * @return generated stacking constraint matrix
     */
    private static int[][] generateStackingConstraintMatrixApproachTwo(Item[] items) {
        int[][] stackingConstraintMatrix = new int[NUMBER_OF_ITEMS][NUMBER_OF_ITEMS];

        for (int i = 0; i < NUMBER_OF_ITEMS; i++) {
            for (int j = 0; j < NUMBER_OF_ITEMS; j++) {

                if (i == j) { stackingConstraintMatrix[i][j] = 1; }

                // If the items have the same length and width, they're stackable in both directions.
                if (items[i].getLength() == items[j].getLength()
                    && items[i].getWidth() == items[j].getWidth()) {
                        stackingConstraintMatrix[i][j] = 1;
                        stackingConstraintMatrix[j][i] = 1;
                } else if (items[i].getLength() <= items[j].getLength()
                    && items[i].getWidth() <= items[j].getWidth()) {
                        stackingConstraintMatrix[i][j] = 1;
                } else {
                    stackingConstraintMatrix[i][j] = 0;
                }
            }
        }
        return stackingConstraintMatrix;
    }

    /**
     * Generates the stacking constraint matrix with "random" 0/1 entries based on a given chance.
     *
     * @return generated stacking constraint matrix
     */
    private static int[][] generateStackingConstraintMatrixApproachOne() {
        int[][] stackingConstraintMatrix = new int[NUMBER_OF_ITEMS][NUMBER_OF_ITEMS];
        for (int i = 0; i < NUMBER_OF_ITEMS; i++) {
            for (int j = 0; j < NUMBER_OF_ITEMS; j++) {
                if (i == j) {
                    stackingConstraintMatrix[i][j] = 1;
                } else {
                    if (Math.random() >= 1.0 - CHANCE_FOR_ONE_IN_STACKING_CONSTRAINTS) {
                        stackingConstraintMatrix[i][j] = 1;
                    } else {
                        stackingConstraintMatrix[i][j] = 0;
                    }
                }
            }
        }
        if (TRANSITIVE_STACKING_CONSTRAINTS) {
            stackingConstraintMatrix = makeMatrixTransitive(stackingConstraintMatrix);
        }
        return stackingConstraintMatrix;
    }

    /**
     * Adds the one-entries in the stacking constraint matrix that follow from transitivity.
     *
     * @param matrix - matrix to be extended
     * @return extended matrix
     */
    private static int[][] makeMatrixTransitive(int[][] matrix) {
        // The ones that are added can then induce new ones in other rows,
        // therefore the process has to be repeated as long as there are changes.
        boolean oneEntryAdded = true;
        while (oneEntryAdded) {
            oneEntryAdded = false;
            for (int i = 0; i < matrix.length; i++) {
                for (int j = 0; j < matrix[0].length; j++) {
                    if (i != j && matrix[i][j] == 0) {
                        for (int k = 0; k < matrix[0].length; k++) {
                            if (matrix[i][k] == 1 && i != k && j != k) {
                                if (matrix[k][j] == 1) {
                                    matrix[i][j] = 1;
                                    oneEntryAdded = true;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        return matrix;
    }

    /**
     * Computes the number of stacks available in the instances.
     *
     * @return number of available stacks
     */
    private static int computeNumberOfStacks() {
        int numOfStacks = (int)Math.ceil((float)NUMBER_OF_ITEMS / (float)STACK_CAPACITY);
        if (ADDITIONAL_STACK_PERCENTAGE > 0) {
            numOfStacks = (int)(Math.ceil(numOfStacks + numOfStacks * ((float)ADDITIONAL_STACK_PERCENTAGE / 100.0)));
        }
        return numOfStacks;
    }

    /**
     * Generates the matrix containing the transport costs for item-stack-assignments.
     * The placement constraint that forbid certain item-stack-assignments are indirectly implemented
     * via high cost entries.
     *
     * @param costs          - matrix of costs to be filled
     * @param numOfStacks    - number of available stacks
     * @param stackPositions - positions of the stacks in the storage area
     */
    private static void generateCosts(double[][] costs, int numOfStacks, Item[] items, List<GridPosition> stackPositions) {
        for (int i = 0; i < NUMBER_OF_ITEMS; i++) {
            for (int j = 0; j < numOfStacks; j++) {
                costs[i][j] = HeuristicUtil.computeManhattanDist(i, j, items, stackPositions);
            }
        }
        // Implements the placement constraints via high cost entries.
        for (int i = 0; i < NUMBER_OF_ITEMS; i++) {
            for (int j = 0; j < numOfStacks; j++) {
                if (Math.random() >= CHANCE_FOR_ONE_IN_PLACEMENT_CONSTRAINTS) {
                    costs[i][j] = Integer.MAX_VALUE / NUMBER_OF_ITEMS;
                }
            }
        }
    }

    /**
     * Writes the config of the generated instance set to the file system.
     */
    private static void writeConfig() {
        InstanceWriter.writeConfig(
            INSTANCE_PREFIX + "instance_set_config.csv", NUMBER_OF_INSTANCES, NUMBER_OF_ITEMS,
            STACK_CAPACITY, ADDITIONAL_STACK_PERCENTAGE, CHANCE_FOR_ONE_IN_STACKING_CONSTRAINTS,
            CHANCE_FOR_ONE_IN_PLACEMENT_CONSTRAINTS, USING_STACKING_CONSTRAINT_GENERATION_APPROACH_ONE,
            TRANSITIVE_STACKING_CONSTRAINTS, ITEM_LENGTH_LB, ITEM_LENGTH_UB, ITEM_WIDTH_LB, ITEM_WIDTH_UB,
            STORAGE_AREA_SLOT_LENGTH, STORAGE_AREA_SLOT_WIDTH, STORAGE_AREA_VEHICLE_DISTANCE_FACTOR,
            NUMBER_OF_ROWS_IN_STORAGE_AREA
        );
    }

    /**
     * Generates an instance of a stacking problem and writes it to the file system.
     *
     * @param idx                      - index of the instance to be generated
     * @param numOfStacks              - number of available stacks
     * @param items                    - items that are part of the instance
     * @param stackPositions           - stack positions in the storage area
     * @param stackingConstraintMatrix - matrix containing the stacking constraints
     * @param costs                    - matrix containing the transport costs for items
     */
    private static void generateInstance(
        int idx, int numOfStacks, Item[] items, List<GridPosition> stackPositions,
        int[][] stackingConstraintMatrix, double[][] costs
    ) {
        String idxString = idx < 10 ? "0" + idx : String.valueOf(idx);
        String instanceName = "slp_instance_" + NUMBER_OF_ITEMS + "_" + numOfStacks + "_" + STACK_CAPACITY + "_" + idxString;

        Instance instance = new Instance(
            items, numOfStacks, stackPositions, STACK_CAPACITY, stackingConstraintMatrix, costs, instanceName
        );
        InstanceWriter.writeInstance(INSTANCE_PREFIX + instanceName + ".txt", instance);
        writeConfig();
    }
}

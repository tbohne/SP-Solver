package SP.io;

import SP.experiments.TestDataGenerator;
import SP.representations.Instance;
import SP.representations.Item;
import SP.representations.GridPosition;

import java.io.*;

/**
 * Provides functionalities to write instances of stacking problems to the file system.
 *
 * @author Tim Bohne
 */
public class InstanceWriter {

    /**
     * Writes the specified instance to the file with the specified name.
     *
     * @param filename - file the instance gets written to
     * @param instance - instance to be written to the file
     */
    public static void writeInstance(String filename, Instance instance) {

        File file = new File(filename);

        try {
            FileOutputStream fos = new FileOutputStream(file);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));

            bw.write(instance.getItems().length + "\n");
            bw.write(instance.getStacks().length + "\n");
            bw.write(instance.getStackCapacity() + "\n");
            bw.newLine();

            writeStackingConstraints(instance, bw);
            writeCosts(instance, bw);
            writeItemPositions(instance, bw);
            writeStackPositions(instance, bw);
            writeItemDimensions(instance, bw);

            bw.close();
            fos.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Writes the configuration of an instance set to the specified file.
     *
     * @param filename                                     - name of the file to be written to
     * @param numOfInstances                               - number of instances of the set
     * @param numOfItems                                   - number of items that are part of the instances
     * @param stackCapacity                                - capacity of the instance's stacks
     * @param additionalStackPercentage                    - percentage to be added upon the minimum number of stacks (n/b)
     * @param chanceForOneInStackingConstraints            - chance for a 1-entry in the stacking constraint matrix
     * @param chanceForOneInPlacementConstraints           - chance for a 1-entry in the placement constraint matrix
     * @param usingStackingConstraintGenerationApproachOne - determines whether the 1st approach of stacking constraint
     *                                                       generation is used (2nd approach otherwise)
     * @param itemLengthLB                                 - lower bound of the item lengths
     * @param itemLengthUB                                 - upper bound of the item lengths
     * @param itemWidthLB                                  - lower bound of the item widths
     * @param itemWidthUB                                  - upper bound of the item widths
     */
    public static void writeConfig(
        String filename, int numOfInstances, int numOfItems, int stackCapacity, int additionalStackPercentage,
        float chanceForOneInStackingConstraints, float chanceForOneInPlacementConstraints,
        boolean usingStackingConstraintGenerationApproachOne, boolean transitiveStackingConstraints,
        TestDataGenerator.costGenerationApproaches costGenerationApproach, float minCost, float maxCost,
        float itemLengthLB, float itemLengthUB, float itemWidthLB, float itemWidthUB, float storageAreaSlotLength,
        float storageAreaSlotWidth, float storageAreaVehicleDistanceFactor, int numberOfRowsInStorageArea
    ) {

        File file = new File(filename);

        try {
            FileOutputStream fos = new FileOutputStream(file);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));

            bw.write(
                "numOfInstances,numOfItems,stackCapacity,additionalStackPercentage,"
                + "chanceForOneInStackingConstraints,chanceForOneInPlacementConstraints,"
                + "usingStackingConstraintGenerationApproachOne,transitiveStackingConstraints,"
                + "costGenerationApproach,minCost, maxCost,itemLengthLB,itemLengthUB,itemWidthLB,itemWidthUB,"
                + "storageAreaSlotLength,storageAreaSlotWidth,storageAreaVehicleDistanceFactor,numberOfRowsInStorageArea\n"
            );
            bw.write(
                numOfInstances + "," + numOfItems + "," + stackCapacity + "," + additionalStackPercentage + ","
                + chanceForOneInStackingConstraints + "," + chanceForOneInPlacementConstraints + ","
                + usingStackingConstraintGenerationApproachOne + "," + transitiveStackingConstraints + ","
                + costGenerationApproach + "," + minCost + "," + maxCost + "," + itemLengthLB + "," + itemLengthUB + ","
                + itemWidthLB + "," + itemWidthUB + "," + storageAreaSlotLength + "," + storageAreaSlotWidth + ","
                + storageAreaVehicleDistanceFactor + "," + numberOfRowsInStorageArea
            );

            bw.close();
            fos.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Writes the stacking constraints to the file using the specified buffered writer.
     *
     * @param instance - instance that is written to the file
     * @param bw       - buffered writer pointing to the part of the file to write to
     * @throws IOException for errors during writing procedure
     */
    private static void writeStackingConstraints(Instance instance, BufferedWriter bw) throws IOException {
        for (int i = 0; i < instance.getStackingConstraints().length; i++) {
            for (int j = 0; j < instance.getStackingConstraints()[0].length; j++) {
                bw.write(instance.getStackingConstraints()[i][j] + " ");
            }
            bw.newLine();
        }
        bw.newLine();
    }

    /**
     * Writes the costs to the file using the specified buffered writer.
     *
     * @param instance - instance that is written to the file
     * @param bw       - buffered writer pointing to the part of the file to write to
     * @throws IOException for errors during writing procedure
     */
    private static void writeCosts(Instance instance, BufferedWriter bw) throws IOException {
        for (int i = 0; i < instance.getCosts().length; i++) {
            for (int j = 0; j < instance.getCosts()[0].length; j++) {
                bw.write(String.format("%.2f", instance.getCosts()[i][j]) + " ");
            }
            bw.newLine();
        }
        bw.newLine();
    }

    /**
     * Writes the item positions to the file using the specified buffered writer.
     *
     * @param instance - instance that is written to the file
     * @param bw       - buffered writer pointing to the part of the file to write to
     * @throws IOException for errors during writing procedure
     */
    private static void writeItemPositions(Instance instance, BufferedWriter bw) throws IOException {
        for (Item item : instance.getItemObjects()) {
            bw.write(item.getPosition() + " ");
        }
        bw.newLine();
    }

    /**
     * Writes the stack positions to the file using the specified buffered writer.
     *
     * @param instance - instance that is written to the file
     * @param bw       - buffered writer pointing to the part of the file to write to
     * @throws IOException for errors during writing procedure
     */
    private static void writeStackPositions(Instance instance, BufferedWriter bw) throws IOException {
        for (GridPosition stackPos : instance.getStackPositions()) {
            bw.write(stackPos + " ");
        }
        bw.newLine();
    }

    /**
     * Writes the item dimensions to the file using the specified buffered writer.
     *
     * @param instance - instance that is written to the file
     * @param bw       - buffered writer pointing to the part of the file to write to
     * @throws IOException for errors during writing procedure
     */
    private static void writeItemDimensions(Instance instance, BufferedWriter bw) throws IOException {
        for (Item item : instance.getItemObjects()) {
            bw.write("(" + item.getLength() + "," + item.getWidth() + ")" + " ");
        }
        bw.newLine();
    }
}

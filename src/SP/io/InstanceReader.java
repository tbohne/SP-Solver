package SP.io;

import SP.representations.Instance;
import SP.representations.Item;
import SP.representations.GridPosition;
import SP.util.RepresentationUtil;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides functionalities to read instances of stacking problems from the file system.
 *
 * @author Tim Bohne
 */
public class InstanceReader {

    /**
     * Reads an instance of a stacking problem from the specified file.
     *
     * @param filename - name of the file to read from
     * @return read instance
     */
    public static Instance readInstance(String filename) {

        int numberOfItems = 0;
        int numberOfStacks = 0;
        int stackCapacity = 0;
        List<GridPosition> itemPositions = new ArrayList<>();
        List<GridPosition> stackPositions = new ArrayList<>();
        List<List<Float>> itemDimensions = new ArrayList<>();
        int[][] stackingConstraints = new int[numberOfItems][];
        double[][] costs = new double[numberOfItems][];

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            numberOfItems = Integer.parseInt(reader.readLine().trim());
            numberOfStacks = Integer.parseInt(reader.readLine().trim());
            stackCapacity = Integer.parseInt(reader.readLine().trim());

            if (reader.readLine().trim().equals("")) {
                stackingConstraints = RepresentationUtil.castFloatingPointMatrix(readMatrix(reader, numberOfItems));
            }
            costs = readMatrix(reader, numberOfItems);
            readPositions(itemPositions, reader);
            readPositions(stackPositions, reader);

            String line = reader.readLine();
            if (line != null) {
                readItemDimensions(itemDimensions, line.trim());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Item[] items = new Item[numberOfItems];
        createItems(numberOfItems, itemDimensions, items, itemPositions);

        String instanceName = filename.replace("res/", "").replace(".txt", "");
        return new Instance(items, numberOfStacks, stackPositions, stackCapacity, stackingConstraints, costs, instanceName);
    }

    /**
     * Reads matrices (stacking constraints, costs, ...) from an instance file using the specified reader.
     *
     * @param reader        - buffered reader pointing to the part of the file containing the matrix to be read
     * @param numberOfItems - number of items that are part of the instance
     * @return read matrix
     */
    private static double[][] readMatrix(BufferedReader reader, int numberOfItems) {

        double[][] matrix = new double[numberOfItems][];

        try {
            String line = reader.readLine().trim();
            int row = 0;
            // Reading as long as there are lines belonging to the matrix.
            while (!line.equals("")) {
                String[] stringOfIntegers = line.split(" ");
                matrix[row] = new double[stringOfIntegers.length];
                for (int col = 0; col < stringOfIntegers.length; col++) {
                    matrix[row][col] = Double.parseDouble(stringOfIntegers[col]);
                }
                line = reader.readLine();
                line = line == null ? "" : line.trim();
                row++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return matrix;
    }

    /**
     * Reads positions (item pos / stack pos) from an instance file using the specified reader.
     *
     * @param positions - list of positions to be filled
     * @param reader    - reader pointing to the part of the file containing the positions
     */
    private static void readPositions(List<GridPosition> positions, BufferedReader reader) {
        try {
            String line = reader.readLine().trim();
            String[] stringOfPositions = line.split(" ");
            for (String position : stringOfPositions) {
                double xCoord = Double.parseDouble(position.split(",")[0].replace("(", "").trim());
                double yCoord = Double.parseDouble(position.split(",")[1].replace(")", "").trim());
                positions.add(new GridPosition(xCoord, yCoord));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Reads item dimensions (length / width) from an instance file using the specified reader.
     *
     * @param listOfDimensions - list of dimensions to be filled
     * @param line             - line of the file containing the dimensions
     */
    private static void readItemDimensions(List<List<Float>> listOfDimensions, String line) {
        String[] stringOfDimensions = line.split(" ");
        for (String dim : stringOfDimensions) {
            float length = Float.parseFloat(dim.split(",")[0].replace("(", "").trim());
            float width = Float.parseFloat(dim.split(",")[1].replace(")", "").trim());
            List<Float> dimensions = new ArrayList<>();
            dimensions.add(length);
            dimensions.add(width);
            listOfDimensions.add(dimensions);
        }
    }

    /**
     * Creates the items based on the read data.
     *
     * @param numberOfItems  - number of items to be generated
     * @param itemDimensions - list of the item's dimensions
     * @param items          - array of items to be filled
     * @param itemPositions  - positions of the items on the grid
     */
    private static void createItems(
        int numberOfItems, List<List<Float>> itemDimensions, Item[] items, List<GridPosition> itemPositions
    ) {
        for (int i = 0; i < numberOfItems; i++) {
            if (itemDimensions.size() > 0) {
                items[i] = new Item(i, itemDimensions.get(i).get(0), itemDimensions.get(i).get(1), itemPositions.get(i));
            } else {
                // no dimensions given (used to handle deprecated instances)
                items[i] = new Item(i, 0, 0, itemPositions.get(i));
            }
        }
    }
}

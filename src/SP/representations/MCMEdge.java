package SP.representations;

/**
 * Represents an edge in a maximum cardinality matching.
 *
 * @author Tim Bohne
 */
public class MCMEdge implements Comparable<MCMEdge> {

    private int vertexOne;
    private int vertexTwo;
    private int rating;

    /**
     * Constructor
     *
     * @param vertexOne - first vertex of the edge
     * @param vertexTwo - second vertex of the edge
     * @param rating    - rating of the edge (used in rating system of heuristic)
     */
    public MCMEdge(int vertexOne, int vertexTwo, int rating) {
        this.vertexOne = vertexOne;
        this.vertexTwo = vertexTwo;
        this.rating = rating;
    }

    /**
     * Copy-Constructor
     *
     * @param edge - edge to be copied
     */
    public MCMEdge(MCMEdge edge) {
        this.vertexOne = edge.getVertexOne();
        this.vertexTwo = edge.getVertexTwo();
        this.rating = edge.getRating();
    }

    /**
     * Returns the index of the edge's first vertex.
     *
     * @return first vertex of the edge
     */
    public int getVertexOne() {
        return vertexOne;
    }

    /**
     * Returns the index of the edge's second vertex.
     *
     * @return second vertex of the edge
     */
    public int getVertexTwo() {
        return vertexTwo;
    }

    /**
     * Returns the edge's rating which is used and determined in the heuristics.
     *
     * @return rating of the edge
     */
    public int getRating() {
        return rating;
    }

    /**
     * Sets the edge's rating (determined in the heuristics).
     *
     * @param rating - rating to be set
     */
    public void setRating(int rating) {
        this.rating = rating;
    }

    /**
     * Flips the edge's vertices.
     */
    public void flipVertices() {
        int tmp = this.vertexOne;
        this.vertexOne = this.vertexTwo;
        this.vertexTwo = tmp;
    }

    /**
     * Compares two edges in terms of their ratings.
     *
     * @param edge - edge to be compared to
     * @return whether this edge's rating is less than, equal to, or greater than the other edge's rating
     */
    @Override
    public int compareTo(MCMEdge edge) {
        return this.rating - edge.rating;
    }

    /**
     * Returns a string visualizing the edge.
     *
     * @return string visualizing the edge
     */
    @Override
    public String toString() {
        return "(" + this.vertexOne + ", " + this.vertexTwo + ")";
    }
}

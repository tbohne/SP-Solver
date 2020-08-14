package SP.representations;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of the GraphPath interface to be used in the ejection chain operator.
 *
 * @author Tim Bohne
 */
public class EjectionChainPath implements GraphPath {

    private List<DefaultWeightedEdge> edgeList;
    private DefaultDirectedWeightedGraph<String, DefaultWeightedEdge> graph;

    /**
     * Constructor
     *
     * @param graph - graph containing the path
     */
    public EjectionChainPath(DefaultDirectedWeightedGraph<String, DefaultWeightedEdge> graph) {
        this.edgeList = new ArrayList<>();
        this.graph = graph;
    }

    /**
     * Sets the path based on the specified string of edges.
     *
     * @param path  - string of edges to be set as path
     * @param graph - graph containing the path
     */
    public void setPath(List<String> path, DefaultDirectedWeightedGraph<String, DefaultWeightedEdge> graph) {
        for (int i = 0; i < path.size() - 1; i++) {
            this.edgeList.add(graph.getEdge(path.get(i), path.get(i + 1)));
        }
    }

    /**
     * Returns the edges contained in the path.
     *
     * @return edge list
     */
    @Override
    public List<DefaultWeightedEdge> getEdgeList() {
        return this.edgeList;
    }

    /**
     * Returns the vertices contained in the path.
     *
     * @return vertex list
     */
    @Override
    public List<String> getVertexList() {
        // TODO
        return new ArrayList<>();
    }

    /**
     * Returns the graph that contains the path.
     *
     * @return graph containing the path
     */
    @Override
    public Graph getGraph() {
        return this.graph;
    }

    /**
     * Returns the path's starting vertex.
     *
     * @return start vertex
     */
    @Override
    public Object getStartVertex() {
        return this.edgeList.get(0);
    }

    /**
     * Returns the path's final vertex.
     *
     * @return end vertex
     */
    @Override
    public Object getEndVertex() {
        return this.edgeList.get(this.edgeList.size() - 1);
    }

    /**
     * Returns the weight of the path (sum of edge costs).
     *
     * @return path weight
     */
    @Override
    public double getWeight() {
        double weight = 0.0;
        for (DefaultWeightedEdge edge : this.edgeList) {
            weight += graph.getEdgeWeight(edge);
        }
        return weight;
    }
}

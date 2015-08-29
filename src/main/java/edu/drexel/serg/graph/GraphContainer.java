package edu.drexel.serg.graph;

/**
 * Created by bsmitc on 8/29/15.
 */

import edu.uci.ics.jung.graph.impl.*;
import edu.uci.ics.jung.graph.decorators.*;
import edu.uci.ics.jung.graph.*;
import edu.uci.ics.jung.algorithms.importance.*;

/**
 *
 * @author Brian Mitchell
 */
public class GraphContainer {

    private Graph graph;
    private EdgeWeightLabeller edgeLabeller;
    private StringLabeller     verteLabeller;
    private int                simplifyPct = 100;

    /** Creates a new instance of GraphContainer */
    public GraphContainer() {


    }

    public Graph getGraph() {
        return graph;
    }

    public void setGraph(Graph graph) {
        this.graph = graph;
    }

    public EdgeWeightLabeller getEdgeLabeller() {
        return edgeLabeller;
    }

    public void setEdgeLabeller(EdgeWeightLabeller edgeLabeller) {
        this.edgeLabeller = edgeLabeller;
    }

    public StringLabeller getVerteLabeller() {
        return verteLabeller;
    }

    public void setVerteLabeller(StringLabeller verteLabeller) {
        this.verteLabeller = verteLabeller;
    }

    public int getSimplifyPct() {
        return simplifyPct;
    }

    public void setSimplifyPct(int simplifyPct) {
        this.simplifyPct = simplifyPct;
    }

}


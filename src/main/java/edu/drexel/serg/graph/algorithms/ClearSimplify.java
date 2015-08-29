/*
 * ClearSimplify.java
 *
 * Created on November 24, 2006, 10:19 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package edu.drexel.serg.graph.algorithms;

import edu.drexel.serg.graph.*;
import java.util.*;
import edu.uci.ics.jung.graph.impl.*;
import edu.uci.ics.jung.graph.decorators.*;
import edu.uci.ics.jung.graph.*; 
import edu.uci.ics.jung.algorithms.importance.*;
import edu.uci.ics.jung.algorithms.importance.PageRank;
import edu.uci.ics.jung.utils.UserData;

/**
 *
 * @author Brian Mitchell
 */
public class ClearSimplify implements ISimplifyAlgorithm{
    
    /** Creates a new instance of ClearSimplify */
    public ClearSimplify() {
    }
    
    public static ISimplifyAlgorithm CreateInstance()
    {
        return new ClearSimplify();
    }
    
    public GraphContainer RunSimplify(GraphContainer gc)
    {
        Graph g = gc.getGraph();
        
        Iterator i = g.getVertices().iterator();
        
        while(i.hasNext())
        {
            Vertex vtx = (Vertex)i.next();
            
            if(vtx.getUserDatum(KeyEnum.INCLUDE_KEY.toString())!=null)
            {
                vtx.removeUserDatum(KeyEnum.INCLUDE_KEY.toString());
            }
        }
        
        return gc;
    }
    
}

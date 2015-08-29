/*
 * BetweennessSimplify.java
 *
 * Created on November 28, 2006, 8:19 AM
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
public class BetweennessSimplify implements ISimplifyAlgorithm {
    
    /** Creates a new instance of BetweennessSimplify */
    public BetweennessSimplify() {
    }
    
    public static ISimplifyAlgorithm CreateInstance()
    {
        return new BetweennessSimplify();
    }
    
    public GraphContainer RunSimplify(GraphContainer gc)
    {
        Graph g = gc.getGraph();
        
         BetweennessCentrality bcranker = new BetweennessCentrality(g,true,false);
         bcranker.evaluate();
        
        //now handle rankings
        List<NodeRanking> rlist = (List<NodeRanking>)bcranker.getRankings();
        float simplifyPct = gc.getSimplifyPct() / 100f;
        
        int keep = Math.round(rlist.size() * simplifyPct);
        
        for(int i = 0; i < rlist.size(); i++)
        {
            NodeRanking nr = rlist.get(i);
            
            nr.vertex.setUserDatum(KeyEnum.IMPORTANCE_RANK.toString(),new Integer(i),
                    UserData.SHARED);
            nr.vertex.setUserDatum(KeyEnum.IMPORTANCE_VALUE.toString(), new Double(nr.rankScore),
                    UserData.SHARED);

            
            if(i <= keep)
            {
                int relRank = (int)((1.0f - ((float)i/(float)(keep)))*100f);
                
                nr.vertex.setUserDatum(KeyEnum.INCLUDE_KEY.toString(), new Boolean(true),
                    UserData.SHARED);
                nr.vertex.setUserDatum(KeyEnum.IMPORTANCE_REL_RANK.toString(),new Integer(relRank),
                    UserData.SHARED);
                nr.vertex.setUserDatum(KeyEnum.VERTEX_TYPE.toString(), VertexStateEnum.ROOT_VERTEX,
                    UserData.SHARED);
            }
            else
            {
                nr.vertex.setUserDatum(KeyEnum.INCLUDE_KEY.toString(), new Boolean(false),
                    UserData.SHARED);
                nr.vertex.setUserDatum(KeyEnum.VERTEX_TYPE.toString(), VertexStateEnum.NO_ASSOCIATION,
                    UserData.SHARED);
                nr.vertex.setUserDatum(KeyEnum.IMPORTANCE_REL_RANK.toString(),new Integer(0),
                        UserData.SHARED);
            }
        }
        //pr.printRankings(true,true);
        return gc;
    }
    
}

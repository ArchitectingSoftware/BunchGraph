/*
 * HITSSimplify.java
 *
 * Created on November 28, 2006, 8:12 AM
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
public class HITSSimplify implements ISimplifyAlgorithm, ISimplifyWithRoots {
    
    /** Creates a new instance of HITSSimplify */
    public HITSSimplify() {
    }
    
    public static ISimplifyAlgorithm CreateInstance()
    {
        return new HITSSimplify();
    }
    
    public GraphContainer RunSimplify(GraphContainer gc)
    {
        Graph g = gc.getGraph();
        
        HITS hr = new HITS(g);
        hr.evaluate();
        
        //now handle rankings
        List<NodeRanking> rlist = (List<NodeRanking>)hr.getRankings();
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
    
    public GraphContainer RunSimplifyWithRoots(GraphContainer gc, Set rootSet, int neighborhood)
    {
        Graph g = gc.getGraph();
        
        //create and populate a hast table for fast lookup
        HashMap<String,Vertex> hm = new HashMap<String,Vertex>();
        Iterator itr = rootSet.iterator();
        while(itr.hasNext())
        {
            Vertex vtx = (Vertex)itr.next();
            vtx.setUserDatum(KeyEnum.INCLUDE_KEY.toString(), new Boolean(true),
                    UserData.SHARED);
            vtx.setUserDatum(KeyEnum.VERTEX_TYPE.toString(), VertexStateEnum.ROOT_VERTEX,
                    UserData.SHARED);
            vtx.setUserDatum(KeyEnum.IMPORTANCE_REL_RANK.toString(),new Integer(1),
                    UserData.SHARED);
            String vtxName = (String)vtx.getUserDatum(KeyEnum.VERTEX_NAME.toString());
            hm.put(vtxName,vtx);
        }
        
        HITSWithPriors pr = new HITSWithPriors((DirectedGraph)g, 0.15,rootSet);
        pr.evaluate();
        
        //now handle rankings
        List<NodeRanking> rlist = (List<NodeRanking>)pr.getRankings();
        
        int keep = 0;
        
        for(int i = 0; i < rlist.size(); i++)
        {
            NodeRanking nr = rlist.get(i);
            
            nr.vertex.setUserDatum(KeyEnum.IMPORTANCE_RANK.toString(),new Integer(i),
                    UserData.SHARED);
            nr.vertex.setUserDatum(KeyEnum.IMPORTANCE_VALUE.toString(), new Double(nr.rankScore),
                    UserData.SHARED);
            
            String vtxName = (String)nr.vertex.getUserDatum(KeyEnum.VERTEX_NAME.toString());
            
            if(keep < neighborhood)
            {
                int relRank = (int)((1.0f - ((float)keep/(float)(neighborhood)))*100f);
                
                if(hm.containsKey(vtxName))
                {
                    nr.vertex.setUserDatum(KeyEnum.VERTEX_TYPE.toString(), VertexStateEnum.COMBINED_VERTEX,
                        UserData.SHARED);
                }
                else
                {
                    //if the rank is zero, the order is random so ignore these nodes, build up the neighborhood
                    if(nr.rankScore > 0)
                    {
                    nr.vertex.setUserDatum(KeyEnum.INCLUDE_KEY.toString(), new Boolean(true),
                        UserData.SHARED);
                    nr.vertex.setUserDatum(KeyEnum.IMPORTANCE_REL_RANK.toString(),new Integer(relRank),
                        UserData.SHARED);
                    nr.vertex.setUserDatum(KeyEnum.VERTEX_TYPE.toString(), VertexStateEnum.AFFILIATED_VERTEX,
                        UserData.SHARED);
                    }
                    else
                    {
                        nr.vertex.setUserDatum(KeyEnum.INCLUDE_KEY.toString(), new Boolean(false),
                            UserData.SHARED);
                        nr.vertex.setUserDatum(KeyEnum.VERTEX_TYPE.toString(), VertexStateEnum.NO_ASSOCIATION,
                            UserData.SHARED);
                    }
                    keep++;
                }
            }
            else
            {
                 if(hm.containsKey(vtxName))
                     continue;
                 else
                 {
                    nr.vertex.setUserDatum(KeyEnum.INCLUDE_KEY.toString(), new Boolean(false),
                        UserData.SHARED);
                    nr.vertex.setUserDatum(KeyEnum.VERTEX_TYPE.toString(), VertexStateEnum.NO_ASSOCIATION,
                        UserData.SHARED);
                 }
            }
        }
        //pr.printRankings(true,true);
        return gc;
    }
    
}

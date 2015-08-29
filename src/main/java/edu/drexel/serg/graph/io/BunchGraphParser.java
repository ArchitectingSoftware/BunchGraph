/*
 * BunchGraphParser.java
 *
 * Created on November 20, 2006, 9:25 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package edu.drexel.serg.graph.io;

import edu.uci.ics.jung.utils.UserData;
import java.io.*;
import java.util.*;
import edu.uci.ics.jung.graph.impl.*;
import edu.uci.ics.jung.graph.decorators.*;
import edu.uci.ics.jung.graph.*; 
import edu.uci.ics.jung.algorithms.importance.*;

import edu.drexel.serg.graph.*;


/**
 *
 * @author Brian Mitchell
 */
public class BunchGraphParser {
    
    /** Creates a new instance of BunchGraphParser */
    private class GraphNode
    {
        private String nodeName;
        ArrayList<GraphEdge> outEdges = new ArrayList<GraphEdge>();

        public GraphNode(String nodeName)
        {
            this.nodeName = nodeName;
        }
        public String getNodeName() {
            return nodeName;
        }

        public void setNodeName(String nodeName) {
            this.nodeName = nodeName;
        }
        
        public void addEdge(GraphEdge ge)
        {
            outEdges.add(ge);
        }
        
        public List<GraphEdge> getOutEdges()
        {
            return outEdges;
        }
    }
    private class GraphEdge
    {
        private GraphNode srcNode;
        private GraphNode dstNode;
        private int       weight;
        
        public GraphEdge(GraphNode src, GraphNode dest)
        {
            srcNode = src;
            dstNode = dest;
            weight = 1;
        }
        
        public GraphEdge(GraphNode src, GraphNode dest, int wgt)
        {
            srcNode = src;
            dstNode = dest;
            weight = wgt;
        }

        public GraphNode getSrcNode() {
            return srcNode;
        }

        public GraphNode getDstNode() {
            return dstNode;
        }

        public int getWeight() {
            return weight;
        }
    }
    private class InputGraph
    {
        private HashMap<String,GraphNode> nodeMap = new HashMap<String,GraphNode>();
        private ArrayList<GraphEdge> edgeList = new ArrayList<GraphEdge>();
        
        public GraphNode getNode(String nodeName)
        {
            GraphNode node = getNodeMap().get(nodeName);
            if(node == null)
            {
                node = new GraphNode(nodeName);
                nodeMap.put(nodeName,node);
            }
            
            return node;
        }
        public void AddEdge(String src, String dest, int weight)
        {
            GraphNode gnSrc = getNode(src);
            GraphNode gnDest = getNode(dest);
            GraphEdge edge = new GraphEdge(gnSrc,gnDest,weight);
            getEdgeList().add(edge);
            gnSrc.addEdge(edge);
        }
        public void AddEdge(String src, String dest)
        {
            AddEdge(src,dest,1);
        }

        public ArrayList<GraphEdge> getEdgeList() {
            return edgeList;
        }

        public HashMap<String, GraphNode> getNodeMap() {
            return nodeMap;
        }
        
        public void PrintMe()
        {
            for(int i = 0; i < edgeList.size(); i++)
            {
                GraphEdge ge = edgeList.get(i);
                System.out.println(i+": "+ge.getSrcNode().getNodeName()+" -->"+
                        ge.getDstNode().getNodeName()+" ("+ ge.getWeight()+")");
            }
        }
    }
    
    public BunchGraphParser() {
    }
    
        public GraphContainer parseInput(String fileName)
        {
            try
            {
                InputGraph inGraph = new InputGraph();
                BufferedReader reader = new BufferedReader(new FileReader(fileName));
                String currentLine = null;
                while((currentLine = reader.readLine()) != null)
                {
                    if(currentLine == "") continue;
                    
                     StringTokenizer tok = new StringTokenizer(currentLine," \r\n\t,;");
                     int tokCount = tok.countTokens();
                     if(tokCount == 0)
                         continue;
                     
                     String first = tok.nextToken();
                     if (first.charAt(0) == '/' && first.charAt(1) == '/') { //then its a comment, ignore
                        continue;
                     }
                     
                     String second = tok.nextToken();
                     Integer third = null;
                     
                     if(tokCount >= 3)
                         third = Integer.parseInt(tok.nextToken());
                     
                     if(third != null)
                         inGraph.AddEdge(first,second,third.intValue());
                     else
                         inGraph.AddEdge(first,second);
                }
                //for now print graph
                inGraph.PrintMe();
                
                //temp graph build
                DirectedSparseGraph g = new DirectedSparseGraph();
                EdgeWeightLabeller edgeWeights = EdgeWeightLabeller.getLabeller(g);
                StringLabeller vtxNames = StringLabeller.getLabeller(g);
                Iterator<String> ndeI = inGraph.getNodeMap().keySet().iterator();
                int idLabel = 1;
                while(ndeI.hasNext())
                {
                    String ndeName = ndeI.next();
                    DirectedSparseVertex nv = new DirectedSparseVertex();
                    g.addVertex(nv);
                    nv.addUserDatum(KeyEnum.VERTEX_NAME.toString(),ndeName,UserData.SHARED);
                    nv.addUserDatum(KeyEnum.GRAPHML_ID.toString(), new Integer(idLabel++), UserData.SHARED);
                    vtxNames.setLabel(nv,ndeName);
                }
                
                //now the edges
                ArrayList<GraphEdge> edgeA = inGraph.getEdgeList();
                for(int i = 0; i < edgeA.size(); i++)
                {
                    GraphEdge currEdge = edgeA.get(i);
                    Vertex vs = vtxNames.getVertex(currEdge.getSrcNode().getNodeName());
                    Vertex vd = vtxNames.getVertex(currEdge.getDstNode().getNodeName());
                    DirectedSparseEdge newEdge = new DirectedSparseEdge(vs,vd);
                    newEdge.setUserDatum(KeyEnum.EDGE_WEIGHT.toString(),new Integer(currEdge.getWeight()),UserData.SHARED);
                    g.addEdge(newEdge);
                    newEdge.addUserDatum(KeyEnum.GRAPHML_ID.toString(), new Integer(idLabel++), UserData.SHARED);
                    edgeWeights.setNumber(newEdge,currEdge.getWeight());
                }
                
                GraphContainer gc = new GraphContainer();
                gc.setGraph(g);
                gc.setVerteLabeller(vtxNames);
                gc.setEdgeLabeller(edgeWeights);
                
                return gc;
                
                /*
                System.out.println("PAGE RANK RESULTS");
                PageRank pr = new PageRank(g,0.15,"EDGE_WEIGHT");
                pr.evaluate();
                pr.printRankings(true,true);
                
                System.out.println("\r\n\r\nHITS RESULTS");
                HITS ranker = new HITS(g);
                ranker.evaluate();
                ranker.printRankings(true,true);
                
                System.out.println("\r\n\r\n BETWEENESS RESULTS");
                BetweennessCentrality bcranker = new BetweennessCentrality(g,true,false);
                bcranker.evaluate();
                bcranker.printRankings(true,true);
                */
                
                
                
                /*
                DirectedSparseVertex v1 = new DirectedSparseVertex();
                v1.addUserDatum("label","v1",UserData.SHARED);
                DirectedSparseVertex v2 = new DirectedSparseVertex();
                v2.addUserDatum("label","v2",UserData.SHARED);
                g.addVertex(v1);
                
                g.addVertex(v2);
                DirectedSparseEdge e1 = new DirectedSparseEdge(v1,v2);
                g.addEdge(e1);
                
                EdgeWeightLabeller ewl = EdgeWeightLabeller.getLabeller(g);
                StringLabeller sl = StringLabeller.getLabeller(g);
         
               ewl.setWeight(e1,10);
               sl.setLabel(v1,"V1Label");
               sl.setLabel(v2,"V2Label");
               
               
               Set vx = g.getVertices();
               Iterator<Vertex> i = vx.iterator();
               while(i.hasNext())
               {
                   Vertex vi = i.next();
                   String slbl = sl.getLabel(vi);
                   System.out.println("LABEL = "+slbl);
               }
               
               Iterator<Edge> ei = v1.getOutEdges().iterator();
               while(ei.hasNext())
               {
                    Edge edgei = ei.next();
                    System.out.println("EdgeW = "+ewl.getWeight(edgei));
               }
               
               */
               
               //EdgeWeightLabeller ewl = new EdgeWeightLabeller(g);
               
               
                
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
            
            return null;
        }
    
}

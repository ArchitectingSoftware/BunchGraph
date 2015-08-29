/*
 * ISimplifyWithRoots.java
 *
 * Created on November 29, 2006, 8:19 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package edu.drexel.serg.graph.algorithms;

import java.util.Set;
import edu.drexel.serg.graph.GraphContainer;

/**
 *
 * @author Brian Mitchell
 */
public interface ISimplifyWithRoots {
    GraphContainer RunSimplifyWithRoots(GraphContainer gc, Set rootSet, int neighborhood);  
}

/*
 * Main.java
 *
 * Created on November 21, 2006, 3:56 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package edu.drexel.serg.graph.ui;
import edu.drexel.serg.graph.io.BunchGraphParser;
/**
 *
 * @author Brian Mitchell
 */
public class Main {
    
    /** Creates a new instance of Main */
    public Main() {
       
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
         BunchGraphParser bgp = new BunchGraphParser();
        bgp.parseInput("c:\\research\\mdgs\\compiler");
    }
    
}

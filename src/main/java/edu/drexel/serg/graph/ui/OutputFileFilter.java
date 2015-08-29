/*
 * OutputFileFilter.java
 *
 * Created on November 26, 2006, 4:10 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

/**
 *
 * @author Brian Mitchell
 */

package edu.drexel.serg.graph.ui;

import java.io.File; 
import javax.swing.filechooser.*;

public class OutputFileFilter extends FileFilter{
    
    /** Creates a new instance of OutputFileFilter */
      String[] extensions; String description;
  public OutputFileFilter(String ext) {this (new String[] {ext},null);}

  public OutputFileFilter(String[] exts, String descr)
  {
    extensions = new String[exts.length];
    for (int i=exts.length-1;i>=0;i--){extensions[i]=exts[i].toLowerCase();}
    description=(descr==null?exts[0]+" files":descr);
  }
  public boolean accept(File f)
  {
    if (f.isDirectory()) {return true;}
    String name = f.getName().toLowerCase();
    for (int i=extensions.length-1;i>=0;i--)
      {if (name.endsWith(extensions[i])) {return true;} }
    return false;
  }
  public String getDescription() {return description;}
  
  public String getFilter()
  {
      return extensions[0];
  }
}

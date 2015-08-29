/*
 * TestFrame2.java
 *
 * Created on November 23, 2006, 2:59 PM
 */

/**
 *
 * @author  Brian Mitchell
 */

package edu.drexel.serg.graph.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.Color;
import edu.drexel.serg.graph.*;
import edu.drexel.serg.graph.io.*;

import edu.uci.ics.jung.graph.impl.*;
import edu.uci.ics.jung.graph.decorators.*;
import edu.uci.ics.jung.graph.*; 
import edu.uci.ics.jung.algorithms.importance.*;
import edu.uci.ics.jung.visualization.*;
import edu.uci.ics.jung.visualization.contrib.*;
import edu.uci.ics.jung.visualization.control.*;
import edu.uci.ics.jung.visualization.subLayout.*;

import javax.swing.border.TitledBorder;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.event.*;


public class TestFrame2 extends javax.swing.JFrame {
    
    /** Creates new form TestFrame2 */
    ScalingControl scaler = new CrossoverScalingControl();
    
    public TestFrame2() {
        initComponents();
        
        this.setSize(800,600);
        
        //now do stuff
        BunchGraphParser bgp = new BunchGraphParser();
        GraphContainer gc = bgp.parseInput("c:\\research\\mdgs\\swing.mdg");
        
        //Create a simple layout frame
        //specify the Fruchterman-Rheingold layout algorithm
        final SubLayoutDecorator layout = new SubLayoutDecorator(new FRLayout(gc.getGraph()));
        final PickedState ps = new MultiPickedState();
        PluggableRenderer pr = new PluggableRenderer();
        
        final GraphContainer gcInner = gc;
         VertexStringer vertStringer = new VertexStringer() {
            public String getLabel(ArchetypeVertex v) {
                return gcInner.getVerteLabeller().getLabel(v);
            }
           };
           
          
           pr.setVertexStringer(vertStringer);

		pr.setVertexPaintFunction(new VertexPaintFunction() {
			public Paint getFillPaint(Vertex v) {
				Color k = Color.RED; //(Color) v.getUserDatum(DEMOKEY);
				if (k != null)
					return k;
				return Color.white;
			}

			public Paint getDrawPaint(Vertex v) {
				if(ps.isPicked(v)) {
					return Color.cyan;
				} else {
					return Color.BLACK;
				}
			}
		});

		pr.setEdgePaintFunction(new EdgePaintFunction() {
			public Paint getDrawPaint(Edge e) {
				Color k = Color.BLUE; //(Color) e.getUserDatum(DEMOKEY);
				if (k != null)
					return k;
				return Color.blue;
			}
            public Paint getFillPaint(Edge e)
            {
                return null;
            }
		});

        pr.setEdgeStrokeFunction(new EdgeStrokeFunction()
            {
                protected final Stroke THIN = new BasicStroke(1);
                protected final Stroke THICK= new BasicStroke(2);
                public Stroke getStroke(Edge e)
                {
                    Color c =  Color.LIGHT_GRAY; //(Color)e.getUserDatum(DEMOKEY);
                    if (c == Color.LIGHT_GRAY)
                        return THIN;
                    else 
                        return THICK;
                }
            });


		final VisualizationViewer vv = new VisualizationViewer(layout, pr);
		vv.setBackground( Color.white );
		//Tell the renderer to use our own customized color rendering
        
		//add restart button
		JButton scramble = new JButton("Restart");
		scramble.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				vv.restart();
			}

		});
		
		DefaultModalGraphMouse gm = new DefaultModalGraphMouse();
                
		vv.setGraphMouse(gm);
		vv.setPickSupport(new ShapePickSupport());
		vv.setPickedState(ps);
		
		final JToggleButton groupVertices = new JToggleButton("Group Clusters");

		//Create slider to adjust the number of edges to remove when clustering
		final JSlider edgeBetweennessSlider = new JSlider(JSlider.HORIZONTAL);
        edgeBetweennessSlider.setBackground(Color.WHITE);
		edgeBetweennessSlider.setPreferredSize(new Dimension(210, 50));
		edgeBetweennessSlider.setPaintTicks(true);
		edgeBetweennessSlider.setMaximum(gc.getGraph().numEdges());
		edgeBetweennessSlider.setMinimum(0);
		edgeBetweennessSlider.setValue(0);
		edgeBetweennessSlider.setMajorTickSpacing(10);
		edgeBetweennessSlider.setPaintLabels(true);
		edgeBetweennessSlider.setPaintTicks(true);

//		edgeBetweennessSlider.setBorder(BorderFactory.createLineBorder(Color.black));
		//TO DO: edgeBetweennessSlider.add(new JLabel("Node Size (PageRank With Priors):"));
		//I also want the slider value to appear
		final JPanel eastControls = new JPanel();
		eastControls.setOpaque(true);
		eastControls.setLayout(new BoxLayout(eastControls, BoxLayout.Y_AXIS));
		eastControls.add(Box.createVerticalGlue());
		eastControls.add(edgeBetweennessSlider);

		final String COMMANDSTRING = "Edges removed for clusters: ";
		final String eastSize = COMMANDSTRING + edgeBetweennessSlider.getValue();
		
		final TitledBorder sliderBorder = BorderFactory.createTitledBorder(eastSize);
		eastControls.setBorder(sliderBorder);
		//eastControls.add(eastSize);
		eastControls.add(Box.createVerticalGlue());
		
		groupVertices.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
					//clusterAndRecolor(layout, edgeBetweennessSlider.getValue(), 
					//		similarColors, e.getStateChange() == ItemEvent.SELECTED);
			}});


		//clusterAndRecolor(layout, 0, similarColors, groupVertices.isSelected());

		edgeBetweennessSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				if (!source.getValueIsAdjusting()) {
					int numEdgesToRemove = source.getValue();
					//clusterAndRecolor(layout, numEdgesToRemove, similarColors,
					//		groupVertices.isSelected());
					sliderBorder.setTitle(
						COMMANDSTRING + edgeBetweennessSlider.getValue());
					eastControls.repaint();
					vv.validate();
					vv.repaint();
				}
			}
		});

        JButton plus = new JButton("+");
        plus.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                scaler.scale(vv, 1.1f, vv.getCenter());
            }
        });
        JButton minus = new JButton("-");
        minus.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                scaler.scale(vv, 1/1.1f, vv.getCenter());
            }
        });
        
        JPanel zoomPanel = new JPanel(new GridLayout(0,1));
        zoomPanel.setBorder(BorderFactory.createTitledBorder("Scale"));
        zoomPanel.add(plus);
        zoomPanel.add(minus);
        
		Container content = getContentPane();
		content.add(new GraphZoomScrollPane(vv));
		JPanel south = new JPanel();
		JPanel grid = new JPanel(new GridLayout(2,1));
		grid.add(scramble);
		grid.add(groupVertices);
		south.add(zoomPanel);  //grid
		south.add(eastControls);
		JPanel p = new JPanel();
		p.setBorder(BorderFactory.createTitledBorder("Mouse Mode"));
		p.add(gm.getModeComboBox());
		south.add(p);
		content.add(south, BorderLayout.SOUTH);
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        pack();
    }// </editor-fold>//GEN-END:initComponents
    
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new TestFrame2().setVisible(true);
            }
        });
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
    
}

/*
 * MainFrameWindow.java
 *
 * Created on November 22, 2006, 5:59 PM
 */

/**
 *
 * @author  Brian Mitchell
 */

package edu.drexel.serg.graph.ui;



import java.awt.image.ImageObserver;
import java.awt.image.ImageProducer;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.awt.Color;
import java.io.InputStream;


import edu.uci.ics.jung.graph.impl.*;
import edu.uci.ics.jung.graph.decorators.*;
import edu.uci.ics.jung.graph.*; 
import edu.uci.ics.jung.algorithms.importance.*;
import edu.uci.ics.jung.utils.UserData;
import edu.uci.ics.jung.visualization.*;
import edu.uci.ics.jung.visualization.contrib.*;
import edu.uci.ics.jung.visualization.control.*;
import edu.uci.ics.jung.visualization.subLayout.*;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse.Mode;
import edu.uci.ics.jung.visualization.BirdsEyeVisualizationViewer;
import edu.uci.ics.jung.utils.Pair;

import edu.drexel.serg.graph.*;
import edu.drexel.serg.graph.io.*;
import edu.drexel.serg.graph.algorithms.*;

public class MainFrameWindow extends javax.swing.JFrame {
    
    public enum SelectMode
    {
        PAN,
        SELECT
    }
    
    
    private final static class VertexShapeSizeAspect 
    extends AbstractVertexShapeFunction 
    implements VertexSizeFunction
    {
        private boolean scaleVertex = true;
        protected static final int DEFAULT_VERTEX_SIZE = 15;
        protected static final int SCALE_RATIO = 5;
        
        public VertexShapeSizeAspect()
        {
            setSizeFunction(this);
        }
        public VertexShapeSizeAspect(boolean scale)
        {
            setScaleVertex(scale);
            setSizeFunction(this);
        }
        
        public int getSize(Vertex v)
        {
            if(isScaleVertex() == false)
                return DriverAppConstants.DefaultNodeSize;
            
            VertexStateEnum vs = (VertexStateEnum)v.getUserDatum(KeyEnum.VERTEX_TYPE.toString());
            if ((vs!=null)&&(vs == VertexStateEnum.COMBINED_VERTEX))
                return DriverAppConstants.DefaultNodeSize;
            
            Integer impValue = (Integer)v.getUserDatum(KeyEnum.IMPORTANCE_REL_RANK.toString());
            int iVal = 0;
            
            if(impValue != null)
                iVal = impValue.intValue();
         //   if (scale)
         //       return (int)(voltages.getNumber(v).doubleValue() * 30) + 20;
         //   else
            double dsize = (((double)iVal/100d) * DriverAppConstants.DefaultNodeSize * 
                    DriverAppConstants.DefaultScaleRatio) + DriverAppConstants.DefaultNodeSize;
              return (int)dsize; //default 20
        }
        
        public Shape getShape(Vertex v)
        {   
            return factory.getEllipse(v);
        }

        public boolean isScaleVertex() {
            return scaleVertex;
        }

        public void setScaleVertex(boolean scaleVertex) {
            this.scaleVertex = scaleVertex;
        }
    }
    
    protected class PopupGraphMousePlugin extends AbstractPopupGraphMousePlugin implements MouseListener 
    { 
         public PopupGraphMousePlugin() {
             this(MouseEvent.BUTTON3_MASK);
         }
         public PopupGraphMousePlugin(int modifiers) {
             super(modifiers);
         }
         
         protected void handlePopup(MouseEvent e) {
             final VisualizationViewer vv =
                     (VisualizationViewer)e.getSource();
             Point2D p = vv.inverseViewTransform(e.getPoint());
             
             PickSupport pickSupport = vv.getPickSupport();
             PickedState pstate = _vvProperty.getPickedState();
             
             if(pstate == null)
                 return;
             
             final java.util.Set sPicked = pstate.getPickedVertices();
             if((sPicked == null) || (sPicked.size() == 0))
                 return;
             
             
             JPopupMenu popup = new JPopupMenu();
             popup.add(new AbstractAction("PageRank with Priors") {
                 public void actionPerformed(ActionEvent e) {
                     PageRankSimplify prAlg = new PageRankSimplify();
                     prAlg.RunSimplifyWithRoots(_gc,sPicked,Integer.parseInt(NeighborhoodEF.getText()));
                     vv.repaint();
                 }
             });
             popup.add(new AbstractAction("HITS with Priors"){
                 public void actionPerformed(ActionEvent e) {
                     HITSSimplify prAlg = new HITSSimplify();
                     prAlg.RunSimplifyWithRoots(_gc,sPicked,Integer.parseInt(NeighborhoodEF.getText()));
                     vv.repaint();
                 }
             });
             popup.add(new AbstractAction("Markov"){
                 public void actionPerformed(ActionEvent e) {
                     MarkovSimplify prAlg = new MarkovSimplify();
                     prAlg.RunSimplifyWithRoots(_gc,sPicked,Integer.parseInt(NeighborhoodEF.getText()));
                     vv.repaint();
                 }
             });
             popup.add(new AbstractAction("KStepMarkov"){
                 public void actionPerformed(ActionEvent e) {
                     vv.repaint();
                 }
             });
             popup.add(new AbstractAction("WeightedNIPaths"){
                 public void actionPerformed(ActionEvent e) {
                     vv.repaint();
                 }
             });
             popup.show(vv, e.getX(), e.getY());
             
         }
     }
    
    public class VertexToolTips extends DefaultToolTipFunction {
        
        public String getToolTipText(Vertex v) {
           String vName = (String)v.getUserDatum(KeyEnum.VERTEX_NAME.toString());
           Boolean rankedNode = (Boolean)v.getUserDatum(KeyEnum.INCLUDE_KEY.toString());
           if(rankedNode != null)
           {
               if(rankedNode.booleanValue() == true)
               {
                   Integer impString = (Integer)v.getUserDatum(KeyEnum.IMPORTANCE_REL_RANK.toString());
                   vName += "  [Importance Ratio: "+ impString+"]";
               }
           }
           return vName;
        }
        public String getToolTipText(Edge edge) {
            Pair p = edge.getEndpoints();
            String n1 = (String)((Vertex)p.getFirst()).getUserDatum(KeyEnum.VERTEX_NAME.toString());
            String n2 = (String)((Vertex)p.getSecond()).getUserDatum(KeyEnum.VERTEX_NAME.toString());
            
            return "["+n1+"] -- ["+n2+"]";
        }
    }

    
    
    
    private SelectMode sMode = SelectMode.SELECT;
    DefaultModalGraphMouse _gm = null;
    VisualizationViewer _vvProperty = null;
    GraphContainer _gc = null;
    ScalingControl scaler = new CrossoverScalingControl();
    int            _scaleTracker = 0;
    String          defaultMDGDirectgory = null;
    VertexShapeSizeAspect _vertexPainter = null;
    //BirdsEyeVisualizationViewer _birdViewer = null;
    
    
    private boolean  isRenderingSetup = false;
    
    
    public static void center(JFrame frame) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Point center = ge.getCenterPoint();
        Rectangle bounds = ge.getMaximumWindowBounds();
        int w = Math.max(bounds.width/2, Math.min(frame.getWidth(), bounds.width));
        int h = Math.max(bounds.height/2, Math.min(frame.getHeight(), bounds.height));
        int x = center.x - w/2, y = center.y - h/2;
        frame.setBounds(x, y, w, h);
        if (w == bounds.width && h == bounds.height)
            frame.setExtendedState(Frame.MAXIMIZED_BOTH);
        frame.validate();
    }

    /** Creates new form MainFrameWindow */
    public MainFrameWindow() {
        initComponents();
        center(this);
        GraphView.setBackground(Color.WHITE);
        
        try
        {
            java.util.Properties prop = new java.util.Properties();
            //java.io.FileInputStream propIn = new java.io.FileInputStream("GraphSimplification.properties");

            ClassLoader classloader = Thread.currentThread().getContextClassLoader();
            InputStream is = classloader.getResourceAsStream("GraphSimplification.properties");

            //prop.load(propIn);
            prop.load(is);

            defaultMDGDirectgory = prop.getProperty("DefaultMDGDirectory");
            DriverAppConstants.DefaultMDGDirectory = defaultMDGDirectgory;
            
            String defaultSimplifyPct = prop.getProperty("InitialSimplificationPct");
            if(defaultSimplifyPct != null)
            {
                int newPct = Integer.parseInt(defaultSimplifyPct);
                this.SimplificationPctSlider.setValue(newPct);
                this.SimplifyPctST.setText(newPct + " %");
                DriverAppConstants.DefaultSimplificationPct = newPct;
            }
            
            String appPropStr = prop.getProperty("DefaultNodeSize");
            if(appPropStr != null)
            {
                DriverAppConstants.DefaultNodeSize = Integer.parseInt(appPropStr);
            }

            appPropStr = prop.getProperty("DefaultScaleRatio");
            if(appPropStr != null)
            {
                DriverAppConstants.DefaultScaleRatio = Integer.parseInt(appPropStr);
            }
            
            //process algorithm constants
            String AlgPropertyStr = prop.getProperty("PageRankBiasParam");
            if(AlgPropertyStr != null)
            {
                AlgorithmConstants.PageRankBiasParam = Double.parseDouble(AlgPropertyStr);
            }
            AlgPropertyStr = prop.getProperty("KStepMarkovKValue");
            if(AlgPropertyStr != null)
            {
                AlgorithmConstants.KStepMarkovKValue = Integer.parseInt(AlgPropertyStr);
            }
            AlgPropertyStr = prop.getProperty("HITSRootSetBias");
            if(AlgPropertyStr != null)
            {
                AlgorithmConstants.HITSRootSetBias = Double.parseDouble(AlgPropertyStr);
            }
            AlgPropertyStr = prop.getProperty("WeightedNIPathsAlpha");
            if(AlgPropertyStr != null)
            {
                AlgorithmConstants.WeightedNIPathsAlpha = Double.parseDouble(AlgPropertyStr);
            }
            AlgPropertyStr = prop.getProperty("WeightedNIPathsMaxDepth");
            if(AlgPropertyStr != null)
            {
                AlgorithmConstants.WeightedNIPathsMaxDepth = Integer.parseInt(AlgPropertyStr);
            }
        }
        catch(Exception e)
        {
            //do nothing, just dont use default properties
            System.out.println("Unable to open property file");
            e.printStackTrace();
        }
        
        
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        jFrame1 = new javax.swing.JFrame();
        jButton1 = new javax.swing.JButton();
        MainPannel = new javax.swing.JPanel();
        GraphView = new javax.swing.JPanel();
        GraphModePB = new javax.swing.JButton();
        ShowVertexLabelCB = new javax.swing.JCheckBox();
        jLabel1 = new javax.swing.JLabel();
        LayoutCB = new javax.swing.JComboBox();
        jLabel2 = new javax.swing.JLabel();
        SimplifyAlgorithmCB = new javax.swing.JComboBox();
        ZoomInPB = new javax.swing.JButton();
        ZoomOutPB = new javax.swing.JButton();
        ResetScalePB = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        MDGFileNameST = new javax.swing.JLabel();
        HideFilteredEdgesCB = new javax.swing.JCheckBox();
        SimplificationPctSlider = new javax.swing.JSlider();
        jLabel4 = new javax.swing.JLabel();
        SimplifyPctST = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        NeighborhoodEF = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        ScaleNodesCB = new javax.swing.JCheckBox();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        OpenMDGMnu = new javax.swing.JMenuItem();
        SaveAsMnu = new javax.swing.JMenuItem();
        ExitMnu = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();
        PropertiesMnu = new javax.swing.JMenuItem();

        org.jdesktop.layout.GroupLayout jFrame1Layout = new org.jdesktop.layout.GroupLayout(jFrame1.getContentPane());
        jFrame1.getContentPane().setLayout(jFrame1Layout);
        jFrame1Layout.setHorizontalGroup(
            jFrame1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 400, Short.MAX_VALUE)
        );
        jFrame1Layout.setVerticalGroup(
            jFrame1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 300, Short.MAX_VALUE)
        );

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("SERG Graph Simplification");
        setAlwaysOnTop(true);
        jButton1.setText("TestMe");
        jButton1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                TestMeButtonDown(evt);
            }
        });

        org.jdesktop.layout.GroupLayout MainPannelLayout = new org.jdesktop.layout.GroupLayout(MainPannel);
        MainPannel.setLayout(MainPannelLayout);
        MainPannelLayout.setHorizontalGroup(
            MainPannelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 914, Short.MAX_VALUE)
        );
        MainPannelLayout.setVerticalGroup(
            MainPannelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 0, Short.MAX_VALUE)
        );

        GraphView.setLayout(new java.awt.BorderLayout());

        GraphModePB.setText("PanMode");
        GraphModePB.setEnabled(false);
        GraphModePB.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                GraphModePBChange(evt);
            }
        });

        ShowVertexLabelCB.setSelected(true);
        ShowVertexLabelCB.setText("Show Vertex Labels");
        ShowVertexLabelCB.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        ShowVertexLabelCB.setMargin(new java.awt.Insets(0, 0, 0, 0));
        ShowVertexLabelCB.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                ShowLabelStateChange(evt);
            }
        });

        jLabel1.setText("Layout:");

        LayoutCB.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Fruchterman-Reingold", "Kamada-Kawai", "Spring", "Self-Organizing Map", "Circle" }));
        LayoutCB.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                LayoutCBItemStateChanged(evt);
            }
        });
        LayoutCB.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                LayoutCBPropertyChange(evt);
            }
        });
        LayoutCB.addInputMethodListener(new java.awt.event.InputMethodListener() {
            public void caretPositionChanged(java.awt.event.InputMethodEvent evt) {
            }
            public void inputMethodTextChanged(java.awt.event.InputMethodEvent evt) {
                LayoutCBInputMethodTextChanged(evt);
            }
        });

        jLabel2.setText("Simplification Algorithm:");

        SimplifyAlgorithmCB.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "NONE", "PageRank", "HITS", "EdgeFlow", "BaryCenter" }));
        SimplifyAlgorithmCB.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                SimplifyAlgorithmCBItemStateChanged(evt);
            }
        });

        ZoomInPB.setText("Zoom In");
        ZoomInPB.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                ZoomInPBMouseClicked(evt);
            }
        });

        ZoomOutPB.setText("Zoom Out");
        ZoomOutPB.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                ZoomOutPBMouseClicked(evt);
            }
        });

        ResetScalePB.setText("Reset Scale");
        ResetScalePB.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                ResetScalePBMouseClicked(evt);
            }
        });

        jLabel3.setText("MDG File Name:  ");

        MDGFileNameST.setText("<<none>>");

        HideFilteredEdgesCB.setText("Hilde Filtered Edges");
        HideFilteredEdgesCB.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        HideFilteredEdgesCB.setMargin(new java.awt.Insets(0, 0, 0, 0));
        HideFilteredEdgesCB.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                HideFilteredEdgesCBStateChanged(evt);
            }
        });

        SimplificationPctSlider.setMajorTickSpacing(10);
        SimplificationPctSlider.setMinorTickSpacing(1);
        SimplificationPctSlider.setPaintLabels(true);
        SimplificationPctSlider.setPaintTicks(true);
        SimplificationPctSlider.setSnapToTicks(true);
        SimplificationPctSlider.setValue(25);
        SimplificationPctSlider.setEnabled(false);
        SimplificationPctSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                SimplificationPctSliderStateChanged(evt);
            }
        });

        jLabel4.setText("Simplifcation Percentage:");

        SimplifyPctST.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        SimplifyPctST.setText("25 %");

        jLabel5.setText("Selected Node Neighborhood:");

        NeighborhoodEF.setText("5");

        jLabel6.setFont(new java.awt.Font("Tahoma", 1, 11));
        jLabel6.setForeground(java.awt.Color.red);
        jLabel6.setText("RED: Unclassified ");

        jLabel7.setFont(new java.awt.Font("Tahoma", 1, 11));
        jLabel7.setForeground(java.awt.Color.blue);
        jLabel7.setText("BLUE:  Filtered Root ");

        jLabel8.setFont(new java.awt.Font("Tahoma", 1, 11));
        jLabel8.setForeground(java.awt.Color.green);
        jLabel8.setText("GREEN: Global Root ");

        jLabel9.setFont(new java.awt.Font("Tahoma", 1, 11));
        jLabel9.setForeground(java.awt.Color.magenta);
        jLabel9.setText("MAGNETA: Related to Root ");

        ScaleNodesCB.setSelected(true);
        ScaleNodesCB.setText("Scale Nodes to Importance");
        ScaleNodesCB.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        ScaleNodesCB.setMargin(new java.awt.Insets(0, 0, 0, 0));
        ScaleNodesCB.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                ScaleNodesCBStateChanged(evt);
            }
        });

        jMenu1.setText("File");
        OpenMDGMnu.setText("Open MDG...");
        OpenMDGMnu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                OpenMDGMnuActionPerformed(evt);
            }
        });

        jMenu1.add(OpenMDGMnu);

        SaveAsMnu.setText("Save As...");
        SaveAsMnu.setEnabled(false);
        SaveAsMnu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SaveAsMnuActionPerformed(evt);
            }
        });

        jMenu1.add(SaveAsMnu);

        ExitMnu.setText("Exit");
        ExitMnu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ExitMnuActionPerformed(evt);
            }
        });

        jMenu1.add(ExitMnu);

        jMenuBar1.add(jMenu1);

        jMenu2.setText("Options");
        PropertiesMnu.setText("Change Properties...");
        PropertiesMnu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                PropertiesMnuActionPerformed(evt);
            }
        });

        jMenu2.add(PropertiesMnu);

        jMenuBar1.add(jMenu2);

        setJMenuBar(jMenuBar1);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(MainPannel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(jLabel3)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(MDGFileNameST, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 322, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jLabel6)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jLabel7)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jLabel8)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jLabel9))
            .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                    .add(jButton1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(ZoomInPB, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .add(11, 11, 11)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(layout.createSequentialGroup()
                        .add(jLabel5)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(NeighborhoodEF, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 39, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(GraphModePB)
                    .add(layout.createSequentialGroup()
                        .add(ZoomOutPB)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(ResetScalePB)))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 78, Short.MAX_VALUE)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                        .add(jLabel1)
                        .add(18, 18, 18))
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                            .add(jLabel4)
                            .add(jLabel2)
                            .add(SimplifyPctST, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 60, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)))
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(SimplificationPctSlider, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 228, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false)
                        .add(org.jdesktop.layout.GroupLayout.LEADING, LayoutCB, 0, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .add(org.jdesktop.layout.GroupLayout.LEADING, SimplifyAlgorithmCB, 0, 202, Short.MAX_VALUE)))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(ShowVertexLabelCB)
                    .add(HideFilteredEdgesCB)
                    .add(ScaleNodesCB))
                .add(62, 62, 62))
            .add(GraphView, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 914, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel3)
                    .add(jLabel9)
                    .add(jLabel8)
                    .add(jLabel7)
                    .add(jLabel6)
                    .add(MDGFileNameST))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(GraphView, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 469, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(MainPannel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                    .add(layout.createSequentialGroup()
                        .add(6, 6, 6)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(GraphModePB)
                            .add(jButton1))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(ZoomOutPB)
                            .add(ResetScalePB)
                            .add(ZoomInPB))
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(layout.createSequentialGroup()
                                .add(22, 22, 22)
                                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                    .add(jLabel5)
                                    .add(NeighborhoodEF, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                            .add(layout.createSequentialGroup()
                                .add(13, 13, 13)
                                .add(jLabel4))))
                    .add(layout.createSequentialGroup()
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jLabel1)
                            .add(LayoutCB, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(ShowVertexLabelCB))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jLabel2)
                            .add(SimplifyAlgorithmCB, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(HideFilteredEdgesCB))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                .add(SimplificationPctSlider, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .add(SimplifyPctST))
                            .add(ScaleNodesCB))))
                .addContainerGap())
        );
        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void PropertiesMnuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_PropertiesMnuActionPerformed
// TODO add your handling code here:
        PropertyManagerDialog pmd = new PropertyManagerDialog();
        pmd.show();
    }//GEN-LAST:event_PropertiesMnuActionPerformed

    private void ScaleNodesCBStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_ScaleNodesCBStateChanged
// TODO add your handling code here:
        boolean state = this.ScaleNodesCB.isSelected();
        if(_vertexPainter == null)
            return;
        
        _vertexPainter.setScaleVertex(state);
        
        //redraw graph with new value
        _vvProperty.invalidate();
        _vvProperty.validate();
        _vvProperty.repaint();
    }//GEN-LAST:event_ScaleNodesCBStateChanged

    private void SimplificationPctSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_SimplificationPctSliderStateChanged
// TODO add your handling code here:
        JSlider source = (JSlider) evt.getSource();
        int newPct = source.getValue();
        this.SimplifyPctST.setText(newPct+" %");
	if (!source.getValueIsAdjusting()) {
            if(_vvProperty != null)
                RunSimplifyUpdate();
        }
    }//GEN-LAST:event_SimplificationPctSliderStateChanged

    private void HideFilteredEdgesCBStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_HideFilteredEdgesCBStateChanged
// TODO add your handling code here:
        //just need to force a repaint
        if(_vvProperty != null)
        {
            _vvProperty.invalidate();
            _vvProperty.validate();
            _vvProperty.repaint();
        }
    }//GEN-LAST:event_HideFilteredEdgesCBStateChanged

    private void SaveAsMnuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SaveAsMnuActionPerformed
// TODO add your handling code here:
        JFileChooser jfc;
        if(defaultMDGDirectgory != null)
            jfc = new JFileChooser(defaultMDGDirectgory);
        else
            jfc = new JFileChooser();
        
        OutputFileFilter jpgFilter = new OutputFileFilter(new String[]{"jpg"},"JPEG Files (*.jpg)");
        OutputFileFilter pngFilter = new OutputFileFilter(new String[]{"png"},"PNG Files (*.png)");
        OutputFileFilter gifFilter = new OutputFileFilter(new String[]{"gif"},"GIF Files (*.gif)");
        
        jfc.addChoosableFileFilter(gifFilter);
        jfc.addChoosableFileFilter(pngFilter);
        jfc.addChoosableFileFilter(jpgFilter);
        
        
        int retVal = jfc.showSaveDialog(this);
        if(retVal == JFileChooser.APPROVE_OPTION)
        {
            try
            {
                String fname = jfc.getSelectedFile().getCanonicalPath();
                                
                OutputFileFilter off = (OutputFileFilter)jfc.getFileFilter();
                String ext = off.getFilter();
                
                if(!fname.endsWith("."+off.getFilter()))
                    fname += "."+off.getFilter();
                
                
                java.io.File f = new java.io.File(fname);
                
                if(f.exists())
                {
                    int returnValue = JOptionPane.showConfirmDialog(this,"Do you want to overwrite "+fname+"?  This file already exists",
                            "File Exists",JOptionPane.YES_NO_OPTION);
                    
                    if(returnValue == JOptionPane.YES_OPTION)
                    {
                        System.out.println("File Name: "+fname);
                        
                        java.awt.image.BufferedImage image = getImage();
                
                        java.io.BufferedOutputStream out = new java.io.BufferedOutputStream(new
                        java.io.FileOutputStream(fname));
                
                        javax.imageio.ImageIO.write(image,off.getFilter(),out);
                    }
                }
                else
                {
                        System.out.println("File Name: "+fname);
                        
                        java.awt.image.BufferedImage image = getImage();
                
                        java.io.BufferedOutputStream out = new java.io.BufferedOutputStream(new
                        java.io.FileOutputStream(fname));
                
                        javax.imageio.ImageIO.write(image,off.getFilter(),out);
                }
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }
        
    }//GEN-LAST:event_SaveAsMnuActionPerformed

    protected java.awt.image.BufferedImage getImage()
    {
        try
        {
            
            _vvProperty.setDoubleBuffered(false);
            java.awt.image.BufferedImage image = 
                    new java.awt.image.BufferedImage(_vvProperty.getWidth(), _vvProperty.getHeight(),
                        java.awt.image.BufferedImage.TYPE_INT_BGR);
            
            java.awt.Graphics2D g2= image.createGraphics();
            
            g2.setBackground(Color.WHITE);
            g2.fillRect(0,0,_vvProperty.getWidth(),_vvProperty.getHeight());
            _vvProperty.paintAll(g2);
            
            _vvProperty.setDoubleBuffered(true);
                  
            return image;       
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        
        return null;
    }
    private void OpenMDGMnuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_OpenMDGMnuActionPerformed
// TODO add your handling code here:
        //open a mdg file
        JFileChooser jfc;
        String fname = null;
        if(DriverAppConstants.DefaultMDGDirectory != null)
            jfc = new JFileChooser(DriverAppConstants.DefaultMDGDirectory);
        else
            jfc = new JFileChooser();
        
        int retVal = jfc.showOpenDialog(this);
        if(retVal == JFileChooser.APPROVE_OPTION)
        {
            try
            {
                fname = jfc.getSelectedFile().getCanonicalPath();
      
                
                BunchGraphParser bgp = new BunchGraphParser();
                GraphContainer gc = bgp.parseInput(fname);
                _gc = gc;
                
                if(isRenderingSetup == false)
                {
                    Render(gc);
                    isRenderingSetup = true;
                }
                else
                {
                    //need to redo the layout to redraw graph
                    ReDoLayout(gc);
                }
                this.SaveAsMnu.setEnabled(true);
                MDGFileNameST.setText(fname + "    [Nodes: "+gc.getGraph().getVertices().size()+
                        "   Edges: "+gc.getGraph().getEdges().size()+"]");
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }
    }//GEN-LAST:event_OpenMDGMnuActionPerformed

    protected void RunSimplifyUpdate()
    {
        ISimplifyAlgorithm pAlg = ClearSimplify.CreateInstance();
            
        if(this.SimplifyAlgorithmCB.getSelectedItem() == "PageRank")
        {
                pAlg = PageRankSimplify.CreateInstance();
                SimplificationPctSlider.setEnabled(true);
        }
        else if(this.SimplifyAlgorithmCB.getSelectedItem() == "HITS")
        {
                pAlg = HITSSimplify.CreateInstance();
                SimplificationPctSlider.setEnabled(true);
        } 
        else if(this.SimplifyAlgorithmCB.getSelectedItem() == "EdgeFlow")
        {
                pAlg = BetweennessSimplify.CreateInstance();
                SimplificationPctSlider.setEnabled(true);
        }
        else if(this.SimplifyAlgorithmCB.getSelectedItem() == "BaryCenter")
        {
                pAlg = BaryCenterSimplify.CreateInstance();
                SimplificationPctSlider.setEnabled(true);
        }
        else
        {
                SimplificationPctSlider.setEnabled(false);
                resetGraphProperties();
        }
        
            
        _gc.setSimplifyPct(this.SimplificationPctSlider.getValue());
        pAlg.RunSimplify(_gc);
            
        _vvProperty.invalidate();
        _vvProperty.validate();
        _vvProperty.repaint();
    }
    
    private void SimplifyAlgorithmCBItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_SimplifyAlgorithmCBItemStateChanged
// TODO add your handling code here:
        if ( evt.getStateChange() == java.awt.event.ItemEvent.SELECTED )
        {
            RunSimplifyUpdate();
        }
    }//GEN-LAST:event_SimplifyAlgorithmCBItemStateChanged

    private void ResetScalePBMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ResetScalePBMouseClicked
// TODO add your handling code here:
        
        float adjustment = _scaleTracker * 0.1f;
        if(adjustment < 0) //we are zoomed out so zoom in
        {
            adjustment *= -1f; //convert to positive
            scaler.scale(_vvProperty, 1+adjustment, _vvProperty.getCenter());
            
        }
        else if(adjustment > 0) //we are zoomed in, so zoom out
        {
            adjustment = 1 / adjustment;
            scaler.scale(_vvProperty, adjustment, _vvProperty.getCenter());
            
        }
        _scaleTracker = 0;
    }//GEN-LAST:event_ResetScalePBMouseClicked

    private void ZoomOutPBMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ZoomOutPBMouseClicked
// TODO add your handling code here:
        scaler.scale(_vvProperty, 1/1.1f, _vvProperty.getCenter());
        _scaleTracker--;
    }//GEN-LAST:event_ZoomOutPBMouseClicked

    private void ZoomInPBMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ZoomInPBMouseClicked
// TODO add your handling code here:
        scaler.scale(_vvProperty, 1.1f, _vvProperty.getCenter());
        _scaleTracker++;
    }//GEN-LAST:event_ZoomInPBMouseClicked

    private void ExitMnuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ExitMnuActionPerformed
// TODO add your handling code here:
        this.dispose();
    }//GEN-LAST:event_ExitMnuActionPerformed

    private void LayoutCBPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_LayoutCBPropertyChange
// TODO add your handling code here:
        String sel = (String)LayoutCB.getSelectedItem();
    }//GEN-LAST:event_LayoutCBPropertyChange

    private void LayoutCBInputMethodTextChanged(java.awt.event.InputMethodEvent evt) {//GEN-FIRST:event_LayoutCBInputMethodTextChanged
// TODO add your handling code here:
        String sel = (String)LayoutCB.getSelectedItem();
    }//GEN-LAST:event_LayoutCBInputMethodTextChanged

    private void ReDoLayout(GraphContainer gc)
    {
        Class [] layoutMgr = new Class[] 
        {
            FRLayout.class,
            KKLayout.class,
            edu.uci.ics.jung.visualization.SpringLayout.class,
            ISOMLayout.class,
            CircleLayout.class,
        };
        int sel = LayoutCB.getSelectedIndex();
        
        Object[] constructorArgs =
                { gc.getGraph()};
        
        Class lay = layoutMgr[sel];
        
        try
        {
        
            java.lang.reflect.Constructor constructor = lay.getConstructor(new Class[] {Graph.class});
                
            Object o = constructor.newInstance(constructorArgs);
            Layout l = (Layout) o;
            _vvProperty.stop();
            //_vvProperty.setGraphLayout(l);
            _vvProperty.setGraphLayout(l,true);
            //_vvProperty.setGraphLayout(l, false);
            _vvProperty.restart();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
    private void LayoutCBItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_LayoutCBItemStateChanged
        
        ReDoLayout(_gc);
    }//GEN-LAST:event_LayoutCBItemStateChanged

    private void ShowLabelStateChange(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_ShowLabelStateChange
// TODO add your handling code here:
        _vvProperty.invalidate();
        _vvProperty.validate();
        _vvProperty.repaint();
    }//GEN-LAST:event_ShowLabelStateChange

    private void GraphModePBChange(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_GraphModePBChange
// TODO add your handling code here:
        
        if(sMode == SelectMode.PAN)
        {
            _gm.setMode(Mode.PICKING);
            sMode = SelectMode.SELECT;
            this.GraphModePB.setText("Select Mode");
        }
        else
        {
            _gm.setMode(Mode.TRANSFORMING);
            sMode = SelectMode.PAN;
            this.GraphModePB.setText("Pan Mode");
        }
        
    }//GEN-LAST:event_GraphModePBChange

   private void Render(GraphContainer gc)
   {
        final SubLayoutDecorator layout = new SubLayoutDecorator(new FRLayout(gc.getGraph()));
        final PickedState ps = new MultiPickedState();
        PluggableRenderer pr = new PluggableRenderer();
        
        //paint Vertex paint manager
	pr.setVertexPaintFunction(new VertexPaintFunction() {
            public Paint getFillPaint(Vertex v) 
            {    
                //todo simplify this logic
                Boolean incKey = (Boolean)v.getUserDatum(KeyEnum.INCLUDE_KEY.toString());
                if (ps.isPicked(v))
                    return Color.YELLOW;
                
                if (incKey == null)
                    return Color.RED;
                else
                {
                    if(incKey.booleanValue() == true)
                    {
                        VertexStateEnum vs = (VertexStateEnum)v.getUserDatum(KeyEnum.VERTEX_TYPE.toString());
                        if(vs == VertexStateEnum.ROOT_VERTEX)
                            return Color.GREEN;
                        else if (vs == VertexStateEnum.COMBINED_VERTEX)
                            return Color.BLUE;
                        else if (vs == VertexStateEnum.AFFILIATED_VERTEX)
                            return Color.MAGENTA;
                        else
                            return Color.GRAY;
                    }
                    else if(HideFilteredEdgesCB.isSelected() == true)
                        return null;
                    else
                        return Color.LIGHT_GRAY;
                }
            }

            public Paint getDrawPaint(Vertex v) {
                Boolean incKey = (Boolean)v.getUserDatum(KeyEnum.INCLUDE_KEY.toString());
		if(ps.isPicked(v)) {
			return Color.BLUE;
		} else {
                        if ((incKey == null) || (HideFilteredEdgesCB.isSelected() == false) || (incKey.booleanValue() == true))
                            return Color.BLACK;
                        else
                            return null;
		}
            }
        });

	pr.setEdgePaintFunction(new EdgePaintFunction() {
            public Paint getDrawPaint(Edge e) {
                Pair p = e.getEndpoints();
                Vertex v1 = (Vertex)p.getFirst();
                Vertex v2 = (Vertex)p.getSecond();
                Boolean v1Key = (Boolean)v1.getUserDatum(KeyEnum.INCLUDE_KEY.toString());
                Boolean v2Key = (Boolean)v2.getUserDatum(KeyEnum.INCLUDE_KEY.toString());
                if((v1Key == null) || (v2Key == null))
                    return Color.BLUE;
                
                if((v1Key.booleanValue()==true) && (v2Key.booleanValue()==true))
                    return Color.BLACK;
                else if(HideFilteredEdgesCB.isSelected() == true)
                    return null;
                else
                    return Color.LIGHT_GRAY;
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
                Color c = Color.LIGHT_GRAY; //(Color)e.getUserDatum(DEMOKEY);
                if (c == Color.LIGHT_GRAY)
                    return THIN;
                else 
                    return THICK;
            }
        });

        //control the edge labels
        final GraphContainer gcInner = gc;
        VertexStringer vertStringer = new VertexStringer() {
            public String getLabel(ArchetypeVertex v) {
                Boolean incKey = (Boolean)v.getUserDatum(KeyEnum.INCLUDE_KEY.toString());
                if (ShowVertexLabelCB.isSelected())
                {
                    if ((incKey == null) || (HideFilteredEdgesCB.isSelected() == false) || (incKey.booleanValue() == true))
                        return _gc.getVerteLabeller().getLabel(v);
                    else
                        return null;
                }
                else
                    return null;
            }
        };
        pr.setVertexStringer(vertStringer);
        
        //make the verte4x font function bold
        ConstantVertexFontFunction vfont = new ConstantVertexFontFunction(
                new Font("Helvetica", Font.BOLD, 12));
        pr.setVertexFontFunction(vfont);
        
        Dimension dgv = GraphView.getSize();
        Dimension dimVV = new Dimension(dgv.width,dgv.height);
        
        final VisualizationViewer vv = new VisualizationViewer(layout, pr, dimVV); //remove last arg later
        _vvProperty = vv;
        vv.setBackground( Color.white );
       
        
        
            
        DefaultModalGraphMouse gm = new DefaultModalGraphMouse();
	vv.setGraphMouse(gm);
	vv.setPickSupport(new ShapePickSupport());
	vv.setPickedState(ps);
        
        vv.setToolTipFunction(new VertexToolTips());
        gm.add(new PopupGraphMousePlugin());
        //set class property for later
        _gm = gm;
        
                
        GraphZoomScrollPane gvsp = new GraphZoomScrollPane(vv);   
        GraphView.add(gvsp);
        
        _gm.setMode(Mode.PICKING);
            
        //this.ReDoLayout(gc);
        
        _vertexPainter = new VertexShapeSizeAspect(ScaleNodesCB.isSelected());
        
        pr.setVertexShapeFunction(_vertexPainter);

        //render it
        vv.invalidate();
        vv.revalidate();
        vv.repaint();
        
        //JScrollBar horiz = gvsp.getHorizontalScrollBar();
        //JScrollBar vert = gvsp.getVerticalScrollBar();
        
        
        //Dimension d = GraphView.getLocation().getSize();
        //int px = d.width / 3;
        //int py = d.height / 3;
        //vv.setLocation(px,py);
        
        
        _gc = gc;
        this.GraphModePB.setEnabled(true);
   }

    private void resetGraphProperties()
    {
        Graph g = _gc.getGraph();
        java.util.Iterator vi = g.getVertices().iterator();
        while(vi.hasNext()){
            Vertex v = (Vertex)vi.next();

            v.removeUserDatum(KeyEnum.INCLUDE_KEY.toString());
            v.setUserDatum(KeyEnum.VERTEX_TYPE.toString(), VertexStateEnum.NO_ASSOCIATION,
                    UserData.SHARED);
            v.setUserDatum(KeyEnum.IMPORTANCE_REL_RANK.toString(),new Integer(0),
                    UserData.SHARED);
        }

    }
   

    private void TestMeButtonDown(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_TestMeButtonDown
// TODO add your handling code here:
        Point p = _vvProperty.getLocation();
        System.out.println("X="+p.x+" Y="+p.y);
        
        PickSupport ps = _vvProperty.getPickSupport();
        PickedState pstate = _vvProperty.getPickedState();
        
        if (pstate != null)
        {
            java.util.Set s = pstate.getPickedVertices();
            java.util.Iterator i = s.iterator();
            while(i.hasNext())
            {
                Vertex v = (Vertex)i.next();
                System.out.println("Vertex: "+ (String)v.getUserDatum(KeyEnum.VERTEX_NAME.toString()));
            }
        }
    }//GEN-LAST:event_TestMeButtonDown
    
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new MainFrameWindow().setVisible(true);
            }
        });
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem ExitMnu;
    private javax.swing.JButton GraphModePB;
    private javax.swing.JPanel GraphView;
    private javax.swing.JCheckBox HideFilteredEdgesCB;
    private javax.swing.JComboBox LayoutCB;
    private javax.swing.JLabel MDGFileNameST;
    private javax.swing.JPanel MainPannel;
    private javax.swing.JTextField NeighborhoodEF;
    private javax.swing.JMenuItem OpenMDGMnu;
    private javax.swing.JMenuItem PropertiesMnu;
    private javax.swing.JButton ResetScalePB;
    private javax.swing.JMenuItem SaveAsMnu;
    private javax.swing.JCheckBox ScaleNodesCB;
    private javax.swing.JCheckBox ShowVertexLabelCB;
    private javax.swing.JSlider SimplificationPctSlider;
    private javax.swing.JComboBox SimplifyAlgorithmCB;
    private javax.swing.JLabel SimplifyPctST;
    private javax.swing.JButton ZoomInPB;
    private javax.swing.JButton ZoomOutPB;
    private javax.swing.JButton jButton1;
    private javax.swing.JFrame jFrame1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenuBar jMenuBar1;
    // End of variables declaration//GEN-END:variables
    
}

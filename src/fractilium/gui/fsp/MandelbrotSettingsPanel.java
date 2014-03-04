/* 
 * !---------------------------------------------------------------------------!
 *   MandelbrotSettingsPanel.java
 * 
 *   Provides an interface to MandelbrotEngine. Can be used as a standalone
 *   panel.
 * 
 *   Creation date: 04/12/2012
 *   Author: Arindam Biswas <ari.bsws at gmail.com>
 * !---------------------------------------------------------------------------!
 */

package fractilium.gui.fsp;

import fractilium.Main;
import fractilium.engine.MandelbrotEngine;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 *
 * @author Arindam Biswas <ari.bsws at gmail.com>
 */
public class MandelbrotSettingsPanel extends javax.swing.JPanel implements MandelbrotEngine.EventHandler {
    
    public static interface ParentContainer {
        
    }
    
    private static final int PRECISION = 200;
    private int plusChainSampleSize, plusChainStep, plusChainPosition, plusChainLimit;
    private double imageRotation;
    private Rectangle outputSize;
    private MathContext mathCont, mathContDisp; // mcd is the MathContext for display values
    private BigDecimal planeMinX, planeMinY, planeMaxX, planeMaxY, planeUnitX, planeUnitY, selMinX, selMinY, selMaxX, selMaxY;
    private Main m;
    private boolean renderInProgress, starTask, plusTask;
    private MandelbrotEngine.Statistics stats;

    /**
     * Creates new form MandelbrotSettingsPanel
     */
    public MandelbrotSettingsPanel(Main m, Rectangle outputSize) {
        initComponents();
        if (!outputSize.isEmpty()) {
            this.outputSize = outputSize;
        } else {
            this.outputSize = new Rectangle(100, 100);

        }
        imageRotation = 0;
        starTask = plusTask = false;
        mathCont = new MathContext(PRECISION, RoundingMode.HALF_EVEN);
        mathContDisp = new MathContext(4, RoundingMode.HALF_EVEN);
        planeMinX = new BigDecimal("-2", mathCont);
        planeMaxX = new BigDecimal("1", mathCont);
        planeMinY = new BigDecimal("-1.5", mathCont);
        planeMaxY = new BigDecimal("1.5", mathCont);
        setSelRenRegion(planeMinX, planeMaxX, planeMinY, planeMaxY);
        setCurRenRegion(planeMinX, planeMaxX, planeMinY, planeMaxY);
        this.m = m;
        stats = new MandelbrotEngine.Statistics(0, 0, 0, 0, 0);
        MandelbrotEngine.initialize(this, PRECISION);
    }

    private MandelbrotEngine.Parameters.MandelbrotVariant getMandelbrotVariant(String s) {
        switch (s) {
            case "Regular":
                return MandelbrotEngine.Parameters.MandelbrotVariant.REGULAR;
            case "Buddhabrot":
                return MandelbrotEngine.Parameters.MandelbrotVariant.BUDDHABROT;
            default:
                return MandelbrotEngine.Parameters.MandelbrotVariant.REGULAR;
        }
    }

    private MandelbrotEngine.Parameters.ColouringMethod getColouringMethod(String s) {
        switch (s) {
            case "Regular":
                return MandelbrotEngine.Parameters.ColouringMethod.REGULAR;
            case "Red":
                return MandelbrotEngine.Parameters.ColouringMethod.RED;
            case "Green":
                return MandelbrotEngine.Parameters.ColouringMethod.GREEN;
            case "Blue":
                return MandelbrotEngine.Parameters.ColouringMethod.BLUE;
            default:
                return MandelbrotEngine.Parameters.ColouringMethod.REGULAR;
        }
    }

    public void setOutputSize(Rectangle r) {
        outputSize = r;
        planeUnitX = planeMaxX.subtract(planeMinX, mathCont).divide(new BigDecimal(outputSize.width + 1, mathCont), mathCont); // Zero output size means the output has just one pixel.
        planeUnitY = planeMaxY.subtract(planeMinY, mathCont).divide(new BigDecimal(outputSize.height + 1, mathCont), mathCont); // Zero output size means the output has just one pixel.
        sSizeLabel.setText(r.width + "x" + r.height);
    }

    public void setSelectionRegion(Rectangle r) {
        BigDecimal selMinX, selMinY, selMaxX, selMaxY, temp1, temp2, temp3;
        double aspectRatio;

        selMinX = planeMinX.add(planeUnitX.multiply(new BigDecimal(r.x, mathCont)), mathCont);
        selMaxY = planeMaxY.subtract(planeUnitY.multiply(new BigDecimal(r.y, mathCont)), mathCont);
        selMaxX = selMinX.add(planeUnitX.multiply(new BigDecimal(r.width + 1, mathCont), mathCont));
        selMinY = selMaxY.subtract(planeUnitY.multiply(new BigDecimal(r.height + 1, mathCont), mathCont));

        aspectRatio = outputSize.width / (double) outputSize.height;
        temp3 = selMaxX.subtract(selMinX, mathCont).divide(selMaxY.subtract(selMinY, mathCont), mathCont);
        if (temp3.compareTo(new BigDecimal(aspectRatio, mathCont)) > 0) {
            temp1 = selMaxX.subtract(selMinX, mathCont);
            temp2 = selMaxY.subtract(selMinY, mathCont);
            temp1 = temp1.divide(new BigDecimal(aspectRatio, mathCont), mathCont).subtract(temp2, mathCont);
            temp1 = temp1.divide(new BigDecimal(2, mathCont), mathCont);
            selMinY = selMinY.subtract(temp1, mathCont);
            selMaxY = selMaxY.add(temp1, mathCont);
        } else if (temp3.compareTo(new BigDecimal(aspectRatio, mathCont)) < 0) {
            temp1 = selMaxX.subtract(selMinX, mathCont);
            temp2 = selMaxY.subtract(selMinY, mathCont);
            temp1 = temp2.multiply(new BigDecimal(aspectRatio, mathCont), mathCont).subtract(temp1, mathCont);
            temp1 = temp1.divide(new BigDecimal(2, mathCont), mathCont);
            selMinX = selMinX.subtract(temp1, mathCont);
            selMaxX = selMaxX.add(temp1, mathCont);
        }
        setSelRenRegion(selMinX, selMaxX, selMinY, selMaxY);
    }

    private void setCurRenRegion(BigDecimal planeMinX, BigDecimal planeMaxX, BigDecimal planeMinY, BigDecimal planeMaxY) {
        this.planeMinX = planeMinX;
        this.planeMaxX = planeMaxX;
        this.planeMinY = planeMinY;
        this.planeMaxY = planeMaxY;
        planeUnitX = planeMaxX.subtract(planeMinX, mathCont).divide(new BigDecimal(outputSize.width + 1, mathCont), mathCont); // Zero output size means the output has just one pixel.
        planeUnitY = planeMaxY.subtract(planeMinY, mathCont).divide(new BigDecimal(outputSize.height + 1, mathCont), mathCont); // Zero output size means the output has just one pixel.
        curRenRegXLabel.setText("X: " + planeMinX.round(mathContDisp).toEngineeringString() + " + " + planeMaxX.subtract(planeMinX, mathCont).round(mathContDisp).toEngineeringString());
        curRenRegYLabel.setText("Y: " + planeMinY.round(mathContDisp).toEngineeringString() + " + " + planeMaxY.subtract(planeMinY, mathCont).round(mathContDisp).toEngineeringString());
    }

    private void setSelRenRegion(BigDecimal selMinX, BigDecimal selMaxX, BigDecimal selMinY, BigDecimal selMaxY) {
        this.selMinX = selMinX;
        this.selMaxX = selMaxX;
        this.selMinY = selMinY;
        this.selMaxY = selMaxY;
        selRenRegXLabel.setText("X: " + selMinX.round(mathContDisp).toEngineeringString() + " + " + selMaxX.subtract(selMinX, mathCont).round(mathContDisp).toEngineeringString());
        selRenRegYLabel.setText("Y: " + selMinY.round(mathContDisp).toEngineeringString() + " + " + selMaxY.subtract(selMinY, mathCont).round(mathContDisp).toEngineeringString());
    }

    public void startRendering() {
        MandelbrotEngine.Parameters p;

        if (autoAdjustIterLimitCheckBox.isSelected()) {
            int limit;

            limit = Integer.parseInt(maxIterTextField.getText());
            if (stats.minIterations > 0.125 * limit) {
                maxIterTextField.setText(String.format("%d", (int) (stats.minIterations / 0.125 + 1)));
            } else if (stats.meanIterations > 0 && stats.meanIterations < 0.125 * limit) {
                maxIterTextField.setText(String.format("%d", (int) (stats.meanIterations * 8)));
            }
        }

        m.clearSelectionRectangle();
        setCurRenRegion(selMinX, selMaxX, selMinY, selMaxY);
        p = new MandelbrotEngine.Parameters(planeMinX, planeMaxX, planeMinY, planeMaxY,
                outputSize.width, outputSize.height, Integer.parseInt(maxIterTextField.getText()),
                arbPrecCheckBox.isSelected(), Integer.parseInt(arbPrecTextField.getText()),
                Integer.parseInt(sampleSizeTextField.getText()), getMandelbrotVariant((String) mbrotVarComboBox.getSelectedItem()), getColouringMethod((String) colMethComboBox
                        .getSelectedItem()));

        MandelbrotEngine.setParameters(p);
        renderInProgress = true;
        MandelbrotEngine.startRendering();
    }

    public void drawImage() {
        BufferedImage i;
        Graphics g;

        g = m.getImagePanelGraphics();
        i = MandelbrotEngine.getImage();
        if (i.getWidth() == outputSize.width && i.getHeight() == outputSize.height) {
            g.drawImage(i, 0, 0, null);
        } else {
            g.drawImage(i.getScaledInstance(outputSize.width, outputSize.height,
                    Image.SCALE_SMOOTH), 0, 0, null);
        }
    }
    
    public void redrawImage() {
        BufferedImage i;
        Graphics g;

        g = m.getImagePanelGraphics();
        i = MandelbrotEngine.getImage();
        if (i == null) {
            return;
        }
        m.clearSelectionRectangle();
        g.clearRect(0, 0, outputSize.width, outputSize.height);
        if (i.getWidth() == outputSize.width && i.getHeight() == outputSize.height) {
            g.drawImage(i, 0, 0, null);
        } else {
            g.drawImage(i.getScaledInstance(outputSize.width, outputSize.height,
                    Image.SCALE_SMOOTH), 0, 0, null);
        }

    }

    public void redrawImageRotated(double rotation) {
        BufferedImage i, temp;
        Graphics g;
        AffineTransform t;
        AffineTransformOp op;

        m.clearSelectionRectangle();
        i = MandelbrotEngine.getImage();
        if (i == null) {
            return;
        }

        imageRotation += rotation;
        g = m.getImagePanelGraphics();

        t = AffineTransform.getRotateInstance(imageRotation, (i.getWidth() - 1) / 2, (i.getHeight() - 1) / 2);
        op = new AffineTransformOp(t, AffineTransformOp.TYPE_BILINEAR);
        temp = op.filter(i, null);

        g.drawImage(temp, 0, 0, null);
    }

    public void zoom(Point p, double zoomFactor) {
        if (renderInProgress) {
            return;
        }
        BigDecimal x, y, sizeX, sizeY;
        double rX, rY, aspectRatio;

        rX = p.x / (double) outputSize.width;
        rY = p.y / (double) outputSize.height;
        aspectRatio = outputSize.width / (double) outputSize.height;

        sizeX = planeMaxX.subtract(planeMinX, mathCont).divide(new BigDecimal(zoomFactor, mathCont),
                mathCont);
        sizeY = planeMaxY.subtract(planeMinY, mathCont).divide(new BigDecimal(zoomFactor, mathCont),
                mathCont);
        if (sizeX.divide(sizeY, mathCont).compareTo(new BigDecimal(aspectRatio, mathCont)) > 0) {
            sizeY = sizeX.divide(new BigDecimal(aspectRatio, mathCont), mathCont);
        } else {
            sizeX = sizeY.multiply(new BigDecimal(aspectRatio, mathCont), mathCont);
        }

        x = planeMinX.add(planeUnitX.multiply(new BigDecimal(p.x, mathCont), mathCont), mathCont).subtract(sizeX.multiply(new BigDecimal(rX, mathCont), mathCont), mathCont);
        y = planeMaxY.subtract(planeUnitY.multiply(new BigDecimal(p.y, mathCont), mathCont), mathCont).subtract(sizeY.multiply(new BigDecimal(1 - rY, mathCont), mathCont), mathCont);

        setSelRenRegion(x, x.add(sizeX, mathCont), y, y.add(sizeY, mathCont));
        startRendering();
    }

    public void resetRenderingRegion() {
        setSelRenRegion(new BigDecimal("-2.0", mathCont), new BigDecimal("1.0", mathCont), new BigDecimal("-1.5", mathCont), new BigDecimal("1.5", mathCont));
    }

    private void plusChainRender() {
        MandelbrotEngine.Parameters p;

        if (plusChainPosition > plusChainLimit) {
            plusTask = false;
            return;
        }

        p = new MandelbrotEngine.Parameters(planeMinX, planeMaxX, planeMinY, planeMaxY,
                1024, 1024, 4000,
                arbPrecCheckBox.isSelected(), Integer.parseInt(arbPrecTextField.getText()),
                plusChainSampleSize, MandelbrotEngine.Parameters.MandelbrotVariant.BUDDHABROT, getColouringMethod((String) colMethComboBox
                        .getSelectedItem()));

        plusTask = renderInProgress = true;
        MandelbrotEngine.startRendering();
        plusChainPosition++;
        plusChainSampleSize += plusChainStep;
    }

    public void writeImageToFile(File f) {
        BufferedImage i;

        i = MandelbrotEngine.getImage();
        if (i == null) {
            return;
        }
        try {
            ImageIO.write(i, "png", f);
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void renderingBegun() {
        m.getProgressBar().setIndeterminate(true);
        m.getNotificationAreaLabel().setText("Rendering begun");
    }

    @Override
    public void regionRendered(Rectangle region) {
        m.getNotificationAreaLabel().setText("Region rendered: " + region);
    }

    @Override
    public void renderingEnded() {
        if (starTask) {
            try {
                try {
                    ImageIO.write(MandelbrotEngine.getImage(), "png", new File(new URI("file:///home/androkot/fractal.png")));
                    m.getProgressBar().setIndeterminate(false);
                    renderInProgress = starTask = false;
                    return;
                } catch (URISyntaxException ex) {
                    Logger.getLogger(MandelbrotSettingsPanel.class.getName()).log(Level.SEVERE, null, ex);
                }
            } catch (IOException ex) {
                Logger.getLogger(MandelbrotSettingsPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else if (plusTask) {
            try {
                try {
                    ImageIO.write(MandelbrotEngine.getImage(), "png", new File(new URI(String
                            .format("file:///home/androkot/Scratch/Fractilium/buddha/frames/%d.png",
                                    plusChainPosition - 1))));
                    m.getProgressBar().setIndeterminate(false);
                    renderInProgress = plusTask = false;
                    drawImage();
                    plusChainRender();
                    return;
                } catch (URISyntaxException ex) {
                    Logger.getLogger(MandelbrotSettingsPanel.class.getName()).log(Level.SEVERE, null, ex);
                }
            } catch (IOException ex) {
                Logger.getLogger(MandelbrotSettingsPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        drawImage();
        m.getProgressBar().setIndeterminate(false);
        m.getNotificationAreaLabel().setText("Rendered image");
        renderInProgress = false;
    }

    @Override
    public void errorOccurred() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void statsGenerated() {

        stats = MandelbrotEngine.getStatistics();

        sIterLabel.setText(String.format("min: %d avg: %.2f max: %d", stats.minIterations,
                stats.meanIterations, stats.maxIterations));
        sConvLabel.setText(String.format("conv: %d div: %d", stats.convergentPoints,
                outputSize.width * outputSize.height - stats.convergentPoints));
        sScaleLabel.setText(new BigDecimal(3, mathCont).divide(planeMaxX.subtract(planeMinX, mathCont),
                mathContDisp).toString() + "x");
        m.getNotificationAreaLabel().setText(String.format("Rendered in %.3f ms", stats.renderingTime));
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content om this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        curRenRegXLabel = new javax.swing.JLabel();
        curRenRegYLabel = new javax.swing.JLabel();
        starButton = new javax.swing.JButton();
        selRenRegYLabel = new javax.swing.JLabel();
        selRenRegXLabel = new javax.swing.JLabel();
        plusButton = new javax.swing.JButton();
        minusButton = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        maxIterTextField = new javax.swing.JTextField();
        arbPrecCheckBox = new javax.swing.JCheckBox();
        aaCheckBox = new javax.swing.JCheckBox();
        arbPrecTextField = new javax.swing.JTextField();
        arbPrecLabel = new javax.swing.JLabel();
        sampleSizeTextField = new javax.swing.JTextField();
        sampleSizeLabel = new javax.swing.JLabel();
        autoAdjustIterLimitCheckBox = new javax.swing.JCheckBox();
        jPanel3 = new javax.swing.JPanel();
        jLabel11 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        mbrotVarComboBox = new javax.swing.JComboBox();
        colMethComboBox = new javax.swing.JComboBox();
        jPanel4 = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        sSizeLabel = new javax.swing.JLabel();
        sIterLabel = new javax.swing.JLabel();
        sConvLabel = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        sScaleLabel = new javax.swing.JLabel();
        drawButton = new javax.swing.JButton();

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Rendering Region"));

        jLabel1.setText("Current");

        jLabel2.setText("Selection");

        curRenRegXLabel.setText("0 + 0");

        curRenRegYLabel.setText("0 + 0");

        starButton.setText("*");
        starButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                starButtonActionPerformed(evt);
            }
        });

        selRenRegYLabel.setText("0 + 0");

        selRenRegXLabel.setText("0 + 0");

        plusButton.setText("+");
        plusButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                plusButtonActionPerformed(evt);
            }
        });

        minusButton.setText("-");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(selRenRegYLabel))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(curRenRegXLabel, javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(curRenRegYLabel, javax.swing.GroupLayout.Alignment.TRAILING)))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(starButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(plusButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(minusButton)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(selRenRegXLabel)))))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(curRenRegXLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(curRenRegYLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(selRenRegXLabel)
                    .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(selRenRegYLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(starButton)
                    .addComponent(plusButton)
                    .addComponent(minusButton))
                .addContainerGap())
        );

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Renderer Settings"));

        jLabel5.setText("Max Iterations");

        maxIterTextField.setText("50");

        arbPrecCheckBox.setText("Arbitrary Precision");
        arbPrecCheckBox.setEnabled(false);
        arbPrecCheckBox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                arbPrecCheckBoxItemStateChanged(evt);
            }
        });

        aaCheckBox.setText("Anti-aliasing");
        aaCheckBox.setEnabled(false);
        aaCheckBox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                aaCheckBoxItemStateChanged(evt);
            }
        });

        arbPrecTextField.setText("200");
        arbPrecTextField.setEnabled(false);

        arbPrecLabel.setText("Bits");
        arbPrecLabel.setEnabled(false);

        sampleSizeTextField.setText("100000");

        sampleSizeLabel.setText("Sample Size");

        autoAdjustIterLimitCheckBox.setText("Auto");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(arbPrecCheckBox)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(arbPrecLabel))
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(aaCheckBox)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(sampleSizeLabel)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(arbPrecTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                                .addComponent(sampleSizeTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap())))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(autoAdjustIterLimitCheckBox)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(maxIterTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap())))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(maxIterTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(autoAdjustIterLimitCheckBox))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(aaCheckBox)
                    .addComponent(sampleSizeTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sampleSizeLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(arbPrecCheckBox)
                    .addComponent(arbPrecTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(arbPrecLabel)))
        );

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Algorithms"));

        jLabel11.setText("Variant");

        jLabel12.setText("Colouring Method");

        mbrotVarComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Regular", "Buddhabrot" }));

        colMethComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Regular", "Red", "Green", "Blue" }));

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel11)
                    .addComponent(jLabel12))
                .addGap(15, 15, 15)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(colMethComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(mbrotVarComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel11)
                    .addComponent(mbrotVarComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel12)
                    .addComponent(colMethComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder("Statistics"));

        jLabel6.setText("Iterations");
        jLabel6.setAlignmentX(0.5F);

        jLabel9.setText("Convergence");

        jLabel3.setText("Size");

        sSizeLabel.setFont(new java.awt.Font("Tahoma", 2, 11)); // NOI18N
        sSizeLabel.setText("0x0");
        sSizeLabel.setPreferredSize(new java.awt.Dimension(22, 14));

        sIterLabel.setFont(new java.awt.Font("Tahoma", 2, 11)); // NOI18N
        sIterLabel.setText("min: 0 avg: 0 max: 0");
        sIterLabel.setPreferredSize(new java.awt.Dimension(101, 14));

        sConvLabel.setFont(new java.awt.Font("Tahoma", 2, 11)); // NOI18N
        sConvLabel.setText("con: 0 div: 0");
        sConvLabel.setPreferredSize(new java.awt.Dimension(62, 14));

        jLabel4.setText("Scale");

        sScaleLabel.setFont(new java.awt.Font("Tahoma", 2, 11)); // NOI18N
        sScaleLabel.setText("0x");
        sScaleLabel.setPreferredSize(new java.awt.Dimension(33, 14));

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel6)
                            .addComponent(jLabel9)
                            .addComponent(sIterLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(sConvLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(sSizeLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(sScaleLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(sSizeLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sScaleLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel6)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(sIterLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel9)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(sConvLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        drawButton.setMnemonic('D');
        drawButton.setText("Render");
        drawButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                drawButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel4, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(drawButton)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(drawButton)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

	private void aaCheckBoxItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_aaCheckBoxItemStateChanged
            if (aaCheckBox.isSelected()) {
                sampleSizeLabel.setEnabled(true);
                sampleSizeTextField.setEnabled(true);
            } else {
                sampleSizeLabel.setEnabled(false);
                sampleSizeTextField.setEnabled(false);
            }
	}//GEN-LAST:event_aaCheckBoxItemStateChanged

	private void arbPrecCheckBoxItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_arbPrecCheckBoxItemStateChanged
            if (arbPrecCheckBox.isSelected()) {
                arbPrecLabel.setEnabled(true);
                arbPrecTextField.setEnabled(true);
            } else {
                arbPrecLabel.setEnabled(false);
                arbPrecTextField.setEnabled(false);
            }
	}//GEN-LAST:event_arbPrecCheckBoxItemStateChanged

    private void drawButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_drawButtonActionPerformed
        if (!renderInProgress) {
            startRendering();
        }
    }//GEN-LAST:event_drawButtonActionPerformed

    private void starButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_starButtonActionPerformed
        MandelbrotEngine.Parameters p;

        setCurRenRegion(selMinX, selMaxX, selMinY, selMaxY);
        p = new MandelbrotEngine.Parameters(planeMinX, planeMaxX, planeMinY, planeMaxY,
                2000, 2000, Integer.parseInt(maxIterTextField.getText()),
                arbPrecCheckBox.isSelected(), Integer.parseInt(arbPrecTextField.getText()),
                Integer.parseInt(sampleSizeTextField.getText()), getMandelbrotVariant((String) mbrotVarComboBox.getSelectedItem()), getColouringMethod((String) colMethComboBox
                        .getSelectedItem()));

        starTask = renderInProgress = true;
        MandelbrotEngine.startRendering();
    }//GEN-LAST:event_starButtonActionPerformed

    private void plusButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_plusButtonActionPerformed
        plusChainSampleSize = 513300;
        plusChainStep = 100;
        plusChainPosition = 7224;
        plusChainLimit = 100000;

        plusChainRender();
    }//GEN-LAST:event_plusButtonActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox aaCheckBox;
    private javax.swing.JCheckBox arbPrecCheckBox;
    private javax.swing.JLabel arbPrecLabel;
    private javax.swing.JTextField arbPrecTextField;
    private javax.swing.JCheckBox autoAdjustIterLimitCheckBox;
    private javax.swing.JComboBox colMethComboBox;
    private javax.swing.JLabel curRenRegXLabel;
    private javax.swing.JLabel curRenRegYLabel;
    private javax.swing.JButton drawButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JTextField maxIterTextField;
    private javax.swing.JComboBox mbrotVarComboBox;
    private javax.swing.JButton minusButton;
    private javax.swing.JButton plusButton;
    private javax.swing.JLabel sConvLabel;
    private javax.swing.JLabel sIterLabel;
    private javax.swing.JLabel sScaleLabel;
    private javax.swing.JLabel sSizeLabel;
    private javax.swing.JLabel sampleSizeLabel;
    private javax.swing.JTextField sampleSizeTextField;
    private javax.swing.JLabel selRenRegXLabel;
    private javax.swing.JLabel selRenRegYLabel;
    private javax.swing.JButton starButton;
    // End of variables declaration//GEN-END:variables
}

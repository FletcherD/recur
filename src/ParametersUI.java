import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;

/**
 * Created with IntelliJ IDEA.
 * User: fdostie
 * Date: 12/16/12
 * Time: 10:36 PM
 * To change this template use File | Settings | File Templates.
 */
public class ParametersUI implements ChangeListener {
    private Parameters uiParameters;
    private Recur.SharedParameterUpdate pUpdate;
    private boolean sliderListenerActive;

    private JSlider sliderScale;
    private JPanel jPanel;
    private JSlider sliderRotate;
    private JSlider sliderContrast;
    private JSlider sliderBrightness;
    private JPanel gammaPanel;
    private JSlider sliderGammaR;
    private JSlider sliderGammaG;
    private JSlider sliderGammaB;
    private JSlider sliderBokehRadius;
    private JSlider sliderUnsharpRadius;
    private JSlider sliderUnsharpWeight;
    private JSlider sliderNoise;

    private JEditorPane parametersField;
    private JButton importButton;
    private JButton exportButton;
    private JFormattedTextField fieldWidth;
    private JFormattedTextField fieldMatrixSize;
    private JFormattedTextField fieldHeight;
    private JCheckBox checkBoxNoise;
    private JFormattedTextField fieldCenterY;
    private JFormattedTextField fieldCenterX;
    private JFormattedTextField fieldRotation;
    private JFormattedTextField fieldScaleFactor;
    private JFormattedTextField fieldBrightnessR;
    private JFormattedTextField fieldBrightnessG;
    private JFormattedTextField fieldBrightnessB;
    private JFormattedTextField fieldContrastR;
    private JFormattedTextField fieldContrastG;
    private JFormattedTextField fieldContrastB;
    private JFormattedTextField fieldGammaR;
    private JFormattedTextField fieldGammaG;
    private JFormattedTextField fieldGammaB;
    private JFormattedTextField fieldFocalRadius;
    private JFormattedTextField fieldUnsharpRadius;
    private JFormattedTextField fieldUnsharpWeight;
    private JFormattedTextField fieldNoiseStdev;
    private JButton advancedApplyButton;
    private JTextPane clInfoPane;

    public ParametersUI(Recur.SharedParameterUpdate in, int mainWidth, int mainHeight) {
        pUpdate = in;
        uiParameters = new Parameters(in.parameters);
        sliderListenerActive = true;

        JFrame frame = new JFrame("Parameters");
        frame.setContentPane(jPanel);
        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        frame.pack();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setLocation(screenSize.width/2 + mainWidth/2 + 20, screenSize.height/2 - frame.getHeight()/2);
        frame.setVisible(true);

        setupFields();
        updateSliders();
        updateFields();

        sliderScale.addChangeListener(this);
        sliderRotate.addChangeListener(this);
        sliderContrast.addChangeListener(this);
        sliderBrightness.addChangeListener(this);
        sliderGammaR.addChangeListener(this);
        sliderGammaG.addChangeListener(this);
        sliderGammaB.addChangeListener(this);
        sliderBokehRadius.addChangeListener(this);
        sliderUnsharpRadius.addChangeListener(this);
        sliderUnsharpWeight.addChangeListener(this);

        sliderNoise.addChangeListener(this);
        exportButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    parametersField.setText(uiParameters.serialize());
                } catch (Exception err) {
                    System.out.println("Error while serializing parameters!! WTF?");
                }
            }
        });
        importButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    uiParameters.deserialize(parametersField.getText());
                } catch (Exception err) {
                    System.out.println("Invalid parameter string!");
                }
                updateSliders();
                updateFields();
                pUpdate.setUpdate(uiParameters);
            }
        });
        advancedApplyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fieldChanged();
            }
        });
        frame.requestFocus();
    }

    public void stateChanged(ChangeEvent e) {
        if(e.getSource() instanceof JSlider) {
            sliderChanged(e.getSource() == sliderBrightness, e.getSource() == sliderContrast);
        }
    }

    private void sliderChanged(boolean updateBrightness, boolean updateContrast) {
        if(!sliderListenerActive)
            return;
        uiParameters.gamma[0] = ((float) sliderGammaR.getValue()/1000.0f);
        uiParameters.gamma[1] = ((float) sliderGammaG.getValue()/1000.0f);
        uiParameters.gamma[2] = ((float) sliderGammaB.getValue()/1000.0f);
        if(updateBrightness) {
            uiParameters.brightness[0] = ((float) sliderBrightness.getValue()/1000.0f);
            uiParameters.brightness[1] = ((float) sliderBrightness.getValue()/1000.0f);
            uiParameters.brightness[2] = ((float) sliderBrightness.getValue()/1000.0f);
        }
        if(updateContrast) {
            uiParameters.contrast[0] = ((float) sliderContrast.getValue()/1000.0f);
            uiParameters.contrast[1] = ((float) sliderContrast.getValue()/1000.0f);
            uiParameters.contrast[2] = ((float) sliderContrast.getValue()/1000.0f);
        }
        uiParameters.rotateAngle = (Math.PI * (double) sliderRotate.getValue()/500.0);
        uiParameters.scaleFactor = ((double) sliderScale.getValue() / 100.0);
        uiParameters.unsharpWeight = ((float) sliderUnsharpWeight.getValue()/100.0f);
        uiParameters.unsharpRadius = ((float) sliderUnsharpRadius.getValue() / 100.0f);
        uiParameters.blurRadius = ((float) sliderBokehRadius.getValue() / 100.0f);
        uiParameters.noiseStd = (float) Math.pow(10, (double) sliderNoise.getValue() / 100.0);

        updateFields();
        pUpdate.setUpdate(uiParameters);
    }

    private void fieldChanged() {
        uiParameters.contrast[0] = ((Number)fieldContrastR.getValue()).floatValue();
        uiParameters.contrast[1] = ((Number)fieldContrastG.getValue()).floatValue();
        uiParameters.contrast[2] = ((Number)fieldContrastB.getValue()).floatValue();
        uiParameters.brightness[0] = ((Number)fieldBrightnessR.getValue()).floatValue();
        uiParameters.brightness[1] = ((Number)fieldBrightnessG.getValue()).floatValue();
        uiParameters.brightness[2] = ((Number)fieldBrightnessB.getValue()).floatValue();
        uiParameters.gamma[0] = ((Number)fieldGammaR.getValue()).floatValue();
        uiParameters.gamma[1] = ((Number)fieldGammaG.getValue()).floatValue();
        uiParameters.gamma[2] = ((Number)fieldGammaB.getValue()).floatValue();
        uiParameters.rotateAngle = ((Number)fieldRotation.getValue()).doubleValue();
        uiParameters.scaleFactor = ((Number)fieldScaleFactor.getValue()).doubleValue();
        uiParameters.unsharpRadius = ((Number)fieldUnsharpRadius.getValue()).floatValue();
        uiParameters.blurRadius = ((Number)fieldFocalRadius.getValue()).floatValue();
        uiParameters.unsharpWeight = ((Number)fieldUnsharpWeight.getValue()).floatValue();
        uiParameters.noiseStd = ((Number)fieldNoiseStdev.getValue()).floatValue();
        uiParameters.center[0] = ((Number)fieldCenterX.getValue()).floatValue();
        uiParameters.center[1] = ((Number)fieldCenterY.getValue()).floatValue();

        uiParameters.noiseOn = checkBoxNoise.isSelected();
        uiParameters.matrixSize = ((Number)fieldMatrixSize.getValue()).intValue();
        uiParameters.width = ((Number)fieldWidth.getValue()).intValue();
        uiParameters.height = ((Number)fieldHeight.getValue()).intValue();

        sliderListenerActive = false;
        updateSliders();
        sliderListenerActive = true;
        pUpdate.setUpdate(uiParameters);
    }

    public void updateClInfo(Recur.SharedParameterUpdate.ClInformation clInfo) {
        if(clInfo.update) {
            String info = "";
            info += "FPS: " + String.format("%.1f", clInfo.fps) + "\n\n";
            info += "OpenCL Device: " + clInfo.device + "\n";
            info += "OpenCL Version: " + clInfo.version + "\n";
            info += "OpenCL Device Memory Size: " + clInfo.memSize/(1024*1024) + " MB\n";
            info += "OpenCL Device Clock Frequency: " + clInfo.frequency + " MHz\n";
            info += "OpenCL Device Compute Units: " + clInfo.computeUnits + "\n";
            clInfoPane.setText(info);
            clInfo.update = false;
        }
    }

    private void updateSliders() {
        sliderRotate.setValue((int)(500.0*uiParameters.rotateAngle/Math.PI));
        sliderScale.setValue((int)(100*uiParameters.scaleFactor));
        sliderContrast.setValue((int)(1000*uiParameters.contrast[0]));
        sliderBrightness.setValue((int)(1000*uiParameters.brightness[0]));
        sliderGammaR.setValue((int)(1000*uiParameters.gamma[0]));
        sliderGammaG.setValue((int)(1000*uiParameters.gamma[1]));
        sliderGammaB.setValue((int)(1000*uiParameters.gamma[2]));
        sliderBokehRadius.setValue((int) (100 * uiParameters.blurRadius));
        sliderUnsharpRadius.setValue((int) (100 * uiParameters.unsharpRadius));
        sliderUnsharpWeight.setValue((int) (100 * uiParameters.unsharpWeight));
        sliderNoise.setValue((int) (Math.log10(uiParameters.noiseStd) * 100.0));
    }

    private void updateFields() {
        fieldWidth.setValue(uiParameters.width);
        fieldHeight.setValue(uiParameters.height);
        fieldMatrixSize.setValue(uiParameters.matrixSize);
        checkBoxNoise.setSelected(uiParameters.noiseOn);
        fieldNoiseStdev.setEnabled(uiParameters.noiseOn);

        fieldCenterX.setValue(uiParameters.center[0]);
        fieldCenterY.setValue(uiParameters.center[1]);

        fieldRotation.setValue(uiParameters.rotateAngle);
        fieldScaleFactor.setValue(uiParameters.scaleFactor);

        fieldBrightnessR.setValue(uiParameters.brightness[0]);
        fieldBrightnessG.setValue(uiParameters.brightness[1]);
        fieldBrightnessB.setValue(uiParameters.brightness[2]);

        fieldContrastR.setValue(uiParameters.contrast[0]);
        fieldContrastG.setValue(uiParameters.contrast[1]);
        fieldContrastB.setValue(uiParameters.contrast[2]);

        fieldGammaR.setValue(uiParameters.gamma[0]);
        fieldGammaG.setValue(uiParameters.gamma[1]);
        fieldGammaB.setValue(uiParameters.gamma[2]);

        fieldFocalRadius.setValue(uiParameters.blurRadius);
        fieldUnsharpRadius.setValue(uiParameters.unsharpRadius);
        fieldUnsharpWeight.setValue(uiParameters.unsharpWeight);
        fieldNoiseStdev.setValue(uiParameters.noiseStd);
    }

    private void setupFields() {
        DecimalFormat decimalFormat = new DecimalFormat("0.000");
        DecimalFormat integerFormat = new DecimalFormat("0");
        DecimalFormat scientificFormat = new DecimalFormat("0.0E0");
        NumberFormatter scientificFormatter = new NumberFormatter(scientificFormat);
        scientificFormatter.setOverwriteMode(true);
        DefaultFormatterFactory decimalFormatterFactory = new DefaultFormatterFactory(new NumberFormatter(decimalFormat));
        DefaultFormatterFactory integerFormatterFactory = new DefaultFormatterFactory(new NumberFormatter(integerFormat));
        fieldNoiseStdev.setFormatterFactory(new DefaultFormatterFactory(scientificFormatter));
        fieldBrightnessR.setFormatterFactory(decimalFormatterFactory);
        fieldBrightnessG.setFormatterFactory(decimalFormatterFactory);
        fieldBrightnessB.setFormatterFactory(decimalFormatterFactory);
        fieldContrastR.setFormatterFactory(decimalFormatterFactory);
        fieldContrastG.setFormatterFactory(decimalFormatterFactory);
        fieldContrastB.setFormatterFactory(decimalFormatterFactory);
        fieldGammaR.setFormatterFactory(decimalFormatterFactory);
        fieldGammaG.setFormatterFactory(decimalFormatterFactory);
        fieldGammaB.setFormatterFactory(decimalFormatterFactory);
        fieldMatrixSize.setFormatterFactory(integerFormatterFactory);
        fieldWidth.setFormatterFactory(integerFormatterFactory);
        fieldHeight.setFormatterFactory(integerFormatterFactory);
        fieldCenterX.setFormatterFactory(decimalFormatterFactory);
        fieldCenterY.setFormatterFactory(decimalFormatterFactory);
        fieldFocalRadius.setFormatterFactory(decimalFormatterFactory);
        fieldUnsharpRadius.setFormatterFactory(decimalFormatterFactory);
        fieldUnsharpWeight.setFormatterFactory(decimalFormatterFactory);
        fieldRotation.setFormatterFactory(decimalFormatterFactory);
        fieldScaleFactor.setFormatterFactory(decimalFormatterFactory);
    }
}

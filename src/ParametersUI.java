import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created with IntelliJ IDEA.
 * User: fdostie
 * Date: 12/16/12
 * Time: 10:36 PM
 * To change this template use File | Settings | File Templates.
 */
public class ParametersUI {
    private Parameters uiParameters;
    private Recur.SharedParameterUpdate pUpdate;
    private JSlider sliderScale;
    private JPanel jPanel;
    private JSlider sliderRotate;
    private JSlider sliderContrastR;
    private JSlider sliderContrastG;
    private JSlider sliderContrastB;
    private JSlider sliderBrightnessR;
    private JSlider sliderBokehRadius;
    private JSlider sliderUnsharpRadius;
    private JSlider sliderUnsharpWeight;
    private JSlider sliderNoise;
    private JEditorPane parametersField;
    private JButton importButton;
    private JButton exportButton;
    private JSlider sliderBrightnessG;
    private JSlider sliderBrightnessB;
    private JSlider sliderGammaR;
    private JSlider sliderGammaG;
    private JSlider sliderGammaB;
    private JPanel gammaPanel;
    private JFormattedTextField formattedTextFieldScale;

    public ParametersUI(Recur.SharedParameterUpdate in, int mainWidth, int mainHeight) {
        pUpdate = in;
        uiParameters = new Parameters(in.parameters);

        JFrame frame = new JFrame("Parameters");
        frame.setContentPane(jPanel);
        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        frame.pack();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setLocation(screenSize.width/2 + mainWidth/2 + 20, screenSize.height/2 - frame.getHeight()/2);
        frame.setVisible(true);

        updateSliders();

        sliderScale.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                uiParameters.scaleFactor = ((double) sliderScale.getValue() / 100.0);
                pUpdate.setUpdate(uiParameters);
            }
        });

        sliderRotate.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                uiParameters.rotateAngle = (Math.PI * (double) sliderRotate.getValue()/500.0);
                pUpdate.setUpdate(uiParameters);
            }
        });

        sliderContrastR.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                updateContrast();
            }
        });
        sliderContrastG.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                updateContrast();
            }
        });
        sliderContrastB.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                updateContrast();
            }
        });

        sliderBrightnessR.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                updateBrightness();
            }
        });
        sliderBrightnessG.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                updateBrightness();
            }
        });
        sliderBrightnessB.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                updateBrightness();
            }
        });

        sliderGammaR.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                updateGamma();
            }
        });
        sliderGammaG.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                updateGamma();
            }
        });
        sliderGammaB.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                updateGamma();
            }
        });

        sliderBrightnessR.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                float contrast = ((float) sliderBrightnessR.getValue()/1000.0f);
                for(int i=0; i<uiParameters.brightness.length; i++) {
                    uiParameters.brightness[i] = contrast;
                }
                pUpdate.setUpdate(uiParameters);
            }
        });

        sliderBokehRadius.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                uiParameters.blurRadius = ((float) sliderBokehRadius.getValue() / 100.0f);
                pUpdate.setUpdate(uiParameters);
            }
        });

        sliderUnsharpRadius.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                uiParameters.unsharpRadius = ((float) sliderUnsharpRadius.getValue() / 100.0f);
                pUpdate.setUpdate(uiParameters);
            }
        });

        sliderUnsharpWeight.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                uiParameters.unsharpWeight = ((float) sliderUnsharpWeight.getValue()/100.0f);
                pUpdate.setUpdate(uiParameters);
            }
        });

        sliderNoise.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                uiParameters.noiseStd = (float) Math.pow(10, (double) sliderNoise.getValue() / 100.0);
                pUpdate.setUpdate(uiParameters);
            }
        });
        exportButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                parametersField.setText(uiParameters.serialize());
            }
        });
        importButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                uiParameters.deserialize(parametersField.getText());
                updateSliders();
                pUpdate.setUpdate(uiParameters);
            }
        });
    }

    private void updateContrast() {
        uiParameters.contrast[0] = ((float) sliderContrastR.getValue()/1000.0f);
        uiParameters.contrast[1] = ((float) sliderContrastG.getValue()/1000.0f);
        uiParameters.contrast[2] = ((float) sliderContrastB.getValue()/1000.0f);
        pUpdate.setUpdate(uiParameters);
    }
    private void updateBrightness() {
        uiParameters.brightness[0] = ((float) sliderBrightnessR.getValue()/1000.0f);
        uiParameters.brightness[1] = ((float) sliderBrightnessG.getValue()/1000.0f);
        uiParameters.brightness[2] = ((float) sliderBrightnessB.getValue()/1000.0f);
        pUpdate.setUpdate(uiParameters);
    }
    private void updateGamma() {
        uiParameters.gamma[0] = ((float) sliderGammaR.getValue()/1000.0f);
        uiParameters.gamma[1] = ((float) sliderGammaG.getValue()/1000.0f);
        uiParameters.gamma[2] = ((float) sliderGammaB.getValue()/1000.0f);
        pUpdate.setUpdate(uiParameters);
    }

    private void updateSliders() {
        sliderRotate.setValue((int)(500.0*uiParameters.rotateAngle/Math.PI));
        sliderScale.setValue((int)(100*uiParameters.scaleFactor));

        sliderContrastR.setValue((int)(1000*uiParameters.contrast[0]));
        sliderContrastG.setValue((int)(1000*uiParameters.contrast[1]));
        sliderContrastB.setValue((int)(1000*uiParameters.contrast[2]));

        sliderBrightnessR.setValue((int)(1000*uiParameters.brightness[0]));
        sliderBrightnessG.setValue((int)(1000*uiParameters.brightness[1]));
        sliderBrightnessB.setValue((int)(1000*uiParameters.brightness[2]));

        sliderGammaR.setValue((int)(1000*uiParameters.gamma[0]));
        sliderGammaG.setValue((int)(1000*uiParameters.gamma[1]));
        sliderGammaB.setValue((int)(1000*uiParameters.gamma[2]));

        sliderBrightnessR.setValue((int) (1000 * uiParameters.brightness[0]));
        sliderBokehRadius.setValue((int) (100 * uiParameters.blurRadius));
        sliderUnsharpRadius.setValue((int) (100 * uiParameters.unsharpRadius));
        sliderUnsharpWeight.setValue((int) (100 * uiParameters.unsharpWeight));
        sliderNoise.setValue((int) (Math.log10(uiParameters.noiseStd) * 100.0));
    }
}

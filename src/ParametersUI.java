import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.NumberFormat;

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
    private JSlider sliderContrast;
    private JSlider sliderBrightness;
    private JSlider sliderBokehR;
    private JSlider sliderUnsharpR;
    private JSlider sliderUnsharpWeight;
    private JSlider sliderNoise;
    private JSlider sliderGamma;
    private JFormattedTextField formattedTextFieldScale;

    public ParametersUI(Recur.SharedParameterUpdate in, int mainWidth, int mainHeight) {
        pUpdate = in;
        uiParameters = new Parameters(in.parameters);

        JFrame frame = new JFrame("Parameters");
        frame.setContentPane(jPanel);
        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        frame.pack();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setLocation(screenSize.width/2 + mainWidth/2+5, screenSize.height/2 - frame.getHeight()/2);
        frame.setVisible(true);

        sliderScale.setValue((int)(100*uiParameters.scaleFactor));
        sliderScale.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                uiParameters.scaleFactor = ((double) sliderScale.getValue() / 100.0);
                pUpdate.setUpdate(uiParameters);
            }
        });

        sliderRotate.setValue((int)(500.0*uiParameters.rotateAngle/Math.PI));
        sliderRotate.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                uiParameters.rotateAngle = (Math.PI * (double) sliderRotate.getValue()/500.0);
                pUpdate.setUpdate(uiParameters);
            }
        });

        sliderContrast.setValue((int)(1000*uiParameters.contrast[0]));
        sliderContrast.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                float contrast = ((float) sliderContrast.getValue()/1000.0f);
                for(int i=0; i<uiParameters.contrast.length; i++) {
                    uiParameters.contrast[i] = contrast;
                }
                pUpdate.setUpdate(uiParameters);
            }
        });

        sliderBrightness.setValue((int) (1000 * uiParameters.brightness[0]));
        sliderBrightness.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                float contrast = ((float)sliderBrightness.getValue()/1000.0f);
                for(int i=0; i<uiParameters.brightness.length; i++) {
                    uiParameters.brightness[i] = contrast;
                }
                pUpdate.setUpdate(uiParameters);
            }
        });

        sliderBokehR.setValue((int) (100 * uiParameters.blurRadius));
        sliderBokehR.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                uiParameters.blurRadius = ((float) sliderBokehR.getValue()/100.0f);
                pUpdate.setUpdate(uiParameters);
            }
        });

        sliderUnsharpR.setValue((int) (100 * uiParameters.unsharpRadius));
        sliderUnsharpR.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                uiParameters.unsharpRadius = ((float) sliderUnsharpR.getValue()/100.0f);
                pUpdate.setUpdate(uiParameters);
            }
        });

        sliderUnsharpWeight.setValue((int) (100 * uiParameters.unsharpWeight));
        sliderUnsharpWeight.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                uiParameters.unsharpWeight = ((float) sliderUnsharpWeight.getValue()/100.0f);
                pUpdate.setUpdate(uiParameters);
            }
        });

        sliderNoise.setValue((int) (Math.log10(uiParameters.noiseStd) * 100.0));
        sliderNoise.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                uiParameters.noiseStd = (float)Math.pow(10, (double)sliderNoise.getValue()/100.0);
                pUpdate.setUpdate(uiParameters);
            }
        });
    }
}

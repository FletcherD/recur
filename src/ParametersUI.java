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
    private JFormattedTextField formattedTextFieldScale;

    public ParametersUI(Recur.SharedParameterUpdate in) {
        pUpdate = in;
        uiParameters = new Parameters();

        JFrame frame = new JFrame("Parameters");
        frame.setContentPane(jPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setLocation(screenSize.width/2 + uiParameters.width/2+5, screenSize.height/2 - frame.getHeight()/2);
        frame.setVisible(true);

        sliderScale.setValue((int)(100*uiParameters.scaleFactor));
        sliderScale.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                uiParameters.scaleFactor = ((double) sliderScale.getValue() / 100.0);
                pUpdate.setUpdate(uiParameters);
            }
        });

        sliderRotate.setValue((int)(100*uiParameters.rotateAngle));
        sliderRotate.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                uiParameters.rotateAngle = ((double) sliderRotate.getValue()/100.0);
                pUpdate.setUpdate(uiParameters);
            }
        });

        sliderContrast.setValue((int)(1000*uiParameters.contrast[0]));
        sliderContrast.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                double contrast = ((double) sliderContrast.getValue()/1000.0);
                for(int i=0; i<uiParameters.contrast.length; i++) {
                    uiParameters.contrast[i] = contrast;
                }
                pUpdate.setUpdate(uiParameters);
            }
        });

        sliderBrightness.setValue((int) (100 * uiParameters.brightness[0]));
        sliderBrightness.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                double contrast = ((double) sliderBrightness.getValue()/100.0);
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
                uiParameters.blurRadius = ((double) sliderBokehR.getValue()/100.0);
                pUpdate.setUpdate(uiParameters);
            }
        });

        sliderUnsharpR.setValue((int) (100 * uiParameters.unsharpRadius));
        sliderUnsharpR.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                uiParameters.unsharpRadius = ((double) sliderUnsharpR.getValue()/100.0);
                pUpdate.setUpdate(uiParameters);
            }
        });

        sliderUnsharpWeight.setValue((int) (100 * uiParameters.unsharpWeight));
        sliderUnsharpWeight.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                uiParameters.unsharpWeight = ((double) sliderUnsharpWeight.getValue()/100.0);
                pUpdate.setUpdate(uiParameters);
            }
        });

        sliderNoise.setValue((int) (Math.log10(uiParameters.noiseStd) * 100.0));
        sliderNoise.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                uiParameters.noiseStd = Math.pow(10, (double)sliderNoise.getValue()/100.0);
                pUpdate.setUpdate(uiParameters);
            }
        });
    }
}

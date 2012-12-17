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

        sliderContrast.setValue((int)(100*uiParameters.brightness[0]));
        sliderRotate.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                double contrast = ((double) sliderContrast.getValue()/100.0);
                for(int i=0; i<uiParameters.contrast.length; i++) {
                    uiParameters.contrast[i] = contrast;
                }
                pUpdate.setUpdate(uiParameters);
            }
        });

        sliderBrightness.setValue((int) (100 * uiParameters.brightness[0]));
        sliderRotate.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                double contrast = ((double) sliderBrightness.getValue()/100.0);
                for(int i=0; i<uiParameters.brightness.length; i++) {
                    uiParameters.brightness[i] = contrast;
                }
                pUpdate.setUpdate(uiParameters);
            }
        });
    }
}

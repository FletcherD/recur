/**
 * Created with IntelliJ IDEA.
 * User: fletcher
 * Date: 8/19/12
 * Time: 4:42 PM
 * To change this template use File | Settings | File Templates.
 */

public class Parameters {
    int width;
    int height;
    int pixelNum;

    double rotateAngle;
    double scaleFactor;

    int matrixSize;
    double blurRadius;
    double unsharpRadius;
    double unsharpWeight;

    double contrast[];
    double brightness[];
    double gamma;

    double borderColor[];

    boolean noiseOn;
    double noiseStd;

    float[] debugMatrix = null;

    boolean updated = false;
    Parameters lastParameters;

    Parameters() {
        width = 300;
        height = 300;
        pixelNum = width*height;
        rotateAngle = Math.PI * (1.0/5.0);
        scaleFactor = 1.75;
        matrixSize = 5;
        blurRadius = 0.5;
        unsharpRadius = 1.0;
        unsharpWeight = 1.0;
        contrast = new double[]{1.0, 1.0, 1.0};
        brightness = new double[]{0.0, 0.0, 0.0};
        borderColor = new double[]{0.05, 0.03, 0.03};
        gamma = 3.0;
        noiseOn = true;
        noiseStd = 0.001;
    }

    Parameters(Parameters in){

        width = in.width;
        height = in.height;
        pixelNum = in.pixelNum;
        rotateAngle = in.rotateAngle;
        scaleFactor = in.scaleFactor;
        matrixSize = in.matrixSize;
        blurRadius = in.blurRadius;
        unsharpRadius = in.unsharpRadius;
        unsharpWeight = in.unsharpWeight;
        contrast = in.contrast;
        brightness = in.brightness;
        borderColor = in.borderColor;
        gamma = in.gamma;
        noiseOn = in.noiseOn;
        noiseStd = in.noiseStd;
    }

    public String arrayFormatC(double in[]) {
        String s = "{";
        for(int i = 0; i < in.length; i++) {
            s = s + Double.toString(in[i]);
            if(i != in.length-1) {
                s = s + ", ";
            }
        }
        s = s + "}";
        return s;
    }

    public double[] getBorderColorGamma() {
        double[] r = new double[borderColor.length];
        for(int i = 0; i < borderColor.length; i++) {
            r[i] = Math.pow(borderColor[i], gamma);
        }
        return r;
    }

    public synchronized float[] getDebugMatrix() {
        while(debugMatrix == null) {
            try {
                wait();
            } catch (InterruptedException e) {

            }
        }
        return debugMatrix;
    }

    public synchronized void setDebugMatrix(float[] in) {
        debugMatrix = in;
        notifyAll();
    }
}

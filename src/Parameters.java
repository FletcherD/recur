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

    Parameters() {
        width = 500;
        height = 500;
        pixelNum = width*height;
        rotateAngle = Math.PI * (1.0/5.0);
        scaleFactor = 1.99;
        matrixSize = 5;
        blurRadius = 0.5;
        unsharpRadius = 1.0;
        unsharpWeight = 1.0;
        contrast = new double[]{1.0, 1.0, 1.0, 1.0};
        brightness = new double[]{0.0, 0.0, 0.0, 0.0};
        borderColor = new double[]{0.05, 0.03, 0.03, 0.0};
        gamma = 3.0;
        noiseOn = true;
        noiseStd = 0.001;
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
        double[] r = new double[4];
        for(int i = 0; i < 4; i++) {
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

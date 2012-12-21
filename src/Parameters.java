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
    float[] center;

    double rotateAngle;
    double scaleFactor;

    int matrixSize;
    float blurRadius;
    float unsharpRadius;
    float unsharpWeight;

    float contrast[];
    float brightness[];
    float gamma[];

    float borderColor[];

    boolean noiseOn;
    float noiseStd;

    float[] debugMatrix = null;

    Parameters() {
        width = 400;
        height = 400;
        pixelNum = width*height;
        center = new float[]{(float)(Math.floor(width/2.0)+0.25), (float)(Math.floor(height/2.0)+0.25)};
        rotateAngle = Math.PI * (1.0/6.0);
        scaleFactor = 1.75;
        matrixSize = 5;
        blurRadius = 0.5f;
        unsharpRadius = 1.0f;
        unsharpWeight = 1.0f;
        contrast = new float[]{1.0f, 1.0f, 1.0f};
        brightness = new float[]{0.0f, 0.0f, 0.0f};
        borderColor = new float[]{0.05f, 0.03f, 0.03f};
        gamma = new float[]{2.0f, 3.0f, 1.0f};
        noiseOn = true;
        noiseStd = 0.001f;
    }

    Parameters(Parameters in){

        width = in.width;
        height = in.height;
        pixelNum = in.pixelNum;
        center = in.center.clone();
        rotateAngle = in.rotateAngle;
        scaleFactor = in.scaleFactor;
        matrixSize = in.matrixSize;
        blurRadius = in.blurRadius;
        unsharpRadius = in.unsharpRadius;
        unsharpWeight = in.unsharpWeight;
        contrast = in.contrast.clone();
        brightness = in.brightness.clone();
        borderColor = in.borderColor.clone();
        gamma = in.gamma.clone();
        noiseOn = in.noiseOn;
        noiseStd = in.noiseStd;
    }

    public String arrayFormatC(float in[]) {
        String s = "{";
        for(int i = 0; i < in.length; i++) {
            s = s + Float.toString(in[i]);
            if(i != in.length-1) {
                s = s + ", ";
            }
        }
        s = s + "}";
        return s;
    }

    public float[] getBorderColorGamma() {
        float[] r = new float[borderColor.length];
        for(int i = 0; i < borderColor.length; i++) {
            r[i] = (float)Math.pow(borderColor[i], gamma[i]);
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

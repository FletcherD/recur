import java.text.DecimalFormat;

/**
 * Created with IntelliJ IDEA.
 * User: fletcher
 * Date: 8/19/12
 * Time: 4:42 PM
 * To change this template use File | Settings | File Templates.
 */

public class Parameters
{
    transient int width;
    transient int height;
    float[] center;
    transient int fpsLimit;

    double rotateAngle;
    double scaleFactor;

    transient int matrixSize;
    float blurRadius;
    float unsharpRadius;
    float unsharpWeight;

    float contrast[];
    float brightness[];
    float gamma[];

    transient float borderColor[];

    transient boolean noiseOn;
    float noiseStd;

    transient float[] debugMatrix = null;

    Parameters() {
        width = 300;
        height = 300;
        center = new float[]{(float)(Math.floor(width/2.0)+0.25), (float)(Math.floor(height/2.0)+0.25)};
        fpsLimit = 60;
        rotateAngle = (1.0/5.0);
        scaleFactor = 1.15;
        matrixSize = 5;
        blurRadius = 0.5f;
        unsharpRadius = 1.0f;
        unsharpWeight = 0.5f;
        contrast = new float[]{1.0f, 1.0f, 1.0f};
        brightness = new float[]{0.0f, 0.0f, 0.0f};
        borderColor = new float[]{0.05f, 0.03f, 0.03f};
        gamma = new float[]{2.0f, 1.8f, 2.2f};
        noiseOn = true;
        noiseStd = 0.001f;
    }

    Parameters(Parameters in){
        clone(in);
    }

    public int pixelNum() {
        return width * height;
    }

    public void clone(Parameters in){
        width = in.width;
        height = in.height;
        center = in.center.clone();
        fpsLimit = in.fpsLimit;
        rotateAngle = in.rotateAngle;
        scaleFactor = in.scaleFactor;
        matrixSize = in.matrixSize;
        blurRadius = in.blurRadius;
        unsharpRadius = in.unsharpRadius;
        unsharpWeight = in.unsharpWeight;
        contrast = in.contrast.clone();
        brightness = in.brightness.clone();
        if(in.borderColor != null)
            borderColor = in.borderColor.clone();
        gamma = in.gamma.clone();
        noiseOn = in.noiseOn;
        noiseStd = in.noiseStd;
    }

    public void partialClone(Parameters in){
        center = in.center.clone();
        rotateAngle = in.rotateAngle;
        scaleFactor = in.scaleFactor;
        blurRadius = in.blurRadius;
        unsharpRadius = in.unsharpRadius;
        unsharpWeight = in.unsharpWeight;
        contrast = in.contrast.clone();
        brightness = in.brightness.clone();
        gamma = in.gamma.clone();
        noiseStd = in.noiseStd;
    }

    public String serialize() {
        String out = "";
        DecimalFormat fMat = new DecimalFormat("0.######");
        out += String.format("%s %s ", fMat.format(center[0]), fMat.format(center[1]));
        out += String.format("%s %s ", fMat.format(rotateAngle), fMat.format(scaleFactor));
        out += String.format("%s %s %s ", fMat.format(blurRadius), fMat.format(unsharpRadius), fMat.format(unsharpWeight));
        out += String.format("%s %s %s ", fMat.format(contrast[0]), fMat.format(contrast[1]), fMat.format(contrast[2]));
        out += String.format("%s %s %s ", fMat.format(brightness[0]), fMat.format(brightness[1]), fMat.format(brightness[2]));
        out += String.format("%s %s %s ", fMat.format(gamma[0]), fMat.format(gamma[1]), fMat.format(gamma[2]));
        DecimalFormat scifMat = new DecimalFormat("0.00E0");
        out += String.format("%s ", scifMat.format(noiseStd));
        return out;
    }

    public void deserialize(String in) {
        class FieldReader {
            String[] fields;
            int idx = 0;
            FieldReader(String[] in) { fields = in; }
            public String read() throws Exception{
                return fields[idx++];
            }
        }
        FieldReader reader = new FieldReader(in.split(" "));
        try {
            center[0] = Float.parseFloat(reader.read()); center[1] = Float.parseFloat(reader.read());
            rotateAngle = Float.parseFloat(reader.read()); scaleFactor = Float.parseFloat(reader.read());
            blurRadius = Float.parseFloat(reader.read());
            unsharpRadius = Float.parseFloat(reader.read());
            unsharpWeight = Float.parseFloat(reader.read());
            contrast[0] = Float.parseFloat(reader.read()); contrast[1] = Float.parseFloat(reader.read()); contrast[2] = Float.parseFloat(reader.read());
            brightness[0] = Float.parseFloat(reader.read()); brightness[1] = Float.parseFloat(reader.read()); brightness[2] = Float.parseFloat(reader.read());
            gamma[0] = Float.parseFloat(reader.read()); gamma[1] = Float.parseFloat(reader.read()); gamma[2] = Float.parseFloat(reader.read());
            noiseStd = Float.parseFloat(reader.read());
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
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

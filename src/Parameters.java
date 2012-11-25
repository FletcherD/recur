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

    double colorContrast[];
    double brightness;

    boolean noiseOn;
    double noiseStd;

    Parameters() {
        width = 500;
        height = 500;
        pixelNum = width*height;
        rotateAngle = 2.0/5.0 * Math.PI;
        scaleFactor = 1.01;
        matrixSize = 3;
        blurRadius = 0.8;
        unsharpRadius = 1.2;
        unsharpWeight = 0.9;
        colorContrast = new double[]{1.0, 1.0, 1.0};
        brightness = 0.0;
        noiseOn = true;
        noiseStd = 0.01;
    }
}

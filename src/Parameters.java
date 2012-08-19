/**
 * Created with IntelliJ IDEA.
 * User: fletcher
 * Date: 8/19/12
 * Time: 4:42 PM
 * To change this template use File | Settings | File Templates.
 */
import java.math.*;

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
    double noiseWeight;

    int randomDataSize;

    Parameters() {
        width = 500;
        height = 500;
        pixelNum = width*height;
        rotateAngle = Math.PI / 6.0;
        scaleFactor = 1.05;
        matrixSize = 3;
        blurRadius = 1.0;
        unsharpRadius = 1.5;
        unsharpWeight = 1.0;
        colorContrast = new double[]{1.0, 1.0, 1.0};
        brightness = 0.0;
        noiseOn = false;
        randomDataSize = 2 << 12;
    }
}

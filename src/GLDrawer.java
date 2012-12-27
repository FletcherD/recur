import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.*;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.Util.checkGLError;

/**
 * Created with IntelliJ IDEA.
 * User: fdostie
 * Date: 11/22/12
 * Time: 2:56 AM
 * To change this template use File | Settings | File Templates.
 */
    public class GLDrawer implements Runnable {
    ImageData imageData;
    int textureID;
    boolean mouseStatus[] = { false, false };
    boolean keyStatus = false;
    boolean runToggle = true;
    Recur.SharedGlData sharedGlData;

    Parameters parameters;

    public GLDrawer(ImageData inImageData, Recur.SharedParameterUpdate inParameters, Recur.SharedGlData inShared) throws LWJGLException {
        parameters = inParameters.parameters;
        imageData = inImageData;
        sharedGlData = inShared;
    }

    public void init() throws LWJGLException {

        Display.setLocation((Display.getDisplayMode().getWidth() - parameters.width) / 2,
                (Display.getDisplayMode().getHeight() - parameters.height) / 2);
        Display.setDisplayMode(new DisplayMode(parameters.width*2, parameters.height*2));
        Display.setTitle("Recur");
        Display.setVSyncEnabled(true);
        Display.create();
        int totalMem = glGetInteger(0x9048 /*GL_GPU_MEM_INFO_TOTAL_AVAILABLE_MEM_NVX*/);
        int availMem = glGetInteger(0x9049 /*GL_GPU_MEM_INFO_CURRENT_AVAILABLE_MEM_NVX*/);
        System.out.println("OpenGL Video Memory: " + availMem + " / " + totalMem + " kB available");

        glEnable(GL_TEXTURE_2D);
        glDisable(GL_DEPTH_TEST);

        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(0.0f, 1.0f, 1.0f, 0.0f, -1.0f, 1.0f);

        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();

        textureID = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureID);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP);
        FloatBuffer data = BufferUtils.createFloatBuffer(parameters.width * parameters.height * 4);
        setupBuffers();
        glBindTexture(GL_TEXTURE_2D, textureID);
        sharedGlData.set(Display.getDrawable());
    }

    public void setupBuffers() {
        IntBuffer intBuffer = BufferUtils.createIntBuffer(2);
        BufferUtils.zeroBuffer(intBuffer);
        glGenBuffers(intBuffer);
        FloatBuffer data = BufferUtils.createFloatBuffer(parameters.pixelNum * 4);

        glBindBuffer(GL_ARRAY_BUFFER, intBuffer.get(0));
        glBufferData(GL_ARRAY_BUFFER, data, GL_DYNAMIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, intBuffer.get(1));
        glBufferData(GL_ARRAY_BUFFER, data, GL_DYNAMIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        imageData.setBuffers(intBuffer);
    }

    public void run() {
        try {
            init();
        } catch (LWJGLException l) {
            l.printStackTrace();
            System.out.println("Failed to initialize Recur.");
            return;
        }
        boolean closeRequested = false;
        int frames = 0;
        long startTime = System.currentTimeMillis();
        while (!closeRequested) {
            try {
                sharedGlData.glAcquire();
            } catch (LWJGLException e) {
                e.printStackTrace();
                return;
            }

            //glClear(GL_COLOR_BUFFER_BIT);
            try {
                glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, imageData.getBuffer());
                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB8, parameters.width, parameters.height, 0, GL_RGBA, GL_FLOAT, (long) 0);
                checkGLError();
            } catch (OpenGLException e) {
                e.printStackTrace();
                System.out.println("Failed to bind pixel buffer.");
                return;
            }

            glColor4f(1, 1, 1, 1);
            glBegin(GL_QUADS);
            glTexCoord2f(0.0f, 0.0f);   glVertex2f(0.0f, 0.0f);
            glTexCoord2f(1.0f, 0.0f);   glVertex2f(1.0f, 0.0f);
            glTexCoord2f(1.0f, 1.0f);   glVertex2f(1.0f, 1.0f);
            glTexCoord2f(0.0f, 1.0f);   glVertex2f(0.0f, 1.0f);
            glEnd();

            Display.update();
            closeRequested = Display.isCloseRequested();
            sharedGlData.release();

            if(runToggle || (Mouse.isButtonDown(0) && !mouseStatus[0])) {
                imageData.readFrame();
            }
            if(Mouse.isButtonDown(1) && !mouseStatus[1]) {
                int x = Mouse.getX() / (Display.getWidth() / parameters.width);
                int y = (Display.getHeight() - Mouse.getY()) / (Display.getWidth() / parameters.width);
                printMatrix(Mouse.getX(), y);
                printPosition(Mouse.getX(), y);
            }
            if(Keyboard.isKeyDown(Keyboard.KEY_SPACE) && !keyStatus) {
                runToggle = !runToggle;
            }
            mouseStatus[0] = Mouse.isButtonDown(0);
            mouseStatus[1] = Mouse.isButtonDown(1);
            keyStatus = Keyboard.isKeyDown(Keyboard.KEY_SPACE);
            /*frames++;
            long timeUsed = System.currentTimeMillis() - startTime;
            if (timeUsed > 2000) {
                System.out.println(frames + " frames drawn in " + timeUsed / 1000f + " seconds = "
                        + (frames / (timeUsed / 1000f)));
                startTime = System.currentTimeMillis();
                frames = 0;
            }    */
        }
    }

    private void printMatrix(int x, int y) {
        float[] bMatrix = parameters.getDebugMatrix();
        int startIdx = (parameters.matrixSize * parameters.matrixSize) * ((y * parameters.width) + x);
        System.out.println(x + ", " + y);
        for (int m = 0; m < parameters.matrixSize; m++) {
            for (int n = 0; n < parameters.matrixSize; n++) {
                System.out.format("%.3f ", bMatrix[startIdx+(m*parameters.matrixSize)+n] * 1.0f);
            }
            System.out.println();
        }
    }

    private void printPosition(int x, int y) {
        double XCENTER = parameters.width/2.0f;
        double YCENTER = parameters.height/2.0f;
        double oldxPos = (float)(x) - XCENTER;
        double yPos = (float)(y) - YCENTER;
        double[] rMatrix = { (Math.sin(-parameters.rotateAngle) * parameters.scaleFactor), (Math.cos(-parameters.rotateAngle) * parameters.scaleFactor) };
        double xPos = (oldxPos*rMatrix[1]) - (yPos*rMatrix[0]) + XCENTER;
        yPos = (oldxPos*rMatrix[0]) + (yPos*rMatrix[1]) + YCENTER;
        System.out.format("%.2f , %.2f\n", xPos, yPos);
    }
}

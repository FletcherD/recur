import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.*;

import javax.swing.*;
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
    int zoom;
    int oldWidth, oldHeight;

    public GLDrawer(ImageData inImageData, Recur.SharedParameterUpdate inParameters, Recur.SharedGlData inShared) {
        parameters = inParameters.parameters;
        imageData = inImageData;
        sharedGlData = inShared;
        try{
            DisplayMode current = Display.getDisplayMode();
            int maxWidth = current.getWidth()-50;
            int maxHeight = current.getHeight()-100;
            zoom = (int)Math.floor(Math.min(maxWidth/parameters.width, maxHeight/parameters.height));
            zoom = Math.max(zoom,1);
        } catch(Exception i) {
            i.printStackTrace();
        }
    }

    public void init() throws LWJGLException {

        Display.setLocation((Display.getDisplayMode().getWidth() - parameters.width*zoom) / 2,
                (Display.getDisplayMode().getHeight() - parameters.height*zoom) / 2);
        Display.setDisplayMode(new DisplayMode(parameters.width*zoom, parameters.height*zoom));
        oldWidth = parameters.width*zoom; oldHeight = parameters.height*zoom;
        Display.setTitle("Recur");
        Display.setVSyncEnabled(false);
        Display.setResizable(true);
        Display.create();

        // Shows video memory, but only on Nvidia cards
        //        int totalMem = glGetInteger(0x9048 /*GL_GPU_MEM_INFO_TOTAL_AVAILABLE_MEM_NVX*/);
        //        int availMem = glGetInteger(0x9049 /*GL_GPU_MEM_INFO_CURRENT_AVAILABLE_MEM_NVX*/);
        //        System.out.println("OpenGL Video Memory: " + availMem + " / " + totalMem + " kB available");

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
        FloatBuffer data = BufferUtils.createFloatBuffer(parameters.pixelNum() * 4);

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
            JOptionPane.showMessageDialog(new JFrame(), l.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            l.printStackTrace();
            return;
        }
        boolean closeRequested = false;
        while (!closeRequested && !sharedGlData.finished && !sharedGlData.restart) {
            try {
                sharedGlData.glAcquire();
            } catch (LWJGLException e) {
                JOptionPane.showMessageDialog(new JFrame(), e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
                return;
            }
            if(oldWidth != Display.getWidth() || oldHeight != Display.getHeight()) {
                resized();
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
            if(Keyboard.isKeyDown(Keyboard.KEY_SPACE) && !keyStatus) {
                runToggle = !runToggle;
            }
            mouseStatus[0] = Mouse.isButtonDown(0);
            mouseStatus[1] = Mouse.isButtonDown(1);
            keyStatus = Keyboard.isKeyDown(Keyboard.KEY_SPACE);
        }
        sharedGlData.finished = true;
        imageData.readFrame();
        dispose();
    }

    private void dispose() {
        try {
        sharedGlData.glAcquire();
        } catch (Exception e) {
            e.printStackTrace();
        }
        glDeleteBuffers(imageData.getBuffer(0));
        glDeleteBuffers(imageData.getBuffer(1));
        Display.destroy();
        sharedGlData.destroyRelease();
    }

    private void resized() {
        oldWidth = Display.getWidth();
        oldHeight = Display.getHeight();
        zoom = (int)Math.floor(Math.min(oldWidth/parameters.width, oldHeight/parameters.height));
        zoom = Math.max(zoom,1);
        glViewport((oldWidth-getWidth())/2, (oldHeight-getHeight())/2, getWidth(), getHeight());
        glClear(GL_COLOR_BUFFER_BIT);
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

    public int getWidth() {
        return parameters.width*zoom;
    }
    public int getHeight() {
        return parameters.height*zoom;
    }
}

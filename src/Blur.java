/*
 * Copyright (c) 2002-2008 LWJGL Project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'LWJGL' nor the names of
 *   its contributors may be used to endorse or promote products derived
 *   from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/*
 * 3-D gear wheels. Originally by Brian Paul
 */

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ByteBuffer;
import java.util.Random;
import java.math.*;

import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.LWJGLException;
import org.lwjgl.LWJGLUtil;
import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.*;

import static org.lwjgl.opengl.GL11.*;

public class Blur {

    int[] PBids = new int[2];
    int imageIndex = 0;
    int imageNextIndex = 1;
    int textureID;
    boolean mouseStatus[] = { false, false };
    boolean keyStatus = false;
    boolean runToggle = false;
    CLRunner clRunner;
    float[] bMatrix = null;

    Parameters parameters;

    public static void main(String[] args) {
        new Blur().execute();
        System.exit(0);
    }

    /**
     *
     */
    private void execute() {
        try {
            init();
        } catch (LWJGLException le) {
            le.printStackTrace();
            System.out.println("Failed to initialize Blur.");
            return;
        }

        loop();

        destroy();
    }

    /**
     *
     */
    private void destroy() {
        Display.destroy();
    }

    private void init() throws LWJGLException {
        parameters = new Parameters();

        Display.setLocation((Display.getDisplayMode().getWidth() - parameters.width) / 2,
                (Display.getDisplayMode().getHeight() - parameters.height) / 2);
        Display.setDisplayMode(new DisplayMode(parameters.width, parameters.height));
        Display.setTitle("Blur");
        //Display.setVSyncEnabled(true);
        Display.create();
        int totalMem = glGetInteger(0x9048 /*GL_GPU_MEM_INFO_TOTAL_AVAILABLE_MEM_NVX*/);
        int availMem = glGetInteger(0x9049 /*GL_GPU_MEM_INFO_CURRENT_AVAILABLE_MEM_NVX*/);
        System.out.println(availMem + " / " + totalMem + " kB available");

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
        IntBuffer data = BufferUtils.createIntBuffer(parameters.width * parameters.height);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, parameters.width, parameters.height, 0, GL_RGBA, GL_UNSIGNED_BYTE, data);
        setupBuffers();
        glBindTexture(GL_TEXTURE_2D, textureID);
        try {
            clRunner = new CLRunner(PBids, parameters);
        } catch (Exception le) {
            le.printStackTrace();
            System.out.println("Failed to initialize OpenCL.");
            System.exit(0);
        }
    }

    /**
     *
     */
    private void loop() {
        long startTime = System.currentTimeMillis() + 5000;
        long fps = 0;

        while (!Display.isCloseRequested()) {

            //glClear(GL_COLOR_BUFFER_BIT);

            GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, PBids[imageIndex]);
            glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, parameters.width, parameters.height, GL_RGBA, GL_UNSIGNED_BYTE, (long) 0);

            glColor4f(1, 1, 1, 1);
            glBegin(GL_QUADS);
            glTexCoord2f(0.0f, 0.0f);   glVertex2f(0.0f, 0.0f);
            glTexCoord2f(1.0f, 0.0f);   glVertex2f(1.0f, 0.0f);
            glTexCoord2f(1.0f, 1.0f);   glVertex2f(1.0f, 1.0f);
            glTexCoord2f(0.0f, 1.0f);   glVertex2f(0.0f, 1.0f);
            glEnd();

            Display.update();
            if (startTime > System.currentTimeMillis()) {
                fps++;
            } else {
                long timeUsed = 5000 + (startTime - System.currentTimeMillis());
                startTime = System.currentTimeMillis() + 5000;
                System.out.println(fps + " frames in " + timeUsed / 1000f + " seconds = "
                        + (fps / (timeUsed / 1000f)));
                fps = 0;
            }


            if(runToggle || (Mouse.isButtonDown(0) && mouseStatus[0] == false)) {
                flipBuffers();
            }
            if(Mouse.isButtonDown(1) && !mouseStatus[1]) {
                int y = parameters.height - Mouse.getY();
                printMatrix(Mouse.getX(), y);
                printPosition(Mouse.getX(), y);
            }
            if(Keyboard.isKeyDown(Keyboard.KEY_SPACE) && !keyStatus) {
            	runToggle = !runToggle;
            }
            mouseStatus[0] = Mouse.isButtonDown(0);
            mouseStatus[1] = Mouse.isButtonDown(1);
            keyStatus = Keyboard.isKeyDown(Keyboard.KEY_SPACE);

        }

        clRunner.dispose();
    }

    private void flipBuffers() {
        imageIndex = imageNextIndex;
        imageNextIndex = (imageIndex + 1) % 2;
        clRunner.flipBuffers(imageIndex);
    }

    private void printMatrix(int x, int y) {
        if (bMatrix == null) {
            bMatrix = clRunner.getMatrices();
        }
        int startIdx = (parameters.matrixSize * parameters.matrixSize) * ((y * parameters.width) + x);
        System.out.println(x + ", " + y);
        for (int m = 0; m < parameters.matrixSize; m++) {
            for (int n = 0; n < parameters.matrixSize; n++) {
                System.out.format("%.3f ", bMatrix[startIdx+(m*parameters.matrixSize)+n] * 10.0f);
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

    private void setupBuffers() {
        IntBuffer intBuffer = BufferUtils.createIntBuffer(2);
        BufferUtils.zeroBuffer(intBuffer);
        GL15.glGenBuffers(intBuffer);
        PBids[0] = intBuffer.get(0);
        PBids[1] = intBuffer.get(1);
        IntBuffer data = BufferUtils.createIntBuffer(parameters.pixelNum);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, PBids[0]);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, data, GL15.GL_DYNAMIC_DRAW);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, PBids[1]);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, data, GL15.GL_DYNAMIC_DRAW);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }
}

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


import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.Drawable;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class Recur {

    CLRunner clRunner;
    GLDrawer glDrawer;
    ImageData imageData;
    Thread glThread;
    Thread clThread;

    public class SharedGlData {
        private Drawable drawable;
        boolean initialized = false;
        boolean clWaiting = false;
        boolean finished = false;
        public Lock lock = new ReentrantLock();

        public synchronized void set(Drawable d) {
            try {glAcquire();} catch (LWJGLException e) {e.printStackTrace();}
            drawable = d;
            initialized = true;
            release();
            notifyAll();
        }

        public synchronized Drawable get() {
            while(!initialized) {
                try {
                    wait();
                } catch (InterruptedException e) {e.printStackTrace();}
            }
            try{ clAcquire(); } catch (LWJGLException e) {e.printStackTrace();}
            return drawable;
        }

        public void release() {
            try { Display.releaseContext(); } catch (LWJGLException e) {e.printStackTrace();}
            lock.unlock();
        }

        public synchronized void glAcquire() throws LWJGLException {
            while (clWaiting) {
                try {
                    wait();
                } catch (InterruptedException e) {e.printStackTrace();}
            }
            lock.lock();
            Display.makeCurrent();
        }

        public synchronized void clAcquire() throws LWJGLException {
            clWaiting = true;
            notifyAll();
            lock.lock();
            clWaiting = false;
            notifyAll();
            Display.makeCurrent();
        }

        public void setFinished() {
            finished = true;
        }
        public boolean isFinished() {
            return finished;
        }
    }
    SharedGlData sharedGlData = new SharedGlData();

    public class SharedParameterUpdate {
        public Parameters parameters;
        public Parameters oldParameters;
        boolean update=false;

        SharedParameterUpdate() {
            parameters = new Parameters();
        }

        public synchronized void setUpdate(Parameters in){
            if(!update) {
                oldParameters = parameters;
            }
            parameters = new Parameters(in);
            update = true;
        }

        public synchronized boolean getUpdate(){
            if(update) {
                update = false;
                return true;
            }
            return false;
        }
    }
    SharedParameterUpdate parameterUpdate = new SharedParameterUpdate();

    public static void main(String[] args) {
        new Recur().execute();
        System.exit(0);
    }

    /**
     *
     */
    private void execute() {

        try {
            imageData = new ImageData();
            glDrawer = new GLDrawer(imageData, parameterUpdate, sharedGlData);
            glThread = new Thread((glDrawer));
            glThread.start();
        } catch (LWJGLException le) {
            le.printStackTrace();
            System.out.println("Failed to initialize OpenGL.\n" +
                    "Press any key to quit.");
            try{ System.in.read(); } catch(Exception e) {}
            return;
        }

        try {
            clRunner = new CLRunner(imageData, parameterUpdate, sharedGlData);
            clThread = new Thread(clRunner);
            clThread.start();
        } catch (Exception le) {
            le.printStackTrace();
            System.out.println("Failed to initialize OpenCL.\n" +
                    "Press any key to quit.");
            try{ System.in.read(); } catch(Exception e) {}
            System.exit(0);
        }
        ParametersUI parametersUI = new ParametersUI(parameterUpdate, glDrawer.getWidth(), glDrawer.getHeight());

        while(glThread.isAlive() || clThread.isAlive()) {
            //try {
                //glThread.join();
                //sharedGlData.setFinished();
            //} catch (InterruptedException e) {}
        }
    }

    /**
     *
     */



}
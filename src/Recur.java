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
        boolean restart = false;
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
        public void destroyRelease() {
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
    }
    SharedGlData sharedGlData;

    public class SharedParameterUpdate {
        public Parameters parameters;
        public Parameters oldParameters;
        boolean update=false;

        SharedParameterUpdate() {
            parameters = new Parameters();
        }

        public synchronized void setUpdate(Parameters in){
            if(!update) {
                oldParameters = new Parameters(parameters);
            }
            parameters.clone(in);
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
    ParametersUI parametersUI = null;

    public static void main(String[] args) {
        new Recur().execute();
        System.exit(0);
    }

    /**
     *
     */
    private void execute() {
        do {
            sharedGlData = new SharedGlData();
            imageData = new ImageData();

            glDrawer = new GLDrawer(imageData, parameterUpdate, sharedGlData);
            glThread = new Thread((glDrawer));
            glThread.start();

            clRunner = new CLRunner(imageData, parameterUpdate, sharedGlData);
            clThread = new Thread(clRunner);
            clThread.start();

            if(parametersUI == null)
                parametersUI = new ParametersUI(parameterUpdate, glDrawer.getWidth(), glDrawer.getHeight());
            while(glThread.isAlive() && clThread.isAlive()) {}
            try{
                sharedGlData.finished = true;
                glThread.join(); clThread.join();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } while (sharedGlData.restart);
    }

}

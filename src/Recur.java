import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.Drawable;

import java.io.File;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
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
        public class Arguments {
            boolean takeScreenshot=false;
            int framesUntilScreenshot;
            int framesDuringScreenshot;
        }
        public Arguments arguments;

        public class ClInformation {
            float fps;
            float elapsedTime=0;
            String device;
            String version;
            long memSize;
            long frequency;
            long computeUnits;
            boolean update=false;
        }
        public ClInformation clInfo;

        SharedParameterUpdate() {
            parameters = new Parameters();
            clInfo = new ClInformation();
            arguments = new Arguments();
        }
        SharedParameterUpdate(String[] args) {
            parameters = new Parameters();
            clInfo = new ClInformation();
            arguments = new Arguments();
            if(args.length == 0) {
                return;
            }
            try {
                String data = args[0];
                parameters.deserialize(data);
                if(args.length > 1) {
                    arguments.framesUntilScreenshot = Integer.parseInt(args[1]);
                    arguments.framesDuringScreenshot = Integer.parseInt(args[2]);
                    arguments.takeScreenshot = true;
                    if(args.length > 3) {
                        parameters.width = Integer.parseInt(args[3]);
                        parameters.height = Integer.parseInt(args[3]);
                    }
                }
            } catch (Exception e) {
                System.err.println("Argument error: " + e.getMessage());
                System.exit(1);
            }
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
    SharedParameterUpdate parameterUpdate;
    ParametersUI parametersUI = null;

    public static void main(String[] args) {
        Recur rApp = new Recur(args);
        rApp.execute();
        System.exit(0);
    }

    Recur(String[] args) {
        parameterUpdate = new SharedParameterUpdate(args);
    }

    /**
     *
     */
    private void execute() {
        do {
            sharedGlData = new SharedGlData();
            imageData = new ImageData();

            clRunner = new CLRunner(imageData, parameterUpdate, sharedGlData);
            clThread = new Thread(clRunner);
            clThread.start();

            while(clThread.isAlive()) {

            }
            try{
                sharedGlData.finished = true;
                clThread.join();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } while (sharedGlData.restart);
    }

}

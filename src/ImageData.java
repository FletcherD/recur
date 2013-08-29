import java.nio.IntBuffer;

/**
 * Created with IntelliJ IDEA.
 * User: fdostie
 * Date: 11/22/12
 * Time: 1:57 AM
 * To change this template use File | Settings | File Templates.
 */
public class ImageData {
    private int frameNum = 0;
    private int lastReadFrame = 0;
    private int flip = 0;
    private int[] PBids;
    private boolean initialized = false;

    public ImageData() {
        PBids = new int[2];
    }

    public synchronized void setBuffers(IntBuffer buf) {
        PBids[0] = buf.get(0);
        PBids[1] = buf.get(1);
        initialized = true;
        notifyAll();
    }

    public synchronized int getBuffer(int i) {
        while(!initialized) {
            try {
                wait();
            } catch (InterruptedException e) {e.printStackTrace(); }
        }
        return PBids[i];
    }
    public synchronized int getBuffer() {
        return getBuffer(getFlip());
    }
    public synchronized int getWorkingBuffer() {
        return getBuffer(getWorkingFlip());
    }
    public synchronized int getFrameNum() {
        return frameNum;
    }
    public synchronized int getFlip() {
        return flip;
    }
    public synchronized int getWorkingFlip() {
        return (flip+1)%2;
    }

    public synchronized void waitForRead() {
        while(lastReadFrame < frameNum) {
            try {
                wait();
            } catch (InterruptedException e) {e.printStackTrace();}
        }
    }
    public synchronized void flipBuffers() {
        flip = (flip+1)%2;
        frameNum++;
        notifyAll();
    }
    public synchronized void readFrame() {
        lastReadFrame = frameNum;
        notifyAll();
    }
}

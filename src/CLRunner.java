import org.lwjgl.opencl.Util;
import org.lwjgl.opencl.CLMem;
import org.lwjgl.opencl.CLCommandQueue;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.CLProgram;
import org.lwjgl.opencl.CLKernel;

import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.security.SecureRandom;
import java.util.List;
import java.util.Random;
import java.io.*;

import org.lwjgl.opencl.CL;
import org.lwjgl.opencl.CLContext;
import org.lwjgl.opencl.CLDevice;
import org.lwjgl.opencl.CLPlatform;
import org.lwjgl.opencl.CL10GL;
import org.lwjgl.opengl.Drawable;

import static org.lwjgl.opencl.CL10.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * Created with IntelliJ IDEA.
 * User: fletcher
 * Date: 8/11/12
 * Time: 4:47 PM
 * To change this template use File | Settings | File Templates.
 */


public class CLRunner implements Runnable {
    ImageData imageData;

    CLPlatform platform;
    CLContext context;
    CLCommandQueue iterateQueue;
    CLCommandQueue convertQueue;
    CLCommandQueue randomQueue;
    CLProgram program;
    CLKernel blurKernel;
    CLKernel unsharpKernel;
    CLKernel convertKernel;
    CLKernel randomKernel;
    List<CLDevice> devices;
    PointerBuffer kernelPixelWorkSize = BufferUtils.createPointerBuffer(1);
    PointerBuffer kernelRandomWorkSize = BufferUtils.createPointerBuffer(1);
    
    CLMem[] pboMem = new CLMem[2];
    CLMem floatImage;
    CLMem intermediateImage;
    
    CLMem rMatrix;
    CLMem mPositions;
    CLMem blurMatrix;
    CLMem blurStd;
    CLMem unsharpMatrix;
    CLMem gaussianStd;
    CLMem gaussianLookup;
    
    CLMem randomData;
    Parameters parameters;
    Recur.SharedGlData sharedGlData;

    public CLRunner(ImageData inImageData, Parameters p, Recur.SharedGlData inShared) {
        imageData = inImageData;
        parameters = p;
        sharedGlData = inShared;
    }

    private void init() throws Exception {
        Drawable d = sharedGlData.get();
        CL.create();
        platform = CLPlatform.getPlatforms().get(0);
        devices = platform.getDevices(CL_DEVICE_TYPE_GPU);
        IntBuffer err = BufferUtils.createIntBuffer(1);
        context = CLContext.create(platform, devices, null, d, err);
        Util.checkCLError(err.get(0));
        iterateQueue = clCreateCommandQueue(context, devices.get(0), CL_QUEUE_PROFILING_ENABLE, null);
        convertQueue = clCreateCommandQueue(context, devices.get(0), CL_QUEUE_PROFILING_ENABLE, null);
        randomQueue = clCreateCommandQueue(context, devices.get(0), CL_QUEUE_PROFILING_ENABLE, null);
        glFinish();
        pboMem[0] = CL10GL.clCreateFromGLBuffer(context, CL_MEM_WRITE_ONLY, imageData.getBuffer(0), null);
        pboMem[1] = CL10GL.clCreateFromGLBuffer(context, CL_MEM_WRITE_ONLY, imageData.getBuffer(1), null);

        rMatrix = clCreateBuffer(context, CL_MEM_READ_ONLY, BufferUtils.createFloatBuffer(2), null);
        mPositions = clCreateBuffer(context, CL_MEM_READ_WRITE, BufferUtils.createIntBuffer(parameters.pixelNum * 2), null);
        blurStd = clCreateBuffer(context, CL_MEM_READ_ONLY, BufferUtils.createFloatBuffer(1), null);
        gaussianStd = clCreateBuffer(context, CL_MEM_READ_ONLY, BufferUtils.createFloatBuffer(1), null);
        blurMatrix = clCreateBuffer(context, CL_MEM_READ_ONLY, BufferUtils.createFloatBuffer(parameters.matrixSize * parameters.matrixSize * parameters.pixelNum), null);
        unsharpMatrix = clCreateBuffer(context, CL_MEM_READ_ONLY, BufferUtils.createFloatBuffer(parameters.matrixSize * parameters.matrixSize), null);
        gaussianLookup = clCreateBuffer(context, CL_MEM_READ_ONLY, BufferUtils.createFloatBuffer(1024), null);
        FloatBuffer fImage = BufferUtils.createFloatBuffer(parameters.pixelNum * 4);
        for(int i = 0; i < parameters.pixelNum ; i++) {
            float value = 0.5f;
            for(int j = 0; j < 4; j++) { fImage.put(value); }
        }
        fImage.rewind();
        floatImage = clCreateBuffer(context, CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR, fImage, null);
        intermediateImage = clCreateBuffer(context, CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR, fImage, null);

        if(parameters.noiseOn) {
            randomData = clCreateBuffer(context, CL_MEM_READ_WRITE, BufferUtils.createLongBuffer(parameters.pixelNum/2), null);
            generateRandomData();
        }

        buildKernel();
        setLinearTransform();
        setGaussianBlur();
        setUnsharp();
        setNoiseStdev();
        calculateBlurMatrices();
        calculateGaussianLookup();
        sharedGlData.release();
    }

    public void run() {
        int frames = 0;
        try{
            init();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Failed to run OpenCL thread.");
            return;
        }
        long startTime = System.currentTimeMillis();
        while(true)
        {
            flipBuffers();
            frames++;
            long timeUsed = System.currentTimeMillis() - startTime;
            if (timeUsed > 2000) {
                System.out.println(frames + " frames rendered in " + timeUsed / 1000f + " seconds = "
                        + (frames / (timeUsed / 1000f)));
                startTime = System.currentTimeMillis();
                frames = 0;
            }
        }
    }

    public void flipBuffers() {
        clEnqueueNDRangeKernel(iterateQueue, blurKernel, 1, null, kernelPixelWorkSize, null, null, null);
        clEnqueueNDRangeKernel(iterateQueue, unsharpKernel, 1, null, kernelPixelWorkSize, null, null, null);
        clFinish(iterateQueue);
        sharedGlData.clAcquire();
        CL10GL.clEnqueueAcquireGLObjects(iterateQueue, pboMem[imageData.getWorkingFlip()], null, null);
        clEnqueueCopyBuffer(iterateQueue, floatImage, pboMem[imageData.getWorkingFlip()], 0, 0, 4*4*kernelPixelWorkSize.get(0), null, null);
        CL10GL.clEnqueueReleaseGLObjects(iterateQueue, pboMem[imageData.getWorkingFlip()], null, null);
        clFinish(iterateQueue);
        sharedGlData.release();

        imageData.flipBuffers();

        if(parameters.noiseOn) {
            clEnqueueNDRangeKernel(randomQueue, randomKernel, 1, null, kernelRandomWorkSize, null, null, null);
        }
    }

    public void buildKernel() throws Exception {
        IntBuffer err = BufferUtils.createIntBuffer(1);
        String source;
        try {
            source = readKernelSource("/clCode/kernel.cl");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Failed to read kernel file.");
            return;
        }
        source = source.replaceAll("WIDTH", Integer.toString(parameters.width));
        source = source.replaceAll("HEIGHT", Integer.toString(parameters.height));
        source = source.replaceAll("MSIZE", Integer.toString(parameters.matrixSize));
        source = source.replaceAll("BRIGHTNESS", Double.toString(parameters.brightness));
        if(parameters.noiseOn) {
            source = source.replaceAll("//NOISE_DEFINE", "#define NOISE");
        }

        try {
            program = clCreateProgramWithSource(context, source, err);
            Util.checkCLError(err.get(0));
            Util.checkCLError(clBuildProgram(program, devices.get(0), "", null));
        } catch (Exception er) {
            PointerBuffer logSize = BufferUtils.createPointerBuffer(1);
            clGetProgramBuildInfo(program, devices.get(0), CL_PROGRAM_BUILD_LOG, null, logSize);
            ByteBuffer logBuf = BufferUtils.createByteBuffer((int)(logSize.get(0)));
            clGetProgramBuildInfo(program, devices.get(0), CL_PROGRAM_BUILD_LOG, logBuf, logSize);
            String log = "";
            while(logBuf.hasRemaining()) { log += (char)(logBuf.get()); }
            System.out.println(log);
            throw er;
        }
        // sum has to match a kernel method name in the OpenCL source
        blurKernel = clCreateKernel(program, "iterate", null);
        unsharpKernel = clCreateKernel(program, "unsharpMask", null);
        convertKernel = clCreateKernel(program, "convertToPixelBuf", null);
        randomKernel = clCreateKernel(program, "advanceRandomNumbers", null);
        blurKernel.setArg(0, floatImage);
        blurKernel.setArg(1, intermediateImage);
        blurKernel.setArg(2, mPositions);
        blurKernel.setArg(3, blurMatrix);
        blurKernel.setArg(4, randomData);
        blurKernel.setArg(5, gaussianLookup);
        convertKernel.setArg(0, floatImage);
        unsharpKernel.setArg(0, intermediateImage);
        unsharpKernel.setArg(1, floatImage);
        unsharpKernel.setArg(2, unsharpMatrix);
        randomKernel.setArg(0, randomData);
        kernelPixelWorkSize.put(0, parameters.pixelNum);
        kernelRandomWorkSize.put(0, parameters.pixelNum/2);
    }

    public void dispose() {
        clReleaseKernel(blurKernel);
        clReleaseKernel(convertKernel);
        clReleaseKernel(unsharpKernel);
        clReleaseKernel(randomKernel);
        clReleaseProgram(program);
        clReleaseCommandQueue(iterateQueue);
        clReleaseContext(context);
        CL.destroy();
    }

    public void setLinearTransform() {
    	double scale = 1.0/parameters.scaleFactor;
        FloatBuffer rMat = BufferUtils.createFloatBuffer(2);
        rMat.put((float)(Math.sin(parameters.rotateAngle) * scale));
        rMat.put((float)(Math.cos(parameters.rotateAngle) * scale));
        rMat.rewind();
        Util.checkCLError(clEnqueueWriteBuffer(iterateQueue, rMatrix, 1, 0, rMat, null, null));
    }

    public void setGaussianBlur() {
        FloatBuffer bStd = BufferUtils.createFloatBuffer(1);
        bStd.put((float)(-parameters.blurRadius));
        bStd.rewind();
        Util.checkCLError(clEnqueueWriteBuffer(iterateQueue, blurStd, 1, 0, bStd, null, null));
    }

    public void setNoiseStdev() {
        FloatBuffer nStd = BufferUtils.createFloatBuffer(1);
        nStd.put((float)(parameters.noiseStd));
        nStd.rewind();
        Util.checkCLError(clEnqueueWriteBuffer(iterateQueue, gaussianStd, 1, 0, nStd, null, null));
    }

    public void setUnsharp() {
        FloatBuffer unsharp = BufferUtils.createFloatBuffer(parameters.matrixSize * parameters.matrixSize);
        double sum = 0;
        for(int m = 0; m < parameters.matrixSize; m++) {
            for(int n = 0; n < parameters.matrixSize; n++) {
                double x = (double)m - (double)(parameters.matrixSize-1) / 2.0;
                double y = (double)n - (double)(parameters.matrixSize-1) / 2.0;
                double value = Math.exp(-(x*x + y*y) / parameters.unsharpRadius);
                sum += value;
            }
        }
        sum /= (-parameters.unsharpWeight);
        for(int m = 0; m < parameters.matrixSize; m++) {
            for(int n = 0; n < parameters.matrixSize; n++) {
                double x = (double)m - (double)(parameters.matrixSize-1) / 2.0;
                double y = (double)n - (double)(parameters.matrixSize-1) / 2.0;
                double value = Math.exp(-(x*x + y*y) / parameters.unsharpRadius) / sum;
                if(m == parameters.matrixSize/2 && n == parameters.matrixSize/2) { value += (1.0 + parameters.unsharpWeight); }
                unsharp.put((float)value);
            }
        }
        unsharp.rewind();
        Util.checkCLError(clEnqueueWriteBuffer(iterateQueue, unsharpMatrix, 1, 0, unsharp, null, null));
    }

    public float[] getMatrices() {
        FloatBuffer mBuf = BufferUtils.createFloatBuffer(parameters.pixelNum * parameters.matrixSize * parameters.matrixSize);
        clEnqueueReadBuffer(iterateQueue, blurMatrix, 1, 0, mBuf, null, null);
        float[] bMatrix = new float[parameters.pixelNum * parameters.matrixSize * parameters.matrixSize];
        clFinish(iterateQueue);
        mBuf.get(bMatrix);
        return bMatrix;
    }

    public float[] getGaussianLookup() {
        FloatBuffer mBuf = BufferUtils.createFloatBuffer(1024);
        clEnqueueReadBuffer(iterateQueue, gaussianLookup, 1, 0, mBuf, null, null);
        float[] bMatrix = new float[1024];
        clFinish(iterateQueue);
        mBuf.get(bMatrix);
        return bMatrix;
    }

    public void calculateBlurMatrices() {
        IntBuffer err = BufferUtils.createIntBuffer(1);
        CLKernel calcBlurKernel = clCreateKernel(program, "createBlurMatrices", err);
        Util.checkCLError(err.get(0));
        CLMem blurMatrixTemp = clCreateBuffer(context, CL_MEM_WRITE_ONLY, BufferUtils.createFloatBuffer(parameters.matrixSize * parameters.matrixSize * parameters.pixelNum), null);
        calcBlurKernel.setArg(0, blurMatrixTemp);
        calcBlurKernel.setArg(1, mPositions);
        calcBlurKernel.setArg(2, rMatrix);
        calcBlurKernel.setArg(3, blurStd);
        Util.checkCLError(clEnqueueNDRangeKernel(iterateQueue, calcBlurKernel, 1, null, kernelPixelWorkSize, null, null, null));
        clFinish(iterateQueue);
        Util.checkCLError(clEnqueueCopyBuffer(iterateQueue, blurMatrixTemp, blurMatrix, 0, 0, parameters.pixelNum * parameters.matrixSize * parameters.matrixSize*4, null, null));
        clReleaseKernel(calcBlurKernel);
        clFinish(iterateQueue);
    }

    public void calculateGaussianLookup() {
        IntBuffer err = BufferUtils.createIntBuffer(1);
        CLKernel calcGaussianKernel = clCreateKernel(program, "createGaussianLookup", err);
        Util.checkCLError(err.get(0));
        CLMem gaussianLookupTemp = clCreateBuffer(context, CL_MEM_WRITE_ONLY, BufferUtils.createFloatBuffer(1024), null);
        calcGaussianKernel.setArg(0, gaussianLookupTemp);
        calcGaussianKernel.setArg(1, gaussianStd);
        PointerBuffer kernelGaussianWorkSize = BufferUtils.createPointerBuffer(1);
        kernelGaussianWorkSize.put(0, 1024);
        Util.checkCLError(clEnqueueNDRangeKernel(iterateQueue, calcGaussianKernel, 1, null, kernelGaussianWorkSize, null, null, null));
        clFinish(iterateQueue);
        Util.checkCLError(clEnqueueCopyBuffer(iterateQueue, gaussianLookupTemp, gaussianLookup, 0, 0, 1024*4, null, null));
        clReleaseKernel(calcGaussianKernel);
        clFinish(iterateQueue);
    }

    String readKernelSource(String filename) throws Exception {
        URL url = this.getClass().getResource(filename);
        BufferedInputStream in = new BufferedInputStream(url.openStream());
        byte[] buffer = new byte[in.available()];
        int err = in.read(buffer);
        return new String(buffer);
    }
    
    public void generateRandomData() {
        SecureRandom random = new SecureRandom();
        LongBuffer randomDataLocal = BufferUtils.createLongBuffer(parameters.pixelNum / 2);
    	for(int i = 0; i < parameters.pixelNum/2; i++) {
			randomDataLocal.put(random.nextLong());
    	}
    	randomDataLocal.rewind();
        Util.checkCLError(clEnqueueWriteBuffer(iterateQueue, randomData, 1, 0, randomDataLocal, null, null));
    }
}

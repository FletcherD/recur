import org.lwjgl.opencl.Util;
import org.lwjgl.opencl.CLMem;
import org.lwjgl.opencl.CLCommandQueue;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.CLProgram;
import org.lwjgl.opencl.CLKernel;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.io.*;

import org.lwjgl.opencl.CL;
import org.lwjgl.opencl.CLContext;
import org.lwjgl.opencl.CLDevice;
import org.lwjgl.opencl.CLPlatform;
import org.lwjgl.opencl.CL10GL;
import org.lwjgl.opengl.Display;

import static org.lwjgl.opencl.CL10.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * Created with IntelliJ IDEA.
 * User: fletcher
 * Date: 8/11/12
 * Time: 4:47 PM
 * To change this template use File | Settings | File Templates.
 */
public class CLRunner {

    CLPlatform platform;
    CLContext context;
    CLCommandQueue queue;
    CLProgram program;
    CLKernel blurKernel;
    CLKernel unsharpKernel;
    CLKernel convertKernel;
    List<CLDevice> devices;
    PointerBuffer kernel1DGlobalWorkSize = BufferUtils.createPointerBuffer(1);
    
    CLMem[] pboMem = new CLMem[2];
    CLMem floatImage;
    CLMem intermediateImage;
    
    CLMem rMatrix;
    CLMem blurMatrix;
    CLMem blurStd;
    CLMem unsharpMatrix;
    CLMem mPositions;
    
    CLMem randomData;
    CLMem randomDataPos;
    long randomSeed = (new Date()).getTime();
    int randomIdx = 0;
    LongBuffer randomDataLocal;
    Parameters parameters;

    public CLRunner(int[] PBids, Parameters p) throws Exception{
        parameters = p;
        CL.create();
        platform = CLPlatform.getPlatforms().get(0);
        devices = platform.getDevices(CL_DEVICE_TYPE_GPU);
        IntBuffer err = BufferUtils.createIntBuffer(1);
        context = CLContext.create(platform, devices, null, Display.getDrawable(), err);
        Util.checkCLError(err.get(0));
        queue = clCreateCommandQueue(context, devices.get(0), CL_QUEUE_PROFILING_ENABLE, null);
        glFinish();
        pboMem[0] = CL10GL.clCreateFromGLBuffer(context, CL_MEM_WRITE_ONLY, PBids[0], null);
        pboMem[1] = CL10GL.clCreateFromGLBuffer(context, CL_MEM_WRITE_ONLY, PBids[1], null);

        rMatrix = clCreateBuffer(context, CL_MEM_READ_ONLY, BufferUtils.createFloatBuffer(2), null);
        mPositions = clCreateBuffer(context, CL_MEM_READ_WRITE, BufferUtils.createIntBuffer(parameters.pixelNum*2), null);
        blurStd = clCreateBuffer(context, CL_MEM_READ_ONLY, BufferUtils.createFloatBuffer(1), null);
        blurMatrix = clCreateBuffer(context, CL_MEM_READ_WRITE, BufferUtils.createFloatBuffer(parameters.matrixSize * parameters.matrixSize * parameters.pixelNum), null);
        unsharpMatrix = clCreateBuffer(context, CL_MEM_READ_ONLY, BufferUtils.createFloatBuffer(parameters.matrixSize * parameters.matrixSize), null);
        randomData = clCreateBuffer(context, CL_MEM_READ_ONLY, BufferUtils.createLongBuffer(parameters.randomDataSize), null);
        randomDataPos = clCreateBuffer(context, CL_MEM_READ_ONLY, BufferUtils.createIntBuffer(1), null);
        generateRandomData();
        FloatBuffer fImage = BufferUtils.createFloatBuffer(parameters.pixelNum * 4);
        Random random = new Random();
        for(int i = 0; i < parameters.pixelNum ; i++) {
        	float value = random.nextFloat() * 255.0f;
            for(int j = 0; j < 4; j++) { fImage.put(value); }
        }
        fImage.rewind();
        floatImage = clCreateBuffer(context, CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR, fImage, null);
        intermediateImage = clCreateBuffer(context, CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR, fImage, null);

        try {
            buildKernel();
        } catch (Exception er) {
            er.printStackTrace();
            throw er;
        }
        setLinearTransform();
        setGaussianBlur();
        setUnsharp();
        calculateBlurMatrices();
    }

    public void buildKernel() throws Exception {
        IntBuffer err = BufferUtils.createIntBuffer(1);
        String source;
        String sourceDir = "//home//fletcher//Documents//Blur//clCode";
        try {
            source = readKernelSource(sourceDir + "//kernel.cl");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Failed to read kernel file.");
            return;
        }
        source = source.replaceAll("WIDTH", Integer.toString(parameters.width));
        source = source.replaceAll("HEIGHT", Integer.toString(parameters.height));
        source = source.replaceAll("MSIZE", Integer.toString(parameters.matrixSize));
        source = source.replaceAll("BRIGHTNESS", Double.toString(parameters.brightness));
        source = source.replaceAll("SOURCEDIR", sourceDir);
        if(parameters.noiseOn) {
            source = source.replaceAll("NOISEPARAM", Double.toString(parameters.noiseWeight));
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
        blurKernel.setArg(0, floatImage);
        blurKernel.setArg(1, intermediateImage);
        blurKernel.setArg(2, mPositions);
        blurKernel.setArg(3, blurMatrix);
        blurKernel.setArg(4, randomData);
        blurKernel.setArg(5, randomDataPos);
        convertKernel.setArg(0, floatImage);
        unsharpKernel.setArg(0, intermediateImage);
        unsharpKernel.setArg(1, floatImage);
        unsharpKernel.setArg(2, unsharpMatrix);
        kernel1DGlobalWorkSize.put(0, parameters.pixelNum);
    }

    public void dispose() {
        clReleaseKernel(blurKernel);
        clReleaseKernel(convertKernel);
        clReleaseKernel(unsharpKernel);
        clReleaseProgram(program);
        clReleaseCommandQueue(queue);
        clReleaseContext(context);
        CL.destroy();
    }

    public void setLinearTransform() {
    	double scale = 1.0/parameters.scaleFactor;
        FloatBuffer rMat = BufferUtils.createFloatBuffer(2);
        rMat.put((float)(Math.sin(parameters.rotateAngle) * scale));
        rMat.put((float)(Math.cos(parameters.rotateAngle) * scale));
        rMat.rewind();
        Util.checkCLError(clEnqueueWriteBuffer(queue, rMatrix, 1, 0, rMat, null, null));
    }

    public void setGaussianBlur() {
        FloatBuffer bStd = BufferUtils.createFloatBuffer(1);
        bStd.put((float)(-parameters.blurRadius));
        bStd.rewind();
        Util.checkCLError(clEnqueueWriteBuffer(queue, blurStd, 1, 0, bStd, null, null));
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
        Util.checkCLError(clEnqueueWriteBuffer(queue, unsharpMatrix, 1, 0, unsharp, null, null));
    }

    public float[] getMatrices() {
        FloatBuffer mBuf = BufferUtils.createFloatBuffer(parameters.pixelNum * parameters.matrixSize * parameters.matrixSize);
        clEnqueueReadBuffer(queue, blurMatrix, 1, 0, mBuf, null, null);
        float[] bMatrix = new float[parameters.pixelNum * parameters.matrixSize * parameters.matrixSize];
        clFinish(queue);
        mBuf.get(bMatrix);
        return bMatrix;
    }

    public void calculateBlurMatrices() {
        clFinish(queue);
        IntBuffer err = BufferUtils.createIntBuffer(1);
        CLKernel calcBlurKernel = clCreateKernel(program, "createBlurMatrices", err);
        Util.checkCLError(err.get(0));
        calcBlurKernel.setArg(0, blurMatrix);
        calcBlurKernel.setArg(1, mPositions);
        calcBlurKernel.setArg(2, rMatrix);
        calcBlurKernel.setArg(3, blurStd);
        Util.checkCLError(clEnqueueNDRangeKernel(queue, calcBlurKernel, 1, null, kernel1DGlobalWorkSize, null, null, null));
        clFinish(queue);
        clReleaseKernel(calcBlurKernel);
    }

    String readKernelSource(String filename) throws Exception {
        BufferedReader in = new BufferedReader(new FileReader(filename));
        String r = "";
        while (in.ready()) {
            r += in.readLine() + "\n";
        }
        in.close();
        return r;
    }

    static PointerBuffer toPointerBuffer(long[] ints) {
        PointerBuffer buf = BufferUtils.createPointerBuffer(ints.length).put(ints);
        buf.rewind();
        return buf;
    }

    public void flipBuffers(int index) {
        CL10GL.clEnqueueAcquireGLObjects(queue, pboMem[index], null, null);
        clFinish(queue);
        convertKernel.setArg(1, pboMem[index]);
        clEnqueueNDRangeKernel(queue, blurKernel, 1, null, kernel1DGlobalWorkSize, null, null, null);
        clEnqueueNDRangeKernel(queue, convertKernel, 1, null, kernel1DGlobalWorkSize, null, null, null);
        advanceRandomData();
        clFinish(queue);
        CL10GL.clEnqueueReleaseGLObjects(queue, pboMem[index], null, null);
        clEnqueueNDRangeKernel(queue, unsharpKernel, 1, null, kernel1DGlobalWorkSize, null, null, null);
        clFinish(queue);
    }
    
    public void generateRandomData() {
    	randomIdx = 0;
        randomDataLocal = BufferUtils.createLongBuffer(parameters.randomDataSize);
    	for(int i = 0; i < parameters.randomDataSize; i++) {
			randomSeed ^= (randomSeed << 21);
			randomSeed ^= (randomSeed >>> 35);
			randomSeed ^= (randomSeed << 4);
			randomDataLocal.put(randomSeed);
    	}
    	randomDataLocal.rewind();
        Util.checkCLError(clEnqueueWriteBuffer(queue, randomData, 1, 0, randomDataLocal, null, null));
        IntBuffer rPosBuf = BufferUtils.createIntBuffer(1);
        rPosBuf.put(randomIdx);
        Util.checkCLError(clEnqueueWriteBuffer(queue, randomDataPos, 1, 0, rPosBuf, null, null));    	
    }
    
    public void advanceRandomData() {
    	randomIdx += parameters.pixelNum;
    	if(randomIdx >= parameters.randomDataSize) {
    		generateRandomData();
    	}
    	else {
	        IntBuffer rPosBuf = BufferUtils.createIntBuffer(1);
	        rPosBuf.put(randomIdx);
	        Util.checkCLError(clEnqueueWriteBuffer(queue, randomDataPos, 1, 0, rPosBuf, null, null));     
    	}
    }
}

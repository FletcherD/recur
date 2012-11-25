//NOISE_DEFINE
__constant float2 windowCenter = {(float)WIDTH / 2.0, (float)HEIGHT / 2.0};
__constant float4 centerColor = {0.5, 0.5, 0.5, 0.0};
__constant float4 borderColor = {0.05, 0.03, 0.03, 0.0};
__constant float4 colorTransform[] = { {1.0, 0.0, 0.0, 0.0},
                                       {0.0, 1.0, 0.0, 0.0},
                                       {0.0, 0.0, 1.0, 0.0} };

__inline__ float erfinv(float x)
{
    float w, p;
    w = -log((1.0f-x)*(1.0f+x));
    if ( w < 5.000000f ) {
        w = w - 2.500000f;
        p =  2.81022636e-08f;
        p =  3.43273939e-07f + p*w;
        p =  -3.5233877e-06f + p*w;
        p = -4.39150654e-06f + p*w;
        p =   0.00021858087f + p*w;
        p =  -0.00125372503f + p*w;
        p =  -0.00417768164f + p*w;
        p =     0.246640727f + p*w;
        p =      1.50140941f + p*w;
    }
    else {
        w = sqrt(w) - 3.000000f;
        p = -0.000200214257f;
        p =  0.000100950558f + p*w;
        p =   0.00134934322f + p*w;
        p =  -0.00367342844f + p*w;
        p =   0.00573950773f + p*w;
        p =   -0.0076224613f + p*w;
        p =   0.00943887047f + p*w;
        p =      1.00167406f + p*w;
        p =      2.83297682f + p*w;
    }
    return p*x;
}

float4 randomIntToColor(const uint color, __constant float *gaussianLookup)
{
    float4 r;
    r.x = gaussianLookup[color&(1024-1)];
    r.y = gaussianLookup[(color>>10)&(1024-1)];
    r.z = gaussianLookup[(color>>20)&(1024-1)];
    return r;
}

float4 adjustColor(const float4 colorIn, const float4 randomColor)
{
    float4 color = ((colorIn - centerColor)) ;
    color = (color.x * colorTransform[0]) + (color.y * colorTransform[1]) + (color.z * colorTransform[2]);
    #ifdef NOISE
    	color += BRIGHTNESS + centerColor + randomColor;
    #else
    	color += BRIGHTNESS + centerColor;
    #endif

    return clamp(color, 0.0f, 1.0f);
}

kernel void unsharpMask(global const float4 *in, global float4 *out, __constant float *unsharpMatrix) {
    unsigned int xid = get_global_id(0);
    int x = xid%WIDTH;
    int y = xid/WIDTH;
    float4 color = {0.0f, 0.0f, 0.0f, 0.0f};
    for(int n = max(0, (MSIZE/2)-y); n < min(MSIZE, HEIGHT-y+(MSIZE/2)); n++) {
        int idx = (y+n-(MSIZE/2)) * WIDTH;
        for(int m = max(0, (MSIZE/2)-x); m < min(MSIZE, WIDTH-x+(MSIZE/2)); m++) {
            int newX = x+m-(MSIZE/2);
            float matrixEntry = unsharpMatrix[(n*MSIZE)+m];
            color += in[idx + newX] * matrixEntry;
            //color = mad(in[idx+newX], matrixEntry, color);
        }
    }
    out[xid] = color;
}

kernel void iterate(global const float4 *in, global float4 *out, global const int2 *positions, 
					global const float *blurMatrices, global const uint *randomData, __constant float *gaussianLookup) {
    unsigned int xid = get_global_id(0);
    int2 matrixPos = positions[xid];
    if(matrixPos.x == 0xFFFF)
    {
        #ifdef NOISE
            out[xid] = adjustColor(borderColor, randomIntToColor(randomData[xid], gaussianLookup));
        #else
            out[xid] = adjustColor(borderColor, 0);
        #endif
        return;
    }

    float4 color = {0.0, 0.0, 0.0, 0.0};
    unsigned int matrixIdx = xid * MSIZE * MSIZE;
    for(int m = 0; m < MSIZE; m++) {
        int x = matrixPos.x + m;
        if(x < 0 || x > WIDTH-1)
        {
            for(int n = 0; n < MSIZE; n++) {
                float matrixEntry = blurMatrices[matrixIdx+(n*MSIZE)+m];
                color += borderColor * matrixEntry;
            }
            continue;
        }
        for(int n = 0; n < MSIZE; n++) {
            int y = matrixPos.y + n;
            float matrixEntry = blurMatrices[matrixIdx+(n*MSIZE)+m];
            if(y < 0 || y > HEIGHT-1) {
                color += borderColor * matrixEntry;
                continue;
            }
            int newIdx = y * WIDTH + x;
            color += in[newIdx] * matrixEntry;
        }
    }
    #ifdef NOISE
        out[xid] = adjustColor(color, randomIntToColor(randomData[xid], gaussianLookup));
    #else
        out[xid] = adjustColor(color, 0);
    #endif
}

kernel void advanceRandomNumbers(global ulong *random) {
    uint xid = get_global_id(0);
    ulong x = random[xid];
    x ^= (x << 21);
    x ^= (x >> 35);
    x ^= (x << 4);
    random[xid] = x;
}

kernel void convertToPixelBuf(global const float4 *in, global float4 *out) {
	unsigned int xid = get_global_id(0);
	out[xid] = (in[xid]);
} 

kernel void createBlurMatrices(global float *blurMatrices, global int2 *positions, __constant float *rMatrix, __constant float *blurStd) {
    const float XCENT = WIDTH/2.0;
    const float YCENT = HEIGHT/2.0;

    unsigned int xid = get_global_id(0);
    float oldxPos = (float)(xid%WIDTH) - XCENT;
    float yPos = (float)(xid/WIDTH) - YCENT;
    float xPos = (oldxPos*rMatrix[1]) - (yPos*rMatrix[0]) + XCENT;
          yPos = (oldxPos*rMatrix[0]) + (yPos*rMatrix[1]) + YCENT;
    float scale = hypot(rMatrix[0], rMatrix[1]);

    if(xPos >= -(MSIZE) && xPos < WIDTH+(MSIZE) && yPos >= -(MSIZE) && yPos < HEIGHT+(MSIZE))
    {
        int2 matrixPos;
        matrixPos.x = round(xPos) - MSIZE/2;
        matrixPos.y = round(yPos) - MSIZE/2;
        positions[xid] = matrixPos;
        unsigned int matrixIdx = xid * MSIZE * MSIZE;
        float sum = 0;
        for(int m = 0; m < MSIZE; m++) {
            for(int n = 0; n < MSIZE; n++) {
                float x = matrixPos.x+m - xPos;
                float y = matrixPos.y+n - yPos;
                float entry = exp((x*x + y*y) / blurStd[0]);
                blurMatrices[matrixIdx+(n*MSIZE)+m] = entry;
                sum += entry;
            }
        }
        for(int m = 0; m < MSIZE; m++) {
            for(int n = 0; n < MSIZE; n++) {
                blurMatrices[matrixIdx+(n*MSIZE)+m] /= sum;
            }
        }
    }
    else
    {
        positions[xid].x = 0xFFFF;
    }
}

kernel void createGaussianLookup(global float *gaussianLookup, __constant float *stdev) {
    const int N = 1024;
    const float xLim = 0.990;
    unsigned int xid = get_global_id(0);
    float x = -xLim + (2*xLim/((float)(N-1)))*xid;
    gaussianLookup[xid] = sqrt(2.0f) * stdev[0] * erfinv(x);
}
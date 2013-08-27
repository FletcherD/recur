//NOISE_DEFINE
#define M_PI 3.14159265358979323846264338327950288419716939937510582
__constant float3 centerColor = {0.5f, 0.5f, 0.5f};

struct ColorParams
{
    float2 center;
    float3 brightness;
    float3 contrast;
    float3 borderColor;
    float3 borderColorGamma;
    float3 gamma;
    float noiseStd;
    float blurR;
    float unsharpR;
    float unsharpMag;
} ;

float3 randomIntToColor(const uint color, __constant float *gaussianLookup)
{
    float3 r;
    r.x = gaussianLookup[color&(1024-1)];
    r.y = gaussianLookup[(color>>10)&(1024-1)];
    r.z = gaussianLookup[(color>>20)&(1024-1)];
    return r;
}

float3 adjustColor(const float3 colorIn, const float3 randomColor, __constant struct ColorParams *cParams)
{
    float3 color = ((colorIn - centerColor)) ;
    color *= cParams->contrast;
    #ifdef NOISE
    	color += cParams->brightness + centerColor + randomColor;
    #else
    	color += cParams->brightness + centerColor;
    #endif

    return clamp(color, 0.0f, 1.0f);
}

kernel void gammaApply(global float3 *in, __constant struct ColorParams *cParams) {
    unsigned int xid = get_global_id(0);
    // pow() behaves strangely on some machines, native_powr() is faster and better behaved
    // max is there because this weirdly goes negative on some machines
    in[xid] = max( native_powr(in[xid], cParams->gamma), 0.0f);
}

__inline__ float3 gammaCorrect(const float3 in, __constant struct ColorParams *cParams) {
    return native_powr(in, 1.0f/(cParams->gamma));
}

kernel void iterate(global const float3 *in, global float3 *out, global const int2 *positions,
					global const float *blurMatrices, global const uint *randomData, __constant float *gaussianLookup, __constant struct ColorParams *cParams) {
    unsigned int xid = get_global_id(0);
    int2 matrixPos = positions[xid];
    if(matrixPos.x == 0xFFFF)
    {
        #ifdef NOISE
            out[xid] = adjustColor(cParams->borderColor, randomIntToColor(randomData[xid], gaussianLookup), cParams);
        #else
            out[xid] = adjustColor(cParams->borderColor, 0, cParams);
        #endif
        return;
    }

    float3 color = {0.0f, 0.0f, 0.0f};
    unsigned int matrixIdx = xid * MSIZE * MSIZE;
    for(int m = 0; m < MSIZE; m++) {
        int x = matrixPos.x + m;
        if(x < 0 || x > WIDTH-1)
        {
            for(int n = 0; n < MSIZE; n++) {
                float matrixEntry = blurMatrices[matrixIdx+(n*MSIZE)+m];
                color += cParams->borderColorGamma * matrixEntry;
            }
            continue;
        }
        for(int n = 0; n < MSIZE; n++) {
            int y = matrixPos.y + n;
            float matrixEntry = blurMatrices[matrixIdx+(n*MSIZE)+m];
            if(y < 0 || y > HEIGHT-1) {
                color += cParams->borderColorGamma * matrixEntry;
                continue;
            }
            int newIdx = y * WIDTH + x;
            color += in[newIdx] * matrixEntry;
        }
    }
    color = gammaCorrect(color, cParams);
    #ifdef NOISE
        out[xid] = adjustColor(color, randomIntToColor(randomData[xid], gaussianLookup), cParams);
    #else
        out[xid] = adjustColor(color, 0, cParams);
    #endif
}

kernel void unsharpMask(global const float3 *in, global float3 *out, __constant float *unsharpMatrix) {
    unsigned int xid = get_global_id(0);
    int x = xid%WIDTH;
    int y = xid/WIDTH;
    float3 color = {0.0f, 0.0f, 0.0f};
    for(int n = max(0, (MSIZE/2)-y); n < min(MSIZE, HEIGHT-y+(MSIZE/2)); n++) {
        int idx = (y+n-(MSIZE/2)) * WIDTH;
        for(int m = max(0, (MSIZE/2)-x); m < min(MSIZE, WIDTH-x+(MSIZE/2)); m++) {
            int newX = x+m-(MSIZE/2);
            float matrixEntry = unsharpMatrix[(n*MSIZE)+m];
            color += in[idx + newX] * matrixEntry;
        }
    }
    out[xid] = color;
}

///////    ITERATE ALL THE RANDOM NUMBERS    ///////

kernel void advanceRandomNumbers(global ulong *random) {
    uint xid = get_global_id(0);
    ulong x = random[xid];
    x ^= (x << 21);
    x ^= (x >> 35);
    x ^= (x << 4);
    random[xid] = x;
}

///////    MATRIX COMPUTATION FUNCTIONS    ///////

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
                float entry = exp((x*x + y*y) / -blurStd[0]);
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

__inline__ float chordArea(float r, float d)
{
    float area = pow(r,2.0)*acos(d/r);
    area -= d * sqrt(pow(r,2.0) - pow(d,2.0));
    return area;
}

kernel void createBokehMatrices(global float *blurMatrices, global int2 *positions, __constant float *rMatrix, __constant struct ColorParams *cParams) {
    unsigned int xid = get_global_id(0);
    float oldxPos = (float)(xid%WIDTH) - cParams->center.x;
    float yPos = (float)(xid/WIDTH) - cParams->center.y;
    float xPos = (oldxPos*rMatrix[1]) - (yPos*rMatrix[0]) + cParams->center.x;
          yPos = (oldxPos*rMatrix[0]) + (yPos*rMatrix[1]) + cParams->center.y;
    float pixelRadius = hypot(rMatrix[0], rMatrix[1])/2.0;

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
                float d = hypot(x, y);
                float entry = 0;
                if(d <= cParams->blurR - pixelRadius) {
                    entry = M_PI*pow(pixelRadius,2.0);
                } else if(d <= pixelRadius - cParams->blurR) {
                    entry = M_PI*pow(cParams->blurR,2.0);
                } else if(d < cParams->blurR + pixelRadius) {
                    float bSegHeight = pow(d,2.0) - pow(pixelRadius,2.0) + pow(cParams->blurR,2.0);
                    bSegHeight /= (2.0*d);
                    entry = chordArea(cParams->blurR,bSegHeight) + chordArea(pixelRadius,d-bSegHeight);
                } else {
                    blurMatrices[matrixIdx+(n*MSIZE)+m] = 0;
                    continue;
                }
                blurMatrices[matrixIdx+(n*MSIZE)+m] = entry;
                sum += entry;
            }
        }
        for(int m = 0; m < MSIZE; m++) {
            for(int n = 0; n < MSIZE; n++) {
                blurMatrices[matrixIdx+(n*MSIZE)+m] /= 0.785 * (hypot(rMatrix[0], rMatrix[1])) * M_PI*pow(cParams->blurR,2.0);
            }
        }
    }
    else
    {
        positions[xid].x = 0xFFFF;
    }
}

kernel void findParamOffsets(global int *offsets) {
    struct ColorParams cParams;
    int i = 0;
    offsets[i++] = (float*)(&(cParams.center)) - (float*)(&cParams);
    offsets[i++] = (float*)(&(cParams.brightness)) - (float*)(&cParams);
    offsets[i++] = (float*)(&(cParams.contrast)) - (float*)(&cParams);
    offsets[i++] = (float*)(&(cParams.borderColor)) - (float*)(&cParams);
    offsets[i++] = (float*)(&(cParams.borderColorGamma)) - (float*)(&cParams);
    offsets[i++] = (float*)(&(cParams.gamma)) - (float*)(&cParams);
    offsets[i++] = (float*)(&(cParams.noiseStd)) - (float*)(&cParams);
    offsets[i++] = (float*)(&(cParams.blurR)) - (float*)(&cParams);
    offsets[i++] = (float*)(&(cParams.unsharpR)) - (float*)(&cParams);
    offsets[i++] = (float*)(&(cParams.unsharpMag)) - (float*)(&cParams);
}

///////    GAUSSIAN FUNCTIONS    ////////

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

kernel void createGaussianLookup(global float *gaussianLookup, __constant float *stdev) {
    const int N = 1024;
    const float xLim = 0.990;
    unsigned int xid = get_global_id(0);
    float x = -xLim + (2*xLim/((float)(N-1)))*xid;
    gaussianLookup[xid] = sqrt(2.0f) * stdev[0] * erfinv(x);
}

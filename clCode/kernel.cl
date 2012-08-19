//NOISE_DEFINE
__constant float2 windowCenter = {(float)WIDTH / 2.0, (float)HEIGHT / 2.0};
__constant float4 centerColor = {(255.0/2.0), (255.0/2.0), (255.0/2.0), (255.0/2.0)};
__constant float4 borderColor = {0.0, 0.0, 0.0, 0.0};
__constant float4 colorTransform[] = {	{0.9, 0.05, 0.05, 0.0},
										{0.05, 0.9, 0.05, 0.0},
										{0.05, 0.05, 0.9, 0.0} };

float4 colorToFloat4(int color)
{
    uchar4 colorC = color;
    float4 r;
    r.x = (float)(color&0x0000FF);
    r.y = (float)((color&0x00FF00)>>8);
    r.z = (float)((color&0xFF0000)>>16);
    return r;
}
int float4ToColor(float4 color)
{
    uchar4 r;
    r.x = (uchar)(max(0, min(255, (int)(round(color.x)))));
    r.y = (uchar)(max(0, min(255, (int)(round(color.y)))));
    r.z = (uchar)(max(0, min(255, (int)(round(color.z)))));
    return (int)r;
}

float4 adjustColor(float4 color, uint randomInt)
{
    color = ((color - centerColor)) ;
    color = (color.x * colorTransform[0]) + (color.y * colorTransform[1]) + (color.z * colorTransform[2]);
    #ifdef NOISE
        float4 noise = colorToFloat4(randomInt);
    	color += BRIGHTNESS + centerColor + ((noise - centerColor) * NOISEPARAM);
    #else
    	color += BRIGHTNESS + centerColor;
    #endif    

    return color;
}

kernel void unsharpMask(global const float4 *in, global float4 *out, __constant float *unsharpMatrix) {
    unsigned int xid = get_global_id(0);
    int x = xid%WIDTH;
    int y = xid/WIDTH;
    float4 color = {0.0, 0.0, 0.0, 0.0};
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

kernel void iterate(global const float4 *in, global float4 *out, global const int2 *positions, 
					global const float *blurMatrices, __constant uint* randomData, __constant uint* randomPos) {
    unsigned int xid = get_global_id(0);
    int2 matrixPos = positions[xid];
    if(matrixPos.x == 0xFFFF)
    {
        out[xid] = borderColor;
        return;
    }

    float4 color = {0.0, 0.0, 0.0, 0.0};
    unsigned int matrixIdx = xid * MSIZE * MSIZE;
    for(int m = 0; m < MSIZE; m++) {
        int x = matrixPos.x + m;
        if(x < 0 || x > WIDTH-1) { continue; }
        for(int n = 0; n < MSIZE; n++) {
            int y = matrixPos.y + n;
            if(y < 0 || y > HEIGHT-1) { continue; }
            int newIdx = y * WIDTH + x;
            float matrixEntry = blurMatrices[matrixIdx+(n*MSIZE)+m];
            color += in[newIdx] * matrixEntry;
        }
    }
    out[xid] = adjustColor(color, randomData[randomPos[0] + xid]);
}

kernel void convertToPixelBuf(global const float4 *in, global int *out) {
	unsigned int xid = get_global_id(0);
	out[xid] = float4ToColor(in[xid]);
} 

kernel void createBlurMatrices(global float *blurMatrices, global int2 *positions, __constant float *rMatrix, __constant float *blurStd) {
    const float XCENT = WIDTH/2.0;
    const float YCENT = HEIGHT/2.0;

    unsigned int xid = get_global_id(0);
    float oldxPos = (float)(xid%WIDTH) - XCENT;
    float yPos = (float)(xid/WIDTH) - YCENT;
    float xPos = (oldxPos*rMatrix[1]) - (yPos*rMatrix[0]) + XCENT;
          yPos = (oldxPos*rMatrix[0]) + (yPos*rMatrix[1]) + YCENT;

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

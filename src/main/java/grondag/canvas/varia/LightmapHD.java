package grondag.canvas.varia;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import grondag.canvas.Canvas;
import grondag.canvas.apiimpl.QuadViewImpl;
import grondag.canvas.apiimpl.util.AoFaceData;
import net.minecraft.util.math.MathHelper;

public class LightmapHD {
    static final int TEX_SIZE = 512;
    static final int LIGHTMAP_SIZE = 4;
    static final int RADIUS = LIGHTMAP_SIZE / 2;
    static final int LIGHTMAP_PIXELS = LIGHTMAP_SIZE * LIGHTMAP_SIZE;
    static final int IDX_SIZE = 512 / LIGHTMAP_SIZE;
    static final int MAX_COUNT = IDX_SIZE * IDX_SIZE;
    // UGLY - consider making this a full unsigned short
    // for initial pass didn't want to worry about signed value mistakes
    /** Scale of texture units sent to shader. Shader should divide by this. */
    static final int BUFFER_SCALE = 0x8000;
    static final int UNITS_PER_PIXEL = BUFFER_SCALE / TEX_SIZE;
    static final float TEXTURE_TO_BUFFER = (float) BUFFER_SCALE / TEX_SIZE;
    
    private static final LightmapHD[] maps = new LightmapHD[MAX_COUNT];
    
    private static final AtomicInteger nextIndex = new AtomicInteger();
    
    public static void forceReload() {
        nextIndex.set(0);
        MAP.clear();
    }
    
    public static void forEach(Consumer<LightmapHD> consumer) {
        final int limit = Math.min(MAX_COUNT, nextIndex.get());
        for(int i = 0; i < limit; i++) {
            consumer.accept(maps[i]);
        }
    }
    
    private static class Key {
        private int[] light = new int[LIGHTMAP_PIXELS];
        private int hashCode;
        
        Key() {
        }

        Key(int[] light) {
            System.arraycopy(light, 0, this.light, 0, LIGHTMAP_PIXELS);
            computeHash();
        }
        
        /**
         * Call after mutating {@link #light}
         */
        void computeHash() {
            this.hashCode = Arrays.hashCode(light);
        }
        
        @Override
        public boolean equals(Object other) {
            if(other == null || !(other instanceof Key)) {
                return false;
            }
            int[] otherLight = ((Key)other).light;
            return Arrays.equals(light, otherLight);
        }
        
        @Override
        public int hashCode() {
            return hashCode;
        }
    }
    
    static final ThreadLocal<Key> TEMPLATES = ThreadLocal.withInitial(Key::new);
    
    static final ConcurrentHashMap<Key, LightmapHD> MAP = new ConcurrentHashMap<>();
    
    public static LightmapHD findBlock(AoFaceData faceData) {
        return find(faceData, LightmapHD::mapBlock);
    }
    
    public static LightmapHD findSky(AoFaceData faceData) {
        return find(faceData, LightmapHD::mapSky);
    }
    
    private static void mapBlock(AoFaceData faceData, int[] search) {
        final float s0 = input(faceData.light0);
        final float s1 = input(faceData.light1);
        final float s2 = input(faceData.light2);
        final float s3 = input(faceData.light3);
        
        final float c0 = input(faceData.cLight0);
        final float c1 = input(faceData.cLight1);
        final float c2 = input(faceData.cLight2);
        final float c3 = input(faceData.cLight3);
        
        final float center = input(faceData.lightCenter);
        compute(search, center, s0, s1, s2, s3, c0, c1, c2, c3);
    }
    
    private static LightmapHD find(AoFaceData faceData, BiConsumer<AoFaceData, int[]> mapper) {
        Key key = TEMPLATES.get();
        int[] search = key.light;
        
        mapper.accept(faceData, search);

        key.computeHash();
        
        LightmapHD result = MAP.get(key);
        
        if(result == null) {
            // create new key object to avoid putting threadlocal into map
            key = new Key(search);
            result = MAP.computeIfAbsent(key, k -> new LightmapHD(k.light));
        }
        
        return result;
    }
    
    private static float input(int b) {
        b &= 0xFF;
        if(b > 240) {
            b = 240;
        }
        return b / 16f;
    }
    
    private static void mapSky(AoFaceData faceData, int[] search) {
        final float s0 = input(faceData.light0 >>> 16);
        final float s1 = input(faceData.light1 >>> 16);
        final float s2 = input(faceData.light2 >>> 16);
        final float s3 = input(faceData.light3 >>> 16);
        
        final float c0 = input(faceData.cLight0 >>> 16);
        final float c1 = input(faceData.cLight1 >>> 16);
        final float c2 = input(faceData.cLight2 >>> 16);
        final float c3 = input(faceData.cLight3 >>> 16);
        
        final float center = input(faceData.lightCenter >>> 16);
        
        //TODO: remove
//        if(center == 15 && s0 == 15 && s1 == 15 && s2 == 15 && s3 == 15 && c0 == 15 && c1 == 15 && c2 == 15 && c3 == 15) {
//            System.out.println("boop");
//        }
        
        compute(search, center, s0, s1, s2, s3, c0, c1, c2, c3);
    }
    
    public final int uMinImg;
    public final int vMinImg;
    public final int[] light;
    
    private LightmapHD(int[] light) {
        final int index = nextIndex.getAndIncrement();
        final int s = index % IDX_SIZE;
        final int t = index / IDX_SIZE;
        uMinImg = s * LIGHTMAP_SIZE;
        vMinImg = t * LIGHTMAP_SIZE;
        this.light = new int[LIGHTMAP_PIXELS];
        System.arraycopy(light, 0, this.light, 0, LIGHTMAP_PIXELS);
        
        if(index >= MAX_COUNT) {
            //TODO: put back
            //assert false : "Out of lightmap space.";
            Canvas.LOG.info("Out of lightmap space for index = " + index);
            return;
        }
        
        maps[index] = this;
        
        SmoothLightmapTexture.instance().setDirty();
    }
    
    private static void compute(int[] light, float center, 
            float s0, float s1, float s2, float s3,
            float c0, float c1, float c2, float c3) {
        /**
         * Note that Ao data order is different from vertex order.
         * We will need to remap that here unless/until Ao data is simplified.
         * wc0 = s0 s3 c1
         * wc1 = s0 s2 c0
         * wc2 = s1 s2 c2
         * wc3 = s1 s3 c3
         * 
         * c1 s0 c0 s2 c2 s1 c3 s3
         * w0    w1    w2    w3
         * u0v0  u1v0  u1v1  u0v1
         */
        
        // quadrant 0, 0
        light[index(1, 1)] = output(inside(center, s0, s3, c1));
        light[index(0, 0)] = output(corner(center, s0, s3, c1));
        light[index(0, 1)] = output(side(center, s0, s3, c1 ));
        light[index(1, 0)] = output(side(center, s3, s0, c1 ));
        
        // quadrant 1, 0
        light[index(2, 1)] = output(inside(center, s0, s2, c0));
        light[index(3, 0)] = output(corner(center, s0, s2, c0));
        light[index(2, 0)] = output(side(center, s2, s0, c0 ));
        light[index(3, 1)] = output(side(center, s0, s2, c0 ));
        
        // quadrant 1, 1
        light[index(2, 2)] = output(inside(center, s1, s2, c2));
        light[index(3, 3)] = output(corner(center, s1, s2, c2));
        light[index(3, 2)] = output(side(center, s1, s2, c2 ));
        light[index(2, 3)] = output(side(center, s2, s1, c2 ));
        
        // quadrant 0, 1
        light[index(1, 2)] = output(inside(center, s1, s3, c3));
        light[index(0, 3)] = output(corner(center, s1, s3, c3));
        light[index(1, 3)] = output(side(center, s3, s1, c3 ));
        light[index(0, 2)] = output(side(center, s1, s3, c3 ));
        
        //TODO: remove
//      if(center == 0 && s0 == 0 && s1 == 0 && s2 == 0 && s3 == 0 && c0 == 0 && c1 == 0 && c2 == 0 && c3 == 0) {
//          for(int i : light) {
//              if(i != 8)
//              System.out.println("boop");
//          }
//      }
//        for(int i = 0; i < LIGHTMAP_PIXELS; i++) {
//            light[i] = 25;
//        }
    }
    
    private static float pclamp(float in) {
        return in < 0f ? 0f : in;
    }
    
    public static int index(int u, int v) {
        return v * LIGHTMAP_SIZE + u;
    }
    
    //FIX: is 1 right?
    private static int output(float in) {
        if(in < 1) {
            in = 1;
        } else if(in > 15) {
            in = 15;
        }
        int result = Math.round(in * 16f);
        
        return 8 + result;
    }
    
    public int coord(QuadViewImpl q, int i) {
        //PERF could compress coordinates sent to shader by 
        // sending lightmap/shademap index with same uv for each
        // would probably save 2 bytes - send 2 + 3 shorts instead of 6
        // or put each block/sky/ao combination in a texture and send 4 shorts...
        // 2 for uv and 2 to lookup the combinations
//        float u = uMinImg + 0.5f + q.u[i] * (LIGHTMAP_SIZE - 1);
//        float v = vMinImg + 0.5f + q.v[i] * (LIGHTMAP_SIZE - 1);
        int u = Math.round(uMinImg * TEXTURE_TO_BUFFER + 1 + q.u[i] * (LIGHTMAP_SIZE * TEXTURE_TO_BUFFER - 2));
        int v = Math.round(vMinImg * TEXTURE_TO_BUFFER + 1 + q.v[i] * (LIGHTMAP_SIZE * TEXTURE_TO_BUFFER - 2));
        return u | (v << 16);
    }
    
    private static float max(float a, float b, float c, float d) {
        return Math.max(Math.max(a, b), Math.max(c, d));
    }
    
    private static float inside(float self, float a, float b, float corner) {
        //UGLY: find symmetrical derivation
        if(self == a && self == b && self == corner) {
            return corner;
        }
        return max(self, pclamp(a - 1f), pclamp(b - 1f), pclamp(corner - 1.41f));
    }
    
    private static float sideInner(float self, float far, float near, float corner) {
        //UGLY: find symmetrical derivation
        if(self == far && self == near && self == corner) {
            return self;
        }
        return max(pclamp(self - .33f), pclamp(far - 1.05f), pclamp(near - .67f), pclamp(corner - 1.2f));
    }
    
    private static float side(float self, float far, float near, float corner) {
        //UGLY: find symmetrical derivation
        if(self == far && self == near && self == corner) {
            return self;
        }
        float s = sideInner(self, far, near, corner);
        float t = sideInner(near, corner, self, far);
        return (s + t) * 0.5f;
    }
    
    static final float CELL_DISTANCE = RADIUS * 2 - 1;
    static final float INVERSE_CELL_DISTANCE = 1f / CELL_DISTANCE;
    
    private static int pixelDist(int c) {
        return c >= RADIUS ? c - RADIUS : RADIUS - 1 - c;
    }
    
    private static float dist(int u, int v) {
        float a = pixelDist(u);
        float b = pixelDist(v);
        return MathHelper.sqrt((a * a + b * b)) * INVERSE_CELL_DISTANCE;
    }
    
    static final float SELF_CORNER_LOSS = dist(0, 0);
    static final float DIAG_CORNER_LOSS = dist(-1, -1);
    static final float SIDE_CORNER_LOSS = dist(-1, 0);
    

    
    private static int side(int c) {
        return c >= RADIUS ? 1 : -1;
    }
    
    private static float cornerInner(float self, float corner, float uVal, float vVal) {
        return max(pclamp(self - SELF_CORNER_LOSS), pclamp(uVal - SIDE_CORNER_LOSS), pclamp(vVal - SIDE_CORNER_LOSS), pclamp(corner - DIAG_CORNER_LOSS));
    }
    
    private static float corner(float self, float uVal, float vVal, float corner) {
        float a = cornerInner(self, corner, uVal, vVal);
        float b = cornerInner(corner, self, uVal, vVal);
        float c = cornerInner(uVal, vVal, corner, self);
        float d = cornerInner(vVal, uVal, corner, self);
        return mean(a, b, c, d);
    }
    
    private static float mean(float a, float b, float c, float d) {
        return (a + b + c + d) * 0.25f;
    }
    

}

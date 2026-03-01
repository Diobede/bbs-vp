package mchorse.bbs_mod.cubic.render.vao;

import net.minecraft.client.render.VertexConsumer;

import java.util.List;

/**
 * Collects block model vertices emitted via VertexConsumer and converts quads to triangles,
 * producing arrays suitable for {@link ModelVAO} upload.
 */
public class StructureVAOCollector implements VertexConsumer
{
    private final FloatBuf positions = new FloatBuf(8192);
    private final FloatBuf normals = new FloatBuf(8192);
    private final FloatBuf texCoords = new FloatBuf(8192);
    private final FloatBuf tangents = new FloatBuf(8192);

    private final Vtx[] quad = new Vtx[4];
    private int quadIndex = 0;

    /* working per-vertex state until next() */
    private float vx, vy, vz;
    private float vnx, vny, vnz;
    private float vu, vv;
    private boolean computeTangents = true;
    private final float[] tangentTmp = new float[3];

    public StructureVAOCollector()
    {
        for (int i = 0; i < 4; i++)
        {
            this.quad[i] = new Vtx();
        }
    }

    public void setComputeTangents(boolean computeTangents)
    {
        this.computeTangents = computeTangents;
    }

    @Override
    public VertexConsumer vertex(double x, double y, double z)
    {
        this.vx = (float) x;
        this.vy = (float) y;
        this.vz = (float) z;
        return this;
    }

    @Override
    public VertexConsumer color(int red, int green, int blue, int alpha)
    {
        /* Per-vertex color is not used; global color is provided via shader attribute. */
        return this;
    }

    @Override
    public VertexConsumer texture(float u, float v)
    {
        this.vu = u;
        this.vv = v;
        return this;
    }

    @Override
    public VertexConsumer overlay(int u, int v)
    {
        /* Overlay provided via shader attribute; ignore per-vertex overlay. */
        return this;
    }

    @Override
    public VertexConsumer light(int u, int v)
    {
        /* Lightmap provided via shader attribute; ignore per-vertex light. */
        return this;
    }

    @Override
    public VertexConsumer normal(float x, float y, float z)
    {
        this.vnx = x;
        this.vny = y;
        this.vnz = z;
        return this;
    }

    @Override
    public void next()
    {
        Vtx v = this.quad[this.quadIndex];
        v.x = this.vx; v.y = this.vy; v.z = this.vz;
        v.nx = this.vnx; v.ny = this.vny; v.nz = this.vnz;
        v.u = this.vu; v.v = this.vv;

        this.quadIndex++;

        if (this.quadIndex == 4)
        {
            /* Triangulate quad: (0,1,2) and (0,2,3) */
            this.emitTriangle(this.quad[0], this.quad[1], this.quad[2]);
            this.emitTriangle(this.quad[0], this.quad[2], this.quad[3]);
            this.quadIndex = 0;
        }
    }

    private void emitTriangle(Vtx a, Vtx b, Vtx c)
    {
        /* Ensure normal is computed if missing */
        if (this.isZero(a.nx, a.ny, a.nz) || this.isZero(b.nx, b.ny, b.nz) || this.isZero(c.nx, c.ny, c.nz))
        {
            float[] n = this.computeTriangleNormal(a, b, c);
            
            /* Apply calculated normal to all vertices for flat shading (since we don't have smooth groups) */
            /* If we want smooth shading, we would need to average normals, but block models are usually flat. */
            
            if (this.isZero(a.nx, a.ny, a.nz)) { a.nx = n[0]; a.ny = n[1]; a.nz = n[2]; }
            if (this.isZero(b.nx, b.ny, b.nz)) { b.nx = n[0]; b.ny = n[1]; b.nz = n[2]; }
            if (this.isZero(c.nx, c.ny, c.nz)) { c.nx = n[0]; c.ny = n[1]; c.nz = n[2]; }
            
            /* 
             * DARK FACE FIX:
             * Some models might have inverted winding order or just need simple lighting.
             * If normals are pointing "in", they will be black.
             * For typical block rendering, we can try to enforce "up/out" relative to face center?
             * 
             * Actually, simpler fix: Just ensure normals are always unit length and non-zero.
             * The previous fix did that.
             * 
             * If specific faces are still dark, it might be due to the specific lighting shader
             * expecting positive normals relative to view or light source.
             * 
             * Let's double check if we can improve tangent calculation as well,
             * as bad tangents can mess up normal mapping in shaders.
             */
        }
        
        /* 
         * Final Safety: If normal is still zero (collinear vertices?), force UP vector 
         * to avoid NaN/black in shader. 
         */
        if (this.isZero(a.nx, a.ny, a.nz)) { a.nx = 0F; a.ny = 1F; a.nz = 0F; }
        if (this.isZero(b.nx, b.ny, b.nz)) { b.nx = 0F; b.ny = 1F; b.nz = 0F; }
        if (this.isZero(c.nx, c.ny, c.nz)) { c.nx = 0F; c.ny = 1F; c.nz = 0F; }

        this.positions.add3(a.x, a.y, a.z);
        this.positions.add3(b.x, b.y, b.z);
        this.positions.add3(c.x, c.y, c.z);

        this.normals.add3(a.nx, a.ny, a.nz);
        this.normals.add3(b.nx, b.ny, b.nz);
        this.normals.add3(c.nx, c.ny, c.nz);

        this.texCoords.add2(a.u, a.v);
        this.texCoords.add2(b.u, b.v);
        this.texCoords.add2(c.u, c.v);

        if (this.computeTangents)
        {
            float[] t = this.computeTriangleTangent(a, b, c);
            this.tangents.add4(t[0], t[1], t[2], 1F);
            this.tangents.add4(t[0], t[1], t[2], 1F);
            this.tangents.add4(t[0], t[1], t[2], 1F);
        }
        else
        {
            this.tangents.add4(1F, 0F, 0F, 1F);
            this.tangents.add4(1F, 0F, 0F, 1F);
            this.tangents.add4(1F, 0F, 0F, 1F);
        }
    }

    private boolean isZero(float x, float y, float z)
    {
        return x == 0F && y == 0F && z == 0F;
    }

    private float[] computeTriangleNormal(Vtx a, Vtx b, Vtx c)
    {
        float x1 = b.x - a.x, y1 = b.y - a.y, z1 = b.z - a.z;
        float x2 = c.x - a.x, y2 = c.y - a.y, z2 = c.z - a.z;
        
        /* Cross product: (b - a) x (c - a) */
        float nx = y1 * z2 - z1 * y2;
        float ny = z1 * x2 - x1 * z2;
        float nz = x1 * y2 - y1 * x2;
        
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        
        if (len < 1e-8F)
        {
            return new float[] { 0F, 1F, 0F };
        }
        
        /* 
         * Fix dark faces: Check if normal is pointing inwards or inverted.
         * For standard block models, normals should align with cardinal axes or be consistent.
         * If the winding order is CW instead of CCW (or vice versa depending on GL state),
         * the normal might be inverted.
         * 
         * However, without a reference center, we can't easily determine "outward".
         * But usually, vanilla models are CCW. If a specific face is dark, it might be 
         * because the light calculation expects a specific normal direction.
         * 
         * In many cases, simply normalizing is enough. If it's still dark, 
         * it might be a specific block model issue. 
         * 
         * Let's ensure the normal is normalized.
         */
        
        return new float[] { nx / len, ny / len, nz / len };
    }

    private float[] computeTriangleTangent(Vtx a, Vtx b, Vtx c)
    {
        float x1 = b.x - a.x, y1 = b.y - a.y, z1 = b.z - a.z;
        float x2 = c.x - a.x, y2 = c.y - a.y, z2 = c.z - a.z;
        float u1 = b.u - a.u, v1 = b.v - a.v;
        float u2 = c.u - a.u, v2 = c.v - a.v;

        float denom = (u1 * v2 - u2 * v1);
        if (Math.abs(denom) < 1e-8F)
        {
            float len = (float) Math.sqrt(x1 * x1 + y1 * y1 + z1 * z1);
            if (len < 1e-8F)
            {
                this.tangentTmp[0] = 1F;
                this.tangentTmp[1] = 0F;
                this.tangentTmp[2] = 0F;
                return this.tangentTmp;
            }
            this.tangentTmp[0] = x1 / len;
            this.tangentTmp[1] = y1 / len;
            this.tangentTmp[2] = z1 / len;
            return this.tangentTmp;
        }

        float f = 1.0F / denom;
        float tx = f * (v2 * x1 - v1 * x2);
        float ty = f * (v2 * y1 - v1 * y2);
        float tz = f * (v2 * z1 - v1 * z2);

        float len = (float) Math.sqrt(tx * tx + ty * ty + tz * tz);
        if (len < 1e-8F)
        {
            this.tangentTmp[0] = 1F;
            this.tangentTmp[1] = 0F;
            this.tangentTmp[2] = 0F;
            return this.tangentTmp;
        }
        this.tangentTmp[0] = tx / len;
        this.tangentTmp[1] = ty / len;
        this.tangentTmp[2] = tz / len;
        return this.tangentTmp;
    }

    @Override
    public void fixedColor(int red, int green, int blue, int alpha)
    {
        /* no-op */
    }

    @Override
    public void unfixColor()
    {
        /* no-op */
    }

    public ModelVAOData toData()
    {
        float[] v = this.positions.toArray();
        float[] n = this.normals.toArray();
        float[] t = this.tangents.toArray();
        float[] uv = this.texCoords.toArray();
        return new ModelVAOData(v, n, t, uv);
    }

    private static float[] toArray(List<Float> list)
    {
        float[] arr = new float[list.size()];
        for (int i = 0; i < arr.length; i++)
        {
            arr[i] = list.get(i);
        }
        return arr;
    }

    private static final class FloatBuf
    {
        float[] data;
        int size;

        FloatBuf(int initial)
        {
            this.data = new float[Math.max(16, initial)];
            this.size = 0;
        }

        void ensure(int add)
        {
            int need = this.size + add;
            if (need > this.data.length)
            {
                int cap = Math.max(need, this.data.length + (this.data.length >>> 1));
                float[] n = new float[cap];
                System.arraycopy(this.data, 0, n, 0, this.size);
                this.data = n;
            }
        }

        void add(float v)
        {
            this.ensure(1);
            this.data[this.size++] = v;
        }

        void add3(float a, float b, float c)
        {
            this.ensure(3);
            this.data[this.size++] = a;
            this.data[this.size++] = b;
            this.data[this.size++] = c;
        }

        void add2(float a, float b)
        {
            this.ensure(2);
            this.data[this.size++] = a;
            this.data[this.size++] = b;
        }

        void add4(float a, float b, float c, float d)
        {
            this.ensure(4);
            this.data[this.size++] = a;
            this.data[this.size++] = b;
            this.data[this.size++] = c;
            this.data[this.size++] = d;
        }

        float[] toArray()
        {
            float[] out = new float[this.size];
            System.arraycopy(this.data, 0, out, 0, this.size);
            return out;
        }
    }

    private static class Vtx
    {
        float x, y, z;
        float nx, ny, nz;
        float u, v;
    }
}

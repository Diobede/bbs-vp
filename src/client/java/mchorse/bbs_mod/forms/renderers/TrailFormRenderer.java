package mchorse.bbs_mod.forms.renderers;

import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.camera.Camera;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.forms.ITickable;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.TrailForm;
import mchorse.bbs_mod.graphics.Draw;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.ui.framework.UIContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class TrailFormRenderer extends FormRenderer<TrailForm> implements ITickable
{
    private int tick;
    private final Map<FormRenderType, ArrayDeque<Trail>> record = new HashMap<>();
    private final ArrayDeque<Trail> pool = new ArrayDeque<>();

    private final Vector4f tempTop = new Vector4f();
    private final Vector4f tempBottom = new Vector4f();
    private final Matrix4f camInverse = new Matrix4f();

    public TrailFormRenderer(TrailForm form)
    {
        super(form);
    }

    @Override
    protected void renderInUI(UIContext context, int x1, int y1, int x2, int y2)
    {
        Texture texture = context.render.getTextures().getTexture(this.form.texture.get());

        float min = Math.min(texture.width, texture.height);
        int ow = (x2 - x1) - 4;
        int oh = (y2 - y1) - 4;

        int w = (int) ((texture.width / min) * ow);
        int h = (int) ((texture.height / min) * ow);

        int x = x1 + (ow - w) / 2 + 2;
        int y = y1 + (oh - h) / 2 + 2;

        context.batcher.fullTexturedBox(texture, x, y, w, h);
    }

    @Override
    protected void render3D(FormRenderingContext context)
    {
        super.render3D(context);

        if (BBSRendering.isIrisShadowPass() || context.type == FormRenderType.ITEM_INVENTORY)
        {
            return;
        }

        if (context.modelRenderer || context.ui)
        {
            MatrixStack stack = context.stack;
            float scale = BBSSettings.axesScale.get();
            float axisSize = 1F;
            float axisOffset = 0.01F;
            float outlineSize = 1.01F;
            float outlineOffset = 0.02F;

            axisOffset *= scale;
            outlineOffset *= scale;

            BufferBuilder builder = Tessellator.getInstance().getBuffer();

            builder.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

            Draw.fillBox(builder, stack, -outlineOffset, -outlineSize, -outlineOffset, outlineOffset, outlineSize, outlineOffset, 0, 0, 0);
            Draw.fillBox(builder, stack, -axisOffset, -axisSize, -axisOffset, axisOffset, axisSize, axisOffset, 0, 1, 0);

            RenderSystem.setShader(GameRenderer::getPositionColorProgram);
            RenderSystem.disableDepthTest();

            BufferRenderer.drawWithGlobalProgram(builder.end());

            return;
        }

        if (!BBSRendering.isRenderingWorld())
        {
            return;
        }

        MatrixStack stack = context.stack;
        this.camInverse.set(RenderSystem.getInverseViewRotationMatrix());

        Camera camera = context.camera;
        double baseX = camera.position.x;
        double baseY = camera.position.y;
        double baseZ = camera.position.z;

        float current = (float) this.tick + context.transition;
        ArrayDeque<Trail> trails = this.record.computeIfAbsent(context.type, (k) -> new ArrayDeque<>());

        if (!this.form.paused.get())
        {
            Matrix4f modelView = stack.peek().getPositionMatrix();

            this.tempTop.set(0F, 1F, 0F, 1F);
            this.tempBottom.set(0F, -1F, 0F, 1F);

            modelView.transform(this.tempTop);
            modelView.transform(this.tempBottom);
            this.camInverse.transform(this.tempTop);
            this.camInverse.transform(this.tempBottom);

            this.tempTop.mul(1F / this.tempTop.w);
            this.tempBottom.mul(1F / this.tempBottom.w);

            Trail record = this.getTrail();

            record.tick = current;
            record.top.set(this.tempTop.x + baseX, this.tempTop.y + baseY, this.tempTop.z + baseZ);
            record.bottom.set(this.tempBottom.x + baseX, this.tempBottom.y + baseY, this.tempBottom.z + baseZ);

            double dx = this.tempTop.x - this.tempBottom.x;
            double dy = this.tempTop.y - this.tempBottom.y;
            double dz = this.tempTop.z - this.tempBottom.z;

            record.stop = dx * dx + dy * dy + dz * dz < 1.0E-4D;

            trails.addLast(record);
        }

        boolean loop = this.form.loop.get();
        float length = this.form.length.get();
        float end = current - length;
        boolean render = false;
        boolean lastStop = true;

        while (!trails.isEmpty() && trails.peekFirst().tick < end)
        {
            this.pool.add(trails.removeFirst());
        }

        for (Trail trail : trails)
        {
            render |= !trail.stop && !lastStop;
            lastStop = trail.stop;
        }

        if (!render || trails.size() <= 1 || !(length > 0.001D))
        {
            return;
        }

        BBSModClient.getTextures().bindTexture(this.form.texture.get());

        stack.push();

        Trail last = null;
        Trail trail;
        BufferBuilder builder = Tessellator.getInstance().getBuffer();
        Matrix4f m = stack.peek().getPositionMatrix();

        m.set(this.camInverse);
        m.invert();

        builder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);

        for (Iterator<Trail> it = trails.iterator(); it.hasNext(); last = trail)
        {
            trail = it.next();

            if (last != null && !last.stop && !trail.stop)
            {
                double x1 = trail.top.x - baseX;
                double x2 = trail.bottom.x - baseX;
                double x3 = last.bottom.x - baseX;
                double x4 = last.top.x - baseX;

                double y1 = trail.top.y - baseY;
                double y2 = trail.bottom.y - baseY;
                double y3 = last.bottom.y - baseY;
                double y4 = last.top.y - baseY;

                double z1 = trail.top.z - baseZ;
                double z2 = trail.bottom.z - baseZ;
                double z3 = last.bottom.z - baseZ;
                double z4 = last.top.z - baseZ;

                if (loop)
                {
                    float u1 = trail.tick / length;
                    float u2 = last.tick / length;

                    builder.vertex(m, (float) x1, (float) y1, (float) z1).texture(u1, 0F).next();
                    builder.vertex(m, (float) x2, (float) y2, (float) z2).texture(u1, 1F).next();
                    builder.vertex(m, (float) x3, (float) y3, (float) z3).texture(u2, 1F).next();
                    builder.vertex(m, (float) x4, (float) y4, (float) z4).texture(u2, 0F).next();
                    /* Other side */
                    builder.vertex(m, (float) x4, (float) y4, (float) z4).texture(u2, 0F).next();
                    builder.vertex(m, (float) x3, (float) y3, (float) z3).texture(u2, 1F).next();
                    builder.vertex(m, (float) x2, (float) y2, (float) z2).texture(u1, 1F).next();
                    builder.vertex(m, (float) x1, (float) y1, (float) z1).texture(u1, 0F).next();
                }
                else
                {
                    float u1 = (current - trail.tick) / length;
                    float u2 = (current - last.tick) / length;

                    builder.vertex(m, (float) x1, (float) y1, (float) z1).texture(u1, 0F).next();
                    builder.vertex(m, (float) x2, (float) y2, (float) z2).texture(u1, 1F).next();
                    builder.vertex(m, (float) x3, (float) y3, (float) z3).texture(u2, 1F).next();
                    builder.vertex(m, (float) x4, (float) y4, (float) z4).texture(u2, 0F).next();
                    /* Other side */
                    builder.vertex(m, (float) x4, (float) y4, (float) z4).texture(u2, 0F).next();
                    builder.vertex(m, (float) x3, (float) y3, (float) z3).texture(u2, 1F).next();
                    builder.vertex(m, (float) x2, (float) y2, (float) z2).texture(u1, 1F).next();
                    builder.vertex(m, (float) x1, (float) y1, (float) z1).texture(u1, 0F).next();
                }
            }
            else
            {
                length = current - trail.tick;
            }
        }

        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableBlend();
        BufferRenderer.drawWithGlobalProgram(builder.end());
        RenderSystem.enableDepthTest();

        stack.pop();
    }

    @Override
    public void tick(IEntity entity)
    {
        this.tick += 1;
    }

    private Trail getTrail()
    {
        if (this.pool.isEmpty())
        {
            Trail trail = new Trail();

            trail.top = new Vector3d();
            trail.bottom = new Vector3d();

            return trail;
        }

        return this.pool.removeLast();
    }

    public static class Trail
    {
        public float tick;
        public Vector3d top;
        public Vector3d bottom;
        public boolean stop;
    }
}
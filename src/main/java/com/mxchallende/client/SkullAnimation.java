package com.mxchallende.client;

import java.util.Locale;
import java.util.Random;

import org.joml.Matrix4f;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mxchallende.Config;
import com.mxchallende.MXChallenge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;

/**
 * Animación épica de la calavera (cliente). Reutiliza las 80 texturas del datapack:
 *   frames  1-20  entrada (la calavera crece)
 *   frames 21-66  bucle (maracas)
 *   frames 67-80  salida (se funde a negro)
 * y le suma: destello + impacto con sacudida, rayos giratorios, resplandor aditivo rojo,
 * viñeta roja, brasas, latido y barras de cine. Solo la calavera, sin texto.
 */
public final class SkullAnimation {
    // ---- línea de tiempo (ticks, 20 por segundo) ----
    public static final int TOTAL = 86;   // = duración de reviil.ogg (4.27 s ≈ 85 ticks)
    private static final int INTRO_END = 20;
    private static final int IMPACT = 15;
    private static final int BEAT_START = 25;
    private static final int BEAT_EVERY = 10;
    private static final int LOOP_END = 66;   // el bucle (frames 21-66) dura una sola pasada
    private static final int OUTRO_TICKS = 18; // frames 67-80 repartidos hasta que termina el audio (tick 84)
    private static final int LOOP_FIRST = 21;   // frame 21
    private static final int LOOP_LEN = 46;     // frames 21..66
    private static final int OUTRO_FIRST = 67;  // frame 67
    private static final int FRAME_COUNT = 80;

    private static final ResourceLocation[] FRAMES = new ResourceLocation[FRAME_COUNT];
    private static final boolean[] FILTERED = new boolean[FRAME_COUNT];
    static {
        for (int i = 0; i < FRAME_COUNT; i++) {
            FRAMES[i] = new ResourceLocation(MXChallenge.MODID, String.format(Locale.ROOT, "textures/gui/skull/%03d.png", i + 1));
        }
    }

    private static boolean active;
    private static int tick;
    private static final int color = 0xFF2A2A; // contorno rojo

    private static final int EMBERS = 30;
    private static final float[] emberX = new float[EMBERS];
    private static final float[] emberSpeed = new float[EMBERS];
    private static final float[] emberPhase = new float[EMBERS];
    private static final float[] emberSize = new float[EMBERS];

    // ------------------------------------------------------------------------------------
    public static void start() {
        tick = 0;
        active = true;

        Random r = new Random();
        for (int i = 0; i < EMBERS; i++) {
            emberX[i] = r.nextFloat();
            emberSpeed[i] = 0.006F + r.nextFloat() * 0.010F;
            emberPhase[i] = r.nextFloat();
            emberSize[i] = 1.0F + r.nextFloat() * 1.6F;
        }
    }

    public static boolean isActive() {
        return active;
    }

    public static float time(float partialTick) {
        return tick + partialTick;
    }

    /** Llamado una vez por tick de cliente. */
    public static void tick() {
        if (!active) return;
        tick++;
        if (tick >= TOTAL) {
            active = false;
            return;
        }
        if (!Config.CLIENT.epicSounds.get()) return;
        Minecraft mc = Minecraft.getInstance();
        if (tick == IMPACT) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.LIGHTNING_BOLT_THUNDER, 0.9F, 1.0F));
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.WITHER_SPAWN, 0.6F, 0.35F));
        } else if (tick >= BEAT_START && tick < LOOP_END - 4 && (tick - BEAT_START) % BEAT_EVERY == 0) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.WARDEN_HEARTBEAT, 1.0F, 1.0F));
        }
    }

    // ---- curvas ----
    private static float clamp01(float x) { return x < 0 ? 0 : (x > 1 ? 1 : x); }
    private static float easeOutCubic(float x) { x = 1 - clamp01(x); return 1 - x * x * x; }
    private static float easeInCubic(float x) { x = clamp01(x); return x * x * x; }

    /** 0..1: pulso de latido (se dispara cada 10 ticks mientras suena el bucle). */
    private static float beat(float t) {
        if (t < BEAT_START || t >= LOOP_END) return 0;
        float p = (t - BEAT_START) % BEAT_EVERY;
        return (float) Math.exp(-p * 0.45F);
    }

    /** 0..1: intensidad de sacudida (retumbo en la entrada, golpe fuerte en el impacto). */
    public static float shakeAt(float t) {
        if (t < IMPACT) return 0.06F + 0.14F * (t / IMPACT);
        float k = 1F - (t - IMPACT) / 22F;
        return k > 0 ? k * k : 0F;
    }

    private static int frameFor(float t) {
        int ti = (int) Math.floor(t);
        if (ti < INTRO_END) return ti + 1;
        if (ti < LOOP_END) return LOOP_FIRST + ((ti - INTRO_END) % LOOP_LEN);
        return Math.min(FRAME_COUNT, OUTRO_FIRST + (ti - LOOP_END) * (FRAME_COUNT - OUTRO_FIRST + 1) / OUTRO_TICKS);
    }

    private static int argb(int rgb, float a) {
        return ((int) (clamp01(a) * 255F) << 24) | (rgb & 0xFFFFFF);
    }

    // ------------------------------------------------------------------------------------
    public static void render(GuiGraphics g, float partialTick, int w, int h) {
        if (!active) return;
        final float t = time(partialTick);
        final float master = 1F - easeInCubic((t - (TOTAL - 14)) / 14F); // fundido final
        final float shake = shakeAt(t);
        final float b = beat(t);
        final float cr = ((color >> 16) & 255) / 255F;
        final float cg = ((color >> 8) & 255) / 255F;
        final float cb = (color & 255) / 255F;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // ===== escena (con sacudida) =====
        g.pose().pushPose();
        float dx = ((float) Math.sin(t * 47.3F) * 0.7F + (float) Math.cos(t * 31.1F + 1.3F) * 0.4F) * shake * 10F;
        float dy = ((float) Math.cos(t * 41.7F) * 0.7F + (float) Math.sin(t * 27.9F + 0.6F) * 0.4F) * shake * 8F;
        g.pose().translate(dx, dy, 0);

        // fondo oscuro
        float dark = 0.72F * clamp01(t / 8F) * master;
        g.fill(-60, -60, w + 60, h + 60, argb(0x000000, dark));

        // viñeta del color de la dificultad (late con el bucle)
        float vig = (0.30F + 0.30F * b) * clamp01(t / 10F) * master;
        int edge = (int) (h * 0.30F);
        int edgeX = (int) (w * 0.22F);
        g.fillGradient(-60, -60, w + 60, edge, argb(color, vig), argb(color, 0F));
        g.fillGradient(-60, h - edge, w + 60, h + 60, argb(color, 0F), argb(color, vig));
        for (int i = 0; i < 12; i++) { // bordes laterales (degradado horizontal por columnas)
            float k = 1F - i / 12F;
            int col = argb(color, vig * 0.8F * k * k);
            int x0 = i * edgeX / 12;
            int x1 = (i + 1) * edgeX / 12;
            g.fill(x0, -60, x1, h + 60, col);
            g.fill(w - x1, -60, w - x0, h + 60, col);
        }

        float size = Math.min(w, h) * 0.50F;
        float cx = w / 2F;
        float cy = h * 0.40F;

        // rayos giratorios detrás de la calavera
        if (t >= IMPACT - 2) {
            float rayA = 0.30F * easeOutCubic((t - (IMPACT - 2)) / 8F) * master * (0.85F + 0.15F * b);
            drawRays(g, cx, cy, Math.max(w, h) * 0.9F, t * 0.012F, 14, cr, cg, cb, rayA);
        }

        // brasas que suben
        drawEmbers(g, t, w, h, master);

        // escala de la calavera: golpe en el impacto, latido, crecimiento al salir
        float scale = 1F;
        if (t >= IMPACT) scale += 0.30F * (1F - easeOutCubic((t - IMPACT) / 10F));
        scale += 0.035F * b;
        if (t >= LOOP_END) scale += 0.35F * easeInCubic((t - LOOP_END) / (TOTAL - LOOP_END));
        float sway = (float) Math.sin(t * 0.15F) * 0.026F; // radianes
        int fi = frameFor(t) - 1;
        ResourceLocation tex = FRAMES[fi];
        smooth(fi);

        // resplandor aditivo del color de la dificultad
        if (t >= IMPACT - 3) {
            float glowA = 0.55F * clamp01((t - (IMPACT - 3)) / 6F) * (0.65F + 0.35F * b) * master;
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            RenderSystem.setShaderColor(cr, cg, cb, glowA);
            drawSkull(g, tex, cx, cy, size * scale * 1.16F, sway);
            RenderSystem.defaultBlendFunc();
        }

        // calavera
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        drawSkull(g, tex, cx, cy, size * scale, sway);

        g.pose().popPose();

        // ===== capa fija (sin sacudida) =====
        // destello blanco del impacto (+ pequeño golpe al arrancar)
        float flash = 0F;
        if (t < 3F) flash = 0.35F * (1F - t / 3F);
        if (t >= IMPACT && t < IMPACT + 7F) flash = Math.max(flash, 0.85F * (1F - (t - IMPACT) / 7F));
        if (flash > 0F) g.fill(0, 0, w, h, argb(0xFFFFFF, flash));

        // barras de cine
        float bars = Math.min(clamp01(t / 10F), clamp01((TOTAL - t) / 10F));
        int bar = (int) (h * 0.11F * easeOutCubic(bars));
        if (bar > 0) {
            g.fill(0, 0, w, bar, 0xFF000000);
            g.fill(0, h - bar, w, h, 0xFF000000);
            int line = argb(color, 0.85F * master);
            g.fill(0, bar, w, bar + 1, line);
            g.fill(0, h - bar - 1, w, h - bar, line);
        }

        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    // ------------------------------------------------------------------------------------
    private static void smooth(int frameIndex) {
        if (!FILTERED[frameIndex]) {
            Minecraft.getInstance().getTextureManager().getTexture(FRAMES[frameIndex]).setFilter(true, false);
            FILTERED[frameIndex] = true;
        }
    }

    private static void drawSkull(GuiGraphics g, ResourceLocation tex, float cx, float cy, float size, float rot) {
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0);
        g.pose().mulPose(com.mojang.math.Axis.ZP.rotation(rot));
        float s = size / 256F;
        g.pose().scale(s, s, 1F);
        g.blit(tex, -128, -128, 256, 256, 0F, 0F, 256, 256, 256, 256);
        g.pose().popPose();
    }

    private static void drawRays(GuiGraphics g, float cx, float cy, float radius, float rot, int count,
                                 float r, float gr, float bl, float alpha) {
        Matrix4f m = g.pose().last().pose();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder buf = Tesselator.getInstance().getBuilder();
        buf.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        float step = (float) (Math.PI * 2.0 / count);
        float half = step * 0.22F;
        for (int i = 0; i < count; i++) {
            float a = rot + i * step;
            buf.vertex(m, cx, cy, 0F).color(r, gr, bl, alpha).endVertex();
            buf.vertex(m, cx + (float) Math.cos(a - half) * radius, cy + (float) Math.sin(a - half) * radius, 0F)
                    .color(r, gr, bl, 0F).endVertex();
            buf.vertex(m, cx + (float) Math.cos(a + half) * radius, cy + (float) Math.sin(a + half) * radius, 0F)
                    .color(r, gr, bl, 0F).endVertex();
        }
        BufferUploader.drawWithShader(buf.end());
    }

    private static void drawEmbers(GuiGraphics g, float t, int w, int h, float master) {
        float ramp = clamp01((t - 8F) / 10F);
        int mix = lerpRgb(color, 0xFF9A1F, 0.5F);
        for (int i = 0; i < EMBERS; i++) {
            float prog = (t * emberSpeed[i] + emberPhase[i]) % 1F;
            float x = emberX[i] * w + (float) Math.sin(t * 0.1F + emberPhase[i] * 20F) * 12F;
            float y = h * (1.05F - prog * 1.1F);
            float a = (float) Math.sin(prog * Math.PI) * 0.9F * master * ramp;
            if (a < 0.03F) continue;
            int sz = Math.max(1, (int) emberSize[i]);
            g.fill((int) x, (int) y, (int) x + sz, (int) y + sz, argb(mix, a));
        }
    }

    private static int lerpRgb(int a, int b, float k) {
        int r = (int) (((a >> 16) & 255) * (1 - k) + ((b >> 16) & 255) * k);
        int gg = (int) (((a >> 8) & 255) * (1 - k) + ((b >> 8) & 255) * k);
        int bb = (int) ((a & 255) * (1 - k) + (b & 255) * k);
        return (r << 16) | (gg << 8) | bb;
    }

    private SkullAnimation() {}
}

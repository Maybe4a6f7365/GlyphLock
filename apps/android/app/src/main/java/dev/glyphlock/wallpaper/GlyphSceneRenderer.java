package dev.glyphlock.wallpaper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.Shader;
import android.graphics.Typeface;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Morph-first renderer for GlyphLock.
 *
 * The event is not drawn as a bitmap, card, cavity, or overlay. A representative subset of
 * the ambient artwork is promoted into a persistent glyph topology. Every promoted source
 * glyph receives an event target and a result target. During reveal the static ambient raster
 * hands off to those glyphs; the glyphs then move, resize, recolor, and change identity until
 * the same material becomes readable event language and its surrounding technical structure.
 */
final class GlyphSceneRenderer {
    private static final String RAMP = "  .·,:;+i1tfLCG08@";
    private static final String BASE_GLYPHS = " .·,:;+=x1I|/\\()[]{}<>#08@";
    private static final String STRUCTURE_GLYPHS = "·.:;|/\\+−=[]{}<>01";
    private static final Typeface MONO = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL);
    private static final float FLUID_REFERENCE_WIDTH = 1080f;
    private static final float FLUID_REFERENCE_HEIGHT = 2400f;
    private static final float MASK_BLACK_CUTOFF = 10f / 255f;
    private static final float CLOCK_SAFE_LEFT = 0.135f;
    private static final float CLOCK_SAFE_RIGHT = 0.865f;
    private static final float CLOCK_SAFE_BOTTOM = 0.130f;
    private static final float GESTURE_SAFE_LEFT = 0.055f;
    private static final float GESTURE_SAFE_RIGHT = 0.945f;
    private static final float GESTURE_SAFE_TOP = 0.925f;

    private GlyphSceneRenderer() {}

    static final class Scene {
        final int width;
        final int height;
        final Bitmap baseBitmap;
        final List<AmbientGlyph> ambientGlyphs;
        final List<GlyphPoint> morphSources;
        volatile List<MorphGlyph> morphGlyphs;
        volatile int accent;
        final DemoCatalog.Theme theme;
        final Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        final Paint glyphPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        final Paint effectPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        final char[] glyphBuffer = new char[1];
        final float[] motionScratch = new float[2];
        long previewMinute = Long.MIN_VALUE;
        String previewTime = "";
        String previewDate = "";

        Scene(
                int width,
                int height,
                Bitmap baseBitmap,
                List<AmbientGlyph> ambientGlyphs,
                List<GlyphPoint> morphSources,
                List<MorphGlyph> morphGlyphs,
                int accent,
                DemoCatalog.Theme theme
        ) {
            this.width = width;
            this.height = height;
            this.baseBitmap = baseBitmap;
            this.ambientGlyphs = ambientGlyphs;
            this.morphSources = morphSources;
            this.morphGlyphs = morphGlyphs;
            this.accent = accent;
            this.theme = theme;
            glyphPaint.setTypeface(MONO);
            glyphPaint.setTextAlign(Paint.Align.CENTER);
        }

        void recycle() {
            if (!baseBitmap.isRecycled()) baseBitmap.recycle();
        }
    }

    /** A target-only scene update. The expensive artwork bitmap and source topology are reused. */
    static final class Retarget {
        final List<MorphGlyph> morphGlyphs;
        final int accent;

        Retarget(List<MorphGlyph> morphGlyphs, int accent) {
            this.morphGlyphs = morphGlyphs;
            this.accent = accent;
        }
    }

    private static final class GlyphPoint {
        final float x;
        final float y;
        final char glyph;
        final float alpha;
        final float size;

        GlyphPoint(float x, float y, char glyph, float alpha, float size) {
            this.x = x;
            this.y = y;
            this.glyph = glyph;
            this.alpha = alpha;
            this.size = size;
        }
    }

    private static final class AmbientGlyph {
        final float x;
        final float y;
        final char glyph;
        final float size;
        final float alpha;
        final float phase;
        final float depth;
        final float radius;
        final float edgeMobility;
        final float energyCoordinate;
        final float palettePhase;
        final float largeCurlSumSin;
        final float largeCurlSumCos;
        final float largeCurlDifferenceSin;
        final float largeCurlDifferenceCos;
        final float fineCurlSumSin;
        final float fineCurlSumCos;
        final float fineCurlDifferenceSin;
        final float fineCurlDifferenceCos;
        final float energyPhaseSin;
        final float energyPhaseCos;
        final float energyPhaseScaledSin;
        final float energyPhaseScaledCos;
        final boolean glitchEligible;
        final float glitchDirection;
        final float glitchJitter;
        final boolean echo;
        final boolean secondaryEcho;
        final boolean glow;

        AmbientGlyph(
                float x,
                float y,
                char glyph,
                float size,
                float alpha,
                float phase,
                float depth,
                float radius,
                float edgeMobility,
                float energyCoordinate,
                float palettePhase,
                float largeCurlSumSin,
                float largeCurlSumCos,
                float largeCurlDifferenceSin,
                float largeCurlDifferenceCos,
                float fineCurlSumSin,
                float fineCurlSumCos,
                float fineCurlDifferenceSin,
                float fineCurlDifferenceCos,
                float energyPhaseSin,
                float energyPhaseCos,
                float energyPhaseScaledSin,
                float energyPhaseScaledCos,
                boolean glitchEligible,
                float glitchDirection,
                float glitchJitter,
                boolean echo,
                boolean secondaryEcho,
                boolean glow
        ) {
            this.x = x;
            this.y = y;
            this.glyph = glyph;
            this.size = size;
            this.alpha = alpha;
            this.phase = phase;
            this.depth = depth;
            this.radius = radius;
            this.edgeMobility = edgeMobility;
            this.energyCoordinate = energyCoordinate;
            this.palettePhase = palettePhase;
            this.largeCurlSumSin = largeCurlSumSin;
            this.largeCurlSumCos = largeCurlSumCos;
            this.largeCurlDifferenceSin = largeCurlDifferenceSin;
            this.largeCurlDifferenceCos = largeCurlDifferenceCos;
            this.fineCurlSumSin = fineCurlSumSin;
            this.fineCurlSumCos = fineCurlSumCos;
            this.fineCurlDifferenceSin = fineCurlDifferenceSin;
            this.fineCurlDifferenceCos = fineCurlDifferenceCos;
            this.energyPhaseSin = energyPhaseSin;
            this.energyPhaseCos = energyPhaseCos;
            this.energyPhaseScaledSin = energyPhaseScaledSin;
            this.energyPhaseScaledCos = energyPhaseScaledCos;
            this.glitchEligible = glitchEligible;
            this.glitchDirection = glitchDirection;
            this.glitchJitter = glitchJitter;
            this.echo = echo;
            this.secondaryEcho = secondaryEcho;
            this.glow = glow;
        }
    }

    private enum TargetRole {
        TITLE(0.085f),
        SUMMARY(0.145f),
        META(0.055f),
        ACTION(0.175f),
        STRUCTURE(0f);

        final float delay;

        TargetRole(float delay) {
            this.delay = delay;
        }
    }

    private enum PushAxis {
        HORIZONTAL,
        VERTICAL,
        RADIAL
    }

    /** The operational purpose behind a visual grammar. Geometry must express this job. */
    private enum SystemIntent {
        DEFENSE,
        REACTOR,
        NAVIGATION,
        NETWORK,
        BIOMETRIC,
        VAULT,
        TEMPORAL,
        SENSOR,
        ANALYSIS
    }

    private static final class TargetGlyph {
        final float x;
        final float y;
        final char glyph;
        final float size;
        final float alpha;
        final int color;
        final boolean text;
        final TargetRole role;

        TargetGlyph(
                float x,
                float y,
                char glyph,
                float size,
                float alpha,
                int color,
                boolean text,
                TargetRole role
        ) {
            this.x = x;
            this.y = y;
            this.glyph = glyph;
            this.size = size;
            this.alpha = alpha;
            this.color = color;
            this.text = text;
            this.role = role;
        }
    }

    private static final class TextBand {
        final RectF bounds;
        final PushAxis pushAxis;

        TextBand(RectF bounds, PushAxis pushAxis) {
            this.bounds = bounds;
            this.pushAxis = pushAxis;
        }
    }

    private static final class FittedLines {
        final List<String> lines;
        final float textSize;

        FittedLines(List<String> lines, float textSize) {
            this.lines = lines;
            this.textSize = textSize;
        }
    }

    private static final class TargetLayout {
        final List<TargetGlyph> targets;
        final List<TextBand> textBands;
        final RectF semanticBounds;
        final float centerX;
        final float centerY;

        TargetLayout(
                List<TargetGlyph> targets,
                List<TextBand> textBands,
                RectF semanticBounds,
                float centerX,
                float centerY
        ) {
            this.targets = targets;
            this.textBands = textBands;
            this.semanticBounds = semanticBounds;
            this.centerX = centerX;
            this.centerY = centerY;
        }
    }

    private static final class MorphGlyph {
        final GlyphPoint source;
        final TargetGlyph previousTarget;
        final TargetGlyph eventTarget;
        final TargetGlyph resultTarget;
        final float phase;
        final float arc;
        final float delay;
        final float duration;

        MorphGlyph(
                GlyphPoint source,
                TargetGlyph previousTarget,
                TargetGlyph eventTarget,
                TargetGlyph resultTarget,
                float phase,
                float arc,
                float delay,
                float duration
        ) {
            this.source = source;
            this.previousTarget = previousTarget;
            this.eventTarget = eventTarget;
            this.resultTarget = resultTarget;
            this.phase = phase;
            this.arc = arc;
            this.delay = delay;
            this.duration = duration;
        }
    }

    static Scene build(
            Context context,
            int surfaceWidth,
            int surfaceHeight,
            DemoCatalog.Theme theme,
            DemoCatalog.Event event,
            RenderQuality quality
    ) {
        int safeWidth = Math.max(360, surfaceWidth);
        int renderWidth = Math.min(quality.maxRenderWidth, safeWidth);
        int renderHeight = Math.max(
                800,
                Math.round(renderWidth * (surfaceHeight / (float) Math.max(1, surfaceWidth)))
        );

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        options.inSampleSize = quality == RenderQuality.ECO ? 2 : 1;
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        Bitmap decoded = BitmapFactory.decodeResource(context.getResources(), theme.maskResource, options);
        if (decoded == null) throw new IllegalStateException("Unable to decode glyph scene mask");
        Bitmap mask = Bitmap.createScaledBitmap(decoded, renderWidth, renderHeight, true);
        if (mask != decoded) decoded.recycle();

        List<GlyphPoint> basePoints = extractGlyphPoints(mask, renderWidth, renderHeight, theme, quality);
        mask.recycle();
        addFullScreenSourceField(basePoints, renderWidth, renderHeight, theme, quality);
        if (basePoints.isEmpty()) throw new IllegalStateException("Glyph scene mask produced no points");

        Bitmap base = renderBaseBitmap(renderWidth, renderHeight, basePoints, theme);
        List<AmbientGlyph> ambientGlyphs = buildAmbientGlyphs(
                basePoints,
                renderWidth,
                renderHeight,
                theme,
                quality
        );
        List<GlyphPoint> morphSources = selectMorphSources(basePoints, theme, quality);
        TargetLayout eventLayout = buildTargetLayout(renderWidth, renderHeight, theme, event, false);
        TargetLayout resultLayout = buildTargetLayout(renderWidth, renderHeight, theme, event, true);
        List<MorphGlyph> morphGlyphs = buildMorphGlyphs(
                morphSources,
                eventLayout,
                resultLayout,
                renderWidth,
                renderHeight,
                theme,
                event,
                null,
                false
        );

        return new Scene(
                renderWidth,
                renderHeight,
                base,
                ambientGlyphs,
                morphSources,
                morphGlyphs,
                event.accent,
                theme
        );
    }

    /**
     * Compiles a new event onto the scene's existing source particles. This intentionally does
     * not decode the mask or rebuild the ambient bitmap, so successive notifications can arrive
     * as a direct semantic-to-semantic topology change.
     */
    static Retarget prepareRetarget(
            Scene scene,
            DemoCatalog.Event event,
            boolean fromResult
    ) {
        List<MorphGlyph> previous = scene.morphGlyphs;
        TargetLayout eventLayout = buildTargetLayout(
                scene.width,
                scene.height,
                scene.theme,
                event,
                false
        );
        TargetLayout resultLayout = buildTargetLayout(
                scene.width,
                scene.height,
                scene.theme,
                event,
                true
        );
        List<MorphGlyph> morphGlyphs = buildMorphGlyphs(
                scene.morphSources,
                eventLayout,
                resultLayout,
                scene.width,
                scene.height,
                scene.theme,
                event,
                previous,
                fromResult
        );
        return new Retarget(morphGlyphs, event.accent);
    }

    static void applyRetarget(Scene scene, Retarget retarget) {
        scene.morphGlyphs = retarget.morphGlyphs;
        scene.accent = retarget.accent;
    }

    static void draw(
            Canvas output,
            Scene scene,
            ExperienceController.Frame frame,
            long nowMs,
            boolean showPreviewClock
    ) {
        output.drawColor(Color.BLACK);
        int targetWidth = output.getWidth();
        int targetHeight = output.getHeight();

        float reveal = frame.revealProgress;
        // The high-density raster hands visual responsibility to the live topology. Keeping only
        // a quiet source imprint prevents letter collisions without cutting a card-shaped cavity.
        float baseAlpha = 1f - 0.92f * GlyphMath.smooth(reveal / 0.70f);
        output.save();
        output.scale(targetWidth / (float) scene.width, targetHeight / (float) scene.height);
        scene.bitmapPaint.setAlpha(Math.round(255f * baseAlpha));
        if (scene.bitmapPaint.getAlpha() > 0) {
            output.drawBitmap(scene.baseBitmap, 0f, 0f, scene.bitmapPaint);
        }
        drawAmbientMotion(output, scene, frame, nowMs);
        drawMorphField(output, scene, frame, nowMs);
        output.restore();

        if (showPreviewClock) drawPreviewChrome(output, scene);
    }

    private static List<GlyphPoint> extractGlyphPoints(
            Bitmap mask,
            int width,
            int height,
            DemoCatalog.Theme theme,
            RenderQuality quality
    ) {
        int[] pixels = new int[width * height];
        mask.getPixels(pixels, 0, width, 0, 0, width, height);
        List<GlyphPoint> points = new ArrayList<>();
        int step = quality.glyphStepFor(width);
        int top = Math.round(height * 0.05f);
        int bottom = Math.round(height * 0.97f);
        int side = Math.round(width * 0.028f);

        for (int y = top; y < bottom; y += step) {
            for (int x = side; x < width - side; x += step) {
                int pixel = pixels[y * width + x];
                float value = Color.red(pixel) / 255f;
                float noise = GlyphMath.hash(x, y, theme.ordinal() + 1f);
                float threshold = 0.037f + noise * 0.105f;
                // Preserve true black around the hero system. Background dust is admitted
                // deliberately and sparsely instead of letting the random gate populate roughly
                // one cell in eleven across the entire screen.
                if (value < MASK_BLACK_CUTOFF) {
                    if (noise < 0.988f) continue;
                } else if (value < threshold && noise < 0.955f) {
                    continue;
                }
                float level = GlyphMath.clamp01(
                        value * 1.10f * theme.exposure + (noise - 0.5f) * 0.10f
                );
                int rampIndex = Math.min(RAMP.length() - 1, (int) Math.floor(level * RAMP.length()));
                char glyph = RAMP.charAt(rampIndex);
                if (level < 0.20f || noise > 0.88f) {
                    String vocabulary = theme.textureGlyphs + BASE_GLYPHS;
                    glyph = vocabulary.charAt(Math.min(
                            vocabulary.length() - 1,
                            (int) (noise * vocabulary.length())
                    ));
                }
                points.add(new GlyphPoint(
                        x + (noise - 0.5f) * 1.25f,
                        y + (GlyphMath.hash(y, x, 3f) - 0.5f) * 1.15f,
                        glyph,
                        GlyphMath.clamp01(0.08f + level * 0.94f),
                        width * (0.0083f + level * 0.0039f)
                ));
            }
        }
        return points;
    }

    /**
     * Extends each iconic mask into a full-screen ASCII composition. These are source particles,
     * not a decorative overlay: the matcher later recruits them into the event topology.
     */
    private static void addFullScreenSourceField(
            List<GlyphPoint> points,
            int width,
            int height,
            DemoCatalog.Theme theme,
            RenderQuality quality
    ) {
        int runtimeStart = points.size();
        float density = quality == RenderQuality.ECO ? 0.72f
                : quality == RenderQuality.LUX ? 1.10f : 0.90f;
        float left = width * 0.035f;
        float right = width * 0.965f;
        float top = height * 0.115f;
        float bottom = height * 0.965f;
        float centerX = width * theme.atmosphereX;
        float centerY = height * 0.53f;
        float size = width * 0.0094f;
        int seed = 1709 + theme.ordinal() * 311;

        // Broken top and lower telemetry rails tie every composition to the physical screen.
        addSourceLine(points, left, top, width * 0.34f, top, scaled(42, density), seed,
                size, 0.27f, theme);
        addSourceLine(points, width * 0.66f, top, right, top, scaled(42, density), seed + 43,
                size, 0.22f, theme);
        addSourceLine(points, left, bottom, width * 0.42f, bottom, scaled(52, density), seed + 89,
                size, 0.20f, theme);
        addSourceLine(points, width * 0.58f, bottom, right, bottom, scaled(52, density), seed + 137,
                size, 0.25f, theme);

        switch (theme.compositionStyle) {
            case FIGURE: {
                addSourceEllipse(points, centerX, height * 0.52f, width * 0.455f, height * 0.385f,
                        scaled(230, density), seed + 181, size, 0.24f, theme);
                for (int side = -1; side <= 1; side += 2) {
                    float edgeX = side < 0 ? left : right;
                    float innerX = centerX + side * width * 0.27f;
                    addSourceLine(points, edgeX, top, innerX, height * 0.46f,
                            scaled(90, density), seed + side * 13, size, 0.32f, theme);
                    addSourceLine(points, innerX, height * 0.46f, edgeX, bottom,
                            scaled(110, density), seed + side * 29, size, 0.30f, theme);
                    for (int ray = 0; ray < 5; ray++) {
                        float y = GlyphMath.mix(top + height * 0.05f, bottom - height * 0.06f, ray / 4f);
                        addSourceLine(points, edgeX, y, centerX + side * width * 0.10f,
                                centerY + (ray - 2) * height * 0.065f,
                                scaled(48, density), seed + ray * 47 + side, size, 0.20f, theme);
                    }
                }
                break;
            }
            case CORE:
            case ORBITAL_BAND:
            case DIAL: {
                for (int ring = 0; ring < 4; ring++) {
                    addSourceEllipse(
                            points,
                            centerX,
                            centerY,
                            width * (0.265f + ring * 0.064f),
                            height * (0.145f + ring * 0.073f),
                            scaled(150 + ring * 34, density),
                            seed + ring * 61,
                            size,
                            0.33f - ring * 0.035f,
                            theme
                    );
                }
                for (int spoke = 0; spoke < 18; spoke++) {
                    float angle = (float) (Math.PI * 2.0 * spoke / 18.0 - Math.PI * 0.5);
                    float innerX = centerX + (float) Math.cos(angle) * width * 0.16f;
                    float innerY = centerY + (float) Math.sin(angle) * height * 0.085f;
                    float outerX = centerX + (float) Math.cos(angle) * width * 0.46f;
                    float outerY = centerY + (float) Math.sin(angle) * height * 0.39f;
                    addSourceLine(points, innerX, innerY, outerX, outerY,
                            scaled(38, density), seed + 300 + spoke * 23, size, 0.20f, theme);
                }
                break;
            }
            case ARCHITECTURE:
            case CASCADE: {
                for (int frame = 0; frame < 5; frame++) {
                    float insetX = width * (0.025f + frame * 0.055f);
                    float insetY = height * (0.115f + frame * 0.052f);
                    float frameBottom = height * (0.965f - frame * 0.045f);
                    float frameLeft = insetX;
                    float frameRight = width - insetX;
                    addSourceLine(points, frameLeft, insetY, frameRight, insetY,
                            scaled(70, density), seed + frame * 101, size, 0.20f, theme);
                    addSourceLine(points, frameLeft, frameBottom, frameRight, frameBottom,
                            scaled(70, density), seed + frame * 101 + 17, size, 0.24f, theme);
                    addSourceLine(points, frameLeft, insetY, frameLeft, frameBottom,
                            scaled(82, density), seed + frame * 101 + 31, size, 0.22f, theme);
                    addSourceLine(points, frameRight, insetY, frameRight, frameBottom,
                            scaled(82, density), seed + frame * 101 + 47, size, 0.22f, theme);
                }
                for (int lane = 0; lane < 7; lane++) {
                    float x = GlyphMath.mix(left, right, lane / 6f);
                    addSourceLine(points, x, top, centerX, centerY,
                            scaled(54, density), seed + 700 + lane * 29, size, 0.18f, theme);
                    addSourceLine(points, centerX, centerY, x, bottom,
                            scaled(60, density), seed + 900 + lane * 31, size, 0.18f, theme);
                }
                break;
            }
            case SPLICE: {
                for (int ribbon = -2; ribbon <= 2; ribbon++) {
                    float baseX = centerX + ribbon * width * 0.105f;
                    addSourceWave(points, baseX, top, bottom, width * (0.055f + Math.abs(ribbon) * 0.008f),
                            4.5f + Math.abs(ribbon), scaled(150, density), seed + ribbon * 53,
                            size, 0.22f + (2 - Math.abs(ribbon)) * 0.035f, theme);
                }
                for (int bridge = 0; bridge < 12; bridge++) {
                    float y = GlyphMath.mix(top, bottom, bridge / 11f);
                    float phase = bridge * 0.83f;
                    float x1 = centerX - width * (0.30f + 0.055f * (float) Math.sin(phase));
                    float x2 = centerX + width * (0.30f + 0.055f * (float) Math.cos(phase));
                    addSourceLine(points, x1, y, x2, y + (float) Math.sin(phase) * height * 0.018f,
                            scaled(54, density), seed + 1200 + bridge * 37, size, 0.18f, theme);
                }
                break;
            }
            case CONSTELLATION: {
                float[][] nodes = fullScreenNodes(width, height);
                for (int node = 0; node < nodes.length; node++) {
                    addSourceEllipse(points, nodes[node][0], nodes[node][1], width * 0.040f,
                            height * 0.019f, scaled(28, density), seed + node * 83,
                            size, 0.34f, theme);
                }
                int[][] edges = fullScreenEdges();
                for (int edge = 0; edge < edges.length; edge++) {
                    float[] from = nodes[edges[edge][0]];
                    float[] to = nodes[edges[edge][1]];
                    addSourceLine(points, from[0], from[1], to[0], to[1],
                            scaled(62, density), seed + 1600 + edge * 43, size, 0.21f, theme);
                }
                break;
            }
            case FIELD:
            default: {
                for (int row = 0; row < 12; row++) {
                    float y = GlyphMath.mix(top, bottom, row / 11f);
                    float amplitude = height * (0.014f + (row % 3) * 0.004f);
                    addSourceHorizontalWave(points, left, right, y, amplitude,
                            3.0f + (row % 4) * 0.65f, scaled(104, density),
                            seed + row * 79, size, 0.18f + (row % 4) * 0.025f, theme);
                }
                break;
            }
        }
        addSourceSystemSignature(
                points,
                width,
                height,
                theme,
                density,
                seed + 5000,
                size * 1.03f
        );
        preserveSourceCountOutsideSafeZones(
                points,
                runtimeStart,
                width,
                height,
                seed + 9000
        );
    }

    private static int scaled(int value, float density) {
        return Math.max(2, Math.round(value * density));
    }

    private static void addSourceLine(
            List<GlyphPoint> points,
            float startX,
            float startY,
            float endX,
            float endY,
            int count,
            int seed,
            float size,
            float alpha,
            DemoCatalog.Theme theme
    ) {
        int safeCount = Math.max(2, count);
        for (int index = 0; index < safeCount; index++) {
            float t = index / (float) (safeCount - 1);
            addSourceFieldPoint(
                    points,
                    GlyphMath.mix(startX, endX, t),
                    GlyphMath.mix(startY, endY, t),
                    seed + index,
                    size,
                    alpha,
                    theme
            );
        }
    }

    private static void addSourceEllipse(
            List<GlyphPoint> points,
            float centerX,
            float centerY,
            float radiusX,
            float radiusY,
            int count,
            int seed,
            float size,
            float alpha,
            DemoCatalog.Theme theme
    ) {
        int safeCount = Math.max(12, count);
        for (int index = 0; index < safeCount; index++) {
            if ((index + seed) % 17 == 0 || (index + seed) % 29 == 0) continue;
            float angle = (float) (Math.PI * 2.0 * index / safeCount);
            addSourceFieldPoint(
                    points,
                    centerX + (float) Math.cos(angle) * radiusX,
                    centerY + (float) Math.sin(angle) * radiusY,
                    seed + index,
                    size,
                    alpha,
                    theme
            );
        }
    }

    private static void addSourceWave(
            List<GlyphPoint> points,
            float baseX,
            float top,
            float bottom,
            float amplitude,
            float turns,
            int count,
            int seed,
            float size,
            float alpha,
            DemoCatalog.Theme theme
    ) {
        int safeCount = Math.max(2, count);
        for (int index = 0; index < safeCount; index++) {
            float t = index / (float) (safeCount - 1);
            addSourceFieldPoint(
                    points,
                    baseX + (float) Math.sin(t * Math.PI * 2f * turns + seed * 0.017f) * amplitude,
                    GlyphMath.mix(top, bottom, t),
                    seed + index,
                    size,
                    alpha,
                    theme
            );
        }
    }

    private static void addSourceHorizontalWave(
            List<GlyphPoint> points,
            float left,
            float right,
            float baseY,
            float amplitude,
            float turns,
            int count,
            int seed,
            float size,
            float alpha,
            DemoCatalog.Theme theme
    ) {
        int safeCount = Math.max(2, count);
        for (int index = 0; index < safeCount; index++) {
            float t = index / (float) (safeCount - 1);
            addSourceFieldPoint(
                    points,
                    GlyphMath.mix(left, right, t),
                    baseY + (float) Math.sin(t * Math.PI * 2f * turns + seed * 0.013f) * amplitude,
                    seed + index,
                    size,
                    alpha,
                    theme
            );
        }
    }

    /** Adds recognizable subsystem hardware on top of the theme's broader composition. */
    private static void addSourceSystemSignature(
            List<GlyphPoint> points,
            int width,
            int height,
            DemoCatalog.Theme theme,
            float density,
            int seed,
            float size
    ) {
        float left = width * 0.035f;
        float right = width * 0.965f;
        float top = height * 0.125f;
        float bottom = height * 0.955f;
        float centerX = width * 0.5f;
        float centerY = height * 0.515f;

        // Open targeting corners and telemetry ticks establish a cinematic command surface.
        addSourceLine(points, left, height * 0.205f, left, height * 0.145f,
                scaled(18, density), seed, size, 0.52f, theme);
        addSourceLine(points, left, height * 0.145f, width * 0.165f, height * 0.145f,
                scaled(24, density), seed + 23, size, 0.47f, theme);
        addSourceLine(points, right, height * 0.245f, right, height * 0.175f,
                scaled(20, density), seed + 51, size, 0.48f, theme);
        addSourceLine(points, width * 0.835f, height * 0.175f, right, height * 0.175f,
                scaled(24, density), seed + 79, size, 0.43f, theme);
        for (int tick = 0; tick < 13; tick++) {
            float y = GlyphMath.mix(height * 0.285f, height * 0.865f, tick / 12f);
            float tickWidth = width * (tick % 4 == 0 ? 0.052f : 0.025f);
            addSourceLine(points, left, y, left + tickWidth, y,
                    scaled(tick % 4 == 0 ? 10 : 5, density), seed + 110 + tick * 17,
                    size, tick % 4 == 0 ? 0.55f : 0.34f, theme);
            addSourceLine(points, right - tickWidth, y + height * 0.012f, right,
                    y + height * 0.012f, scaled(tick % 4 == 0 ? 10 : 5, density),
                    seed + 340 + tick * 19, size,
                    tick % 4 == 0 ? 0.50f : 0.31f, theme);
        }

        switch (systemIntentFor(theme)) {
            case DEFENSE: {
                // Helmet visor, shoulder plates, and a segmented exoskeleton spine.
                addSourceLine(points, width * 0.18f, height * 0.270f,
                        width * 0.36f, height * 0.325f, scaled(38, density),
                        seed + 610, size, 0.62f, theme);
                addSourceLine(points, width * 0.36f, height * 0.325f,
                        centerX, height * 0.292f, scaled(30, density),
                        seed + 653, size, 0.70f, theme);
                addSourceLine(points, centerX, height * 0.292f,
                        width * 0.64f, height * 0.325f, scaled(30, density),
                        seed + 691, size, 0.70f, theme);
                addSourceLine(points, width * 0.64f, height * 0.325f,
                        width * 0.82f, height * 0.270f, scaled(38, density),
                        seed + 729, size, 0.62f, theme);
                for (int side = -1; side <= 1; side += 2) {
                    float shoulder = centerX + side * width * 0.30f;
                    addSourceLine(points, shoulder, height * 0.39f,
                            centerX + side * width * 0.44f, height * 0.49f,
                            scaled(34, density), seed + 780 + side * 11,
                            size, 0.58f, theme);
                    addSourceLine(points, centerX + side * width * 0.44f, height * 0.49f,
                            centerX + side * width * 0.34f, height * 0.68f,
                            scaled(44, density), seed + 840 + side * 13,
                            size, 0.48f, theme);
                }
                for (int segment = 0; segment < 7; segment++) {
                    float y = height * (0.38f + segment * 0.075f);
                    float half = width * (0.028f + (segment % 2) * 0.012f);
                    addSourceLine(points, centerX - half, y, centerX + half, y,
                            scaled(16, density), seed + 920 + segment * 29,
                            size, segment == 0 ? 0.72f : 0.45f, theme);
                }
                break;
            }
            case REACTOR: {
                for (int ring = 0; ring < 3; ring++) {
                    float rx = width * (0.115f + ring * 0.060f);
                    float ry = height * (0.055f + ring * 0.031f);
                    addSourceArc(points, centerX, centerY, rx, ry,
                            -2.80f + ring * 0.32f, 2.05f,
                            scaled(70 + ring * 16, density), seed + 1050 + ring * 91,
                            size, 0.70f - ring * 0.08f, theme);
                    addSourceArc(points, centerX, centerY, rx, ry,
                            0.35f + ring * 0.26f, 1.65f,
                            scaled(58 + ring * 14, density), seed + 1260 + ring * 97,
                            size, 0.62f - ring * 0.07f, theme);
                }
                for (int conduit = 0; conduit < 8; conduit++) {
                    float angle = (float) (Math.PI * 2.0 * conduit / 8.0);
                    addSourceLine(points,
                            centerX + (float) Math.cos(angle) * width * 0.21f,
                            centerY + (float) Math.sin(angle) * height * 0.11f,
                            centerX + (float) Math.cos(angle) * width * 0.45f,
                            centerY + (float) Math.sin(angle) * height * 0.35f,
                            scaled(34, density), seed + 1500 + conduit * 31,
                            size, conduit % 2 == 0 ? 0.57f : 0.40f, theme);
                }
                break;
            }
            case NAVIGATION: {
                addSourceArc(points, centerX, height * 0.46f, width * 0.44f,
                        height * 0.205f, -2.95f, 2.30f, scaled(142, density),
                        seed + 1690, size, 0.57f, theme);
                addSourceArc(points, centerX, height * 0.46f, width * 0.34f,
                        height * 0.315f, -0.42f, 2.15f, scaled(126, density),
                        seed + 1850, size, 0.48f, theme);
                float[][] waypoints = fullScreenNodes(width, height);
                for (int waypoint = 0; waypoint < waypoints.length; waypoint += 2) {
                    addSourceEllipse(points, waypoints[waypoint][0], waypoints[waypoint][1],
                            width * 0.015f, height * 0.007f, scaled(18, density),
                            seed + 2010 + waypoint * 37, size, 0.74f, theme);
                }
                break;
            }
            case NETWORK: {
                for (int bus = 0; bus < 7; bus++) {
                    float y = height * (0.25f + bus * 0.105f);
                    float joinX = centerX + (bus % 2 == 0 ? -1f : 1f) * width * 0.12f;
                    addSourceLine(points, left, y, joinX, y,
                            scaled(45, density), seed + 2180 + bus * 47,
                            size, 0.43f, theme);
                    addSourceLine(points, joinX, y, joinX, centerY,
                            scaled(35, density), seed + 2390 + bus * 53,
                            size, 0.36f, theme);
                    addSourceEllipse(points, joinX, y, width * 0.010f, height * 0.005f,
                            scaled(14, density), seed + 2600 + bus * 59,
                            size, 0.72f, theme);
                }
                break;
            }
            case BIOMETRIC: {
                addSourceWave(points, centerX - width * 0.075f, top, bottom,
                        width * 0.105f, 4.2f, scaled(185, density),
                        seed + 2790, size, 0.64f, theme);
                addSourceWave(points, centerX + width * 0.075f, top, bottom,
                        width * 0.105f, 4.2f, scaled(185, density),
                        seed + 2990, size, 0.58f, theme);
                for (int pair = 0; pair < 12; pair++) {
                    float t = pair / 11f;
                    float y = GlyphMath.mix(height * 0.18f, height * 0.90f, t);
                    float swing = (float) Math.sin(t * Math.PI * 8.4f) * width * 0.105f;
                    addSourceLine(points, centerX - swing, y, centerX + swing, y,
                            scaled(24, density), seed + 3200 + pair * 31,
                            size, pair % 3 == 0 ? 0.64f : 0.40f, theme);
                }
                break;
            }
            case VAULT: {
                for (int gate = 0; gate < 6; gate++) {
                    float inset = width * (0.10f + gate * 0.045f);
                    float gateTop = height * (0.18f + gate * 0.040f);
                    float gateBottom = height * (0.91f - gate * 0.035f);
                    addSourceLine(points, inset, gateTop, centerX, gateTop + height * 0.060f,
                            scaled(38, density), seed + 3410 + gate * 67,
                            size, 0.53f - gate * 0.035f, theme);
                    addSourceLine(points, centerX, gateTop + height * 0.060f,
                            width - inset, gateTop, scaled(38, density),
                            seed + 3630 + gate * 71, size, 0.53f - gate * 0.035f, theme);
                    addSourceLine(points, inset, gateTop, inset + width * 0.055f, gateBottom,
                            scaled(54, density), seed + 3850 + gate * 73,
                            size, 0.39f, theme);
                }
                break;
            }
            case TEMPORAL: {
                addSourceArc(points, centerX, height * 0.42f, width * 0.38f,
                        height * 0.19f, -2.82f, 2.20f, scaled(150, density),
                        seed + 4070, size, 0.61f, theme);
                for (int tick = 0; tick < 24; tick++) {
                    float angle = (float) (Math.PI * 2.0 * tick / 24.0 - Math.PI * 0.5);
                    float innerX = centerX + (float) Math.cos(angle) * width * 0.31f;
                    float innerY = height * 0.42f + (float) Math.sin(angle) * height * 0.155f;
                    float outerX = centerX + (float) Math.cos(angle) * width * 0.36f;
                    float outerY = height * 0.42f + (float) Math.sin(angle) * height * 0.185f;
                    addSourceLine(points, innerX, innerY, outerX, outerY,
                            scaled(tick % 6 == 0 ? 8 : 4, density),
                            seed + 4250 + tick * 17, size,
                            tick % 6 == 0 ? 0.70f : 0.38f, theme);
                }
                addSourceLine(points, centerX, height * 0.58f, centerX, bottom,
                        scaled(90, density), seed + 4490, size, 0.48f, theme);
                break;
            }
            case SENSOR: {
                float sensorY = height * 0.45f;
                addSourceArc(points, centerX, sensorY, width * 0.43f, height * 0.22f,
                        -2.95f, 2.75f, scaled(165, density), seed + 4610,
                        size, 0.59f, theme);
                for (int ray = 0; ray < 13; ray++) {
                    float t = ray / 12f;
                    float x = GlyphMath.mix(width * 0.08f, width * 0.92f, t);
                    addSourceLine(points, centerX, sensorY, x,
                            height * (0.20f + Math.abs(t - 0.5f) * 0.18f),
                            scaled(42, density), seed + 4790 + ray * 31,
                            size, ray % 3 == 0 ? 0.50f : 0.30f, theme);
                }
                for (int bar = 0; bar < 15; bar++) {
                    float x = GlyphMath.mix(width * 0.14f, width * 0.86f, bar / 14f);
                    float magnitude = height * (0.025f + GlyphMath.hash(bar, seed, 7f) * 0.090f);
                    addSourceLine(points, x, height * 0.88f - magnitude, x, height * 0.88f,
                            scaled(18, density), seed + 5210 + bar * 23,
                            size, 0.38f + magnitude / height, theme);
                }
                break;
            }
            case ANALYSIS:
            default: {
                float horizonY = height * 0.73f;
                addSourceLine(points, left, horizonY, right, horizonY,
                        scaled(120, density), seed + 5480, size, 0.56f, theme);
                for (int lane = 0; lane < 9; lane++) {
                    float horizonX = GlyphMath.mix(width * 0.30f, width * 0.70f, lane / 8f);
                    float floorX = GlyphMath.mix(left, right, lane / 8f);
                    addSourceLine(points, horizonX, horizonY, floorX, bottom,
                            scaled(52, density), seed + 5610 + lane * 31,
                            size, lane % 2 == 0 ? 0.43f : 0.29f, theme);
                }
                for (int row = 1; row <= 6; row++) {
                    float t = row / 6f;
                    float eased = t * t;
                    float y = GlyphMath.mix(horizonY, bottom, eased);
                    float half = GlyphMath.mix(width * 0.20f, width * 0.465f, t);
                    addSourceLine(points, centerX - half, y, centerX + half, y,
                            scaled(76, density), seed + 5910 + row * 37,
                            size, 0.28f + t * 0.14f, theme);
                }
                addSourceLine(points, width * 0.12f, height * 0.355f,
                        width * 0.88f, height * 0.355f, scaled(104, density),
                        seed + 6170, size, 0.68f, theme);
                break;
            }
        }
    }

    private static void addSourceArc(
            List<GlyphPoint> points,
            float centerX,
            float centerY,
            float radiusX,
            float radiusY,
            float startAngle,
            float sweep,
            int count,
            int seed,
            float size,
            float alpha,
            DemoCatalog.Theme theme
    ) {
        int safeCount = Math.max(3, count);
        for (int index = 0; index < safeCount; index++) {
            float t = index / (float) (safeCount - 1);
            float angle = startAngle + sweep * t;
            addSourceFieldPoint(
                    points,
                    centerX + (float) Math.cos(angle) * radiusX,
                    centerY + (float) Math.sin(angle) * radiusY,
                    seed + index,
                    size,
                    alpha,
                    theme
            );
        }
    }

    private static void addSourceFieldPoint(
            List<GlyphPoint> points,
            float x,
            float y,
            int index,
            float size,
            float alpha,
            DemoCatalog.Theme theme
    ) {
        float noise = GlyphMath.hash(index, x, y);
        if (noise < 0.11f) return;
        String vocabulary = theme.textureGlyphs + STRUCTURE_GLYPHS;
        char glyph = vocabulary.charAt(Math.min(
                vocabulary.length() - 1,
                (int) (GlyphMath.hash(index, y, x + 17f) * vocabulary.length())
        ));
        points.add(new GlyphPoint(
                x + (noise - 0.5f) * size * 0.55f,
                y + (GlyphMath.hash(y, index, 19f) - 0.5f) * size * 0.45f,
                glyph,
                GlyphMath.clamp01(alpha * (0.72f + noise * 0.48f)),
                size * (0.88f + noise * 0.20f)
        ));
    }

    /**
     * Removes runtime-authored source points from lock-screen clock and gesture regions while
     * preserving the exact source count. Replacements stay in the side telemetry gutters, so
     * downstream particle budgets do not shrink when a dense rail crosses a protected zone.
     */
    private static void preserveSourceCountOutsideSafeZones(
            List<GlyphPoint> points,
            int runtimeStart,
            int width,
            int height,
            int seed
    ) {
        if (runtimeStart >= points.size()) return;
        List<GlyphPoint> accepted = new ArrayList<>(points.size() - runtimeStart);
        List<GlyphPoint> rejected = new ArrayList<>();
        for (int index = runtimeStart; index < points.size(); index++) {
            GlyphPoint point = points.get(index);
            if (isRuntimeSafeZone(point.x, point.y, width, height)) rejected.add(point);
            else accepted.add(point);
        }
        if (rejected.isEmpty()) return;

        points.subList(runtimeStart, points.size()).clear();
        points.addAll(accepted);
        for (int index = 0; index < rejected.size(); index++) {
            GlyphPoint point = rejected.get(index);
            points.add(new GlyphPoint(
                    safeReplacementX(width, seed, index),
                    safeReplacementY(height, seed, index),
                    point.glyph,
                    point.alpha,
                    point.size
            ));
        }
    }

    private static boolean isRuntimeSafeZone(
            float x,
            float y,
            int width,
            int height
    ) {
        boolean clock = x >= width * CLOCK_SAFE_LEFT
                && x <= width * CLOCK_SAFE_RIGHT
                && y <= height * CLOCK_SAFE_BOTTOM;
        boolean gesture = x >= width * GESTURE_SAFE_LEFT
                && x <= width * GESTURE_SAFE_RIGHT
                && y >= height * GESTURE_SAFE_TOP;
        return clock || gesture;
    }

    private static float safeReplacementX(int width, int seed, int index) {
        float inset = 0.035f + GlyphMath.hash(index, seed, 17f) * 0.070f;
        return GlyphMath.hash(seed, index, 23f) < 0.5f
                ? width * inset
                : width * (1f - inset);
    }

    private static float safeReplacementY(int height, int seed, int index) {
        return height * (0.16f + GlyphMath.hash(index, seed, 31f) * 0.72f);
    }

    private static float[][] fullScreenNodes(int width, int height) {
        return new float[][] {
                { width * 0.075f, height * 0.18f },
                { width * 0.88f, height * 0.16f },
                { width * 0.20f, height * 0.40f },
                { width * 0.78f, height * 0.43f },
                { width * 0.08f, height * 0.72f },
                { width * 0.91f, height * 0.76f },
                { width * 0.28f, height * 0.93f },
                { width * 0.74f, height * 0.91f },
                { width * 0.50f, height * 0.25f },
                { width * 0.50f, height * 0.84f }
        };
    }

    private static int[][] fullScreenEdges() {
        return new int[][] {
                { 0, 2 }, { 0, 8 }, { 1, 8 }, { 1, 3 }, { 2, 8 }, { 8, 3 },
                { 2, 4 }, { 2, 9 }, { 3, 9 }, { 3, 5 }, { 4, 9 }, { 9, 5 },
                { 4, 6 }, { 6, 9 }, { 9, 7 }, { 7, 5 }, { 8, 9 }
        };
    }

    private static SystemIntent systemIntentFor(DemoCatalog.Theme theme) {
        switch (theme) {
            case SENTINEL:
            case MOTH:
                return SystemIntent.DEFENSE;
            case FUSION_CORE:
            case MUON_CHAMBER:
                return SystemIntent.REACTOR;
            case ORBIT:
            case EVENT_HORIZON:
            case DYSON_RELAY:
            case LAGRANGE_GARDEN:
                return SystemIntent.NAVIGATION;
            case CIPHER_CATHEDRAL:
            case PACKET_BLOOM:
            case TESSERACT_ENGINE:
            case VECTOR_SHRINE:
                return SystemIntent.NETWORK;
            case HELIX_ARRAY:
                return SystemIntent.BIOMETRIC;
            case CRYO_VAULT:
            case RECURSIVE_MONOLITH:
                return SystemIntent.VAULT;
            case CHRONO_LOOM:
                return SystemIntent.TEMPORAL;
            case SPECTRAL_OBSERVATORY:
                return SystemIntent.SENSOR;
            case NEURAL_HALO:
            case QUANTUM_LATTICE:
            case INTERFERENCE_FIELD:
            default:
                return SystemIntent.ANALYSIS;
        }
    }

    private static Bitmap renderBaseBitmap(
            int width,
            int height,
            List<GlyphPoint> points,
            DemoCatalog.Theme theme
    ) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.BLACK);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        paint.setTypeface(MONO);
        paint.setTextAlign(Paint.Align.CENTER);

        int secondaryColor = fluidSecondaryColor(theme.atmosphereColor);
        int tertiaryColor = fluidTertiaryColor(theme.atmosphereColor);
        char[] one = new char[1];
        for (GlyphPoint point : points) {
            float hueWave = 0.5f + 0.5f * (float) Math.sin(
                    point.y * 0.0044f + point.x * 0.0018f + theme.ordinal() * 0.63f
            );
            int localTint = mixColor(
                    theme.atmosphereColor,
                    secondaryColor,
                    0.10f + point.alpha * hueWave * 0.62f
            );
            if (point.alpha > 0.74f) {
                paint.setColor(mixColor(localTint, tertiaryColor, 0.34f));
                paint.setAlpha(Math.round(14f + point.alpha * 26f));
                paint.setTextSize(point.size * 1.64f);
                one[0] = point.glyph;
                canvas.drawText(one, 0, 1, point.x, point.y, paint);
            }

            int value = Math.round(92 + 160 * point.alpha);
            float tintMix = 0.26f + point.alpha * 0.40f;
            paint.setColor(Color.rgb(
                    Math.round(GlyphMath.mix(value, Color.red(localTint), tintMix)),
                    Math.round(GlyphMath.mix(value, Color.green(localTint), tintMix)),
                    Math.round(GlyphMath.mix(value, Color.blue(localTint), tintMix))
            ));
            paint.setAlpha(Math.round((0.16f + point.alpha * 0.84f) * 246f));
            paint.setTextSize(point.size * 1.04f);
            one[0] = point.glyph;
            canvas.drawText(one, 0, 1, point.x, point.y, paint);

            // A few high-value cells become white-hot anchors. The larger tinted pass above
            // supplies bloom; this smaller pass preserves the crisp code-like core.
            if (point.alpha > 0.88f
                    && GlyphMath.hash(point.x, point.y, theme.ordinal() + 71f) > 0.95f) {
                paint.setColor(mixColor(localTint, Color.WHITE, 0.70f));
                paint.setAlpha(Math.round(54f + point.alpha * 94f));
                paint.setTextSize(point.size * 0.88f);
                canvas.drawText(one, 0, 1, point.x, point.y, paint);
            }
        }

        Paint atmosphere = new Paint(Paint.ANTI_ALIAS_FLAG);
        atmosphere.setShader(new RadialGradient(
                width * theme.atmosphereX,
                height * theme.atmosphereY,
                width * 0.76f,
                new int[] {
                        withAlpha(mixColor(theme.atmosphereColor, secondaryColor, 0.28f), 37),
                        withAlpha(tertiaryColor, 11),
                        Color.TRANSPARENT
                },
                new float[] { 0f, 0.48f, 1f },
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(
                width * theme.atmosphereX,
                height * theme.atmosphereY,
                width * 0.76f,
                atmosphere
        );
        return bitmap;
    }

    private static List<AmbientGlyph> buildAmbientGlyphs(
            List<GlyphPoint> points,
            int width,
            int height,
            DemoCatalog.Theme theme,
            RenderQuality quality
    ) {
        List<GlyphPoint> candidates = new ArrayList<>();
        for (GlyphPoint point : points) {
            if (point.alpha > 0.18f) candidates.add(point);
        }
        int desired = Math.round(980f * quality.liveDensity);
        int count = Math.min(desired, candidates.size());
        List<AmbientGlyph> live = new ArrayList<>(count);
        float focusX = width * theme.atmosphereX;
        float focusY = height * theme.atmosphereY;
        float toReferenceX = FLUID_REFERENCE_WIDTH / Math.max(1f, width);
        float toReferenceY = FLUID_REFERENCE_HEIGHT / Math.max(1f, height);
        float twoPi = (float) (Math.PI * 2.0);
        for (int i = 0; i < count; i++) {
            GlyphPoint point = candidates.get(
                    (i * 97 + theme.name().length() * 53) % candidates.size()
            );
            float referenceX = point.x * toReferenceX;
            float referenceY = point.y * toReferenceY;
            float phase = GlyphMath.hash(i, referenceX, referenceY) * twoPi;
            float depth = 0.25f + 0.75f * GlyphMath.hash(referenceY, i, 11f);

            // The browser lab expresses the shared curl field in a 1080x2400 reference space.
            // Store trig bases once; draw-time angle-addition identities reuse frame-level time
            // samples instead of evaluating eight sine/cosine terms per glyph and frame.
            float largeXPhase = referenceX * 0.0036f + phase * 0.18f;
            float largeYPhase = referenceY * 0.0027f - phase * 0.11f;
            float fineXPhase = referenceX * 0.0107f + phase * 0.47f;
            float fineYPhase = referenceY * 0.0081f - phase * 0.39f;
            float largeSumPhase = largeXPhase + largeYPhase;
            float largeDifferencePhase = largeXPhase - largeYPhase;
            float fineSumPhase = fineXPhase + fineYPhase;
            float fineDifferencePhase = fineXPhase - fineYPhase;
            boolean glitchEligible = GlyphMath.hash(referenceX, referenceY, 91f) > 0.973f;
            live.add(new AmbientGlyph(
                    point.x,
                    point.y,
                    point.glyph,
                    point.size,
                    point.alpha,
                    phase,
                    depth,
                    (float) Math.hypot(point.x - focusX, point.y - focusY),
                    0.34f + 0.66f * (1f - point.alpha),
                    point.y + point.x * 0.24f,
                    phase / twoPi * 0.31f
                            + point.x / Math.max(1f, width) * 0.18f
                            + point.y / Math.max(1f, height) * 0.11f,
                    (float) Math.sin(largeSumPhase),
                    (float) Math.cos(largeSumPhase),
                    (float) Math.sin(largeDifferencePhase),
                    (float) Math.cos(largeDifferencePhase),
                    (float) Math.sin(fineSumPhase),
                    (float) Math.cos(fineSumPhase),
                    (float) Math.sin(fineDifferencePhase),
                    (float) Math.cos(fineDifferencePhase),
                    (float) Math.sin(phase),
                    (float) Math.cos(phase),
                    (float) Math.sin(phase * 0.7f),
                    (float) Math.cos(phase * 0.7f),
                    glitchEligible,
                    GlyphMath.hash(referenceY, referenceX, 103f) > 0.5f ? 1f : -1f,
                    GlyphMath.hash(referenceX, referenceY, 107f) - 0.5f,
                    GlyphMath.hash(i, referenceX, 191f) > 0.835f,
                    GlyphMath.hash(i, referenceY, 193f) > 0.945f,
                    GlyphMath.hash(referenceX, i, 197f) > 0.89f
            ));
        }
        return live;
    }

    private static List<GlyphPoint> selectMorphSources(
            List<GlyphPoint> points,
            DemoCatalog.Theme theme,
            RenderQuality quality
    ) {
        List<GlyphPoint> candidates = new ArrayList<>();
        for (GlyphPoint point : points) {
            if (point.alpha > 0.15f && !Character.isWhitespace(point.glyph)) {
                candidates.add(point);
            }
        }
        if (candidates.isEmpty()) candidates.addAll(points);
        int count = Math.min(quality.maximumParticles, candidates.size());
        List<GlyphPoint> selected = new ArrayList<>(count);

        // Round-robin spatial buckets prevent a bright central mask from monopolizing the live
        // topology. Edge rails and corner constellations therefore remain present and movable.
        int columns = 8;
        int rows = 14;
        List<List<GlyphPoint>> buckets = new ArrayList<>(columns * rows);
        for (int bucket = 0; bucket < columns * rows; bucket++) buckets.add(new ArrayList<>());
        float candidateWidth = candidatesWidth(candidates);
        float candidateHeight = candidatesHeight(candidates);
        for (GlyphPoint point : candidates) {
            int column = Math.max(0, Math.min(columns - 1,
                    (int) (point.x / candidateWidth * columns)));
            int row = Math.max(0, Math.min(rows - 1,
                    (int) (point.y / candidateHeight * rows)));
            buckets.get(row * columns + column).add(point);
        }
        int[] taken = new int[buckets.size()];
        Set<GlyphPoint> used = new HashSet<>();
        boolean progress = true;
        int rotation = Math.floorMod(theme.ordinal() * 11, buckets.size());
        while (selected.size() < count && progress) {
            progress = false;
            for (int offset = 0; offset < buckets.size() && selected.size() < count; offset++) {
                int bucketIndex = (offset + rotation) % buckets.size();
                List<GlyphPoint> bucket = buckets.get(bucketIndex);
                if (taken[bucketIndex] >= bucket.size()) continue;
                int position = Math.floorMod(
                        theme.ordinal() * 17 + taken[bucketIndex] * 37,
                        bucket.size()
                );
                // Linear fallback keeps the walk unique when the modular step revisits an item.
                while (used.contains(bucket.get(position))) position = (position + 1) % bucket.size();
                GlyphPoint point = bucket.get(position);
                taken[bucketIndex]++;
                used.add(point);
                selected.add(point);
                progress = true;
            }
        }
        return selected;
    }

    private static float candidatesWidth(List<GlyphPoint> candidates) {
        float maximum = 1f;
        for (GlyphPoint point : candidates) maximum = Math.max(maximum, point.x);
        return maximum;
    }

    private static float candidatesHeight(List<GlyphPoint> candidates) {
        float maximum = 1f;
        for (GlyphPoint point : candidates) maximum = Math.max(maximum, point.y);
        return maximum;
    }

    private static TargetLayout buildTargetLayout(
            int width,
            int height,
            DemoCatalog.Theme theme,
            DemoCatalog.Event event,
            boolean result
    ) {
        List<TargetGlyph> targets = new ArrayList<>();
        List<TextBand> bands = new ArrayList<>();
        float centerX = width * theme.atmosphereX;
        // The language uses the display, not a narrow notification-shaped island. The generous
        // inset still protects curved screens and OEM gesture regions.
        float semanticWidth = Math.max(0.84f, Math.min(0.88f, theme.semanticWidth + 0.18f));
        float contentWidth = width * semanticWidth;
        float left = centerX - contentWidth * 0.5f;
        float right = centerX + contentWidth * 0.5f;
        float anchorY = height * theme.semanticY;

        int accent = event.accent;
        String eyebrow = (result ? "LOCAL RESULT · READY" : event.eyebrow).toUpperCase(Locale.ROOT);
        String title = (result ? event.resultTitle : event.title).toUpperCase(Locale.ROOT);
        String summary = (result ? event.resultSummary : event.summary).toUpperCase(Locale.ROOT);

        Paint eyebrowPaint = monoPaint(width * 0.0280f, true, withAlpha(accent, 246));
        eyebrowPaint.setTextSize(fitTextSize(
                eyebrowPaint,
                eyebrow,
                contentWidth,
                width * 0.0280f,
                width * 0.0210f
        ));

        Paint titlePaint = monoPaint(width * 0.110f, true, Color.rgb(248, 250, 251));
        FittedLines fittedTitle = fitWrappedLines(
                titlePaint,
                title,
                contentWidth,
                3,
                width * 0.110f,
                width * 0.082f
        );
        titlePaint.setTextSize(fittedTitle.textSize);
        List<String> titleLines = fittedTitle.lines;

        Paint summaryPaint = monoPaint(width * 0.0500f, true, Color.rgb(232, 239, 242));
        FittedLines fittedSummary = fitWrappedLines(
                summaryPaint,
                summary,
                contentWidth,
                5,
                width * 0.0500f,
                width * 0.0320f
        );
        summaryPaint.setTextSize(fittedSummary.textSize);
        List<String> summaryLines = fittedSummary.lines;

        // Spread the semantic glyphs through the composition. This is intentionally not a
        // compact title/body stack: the ambient field has room to transform around each phrase.
        float metaY = Math.max(height * 0.205f, anchorY - height * 0.315f);
        float titleY = Math.max(height * 0.315f, metaY + height * 0.082f);
        List<Float> titleBaselines = new ArrayList<>();
        for (String ignored : titleLines) {
            titleBaselines.add(titleY);
            titleY += Math.max(titlePaint.getTextSize() * 1.22f, height * 0.057f);
        }
        float summaryY = Math.max(height * 0.565f, titleY + height * 0.052f);
        float summaryBottom = height * 0.885f;
        float idealLineHeight = Math.max(summaryPaint.getTextSize() * 1.52f, height * 0.043f);
        float availableLineHeight = summaryLines.size() <= 1
                ? idealLineHeight
                : (summaryBottom - summaryY) / (summaryLines.size() - 1f);
        float lineHeight = Math.min(idealLineHeight, availableLineHeight);
        List<Float> summaryBaselines = new ArrayList<>();
        for (String ignored : summaryLines) {
            summaryBaselines.add(summaryY);
            summaryY += lineHeight;
        }

        Paint.Align primaryAlign = theme.compositionStyle == DemoCatalog.CompositionStyle.ARCHITECTURE
                || theme.compositionStyle == DemoCatalog.CompositionStyle.CASCADE
                ? Paint.Align.LEFT
                : Paint.Align.CENTER;
        Paint.Align metaAlign = primaryAlign;
        if (theme.compositionStyle == DemoCatalog.CompositionStyle.FIELD
                || theme.compositionStyle == DemoCatalog.CompositionStyle.CONSTELLATION) {
            metaAlign = Paint.Align.LEFT;
        } else if (theme.compositionStyle == DemoCatalog.CompositionStyle.CASCADE) {
            metaAlign = Paint.Align.RIGHT;
        }

        float primaryAnchor = primaryAlign == Paint.Align.LEFT ? left : centerX;
        float metaAnchor = metaAlign == Paint.Align.LEFT ? left
                : metaAlign == Paint.Align.RIGHT ? right : centerX;
        PushAxis pushAxis = pushAxisFor(theme.compositionStyle);

        // Language is encoded only as stroke coordinates. Multiple retained source symbols form
        // every visible letter; no particle ever becomes a literal content character.
        for (int i = 0; i < titleLines.size(); i++) {
            float lineAnchor = primaryAnchor;
            if (theme.compositionStyle == DemoCatalog.CompositionStyle.CONSTELLATION) {
                lineAnchor += (i == 0 ? -width * 0.018f : width * 0.018f);
            }
            addMacroLineTargets(
                    targets, bands, titleLines.get(i), lineAnchor, titleBaselines.get(i), titlePaint, width,
                    Color.rgb(244, 248, 250), 1f, primaryAlign, TargetRole.TITLE, pushAxis
            );
        }
        for (int i = 0; i < summaryLines.size(); i++) {
            float lineAnchor = primaryAnchor;
            Paint.Align lineAlign = primaryAlign;
            if (theme.compositionStyle == DemoCatalog.CompositionStyle.FIELD) {
                lineAnchor = centerX + (i % 2 == 0 ? -width * 0.025f : width * 0.025f);
                lineAlign = Paint.Align.CENTER;
            } else if (theme.compositionStyle == DemoCatalog.CompositionStyle.CASCADE) {
                lineAnchor = left + width * (0.018f + i * 0.026f);
                lineAlign = Paint.Align.LEFT;
            } else if (theme.compositionStyle == DemoCatalog.CompositionStyle.CONSTELLATION) {
                float[] offsets = { -0.052f, 0.052f, 0f };
                lineAnchor = centerX + width * offsets[Math.min(i, offsets.length - 1)];
                lineAlign = Paint.Align.CENTER;
            }
            addMacroLineTargets(
                    targets, bands, summaryLines.get(i), lineAnchor, summaryBaselines.get(i), summaryPaint, width,
                    Color.rgb(232, 239, 242), 1f, lineAlign, TargetRole.SUMMARY, pushAxis
            );
        }
        addMacroLineTargets(
                targets, bands, eyebrow, metaAnchor, metaY, eyebrowPaint, width,
                withAlpha(accent, 246), 1f, metaAlign, TargetRole.META, pushAxis
        );

        RectF semanticBounds = new RectF(
                left,
                metaY - eyebrowPaint.getTextSize(),
                right,
                Math.min(summaryBottom, summaryY + summaryPaint.getTextSize() * 0.30f)
        );
        addFullScreenEventStructure(targets, width, height, theme, event, result);
        clearStructureFromTextBands(targets, bands, width, height);
        return new TargetLayout(targets, bands, semanticBounds, centerX, anchorY);
    }

    private static PushAxis pushAxisFor(DemoCatalog.CompositionStyle style) {
        switch (style) {
            case ARCHITECTURE:
            case SPLICE:
            case CASCADE:
                return PushAxis.HORIZONTAL;
            case FIELD:
                return PushAxis.VERTICAL;
            case FIGURE:
            case CORE:
            case ORBITAL_BAND:
            case DIAL:
            case CONSTELLATION:
            default:
                return PushAxis.RADIAL;
        }
    }

    private static void addMacroLineTargets(
            List<TargetGlyph> targets,
            List<TextBand> bands,
            String text,
            float anchorX,
            float baseline,
            Paint paint,
            int canvasWidth,
            int color,
            float alpha,
            Paint.Align align,
            TargetRole role,
            PushAxis pushAxis
    ) {
        float lineWidth = paint.measureText(text);
        float startX;
        if (align == Paint.Align.CENTER) startX = anchorX - lineWidth * 0.5f;
        else if (align == Paint.Align.RIGHT) startX = anchorX - lineWidth;
        else startX = anchorX;

        Path letterShapes = new Path();
        paint.getTextPath(text, 0, text.length(), startX, baseline, letterShapes);
        float particleSize;
        switch (role) {
            case TITLE:
                particleSize = Math.max(canvasWidth * 0.0046f, paint.getTextSize() * 0.064f);
                break;
            case SUMMARY:
                particleSize = Math.max(canvasWidth * 0.0038f, paint.getTextSize() * 0.105f);
                break;
            case META:
            case ACTION:
                particleSize = Math.max(canvasWidth * 0.0038f, paint.getTextSize() * 0.145f);
                break;
            case STRUCTURE:
            default:
                particleSize = Math.max(canvasWidth * 0.0038f, paint.getTextSize() * 0.13f);
                break;
        }
        float spacing = particleSize * 1.08f;

        // Compile the filled letter strokes into coordinates. Nothing here is drawn as a text
        // layer: each occupied cell becomes a destination for one retained artwork glyph.
        RectF shapeBounds = new RectF();
        letterShapes.computeBounds(shapeBounds, true);
        Region clip = new Region(
                (int) Math.floor(shapeBounds.left) - 2,
                (int) Math.floor(shapeBounds.top) - 2,
                (int) Math.ceil(shapeBounds.right) + 2,
                (int) Math.ceil(shapeBounds.bottom) + 2
        );
        Region filledLetters = new Region();
        filledLetters.setPath(letterShapes, clip);
        float probeOffset = spacing * 0.30f;
        for (float cellY = shapeBounds.top + spacing * 0.42f;
                cellY <= shapeBounds.bottom + spacing * 0.22f;
                cellY += spacing) {
            for (float cellX = shapeBounds.left + spacing * 0.42f;
                    cellX <= shapeBounds.right + spacing * 0.22f;
                    cellX += spacing) {
                float targetX = 0f;
                float targetY = 0f;
                int hits = 0;
                for (int probeY = -1; probeY <= 1; probeY++) {
                    for (int probeX = -1; probeX <= 1; probeX++) {
                        float sampleX = cellX + probeX * probeOffset;
                        float sampleY = cellY + probeY * probeOffset;
                        if (!filledLetters.contains(Math.round(sampleX), Math.round(sampleY))) continue;
                        targetX += sampleX;
                        targetY += sampleY;
                        hits++;
                    }
                }
                if (hits == 0) continue;
                targets.add(new TargetGlyph(
                        targetX / hits,
                        targetY / hits,
                        '.', // Placeholder; buildMorphGlyphs restores the recruited source symbol.
                        particleSize,
                        alpha,
                        color,
                        true,
                        role
                ));
            }
        }

        float padX = particleSize * 1.10f;
        float padY = particleSize * 1.20f;
        bands.add(new TextBand(
                new RectF(
                        shapeBounds.left - padX,
                        shapeBounds.top - padY,
                        shapeBounds.right + padX,
                        shapeBounds.bottom + padY
                ),
                pushAxis
        ));
    }

    /**
     * Builds an event-state composition across the physical screen. None of these coordinates
     * are derived from the semantic bounds, so the decoration cannot become a notification
     * outline. Deliberate gaps keep every grammar open and asymmetrical.
     */
    private static void addFullScreenEventStructure(
            List<TargetGlyph> targets,
            int width,
            int height,
            DemoCatalog.Theme theme,
            DemoCatalog.Event event,
            boolean result
    ) {
        int runtimeStart = targets.size();
        int color = mixColor(theme.atmosphereColor, event.accent, result ? 0.64f : 0.50f);
        float size = width * 0.0108f;
        float left = width * 0.035f;
        float right = width * 0.965f;
        float top = height * 0.125f;
        float bottom = height * 0.950f;
        float centerX = width * theme.atmosphereX;
        float centerY = height * (result ? 0.555f : 0.515f);
        float phase = result ? 0.72f : 0.10f;
        int seed = 3109 + theme.ordinal() * 173 + (result ? 997 : 0);

        // Registration fragments connect the composition to the device without enclosing it.
        addStructureLine(targets, left, top, width * 0.205f, top + height * 0.011f,
                21, seed, size, 0.29f, color);
        addStructureLine(targets, width * 0.765f, top + height * 0.054f, right,
                top + height * 0.034f, 24, seed + 31, size, 0.24f, color);
        addStructureLine(targets, left, bottom - height * 0.075f, width * 0.165f, bottom,
                19, seed + 67, size, 0.22f, color);
        addStructureLine(targets, right, height * 0.735f, right - width * 0.018f, bottom,
                25, seed + 101, size, 0.27f, color);

        addTargetSystemSignature(
                targets,
                width,
                height,
                theme,
                result,
                seed + 7000,
                size,
                color
        );

        switch (theme.compositionStyle) {
            case FIGURE: {
                addTargetVerticalWave(targets, width * 0.105f, top, bottom, width * 0.046f,
                        3.2f, phase, 74, seed + 130, size, 0.38f, color);
                addTargetVerticalWave(targets, width * 0.895f, top + height * 0.025f, bottom,
                        width * 0.056f, 2.7f, phase + 1.7f, 78, seed + 217, size, 0.34f, color);
                addTargetVerticalWave(targets, width * 0.235f, height * 0.245f, height * 0.855f,
                        width * 0.030f, 4.4f, phase + 0.8f, 52, seed + 301, size, 0.25f, color);
                addTargetVerticalWave(targets, width * 0.765f, height * 0.185f, height * 0.825f,
                        width * 0.034f, 4.1f, phase + 2.2f, 54, seed + 367, size, 0.26f, color);
                for (int ray = 0; ray < 7; ray++) {
                    float y = GlyphMath.mix(height * 0.19f, height * 0.90f, ray / 6f);
                    float destinationY = centerY + (ray - 3) * height * 0.056f;
                    addStructureLine(targets, left, y, width * 0.285f, destinationY,
                            10, seed + 430 + ray * 17, size, 0.21f, color);
                    if ((ray & 1) == 0) {
                        addStructureLine(targets, right, y + height * 0.025f,
                                width * 0.715f, destinationY - height * 0.020f,
                                10, seed + 570 + ray * 19, size, 0.19f, color);
                    }
                }
                break;
            }
            case CORE:
            case ORBITAL_BAND:
            case DIAL: {
                float orbitalTilt = theme.compositionStyle == DemoCatalog.CompositionStyle.ORBITAL_BAND
                        ? 0.22f : 0f;
                for (int ring = 0; ring < 4; ring++) {
                    float radiusX = width * (0.275f + ring * 0.061f);
                    float radiusY = height * (0.150f + ring * 0.060f);
                    float start = -2.92f + ring * 0.31f + phase + orbitalTilt;
                    addStructureArc(targets, centerX, centerY, radiusX, radiusY,
                            start, 1.05f + ring * 0.06f, 34 + ring * 4,
                            seed + 650 + ring * 79, size, 0.36f - ring * 0.035f, color);
                    addStructureArc(targets, centerX, centerY, radiusX, radiusY,
                            start + 2.65f, 0.78f + ring * 0.055f, 28 + ring * 4,
                            seed + 820 + ring * 83, size, 0.30f - ring * 0.030f, color);
                }
                for (int spoke = 0; spoke < 14; spoke++) {
                    float angle = (float) (Math.PI * 2.0 * spoke / 14.0 + phase * 0.44f);
                    if (spoke % 4 == 1) continue;
                    float innerX = centerX + (float) Math.cos(angle) * width * 0.355f;
                    float innerY = centerY + (float) Math.sin(angle) * height * 0.300f;
                    float outerX = centerX + (float) Math.cos(angle) * width * 0.465f;
                    float outerY = centerY + (float) Math.sin(angle) * height * 0.405f;
                    addStructureLine(targets, innerX, innerY, outerX, outerY,
                            spoke % 3 == 0 ? 9 : 6, seed + 1010 + spoke * 23,
                            size, spoke % 3 == 0 ? 0.35f : 0.23f, color);
                }
                break;
            }
            case ARCHITECTURE: {
                for (int lane = 0; lane < 6; lane++) {
                    float laneX = GlyphMath.mix(left, right, lane / 5f);
                    float lean = (lane - 2.5f) * width * 0.058f;
                    addStructureLine(targets, laneX, top, laneX - lean, height * 0.405f,
                            30, seed + 1180 + lane * 41, size, 0.25f, color);
                    addStructureLine(targets, laneX + lean * 0.34f, height * 0.705f,
                            laneX - lean * 0.65f, bottom, 28,
                            seed + 1430 + lane * 43, size, 0.27f, color);
                }
                for (int tier = 0; tier < 7; tier++) {
                    float y = height * (0.165f + tier * 0.118f);
                    float inset = width * (0.02f + (tier % 3) * 0.038f);
                    addStructureLine(targets, left + inset, y, width * 0.285f, y + height * 0.018f,
                            17, seed + 1690 + tier * 37, size, 0.23f, color);
                    addStructureLine(targets, width * 0.735f, y - height * 0.014f, right - inset, y,
                            17, seed + 1840 + tier * 41, size, 0.20f, color);
                }
                break;
            }
            case SPLICE: {
                for (int ribbon = -2; ribbon <= 2; ribbon++) {
                    float baseX = centerX + ribbon * width * 0.185f;
                    addTargetVerticalWave(targets, baseX, top, bottom,
                            width * (0.037f + Math.abs(ribbon) * 0.010f),
                            3.8f + Math.abs(ribbon) * 0.55f,
                            phase + ribbon * 0.72f,
                            67, seed + 2010 + ribbon * 71,
                            size, 0.25f + (2 - Math.abs(ribbon)) * 0.035f, color);
                }
                for (int bridge = 0; bridge < 9; bridge++) {
                    float y = GlyphMath.mix(height * 0.19f, height * 0.91f, bridge / 8f);
                    float wave = (float) Math.sin(bridge * 1.13f + phase) * width * 0.038f;
                    addStructureLine(targets, left, y, width * 0.26f + wave,
                            y + height * 0.018f, 16, seed + 2290 + bridge * 29,
                            size, 0.20f, color);
                    if ((bridge & 1) == 0) {
                        addStructureLine(targets, width * 0.74f - wave, y - height * 0.013f,
                                right, y, 16, seed + 2430 + bridge * 31,
                                size, 0.22f, color);
                    }
                }
                break;
            }
            case CASCADE: {
                for (int step = 0; step < 10; step++) {
                    float y = height * (0.145f + step * 0.083f);
                    float run = width * (0.12f + (step % 4) * 0.035f);
                    float leftX = left + (step % 3) * width * 0.022f;
                    addStructureLine(targets, leftX, y, leftX + run,
                            y + height * 0.035f, 18, seed + 2600 + step * 37,
                            size, 0.24f + (step % 3) * 0.025f, color);
                    if (step % 3 != 1) {
                        float rightX = right - (step % 4) * width * 0.019f;
                        addStructureLine(targets, rightX - run * 0.82f,
                                y + height * 0.018f, rightX, y,
                                16, seed + 2810 + step * 41, size, 0.21f, color);
                    }
                }
                addTargetVerticalWave(targets, width * 0.085f, top, bottom,
                        width * 0.022f, 2.4f, phase, 72,
                        seed + 3030, size, 0.30f, color);
                addTargetVerticalWave(targets, width * 0.915f, top, bottom,
                        width * 0.026f, 2.8f, phase + 1.3f, 70,
                        seed + 3110, size, 0.26f, color);
                break;
            }
            case CONSTELLATION: {
                float[][] nodes = fullScreenNodes(width, height);
                for (int node = 0; node < nodes.length; node++) {
                    float nodeX = width - nodes[node][0] + (result ? width * 0.012f : 0f);
                    float nodeY = nodes[node][1] + (float) Math.sin(node * 1.7f + phase)
                            * height * 0.025f;
                    addStructureArc(targets, nodeX, nodeY, width * 0.030f,
                            height * 0.014f, phase + node * 0.31f, 4.65f,
                            18, seed + 3290 + node * 43, size, 0.36f, color);
                }
                int[][] edges = fullScreenEdges();
                for (int edge = 0; edge < edges.length; edge++) {
                    float[] from = nodes[edges[edge][0]];
                    float[] to = nodes[edges[edge][1]];
                    addStructureLine(targets,
                            width - from[0], from[1], width - to[0], to[1],
                            15, seed + 3510 + edge * 31, size, 0.21f, color);
                }
                break;
            }
            case FIELD:
            default: {
                for (int row = 0; row < 12; row++) {
                    float y = GlyphMath.mix(top, bottom, row / 11f);
                    addTargetHorizontalWave(targets, left, right, y,
                            height * (0.010f + (row % 3) * 0.003f),
                            2.6f + (row % 4) * 0.56f,
                            phase + row * 0.42f, 48,
                            seed + 3780 + row * 47, size,
                            0.18f + (row % 4) * 0.035f, color);
                }
                break;
            }
        }
        preserveTargetCountOutsideSafeZones(
                targets,
                runtimeStart,
                width,
                height,
                seed + 9000
        );
    }

    /**
     * The activated form of each subsystem. The source geometry and these destinations describe
     * a job: armor acquires, a reactor routes power, a vault unlocks, a sensor scans, and so on.
     */
    private static void addTargetSystemSignature(
            List<TargetGlyph> targets,
            int width,
            int height,
            DemoCatalog.Theme theme,
            boolean result,
            int seed,
            float size,
            int color
    ) {
        float left = width * 0.035f;
        float right = width * 0.965f;
        float centerX = width * 0.5f;
        float phase = result ? 0.62f : 0f;
        int hotColor = mixColor(color, Color.WHITE, result ? 0.42f : 0.28f);

        switch (systemIntentFor(theme)) {
            case DEFENSE: {
                float visorY = height * (result ? 0.285f : 0.305f);
                addStructureLine(targets, width * 0.085f, visorY + height * 0.035f,
                        width * 0.39f, visorY, 42, seed, size, 0.62f, hotColor);
                addStructureLine(targets, width * 0.61f, visorY,
                        width * 0.915f, visorY + height * 0.035f,
                        42, seed + 47, size, 0.62f, hotColor);
                addStructureLine(targets, width * 0.39f, visorY,
                        centerX, visorY + height * 0.028f,
                        18, seed + 97, size, 0.76f, hotColor);
                addStructureLine(targets, centerX, visorY + height * 0.028f,
                        width * 0.61f, visorY,
                        18, seed + 127, size, 0.76f, hotColor);
                for (int plate = 0; plate < 6; plate++) {
                    float y = height * (0.43f + plate * 0.085f);
                    float inner = width * (0.25f + plate * 0.012f);
                    float outer = width * (0.055f + (plate % 2) * 0.025f);
                    addStructureLine(targets, outer, y, inner,
                            y + height * 0.025f, 24, seed + 170 + plate * 37,
                            size, plate % 2 == 0 ? 0.46f : 0.32f, color);
                    addStructureLine(targets, width - inner, y + height * 0.025f,
                            width - outer, y, 24, seed + 410 + plate * 41,
                            size, plate % 2 == 0 ? 0.46f : 0.32f, color);
                }
                break;
            }
            case REACTOR: {
                float coreY = height * (result ? 0.835f : 0.805f);
                for (int ring = 0; ring < 4; ring++) {
                    float rx = width * (0.075f + ring * 0.055f);
                    float ry = height * (0.034f + ring * 0.025f);
                    addStructureArc(targets, centerX, coreY, rx, ry,
                            -2.85f + ring * 0.41f + phase, 1.82f,
                            28 + ring * 7, seed + 690 + ring * 61,
                            size, 0.72f - ring * 0.09f, ring == 0 ? hotColor : color);
                    addStructureArc(targets, centerX, coreY, rx, ry,
                            0.22f + ring * 0.35f + phase, 1.46f,
                            24 + ring * 6, seed + 920 + ring * 67,
                            size, 0.64f - ring * 0.08f, ring == 0 ? hotColor : color);
                }
                for (int port = 0; port < 8; port++) {
                    float angle = (float) (Math.PI * 2.0 * port / 8.0 + phase * 0.2f);
                    float x1 = centerX + (float) Math.cos(angle) * width * 0.24f;
                    float y1 = coreY + (float) Math.sin(angle) * height * 0.105f;
                    float x2 = centerX + (float) Math.cos(angle) * width * 0.46f;
                    float y2 = coreY + (float) Math.sin(angle) * height * 0.165f;
                    addStructureLine(targets, x1, y1, x2, y2,
                            port % 2 == 0 ? 20 : 13, seed + 1190 + port * 29,
                            size, port % 2 == 0 ? 0.50f : 0.30f, color);
                }
                break;
            }
            case NAVIGATION: {
                float[][] nodes = {
                        { width * 0.07f, height * 0.20f },
                        { width * 0.90f, height * 0.17f },
                        { width * 0.82f, height * 0.42f },
                        { width * 0.93f, height * 0.72f },
                        { width * 0.71f, height * 0.92f },
                        { width * 0.27f, height * 0.90f },
                        { width * 0.08f, height * 0.68f },
                        { width * 0.18f, height * 0.40f }
                };
                int[] route = result
                        ? new int[] { 0, 2, 1, 4, 6, 3, 5 }
                        : new int[] { 0, 7, 5, 4, 3, 2, 1 };
                for (int leg = 0; leg < route.length - 1; leg++) {
                    float[] from = nodes[route[leg]];
                    float[] to = nodes[route[leg + 1]];
                    addStructureLine(targets, from[0], from[1], to[0], to[1],
                            32, seed + 1430 + leg * 43, size,
                            leg == route.length - 2 ? 0.68f : 0.38f,
                            leg == route.length - 2 ? hotColor : color);
                }
                for (int node = 0; node < nodes.length; node++) {
                    addStructureArc(targets, nodes[node][0], nodes[node][1],
                            width * 0.019f, height * 0.008f,
                            node * 0.37f + phase, 4.72f, 14,
                            seed + 1740 + node * 31, size,
                            node == route[route.length - 1] ? 0.78f : 0.48f,
                            node == route[route.length - 1] ? hotColor : color);
                }
                break;
            }
            case NETWORK: {
                for (int channel = 0; channel < 9; channel++) {
                    boolean fromLeft = (channel & 1) == 0;
                    float y = height * (0.17f + channel * 0.087f);
                    float edgeX = fromLeft ? left : right;
                    float elbowX = width * (fromLeft ? 0.25f : 0.75f)
                            + (float) Math.sin(channel + phase) * width * 0.035f;
                    float busY = height * (0.48f + (channel - 4) * 0.022f);
                    addStructureLine(targets, edgeX, y, elbowX, y,
                            24, seed + 1970 + channel * 47, size, 0.39f, color);
                    addStructureLine(targets, elbowX, y, elbowX, busY,
                            19, seed + 2210 + channel * 53, size, 0.34f, color);
                    addStructureLine(targets, elbowX, busY,
                            centerX + (fromLeft ? -1f : 1f) * width * 0.10f,
                            busY, 15, seed + 2470 + channel * 59,
                            size, channel == 4 ? 0.70f : 0.43f,
                            channel == 4 ? hotColor : color);
                }
                break;
            }
            case BIOMETRIC: {
                for (int side = -1; side <= 1; side += 2) {
                    addTargetVerticalWave(targets, centerX + side * width * 0.34f,
                            height * 0.135f, height * 0.94f,
                            width * 0.070f, 4.25f, phase + side * 0.72f,
                            92, seed + 2760 + side * 79, size, 0.55f, color);
                }
                for (int pair = 0; pair < 13; pair++) {
                    float y = GlyphMath.mix(height * 0.16f, height * 0.92f, pair / 12f);
                    float swing = (float) Math.sin(pair * 1.05f + phase) * width * 0.055f;
                    addStructureLine(targets, left, y,
                            width * 0.19f + swing, y + height * 0.012f,
                            16, seed + 2940 + pair * 29, size,
                            pair % 3 == 0 ? 0.48f : 0.28f, color);
                    addStructureLine(targets, width * 0.81f - swing,
                            y - height * 0.012f, right, y,
                            16, seed + 3350 + pair * 31, size,
                            pair % 3 == 0 ? 0.48f : 0.28f, color);
                }
                break;
            }
            case VAULT: {
                // Six nested source gates unlock into separated armor chevrons.
                for (int gate = 0; gate < 6; gate++) {
                    float yTop = height * (0.16f + gate * 0.052f);
                    float yBottom = height * (0.91f - gate * 0.045f);
                    float split = width * (0.23f + gate * 0.035f);
                    addStructureLine(targets, left, yTop, centerX - split,
                            height * 0.49f, 31, seed + 3760 + gate * 61,
                            size, 0.52f - gate * 0.045f, color);
                    addStructureLine(targets, centerX + split, height * 0.49f,
                            right, yTop, 31, seed + 3990 + gate * 67,
                            size, 0.52f - gate * 0.045f, color);
                    addStructureLine(targets, left + gate * width * 0.014f, yBottom,
                            width * 0.22f, yBottom - height * 0.035f,
                            17, seed + 4230 + gate * 71, size, 0.34f, color);
                    addStructureLine(targets, width * 0.78f,
                            yBottom - height * 0.035f,
                            right - gate * width * 0.014f, yBottom,
                            17, seed + 4470 + gate * 73, size, 0.34f, color);
                }
                break;
            }
            case TEMPORAL: {
                float dialY = height * 0.82f;
                addStructureArc(targets, centerX, dialY, width * 0.36f,
                        height * 0.13f, -2.90f + phase, 2.18f,
                        104, seed + 4720, size, 0.56f, color);
                for (int tick = 0; tick < 28; tick++) {
                    float angle = (float) (Math.PI * 2.0 * tick / 28.0 - Math.PI * 0.5 + phase * 0.2f);
                    float innerX = centerX + (float) Math.cos(angle) * width * 0.31f;
                    float innerY = dialY + (float) Math.sin(angle) * height * 0.108f;
                    float outerX = centerX + (float) Math.cos(angle) * width * 0.36f;
                    float outerY = dialY + (float) Math.sin(angle) * height * 0.132f;
                    addStructureLine(targets, innerX, innerY, outerX, outerY,
                            tick % 7 == 0 ? 7 : 3, seed + 4860 + tick * 17,
                            size, tick % 7 == 0 ? 0.72f : 0.35f,
                            tick % 7 == 0 ? hotColor : color);
                }
                addStructureLine(targets, centerX, height * 0.13f,
                        centerX + (result ? width * 0.12f : -width * 0.12f),
                        height * 0.92f, 96, seed + 5180, size, 0.45f, color);
                break;
            }
            case SENSOR: {
                float originY = height * 0.90f;
                for (int ray = 0; ray < 13; ray++) {
                    float t = ray / 12f;
                    float targetX = GlyphMath.mix(left, right, t);
                    float targetY = height * (0.16f + Math.abs(t - 0.5f) * 0.17f);
                    addStructureLine(targets, centerX, originY, targetX, targetY,
                            46, seed + 5390 + ray * 37, size,
                            ray == (result ? 8 : 4) ? 0.66f : 0.26f,
                            ray == (result ? 8 : 4) ? hotColor : color);
                }
                float scanY = height * (result ? 0.69f : 0.62f);
                addStructureLine(targets, left, scanY, right, scanY,
                        104, seed + 5890, size, 0.72f, hotColor);
                break;
            }
            case ANALYSIS:
            default: {
                float horizonY = height * (result ? 0.70f : 0.75f);
                addStructureLine(targets, left, horizonY, right, horizonY,
                        108, seed + 6120, size, 0.67f, hotColor);
                for (int lane = 0; lane < 9; lane++) {
                    float horizonX = GlyphMath.mix(width * 0.32f, width * 0.68f, lane / 8f);
                    float floorX = GlyphMath.mix(left, right, lane / 8f);
                    addStructureLine(targets, horizonX, horizonY, floorX,
                            height * 0.955f, 43, seed + 6250 + lane * 31,
                            size, lane % 2 == 0 ? 0.42f : 0.25f, color);
                }
                for (int row = 1; row <= 5; row++) {
                    float t = row / 5f;
                    float y = GlyphMath.mix(horizonY, height * 0.955f, t * t);
                    float half = GlyphMath.mix(width * 0.18f, width * 0.465f, t);
                    addStructureLine(targets, centerX - half, y, centerX + half, y,
                            66, seed + 6570 + row * 41, size,
                            0.28f + t * 0.18f, color);
                }
                for (int marker = 0; marker < 7; marker++) {
                    float x = GlyphMath.mix(width * 0.09f, width * 0.91f, marker / 6f);
                    float markerHeight = height * (0.035f
                            + GlyphMath.hash(marker, seed, 19f) * 0.085f);
                    addStructureLine(targets, x, horizonY - markerHeight,
                            x, horizonY, 16, seed + 6820 + marker * 23,
                            size, marker == (result ? 5 : 2) ? 0.70f : 0.37f,
                            marker == (result ? 5 : 2) ? hotColor : color);
                }
                break;
            }
        }
    }

    private static void addStructureArc(
            List<TargetGlyph> targets,
            float centerX,
            float centerY,
            float radiusX,
            float radiusY,
            float startAngle,
            float sweep,
            int count,
            int seed,
            float size,
            float alpha,
            int color
    ) {
        int safeCount = Math.max(3, count);
        for (int index = 0; index < safeCount; index++) {
            float t = index / (float) (safeCount - 1);
            float angle = startAngle + sweep * t;
            addStructureTarget(
                    targets,
                    centerX + (float) Math.cos(angle) * radiusX,
                    centerY + (float) Math.sin(angle) * radiusY,
                    seed + index,
                    size,
                    alpha,
                    color
            );
        }
    }

    private static void addTargetVerticalWave(
            List<TargetGlyph> targets,
            float baseX,
            float top,
            float bottom,
            float amplitude,
            float turns,
            float phase,
            int count,
            int seed,
            float size,
            float alpha,
            int color
    ) {
        int safeCount = Math.max(3, count);
        for (int index = 0; index < safeCount; index++) {
            float t = index / (float) (safeCount - 1);
            addStructureTarget(
                    targets,
                    baseX + (float) Math.sin(t * Math.PI * 2f * turns + phase) * amplitude,
                    GlyphMath.mix(top, bottom, t),
                    seed + index,
                    size,
                    alpha,
                    color
            );
        }
    }

    private static void addTargetHorizontalWave(
            List<TargetGlyph> targets,
            float left,
            float right,
            float baseY,
            float amplitude,
            float turns,
            float phase,
            int count,
            int seed,
            float size,
            float alpha,
            int color
    ) {
        int safeCount = Math.max(3, count);
        for (int index = 0; index < safeCount; index++) {
            float t = index / (float) (safeCount - 1);
            addStructureTarget(
                    targets,
                    GlyphMath.mix(left, right, t),
                    baseY + (float) Math.sin(t * Math.PI * 2f * turns + phase) * amplitude,
                    seed + index,
                    size,
                    alpha,
                    color
            );
        }
    }

    private static void clearStructureFromTextBands(
            List<TargetGlyph> targets,
            List<TextBand> bands,
            int width,
            int height
    ) {
        float horizontalClearance = width * 0.017f;
        float verticalClearance = height * 0.007f;
        for (int targetIndex = targets.size() - 1; targetIndex >= 0; targetIndex--) {
            TargetGlyph target = targets.get(targetIndex);
            if (target.text) continue;
            for (TextBand band : bands) {
                if (target.x >= band.bounds.left - horizontalClearance
                        && target.x <= band.bounds.right + horizontalClearance
                        && target.y >= band.bounds.top - verticalClearance
                        && target.y <= band.bounds.bottom + verticalClearance) {
                    targets.remove(targetIndex);
                    break;
                }
            }
        }
    }

    private static void addSemanticStructure(
            List<TargetGlyph> targets,
            RectF bounds,
            int width,
            int height,
            DemoCatalog.Theme theme,
            DemoCatalog.Event event,
            boolean result
    ) {
        int color = mixColor(theme.atmosphereColor, event.accent, result ? 0.58f : 0.46f);
        float centerX = bounds.centerX();
        float centerY = bounds.centerY();
        float size = width * 0.0106f;
        switch (theme.compositionStyle) {
            case FIGURE: {
                for (int i = 0; i < 68; i++) {
                    float t = i / 67f;
                    float y = GlyphMath.mix(bounds.top - height * 0.070f, bounds.bottom + height * 0.080f, t);
                    float spread = width * (0.235f + 0.115f * (float) Math.sin(t * Math.PI));
                    float pulse = (float) Math.sin(t * Math.PI * 5f) * width * 0.012f;
                    addStructureTarget(targets, centerX - spread - pulse, y, i, size, 0.43f, color);
                    addStructureTarget(targets, centerX + spread + pulse, y, i + 31, size, 0.43f, color);
                    if ((i % 4) == 0) {
                        addStructureTarget(targets, centerX + (float) Math.sin(t * Math.PI * 6f) * width * 0.015f, y, i + 61, size, 0.30f, color);
                    }
                }
                break;
            }
            case CORE: {
                for (int ring = 0; ring < 4; ring++) {
                    float rx = bounds.width() * (0.54f + ring * 0.090f);
                    float ry = bounds.height() * (0.54f + ring * 0.105f);
                    int count = 54 + ring * 16;
                    for (int i = 0; i < count; i++) {
                        if ((i + ring * 2) % 8 == 0) continue;
                        float a = (float) (Math.PI * 2.0 * i / count);
                        addStructureTarget(
                                targets,
                                centerX + (float) Math.cos(a) * rx,
                                centerY + (float) Math.sin(a) * ry,
                                i + ring * 73,
                                size,
                                0.42f - ring * 0.055f,
                                color
                        );
                    }
                }
                break;
            }
            case ORBITAL_BAND: {
                for (int band = 0; band < 2; band++) {
                    int count = band == 0 ? 128 : 96;
                    for (int i = 0; i < count; i++) {
                        float a = (float) (Math.PI * 2.0 * i / count + band * 0.21);
                        if (Math.abs(Math.sin(a)) < 0.12f && Math.cos(a) > 0f && i % 3 != 0) continue;
                        float rx = bounds.width() * (0.57f + band * 0.12f + 0.04f * (float) Math.sin(a * 3f));
                        float ry = bounds.height() * (0.62f + band * 0.11f);
                        addStructureTarget(
                                targets,
                                centerX + (float) Math.cos(a) * rx,
                                centerY + (float) Math.sin(a) * ry,
                                i + band * 131,
                                size,
                                band == 0 ? 0.43f : 0.27f,
                                color
                        );
                    }
                }
                break;
            }
            case ARCHITECTURE: {
                for (int i = 0; i < 72; i++) {
                    float y = GlyphMath.mix(bounds.top - height * 0.055f, bounds.bottom + height * 0.065f, i / 71f);
                    if (i % 5 != 1) {
                        addStructureTarget(targets, bounds.left - width * 0.067f, y, i, size, 0.43f, color);
                        addStructureTarget(targets, bounds.right + width * 0.067f, y, i + 37, size, 0.36f, color);
                    }
                    if (i % 6 == 0) addStructureTarget(targets, bounds.left - width * 0.025f, y, i + 73, size, 0.28f, color);
                }
                for (int rail = 0; rail < 2; rail++) {
                    float y = rail == 0 ? bounds.top - height * 0.038f : bounds.bottom + height * 0.043f;
                    for (int i = 0; i < 42; i++) {
                        if (i > 14 && i < 27 && rail == 0) continue;
                        float x = GlyphMath.mix(bounds.left - width * 0.048f, bounds.right + width * 0.048f, i / 41f);
                        addStructureTarget(targets, x, y, i + rail * 51, size, rail == 0 ? 0.36f : 0.24f, color);
                    }
                }
                break;
            }
            case SPLICE: {
                for (int i = 0; i < 76; i++) {
                    float t = i / 75f;
                    float y = GlyphMath.mix(bounds.top - height * 0.085f, bounds.bottom + height * 0.085f, t);
                    float wave = (float) Math.sin(t * Math.PI * 10f) * width * 0.072f;
                    addStructureTarget(targets, bounds.left - width * 0.059f + wave, y, i, size, 0.43f, color);
                    addStructureTarget(targets, bounds.right + width * 0.059f - wave, y, i + 43, size, 0.43f, color);
                    if (i % 3 == 0) addStructureTarget(targets, centerX + (float) Math.sin(t * Math.PI * 10f) * width * 0.009f, y, i + 89, size, 0.25f, color);
                }
                break;
            }
            case DIAL: {
                for (int ring = 0; ring < 3; ring++) {
                    int count = 112 - ring * 14;
                    float rx = bounds.width() * (0.57f + ring * 0.105f);
                    float ry = bounds.height() * (0.59f + ring * 0.105f);
                    for (int i = 0; i < count; i++) {
                        float angle = (float) (Math.PI * 2.0 * i / count - Math.PI * 0.5);
                        if ((i + ring * 3) % 11 == 0) continue;
                        addStructureTarget(
                                targets,
                                centerX + (float) Math.cos(angle) * rx,
                                centerY + (float) Math.sin(angle) * ry,
                                i + ring * 127,
                                size,
                                0.43f - ring * 0.075f,
                                color
                        );
                    }
                }
                for (int tick = 0; tick < 36; tick++) {
                    float angle = (float) (Math.PI * 2.0 * tick / 36.0 - Math.PI * 0.5);
                    float innerX = centerX + (float) Math.cos(angle) * bounds.width() * 0.46f;
                    float innerY = centerY + (float) Math.sin(angle) * bounds.height() * 0.47f;
                    float outerX = centerX + (float) Math.cos(angle) * bounds.width() * 0.52f;
                    float outerY = centerY + (float) Math.sin(angle) * bounds.height() * 0.53f;
                    addStructureLine(
                            targets, innerX, innerY, outerX, outerY,
                            tick % 3 == 0 ? 4 : 2,
                            tick * 7,
                            size,
                            tick % 3 == 0 ? 0.46f : 0.27f,
                            color
                    );
                }
                break;
            }
            case CASCADE: {
                float leftRail = bounds.left - width * 0.075f;
                float rightRail = bounds.right + width * 0.075f;
                for (int i = 0; i < 78; i++) {
                    float t = i / 77f;
                    float y = GlyphMath.mix(bounds.top - height * 0.065f, bounds.bottom + height * 0.075f, t);
                    if (i % 4 != 1) {
                        addStructureTarget(targets, leftRail, y, i, size, 0.44f, color);
                        addStructureTarget(targets, rightRail, y, i + 41, size, 0.34f, color);
                    }
                    if (i % 5 == 0) {
                        float stair = (i % 10 < 5 ? -1f : 1f) * width * (0.030f + (i % 3) * 0.012f);
                        addStructureLine(
                                targets,
                                centerX + stair,
                                y - height * 0.018f,
                                centerX - stair * 0.35f,
                                y + height * 0.018f,
                                5,
                                i + 83,
                                size,
                                0.32f,
                                color
                        );
                    }
                }
                for (int row = 0; row < 8; row++) {
                    float y = bounds.top - height * 0.042f + row * height * 0.023f;
                    float inset = row * width * 0.018f;
                    addStructureLine(
                            targets,
                            bounds.left - width * 0.025f + inset,
                            y,
                            bounds.right + width * 0.025f - inset,
                            y,
                            28 - row * 2,
                            row * 31,
                            size,
                            0.34f - row * 0.025f,
                            color
                    );
                }
                break;
            }
            case CONSTELLATION: {
                float[][] nodes = {
                        { centerX - bounds.width() * 0.56f, centerY - bounds.height() * 0.26f },
                        { centerX - bounds.width() * 0.30f, centerY + bounds.height() * 0.48f },
                        { centerX, centerY - bounds.height() * 0.58f },
                        { centerX + bounds.width() * 0.34f, centerY + bounds.height() * 0.44f },
                        { centerX + bounds.width() * 0.58f, centerY - bounds.height() * 0.18f },
                        { centerX, centerY + bounds.height() * 0.60f }
                };
                int[][] edges = {
                        { 0, 2 }, { 2, 4 }, { 0, 1 }, { 1, 5 }, { 5, 3 },
                        { 3, 4 }, { 1, 2 }, { 2, 3 }, { 0, 5 }, { 4, 5 }
                };
                for (int i = 0; i < nodes.length; i++) {
                    float x = nodes[i][0];
                    float y = nodes[i][1];
                    for (int ring = 0; ring < 3; ring++) {
                        int count = 12 + ring * 4;
                        float radiusX = width * (0.018f + ring * 0.011f);
                        float radiusY = height * (0.010f + ring * 0.005f);
                        for (int point = 0; point < count; point++) {
                            float angle = (float) (Math.PI * 2.0 * point / count);
                            addStructureTarget(
                                    targets,
                                    x + (float) Math.cos(angle) * radiusX,
                                    y + (float) Math.sin(angle) * radiusY,
                                    i * 43 + ring * 17 + point,
                                    size,
                                    0.46f - ring * 0.10f,
                                    color
                            );
                        }
                    }
                }
                for (int edge = 0; edge < edges.length; edge++) {
                    float[] from = nodes[edges[edge][0]];
                    float[] to = nodes[edges[edge][1]];
                    addStructureLine(
                            targets,
                            from[0], from[1], to[0], to[1],
                            22,
                            edge * 37,
                            size,
                            0.25f,
                            color
                    );
                }
                break;
            }
            case FIELD:
            default: {
                for (int row = -4; row <= 4; row++) {
                    float y = centerY + row * height * 0.056f;
                    for (int i = 0; i < 66; i++) {
                        if ((i + row + 13) % 5 == 0) continue;
                        float t = i / 65f;
                        float x = GlyphMath.mix(bounds.left - width * 0.089f, bounds.right + width * 0.089f, t);
                        float wave = (float) Math.sin(t * Math.PI * 5f + row * 0.77f) * height * 0.011f;
                        addStructureTarget(targets, x, y + wave, i + row * 29, size, 0.22f + 0.035f * (4 - Math.abs(row)), color);
                    }
                }
                break;
            }
        }
    }

    private static void addStructureLine(
            List<TargetGlyph> targets,
            float startX,
            float startY,
            float endX,
            float endY,
            int count,
            int seed,
            float size,
            float alpha,
            int color
    ) {
        int safeCount = Math.max(2, count);
        for (int i = 0; i < safeCount; i++) {
            float t = i / (float) (safeCount - 1);
            addStructureTarget(
                    targets,
                    GlyphMath.mix(startX, endX, t),
                    GlyphMath.mix(startY, endY, t),
                    seed + i,
                    size,
                    alpha,
                    color
            );
        }
    }

    private static void addStructureTarget(
            List<TargetGlyph> targets,
            float x,
            float y,
            int index,
            float size,
            float alpha,
            int color
    ) {
        char glyph = STRUCTURE_GLYPHS.charAt(Math.floorMod(index * 5 + 3, STRUCTURE_GLYPHS.length()));
        targets.add(new TargetGlyph(x, y, glyph, size, alpha, color, false, TargetRole.STRUCTURE));
    }

    /** Applies the same protected regions to focused runtime hardware without changing count. */
    private static void preserveTargetCountOutsideSafeZones(
            List<TargetGlyph> targets,
            int runtimeStart,
            int width,
            int height,
            int seed
    ) {
        if (runtimeStart >= targets.size()) return;
        List<TargetGlyph> accepted = new ArrayList<>(targets.size() - runtimeStart);
        List<TargetGlyph> rejected = new ArrayList<>();
        for (int index = runtimeStart; index < targets.size(); index++) {
            TargetGlyph target = targets.get(index);
            if (isRuntimeSafeZone(target.x, target.y, width, height)) rejected.add(target);
            else accepted.add(target);
        }
        if (rejected.isEmpty()) return;

        targets.subList(runtimeStart, targets.size()).clear();
        targets.addAll(accepted);
        for (int index = 0; index < rejected.size(); index++) {
            TargetGlyph target = rejected.get(index);
            targets.add(new TargetGlyph(
                    safeReplacementX(width, seed, index),
                    safeReplacementY(height, seed, index),
                    target.glyph,
                    target.size,
                    target.alpha,
                    target.color,
                    target.text,
                    target.role
            ));
        }
    }

    private static FittedLines fitWrappedLines(
            Paint paint,
            String text,
            float maxWidth,
            int preferredMaximumLines,
            float preferredSize,
            float minimumSize
    ) {
        paint.setTextSize(preferredSize);
        List<String> preferredLines = wrapTextFully(paint, text, maxWidth);
        if (preferredLines.size() <= preferredMaximumLines) {
            return new FittedLines(preferredLines, preferredSize);
        }

        paint.setTextSize(minimumSize);
        List<String> minimumLines = wrapTextFully(paint, text, maxWidth);
        if (minimumLines.size() > preferredMaximumLines) {
            // Preserve every character rather than silently replacing the tail with an ellipsis.
            return new FittedLines(minimumLines, minimumSize);
        }

        float low = minimumSize;
        float high = preferredSize;
        List<String> bestLines = minimumLines;
        for (int iteration = 0; iteration < 12; iteration++) {
            float candidateSize = (low + high) * 0.5f;
            paint.setTextSize(candidateSize);
            List<String> candidateLines = wrapTextFully(paint, text, maxWidth);
            if (candidateLines.size() <= preferredMaximumLines) {
                low = candidateSize;
                bestLines = candidateLines;
            } else {
                high = candidateSize;
            }
        }
        return new FittedLines(bestLines, low);
    }

    /** Wraps all supplied content. Long tokens are split, never visually clipped. */
    private static List<String> wrapTextFully(Paint paint, String text, float maxWidth) {
        String clean = text == null ? "" : text.trim();
        List<String> lines = new ArrayList<>();
        if (clean.isEmpty()) {
            lines.add("");
            return lines;
        }

        StringBuilder line = new StringBuilder();
        for (String rawWord : clean.split("\\s+")) {
            String word = rawWord;
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (paint.measureText(candidate) <= maxWidth) {
                line.setLength(0);
                line.append(candidate);
                continue;
            }

            if (line.length() > 0) {
                lines.add(line.toString());
                line.setLength(0);
            }
            while (!word.isEmpty() && paint.measureText(word) > maxWidth) {
                int split = longestFittingPrefix(paint, word, maxWidth);
                lines.add(word.substring(0, split));
                word = word.substring(split);
            }
            if (!word.isEmpty()) line.append(word);
        }
        if (line.length() > 0) lines.add(line.toString());
        return lines;
    }

    private static int longestFittingPrefix(Paint paint, String value, float maxWidth) {
        int low = 1;
        int high = value.length();
        while (low < high) {
            int middle = (low + high + 1) / 2;
            if (paint.measureText(value, 0, middle) <= maxWidth) low = middle;
            else high = middle - 1;
        }
        return Math.max(1, low);
    }

    private static List<MorphGlyph> buildMorphGlyphs(
            List<GlyphPoint> sources,
            TargetLayout eventLayout,
            TargetLayout resultLayout,
            int width,
            int height,
            DemoCatalog.Theme theme,
            DemoCatalog.Event event,
            List<MorphGlyph> previousMorphs,
            boolean previousResult
    ) {
        TargetGlyph[] eventAssignments = assignCoherentTargets(sources, eventLayout.targets, width, height);
        TargetGlyph[] resultAssignments = assignCoherentTargets(sources, resultLayout.targets, width, height);
        List<MorphGlyph> morphs = new ArrayList<>(sources.size());
        float focusX = width * theme.atmosphereX;
        float focusY = height * theme.atmosphereY;
        float diagonal = Math.max(1f, (float) Math.hypot(width, height));

        for (int i = 0; i < sources.size(); i++) {
            GlyphPoint source = sources.get(i);
            TargetGlyph eventTarget = eventAssignments[i];
            if (eventTarget == null) {
                eventTarget = buildFillerTarget(
                        source,
                        eventLayout,
                        width,
                        height,
                        theme,
                        event,
                        i,
                        false
                );
            }
            TargetGlyph resultTarget = resultAssignments[i];
            if (resultTarget == null) {
                resultTarget = buildFillerTarget(
                        source,
                        resultLayout,
                        width,
                        height,
                        theme,
                        event,
                        i,
                        true
                );
            }

            // Meaning is carried by where the source symbol settles, never by replacing it with
            // a literal event character. Many of these retained symbols form each macro-stroke.
            eventTarget = withGlyph(eventTarget, source.glyph);
            resultTarget = withGlyph(resultTarget, source.glyph);

            TargetGlyph previousTarget = previousMorphs != null && i < previousMorphs.size()
                    ? (previousResult
                            ? previousMorphs.get(i).resultTarget
                            : previousMorphs.get(i).eventTarget)
                    : eventTarget;

            float distance = (float) Math.hypot(source.x - focusX, source.y - focusY) / diagonal;
            float noise = GlyphMath.hash(i, source.x, source.y);
            float roleDelay = eventTarget.role.delay;
            float transformationOrder = transformationOrder(
                    theme,
                    source,
                    eventTarget,
                    width,
                    height
            );
            morphs.add(new MorphGlyph(
                    source,
                    previousTarget,
                    eventTarget,
                    resultTarget,
                    noise * (float) Math.PI * 2f,
                    width * (0.032f + noise * 0.092f) * theme.motionStrength,
                    0.01f + 0.205f * transformationOrder + roleDelay + 0.030f * noise,
                    0.52f + 0.14f * (1f - distance) + 0.055f * noise
            ));
        }
        return morphs;
    }

    private static float transformationOrder(
            DemoCatalog.Theme theme,
            GlyphPoint source,
            TargetGlyph target,
            int width,
            int height
    ) {
        float centerX = width * 0.5f;
        float centerY = height * 0.515f;
        if (target.text) {
            switch (target.role) {
                case TITLE:
                    // The headline locks from its center outward like a resolved identification.
                    return GlyphMath.clamp01(Math.abs(target.x - centerX) / (width * 0.48f));
                case SUMMARY:
                    // Supporting intelligence is decoded in reading order.
                    return GlyphMath.clamp01(
                            target.x / Math.max(1f, width) * 0.72f
                                    + target.y / Math.max(1f, height) * 0.28f
                    );
                case META:
                case ACTION:
                default:
                    return GlyphMath.clamp01(target.x / Math.max(1f, width));
            }
        }

        float dx = source.x - centerX;
        float dy = source.y - centerY;
        float radius = (float) Math.hypot(dx, dy);
        switch (systemIntentFor(theme)) {
            case DEFENSE:
                // A top-down threat acquisition sweep.
                return GlyphMath.clamp01(source.y / Math.max(1f, height));
            case REACTOR:
                // Power ignition travels from the core into the conduits.
                return GlyphMath.clamp01(radius / Math.max(width * 0.72f, height * 0.38f));
            case NAVIGATION: {
                // Route nodes synchronize clockwise.
                float angle = (float) Math.atan2(dy, dx) + (float) Math.PI;
                return angle / ((float) Math.PI * 2f);
            }
            case NETWORK:
                // Packets enter from the nearest edge and route toward the bus.
                return GlyphMath.clamp01(
                        Math.min(source.x, width - source.x) / Math.max(1f, width * 0.5f)
                );
            case BIOMETRIC:
                // A deliberate strand-by-strand decode.
                return GlyphMath.clamp01(source.y / Math.max(1f, height));
            case VAULT:
                // The central seal releases before its outer gates.
                return GlyphMath.clamp01(Math.abs(source.x - centerX) / Math.max(1f, width * 0.5f));
            case TEMPORAL: {
                float angle = (float) Math.atan2(dy, dx) + (float) Math.PI * 0.5f;
                if (angle < 0f) angle += (float) Math.PI * 2f;
                return (angle % ((float) Math.PI * 2f)) / ((float) Math.PI * 2f);
            }
            case SENSOR:
                // Sensor rays acquire from the bottom origin toward the upper field.
                return 1f - GlyphMath.clamp01(source.y / Math.max(1f, height));
            case ANALYSIS:
            default:
                // The analytical plane rises from its perspective deck.
                return 1f - GlyphMath.clamp01(source.y / Math.max(1f, height));
        }
    }

    private static TargetGlyph withGlyph(TargetGlyph target, char glyph) {
        return new TargetGlyph(
                target.x,
                target.y,
                glyph,
                target.size,
                target.alpha,
                target.color,
                target.text,
                target.role
        );
    }

    private static TargetGlyph[] assignCoherentTargets(
            List<GlyphPoint> sources,
            List<TargetGlyph> targets,
            int width,
            int height
    ) {
        int sourceCount = sources.size();
        int targetCount = targets.size();
        float[] sourceX = new float[sourceCount];
        float[] sourceY = new float[sourceCount];
        float[] sourceAlpha = new float[sourceCount];
        float[] targetX = new float[targetCount];
        float[] targetY = new float[targetCount];
        boolean[] textTarget = new boolean[targetCount];

        for (int i = 0; i < sourceCount; i++) {
            GlyphPoint source = sources.get(i);
            sourceX[i] = source.x;
            sourceY[i] = source.y;
            // Nearby particles still win, but simple marks produce cleaner collective strokes
            // than dense @/8/G shapes when several symbols sit only a few pixels apart.
            sourceAlpha[i] = GlyphMath.clamp01(
                    source.alpha * 0.58f + semanticClarity(source.glyph) * 0.42f
            );
        }
        for (int i = 0; i < targetCount; i++) {
            TargetGlyph target = targets.get(i);
            targetX[i] = target.x;
            targetY[i] = target.y;
            textTarget[i] = target.text;
        }

        int[] targetToSource = SpatialGlyphMatcher.match(
                sourceX,
                sourceY,
                sourceAlpha,
                targetX,
                targetY,
                textTarget,
                width,
                height
        );
        TargetGlyph[] assignments = new TargetGlyph[sourceCount];
        for (int targetIndex = 0; targetIndex < targetCount; targetIndex++) {
            int sourceIndex = targetToSource[targetIndex];
            if (sourceIndex >= 0) assignments[sourceIndex] = targets.get(targetIndex);
        }
        return assignments;
    }

    private static float semanticClarity(char glyph) {
        if (".·,:;'`".indexOf(glyph) >= 0) return 1f;
        if ("|/\\-_=+<>[](){}".indexOf(glyph) >= 0) return 0.78f;
        if ("1iItfLC".indexOf(glyph) >= 0) return 0.54f;
        return 0.24f;
    }

    private static TargetGlyph buildFillerTarget(
            GlyphPoint source,
            TargetLayout layout,
            int width,
            int height,
            DemoCatalog.Theme theme,
            DemoCatalog.Event event,
            int index,
            boolean result
    ) {
        float focusX = width * theme.atmosphereX;
        float focusY = height * theme.atmosphereY;
        float vx = source.x - focusX;
        float vy = source.y - focusY;
        float radius = Math.max(1f, (float) Math.hypot(vx, vy));
        float angle = (float) Math.atan2(vy, vx);
        float noise = GlyphMath.hash(index, source.x, result ? 31f : 23f);
        float x = source.x;
        float y = source.y;
        float seed = event.id.length() * 0.31f + (result ? 0.42f : 0f);

        switch (theme.motionStyle) {
            case ORBITAL: {
                float rotation = (0.08f + noise * 0.13f) * (noise > 0.5f ? 1f : -1f);
                float scale = 0.96f + noise * 0.10f;
                float ca = (float) Math.cos(rotation);
                float sa = (float) Math.sin(rotation);
                x = focusX + (vx * ca - vy * sa) * scale;
                y = focusY + (vx * sa + vy * ca) * scale;
                break;
            }
            case CIRCUIT: {
                float gridX = width * 0.034f;
                float gridY = height * 0.018f;
                float gridTargetX = Math.round((source.x + (noise - 0.5f) * width * 0.045f) / gridX) * gridX;
                float gridTargetY = Math.round((source.y + (noise - 0.5f) * height * 0.025f) / gridY) * gridY;
                x = GlyphMath.mix(source.x, gridTargetX, 0.58f);
                y = GlyphMath.mix(source.y, gridTargetY, 0.58f);
                break;
            }
            case RADIAL: {
                float ring = radius * (0.96f + noise * 0.11f);
                x = focusX + (float) Math.cos(angle + seed * 0.055f) * ring;
                y = focusY + (float) Math.sin(angle + seed * 0.055f) * ring;
                break;
            }
            case BLOOM: {
                float petal = (float) Math.sin(angle * 6f + seed) * width * 0.024f;
                float scale = 0.97f + noise * 0.09f + petal / Math.max(width, radius) * 0.46f;
                x = focusX + vx * scale;
                y = focusY + vy * scale;
                break;
            }
            case WAVE: {
                x += (float) Math.sin(source.y * 0.0105f + seed + noise * 4.5f) * width * 0.030f;
                y += (float) Math.sin(source.x * 0.0090f - seed + noise * 3.5f) * height * 0.010f;
                break;
            }
            case FOLD: {
                float depth = (source.y - focusY) / Math.max(1f, height);
                float fold = (noise > 0.5f ? 1f : -1f) * Math.abs(depth) * width * 0.075f;
                x = focusX + vx * (0.95f + noise * 0.08f) + fold;
                y = focusY + vy * (0.97f + noise * 0.055f);
                break;
            }
            case FLOW:
            default:
                x += (float) Math.sin(source.y * 0.009f + seed + noise * 4f) * width * 0.030f;
                y += (float) Math.cos(source.x * 0.008f + seed + noise * 5f) * height * 0.010f;
                break;
        }

        float semanticDistance = GlyphMath.distanceToRect(
                source.x,
                source.y,
                layout.semanticBounds.left,
                layout.semanticBounds.top,
                layout.semanticBounds.right,
                layout.semanticBounds.bottom
        );
        float deformationInfluence = GlyphMath.localInfluence(
                semanticDistance,
                width * 0.30f,
                identityFloorFor(theme.compositionStyle)
        );
        // Unrecruited particles retain the artwork instead of performing unrelated theme
        // choreography. Only a small local response acknowledges the arriving event.
        float ambientResponse = deformationInfluence * 0.12f;
        x = GlyphMath.mix(source.x, x, ambientResponse);
        y = GlyphMath.mix(source.y, y, ambientResponse);

        float[] warped = warpFillerAroundBands(x, y, layout, width, height, noise);
        x = warped[0];
        y = warped[1];
        x = Math.max(width * 0.018f, Math.min(width * 0.982f, x));
        y = Math.max(height * 0.035f, Math.min(height * 0.975f, y));

        float alpha = GlyphMath.clamp01(source.alpha * (result ? 0.70f : 0.78f));
        int color = mixColor(theme.atmosphereColor, event.accent, result ? 0.46f : 0.35f);
        return new TargetGlyph(
                x,
                y,
                source.glyph,
                source.size * (0.93f + noise * 0.15f),
                alpha,
                color,
                false,
                TargetRole.STRUCTURE
        );
    }

    private static float identityFloorFor(DemoCatalog.CompositionStyle style) {
        switch (style) {
            case CORE:
                return 0.38f;
            case ORBITAL_BAND:
            case DIAL:
                return 0.42f;
            case ARCHITECTURE:
            case CASCADE:
                return 0.34f;
            case SPLICE:
                return 0.36f;
            case FIELD:
            case CONSTELLATION:
                return 0.30f;
            case FIGURE:
            default:
                return 0.32f;
        }
    }

    private static float[] warpFillerAroundBands(
            float x,
            float y,
            TargetLayout layout,
            int width,
            int height,
            float noise
    ) {
        float px = x;
        float py = y;
        for (TextBand band : layout.textBands) {
            if (!band.bounds.contains(px, py)) continue;
            float marginX = width * (0.006f + noise * 0.007f);
            float marginY = height * (0.0025f + noise * 0.0035f);
            switch (band.pushAxis) {
                case HORIZONTAL: {
                    float toLeft = Math.abs(px - band.bounds.left);
                    float toRight = Math.abs(band.bounds.right - px);
                    px = toLeft <= toRight ? band.bounds.left - marginX : band.bounds.right + marginX;
                    break;
                }
                case VERTICAL: {
                    float toTop = Math.abs(py - band.bounds.top);
                    float toBottom = Math.abs(band.bounds.bottom - py);
                    py = toTop <= toBottom ? band.bounds.top - marginY : band.bounds.bottom + marginY;
                    break;
                }
                case RADIAL:
                default: {
                    float dx = px - layout.centerX;
                    float dy = py - layout.centerY;
                    float nx = dx / Math.max(1f, band.bounds.width() * 0.5f);
                    float ny = dy / Math.max(1f, band.bounds.height() * 0.5f);
                    if (Math.abs(nx) > Math.abs(ny)) {
                        px = dx < 0f ? band.bounds.left - marginX : band.bounds.right + marginX;
                    } else {
                        py = dy < 0f ? band.bounds.top - marginY : band.bounds.bottom + marginY;
                    }
                    break;
                }
            }
            break;
        }
        return new float[] { px, py };
    }

    private static void drawMorphField(
            Canvas canvas,
            Scene scene,
            ExperienceController.Frame frame,
            long nowMs
    ) {
        float reveal = frame.revealProgress;
        if (reveal <= 0f) return;

        Paint paint = scene.glyphPaint;
        paint.setTypeface(MONO);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setFakeBoldText(false);
        char[] one = scene.glyphBuffer;
        float seconds = nowMs / 1000f;
        float focusX = scene.width * scene.theme.atmosphereX;
        float focusY = scene.height * scene.theme.atmosphereY;
        float result = frame.resultProgress;
        float handoff = GlyphMath.smooth(reveal / 0.16f);
        boolean retargeting = frame.eventProgress < 0.999f;
        float[] motionPoint = scene.motionScratch;
        List<MorphGlyph> morphGlyphs = scene.morphGlyphs;
        float paletteProgress = retargeting ? frame.eventProgress : reveal;
        int paletteBridge = mixColor(
                scene.theme.atmosphereColor,
                scene.accent,
                0.18f * GlyphMath.clamp01(paletteProgress)
        );
        int secondaryColor = fluidSecondaryColor(paletteBridge);
        int tertiaryColor = fluidTertiaryColor(paletteBridge);

        for (int glyphIndex = 0; glyphIndex < morphGlyphs.size(); glyphIndex++) {
            MorphGlyph glyph = morphGlyphs.get(glyphIndex);
            float local = GlyphMath.staggeredProgress(reveal, glyph.delay, glyph.duration);
            if (local <= 0f && handoff <= 0f) continue;
            float recruit = GlyphMath.smooth(local / 0.22f);
            float move = GlyphMath.easeOutCubic((local - 0.10f) / 0.90f);
            resolveMotion(
                    motionPoint,
                    scene.theme.motionStyle,
                    glyph.source.x,
                    glyph.source.y,
                    glyph.eventTarget.x,
                    glyph.eventTarget.y,
                    move,
                    glyph.phase,
                    glyph.arc,
                    focusX,
                    focusY,
                    scene.width,
                    scene.height
            );
            float eventX = motionPoint[0];
            float eventY = motionPoint[1];

            // Before assembly, an outward pulse recruits the source topology from the artwork's
            // focal point. It is the notification-arrival signal, not decorative idle motion.
            float sourceRadius = Math.max(1f, (float) Math.hypot(
                    glyph.source.x - focusX,
                    glyph.source.y - focusY
            ));
            float arrivalPulse = (float) Math.sin(Math.PI * recruit) * (1f - move);
            eventX += (glyph.source.x - focusX) / sourceRadius
                    * scene.width * 0.016f * arrivalPulse;
            eventY += (glyph.source.y - focusY) / sourceRadius
                    * scene.width * 0.016f * arrivalPulse;

            float semanticProgress = local;
            if (retargeting) {
                float retargetDelay = glyph.eventTarget.role.delay * 0.42f
                        + 0.075f * GlyphMath.hash(glyphIndex, glyph.phase, 47f);
                semanticProgress = GlyphMath.staggeredProgress(
                        frame.eventProgress,
                        retargetDelay,
                        0.76f
                );
                resolveMotion(
                        motionPoint,
                        scene.theme.motionStyle,
                        glyph.previousTarget.x,
                        glyph.previousTarget.y,
                        glyph.eventTarget.x,
                        glyph.eventTarget.y,
                        GlyphMath.easeOutCubic(semanticProgress),
                        glyph.phase + 1.17f,
                        glyph.arc * 0.62f,
                        focusX,
                        focusY,
                        scene.width,
                        scene.height
                );
                eventX = motionPoint[0];
                eventY = motionPoint[1];
            }

            float x = eventX;
            float y = eventY;
            if (result > 0f) {
                resolveMotion(
                        motionPoint,
                        scene.theme.motionStyle,
                        glyph.eventTarget.x,
                        glyph.eventTarget.y,
                        glyph.resultTarget.x,
                        glyph.resultTarget.y,
                        GlyphMath.easeOutCubic(result),
                        glyph.phase + 2.31f,
                        glyph.arc * 0.54f,
                        focusX,
                        focusY,
                        scene.width,
                        scene.height
                );
                x = motionPoint[0];
                y = motionPoint[1];
            }

            TargetGlyph activeTarget = result < 0.5f ? glyph.eventTarget : glyph.resultTarget;
            if (frame.listening) {
                float centerY = scene.height * 0.88f;
                float dx = x - scene.width * 0.5f;
                float dy = y - centerY;
                float distance = Math.max(1f, (float) Math.hypot(dx, dy));
                float pulse = (float) Math.sin(seconds * 6.5f - distance * 0.020f + glyph.phase);
                float influence = (float) Math.exp(-distance / Math.max(1f, scene.width * 0.42f));
                x += dx / distance * pulse * scene.width * 0.010f * influence;
                y += dy / distance * pulse * scene.width * 0.010f * influence;
            }
            if (!activeTarget.text && semanticProgress > 0.78f) {
                applyOperationalMotion(
                        motionPoint,
                        systemIntentFor(scene.theme),
                        x,
                        y,
                        seconds,
                        glyph.phase,
                        scene.width,
                        scene.height,
                        GlyphMath.smooth((semanticProgress - 0.78f) / 0.22f)
                );
                x = motionPoint[0];
                y = motionPoint[1];
            }

            float eventBlend = retargeting
                    ? semanticProgress
                    : GlyphMath.smooth((local - 0.20f) / 0.62f);
            float resultBlend = GlyphMath.smooth((result - 0.14f) / 0.72f);
            float originSize = retargeting ? glyph.previousTarget.size : glyph.source.size;
            float originAlpha = retargeting
                    ? glyph.previousTarget.alpha
                    : glyph.source.alpha * 0.94f;
            int originColor;
            if (eventBlend >= 0.999f) {
                // The origin is fully discarded at this point; avoid a sine and color mix
                // for every settled particle during the operational tail.
                originColor = glyph.eventTarget.color;
            } else if (retargeting) {
                originColor = glyph.previousTarget.color;
            } else {
                float originHue = 0.5f + 0.5f * (float) Math.sin(
                        glyph.source.y * 0.0042f + glyph.source.x * 0.0015f
                                - seconds * 0.27f + glyph.phase * 0.19f
                );
                originColor = mixColor(
                        scene.theme.atmosphereColor,
                        secondaryColor,
                        0.10f + originHue * 0.62f
                );
            }
            float size = GlyphMath.mix(originSize, glyph.eventTarget.size, eventBlend);
            size = GlyphMath.mix(size, glyph.resultTarget.size, resultBlend);
            float alpha = GlyphMath.mix(originAlpha, glyph.eventTarget.alpha, eventBlend);
            alpha = GlyphMath.mix(alpha, glyph.resultTarget.alpha, resultBlend) * handoff;
            if (!activeTarget.text && semanticProgress > 0.78f) {
                float operationalPulse = 0.84f + 0.16f * (0.5f + 0.5f
                        * (float) Math.sin(seconds * 2.4f + glyph.phase));
                alpha *= operationalPulse;
            }
            int eventColor = eventBlend >= 0.999f
                    ? glyph.eventTarget.color
                    : mixColor(originColor, glyph.eventTarget.color, eventBlend);
            int finalColor = resultBlend <= 0.001f
                    ? eventColor
                    : mixColor(eventColor, glyph.resultTarget.color, resultBlend);
            if (!activeTarget.text) {
                float structureHue = 0.5f + 0.5f * (float) Math.sin(
                        seconds * 0.31f + activeTarget.y * 0.0040f + glyph.phase * 0.37f
                );
                int movingStructureColor = mixColor(
                        secondaryColor,
                        tertiaryColor,
                        structureHue
                );
                // The hardware retains a low-amplitude color current after settling; semantic
                // strokes deliberately skip this branch and remain stationary and neutral.
                finalColor = mixColor(
                        finalColor,
                        movingStructureColor,
                        0.10f + 0.10f * (1f - eventBlend)
                );
            }

            // Semantic glyphs briefly bloom as they lock to the baseline, then become perfectly
            // still and crisp for reading. Structural glyphs settle more quietly.
            float settleCenter = activeTarget.text ? 0.78f : 0.72f;
            float settleWidth = activeTarget.text ? 0.13f : 0.16f;
            float settleDistance = (semanticProgress - settleCenter) / settleWidth;
            float settlePulse = (float) Math.exp(-settleDistance * settleDistance);
            size *= 1f + settlePulse * (activeTarget.text ? 0.075f : 0.035f);

            if (activeTarget.text && local > 0.88f) {
                alpha = Math.max(alpha, activeTarget.alpha * 0.98f);
            }
            if (alpha <= 0.012f) continue;

            if (result <= 0.001f
                    && glyphIndex % 7 == 0
                    && semanticProgress > 0.07f
                    && semanticProgress < 0.94f) {
                float trailVisibility = (float) Math.sin(Math.PI * semanticProgress)
                        * handoff * (activeTarget.text ? 0.15f : 0.10f);
                float originX = retargeting ? glyph.previousTarget.x : glyph.source.x;
                float originY = retargeting ? glyph.previousTarget.y : glyph.source.y;
                float echoX = GlyphMath.mix(originX, eventX, 0.72f);
                float echoY = GlyphMath.mix(originY, eventY, 0.72f);
                paint.setFakeBoldText(false);
                paint.setTextSize(GlyphMath.mix(originSize, size, 0.56f));
                paint.setColor(mixColor(secondaryColor, finalColor, 0.42f));
                paint.setAlpha(Math.min(255, Math.round(alpha * trailVisibility * 255f)));
                one[0] = retargeting ? glyph.previousTarget.glyph : glyph.source.glyph;
                canvas.drawText(one, 0, 1, echoX, echoY, paint);
            }

            paint.setFakeBoldText(activeTarget.text);
            paint.setTextSize(size);
            paint.setColor(finalColor);

            float clarity = GlyphMath.smooth((semanticProgress - 0.56f) / 0.30f);
            if (activeTarget.text && clarity > 0f) {
                paint.setTextSize(size * 1.26f);
                paint.setColor(mixColor(scene.accent, finalColor, 0.44f));
                paint.setAlpha(Math.min(255, Math.round(alpha * clarity * 0.11f * 255f)));
                one[0] = result > 0.45f
                        ? glyph.resultTarget.glyph
                        : glyph.eventTarget.glyph;
                canvas.drawText(one, 0, 1, x, y, paint);
                paint.setTextSize(size);
                paint.setColor(finalColor);
            } else if (!activeTarget.text && alpha > 0.42f && glyphIndex % 5 == 0) {
                paint.setFakeBoldText(false);
                paint.setTextSize(size * 1.42f);
                paint.setColor(mixColor(
                        finalColor,
                        scene.accent,
                        0.48f * GlyphMath.smooth(semanticProgress)
                ));
                paint.setAlpha(Math.min(255, Math.round(alpha * 0.10f * 255f)));
                one[0] = result > 0.45f
                        ? glyph.resultTarget.glyph
                        : glyph.eventTarget.glyph;
                canvas.drawText(one, 0, 1, x, y, paint);
                paint.setTextSize(size);
                paint.setColor(finalColor);
            }

            char eventFrom = retargeting ? glyph.previousTarget.glyph : glyph.source.glyph;
            float eventCharBlend = retargeting
                    ? GlyphMath.smooth((semanticProgress - 0.32f) / 0.28f)
                    : GlyphMath.smooth(
                            (local - charStartFor(glyph.eventTarget.role)) / 0.16f
                    );
            if (result <= 0.001f) {
                drawGlyphTransition(
                        canvas,
                        paint,
                        one,
                        eventFrom,
                        glyph.eventTarget.glyph,
                        eventCharBlend,
                        x,
                        y,
                        alpha
                );
            } else {
                char eventGlyph = eventCharBlend >= 0.5f
                        ? glyph.eventTarget.glyph
                        : glyph.source.glyph;
                float resultCharBlend = GlyphMath.smooth((result - 0.40f) / 0.18f);
                drawGlyphTransition(
                        canvas,
                        paint,
                        one,
                        eventGlyph,
                        glyph.resultTarget.glyph,
                        resultCharBlend,
                        x,
                        y,
                        alpha
                );
            }
        }
    }

    /** Keeps resolved system hardware alive while semantic glyphs remain perfectly stationary. */
    private static void applyOperationalMotion(
            float[] output,
            SystemIntent intent,
            float x,
            float y,
            float seconds,
            float phase,
            int width,
            int height,
            float strength
    ) {
        float unit = width / 720f;
        float centerX = width * 0.5f;
        float centerY = height * 0.515f;
        float dx = x - centerX;
        float dy = y - centerY;
        float radius = Math.max(1f, (float) Math.hypot(dx, dy));
        float motionX = x;
        float motionY = y;
        switch (intent) {
            case DEFENSE:
                motionX += (float) Math.sin(seconds * 1.7f - y * 0.018f + phase)
                        * 1.2f * unit * strength;
                break;
            case REACTOR: {
                float tangent = (float) Math.sin(seconds * 2.1f + phase)
                        * 1.8f * unit * strength;
                motionX += -dy / radius * tangent;
                motionY += dx / radius * tangent;
                break;
            }
            case NAVIGATION:
                motionX += (float) Math.cos(seconds * 0.9f + phase) * 1.4f * unit * strength;
                motionY += (float) Math.sin(seconds * 0.9f + phase) * 1.0f * unit * strength;
                break;
            case NETWORK:
                if (Math.sin(phase) >= 0f) {
                    motionX += (float) Math.sin(seconds * 2.8f + phase) * 1.4f * unit * strength;
                } else {
                    motionY += (float) Math.sin(seconds * 2.8f + phase) * 1.4f * unit * strength;
                }
                break;
            case BIOMETRIC:
                motionX += (float) Math.sin(seconds * 1.35f + y * 0.012f + phase)
                        * 1.7f * unit * strength;
                break;
            case VAULT:
                motionX += Math.signum(dx) * (float) Math.sin(seconds * 1.15f + phase)
                        * 0.9f * unit * strength;
                break;
            case TEMPORAL: {
                float tangent = 1.3f * unit * strength;
                motionX += -dy / radius * tangent;
                motionY += dx / radius * tangent;
                break;
            }
            case SENSOR:
                motionY += (float) Math.sin(seconds * 2.0f - x * 0.014f + phase)
                        * 1.5f * unit * strength;
                break;
            case ANALYSIS:
            default:
                motionY += (float) Math.sin(seconds * 1.55f + x * 0.011f + phase)
                        * 1.2f * unit * strength;
                break;
        }
        output[0] = motionX;
        output[1] = motionY;
    }

    /** Resolves one theme-specific path without allocating inside the particle loop. */
    private static void resolveMotion(
            float[] output,
            DemoCatalog.MotionStyle style,
            float fromX,
            float fromY,
            float toX,
            float toY,
            float progress,
            float phase,
            float arc,
            float focusX,
            float focusY,
            int width,
            int height
    ) {
        float p = GlyphMath.clamp01(progress);
        float wave = (float) Math.sin(Math.PI * p);
        float x;
        float y;
        switch (style) {
            case CIRCUIT: {
                boolean horizontalFirst = Math.sin(phase) >= 0f;
                float first = GlyphMath.smooth(Math.min(1f, p * 1.78f));
                float second = GlyphMath.smooth(Math.max(0f, (p - 0.36f) / 0.64f));
                if (horizontalFirst) {
                    x = GlyphMath.mix(fromX, toX, first);
                    y = GlyphMath.mix(fromY, toY, second);
                } else {
                    y = GlyphMath.mix(fromY, toY, first);
                    x = GlyphMath.mix(fromX, toX, second);
                }
                float corner = (float) Math.sin(phase * 1.7f) * width * 0.006f * wave;
                x += horizontalFirst ? 0f : corner;
                y += horizontalFirst ? corner : 0f;
                break;
            }
            case ORBITAL: {
                float midX = (fromX + toX) * 0.5f;
                float midY = (fromY + toY) * 0.5f;
                float vx = midX - focusX;
                float vy = midY - focusY;
                float length = Math.max(1f, (float) Math.hypot(vx, vy));
                float direction = Math.sin(phase) >= 0f ? 1f : -1f;
                float bend = Math.min(arc, (float) Math.hypot(toX - fromX, toY - fromY) * 0.34f);
                float controlX = midX - vy / length * bend * direction;
                float controlY = midY + vx / length * bend * direction;
                float u = 1f - p;
                x = u * u * fromX + 2f * u * p * controlX + p * p * toX;
                y = u * u * fromY + 2f * u * p * controlY + p * p * toY;
                break;
            }
            case RADIAL: {
                float sourceAngle = (float) Math.atan2(fromY - focusY, fromX - focusX);
                float controlRadius = Math.max(width * 0.10f, (float) Math.hypot(fromX - focusX, fromY - focusY) * 0.48f);
                float controlX = focusX + (float) Math.cos(sourceAngle + Math.sin(phase) * 0.34f)
                        * controlRadius;
                float controlY = focusY + (float) Math.sin(sourceAngle + Math.sin(phase) * 0.34f)
                        * controlRadius;
                float u = 1f - p;
                x = u * u * fromX + 2f * u * p * controlX + p * p * toX;
                y = u * u * fromY + 2f * u * p * controlY + p * p * toY;
                break;
            }
            case BLOOM: {
                float angle = (float) Math.atan2(fromY - focusY, fromX - focusX);
                float petal = (float) Math.sin(angle * 6f + phase) * arc * 0.50f;
                float controlX = focusX + (float) Math.cos(angle) * (width * 0.18f + petal);
                float controlY = focusY + (float) Math.sin(angle) * (width * 0.15f + petal * 0.58f);
                float u = 1f - p;
                x = u * u * fromX + 2f * u * p * controlX + p * p * toX;
                y = u * u * fromY + 2f * u * p * controlY + p * p * toY;
                break;
            }
            case WAVE:
                x = GlyphMath.mix(fromX, toX, p)
                        + (float) Math.sin(fromY * 0.012f + phase + p * 6.6f)
                        * arc * 0.48f * wave;
                y = GlyphMath.mix(fromY, toY, p)
                        + (float) Math.sin(fromX * 0.010f - phase + p * 5.2f)
                        * arc * 0.24f * wave;
                break;
            case FOLD: {
                float direction = Math.sin(phase) >= 0f ? 1f : -1f;
                float controlX = focusX + direction * arc * 0.42f;
                float controlY = GlyphMath.mix(fromY, toY, 0.48f);
                float u = 1f - p;
                x = u * u * fromX + 2f * u * p * controlX + p * p * toX;
                y = u * u * fromY + 2f * u * p * controlY + p * p * toY;
                break;
            }
            case FLOW:
            default: {
                float dx = toX - fromX;
                float dy = toY - fromY;
                float length = Math.max(1f, (float) Math.hypot(dx, dy));
                float direction = Math.sin(phase) >= 0f ? 1f : -1f;
                float bend = Math.min(arc * 0.52f, length * 0.22f) * direction;
                float controlX = (fromX + toX) * 0.5f - dy / length * bend;
                float controlY = (fromY + toY) * 0.5f + dx / length * bend;
                float u = 1f - p;
                x = u * u * fromX + 2f * u * p * controlX + p * p * toX;
                y = u * u * fromY + 2f * u * p * controlY + p * p * toY;
                break;
            }
        }
        output[0] = x;
        output[1] = y;
    }

    private static float charStartFor(TargetRole role) {
        switch (role) {
            case TITLE:
                return 0.30f;
            case SUMMARY:
                return 0.38f;
            case META:
                return 0.46f;
            case ACTION:
                return 0.50f;
            case STRUCTURE:
            default:
                return 0.44f;
        }
    }

    private static void drawGlyphTransition(
            Canvas canvas,
            Paint paint,
            char[] one,
            char from,
            char to,
            float blend,
            float x,
            float y,
            float alpha
    ) {
        if (from == to || blend >= 0.84f) {
            paint.setAlpha(Math.min(255, Math.round(alpha * 255f)));
            one[0] = to;
            canvas.drawText(one, 0, 1, x, y, paint);
            return;
        }
        if (blend <= 0.16f) {
            paint.setAlpha(Math.min(255, Math.round(alpha * 255f)));
            one[0] = from;
            canvas.drawText(one, 0, 1, x, y, paint);
            return;
        }
        float local = (blend - 0.16f) / 0.68f;
        paint.setAlpha(Math.min(255, Math.round(alpha * (1f - local) * 255f)));
        one[0] = from;
        canvas.drawText(one, 0, 1, x, y, paint);
        paint.setAlpha(Math.min(255, Math.round(alpha * local * 255f)));
        one[0] = to;
        canvas.drawText(one, 0, 1, x, y, paint);
    }

    private static void drawAmbientMotion(
            Canvas canvas,
            Scene scene,
            ExperienceController.Frame frame,
            long nowMs
    ) {
        Paint paint = scene.glyphPaint;
        paint.setTypeface(MONO);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setFakeBoldText(false);
        char[] one = scene.glyphBuffer;
        float seconds = nowMs / 1000f;
        float focusX = scene.width * scene.theme.atmosphereX;
        float focusY = scene.height * scene.theme.atmosphereY;
        int primaryColor = scene.theme.atmosphereColor;
        int secondaryColor = fluidSecondaryColor(primaryColor);
        int tertiaryColor = fluidTertiaryColor(primaryColor);
        int coreColor = mixColor(tertiaryColor, Color.WHITE, 0.58f);
        float fluidScale = scene.width / FLUID_REFERENCE_WIDTH;
        float toReferenceX = FLUID_REFERENCE_WIDTH / Math.max(1f, scene.width);
        float toReferenceY = FLUID_REFERENCE_HEIGHT / Math.max(1f, scene.height);
        float wakeRadius = GlyphMath.mix(
                scene.width * 0.02f,
                scene.width * 0.92f,
                GlyphMath.easeOutCubic(frame.wakeProgress)
        );
        float wakeStrength = 1f - frame.wakeProgress;
        float fade = 1f - GlyphMath.smooth(frame.revealProgress / 0.54f);
        if (fade <= 0.001f && wakeStrength <= 0.001f) return;

        // Shared cinematic-profile band constants are expressed in the browser lab's reference
        // space, then scaled into the render surface. These values are frame-constant.
        float energySpan = scene.height + scene.width * 0.44f;
        float energyPosition = ((seconds * 92f + scene.theme.name().length() * 71f)
                * fluidScale) % energySpan - scene.width * 0.16f;
        float energySigma = Math.max(1f, 72f * fluidScale);
        float wakeSigma = Math.max(1f, 86f * fluidScale);
        float echoThreshold = 1.2f * fluidScale;
        float echoThresholdSquared = echoThreshold * echoThreshold;
        float largeSumTimeSin = (float) Math.sin(seconds * 0.03f);
        float largeSumTimeCos = (float) Math.cos(seconds * 0.03f);
        float largeDifferenceTimeSin = (float) Math.sin(seconds * 0.23f);
        float largeDifferenceTimeCos = (float) Math.cos(seconds * 0.23f);
        float fineSumTimeSin = (float) Math.sin(-seconds * 0.07f);
        float fineSumTimeCos = (float) Math.cos(-seconds * 0.07f);
        float fineDifferenceTimeSin = (float) Math.sin(-seconds * 0.55f);
        float fineDifferenceTimeCos = (float) Math.cos(-seconds * 0.55f);
        float energyXTimeSin = (float) Math.sin(seconds * 0.8f);
        float energyXTimeCos = (float) Math.cos(seconds * 0.8f);
        float energyYTimeSin = (float) Math.sin(seconds * 0.6f);
        float energyYTimeCos = (float) Math.cos(seconds * 0.6f);

        for (int glyphIndex = 0; glyphIndex < scene.ambientGlyphs.size(); glyphIndex++) {
            AmbientGlyph glyph = scene.ambientGlyphs.get(glyphIndex);
            float strength = scene.theme.motionStrength * glyph.depth;
            float vx = glyph.x - focusX;
            float vy = glyph.y - focusY;
            float radius = Math.max(1f, glyph.radius);
            float x = glyph.x;
            float y = glyph.y;

            switch (scene.theme.motionStyle) {
                case ORBITAL: {
                    float angle = (float) Math.sin(seconds * 0.30f + glyph.phase) * 0.012f * strength
                            + seconds * 0.0018f * (glyph.phase > Math.PI ? -1f : 1f);
                    float ca = (float) Math.cos(angle);
                    float sa = (float) Math.sin(angle);
                    x = focusX + vx * ca - vy * sa;
                    y = focusY + vx * sa + vy * ca;
                    x += (float) Math.sin(seconds * 0.72f + glyph.phase)
                            * 2.4f * fluidScale * strength;
                    break;
                }
                case CIRCUIT: {
                    float lane = ((seconds * 22f * (0.35f + glyph.depth) + glyph.phase * 40f) % 92f) - 46f;
                    y += lane * 0.18f * fluidScale * strength;
                    if (Math.sin(seconds * 1.4f + glyph.phase) > 0.74f) {
                        x += 7f * fluidScale * strength;
                    }
                    break;
                }
                case RADIAL: {
                    float scale = 1f + (float) Math.sin(seconds * 0.82f + glyph.phase) * 0.0068f * strength;
                    x = focusX + vx * scale;
                    y = focusY + vy * scale;
                    float tangent = (float) Math.sin(seconds * 0.5f + glyph.phase)
                            * 4.2f * fluidScale * strength;
                    x += -vy / radius * tangent;
                    y += vx / radius * tangent;
                    break;
                }
                case BLOOM: {
                    float angle = (float) Math.atan2(vy, vx);
                    float petal = (float) Math.sin(angle * 6f + seconds * 0.66f + glyph.phase)
                            * 6.5f * fluidScale * strength;
                    float scale = 1f + petal / Math.max(scene.width * 0.18f, radius) * 0.42f;
                    x = focusX + vx * scale;
                    y = focusY + vy * scale;
                    break;
                }
                case WAVE:
                    x += (float) Math.sin(
                            seconds * 0.52f + glyph.y * toReferenceY * 0.010f + glyph.phase
                    )
                            * 5.2f * fluidScale * strength;
                    y += (float) Math.sin(
                            seconds * 0.36f + glyph.x * toReferenceX * 0.008f - glyph.phase
                    )
                            * 3.7f * fluidScale * strength;
                    break;
                case FOLD: {
                    float depth = (glyph.y - focusY) / Math.max(1f, scene.height);
                    float fold = (float) Math.sin(seconds * 0.32f + glyph.phase)
                            * Math.abs(depth) * 8.5f * fluidScale * strength;
                    x += fold * (glyph.energyPhaseSin >= 0f ? 1f : -1f);
                    y += (float) Math.cos(seconds * 0.27f + glyph.phase)
                            * 1.7f * fluidScale * strength;
                    break;
                }
                case FLOW:
                default:
                    x += (float) Math.sin(
                            seconds * 0.55f + glyph.y * toReferenceY * 0.006f + glyph.phase
                    )
                            * 5.5f * fluidScale * strength;
                    y += (float) Math.cos(
                            seconds * 0.38f + glyph.x * toReferenceX * 0.005f + glyph.phase
                    )
                            * 3.2f * fluidScale * strength;
                    break;
            }

            // The lab's two divergence-like fields are evaluated through product identities and
            // precomputed angle-addition terms. Bright core cells retain lower mobility so the
            // hero silhouette remains recognizable.
            float largeSum = glyph.largeCurlSumSin * largeSumTimeCos
                    + glyph.largeCurlSumCos * largeSumTimeSin;
            float largeDifference = glyph.largeCurlDifferenceSin * largeDifferenceTimeCos
                    + glyph.largeCurlDifferenceCos * largeDifferenceTimeSin;
            float fineSum = glyph.fineCurlSumSin * fineSumTimeCos
                    + glyph.fineCurlSumCos * fineSumTimeSin;
            float fineDifference = glyph.fineCurlDifferenceSin * fineDifferenceTimeCos
                    + glyph.fineCurlDifferenceCos * fineDifferenceTimeSin;
            float curlLargeX = 0.5f * (largeSum + largeDifference);
            float curlLargeY = 0.5f * (largeDifference - largeSum);
            float curlFineX = 0.5f * (fineSum + fineDifference);
            float curlFineY = 0.5f * (fineDifference - fineSum);
            float curlGain = strength * (0.64f + glyph.depth * 0.36f) * glyph.edgeMobility;
            x += (curlLargeX * 5.8f + curlFineX * 1.9f) * fluidScale * curlGain;
            y += (curlLargeY * 4.5f + curlFineY * 1.5f) * fluidScale * curlGain;

            // Eligibility, direction, and jitter are immutable scene data. Only the sparse
            // eligible subset pays for a sine evaluation on a frame where a scan tear can occur.
            float glitchPulse = 0f;
            if (glyph.glitchEligible) {
                glitchPulse = GlyphMath.smooth(((float) Math.sin(
                        seconds * (1.75f + glyph.depth * 0.42f) + glyph.phase * 2.8f
                ) - 0.82f) / 0.18f);
                x += glyph.glitchDirection * glitchPulse * (6f + glyph.depth * 11f)
                        * fluidScale * glyph.edgeMobility;
                y += glitchPulse * glyph.glitchJitter * 3.2f * fluidScale;
            }

            float wakeDistance = (radius - wakeRadius) / wakeSigma;
            float wakeBand = (float) Math.exp(-wakeDistance * wakeDistance) * wakeStrength;
            float energyDistance = (glyph.energyCoordinate - energyPosition) / energySigma;
            float rawEnergyBand = (float) Math.exp(-energyDistance * energyDistance * 0.5f);
            float energyBand = rawEnergyBand * Math.max(fade, wakeStrength * 0.34f);
            float energyXWave = glyph.energyPhaseSin * energyXTimeCos
                    + glyph.energyPhaseCos * energyXTimeSin;
            float energyYWave = glyph.energyPhaseScaledCos * energyYTimeCos
                    + glyph.energyPhaseScaledSin * energyYTimeSin;
            x += energyXWave * energyBand * 4.8f * fluidScale * strength;
            y -= energyYWave * energyBand * 3.1f * fluidScale * strength;

            float twinkle = 0.58f + 0.42f
                    * (float) Math.sin(seconds * (1.1f + glyph.depth) + glyph.phase);
            float alpha = Math.min(
                    0.44f,
                    fade * (0.034f + glyph.alpha * 0.22f) * (0.72f + twinkle * 0.28f)
                            + energyBand * (0.050f + glyph.alpha * 0.075f)
                            + wakeBand * 0.34f
            );
            if (alpha <= 0.015f) continue;

            int liveColor = fluidPaletteColor(
                    primaryColor,
                    secondaryColor,
                    tertiaryColor,
                    seconds * 0.026f + glyph.palettePhase
            );
            liveColor = mixColor(
                    liveColor,
                    coreColor,
                    Math.min(0.52f, energyBand * 0.46f + wakeBand * 0.24f)
            );
            float glyphSize = glyph.size * (1f + wakeBand * 0.10f + energyBand * 0.075f);
            paint.setTextSize(glyphSize);

            float motionX = x - glyph.x;
            float motionY = y - glyph.y;
            float displacementSquared = motionX * motionX + motionY * motionY;
            if (glyph.echo && displacementSquared > echoThresholdSquared) {
                int echoColor = mixColor(tertiaryColor, liveColor, 0.36f);
                float echoAlpha = Math.min(
                        0.048f,
                        alpha * (0.085f + glyph.depth * 0.065f) * (1f + energyBand * 0.7f)
                );
                paint.setColor(echoColor);
                paint.setAlpha(Math.min(255, Math.round(echoAlpha * 255f)));
                one[0] = glyph.glyph;
                canvas.drawText(
                        one,
                        0,
                        1,
                        x - motionX * 0.58f,
                        y - motionY * 0.58f,
                        paint
                );
                if (energyBand > 0.28f && glyph.secondaryEcho) {
                    paint.setColor(secondaryColor);
                    paint.setAlpha(Math.min(255, Math.round(echoAlpha * 0.38f * 255f)));
                    canvas.drawText(
                            one,
                            0,
                            1,
                            x - motionX * 0.91f,
                            y - motionY * 0.91f,
                            paint
                    );
                }
            }
            if (glitchPulse > 0.05f) {
                float glitchAlpha = Math.min(0.065f, alpha * glitchPulse * 0.26f);
                paint.setColor(tertiaryColor);
                paint.setAlpha(Math.min(255, Math.round(glitchAlpha * 255f)));
                one[0] = glyph.glyph;
                canvas.drawText(
                        one,
                        0,
                        1,
                        x - glyph.glitchDirection * (4f + glitchPulse * 4f) * fluidScale,
                        y,
                        paint
                );
            }

            if (energyBand > 0.34f && glyph.depth > 0.48f && glyph.glow) {
                paint.setTextSize(glyphSize * (1.24f + energyBand * 0.16f));
                paint.setColor(coreColor);
                paint.setAlpha(Math.min(255, Math.round(Math.min(0.045f, energyBand * 0.040f) * 255f)));
                one[0] = glyph.glyph;
                canvas.drawText(one, 0, 1, x, y, paint);
                paint.setTextSize(glyphSize);
            }

            paint.setColor(liveColor);
            paint.setAlpha(Math.min(255, Math.round(alpha * 255f)));
            one[0] = glyph.glyph;
            canvas.drawText(one, 0, 1, x, y, paint);
        }

        if (wakeStrength > 0.001f) {
            Paint halo = scene.effectPaint;
            halo.setShader(null);
            halo.setStyle(Paint.Style.STROKE);
            halo.setStrokeWidth(Math.max(1f, scene.width * 0.004f));
            halo.setColor(scene.theme.atmosphereColor);
            halo.setAlpha(Math.round(10f * wakeStrength));
            canvas.drawCircle(focusX, focusY, wakeRadius, halo);
            halo.setStyle(Paint.Style.FILL);
        }
    }

    private static void drawPreviewChrome(Canvas canvas, Scene scene) {
        long minute = System.currentTimeMillis() / 60_000L;
        if (minute != scene.previewMinute) {
            java.time.ZonedDateTime now = java.time.ZonedDateTime.now();
            scene.previewTime = now.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
            scene.previewDate = now.format(java.time.format.DateTimeFormatter.ofPattern("EEE · MMM d"))
                    .toUpperCase(Locale.ROOT);
            scene.previewMinute = minute;
        }
        float width = canvas.getWidth();
        float height = canvas.getHeight();

        Paint timePaint = scene.glyphPaint;
        timePaint.setTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL));
        timePaint.setTextAlign(Paint.Align.CENTER);
        timePaint.setTextSize(width * 0.083f);
        timePaint.setColor(Color.argb(238, 239, 244, 247));
        timePaint.setAlpha(255);
        timePaint.setFakeBoldText(false);
        canvas.drawText(scene.previewTime, width / 2f, height * 0.078f, timePaint);

        timePaint.setTypeface(MONO);
        timePaint.setTextSize(width * 0.011f);
        timePaint.setColor(Color.argb(150, 232, 238, 242));
        canvas.drawText(scene.previewDate, width / 2f, height * 0.092f, timePaint);

        Paint bar = scene.effectPaint;
        bar.setShader(null);
        bar.setStyle(Paint.Style.FILL);
        bar.setColor(Color.argb(105, 236, 243, 246));
        bar.setAlpha(255);
        canvas.drawRoundRect(
                width * 0.465f,
                height * 0.981f,
                width * 0.535f,
                height * 0.983f,
                8f,
                8f,
                bar
        );
    }

    private static Paint monoPaint(float size, boolean bold, int color) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        paint.setTypeface(MONO);
        paint.setTextSize(size);
        paint.setColor(color);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setFakeBoldText(bold);
        return paint;
    }

    private static float fitTextSize(
            Paint paint,
            String text,
            float maxWidth,
            float preferred,
            float minimum
    ) {
        paint.setTextSize(preferred);
        if (paint.measureText(text) <= maxWidth) return preferred;
        float low = minimum;
        float high = preferred;
        for (int i = 0; i < 10; i++) {
            float mid = (low + high) / 2f;
            paint.setTextSize(mid);
            if (paint.measureText(text) <= maxWidth) low = mid;
            else high = mid;
        }
        return low;
    }

    /** Derives the violet/magenta end of a theme's ambient color journey. */
    private static int fluidSecondaryColor(int primary) {
        return Color.rgb(
                Math.round(GlyphMath.mix(Color.red(primary), 255f, 0.18f)),
                Math.round(GlyphMath.mix(Color.green(primary), 72f, 0.58f)),
                Math.round(GlyphMath.mix(Color.blue(primary), 255f, 0.66f))
        );
    }

    /** Derives the cyan specular end used by traveling energy bands and sparse hot cells. */
    private static int fluidTertiaryColor(int primary) {
        return Color.rgb(
                Math.round(GlyphMath.mix(Color.red(primary), 70f, 0.55f)),
                Math.round(GlyphMath.mix(Color.green(primary), 220f, 0.55f)),
                Math.round(GlyphMath.mix(Color.blue(primary), 255f, 0.55f))
        );
    }

    /** Three-stop traveling palette shared with the cinematic browser profile. */
    private static int fluidPaletteColor(int primary, int secondary, int tertiary, float phase) {
        float wrapped = phase - (float) Math.floor(phase);
        if (wrapped < 0.5f) {
            return mixColor(primary, secondary, GlyphMath.smooth(wrapped * 2f));
        }
        return mixColor(secondary, tertiary, GlyphMath.smooth((wrapped - 0.5f) * 2f));
    }

    private static int mixColor(int a, int b, float t) {
        float amount = GlyphMath.clamp01(t);
        return Color.rgb(
                Math.round(GlyphMath.mix(Color.red(a), Color.red(b), amount)),
                Math.round(GlyphMath.mix(Color.green(a), Color.green(b), amount)),
                Math.round(GlyphMath.mix(Color.blue(a), Color.blue(b), amount))
        );
    }

    private static int withAlpha(int color, int alpha) {
        return Color.argb(
                Math.max(0, Math.min(255, alpha)),
                Color.red(color),
                Color.green(color),
                Color.blue(color)
        );
    }
}

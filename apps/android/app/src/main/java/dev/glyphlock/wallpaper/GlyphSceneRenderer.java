package dev.glyphlock.wallpaper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Hybrid Prototype-0 renderer.
 *
 * Dense glyph art is rasterized once, while a smaller set of live glyph particles morphs
 * from the source artwork into event typography. This keeps the first Android proof simple
 * and battery-aware while preserving the visual idea.
 */
final class GlyphSceneRenderer {
    private static final String RAMP = "  .·,:;+i1tfLCG08@";
    private static final String BASE_GLYPHS = " .·,:;+=x1I|/\\()[]{}<>#08@";
    private static final Typeface MONO = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL);

    private GlyphSceneRenderer() {}

    static final class Scene {
        final int width;
        final int height;
        final Bitmap baseBitmap;
        final Bitmap eventBitmap;
        final Bitmap resultBitmap;
        final List<AmbientGlyph> ambientGlyphs;
        final List<Particle> particles;
        final int accent;
        final DemoCatalog.Theme theme;

        Scene(
                int width,
                int height,
                Bitmap baseBitmap,
                Bitmap eventBitmap,
                Bitmap resultBitmap,
                List<AmbientGlyph> ambientGlyphs,
                List<Particle> particles,
                int accent,
                DemoCatalog.Theme theme
        ) {
            this.width = width;
            this.height = height;
            this.baseBitmap = baseBitmap;
            this.eventBitmap = eventBitmap;
            this.resultBitmap = resultBitmap;
            this.ambientGlyphs = ambientGlyphs;
            this.particles = particles;
            this.accent = accent;
            this.theme = theme;
        }

        void recycle() {
            if (!baseBitmap.isRecycled()) baseBitmap.recycle();
            if (!eventBitmap.isRecycled()) eventBitmap.recycle();
            if (!resultBitmap.isRecycled()) resultBitmap.recycle();
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

        AmbientGlyph(float x, float y, char glyph, float size, float alpha, float phase, float depth) {
            this.x = x;
            this.y = y;
            this.glyph = glyph;
            this.size = size;
            this.alpha = alpha;
            this.phase = phase;
            this.depth = depth;
        }
    }

    private static final class Particle {
        final float sourceX;
        final float sourceY;
        final float targetX;
        final float targetY;
        final char glyph;
        final float phase;
        final float arc;
        final float size;
        final float alpha;
        final float delay;
        final float duration;

        Particle(
                float sourceX,
                float sourceY,
                float targetX,
                float targetY,
                char glyph,
                float phase,
                float arc,
                float size,
                float alpha,
                float delay,
                float duration
        ) {
            this.sourceX = sourceX;
            this.sourceY = sourceY;
            this.targetX = targetX;
            this.targetY = targetY;
            this.glyph = glyph;
            this.phase = phase;
            this.arc = arc;
            this.size = size;
            this.alpha = alpha;
            this.delay = delay;
            this.duration = duration;
        }
    }

    static Scene build(
            Context context,
            int surfaceWidth,
            int surfaceHeight,
            DemoCatalog.Theme theme,
            DemoCatalog.Event event
    ) {
        RenderQuality quality = RenderQuality.LUX;
        int safeWidth = Math.max(360, surfaceWidth);
        int renderWidth = Math.min(quality.maxRenderWidth, safeWidth);
        int renderHeight = Math.max(800, Math.round(renderWidth * (surfaceHeight / (float) Math.max(1, surfaceWidth))));

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        Bitmap decoded = BitmapFactory.decodeResource(context.getResources(), theme.maskResource, options);
        if (decoded == null) throw new IllegalStateException("Unable to decode glyph scene mask");
        Bitmap mask = Bitmap.createScaledBitmap(decoded, renderWidth, renderHeight, true);
        if (mask != decoded) decoded.recycle();

        List<GlyphPoint> basePoints = extractGlyphPoints(mask, renderWidth, renderHeight, theme, quality);
        mask.recycle();

        Bitmap base = renderBaseBitmap(renderWidth, renderHeight, basePoints, theme);
        TextGeometry eventGeometry = renderEventBitmap(base, renderWidth, renderHeight, theme, event, false);
        TextGeometry resultGeometry = renderEventBitmap(base, renderWidth, renderHeight, theme, event, true);
        List<AmbientGlyph> ambientGlyphs = buildAmbientGlyphs(basePoints, theme, quality);
        List<Particle> particles = buildParticles(basePoints, eventGeometry.targets, event, theme, renderWidth, renderHeight, quality);

        return new Scene(
                renderWidth,
                renderHeight,
                base,
                eventGeometry.bitmap,
                resultGeometry.bitmap,
                ambientGlyphs,
                particles,
                event.accent,
                theme
        );
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
        Rect destination = new Rect(0, 0, targetWidth, targetHeight);
        Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

        float reveal = frame.revealProgress;
        float result = frame.resultProgress;

        bitmapPaint.setAlpha(Math.round(255f * (1f - reveal * 0.78f)));
        output.drawBitmap(scene.baseBitmap, null, destination, bitmapPaint);

        output.save();
        output.scale(targetWidth / (float) scene.width, targetHeight / (float) scene.height);
        drawAmbientMotion(output, scene, frame, nowMs);
        output.restore();

        bitmapPaint.setAlpha(Math.round(255f * reveal * (1f - result)));
        output.drawBitmap(scene.eventBitmap, null, destination, bitmapPaint);

        if (result > 0f) {
            bitmapPaint.setAlpha(Math.round(255f * reveal * result));
            output.drawBitmap(scene.resultBitmap, null, destination, bitmapPaint);
        }

        output.save();
        output.scale(targetWidth / (float) scene.width, targetHeight / (float) scene.height);
        drawParticles(output, scene, reveal, nowMs);
        if (frame.listening) drawListening(output, scene, nowMs);
        drawScan(output, scene, nowMs, frame.needsAnimation);
        output.restore();

        if (showPreviewClock) drawPreviewChrome(output, nowMs);
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
                if (value < threshold && noise < 0.91f) continue;
                float level = GlyphMath.clamp01(value * 1.10f * theme.exposure + (noise - 0.5f) * 0.10f);
                int rampIndex = Math.min(RAMP.length() - 1, (int) Math.floor(level * RAMP.length()));
                char glyph = RAMP.charAt(rampIndex);
                if (level < 0.20f || noise > 0.88f) {
                    String vocabulary = theme.textureGlyphs + BASE_GLYPHS;
                    glyph = vocabulary.charAt(Math.min(vocabulary.length() - 1, (int) (noise * vocabulary.length())));
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

        int tintRed = Color.red(theme.atmosphereColor);
        int tintGreen = Color.green(theme.atmosphereColor);
        int tintBlue = Color.blue(theme.atmosphereColor);
        char[] one = new char[1];
        for (GlyphPoint point : points) {
            int value = Math.round(72 + 182 * point.alpha);
            float tintMix = 0.12f + point.alpha * 0.13f;
            paint.setColor(Color.rgb(
                    Math.round(GlyphMath.mix(value, tintRed, tintMix)),
                    Math.round(GlyphMath.mix(value, tintGreen, tintMix)),
                    Math.round(GlyphMath.mix(value, tintBlue, tintMix))
            ));
            paint.setAlpha(Math.round(point.alpha * 235f));
            paint.setTextSize(point.size);
            one[0] = point.glyph;
            canvas.drawText(one, 0, 1, point.x, point.y, paint);
        }

        Paint atmosphere = new Paint(Paint.ANTI_ALIAS_FLAG);
        atmosphere.setShader(new RadialGradient(
                width * theme.atmosphereX,
                height * theme.atmosphereY,
                width * 0.76f,
                new int[] { withAlpha(theme.atmosphereColor, 16), withAlpha(theme.atmosphereColor, 5), Color.TRANSPARENT },
                new float[] { 0f, 0.48f, 1f },
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(width * theme.atmosphereX, height * theme.atmosphereY, width * 0.76f, atmosphere);
        return bitmap;
    }

    private static List<AmbientGlyph> buildAmbientGlyphs(
            List<GlyphPoint> points,
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
        for (int i = 0; i < count; i++) {
            GlyphPoint point = candidates.get((i * 97 + theme.ordinal() * 53) % candidates.size());
            live.add(new AmbientGlyph(
                    point.x,
                    point.y,
                    point.glyph,
                    point.size,
                    point.alpha,
                    GlyphMath.hash(i, point.x, point.y) * (float) Math.PI * 2f,
                    0.25f + 0.75f * GlyphMath.hash(point.y, i, 11f)
            ));
        }
        return live;
    }

    private static final class TextGeometry {
        final Bitmap bitmap;
        final List<GlyphPoint> targets;

        TextGeometry(Bitmap bitmap, List<GlyphPoint> targets) {
            this.bitmap = bitmap;
            this.targets = targets;
        }
    }

    private static TextGeometry renderEventBitmap(
            Bitmap base,
            int width,
            int height,
            DemoCatalog.Theme theme,
            DemoCatalog.Event event,
            boolean result
    ) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.BLACK);

        Paint faded = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        faded.setAlpha(87);
        canvas.drawBitmap(base, 0f, 0f, faded);

        float cavityY = height * theme.cavityFraction;
        float cavityCenterY = cavityY + height * 0.09f;
        float radius = width * 0.48f;
        Paint cavity = new Paint(Paint.ANTI_ALIAS_FLAG);
        cavity.setShader(new RadialGradient(
                width / 2f,
                cavityCenterY,
                radius,
                new int[] {
                        Color.argb(252, 0, 0, 0),
                        Color.argb(226, 0, 0, 0),
                        Color.TRANSPARENT
                },
                new float[] { 0f, 0.63f, 1f },
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(width / 2f, cavityCenterY, radius, cavity);

        float left = width * 0.137f;
        float contentWidth = width * 0.726f;
        float y = cavityY;
        int accent = event.accent;

        Paint eyebrow = monoPaint(width * 0.0204f, true, withAlpha(accent, 200));
        canvas.drawText(result ? "LOCAL DEMO · ACTION SIMULATED" : event.eyebrow, left, y, eyebrow);
        y += height * 0.0283f;

        String title = result ? event.resultTitle : event.title;
        Paint titlePaint = monoPaint(width * 0.0537f, false, Color.argb(250, 244, 248, 250));
        titlePaint.setFakeBoldText(false);
        titlePaint.setTextSize(fitTextSize(titlePaint, title, contentWidth, width * 0.0537f, width * 0.035f));
        canvas.drawText(title, left, y, titlePaint);
        y += height * 0.0142f;

        Paint rail = new Paint(Paint.ANTI_ALIAS_FLAG);
        rail.setColor(withAlpha(accent, 112));
        rail.setStrokeWidth(Math.max(1f, width / 540f));
        canvas.drawLine(left, y, left + width * 0.144f, y, rail);
        y += height * 0.0267f;

        String summary = result ? event.resultSummary : event.summary;
        TextPaint summaryPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        summaryPaint.setTypeface(MONO);
        summaryPaint.setColor(Color.argb(214, 226, 233, 237));
        summaryPaint.setTextSize(width * 0.0287f);
        StaticLayout summaryLayout = StaticLayout.Builder
                .obtain(summary, 0, summary.length(), summaryPaint, Math.round(contentWidth))
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setIncludePad(false)
                .setLineSpacing(height * 0.0045f, 1f)
                .setMaxLines(4)
                .setEllipsize(TextUtils.TruncateAt.END)
                .build();
        canvas.save();
        canvas.translate(left, y);
        summaryLayout.draw(canvas);
        canvas.restore();
        y += summaryLayout.getHeight() + height * 0.025f;

        Paint action = monoPaint(width * 0.0185f, true, withAlpha(accent, 214));
        canvas.drawText(result ? "TAP TO RETURN" : event.action, left, y, action);

        Paint verticalRail = new Paint(Paint.ANTI_ALIAS_FLAG);
        verticalRail.setColor(withAlpha(accent, 58));
        verticalRail.setStrokeWidth(Math.max(1f, width / 540f));
        canvas.drawLine(left - width * 0.026f, cavityY - height * 0.023f, left - width * 0.026f, y + height * 0.012f, verticalRail);

        List<GlyphPoint> targets = buildTextTargetPoints(
                width,
                height,
                cavityY,
                left,
                contentWidth,
                title,
                summary,
                titlePaint.getTextSize(),
                summaryPaint
        );
        return new TextGeometry(bitmap, targets);
    }

    private static List<GlyphPoint> buildTextTargetPoints(
            int width,
            int height,
            float cavityY,
            float left,
            float contentWidth,
            String title,
            String summary,
            float titleSize,
            TextPaint summaryPaint
    ) {
        Bitmap mask = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(mask);
        Paint titlePaint = monoPaint(titleSize, false, Color.WHITE);
        float y = cavityY + height * 0.0283f;
        canvas.drawText(title, left, y, titlePaint);
        y += height * 0.0408f;

        StaticLayout layout = StaticLayout.Builder
                .obtain(summary, 0, summary.length(), summaryPaint, Math.round(contentWidth))
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setIncludePad(false)
                .setLineSpacing(height * 0.0045f, 1f)
                .setMaxLines(4)
                .setEllipsize(TextUtils.TruncateAt.END)
                .build();
        Paint originalPaint = new Paint(summaryPaint);
        summaryPaint.setColor(Color.WHITE);
        canvas.save();
        canvas.translate(left, y);
        layout.draw(canvas);
        canvas.restore();
        summaryPaint.setColor(originalPaint.getColor());

        int[] pixels = new int[width * height];
        mask.getPixels(pixels, 0, width, 0, 0, width, height);
        mask.recycle();

        List<GlyphPoint> points = new ArrayList<>();
        int step = Math.max(4, Math.round(width / 155f));
        int startY = Math.max(0, Math.round(cavityY - height * 0.02f));
        int endY = Math.min(height, Math.round(y + layout.getHeight() + height * 0.03f));
        int startX = Math.max(0, Math.round(left - width * 0.04f));
        int endX = Math.min(width, Math.round(left + contentWidth + width * 0.04f));
        for (int yy = startY; yy < endY; yy += step) {
            for (int xx = startX; xx < endX; xx += step) {
                int alpha = Color.alpha(pixels[yy * width + xx]);
                if (alpha < 76) continue;
                float noise = GlyphMath.hash(xx, yy, 19f);
                points.add(new GlyphPoint(xx, yy, '@', 0.88f, width * (0.0075f + noise * 0.004f)));
            }
        }
        return points;
    }

    private static List<Particle> buildParticles(
            List<GlyphPoint> sources,
            List<GlyphPoint> targets,
            DemoCatalog.Event event,
            DemoCatalog.Theme theme,
            int width,
            int height,
            RenderQuality quality
    ) {
        List<GlyphPoint> eligibleSources = new ArrayList<>();
        for (GlyphPoint source : sources) {
            if (source.alpha > 0.24f) eligibleSources.add(source);
        }
        int count = quality.particleCountFor(targets.size());
        List<Particle> particles = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            GlyphPoint target = targets.isEmpty()
                    ? new GlyphPoint(width / 2f, height / 2f, '@', 1f, width * 0.01f)
                    : targets.get((i * 37) % targets.size());
            GlyphPoint source = eligibleSources.isEmpty()
                    ? target
                    : eligibleSources.get((i * 83 + event.id.length() * 101) % eligibleSources.size());
            float noise = GlyphMath.hash(i, event.id.length(), theme.ordinal() + 1f);
            char glyph = event.glyphs.charAt(Math.min(event.glyphs.length() - 1, (int) (noise * event.glyphs.length())));
            float sourceBand = GlyphMath.clamp01(source.y / Math.max(1f, height));
            float targetBand = GlyphMath.clamp01(target.y / Math.max(1f, height));
            particles.add(new Particle(
                    source.x,
                    source.y,
                    target.x,
                    target.y,
                    glyph,
                    noise * (float) Math.PI * 2f,
                    width * (0.026f + noise * 0.090f) * theme.motionStrength,
                    width * (0.0063f + noise * 0.0048f),
                    0.22f + 0.72f * GlyphMath.hash(i, event.id.length(), 2f),
                    0.015f + 0.27f * sourceBand + 0.08f * GlyphMath.hash(i, source.x, 7f),
                    0.56f + 0.13f * (1f - targetBand) + 0.05f * noise
            ));
        }
        return particles;
    }

    private static void drawParticles(Canvas canvas, Scene scene, float reveal, long nowMs) {
        if (reveal <= 0f || reveal >= 1f) return;
        float visibility = (float) Math.sin(Math.PI * reveal);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        paint.setTypeface(MONO);
        paint.setTextAlign(Paint.Align.CENTER);
        char[] one = new char[1];
        int red = Color.red(scene.accent);
        int green = Color.green(scene.accent);
        int blue = Color.blue(scene.accent);
        float focusX = scene.width * scene.theme.atmosphereX;
        float focusY = scene.height * scene.theme.atmosphereY;

        for (Particle particle : scene.particles) {
            float local = GlyphMath.staggeredProgress(reveal, particle.delay, particle.duration);
            if (local <= 0f || local >= 1f) continue;
            float move = GlyphMath.easeOutCubic(local);
            float wave = (float) Math.sin(Math.PI * local);
            float x;
            float y;

            switch (scene.theme.motionStyle) {
                case CIRCUIT: {
                    boolean horizontalFirst = Math.sin(particle.phase) >= 0f;
                    if (horizontalFirst) {
                        x = GlyphMath.mix(particle.sourceX, particle.targetX, GlyphMath.smooth(Math.min(1f, move * 1.85f)));
                        y = GlyphMath.mix(particle.sourceY, particle.targetY, GlyphMath.smooth(Math.max(0f, (move - 0.46f) / 0.54f)));
                    } else {
                        y = GlyphMath.mix(particle.sourceY, particle.targetY, GlyphMath.smooth(Math.min(1f, move * 1.85f)));
                        x = GlyphMath.mix(particle.sourceX, particle.targetX, GlyphMath.smooth(Math.max(0f, (move - 0.46f) / 0.54f)));
                    }
                    x += (float) Math.sin(particle.phase * 1.7f) * particle.arc * 0.055f * wave;
                    y += (float) Math.cos(particle.phase * 1.3f) * particle.arc * 0.055f * wave;
                    break;
                }
                case ORBITAL: {
                    float midX = (particle.sourceX + particle.targetX) * 0.5f;
                    float midY = (particle.sourceY + particle.targetY) * 0.5f;
                    float vx = midX - focusX;
                    float vy = midY - focusY;
                    float length = Math.max(1f, (float) Math.hypot(vx, vy));
                    float direction = Math.sin(particle.phase) >= 0f ? 1f : -1f;
                    float controlX = midX - vy / length * particle.arc * 1.35f * direction;
                    float controlY = midY + vx / length * particle.arc * 1.35f * direction;
                    float u = 1f - move;
                    x = u * u * particle.sourceX + 2f * u * move * controlX + move * move * particle.targetX;
                    y = u * u * particle.sourceY + 2f * u * move * controlY + move * move * particle.targetY;
                    break;
                }
                case RADIAL: {
                    float tangent = particle.phase + move * 2.4f;
                    float controlX = GlyphMath.mix(particle.sourceX, focusX, 0.58f) + (float) Math.cos(tangent) * particle.arc;
                    float controlY = GlyphMath.mix(particle.sourceY, focusY, 0.58f) + (float) Math.sin(tangent) * particle.arc * 0.66f;
                    float u = 1f - move;
                    x = u * u * particle.sourceX + 2f * u * move * controlX + move * move * particle.targetX;
                    y = u * u * particle.sourceY + 2f * u * move * controlY + move * move * particle.targetY;
                    break;
                }
                case BLOOM: {
                    float sourceAngle = (float) Math.atan2(particle.sourceY - focusY, particle.sourceX - focusX);
                    float petal = (float) Math.sin(sourceAngle * 6f + particle.phase) * particle.arc;
                    float controlX = focusX + (float) Math.cos(sourceAngle) * (scene.width * 0.22f + petal);
                    float controlY = focusY + (float) Math.sin(sourceAngle) * (scene.width * 0.18f + petal * 0.55f);
                    float u = 1f - move;
                    x = u * u * particle.sourceX + 2f * u * move * controlX + move * move * particle.targetX;
                    y = u * u * particle.sourceY + 2f * u * move * controlY + move * move * particle.targetY;
                    break;
                }
                case FLOW:
                default:
                    x = GlyphMath.mix(particle.sourceX, particle.targetX, move)
                            + (float) Math.sin(particle.phase + local * 5.2f) * particle.arc * wave;
                    y = GlyphMath.mix(particle.sourceY, particle.targetY, move)
                            + (float) Math.cos(particle.phase * 1.3f + local * 4.1f) * particle.arc * 0.48f * wave;
                    break;
            }

            paint.setTextSize(particle.size);
            paint.setColor(Color.rgb(red, green, blue));
            paint.setAlpha(Math.round(255f * visibility * wave * particle.alpha));
            one[0] = particle.glyph;
            canvas.drawText(one, 0, 1, x, y, paint);
        }

        float waveY = GlyphMath.mix(scene.height * 0.16f, scene.height * 0.84f, reveal);
        Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
        glow.setShader(new LinearGradient(
                0f,
                waveY - scene.height * 0.042f,
                0f,
                waveY + scene.height * 0.042f,
                new int[] { Color.TRANSPARENT, withAlpha(scene.accent, Math.round(10f * visibility)), Color.TRANSPARENT },
                new float[] { 0f, 0.5f, 1f },
                Shader.TileMode.CLAMP
        ));
        canvas.drawRect(0f, waveY - scene.height * 0.042f, scene.width, waveY + scene.height * 0.042f, glow);
    }

    private static void drawAmbientMotion(Canvas canvas, Scene scene, ExperienceController.Frame frame, long nowMs) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        paint.setTypeface(MONO);
        paint.setTextAlign(Paint.Align.CENTER);
        char[] one = new char[1];
        float seconds = nowMs / 1000f;
        float focusX = scene.width * scene.theme.atmosphereX;
        float focusY = scene.height * scene.theme.atmosphereY;
        int red = Color.red(scene.theme.atmosphereColor);
        int green = Color.green(scene.theme.atmosphereColor);
        int blue = Color.blue(scene.theme.atmosphereColor);
        float wakeRadius = GlyphMath.mix(scene.width * 0.02f, scene.width * 0.92f, GlyphMath.easeOutCubic(frame.wakeProgress));
        float wakeStrength = 1f - frame.wakeProgress;
        float fade = 1f - frame.revealProgress * 0.72f;

        for (AmbientGlyph glyph : scene.ambientGlyphs) {
            float strength = scene.theme.motionStrength * glyph.depth;
            float vx = glyph.x - focusX;
            float vy = glyph.y - focusY;
            float radius = Math.max(1f, (float) Math.hypot(vx, vy));
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
                    x += (float) Math.sin(seconds * 0.72f + glyph.phase) * 2.4f * strength;
                    break;
                }
                case CIRCUIT: {
                    float lane = ((seconds * 22f * (0.35f + glyph.depth) + glyph.phase * 40f) % 92f) - 46f;
                    y += lane * 0.18f * strength;
                    if (Math.sin(seconds * 1.4f + glyph.phase) > 0.74f) x += 7f * strength;
                    break;
                }
                case RADIAL: {
                    float scale = 1f + (float) Math.sin(seconds * 0.82f + glyph.phase) * 0.0068f * strength;
                    x = focusX + vx * scale;
                    y = focusY + vy * scale;
                    float tangent = (float) Math.sin(seconds * 0.5f + glyph.phase) * 4.2f * strength;
                    x += -vy / radius * tangent;
                    y += vx / radius * tangent;
                    break;
                }
                case BLOOM: {
                    float angle = (float) Math.atan2(vy, vx);
                    float petal = (float) Math.sin(angle * 6f + seconds * 0.66f + glyph.phase) * 6.5f * strength;
                    float scale = 1f + petal / Math.max(scene.width * 0.18f, radius) * 0.42f;
                    x = focusX + vx * scale;
                    y = focusY + vy * scale;
                    break;
                }
                case FLOW:
                default:
                    x += (float) Math.sin(seconds * 0.55f + glyph.y * 0.006f + glyph.phase) * 5.5f * strength;
                    y += (float) Math.cos(seconds * 0.38f + glyph.x * 0.005f + glyph.phase) * 3.2f * strength;
                    break;
            }

            float wakeDistance = (radius - wakeRadius) / Math.max(1f, scene.width * 0.08f);
            float wakeBand = (float) Math.exp(-wakeDistance * wakeDistance) * wakeStrength;
            float twinkle = 0.58f + 0.42f * (float) Math.sin(seconds * (1.1f + glyph.depth) + glyph.phase);
            float alpha = fade * (0.045f + glyph.alpha * 0.24f) * (0.72f + twinkle * 0.28f) + wakeBand * 0.46f;
            if (alpha <= 0.015f) continue;
            paint.setTextSize(glyph.size * (1f + wakeBand * 0.12f));
            paint.setColor(Color.rgb(red, green, blue));
            paint.setAlpha(Math.min(255, Math.round(alpha * 255f)));
            one[0] = glyph.glyph;
            canvas.drawText(one, 0, 1, x, y, paint);
        }

        if (wakeStrength > 0.001f) {
            Paint halo = new Paint(Paint.ANTI_ALIAS_FLAG);
            halo.setShader(new RadialGradient(
                    focusX,
                    focusY,
                    Math.max(scene.width * 0.01f, wakeRadius + scene.width * 0.10f),
                    new int[] { Color.TRANSPARENT, withAlpha(scene.theme.atmosphereColor, Math.round(15f * wakeStrength)), Color.TRANSPARENT },
                    new float[] { 0.65f, 0.84f, 1f },
                    Shader.TileMode.CLAMP
            ));
            canvas.drawCircle(focusX, focusY, wakeRadius + scene.width * 0.10f, halo);
        }
    }

    private static void drawListening(Canvas canvas, Scene scene, long nowMs) {
        float pulse = 0.5f + 0.5f * (float) Math.sin(nowMs / 145f);
        float centerY = scene.height * 0.88f;
        Paint halo = new Paint(Paint.ANTI_ALIAS_FLAG);
        halo.setShader(new RadialGradient(
                scene.width / 2f,
                centerY,
                scene.width * (0.22f + pulse * 0.03f),
                new int[] { withAlpha(scene.accent, Math.round(30 + pulse * 20)), Color.TRANSPARENT },
                new float[] { 0f, 1f },
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(scene.width / 2f, centerY, scene.width * 0.25f, halo);

        Paint label = monoPaint(scene.width * 0.0185f, true, withAlpha(scene.accent, Math.round(184 + pulse * 56)));
        label.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("LISTENING TO THIS EVENT", scene.width / 2f, centerY + scene.height * 0.008f, label);

        Paint bars = new Paint(Paint.ANTI_ALIAS_FLAG);
        bars.setColor(scene.accent);
        bars.setAlpha(Math.round(180 + pulse * 60));
        float baseY = centerY + scene.height * 0.028f;
        for (int i = -9; i <= 9; i++) {
            float amplitude = 5f + scene.height * 0.015f
                    * Math.abs((float) Math.sin(nowMs / 180f + i * 0.62f))
                    * (1f - Math.abs(i) / 13f);
            float x = scene.width / 2f + i * scene.width * 0.012f;
            canvas.drawRoundRect(x, baseY - amplitude / 2f, x + Math.max(1.5f, scene.width * 0.0028f), baseY + amplitude / 2f, 2f, 2f, bars);
        }
    }

    private static void drawScan(Canvas canvas, Scene scene, long nowMs, boolean active) {
        float alpha = active ? 8f : 4f;
        float y = (nowMs * 0.035f) % scene.height;
        Paint scan = new Paint(Paint.ANTI_ALIAS_FLAG);
        scan.setShader(new LinearGradient(
                0f,
                y - scene.height * 0.034f,
                0f,
                y + scene.height * 0.034f,
                new int[] { Color.TRANSPARENT, Color.argb(Math.round(alpha), 200, 224, 236), Color.TRANSPARENT },
                new float[] { 0f, 0.5f, 1f },
                Shader.TileMode.CLAMP
        ));
        canvas.drawRect(0f, y - scene.height * 0.034f, scene.width, y + scene.height * 0.034f, scan);
    }

    private static void drawPreviewChrome(Canvas canvas, long nowMs) {
        java.time.ZonedDateTime now = java.time.ZonedDateTime.now();
        String time = now.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
        String date = now.format(java.time.format.DateTimeFormatter.ofPattern("EEE · MMM d")).toUpperCase(java.util.Locale.ROOT);
        float width = canvas.getWidth();
        float height = canvas.getHeight();

        Paint timePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        timePaint.setTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL));
        timePaint.setTextAlign(Paint.Align.CENTER);
        timePaint.setTextSize(width * 0.083f);
        timePaint.setColor(Color.argb(238, 239, 244, 247));
        canvas.drawText(time, width / 2f, height * 0.078f, timePaint);

        Paint datePaint = monoPaint(width * 0.011f, false, Color.argb(150, 232, 238, 242));
        datePaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(date, width / 2f, height * 0.092f, datePaint);

        Paint hint = monoPaint(width * 0.008f, false, Color.argb(86, 227, 235, 240));
        hint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("HOLD", width / 2f, height * 0.974f, hint);
        Paint bar = new Paint(Paint.ANTI_ALIAS_FLAG);
        bar.setColor(Color.argb(105, 236, 243, 246));
        canvas.drawRoundRect(width * 0.465f, height * 0.981f, width * 0.535f, height * 0.983f, 8f, 8f, bar);
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

    private static float fitTextSize(Paint paint, String text, float maxWidth, float preferred, float minimum) {
        paint.setTextSize(preferred);
        if (paint.measureText(text) <= maxWidth) return preferred;
        float low = minimum;
        float high = preferred;
        for (int i = 0; i < 10; i++) {
            float mid = (low + high) / 2f;
            paint.setTextSize(mid);
            if (paint.measureText(text) <= maxWidth) low = mid; else high = mid;
        }
        return low;
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

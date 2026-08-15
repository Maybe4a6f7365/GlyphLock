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
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

    private GlyphSceneRenderer() {}

    static final class Scene {
        final int width;
        final int height;
        final Bitmap baseBitmap;
        final List<AmbientGlyph> ambientGlyphs;
        final List<MorphGlyph> morphGlyphs;
        final int accent;
        final DemoCatalog.Theme theme;

        Scene(
                int width,
                int height,
                Bitmap baseBitmap,
                List<AmbientGlyph> ambientGlyphs,
                List<MorphGlyph> morphGlyphs,
                int accent,
                DemoCatalog.Theme theme
        ) {
            this.width = width;
            this.height = height;
            this.baseBitmap = baseBitmap;
            this.ambientGlyphs = ambientGlyphs;
            this.morphGlyphs = morphGlyphs;
            this.accent = accent;
            this.theme = theme;
        }

        void recycle() {
            if (!baseBitmap.isRecycled()) baseBitmap.recycle();
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

    private static final class TargetGlyph {
        final float x;
        final float y;
        final char glyph;
        final float size;
        final float alpha;
        final int color;
        final boolean text;

        TargetGlyph(float x, float y, char glyph, float size, float alpha, int color, boolean text) {
            this.x = x;
            this.y = y;
            this.glyph = glyph;
            this.size = size;
            this.alpha = alpha;
            this.color = color;
            this.text = text;
        }
    }

    private static final class TargetLayout {
        final List<TargetGlyph> textTargets;
        final RectF readableBounds;

        TargetLayout(List<TargetGlyph> textTargets, RectF readableBounds) {
            this.textTargets = textTargets;
            this.readableBounds = readableBounds;
        }
    }

    private static final class MorphGlyph {
        final GlyphPoint source;
        final TargetGlyph eventTarget;
        final TargetGlyph resultTarget;
        final float phase;
        final float arc;
        final float delay;
        final float duration;

        MorphGlyph(
                GlyphPoint source,
                TargetGlyph eventTarget,
                TargetGlyph resultTarget,
                float phase,
                float arc,
                float delay,
                float duration
        ) {
            this.source = source;
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
            DemoCatalog.Event event
    ) {
        RenderQuality quality = RenderQuality.LUX;
        int safeWidth = Math.max(360, surfaceWidth);
        int renderWidth = Math.min(quality.maxRenderWidth, safeWidth);
        int renderHeight = Math.max(
                800,
                Math.round(renderWidth * (surfaceHeight / (float) Math.max(1, surfaceWidth)))
        );

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        Bitmap decoded = BitmapFactory.decodeResource(context.getResources(), theme.maskResource, options);
        if (decoded == null) throw new IllegalStateException("Unable to decode glyph scene mask");
        Bitmap mask = Bitmap.createScaledBitmap(decoded, renderWidth, renderHeight, true);
        if (mask != decoded) decoded.recycle();

        List<GlyphPoint> basePoints = extractGlyphPoints(mask, renderWidth, renderHeight, theme, quality);
        mask.recycle();
        if (basePoints.isEmpty()) throw new IllegalStateException("Glyph scene mask produced no points");

        Bitmap base = renderBaseBitmap(renderWidth, renderHeight, basePoints, theme);
        List<AmbientGlyph> ambientGlyphs = buildAmbientGlyphs(basePoints, theme, quality);
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
                event
        );

        return new Scene(
                renderWidth,
                renderHeight,
                base,
                ambientGlyphs,
                morphGlyphs,
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
        float baseAlpha = 1f - GlyphMath.smooth(reveal / 0.38f);
        bitmapPaint.setAlpha(Math.round(255f * baseAlpha));
        if (bitmapPaint.getAlpha() > 0) output.drawBitmap(scene.baseBitmap, null, destination, bitmapPaint);

        output.save();
        output.scale(targetWidth / (float) scene.width, targetHeight / (float) scene.height);
        drawAmbientMotion(output, scene, frame, nowMs);
        drawMorphField(output, scene, frame, nowMs);
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
                new int[] {
                        withAlpha(theme.atmosphereColor, 16),
                        withAlpha(theme.atmosphereColor, 5),
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

    private static List<GlyphPoint> selectMorphSources(
            List<GlyphPoint> points,
            DemoCatalog.Theme theme,
            RenderQuality quality
    ) {
        List<GlyphPoint> candidates = new ArrayList<>();
        for (GlyphPoint point : points) {
            if (point.alpha > 0.15f) candidates.add(point);
        }
        if (candidates.isEmpty()) candidates.addAll(points);
        int count = Math.min(quality.maximumParticles, candidates.size());
        List<GlyphPoint> selected = new ArrayList<>(count);
        boolean[] used = new boolean[candidates.size()];
        int cursor = Math.floorMod(theme.ordinal() * 149, Math.max(1, candidates.size()));
        for (int i = 0; i < count; i++) {
            cursor = Math.floorMod(cursor + 97, candidates.size());
            while (used[cursor]) cursor = (cursor + 1) % candidates.size();
            used[cursor] = true;
            selected.add(candidates.get(cursor));
        }
        return selected;
    }

    private static TargetLayout buildTargetLayout(
            int width,
            int height,
            DemoCatalog.Theme theme,
            DemoCatalog.Event event,
            boolean result
    ) {
        List<TargetGlyph> targets = new ArrayList<>();
        float left = width * 0.115f;
        float contentWidth = width * 0.77f;
        float top = height * theme.cavityFraction - height * 0.035f;
        float y = top;
        int accent = event.accent;

        Paint eyebrowPaint = monoPaint(width * 0.020f, true, withAlpha(accent, 224));
        String eyebrow = result ? "LOCAL DEMO · ACTION SIMULATED" : event.eyebrow;
        y += eyebrowPaint.getTextSize();
        addLineTargets(targets, eyebrow, left, y, eyebrowPaint, withAlpha(accent, 230), 0.94f, true);

        y += height * 0.032f;
        String title = result ? event.resultTitle : event.title;
        Paint titlePaint = monoPaint(width * 0.056f, false, Color.rgb(241, 247, 250));
        titlePaint.setTextSize(fitTextSize(
                titlePaint,
                title,
                contentWidth,
                width * 0.056f,
                width * 0.034f
        ));
        y += titlePaint.getTextSize();
        addLineTargets(targets, title, left, y, titlePaint, Color.rgb(241, 247, 250), 1f, true);

        y += height * 0.030f;
        Paint summaryPaint = monoPaint(width * 0.0275f, false, Color.rgb(214, 225, 231));
        String summary = result ? event.resultSummary : event.summary;
        List<String> summaryLines = wrapText(summaryPaint, summary, contentWidth, 4);
        float lineHeight = summaryPaint.getTextSize() * 1.48f;
        for (String line : summaryLines) {
            y += lineHeight;
            addLineTargets(
                    targets,
                    line,
                    left,
                    y,
                    summaryPaint,
                    Color.rgb(214, 225, 231),
                    0.86f,
                    true
            );
        }

        y += height * 0.036f;
        Paint actionPaint = monoPaint(width * 0.0188f, true, withAlpha(accent, 232));
        String action = result ? "TAP TO RETURN" : event.action;
        y += actionPaint.getTextSize();
        addLineTargets(targets, action, left, y, actionPaint, withAlpha(accent, 236), 0.96f, true);

        RectF bounds = new RectF(
                left - width * 0.055f,
                top - height * 0.020f,
                left + contentWidth + width * 0.055f,
                y + height * 0.030f
        );
        addFragmentedRails(targets, bounds, width, height, event, result);
        return new TargetLayout(targets, bounds);
    }

    private static void addLineTargets(
            List<TargetGlyph> targets,
            String text,
            float left,
            float baseline,
            Paint paint,
            int color,
            float alpha,
            boolean textTarget
    ) {
        float x = left;
        for (int i = 0; i < text.length(); i++) {
            char glyph = text.charAt(i);
            float advance = paint.measureText(text, i, i + 1);
            if (!Character.isWhitespace(glyph)) {
                targets.add(new TargetGlyph(
                        x + advance * 0.5f,
                        baseline,
                        glyph,
                        paint.getTextSize(),
                        alpha,
                        color,
                        textTarget
                ));
            }
            x += advance;
        }
    }

    private static void addFragmentedRails(
            List<TargetGlyph> targets,
            RectF bounds,
            int width,
            int height,
            DemoCatalog.Event event,
            boolean result
    ) {
        int color = withAlpha(event.accent, result ? 154 : 132);
        float size = width * 0.010f;
        int horizontalCount = 38;
        for (int i = 0; i < horizontalCount; i++) {
            if (i > 7 && i < 18) continue;
            float t = i / (float) (horizontalCount - 1);
            float x = GlyphMath.mix(bounds.left, bounds.right, t);
            char glyph = STRUCTURE_GLYPHS.charAt((i * 5 + event.id.length()) % STRUCTURE_GLYPHS.length());
            targets.add(new TargetGlyph(
                    x,
                    bounds.top,
                    glyph,
                    size,
                    0.34f,
                    color,
                    false
            ));
            if ((i & 1) == 0) {
                targets.add(new TargetGlyph(
                        x,
                        bounds.bottom,
                        glyph,
                        size,
                        0.24f,
                        color,
                        false
                ));
            }
        }
        int verticalCount = 22;
        for (int i = 0; i < verticalCount; i++) {
            if (i > 5 && i < 13) continue;
            float t = i / (float) (verticalCount - 1);
            float y = GlyphMath.mix(bounds.top, bounds.bottom, t);
            char glyph = (i & 1) == 0 ? '│' : '·';
            targets.add(new TargetGlyph(
                    bounds.left,
                    y,
                    glyph,
                    size,
                    0.28f,
                    color,
                    false
            ));
        }
    }

    private static List<String> wrapText(Paint paint, String text, float maxWidth, int maxLines) {
        String[] words = text.trim().split("\\s+");
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            String candidate = current.length() == 0 ? word : current + " " + word;
            if (paint.measureText(candidate) <= maxWidth || current.length() == 0) {
                current.setLength(0);
                current.append(candidate);
                continue;
            }
            lines.add(current.toString());
            current.setLength(0);
            current.append(word);
            if (lines.size() == maxLines - 1) break;
        }
        if (current.length() > 0 && lines.size() < maxLines) lines.add(current.toString());
        if (lines.isEmpty()) lines.add(text);
        return lines;
    }

    private static List<MorphGlyph> buildMorphGlyphs(
            List<GlyphPoint> sources,
            TargetLayout eventLayout,
            TargetLayout resultLayout,
            int width,
            int height,
            DemoCatalog.Theme theme,
            DemoCatalog.Event event
    ) {
        TargetGlyph[] eventAssignments = assignNearestTargets(sources, eventLayout.textTargets, width, height);
        TargetGlyph[] resultAssignments = assignNearestTargets(sources, resultLayout.textTargets, width, height);
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
                        eventLayout.readableBounds,
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
                        resultLayout.readableBounds,
                        width,
                        height,
                        theme,
                        event,
                        i,
                        true
                );
            }

            float distance = (float) Math.hypot(source.x - focusX, source.y - focusY) / diagonal;
            float noise = GlyphMath.hash(i, source.x, source.y);
            morphs.add(new MorphGlyph(
                    source,
                    eventTarget,
                    resultTarget,
                    noise * (float) Math.PI * 2f,
                    width * (0.026f + noise * 0.082f) * theme.motionStrength,
                    0.01f + 0.30f * distance + 0.075f * noise,
                    0.52f + 0.17f * (1f - distance) + 0.06f * noise
            ));
        }
        return morphs;
    }

    private static TargetGlyph[] assignNearestTargets(
            List<GlyphPoint> sources,
            List<TargetGlyph> targets,
            int width,
            int height
    ) {
        TargetGlyph[] assignments = new TargetGlyph[sources.size()];
        boolean[] used = new boolean[sources.size()];
        float verticalWeight = width / (float) Math.max(width, height) * 1.8f;

        for (TargetGlyph target : targets) {
            int best = -1;
            float bestScore = Float.MAX_VALUE;
            for (int i = 0; i < sources.size(); i++) {
                if (used[i]) continue;
                GlyphPoint source = sources.get(i);
                float dx = source.x - target.x;
                float dy = (source.y - target.y) * verticalWeight;
                float score = dx * dx + dy * dy - source.alpha * width * width * 0.012f;
                if (score < bestScore) {
                    bestScore = score;
                    best = i;
                }
            }
            if (best < 0) break;
            used[best] = true;
            assignments[best] = target;
        }
        return assignments;
    }

    private static TargetGlyph buildFillerTarget(
            GlyphPoint source,
            RectF readableBounds,
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
                float rotation = (0.20f + noise * 0.26f) * (noise > 0.5f ? 1f : -1f);
                float scale = 0.92f + noise * 0.19f;
                float ca = (float) Math.cos(rotation);
                float sa = (float) Math.sin(rotation);
                x = focusX + (vx * ca - vy * sa) * scale;
                y = focusY + (vx * sa + vy * ca) * scale;
                break;
            }
            case CIRCUIT: {
                float gridX = width * 0.041f;
                float gridY = height * 0.022f;
                x = Math.round((source.x + (noise - 0.5f) * width * 0.08f) / gridX) * gridX;
                y = Math.round((source.y + (noise - 0.5f) * height * 0.04f) / gridY) * gridY;
                break;
            }
            case RADIAL: {
                float ring = Math.max(radius * (0.92f + noise * 0.20f), width * (0.20f + noise * 0.32f));
                x = focusX + (float) Math.cos(angle + seed * 0.10f) * ring;
                y = focusY + (float) Math.sin(angle + seed * 0.10f) * ring;
                break;
            }
            case BLOOM: {
                float petal = (float) Math.sin(angle * 6f + seed) * width * 0.050f;
                float scale = 0.92f + noise * 0.18f + petal / Math.max(width, radius) * 0.7f;
                x = focusX + vx * scale;
                y = focusY + vy * scale;
                break;
            }
            case FLOW:
            default:
                x += (float) Math.sin(source.y * 0.009f + seed + noise * 4f) * width * 0.055f;
                y += (float) Math.cos(source.x * 0.008f + seed + noise * 5f) * height * 0.016f;
                break;
        }

        float marginX = width * 0.055f;
        float marginY = height * 0.026f;
        RectF protectedArea = new RectF(
                readableBounds.left - marginX,
                readableBounds.top - marginY,
                readableBounds.right + marginX,
                readableBounds.bottom + marginY
        );
        if (protectedArea.contains(x, y)) {
            float toLeft = Math.abs(x - protectedArea.left);
            float toRight = Math.abs(protectedArea.right - x);
            float toTop = Math.abs(y - protectedArea.top) * 0.78f;
            float toBottom = Math.abs(protectedArea.bottom - y) * 0.78f;
            float minimum = Math.min(Math.min(toLeft, toRight), Math.min(toTop, toBottom));
            if (minimum == toLeft) x = protectedArea.left - marginX * (0.25f + noise * 0.6f);
            else if (minimum == toRight) x = protectedArea.right + marginX * (0.25f + noise * 0.6f);
            else if (minimum == toTop) y = protectedArea.top - marginY * (0.3f + noise * 0.8f);
            else y = protectedArea.bottom + marginY * (0.3f + noise * 0.8f);
        }

        x = Math.max(width * 0.018f, Math.min(width * 0.982f, x));
        y = Math.max(height * 0.035f, Math.min(height * 0.975f, y));
        String vocabulary = event.glyphs + theme.textureGlyphs + STRUCTURE_GLYPHS;
        char targetGlyph = noise > 0.70f
                ? vocabulary.charAt(Math.min(vocabulary.length() - 1, (int) (noise * vocabulary.length())))
                : source.glyph;
        float alpha = GlyphMath.clamp01(source.alpha * (result ? 0.42f : 0.50f));
        int color = mixColor(theme.atmosphereColor, event.accent, result ? 0.38f : 0.27f);
        return new TargetGlyph(
                x,
                y,
                targetGlyph,
                source.size * (0.86f + noise * 0.24f),
                alpha,
                color,
                false
        );
    }

    private static void drawMorphField(
            Canvas canvas,
            Scene scene,
            ExperienceController.Frame frame,
            long nowMs
    ) {
        float reveal = frame.revealProgress;
        if (reveal <= 0f) return;

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        paint.setTypeface(MONO);
        paint.setTextAlign(Paint.Align.CENTER);
        char[] one = new char[1];
        float seconds = nowMs / 1000f;
        float focusX = scene.width * scene.theme.atmosphereX;
        float focusY = scene.height * scene.theme.atmosphereY;
        float result = frame.resultProgress;
        float handoff = GlyphMath.smooth(reveal / 0.16f);

        for (MorphGlyph glyph : scene.morphGlyphs) {
            float local = GlyphMath.staggeredProgress(reveal, glyph.delay, glyph.duration);
            if (local <= 0f && handoff <= 0f) continue;
            float move = GlyphMath.easeOutCubic(local);
            float eventX;
            float eventY;
            float wave = (float) Math.sin(Math.PI * local);

            float targetX = glyph.eventTarget.x;
            float targetY = glyph.eventTarget.y;
            switch (scene.theme.motionStyle) {
                case CIRCUIT: {
                    boolean horizontalFirst = Math.sin(glyph.phase) >= 0f;
                    float first = GlyphMath.smooth(Math.min(1f, move * 1.82f));
                    float second = GlyphMath.smooth(Math.max(0f, (move - 0.45f) / 0.55f));
                    if (horizontalFirst) {
                        eventX = GlyphMath.mix(glyph.source.x, targetX, first);
                        eventY = GlyphMath.mix(glyph.source.y, targetY, second);
                    } else {
                        eventY = GlyphMath.mix(glyph.source.y, targetY, first);
                        eventX = GlyphMath.mix(glyph.source.x, targetX, second);
                    }
                    break;
                }
                case ORBITAL: {
                    float midX = (glyph.source.x + targetX) * 0.5f;
                    float midY = (glyph.source.y + targetY) * 0.5f;
                    float vx = midX - focusX;
                    float vy = midY - focusY;
                    float length = Math.max(1f, (float) Math.hypot(vx, vy));
                    float direction = Math.sin(glyph.phase) >= 0f ? 1f : -1f;
                    float controlX = midX - vy / length * glyph.arc * direction;
                    float controlY = midY + vx / length * glyph.arc * direction;
                    float u = 1f - move;
                    eventX = u * u * glyph.source.x + 2f * u * move * controlX + move * move * targetX;
                    eventY = u * u * glyph.source.y + 2f * u * move * controlY + move * move * targetY;
                    break;
                }
                case RADIAL: {
                    float tangent = glyph.phase + move * 2.4f;
                    float controlX = GlyphMath.mix(glyph.source.x, focusX, 0.58f)
                            + (float) Math.cos(tangent) * glyph.arc;
                    float controlY = GlyphMath.mix(glyph.source.y, focusY, 0.58f)
                            + (float) Math.sin(tangent) * glyph.arc * 0.66f;
                    float u = 1f - move;
                    eventX = u * u * glyph.source.x + 2f * u * move * controlX + move * move * targetX;
                    eventY = u * u * glyph.source.y + 2f * u * move * controlY + move * move * targetY;
                    break;
                }
                case BLOOM: {
                    float sourceAngle = (float) Math.atan2(glyph.source.y - focusY, glyph.source.x - focusX);
                    float petal = (float) Math.sin(sourceAngle * 6f + glyph.phase) * glyph.arc;
                    float controlX = focusX + (float) Math.cos(sourceAngle) * (scene.width * 0.22f + petal);
                    float controlY = focusY + (float) Math.sin(sourceAngle) * (scene.width * 0.18f + petal * 0.55f);
                    float u = 1f - move;
                    eventX = u * u * glyph.source.x + 2f * u * move * controlX + move * move * targetX;
                    eventY = u * u * glyph.source.y + 2f * u * move * controlY + move * move * targetY;
                    break;
                }
                case FLOW:
                default:
                    eventX = GlyphMath.mix(glyph.source.x, targetX, move)
                            + (float) Math.sin(glyph.phase + local * 5.2f) * glyph.arc * wave;
                    eventY = GlyphMath.mix(glyph.source.y, targetY, move)
                            + (float) Math.cos(glyph.phase * 1.3f + local * 4.1f) * glyph.arc * 0.48f * wave;
                    break;
            }

            float x = GlyphMath.mix(eventX, glyph.resultTarget.x, result);
            float y = GlyphMath.mix(eventY, glyph.resultTarget.y, result);
            float resultWave = (float) Math.sin(Math.PI * result);
            if (resultWave > 0f) {
                x += (float) Math.sin(glyph.phase * 1.7f + result * 4f) * glyph.arc * 0.13f * resultWave;
                y += (float) Math.cos(glyph.phase * 1.3f + result * 3f) * glyph.arc * 0.07f * resultWave;
            }

            TargetGlyph activeTarget = result < 0.5f ? glyph.eventTarget : glyph.resultTarget;
            if (!activeTarget.text && local >= 0.98f) {
                float drift = scene.theme.motionStrength * 0.62f;
                x += (float) Math.sin(seconds * 0.34f + glyph.phase) * 2.7f * drift;
                y += (float) Math.cos(seconds * 0.27f + glyph.phase * 1.4f) * 1.8f * drift;
            }
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

            float sourceFade = 1f - GlyphMath.smooth((local - 0.20f) / 0.52f);
            float targetFade = GlyphMath.smooth((local - 0.16f) / 0.62f);
            float sourceAlpha = glyph.source.alpha * sourceFade * handoff;
            float eventAlpha = glyph.eventTarget.alpha * targetFade * (1f - result);
            float resultAlpha = glyph.resultTarget.alpha * targetFade * result;

            if (sourceAlpha > 0.015f) {
                paint.setTextSize(GlyphMath.mix(glyph.source.size, glyph.eventTarget.size, move));
                paint.setColor(scene.theme.atmosphereColor);
                paint.setAlpha(Math.min(255, Math.round(sourceAlpha * 232f)));
                one[0] = glyph.source.glyph;
                canvas.drawText(one, 0, 1, x, y, paint);
            }
            if (eventAlpha > 0.015f) {
                paint.setTextSize(GlyphMath.mix(glyph.source.size, glyph.eventTarget.size, move));
                paint.setColor(glyph.eventTarget.color);
                paint.setAlpha(Math.min(255, Math.round(eventAlpha * 255f)));
                one[0] = glyph.eventTarget.glyph;
                canvas.drawText(one, 0, 1, x, y, paint);
            }
            if (resultAlpha > 0.015f) {
                paint.setTextSize(GlyphMath.mix(glyph.eventTarget.size, glyph.resultTarget.size, result));
                paint.setColor(glyph.resultTarget.color);
                paint.setAlpha(Math.min(255, Math.round(resultAlpha * 255f)));
                one[0] = glyph.resultTarget.glyph;
                canvas.drawText(one, 0, 1, x, y, paint);
            }
        }

        drawMorphWave(canvas, scene, reveal, result);
    }

    private static void drawMorphWave(Canvas canvas, Scene scene, float reveal, float result) {
        float visibility = (float) Math.sin(Math.PI * reveal) * (1f - result * 0.65f);
        if (visibility <= 0.001f) return;
        float waveY = GlyphMath.mix(scene.height * 0.13f, scene.height * 0.88f, reveal);
        Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
        glow.setShader(new LinearGradient(
                0f,
                waveY - scene.height * 0.046f,
                0f,
                waveY + scene.height * 0.046f,
                new int[] {
                        Color.TRANSPARENT,
                        withAlpha(scene.accent, Math.round(12f * visibility)),
                        Color.TRANSPARENT
                },
                new float[] { 0f, 0.5f, 1f },
                Shader.TileMode.CLAMP
        ));
        canvas.drawRect(
                0f,
                waveY - scene.height * 0.046f,
                scene.width,
                waveY + scene.height * 0.046f,
                glow
        );
    }

    private static void drawAmbientMotion(
            Canvas canvas,
            Scene scene,
            ExperienceController.Frame frame,
            long nowMs
    ) {
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
        float wakeRadius = GlyphMath.mix(
                scene.width * 0.02f,
                scene.width * 0.92f,
                GlyphMath.easeOutCubic(frame.wakeProgress)
        );
        float wakeStrength = 1f - frame.wakeProgress;
        float fade = 1f - GlyphMath.smooth(frame.revealProgress / 0.54f);
        if (fade <= 0.001f && wakeStrength <= 0.001f) return;

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
                    float petal = (float) Math.sin(angle * 6f + seconds * 0.66f + glyph.phase)
                            * 6.5f * strength;
                    float scale = 1f + petal / Math.max(scene.width * 0.18f, radius) * 0.42f;
                    x = focusX + vx * scale;
                    y = focusY + vy * scale;
                    break;
                }
                case FLOW:
                default:
                    x += (float) Math.sin(seconds * 0.55f + glyph.y * 0.006f + glyph.phase)
                            * 5.5f * strength;
                    y += (float) Math.cos(seconds * 0.38f + glyph.x * 0.005f + glyph.phase)
                            * 3.2f * strength;
                    break;
            }

            float wakeDistance = (radius - wakeRadius) / Math.max(1f, scene.width * 0.08f);
            float wakeBand = (float) Math.exp(-wakeDistance * wakeDistance) * wakeStrength;
            float twinkle = 0.58f + 0.42f
                    * (float) Math.sin(seconds * (1.1f + glyph.depth) + glyph.phase);
            float alpha = fade * (0.045f + glyph.alpha * 0.24f)
                    * (0.72f + twinkle * 0.28f) + wakeBand * 0.46f;
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
                    new int[] {
                            Color.TRANSPARENT,
                            withAlpha(scene.theme.atmosphereColor, Math.round(15f * wakeStrength)),
                            Color.TRANSPARENT
                    },
                    new float[] { 0.65f, 0.84f, 1f },
                    Shader.TileMode.CLAMP
            ));
            canvas.drawCircle(focusX, focusY, wakeRadius + scene.width * 0.10f, halo);
        }
    }

    private static void drawScan(Canvas canvas, Scene scene, long nowMs, boolean active) {
        float alpha = active ? 7f : 3f;
        float y = (nowMs * 0.030f) % scene.height;
        Paint scan = new Paint(Paint.ANTI_ALIAS_FLAG);
        scan.setShader(new LinearGradient(
                0f,
                y - scene.height * 0.030f,
                0f,
                y + scene.height * 0.030f,
                new int[] {
                        Color.TRANSPARENT,
                        Color.argb(Math.round(alpha), 200, 224, 236),
                        Color.TRANSPARENT
                },
                new float[] { 0f, 0.5f, 1f },
                Shader.TileMode.CLAMP
        ));
        canvas.drawRect(
                0f,
                y - scene.height * 0.030f,
                scene.width,
                y + scene.height * 0.030f,
                scan
        );
    }

    private static void drawPreviewChrome(Canvas canvas, long nowMs) {
        java.time.ZonedDateTime now = java.time.ZonedDateTime.now();
        String time = now.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
        String date = now.format(java.time.format.DateTimeFormatter.ofPattern("EEE · MMM d"))
                .toUpperCase(Locale.ROOT);
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

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

    private enum TargetRole {
        TITLE(0f),
        SUMMARY(0.045f),
        META(0.085f),
        ACTION(0.125f),
        STRUCTURE(0.015f);

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
        // Keep a very faint imprint of the original topology during the resolved state.
        // It reads as material continuity, not a second event layer.
        float baseAlpha = 1f - 0.96f * GlyphMath.smooth(reveal / 0.68f);
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
        List<TextBand> bands = new ArrayList<>();
        float centerX = width * theme.atmosphereX;
        float contentWidth = width * theme.semanticWidth;
        float left = centerX - contentWidth * 0.5f;
        float right = centerX + contentWidth * 0.5f;
        float anchorY = height * theme.semanticY;
        float topOffset;
        switch (theme.compositionStyle) {
            case ARCHITECTURE:
            case CASCADE:
                topOffset = height * 0.125f;
                break;
            case SPLICE:
                topOffset = height * 0.118f;
                break;
            case FIELD:
            case CONSTELLATION:
                topOffset = height * 0.112f;
                break;
            case DIAL:
                topOffset = height * 0.102f;
                break;
            case ORBITAL_BAND:
                topOffset = height * 0.105f;
                break;
            case CORE:
            case FIGURE:
            default:
                topOffset = height * 0.108f;
                break;
        }
        float top = anchorY - topOffset;

        int accent = event.accent;
        String eyebrow = result ? "LOCAL DEMO · ACTION SIMULATED" : event.eyebrow;
        String title = result ? event.resultTitle : event.title;
        String summary = result ? event.resultSummary : event.summary;
        String action = result ? "TAP TO RETURN" : event.action;

        Paint eyebrowPaint = monoPaint(width * 0.0182f, true, withAlpha(accent, 224));
        float preferredTitle = theme.compositionStyle == DemoCatalog.CompositionStyle.CASCADE
                ? width * 0.048f
                : width * 0.052f;
        Paint titlePaint = monoPaint(preferredTitle, false, Color.rgb(241, 247, 250));
        titlePaint.setTextSize(fitTextSize(
                titlePaint,
                title,
                contentWidth,
                preferredTitle,
                width * 0.033f
        ));
        Paint summaryPaint = monoPaint(width * 0.0253f, false, Color.rgb(214, 225, 231));
        Paint actionPaint = monoPaint(width * 0.0174f, true, withAlpha(accent, 232));

        float metaY = top + eyebrowPaint.getTextSize();
        float titleY = metaY + height * 0.034f + titlePaint.getTextSize();
        float summaryY = titleY + height * 0.026f;
        float lineHeight = summaryPaint.getTextSize() * 1.44f;
        List<String> summaryLines = wrapText(summaryPaint, summary, contentWidth, 3);
        List<Float> summaryBaselines = new ArrayList<>();
        for (String ignored : summaryLines) {
            summaryY += lineHeight;
            summaryBaselines.add(summaryY);
        }
        float actionY = summaryY + height * 0.034f + actionPaint.getTextSize();

        Paint.Align primaryAlign = theme.compositionStyle == DemoCatalog.CompositionStyle.ARCHITECTURE
                || theme.compositionStyle == DemoCatalog.CompositionStyle.CASCADE
                ? Paint.Align.LEFT
                : Paint.Align.CENTER;
        Paint.Align metaAlign = primaryAlign;
        Paint.Align actionAlign = primaryAlign;
        if (theme.compositionStyle == DemoCatalog.CompositionStyle.FIELD
                || theme.compositionStyle == DemoCatalog.CompositionStyle.CONSTELLATION) {
            metaAlign = Paint.Align.LEFT;
            actionAlign = Paint.Align.RIGHT;
        } else if (theme.compositionStyle == DemoCatalog.CompositionStyle.CASCADE) {
            metaAlign = Paint.Align.RIGHT;
            actionAlign = Paint.Align.LEFT;
        }

        float primaryAnchor = primaryAlign == Paint.Align.LEFT ? left : centerX;
        float metaAnchor = metaAlign == Paint.Align.LEFT ? left
                : metaAlign == Paint.Align.RIGHT ? right : centerX;
        float actionAnchor = actionAlign == Paint.Align.RIGHT ? right : primaryAnchor;
        PushAxis pushAxis = pushAxisFor(theme.compositionStyle);

        // Title and summary are assigned first so high-value source glyphs become language first.
        addAlignedLineTargets(
                targets, bands, title, primaryAnchor, titleY, titlePaint,
                Color.rgb(241, 247, 250), 1f, primaryAlign, TargetRole.TITLE, pushAxis
        );
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
            addAlignedLineTargets(
                    targets, bands, summaryLines.get(i), lineAnchor, summaryBaselines.get(i), summaryPaint,
                    Color.rgb(214, 225, 231), 0.90f, lineAlign, TargetRole.SUMMARY, pushAxis
            );
        }

        if (theme.compositionStyle == DemoCatalog.CompositionStyle.DIAL) {
            float arcCenterY = anchorY + height * 0.003f;
            addArcLineTargets(
                    targets, bands, eyebrow, centerX, arcCenterY,
                    contentWidth * 0.55f, height * 0.092f,
                    -2.62f, -0.52f, eyebrowPaint,
                    withAlpha(accent, 230), 0.96f, TargetRole.META
            );
            addArcLineTargets(
                    targets, bands, action, centerX, arcCenterY,
                    contentWidth * 0.53f, height * 0.102f,
                    0.52f, 2.62f, actionPaint,
                    withAlpha(accent, 236), 0.98f, TargetRole.ACTION
            );
        } else {
            addAlignedLineTargets(
                    targets, bands, eyebrow, metaAnchor, metaY, eyebrowPaint,
                    withAlpha(accent, 230), 0.96f, metaAlign, TargetRole.META, pushAxis
            );
            addAlignedLineTargets(
                    targets, bands, action, actionAnchor, actionY, actionPaint,
                    withAlpha(accent, 236), 0.98f, actionAlign, TargetRole.ACTION, pushAxis
            );
        }

        RectF semanticBounds = new RectF(
                left - width * 0.035f,
                top - height * 0.015f,
                right + width * 0.035f,
                Math.max(actionY + height * 0.022f, anchorY + height * 0.115f)
        );
        addSemanticStructure(targets, semanticBounds, width, height, theme, event, result);
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

    private static void addAlignedLineTargets(
            List<TargetGlyph> targets,
            List<TextBand> bands,
            String text,
            float anchorX,
            float baseline,
            Paint paint,
            int color,
            float alpha,
            Paint.Align align,
            TargetRole role,
            PushAxis pushAxis
    ) {
        float width = paint.measureText(text);
        float startX;
        if (align == Paint.Align.CENTER) startX = anchorX - width * 0.5f;
        else if (align == Paint.Align.RIGHT) startX = anchorX - width;
        else startX = anchorX;

        float x = startX;
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
                        true,
                        role
                ));
            }
            x += advance;
        }

        float padX = paint.getTextSize() * 0.52f;
        float padTop = paint.getTextSize() * 0.34f;
        float padBottom = paint.getTextSize() * 0.28f;
        bands.add(new TextBand(
                new RectF(
                        startX - padX,
                        baseline - paint.getTextSize() * 0.92f - padTop,
                        startX + width + padX,
                        baseline + paint.getTextSize() * 0.24f + padBottom
                ),
                pushAxis
        ));
    }

    private static void addArcLineTargets(
            List<TargetGlyph> targets,
            List<TextBand> bands,
            String text,
            float centerX,
            float centerY,
            float radiusX,
            float radiusY,
            float startAngle,
            float endAngle,
            Paint paint,
            int color,
            float alpha,
            TargetRole role
    ) {
        float total = Math.max(1f, paint.measureText(text));
        float cursor = 0f;
        for (int i = 0; i < text.length(); i++) {
            char glyph = text.charAt(i);
            float advance = paint.measureText(text, i, i + 1);
            float u = (cursor + advance * 0.5f) / total;
            float angle = GlyphMath.mix(startAngle, endAngle, u);
            float x = centerX + (float) Math.cos(angle) * radiusX;
            float y = centerY + (float) Math.sin(angle) * radiusY;
            if (!Character.isWhitespace(glyph)) {
                targets.add(new TargetGlyph(
                        x,
                        y,
                        glyph,
                        paint.getTextSize(),
                        alpha,
                        color,
                        true,
                        role
                ));
                float pad = paint.getTextSize() * 0.60f;
                bands.add(new TextBand(
                        new RectF(x - pad, y - pad, x + pad, y + pad),
                        PushAxis.RADIAL
                ));
            }
            cursor += advance;
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

            float distance = (float) Math.hypot(source.x - focusX, source.y - focusY) / diagonal;
            float noise = GlyphMath.hash(i, source.x, source.y);
            float roleDelay = eventTarget.role.delay;
            morphs.add(new MorphGlyph(
                    source,
                    eventTarget,
                    resultTarget,
                    noise * (float) Math.PI * 2f,
                    width * (0.020f + noise * 0.066f) * theme.motionStrength,
                    0.01f + 0.225f * distance + roleDelay + 0.045f * noise,
                    0.56f + 0.13f * (1f - distance) + 0.055f * noise
            ));
        }
        return morphs;
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
            sourceAlpha[i] = source.alpha;
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
        x = GlyphMath.mix(source.x, x, deformationInfluence);
        y = GlyphMath.mix(source.y, y, deformationInfluence);

        float[] warped = warpFillerAroundBands(x, y, layout, width, height, noise);
        x = warped[0];
        y = warped[1];
        x = Math.max(width * 0.018f, Math.min(width * 0.982f, x));
        y = Math.max(height * 0.035f, Math.min(height * 0.975f, y));

        String vocabulary = event.glyphs + theme.textureGlyphs + STRUCTURE_GLYPHS;
        char targetGlyph = noise > 0.82f
                ? vocabulary.charAt(Math.min(vocabulary.length() - 1, (int) (noise * vocabulary.length())))
                : source.glyph;
        float alpha = GlyphMath.clamp01(source.alpha * (result ? 0.70f : 0.78f));
        int color = mixColor(theme.atmosphereColor, event.accent, result ? 0.46f : 0.35f);
        return new TargetGlyph(
                x,
                y,
                targetGlyph,
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

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        paint.setTypeface(MONO);
        paint.setTextAlign(Paint.Align.CENTER);
        char[] one = new char[1];
        float seconds = nowMs / 1000f;
        float focusX = scene.width * scene.theme.atmosphereX;
        float focusY = scene.height * scene.theme.atmosphereY;
        float result = frame.resultProgress;
        float handoff = GlyphMath.smooth(reveal / 0.16f);

        for (int glyphIndex = 0; glyphIndex < scene.morphGlyphs.size(); glyphIndex++) {
            MorphGlyph glyph = scene.morphGlyphs.get(glyphIndex);
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
                case WAVE:
                    eventX = GlyphMath.mix(glyph.source.x, targetX, move)
                            + (float) Math.sin(glyph.source.y * 0.012f + glyph.phase + local * 7f)
                            * glyph.arc * 0.68f * wave;
                    eventY = GlyphMath.mix(glyph.source.y, targetY, move)
                            + (float) Math.sin(glyph.source.x * 0.010f - glyph.phase + local * 5.5f)
                            * glyph.arc * 0.34f * wave;
                    break;
                case FOLD: {
                    float direction = Math.sin(glyph.phase) >= 0f ? 1f : -1f;
                    float controlX = focusX + direction * glyph.arc * 0.55f;
                    float controlY = GlyphMath.mix(glyph.source.y, targetY, 0.48f);
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

            float eventBlend = GlyphMath.smooth((local - 0.20f) / 0.62f);
            float resultBlend = GlyphMath.smooth((result - 0.14f) / 0.72f);
            float size = GlyphMath.mix(glyph.source.size, glyph.eventTarget.size, eventBlend);
            size = GlyphMath.mix(size, glyph.resultTarget.size, resultBlend);
            float alpha = GlyphMath.mix(glyph.source.alpha * 0.94f, glyph.eventTarget.alpha, eventBlend);
            alpha = GlyphMath.mix(alpha, glyph.resultTarget.alpha, resultBlend) * handoff;
            int eventColor = mixColor(scene.theme.atmosphereColor, glyph.eventTarget.color, eventBlend);
            int finalColor = mixColor(eventColor, glyph.resultTarget.color, resultBlend);

            if (activeTarget.text && local > 0.88f) {
                alpha = Math.max(alpha, activeTarget.alpha * 0.94f);
            }
            if (alpha <= 0.012f) continue;

            if (result <= 0.001f
                    && glyphIndex % 5 == 0
                    && local > 0.08f
                    && local < 0.92f) {
                float trailVisibility = (float) Math.sin(Math.PI * local) * handoff * 0.11f;
                float echoProgress = GlyphMath.clamp01((move - 0.10f) / 0.90f);
                float echoX = GlyphMath.mix(glyph.source.x, eventX, echoProgress);
                float echoY = GlyphMath.mix(glyph.source.y, eventY, echoProgress);
                paint.setTextSize(GlyphMath.mix(glyph.source.size, size, 0.46f));
                paint.setColor(mixColor(scene.theme.atmosphereColor, finalColor, 0.34f));
                paint.setAlpha(Math.min(255, Math.round(alpha * trailVisibility * 255f)));
                one[0] = glyph.source.glyph;
                canvas.drawText(one, 0, 1, echoX, echoY, paint);
            }

            paint.setTextSize(size);
            paint.setColor(finalColor);
            float eventCharBlend = GlyphMath.smooth(
                    (local - charStartFor(glyph.eventTarget.role)) / 0.16f
            );
            if (result <= 0.001f) {
                drawGlyphTransition(
                        canvas,
                        paint,
                        one,
                        glyph.source.glyph,
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

        drawMorphWave(canvas, scene, reveal, result);
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
                case WAVE:
                    x += (float) Math.sin(seconds * 0.58f + glyph.y * 0.010f + glyph.phase)
                            * 5.8f * strength;
                    y += (float) Math.sin(seconds * 0.41f + glyph.x * 0.008f - glyph.phase)
                            * 3.7f * strength;
                    break;
                case FOLD: {
                    float depth = (glyph.y - focusY) / Math.max(1f, scene.height);
                    float fold = (float) Math.sin(seconds * 0.34f + glyph.phase) * 5.2f * strength;
                    x += fold * (0.35f + Math.abs(depth));
                    y += (float) Math.cos(seconds * 0.28f + glyph.phase * 1.3f) * 2.2f * strength;
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

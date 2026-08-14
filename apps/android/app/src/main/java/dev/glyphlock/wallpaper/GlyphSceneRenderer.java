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
        final List<Particle> particles;
        final int accent;

        Scene(
                int width,
                int height,
                Bitmap baseBitmap,
                Bitmap eventBitmap,
                Bitmap resultBitmap,
                List<Particle> particles,
                int accent
        ) {
            this.width = width;
            this.height = height;
            this.baseBitmap = baseBitmap;
            this.eventBitmap = eventBitmap;
            this.resultBitmap = resultBitmap;
            this.particles = particles;
            this.accent = accent;
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

        Particle(
                float sourceX,
                float sourceY,
                float targetX,
                float targetY,
                char glyph,
                float phase,
                float arc,
                float size,
                float alpha
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
        }
    }

    static Scene build(
            Context context,
            int surfaceWidth,
            int surfaceHeight,
            DemoCatalog.Theme theme,
            DemoCatalog.Event event
    ) {
        int safeWidth = Math.max(360, surfaceWidth);
        int renderWidth = Math.min(720, safeWidth);
        int renderHeight = Math.max(800, Math.round(renderWidth * (surfaceHeight / (float) Math.max(1, surfaceWidth))));

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        Bitmap decoded = BitmapFactory.decodeResource(context.getResources(), theme.maskResource, options);
        if (decoded == null) throw new IllegalStateException("Unable to decode glyph scene mask");
        Bitmap mask = Bitmap.createScaledBitmap(decoded, renderWidth, renderHeight, true);
        if (mask != decoded) decoded.recycle();

        List<GlyphPoint> basePoints = extractGlyphPoints(mask, renderWidth, renderHeight, theme);
        mask.recycle();

        Bitmap base = renderBaseBitmap(renderWidth, renderHeight, basePoints, theme);
        TextGeometry eventGeometry = renderEventBitmap(base, renderWidth, renderHeight, theme, event, false);
        TextGeometry resultGeometry = renderEventBitmap(base, renderWidth, renderHeight, theme, event, true);
        List<Particle> particles = buildParticles(basePoints, eventGeometry.targets, event, theme);

        return new Scene(
                renderWidth,
                renderHeight,
                base,
                eventGeometry.bitmap,
                resultGeometry.bitmap,
                particles,
                event.accent
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
            DemoCatalog.Theme theme
    ) {
        int[] pixels = new int[width * height];
        mask.getPixels(pixels, 0, width, 0, 0, width, height);
        List<GlyphPoint> points = new ArrayList<>();
        int step = Math.max(7, Math.round(width / 90f));
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
                float level = GlyphMath.clamp01(value * 1.10f + (noise - 0.5f) * 0.11f);
                int rampIndex = Math.min(RAMP.length() - 1, (int) Math.floor(level * RAMP.length()));
                char glyph = RAMP.charAt(rampIndex);
                if (level < 0.16f) {
                    glyph = BASE_GLYPHS.charAt(Math.min(BASE_GLYPHS.length() - 1, (int) (noise * BASE_GLYPHS.length())));
                }
                points.add(new GlyphPoint(
                        x + (noise - 0.5f) * 1.8f,
                        y + (GlyphMath.hash(y, x, 3f) - 0.5f) * 1.6f,
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

        int cool = theme == DemoCatalog.Theme.ORBIT ? 9 : theme == DemoCatalog.Theme.MOTH ? 4 : 12;
        char[] one = new char[1];
        for (GlyphPoint point : points) {
            int value = Math.round(100 + 155 * point.alpha);
            paint.setColor(Color.rgb(
                    Math.max(0, value - cool),
                    value,
                    Math.min(255, value + cool)
            ));
            paint.setAlpha(Math.round(point.alpha * 255f));
            paint.setTextSize(point.size);
            one[0] = point.glyph;
            canvas.drawText(one, 0, 1, point.x, point.y, paint);
        }

        Paint atmosphere = new Paint(Paint.ANTI_ALIAS_FLAG);
        atmosphere.setShader(new RadialGradient(
                width / 2f,
                height * 0.27f,
                width * 0.73f,
                new int[] { Color.argb(14, 198, 225, 237), Color.TRANSPARENT },
                new float[] { 0f, 1f },
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(width / 2f, height * 0.27f, width * 0.73f, atmosphere);
        return bitmap;
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
            DemoCatalog.Theme theme
    ) {
        List<GlyphPoint> eligibleSources = new ArrayList<>();
        for (GlyphPoint source : sources) {
            if (source.alpha > 0.24f) eligibleSources.add(source);
        }
        int count = Math.min(1400, Math.max(650, targets.size()));
        List<Particle> particles = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            GlyphPoint target = targets.isEmpty()
                    ? new GlyphPoint(360, 1000, '@', 1f, 9f)
                    : targets.get((i * 37) % targets.size());
            GlyphPoint source = eligibleSources.isEmpty()
                    ? target
                    : eligibleSources.get((i * 83 + event.id.length() * 101) % eligibleSources.size());
            float noise = GlyphMath.hash(i, event.id.length(), theme.ordinal() + 1f);
            char glyph = event.glyphs.charAt(Math.min(event.glyphs.length() - 1, (int) (noise * event.glyphs.length())));
            particles.add(new Particle(
                    source.x,
                    source.y,
                    target.x,
                    target.y,
                    glyph,
                    noise * (float) Math.PI * 2f,
                    19f + noise * 60f,
                    5.5f + noise * 4.5f,
                    0.22f + 0.72f * GlyphMath.hash(i, event.id.length(), 2f)
            ));
        }
        return particles;
    }

    private static void drawParticles(Canvas canvas, Scene scene, float reveal, long nowMs) {
        if (reveal <= 0f || reveal >= 1f) return;
        float move = GlyphMath.easeOutCubic(reveal);
        float visibility = (float) Math.sin(Math.PI * reveal);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        paint.setTypeface(MONO);
        paint.setTextAlign(Paint.Align.CENTER);
        char[] one = new char[1];
        int red = Color.red(scene.accent);
        int green = Color.green(scene.accent);
        int blue = Color.blue(scene.accent);

        for (Particle particle : scene.particles) {
            float wave = (float) Math.sin(Math.PI * reveal);
            float x = GlyphMath.mix(particle.sourceX, particle.targetX, move)
                    + (float) Math.sin(particle.phase + reveal * 5.2f) * particle.arc * wave;
            float y = GlyphMath.mix(particle.sourceY, particle.targetY, move)
                    + (float) Math.cos(particle.phase * 1.3f + reveal * 4.1f) * particle.arc * 0.48f * wave;
            paint.setTextSize(particle.size);
            paint.setColor(Color.rgb(red, green, blue));
            paint.setAlpha(Math.round(255f * visibility * particle.alpha));
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
                new int[] { Color.TRANSPARENT, withAlpha(scene.accent, Math.round(9f * visibility)), Color.TRANSPARENT },
                new float[] { 0f, 0.5f, 1f },
                Shader.TileMode.CLAMP
        ));
        canvas.drawRect(0f, waveY - scene.height * 0.042f, scene.width, waveY + scene.height * 0.042f, glow);
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

package dev.glyphlock.wallpaper;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.SystemClock;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/** Full-screen interactive visual proof using only local fixtures. */
final class GlyphPreviewView extends View implements TouchInterpreter.Listener {
    private final ExperienceController experience = new ExperienceController();
    private final TouchInterpreter touch = new TouchInterpreter(this);
    private final ExecutorService sceneExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "glyphlock-preview-scene");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicInteger generation = new AtomicInteger();

    private GlyphSceneRenderer.Scene scene;
    private DemoCatalog.Theme theme;
    private int eventIndex;
    private boolean initialRevealScheduled;

    GlyphPreviewView(Context context) {
        super(context);
        setBackgroundColor(Color.BLACK);
        setFocusable(true);
        theme = DemoPreferences.theme(context);
        eventIndex = DemoPreferences.eventIndex(context);
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        touch.setViewport(width, height);
        if (width > 0 && height > 0) rebuildScene();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        long now = SystemClock.uptimeMillis();
        GlyphSceneRenderer.Scene localScene = scene;
        if (localScene == null) {
            drawLoading(canvas, now);
            postInvalidateDelayed(80L);
            return;
        }

        ExperienceController.Frame frame = experience.frame(now);
        GlyphSceneRenderer.draw(canvas, localScene, frame, now, true);
        if (frame.needsAnimation) postInvalidateDelayed(frame.frameDelayMs);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return touch.onTouchEvent(event) || super.onTouchEvent(event);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        experience.wake(SystemClock.uptimeMillis());
        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        touch.cancel();
        sceneExecutor.shutdownNow();
        GlyphSceneRenderer.Scene old = scene;
        scene = null;
        if (old != null) old.recycle();
    }

    @Override
    public void onTap() {
        long now = SystemClock.uptimeMillis();
        if (experience.state() == ExperienceController.State.AMBIENT) {
            experience.reveal(now);
        } else if (experience.state() == ExperienceController.State.RESULT) {
            experience.collapse(now);
        }
        performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        invalidate();
    }

    @Override
    public void onLongHold() {
        experience.listen(SystemClock.uptimeMillis());
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        invalidate();
    }

    @Override
    public void onNext() {
        eventIndex = Math.floorMod(eventIndex + 1, DemoCatalog.EVENTS.size());
        DemoPreferences.setEventIndex(getContext(), eventIndex);
        experience.reveal(SystemClock.uptimeMillis());
        performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        rebuildScene();
    }

    @Override
    public void onPrevious() {
        eventIndex = Math.floorMod(eventIndex - 1, DemoCatalog.EVENTS.size());
        DemoPreferences.setEventIndex(getContext(), eventIndex);
        experience.reveal(SystemClock.uptimeMillis());
        performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        rebuildScene();
    }

    @Override
    public void onCollapse() {
        experience.collapse(SystemClock.uptimeMillis());
        performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        invalidate();
    }

    private void rebuildScene() {
        final int width = getWidth();
        final int height = getHeight();
        if (width <= 0 || height <= 0) return;
        final int ticket = generation.incrementAndGet();
        final DemoCatalog.Theme requestedTheme = theme;
        final DemoCatalog.Event requestedEvent = DemoCatalog.eventAt(eventIndex);
        final Context appContext = getContext().getApplicationContext();
        sceneExecutor.execute(() -> {
            GlyphSceneRenderer.Scene built;
            try {
                built = GlyphSceneRenderer.build(appContext, width, height, requestedTheme, requestedEvent);
            } catch (RuntimeException error) {
                post(() -> drawFailure(error));
                return;
            }
            post(() -> {
                if (ticket != generation.get() || !isAttachedToWindow()) {
                    built.recycle();
                    return;
                }
                GlyphSceneRenderer.Scene old = scene;
                scene = built;
                if (old != null) old.recycle();
                if (!initialRevealScheduled && DemoPreferences.autoReveal(getContext())) {
                    initialRevealScheduled = true;
                    postDelayed(() -> {
                        experience.reveal(SystemClock.uptimeMillis());
                        invalidate();
                    }, 360L);
                }
                invalidate();
            });
        });
    }

    private void drawFailure(RuntimeException error) {
        setContentDescription("Glyph scene failed to build: " + error.getMessage());
        invalidate();
    }

    private static void drawLoading(Canvas canvas, long now) {
        canvas.drawColor(Color.BLACK);
        float pulse = 0.42f + 0.30f * (float) Math.sin(now / 260f);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.argb(Math.round(255f * pulse), 159, 221, 238));
        paint.setTypeface(android.graphics.Typeface.MONOSPACE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(canvas.getWidth() * 0.024f);
        canvas.drawText("ASSEMBLING GLYPH FIELD", canvas.getWidth() / 2f, canvas.getHeight() / 2f, paint);
    }
}

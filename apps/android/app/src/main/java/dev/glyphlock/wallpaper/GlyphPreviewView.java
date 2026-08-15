package dev.glyphlock.wallpaper;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.os.Process;
import android.os.SystemClock;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Full-screen interactive visual proof using only local fixtures. */
final class GlyphPreviewView extends View implements TouchInterpreter.Listener {
    private final ExperienceController experience = new ExperienceController();
    private final TouchInterpreter touch = new TouchInterpreter(this);
    private final ThreadPoolExecutor sceneExecutor = new ThreadPoolExecutor(
            1,
            1,
            0L,
            TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(1),
            r -> {
        Thread thread = new Thread(() -> {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            r.run();
        }, "glyphlock-preview-scene");
        thread.setDaemon(true);
        return thread;
    }, new ThreadPoolExecutor.DiscardOldestPolicy());
    private final AtomicInteger generation = new AtomicInteger();
    private final BroadcastReceiver notificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!NotificationEventStore.ACTION_EVENT_CHANGED.equals(intent.getAction())) return;
            if (!notificationMode || !isAttachedToWindow()) return;
            eventRevision = NotificationEventStore.revision(getContext());
            retargetScene();
        }
    };

    private GlyphSceneRenderer.Scene scene;
    private DemoCatalog.Theme theme;
    private int eventIndex;
    private long eventRevision;
    private boolean notificationMode;
    private boolean receiverRegistered;
    private boolean initialRevealScheduled;

    GlyphPreviewView(Context context) {
        super(context);
        setBackgroundColor(Color.BLACK);
        setFocusable(true);
        theme = DemoPreferences.theme(context);
        eventIndex = DemoPreferences.eventIndex(context);
        eventRevision = DemoPreferences.selectedEventRevision(context);
        notificationMode = DemoPreferences.notificationEvents(context);
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
        long frameStartedAt = SystemClock.uptimeMillis();
        long now = frameStartedAt;
        GlyphSceneRenderer.Scene localScene = scene;
        if (localScene == null) {
            drawLoading(canvas, now);
            postInvalidateDelayed(80L);
            return;
        }

        ExperienceController.Frame frame = experience.frame(now);
        GlyphSceneRenderer.draw(canvas, localScene, frame, now, true);
        if (frame.needsAnimation) {
            long renderTime = SystemClock.uptimeMillis() - frameStartedAt;
            postInvalidateDelayed(Math.max(1L, frame.frameDelayMs - renderTime));
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return touch.onTouchEvent(event) || super.onTouchEvent(event);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        registerNotificationReceiver();
        experience.wake(SystemClock.uptimeMillis());
        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        touch.cancel();
        generation.incrementAndGet();
        unregisterNotificationReceiver();
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
        } else if (experience.state() == ExperienceController.State.FOCUSED
                && !notificationMode) {
            experience.listen(now);
        } else if (experience.state() == ExperienceController.State.RESULT) {
            experience.collapse(now);
        }
        performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        invalidate();
    }

    @Override
    public void onLongHold() {
        if (notificationMode) {
            experience.reveal(SystemClock.uptimeMillis());
            invalidate();
            return;
        }
        experience.listen(SystemClock.uptimeMillis());
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        invalidate();
    }

    @Override
    public void onNext() {
        if (notificationMode) return;
        eventIndex = Math.floorMod(eventIndex + 1, DemoCatalog.EVENTS.size());
        DemoPreferences.setEventIndex(getContext(), eventIndex);
        performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        retargetScene();
    }

    @Override
    public void onPrevious() {
        if (notificationMode) return;
        eventIndex = Math.floorMod(eventIndex - 1, DemoCatalog.EVENTS.size());
        DemoPreferences.setEventIndex(getContext(), eventIndex);
        performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        retargetScene();
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
        final DemoCatalog.Event requestedEvent = DemoPreferences.selectedEvent(getContext());
        final Context appContext = getContext().getApplicationContext();
        sceneExecutor.execute(() -> {
            GlyphSceneRenderer.Scene built;
            try {
                built = GlyphSceneRenderer.build(
                        appContext,
                        width,
                        height,
                        requestedTheme,
                        requestedEvent,
                        RenderQuality.BALANCED
                );
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
                if (!initialRevealScheduled
                        && (notificationMode || DemoPreferences.autoReveal(getContext()))) {
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

    private void retargetScene() {
        final GlyphSceneRenderer.Scene localScene = scene;
        if (localScene == null) {
            experience.reveal(SystemClock.uptimeMillis());
            rebuildScene();
            return;
        }
        final int ticket = generation.incrementAndGet();
        final DemoCatalog.Event requestedEvent = DemoPreferences.selectedEvent(getContext());
        final boolean fromResult = experience.state() == ExperienceController.State.RESULT;
        sceneExecutor.execute(() -> {
            GlyphSceneRenderer.Retarget retarget;
            try {
                retarget = GlyphSceneRenderer.prepareRetarget(
                        localScene,
                        requestedEvent,
                        fromResult
                );
            } catch (RuntimeException error) {
                post(() -> drawFailure(error));
                return;
            }
            post(() -> {
                if (ticket != generation.get()
                        || !isAttachedToWindow()
                        || scene != localScene) {
                    return;
                }
                GlyphSceneRenderer.applyRetarget(localScene, retarget);
                ExperienceController.State state = experience.state();
                long now = SystemClock.uptimeMillis();
                if (state == ExperienceController.State.AMBIENT
                        || state == ExperienceController.State.COLLAPSING
                        || state == ExperienceController.State.REVEALING) {
                    experience.reveal(now);
                } else {
                    experience.transitionEvent(now);
                }
                invalidate();
            });
        });
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void registerNotificationReceiver() {
        if (receiverRegistered) return;
        IntentFilter filter = new IntentFilter(NotificationEventStore.ACTION_EVENT_CHANGED);
        if (Build.VERSION.SDK_INT >= 33) {
            getContext().registerReceiver(notificationReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            getContext().registerReceiver(notificationReceiver, filter);
        }
        receiverRegistered = true;
    }

    private void unregisterNotificationReceiver() {
        if (!receiverRegistered) return;
        try {
            getContext().unregisterReceiver(notificationReceiver);
        } catch (IllegalArgumentException ignored) {
            // The activity may already have released its receiver context.
        }
        receiverRegistered = false;
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

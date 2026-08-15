package dev.glyphlock.wallpaper;

import android.annotation.SuppressLint;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.os.SystemClock;
import android.service.wallpaper.WallpaperService;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Android live-wallpaper shell for the visual proof. */
public final class GlyphWallpaperService extends WallpaperService {
    @Override
    public Engine onCreateEngine() {
        return new GlyphEngine();
    }

    private final class GlyphEngine extends Engine implements TouchInterpreter.Listener {
        private final Handler handler = new Handler(Looper.getMainLooper());
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
            }, "glyphlock-wallpaper-scene");
            thread.setDaemon(true);
            return thread;
        }, new ThreadPoolExecutor.DiscardOldestPolicy());
        private final AtomicInteger generation = new AtomicInteger();
        private final ExperienceController experience = new ExperienceController();
        private final TouchInterpreter touch = new TouchInterpreter(this);
        private final BroadcastReceiver notificationReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!NotificationEventStore.ACTION_EVENT_CHANGED.equals(intent.getAction())) return;
                if (!DemoPreferences.notificationEvents(GlyphWallpaperService.this)) return;
                if (!visible) return;
                notificationMode = true;
                eventRevision = NotificationEventStore.revision(GlyphWallpaperService.this);
                retargetScene();
            }
        };

        private volatile GlyphSceneRenderer.Scene scene;
        private boolean visible;
        private boolean receiverRegistered;
        private int surfaceWidth;
        private int surfaceHeight;
        private int eventIndex;
        private long eventRevision;
        private boolean notificationMode;
        private DemoCatalog.Theme theme;
        private long lastHandledTapAtMs;

        private final Runnable drawRunnable = this::drawFrame;
        private final Runnable revealRunnable = () -> {
            if (!visible) return;
            experience.reveal(SystemClock.uptimeMillis());
            drawFrame();
        };

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            setTouchEventsEnabled(true);
            theme = DemoPreferences.theme(GlyphWallpaperService.this);
            eventIndex = DemoPreferences.eventIndex(GlyphWallpaperService.this);
            eventRevision = DemoPreferences.selectedEventRevision(GlyphWallpaperService.this);
            notificationMode = DemoPreferences.notificationEvents(GlyphWallpaperService.this);
            registerNotificationReceiver();
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            surfaceWidth = width;
            surfaceHeight = height;
            touch.setViewport(width, height);
            rebuildScene();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            this.visible = visible;
            handler.removeCallbacks(drawRunnable);
            handler.removeCallbacks(revealRunnable);
            if (visible) {
                long now = SystemClock.uptimeMillis();
                experience.wake(now);
                DemoCatalog.Theme configuredTheme = DemoPreferences.theme(GlyphWallpaperService.this);
                int configuredEvent = DemoPreferences.eventIndex(GlyphWallpaperService.this);
                boolean configuredNotificationMode =
                        DemoPreferences.notificationEvents(GlyphWallpaperService.this);
                long configuredRevision =
                        DemoPreferences.selectedEventRevision(GlyphWallpaperService.this);
                if (configuredTheme != theme
                        || configuredEvent != eventIndex
                        || configuredNotificationMode != notificationMode
                        || configuredRevision != eventRevision) {
                    theme = configuredTheme;
                    eventIndex = configuredEvent;
                    notificationMode = configuredNotificationMode;
                    eventRevision = configuredRevision;
                    rebuildScene();
                }
                drawFrame();
                if (scene != null && shouldAutoReveal()) {
                    handler.postDelayed(revealRunnable, 480L);
                }
            }
        }

        @Override
        public void onTouchEvent(MotionEvent event) {
            touch.onTouchEvent(event);
            super.onTouchEvent(event);
        }

        @Override
        public Bundle onCommand(
                String action,
                int x,
                int y,
                int z,
                Bundle extras,
                boolean resultRequested
        ) {
            if (WallpaperManager.COMMAND_TAP.equals(action)) handleTap();
            return super.onCommand(action, x, y, z, extras, resultRequested);
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            visible = false;
            handler.removeCallbacksAndMessages(null);
            touch.cancel();
            super.onSurfaceDestroyed(holder);
        }

        @Override
        public void onDestroy() {
            handler.removeCallbacksAndMessages(null);
            touch.cancel();
            generation.incrementAndGet();
            sceneExecutor.shutdownNow();
            unregisterNotificationReceiver();
            GlyphSceneRenderer.Scene old = scene;
            scene = null;
            if (old != null) old.recycle();
            super.onDestroy();
        }

        @Override
        public void onTap() {
            handleTap();
        }

        private void handleTap() {
            if (!visible) return;
            long now = SystemClock.uptimeMillis();
            // Some hosts deliver both a raw ACTION_UP and COMMAND_TAP for one physical tap.
            if (now - lastHandledTapAtMs < 280L) return;
            lastHandledTapAtMs = now;
            if (experience.state() == ExperienceController.State.AMBIENT) {
                experience.reveal(now);
            } else if (experience.state() == ExperienceController.State.FOCUSED
                    && !notificationMode) {
                experience.listen(now);
            } else if (experience.state() == ExperienceController.State.RESULT) {
                experience.collapse(now);
            }
            drawFrame();
        }

        @Override
        public void onLongHold() {
            if (notificationMode) {
                experience.reveal(SystemClock.uptimeMillis());
                drawFrame();
                return;
            }
            experience.listen(SystemClock.uptimeMillis());
            drawFrame();
        }

        @Override
        public void onNext() {
            if (notificationMode) return;
            eventIndex = Math.floorMod(eventIndex + 1, DemoCatalog.EVENTS.size());
            DemoPreferences.setEventIndex(GlyphWallpaperService.this, eventIndex);
            retargetScene();
        }

        @Override
        public void onPrevious() {
            if (notificationMode) return;
            eventIndex = Math.floorMod(eventIndex - 1, DemoCatalog.EVENTS.size());
            DemoPreferences.setEventIndex(GlyphWallpaperService.this, eventIndex);
            retargetScene();
        }

        @Override
        public void onCollapse() {
            experience.collapse(SystemClock.uptimeMillis());
            drawFrame();
        }

        private void rebuildScene() {
            if (surfaceWidth <= 0 || surfaceHeight <= 0) return;
            final int width = surfaceWidth;
            final int height = surfaceHeight;
            final int ticket = generation.incrementAndGet();
            final DemoCatalog.Theme requestedTheme = theme;
            final DemoCatalog.Event requestedEvent =
                    DemoPreferences.selectedEvent(GlyphWallpaperService.this);
            sceneExecutor.execute(() -> {
                GlyphSceneRenderer.Scene built;
                try {
                    built = GlyphSceneRenderer.build(
                            getApplicationContext(),
                            width,
                            height,
                            requestedTheme,
                            requestedEvent,
                            RenderQuality.ECO
                    );
                } catch (RuntimeException ignored) {
                    return;
                }
                handler.post(() -> {
                    if (ticket != generation.get()) {
                        built.recycle();
                        return;
                    }
                    GlyphSceneRenderer.Scene old = scene;
                    scene = built;
                    if (old != null) old.recycle();
                    drawFrame();
                    if (visible && shouldAutoReveal()) {
                        handler.removeCallbacks(revealRunnable);
                        handler.postDelayed(revealRunnable, 360L);
                    }
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
            final DemoCatalog.Event requestedEvent =
                    DemoPreferences.selectedEvent(GlyphWallpaperService.this);
            final boolean fromResult = experience.state() == ExperienceController.State.RESULT;
            sceneExecutor.execute(() -> {
                GlyphSceneRenderer.Retarget retarget;
                try {
                    retarget = GlyphSceneRenderer.prepareRetarget(
                            localScene,
                            requestedEvent,
                            fromResult
                    );
                } catch (RuntimeException ignored) {
                    return;
                }
                handler.post(() -> {
                    if (ticket != generation.get() || scene != localScene) return;
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
                    drawFrame();
                });
            });
        }

        private void drawFrame() {
            handler.removeCallbacks(drawRunnable);
            if (!visible) return;
            SurfaceHolder holder = getSurfaceHolder();
            Canvas canvas = null;
            long frameStartedAt = SystemClock.uptimeMillis();
            try {
                try {
                    canvas = holder.lockHardwareCanvas();
                } catch (RuntimeException unavailable) {
                    canvas = holder.lockCanvas();
                }
                if (canvas == null) return;
                long now = frameStartedAt;
                GlyphSceneRenderer.Scene localScene = scene;
                if (localScene == null) {
                    drawLoading(canvas, now);
                    handler.postDelayed(drawRunnable, 80L);
                    return;
                }
                ExperienceController.Frame frame = experience.frame(now);
                GlyphSceneRenderer.draw(canvas, localScene, frame, now, false);
                if (frame.needsAnimation) {
                    long renderTime = SystemClock.uptimeMillis() - frameStartedAt;
                    handler.postDelayed(drawRunnable, Math.max(1L, frame.frameDelayMs - renderTime));
                }
            } finally {
                if (canvas != null) holder.unlockCanvasAndPost(canvas);
            }
        }

        private boolean shouldAutoReveal() {
            return notificationMode || DemoPreferences.autoReveal(GlyphWallpaperService.this);
        }

        @SuppressLint("UnspecifiedRegisterReceiverFlag")
        private void registerNotificationReceiver() {
            if (receiverRegistered) return;
            IntentFilter filter = new IntentFilter(NotificationEventStore.ACTION_EVENT_CHANGED);
            if (Build.VERSION.SDK_INT >= 33) {
                GlyphWallpaperService.this.registerReceiver(
                        notificationReceiver,
                        filter,
                        Context.RECEIVER_NOT_EXPORTED
                );
            } else {
                GlyphWallpaperService.this.registerReceiver(notificationReceiver, filter);
            }
            receiverRegistered = true;
        }

        private void unregisterNotificationReceiver() {
            if (!receiverRegistered) return;
            try {
                GlyphWallpaperService.this.unregisterReceiver(notificationReceiver);
            } catch (IllegalArgumentException ignored) {
                // The framework may tear an engine down after its service context is gone.
            }
            receiverRegistered = false;
        }

        private void drawLoading(Canvas canvas, long now) {
            canvas.drawColor(Color.BLACK);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setTypeface(android.graphics.Typeface.MONOSPACE);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(canvas.getWidth() * 0.022f);
            float pulse = 0.35f + 0.25f * (float) Math.sin(now / 260f);
            paint.setColor(Color.argb(Math.round(255f * pulse), 159, 221, 238));
            canvas.drawText(
                    "ASSEMBLING GLYPH FIELD",
                    canvas.getWidth() / 2f,
                    canvas.getHeight() / 2f,
                    paint
            );
        }
    }
}

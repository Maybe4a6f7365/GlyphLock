package dev.glyphlock.wallpaper;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.service.wallpaper.WallpaperService;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/** Android live-wallpaper shell for the visual proof. */
public final class GlyphWallpaperService extends WallpaperService {
    @Override
    public Engine onCreateEngine() {
        return new GlyphEngine();
    }

    private final class GlyphEngine extends Engine implements TouchInterpreter.Listener {
        private final Handler handler = new Handler(Looper.getMainLooper());
        private final ExecutorService sceneExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "glyphlock-wallpaper-scene");
            thread.setDaemon(true);
            return thread;
        });
        private final AtomicInteger generation = new AtomicInteger();
        private final ExperienceController experience = new ExperienceController();
        private final TouchInterpreter touch = new TouchInterpreter(this);

        private volatile GlyphSceneRenderer.Scene scene;
        private boolean visible;
        private int surfaceWidth;
        private int surfaceHeight;
        private int eventIndex;
        private DemoCatalog.Theme theme;

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
                if (configuredTheme != theme || configuredEvent != eventIndex) {
                    theme = configuredTheme;
                    eventIndex = configuredEvent;
                    rebuildScene();
                }
                drawFrame();
                if (scene != null && DemoPreferences.autoReveal(GlyphWallpaperService.this)) {
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
            sceneExecutor.shutdownNow();
            GlyphSceneRenderer.Scene old = scene;
            scene = null;
            if (old != null) old.recycle();
            super.onDestroy();
        }

        @Override
        public void onTap() {
            long now = SystemClock.uptimeMillis();
            if (experience.state() == ExperienceController.State.AMBIENT) {
                experience.reveal(now);
            } else if (experience.state() == ExperienceController.State.RESULT) {
                experience.collapse(now);
            }
            drawFrame();
        }

        @Override
        public void onLongHold() {
            experience.listen(SystemClock.uptimeMillis());
            drawFrame();
        }

        @Override
        public void onNext() {
            eventIndex = Math.floorMod(eventIndex + 1, DemoCatalog.EVENTS.size());
            DemoPreferences.setEventIndex(GlyphWallpaperService.this, eventIndex);
            experience.reveal(SystemClock.uptimeMillis());
            rebuildScene();
        }

        @Override
        public void onPrevious() {
            eventIndex = Math.floorMod(eventIndex - 1, DemoCatalog.EVENTS.size());
            DemoPreferences.setEventIndex(GlyphWallpaperService.this, eventIndex);
            experience.reveal(SystemClock.uptimeMillis());
            rebuildScene();
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
            final DemoCatalog.Event requestedEvent = DemoCatalog.eventAt(eventIndex);
            sceneExecutor.execute(() -> {
                GlyphSceneRenderer.Scene built;
                try {
                    built = GlyphSceneRenderer.build(
                            getApplicationContext(),
                            width,
                            height,
                            requestedTheme,
                            requestedEvent
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
                    if (visible && DemoPreferences.autoReveal(GlyphWallpaperService.this)) {
                        handler.removeCallbacks(revealRunnable);
                        handler.postDelayed(revealRunnable, 360L);
                    }
                });
            });
        }

        private void drawFrame() {
            handler.removeCallbacks(drawRunnable);
            if (!visible) return;
            SurfaceHolder holder = getSurfaceHolder();
            Canvas canvas = null;
            try {
                canvas = holder.lockCanvas();
                if (canvas == null) return;
                long now = SystemClock.uptimeMillis();
                GlyphSceneRenderer.Scene localScene = scene;
                if (localScene == null) {
                    drawLoading(canvas, now);
                    handler.postDelayed(drawRunnable, 80L);
                    return;
                }
                ExperienceController.Frame frame = experience.frame(now);
                GlyphSceneRenderer.draw(canvas, localScene, frame, now, false);
                if (frame.needsAnimation) handler.postDelayed(drawRunnable, frame.frameDelayMs);
            } finally {
                if (canvas != null) holder.unlockCanvasAndPost(canvas);
            }
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

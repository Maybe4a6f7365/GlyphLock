package dev.glyphlock.wallpaper;

import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;

/** Converts best-effort wallpaper touch events into a small semantic vocabulary. */
final class TouchInterpreter {
    interface Listener {
        void onTap();
        void onLongHold();
        void onNext();
        void onPrevious();
        void onCollapse();
    }

    private final Listener listener;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable holdRunnable = this::fireHold;

    private float downX;
    private float downY;
    private long downAt;
    private int width = 1;
    private int height = 1;
    private boolean tracking;
    private boolean holdFired;

    TouchInterpreter(Listener listener) {
        this.listener = listener;
    }

    void setViewport(int width, int height) {
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
    }

    boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getX();
                downY = event.getY();
                downAt = event.getEventTime();
                tracking = true;
                holdFired = false;
                if (downY / height > 0.64f) {
                    handler.removeCallbacks(holdRunnable);
                    handler.postDelayed(holdRunnable, 560L);
                }
                return true;
            case MotionEvent.ACTION_UP:
                handler.removeCallbacks(holdRunnable);
                if (!tracking) return false;
                tracking = false;
                if (holdFired) return true;
                float dx = (event.getX() - downX) / width;
                float dy = (event.getY() - downY) / height;
                long held = event.getEventTime() - downAt;
                if (held > 620L) return true;
                if (Math.abs(dx) > 0.15f && Math.abs(dx) > Math.abs(dy)) {
                    if (dx < 0f) listener.onNext(); else listener.onPrevious();
                } else if (dy < -0.12f) {
                    listener.onCollapse();
                } else {
                    listener.onTap();
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                handler.removeCallbacks(holdRunnable);
                tracking = false;
                return true;
            default:
                return tracking;
        }
    }

    void cancel() {
        handler.removeCallbacks(holdRunnable);
        tracking = false;
    }

    private void fireHold() {
        if (!tracking || holdFired) return;
        holdFired = true;
        listener.onLongHold();
    }
}

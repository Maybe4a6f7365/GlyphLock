package dev.glyphlock.wallpaper;

/** Small deterministic state machine shared by the preview and live wallpaper. */
final class ExperienceController {
    enum State {
        AMBIENT,
        REVEALING,
        FOCUSED,
        EVENT_TRANSITION,
        LISTENING,
        RESULT_TRANSITION,
        RESULT,
        COLLAPSING
    }

    static final class Frame {
        final float revealProgress;
        final float eventProgress;
        final float resultProgress;
        final float wakeProgress;
        final boolean listening;
        final boolean needsAnimation;
        final int frameDelayMs;
        final State state;

        Frame(
                float revealProgress,
                float eventProgress,
                float resultProgress,
                float wakeProgress,
                boolean listening,
                boolean needsAnimation,
                int frameDelayMs,
                State state
        ) {
            this.revealProgress = revealProgress;
            this.eventProgress = eventProgress;
            this.resultProgress = resultProgress;
            this.wakeProgress = wakeProgress;
            this.listening = listening;
            this.needsAnimation = needsAnimation;
            this.frameDelayMs = frameDelayMs;
            this.state = state;
        }
    }

    private State state = State.AMBIENT;
    private long stateStartedAtMs = 0L;
    private long wakeStartedAtMs = 0L;

    State state() {
        return state;
    }

    void wake(long nowMs) {
        wakeStartedAtMs = nowMs;
    }

    void reveal(long nowMs) {
        state = State.REVEALING;
        stateStartedAtMs = nowMs;
    }

    /** Morphs the resolved semantic topology directly into a newly compiled event. */
    void transitionEvent(long nowMs) {
        state = State.EVENT_TRANSITION;
        stateStartedAtMs = nowMs;
    }

    void collapse(long nowMs) {
        if (state == State.AMBIENT) return;
        state = State.COLLAPSING;
        stateStartedAtMs = nowMs;
    }

    void listen(long nowMs) {
        state = State.LISTENING;
        stateStartedAtMs = nowMs;
    }

    void resetAmbient() {
        state = State.AMBIENT;
        stateStartedAtMs = 0L;
    }

    Frame frame(long nowMs) {
        float reveal = 0f;
        float event = 1f;
        float result = 0f;
        boolean listening = false;
        boolean transition = false;
        boolean operationalMotion = false;

        switch (state) {
            case AMBIENT:
                break;
            case REVEALING: {
                // The art-to-language topology change must be visible, not hidden by a fast fade.
                float raw = (nowMs - stateStartedAtMs) / 2450f;
                reveal = GlyphMath.clamp01(raw);
                transition = reveal < 1f;
                if (!transition) {
                    state = State.FOCUSED;
                    stateStartedAtMs = nowMs;
                    operationalMotion = true;
                }
                break;
            }
            case FOCUSED:
                reveal = 1f;
                operationalMotion = nowMs - stateStartedAtMs < 6200L;
                break;
            case EVENT_TRANSITION: {
                reveal = 1f;
                event = GlyphMath.clamp01((nowMs - stateStartedAtMs) / 1500f);
                transition = event < 1f;
                if (!transition) {
                    state = State.FOCUSED;
                    stateStartedAtMs = nowMs;
                    operationalMotion = true;
                }
                break;
            }
            case LISTENING: {
                reveal = 1f;
                listening = true;
                transition = true;
                if (nowMs - stateStartedAtMs >= 1850L) {
                    state = State.RESULT_TRANSITION;
                    stateStartedAtMs = nowMs;
                    listening = false;
                }
                break;
            }
            case RESULT_TRANSITION: {
                reveal = 1f;
                result = GlyphMath.clamp01((nowMs - stateStartedAtMs) / 1350f);
                transition = result < 1f;
                if (!transition) {
                    state = State.RESULT;
                    stateStartedAtMs = nowMs;
                    operationalMotion = true;
                }
                break;
            }
            case RESULT:
                reveal = 1f;
                result = 1f;
                operationalMotion = nowMs - stateStartedAtMs < 6200L;
                break;
            case COLLAPSING: {
                reveal = 1f - GlyphMath.clamp01((nowMs - stateStartedAtMs) / 1900f);
                transition = reveal > 0f;
                if (!transition) state = State.AMBIENT;
                break;
            }
        }

        float wake = wakeStartedAtMs <= 0L
                ? 1f
                : GlyphMath.clamp01((nowMs - wakeStartedAtMs) / 2200f);
        boolean wakeAnimating = wake < 1f;
        // Stable wallpaper states are deliberately static. A perpetual redraw loop competes
        // with the launcher and lock screen even when the user cannot perceive useful motion.
        boolean animate = transition || listening || wakeAnimating || operationalMotion;
        int delay;
        if (transition || listening || wakeAnimating) {
            delay = 33; // A consistent 30 fps transition budget is smooth and battery-safe.
        } else if (operationalMotion) {
            delay = 66; // Brief 15 fps system-life tail; stable text never moves.
        } else {
            delay = 1000; // Not scheduled while needsAnimation is false.
        }

        return new Frame(
                GlyphMath.smooth(reveal),
                GlyphMath.smooth(event),
                GlyphMath.smooth(result),
                GlyphMath.smooth(wake),
                listening,
                animate,
                delay,
                state
        );
    }
}

package dev.glyphlock.wallpaper;

import java.util.Arrays;
import java.util.List;

/** Local-only fixtures used by the visual prototype. No network or agent is involved. */
public final class DemoCatalog {
    private DemoCatalog() {}

    /** Pure-Java ARGB packing keeps fixture metadata available to local JVM tests. */
    private static int rgb(int red, int green, int blue) {
        return 0xFF000000
                | ((red & 0xFF) << 16)
                | ((green & 0xFF) << 8)
                | (blue & 0xFF);
    }

    public enum MotionStyle {
        FLOW,
        ORBITAL,
        CIRCUIT,
        RADIAL,
        BLOOM,
        WAVE,
        FOLD
    }

    /**
     * Describes how information inhabits a theme. These are composition grammars, not cards.
     * The renderer uses them to place language along the artwork's existing structural logic.
     */
    public enum CompositionStyle {
        FIGURE,
        CORE,
        ORBITAL_BAND,
        ARCHITECTURE,
        SPLICE,
        FIELD
    }

    public enum Theme {
        SENTINEL(
                "Sentinel", R.drawable.scene_sentinel, 0.53f, 0.72f,
                "[]<>/\\|01", 1.00f, rgb(206, 229, 240), 0.50f, 0.27f,
                MotionStyle.FLOW, CompositionStyle.FIGURE, 0.72f
        ),
        MOTH(
                "Moth", R.drawable.scene_moth, 0.51f, 0.69f,
                "(){}<>/\\:;", 1.02f, rgb(230, 219, 188), 0.50f, 0.38f,
                MotionStyle.BLOOM, CompositionStyle.FIGURE, 0.88f
        ),
        ORBIT(
                "Orbit", R.drawable.scene_orbit, 0.55f, 0.72f,
                "0O()[]:.;01", 1.05f, rgb(208, 199, 244), 0.50f, 0.34f,
                MotionStyle.ORBITAL, CompositionStyle.ORBITAL_BAND, 0.86f
        ),
        NEURAL_HALO(
                "Neural Halo", R.drawable.scene_neural_halo, 0.53f, 0.68f,
                ".:;~λψ∇01+<>@", 1.20f, rgb(137, 235, 221), 0.50f, 0.35f,
                MotionStyle.RADIAL, CompositionStyle.CORE, 1.05f
        ),
        CIPHER_CATHEDRAL(
                "Cipher Cathedral", R.drawable.scene_cipher_cathedral, 0.53f, 0.62f,
                ".:;|[]{}0x#AF16+-", 1.22f, rgb(246, 194, 119), 0.50f, 0.39f,
                MotionStyle.CIRCUIT, CompositionStyle.ARCHITECTURE, 0.92f
        ),
        QUANTUM_LATTICE(
                "Quantum Lattice", R.drawable.scene_quantum_lattice, 0.56f, 0.72f,
                ".:~λψ∂∇∞01()<>@", 1.19f, rgb(196, 178, 255), 0.50f, 0.38f,
                MotionStyle.WAVE, CompositionStyle.FIELD, 1.08f
        ),
        FUSION_CORE(
                "Fusion Core", R.drawable.scene_fusion_core, 0.55f, 0.66f,
                ".:;=+⊙○()[]|01#@", 1.19f, rgb(126, 221, 250), 0.50f, 0.39f,
                MotionStyle.ORBITAL, CompositionStyle.CORE, 1.18f
        ),
        PACKET_BLOOM(
                "Packet Bloom", R.drawable.scene_packet_bloom, 0.55f, 0.68f,
                ".:;<>[]{}:/\\01TCPIP+@", 1.21f, rgb(166, 240, 193), 0.50f, 0.41f,
                MotionStyle.BLOOM, CompositionStyle.CORE, 1.05f
        ),
        EVENT_HORIZON(
                "Event Horizon", R.drawable.scene_event_horizon, 0.54f, 0.70f,
                ".:;~O0()[]<>∞λ01#@", 1.24f, rgb(255, 173, 112), 0.50f, 0.38f,
                MotionStyle.ORBITAL, CompositionStyle.ORBITAL_BAND, 1.24f
        ),
        TESSERACT_ENGINE(
                "Tesseract Engine", R.drawable.scene_tesseract_engine, 0.52f, 0.60f,
                ".:;|+-=[]{}<>01XYZW", 1.22f, rgb(164, 211, 255), 0.50f, 0.36f,
                MotionStyle.FOLD, CompositionStyle.ARCHITECTURE, 1.08f
        ),
        HELIX_ARRAY(
                "Helix Array", R.drawable.scene_helix_array, 0.56f, 0.62f,
                ".:;~ATCGλψ01/\\()[]", 1.19f, rgb(151, 238, 211), 0.50f, 0.39f,
                MotionStyle.FLOW, CompositionStyle.SPLICE, 1.03f
        ),
        INTERFERENCE_FIELD(
                "Interference Field", R.drawable.scene_interference_field, 0.54f, 0.72f,
                ".:;~≈∿λψ01()<>+@", 1.18f, rgb(239, 176, 242), 0.50f, 0.35f,
                MotionStyle.WAVE, CompositionStyle.FIELD, 1.12f
        ),
        CRYO_VAULT(
                "Cryo Vault", R.drawable.scene_cryo_vault, 0.56f, 0.58f,
                ".:;|[]{}HEXICE01+*", 1.20f, rgb(182, 232, 255), 0.50f, 0.41f,
                MotionStyle.CIRCUIT, CompositionStyle.ARCHITECTURE, 0.96f
        ),
        DYSON_RELAY(
                "Dyson Relay", R.drawable.scene_dyson_relay, 0.55f, 0.66f,
                ".:;O0()[]{}<>01+*#@", 1.23f, rgb(255, 214, 140), 0.50f, 0.36f,
                MotionStyle.ORBITAL, CompositionStyle.ORBITAL_BAND, 1.14f
        ),
        SPECTRAL_OBSERVATORY(
                "Spectral Observatory", R.drawable.scene_spectral_observatory, 0.55f, 0.72f,
                ".:;~≈∿RFHz01[]<>/\\", 1.21f, rgb(145, 225, 255), 0.50f, 0.39f,
                MotionStyle.WAVE, CompositionStyle.FIELD, 1.08f
        ),
        RECURSIVE_MONOLITH(
                "Recursive Monolith", R.drawable.scene_recursive_monolith, 0.54f, 0.58f,
                ".:;|+-=[]{}<>01∞", 1.23f, rgb(221, 205, 255), 0.50f, 0.38f,
                MotionStyle.FOLD, CompositionStyle.ARCHITECTURE, 1.10f
        );

        public final String label;
        public final int maskResource;
        /** Vertical semantic anchor, expressed as a fraction of the canvas height. */
        public final float semanticY;
        /** Maximum language width as a fraction of the canvas width. */
        public final float semanticWidth;
        public final String textureGlyphs;
        public final float exposure;
        public final int atmosphereColor;
        public final float atmosphereX;
        public final float atmosphereY;
        public final MotionStyle motionStyle;
        public final CompositionStyle compositionStyle;
        public final float motionStrength;

        Theme(
                String label,
                int maskResource,
                float semanticY,
                float semanticWidth,
                String textureGlyphs,
                float exposure,
                int atmosphereColor,
                float atmosphereX,
                float atmosphereY,
                MotionStyle motionStyle,
                CompositionStyle compositionStyle,
                float motionStrength
        ) {
            this.label = label;
            this.maskResource = maskResource;
            this.semanticY = semanticY;
            this.semanticWidth = semanticWidth;
            this.textureGlyphs = textureGlyphs;
            this.exposure = exposure;
            this.atmosphereColor = atmosphereColor;
            this.atmosphereX = atmosphereX;
            this.atmosphereY = atmosphereY;
            this.motionStyle = motionStyle;
            this.compositionStyle = compositionStyle;
            this.motionStrength = motionStrength;
        }

        public Theme next() {
            Theme[] values = values();
            return values[(ordinal() + 1) % values.length];
        }
    }

    public static final class Event {
        public final String id;
        public final String eyebrow;
        public final String title;
        public final String summary;
        public final String action;
        public final String resultTitle;
        public final String resultSummary;
        public final String glyphs;
        public final int accent;

        Event(
                String id,
                String eyebrow,
                String title,
                String summary,
                String action,
                String resultTitle,
                String resultSummary,
                String glyphs,
                int accent
        ) {
            this.id = id;
            this.eyebrow = eyebrow;
            this.title = title;
            this.summary = summary;
            this.action = action;
            this.resultTitle = resultTitle;
            this.resultSummary = resultSummary;
            this.glyphs = glyphs;
            this.accent = accent;
        }
    }

    public static final List<Event> EVENTS = Arrays.asList(
            new Event(
                    "mail", "IMPORTANT MAIL · 2 MIN AGO", "DESIGN REVIEW MOVED",
                    "Maya moved tomorrow’s review to 09:30. Current travel time leaves a ten-minute conflict.",
                    "HOLD TO CHECK CALENDAR", "DRAFT READY",
                    "Thursday after 14:00 is clear. A concise reply is prepared for review.",
                    "@<>[]/\\MAYA0930", rgb(159, 221, 238)
            ),
            new Event(
                    "calendar", "CALENDAR · CONFLICT", "FRIDAY COLLISION",
                    "Client review begins at 10:00. Your train arrives at 10:08 and the next free slot is 11:30.",
                    "HOLD TO FIND A SLOT", "OPTION FOUND",
                    "11:30 keeps every attendee and removes the travel risk. Ready to propose.",
                    ":|+−09101130", rgb(213, 204, 166)
            ),
            new Event(
                    "github", "GITHUB · PRODUCTION", "DEPLOYMENT RECOVERED",
                    "The image built successfully. Migration failed after 42 seconds and rollback completed without downtime.",
                    "HOLD TO SHOW THE CAUSE", "CAUSE ISOLATED",
                    "A missing index exceeded the migration window. A safe remediation plan is staged.",
                    "{}[]/#01FAIL42", rgb(186, 178, 238)
            ),
            new Event(
                    "security", "SECURITY · PRIVATE EDGE", "TLS ROTATION DUE",
                    "The edge certificate expires in 36 hours. Two private services still pin the previous chain.",
                    "HOLD TO STAGE ROTATION", "ROTATION PLAN READY",
                    "The new chain validates everywhere. Two pinned services are isolated for a controlled restart.",
                    "X509TLS256[]{}CERT", rgb(246, 194, 119)
            ),
            new Event(
                    "network", "NETWORK · HOME LAB", "PACKET LOSS CLEARED",
                    "A WireGuard route flap caused 3.8 percent loss for ninety seconds. Traffic is stable again.",
                    "HOLD TO TRACE THE PATH", "ROUTE IDENTIFIED",
                    "An overlapping failover rule created the flap. A deterministic route order is ready.",
                    "TCPIPWG0138:<>/\\", rgb(166, 240, 193)
            ),
            new Event(
                    "model", "MODEL LAB · RUN 18420", "SIMULATION CONVERGED",
                    "The run reached target loss after 18,420 steps. One checkpoint dominates the evaluation set.",
                    "HOLD TO INSPECT THE RUN", "CHECKPOINT SELECTED",
                    "Step 17,960 has the best stability margin and no regression on the private test slice.",
                    "λψ∇LOSS18420<>01", rgb(196, 178, 255)
            )
    );

    public static Event eventAt(int index) {
        int size = EVENTS.size();
        int normalized = ((index % size) + size) % size;
        return EVENTS.get(normalized);
    }
}

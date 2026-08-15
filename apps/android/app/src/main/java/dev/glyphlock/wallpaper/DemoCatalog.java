package dev.glyphlock.wallpaper;

import android.graphics.Color;

import java.util.Arrays;
import java.util.List;

/** Local-only fixtures used by Prototype 0. No network or agent is involved. */
public final class DemoCatalog {
    private DemoCatalog() {}

    public enum MotionStyle {
        FLOW,
        ORBITAL,
        CIRCUIT,
        RADIAL,
        BLOOM
    }

    public enum Theme {
        SENTINEL(
                "Sentinel", R.drawable.scene_sentinel, 0.55f,
                "[]<>/\\|01", 1.00f, Color.rgb(206, 229, 240), 0.50f, 0.27f,
                MotionStyle.FLOW, 0.72f
        ),
        MOTH(
                "Moth", R.drawable.scene_moth, 0.57f,
                "(){}<>/\\:;", 1.02f, Color.rgb(230, 219, 188), 0.50f, 0.38f,
                MotionStyle.FLOW, 0.84f
        ),
        ORBIT(
                "Orbit", R.drawable.scene_orbit, 0.60f,
                "0O()[]:.;01", 1.05f, Color.rgb(208, 199, 244), 0.50f, 0.34f,
                MotionStyle.ORBITAL, 0.82f
        ),
        NEURAL_HALO(
                "Neural Halo", R.drawable.scene_neural_halo, 0.58f,
                ".:;~λψ∇01+<>@", 1.20f, Color.rgb(137, 235, 221), 0.50f, 0.35f,
                MotionStyle.RADIAL, 1.05f
        ),
        CIPHER_CATHEDRAL(
                "Cipher Cathedral", R.drawable.scene_cipher_cathedral, 0.56f,
                ".:;|[]{}0x#AF16+-", 1.22f, Color.rgb(246, 194, 119), 0.50f, 0.39f,
                MotionStyle.CIRCUIT, 0.92f
        ),
        QUANTUM_LATTICE(
                "Quantum Lattice", R.drawable.scene_quantum_lattice, 0.61f,
                ".:~λψ∂∇∞01()<>@", 1.19f, Color.rgb(196, 178, 255), 0.50f, 0.38f,
                MotionStyle.ORBITAL, 1.08f
        ),
        FUSION_CORE(
                "Fusion Core", R.drawable.scene_fusion_core, 0.61f,
                ".:;=+⊙○()[]|01#@", 1.19f, Color.rgb(126, 221, 250), 0.50f, 0.39f,
                MotionStyle.ORBITAL, 1.18f
        ),
        PACKET_BLOOM(
                "Packet Bloom", R.drawable.scene_packet_bloom, 0.59f,
                ".:;<>[]{}:/\\01TCPIP+@", 1.21f, Color.rgb(166, 240, 193), 0.50f, 0.41f,
                MotionStyle.BLOOM, 1.05f
        ),
        EVENT_HORIZON(
                "Event Horizon", R.drawable.scene_event_horizon, 0.59f,
                ".:;~O0()[]<>∞λ01#@", 1.24f, Color.rgb(255, 173, 112), 0.50f, 0.38f,
                MotionStyle.ORBITAL, 1.24f
        ),
        TESSERACT_ENGINE(
                "Tesseract Engine", R.drawable.scene_tesseract_engine, 0.57f,
                ".:;|+-=[]{}<>01XYZW", 1.22f, Color.rgb(164, 211, 255), 0.50f, 0.36f,
                MotionStyle.CIRCUIT, 1.08f
        ),
        HELIX_ARRAY(
                "Helix Array", R.drawable.scene_helix_array, 0.61f,
                ".:;~ATCGλψ01/\\()[]", 1.19f, Color.rgb(151, 238, 211), 0.50f, 0.39f,
                MotionStyle.FLOW, 1.03f
        ),
        INTERFERENCE_FIELD(
                "Interference Field", R.drawable.scene_interference_field, 0.58f,
                ".:;~≈∿λψ01()<>+@", 1.18f, Color.rgb(239, 176, 242), 0.50f, 0.35f,
                MotionStyle.RADIAL, 1.12f
        ),
        CRYO_VAULT(
                "Cryo Vault", R.drawable.scene_cryo_vault, 0.60f,
                ".:;|[]{}HEXICE01+*", 1.20f, Color.rgb(182, 232, 255), 0.50f, 0.41f,
                MotionStyle.CIRCUIT, 0.96f
        ),
        DYSON_RELAY(
                "Dyson Relay", R.drawable.scene_dyson_relay, 0.59f,
                ".:;O0()[]{}<>01+*#@", 1.23f, Color.rgb(255, 214, 140), 0.50f, 0.36f,
                MotionStyle.ORBITAL, 1.14f
        );

        public final String label;
        public final int maskResource;
        /** Center of the readable event cavity as a fraction of canvas height. */
        public final float cavityFraction;
        public final String textureGlyphs;
        public final float exposure;
        public final int atmosphereColor;
        public final float atmosphereX;
        public final float atmosphereY;
        public final MotionStyle motionStyle;
        public final float motionStrength;

        Theme(
                String label,
                int maskResource,
                float cavityFraction,
                String textureGlyphs,
                float exposure,
                int atmosphereColor,
                float atmosphereX,
                float atmosphereY,
                MotionStyle motionStyle,
                float motionStrength
        ) {
            this.label = label;
            this.maskResource = maskResource;
            this.cavityFraction = cavityFraction;
            this.textureGlyphs = textureGlyphs;
            this.exposure = exposure;
            this.atmosphereColor = atmosphereColor;
            this.atmosphereX = atmosphereX;
            this.atmosphereY = atmosphereY;
            this.motionStyle = motionStyle;
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
                    "@<>[]/\\MAYA0930", Color.rgb(159, 221, 238)
            ),
            new Event(
                    "calendar", "CALENDAR · CONFLICT", "FRIDAY COLLISION",
                    "Client review begins at 10:00. Your train arrives at 10:08 and the next free slot is 11:30.",
                    "HOLD TO FIND A SLOT", "OPTION FOUND",
                    "11:30 keeps every attendee and removes the travel risk. Ready to propose.",
                    ":|+−09101130", Color.rgb(213, 204, 166)
            ),
            new Event(
                    "github", "GITHUB · PRODUCTION", "DEPLOYMENT RECOVERED",
                    "The image built successfully. Migration failed after 42 seconds and rollback completed without downtime.",
                    "HOLD TO SHOW THE CAUSE", "CAUSE ISOLATED",
                    "A missing index exceeded the migration window. A safe remediation plan is staged.",
                    "{}[]/#01FAIL42", Color.rgb(186, 178, 238)
            ),
            new Event(
                    "security", "SECURITY · PRIVATE EDGE", "TLS ROTATION DUE",
                    "The edge certificate expires in 36 hours. Two private services still pin the previous chain.",
                    "HOLD TO STAGE ROTATION", "ROTATION PLAN READY",
                    "The new chain validates everywhere. Two pinned services are isolated for a controlled restart.",
                    "X509TLS256[]{}CERT", Color.rgb(246, 194, 119)
            ),
            new Event(
                    "network", "NETWORK · HOME LAB", "PACKET LOSS CLEARED",
                    "A WireGuard route flap caused 3.8 percent loss for ninety seconds. Traffic is stable again.",
                    "HOLD TO TRACE THE PATH", "ROUTE IDENTIFIED",
                    "An overlapping failover rule created the flap. A deterministic route order is ready.",
                    "TCPIPWG0138:<>/\\", Color.rgb(166, 240, 193)
            ),
            new Event(
                    "model", "MODEL LAB · RUN 18420", "SIMULATION CONVERGED",
                    "The run reached target loss after 18,420 steps. One checkpoint dominates the evaluation set.",
                    "HOLD TO INSPECT THE RUN", "CHECKPOINT SELECTED",
                    "Step 17,960 has the best stability margin and no regression on the private test slice.",
                    "λψ∇LOSS18420<>01", Color.rgb(196, 178, 255)
            )
    );

    public static Event eventAt(int index) {
        int size = EVENTS.size();
        int normalized = ((index % size) + size) % size;
        return EVENTS.get(normalized);
    }
}

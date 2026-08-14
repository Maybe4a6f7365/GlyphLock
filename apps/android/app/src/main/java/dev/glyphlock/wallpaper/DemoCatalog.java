package dev.glyphlock.wallpaper;

import android.graphics.Color;

import java.util.Arrays;
import java.util.List;

/** Local-only fixtures used by Prototype 0. No network or agent is involved. */
public final class DemoCatalog {
    private DemoCatalog() {}

    public enum Theme {
        SENTINEL("Sentinel", R.drawable.scene_sentinel, 0.55f),
        MOTH("Moth", R.drawable.scene_moth, 0.57f),
        ORBIT("Orbit", R.drawable.scene_orbit, 0.60f);

        public final String label;
        public final int maskResource;
        /** Center of the readable event cavity as a fraction of canvas height. */
        public final float cavityFraction;

        Theme(String label, int maskResource, float cavityFraction) {
            this.label = label;
            this.maskResource = maskResource;
            this.cavityFraction = cavityFraction;
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
                    "mail",
                    "IMPORTANT MAIL · 2 MIN AGO",
                    "DESIGN REVIEW MOVED",
                    "Maya moved tomorrow’s review to 09:30. Current travel time leaves a ten-minute conflict.",
                    "HOLD TO CHECK CALENDAR",
                    "DRAFT READY",
                    "Thursday after 14:00 is clear. A concise reply is prepared for review.",
                    "@<>[]/\\MAYA0930",
                    Color.rgb(159, 221, 238)
            ),
            new Event(
                    "calendar",
                    "CALENDAR · CONFLICT",
                    "FRIDAY COLLISION",
                    "Client review begins at 10:00. Your train arrives at 10:08 and the next free slot is 11:30.",
                    "HOLD TO FIND A SLOT",
                    "OPTION FOUND",
                    "11:30 keeps every attendee and removes the travel risk. Ready to propose.",
                    ":|+−09101130",
                    Color.rgb(213, 204, 166)
            ),
            new Event(
                    "github",
                    "GITHUB · PRODUCTION",
                    "DEPLOYMENT RECOVERED",
                    "The image built successfully. Migration failed after 42 seconds and rollback completed without downtime.",
                    "HOLD TO SHOW THE CAUSE",
                    "CAUSE ISOLATED",
                    "A missing index exceeded the migration window. A safe remediation plan is staged.",
                    "{}[]/#01FAIL42",
                    Color.rgb(186, 178, 238)
            )
    );

    public static Event eventAt(int index) {
        int size = EVENTS.size();
        int normalized = ((index % size) + size) % size;
        return EVENTS.get(normalized);
    }
}

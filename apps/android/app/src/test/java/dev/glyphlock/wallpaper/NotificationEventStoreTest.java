package dev.glyphlock.wallpaper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class NotificationEventStoreTest {
    @Test
    public void normalizationCollapsesWhitespaceAndBoundsPrivateContent() {
        assertEquals(
                "Build finished successfully",
                NotificationEventStore.normalize("  Build\nfinished   successfully  ", 80, "fallback")
        );
        String bounded = NotificationEventStore.normalize(
                "A notification body that is deliberately longer than the target",
                24,
                "fallback"
        );
        assertTrue(bounded.length() <= 24);
        assertTrue(bounded.endsWith("…"));
    }

    @Test
    public void notificationTitleIsBoundedForGlyphOutlineComposition() {
        DemoCatalog.Event event = NotificationEventStore.toEvent(
                "dev.example.mail",
                "Example Mail",
                "A very long notification title that cannot fit on a narrow wallpaper",
                "The body remains available for the smaller outline rows.",
                0xFF9FDDEE,
                42L
        );
        assertTrue(event.title.length() <= 32);
        assertEquals("EXAMPLE MAIL · PHONE NOTIFICATION", event.eyebrow);
    }

    @Test
    public void identicalNotificationCopyAtANewPostTimeIsANewEvent() {
        String first = NotificationEventStore.eventFingerprint(
                "dev.example.chat",
                "Build complete",
                "The same message can be sent twice.",
                100L
        );
        String second = NotificationEventStore.eventFingerprint(
                "dev.example.chat",
                "Build complete",
                "The same message can be sent twice.",
                101L
        );
        assertNotEquals(first, second);
    }
}

package dev.glyphlock.wallpaper;

import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.service.notification.NotificationListenerService;

import java.util.Locale;

/** Local hand-off between Android's notification listener and the wallpaper process. */
final class NotificationEventStore {
    static final String ACTION_EVENT_CHANGED =
            "dev.glyphlock.wallpaper.action.NOTIFICATION_EVENT_CHANGED";
    static final String ACTION_LISTENER_STATE_CHANGED =
            "dev.glyphlock.wallpaper.action.NOTIFICATION_LISTENER_STATE_CHANGED";
    static final String ACTION_SYNC_LATEST =
            "dev.glyphlock.wallpaper.action.SYNC_LATEST_NOTIFICATION";

    private static final String FILE = "glyphlock_notification_events";
    private static final String KEY_REVISION = "revision";
    private static final String KEY_FINGERPRINT = "fingerprint";
    private static final String KEY_PACKAGE = "package";
    private static final String KEY_APP = "app";
    private static final String KEY_TITLE = "title";
    private static final String KEY_BODY = "body";
    private static final String KEY_ACCENT = "accent";
    private static final String KEY_POSTED_AT = "posted_at";
    private static final String KEY_LISTENER_CONNECTED = "listener_connected";

    private static final int[] ACCENTS = {
            0xFF9FDDEE,
            0xFFD5CCA6,
            0xFFBAB2EE,
            0xFFF6C277,
            0xFFA6F0C1,
            0xFFC4B2FF,
    };

    private NotificationEventStore() {}

    static boolean record(
            Context context,
            String packageName,
            String appLabel,
            CharSequence title,
            CharSequence body,
            int notificationColor,
            long postedAt,
            boolean secret
    ) {
        String safePackage = normalize(packageName, 120, "unknown");
        String safeApp = normalize(appLabel, 36, "Android");
        String safeTitle = secret
                ? safeApp
                : normalize(title, 56, safeApp);
        String safeBody = secret
                ? "Content hidden by the notification's privacy setting."
                : normalize(body, 180, "New notification from " + safeApp + ".");
        int accent = readableAccent(notificationColor, safePackage);
        // The same sender and copy may legitimately be posted more than once. The post time keeps
        // a reconnect idempotent while still treating a later identical notification as new.
        String fingerprint = eventFingerprint(safePackage, safeTitle, safeBody, postedAt);

        SharedPreferences preferences = prefs(context);
        if (fingerprint.equals(preferences.getString(KEY_FINGERPRINT, ""))) return false;
        long revision = preferences.getLong(KEY_REVISION, 0L) + 1L;
        return preferences.edit()
                .putLong(KEY_REVISION, revision)
                .putString(KEY_FINGERPRINT, fingerprint)
                .putString(KEY_PACKAGE, safePackage)
                .putString(KEY_APP, safeApp)
                .putString(KEY_TITLE, safeTitle)
                .putString(KEY_BODY, safeBody)
                .putInt(KEY_ACCENT, accent)
                .putLong(KEY_POSTED_AT, postedAt)
                .commit();
    }

    static DemoCatalog.Event latestEvent(Context context) {
        SharedPreferences preferences = prefs(context);
        if (preferences.getLong(KEY_REVISION, 0L) <= 0L) return awaitingEvent();
        return toEvent(
                preferences.getString(KEY_PACKAGE, "unknown"),
                preferences.getString(KEY_APP, "Android"),
                preferences.getString(KEY_TITLE, "New notification"),
                preferences.getString(KEY_BODY, "A notification arrived."),
                preferences.getInt(KEY_ACCENT, ACCENTS[0]),
                preferences.getLong(KEY_POSTED_AT, 0L)
        );
    }

    static long revision(Context context) {
        return prefs(context).getLong(KEY_REVISION, 0L);
    }

    static boolean isListenerEnabled(Context context) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) return false;
        return manager.isNotificationListenerAccessGranted(listenerComponent(context));
    }

    static boolean isListenerConnected(Context context) {
        return isListenerEnabled(context)
                && prefs(context).getBoolean(KEY_LISTENER_CONNECTED, false);
    }

    static void setListenerConnected(Context context, boolean connected) {
        prefs(context).edit().putBoolean(KEY_LISTENER_CONNECTED, connected).apply();
    }

    static boolean requestListenerRebind(Context context) {
        if (!isListenerEnabled(context)) return false;
        try {
            NotificationListenerService.requestRebind(listenerComponent(context));
            return true;
        } catch (RuntimeException unavailable) {
            return false;
        }
    }

    static boolean restartListenerBinding(Context context) {
        if (!isListenerEnabled(context)) return false;
        ComponentName component = listenerComponent(context);
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                // requestRebind is defined for a listener that was first unbound. HyperOS can
                // retain the access grant but lose the live binding after an APK update, so make
                // that lifecycle explicit before asking Android to reconnect it.
                NotificationListenerService.requestUnbind(component);
                Context appContext = context.getApplicationContext();
                new Handler(Looper.getMainLooper()).postDelayed(
                        () -> requestListenerRebind(appContext),
                        180L
                );
            } else {
                NotificationListenerService.requestRebind(component);
            }
            return true;
        } catch (RuntimeException unavailable) {
            return false;
        }
    }

    static boolean requestLatestNotification(Context context) {
        if (!isListenerEnabled(context)) return false;
        setListenerConnected(context, false);
        Intent sync = new Intent(ACTION_SYNC_LATEST);
        sync.setPackage(context.getPackageName());
        context.sendBroadcast(sync);
        Context appContext = context.getApplicationContext();
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // A live service acknowledges the sync broadcast by restoring this bit. If no
            // acknowledgement arrives, recover the OEM's approved-but-unbound state.
            if (!isListenerConnected(appContext)) restartListenerBinding(appContext);
        }, 320L);
        return true;
    }

    static ComponentName listenerComponent(Context context) {
        return new ComponentName(context, GlyphNotificationListenerService.class);
    }

    static DemoCatalog.Event toEvent(
            String packageName,
            String appLabel,
            String title,
            String body,
            int accent,
            long postedAt
    ) {
        String app = normalize(appLabel, 36, "Android").toUpperCase(Locale.ROOT);
        String eventTitle = normalize(title, 32, app).toUpperCase(Locale.ROOT);
        String summary = normalize(body, 180, "New notification from " + app + ".");
        String id = "notification:" + normalize(packageName, 120, "unknown") + ":" + postedAt;
        String glyphs = notificationGlyphs(app + eventTitle);
        return new DemoCatalog.Event(
                id,
                app + " · PHONE NOTIFICATION",
                eventTitle,
                summary,
                "LIVE NOTIFICATION",
                eventTitle,
                summary,
                glyphs,
                accent
        );
    }

    private static DemoCatalog.Event awaitingEvent() {
        return new DemoCatalog.Event(
                "notification:waiting",
                "NOTIFICATION MODE · READY",
                "WAITING FOR AN EVENT",
                "The next phone notification will become the wallpaper event.",
                "LOCAL LISTENER",
                "WAITING FOR AN EVENT",
                "Notification access is active; no event has arrived yet.",
                "<>[]{}01NOTIFY",
                ACCENTS[0]
        );
    }

    static String normalize(CharSequence value, int maxLength, String fallback) {
        String clean = value == null ? "" : value.toString().replaceAll("\\s+", " ").trim();
        if (clean.isEmpty()) clean = fallback;
        if (clean.length() <= maxLength) return clean;
        int end = maxLength - 1;
        if (end > 0 && Character.isHighSurrogate(clean.charAt(end - 1))) end--;
        return clean.substring(0, Math.max(1, end)).trim() + "…";
    }

    static String eventFingerprint(String packageName, String title, String body, long postedAt) {
        return packageName + '\n' + title + '\n' + body + '\n' + postedAt;
    }

    private static String notificationGlyphs(String value) {
        String upper = value.toUpperCase(Locale.ROOT);
        StringBuilder result = new StringBuilder("<>[]{}01@");
        for (int i = 0; i < upper.length() && result.length() < 28; i++) {
            char character = upper.charAt(i);
            if ((character >= 'A' && character <= 'Z') || Character.isDigit(character)) {
                result.append(character);
            }
        }
        return result.toString();
    }

    private static int readableAccent(int notificationColor, String packageName) {
        int rgb = notificationColor & 0x00FFFFFF;
        if (notificationColor == 0 || rgb == 0) {
            return ACCENTS[Math.floorMod(packageName.hashCode(), ACCENTS.length)];
        }
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;
        // Lift dark application colors so text remains legible on the black wallpaper.
        red = Math.round(red * 0.68f + 255f * 0.32f);
        green = Math.round(green * 0.68f + 255f * 0.32f);
        blue = Math.round(blue * 0.68f + 255f * 0.32f);
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }
}

package dev.glyphlock.wallpaper;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

/** Converts user-approved Android notifications into local wallpaper events. */
public final class GlyphNotificationListenerService extends NotificationListenerService {
    private static final String TAG = "GlyphNotification";
    private boolean connected;
    private boolean syncReceiverRegistered;
    private final BroadcastReceiver syncReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!NotificationEventStore.ACTION_SYNC_LATEST.equals(intent.getAction())) return;
            if (!connected || !DemoPreferences.notificationEvents(context)) return;
            NotificationEventStore.setListenerConnected(context, true);
            broadcastListenerState();
            syncNewestActive();
        }
    };

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    public void onCreate() {
        super.onCreate();
        IntentFilter filter = new IntentFilter(NotificationEventStore.ACTION_SYNC_LATEST);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(syncReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(syncReceiver, filter);
        }
        syncReceiverRegistered = true;
    }

    @Override
    public void onDestroy() {
        connected = false;
        NotificationEventStore.setListenerConnected(this, false);
        broadcastListenerState();
        if (syncReceiverRegistered) {
            try {
                unregisterReceiver(syncReceiver);
            } catch (IllegalArgumentException ignored) {
                // The framework may already have released the service receiver.
            }
            syncReceiverRegistered = false;
        }
        super.onDestroy();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification status) {
        if (!connected) {
            connected = true;
            NotificationEventStore.setListenerConnected(this, true);
            broadcastListenerState();
        }
        publish(status);
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        connected = true;
        NotificationEventStore.setListenerConnected(this, true);
        broadcastListenerState();
        Log.i(TAG, "Notification listener connected");

        syncNewestActive();
    }

    private void syncNewestActive() {
        StatusBarNotification newest = null;
        StatusBarNotification[] active;
        try {
            active = getActiveNotifications();
        } catch (RuntimeException unavailable) {
            connected = false;
            NotificationEventStore.setListenerConnected(this, false);
            broadcastListenerState();
            Log.w(TAG, "Unable to read active notifications", unavailable);
            return;
        }
        if (active == null) return;
        for (StatusBarNotification candidate : active) {
            if (!isUseful(candidate)) continue;
            if (newest == null || candidate.getPostTime() > newest.getPostTime()) newest = candidate;
        }
        if (newest != null) publish(newest);
    }

    @Override
    public void onListenerDisconnected() {
        super.onListenerDisconnected();
        connected = false;
        NotificationEventStore.setListenerConnected(this, false);
        broadcastListenerState();
        Log.w(TAG, "Notification listener disconnected");
        if (DemoPreferences.notificationEvents(this)
                && NotificationEventStore.isListenerEnabled(this)) {
            ComponentName component = NotificationEventStore.listenerComponent(this);
            requestRebind(component);
        }
    }

    private void publish(StatusBarNotification status) {
        if (!DemoPreferences.notificationEvents(this)) return;
        if (!isUseful(status)) return;
        Notification notification = status.getNotification();
        CharSequence title = notificationTitle(notification);
        CharSequence body = notificationBody(notification);
        boolean secret = notification.visibility == Notification.VISIBILITY_SECRET;
        boolean changed = NotificationEventStore.record(
                getApplicationContext(),
                status.getPackageName(),
                applicationLabel(status.getPackageName()),
                title,
                body,
                notification.color,
                status.getPostTime(),
                secret
        );
        if (!changed) return;
        Intent eventChanged = new Intent(NotificationEventStore.ACTION_EVENT_CHANGED);
        eventChanged.setPackage(getPackageName());
        sendBroadcast(eventChanged);
        Log.i(TAG, "Published notification event from " + status.getPackageName());
    }

    private boolean isUseful(StatusBarNotification status) {
        if (status == null || getPackageName().equals(status.getPackageName())) return false;
        Notification notification = status.getNotification();
        if (notification == null) return false;
        // Group summaries duplicate their child event. Ongoing notifications are retained because
        // calls, navigation, timers, and transfers are valid phone events for this wallpaper.
        return (notification.flags & Notification.FLAG_GROUP_SUMMARY) == 0;
    }

    private CharSequence notificationTitle(Notification notification) {
        Bundle extras = notification.extras;
        if (extras == null) return null;
        CharSequence title = nonBlank(extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE));
        if (title == null) title = nonBlank(extras.getCharSequence(Notification.EXTRA_TITLE_BIG));
        if (title == null) title = nonBlank(extras.getCharSequence(Notification.EXTRA_TITLE));
        return title;
    }

    private CharSequence notificationBody(Notification notification) {
        Bundle extras = notification.extras;
        if (extras == null) return null;
        CharSequence body = nonBlank(extras.getCharSequence(Notification.EXTRA_BIG_TEXT));
        if (body == null) body = nonBlank(extras.getCharSequence(Notification.EXTRA_TEXT));
        if (body == null) {
            CharSequence[] lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
            if (lines != null) {
                for (int index = lines.length - 1; index >= 0 && body == null; index--) {
                    body = nonBlank(lines[index]);
                }
            }
        }
        if (body == null) body = nonBlank(extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT));
        if (body == null) body = nonBlank(extras.getCharSequence(Notification.EXTRA_SUB_TEXT));
        return body;
    }

    private CharSequence nonBlank(CharSequence value) {
        return value == null || value.toString().trim().isEmpty() ? null : value;
    }

    private void broadcastListenerState() {
        Intent stateChanged = new Intent(NotificationEventStore.ACTION_LISTENER_STATE_CHANGED);
        stateChanged.setPackage(getPackageName());
        sendBroadcast(stateChanged);
    }

    private String applicationLabel(String packageName) {
        PackageManager manager = getPackageManager();
        try {
            ApplicationInfo info = manager.getApplicationInfo(packageName, 0);
            CharSequence label = manager.getApplicationLabel(info);
            return label == null ? packageName : label.toString();
        } catch (PackageManager.NameNotFoundException ignored) {
            return packageName;
        }
    }
}

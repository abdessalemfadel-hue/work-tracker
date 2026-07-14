package com.abdessalem.worktracker;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public final class TrackingService extends Service {
    public static final String ACTION_REFRESH = "com.abdessalem.worktracker.REFRESH";
    public static final String ACTION_BREAK = "com.abdessalem.worktracker.BREAK";
    public static final String ACTION_CLOCK_OUT = "com.abdessalem.worktracker.CLOCK_OUT";
    private static final String CHANNEL_ID = "active_shift";
    private static final int NOTIFICATION_ID = 71;

    private WorkDb db;

    @Override public void onCreate() {
        super.onCreate();
        db = new WorkDb(this);
        createChannel();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? ACTION_REFRESH : intent.getAction();
        if (ACTION_BREAK.equals(action)) toggleBreak();
        if (ACTION_CLOCK_OUT.equals(action)) clockOut();

        ActiveShift active = loadActive();
        if (active == null) {
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }
        startForeground(NOTIFICATION_ID, buildNotification(active));
        return START_STICKY;
    }

    private Notification buildNotification(ActiveShift shift) {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent openPi = PendingIntent.getActivity(this, 1, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent breakIntent = new Intent(this, TrackingService.class).setAction(ACTION_BREAK);
        PendingIntent breakPi = PendingIntent.getService(this, 2, breakIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent outIntent = new Intent(this, TrackingService.class).setAction(ACTION_CLOCK_OUT);
        PendingIntent outPi = PendingIntent.getService(this, 3, outIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        long base = SystemClock.elapsedRealtime() - (System.currentTimeMillis() - shift.clockIn);
        String state = shift.breakStart > 0 ? "On break" : "Working now";
        String breakLabel = shift.breakStart > 0 ? "Resume" : "Break";

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(state)
                .setContentText("Tap to open Work Tracker")
                .setContentIntent(openPi)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setUsesChronometer(shift.breakStart == 0)
                .setWhen(System.currentTimeMillis() - (SystemClock.elapsedRealtime() - base))
                .addAction(0, breakLabel, breakPi)
                .addAction(0, "Clock out", outPi)
                .build();
    }

    private void toggleBreak() {
        ActiveShift shift = loadActive();
        if (shift == null) return;
        long now = System.currentTimeMillis();
        ContentValues values = new ContentValues();
        if (shift.breakStart > 0) {
            int minutes = shift.breakMinutes + (int) ((now - shift.breakStart) / 60000L);
            values.putNull("break_start");
            values.put("break_minutes", minutes);
        } else {
            values.put("break_start", now);
        }
        db.getWritableDatabase().update("shifts", values, "id=?", new String[]{String.valueOf(shift.id)});
        sendBroadcast(new Intent(MainActivity.ACTION_DATA_CHANGED).setPackage(getPackageName()));
    }

    private void clockOut() {
        ActiveShift shift = loadActive();
        if (shift == null) return;
        long now = System.currentTimeMillis();
        int minutes = shift.breakMinutes;
        if (shift.breakStart > 0) minutes += (int) ((now - shift.breakStart) / 60000L);
        ContentValues values = new ContentValues();
        values.put("clock_out", now);
        values.putNull("break_start");
        values.put("break_minutes", minutes);
        db.getWritableDatabase().update("shifts", values, "id=?", new String[]{String.valueOf(shift.id)});
        sendBroadcast(new Intent(MainActivity.ACTION_DATA_CHANGED).setPackage(getPackageName()));
    }

    @Nullable private ActiveShift loadActive() {
        try (Cursor c = db.getReadableDatabase().rawQuery(
                "SELECT id,clock_in,break_start,break_minutes FROM shifts WHERE clock_out IS NULL ORDER BY id DESC LIMIT 1", null)) {
            if (!c.moveToFirst()) return null;
            return new ActiveShift(c.getLong(0), c.getLong(1), c.isNull(2) ? 0 : c.getLong(2), c.getInt(3));
        }
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Active shift",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Persistent timer and shift controls");
            channel.setShowBadge(false);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    @Override public void onDestroy() {
        if (db != null) db.close();
        super.onDestroy();
    }

    @Nullable @Override public IBinder onBind(Intent intent) { return null; }

    private static final class ActiveShift {
        final long id, clockIn, breakStart;
        final int breakMinutes;
        ActiveShift(long id, long clockIn, long breakStart, int breakMinutes) {
            this.id = id; this.clockIn = clockIn; this.breakStart = breakStart; this.breakMinutes = breakMinutes;
        }
    }
}

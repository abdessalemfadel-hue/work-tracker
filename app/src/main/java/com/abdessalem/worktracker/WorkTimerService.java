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
import android.os.Handler;
import android.os.IBinder;

import java.util.Locale;

public final class WorkTimerService extends Service {
    public static final String ACTION_SYNC = "com.abdessalem.worktracker.SYNC";
    public static final String ACTION_BREAK = "com.abdessalem.worktracker.BREAK";
    public static final String ACTION_CLOCK_OUT = "com.abdessalem.worktracker.CLOCK_OUT";
    public static final String ACTION_REFRESH = "com.abdessalem.worktracker.REFRESH";

    private static final String CHANNEL_ID = "active_shift";
    private static final int NOTIFICATION_ID = 88;
    private final Handler handler = new Handler();
    private WorkDb db;

    @Override public void onCreate() {
        super.onCreate();
        db = new WorkDb(this);
        createChannel();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? ACTION_SYNC : intent.getAction();
        if (ACTION_BREAK.equals(action)) toggleBreak();
        if (ACTION_CLOCK_OUT.equals(action)) clockOut();

        Shift shift = readActiveShift();
        if (shift == null) {
            stopForeground(true);
            stopSelf();
            sendBroadcast(new Intent(ACTION_REFRESH).setPackage(getPackageName()));
            return START_NOT_STICKY;
        }


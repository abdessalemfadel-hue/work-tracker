package com.abdessalem.worktracker;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public final class MainActivity extends Activity {
    public static final String ACTION_DATA_CHANGED = "com.abdessalem.worktracker.DATA_CHANGED";
    private static final int CREATE_CSV = 501;
    private static final int REQUEST_NOTIFICATIONS = 502;
    private static final int BG = 0xFF07111E, CARD = 0xFF101D2E, CARD_2 = 0xFF16263B;
    private static final int TEXT = 0xFFF5F8FC, MUTED = 0xFF9EABC0, GREEN = 0xFF4ED27A;
    private static final int ORANGE = 0xFFFFB02E, RED = 0xFFFF625F, BLUE = 0xFF63A9FF;

    private final Handler handler = new Handler();
    private WorkDb db;
    private FrameLayout content;
    private LinearLayout nav;
    private TextView timer, status, todaySummary;
    private Button clockButton, breakButton;
    private long activeId = -1, clockIn = 0, breakStart = 0;
    private int breakMinutes = 0;
    private int currentPage = 0;

    private final BroadcastReceiver dataReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) { refreshState(); showPage(currentPage); }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        db = new WorkDb(this);
        setContentView(buildShell());
        requestNotificationPermission();
        refreshState();
        showPage(0);
        handler.post(ticker);
    }

    @Override protected void onStart() {
        super.onStart();
        ContextCompat.registerReceiver(this, dataReceiver, new IntentFilter(ACTION_DATA_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    @Override protected void onStop() {
        unregisterReceiver(dataReceiver);
        super.onStop();
    }

    @Override protected void onDestroy() {
        handler.removeCallbacks(ticker);
        db.close();
        super.onDestroy();
    }

    private View buildShell() {
        LinearLayout root = column();
        root.setBackgroundColor(BG);

        content = new FrameLayout(this);
        root.addView(content, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        nav = row();
        nav.setPadding(dp(8), dp(8), dp(8), dp(10));
        nav.setBackgroundColor(0xFF0B1726);
        String[] labels = {"Today", "Stats", "History", "Settings"};
        for (int i = 0; i < labels.length; i++) {
            final int page = i;
            Button b = navButton(labels[i]);
            b.setOnClickListener(v -> showPage(page));
            nav.addView(b, new LinearLayout.LayoutParams(0, dp(54), 1));
        }
        root.addView(nav);
        return root;
    }

    private void showPage(int page) {
        currentPage = page;
        content.removeAllViews();
        if (page == 0) content.addView(dashboardPage());
        else if (page == 1) content.addView(statsPage());
        else if (page == 2) content.addView(historyPage());
        else content.addView(settingsPage());
        updateNav();
    }

    private View dashboardPage() {
        ScrollView scroll = scroll();
        LinearLayout root = pageRoot();
        scroll.addView(root);

        root.addView(header("Work Tracker", "Your private, verifiable work record"));

        LinearLayout hero = card(CARD, 24);
        status = text("Not working", 14, true, MUTED);
        timer = text("00:00:00", 48, true, TEXT);
        timer.setPadding(0, dp(16), 0, dp(8));
        hero.addView(status);
        hero.addView(timer);
        hero.addView(text(new SimpleDateFormat("EEEE, d MMMM", Locale.US).format(new Date()), 14, false, MUTED));
        root.addView(hero, cardParams(0));

        LinearLayout actions = row();
        clockButton = action(activeId < 0 ? "Clock in" : "Clock out", activeId < 0 ? GREEN : RED, BG);
        breakButton = action(breakStart > 0 ? "Resume" : "Break", ORANGE, BG);
        clockButton.setOnClickListener(v -> toggleClock());
        breakButton.setOnClickListener(v -> toggleBreak());
        breakButton.setEnabled(activeId >= 0);
        actions.addView(clockButton, weighted(0, 6));
        actions.addView(breakButton, weighted(6, 0));
        root.addView(actions);

        root.addView(section("Today"));
        todaySummary = text(todaySummaryText(), 17, true, TEXT);
        todaySummary.setPadding(dp(18), dp(18), dp(18), dp(18));
        todaySummary.setBackground(round(CARD, 18));
        root.addView(todaySummary);

        LinearLayout quick = row();
        Button note = action("Add note", CARD_2, GREEN);
        Button add = action("Add shift", CARD_2, BLUE);
        note.setOnClickListener(v -> addNote());
        add.setOnClickListener(v -> editShift(-1));
        quick.addView(note, weighted(0, 6));
        quick.addView(add, weighted(6, 0));
        root.addView(quick);

        root.addView(section("This week"));
        root.addView(statStrip(weekStart(), System.currentTimeMillis(), "Weekly total"));

        TextView privacy = text("Offline by design. No account, location, microphone or internet access.", 12, false, MUTED);
        privacy.setGravity(Gravity.CENTER);
        privacy.setPadding(0, dp(26), 0, dp(10));
        root.addView(privacy);
        updateTimer();
        return scroll;
    }

    private View statsPage() {
        ScrollView scroll = scroll();
        LinearLayout root = pageRoot();
        scroll.addView(root);
        root.addView(header("Analytics", "Daily, weekly and monthly evidence"));

        long now = System.currentTimeMillis();
        root.addView(metricCard("Today", totalNet(dayStart(now), now), GREEN, "Hours worked today"));
        root.addView(metricCard("This week", totalNet(weekStart(), now), BLUE, "Monday to now"));
        root.addView(metricCard("This month", totalNet(monthStart(), now), ORANGE, "Calendar month"));

        long monthHours = totalNet(monthStart(), now);
        int days = workedDays(monthStart(), now);
        long average = days == 0 ? 0 : monthHours / days;
        long overtime = Math.max(0, monthHours - days * 8L * 3600000L);

        root.addView(section("Monthly breakdown"));
        LinearLayout grid = row();
        grid.addView(smallMetric("Worked days", String.valueOf(days)), weighted(0, 6));
        grid.addView(smallMetric("Daily average", shortDuration(average)), weighted(6, 0));
        root.addView(grid);
        LinearLayout grid2 = row();
        grid2.addView(smallMetric("Over 8h/day", shortDuration(overtime)), weighted(0, 6));
        grid2.addView(smallMetric("Equivalent days", String.format(Locale.US, "%.1f", overtime / 28800000.0)), weighted(6, 0));
        root.addView(grid2);

        root.addView(section("Last 7 days"));
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.DAY_OF_MONTH, -6);
        for (int i = 0; i < 7; i++) {
            long start = cal.getTimeInMillis();
            cal.add(Calendar.DAY_OF_MONTH, 1);
            long end = cal.getTimeInMillis();
            root.addView(dayBar(start, totalNet(start, end)));
        }
        return scroll;
    }

    private View historyPage() {
        ScrollView scroll = scroll();
        LinearLayout root = pageRoot();
        scroll.addView(root);
        root.addView(header("History", "Review and correct your records"));

        LinearLayout tools = row();
        Button add = action("Add shift", GREEN, BG);
        Button export = action("Export CSV", CARD_2, GREEN);
        add.setOnClickListener(v -> editShift(-1));
        export.setOnClickListener(v -> exportCsv());
        tools.addView(add, weighted(0, 6));
        tools.addView(export, weighted(6, 0));
        root.addView(tools);

        try (Cursor c = db.getReadableDatabase().rawQuery(
                "SELECT id,clock_in,clock_out,break_minutes,note FROM shifts ORDER BY clock_in DESC LIMIT 100", null)) {
            if (!c.moveToFirst()) root.addView(emptyState("No shifts yet", "Clock in or add a past shift."));
            else do { root.addView(historyCard(c.getLong(0), c.getLong(1), c.isNull(2) ? 0 : c.getLong(2), c.getInt(3), c.getString(4))); } while (c.moveToNext());
        }
        return scroll;
    }

    private View settingsPage() {
        ScrollView scroll = scroll();
        LinearLayout root = pageRoot();
        scroll.addView(root);
        root.addView(header("Settings", "Control how the tracker behaves"));

        root.addView(infoCard("Persistent notification", "While clocked in, your timer remains visible in the notification shade and lock screen. Use Break, Resume or Clock out directly from it."));
        root.addView(infoCard("Samsung integration", "Samsung Now Brief is not open to ordinary third-party apps. The persistent live notification is the supported equivalent and works on Samsung lock screens."));
        root.addView(infoCard("Default schedule", "Analytics currently treats time above 8 hours per completed workday as overtime. A configurable schedule is planned next."));

        Button notify = action("Enable notifications", GREEN, BG);
        notify.setOnClickListener(v -> requestNotificationPermission());
        root.addView(notify, cardParams(18));

        Button export = action("Export all records", CARD_2, GREEN);
        export.setOnClickListener(v -> exportCsv());
        root.addView(export, cardParams(12));
        return scroll;
    }

    private View historyCard(long id, long start, long end, int breaks, String note) {
        LinearLayout card = card(CARD, 18);
        TextView date = text(new SimpleDateFormat("EEEE, d MMM yyyy", Locale.US).format(new Date(start)), 16, true, TEXT);
        card.addView(date);
        String endText = end == 0 ? "Active" : time(end);
        long net = end == 0 ? Math.max(0, System.currentTimeMillis() - start - breaks * 60000L) : Math.max(0, end - start - breaks * 60000L);
        card.addView(text(time(start) + " → " + endText + "  •  " + shortDuration(net) + " net", 14, false, MUTED));
        if (note != null && !note.trim().isEmpty()) {
            TextView n = text("“" + note.trim() + "”", 14, false, TEXT);
            n.setPadding(0, dp(10), 0, 0);
            card.addView(n);
        }
        LinearLayout actions = row();
        Button edit = miniAction("Edit", BLUE);
        Button delete = miniAction("Delete", RED);
        edit.setOnClickListener(v -> editShift(id));
        delete.setOnClickListener(v -> confirmDelete(id));
        actions.addView(edit, weightedSmall(0, 6));
        actions.addView(delete, weightedSmall(6, 0));
        card.addView(actions);
        return withMargin(card, 10);
    }

    private void toggleClock() {
        long now = System.currentTimeMillis();
        if (activeId < 0) {
            ContentValues values = new ContentValues();
            values.put("clock_in", now);
            db.getWritableDatabase().insertOrThrow("shifts", null, values);
            feedback(true, "Clocked in");
            startTrackingService();
        } else {
            if (breakStart > 0) breakMinutes += (int) ((now - breakStart) / 60000L);
            ContentValues values = new ContentValues();
            values.put("clock_out", now);
            values.putNull("break_start");
            values.put("break_minutes", breakMinutes);
            db.getWritableDatabase().update("shifts", values, "id=?", new String[]{String.valueOf(activeId)});
            feedback(false, "Shift complete");
            stopService(new Intent(this, TrackingService.class));
        }
        refreshState();
        showPage(0);
    }

    private void toggleBreak() {
        if (activeId < 0) { toast("Clock in first"); return; }
        long now = System.currentTimeMillis();
        ContentValues values = new ContentValues();
        if (breakStart == 0) {
            breakStart = now;
            values.put("break_start", now);
            feedback(false, "Break started");
        } else {
            breakMinutes += (int) ((now - breakStart) / 60000L);
            breakStart = 0;
            values.putNull("break_start");
            values.put("break_minutes", breakMinutes);
            feedback(true, "Back to work");
        }
        db.getWritableDatabase().update("shifts", values, "id=?", new String[]{String.valueOf(activeId)});
        startTrackingService();
        refreshState();
        showPage(0);
    }

    private void refreshState() {
        activeId = -1; clockIn = 0; breakStart = 0; breakMinutes = 0;
        try (Cursor c = db.getReadableDatabase().rawQuery(
                "SELECT id,clock_in,break_start,break_minutes FROM shifts WHERE clock_out IS NULL ORDER BY id DESC LIMIT 1", null)) {
            if (c.moveToFirst()) {
                activeId = c.getLong(0); clockIn = c.getLong(1);
                breakStart = c.isNull(2) ? 0 : c.getLong(2); breakMinutes = c.getInt(3);
            }
        }
        if (activeId >= 0) startTrackingService();
    }

    private void updateTimer() {
        if (timer == null || todaySummary == null) return;
        if (activeId < 0) {
            timer.setText("00:00:00");
            status.setText("Not working"); status.setTextColor(MUTED);
            todaySummary.setText(todaySummaryText());
            return;
        }
        long gross = System.currentTimeMillis() - clockIn;
        long currentBreak = breakStart > 0 ? System.currentTimeMillis() - breakStart : 0;
        long breaks = breakMinutes * 60000L + currentBreak;
        long net = Math.max(0, gross - breaks);
        timer.setText(duration(net));
        status.setText(breakStart > 0 ? "On break" : "Working now");
        status.setTextColor(breakStart > 0 ? ORANGE : GREEN);
        todaySummary.setText("Net " + shortDuration(net) + "  •  Break " + shortDuration(breaks) + "  •  Gross " + shortDuration(gross));
    }

    private String todaySummaryText() {
        long now = System.currentTimeMillis();
        long net = totalNet(dayStart(now), now);
        return "Net " + shortDuration(net) + "  •  " + (net >= 28800000L ? "Target reached" : "Target 08:00");
    }

    private void addNote() {
        if (activeId < 0) { toast("Clock in first"); return; }
        EditText input = new EditText(this);
        input.setHint("Manager request, event, late finish…");
        new AlertDialog.Builder(this).setTitle("Shift note").setView(input)
                .setPositiveButton("Save", (d, w) -> {
                    ContentValues values = new ContentValues(); values.put("note", input.getText().toString().trim());
                    db.getWritableDatabase().update("shifts", values, "id=?", new String[]{String.valueOf(activeId)});
                }).setNegativeButton("Cancel", null).show();
    }

    private void editShift(long id) {
        long start = System.currentTimeMillis();
        long end = System.currentTimeMillis();
        int breaks = 0;
        String note = "";
        if (id >= 0) {
            try (Cursor c = db.getReadableDatabase().rawQuery("SELECT clock_in,clock_out,break_minutes,note FROM shifts WHERE id=?", new String[]{String.valueOf(id)})) {
                if (c.moveToFirst()) { start = c.getLong(0); end = c.isNull(1) ? System.currentTimeMillis() : c.getLong(1); breaks = c.getInt(2); note = c.getString(3); }
            }
        }
        LinearLayout form = column(); form.setPadding(dp(20), 0, dp(20), 0);
        EditText date = field("Date yyyy-MM-dd", format(start, "yyyy-MM-dd"));
        EditText startTime = field("Start HH:mm", format(start, "HH:mm"));
        EditText endTime = field("End HH:mm", format(end, "HH:mm"));
        EditText breakInput = field("Break minutes", String.valueOf(breaks));
        EditText noteInput = field("Note", note == null ? "" : note);
        form.addView(date); form.addView(startTime); form.addView(endTime); form.addView(breakInput); form.addView(noteInput);
        final long shiftId = id;
        new AlertDialog.Builder(this).setTitle(id < 0 ? "Add shift" : "Edit shift").setView(form)
                .setPositiveButton("Save", (d, w) -> {
                    try {
                        long newStart = parseDateTime(date.getText().toString(), startTime.getText().toString());
                        long newEnd = parseDateTime(date.getText().toString(), endTime.getText().toString());
                        if (newEnd <= newStart) throw new IllegalArgumentException("End must be after start");
                        ContentValues values = new ContentValues();
                        values.put("clock_in", newStart); values.put("clock_out", newEnd);
                        values.putNull("break_start"); values.put("break_minutes", Integer.parseInt(breakInput.getText().toString().trim()));
                        values.put("note", noteInput.getText().toString().trim());
                        if (shiftId < 0) db.getWritableDatabase().insertOrThrow("shifts", null, values);
                        else db.getWritableDatabase().update("shifts", values, "id=?", new String[]{String.valueOf(shiftId)});
                        showPage(2);
                    } catch (Exception e) { toast("Could not save: " + e.getMessage()); }
                }).setNegativeButton("Cancel", null).show();
    }

    private void confirmDelete(long id) {
        new AlertDialog.Builder(this).setTitle("Delete this shift?").setMessage("This cannot be undone.")
                .setPositiveButton("Delete", (d, w) -> { db.getWritableDatabase().delete("shifts", "id=?", new String[]{String.valueOf(id)}); refreshState(); showPage(2); })
                .setNegativeButton("Cancel", null).show();
    }

    private void feedback(boolean positive, String label) {
        View ripple = new View(this);
        ripple.setBackground(round(positive ? GREEN : ORANGE, 1000));
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(dp(120), dp(120), Gravity.CENTER);
        addContentView(ripple, p);
        ripple.setScaleX(0.1f); ripple.setScaleY(0.1f); ripple.setAlpha(0.85f);
        ObjectAnimator sx = ObjectAnimator.ofFloat(ripple, View.SCALE_X, 0.1f, 12f);
        ObjectAnimator sy = ObjectAnimator.ofFloat(ripple, View.SCALE_Y, 0.1f, 12f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(ripple, View.ALPHA, 0.85f, 0f);
        AnimatorSet set = new AnimatorSet(); set.setDuration(650); set.playTogether(sx, sy, alpha);
        set.addListener(new AnimatorListenerAdapter() { @Override public void onAnimationEnd(Animator animation) { ((ViewGroup) ripple.getParent()).removeView(ripple); } });
        set.start();
        ripple.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= 26) vibrator.vibrate(VibrationEffect.createOneShot(70, VibrationEffect.DEFAULT_AMPLITUDE));
            else vibrator.vibrate(70);
        }
        toast(label);
    }

    private long totalNet(long from, long to) {
        long total = 0;
        try (Cursor c = db.getReadableDatabase().rawQuery(
                "SELECT clock_in,clock_out,break_minutes,break_start FROM shifts WHERE clock_in>=? AND clock_in<?", new String[]{String.valueOf(from), String.valueOf(to)})) {
            while (c.moveToNext()) {
                long start = c.getLong(0); long end = c.isNull(1) ? System.currentTimeMillis() : c.getLong(1);
                long breaks = c.getInt(2) * 60000L;
                if (!c.isNull(3)) breaks += System.currentTimeMillis() - c.getLong(3);
                total += Math.max(0, end - start - breaks);
            }
        }
        return total;
    }

    private int workedDays(long from, long to) {
        try (Cursor c = db.getReadableDatabase().rawQuery(
                "SELECT COUNT(DISTINCT date(clock_in/1000,'unixepoch','localtime')) FROM shifts WHERE clock_in>=? AND clock_in<?", new String[]{String.valueOf(from), String.valueOf(to)})) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    private View dayBar(long start, long value) {
        LinearLayout line = column(); line.setPadding(dp(16), dp(12), dp(16), dp(12));
        LinearLayout top = row();
        top.addView(text(format(start, "EEE d"), 14, true, TEXT), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        top.addView(text(shortDuration(value), 14, true, value > 28800000L ? ORANGE : GREEN));
        line.addView(top);
        View bar = new View(this); bar.setBackground(round(value > 28800000L ? ORANGE : GREEN, 6));
        int width = Math.max(dp(4), (int) (getResources().getDisplayMetrics().widthPixels * Math.min(1.0, value / 36000000.0)));
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(width, dp(7)); bp.setMargins(0, dp(8), 0, 0); line.addView(bar, bp);
        line.setBackground(round(CARD, 14));
        return withMargin(line, 8);
    }

    private View metricCard(String title, long value, int accent, String caption) {
        LinearLayout c = card(CARD, 20);
        c.addView(text(title, 15, true, accent));
        TextView v = text(shortDuration(value), 34, true, TEXT); v.setPadding(0, dp(8), 0, dp(4)); c.addView(v);
        c.addView(text(caption, 13, false, MUTED));
        return withMargin(c, 10);
    }

    private View smallMetric(String title, String value) {
        LinearLayout c = card(CARD, 16); c.addView(text(title, 12, false, MUTED)); c.addView(text(value, 22, true, TEXT)); return c;
    }

    private View statStrip(long from, long to, String title) {
        LinearLayout c = card(CARD, 18); c.addView(text(title, 13, false, MUTED)); c.addView(text(shortDuration(totalNet(from, to)), 28, true, TEXT)); return c;
    }

    private View infoCard(String title, String body) {
        LinearLayout c = card(CARD, 18); c.addView(text(title, 16, true, TEXT)); TextView b = text(body, 14, false, MUTED); b.setPadding(0, dp(8), 0, 0); c.addView(b); return withMargin(c, 10);
    }

    private View emptyState(String title, String body) {
        LinearLayout c = card(CARD, 20); c.setGravity(Gravity.CENTER); c.addView(text(title, 19, true, TEXT)); c.addView(text(body, 14, false, MUTED)); return withMargin(c, 18);
    }

    private View header(String title, String subtitle) {
        LinearLayout h = column(); h.addView(text(title, 29, true, TEXT)); TextView sub = text(subtitle, 14, false, MUTED); sub.setPadding(0, dp(4), 0, dp(20)); h.addView(sub); return h;
    }

    private void startTrackingService() {
        Intent service = new Intent(this, TrackingService.class).setAction(TrackingService.ACTION_REFRESH);
        ContextCompat.startForegroundService(this, service);
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATIONS);
    }

    private void exportCsv() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT); intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_TITLE, "work-hours-" + format(System.currentTimeMillis(), "yyyy-MM") + ".csv");
        startActivityForResult(intent, CREATE_CSV);
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == CREATE_CSV && result == RESULT_OK && data != null && data.getData() != null) writeCsv(data.getData());
    }

    private void writeCsv(Uri uri) {
        StringBuilder csv = new StringBuilder("Date,Clock in,Clock out,Break minutes,Net hours,Note\n");
        try (Cursor c = db.getReadableDatabase().rawQuery("SELECT clock_in,clock_out,break_minutes,note FROM shifts ORDER BY clock_in", null)) {
            while (c.moveToNext()) {
                long start = c.getLong(0); long end = c.isNull(1) ? 0 : c.getLong(1); int breaks = c.getInt(2);
                long net = end == 0 ? 0 : Math.max(0, end - start - breaks * 60000L);
                String note = c.getString(3) == null ? "" : c.getString(3).replace("\"", "\"\"");
                csv.append(format(start, "yyyy-MM-dd")).append(',').append(time(start)).append(',').append(end == 0 ? "" : time(end)).append(',')
                        .append(breaks).append(',').append(String.format(Locale.US, "%.2f", net / 3600000.0)).append(',').append('"').append(note).append('"').append('\n');
            }
        }
        try (OutputStream stream = getContentResolver().openOutputStream(uri)) {
            if (stream == null) throw new IllegalStateException("Cannot open file");
            stream.write(csv.toString().getBytes(StandardCharsets.UTF_8)); toast("CSV exported");
        } catch (Exception e) { toast("Export failed: " + e.getMessage()); }
    }

    private void updateNav() {
        for (int i = 0; i < nav.getChildCount(); i++) {
            Button b = (Button) nav.getChildAt(i); b.setTextColor(i == currentPage ? GREEN : MUTED); b.setBackgroundColor(0x00000000);
        }
    }

    private final Runnable ticker = new Runnable() {
        @Override public void run() { updateTimer(); handler.postDelayed(this, 1000); }
    };

    private ScrollView scroll() { ScrollView s = new ScrollView(this); s.setFillViewport(true); s.setBackgroundColor(BG); return s; }
    private LinearLayout pageRoot() { LinearLayout v = column(); v.setPadding(dp(20), dp(20), dp(20), dp(36)); return v; }
    private LinearLayout column() { LinearLayout v = new LinearLayout(this); v.setOrientation(LinearLayout.VERTICAL); return v; }
    private LinearLayout row() { LinearLayout v = new LinearLayout(this); v.setOrientation(LinearLayout.HORIZONTAL); return v; }
    private LinearLayout card(int color, int radius) { LinearLayout v = column(); v.setPadding(dp(18), dp(18), dp(18), dp(18)); v.setBackground(round(color, radius)); return v; }
    private TextView section(String value) { TextView t = text(value, 20, true, TEXT); t.setPadding(0, dp(24), 0, dp(12)); return t; }
    private TextView text(String value, int size, boolean bold, int color) { TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color); t.setTypeface(Typeface.create("sans", bold ? Typeface.BOLD : Typeface.NORMAL)); return t; }
    private Button action(String value, int bg, int fg) { Button b = new Button(this); b.setText(value); b.setTextColor(fg); b.setTextSize(14); b.setAllCaps(false); b.setTypeface(Typeface.DEFAULT, Typeface.BOLD); b.setBackground(round(bg, 16)); b.setStateListAnimator(null); return b; }
    private Button miniAction(String value, int color) { return action(value, CARD_2, color); }
    private Button navButton(String value) { Button b = action(value, 0x00000000, MUTED); b.setPadding(dp(2), 0, dp(2), 0); return b; }
    private EditText field(String hint, String value) { EditText e = new EditText(this); e.setHint(hint); e.setText(value); e.setSingleLine(true); return e; }
    private GradientDrawable round(int color, int radius) { GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d; }
    private LinearLayout.LayoutParams weighted(int left, int right) { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(58), 1); p.setMargins(dp(left), dp(14), dp(right), dp(14)); return p; }
    private LinearLayout.LayoutParams weightedSmall(int left, int right) { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(48), 1); p.setMargins(dp(left), dp(12), dp(right), 0); return p; }
    private LinearLayout.LayoutParams cardParams(int top) { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT); p.setMargins(0, dp(top), 0, 0); return p; }
    private View withMargin(View v, int top) { v.setLayoutParams(cardParams(top)); return v; }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private void toast(String value) { Toast.makeText(this, value, Toast.LENGTH_SHORT).show(); }

    private static long dayStart(long ms) { Calendar c = Calendar.getInstance(); c.setTimeInMillis(ms); c.set(Calendar.HOUR_OF_DAY,0); c.set(Calendar.MINUTE,0); c.set(Calendar.SECOND,0); c.set(Calendar.MILLISECOND,0); return c.getTimeInMillis(); }
    private static long weekStart() { Calendar c = Calendar.getInstance(); c.setFirstDayOfWeek(Calendar.MONDAY); c.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY); c.set(Calendar.HOUR_OF_DAY,0); c.set(Calendar.MINUTE,0); c.set(Calendar.SECOND,0); c.set(Calendar.MILLISECOND,0); return c.getTimeInMillis(); }
    private static long monthStart() { Calendar c = Calendar.getInstance(); c.set(Calendar.DAY_OF_MONTH,1); c.set(Calendar.HOUR_OF_DAY,0); c.set(Calendar.MINUTE,0); c.set(Calendar.SECOND,0); c.set(Calendar.MILLISECOND,0); return c.getTimeInMillis(); }
    private static String duration(long ms) { long s = Math.max(0, ms / 1000); return String.format(Locale.US, "%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60); }
    private static String shortDuration(long ms) { long m = Math.max(0, ms / 60000); return String.format(Locale.US, "%02d:%02d", m / 60, m % 60); }
    private static String format(long ms, String pattern) { return new SimpleDateFormat(pattern, Locale.US).format(new Date(ms)); }
    private static String time(long ms) { return format(ms, "HH:mm"); }
    private static long parseDateTime(String date, String time) throws ParseException { return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).parse(date.trim() + " " + time.trim()).getTime(); }
}

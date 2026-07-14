package com.abdessalem.worktracker;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class MainActivity extends Activity {
    private static final int CREATE_CSV = 501;
    private static final int BG = 0xFF08111F, CARD = 0xFF101C2D, TEXT = 0xFFF5F8FC;
    private static final int MUTED = 0xFF9EABC0, GREEN = 0xFF4ED27A, ORANGE = 0xFFFFB02E, RED = 0xFFFF625F;

    private final Handler handler = new Handler();
    private WorkDb db;
    private TextView timer, status, summary, history;
    private Button clockButton, breakButton;
    private long activeId = -1, clockIn = 0, breakStart = 0;
    private int breakMinutes = 0;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        db = new WorkDb(this);
        setContentView(buildUi());
        refresh();
        handler.post(ticker);
    }

    @Override protected void onDestroy() {
        handler.removeCallbacks(ticker);
        db.close();
        super.onDestroy();
    }

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);
        LinearLayout root = column();
        root.setPadding(dp(20), dp(20), dp(20), dp(36));
        scroll.addView(root);

        TextView title = text("Work Tracker", 28, true, TEXT);
        root.addView(title);
        TextView subtitle = text("Private • offline • local only", 14, false, MUTED);
        subtitle.setPadding(0, 0, 0, dp(18));
        root.addView(subtitle);

        LinearLayout hero = card();
        status = text("Not working", 14, true, MUTED);
        timer = text("00:00:00", 44, true, TEXT);
        timer.setPadding(0, dp(14), 0, dp(8));
        hero.addView(status);
        hero.addView(timer);
        hero.addView(text(new SimpleDateFormat("EEEE, d MMMM yyyy", Locale.US).format(new Date()), 14, false, MUTED));
        root.addView(hero, cardParams(0));

        LinearLayout actions = row();
        clockButton = action("Clock in", GREEN, BG);
        breakButton = action("Start break", ORANGE, BG);
        clockButton.setOnClickListener(v -> toggleClock());
        breakButton.setOnClickListener(v -> toggleBreak());
        actions.addView(clockButton, weighted(0, 6));
        actions.addView(breakButton, weighted(6, 0));
        root.addView(actions);

        root.addView(section("Today"));
        summary = text("Net 00:00   •   Break 00:00   •   Gross 00:00", 17, true, TEXT);
        summary.setPadding(dp(16), dp(18), dp(16), dp(18));
        summary.setBackground(round(CARD, 18));
        root.addView(summary);

        LinearLayout tools = row();
        Button note = action("Add note", 0xFF16243A, GREEN);
        Button export = action("Export CSV", 0xFF16243A, GREEN);
        note.setOnClickListener(v -> addNote());
        export.setOnClickListener(v -> exportCsv());
        tools.addView(note, weighted(0, 6));
        tools.addView(export, weighted(6, 0));
        root.addView(tools);

        root.addView(section("Recent shifts"));
        history = text("No records yet", 14, false, TEXT);
        history.setPadding(dp(16), dp(16), dp(16), dp(16));
        history.setBackground(round(CARD, 18));
        root.addView(history);

        Button delete = action("Delete latest record", 0xFF16243A, RED);
        delete.setOnClickListener(v -> confirmDelete());
        root.addView(delete, cardParams(12));

        TextView privacy = text("No internet permission. Your records stay on this device.", 12, false, MUTED);
        privacy.setGravity(Gravity.CENTER);
        privacy.setPadding(0, dp(24), 0, 0);
        root.addView(privacy);
        return scroll;
    }

    private void toggleClock() {
        long now = System.currentTimeMillis();
        if (activeId < 0) {
            ContentValues values = new ContentValues();
            values.put("clock_in", now);
            activeId = db.getWritableDatabase().insertOrThrow("shifts", null, values);
        } else {
            if (breakStart > 0) breakMinutes += (int) ((now - breakStart) / 60000L);
            ContentValues values = new ContentValues();
            values.put("clock_out", now);
            values.putNull("break_start");
            values.put("break_minutes", breakMinutes);
            db.getWritableDatabase().update("shifts", values, "id=?", new String[]{String.valueOf(activeId)});
        }
        refresh();
    }

    private void toggleBreak() {
        if (activeId < 0) { toast("Clock in first"); return; }
        long now = System.currentTimeMillis();
        ContentValues values = new ContentValues();
        if (breakStart == 0) {
            breakStart = now;
            values.put("break_start", now);
        } else {
            breakMinutes += (int) ((now - breakStart) / 60000L);
            breakStart = 0;
            values.putNull("break_start");
            values.put("break_minutes", breakMinutes);
        }
        db.getWritableDatabase().update("shifts", values, "id=?", new String[]{String.valueOf(activeId)});
        refresh();
    }

    private void addNote() {
        if (activeId < 0) { toast("Clock in first"); return; }
        EditText input = new EditText(this);
        input.setHint("Manager request, event, late finish…");
        new AlertDialog.Builder(this).setTitle("Shift note").setView(input)
                .setPositiveButton("Save", (d, w) -> {
                    ContentValues values = new ContentValues();
                    values.put("note", input.getText().toString().trim());
                    db.getWritableDatabase().update("shifts", values, "id=?", new String[]{String.valueOf(activeId)});
                    refresh();
                }).setNegativeButton("Cancel", null).show();
    }

    private void refresh() {
        activeId = -1; clockIn = 0; breakStart = 0; breakMinutes = 0;
        try (Cursor c = db.getReadableDatabase().rawQuery("SELECT id,clock_in,break_start,break_minutes FROM shifts WHERE clock_out IS NULL ORDER BY id DESC LIMIT 1", null)) {
            if (c.moveToFirst()) {
                activeId = c.getLong(0); clockIn = c.getLong(1);
                breakStart = c.isNull(2) ? 0 : c.getLong(2); breakMinutes = c.getInt(3);
            }
        }
        boolean working = activeId >= 0;
        status.setText(working ? (breakStart > 0 ? "On break" : "Working now") : "Not working");
        status.setTextColor(working ? GREEN : MUTED);
        clockButton.setText(working ? "Clock out" : "Clock in");
        clockButton.setBackground(round(working ? RED : GREEN, 16));
        breakButton.setEnabled(working);
        breakButton.setText(breakStart > 0 ? "End break" : "Start break");
        updateHistory();
        updateTimer();
    }

    private void updateTimer() {
        if (activeId < 0) { timer.setText("00:00:00"); summary.setText("Net 00:00   •   Break 00:00   •   Gross 00:00"); return; }
        long gross = System.currentTimeMillis() - clockIn;
        long currentBreak = breakStart > 0 ? System.currentTimeMillis() - breakStart : 0;
        long breaks = breakMinutes * 60000L + currentBreak;
        long net = Math.max(0, gross - breaks);
        timer.setText(duration(net));
        summary.setText("Net " + shortDuration(net) + "   •   Break " + shortDuration(breaks) + "   •   Gross " + shortDuration(gross));
    }

    private void updateHistory() {
        StringBuilder output = new StringBuilder();
        try (Cursor c = db.getReadableDatabase().rawQuery("SELECT clock_in,clock_out,break_minutes,note FROM shifts ORDER BY id DESC LIMIT 10", null)) {
            while (c.moveToNext()) {
                long start = c.getLong(0); Long end = c.isNull(1) ? null : c.getLong(1);
                long net = end == null ? System.currentTimeMillis() - start : Math.max(0, end - start - c.getInt(2) * 60000L);
                output.append(new SimpleDateFormat("EEE, d MMM", Locale.US).format(new Date(start)))
                        .append("\n").append(time(start)).append(" → ").append(end == null ? "Active" : time(end))
                        .append("   •   ").append(shortDuration(net)).append(" net");
                String note = c.getString(3);
                if (note != null && !note.trim().isEmpty()) output.append("\n“").append(note.trim()).append("”");
                output.append("\n\n");
            }
        }
        history.setText(output.length() == 0 ? "No records yet" : output.toString().trim());
    }

    private final Runnable ticker = new Runnable() {
        @Override public void run() { updateTimer(); handler.postDelayed(this, 1000); }
    };

    private void exportCsv() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_TITLE, "work-hours.csv");
        startActivityForResult(intent, CREATE_CSV);
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == CREATE_CSV && result == RESULT_OK && data != null && data.getData() != null) writeCsv(data.getData());
    }

    private void writeCsv(Uri uri) {
        StringBuilder csv = new StringBuilder("Date,Clock in,Clock out,Break minutes,Note\n");
        try (Cursor c = db.getReadableDatabase().rawQuery("SELECT clock_in,clock_out,break_minutes,note FROM shifts ORDER BY clock_in", null)) {
            while (c.moveToNext()) csv.append(date(c.getLong(0))).append(',').append(time(c.getLong(0))).append(',')
                    .append(c.isNull(1) ? "" : time(c.getLong(1))).append(',').append(c.getInt(2)).append(',')
                    .append('"').append(c.getString(3).replace("\"", "\"\"")).append('"').append('\n');
        }
        try (OutputStream stream = getContentResolver().openOutputStream(uri)) {
            if (stream == null) throw new IllegalStateException("Cannot open file");
            stream.write(csv.toString().getBytes(StandardCharsets.UTF_8)); toast("CSV exported");
        } catch (Exception e) { toast("Export failed: " + e.getMessage()); }
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this).setTitle("Delete latest record?").setMessage("This cannot be undone.")
                .setPositiveButton("Delete", (d, w) -> { db.getWritableDatabase().execSQL("DELETE FROM shifts WHERE id=(SELECT MAX(id) FROM shifts)"); refresh(); })
                .setNegativeButton("Cancel", null).show();
    }

    private LinearLayout column() { LinearLayout v = new LinearLayout(this); v.setOrientation(LinearLayout.VERTICAL); return v; }
    private LinearLayout row() { LinearLayout v = new LinearLayout(this); v.setOrientation(LinearLayout.HORIZONTAL); return v; }
    private LinearLayout card() { LinearLayout v = column(); v.setPadding(dp(18), dp(18), dp(18), dp(18)); v.setBackground(round(CARD, 20)); return v; }
    private TextView section(String value) { TextView t = text(value, 20, true, TEXT); t.setPadding(0, dp(24), 0, dp(12)); return t; }
    private TextView text(String value, int size, boolean bold, int color) { TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color); t.setTypeface(Typeface.create("sans", bold ? Typeface.BOLD : Typeface.NORMAL)); return t; }
    private Button action(String value, int bg, int fg) { Button b = new Button(this); b.setText(value); b.setTextColor(fg); b.setTextSize(14); b.setAllCaps(false); b.setTypeface(Typeface.DEFAULT, Typeface.BOLD); b.setBackground(round(bg, 16)); b.setStateListAnimator(null); return b; }
    private GradientDrawable round(int color, int radius) { GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d; }
    private LinearLayout.LayoutParams weighted(int left, int right) { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(58), 1); p.setMargins(dp(left), dp(14), dp(right), dp(14)); return p; }
    private LinearLayout.LayoutParams cardParams(int top) { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT); p.setMargins(0, dp(top), 0, 0); return p; }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private void toast(String value) { Toast.makeText(this, value, Toast.LENGTH_SHORT).show(); }
    private static String duration(long ms) { long s = Math.max(0, ms / 1000); return String.format(Locale.US, "%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60); }
    private static String shortDuration(long ms) { long m = Math.max(0, ms / 60000); return String.format(Locale.US, "%02d:%02d", m / 60, m % 60); }
    private static String date(long ms) { return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date(ms)); }
    private static String time(long ms) { return new SimpleDateFormat("HH:mm", Locale.US).format(new Date(ms)); }
}

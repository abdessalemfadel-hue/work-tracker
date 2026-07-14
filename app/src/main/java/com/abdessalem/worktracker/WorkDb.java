package com.abdessalem.worktracker;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public final class WorkDb extends SQLiteOpenHelper {
    public WorkDb(Context context) { super(context, "work_hours.db", null, 2); }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE shifts (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "clock_in INTEGER NOT NULL," +
                "clock_out INTEGER," +
                "break_start INTEGER," +
                "break_minutes INTEGER NOT NULL DEFAULT 0," +
                "note TEXT NOT NULL DEFAULT ''" +
                ")");
        db.execSQL("CREATE TABLE settings (key TEXT PRIMARY KEY, value TEXT NOT NULL)");
        db.execSQL("INSERT INTO settings(key,value) VALUES('daily_target_hours','8')");
        db.execSQL("INSERT INTO settings(key,value) VALUES('monthly_salary','10000')");
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("CREATE TABLE IF NOT EXISTS settings (key TEXT PRIMARY KEY, value TEXT NOT NULL)");
            db.execSQL("INSERT OR IGNORE INTO settings(key,value) VALUES('daily_target_hours','8')");
            db.execSQL("INSERT OR IGNORE INTO settings(key,value) VALUES('monthly_salary','10000')");
        }
    }
}
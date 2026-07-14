package com.abdessalem.worktracker;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public final class WorkDb extends SQLiteOpenHelper {
    public WorkDb(Context context) { super(context, "work_hours.db", null, 1); }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE shifts (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "clock_in INTEGER NOT NULL," +
                "clock_out INTEGER," +
                "break_start INTEGER," +
                "break_minutes INTEGER NOT NULL DEFAULT 0," +
                "note TEXT NOT NULL DEFAULT ''" +
                ")");
    }
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { }
}

package com.saisonflow.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

/** Persistent history, no fixed maximum number of entries; UI loads pages. */
public final class DownloadStore extends SQLiteOpenHelper {
    public static final class Record {
        public final long id;
        public final String title;
        public final String quality;
        Record(long id, String title, String quality) {
            this.id=id; this.title=title; this.quality=quality;
        }
    }
    public DownloadStore(Context context) {
        super(context, "saisonflow-downloads.db", null, 1);
    }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE downloads (id INTEGER PRIMARY KEY, title TEXT NOT NULL, quality TEXT NOT NULL, created_at INTEGER NOT NULL)");
    }
    @Override public void onUpgrade(SQLiteDatabase db, int old, int now) { }
    public void save(long id, String title, String quality) {
        ContentValues row = new ContentValues();
        row.put("id", id); row.put("title", title); row.put("quality", quality);
        row.put("created_at", System.currentTimeMillis());
        getWritableDatabase().insertWithOnConflict("downloads", null, row, SQLiteDatabase.CONFLICT_REPLACE);
    }
    public List<Record> page(int page, int size) {
        List<Record> rows = new ArrayList<>();
        int safePage = Math.max(0, page);
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT id,title,quality FROM downloads ORDER BY created_at DESC,id DESC LIMIT ? OFFSET ?",
                new String[]{String.valueOf(size), String.valueOf((long)safePage * size)})) {
            while (c.moveToNext()) rows.add(new Record(c.getLong(0), c.getString(1), c.getString(2)));
        }
        return rows;
    }
    public int count() {
        try(Cursor c = getReadableDatabase().rawQuery("SELECT count(*) FROM downloads", null)) {
            return c.moveToFirst()?c.getInt(0):0;
        }
    }
    public void remove(long id) { getWritableDatabase().delete("downloads", "id=?", new String[]{String.valueOf(id)}); }
}

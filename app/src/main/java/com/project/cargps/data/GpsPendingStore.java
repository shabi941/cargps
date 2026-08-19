package com.project.cargps.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Durable, ordered GPS upload queue.
 *
 * Each accepted GPS point is committed to SQLite before any network request is
 * attempted. The unique key makes retries idempotent on the device, while the
 * monotonically ordered query keeps the existing backend mileage calculation
 * from seeing out-of-order points whenever possible.
 */
public final class GpsPendingStore extends SQLiteOpenHelper {
    private static final String DB_NAME = "pending_gps.db";
    private static final int DB_VERSION = 1;
    private static final String TABLE = "pending_gps";

    public static final class Record {
        public long id;
        public String deviceNo;
        public long positionTime;
        public double longitude;
        public double latitude;
        public double speed;
        public float accuracy;
        public int locationType;
        public int satellites;
        public long createdAt;

        public Record(String deviceNo,
                      long positionTime,
                      double longitude,
                      double latitude,
                      double speed,
                      float accuracy,
                      int locationType,
                      int satellites,
                      long createdAt) {
            this.deviceNo = deviceNo;
            this.positionTime = normalizePositionTime(positionTime);
            this.longitude = longitude;
            this.latitude = latitude;
            this.speed = speed;
            this.accuracy = accuracy;
            this.locationType = locationType;
            this.satellites = satellites;
            this.createdAt = createdAt;
        }
    }

    public static final class QueueStats {
        public final long count;
        public final long firstPositionTime;
        public final long lastPositionTime;

        QueueStats(long count, long firstPositionTime, long lastPositionTime) {
            this.count = count;
            this.firstPositionTime = firstPositionTime;
            this.lastPositionTime = lastPositionTime;
        }
    }

    public GpsPendingStore(Context context) {
        super(context.getApplicationContext(), DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
        db.enableWriteAheadLogging();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "device_no TEXT NOT NULL,"
                + "position_time INTEGER NOT NULL,"
                + "longitude REAL NOT NULL,"
                + "latitude REAL NOT NULL,"
                + "speed REAL NOT NULL DEFAULT 0,"
                + "accuracy REAL NOT NULL DEFAULT 0,"
                + "location_type INTEGER NOT NULL DEFAULT 0,"
                + "satellites INTEGER NOT NULL DEFAULT 0,"
                + "created_at INTEGER NOT NULL,"
                + "UNIQUE(device_no, position_time) ON CONFLICT IGNORE"
                + ")");
        db.execSQL("CREATE INDEX idx_pending_gps_time ON " + TABLE
                + "(position_time ASC, id ASC)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Version 1 is the initial schema. Future upgrades must migrate in place;
        // pending GPS data must never be dropped during an app upgrade.
    }

    public synchronized boolean enqueue(Record record) {
        ContentValues values = new ContentValues();
        values.put("device_no", record.deviceNo);
        values.put("position_time", record.positionTime);
        values.put("longitude", record.longitude);
        values.put("latitude", record.latitude);
        values.put("speed", record.speed);
        values.put("accuracy", record.accuracy);
        values.put("location_type", record.locationType);
        values.put("satellites", record.satellites);
        values.put("created_at", record.createdAt);
        long id = getWritableDatabase().insertWithOnConflict(
                TABLE, null, values, SQLiteDatabase.CONFLICT_IGNORE);
        if (id == -1) {
            return false;
        }
        record.id = id;
        return true;
    }

    public synchronized Record peekOldest() {
        try (Cursor cursor = getReadableDatabase().query(
                TABLE,
                new String[]{"id", "device_no", "position_time", "longitude", "latitude",
                        "speed", "accuracy", "location_type", "satellites", "created_at"},
                null, null, null, null,
                "position_time ASC, id ASC",
                "1")) {
            if (!cursor.moveToFirst()) {
                return null;
            }
            Record record = new Record(
                    cursor.getString(1),
                    cursor.getLong(2),
                    cursor.getDouble(3),
                    cursor.getDouble(4),
                    cursor.getDouble(5),
                    cursor.getFloat(6),
                    cursor.getInt(7),
                    cursor.getInt(8),
                    cursor.getLong(9));
            record.id = cursor.getLong(0);
            return record;
        }
    }

    public synchronized boolean delete(long id) {
        return getWritableDatabase().delete(TABLE, "id=?", new String[]{String.valueOf(id)}) == 1;
    }

    public synchronized QueueStats getStats() {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT COUNT(*), COALESCE(MIN(position_time),0),"
                        + " COALESCE(MAX(position_time),0) FROM " + TABLE,
                null)) {
            if (cursor.moveToFirst()) {
                return new QueueStats(cursor.getLong(0), cursor.getLong(1), cursor.getLong(2));
            }
        }
        return new QueueStats(0, 0, 0);
    }

    public static long normalizePositionTime(long positionTime) {
        if (Math.abs(positionTime) >= 1_000_000_000_000L) {
            return positionTime / 1000L;
        }
        return positionTime;
    }
}

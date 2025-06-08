package com.nlu.convertapp.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "ConvertApp.db";
    private static final int DATABASE_VERSION = 1;

    // Table name
    public static final String TABLE_TEXT_STORAGE = "text_storage";

    // Column names
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_DATE = "date";
    public static final String COLUMN_CONTENT = "content";
    public static final String COLUMN_STARRED = "starred";

    // Create table query
    private static final String CREATE_TEXT_STORAGE_TABLE = 
        "CREATE TABLE " + TABLE_TEXT_STORAGE + " (" +
        COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
        COLUMN_DATE + " TEXT NOT NULL, " +
        COLUMN_CONTENT + " TEXT NOT NULL, " +
        COLUMN_STARRED + " INTEGER DEFAULT 0)";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TEXT_STORAGE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // For future database schema updates
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TEXT_STORAGE);
        onCreate(db);
    }
} 
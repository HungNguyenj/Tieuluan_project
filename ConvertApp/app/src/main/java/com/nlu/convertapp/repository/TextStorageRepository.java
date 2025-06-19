package com.nlu.convertapp.repository;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.nlu.convertapp.database.DatabaseHelper;
import com.nlu.convertapp.models.TextStorageItem;

import java.util.ArrayList;
import java.util.List;

public class TextStorageRepository {
    private DatabaseHelper dbHelper;
    private SQLiteDatabase database;

    public TextStorageRepository(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    public void open() {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public long insertText(TextStorageItem item) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_DATE, item.getDate());
        values.put(DatabaseHelper.COLUMN_CONTENT, item.getContent());
        values.put(DatabaseHelper.COLUMN_STARRED, item.isStarred() ? 1 : 0);

        return database.insert(DatabaseHelper.TABLE_TEXT_STORAGE, null, values);
    }

    public List<TextStorageItem> getAllTexts() {
        List<TextStorageItem> items = new ArrayList<>();

        Cursor cursor = database.query(
            DatabaseHelper.TABLE_TEXT_STORAGE,
            null,
            null,
            null,
            null,
            null,
            DatabaseHelper.COLUMN_DATE + " DESC"
        );

        if (cursor != null && cursor.moveToFirst()) {
            do {
                @SuppressLint("Range") String date = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_DATE));
                @SuppressLint("Range") String content = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_CONTENT));
                @SuppressLint("Range") boolean starred = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_STARRED)) == 1;

                items.add(new TextStorageItem(date, content, starred));
            } while (cursor.moveToNext());

            cursor.close();
        }

        return items;
    }

    public int updateText(TextStorageItem item, String oldDate) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_DATE, item.getDate());
        values.put(DatabaseHelper.COLUMN_CONTENT, item.getContent());
        values.put(DatabaseHelper.COLUMN_STARRED, item.isStarred() ? 1 : 0);

        return database.update(
            DatabaseHelper.TABLE_TEXT_STORAGE,
            values,
            DatabaseHelper.COLUMN_DATE + " = ?",
            new String[]{oldDate}
        );
    }

    public int deleteText(String date) {
        return database.delete(
            DatabaseHelper.TABLE_TEXT_STORAGE,
            DatabaseHelper.COLUMN_DATE + " = ?",
            new String[]{date}
        );
    }

    public List<TextStorageItem> getStarredTexts() {
        List<TextStorageItem> items = new ArrayList<>();

        Cursor cursor = database.query(
            DatabaseHelper.TABLE_TEXT_STORAGE,
            null,
            DatabaseHelper.COLUMN_STARRED + " = ?",
            new String[]{"1"},
            null,
            null,
            DatabaseHelper.COLUMN_DATE + " DESC"
        );

        if (cursor != null && cursor.moveToFirst()) {
            do {
                @SuppressLint("Range") String date = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_DATE));
                @SuppressLint("Range") String content = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_CONTENT));
                boolean starred = true;

                items.add(new TextStorageItem(date, content, starred));
            } while (cursor.moveToNext());

            cursor.close();
        }

        return items;
    }
} 
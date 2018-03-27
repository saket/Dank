package me.saket.dank.utils;

import android.database.Cursor;

@SuppressWarnings("SpellCheckingInspection")
public class Cursors {

  public static String string(Cursor cursor, String columnName) {
    return cursor.getString(cursor.getColumnIndexOrThrow(columnName));
  }

  public static long longg(Cursor cursor, String columnName) {
    return cursor.getLong(cursor.getColumnIndexOrThrow(columnName));
  }

  public static int intt(Cursor cursor, String columnName) {
    return cursor.getInt(cursor.getColumnIndexOrThrow(columnName));
  }
}

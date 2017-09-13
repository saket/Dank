package me.saket.dank.utils;

import android.database.Cursor;

public class Cursors {

  public static String string(Cursor cursor, String columnName) {
    return cursor.getString(cursor.getColumnIndexOrThrow(columnName));
  }
}

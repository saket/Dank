package me.saket.dank.ui.appshortcuts;

import android.content.ContentValues;
import android.database.Cursor;

import com.google.auto.value.AutoValue;

import io.reactivex.functions.Function;
import me.saket.dank.utils.Cursors;

@AutoValue
public abstract class AppShortcut implements AppShortcutScreenUiModel {

  static final String TABLE_NAME = "AppShortcut";
  static final String COLUMN_RANK = "rank";
  static final String COLUMN_LABEL = "label";

  public static final String QUERY_CREATE_TABLE =
      "CREATE TABLE " + TABLE_NAME + " ("
          + COLUMN_LABEL + " TEXT NOT NULL PRIMARY KEY, "
          + COLUMN_RANK + " TEXT NOT NULL)";

  public static final String QUERY_GET_ALL_ORDERED_BY_RANK =
      "SELECT * FROM " + TABLE_NAME;

  public static final String WHERE_LABEL =
      COLUMN_LABEL + " = ?";

  public static AppShortcut create(int rank, String label) {
    return new AutoValue_AppShortcut(rank, label);
  }

  public static final Function<Cursor, AppShortcut> MAPPER = cursor -> {
    int rank = Cursors.intt(cursor, COLUMN_RANK);
    String label = Cursors.string(cursor, COLUMN_LABEL);
    return create(rank, label);
  };

  public abstract int rank();

  public abstract String label();

  @Override
  public long adapterId() {
    return label().hashCode();
  }

  public ContentValues toValues() {
    ContentValues values = new ContentValues(2);
    values.put(COLUMN_RANK, rank());
    values.put(COLUMN_LABEL, label());
    return values;
  }
}

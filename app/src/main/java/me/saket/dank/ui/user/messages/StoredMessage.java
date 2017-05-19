package me.saket.dank.ui.user.messages;

import android.content.ContentValues;
import android.database.Cursor;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.Message;

import me.saket.dank.utils.JacksonHelper;
import me.saket.dank.utils.JrawUtils;
import rx.functions.Func1;

/**
 * {@link Message} stored in the DB.
 */
@AutoValue
public abstract class StoredMessage {

  public static final String TABLE_NAME = "StoredMessage";
  static final String COLUMN_ID = "id";
  static final String COLUMN_MESSAGE = "message";
  static final String COLUMN_CREATED_TIME_MS = "created_time_ms";
  static final String COLUMN_FOLDER = "folder";

  public static final String QUERY_CREATE_TABLE =
      "CREATE TABLE " + TABLE_NAME + " ("
          + COLUMN_ID + " TEXT NOT NULL PRIMARY KEY, "
          + COLUMN_MESSAGE + " TEXT NOT NULL, "
          + COLUMN_CREATED_TIME_MS + " INTEGER NOT NULL, "
          + COLUMN_FOLDER + " TEXT NOT NULL)";

  public static final String QUERY_GET_ALL_IN_FOLDER =
      "SELECT * FROM " + TABLE_NAME
          + " WHERE " + COLUMN_FOLDER + " == ?"
          + " ORDER BY " + COLUMN_CREATED_TIME_MS + " DESC"; // Latest message first

  public static final String QUERY_GET_LAST_IN_FOLDER =
      "SELECT * FROM " + TABLE_NAME
          + " WHERE " + COLUMN_FOLDER + " == ?"
          + " ORDER BY " + COLUMN_CREATED_TIME_MS + " ASC"
          + " LIMIT 1";

  public static final String QUERY_WHERE_FOLDER =
      COLUMN_FOLDER + " == ?";

  public abstract String id();

  public abstract Message message();

  public abstract long createdTimeMillis();

  public abstract InboxFolder folder();

  public ContentValues toContentValues(JacksonHelper jacksonHelper) {
    ContentValues values = new ContentValues(4);
    values.put(COLUMN_ID, id());
    values.put(COLUMN_MESSAGE, jacksonHelper.toJson(message()));
    values.put(COLUMN_CREATED_TIME_MS, createdTimeMillis());
    values.put(COLUMN_FOLDER, folder().name());
    return values;
  }

  public static Func1<Cursor, StoredMessage> mapFromCursor(JacksonHelper jacksonHelper) {
    return cursor -> {
      Message message = JrawUtils.parseMessageJson(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MESSAGE)), jacksonHelper);
      long createdTimeMillis = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_TIME_MS));
      InboxFolder folder = InboxFolder.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FOLDER)));
      return create(message.getId(), message, createdTimeMillis, folder);
    };
  }

  public static Func1<Cursor, Message> mapMessageFromCursor(JacksonHelper jacksonHelper) {
    return cursor -> JrawUtils.parseMessageJson(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MESSAGE)), jacksonHelper);
  }

  public static StoredMessage create(String id, Message message, long createdTimeMillis, InboxFolder folder) {
    return new AutoValue_StoredMessage(id, message, createdTimeMillis, folder);
  }

}

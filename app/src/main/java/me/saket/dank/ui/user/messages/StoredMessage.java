package me.saket.dank.ui.user.messages;

import android.content.ContentValues;
import android.database.Cursor;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import net.dean.jraw.models.CommentMessage;
import net.dean.jraw.models.Message;
import net.dean.jraw.models.PrivateMessage;

import java.io.IOException;

import rx.functions.Func1;

/**
 * {@link Message} stored in the DB.
 */
@AutoValue
public abstract class StoredMessage {

  public static final String TABLE_NAME = "StoredMessage";
  static final String COLUMN_ID = "id";
  static final String COLUMN_MESSAGE = "message";
  static final String COLUMN_LATEST_MESSAGE_TIMESTAMP = "latest_message_timestamp";
  static final String COLUMN_FOLDER = "folder";

  public static final String QUERY_CREATE_TABLE =
      "CREATE TABLE " + TABLE_NAME + " ("
          + COLUMN_ID + " TEXT NOT NULL, "
          + COLUMN_MESSAGE + " TEXT NOT NULL, "
          + COLUMN_LATEST_MESSAGE_TIMESTAMP + " INTEGER NOT NULL, "
          + COLUMN_FOLDER + " TEXT NOT NULL, "
          + "PRIMARY KEY (" + COLUMN_ID + ", " + COLUMN_FOLDER + ")"
          + ")";

  public static final String QUERY_GET_ALL_IN_FOLDER =
      "SELECT * FROM " + TABLE_NAME
          + " WHERE " + COLUMN_FOLDER + " == ?"
          + " ORDER BY " + COLUMN_LATEST_MESSAGE_TIMESTAMP + " DESC"; // Latest message first

  public static final String QUERY_GET_LAST_IN_FOLDER =
      "SELECT * FROM " + TABLE_NAME
          + " WHERE " + COLUMN_FOLDER + " == ?"
          + " ORDER BY " + COLUMN_LATEST_MESSAGE_TIMESTAMP + " ASC"
          + " LIMIT 1";

  public static final String QUERY_WHERE_FOLDER =
      COLUMN_FOLDER + " == ?";

  public static final String QUERY_WHERE_FOLDER_AND_ID =
      COLUMN_FOLDER + " == ? AND " + COLUMN_ID + " == ?";

  public abstract String id();

  public abstract Message message();

  /**
   * For {@link PrivateMessage}, this is the timestamp of the last message in the thread.
   * For {@link CommentMessage}, this is the message's timestamp.
   */
  public abstract long latestMessageTimestamp();

  public abstract InboxFolder folder();

  public ContentValues toContentValues(Moshi moshi) {
    ContentValues values = new ContentValues(4);
    values.put(COLUMN_ID, id());
    values.put(COLUMN_MESSAGE, moshi.adapter(Message.class).toJson(message()));
    values.put(COLUMN_LATEST_MESSAGE_TIMESTAMP, latestMessageTimestamp());
    values.put(COLUMN_FOLDER, folder().name());
    return values;
  }

  public static Func1<Cursor, StoredMessage> mapFromCursor(Moshi moshi) {
    return cursor -> {
      Message message = mapMessageFromCursor(moshi).call(cursor);
      long latestMessageTimestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_LATEST_MESSAGE_TIMESTAMP));
      InboxFolder folder = InboxFolder.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FOLDER)));
      return create(message.getId(), message, latestMessageTimestamp, folder);
    };
  }

  public static Func1<Cursor, Message> mapMessageFromCursor(Moshi moshi) {
    return cursor -> {
      JsonAdapter<Message> adapter = moshi.adapter(Message.class);
      try {
        return adapter.fromJson(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MESSAGE)));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    };
  }

  public static StoredMessage create(String id, Message message, long latestMessageTimestamp, InboxFolder folder) {
    return new AutoValue_StoredMessage(id, message, latestMessageTimestamp, folder);
  }

}

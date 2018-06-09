package me.saket.dank.ui.user.messages;

import android.content.ContentValues;
import android.database.Cursor;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.JsonAdapter;

import net.dean.jraw.models.Message;

import java.io.IOException;

import io.reactivex.functions.Function;
import me.saket.dank.data.MoshiAdapter;
import me.saket.dank.utils.Cursors;
import me.saket.dank.utils.Optional;

/**
 * {@link Message} stored in the DB.
 */
@AutoValue
public abstract class CachedMessage {

  public static final String TABLE_NAME = "CachedMessage";
  static final String COLUMN_FULLNAME = "fullname";
  static final String COLUMN_MESSAGE = "message";
  static final String COLUMN_LATEST_MESSAGE_TIME = "latest_message_time";
  static final String COLUMN_FOLDER = "folder";

  public static final String QUERY_CREATE_TABLE =
      "CREATE TABLE " + TABLE_NAME + " ("
          + COLUMN_FULLNAME + " TEXT NOT NULL, "
          + COLUMN_MESSAGE + " TEXT NOT NULL, "
          + COLUMN_LATEST_MESSAGE_TIME + " INTEGER NOT NULL, "
          + COLUMN_FOLDER + " TEXT NOT NULL, "
          + "PRIMARY KEY (" + COLUMN_FULLNAME + ", " + COLUMN_FOLDER + ")"
          + ")";

  public static final String QUERY_GET_ALL_IN_FOLDER =
      "SELECT * FROM " + TABLE_NAME
          + " WHERE " + COLUMN_FOLDER + " == ?"
          + " ORDER BY " + COLUMN_LATEST_MESSAGE_TIME + " DESC"; // Latest message first

  public static final String QUERY_GET_LAST_IN_FOLDER =
      "SELECT * FROM " + TABLE_NAME
          + " WHERE " + COLUMN_FOLDER + " == ?"
          + " ORDER BY " + COLUMN_LATEST_MESSAGE_TIME + " ASC"
          + " LIMIT 1";

  public static final String QUERY_GET_SINGLE =
      "SELECT * FROM " + TABLE_NAME
          + " WHERE " + COLUMN_FULLNAME + " == ? "
          + " AND " + COLUMN_FOLDER + " == ?";

  public static final String WHERE_FOLDER =
      COLUMN_FOLDER + " == ?";

  public static final String WHERE_FOLDER_AND_FULLNAME =
      COLUMN_FOLDER + " == ? AND " + COLUMN_FULLNAME + " == ?";

  public abstract String fullname();

  public abstract Message message();

  /**
   * For private messages, this is the timestamp of the last message in the thread.
   * For comment messages, this is the message's timestamp.
   */
  public abstract long latestMessageTimestamp();

  public abstract InboxFolder folder();

  public ContentValues toContentValues(MoshiAdapter moshiAdapter) {
    ContentValues values = new ContentValues(4);
    values.put(COLUMN_FULLNAME, fullname());
    String json = moshiAdapter.create(Message.class).toJson(message());
    if (!json.contains("distinguished")) {
      throw new AssertionError("Invalid json serialization");
    }
    values.put(COLUMN_MESSAGE, json);
    values.put(COLUMN_LATEST_MESSAGE_TIME, latestMessageTimestamp());
    values.put(COLUMN_FOLDER, folder().name());
    return values;
  }

  public static Function<Cursor, CachedMessage> fromCursor(MoshiAdapter moshiAdapter) {
    return cursor -> {
      Message message = messageFromCursor(moshiAdapter).apply(cursor);
      long latestMessageTimestamp = Cursors.longg(cursor, COLUMN_LATEST_MESSAGE_TIME);
      InboxFolder folder = InboxFolder.valueOf(Cursors.string(cursor, COLUMN_FOLDER));
      return create(message.getFullName(), message, latestMessageTimestamp, folder);
    };
  }

  public static Function<Cursor, Optional<Message>> optionalMessageFromCursor(MoshiAdapter moshiAdapter) {
    return cursor -> Optional.of(messageFromCursor(moshiAdapter).apply(cursor));
  }

  public static Function<Cursor, Message> messageFromCursor(MoshiAdapter moshiAdapter) {
    return cursor -> {
      JsonAdapter<Message> adapter = moshiAdapter.create(Message.class);
      try {
        return adapter.fromJson(Cursors.string(cursor, COLUMN_MESSAGE));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    };
  }

  public static CachedMessage create(String fullName, Message message, long latestMessageTimestamp, InboxFolder folder) {
    return new AutoValue_CachedMessage(fullName, message, latestMessageTimestamp, folder);
  }
}

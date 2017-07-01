package me.saket.dank.ui.submission;

import android.content.ContentValues;
import android.database.Cursor;

import com.google.auto.value.AutoValue;

import rx.functions.Func1;

/**
 * See {@link CommentsManager}.
 */
@AutoValue
public abstract class PendingSyncReply {

  public static final String TABLE_NAME = "PendingReply";
  private static final String COLUMN_PARENT_COMMENT_FULL_NAME = "parent_comment_full_name";
  private static final String COLUMN_BODY = "body";
  private static final String COLUMN_TYPE = "type";
  private static final String COLUMN_PARENT_SUBMISSION_FULL_NAME = "parent_submission_full_name";
  private static final String COLUMN_AUTHOR = "author";
  private static final String COLUMN_CREATED_TIME_MILLIS = "created_time_millis";

  public static final String QUERY_CREATE_TABLE =
      "CREATE TABLE " + TABLE_NAME + " ("
          + COLUMN_PARENT_COMMENT_FULL_NAME + " TEXT NOT NULL, "
          + COLUMN_BODY + " TEXT NOT NULL, "
          + COLUMN_TYPE + " TEXT NOT NULL, "
          + COLUMN_PARENT_SUBMISSION_FULL_NAME + " TEXT NOT NULL, "
          + COLUMN_AUTHOR + " TEXT NOT NULL, "
          + COLUMN_CREATED_TIME_MILLIS + " INTEGER NOT NULL, "
          + "PRIMARY KEY (" + COLUMN_BODY + ", " + COLUMN_CREATED_TIME_MILLIS + ")"
          + ")";

  public static final String QUERY_GET_ALL_FOR_SUBMISSION =
      "SELECT * FROM " + TABLE_NAME
          + " WHERE " + COLUMN_PARENT_SUBMISSION_FULL_NAME + " == ?"
          + " ORDER BY " + COLUMN_CREATED_TIME_MILLIS + " DESC";

  public static final String WHERE_BODY_AND_CREATED_TIME_2 =
      COLUMN_BODY + " = ? AND " + COLUMN_CREATED_TIME_MILLIS + " = ?";

  /**
   * Full-name of parent comment node.
   */
  public abstract String parentCommentFullName();

  public abstract String body();

  public abstract State state();

  public abstract String parentSubmissionFullName();

  public abstract String author();

  public abstract long createdTimeMillis();

  public enum State {
    POSTING,
    POSTED,
    FAILED,
  }

  public static PendingSyncReply create(String parentCommentFullName, String reply, State state, String parentSubmissionFullName, String author,
      long createdTime)
  {
    return new AutoValue_PendingSyncReply(parentCommentFullName, reply, state, parentSubmissionFullName, author, createdTime);
  }

  public PendingSyncReply withType(State newState) {
    return create(parentCommentFullName(), body(), newState, parentSubmissionFullName(), author(), createdTimeMillis());
  }

  public ContentValues toValues() {
    ContentValues contentValues = new ContentValues(6);
    contentValues.put(COLUMN_PARENT_COMMENT_FULL_NAME, parentCommentFullName());
    contentValues.put(COLUMN_BODY, body());
    contentValues.put(COLUMN_TYPE, state().name());
    contentValues.put(COLUMN_PARENT_SUBMISSION_FULL_NAME, parentSubmissionFullName());
    contentValues.put(COLUMN_AUTHOR, author());
    contentValues.put(COLUMN_CREATED_TIME_MILLIS, createdTimeMillis());
    return contentValues;
  }

  public static final Func1<Cursor, PendingSyncReply> MAPPER = cursor -> {
    String parentCommentFullName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PARENT_COMMENT_FULL_NAME));
    String body = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BODY));
    State state = State.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TYPE)));
    String parentSubmissionFullName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PARENT_SUBMISSION_FULL_NAME));
    String author = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_AUTHOR));
    long createdTime = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_TIME_MILLIS));
    return create(parentCommentFullName, body, state, parentSubmissionFullName, author, createdTime);
  };
}

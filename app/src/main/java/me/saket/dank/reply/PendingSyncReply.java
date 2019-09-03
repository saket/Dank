package me.saket.dank.reply;

import android.content.ContentValues;
import android.database.Cursor;

import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.PublicContribution;

import io.reactivex.functions.Function;
import me.saket.dank.utils.Cursors;

/**
 * A reply made locally that acts as a placeholder until its actual model is fetched from remote.
 */
@AutoValue
public abstract class PendingSyncReply {

  public static final String TABLE_NAME = "PendingSyncReply";
  private static final String COLUMN_PARENT_CONTRIBUTION_FULL_NAME = "parent_contribution_full_name";
  private static final String COLUMN_BODY = "body";
  private static final String COLUMN_STATE = "state";
  private static final String COLUMN_PARENT_THREAD_FULL_NAME = "parent_thread_full_name";
  private static final String COLUMN_AUTHOR = "author";
  private static final String COLUMN_CREATED_TIME_MILLIS = "created_time_millis";
  private static final String COLUMN_SENT_TIME_MILLIS = "sent_time_millis";
  private static final String COLUMN_POSTED_FULLNAME = "posted_fullname";

  public static final String QUERY_CREATE_TABLE =
      "CREATE TABLE " + TABLE_NAME + " ("
          + COLUMN_PARENT_CONTRIBUTION_FULL_NAME + " TEXT NOT NULL, "
          + COLUMN_BODY + " TEXT NOT NULL, "
          + COLUMN_STATE + " TEXT NOT NULL, "
          + COLUMN_PARENT_THREAD_FULL_NAME + " TEXT NOT NULL, "
          + COLUMN_AUTHOR + " TEXT NOT NULL, "
          + COLUMN_CREATED_TIME_MILLIS + " INTEGER NOT NULL, "
          + COLUMN_SENT_TIME_MILLIS + " INTEGER NOT NULL, "
          + COLUMN_POSTED_FULLNAME + " TEXT, "
          + "PRIMARY KEY (" + COLUMN_BODY + ", " + COLUMN_CREATED_TIME_MILLIS + ")"
          + ")";

  // NOTE: In case you're ever wondering why messages are not appearing in the correct
  // order in PrivateMessagesThreadActivity -- that activity manually does sorting
  // instead of doing a table join :/.
  public static final String QUERY_GET_ALL_FOR_THREAD =
      "SELECT * FROM " + TABLE_NAME
          + " WHERE " + COLUMN_PARENT_THREAD_FULL_NAME + " == ?"
          + " ORDER BY " + COLUMN_SENT_TIME_MILLIS + " DESC";

  public static final String QUERY_GET_ALL_FAILED =
      "SELECT * FROM " + TABLE_NAME
          + " WHERE " + COLUMN_STATE + " == '" + State.FAILED + "'";

  public static final String WHERE_STATE_AND_THREAD_FULL_NAME =
      COLUMN_STATE + " = ? AND " + COLUMN_PARENT_THREAD_FULL_NAME + " = ?";

  public enum State {
    POSTING,
    POSTED,
    FAILED,
  }

  public abstract String body();

  public abstract State state();

  /**
   * Thread: submission / private message.
   */
  public abstract String parentThreadFullName();

  /**
   * Full-name of parent {@link PublicContribution comment/submission/message}.
   */
  public abstract String parentContributionFullName();

  public abstract String author();

  public abstract long createdTimeMillis();

  public abstract long sentTimeMillis();

  /**
   * Full-name of this comment on remote if it has been posted.
   */
  @Nullable
  public abstract String postedFullName();

  /**
   * @param parentThreadFullName thread == submission / private message.
   */
  public static PendingSyncReply create(String body, State state, String parentThreadFullName, String parentContributionFullName, String author,
      long createdTimeMillis, long sentTimeMillis)
  {
    return create(body, state, parentThreadFullName, parentContributionFullName, author, createdTimeMillis, sentTimeMillis, null);
  }

  private static PendingSyncReply create(String body, State state, String parentThreadFullName, String parentContributionFullName, String author,
      long createdTimeMillis, long sentTimeMillis, @Nullable String postedFullName)
  {
    return builder()
        .body(body)
        .state(state)
        .parentThreadFullName(parentThreadFullName)
        .parentContributionFullName(parentContributionFullName)
        .author(author)
        .createdTimeMillis(createdTimeMillis)
        .sentTimeMillis(sentTimeMillis)
        .postedFullName(postedFullName)
        .build();
  }

  public static PendingSyncReply.Builder builder() {
    return new AutoValue_PendingSyncReply.Builder();
  }

  public abstract PendingSyncReply.Builder toBuilder();

  public ContentValues toValues() {
    ContentValues contentValues = new ContentValues(7);
    contentValues.put(COLUMN_PARENT_CONTRIBUTION_FULL_NAME, parentContributionFullName());
    contentValues.put(COLUMN_BODY, body());
    contentValues.put(COLUMN_STATE, state().name());
    contentValues.put(COLUMN_PARENT_THREAD_FULL_NAME, parentThreadFullName());
    contentValues.put(COLUMN_AUTHOR, author());
    contentValues.put(COLUMN_CREATED_TIME_MILLIS, createdTimeMillis());
    contentValues.put(COLUMN_SENT_TIME_MILLIS, sentTimeMillis());
    contentValues.put(COLUMN_POSTED_FULLNAME, postedFullName());
    return contentValues;
  }

  public static final Function<Cursor, PendingSyncReply> MAPPER = cursor -> {
    String parentContributionFullName = Cursors.string(cursor, COLUMN_PARENT_CONTRIBUTION_FULL_NAME);
    String body = Cursors.string(cursor, COLUMN_BODY);
    State state = State.valueOf(Cursors.string(cursor, COLUMN_STATE));
    String parentThreadFullName = Cursors.string(cursor, COLUMN_PARENT_THREAD_FULL_NAME);
    String author = Cursors.string(cursor, COLUMN_AUTHOR);
    long createdTime = Cursors.longg(cursor, COLUMN_CREATED_TIME_MILLIS);
    long sentTime = Cursors.longg(cursor, COLUMN_SENT_TIME_MILLIS);
    String postedFullName = Cursors.string(cursor, COLUMN_POSTED_FULLNAME);
    return create(body, state, parentThreadFullName, parentContributionFullName, author, createdTime, sentTime, postedFullName);
  };

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder parentContributionFullName(String parentContributionFullName);

    public abstract Builder body(String body);

    public abstract Builder state(State state);

    public abstract Builder parentThreadFullName(String parentThreadFullName);

    public abstract Builder author(String author);

    public abstract Builder createdTimeMillis(long createdTimeMillis);

    public abstract Builder sentTimeMillis(long sentTimeMillis);

    public abstract Builder postedFullName(@Nullable String postedFullName);

    public abstract PendingSyncReply build();
  }
}

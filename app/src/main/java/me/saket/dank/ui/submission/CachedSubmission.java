package me.saket.dank.ui.submission;

import android.content.ContentValues;
import android.database.Cursor;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.Moshi;

import net.dean.jraw.models.Submission;
import net.dean.jraw.models.VoteDirection;

import java.io.IOException;

import me.saket.dank.data.SortingAndTimePeriod;
import rx.functions.Func1;

@AutoValue
public abstract class CachedSubmission {

  public static final String TABLE_NAME = "CachedSubmission";
  private static final String COLUMN_FULLNAME = "full_name";
  private static final String COLUMN_SUBMISSION_JSON = "submission_json";
  private static final String COLUMN_SUBREDDIT_NAME = "subreddit_name";
  private static final String COLUMN_SORTING_INFO = "sort_info";
  private static final String COLUMN_FETCH_TIME = "fetch_time";
  private static final String COLUMN_USER_VOTE_DIRECTION = "user_vote_direction";
  private static final String COLUMN_IS_SAVED_BY_USER = "is_saved_by_user";

  private static final int BOOLEAN_TRUE_INT = 1;
  private static final int BOOLEAN_FALSE_INT = 0;

  public static final String QUERY_CREATE_TABLE =
      "CREATE TABLE " + TABLE_NAME + " ("
          + COLUMN_FULLNAME + " TEXT NOT NULL, "
          + COLUMN_SUBMISSION_JSON + " TEXT NOT NULL, "
          + COLUMN_SUBREDDIT_NAME + " TEXT NOT NULL, "
          + COLUMN_SORTING_INFO + " TEXT NOT NULL, "
          + COLUMN_FETCH_TIME + " INTEGER NOT NULL, "
          + COLUMN_USER_VOTE_DIRECTION + " TEXT NOT NULL, "
          + COLUMN_IS_SAVED_BY_USER + " INTEGER NOT NULL, "
          + "PRIMARY KEY (" + COLUMN_FULLNAME + ", " + COLUMN_SORTING_INFO + ")" +
          ")";

  public static final String QUERY_GET_ALL_IN_SUBREDDIT_WHERE_SORTING_INFO =
      "SELECT * FROM " + TABLE_NAME
          + " WHERE " + COLUMN_SUBREDDIT_NAME + " == ? "
          + "ORDER BY " + COLUMN_FETCH_TIME + " DESC";

  public static String constructQueryToGetAll(String subredditName, SortingAndTimePeriod sortingInfo) {
    return "SELECT * FROM " + TABLE_NAME
        + " WHERE " + COLUMN_SUBREDDIT_NAME + " == " + subredditName
        + " AND " + COLUMN_SORTING_INFO + " == " + sortingInfo.serialize()
        + " ORDER BY " + COLUMN_FETCH_TIME + " DESC";
  }

  public static final String QUERY_GET_LAST_IN_FOLDER =
      "SELECT * FROM " + TABLE_NAME
          + " WHERE " + COLUMN_SUBREDDIT_NAME + " == ?"
          + " ORDER BY " + COLUMN_FETCH_TIME + " ASC"
          + " LIMIT 1";

  public static final String WHERE_SUBREDDIT_NAME =
      COLUMN_SUBREDDIT_NAME + " == ?";

  public abstract String fullName();

  public abstract Submission submission();

  public abstract String subredditName();

  public abstract SortingAndTimePeriod sortingInfo();

  public abstract long fetchTimeMillis();

  // TODO: This doesn't look necessary. Submission already contains this?
  public abstract VoteDirection userVoteDirection();

  public abstract boolean isSavedByUser();

  public ContentValues toContentValues(Moshi moshi) {
    ContentValues contentValues = new ContentValues(7);
    contentValues.put(COLUMN_FULLNAME, fullName());
    contentValues.put(COLUMN_SUBMISSION_JSON, moshi.adapter(Submission.class).toJson(submission()));
    contentValues.put(COLUMN_SUBREDDIT_NAME, subredditName());
    contentValues.put(COLUMN_SORTING_INFO, sortingInfo().serialize());
    contentValues.put(COLUMN_FETCH_TIME, fetchTimeMillis());
    contentValues.put(COLUMN_USER_VOTE_DIRECTION, userVoteDirection().name());
    contentValues.put(COLUMN_IS_SAVED_BY_USER, isSavedByUser() ? BOOLEAN_TRUE_INT : BOOLEAN_FALSE_INT);
    return contentValues;
  }

  public static Func1<Cursor, CachedSubmission> mapFromCursor(Moshi moshi) {
    return cursor -> {
      String fullName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FULLNAME));
      Submission submission = submissionFromJson(moshi, cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SUBMISSION_JSON)));
      String subredditName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SUBREDDIT_NAME));
      SortingAndTimePeriod sortingInfo = SortingAndTimePeriod.valueOf(cursor.getColumnName(cursor.getColumnIndexOrThrow(COLUMN_SORTING_INFO)));
      long fetchTimeMillis = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_FETCH_TIME));
      VoteDirection userVoteDirection = VoteDirection.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_VOTE_DIRECTION)));
      boolean isSavedByUser = BOOLEAN_TRUE_INT == cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_SAVED_BY_USER));
      return create(fullName, submission, subredditName, sortingInfo, fetchTimeMillis, userVoteDirection, isSavedByUser);
    };
  }

  private static Submission submissionFromJson(Moshi moshi, String submissionJson) {
    try {
      return moshi.adapter(Submission.class).fromJson(submissionJson);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static CachedSubmission create(String fullName, Submission submission, String subredditName, SortingAndTimePeriod sortingInfo,
      long downloadTimeMillis, VoteDirection userVoteDirection, boolean isSavedByUser)
  {
    return new AutoValue_CachedSubmission(fullName, submission, subredditName, sortingInfo, downloadTimeMillis, userVoteDirection, isSavedByUser);
  }
}

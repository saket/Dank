package me.saket.dank.ui.submission;

import android.content.ContentValues;
import android.database.Cursor;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.Moshi;

import net.dean.jraw.models.Submission;
import net.dean.jraw.models.VoteDirection;

import java.io.IOException;

import io.reactivex.functions.Function;

@AutoValue
public abstract class CachedSubmissionDeprecated {

  public static final String TABLE_NAME = "CachedSubmissionDeprecated";
  private static final String COLUMN_FULLNAME = "full_name";
  private static final String COLUMN_SUBMISSION_JSON = "submission_json";
  private static final String COLUMN_FOLDER = "sort_info";
  private static final String COLUMN_FETCH_TIME = "fetch_time";
  private static final String COLUMN_USER_VOTE_DIRECTION = "user_vote_direction";
  private static final String COLUMN_IS_SAVED_BY_USER = "is_saved_by_user";

  private static final int BOOLEAN_TRUE_INT = 1;
  private static final int BOOLEAN_FALSE_INT = 0;

  public static final String QUERY_CREATE_TABLE =
      "CREATE TABLE " + TABLE_NAME + " ("
          + COLUMN_FULLNAME + " TEXT NOT NULL, "
          + COLUMN_SUBMISSION_JSON + " TEXT NOT NULL, "
          + COLUMN_FOLDER + " TEXT NOT NULL, "
          + COLUMN_FETCH_TIME + " INTEGER NOT NULL, "
          + COLUMN_USER_VOTE_DIRECTION + " TEXT NOT NULL, "
          + COLUMN_IS_SAVED_BY_USER + " INTEGER NOT NULL, "
          + "PRIMARY KEY (" + COLUMN_FULLNAME + ", " + COLUMN_FOLDER + ")"
          + ")";

  public abstract String fullName();

  public abstract Submission submission();

  public abstract CachedSubmissionFolder folder();

  public abstract long fetchTimeMillis();

  // TODO: This doesn't look necessary. Submission already contains this?
  public abstract VoteDirection userVoteDirection();

  public abstract boolean isSavedByUser();

  public ContentValues toContentValues(Moshi moshi) {
    ContentValues contentValues = new ContentValues(6);
    contentValues.put(COLUMN_FULLNAME, fullName());
    contentValues.put(COLUMN_SUBMISSION_JSON, moshi.adapter(Submission.class).toJson(submission()));
    contentValues.put(COLUMN_FOLDER, folder().serialize());
    contentValues.put(COLUMN_FETCH_TIME, fetchTimeMillis());
    contentValues.put(COLUMN_USER_VOTE_DIRECTION, userVoteDirection().name());
    contentValues.put(COLUMN_IS_SAVED_BY_USER, isSavedByUser() ? BOOLEAN_TRUE_INT : BOOLEAN_FALSE_INT);
    return contentValues;
  }

  public static String constructQueryToGetAll(CachedSubmissionFolder folder) {
    return "SELECT * FROM " + TABLE_NAME
        + " WHERE " + COLUMN_FOLDER + " == '" + folder.serialize() + "'"
        + " ORDER BY " + COLUMN_FETCH_TIME + " ASC";
  }

  public static String constructWhere(CachedSubmissionFolder folder) {
    return COLUMN_FOLDER + " == '" + folder.serialize() + "'";
  }

  public static String constructQueryToGetLastSubmission(CachedSubmissionFolder folder) {
    return "SELECT * FROM " + TABLE_NAME
        + " WHERE " + COLUMN_FOLDER + " == '" + folder.serialize() + "'"
        + " ORDER BY " + COLUMN_FETCH_TIME + " DESC"
        + " LIMIT 1";
  }

  public static String constructQueryToGetCount(CachedSubmissionFolder folder) {
    return "SELECT Count(*) FROM " + TABLE_NAME
        + " WHERE " + COLUMN_FOLDER + " == '" + folder.serialize() + "'";
  }

  public static Function<Cursor, CachedSubmissionDeprecated> mapFromCursor(Moshi moshi) {
    return cursor -> {
      String fullName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FULLNAME));
      Submission submission = mapSubmissionFromCursor(moshi).apply(cursor);
      CachedSubmissionFolder folder = CachedSubmissionFolder.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FOLDER)));
      long fetchTimeMillis = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_FETCH_TIME));
      VoteDirection userVoteDirection = VoteDirection.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_VOTE_DIRECTION)));
      boolean isSavedByUser = BOOLEAN_TRUE_INT == cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_SAVED_BY_USER));
      return create(fullName, submission, folder, fetchTimeMillis, userVoteDirection, isSavedByUser);
    };
  }

  public static Function<Cursor, Submission> mapSubmissionFromCursor(Moshi moshi) {
    return cursor -> submissionFromJson(moshi, cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SUBMISSION_JSON)));
  }

  private static Submission submissionFromJson(Moshi moshi, String submissionJson) {
    try {
      return moshi.adapter(Submission.class).fromJson(submissionJson);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static CachedSubmissionDeprecated create(String fullName, Submission submission, CachedSubmissionFolder folder, long fetchTimeMillis,
      VoteDirection userVoteDirection, boolean isSavedByUser)
  {
    return new AutoValue_CachedSubmissionDeprecated(fullName, submission, folder, fetchTimeMillis, userVoteDirection, isSavedByUser);
  }
}

package me.saket.dank.ui.submission;

import android.content.ContentValues;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.JsonAdapter;

import net.dean.jraw.models.Submission;

/**
 * Cache for submission without comments. Enough to be displayed on the submission list screen.
 */
@AutoValue
public abstract class CachedSubmissionWithoutComments {

  public static final String TABLE_NAME = "CachedSubmissionWithoutComments";
  public static final String COLUMN_SUBMISSION_FULL_NAME = "submission_full_name";
  public static final String COLUMN_SUBMISSION_JSON = "submission_json";
  private static final String COLUMN_SAVE_TIME = "save_time";

  public static final String QUERY_CREATE_TABLE =
      "CREATE TABLE " + TABLE_NAME + " ("
          + COLUMN_SUBMISSION_FULL_NAME + " TEXT NOT NULL PRIMARY KEY, "
          + COLUMN_SUBMISSION_JSON + " TEXT NOT NULL, "
          + COLUMN_SAVE_TIME + " INTEGER NOT NULL"
          + ")";

  public abstract String submissionFullName();

  public abstract Submission submission();

  public abstract long saveTimeMillis();

  public static CachedSubmissionWithoutComments create(String submissionFullName, Submission submissionWithoutComments, long saveTimeMillis) {
    return new AutoValue_CachedSubmissionWithoutComments(submissionFullName, submissionWithoutComments, saveTimeMillis);
  }

  public ContentValues toContentValues(JsonAdapter<Submission> submissionJsonAdapter) {
    ContentValues values = new ContentValues(3);
    values.put(COLUMN_SUBMISSION_FULL_NAME, submissionFullName());
    values.put(COLUMN_SUBMISSION_JSON, submissionJsonAdapter.toJson(submission()));
    values.put(COLUMN_SAVE_TIME, saveTimeMillis());
    return values;
  }
}

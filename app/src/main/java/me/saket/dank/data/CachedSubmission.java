package me.saket.dank.data;

import android.content.ContentValues;
import android.database.Cursor;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.Moshi;

import net.dean.jraw.models.Submission;

import io.reactivex.functions.Function;
import me.saket.dank.utils.DankSubmissionRequest;

@AutoValue
public abstract class CachedSubmission {

  public static final String TABLE_NAME = "CachedSubmission";
  private static final String COLUMN_REQUEST_JSON = "request_json";
  private static final String COLUMN_JSON = "json";
  private static final String COLUMN_SAVE_TIME = "save_time";

  private static final int BOOLEAN_TRUE_INT = 1;
  private static final int BOOLEAN_FALSE_INT = 0;

  public static final String QUERY_CREATE_TABLE =
      "CREATE TABLE " + TABLE_NAME + " ("
          + COLUMN_REQUEST_JSON + " TEXT NOT NULL PRIMARY KEY, "
          + COLUMN_JSON + " TEXT NOT NULL, "
          + COLUMN_SAVE_TIME + " INTEGER NOT NULL"
          + ")";

  public static final String SELECT_BY_REQUEST_JSON =
      "SELECT * FROM " + TABLE_NAME
          + " WHERE " + COLUMN_REQUEST_JSON + " == ?";

  public abstract DankSubmissionRequest request();

  public abstract Submission submission();

  public abstract long saveTimeMillis();

  public static CachedSubmission create(DankSubmissionRequest request, Submission submission, long saveTimeMillis) {
    if (submission.getSuggestedSort() != null && submission.getSuggestedSort() != request.commentSort()) {
      new Exception("Suggested sort is different from request").printStackTrace();
    }
    return new AutoValue_CachedSubmission(request, submission, saveTimeMillis);
  }

  public ContentValues toContentValues(Moshi moshi) {
    ContentValues contentValues = new ContentValues(3);
    contentValues.put(COLUMN_REQUEST_JSON, moshi.adapter(DankSubmissionRequest.class).toJson(request()));
    contentValues.put(COLUMN_JSON, moshi.adapter(Submission.class).toJson(submission()));
    contentValues.put(COLUMN_SAVE_TIME, saveTimeMillis());
    return contentValues;
  }

  public static Function<Cursor, CachedSubmission> cursorMapper(Moshi moshi) {
    return cursor -> {
      String requestJson = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_REQUEST_JSON));
      String submissionJson = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_JSON));
      DankSubmissionRequest request = moshi.adapter(DankSubmissionRequest.class).fromJson(requestJson);
      Submission submission = moshi.adapter(Submission.class).fromJson(submissionJson);
      long saveTimeMillis = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_SAVE_TIME));
      return create(request, submission, saveTimeMillis);
    };
  }
}

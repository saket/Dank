package me.saket.dank.ui.submission;

import android.content.ContentValues;
import android.database.Cursor;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.Moshi;

import net.dean.jraw.models.Submission;

import io.reactivex.functions.Function;
import me.saket.dank.utils.Cursors;
import me.saket.dank.utils.DankSubmissionRequest;

@AutoValue
public abstract class CachedSubmissionWithComments {

  public static final String TABLE_NAME = "CachedSubmissionWithComments";
  private static final String COLUMN_REQUEST_JSON = "request_json";
  private static final String COLUMN_JSON = "json";
  private static final String COLUMN_UPDATE_TIME = "save_time";

  public static final String QUERY_CREATE_TABLE =
      "CREATE TABLE " + TABLE_NAME + " ("
          + COLUMN_REQUEST_JSON + " TEXT NOT NULL PRIMARY KEY, "
          + COLUMN_JSON + " TEXT NOT NULL, "
          + COLUMN_UPDATE_TIME + " INTEGER NOT NULL"
          + ")";

  public static final String SELECT_BY_REQUEST_JSON =
      "SELECT * FROM " + TABLE_NAME
          + " WHERE " + COLUMN_REQUEST_JSON + " == ?";

  public static final String WHERE_UPDATE_TIME_BEFORE = COLUMN_UPDATE_TIME + " < ?";
  public static final String SELECT_WHERE_UPDATE_TIME_BEFORE =
      "SELECT * FROM " + TABLE_NAME
          + " WHERE " + COLUMN_UPDATE_TIME + " < ?";

  public abstract DankSubmissionRequest request();

  public abstract Submission submission();

  public abstract long updateTimeMillis();

  public static CachedSubmissionWithComments create(DankSubmissionRequest request, Submission submission, long saveTimeMillis) {
    if (submission.getSuggestedSort() != null && submission.getSuggestedSort() != request.commentSort()) {
      new Exception("Suggested sort is different from request").printStackTrace();
    }
    return new AutoValue_CachedSubmissionWithComments(request, submission, saveTimeMillis);
  }

  public ContentValues toContentValues(Moshi moshi) {
    ContentValues contentValues = new ContentValues(3);
    contentValues.put(COLUMN_REQUEST_JSON, moshi.adapter(DankSubmissionRequest.class).toJson(request()));
    contentValues.put(COLUMN_JSON, moshi.adapter(Submission.class).toJson(submission()));
    contentValues.put(COLUMN_UPDATE_TIME, updateTimeMillis());
    return contentValues;
  }

  public static Function<Cursor, CachedSubmissionWithComments> cursorMapper(Moshi moshi) {
    return cursor -> {
      String requestJson = Cursors.string(cursor, COLUMN_REQUEST_JSON);
      String submissionJson = Cursors.string(cursor, COLUMN_JSON);
      DankSubmissionRequest request = moshi.adapter(DankSubmissionRequest.class).fromJson(requestJson);
      Submission submission = moshi.adapter(Submission.class).fromJson(submissionJson);
      long saveTimeMillis = Cursors.longg(cursor, COLUMN_UPDATE_TIME);
      //noinspection ConstantConditions
      return create(request, submission, saveTimeMillis);
    };
  }
}

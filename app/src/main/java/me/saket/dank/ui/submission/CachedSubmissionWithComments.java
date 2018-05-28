package me.saket.dank.ui.submission;

import com.google.auto.value.AutoValue;

@AutoValue
@Deprecated
public abstract class CachedSubmissionWithComments {

//  public static final String TABLE_NAME = "CachedSubmissionWithComments";
//  private static final String COLUMN_REQUEST_JSON = "request_json";
//  private static final String COLUMN_JSON = "json";
//  private static final String COLUMN_UPDATE_TIME = "save_time";
//
//  public static final String QUERY_CREATE_TABLE =
//      "CREATE TABLE " + TABLE_NAME + " ("
//          + COLUMN_REQUEST_JSON + " TEXT NOT NULL PRIMARY KEY, "
//          + COLUMN_JSON + " TEXT NOT NULL, "
//          + COLUMN_UPDATE_TIME + " INTEGER NOT NULL"
//          + ")";
//
//  public static final String SELECT_BY_REQUEST_JSON =
//      "SELECT * FROM " + TABLE_NAME
//          + " WHERE " + COLUMN_REQUEST_JSON + " == ?";
//
//  public static final String WHERE_REQUEST_JSON =
//      COLUMN_REQUEST_JSON + " == ?";
//
//  public static final String WHERE_UPDATE_TIME_BEFORE = COLUMN_UPDATE_TIME + " < ?";
//
//  public static final String SELECT_WHERE_UPDATE_TIME_BEFORE =
//      "SELECT * FROM " + TABLE_NAME
//          + " WHERE " + COLUMN_UPDATE_TIME + " < ?";
//
//  public abstract DankSubmissionRequest request();
//
//  public abstract Submission submission();
//
//  public abstract long updateTimeMillis();
//
//  public static CachedSubmissionWithComments create(DankSubmissionRequest request, Submission submission, long createTimeMillis) {
//    // Submission's suggested sort can be different from
//    return new AutoValue_CachedSubmissionWithComments(request, submission, createTimeMillis);
//  }
//
//  public ContentValues toContentValues(Moshi moshi) {
//    ContentValues contentValues = new ContentValues(3);
//    contentValues.put(COLUMN_REQUEST_JSON, moshi.adapter(DankSubmissionRequest.class).toJson(request()));
//    contentValues.put(COLUMN_JSON, moshi.adapter(Submission.class).toJson(submission()));
//    contentValues.put(COLUMN_UPDATE_TIME, updateTimeMillis());
//    return contentValues;
//  }
//
//  public static Function<Cursor, CachedSubmissionWithComments> cursorMapper(Moshi moshi) {
//    return cursor -> {
//      String requestJson = Cursors.string(cursor, COLUMN_REQUEST_JSON);
//      String submissionJson = Cursors.string(cursor, COLUMN_JSON);
//      DankSubmissionRequest request = moshi.adapter(DankSubmissionRequest.class).fromJson(requestJson);
//      Submission submission = moshi.adapter(Submission.class).fromJson(submissionJson);
//      long saveTimeMillis = Cursors.longg(cursor, COLUMN_UPDATE_TIME);
//      //noinspection ConstantConditions
//      return create(request, submission, saveTimeMillis);
//    };
//  }
}

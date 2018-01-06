package me.saket.dank.ui.submission;

import android.database.Cursor;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import net.dean.jraw.models.Submission;

import io.reactivex.functions.Function;
import me.saket.dank.utils.Cursors;

/**
 * Holds joined values of {@link CachedSubmissionList} and {@link CachedSubmissionWithoutComments} table rows.
 */
@AutoValue
public abstract class CachedSubmissionList {

  public abstract String submissionFullName();

  public abstract Submission submissionWithoutComments();

  public static CachedSubmissionList create(String submissionFullName, Submission submissionWithoutComments) {
    return new AutoValue_CachedSubmissionList(submissionFullName, submissionWithoutComments);
  }

  public static String constructQueryToGetAll(Moshi moshi, String subredditName, SortingAndTimePeriod sortingAndTimePeriod) {
    return queryToGetAll(moshi, subredditName, sortingAndTimePeriod, "ASC");
  }

  public static String constructQueryToGetLastSubmission(Moshi moshi, String subredditName, SortingAndTimePeriod sortingAndTimePeriod) {
    return queryToGetAll(moshi, subredditName, sortingAndTimePeriod, "DESC") + " LIMIT 1";
  }

  private static String queryToGetAll(Moshi moshi, String subredditName, SortingAndTimePeriod sortingAndTimePeriod, String sortOrder) {
    String sortingAndTimePeriodJson = moshi.adapter(SortingAndTimePeriod.class).toJson(sortingAndTimePeriod);

    String columns = CachedSubmissionId.TABLE_NAME + "." + CachedSubmissionId.COLUMN_SUBMISSION_FULL_NAME
        + ", " + CachedSubmissionWithoutComments.TABLE_NAME + "." + CachedSubmissionWithoutComments.COLUMN_SUBMISSION_JSON;

    return "SELECT " + columns + " FROM " + CachedSubmissionId.TABLE_NAME
        + " INNER JOIN " + CachedSubmissionWithoutComments.TABLE_NAME
        + " ON " + CachedSubmissionId.TABLE_NAME + "." + CachedSubmissionId.COLUMN_SUBMISSION_FULL_NAME + " = " + CachedSubmissionWithoutComments.TABLE_NAME + "." + CachedSubmissionWithoutComments.COLUMN_SUBMISSION_FULL_NAME
        + " WHERE " + CachedSubmissionId.TABLE_NAME + "." + CachedSubmissionId.COLUMN_SUBREDDIT_NAME + " == '" + subredditName + "'"
        + " AND " + CachedSubmissionId.TABLE_NAME + "." + CachedSubmissionId.COLUMN_SORTING_AND_TIME_PERIOD_JSON + " == '" + sortingAndTimePeriodJson + "'"
        + " ORDER BY " + CachedSubmissionId.TABLE_NAME + "." + CachedSubmissionId.COLUMN_SAVE_TIME + " " + sortOrder;
  }

  public static Function<Cursor, Submission> cursorMapper(JsonAdapter<Submission> submissionJsonAdapter) {
    return cursor -> {
      String submissionJson = Cursors.string(cursor, CachedSubmissionWithoutComments.COLUMN_SUBMISSION_JSON);
      return submissionJsonAdapter.fromJson(submissionJson);
    };
  }
}

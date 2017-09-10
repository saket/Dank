package me.saket.dank.data;

import android.support.annotation.CheckResult;

import com.squareup.moshi.Moshi;
import com.squareup.sqlbrite2.BriteDatabase;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;
import me.saket.dank.utils.DankSubmissionRequest;

@Singleton
public class SubmissionRepository {

  private final BriteDatabase briteDatabase;
  private final Moshi moshi;
  private final DankRedditClient dankRedditClient;

  @Inject
  public SubmissionRepository(BriteDatabase briteDatabase, Moshi moshi, DankRedditClient dankRedditClient) {
    this.briteDatabase = briteDatabase;
    this.moshi = moshi;
    this.dankRedditClient = dankRedditClient;
  }

  /**
   * Get from DB or from the network if not present in DB.
   */
  @CheckResult
  public Observable<CachedSubmission> submissionWithComments(DankSubmissionRequest submissionRequest) {
    String requestJson = moshi.adapter(DankSubmissionRequest.class).toJson(submissionRequest);

    return briteDatabase.createQuery(CachedSubmission.TABLE_NAME, CachedSubmission.SELECT_BY_REQUEST_JSON, requestJson)
        .mapToList(CachedSubmission.cursorMapper(moshi))
        .flatMap(cachedSubmissions -> {
          if (cachedSubmissions.isEmpty()) {
            return dankRedditClient.submission(submissionRequest)
                .flatMapObservable(submission -> {
                  long saveTimeMillis = System.currentTimeMillis();
                  CachedSubmission cachedSubmission = CachedSubmission.create(submissionRequest, submission, saveTimeMillis);
                  briteDatabase.insert(CachedSubmission.TABLE_NAME, cachedSubmission.toContentValues(moshi));

                  // Inserting an item into the DB will trigger another update, so terminate this flatmap stream here.
                  return Observable.empty();
                });

          } else {
            return Observable.just(cachedSubmissions.get(0));
          }
        });
  }
}

package me.saket.dank.ui.media;

import static java.lang.Integer.parseInt;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.CheckResult;

import com.jakewharton.rxrelay2.BehaviorRelay;
import com.jakewharton.rxrelay2.Relay;

import java.io.File;
import java.util.TimeZone;
import javax.inject.Inject;
import javax.inject.Singleton;

import hirondelle.date4j.DateTime;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import me.saket.dank.data.FileUploadProgressEvent;
import me.saket.dank.data.exceptions.ImgurApiRequestRateLimitReachedException;
import me.saket.dank.data.exceptions.ImgurApiUploadRateLimitReachedException;
import me.saket.dank.data.exceptions.InvalidImgurAlbumException;
import me.saket.dank.urlparser.ImgurAlbumUnresolvedLink;
import me.saket.dank.di.DankApi;
import me.saket.dank.utils.okhttp.OkHttpRequestBodyWithProgress;
import me.saket.dank.utils.okhttp.OkHttpRequestWriteProgressListener;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.HttpException;
import retrofit2.Response;
import timber.log.Timber;

/**
 * TODO: Tests.
 */
@Singleton
public class ImgurRepository {

  private static final String KEY_REQUEST_LIMIT = "requestsLimit";
  private static final String KEY_REMAINING_REQUESTS = "remainingRequests";
  private static final String KEY_UPLOAD_LIMIT = "uploadsLimit";
  private static final String KEY_REMAINING_UPLOADS = "remainingUploads";
  private static final String KEY_RATE_LIMIT_LAST_CHECK = "rateLimitsLastCheck";

  /**
   * Stop all Imgur requests once we're only left with 10% of our allocated requests.
   */
  private static final float LIMIT_THRESHOLD_FACTOR = 0.1f;

  private final SharedPreferences sharedPreferences;
  private final DankApi dankApi;

  @Inject
  public ImgurRepository(Application appContext, DankApi dankApi) {
    sharedPreferences = appContext.getSharedPreferences(appContext.getPackageName() + "_imgur_ratelimits", Context.MODE_PRIVATE);
    this.dankApi = dankApi;
  }

  /**
   * <p>
   * TODO: If needed, get the rate limits if they're not cached.
   * Remember to handle {@link ImgurApiRequestRateLimitReachedException}.
   *
   * @throws InvalidImgurAlbumException               If an invalid Imgur link was found. Right now this happens only when no images are
   *                                                  returned by Imgur.
   * @throws ImgurApiRequestRateLimitReachedException If Imgur's API limit is reached and no more API requests can be made till the next month.
   */
  public Single<ImgurResponse> gallery(ImgurAlbumUnresolvedLink imgurAlbumUnresolvedLink) {
    return dankApi.imgurAlbum(imgurAlbumUnresolvedLink.albumId())
        .map(throwIfHttpError())
        .doOnSuccess(saveImgurApiRateLimits())
        .map(response -> response.body())
        .onErrorResumeNext(error -> {
          // Api returns a 404 when it was a single image and not an album.
          if (error instanceof HttpException && ((HttpException) error).code() == 404) {
            return Single.just(ImgurAlbumResponse.createEmpty());
          } else {
            return Single.error(error);
          }
        })
        .flatMap(albumResponse -> {
          if (albumResponse.hasImages()) {
            return Single.just(albumResponse);

          } else {
            // Okay, let's check if it was a single image.
            return dankApi.imgurImage(imgurAlbumUnresolvedLink.albumId())
                .doOnSuccess(saveImgurApiRateLimits())
                .map(response -> response.body());
          }
        })
        .doOnSuccess(albumResponse -> {
          if (!albumResponse.hasImages()) {
            throw new InvalidImgurAlbumException();
          }
        })
        .doOnSubscribe(o -> {
          resetRateLimitsIfMonthChanged();

          if (isApiRequestLimitReached()) {
            throw new ImgurApiRequestRateLimitReachedException();
          } else {
            Timber.i("Rate limits not reached");
          }
        });
  }

  /**
   * Remember to handle {@link ImgurApiUploadRateLimitReachedException}.
   */
  @CheckResult
  public Observable<FileUploadProgressEvent<ImgurUploadResponse>> uploadImage(File imageFile, String mimeType) {
    Relay<Float> uploadProgressStream = BehaviorRelay.createDefault(0f);

    RequestBody requestBody = RequestBody.create(MediaType.parse(mimeType), imageFile);
    OkHttpRequestWriteProgressListener uploadProgressListener = (bytesRead, totalBytes) -> {
      float progress = (float) bytesRead / totalBytes;
      uploadProgressStream.accept(progress);
    };
    RequestBody requestBodyWithProgress = OkHttpRequestBodyWithProgress.wrap(requestBody, uploadProgressListener);
    MultipartBody.Part multipartBodyPart = MultipartBody.Part.createFormData("image", imageFile.getName(), requestBodyWithProgress);

    Observable<FileUploadProgressEvent<ImgurUploadResponse>> uploadStream = dankApi.uploadToImgur(multipartBodyPart, "file")
        .map(throwIfHttpError())
        .doOnSuccess(saveImgurApiRateLimits())
        .map(response -> response.body())
        .doOnSubscribe(o -> {
          resetRateLimitsIfMonthChanged();

          if (isApiUploadLimitReached()) {
            throw new ImgurApiUploadRateLimitReachedException();
          } else {
            Timber.i("Rate limits not reached");
          }
        })
        .map(response -> FileUploadProgressEvent.createUploaded(response))
        .toObservable();

    return uploadProgressStream
        .map(progress -> FileUploadProgressEvent.<ImgurUploadResponse>createInFlight(progress))
        .mergeWith(uploadStream);
  }

  private <T> Function<Response<T>, Response<T>> throwIfHttpError() {
    return response -> {
      if (response.isSuccessful()) {
        return response;
      } else {
        throw new HttpException(response);
      }
    };
  }

  private Consumer<Response> saveImgurApiRateLimits() {
    return response -> {
      Headers responseHeaders = response.headers();
      saveApiRequestLimit(parseInt(responseHeaders.get("X-RateLimit-Requests-Limit")));
      saveRemainingApiRequests(parseInt(responseHeaders.get("X-RateLimit-Requests-Remaining")));
      saveApiUploadLimit(parseInt(responseHeaders.get("X-RateLimit-Uploads-Limit")));
      saveRemainingApiUploads(parseInt(responseHeaders.get("X-RateLimit-Uploads-Remaining")));

      saveRateLimitsLastCheckedAt(responseHeaders.getDate("Date").getTime());
    };
  }

  private void saveRateLimitsLastCheckedAt(long lastCheckedMillis) {
    sharedPreferences.edit().putLong(KEY_RATE_LIMIT_LAST_CHECK, lastCheckedMillis).apply();
  }

  private long rateLimitsLastCheckedAtMillis(long valueIfEmpty) {
    return sharedPreferences.getLong(KEY_RATE_LIMIT_LAST_CHECK, valueIfEmpty);
  }

  private void resetRateLimitsIfMonthChanged() {
    long lastCheckedMillis = rateLimitsLastCheckedAtMillis(-1);
    if (lastCheckedMillis == -1) {
      return;
    }

    DateTime lastCheckedDateTime = DateTime.forInstant(lastCheckedMillis, TimeZone.getTimeZone("UTC"));
    DateTime nowDateTime = DateTime.now(TimeZone.getTimeZone("UTC"));

    Timber.i("Now month: %s, Last checked month: %s", nowDateTime.getMonth(), lastCheckedDateTime.getMonth());

    if (nowDateTime.getMonth() > lastCheckedDateTime.getMonth()) {
      Timber.i("Months have changed. Resetting Imgur rate limit");

      // Months have changed! Reset the limits.
      sharedPreferences.edit().clear().apply();
    }
  }

  private void saveRemainingApiRequests(int requestsRemaining) {
    sharedPreferences.edit().putInt(KEY_REMAINING_REQUESTS, requestsRemaining).apply();
  }

  private void saveApiRequestLimit(int requestLimit) {
    sharedPreferences.edit().putInt(KEY_REQUEST_LIMIT, requestLimit).apply();
  }

  private boolean isApiRequestLimitReached() {
    if (sharedPreferences.contains(KEY_REMAINING_REQUESTS) && sharedPreferences.contains(KEY_REQUEST_LIMIT)) {
      int remainingRequests = sharedPreferences.getInt(KEY_REMAINING_REQUESTS, 0);
      int requestLimit = sharedPreferences.getInt(KEY_REQUEST_LIMIT, 0);

      Timber.i("remainingRequests: %s", remainingRequests);
      Timber.i("requestLimit: %s", requestLimit);

      if (remainingRequests < requestLimit * LIMIT_THRESHOLD_FACTOR) {
        Timber.w("Imgur api request limit reached :(");
        return true;
      }
    }
    return false;
  }

  private void saveRemainingApiUploads(int uploadsRemaining) {
    sharedPreferences.edit().putInt(KEY_REMAINING_UPLOADS, uploadsRemaining).apply();
  }

  private void saveApiUploadLimit(int uploadLimit) {
    sharedPreferences.edit().putInt(KEY_UPLOAD_LIMIT, uploadLimit).apply();
  }

  private boolean isApiUploadLimitReached() {
    if (sharedPreferences.contains(KEY_REMAINING_UPLOADS) && sharedPreferences.contains(KEY_UPLOAD_LIMIT)) {
      int remainingUploads = sharedPreferences.getInt(KEY_REMAINING_UPLOADS, 0);
      int uploadLimit = sharedPreferences.getInt(KEY_UPLOAD_LIMIT, 0);

      if (remainingUploads < uploadLimit * LIMIT_THRESHOLD_FACTOR) {
        Timber.i("remainingUploads: %s", remainingUploads);
        Timber.i("uploadLimit: %s", uploadLimit);
        return true;
      }
    }
    return false;
  }
}

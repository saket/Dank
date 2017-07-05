package me.saket.dank.utils;

import static io.reactivex.Single.just;
import static java.lang.Integer.parseInt;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

import java.util.TimeZone;

import hirondelle.date4j.DateTime;
import io.reactivex.Single;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import me.saket.dank.data.ImgurAlbumResponse;
import me.saket.dank.data.ImgurResponse;
import me.saket.dank.data.MediaLink;
import me.saket.dank.data.exceptions.ImgurApiRateLimitReachedException;
import me.saket.dank.data.exceptions.InvalidImgurAlbumException;
import me.saket.dank.di.Dank;
import okhttp3.Headers;
import retrofit2.HttpException;
import retrofit2.Response;
import rx.exceptions.OnErrorThrowable;
import timber.log.Timber;

/**
 * TODO: Tests.
 */
public class ImgurManager {

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

  public ImgurManager(Context context) {
    sharedPreferences = context.getSharedPreferences(context.getPackageName() + "_imgur_ratelimits", Context.MODE_PRIVATE);
  }

  /**
   * TODO: Cache.
   * <p>
   * TODO: If needed, get the rate limits if they're not cached.
   *
   * @throws InvalidImgurAlbumException        If an invalid Imgur link was found.
   * @throws ImgurApiRateLimitReachedException If Imgur's API limit is reached and no more API requests can be made till the next month.
   */
  public Single<ImgurResponse> gallery(MediaLink.ImgurUnresolvedGallery imgurUnresolvedGalleryLink) {
    return Dank.api()
        .imgurAlbumPaid(imgurUnresolvedGalleryLink.albumId())
        .map(throwIfHttpError())
        .doOnSuccess(saveImgurApiRateLimits())
        .map(extractResponseBody())
        .onErrorResumeNext(error -> {
          if (error instanceof OnErrorThrowable) {
            error = error.getCause();
          }

          // Api returns a 404 when it was a single image and not an album.
          if (error instanceof HttpException && ((HttpException) error).code() == 404) {
            return just(ImgurAlbumResponse.createEmpty());
          } else {
            throw OnErrorThrowable.from(error);
          }
        })
        .flatMap(albumResponse -> {
          if (albumResponse.hasImages()) {
            return Single.just(albumResponse);

          } else {
            // Okay, let's check if it was a single image.
            return Dank.api()
                .imgurImagePaid(imgurUnresolvedGalleryLink.albumId())
                .doOnSuccess(saveImgurApiRateLimits())
                .map(extractResponseBody());
          }
        })
        .doOnSuccess(albumResponse -> {
          if (!albumResponse.hasImages()) {
            throw OnErrorThrowable.from(new InvalidImgurAlbumException());
          }
        })
        .doOnSubscribe(o -> {
          resetRateLimitsIfMonthChanged();

          if (isApiRequestLimitReached()) {
            throw new ImgurApiRateLimitReachedException();
          } else {
            Timber.i("Rate limits not reached");
          }
        });
  }

  private <T> Function<Response<T>, Response<T>> throwIfHttpError() {
    return response -> {
      if (response.isSuccessful()) {
        return response;
      } else {
        throw OnErrorThrowable.from(new HttpException(response));
      }
    };
  }

  @NonNull
  private <T extends ImgurResponse> io.reactivex.functions.Function<Response<T>, T> extractResponseBody() {
    return response -> response.body();
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

      if (remainingUploads > uploadLimit * LIMIT_THRESHOLD_FACTOR) {
        return true;
      }
    }
    return false;
  }
}

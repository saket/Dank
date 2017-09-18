package me.saket.dank.ui.subreddits;

import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.support.annotation.CheckResult;
import android.util.Pair;

import net.dean.jraw.models.CommentSort;
import net.dean.jraw.models.Submission;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import me.saket.dank.ui.submission.SubmissionRepository;
import me.saket.dank.utils.DankSubmissionRequest;
import timber.log.Timber;

/**
 * TODO: Block multiple in-flight requests.
 */
@Singleton
public class CachePreFiller {

  private SubmissionRepository submissionRepository;
  private ConnectivityManager connectivityManager;

  private enum PreFillThing {
    COMMENTS,
    IMAGES,
    VIDEOS,
    LINK_METADATA
  }

  private enum PreFillPreference {
    WIFI_ONLY,
    WIFI_OR_MOBILE_DATA,
    NEVER,
  }

  @Inject
  public CachePreFiller(SubmissionRepository submissionRepository, ConnectivityManager connectivityManager) {
    this.submissionRepository = submissionRepository;
    this.connectivityManager = connectivityManager;
  }

  public void preFillContent(List<Submission> submissions) {
    // TODO: 19/09/17
  }

  @CheckResult
  public Completable preFillComments(List<Submission> submissions) {
    return streamPreFillWindows(PreFillThing.COMMENTS)
        .doOnNext(o -> Timber.d("Starting prefetch for comments"))
        .doOnDispose(() -> Timber.i("Stopping prefetch"))
        .flatMap(o -> Observable.fromIterable(submissions))
        .subscribeOn(Schedulers.single())
        .map(submission -> {
          DankSubmissionRequest request = DankSubmissionRequest.builder(submission.getId())
              .commentSort(submission.getSuggestedSort() != null ? submission.getSuggestedSort() : CommentSort.TOP)
              .build();
          return Pair.create(submission, request);
        })
        .concatMap(pair -> {
          DankSubmissionRequest submissionRequest = pair.second;
          return submissionRepository.submissionWithComments(submissionRequest)
              .doOnSubscribe(submission -> Timber.i("Pre-fetching %s", pair.first.getTitle()))
              .take(1);
        })
        .onErrorResumeNext(error -> {
          Timber.e(error, "Error while pre-filling cache with comments");
          return Observable.empty();
        })
        .ignoreElements();
  }

  @CheckResult
  private Observable<Boolean> streamPreFillWindows(PreFillThing thing) {
    // TODO: Get from user preferences for PreFillThing.
    Observable<PreFillPreference> preferenceStream = Observable.just(PreFillPreference.WIFI_ONLY);
    Observable<NetworkInfo> networkInfoStream = streamNetworkInfo().doOnNext(o -> Timber.i("Network changed"));

    return Observable.combineLatest(preferenceStream, networkInfoStream, Pair::create)
        .filter(pair -> pair.first != PreFillPreference.NEVER)
        .map(pair -> satisfiesNetworkRequirement(pair.first, pair.second))
        .filter(satisfied -> satisfied);
  }

  @CheckResult
  private Observable<NetworkInfo> streamNetworkInfo() {
    return Observable.create(emitter -> {
      ConnectivityManager.NetworkCallback networkCallbacks = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(Network network) {
          emitter.onNext(connectivityManager.getActiveNetworkInfo());
        }
      };

      NetworkRequest networkRequest = new NetworkRequest.Builder()
          .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
          .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
          .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
          .build();
      // ConnectivityManager gives a callback with the default value right away.
      connectivityManager.registerNetworkCallback(networkRequest, networkCallbacks);
      emitter.setCancellable(() -> connectivityManager.unregisterNetworkCallback(networkCallbacks));
    });
  }

  private boolean satisfiesNetworkRequirement(PreFillPreference preference, NetworkInfo networkInfo) throws AssertionError {
    if (!networkInfo.isConnectedOrConnecting()) {
      return false;
    }

    Timber.d("--------------------------------");
    boolean isConnectedToWifi = networkInfo.getType() == ConnectivityManager.TYPE_WIFI;
    boolean isConnectedToMobileData = networkInfo.getType() == ConnectivityManager.TYPE_MOBILE;
    //Timber.i("preference: %s", preference);
    //Timber.i("isConnectedToWifi: %s", isConnectedToWifi);
    //Timber.i("isConnectedToMobileData: %s", isConnectedToMobileData);

    if (preference == PreFillPreference.WIFI_ONLY) {
      return isConnectedToWifi;
    }

    if (preference == PreFillPreference.WIFI_OR_MOBILE_DATA) {
      return isConnectedToMobileData || isConnectedToWifi;
    }

    throw new AssertionError("Unknown network type. PreFillPreference: " + preference + ", network type: " + networkInfo.getType());
  }
}

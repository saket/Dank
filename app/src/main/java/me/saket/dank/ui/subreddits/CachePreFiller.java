package me.saket.dank.ui.subreddits;

import android.app.Application;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.support.annotation.CheckResult;
import android.util.Pair;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.Target;

import net.dean.jraw.models.CommentSort;
import net.dean.jraw.models.Submission;
import net.dean.jraw.models.Thumbnails;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.functions.BiPredicate;
import me.saket.dank.R;
import me.saket.dank.data.links.ImgurAlbumLink;
import me.saket.dank.data.links.Link;
import me.saket.dank.data.links.MediaLink;
import me.saket.dank.ui.media.MediaHostRepository;
import me.saket.dank.ui.submission.SubmissionRepository;
import me.saket.dank.utils.DankSubmissionRequest;
import me.saket.dank.utils.UrlParser;
import timber.log.Timber;

@Singleton
public class CachePreFiller {

  private final Application appContext;
  private final SubmissionRepository submissionRepository;
  private final ConnectivityManager connectivityManager;
  private final MediaHostRepository mediaHostRepository;
  private final int thumbnailWidthForSubmissionAlbumLink;

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
  public CachePreFiller(Application appContext, SubmissionRepository submissionRepository, ConnectivityManager connectivityManager,
      MediaHostRepository mediaHostRepository)
  {
    this.appContext = appContext;
    this.submissionRepository = submissionRepository;
    this.connectivityManager = connectivityManager;
    this.mediaHostRepository = mediaHostRepository;

    thumbnailWidthForSubmissionAlbumLink = appContext.getResources().getDimensionPixelSize(R.dimen.submission_link_thumbnail_width_album);
  }

  /**
   * TODO: Block multiple in-flight requests.
   * TODO: Handle PreFillPreference.NEVER.
   * TODO: Skip already cached items.
   * TODO: Rename Link.Type.REDDIT_HOSTED to something else.
   */
  @CheckResult
  public Completable preFill(List<Submission> submissions, int deviceDisplayWidth) {
    Observable<Pair<Submission, Link>> submissionAndContentStream = Observable.fromIterable(submissions)
        .map(submission -> {
          Link contentLink = UrlParser.parse(submission.getUrl());
          return Pair.create(submission, contentLink);
        });

    Observable<PreFillPreference> preferenceStream = Observable.just(PreFillPreference.WIFI_ONLY);
    Observable<NetworkInfo> networkChangeStream = streamNetworkInfoChanges().doOnNext(o -> Timber.i("Network changed"));

    Timber.d("Pre-filling cache for %s submissions", submissions.size());

    Observable<Pair<PreFillPreference, NetworkInfo>> preferenceAndNetworkInfoStream = Observable.combineLatest(
        preferenceStream,
        networkChangeStream.distinctUntilChanged(networkInfoComparator()),
        Pair::create
    );

    // Images and GIFs.
    Observable<Object> imagePreFillStream = preferenceAndNetworkInfoStream
        .filter(pair -> satisfiesNetworkRequirement(pair.first, pair.second))
        .switchMap(o -> submissionAndContentStream
            .filter(pair -> pair.second.isImageOrGif() || pair.second.isMediaAlbum())
            .concatMap(pair -> {
              Submission submission = pair.first;
              MediaLink contentLink = (MediaLink) pair.second;

              //Timber.i("Caching image link: %s", submission.getTitle());
              return preFillSingleImageOrAlbum(submission, contentLink, deviceDisplayWidth)
                  .onErrorComplete()
                  .toObservable();
            })
        );

//    return preferenceAndNetworkInfoStream
//        .switchMap(pair -> {
//          PreFillPreference preFillPreference = pair.first;
//          NetworkInfo networkInfo = pair.second;
//
//          return submissionStream.concatMap(submission ->
//              preFillComments(submission, preFillPreference, networkInfo)
//                  .doOnSubscribe(o -> Timber.i("%s", submission.getTitle()))
//                  .andThen(preFillImage(submission, preFillPreference, networkInfo))
//                  //.doOnNext(preFillImageRequestStream)
//                  //.doOnNext(preFillCommentsRequestStream)
//                  //.doOnNext(preFillLinkMetadataRequestStream)
//                  //.doOnNext(preFillVideoRequestStream)
//                  .toObservable()
//          );
//        })
//        .doOnNext(o -> Timber.d("All pre-filled!"))
//        .doOnDispose(() -> Timber.i("Stopping pre-fill"))
//        .take(2)
//        .ignoreElements();

    return Observable.merge(imagePreFillStream, Observable.empty()).ignoreElements();
  }

  @CheckResult
  private Observable<NetworkInfo> streamNetworkInfoChanges() {
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
      emitter.setCancellable(() -> {
        Timber.d("Unregistering network callbacks");
        connectivityManager.unregisterNetworkCallback(networkCallbacks);
      });
    });
  }

  private BiPredicate<NetworkInfo, NetworkInfo> networkInfoComparator() {
    return (last, current) -> last.isConnectedOrConnecting() == current.isConnectedOrConnecting() && last.getType() == current.getType();
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

  private Completable preFillSingleImageOrAlbum(Submission submission, MediaLink mediaLink, int deviceDisplayWidth) {
    return mediaHostRepository.resolveActualLinkIfNeeded(mediaLink)
        .map(resolvedLink -> {
          switch (resolvedLink.type()) {
            case SINGLE_IMAGE_OR_GIF:
              Thumbnails redditSuppliedImages = submission.getThumbnails();
              String imageUrl = mediaHostRepository.findOptimizedQualityImageForDisplay(
                  redditSuppliedImages,
                  deviceDisplayWidth,
                  resolvedLink.lowQualityUrl()
              );
              return Collections.singletonList(imageUrl);

            // We cannot cache the entire album, but we can make the first image available right away.
            case MEDIA_ALBUM:
              if (!(resolvedLink instanceof ImgurAlbumLink)) {
                throw new UnsupportedOperationException();
              }
              String firstImageUrl = ((ImgurAlbumLink) resolvedLink).images().get(0).lowQualityUrl();
              String albumCoverImageUrl = mediaHostRepository.findOptimizedQualityImageForDisplay(
                  submission.getThumbnails(),
                  thumbnailWidthForSubmissionAlbumLink,
                  ((ImgurAlbumLink) resolvedLink).coverImageUrl()
              );
              return Arrays.asList(albumCoverImageUrl, firstImageUrl);

            default:
              throw new AssertionError();
          }
        })
        .flatMapCompletable(imageUrls -> Completable.fromAction(() -> {
          for (String imageUrl : imageUrls) {
            // Glide internally also maintains a queue, but we want to load them sequentially
            // ourselves so that this Rx chain can be canceled later when the subreddit changes.
            //final long startTime = System.currentTimeMillis();
            Glide.with(appContext)
                .downloadOnly()
                .load(imageUrl)
                .submit(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                .get();
            //Timber.i("Image downloaded: %s, time: %sms", imageUrl, System.currentTimeMillis() - startTime);
          }
        }));
  }

  private Completable preFillComments(Submission submission, PreFillPreference preFillPreference, NetworkInfo networkInfo) {
    if (satisfiesNetworkRequirement(preFillPreference, networkInfo)) {
      DankSubmissionRequest request = DankSubmissionRequest.builder(submission.getId())
          .commentSort(submission.getSuggestedSort() != null ? submission.getSuggestedSort() : CommentSort.TOP)
          .build();
      // TODO: Check in DB before making this expensive call.
      return submissionRepository.submissionWithComments(request)
          .take(1)
          .ignoreElements()
          .doOnComplete(() -> Timber.i("- comments pre-filled"));

    } else {
      return Completable.complete();
    }
  }

//
//  @CheckResult
//  private Observable<Boolean> streamPreFillWindows(PreFillThing thing) {
//    // TODO: Get from user preferences for PreFillThing.
//    Observable<PreFillPreference> preferenceStream = Observable.just(PreFillPreference.WIFI_ONLY);
//    Observable<NetworkInfo> networkInfoStream = streamNetworkInfoChanges().doOnNext(o -> Timber.i("Network changed"));
//
//    return Observable.combineLatest(preferenceStream, networkInfoStream, Pair::create)
//        .filter(pair -> pair.first != PreFillPreference.NEVER)
//        .map(pair -> satisfiesNetworkRequirement(pair.first, pair.second))
//        .filter(satisfied -> satisfied);
//  }
}

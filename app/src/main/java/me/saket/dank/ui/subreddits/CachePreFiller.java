package me.saket.dank.ui.subreddits;

import android.app.Application;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
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
import io.reactivex.functions.Predicate;
import me.saket.dank.R;
import me.saket.dank.data.LinkMetadataRepository;
import me.saket.dank.data.links.ImgurAlbumLink;
import me.saket.dank.data.links.Link;
import me.saket.dank.data.links.MediaLink;
import me.saket.dank.ui.media.MediaHostRepository;
import me.saket.dank.ui.submission.SubmissionLinkViewHolder;
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
  private final LinkMetadataRepository linkMetadataRepository;
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
      MediaHostRepository mediaHostRepository, LinkMetadataRepository linkMetadataRepository)
  {
    this.appContext = appContext;
    this.submissionRepository = submissionRepository;
    this.connectivityManager = connectivityManager;
    this.mediaHostRepository = mediaHostRepository;
    this.linkMetadataRepository = linkMetadataRepository;

    thumbnailWidthForSubmissionAlbumLink = appContext.getResources().getDimensionPixelSize(R.dimen.submission_link_thumbnail_width_album);
  }

  /**
   * TODO: Block multiple in-flight requests.
   * TODO: Skip already cached items.
   * TODO: Tests.
   */
  @CheckResult
  public Completable preFill(List<Submission> submissions, int deviceDisplayWidth) {
    Observable<Pair<Submission, Link>> submissionAndContentLinkStream = Observable.fromIterable(submissions)
        .map(submission -> {
          Link contentLink = UrlParser.parse(submission.getUrl());
          return Pair.create(submission, contentLink);
        });

    Observable<NetworkInfo> networkChangeStream = streamNetworkInfoChanges().distinctUntilChanged(networkInfoComparator());
    //Timber.d("Pre-filling cache for %s submissions", submissions.size());

    // Images and GIFs that couldn't be converted to videos.
    Observable imageCachePreFillStream = Observable.combineLatest(streamPreFillPreference(PreFillThing.IMAGES), networkChangeStream, Pair::create)
        .switchMap(preferenceAndNetworkInfo -> Observable.just(preferenceAndNetworkInfo)
            .filter(isCachingAllowed())
            .filter(satisfiesNetworkRequirement())
            .flatMap(o -> submissionAndContentLinkStream
                .filter(submissionContentAreImages())
                .concatMap(submissionAndLink -> preFillImageOrAlbum(submissionAndLink.first, (MediaLink) submissionAndLink.second, deviceDisplayWidth)
                    .onErrorComplete()
                    .toObservable())
            )
        );

    // Link metadata.
    Observable linkCacheFillStream = Observable.combineLatest(streamPreFillPreference(PreFillThing.LINK_METADATA), networkChangeStream, Pair::create)
        .switchMap(preferenceAndNetworkInfo -> Observable.just(preferenceAndNetworkInfo)
            .filter(isCachingAllowed())
            .filter(satisfiesNetworkRequirement())
            .flatMap(o -> submissionAndContentLinkStream
                .filter(submissionContentIsExternalLink())
                .concatMap(submissionAndLink -> preFillLinkMetadata(submissionAndLink.first, submissionAndLink.second)
                    .toObservable()
                    .onErrorResumeNext(Observable.empty())
                )
            )
        );

    // Comments.
    Observable commentCacheFillStream = Observable.combineLatest(streamPreFillPreference(PreFillThing.COMMENTS), networkChangeStream, Pair::create)
        .switchMap(pair -> Observable.just(pair)
            .filter(isCachingAllowed())
            .filter(satisfiesNetworkRequirement())
            .flatMap(o -> submissionAndContentLinkStream.concatMap(submissionAndLink -> preFillComment(submissionAndLink.first)
                //.doOnSubscribe(d -> Timber.i("Caching comments: %s", submissionAndLink.second.unparsedUrl()))
                .toObservable())
                .onErrorResumeNext(Observable.empty())
            )
        );

    return Observable.merge(imageCachePreFillStream, linkCacheFillStream, commentCacheFillStream).ignoreElements();
  }

  private Predicate<Pair<PreFillPreference, NetworkInfo>> isCachingAllowed() {
    return pair -> pair.first != PreFillPreference.NEVER;
  }

  private Predicate<Pair<PreFillPreference, NetworkInfo>> satisfiesNetworkRequirement() {
    return pair -> {
      PreFillPreference preFillPreference = pair.first;
      NetworkInfo networkInfo = pair.second;
      if (!networkInfo.isConnectedOrConnecting()) {
        return false;
      }

      Timber.d("--------------------------------");
      boolean isConnectedToWifi = networkInfo.getType() == ConnectivityManager.TYPE_WIFI;
      boolean isConnectedToMobileData = networkInfo.getType() == ConnectivityManager.TYPE_MOBILE;
      //Timber.i("preference: %s", preference);
      //Timber.i("isConnectedToWifi: %s", isConnectedToWifi);
      //Timber.i("isConnectedToMobileData: %s", isConnectedToMobileData);

      if (preFillPreference == PreFillPreference.WIFI_ONLY) {
        return isConnectedToWifi;
      }

      if (preFillPreference == PreFillPreference.WIFI_OR_MOBILE_DATA) {
        return isConnectedToMobileData || isConnectedToWifi;
      }

      throw new AssertionError("Unknown network type. PreFillPreference: " + preFillPreference + ", network type: " + networkInfo.getType());
    };
  }

  private Observable<PreFillPreference> streamPreFillPreference(PreFillThing images) {
    return Observable.just(PreFillPreference.WIFI_ONLY);
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

  @NonNull
  private Predicate<Pair<Submission, Link>> submissionContentAreImages() {
    return submissionAndLink -> submissionAndLink.second.isImageOrGif() || submissionAndLink.second.isMediaAlbum();
  }

  private Completable preFillImageOrAlbum(Submission submission, MediaLink mediaLink, int deviceDisplayWidth) {
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
        .flatMapCompletable(imageUrls -> downloadImages(imageUrls));
  }

  private Completable downloadImages(List<String> imageUrls) {
    return Completable.fromAction(() -> {
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
    });
  }

  @NonNull
  private Predicate<Pair<Submission, Link>> submissionContentIsExternalLink() {
    return submissionAndLink -> {
      Link contentLink = submissionAndLink.second;
      Submission submission = submissionAndLink.first;
      boolean isAnotherRedditPage = contentLink.isRedditPage() && !submission.isSelfPost();
      //noinspection ConstantConditions
      return (SubmissionLinkViewHolder.UNFURL_REDDIT_PAGES_AS_EXTERNAL_LINKS && isAnotherRedditPage) || contentLink.isExternal();
    };
  }

  private Completable preFillLinkMetadata(Submission submission, Link contentLink) {
    return linkMetadataRepository.unfurl(contentLink)
        .flatMapCompletable(linkMetadata -> {
          String faviconUrl = linkMetadata.faviconUrl();
          String thumbnailImageUrl = mediaHostRepository.findOptimizedQualityImageForDisplay(
              submission.getThumbnails(),
              thumbnailWidthForSubmissionAlbumLink,
              linkMetadata.imageUrl()
          );

          return downloadImages(Arrays.asList(thumbnailImageUrl, faviconUrl));
        });
  }

  private Completable preFillComment(Submission submission) {
    DankSubmissionRequest request = DankSubmissionRequest.builder(submission.getId())
        .commentSort(submission.getSuggestedSort() != null ? submission.getSuggestedSort() : CommentSort.TOP)
        .build();

    return submissionRepository.submissionWithComments(request)
        .take(1)
        .ignoreElements();
        //.doOnComplete(() -> Timber.i("- comments pre-filled"));
  }
}

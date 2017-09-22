package me.saket.dank.ui.subreddits;

import android.app.Application;
import android.net.ConnectivityManager;
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
import java.util.concurrent.Executors;
import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;
import me.saket.dank.R;
import me.saket.dank.data.CachePreFillNetworkPreference;
import me.saket.dank.data.CachePreFillThing;
import me.saket.dank.data.LinkMetadataRepository;
import me.saket.dank.data.UserPreferences;
import me.saket.dank.data.links.ImgurAlbumLink;
import me.saket.dank.data.links.Link;
import me.saket.dank.data.links.MediaLink;
import me.saket.dank.ui.media.MediaHostRepository;
import me.saket.dank.ui.submission.SubmissionLinkViewHolder;
import me.saket.dank.ui.submission.SubmissionRepository;
import me.saket.dank.utils.DankSubmissionRequest;
import me.saket.dank.utils.NetworkStateListener;
import me.saket.dank.utils.NetworkStateListener.NetworkState;
import me.saket.dank.utils.UrlParser;

@Singleton
public class CachePreFiller {

  private final Application appContext;
  private final SubmissionRepository submissionRepository;
  private final NetworkStateListener networkStateListener;
  private final MediaHostRepository mediaHostRepository;
  private final LinkMetadataRepository linkMetadataRepository;
  private final int thumbnailWidthForSubmissionAlbumLink;
  private final UserPreferences userPreferences;

  @Inject
  public CachePreFiller(Application appContext, SubmissionRepository submissionRepository, NetworkStateListener networkStateListener,
      MediaHostRepository mediaHostRepository, LinkMetadataRepository linkMetadataRepository, UserPreferences userPreferences)
  {
    this.appContext = appContext;
    this.submissionRepository = submissionRepository;
    this.networkStateListener = networkStateListener;
    this.mediaHostRepository = mediaHostRepository;
    this.linkMetadataRepository = linkMetadataRepository;
    this.userPreferences = userPreferences;

    thumbnailWidthForSubmissionAlbumLink = appContext.getResources().getDimensionPixelSize(R.dimen.submission_link_thumbnail_width_album);
  }

  /**
   * TODO: Block multiple in-flight requests.
   * TODO: Skip already cached items.
   * TODO: Tests.
   */
  @CheckResult
  public Completable preFillInParallelThreads(List<Submission> submissions, int deviceDisplayWidth) {
    Observable<Pair<Submission, Link>> submissionAndContentLinkStream = Observable.fromIterable(submissions)
        .map(submission -> {
          Link contentLink = UrlParser.parse(submission.getUrl());
          return Pair.create(submission, contentLink);
        });

    Observable<NetworkState> networkChangeStream = networkStateListener.streamNetworkStateChanges().distinctUntilChanged();
    //Timber.d("Pre-filling cache for %s submissions", submissions.size());

    Scheduler scheduler = Schedulers.from(Executors.newCachedThreadPool());

    // Images and GIFs that couldn't be converted to videos.
    Observable imageCachePreFillStream = Observable.combineLatest(streamPreFillPreference(CachePreFillThing.IMAGES), networkChangeStream, Pair::create)
        .switchMap(preferenceAndNetworkState -> Observable.just(preferenceAndNetworkState)
            .filter(isCachingAllowed())
            .filter(satisfiesNetworkRequirement())
            .flatMap(o -> submissionAndContentLinkStream
                .filter(submissionContentAreImages())
                .concatMap(submissionAndLink -> preFillImageOrAlbum(submissionAndLink.first, (MediaLink) submissionAndLink.second, deviceDisplayWidth)
                    .subscribeOn(scheduler)
                    //.doOnSubscribe(d -> Timber.i("Caching image: %s", submissionAndLink.first.getTitle()))
                    .onErrorComplete()
                    .toObservable())
            )
        );

    // Link metadata.
    Observable linkCacheFillStream = Observable.combineLatest(streamPreFillPreference(CachePreFillThing.LINK_METADATA), networkChangeStream, Pair::create)
        .switchMap(preferenceAndNetworkState -> Observable.just(preferenceAndNetworkState)
            .filter(isCachingAllowed())
            .filter(satisfiesNetworkRequirement())
            .flatMap(o -> submissionAndContentLinkStream
                .filter(submissionContentIsExternalLink())
                .concatMap(submissionAndLink -> preFillLinkMetadata(submissionAndLink.first, submissionAndLink.second)
                    .subscribeOn(scheduler)
                    //.doOnSubscribe(d -> Timber.i("Caching link: %s", submissionAndLink.first.getTitle()))
                    .toObservable()
                    .onErrorResumeNext(Observable.empty())
                )
            )
        );

    // Comments.
    Observable commentCacheFillStream = Observable.combineLatest(streamPreFillPreference(CachePreFillThing.COMMENTS), networkChangeStream, Pair::create)
        .switchMap(pair -> Observable.just(pair)
            .filter(isCachingAllowed())
            .filter(satisfiesNetworkRequirement())
            .flatMap(o -> submissionAndContentLinkStream.concatMap(submissionAndLink -> preFillComment(submissionAndLink.first)
                .subscribeOn(scheduler)
                //.doOnSubscribe(d -> Timber.i("Caching comments: %s", submissionAndLink.first.getTitle()))
                .toObservable())
                .onErrorResumeNext(Observable.empty())
            )
        );

    return Observable.merge(imageCachePreFillStream, linkCacheFillStream, commentCacheFillStream).ignoreElements();
  }

  private Predicate<Pair<CachePreFillNetworkPreference, NetworkState>> isCachingAllowed() {
    return pair -> pair.first != CachePreFillNetworkPreference.NEVER;
  }

  private Predicate<Pair<CachePreFillNetworkPreference, NetworkState>> satisfiesNetworkRequirement() {
    return pair -> {
      CachePreFillNetworkPreference preFillPreference = pair.first;
      NetworkState networkState = pair.second;
      if (!networkState.isConnectedOrConnectingToInternet()) {
        return false;
      }

      //Timber.d("--------------------------------");
      boolean isConnectedToWifi = networkState.networkType() == ConnectivityManager.TYPE_WIFI;
      boolean isConnectedToMobileData = networkState.networkType() == ConnectivityManager.TYPE_MOBILE;
      //Timber.i("preference: %s", preference);
      //Timber.i("isConnectedToWifi: %s", isConnectedToWifi);
      //Timber.i("isConnectedToMobileData: %s", isConnectedToMobileData);

      if (preFillPreference == CachePreFillNetworkPreference.WIFI_ONLY) {
        return isConnectedToWifi;
      }

      if (preFillPreference == CachePreFillNetworkPreference.WIFI_OR_MOBILE_DATA) {
        return isConnectedToMobileData || isConnectedToWifi;
      }

      throw new AssertionError("Unknown network type. PreFillPreference: " + preFillPreference + ", network state: " + networkState);
    };
  }

  private Observable<CachePreFillNetworkPreference> streamPreFillPreference(CachePreFillThing thing) {
    return userPreferences.cachePreFillNetworkPreference(thing);
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

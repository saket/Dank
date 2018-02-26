package me.saket.dank.ui.subreddit;

import android.app.Application;
import android.support.annotation.CheckResult;
import android.support.annotation.Px;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.f2prateek.rx.preferences2.Preference;

import net.dean.jraw.models.CommentSort;
import net.dean.jraw.models.Submission;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Lazy;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.functions.Predicate;
import me.saket.dank.data.CachePreFillThing;
import me.saket.dank.data.LinkMetadataRepository;
import me.saket.dank.data.NetworkStrategy;
import me.saket.dank.data.links.ImgurAlbumLink;
import me.saket.dank.data.links.Link;
import me.saket.dank.data.links.MediaLink;
import me.saket.dank.ui.media.MediaHostRepository;
import me.saket.dank.ui.submission.SubmissionImageHolder;
import me.saket.dank.ui.submission.SubmissionRepository;
import me.saket.dank.ui.submission.adapter.ImageWithMultipleVariants;
import me.saket.dank.ui.submission.adapter.SubmissionContentLinkUiConstructor;
import me.saket.dank.urlparser.UrlParser;
import me.saket.dank.utils.DankSubmissionRequest;
import me.saket.dank.utils.NetworkStateListener;
import me.saket.dank.utils.Optional;
import me.saket.dank.utils.Pair;
import me.saket.dank.utils.RxUtils;

/**
 * Pre-fetches submission content and comments.
 */
@Singleton
public class CachePreFiller {

  private static final int SUBMISSION_LIMIT_PER_SUBREDDIT = 30;

  private final Application appContext;
  private final SubmissionRepository submissionRepository;
  private final NetworkStateListener networkStateListener;
  private final MediaHostRepository mediaHostRepository;
  private final LinkMetadataRepository linkMetadataRepository;

  private final Lazy<Scheduler> preFillingScheduler;
  private final Lazy<Map<CachePreFillThing, Preference<NetworkStrategy>>> preFillingNetworkStrategies;
  private final Lazy<UrlParser> urlParser;

  // Key: <submission-fullname>_<CachePreFillThing>.
  private Set<String> completedPreFills = new HashSet<>(50);

  @Inject
  public CachePreFiller(
      Application appContext,
      SubmissionRepository submissionRepository,
      NetworkStateListener networkStateListener,
      MediaHostRepository mediaHostRepository,
      LinkMetadataRepository linkMetadataRepository,
      Lazy<UrlParser> urlParser,
      @Named("cache_pre_filling") Lazy<Scheduler> preFillingScheduler,
      @Named("cache_pre_filling_network_strategies") Lazy<Map<CachePreFillThing, Preference<NetworkStrategy>>> preFillingNetworkStrategies)
  {
    this.appContext = appContext;
    this.submissionRepository = submissionRepository;
    this.networkStateListener = networkStateListener;
    this.mediaHostRepository = mediaHostRepository;
    this.linkMetadataRepository = linkMetadataRepository;
    this.urlParser = urlParser;
    this.preFillingNetworkStrategies = preFillingNetworkStrategies;
    this.preFillingScheduler = preFillingScheduler;
  }

  @CheckResult
  public Completable preFillInParallelThreads(List<Submission> submissions, @Px int deviceDisplayWidth, @Px int submissionAlbumLinkThumbnailWidth) {
    // WARNING: this Observable is intentionally not shared to allow parallel execution of its subscribers.
    Observable<Pair<Submission, Link>> submissionAndContentLinkStream = Observable.fromIterable(submissions)
        .take(SUBMISSION_LIMIT_PER_SUBREDDIT)
        .map(submission -> {
          Link contentLink = urlParser.get().parse(submission.getUrl(), submission);
          return Pair.create(submission, contentLink);
        });

    //Timber.d("Pre-filling cache for %s submissions", submissions.size());

    // Images and GIFs that couldn't be converted to videos.
    Observable imageCachePreFillStream = preFillingNetworkStrategies.get().get(CachePreFillThing.IMAGES).asObservable()
        .flatMap(strategy -> networkStateListener.streamNetworkInternetCapability(strategy, Optional.empty()))
        .doOnNext(RxUtils.errorIfMainThread())
        .switchMap(canPreFill -> {
          if (!canPreFill) {
            // Cannot use filter() instead here so that switchMap() gets called and cancels the previous call.
            // Observable.empty() is also important so that the stream completes and the network state change
            // listener is freed.
            return Observable.empty();
          }

          return submissionAndContentLinkStream
              .filter(submissionContentAreStaticImages())
              .concatMap(submissionAndLink -> {
                Submission submission = submissionAndLink.first();
                MediaLink mediaLink = (MediaLink) submissionAndLink.second();
                return preFillImageOrAlbum(submission, mediaLink, deviceDisplayWidth, submissionAlbumLinkThumbnailWidth)
                    .subscribeOn(preFillingScheduler.get())
                    //.doOnSubscribe(d -> Timber.i("Caching image: %s", submissionAndLink.first().getTitle()))
                    .onErrorComplete()
                    .toObservable();
              });
        });

    // Link metadata.
    Observable linkCacheFillStream = preFillingNetworkStrategies.get().get(CachePreFillThing.LINK_METADATA).asObservable()
        .flatMap(strategy -> networkStateListener.streamNetworkInternetCapability(strategy, Optional.empty()))
        .switchMap(canPreFill -> {
          if (!canPreFill) {
            return Observable.empty();
          }

          return submissionAndContentLinkStream
              .filter(submissionContentIsExternalLink())
              .concatMap(
                  submissionAndLink -> preFillLinkMetadata(submissionAndLink.first(), submissionAndLink.second(), submissionAlbumLinkThumbnailWidth)
                      //.subscribeOn(preFillingScheduler.get())
                      //.doOnSubscribe(d -> Timber.i("Caching link: %s", submissionAndLink.first().getTitle()))
                      .toObservable()
                      .onErrorResumeNext(Observable.empty())
              );
        });

    // Comments.
    Observable commentCacheFillStream = preFillingNetworkStrategies.get().get(CachePreFillThing.COMMENTS).asObservable()
        .flatMap(strategy -> networkStateListener.streamNetworkInternetCapability(strategy, Optional.empty()))
        .switchMap(canPreFill -> {
          if (!canPreFill) {
            return Observable.empty();
          }

          return submissionAndContentLinkStream.concatMap(submissionAndLink -> preFillComment(submissionAndLink.first())
              .subscribeOn(preFillingScheduler.get())
              //.doOnSubscribe(d -> Timber.i("Caching comments: %s", submissionAndLink.first.getTitle()))
              .toObservable())
              .onErrorResumeNext(Observable.empty());
        });

    return Observable.merge(imageCachePreFillStream, linkCacheFillStream, commentCacheFillStream).ignoreElements();
  }

  private Predicate<Pair<Submission, Link>> submissionContentAreStaticImages() {
    //noinspection ConstantConditions
    return submissionAndLink -> submissionAndLink.second().isImage() || submissionAndLink.second().isMediaAlbum();
  }

  private Completable preFillImageOrAlbum(Submission submission, MediaLink mediaLink, int deviceDisplayWidth, int submissionAlbumLinkThumbnailWidth) {
    if (isThingAlreadyPreFilled(submission, CachePreFillThing.IMAGES)) {
      //Timber.i("Image skipping: %s", submission.getTitle());
      return Completable.complete();
    }

    //if (mediaLink instanceof ImgurAlbumLink) {
    //  Timber.i("Pre-filling image/album %s", submission.getTitle());
    //}
    return mediaHostRepository.resolveActualLinkIfNeeded(mediaLink)
        .map(resolvedLink -> {
          ImageWithMultipleVariants redditSuppliedImages = ImageWithMultipleVariants.of(submission.getThumbnails());
          switch (resolvedLink.type()) {
            case SINGLE_IMAGE:
              String imageUrl = redditSuppliedImages.findNearestFor(deviceDisplayWidth, resolvedLink.lowQualityUrl());
              return Collections.singletonList(imageUrl);

            // We cannot cache the entire album, but we can make the first image available right away.
            case MEDIA_ALBUM:
              if (!(resolvedLink instanceof ImgurAlbumLink) || !SubmissionImageHolder.LOAD_LOW_QUALITY_IMAGES) {
                throw new UnsupportedOperationException();
              }
              String firstImageUrl = ((ImgurAlbumLink) resolvedLink).images().get(0).lowQualityUrl();
              String albumCoverImageUrl = redditSuppliedImages.findNearestFor(
                  submissionAlbumLinkThumbnailWidth,
                  ((ImgurAlbumLink) resolvedLink).coverImageUrl()
              );
              return Arrays.asList(albumCoverImageUrl, firstImageUrl);

            default:
              throw new AssertionError();
          }
        })
        .flatMapCompletable(imageUrls -> downloadImages(imageUrls))
        //.doOnComplete(() -> Timber.i("Image done: %s", submission.getTitle()))
        .doOnComplete(() -> markThingAsPreFilled(submission, CachePreFillThing.IMAGES));
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
            .apply(RequestOptions.priorityOf(Priority.LOW))
            .submit(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
            .get();
        //Timber.i("Image downloaded: %s, time: %sms", imageUrl, System.currentTimeMillis() - startTime);
      }
    });
  }

  private Predicate<Pair<Submission, Link>> submissionContentIsExternalLink() {
    return submissionAndLink -> {
      Link contentLink = submissionAndLink.second();
      Submission submission = submissionAndLink.first();
      //noinspection ConstantConditions
      boolean isAnotherRedditPage = contentLink.isRedditPage() && !submission.isSelfPost();
      //noinspection ConstantConditions
      return (SubmissionContentLinkUiConstructor.UNFURL_REDDIT_PAGES_AS_EXTERNAL_LINKS && isAnotherRedditPage) || contentLink.isExternal();
    };
  }

  private Completable preFillLinkMetadata(Submission submission, Link contentLink, int submissionAlbumLinkThumbnailWidth) {
    if (isThingAlreadyPreFilled(submission, CachePreFillThing.LINK_METADATA)) {
      //Timber.i("Link skipping: %s", submission.getTitle());
      return Completable.complete();
    }

    return linkMetadataRepository.unfurl(contentLink)
        .flatMapCompletable(linkMetadata -> {
          List<String> imagesToDownload = new ArrayList<>(2);
          if (linkMetadata.hasFavicon()) {
            imagesToDownload.add(linkMetadata.faviconUrl());
          }
          //noinspection ConstantConditions
          if (linkMetadata.hasImage() && !UrlParser.isGifUrl(linkMetadata.imageUrl())) {
            ImageWithMultipleVariants redditSuppliedImages = ImageWithMultipleVariants.of(submission.getThumbnails());
            //noinspection ConstantConditions
            String thumbnailImageUrl = redditSuppliedImages.findNearestFor(submissionAlbumLinkThumbnailWidth, linkMetadata.imageUrl());
            imagesToDownload.add(thumbnailImageUrl);
          }
          return downloadImages(imagesToDownload);
        })
        //.doOnComplete(() -> Timber.i("Link done: %s", submission.getTitle()))
        .doOnComplete(() -> markThingAsPreFilled(submission, CachePreFillThing.LINK_METADATA));
  }

  private Completable preFillComment(Submission submission) {
    if (isThingAlreadyPreFilled(submission, CachePreFillThing.COMMENTS)) {
      //Timber.i("Comments skipping: %s", submission.getTitle());
      return Completable.complete();
    }

    DankSubmissionRequest request = DankSubmissionRequest.builder(submission.getId())
        .commentSort(submission.getSuggestedSort() != null ? submission.getSuggestedSort() : CommentSort.TOP)
        .build();

    return submissionRepository.submissionWithComments(request)
        .take(1)
        .ignoreElements()
        //.doOnComplete(() -> Timber.i("Comments done: %s", submission.getTitle()))
        .doOnComplete(() -> markThingAsPreFilled(submission, CachePreFillThing.COMMENTS));
  }

  private boolean isThingAlreadyPreFilled(Submission submission, CachePreFillThing thing) {
    return completedPreFills.contains(submission.getFullName() + "_" + thing.name());
  }

  private void markThingAsPreFilled(Submission submission, CachePreFillThing thing) {
    completedPreFills.add(submission.getFullName() + "_" + thing.name());
  }
}

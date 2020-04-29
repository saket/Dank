package me.saket.dank.cache;

import android.app.Application;
import android.graphics.drawable.Drawable;

import androidx.annotation.CheckResult;
import androidx.annotation.Px;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.request.RequestOptions;
import com.f2prateek.rx.preferences2.Preference;

import net.dean.jraw.models.Submission;

import java.util.ArrayList;
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
import io.reactivex.Single;
import io.reactivex.functions.Predicate;
import me.saket.dank.data.CachePreFillThing;
import me.saket.dank.data.LinkMetadataRepository;
import me.saket.dank.reddit.Reddit;
import me.saket.dank.ui.media.MediaHostRepository;
import me.saket.dank.ui.preferences.NetworkStrategy;
import me.saket.dank.ui.submission.AuditedCommentSort;
import me.saket.dank.ui.submission.AuditedCommentSort.SelectedBy;
import me.saket.dank.ui.submission.SubmissionImageLoader;
import me.saket.dank.ui.submission.SubmissionRepository;
import me.saket.dank.ui.submission.adapter.ImageWithMultipleVariants;
import me.saket.dank.ui.submission.adapter.SubmissionContentLinkUiConstructor;
import me.saket.dank.urlparser.ImgurAlbumLink;
import me.saket.dank.urlparser.ImgurLink;
import me.saket.dank.urlparser.Link;
import me.saket.dank.urlparser.MediaLink;
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
  private final Lazy<SubmissionImageLoader> submissionImageLoader;

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
      Lazy<SubmissionImageLoader> submissionImageLoader,
      @Named("cache_pre_filling") Lazy<Scheduler> preFillingScheduler,
      @Named("cache_pre_filling_network_strategies") Lazy<Map<CachePreFillThing, Preference<NetworkStrategy>>> preFillingNetworkStrategies)
  {
    this.appContext = appContext;
    this.submissionRepository = submissionRepository;
    this.networkStateListener = networkStateListener;
    this.mediaHostRepository = mediaHostRepository;
    this.linkMetadataRepository = linkMetadataRepository;
    this.urlParser = urlParser;
    this.submissionImageLoader = submissionImageLoader;
    this.preFillingNetworkStrategies = preFillingNetworkStrategies;
    this.preFillingScheduler = preFillingScheduler;
  }

  private void log(String message, Object... args) {
    //Timber.d(message, args);
  }

  @CheckResult
  public Completable preFillInParallelThreads(List<Submission> submissions, @Px int submissionAlbumLinkThumbnailWidth) {
    log("Pre-filling");

    // WARNING: this Observable is intentionally not shared to allow parallel execution of its subscribers.
    Observable<Pair<Submission, Link>> submissionAndContentLinkStream = Observable.fromIterable(submissions)
        .take(SUBMISSION_LIMIT_PER_SUBREDDIT)
        .map(submission -> {
          Link contentLink = urlParser.get().parse(submission.getUrl(), submission);
          return Pair.create(submission, contentLink);
        });

    // Images and GIFs that couldn't be converted to videos.
    Observable imageCachePreFillStream = preFillingNetworkStrategies.get().get(CachePreFillThing.IMAGES).asObservable()
        //.doOnNext(strategy -> Timber.i("Caching images strategy: %s", strategy))
        .flatMap(strategy -> networkStateListener.streamNetworkInternetCapability(strategy, Optional.empty()))
        //.doOnNext(canPreFill -> Timber.i("canPreFill: %s", canPreFill))
        .doOnNext(RxUtils.errorIfMainThread())
        .switchMap(canPreFill -> {
          if (!canPreFill) {
            // Cannot use filter() instead here so that switchMap() gets called and cancels the previous call.
            // Observable.empty() is also important so that the stream completes and the network state change
            // listener is freed.
            // Update: We're using never() and not empty() because otherwise caching never proceeds if
            // canPreFill is false on first attempt.
            //Timber.w("Cannot pre-fill images");
            return Observable.never();
          }

          log("Pre-filling images for %s submissions", submissions.size());

          return submissionAndContentLinkStream
              .filter(submissionContentAreStaticImages())
              .concatMap(submissionAndLink -> {
                Submission submission = submissionAndLink.first();
                MediaLink mediaLink = (MediaLink) submissionAndLink.second();
                return preFillImageOrAlbum(submission, mediaLink, submissionAlbumLinkThumbnailWidth)
                    .subscribeOn(preFillingScheduler.get())
                    //.doOnSubscribe(d -> log("Caching image: %s", submissionAndLink.first().getTitle()))
                    //.doOnComplete(() -> log("Cached image: %s", submissionAndLink.first().getTitle()))
                    .onErrorComplete()
                    .toObservable();
              });
        });

    // Link metadata.
    Observable linkCacheFillStream = preFillingNetworkStrategies.get().get(CachePreFillThing.LINK_METADATA).asObservable()
        .flatMap(strategy -> networkStateListener.streamNetworkInternetCapability(strategy, Optional.empty()))
        .switchMap(canPreFill -> {
          if (!canPreFill) {
            //Timber.w("Cannot pre-fill links");
            return Observable.never();
          }

          //log("Pre-filling links for %s submissions", submissions.size());

          return submissionAndContentLinkStream
              .filter(submissionContentIsExternalLink())
              .concatMap(
                  submissionAndLink -> preFillLinkMetadata(submissionAndLink.first(), submissionAndLink.second(), submissionAlbumLinkThumbnailWidth)
                      .subscribeOn(preFillingScheduler.get())
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
            //Timber.w("Cannot pre-fill comments");
            return Observable.never();
          }

          //log("Pre-filling comments for %s submissions", submissions.size());

          return submissionAndContentLinkStream.concatMap(submissionAndLink -> preFillComment(submissionAndLink.first())
              .subscribeOn(preFillingScheduler.get())
              //.doOnSubscribe(d -> Timber.i("Caching comments: %s", submissionAndLink.first().getTitle()))
              .toObservable())
              .onErrorResumeNext(Observable.empty());
        });

    return Observable.merge(imageCachePreFillStream, linkCacheFillStream, commentCacheFillStream).ignoreElements();
  }

  private Predicate<Pair<Submission, Link>> submissionContentAreStaticImages() {
    //noinspection ConstantConditions
    return submissionAndLink -> submissionAndLink.second().isImage() || submissionAndLink.second().isMediaAlbum();
  }

  private Completable preFillImageOrAlbum(Submission submission, MediaLink mediaLink, int submissionAlbumLinkThumbnailWidth) {
    if (isThingAlreadyPreFilled(submission, CachePreFillThing.IMAGES)) {
      log("Image skipping: %s", submission.getTitle());
      return Completable.complete();
    }

    // I considered using a Single here, but Single#filter() returns a Maybe, which isn't desired.
    Observable<MediaLink> replayedResolvedLinks = mediaHostRepository.resolveActualLinkIfNeeded(mediaLink)
        .take(1)
        .replay()
        .refCount();

    Observable<Object> checks = replayedResolvedLinks
        .flatMap(resolvedLink -> {
          if (resolvedLink.isImageOrGif() || resolvedLink.isMediaAlbum()) {
            return Observable.empty();
          } else {
            return Observable.error(new AssertionError("Unsupported link for image caching: " + resolvedLink));
          }
        });

    RequestOptions imageLoadOptions = RequestOptions.priorityOf(Priority.LOW);

    Observable<Drawable> singleImageLoad = replayedResolvedLinks
        .filter(resolvedLink -> resolvedLink.isImageOrGif())
        .flatMapSingle(resolvedLink -> submissionImageLoader.get().load(appContext, resolvedLink, submission.getPreview(), imageLoadOptions));

    Observable<Drawable> albumImagesLoad = replayedResolvedLinks
        .filter(resolvedLink -> resolvedLink.isMediaAlbum())
        .cast(ImgurAlbumLink.class)
        .flatMap(albumLink -> {
          ImgurLink firstImage = albumLink.images().get(0);
          Single<Drawable> firstImageLoad = submissionImageLoader.get().load(appContext, firstImage, imageLoadOptions);

          ImageWithMultipleVariants redditSuppliedImages = ImageWithMultipleVariants.Companion.of(submission.getPreview());
          String optimizedCoverImageUrl = redditSuppliedImages.findNearestFor(submissionAlbumLinkThumbnailWidth, albumLink.coverImageUrl());
          Single<Drawable> coverImageLoad = submissionImageLoader.get().loadImage(appContext, optimizedCoverImageUrl, imageLoadOptions);

          return coverImageLoad
              .mergeWith(firstImageLoad)
              .toObservable();
        });

    return Observable.merge(checks, singleImageLoad, albumImagesLoad)
        .ignoreElements()
        .doOnComplete(() -> log("Image done: %s", submission.getTitle()))
        .doOnComplete(() -> markThingAsPreFilled(submission, CachePreFillThing.IMAGES));
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
        .map(linkMetadata -> {
          List<String> imagesToDownload = new ArrayList<>(2);
          if (linkMetadata.hasFavicon()) {
            imagesToDownload.add(linkMetadata.getFaviconUrl());
          }
          if (linkMetadata.hasImage() && !UrlParser.isGifUrl(linkMetadata.getImageUrl())) {
            ImageWithMultipleVariants redditSuppliedImages = ImageWithMultipleVariants.Companion.of(submission.getPreview());
            //noinspection ConstantConditions
            String thumbnailImageUrl = redditSuppliedImages.findNearestFor(submissionAlbumLinkThumbnailWidth, linkMetadata.getImageUrl());
            imagesToDownload.add(thumbnailImageUrl);
          }
          return imagesToDownload;
        })
        .flatMapCompletable(imageUrls -> Completable.fromAction(() -> {
          for (String imageUrl : imageUrls) {
            // Glide internally also maintains a queue, but we want to load them sequentially
            // ourselves so that this Rx chain can be canceled later when the subreddit changes.
            Glide.with(appContext)
                .load(imageUrl)
                .submit()
                .get();
          }
        }))
        .doOnComplete(() -> log("Link done: %s", submission.getTitle()))
        .doOnComplete(() -> markThingAsPreFilled(submission, CachePreFillThing.LINK_METADATA));
  }

  private Completable preFillComment(Submission submission) {
    if (isThingAlreadyPreFilled(submission, CachePreFillThing.COMMENTS)) {
      //Timber.i("Comments skipping: %s", submission.getTitle());
      return Completable.complete();
    }

    AuditedCommentSort auditedSort = Optional.ofNullable(submission.getSuggestedSort())
        .map(sort -> AuditedCommentSort.create(sort, SelectedBy.SUBMISSION_SUGGESTED))
        .orElse(AuditedCommentSort.create(Reddit.Companion.getDEFAULT_COMMENT_SORT(), SelectedBy.DEFAULT));

    DankSubmissionRequest request = DankSubmissionRequest.builder(submission.getId())
        .commentSort(auditedSort)
        .build();

    return submissionRepository.submissionWithComments(request)
        .take(1)
        .ignoreElements()
        .onErrorComplete()
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

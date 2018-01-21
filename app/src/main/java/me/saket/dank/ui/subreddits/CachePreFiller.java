package me.saket.dank.ui.subreddits;

import android.app.Application;
import android.support.annotation.CheckResult;
import android.support.annotation.Px;
import android.support.v4.util.Pair;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;

import net.dean.jraw.models.CommentSort;
import net.dean.jraw.models.Submission;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;
import me.saket.dank.data.CachePreFillThing;
import me.saket.dank.data.LinkMetadataRepository;
import me.saket.dank.data.UserPreferences;
import me.saket.dank.data.links.ImgurAlbumLink;
import me.saket.dank.data.links.Link;
import me.saket.dank.data.links.MediaLink;
import me.saket.dank.ui.media.MediaHostRepository;
import me.saket.dank.ui.submission.SubmissionImageHolder;
import me.saket.dank.ui.submission.SubmissionRepository;
import me.saket.dank.ui.submission.adapter.ImageWithMultipleVariants;
import me.saket.dank.ui.submission.adapter.SubmissionContentLinkUiModelConstructor;
import me.saket.dank.utils.DankSubmissionRequest;
import me.saket.dank.utils.NetworkStateListener;
import me.saket.dank.utils.UrlParser;

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
  private final UserPreferences userPreferences;

  // Key: <submission-fullname>_<CachePreFillThing>.
  private Set<String> completedPreFills = new HashSet<>(50);

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
  }

  @CheckResult
  public Completable preFillInParallelThreads(List<Submission> submissions, @Px int deviceDisplayWidth, @Px int submissionAlbumLinkThumbnailWidth) {
    Observable<Pair<Submission, Link>> submissionAndContentLinkStream = Observable.fromIterable(submissions)
        .take(SUBMISSION_LIMIT_PER_SUBREDDIT)
        .map(submission -> {
          Link contentLink = UrlParser.parse(submission.getUrl());
          return Pair.create(submission, contentLink);
        });

    Scheduler scheduler = Schedulers.from(Executors.newCachedThreadPool());
    //Timber.d("Pre-filling cache for %s submissions", submissions.size());

    // Images and GIFs that couldn't be converted to videos.
    Observable imageCachePreFillStream = userPreferences.streamCachePreFillNetworkStrategy(CachePreFillThing.IMAGES)
        .flatMap(strategy -> networkStateListener.streamNetworkInternetCapability(strategy))
        .switchMap(canPreFill -> {
          if (!canPreFill) {
            // Cannot use filter() instead here so that switchMap() gets called and cancels the previous call.
            // Observable.empty() is also important so that the stream completes and the network state change
            // listener is freed.
            return Observable.empty();
          }

          return submissionAndContentLinkStream
              .filter(submissionContentAreImages())
              .concatMap(submissionAndLink -> {
                Submission submission = submissionAndLink.first;
                MediaLink mediaLink = (MediaLink) submissionAndLink.second;
                return preFillImageOrAlbum(submission, mediaLink, deviceDisplayWidth, submissionAlbumLinkThumbnailWidth)
                    .subscribeOn(scheduler)
                    //.doOnSubscribe(d -> Timber.i("Caching image: %s", submissionAndLink.first.getTitle()))
                    .onErrorComplete()
                    .toObservable();
              });
        });

    // Link metadata.
    Observable linkCacheFillStream = userPreferences.streamCachePreFillNetworkStrategy(CachePreFillThing.LINK_METADATA)
        .flatMap(strategy -> networkStateListener.streamNetworkInternetCapability(strategy))
        .switchMap(canPreFill -> {
          if (!canPreFill) {
            return Observable.empty();
          }

          return submissionAndContentLinkStream
              .filter(submissionContentIsExternalLink())
              .concatMap(
                  submissionAndLink -> preFillLinkMetadata(submissionAndLink.first, submissionAndLink.second, submissionAlbumLinkThumbnailWidth)
                      .subscribeOn(scheduler)
                      //.doOnSubscribe(d -> Timber.i("Caching link: %s", submissionAndLink.first.getTitle()))
                      .toObservable()
                      .onErrorResumeNext(Observable.empty())
              );
        });

    // Comments.
    Observable commentCacheFillStream = userPreferences.streamCachePreFillNetworkStrategy(CachePreFillThing.COMMENTS)
        .flatMap(strategy -> networkStateListener.streamNetworkInternetCapability(strategy))
        .switchMap(canPreFill -> {
          if (!canPreFill) {
            return Observable.empty();
          }

          return submissionAndContentLinkStream.concatMap(submissionAndLink -> preFillComment(submissionAndLink.first)
              .subscribeOn(scheduler)
              //.doOnSubscribe(d -> Timber.i("Caching comments: %s", submissionAndLink.first.getTitle()))
              .toObservable())
              .onErrorResumeNext(Observable.empty());
        });

    return Observable.merge(imageCachePreFillStream, linkCacheFillStream, commentCacheFillStream).ignoreElements();
  }

  private Predicate<Pair<Submission, Link>> submissionContentAreImages() {
    //noinspection ConstantConditions
    return submissionAndLink -> submissionAndLink.second.isImageOrGif() || submissionAndLink.second.isMediaAlbum();
  }

  private Completable preFillImageOrAlbum(Submission submission, MediaLink mediaLink, int deviceDisplayWidth, int submissionAlbumLinkThumbnailWidth) {
    if (isThingAlreadyPreFilled(submission, CachePreFillThing.IMAGES)) {
      //Timber.i("Image skipping: %s", submission.getTitle());
      return Completable.complete();
    }

    return mediaHostRepository.resolveActualLinkIfNeeded(mediaLink)
        .map(resolvedLink -> {
          ImageWithMultipleVariants redditSuppliedImages = ImageWithMultipleVariants.of(submission.getThumbnails());
          switch (resolvedLink.type()) {
            case SINGLE_IMAGE_OR_GIF:
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
      Link contentLink = submissionAndLink.second;
      Submission submission = submissionAndLink.first;
      //noinspection ConstantConditions
      boolean isAnotherRedditPage = contentLink.isRedditPage() && !submission.isSelfPost();
      //noinspection ConstantConditions
      return (SubmissionContentLinkUiModelConstructor.UNFURL_REDDIT_PAGES_AS_EXTERNAL_LINKS && isAnotherRedditPage) || contentLink.isExternal();
    };
  }

  private Completable preFillLinkMetadata(Submission submission, Link contentLink, int submissionAlbumLinkThumbnailWidth) {
    if (isThingAlreadyPreFilled(submission, CachePreFillThing.LINK_METADATA)) {
      //Timber.i("Link skipping: %s", submission.getTitle());
      return Completable.complete();
    }

    return linkMetadataRepository.unfurl(contentLink)
        .flatMapCompletable(linkMetadata -> {
          String faviconUrl = linkMetadata.faviconUrl();
          ImageWithMultipleVariants redditSuppliedImages = ImageWithMultipleVariants.of(submission.getThumbnails());
          String thumbnailImageUrl = redditSuppliedImages.findNearestFor(submissionAlbumLinkThumbnailWidth, linkMetadata.imageUrl());

          List<String> imagesToDownload = new ArrayList<>(2);
          if (faviconUrl != null) {
            imagesToDownload.add(faviconUrl);
          }
          if (thumbnailImageUrl != null) {
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

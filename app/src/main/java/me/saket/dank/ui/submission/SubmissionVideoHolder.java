package me.saket.dank.ui.submission;

import static io.reactivex.android.schedulers.AndroidSchedulers.mainThread;
import static io.reactivex.schedulers.Schedulers.io;

import android.graphics.Bitmap;
import android.support.annotation.CheckResult;
import android.view.View;
import android.widget.ProgressBar;

import com.danikula.videocache.HttpProxyCacheServer;
import com.devbrackets.android.exomedia.ui.widget.VideoView;
import com.f2prateek.rx.preferences2.Preference;
import com.jakewharton.rxbinding2.internal.Notification;
import com.jakewharton.rxrelay2.BehaviorRelay;
import com.jakewharton.rxrelay2.PublishRelay;

import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Named;

import dagger.Lazy;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Single;
import io.reactivex.functions.Function;
import me.saket.dank.data.NetworkStrategy;
import me.saket.dank.data.links.MediaLink;
import me.saket.dank.ui.media.MediaHostRepository;
import me.saket.dank.utils.ExoPlayerManager;
import me.saket.dank.utils.NetworkStateListener;
import me.saket.dank.utils.Optional;
import me.saket.dank.utils.VideoFormat;
import me.saket.dank.utils.Views;
import me.saket.dank.utils.lifecycle.LifecycleStreams;
import me.saket.dank.utils.lifecycle.ViewLifecycleEvent;
import me.saket.dank.widgets.InboxUI.ExpandablePageLayout;
import me.saket.dank.widgets.ScrollingRecyclerViewSheet;

/**
 * Manages loading of video in {@link SubmissionPageLayout}.
 */
public class SubmissionVideoHolder {

  private final PublishRelay<Integer> videoWidthChangeStream = PublishRelay.create();
  private final BehaviorRelay<Object> videoPreparedStream = BehaviorRelay.create();
  private final PublishRelay<Object> videoClickStream = PublishRelay.create();
  private final Lazy<MediaHostRepository> mediaHostRepository;
  private final Lazy<HttpProxyCacheServer> httpProxyCacheServer;
  private final Lazy<NetworkStateListener> networkStateListener;
  private final Lazy<Preference<NetworkStrategy>> highResolutionMediaNetworkStrategy;
  private final Lazy<Preference<NetworkStrategy>> autoPlayVideosNetworkStrategy;

  private ExoPlayerManager exoPlayerManager;
  private VideoView contentVideoView;
  private ScrollingRecyclerViewSheet commentListParentSheet;
  private ExpandablePageLayout submissionPageLayout;
  private SubmissionPageLifecycleStreams lifecycle;
  private int deviceDisplayHeight;
  private int statusBarHeight;
  private int minimumGapWithBottom;
  private ProgressBar contentLoadProgressView;

  @Inject
  public SubmissionVideoHolder(
      Lazy<MediaHostRepository> mediaHostRepository,
      Lazy<HttpProxyCacheServer> httpProxyCacheServer,
      Lazy<NetworkStateListener> networkStateListener,
      @Named("high_resolution_media_network_strategy") Lazy<Preference<NetworkStrategy>> highResolutionMediaNetworkStrategy,
      @Named("auto_play_videos_network_strategy") Lazy<Preference<NetworkStrategy>> autoPlayVideosNetworkStrategy)
  {
    this.mediaHostRepository = mediaHostRepository;
    this.httpProxyCacheServer = httpProxyCacheServer;
    this.networkStateListener = networkStateListener;
    this.highResolutionMediaNetworkStrategy = highResolutionMediaNetworkStrategy;
    this.autoPlayVideosNetworkStrategy = autoPlayVideosNetworkStrategy;
  }

  /**
   * <var>displayWidth</var> and <var>statusBarHeight</var> are used for capturing the video's bitmap,
   * which is in turn used for generating status bar tint. To minimize bitmap creation time, a Bitmap
   * of height equal to the status bar is created instead of the entire video height.
   *
   * @param minimumGapWithBottom The difference between video's bottom and the window's bottom.
   */
  public void setup(
      ExoPlayerManager exoPlayerManager,
      VideoView contentVideoView,
      ScrollingRecyclerViewSheet commentListParentSheet,
      ProgressBar contentLoadProgressView,
      ExpandablePageLayout submissionPageLayout,
      SubmissionPageLifecycleStreams lifecycle,
      int deviceDisplayHeight,
      int statusBarHeight,
      int minimumGapWithBottom)
  {
    this.exoPlayerManager = exoPlayerManager;
    this.contentVideoView = contentVideoView;
    this.commentListParentSheet = commentListParentSheet;
    this.contentLoadProgressView = contentLoadProgressView;
    this.submissionPageLayout = submissionPageLayout;
    this.lifecycle = lifecycle;
    this.deviceDisplayHeight = deviceDisplayHeight;
    this.statusBarHeight = statusBarHeight;
    this.minimumGapWithBottom = minimumGapWithBottom;
  }

  @CheckResult
  public Completable load(MediaLink mediaLink) {
    // Later hidden inside loadVideo(), when the video's height becomes available.
    contentLoadProgressView.setVisibility(View.VISIBLE);

    Single<Boolean> loadHighQualityVideoPredicate = highResolutionMediaNetworkStrategy.get()
        .asObservable()
        .flatMap(strategy -> networkStateListener.get().streamNetworkInternetCapability(strategy, Optional.empty()))
        .firstOrError();

    Completable autoPlayVideoIfAllowed = autoPlayVideosNetworkStrategy.get()
        .asObservable()
        .flatMap(strategy -> networkStateListener.get().streamNetworkInternetCapability(strategy, Optional.of(mainThread())))
        .firstOrError()
        .flatMap(waitTillInForeground())
        .flatMapCompletable(canAutoPlay -> canAutoPlay
            ? Completable.fromAction(() -> exoPlayerManager.startVideoPlayback())
            : Completable.complete());

    return mediaHostRepository.get().resolveActualLinkIfNeeded(mediaLink)
        .subscribeOn(io())
        .flatMap(resolvedLink -> loadHighQualityVideoPredicate
            .map(loadHQ -> loadHQ ? resolvedLink.highQualityUrl() : resolvedLink.lowQualityUrl()))
        .observeOn(mainThread())
        .flatMapCompletable(videoUrl -> loadVideo(videoUrl))
        .andThen(autoPlayVideoIfAllowed);
  }

  private <T> Function<T, Single<T>> waitTillInForeground() {
    return item -> lifecycle.replayedEvents()
        .take(1)
        .map(ev -> ev == ViewLifecycleEvent.STOP || ev == ViewLifecycleEvent.PAUSE)
        .flatMapCompletable(isInBackground -> isInBackground
            ? lifecycle.onStart().take(1).ignoreElements()
            : Completable.complete())
        .andThen(Single.just(item));
  }

  @CheckResult
  public Observable<Bitmap> streamVideoFirstFrameBitmaps() {
    return Observable.zip(videoPreparedStream, videoWidthChangeStream, (o, videoWidth) -> videoWidth)
        .delay(new Function<Integer, ObservableSource<Integer>>() {
          private boolean firstDelayDone;

          @Override
          public ObservableSource<Integer> apply(Integer videoWidth) throws Exception {
            if (firstDelayDone) {
              return Observable.just(videoWidth);
            } else {
              firstDelayDone = true;
              // Adding delay because onPrepared() gets called way too early when
              // loading a video for the first time.
              return Observable.just(videoWidth).delay(200, TimeUnit.MILLISECONDS);
            }
          }
        })
        .map(videoWidth -> exoPlayerManager.getBitmapOfCurrentVideoFrame(videoWidth, statusBarHeight, Bitmap.Config.RGB_565));
  }

  @CheckResult
  public Observable<Object> streamVideoClicks() {
    return videoClickStream;
  }

  private Completable loadVideo(String videoUrl) {
    return Completable.create(emitter -> {
      if (contentVideoView.getVideoControls() == null) {
        SubmissionVideoControlsView controlsView = new SubmissionVideoControlsView(contentVideoView.getContext());
        controlsView.setOnClickListener(o -> {
          videoClickStream.accept(LifecycleStreams.NOTHING);
        });
        contentVideoView.setControls(controlsView);
      }

      exoPlayerManager.setOnVideoSizeChangeListener((resizedVideoWidth, resizedVideoHeight, actualVideoWidth, actualVideoHeight) -> {
        //noinspection ConstantConditions
        int seekBarContainerHeight = ((SubmissionVideoControlsView) contentVideoView.getVideoControls()).heightOfControlButtons();

        // This height is independent of whether the content is resized by the keyboard.
        int spaceAvailableIndependentOfKeyboard = deviceDisplayHeight - statusBarHeight - minimumGapWithBottom - seekBarContainerHeight;
        int videoHeightAdjustedToFitInSpace = Math.min(spaceAvailableIndependentOfKeyboard, resizedVideoHeight + seekBarContainerHeight);
        Views.setHeight(contentVideoView, videoHeightAdjustedToFitInSpace);

        // Wait for the height change to happen and then reveal the video.
        Views.executeOnNextLayout(contentVideoView, () -> {
          contentLoadProgressView.setVisibility(View.GONE);

          // Warning: Avoid getting the height from view instead of reusing videoHeightAdjustedToFitInSpace.
          // I was seeing older values instead of the one passed to setHeight().
          int videoHeightMinusToolbar = videoHeightAdjustedToFitInSpace - commentListParentSheet.getTop();
          commentListParentSheet.setScrollingEnabled(true);
          commentListParentSheet.setMaxScrollY(videoHeightMinusToolbar);

          if (submissionPageLayout.isExpanded()) {
            commentListParentSheet.smoothScrollTo(videoHeightMinusToolbar);
          } else {
            commentListParentSheet.scrollTo(videoHeightMinusToolbar);
          }

          exoPlayerManager.setOnVideoSizeChangeListener(null);
          videoWidthChangeStream.accept(actualVideoWidth);
        });
      });

      VideoFormat videoFormat = VideoFormat.parse(videoUrl);
      if (videoFormat.canBeCached()) {
        String cachedVideoUrl = httpProxyCacheServer.get().getProxyUrl(videoUrl);
        exoPlayerManager.setVideoUriToPlayInLoop(cachedVideoUrl, videoFormat);
      } else {
        exoPlayerManager.setVideoUriToPlayInLoop(videoUrl, videoFormat);
      }

      contentVideoView.setOnPreparedListener(() -> {
        emitter.onComplete();
        videoPreparedStream.accept(Notification.INSTANCE);
      });
      exoPlayerManager.setOnErrorListener(e -> emitter.onError(e));
    });
  }

  public void pausePlayback() {
    exoPlayerManager.pauseVideoPlayback();
  }
}

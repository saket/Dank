package me.saket.dank.ui.submission;

import android.graphics.Bitmap;
import android.util.Size;

import androidx.annotation.CheckResult;

import com.danikula.videocache.HttpProxyCacheServer;
import com.devbrackets.android.exomedia.ui.widget.VideoControls;
import com.devbrackets.android.exomedia.ui.widget.VideoView;
import com.f2prateek.rx.preferences2.Preference;
import com.jakewharton.rxbinding2.internal.Notification;
import com.jakewharton.rxrelay2.BehaviorRelay;
import com.jakewharton.rxrelay2.PublishRelay;
import com.jakewharton.rxrelay2.Relay;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import dagger.Lazy;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Single;
import io.reactivex.functions.Function;
import me.saket.dank.ui.UiEvent;
import me.saket.dank.ui.preferences.NetworkStrategy;
import me.saket.dank.ui.submission.events.SubmissionVideoLoadStarted;
import me.saket.dank.ui.submission.events.SubmissionVideoLoadSucceeded;
import me.saket.dank.urlparser.MediaLink;
import me.saket.dank.utils.ExoPlayerManager;
import me.saket.dank.utils.NetworkStateListener;
import me.saket.dank.utils.Optional;
import me.saket.dank.utils.VideoFormat;
import me.saket.dank.utils.Views;
import me.saket.dank.utils.lifecycle.ViewLifecycleEvent;
import me.saket.dank.widgets.ScrollingRecyclerViewSheet;

import static io.reactivex.android.schedulers.AndroidSchedulers.mainThread;

/**
 * Manages loading of video in {@link SubmissionPageLayout}.
 */
public class SubmissionVideoHolder {

  private final PublishRelay<Integer> videoWidthChangeStream = PublishRelay.create();
  private final BehaviorRelay<Object> videoPreparedStream = BehaviorRelay.create();
  private final PublishRelay<SubmissionVideoClickEvent> videoClickStream = PublishRelay.create();
  private final Lazy<HttpProxyCacheServer> httpProxyCacheServer;
  private final Lazy<NetworkStateListener> networkStateListener;
  private final Lazy<Preference<NetworkStrategy>> hdMediaNetworkStrategy;
  private final Lazy<Preference<NetworkStrategy>> autoPlayVideosNetworkStrategy;

  private Relay<UiEvent> uiEvents;
  private ExoPlayerManager exoPlayerManager;
  private VideoView contentVideoView;
  private ScrollingRecyclerViewSheet commentListParentSheet;
  private SubmissionPageLayout submissionPageLayout;
  private SubmissionPageLifecycleStreams lifecycle;
  private Size deviceDisplaySize;
  private int statusBarHeight;
  private int minimumGapWithBottom;

  @Inject
  public SubmissionVideoHolder(
      Lazy<HttpProxyCacheServer> httpProxyCacheServer,
      Lazy<NetworkStateListener> networkStateListener,
      @Named("hd_media_in_submissions") Lazy<Preference<NetworkStrategy>> hdMediaNetworkStrategy,
      @Named("auto_play_videos") Lazy<Preference<NetworkStrategy>> autoPlayVideosNetworkStrategy)
  {
    this.httpProxyCacheServer = httpProxyCacheServer;
    this.networkStateListener = networkStateListener;
    this.hdMediaNetworkStrategy = hdMediaNetworkStrategy;
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
      Relay<UiEvent> uiEvents,
      ExoPlayerManager exoPlayerManager,
      VideoView contentVideoView,
      ScrollingRecyclerViewSheet commentListParentSheet,
      SubmissionPageLayout submissionPageLayout,
      SubmissionPageLifecycleStreams lifecycle,
      Size deviceDisplaySize,
      int statusBarHeight,
      int minimumGapWithBottom)
  {
    this.uiEvents = uiEvents;
    this.exoPlayerManager = exoPlayerManager;
    this.contentVideoView = contentVideoView;
    this.commentListParentSheet = commentListParentSheet;
    this.submissionPageLayout = submissionPageLayout;
    this.lifecycle = lifecycle;
    this.deviceDisplaySize = deviceDisplaySize;
    this.statusBarHeight = statusBarHeight;
    this.minimumGapWithBottom = minimumGapWithBottom;
  }

  @CheckResult
  public Completable load(MediaLink mediaLink) {
    // Later hidden inside loadVideo(), when the video's height becomes available.
    uiEvents.accept(SubmissionVideoLoadStarted.create());

    Single<Boolean> canLoadHighQualityVideos = hdMediaNetworkStrategy.get()
        .asObservable()
        .flatMap(strategy -> networkStateListener.get().streamNetworkInternetCapability(strategy, Optional.empty()))
        .firstOrError();

    Completable autoPlayVideoIfAllowed = autoPlayVideosNetworkStrategy.get()
        .asObservable()
        .flatMap(strategy -> networkStateListener.get().streamNetworkInternetCapability(strategy, Optional.of(mainThread())))
        .firstOrError()
        .flatMap(waitTillInForeground())
        .flatMapCompletable(canAutoPlay -> canAutoPlay
            ? Completable.fromAction(() -> exoPlayerManager.startPlayback())
            : Completable.complete());

    // FIXME: SubmissionPageLayout is already resolving actual link. Why do it again here?
    return canLoadHighQualityVideos
        .map(loadHQ -> loadHQ ? mediaLink.highQualityUrl() : mediaLink.lowQualityUrl())
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
          public ObservableSource<Integer> apply(Integer videoWidth) {
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
  public Observable<SubmissionVideoClickEvent> streamVideoClicks() {
    return videoClickStream;
  }

  private Completable loadVideo(String videoUrl) {
    return Completable.create(emitter -> {
      if (contentVideoView.getVideoControls() == null) {
        SubmissionVideoControlsView controlsView = new SubmissionVideoControlsView(contentVideoView.getContext());
        controlsView.setOnClickListener(o ->
            videoClickStream.accept(SubmissionVideoClickEvent.create(exoPlayerManager.getCurrentSeekPosition()))
        );
        contentVideoView.setControls(controlsView);
      }

      resetPlayIcon();

      exoPlayerManager.setOnVideoSizeChangeListener((actualVideoWidth, actualVideoHeight) -> {
        //noinspection ConstantConditions
        int seekBarContainerHeight = ((SubmissionVideoControlsView) contentVideoView.getVideoControls()).heightOfControlButtons();

        //Timber.d("-------------------------------");
        //Timber.i("resizedHeight: %s", resizedHeight);
        //Timber.i("actualVideoHeight: %s", actualVideoHeight);
        //Timber.i("seekBarContainerHeight: %s", seekBarContainerHeight);

        float widthResizeFactor = (float) deviceDisplaySize.getWidth() / actualVideoWidth;
        int resizedHeight = (int) (widthResizeFactor * actualVideoHeight);

        // This height is independent of whether the content is resized by the keyboard.
        int spaceAvailableIndependentOfKeyboard = deviceDisplaySize.getHeight() - statusBarHeight - minimumGapWithBottom;
        int heightAdjustedToFitInSpace = Math.min(spaceAvailableIndependentOfKeyboard, resizedHeight + seekBarContainerHeight);
        Views.setHeight(contentVideoView, heightAdjustedToFitInSpace);

        //Timber.i("spaceAvailableIndependentOfKeyboard: %s", spaceAvailableIndependentOfKeyboard);
        //Timber.i("heightAdjustedToFitInSpace: %s", heightAdjustedToFitInSpace);

        // Wait for the height change to happen and then reveal the video.
        Views.executeOnNextLayout(contentVideoView, () -> {
          uiEvents.accept(SubmissionVideoLoadSucceeded.create());

          // Warning: Avoid getting the height from view instead of reusing heightAdjustedToFitInSpace.
          // I was seeing older values instead of the one passed to setHeight().
          int videoHeightMinusToolbar = heightAdjustedToFitInSpace - commentListParentSheet.getTop();
          commentListParentSheet.setScrollingEnabled(true);
          commentListParentSheet.setMaxScrollY(videoHeightMinusToolbar);

          if (submissionPageLayout.shouldExpandMediaSmoothly()) {
            if (submissionPageLayout.isExpanded()) {
              commentListParentSheet.smoothScrollTo(videoHeightMinusToolbar);
            } else {
              lifecycle.onPageExpand()
                  .take(1)
                  .takeUntil(lifecycle.onPageCollapseOrDestroy())
                  .subscribe(o -> commentListParentSheet.smoothScrollTo(videoHeightMinusToolbar));
            }
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

      emitter.setCancellable(() -> exoPlayerManager.setOnVideoSizeChangeListener(null));
    });
  }

  private void resetPlayIcon() {
    VideoControls controlsView = contentVideoView.getVideoControls();
    //noinspection ConstantConditions
    controlsView.updatePlayPauseImage(false);
  }

  public void resetPlayback() {
    exoPlayerManager.resetPlayback();
  }
}

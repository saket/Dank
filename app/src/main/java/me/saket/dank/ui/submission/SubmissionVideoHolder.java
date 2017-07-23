package me.saket.dank.ui.submission;

import static me.saket.dank.utils.RxUtils.applySchedulersSingle;
import static me.saket.dank.utils.RxUtils.logError;

import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.CheckResult;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.devbrackets.android.exomedia.ui.widget.VideoView;
import com.jakewharton.rxbinding2.internal.Notification;
import com.jakewharton.rxrelay2.BehaviorRelay;
import com.jakewharton.rxrelay2.PublishRelay;
import com.jakewharton.rxrelay2.Relay;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Single;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import me.saket.dank.data.MediaLink;
import me.saket.dank.di.Dank;
import me.saket.dank.utils.ExoPlayerManager;
import me.saket.dank.utils.Views;
import me.saket.dank.widgets.DankVideoControlsView;
import me.saket.dank.widgets.InboxUI.ExpandablePageLayout;
import me.saket.dank.widgets.ScrollingRecyclerViewSheet;

/**
 * Manages loading of video in {@link SubmissionFragment}.
 */
public class SubmissionVideoHolder {

  private final ViewGroup contentVideoViewContainer;
  private final VideoView contentVideoView;
  private final ScrollingRecyclerViewSheet commentListParentSheet;

  private final ExpandablePageLayout submissionPageLayout;
  private final ExoPlayerManager exoPlayerManager;
  private final int deviceDisplayHeight;
  private final int minimumGapWithBottom;
  private final ProgressBar contentLoadProgressView;
  private final Relay<Integer> videoWidthChangeStream = PublishRelay.create();
  private final Relay<Object> videoPreparedStream = BehaviorRelay.create();

  /**
   * <var>displayWidth</var> and <var>statusBarHeight</var> are used for capturing the video's bitmap,
   * which is in turn used for generating status bar tint. To minimize bitmap creation time, a Bitmap
   * of height statusBarHeight is created instead of the entire video height.
   *
   * @param deviceDisplayHeight
   * @param minimumGapWithBottom The difference between video's bottom and the window's bottom will
   */
  public SubmissionVideoHolder(ViewGroup contentVideoViewContainer, VideoView contentVideoView, ScrollingRecyclerViewSheet commentListParentSheet,
      ProgressBar contentLoadProgressView, ExpandablePageLayout submissionPageLayout, ExoPlayerManager exoPlayerManager,
      int deviceDisplayHeight, int minimumGapWithBottom)
  {
    this.contentVideoViewContainer = contentVideoViewContainer;
    this.contentVideoView = contentVideoView;
    this.commentListParentSheet = commentListParentSheet;
    this.submissionPageLayout = submissionPageLayout;
    this.contentLoadProgressView = contentLoadProgressView;
    this.exoPlayerManager = exoPlayerManager;
    this.deviceDisplayHeight = deviceDisplayHeight;
    this.minimumGapWithBottom = minimumGapWithBottom;

    DankVideoControlsView controlsView = new DankVideoControlsView(contentVideoView.getContext());
    contentVideoView.setControls(controlsView);
    contentVideoView.setOnPreparedListener(() -> videoPreparedStream.accept(Notification.INSTANCE));
  }

  public Disposable load(MediaLink mediaLink) {
    Single<? extends MediaLink> videoUrlObservable = mediaLink instanceof MediaLink.StreamableUnknown
        ? getStreamableVideoDetails(((MediaLink.StreamableUnknown) mediaLink))
        : Single.just(mediaLink);

    return videoUrlObservable
        .doOnSubscribe(__ -> contentLoadProgressView.setVisibility(View.VISIBLE))
        .map(link -> link.lowQualityVideoUrl())
        .subscribe(loadVideo(), logError("Couldn't load video"));
    // TODO: 01/04/17 Handle error.
  }

  /**
   * <var>displayWidth</var> and <var>statusBarHeight</var> are used for creating a bitmap for capturing
   * the video's first frame. Creating a bitmap is expensive so we'll
   */
  @CheckResult
  public Observable<Bitmap> streamVideoFirstFrameBitmaps(int statusBarHeight) {
    return Observable
        .zip(videoPreparedStream, videoWidthChangeStream, (o, videoWidth) -> videoWidth)
        .delay(new Function<Integer, ObservableSource<Integer>>() {
          private boolean firstDelayDone;

          @Override
          public ObservableSource<Integer> apply(@NonNull Integer videoWidth) throws Exception {
            // onPrepared() gets called way too early when loading a video for the first time.
            // We'll manually add a delay.
            if (firstDelayDone) {
              return Observable.just(videoWidth);
            } else {
              firstDelayDone = true;
              return Observable.just(videoWidth).delay(200, TimeUnit.MILLISECONDS);
            }
          }
        })
        .map(videoWidth -> exoPlayerManager.getBitmapOfCurrentVideoFrame(videoWidth, statusBarHeight, Bitmap.Config.RGB_565));
  }

  // TODO: 01/04/17 Cache.
  private Single<MediaLink.Streamable> getStreamableVideoDetails(MediaLink.StreamableUnknown streamableLink) {
    return Dank.api()
        .streamableVideoDetails(streamableLink.videoId())
        .compose(applySchedulersSingle())
        .map(response -> MediaLink.Streamable.create(
            response.url(),
            response.files().lowQualityVideo().url(),
            response.files().highQualityVideo().url()
        ));
  }

  private Consumer<String> loadVideo() {
    return videoUrl -> {
      exoPlayerManager.setOnVideoSizeChangeListener((resizedVideoWidth, resizedVideoHeight, actualVideoWidth, actualVideoHeight) -> {
        int contentHeightWithoutKeyboard = deviceDisplayHeight - minimumGapWithBottom - Views.statusBarHeight(contentVideoView.getResources());
        Views.setHeight(contentVideoView, Math.min(contentHeightWithoutKeyboard, resizedVideoHeight));

        // Wait for the height change to happen and then reveal the video.
        Views.executeOnNextLayout(contentVideoView, () -> {
          contentLoadProgressView.setVisibility(View.GONE);

          int videoHeightMinusToolbar = contentVideoView.getHeight() - commentListParentSheet.getTop();
          commentListParentSheet.setScrollingEnabled(true);
          commentListParentSheet.setMaxScrollY(videoHeightMinusToolbar);
          commentListParentSheet.scrollTo(videoHeightMinusToolbar, submissionPageLayout.isExpanded() /* smoothScroll */);

          exoPlayerManager.setOnVideoSizeChangeListener(null);
          videoWidthChangeStream.accept(actualVideoWidth);
        });
      });

      String cachedVideoUrl = Dank.httpProxyCacheServer().getProxyUrl(videoUrl);
      exoPlayerManager.playVideoInLoop(Uri.parse(cachedVideoUrl));
    };
  }

  public void pausePlayback() {
    exoPlayerManager.pauseVideoPlayback();
  }
}


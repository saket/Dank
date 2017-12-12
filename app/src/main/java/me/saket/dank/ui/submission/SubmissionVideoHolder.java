package me.saket.dank.ui.submission;

import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.CheckResult;
import android.view.View;
import android.widget.ProgressBar;

import com.danikula.videocache.HttpProxyCacheServer;
import com.devbrackets.android.exomedia.ui.widget.VideoView;
import com.jakewharton.rxbinding2.internal.Notification;
import com.jakewharton.rxrelay2.BehaviorRelay;
import com.jakewharton.rxrelay2.PublishRelay;
import com.jakewharton.rxrelay2.Relay;

import java.util.concurrent.TimeUnit;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import me.saket.dank.data.links.MediaLink;
import me.saket.dank.ui.media.MediaHostRepository;
import me.saket.dank.utils.ExoPlayerManager;
import me.saket.dank.utils.Views;
import me.saket.dank.widgets.DankVideoControlsView;
import me.saket.dank.widgets.InboxUI.ExpandablePageLayout;
import me.saket.dank.widgets.ScrollingRecyclerViewSheet;

/**
 * Manages loading of video in {@link SubmissionFragment}.
 */
public class SubmissionVideoHolder {

  private final VideoView contentVideoView;
  private final ScrollingRecyclerViewSheet commentListParentSheet;

  private final ExpandablePageLayout submissionPageLayout;
  private final ExoPlayerManager exoPlayerManager;
  private final MediaHostRepository mediaHostRepository;
  private final int deviceDisplayHeight;
  private final int minimumGapWithBottom;
  private final ProgressBar contentLoadProgressView;
  private final Relay<Integer> videoWidthChangeStream = PublishRelay.create();
  private final Relay<Object> videoPreparedStream = BehaviorRelay.create();
  private final HttpProxyCacheServer httpProxyCacheServer;

  /**
   * <var>displayWidth</var> and <var>statusBarHeight</var> are used for capturing the video's bitmap,
   * which is in turn used for generating status bar tint. To minimize bitmap creation time, a Bitmap
   * of height equal to the status bar is created instead of the entire video height.
   *
   * @param minimumGapWithBottom The difference between video's bottom and the window's bottom will
   */
  public SubmissionVideoHolder(VideoView contentVideoView, ScrollingRecyclerViewSheet commentListParentSheet,
      ProgressBar contentLoadProgressView, ExpandablePageLayout submissionPageLayout, ExoPlayerManager exoPlayerManager,
      MediaHostRepository mediaHostRepository, HttpProxyCacheServer httpProxyCacheServer, int deviceDisplayHeight, int minimumGapWithBottom)
  {
    this.contentVideoView = contentVideoView;
    this.commentListParentSheet = commentListParentSheet;
    this.submissionPageLayout = submissionPageLayout;
    this.contentLoadProgressView = contentLoadProgressView;
    this.exoPlayerManager = exoPlayerManager;
    this.mediaHostRepository = mediaHostRepository;
    this.httpProxyCacheServer = httpProxyCacheServer;
    this.deviceDisplayHeight = deviceDisplayHeight;
    this.minimumGapWithBottom = minimumGapWithBottom;

    DankVideoControlsView controlsView = new DankVideoControlsView(contentVideoView.getContext());
    contentVideoView.setControls(controlsView);
    contentVideoView.setOnPreparedListener(() -> videoPreparedStream.accept(Notification.INSTANCE));
  }

  @CheckResult
  public Completable load(MediaLink mediaLink, boolean loadHighQualityVideo) {
    // Later hidden inside loadVideo(), when the video's height becomes available.
    contentLoadProgressView.setVisibility(View.VISIBLE);

    return mediaHostRepository.resolveActualLinkIfNeeded(mediaLink)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .map(link -> loadHighQualityVideo ? link.highQualityUrl() : link.lowQualityUrl())
        .flatMapCompletable(videoUrl -> loadVideo(videoUrl));
  }

  /**
   * <var>displayWidth</var> and <var>statusBarHeight</var> are used for creating a bitmap for capturing
   * the video's first frame. Creating a bitmap is expensive so we'll
   */
  @CheckResult
  public Observable<Bitmap> streamVideoFirstFrameBitmaps(int statusBarHeight) {
    return Observable.zip(videoPreparedStream, videoWidthChangeStream, (o, videoWidth) -> videoWidth)
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

  private Completable loadVideo(String videoUrl) {
    return Completable.fromAction(() -> {
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

      String cachedVideoUrl = httpProxyCacheServer.getProxyUrl(videoUrl);
      exoPlayerManager.setVideoUriToPlayInLoop(Uri.parse(cachedVideoUrl));
    });
  }

  public void pausePlayback() {
    exoPlayerManager.pauseVideoPlayback();
  }
}


package me.saket.dank.utils;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.view.View;

import com.devbrackets.android.exomedia.core.video.exo.ExoTextureVideoView;
import com.devbrackets.android.exomedia.ui.widget.VideoView;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.LoopingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import me.saket.dank.R;
import me.saket.dank.utils.lifecycle.LifecycleStreams;

public class ExoPlayerManager {

  private final VideoView playerView;
  private final ExoTextureVideoView textureVideoView;
  private @Nullable OnVideoSizeChangeListener videoSizeChangeListener;
  private boolean wasPlayingUponPause;
  private Bitmap cachedBitmapForFrameCapture;

  public interface OnVideoSizeChangeListener {
    void onVideoSizeChange(int resizedVideoWidth, int resizedVideoHeight, int actualVideoWidth, int actualVideoHeight);
  }

  public static ExoPlayerManager newInstance(LifecycleStreams lifecycleStreams, VideoView playerView) {
    ExoPlayerManager exoPlayerManager = new ExoPlayerManager(playerView);
    exoPlayerManager.setupVideoView();

    lifecycleStreams.onDestroy().subscribe(o -> exoPlayerManager.releasePlayer());
    lifecycleStreams.onPause().subscribe(o -> exoPlayerManager.pauseVideoPlayback());
    lifecycleStreams.onResume().subscribe(o -> {
      if (exoPlayerManager.wasPlayingUponPause) {
        exoPlayerManager.startVideoPlayback();
      }
    });

    return exoPlayerManager;
  }

  public ExoPlayerManager(VideoView playerView) {
    this.playerView = playerView;
    this.textureVideoView = playerView.findViewById(R.id.exomedia_video_view);
  }

  public void setOnVideoSizeChangeListener(@Nullable OnVideoSizeChangeListener listener) {
    videoSizeChangeListener = listener;
  }

  public View getTextureView() {
    return textureVideoView;
  }

  public void setupVideoView() {
    playerView.setOnVideoSizedChangedListener((unscaledWidth, unscaledHeight) -> {
      if (videoSizeChangeListener != null) {
        float widthFactor = (float) playerView.getWidth() / unscaledWidth;
        int videoWidthAfterResize = (int) (unscaledWidth * widthFactor);
        int videoHeightAfterResize = (int) (unscaledHeight * widthFactor);
        videoSizeChangeListener.onVideoSizeChange(videoWidthAfterResize, videoHeightAfterResize, unscaledWidth, unscaledHeight);
      }
    });
  }

  public void setVideoUriToPlayInLoop(Uri videoUri) {
    // Produces DataSource instances through which media data is loaded.
    DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(playerView.getContext(),
        Util.getUserAgent(playerView.getContext(), playerView.getContext().getPackageName())
    );

    // Produces Extractor instances for parsing the media data.
    ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
    MediaSource videoSource = new LoopingMediaSource(new ExtractorMediaSource(
        videoUri,
        dataSourceFactory,
        extractorsFactory,
        null,
        null
    ));
    playerView.setVideoURI(videoUri, videoSource);
  }

  public void startVideoPlayback() {
    playerView.start();
  }

  public void pauseVideoPlayback() {
    wasPlayingUponPause = playerView.isPlaying();
    playerView.pause();
  }

  private void releasePlayer() {
    playerView.release();
  }

  public Bitmap getBitmapOfCurrentVideoFrame(int width, int height, Bitmap.Config bitmapConfig) {
    if (cachedBitmapForFrameCapture == null) {
      cachedBitmapForFrameCapture = Bitmap.createBitmap(width, height, bitmapConfig);
    } else {
      if (cachedBitmapForFrameCapture.getWidth() != width && cachedBitmapForFrameCapture.getHeight() != height) {
        throw new IllegalStateException("Use the same dimensions so that bitmap can be cached");
      }
      cachedBitmapForFrameCapture.eraseColor(Color.TRANSPARENT);
    }
    return textureVideoView.getBitmap(cachedBitmapForFrameCapture);
  }
}

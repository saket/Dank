package me.saket.dank.utils;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.view.View;

import com.devbrackets.android.exomedia.core.video.exo.ExoTextureVideoView;
import com.devbrackets.android.exomedia.ui.widget.VideoView;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.LoopingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Consumer;
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

  @SuppressWarnings("unchecked")
  public static ExoPlayerManager newInstance(LifecycleStreams lifecycleStreams, VideoView playerView) {
    ExoPlayerManager exoPlayerManager = new ExoPlayerManager(playerView);
    exoPlayerManager.setupVideoView();

    lifecycleStreams.onDestroy().subscribe(o -> exoPlayerManager.releasePlayer());
    lifecycleStreams.onPause().subscribe(o -> exoPlayerManager.pausePlayback());
    lifecycleStreams.onResume().subscribe(o -> {
      if (exoPlayerManager.wasPlayingUponPause) {
        exoPlayerManager.startPlayback();
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

  public void setVideoUriToPlayInLoop(String videoUrl, VideoFormat videoFormat) {
    Uri videoURI = Uri.parse(videoUrl);
    MediaSource source = createMediaSource(videoURI, videoFormat);
    MediaSource loopingSource = new LoopingMediaSource(source);
    playerView.setVideoURI(videoURI, loopingSource);
  }

  private MediaSource createMediaSource(Uri videoURI, VideoFormat videoFormat) {
    // Produces DataSource instances through which media data is loaded.
    DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(
        playerView.getContext(),
        Util.getUserAgent(playerView.getContext(), playerView.getContext().getPackageName()));

    switch (videoFormat) {
      case DASH:
        return new DashMediaSource
            .Factory(new DefaultDashChunkSource.Factory(dataSourceFactory), dataSourceFactory)
            .createMediaSource(videoURI);

      case SMOOTH_STREAMING:
        return new SsMediaSource
            .Factory(new DefaultSsChunkSource.Factory(dataSourceFactory), dataSourceFactory)
            .createMediaSource(videoURI);

      case HLS:
        return new HlsMediaSource
            .Factory(dataSourceFactory)
            .createMediaSource(videoURI);

      case OTHER:
        return new ExtractorMediaSource
            .Factory(dataSourceFactory)
            .createMediaSource(videoURI);

      default: {
        throw new IllegalStateException("Unsupported type: " + videoFormat + ", for videoURI: " + videoURI);
      }
    }
  }

  public void resetPlayback() {
    playerView.suspend();
    if (playerView.getVideoUri() != null) {
      playerView.setVideoURI(null);
    }
  }

  public void startPlayback() {
    playerView.start();
  }

  public void pausePlayback() {
    wasPlayingUponPause = playerView.isPlaying();
    playerView.pause();
  }

  public void setOnErrorListener(Consumer<Throwable> errorListener) {
    playerView.setOnErrorListener(e -> {
      try {
        errorListener.accept(e);
        return true;  // true == error handled.

      } catch (Exception anotherE) {
        throw Exceptions.propagate(anotherE);
      }
    });
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

package me.saket.dank.utils;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.view.View;

import androidx.annotation.Nullable;

import com.devbrackets.android.exomedia.core.video.exo.ExoTextureVideoView;
import com.devbrackets.android.exomedia.listener.OnVideoSizeChangedListener;
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

import io.reactivex.Completable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Consumer;
import me.saket.dank.R;
import me.saket.dank.utils.lifecycle.LifecycleStreams;

public class ExoPlayerManager {

  private final VideoView playerView;
  private final ExoTextureVideoView textureVideoView;
  private Bitmap cachedBitmapForFrameCapture;
  private boolean audioFocusHandlingDisabled;

  @SuppressWarnings("unchecked")
  public static ExoPlayerManager newInstance(VideoView playerView) {
    return new ExoPlayerManager(playerView);
  }

  public ExoPlayerManager(VideoView playerView) {
    this.playerView = playerView;
    this.textureVideoView = playerView.findViewById(R.id.exomedia_video_view);
  }

  @SuppressWarnings("unchecked")
  public Completable manageLifecycle(LifecycleStreams lifecycle) {
    Completable release = Completable.create(emitter -> emitter.setCancellable(() -> releasePlayer()));

    Completable autoPauseAndResume = lifecycle.onPause()
        .filter(o -> playerView.isPlaying())
        .flatMapCompletable(o -> Completable.fromAction(() -> pausePlayback())
            .andThen(lifecycle.onResume()
                .take(1)
                .ignoreElements())
            .andThen(Completable.fromAction(() -> startPlayback())));

    return release
        .mergeWith(autoPauseAndResume);
  }

  public void setOnVideoSizeChangeListener(@Nullable OnVideoSizeChangedListener listener) {
    playerView.setOnVideoSizedChangedListener(listener);
  }

  public View getTextureView() {
    return textureVideoView;
  }

  public void setVideoUriToPlayInLoop(String videoUrl, VideoFormat videoFormat) {
    if (!audioFocusHandlingDisabled) {
      playerView.setHandleAudioFocus(false);
      audioFocusHandlingDisabled = true;
    }

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
    //Timber.w("reset");
    if (playerView.isPlaying()) {
      playerView.stopPlayback();
      playerView.setVideoURI(null);
    }
  }

  public void startPlayback() {
    //Timber.w("playing");
    playerView.start();
  }

  public void pausePlayback() {
    //Timber.w("pause");
    playerView.pause();
  }

  public void seekTo(long toMilliseconds) {
    playerView.seekTo(toMilliseconds);
  }

  public long getCurrentSeekPosition() {
    return playerView.getCurrentPosition();
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
    //Timber.w("Releasing player");
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

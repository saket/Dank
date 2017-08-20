package me.saket.dank.widgets;

import android.content.Context;
import android.graphics.Outline;
import android.support.annotation.IntRange;
import android.support.annotation.Px;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.devbrackets.android.exomedia.ui.widget.VideoControls;
import com.devbrackets.android.exomedia.ui.widget.VideoView;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.saket.dank.R;
import me.saket.dank.utils.Views;

/**
 * Playback controls for a video.
 */
public class DankVideoControlsView extends VideoControls {

  @BindView(R.id.exomedia_controls_play_icon) ImageView playIconView;
  @BindView(R.id.exomedia_controls_video_seek) SeekBar progressSeekBar;
  @BindView(R.id.exomedia_controls_video_seek_container) ViewGroup progressSeekBarContainer;

  private boolean userInteractingWithSeek;
  private VideoProgressChangeListener progressChangeListener;

  public enum VideoState {
    PREPARING,
    PREPARED
  }

  public interface VideoProgressChangeListener {
    void onProgressChange();
  }

  public DankVideoControlsView(Context context) {
    super(context);
  }

  public void setVideoProgressChangeListener(VideoProgressChangeListener listener) {
    progressChangeListener = listener;
  }

  /**
   * Extra space required below the video so that the progress seek-bar and its extra touch space are visible.
   */
  @Px
  public int getBottomExtraSpaceForProgressSeekBar() {
    return progressSeekBar.getHeight() + progressSeekBarContainer.getPaddingBottom();
  }

  public void showVideoState(VideoState videoState) {
    switch (videoState) {
      case PREPARING:
        loadingProgressBar.setVisibility(VISIBLE);
        progressSeekBarContainer.setVisibility(INVISIBLE);
        playPauseButton.setClickable(false);
        break;

      case PREPARED:
        playPauseButton.setClickable(true);
        progressSeekBarContainer.setVisibility(VISIBLE);
        break;
    }
  }

  @Override
  public void setVideoView(VideoView videoView) {
    super.setVideoView(videoView);

    // We want the seek-bar to be positioned below the video.
    // The current View hierarchy of VideoView is this:
    // ViewView (RelativeLayout)
    // - RelativeLayout
    //   - TextureView
    //   - ImageView (for preview)
    // - Controls View.
    Views.executeOnMeasure(progressSeekBarContainer, () -> {
      View videoTextureViewContainer = videoView.getChildAt(0);
      RelativeLayout.LayoutParams textureViewContainerParams = ((RelativeLayout.LayoutParams) videoTextureViewContainer.getLayoutParams());
      textureViewContainerParams.topMargin = -progressSeekBarContainer.getPaddingBottom();
      videoTextureViewContainer.setLayoutParams(textureViewContainerParams);
    });
  }

  @Override
  protected void retrieveViews() {
    ButterKnife.bind(this, this);

    playPauseButton = ButterKnife.findById(this, R.id.exomedia_controls_play_pause_btn);
    loadingProgressBar = ButterKnife.findById(this, R.id.exomedia_controls_video_loading);

    // Cannot remove these because ExoMedia expects them to be non-null.
    descriptionTextView = currentTimeTextView = endTimeTextView = titleTextView = subTitleTextView = new TextView(getContext());
    previousButton = nextButton = new ImageButton(getContext());
    textContainer = new LinearLayout(getContext());

    playIconView.setOutlineProvider(new ViewOutlineProvider() {
      @Override
      public void getOutline(View view, Outline outline) {
        outline.setOval(0, 0, view.getWidth(), view.getHeight());
      }
    });
  }

  @Override
  protected int getLayoutResource() {
    return R.layout.custom_video_controls;
  }

  @Override
  public void setPosition(@IntRange(from = 0L) long position) {
    progressSeekBar.setProgress((int) position);

    if (progressChangeListener != null) {
      progressChangeListener.onProgressChange();
    }
  }

  @Override
  public void setDuration(@IntRange(from = 0L) long duration) {
    if (duration != progressSeekBar.getMax()) {
      progressSeekBar.setMax((int) duration);
    }
  }

  @Override
  public void updateProgress(@IntRange(from = 0L) long position, @IntRange(from = 0L) long duration, @IntRange(from = 0L, to = 100L) int bufferPercent) {
    if (!userInteractingWithSeek) {
      progressSeekBar.setSecondaryProgress((int) (progressSeekBar.getMax() * ((float) bufferPercent / 100)));
      progressSeekBar.setProgress((int) position);
    }

    if (progressChangeListener != null) {
      progressChangeListener.onProgressChange();
    }
  }

  @Override
  protected void registerListeners() {
    super.registerListeners();
    progressSeekBar.setOnSeekBarChangeListener(new SeekBarChanged());
    playPauseButton.setOnClickListener(__ -> onPlayPauseClick());
  }

  @Override
  protected void animateVisibility(boolean toVisible) {
    isVisible = toVisible;
  }

  @Override
  protected void updateTextContainerVisibility() {

  }

  @Override
  public void showLoading(boolean initialLoad) {
    loadingProgressBar.setVisibility(VISIBLE);
    //updatePlayPauseImage(false);
  }

  @Override
  public void finishLoading() {
    loadingProgressBar.setVisibility(GONE);
    //updatePlayPauseImage(videoView != null && videoView.isPlaying());
  }

  @Override
  public void show() {
    super.show();
  }

  @Override
  public void hideDelayed(long delay) {
    if (!userInteractingWithSeek) {
      super.hideDelayed(delay);
    }
  }

  @Override
  public void updatePlayPauseImage(boolean isPlaying) {
    if (!userInteractingWithSeek) {
      playIconView.setVisibility(isPlaying ? GONE : VISIBLE);
    }
  }

  @Override
  protected void updateButtonDrawables() {
  }

  /**
   * Listens to the seek bar change events and correctly handles the changes
   */
  protected class SeekBarChanged implements SeekBar.OnSeekBarChangeListener {
    private long seekToTime;

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
      if (!fromUser) {
        return;
      }

      seekToTime = progress;
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
      userInteractingWithSeek = true;
      if (seekListener == null || !seekListener.onSeekStarted()) {
        internalListener.onSeekStarted();
      }
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
      userInteractingWithSeek = false;
      if (seekListener == null || !seekListener.onSeekEnded(seekToTime)) {
        internalListener.onSeekEnded(seekToTime);
      }
    }
  }
}

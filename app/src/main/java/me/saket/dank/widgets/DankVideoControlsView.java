package me.saket.dank.widgets;

import android.content.Context;
import android.graphics.Outline;
import android.support.annotation.IntRange;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.devbrackets.android.exomedia.ui.widget.VideoControls;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.saket.dank.R;

/**
 * Playback controls for a video.
 */
public class DankVideoControlsView extends VideoControls {

  @BindView(R.id.exomedia_controls_play_icon) ImageView playIconView;
  @BindView(R.id.exomedia_controls_video_seek) SeekBar progressSeekBar;

  private boolean userInteractingWithSeek;
  private VideoProgressChangeListener progressChangeListener;

  public interface VideoProgressChangeListener {
    void onProgressChange();
  }

  public DankVideoControlsView(Context context) {
    super(context);
  }

  public void setVideoProgressChangeListener(VideoProgressChangeListener listener) {
    progressChangeListener = listener;
  }

  public SeekBar getProgressSeekBar() {
    return progressSeekBar;
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
    updatePlayPauseImage(false);
  }

  @Override
  public void finishLoading() {
    loadingProgressBar.setVisibility(GONE);

    updatePlayPauseImage(videoView != null && videoView.isPlaying());
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

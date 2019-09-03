package me.saket.dank.widgets;

import android.content.Context;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.IntRange;

import com.devbrackets.android.exomedia.ui.widget.VideoControls;

import me.saket.dank.R;

/**
 * Playback controls for a video.
 */
public abstract class ExoMediaVideoControlsView extends VideoControls {

  public SeekBar progressSeekBar;
  public ProgressWithFileSizeView loadingProgress;

  private boolean userInteractingWithSeek;
  private VideoProgressChangeListener progressChangeListener;

  public interface VideoProgressChangeListener {
    void onProgressChange();
  }

  public ExoMediaVideoControlsView(Context context) {
    super(context);
  }

  public void setVideoProgressChangeListener(VideoProgressChangeListener listener) {
    progressChangeListener = listener;
  }

  @Override
  protected void retrieveViews() {
    progressSeekBar = findViewById(R.id.videocontrols_seek);
    loadingProgress = findViewById(R.id.videocontrols_video_loading);
    playPauseButton = findViewById(R.id.videocontrols_play_pause);
    loadingProgress.setIndeterminate(true);
    loadingProgress.setProgressBarBackgroundFillEnabled(false);

    // Cannot remove these because ExoMedia expects them to be non-null.
    descriptionTextView = currentTimeTextView = endTimeTextView = titleTextView = subTitleTextView = new TextView(getContext());
    previousButton = nextButton = new ImageButton(getContext());
    textContainer = new LinearLayout(getContext());
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
  protected void updateTextContainerVisibility() {}

  @Override
  public void showLoading(boolean initialLoad) {
    loadingProgress.setVisibility(VISIBLE);
  }

  @Override
  public void finishLoading() {
    loadingProgress.setVisibility(GONE);
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
    updatePlayPauseImage(isPlaying, userInteractingWithSeek);
  }

  protected abstract void updatePlayPauseImage(boolean isPlaying, boolean userInteractingWithSeek);

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

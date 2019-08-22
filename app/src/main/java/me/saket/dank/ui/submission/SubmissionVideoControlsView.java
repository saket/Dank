package me.saket.dank.ui.submission;

import android.content.Context;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.text.format.DateUtils;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Px;
import com.devbrackets.android.exomedia.ui.widget.VideoView;

import java.util.Formatter;
import java.util.Locale;

import me.saket.dank.R;
import me.saket.dank.utils.Views;
import me.saket.dank.widgets.ExoMediaVideoControlsView;

public class SubmissionVideoControlsView extends ExoMediaVideoControlsView {

  private static final StringBuilder TIME_DURATION_FORMAT_BUILDER = new StringBuilder();
  private static final Formatter TIME_DURATION_FORMATTER = new Formatter(TIME_DURATION_FORMAT_BUILDER, Locale.ENGLISH);

  private final AnimatedVectorDrawable playToPauseDrawable;
  private final AnimatedVectorDrawable pauseToPlayDrawable;

  protected ViewGroup buttonsContainer;
  protected TextView remainingDurationView;

  public SubmissionVideoControlsView(Context context) {
    super(context);
    playToPauseDrawable = (AnimatedVectorDrawable) context.getDrawable(R.drawable.avd_play_to_pause_white_24dp);
    pauseToPlayDrawable = (AnimatedVectorDrawable) context.getDrawable(R.drawable.avd_pause_to_play_white_24dp);
  }

  @Px
  public int heightOfControlButtons() {
    return buttonsContainer.getHeight();
  }

  @Override
  protected int getLayoutResource() {
    return R.layout.view_submission_video_controls;
  }

  @Override
  protected void retrieveViews() {
    super.retrieveViews();
    buttonsContainer = findViewById(R.id.videocontrols_buttons_container);
    remainingDurationView = findViewById(R.id.videocontrols_remaining_duration);
  }

  @Override
  public void setVideoView(@SuppressWarnings("NullableProblems") VideoView videoView) {
    super.setVideoView(videoView);

    // The seek-bar needs to be positioned below the video. ExoMedia by default places controls
    // above the video inside a RelativeLayout. SubmissionVideoHolder resizes the video to include
    // extra space for the SeekBar.
    Views.executeOnMeasure(buttonsContainer, () -> {
      RelativeLayout videoTextureViewContainer = (RelativeLayout) videoView.getChildAt(0);
      RelativeLayout.LayoutParams textureViewContainerLP = ((RelativeLayout.LayoutParams) videoTextureViewContainer.getLayoutParams());
      textureViewContainerLP.bottomMargin = heightOfControlButtons();
      videoTextureViewContainer.setLayoutParams(textureViewContainerLP);
    });
  }

  @Override
  protected void updatePlayPauseImage(boolean isPlaying, boolean userInteractingWithSeek) {
    playPauseButton.setImageResource(R.drawable.avd_play_to_pause_white_24dp);

    if (isPlaying) {
      playPauseButton.setImageDrawable(playToPauseDrawable);
      playPauseButton.setContentDescription(getResources().getString(R.string.submission_video_controls_cd_pause));
    } else {
      playPauseButton.setImageDrawable(pauseToPlayDrawable);
      playPauseButton.setContentDescription(getResources().getString(R.string.submission_video_controls_cd_play));
    }

    animatePlayPauseButton();
  }

  private void animatePlayPauseButton() {
    ((AnimatedVectorDrawable) playPauseButton.getDrawable()).start();
  }

  @Override
  public void updateProgress(long position, long duration, int bufferPercent) {
    super.updateProgress(position, duration, bufferPercent);

    long remainingDurationMillis = duration - position;
    remainingDurationView.setText(formatTimeDuration(remainingDurationMillis));
  }

  /**
   * Format milliseconds to h:mm:ss.
   */
  private static String formatTimeDuration(long milliseconds) {
    if (milliseconds < 0) {
      return "";
    }

    long seconds = (milliseconds % DateUtils.MINUTE_IN_MILLIS) / DateUtils.SECOND_IN_MILLIS;
    long minutes = (milliseconds % DateUtils.HOUR_IN_MILLIS) / DateUtils.MINUTE_IN_MILLIS;
    long hours = (milliseconds % DateUtils.DAY_IN_MILLIS) / DateUtils.HOUR_IN_MILLIS;

    TIME_DURATION_FORMAT_BUILDER.setLength(0);
    if (hours > 0) {
      return TIME_DURATION_FORMATTER.format("%d:%02d:%02d", hours, minutes, seconds).toString();
    }
    return TIME_DURATION_FORMATTER.format("%02d:%02d", minutes, seconds).toString();
  }
}

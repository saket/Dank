package me.saket.dank.ui.submission;

import android.content.Context;
import android.graphics.drawable.AnimatedVectorDrawable;
import androidx.annotation.Px;
import android.text.format.DateUtils;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.devbrackets.android.exomedia.ui.widget.VideoView;

import java.util.Formatter;
import java.util.Locale;

import me.saket.dank.R;
import me.saket.dank.utils.ReversibleAnimatedVectorDrawable;
import me.saket.dank.utils.Views;
import me.saket.dank.widgets.ExoMediaVideoControlsView;

public class SubmissionVideoControlsView extends ExoMediaVideoControlsView {

  private static final StringBuilder TIME_DURATION_FORMAT_BUILDER = new StringBuilder();
  private static final Formatter TIME_DURATION_FORMATTER = new Formatter(TIME_DURATION_FORMAT_BUILDER, Locale.ENGLISH);

  private ReversibleAnimatedVectorDrawable playPauseIcon;
  protected ViewGroup buttonsContainer;
  protected TextView remainingDurationView;

  public SubmissionVideoControlsView(Context context) {
    super(context);
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
    playPauseIcon = new ReversibleAnimatedVectorDrawable(((AnimatedVectorDrawable) playPauseButton.getDrawable()));
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
    if (isPlaying) {
      playPauseIcon.play();
      playPauseButton.setContentDescription(getResources().getString(R.string.submission_video_controls_cd_pause));
    } else {
      playPauseIcon.reverse();
      playPauseButton.setContentDescription(getResources().getString(R.string.submission_video_controls_cd_play));
    }
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

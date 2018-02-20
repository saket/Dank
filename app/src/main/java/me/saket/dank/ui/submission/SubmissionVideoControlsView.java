package me.saket.dank.ui.submission;

import android.content.Context;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.devbrackets.android.exomedia.ui.widget.VideoView;

import me.saket.dank.R;
import me.saket.dank.utils.ReversibleAnimatedVectorDrawable;
import me.saket.dank.utils.Views;
import me.saket.dank.widgets.ExoMediaVideoControlsView;

public class SubmissionVideoControlsView extends ExoMediaVideoControlsView {

  private ViewGroup buttonsContainer;
  private ReversibleAnimatedVectorDrawable playPauseIcon;

  public SubmissionVideoControlsView(Context context) {
    super(context);
  }

  @Override
  protected int getLayoutResource() {
    return R.layout.view_submission_video_controls;
  }

  @Override
  protected void retrieveViews() {
    super.retrieveViews();
    buttonsContainer = findViewById(R.id.submission_videocontrols_buttons_container);
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
      textureViewContainerLP.topMargin = -buttonsContainer.getHeight();
      videoTextureViewContainer.setLayoutParams(textureViewContainerLP);
    });
  }

  public int heightOfControlButtons() {
    return buttonsContainer.getHeight();
  }

  @Override
  protected void updatePlayPauseImage(boolean isPlaying, boolean userInteractingWithSeek) {
    if (isPlaying) {
      playPauseIcon.play();
    } else {
      playPauseIcon.reverse();
    }
  }
}

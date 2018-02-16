package me.saket.dank.ui.submission;

import android.content.Context;

import me.saket.dank.R;
import me.saket.dank.widgets.ExoMediaVideoControlsView;

public class SubmissionVideoControlsView extends ExoMediaVideoControlsView {

  public SubmissionVideoControlsView(Context context) {
    super(context);
  }

  @Override
  protected int getLayoutResource() {
    return R.layout.view_submission_video_controls;
  }

  @Override
  protected void updatePlayPauseImage(boolean isPlaying, boolean userInteractingWithSeek) {
    // TODO.
  }
}

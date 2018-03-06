package me.saket.dank.ui.media;

import android.content.Context;

import me.saket.dank.ui.submission.SubmissionVideoControlsView;

public class MediaViewerVideoControlsView extends SubmissionVideoControlsView {

  /**
   * See {@link #showVideoState(VideoState)}.
   */
  public enum VideoState {
    PREPARING,
    PREPARED
  }

  public MediaViewerVideoControlsView(Context context) {
    super(context);
  }

  public void showVideoState(VideoState videoState) {
    switch (videoState) {
      case PREPARING:
        playPauseButton.setClickable(false);
        loadingProgressBar.setVisibility(VISIBLE);
        progressSeekBar.setVisibility(INVISIBLE);
        break;

      case PREPARED:
        playPauseButton.setClickable(true);
        progressSeekBar.setVisibility(VISIBLE);
        break;
    }
  }
}

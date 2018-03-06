package me.saket.dank.ui.media;

import android.content.Context;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;

import com.devbrackets.android.exomedia.ui.widget.VideoView;

import me.saket.dank.R;
import me.saket.dank.ui.submission.SubmissionVideoControlsView;

public class MediaViewerVideoControlsView extends SubmissionVideoControlsView {

  private VideoView videoView;
  private ViewGroup controlsRootContainer;

  /**
   * See {@link #showVideoState(VideoState)}.
   */
  public enum VideoState {
    PREPARING,
    PREPARED
  }

  public MediaViewerVideoControlsView(Context context) {
    super(context);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      // Submission controls have a ripple to indicate clicks.
      // The click indicator isn't needed here.
      controlsRootContainer.setForeground(null);
    }
  }

  @Override
  public void setVideoView(VideoView videoView) {
    this.videoView = videoView;
    super.setVideoView(videoView);
  }

  @Override
  protected void retrieveViews() {
    super.retrieveViews();
    controlsRootContainer = findViewById(R.id.videocontrols_root);
  }

  public void showVideoState(VideoState videoState) {
    switch (videoState) {
      case PREPARING:
        setVisibilityOfAllChildrenExceptProgress(INVISIBLE);
        loadingProgress.setVisibility(VISIBLE);
        videoView.setBackground(null);
        break;

      case PREPARED:
        setVisibilityOfAllChildrenExceptProgress(VISIBLE);
        videoView.setBackgroundResource(R.color.submission_media_content_background_padding);
        break;
    }
  }

  private void setVisibilityOfAllChildrenExceptProgress(int visibility) {
    for (int i = 0; i < getChildCount(); i++) {
      View child = getChildAt(i);
      if (child.getId() != loadingProgress.getId()) {
        setVisibility(visibility);
      }
    }
  }
}

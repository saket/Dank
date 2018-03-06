package me.saket.dank.ui.media;

import android.content.Context;
import android.view.View;

import com.devbrackets.android.exomedia.ui.widget.VideoView;

import me.saket.dank.R;
import me.saket.dank.ui.submission.SubmissionVideoControlsView;

public class MediaViewerVideoControlsView extends SubmissionVideoControlsView {

  private VideoView videoView;

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

  @Override
  public void setVideoView(VideoView videoView) {
    this.videoView = videoView;
    super.setVideoView(videoView);
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

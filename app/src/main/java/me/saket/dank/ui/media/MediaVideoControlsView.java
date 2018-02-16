package me.saket.dank.ui.media;

import android.content.Context;
import android.graphics.Outline;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.devbrackets.android.exomedia.ui.widget.VideoView;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.saket.dank.R;
import me.saket.dank.utils.Views;
import me.saket.dank.widgets.ExoMediaVideoControlsView;

public class MediaVideoControlsView extends ExoMediaVideoControlsView {

  @BindView(R.id.exomedia_controls_play_icon) public ImageView playIconView;

  public MediaVideoControlsView(Context context) {
    super(context);
  }

  @Override
  protected int getLayoutResource() {
    return R.layout.view_media_viewer_video_controls;
  }

  @Override
  public void setVideoView(@SuppressWarnings("NullableProblems") VideoView videoView) {
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
    super.retrieveViews();
    ButterKnife.bind(this, this);

    playIconView.setOutlineProvider(new ViewOutlineProvider() {
      @Override
      public void getOutline(View view, Outline outline) {
        outline.setOval(0, 0, view.getWidth(), view.getHeight());
      }
    });
  }

  @Override
  protected void updatePlayPauseImage(boolean isPlaying, boolean userInteractingWithSeek) {
    if (!userInteractingWithSeek) {
      playIconView.setVisibility(isPlaying ? GONE : VISIBLE);
    }
  }
}

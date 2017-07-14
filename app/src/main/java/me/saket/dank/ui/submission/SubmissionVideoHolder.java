package me.saket.dank.ui.submission;

import static me.saket.dank.utils.RxUtils.applySchedulersSingle;
import static me.saket.dank.utils.RxUtils.logError;
import static me.saket.dank.utils.Views.executeOnNextLayout;
import static me.saket.dank.utils.Views.setHeight;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.support.v7.graphics.Palette;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.devbrackets.android.exomedia.core.video.exo.ExoTextureVideoView;
import com.devbrackets.android.exomedia.ui.widget.VideoView;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import me.saket.dank.R;
import me.saket.dank.data.MediaLink;
import me.saket.dank.di.Dank;
import me.saket.dank.utils.Colors;
import me.saket.dank.utils.ExoPlayerManager;
import me.saket.dank.widgets.DankVideoControlsView;
import me.saket.dank.widgets.InboxUI.ExpandablePageLayout;
import me.saket.dank.widgets.ScrollingRecyclerViewSheet;

/**
 * Manages loading of video in {@link SubmissionFragment}.
 */
public class SubmissionVideoHolder {

  @BindView(R.id.submission_video_container) ViewGroup contentVideoViewContainer;
  @BindView(R.id.submission_video) VideoView contentVideoView;
  @BindView(R.id.submission_comment_list_parent_sheet) ScrollingRecyclerViewSheet commentListParentSheet;

  private final ExpandablePageLayout submissionPageLayout;
  private final ExoPlayerManager exoPlayerManager;
  private final ProgressBar contentLoadProgressView;
  private Bitmap videoBitmap;
  private DankVideoControlsView controlsView;

  public SubmissionVideoHolder(View submissionLayout, ProgressBar contentLoadProgressView, ExpandablePageLayout submissionPageLayout,
      ExoPlayerManager exoPlayerManager)
  {
    ButterKnife.bind(this, submissionLayout);
    this.submissionPageLayout = submissionPageLayout;
    this.contentLoadProgressView = contentLoadProgressView;
    this.exoPlayerManager = exoPlayerManager;

    controlsView = new DankVideoControlsView(contentVideoView.getContext());
    controlsView.insertSeekBarIn(contentVideoViewContainer);
    contentVideoView.setControls(controlsView);
  }

  public Disposable load(MediaLink mediaLink) {
    Single<? extends MediaLink> videoUrlObservable = mediaLink instanceof MediaLink.StreamableUnknown
        ? getStreamableVideoDetails(((MediaLink.StreamableUnknown) mediaLink))
        : Single.just(mediaLink);

    return videoUrlObservable
        .doOnSubscribe(__ -> contentLoadProgressView.setVisibility(View.VISIBLE))
        .map(link -> link.lowQualityVideoUrl())
        .subscribe(load(), error -> {
          // TODO: 01/04/17 Handle error.
          logError("Couldn't load video").accept(error);
        });
  }

  // TODO: 01/04/17 Cache.
  private Single<MediaLink.Streamable> getStreamableVideoDetails(MediaLink.StreamableUnknown streamableLink) {
    return Dank.api()
        .streamableVideoDetails(streamableLink.videoId())
        .compose(applySchedulersSingle())
        .map(response -> MediaLink.Streamable.create(
            response.url(),
            response.files().lowQualityVideo().url(),
            response.files().highQualityVideo().url()
        ));
  }

  private Consumer<String> load() {
    return videoUrl -> {
      exoPlayerManager.setOnVideoSizeChangeListener((videoWidth, videoHeight) -> {
        setHeight(contentVideoView, videoHeight);

        // Wait for the height change to happen and then reveal the video.
        executeOnNextLayout(contentVideoView, () -> {
          contentLoadProgressView.setVisibility(View.GONE);
          commentListParentSheet.setScrollingEnabled(true);

          int videoHeightMinusToolbar = contentVideoViewContainer.getHeight() - commentListParentSheet.getTop();
          commentListParentSheet.setMaxScrollY(videoHeightMinusToolbar);
          commentListParentSheet.scrollTo(videoHeightMinusToolbar, submissionPageLayout.isExpanded() /* smoothScroll */);

          exoPlayerManager.setOnVideoSizeChangeListener(null);
        });
      });

      String cachedVideoUrl = Dank.httpProxyCacheServer().getProxyUrl(videoUrl);
      exoPlayerManager.playVideoInLoop(Uri.parse(cachedVideoUrl));

      tintVideoControlsWithVideo();
    };
  }

  private void tintVideoControlsWithVideo() {
    LayerDrawable seekBarDrawable = (LayerDrawable) controlsView.getProgressSeekBar().getProgressDrawable();
    Drawable progressDrawable = seekBarDrawable.findDrawableByLayerId(android.R.id.progress);
    Drawable bufferDrawable = seekBarDrawable.findDrawableByLayerId(android.R.id.secondaryProgress);

    View viewById = contentVideoView.findViewById(R.id.exomedia_video_view);
    ExoTextureVideoView textureVideoView = (ExoTextureVideoView) viewById;

    // TODO: Move this to an async code block.
    // TODO: Transition colors smoothly + use default colors on start.

    controlsView.setVideoProgressChangeListener(() -> {
      //final long startTime = System.currentTimeMillis();
      if (videoBitmap == null) {
        videoBitmap = Bitmap.createBitmap(textureVideoView.getResources().getDisplayMetrics(), 10, 10, Bitmap.Config.RGB_565);
      }

      Palette.from(textureVideoView.getBitmap(videoBitmap))
          .generate(palette -> {
            int progressColor = palette.getMutedColor(palette.getVibrantColor(-1));
            if (progressColor != -1) {
              progressDrawable.setTint(progressColor);
              bufferDrawable.setTint(Colors.applyAlpha(progressColor, 2f));
              controlsView.setVideoProgressChangeListener(null);
            }
          });
      //Timber.i("Palette in: %sms", System.currentTimeMillis() - startTime);
    });
  }

  public void pausePlayback() {
    exoPlayerManager.pauseVideoPlayback();
  }
}


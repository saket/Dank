package me.saket.dank.ui.media;

import android.animation.LayoutTransition;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ViewFlipper;

import com.devbrackets.android.exomedia.ui.widget.VideoView;
import com.jakewharton.rxrelay2.BehaviorRelay;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.saket.dank.R;
import me.saket.dank.data.ErrorResolver;
import me.saket.dank.data.ResolvedError;
import me.saket.dank.di.Dank;
import me.saket.dank.utils.ExoPlayerManager;
import me.saket.dank.utils.MediaHostRepository;
import me.saket.dank.utils.Views;
import me.saket.dank.widgets.DankVideoControlsView;
import me.saket.dank.widgets.ErrorStateView;
import me.saket.dank.widgets.MediaAlbumViewerTitleDescriptionView;
import me.saket.dank.widgets.binoculars.FlickDismissLayout;
import me.saket.dank.widgets.binoculars.FlickGestureListener;
import timber.log.Timber;

public class MediaVideoFragment extends BaseMediaViewerFragment {

  private static final String KEY_MEDIA_ITEM = "mediaItem";

  @BindView(R.id.albumviewer_video_flickdismisslayout) FlickDismissLayout flickDismissViewGroup;
  @BindView(R.id.albumviewer_video_video) VideoView videoView;
  @BindView(R.id.albumviewer_video_title_description) MediaAlbumViewerTitleDescriptionView titleDescriptionView;
  @BindView(R.id.albumviewer_video_title_description_dimming) View titleDescriptionBackgroundDimmingView;
  @BindView(R.id.albumviewer_video_error) ErrorStateView loadErrorStateView;
  @BindView(R.id.albumviewer_video_content_flipper) ViewFlipper contentViewFlipper;

  @Inject MediaHostRepository mediaHostRepository;
  @Inject ErrorResolver errorResolver;

  private enum ScreenState {
    LOADING_VIDEO_OR_READY,
    FAILED
    // There's no progress state, because ExoMedia internally maintains a progress indicator for buffering.
  }

  private MediaAlbumItem mediaAlbumItem;
  private ExoPlayerManager exoPlayerManager;
  private BehaviorRelay<Boolean> fragmentVisibleToUserStream = BehaviorRelay.create();

  static MediaVideoFragment create(MediaAlbumItem mediaAlbumItem) {
    MediaVideoFragment fragment = new MediaVideoFragment();
    Bundle args = new Bundle(1);
    args.putParcelable(KEY_MEDIA_ITEM, mediaAlbumItem);
    fragment.setArguments(args);
    return fragment;
  }

  @Override
  public void onAttach(Context context) {
    Dank.dependencyInjector().inject(this);
    super.onAttach(context);

    if (!(getActivity() instanceof MediaFragmentCallbacks) || !(getActivity() instanceof FlickGestureListener.GestureCallbacks)) {
      throw new AssertionError();
    }
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    super.onCreateView(inflater, container, savedInstanceState);
    View layout = inflater.inflate(R.layout.fragment_album_viewer_page_video, container, false);
    ButterKnife.bind(this, layout);

    mediaAlbumItem = getArguments().getParcelable(KEY_MEDIA_ITEM);
    //noinspection ConstantConditions
    super.setMediaLink(mediaAlbumItem.mediaLink());
    super.setTitleDescriptionView(titleDescriptionView);
    super.setImageDimmingView(titleDescriptionBackgroundDimmingView);

    moveToScreenState(ScreenState.LOADING_VIDEO_OR_READY);

    return layout;
  }

  @Override
  public void onViewCreated(View fragmentLayout, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(fragmentLayout, savedInstanceState);

    // Make the video flick-dismissible.
    setupFlickGestures(flickDismissViewGroup);

    // Keep the video below the status bar and above the control buttons.
    unsubscribeOnDestroy(
        ((MediaFragmentCallbacks) getActivity())
            .optionButtonsHeight()
            .subscribe(optionButtonsHeight -> {
              int statusBarHeight = Views.statusBarHeight(getResources());
              Views.setPaddingVertical(
                  contentViewFlipper,
                  contentViewFlipper.getPaddingTop() + statusBarHeight,
                  contentViewFlipper.getPaddingBottom() + optionButtonsHeight
              );
            })
    );

    exoPlayerManager = ExoPlayerManager.newInstance(this, videoView);
    DankVideoControlsView videoControlsView = new DankVideoControlsView(getActivity());
    videoView.setControls(videoControlsView);
    videoControlsView.showVideoState(DankVideoControlsView.VideoState.PREPARING);

    // The preview image takes time to be drawn. Fade the video in slowly.
    LayoutTransition layoutTransition = ((ViewGroup) videoView.getParent()).getLayoutTransition();
    videoView.setLayoutTransition(layoutTransition);
    View textureViewContainer = ((ViewGroup) exoPlayerManager.getTextureView().getParent());
    textureViewContainer.setVisibility(View.INVISIBLE);

    videoView.setOnPreparedListener(() -> {
      textureViewContainer.setVisibility(View.VISIBLE);
      videoControlsView.showVideoState(DankVideoControlsView.VideoState.PREPARED);

      // Auto-play when this Fragment becomes visible.
      unsubscribeOnDestroy(
          fragmentVisibleToUserStream
              .subscribe(visibleToUser -> {
                if (!visibleToUser) {
                  exoPlayerManager.pauseVideoPlayback();
                } else {
                  exoPlayerManager.startVideoPlayback();
                }
              }));
    });

    // VideoView internally sets its height to match-parent. Forcefully resize it to match the video height.
    exoPlayerManager.setOnVideoSizeChangeListener((resizedVideoWidth, resizedVideoHeight, actualVideoWidth, actualVideoHeight) -> {
      Views.setHeight(videoView, resizedVideoHeight + videoControlsView.getBottomExtraSpaceForProgressSeekBar());
    });

    loadVideo(mediaAlbumItem);
  }

  private void moveToScreenState(ScreenState screenState) {
    if (screenState == ScreenState.LOADING_VIDEO_OR_READY) {
      contentViewFlipper.setDisplayedChild(contentViewFlipper.indexOfChild(videoView));

    } else if (screenState == ScreenState.FAILED) {
      contentViewFlipper.setDisplayedChild(contentViewFlipper.indexOfChild(contentViewFlipper.findViewById(R.id.albumviewer_video_error_container)));

    } else {
      throw new UnsupportedOperationException();
    }
  }

  private void loadVideo(MediaAlbumItem mediaAlbumItem) {
    moveToScreenState(ScreenState.LOADING_VIDEO_OR_READY);

    String videoUrl = mediaAlbumItem.highDefinitionEnabled()
        ? mediaAlbumItem.mediaLink().highQualityUrl()
        : mediaAlbumItem.mediaLink().lowQualityUrl();

    videoView.setOnErrorListener(error -> {
      moveToScreenState(ScreenState.FAILED);

      ResolvedError resolvedError = errorResolver.resolve(error);
      loadErrorStateView.applyFrom(resolvedError);
      loadErrorStateView.setOnRetryClickListener(o -> loadVideo(mediaAlbumItem));

      if (resolvedError.isUnknown()) {
        Timber.e(error, "Error while loading video: %s", videoUrl);
      }

      return true;    // True to indicate it was handled.
    });

    String cachedVideoUrl = Dank.httpProxyCacheServer().getProxyUrl(videoUrl);
    exoPlayerManager.setVideoUriToPlayInLoop(Uri.parse(cachedVideoUrl));
  }

  @Override
  public void handleMediaItemUpdate(MediaAlbumItem updatedMediaAlbumItem) {
    long positionBeforeReloadMillis = videoView.getCurrentPosition();

    loadVideo(updatedMediaAlbumItem);

    videoView.seekTo(positionBeforeReloadMillis);
  }

  @Override
  public void setUserVisibleHint(boolean isVisibleToUser) {
    super.setUserVisibleHint(isVisibleToUser);
    fragmentVisibleToUserStream.accept(isVisibleToUser);
  }

  private void setupFlickGestures(FlickDismissLayout flickDismissLayout) {
    FlickGestureListener flickListener = super.createFlickGestureListener(((FlickGestureListener.GestureCallbacks) getActivity()));
    flickListener.setContentHeightProvider(new FlickGestureListener.ContentHeightProvider() {
      @Override
      public int getContentHeightForDismissAnimation() {
        return videoView.getHeight();
      }

      @Override
      public int getContentHeightForCalculatingThreshold() {
        return videoView.getHeight();
      }
    });
    flickListener.setOnTouchDownReturnValueProvider((MotionEvent event) -> {
      // Hackkyyy hacckkk. Ugh.
      return !Views.touchLiesOn(loadErrorStateView.getRetryButton(), event.getRawX(), event.getRawY());
    });
    flickDismissLayout.setFlickGestureListener(flickListener);
  }
}

package me.saket.dank.ui.media;

import android.animation.LayoutTransition;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

import com.devbrackets.android.exomedia.ui.widget.VideoView;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import me.saket.dank.R;
import me.saket.dank.data.MediaLink;
import me.saket.dank.di.Dank;
import me.saket.dank.ui.DankFragment;
import me.saket.dank.utils.ExoPlayerManager;
import me.saket.dank.utils.StreamableRepository;
import me.saket.dank.utils.Views;
import me.saket.dank.widgets.DankVideoControlsView;
import me.saket.dank.widgets.binoculars.FlickDismissLayout;
import me.saket.dank.widgets.binoculars.FlickGestureListener;
import timber.log.Timber;

public class MediaVideoFragment extends DankFragment {

  private static final String KEY_MEDIA_ITEM = "mediaItem";

  @BindView(R.id.albumviewervideo_flickdismisslayout) FlickDismissLayout flickDismissViewGroup;
  @BindView(R.id.albumviewervideo_video) VideoView videoView;

  @Inject StreamableRepository streamableRepository;

  private ExoPlayerManager exoPlayerManager;

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
    return layout;
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    // Make the image flick-dismissible.
    setupFlickGestures(flickDismissViewGroup);

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
    });

    MediaAlbumItem mediaAlbumItem = getArguments().getParcelable(KEY_MEDIA_ITEM);
    assert mediaAlbumItem != null;
    boolean loadHighQualityVideo = false; // TODO: Get this from user's data preferences.
    unsubscribeOnDestroy(
        load(mediaAlbumItem.mediaLink(), loadHighQualityVideo)
    );

    // Toggle immersive when the user clicks anywhere.
    videoView.setOnClickListener(v -> ((MediaFragmentCallbacks) getActivity()).onClickMediaItem());

    // VideoView internally sets its height to match-parent. Forcefully resize it to match the video height.
    exoPlayerManager.setOnVideoSizeChangeListener((resizedVideoWidth, resizedVideoHeight, actualVideoWidth, actualVideoHeight) -> {
      Views.setHeight(videoView, resizedVideoHeight + videoControlsView.getBottomExtraSpaceForProgressSeekBar());
    });
  }

  private void setupFlickGestures(FlickDismissLayout flickDismissLayout) {
    FlickGestureListener flickListener = new FlickGestureListener(ViewConfiguration.get(getContext()));
    flickListener.setFlickThresholdSlop(.5f);    // Dismiss once the video is swiped 50% away.
    flickListener.setGestureCallbacks((FlickGestureListener.GestureCallbacks) getActivity());
    flickListener.setContentHeightProvider(() -> videoView.getHeight());
    flickDismissLayout.setFlickGestureListener(flickListener);
  }

  public Disposable load(MediaLink mediaLink, boolean loadHighQualityVideo) {
    return Single.just(mediaLink)
        .flatMap(link -> {
          if (link instanceof MediaLink.StreamableUnresolved) {
            return streamableRepository.video(((MediaLink.StreamableUnresolved) link).videoId());
          } else {
            return Single.just(link);
          }
        })
        .doOnSubscribe(o -> Timber.i("TODO: show loading progress indicator"))
        .map(link -> loadHighQualityVideo ? link.highQualityVideoUrl() : link.lowQualityVideoUrl())
        .subscribe(
            videoUrl -> {
              // TODO Cache: String cachedVideoUrl = Dank.httpProxyCacheServer().getProxyUrl(videoUrl);
              exoPlayerManager.playVideoInLoop(Uri.parse(videoUrl));
            },
            error -> {
              // TODO: 01/04/17 Handle error.
              Timber.e(error, "Couldn't load video");
            }
        );
  }
}

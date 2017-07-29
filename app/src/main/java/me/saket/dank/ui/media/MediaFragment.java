package me.saket.dank.ui.media;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;

import me.saket.dank.R;
import me.saket.dank.utils.GlidePaddingTransformation;
import me.saket.dank.widgets.ZoomableImageView;
import me.saket.dank.widgets.binoculars.FlickDismissLayout;
import me.saket.dank.widgets.binoculars.FlickGestureListener;

/**
 * Contains an image or a video.
 */
public class MediaFragment extends Fragment {

  private static final String KEY_MEDIA_ITEM = "mediaItem";

  private ZoomableImageView imageView;

  interface OnMediaItemClickListener {
    void onClickMediaItem();
  }

  static MediaFragment create(MediaAlbumItem mediaAlbumItem) {
    MediaFragment fragment = new MediaFragment();
    Bundle args = new Bundle(1);
    args.putParcelable(KEY_MEDIA_ITEM, mediaAlbumItem);
    fragment.setArguments(args);
    return fragment;
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);

    if (!(getActivity() instanceof OnMediaItemClickListener)) {
      throw new AssertionError("Activity doesn't implement OnMediaItemClickListener");
    }
    if (!(getActivity() instanceof FlickGestureListener.GestureCallbacks)) {
      throw new AssertionError("Activity doesn't implement FlickGestureListener.GestureCallbacks");
    }
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_page_photo, container, false);
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    imageView = (ZoomableImageView) view.findViewById(R.id.photopage_imageview);
    FlickDismissLayout imageContainerView = (FlickDismissLayout) view.findViewById(R.id.photopage_flickdismisslayout);

    if (view.getParent() instanceof ViewPager) {
      imageView.enableScrollInViewPager((ViewPager) view.getParent());
    }
    imageView.setGestureRotationEnabled(true);

    //noinspection ConstantConditions
    MediaAlbumItem mediaAlbumItem = getArguments().getParcelable(KEY_MEDIA_ITEM);
    int deviceDisplayWidth = getResources().getDisplayMetrics().widthPixels;
    assert mediaAlbumItem != null;

    GlidePaddingTransformation glidePaddingTransformation = new GlidePaddingTransformation(getActivity(), Color.TRANSPARENT) {
      final Size onePxTransparentBorder = new Size(1, 1);
      @Override
      public Size getPadding(int imageWidth, int imageHeight) {
        return onePxTransparentBorder;
      }
    };

    Glide.with(this)
        .load(mediaAlbumItem.mediaLink().optimizedImageUrl(deviceDisplayWidth))
        .asBitmap()
        .transform(glidePaddingTransformation)
        .into(imageView);

    // Make the image flick-dismissible.
    setupFlickGestures(imageContainerView);

    // Toggle immersive when the user clicks anywhere.
    imageView.setOnClickListener(v -> ((OnMediaItemClickListener) getActivity()).onClickMediaItem());
  }

  private void setupFlickGestures(FlickDismissLayout imageContainerView) {
    FlickGestureListener flickListener = new FlickGestureListener(ViewConfiguration.get(getContext()));
    flickListener.setFlickThresholdSlop(.5f, imageView);    // Dismiss once the image is swiped 50% away from its original location.
    flickListener.setGestureCallbacks((FlickGestureListener.GestureCallbacks) getActivity());
    flickListener.setOnGestureIntercepter((deltaY) -> {
      // Don't listen for flick gestures if the image can pan further.
      boolean isScrollingUpwards = deltaY < 0;
      return imageView.canPanFurtherVertically(isScrollingUpwards);
    });
    imageContainerView.setFlickGestureListener(flickListener);
  }

  /**
   * Called when the fragment becomes visible / hidden to the user.
   */
  @Override
  public void setUserVisibleHint(boolean isVisibleToUser) {
    super.setUserVisibleHint(isVisibleToUser);
    if (!isVisibleToUser && imageView != null) {
      // Photo is no longer visible.
      imageView.resetState();
    }
  }
}

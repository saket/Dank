package me.saket.dank.ui.media;

import android.content.Context;
import android.graphics.Bitmap;
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

import butterknife.BindView;
import butterknife.ButterKnife;
import me.saket.dank.R;
import me.saket.dank.utils.GlidePaddingTransformation;
import me.saket.dank.utils.GlideUtils;
import me.saket.dank.widgets.ZoomableImageView;
import me.saket.dank.widgets.binoculars.FlickDismissLayout;
import me.saket.dank.widgets.binoculars.FlickGestureListener;

/**
 * Contains an image or a video.
 */
public class MediaFragment extends Fragment {

  private static final String KEY_MEDIA_ITEM = "mediaItem";

  @BindView(R.id.imageviewer_imageview) ZoomableImageView imageView;
  @BindView(R.id.imageviewer_flickdismisslayout) FlickDismissLayout imageContainerView;

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
    View layout = inflater.inflate(R.layout.fragment_page_photo, container, false);
    ButterKnife.bind(this, layout);
    return layout;
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    imageView.enableScrollInViewPager((ViewPager) view.getParent());
    imageView.setGestureRotationEnabled(true);

    //noinspection ConstantConditions
    MediaAlbumItem mediaAlbumItem = getArguments().getParcelable(KEY_MEDIA_ITEM);
    int deviceDisplayWidth = getResources().getDisplayMetrics().widthPixels;
    assert mediaAlbumItem != null;

    imageView.setVisibility(View.INVISIBLE);
    Glide.with(this)
        .load(mediaAlbumItem.mediaLink().optimizedImageUrl(deviceDisplayWidth))
        .asBitmap()
        .transform(new GlidePaddingTransformation(getActivity(), Color.TRANSPARENT) {
          @Override
          public Size getPadding(int imageWidth, int imageHeight) {
            return new Size(0, 0);
          }
        })
        .listener(new GlideUtils.SimpleRequestListener<String, Bitmap>() {
          @Override
          public void onResourceReady(Bitmap resource) {
            imageView.setVisibility(View.VISIBLE);
          }

          @Override
          public void onException(Exception e) {
            super.onException(e);
          }
        })
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

package me.saket.dank.ui.media;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.bumptech.glide.request.target.Target;
import com.github.rahatarmanahmed.cpv.CircularProgressView;
import com.github.rahatarmanahmed.cpv.CircularProgressViewAdapter;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.saket.dank.R;
import me.saket.dank.ui.DankFragment;
import me.saket.dank.utils.GlidePaddingTransformation;
import me.saket.dank.utils.GlideUtils;
import me.saket.dank.utils.glide.GlideProgressTarget;
import me.saket.dank.widgets.ZoomableImageView;
import me.saket.dank.widgets.binoculars.FlickDismissLayout;
import me.saket.dank.widgets.binoculars.FlickGestureListener;

/**
 * Contains an image or a video.
 */
public class MediaFragment extends DankFragment {

  private static final String KEY_MEDIA_ITEM = "mediaItem";

  @BindView(R.id.imageviewer_flickdismisslayout) FlickDismissLayout imageContainerView;
  @BindView(R.id.imageviewer_imageview) ZoomableImageView imageView;
  @BindView(R.id.imageviewer_progress) CircularProgressView progressView;

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
    super.onCreateView(inflater, container, savedInstanceState);
    View layout = inflater.inflate(R.layout.fragment_page_photo, container, false);
    ButterKnife.bind(this, layout);
    return layout;
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    //noinspection ConstantConditions
    MediaAlbumItem mediaAlbumItem = getArguments().getParcelable(KEY_MEDIA_ITEM);
    int deviceDisplayWidth = getResources().getDisplayMetrics().widthPixels;
    assert mediaAlbumItem != null;

    imageView.setGestureRotationEnabled(true);
    imageView.setVisibility(View.INVISIBLE);
    loadImage(mediaAlbumItem.mediaLink().optimizedImageUrl(deviceDisplayWidth));

    // TODO: remove
//    {
//      progressView.setIndeterminate(true);
//      progressView.startAnimation();
//      progressView.setVisibility(View.VISIBLE);
//    }

    // CircularProgressView smoothly animates the progress, which means there's a certain delay between
    // updating its progress and the radial progress bar reaching the progress. So setting its
    // visibility based on the progress is not an option. We use its provided listener instead to hide
    // progress only once it has reached 100%.
    progressView.addListener(new CircularProgressViewAdapter() {
      @Override
      public void onProgressUpdateEnd(float currentProgress) {
        progressView.setVisibility(currentProgress < 100 ? View.VISIBLE : View.GONE);
      }
    });

    // Make the image flick-dismissible.
    setupFlickGestures(imageContainerView);

    // Toggle immersive when the user clicks anywhere.
    imageView.setOnClickListener(v -> ((OnMediaItemClickListener) getActivity()).onClickMediaItem());
  }

  private void loadImage(String imageUrl) {
    ImageLoadProgressTarget<Bitmap> progressTarget = new ImageLoadProgressTarget<>(new BitmapImageViewTarget(imageView), progressView);
    progressTarget.setModel(imageUrl);

    Glide.with(this)
        .load(imageUrl)
        .asBitmap()

//        .skipMemoryCache(true)
//        .diskCacheStrategy(DiskCacheStrategy.NONE)

        .transform(new GlidePaddingTransformation(getActivity(), Color.TRANSPARENT) {
          @Override
          public Size getPadding(int imageWidth, int imageHeight) {
            return new Size(1, 1);
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
        .into(progressTarget);
  }

  private void setupFlickGestures(FlickDismissLayout imageContainerView) {
    FlickGestureListener flickListener = new FlickGestureListener(ViewConfiguration.get(getContext()), imageView);
    flickListener.setFlickThresholdSlop(.5f, imageView);    // Dismiss once the image is swiped 50% away from its original location.
    flickListener.setGestureCallbacks((FlickGestureListener.GestureCallbacks) getActivity());
    flickListener.setContentHeightProvider(() -> {
      return (int) imageView.getZoomedImageHeight();
    });
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

  private static class ImageLoadProgressTarget<Z> extends GlideProgressTarget<String, Z> {
    private final CircularProgressView progressView;

    public ImageLoadProgressTarget(Target<Z> target, CircularProgressView progressView) {
      super(target);
      this.progressView = progressView;
    }

    @Override
    public float getGranularityPercentage() {
      return 0.1f;
    }

    @Override
    protected void onConnecting() {
    }

    @Override
    protected void onDownloading(long bytesRead, long expectedLength) {
      int progress = (int) (100 * (float) bytesRead / expectedLength);
      progressView.setProgress(progress);
    }

    @Override
    protected void onDownloaded() {
      progressView.setProgress(100);
    }

    @Override
    protected void onDelivered() {
    }
  }
}

package me.saket.dank.ui.media;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.DrawableImageViewTarget;
import com.bumptech.glide.request.target.Target;
import com.github.rahatarmanahmed.cpv.CircularProgressView;
import com.github.rahatarmanahmed.cpv.CircularProgressViewAdapter;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.saket.dank.R;
import me.saket.dank.di.Dank;
import me.saket.dank.ui.DankFragment;
import me.saket.dank.utils.Animations;
import me.saket.dank.utils.MediaHostRepository;
import me.saket.dank.utils.glide.GlidePaddingTransformation;
import me.saket.dank.utils.glide.GlideProgressTarget;
import me.saket.dank.utils.glide.GlideUtils;
import me.saket.dank.widgets.ZoomableImageView;
import me.saket.dank.widgets.binoculars.FlickDismissLayout;
import me.saket.dank.widgets.binoculars.FlickGestureListener;
import timber.log.Timber;

public class MediaImageFragment extends DankFragment {

  private static final String KEY_MEDIA_ITEM = "mediaItem";

  @BindView(R.id.albumviewerimage_flickdismisslayout) FlickDismissLayout flickDismissViewGroup;
  @BindView(R.id.albumviewerimage_imageview) ZoomableImageView imageView;
  @BindView(R.id.albumviewerimage_progress) CircularProgressView progressView;

  @Inject MediaHostRepository mediaHostRepository;

  static MediaImageFragment create(MediaAlbumItem mediaAlbumItem) {
    MediaImageFragment fragment = new MediaImageFragment();
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
    View layout = inflater.inflate(R.layout.fragment_album_viewer_page_image, container, false);
    ButterKnife.bind(this, layout);
    return layout;
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    //noinspection ConstantConditions
    MediaAlbumItem mediaAlbumItem = getArguments().getParcelable(KEY_MEDIA_ITEM);
    assert mediaAlbumItem != null;

    imageView.setGestureRotationEnabled(true);
    imageView.setVisibility(View.INVISIBLE);

    String optimizedImageUrl = mediaHostRepository.findOptimizedQualityImageForDevice(
        mediaAlbumItem.mediaLink().lowQualityUrl(),
        ((MediaFragmentCallbacks) getActivity()).getRedditSuppliedImages(),
        ((MediaFragmentCallbacks) getActivity()).getDeviceDisplayWidth()
    );
    Timber.i("optimizedImageUrl: %s", optimizedImageUrl);
    loadImage(optimizedImageUrl);

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
    setupFlickGestures(flickDismissViewGroup);

    // Toggle immersive when the user clicks anywhere.
    imageView.setOnClickListener(v -> ((MediaFragmentCallbacks) getActivity()).onClickMediaItem());
  }

  private void loadImage(String imageUrl) {
    ImageLoadProgressTarget<Drawable> progressTarget = new ImageLoadProgressTarget<>(new DrawableImageViewTarget(imageView), progressView);
    progressTarget.setModel(getActivity(), imageUrl);

    Glide.with(getActivity())
        .load(imageUrl)
        .apply(RequestOptions.bitmapTransform(new GlidePaddingTransformation(getActivity(), Color.TRANSPARENT) {
          @Override
          public Size getPadding(int imageWidth, int imageHeight) {
            return new Size(1, 1);
          }
        }))
        .listener(new GlideUtils.SimpleRequestListener<Drawable>() {
          @Override
          public void onResourceReady(Drawable resource) {
            imageView.setVisibility(View.VISIBLE);

            // Entry transition.
            imageView.setTranslationY(resource.getIntrinsicHeight() / 20);
            imageView.setRotation(-2);
            imageView.animate()
                .translationY(0f)
                .rotation(0)
                .setInterpolator(Animations.INTERPOLATOR)
                .start();
          }

          @Override
          public void onLoadFailed(Exception e) {
            super.onLoadFailed(e);
          }
        })
        .into(progressTarget);
  }

  private void setupFlickGestures(FlickDismissLayout imageContainerView) {
    FlickGestureListener flickListener = new FlickGestureListener(ViewConfiguration.get(getContext()));
    flickListener.setFlickThresholdSlop(.5f);    // Dismiss once the image is swiped 50% away.
    flickListener.setGestureCallbacks((FlickGestureListener.GestureCallbacks) getActivity());
    flickListener.setContentHeightProvider(() -> (int) imageView.getZoomedImageHeight());
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

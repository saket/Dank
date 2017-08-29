package me.saket.dank.ui.media;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.DrawableImageViewTarget;
import com.bumptech.glide.request.target.Target;
import com.github.rahatarmanahmed.cpv.CircularProgressViewAdapter;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.saket.dank.R;
import me.saket.dank.data.links.MediaLink;
import me.saket.dank.di.Dank;
import me.saket.dank.utils.Animations;
import me.saket.dank.utils.FileSizeUnit;
import me.saket.dank.utils.MediaHostRepository;
import me.saket.dank.utils.glide.GlidePaddingTransformation;
import me.saket.dank.utils.glide.GlideProgressTarget;
import me.saket.dank.utils.glide.GlideUtils;
import me.saket.dank.widgets.ProgressWithFileSizeView;
import me.saket.dank.widgets.ZoomableImageView;
import me.saket.dank.widgets.binoculars.FlickDismissLayout;
import me.saket.dank.widgets.binoculars.FlickGestureListener;

public class MediaImageFragment extends BaseMediaViewerFragment {

  private static final String KEY_MEDIA_ITEM = "mediaItem";

  @BindView(R.id.albumviewerimage_flickdismisslayout) FlickDismissLayout flickDismissViewGroup;
  @BindView(R.id.albumviewerimage_imageview) ZoomableImageView imageView;
  @BindView(R.id.albumviewerimage_progress) ProgressWithFileSizeView progressView;
  @BindView(R.id.albumviewerimage_title_description) MediaAlbumViewerTitleDescriptionView titleDescriptionView;
  @BindView(R.id.albumviewerimage_title_description_dimming) View titleDescriptionBackgroundDimmingView;

  @Inject MediaHostRepository mediaHostRepository;

  private MediaAlbumItem mediaAlbumItem;

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
    ViewGroup layout = (ViewGroup) inflater.inflate(R.layout.fragment_album_viewer_page_image, container, false);
    ButterKnife.bind(this, layout);

    mediaAlbumItem = getArguments().getParcelable(KEY_MEDIA_ITEM);
    //noinspection ConstantConditions
    super.setMediaLink(mediaAlbumItem.mediaLink());
    super.setTitleDescriptionView(titleDescriptionView);
    super.setImageDimmingView(titleDescriptionBackgroundDimmingView);

    return layout;
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    imageView.setGestureRotationEnabled(true);
    imageView.setVisibility(View.INVISIBLE);    // Becomes VISIBLE when the image actually loads.

    MediaLink mediaLinkToShow = mediaAlbumItem.mediaLink();
    String optimizedImageUrl = mediaHostRepository.findOptimizedQualityImageForDevice(
        mediaLinkToShow.lowQualityUrl(),
        ((MediaFragmentCallbacks) getActivity()).getRedditSuppliedImages(),
        ((MediaFragmentCallbacks) getActivity()).getDeviceDisplayWidth()
    );
    loadImage(optimizedImageUrl);

    // CircularProgressView smoothly animates the progress, which means there's a certain delay between
    // updating its progress and the radial progress bar reaching the progress. So setting its
    // visibility based on the progress is not an option. We use its provided listener instead to hide
    // progress only once it has reached 100%.
    progressView.addProgressAnimationListener(new CircularProgressViewAdapter() {
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
    ImageLoadProgressTarget<Drawable> targetWithProgress = new ImageLoadProgressTarget<>(new DrawableImageViewTarget(imageView), progressView);
    targetWithProgress.setModel(getActivity(), imageUrl);

    Glide.with(this)
        .load(imageUrl)
        // Adding a 1px transparent border improves anti-aliasing.
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
        .into(targetWithProgress);
  }

  private void setupFlickGestures(FlickDismissLayout imageContainerView) {
    FlickGestureListener flickListener = super.createFlickGestureListener(((FlickGestureListener.GestureCallbacks) getActivity()));
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
      titleDescriptionView.resetScrollY();
      imageView.resetState();
    }
  }

  private static class ImageLoadProgressTarget<Z> extends GlideProgressTarget<String, Z> {
    private final ProgressWithFileSizeView progressWithFileSizeView;

    public ImageLoadProgressTarget(Target<Z> target, ProgressWithFileSizeView progressWithFileSizeView) {
      super(target);
      this.progressWithFileSizeView = progressWithFileSizeView;
    }

    @Override
    public float getGranularityPercentage() {
      return 0.1f;
    }

    @Override
    protected void onConnecting() {
    }

    @Override
    protected void onDownloading(long bytesRead, long expectedBytes) {
      int progress = (int) (100 * (float) bytesRead / expectedBytes);
      progressWithFileSizeView.setFileSizeBytes(expectedBytes, FileSizeUnit.BYTES);
      progressWithFileSizeView.setProgress(progress);
    }

    @Override
    protected void onDownloaded() {
      progressWithFileSizeView.setProgress(100);
    }

    @Override
    protected void onDelivered() {
    }
  }
}

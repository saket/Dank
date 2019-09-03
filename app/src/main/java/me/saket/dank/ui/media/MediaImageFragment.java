package me.saket.dank.ui.media;

import android.animation.LayoutTransition;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Size;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.DrawableImageViewTarget;
import com.bumptech.glide.request.target.Target;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import dagger.Lazy;
import me.saket.dank.R;
import me.saket.dank.data.ErrorResolver;
import me.saket.dank.data.ResolvedError;
import me.saket.dank.di.Dank;
import me.saket.dank.ui.submission.adapter.ImageWithMultipleVariants;
import me.saket.dank.utils.Animations;
import me.saket.dank.utils.FileSizeUnit;
import me.saket.dank.utils.Views;
import me.saket.dank.utils.glide.GlidePaddingTransformation;
import me.saket.dank.utils.glide.GlideProgressTarget;
import me.saket.dank.utils.glide.GlideUtils.SimpleRequestListener;
import me.saket.dank.widgets.ErrorStateView;
import me.saket.dank.widgets.MediaAlbumViewerTitleDescriptionView;
import me.saket.dank.widgets.ProgressWithFileSizeView;
import me.saket.dank.widgets.ZoomableImageView;
import me.saket.dank.widgets.binoculars.FlickDismissLayout;
import me.saket.dank.widgets.binoculars.FlickGestureListener;
import timber.log.Timber;

public class MediaImageFragment extends BaseMediaViewerFragment {

  private static final String KEY_MEDIA_ITEM = "mediaItem";

  @BindView(R.id.albumviewer_image_flickdismisslayout) FlickDismissLayout flickDismissViewGroup;
  @BindView(R.id.albumviewer_image_imageview) ZoomableImageView imageView;
  @BindView(R.id.albumviewer_image_progress) ProgressWithFileSizeView progressView;
  @BindView(R.id.albumviewer_image_title_description) MediaAlbumViewerTitleDescriptionView titleDescriptionView;
  @BindView(R.id.albumviewer_image_title_description_dimming) View titleDescriptionBackgroundDimmingView;
  @BindView(R.id.albumviewer_image_long_image_scroll_hint) View longImageScrollHint;
  @BindView(R.id.albumviewer_image_error_container) ViewGroup loadErrorContainerView;
  @BindView(R.id.albumviewer_image_error) ErrorStateView loadErrorStateView;

  @Inject MediaHostRepository mediaHostRepository;
  @Inject Lazy<ErrorResolver> errorResolver;

  private enum ScreenState {
    LOADING_IMAGE,
    IMAGE_READY,
    FAILED
  }

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

    //noinspection ConstantConditions
    MediaAlbumItem mediaAlbumItem = getArguments().getParcelable(KEY_MEDIA_ITEM);
    //noinspection ConstantConditions
    super.setMediaLink(mediaAlbumItem.mediaLink());
    super.setTitleDescriptionView(titleDescriptionView);
    super.setImageDimmingView(titleDescriptionBackgroundDimmingView);

    calculateUrlAndLoadImage(mediaAlbumItem, true);

    return layout;
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    // Make the image flick-dismissible.
    setupFlickGestures(flickDismissViewGroup);

    // Toggle immersive when the user clicks anywhere.
    imageView.setOnClickListener(v -> {
      // https://app.bugsnag.com/uncommon-dot-is/dank/errors/5af6efaef194ec001852de34
      if (isAdded()) {
        ((MediaFragmentCallbacks) requireActivity()).toggleImmersiveMode();
      }
    });

    // Show title and description above the Activity option buttons.
    //noinspection ConstantConditions
    ((MediaFragmentCallbacks) getActivity()).optionButtonsHeight()
        .takeUntil(lifecycle().onDestroy().ignoreElements())
        .subscribe(
            optionButtonsHeight -> {
              int defaultBottomMargin = getResources().getDimensionPixelSize(R.dimen.mediaalbumviewer_image_scroll_hint_bottom_margin);
              Views.setMarginBottom(longImageScrollHint, optionButtonsHeight + defaultBottomMargin);
            }, error -> {
              ResolvedError resolvedError = errorResolver.get().resolve(error);
              resolvedError.ifUnknown(() -> Timber.e(error, "Error while trying to get option buttons' height"));
            });
  }

  /**
   * Called when the fragment becomes visible / hidden to the user.
   */
  @Override
  public void setUserVisibleHint(boolean isVisibleToUser) {
    super.setUserVisibleHint(isVisibleToUser);

    // Checking imageView for nullability because setUserVisibleHint() gets called even before onCreateView().
    if (imageView != null && !isVisibleToUser) {
      // Photo is no longer visible.
      titleDescriptionView.resetScrollY();
      imageView.resetState();
    }
  }

  @Override
  public void handleMediaItemUpdate(MediaAlbumItem updatedMediaAlbumItem) {
    calculateUrlAndLoadImage(updatedMediaAlbumItem, false);
  }

  private void moveToScreenState(ScreenState screenState) {
    loadErrorContainerView.setVisibility(screenState == ScreenState.FAILED ? View.VISIBLE : View.GONE);
    progressView.setVisibility(screenState == ScreenState.LOADING_IMAGE ? View.VISIBLE : View.GONE);
    imageView.setVisibility(screenState == ScreenState.IMAGE_READY ? View.VISIBLE : View.INVISIBLE);
  }

  private void calculateUrlAndLoadImage(MediaAlbumItem mediaAlbumItemToShow, boolean isFirstLoad) {
    moveToScreenState(ScreenState.LOADING_IMAGE);

    ((MediaFragmentCallbacks) requireActivity()).getRedditSuppliedImages()
        .takeUntil(lifecycle().onDestroyCompletable())
        .subscribe(
            redditImages -> {
              String imageUrl;
              if (mediaAlbumItemToShow.highDefinitionEnabled()) {
                imageUrl = mediaAlbumItemToShow.mediaLink().highQualityUrl();

              } else {
                String lowQualityUrl = mediaAlbumItemToShow.mediaLink().lowQualityUrl();
                if (mediaAlbumItemToShow.mediaLink().isGif()) {
                  imageUrl = lowQualityUrl;


                } else {
                  int deviceDisplayWidth = ((MediaFragmentCallbacks) requireActivity()).getDeviceDisplayWidth();
                  ImageWithMultipleVariants imageWithMultipleVariants = ImageWithMultipleVariants.Companion.of(redditImages);
                  imageUrl = imageWithMultipleVariants.findNearestFor(deviceDisplayWidth, lowQualityUrl);
                }
              }

              loadImage(mediaAlbumItemToShow, isFirstLoad, imageUrl, false);
              imageView.setOnImageTooLargeExceptionListener(e -> {
                Timber.e("Failed to draw image: %s, url: %s", e.getMessage(), imageUrl);
                loadImage(mediaAlbumItemToShow, isFirstLoad, imageUrl, true);
              });
            }, error -> {
              ResolvedError resolvedError = errorResolver.get().resolve(error);
              resolvedError.ifUnknown(() -> Timber.e(error, "Error while trying to get option buttons' height"));
            });
  }

  private void loadImage(MediaAlbumItem mediaAlbumItemToShow, boolean isFirstLoad, String imageUrl, boolean downSampleToFixError) {
    DrawableImageViewTarget target = new DrawableImageViewTarget(imageView.view());
    ImageLoadProgressTarget<Drawable> targetWithProgress = new ImageLoadProgressTarget<>(target, progressView);
    targetWithProgress.setModel(requireActivity(), imageUrl);

    Size deviceDisplaySize = new Size(getResources().getDisplayMetrics().widthPixels, getResources().getDisplayMetrics().heightPixels);

    RequestOptions options = new RequestOptions()
        .priority(Priority.IMMEDIATE)
        .transform(new GlidePaddingTransformation(requireActivity(), Color.TRANSPARENT) {
          @Override
          public Size getPadding(int imageWidth, int imageHeight) {
            // Adding a 1px transparent border improves anti-aliasing when rotating image (flick-dismiss).
            return new Size(1, 1);
          }
        });
    //.apply(new RequestOptions().skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE))

    // Glide sometimes fails to load even tiny images with a "Failed to draw image: Canvas:
    // trying to draw too large(118103056bytes) bitmap."
    RequestOptions downscaledOptions;
    if (downSampleToFixError) {
      downscaledOptions = options
          .downsample(DownsampleStrategy.AT_LEAST)
          .override(deviceDisplaySize.getWidth(), deviceDisplaySize.getHeight());
    } else {
      downscaledOptions = options;
    }

    Glide.with(this)
        .load(imageUrl)
        .apply(downscaledOptions)
        .listener(new SimpleRequestListener<Drawable>() {
          @Override
          public void onResourceReady(Drawable drawable) {
            moveToScreenState(ScreenState.IMAGE_READY);

            Views.executeOnMeasure(imageView.view(), () -> {
              int deviceDisplayWidth = getResources().getDisplayMetrics().widthPixels;
              float widthResizeFactor = deviceDisplayWidth / (float) drawable.getMinimumWidth();
              float resizedImageHeight = drawable.getIntrinsicHeight() * widthResizeFactor;
              boolean isImageLongerThanWindow = resizedImageHeight > imageView.getHeight();

              if (isImageLongerThanWindow) {
                imageView.setGravity(Gravity.TOP);
                showImageScrollHint(drawable.getIntrinsicHeight(), imageView.getHeight());
              }

              if (isFirstLoad) {
                // Entry transition.
                imageView.setTranslationY(drawable.getIntrinsicHeight() / 20);
                imageView.setRotation(-2);
                imageView.animate()
                    .translationY(0f)
                    .rotation(0)
                    .setInterpolator(Animations.INTERPOLATOR)
                    .start();
              }
            });
          }

          @Override
          public void onLoadFailed(@Nullable Exception e) {
            moveToScreenState(ScreenState.FAILED);

            ResolvedError resolvedError = errorResolver.get().resolve(e);
            loadErrorStateView.applyFrom(resolvedError);
            loadErrorStateView.setOnRetryClickListener(o -> calculateUrlAndLoadImage(mediaAlbumItemToShow, isFirstLoad));

            if (resolvedError.isUnknown()) {
              Timber.e(e, "Error while loading image: %s", imageUrl);
            }
          }
        })
        .into(targetWithProgress);
  }

  private void setupFlickGestures(FlickDismissLayout imageContainerView) {
    //noinspection ConstantConditions
    FlickGestureListener flickListener = super.createFlickGestureListener(((FlickGestureListener.GestureCallbacks) getActivity()));
    flickListener.setContentHeightProvider(new FlickGestureListener.ContentHeightProvider() {
      @Override
      public int getContentHeightForDismissAnimation() {
        return (int) imageView.getZoomedImageHeight();
      }

      @Override
      public int getContentHeightForCalculatingThreshold() {
        // A non-MATCH_PARENT height is important so that the user can easily dismiss the image if it's taking too long to load.
        if (!imageView.hasImage()) {
          return getResources().getDimensionPixelSize(R.dimen.mediaalbumviewer_image_height_when_empty);
        }
        return (int) imageView.getVisibleZoomedImageHeight();
      }
    });
    flickListener.setOnGestureIntercepter((deltaY) -> {
      // Don't listen for flick gestures if the image can pan further.
      boolean isScrollingUpwards = deltaY < 0;
      return imageView.canPanFurtherVertically(isScrollingUpwards);
    });
    imageContainerView.setFlickGestureListener(flickListener);
  }

  /**
   * Show a tooltip at the bottom of the image, hinting the user that the image is long and can be scrolled.
   */
  private void showImageScrollHint(float imageHeight, float visibleImageHeight) {
    longImageScrollHint.setVisibility(View.VISIBLE);
    longImageScrollHint.setAlpha(0f);

    // Postpone till measure because we need the height.
    Views.executeOnMeasure(longImageScrollHint, () -> {
      longImageScrollHint.setTranslationY(longImageScrollHint.getHeight() / 2);
      longImageScrollHint.animate()
          .alpha(1f)
          .translationY(0f)
          .setDuration(300)
          .setInterpolator(Animations.INTERPOLATOR)
          .start();
    });

    // Hide the tooltip once the user starts scrolling the image.
    imageView.addOnImagePanChangeListener(new ZoomableImageView.OnPanChangeListener() {
      private boolean hidden = false;

      // FIXME: Unregister the listener instead of maintaining state. And everyone knows using a boolean is hack anyway.
      @Override
      public void onPanChange(float scrollY) {
        if (hidden) {
          return;
        }

        float distanceScrolledY = Math.abs(scrollY);
        float distanceScrollableY = imageHeight - visibleImageHeight;
        float scrolledPercentage = distanceScrolledY / distanceScrollableY;

        // Hide it after the image has been scrolled 10% of its height.
        if (scrolledPercentage > 0.1f) {
          hidden = true;
          longImageScrollHint.animate()
              .alpha(0f)
              .translationY(-longImageScrollHint.getHeight() / 2)
              .setDuration(300)
              .setInterpolator(Animations.INTERPOLATOR)
              .withEndAction(() -> {
                ViewGroup parent = (ViewGroup) longImageScrollHint.getParent();
                LayoutTransition layoutTransitionBak = parent.getLayoutTransition();
                parent.setLayoutTransition(null);
                longImageScrollHint.setVisibility(View.GONE);
                parent.setLayoutTransition(layoutTransitionBak);
              })
              .start();
        }
      }
    });
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
      progressWithFileSizeView.setProgress(0);
    }

    @Override
    protected void onDownloading(long bytesRead, long expectedBytes) {
      int progress = (int) (100 * (float) bytesRead / expectedBytes);
      progressWithFileSizeView.setFileSize(expectedBytes, FileSizeUnit.BYTES);
      progressWithFileSizeView.setProgress(progress);
    }

    // Not called when the image is fetched from cache.
    @Override
    protected void onDownloaded() {
      progressWithFileSizeView.setProgress(100);
    }

    @Override
    protected void onDelivered() {
      progressWithFileSizeView.setProgress(100);
    }
  }
}

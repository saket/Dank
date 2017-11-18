package me.saket.dank.ui.media;

import android.animation.LayoutTransition;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Size;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.alexvasilkov.gestures.GestureController;
import com.alexvasilkov.gestures.State;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.DrawableImageViewTarget;
import com.bumptech.glide.request.target.Target;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.saket.dank.R;
import me.saket.dank.data.ErrorResolver;
import me.saket.dank.data.ResolvedError;
import me.saket.dank.di.Dank;
import me.saket.dank.utils.Animations;
import me.saket.dank.utils.FileSizeUnit;
import me.saket.dank.utils.Views;
import me.saket.dank.utils.glide.GlidePaddingTransformation;
import me.saket.dank.utils.glide.GlideProgressTarget;
import me.saket.dank.utils.glide.GlideUtils;
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
  @Inject ErrorResolver errorResolver;

  private ScreenState currentScreenState;

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

    MediaAlbumItem mediaAlbumItem = getArguments().getParcelable(KEY_MEDIA_ITEM);
    //noinspection ConstantConditions
    super.setMediaLink(mediaAlbumItem.mediaLink());
    super.setTitleDescriptionView(titleDescriptionView);
    super.setImageDimmingView(titleDescriptionBackgroundDimmingView);

    loadImage(mediaAlbumItem, true);

    return layout;
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    // Make the image flick-dismissible.
    setupFlickGestures(flickDismissViewGroup);

    // Toggle immersive when the user clicks anywhere.
    imageView.setOnClickListener(v -> ((MediaFragmentCallbacks) getActivity()).onClickMediaItem());

    // Show title and description above the Activity option buttons.
    unsubscribeOnDestroy(
        ((MediaFragmentCallbacks) getActivity()).optionButtonsHeight()
            .subscribe(optionButtonsHeight -> {
              int defaultBottomMargin = getResources().getDimensionPixelSize(R.dimen.mediaalbumviewer_image_scroll_hint_bottom_margin);
              Views.setMarginBottom(longImageScrollHint, optionButtonsHeight + defaultBottomMargin);
            })
    );
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
    loadImage(updatedMediaAlbumItem, false);
  }

  private void moveToScreenState(ScreenState screenState) {
    currentScreenState = screenState;

    loadErrorContainerView.setVisibility(screenState == ScreenState.FAILED ? View.VISIBLE : View.GONE);
    progressView.setVisibility(screenState == ScreenState.LOADING_IMAGE ? View.VISIBLE : View.GONE);
    imageView.setVisibility(screenState == ScreenState.IMAGE_READY ? View.VISIBLE : View.INVISIBLE);
  }

  private void loadImage(MediaAlbumItem mediaAlbumItemToShow, boolean isFirstLoad) {
    moveToScreenState(ScreenState.LOADING_IMAGE);

    String imageUrl;

    if (mediaAlbumItemToShow.highDefinitionEnabled()) {
      imageUrl = mediaAlbumItemToShow.mediaLink().highQualityUrl();

    } else {
      imageUrl = mediaHostRepository.findOptimizedQualityImageForDisplay(
          ((MediaFragmentCallbacks) getActivity()).getRedditSuppliedImages(),
          ((MediaFragmentCallbacks) getActivity()).getDeviceDisplayWidth(),
          mediaAlbumItemToShow.mediaLink().lowQualityUrl()
      );
    }

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
        //.apply(new RequestOptions().skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE))
        .listener(new GlideUtils.SimpleRequestListener<Drawable>() {
          @Override
          public void onResourceReady(Drawable drawable) {
            moveToScreenState(ScreenState.IMAGE_READY);

            if (!imageView.isLaidOut()) {
              throw new AssertionError("ImageView needs to get laid.");
            }

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
          }

          @Override
          public void onLoadFailed(Exception e) {
            moveToScreenState(ScreenState.FAILED);

            ResolvedError resolvedError = errorResolver.resolve(e);
            loadErrorStateView.applyFrom(resolvedError);
            loadErrorStateView.setOnRetryClickListener(o -> loadImage(mediaAlbumItemToShow, isFirstLoad));

            if (resolvedError.isUnknown()) {
              Timber.e(e, "Error while loading image: %s", imageUrl);
            }
          }
        })
        .into(targetWithProgress);
  }

  private void setupFlickGestures(FlickDismissLayout imageContainerView) {
    FlickGestureListener flickListener = super.createFlickGestureListener(((FlickGestureListener.GestureCallbacks) getActivity()));
    flickListener.setContentHeightProvider(new FlickGestureListener.ContentHeightProvider() {
      @Override
      public int getContentHeightForDismissAnimation() {
        return (int) imageView.getZoomedImageHeight();
      }

      @Override
      public int getContentHeightForCalculatingThreshold() {
        // A non-MATCH_PARENT height is important so that the user can easily dismiss the image if it's taking too long to load.
        if (imageView.getDrawable() == null) {
          return getResources().getDimensionPixelSize(R.dimen.mediaalbumviewer_image_height_when_empty);
        }
        return imageView.getDrawable().getIntrinsicHeight();
      }
    });
    flickListener.setOnGestureIntercepter((deltaY) -> {
      // Don't listen for flick gestures if the image can pan further.
      boolean isScrollingUpwards = deltaY < 0;
      return imageView.canPanFurtherVertically(isScrollingUpwards);
    });
    flickListener.setOnTouchDownReturnValueProvider((MotionEvent event) -> {
      switch (currentScreenState) {
        case LOADING_IMAGE:
          // Bug workaround: ViewPager is not calling its canScroll() method when an image is
          // being loaded, no idea why. As a result, flick-to-dismiss does not work until the
          // image is loaded.
          return progressView.getProgress() < 100;

        case IMAGE_READY:
          return false;

        case FAILED:
          // Hackkyyy hacckkk. Ugh.
          return !Views.touchLiesOn(loadErrorStateView.getRetryButton(), event.getRawX(), event.getRawY());

        default:
          throw new UnsupportedOperationException("Unknown screen state");
      }
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
    GestureController.OnStateChangeListener imageScrollListener = new GestureController.OnStateChangeListener() {
      private boolean hidden = false;

      @Override
      public void onStateChanged(State state) {
        if (hidden) {
          return;
        }

        float distanceScrolledY = Math.abs(state.getY());
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

      @Override
      public void onStateReset(State oldState, State newState) {}
    };
    imageView.getController().addOnStateChangeListener(imageScrollListener);
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

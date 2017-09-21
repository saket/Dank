package me.saket.dank.ui.submission;

import static me.saket.dank.utils.Views.executeOnMeasure;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.CheckResult;
import android.support.annotation.Nullable;
import android.util.Size;
import android.view.Gravity;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.alexvasilkov.gestures.GestureController;
import com.alexvasilkov.gestures.State;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.ImageViewTarget;
import com.jakewharton.rxrelay2.PublishRelay;
import com.jakewharton.rxrelay2.Relay;

import net.dean.jraw.models.Thumbnails;

import butterknife.BindColor;
import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import me.saket.dank.R;
import me.saket.dank.data.links.MediaLink;
import me.saket.dank.utils.Animations;
import me.saket.dank.ui.media.MediaHostRepository;
import me.saket.dank.utils.Views;
import me.saket.dank.utils.glide.GlidePaddingTransformation;
import me.saket.dank.utils.glide.GlideUtils;
import me.saket.dank.widgets.InboxUI.ExpandablePageLayout;
import me.saket.dank.widgets.InboxUI.SimpleExpandablePageStateChangeCallbacks;
import me.saket.dank.widgets.ScrollingRecyclerViewSheet;
import me.saket.dank.widgets.ZoomableImageView;
import timber.log.Timber;

/**
 * Manages showing of content image in {@link SubmissionFragment}. Only supports showing a single image right now.
 */
public class SubmissionImageHolder {

  private final MediaHostRepository mediaHostRepository;
  @BindView(R.id.submission_image_scroll_hint) View imageScrollHintView;
  @BindView(R.id.submission_image) ZoomableImageView imageView;
  @BindView(R.id.submission_comment_list_parent_sheet) ScrollingRecyclerViewSheet commentListParentSheet;
  @BindColor(R.color.submission_media_content_background_padding) int paddingColorForSmallImages;

  private final int deviceDisplayWidth;
  private final ExpandablePageLayout submissionPageLayout;
  private final ProgressBar contentLoadProgressView;
  private final Relay<Drawable> imageStream = PublishRelay.create();
  private final GlidePaddingTransformation glidePaddingTransformation;
  private GestureController.OnStateChangeListener imageScrollListener;

  /**
   * God knows why (if he/she exists), ButterKnife is failing to bind <var>contentLoadProgressView</var>,
   * so we're supplying it manually from the fragment.
   */
  public SubmissionImageHolder(View submissionLayout, ProgressBar contentLoadProgressView, ExpandablePageLayout submissionPageLayout,
      MediaHostRepository mediaHostRepository, int deviceDisplayWidth)
  {
    ButterKnife.bind(this, submissionLayout);
    this.mediaHostRepository = mediaHostRepository;
    this.submissionPageLayout = submissionPageLayout;
    this.contentLoadProgressView = contentLoadProgressView;
    this.deviceDisplayWidth = deviceDisplayWidth;

    imageView.setGravity(Gravity.TOP);

    // Reset everything when the page is collapsed.
    submissionPageLayout.addStateChangeCallbacks(new SimpleExpandablePageStateChangeCallbacks() {
      @Override
      public void onPageCollapsed() {
        resetViews();
      }
    });

    // This transformation adds padding to images that are way too small (presently < 2 x toolbar height).
    glidePaddingTransformation = new GlidePaddingTransformation(imageView.getContext(), paddingColorForSmallImages) {
      @Override
      public Size getPadding(int imageWidth, int imageHeight) {
        int minDesiredImageHeight = commentListParentSheet.getTop() * 2;
        float widthResizeFactor = deviceDisplayWidth / (float) imageWidth;  // Because ZoomableImageView will resize the image to fill space.

        if (imageHeight < minDesiredImageHeight) {
          // Image is too small to be displayed.
          int minImageHeightBeforeResizing = (int) (minDesiredImageHeight / widthResizeFactor);
          return new Size(0, (minImageHeightBeforeResizing - imageHeight) / 2);

        } else {
          return new Size(0, 0);
        }
      }
    };
  }

  // TODO Do not hold bitmaps. They might get recycled.
  @CheckResult
  @Deprecated
  public Observable<Bitmap> streamImageBitmaps() {
    return imageStream.map(drawable -> getBitmapFromDrawable(drawable));
  }

  private void resetViews() {
    if (imageScrollListener != null) {
      imageView.getController().removeOnStateChangeListener(imageScrollListener);
    }
    Glide.with(imageView).clear(imageView);
    imageScrollHintView.setVisibility(View.GONE);
  }

  public void load(MediaLink contentLink, Thumbnails redditSuppliedImages) {
    contentLoadProgressView.setIndeterminate(true);
    contentLoadProgressView.setVisibility(View.VISIBLE);

    Glide.with(imageView)
        .load(mediaHostRepository.findOptimizedQualityImageForDisplay(redditSuppliedImages, deviceDisplayWidth, contentLink.lowQualityUrl()))
        .apply(new RequestOptions()
            .priority(Priority.IMMEDIATE)
            .transform(glidePaddingTransformation)
        )
        .listener(new GlideUtils.SimpleRequestListener<Drawable>() {
          @Override
          public void onResourceReady(Drawable drawable) {
            executeOnMeasure(imageView, () -> {
              float widthResizeFactor = deviceDisplayWidth / (float) drawable.getMinimumWidth();
              float imageHeight = drawable.getIntrinsicHeight() * widthResizeFactor;
              float visibleImageHeight = Math.min(imageHeight, imageView.getHeight());

              // Reveal the image smoothly or right away depending upon whether or not this
              // page is already expanded and visible.
              Views.executeOnNextLayout(commentListParentSheet, () -> {
                int imageHeightMinusToolbar = (int) (visibleImageHeight - commentListParentSheet.getTop());
                commentListParentSheet.setScrollingEnabled(true);
                commentListParentSheet.setMaxScrollY(imageHeightMinusToolbar);
                commentListParentSheet.scrollTo(imageHeightMinusToolbar, submissionPageLayout.isExpanded() /* smoothScroll */);

                if (imageHeight > visibleImageHeight) {
                  // Image is scrollable. Let the user know about this.
                  showImageScrollHint(imageHeight, visibleImageHeight);
                }
              });
            });

            imageStream.accept(drawable);
            contentLoadProgressView.setVisibility(View.GONE);
          }

          @Override
          public void onLoadFailed(Exception e) {
            Timber.e("Couldn't load image");
            contentLoadProgressView.setVisibility(View.GONE);

            // TODO: 04/04/17 Show a proper error.
            Toast.makeText(imageView.getContext(), "Couldn't load image", Toast.LENGTH_SHORT).show();
          }
        })
        .into(new ImageViewTarget<Drawable>(imageView) {
          @Override
          protected void setResource(@Nullable Drawable resource) {
            imageView.setImageDrawable(resource);
          }
        });
  }

  /**
   * Show a tooltip at the bottom of the image, hinting the user that the image is long and can be scrolled.
   */
  private void showImageScrollHint(float imageHeight, float visibleImageHeight) {
    imageScrollHintView.setVisibility(View.VISIBLE);
    imageScrollHintView.setAlpha(0f);

    // Postpone till measure because we need the height.
    executeOnMeasure(imageScrollHintView, () -> {
      Runnable hintEntryAnimationRunnable = () -> {
        imageScrollHintView.setTranslationY(imageScrollHintView.getHeight() / 2);
        imageScrollHintView.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(300)
            .setInterpolator(Animations.INTERPOLATOR)
            .start();
      };

      // Show the hint only when the page has expanded.
      if (submissionPageLayout.isExpanded()) {
        hintEntryAnimationRunnable.run();
      } else {
        submissionPageLayout.addStateChangeCallbacks(new SimpleExpandablePageStateChangeCallbacks() {
          @Override
          public void onPageExpanded() {
            hintEntryAnimationRunnable.run();
            submissionPageLayout.removeStateChangeCallbacks(this);
          }
        });
      }
    });

    // Hide the tooltip once the user starts scrolling the image.
    imageScrollListener = new GestureController.OnStateChangeListener() {
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
          imageScrollHintView.animate()
              .alpha(0f)
              .translationY(-imageScrollHintView.getHeight() / 2)
              .setDuration(300)
              .setInterpolator(Animations.INTERPOLATOR)
              .withEndAction(() -> imageScrollHintView.setVisibility(View.GONE))
              .start();
        }
      }

      @Override
      public void onStateReset(State oldState, State newState) {
      }
    };
    imageView.getController().addOnStateChangeListener(imageScrollListener);
  }

  private static Bitmap getBitmapFromDrawable(Drawable drawable) {
    if (drawable instanceof BitmapDrawable) {
      return ((BitmapDrawable) drawable).getBitmap();
    } else if (drawable instanceof GifDrawable) {
      return ((GifDrawable) drawable).getFirstFrame();
    } else {
      throw new IllegalStateException("Unknown Drawable: " + drawable);
    }
  }
}

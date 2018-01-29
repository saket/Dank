package me.saket.dank.ui.submission;

import static io.reactivex.android.schedulers.AndroidSchedulers.mainThread;
import static io.reactivex.schedulers.Schedulers.io;
import static me.saket.dank.utils.Views.executeOnMeasure;

import android.graphics.Bitmap;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.CheckResult;
import android.util.Size;
import android.view.Gravity;
import android.view.View;
import android.widget.ProgressBar;

import com.alexvasilkov.gestures.GestureController;
import com.alexvasilkov.gestures.State;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.RequestOptions;
import com.jakewharton.rxrelay2.PublishRelay;
import com.jakewharton.rxrelay2.Relay;

import net.dean.jraw.models.Thumbnails;

import butterknife.BindColor;
import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.Consumer;
import me.saket.dank.R;
import me.saket.dank.data.links.MediaLink;
import me.saket.dank.ui.submission.adapter.ImageWithMultipleVariants;
import me.saket.dank.utils.Animations;
import me.saket.dank.utils.Views;
import me.saket.dank.utils.glide.GlidePaddingTransformation;
import me.saket.dank.widgets.InboxUI.ExpandablePageLayout;
import me.saket.dank.widgets.InboxUI.SimpleExpandablePageStateChangeCallbacks;
import me.saket.dank.widgets.ScrollingRecyclerViewSheet;
import me.saket.dank.widgets.ZoomableImageView;
import timber.log.Timber;

/**
 * Manages showing of content image in {@link SubmissionPageLayout}. Only supports showing a single image right now.
 */
public class SubmissionImageHolder {

  // TO ensure that both CachePreFiller and SubmissionImageHolder are in sync.
  public static final boolean LOAD_LOW_QUALITY_IMAGES = true;

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
  public SubmissionImageHolder(
      SubmissionPageLifecycleStreams lifecycleStreams,
      View submissionLayout,
      ProgressBar contentLoadProgressView,
      ExpandablePageLayout submissionPageLayout,
      int deviceDisplayWidth)
  {
    ButterKnife.bind(this, submissionLayout);
    this.submissionPageLayout = submissionPageLayout;
    this.contentLoadProgressView = contentLoadProgressView;
    this.deviceDisplayWidth = deviceDisplayWidth;

    imageView.setGravity(Gravity.TOP);

    // Reset everything when the page is collapsed.
    lifecycleStreams.onPageCollapseOrDestroy()
        .subscribe(resetViews());

    // This transformation adds padding to images that are way too small (presently < 2 x toolbar height).
    glidePaddingTransformation = new GlidePaddingTransformation(imageView.getContext(), paddingColorForSmallImages) {
      @Override
      public Size getPadding(int imageWidth, int imageHeight) {
        float minDesiredImageHeight = commentListParentSheet.getTop() * 2;

        // Because ZoomableImageView will resize the image to fill space.
        float widthResizeFactor = deviceDisplayWidth / (float) imageWidth;
        float resizedHeight = imageHeight * widthResizeFactor;

        if (resizedHeight < minDesiredImageHeight) {
          // Image is too small to be displayed.
          return new Size(0, (int) ((minDesiredImageHeight - resizedHeight) / 2));
        } else {
          return new Size(0, 0);
        }
      }
    };
  }

  @CheckResult
  public Observable<Bitmap> streamImageBitmaps() {
    return imageStream.map(SubmissionImageHolder::bitmapFromDrawable);
  }

  private Consumer<Object> resetViews() {
    return o -> {
      if (imageScrollListener != null) {
        imageView.getController().removeOnStateChangeListener(imageScrollListener);
      }
      imageScrollHintView.setVisibility(View.GONE);
    };
  }

  /**
   * Note: image loading is canceled in {@link #resetViews()}.
   */
  @CheckResult
  public Completable load(MediaLink mediaLink, Thumbnails redditSuppliedThumbnails) {
    if (!LOAD_LOW_QUALITY_IMAGES) {
      throw new AssertionError();
    }

    contentLoadProgressView.setVisibility(View.VISIBLE);

    return Single
        .fromCallable(() -> {
          // Images supplied by Reddit are static, so cannot optimize for GIFs.
          String defaultImageUrl = mediaLink.lowQualityUrl();
          String optimizedImageUrl = mediaLink.isGif()
              ? defaultImageUrl
              : ImageWithMultipleVariants.of(redditSuppliedThumbnails).findNearestFor(deviceDisplayWidth, defaultImageUrl);

          return Glide.with(imageView)
              .load(optimizedImageUrl)
              .apply(new RequestOptions()
                  .priority(Priority.IMMEDIATE)
                  .transform(glidePaddingTransformation))
              .submit()
              .get();
        })
        .subscribeOn(io())
        .observeOn(mainThread())
        .doOnSuccess(drawable -> {
          imageView.setImageDrawable(drawable);
          if (drawable instanceof Animatable) {
            ((Animatable) drawable).start();
          }
        })
        .doOnSuccess(drawable -> contentLoadProgressView.setVisibility(View.GONE))
        .flatMap(drawable -> Views.rxWaitTillMeasured(imageView).toSingleDefault(drawable))
        .doOnSuccess(drawable -> {
          float widthResizeFactor = deviceDisplayWidth / (float) drawable.getIntrinsicWidth();
          float imageHeight = drawable.getIntrinsicHeight() * widthResizeFactor;
          float visibleImageHeight = Math.min(imageHeight, imageView.getHeight());

          // Reveal the image smoothly if the page is already expanded or right away if it's not.
          commentListParentSheet.post(() -> {
            int imageHeightMinusToolbar = (int) (visibleImageHeight - commentListParentSheet.getTop());
            commentListParentSheet.setScrollingEnabled(true);
            commentListParentSheet.setMaxScrollY(imageHeightMinusToolbar);
            commentListParentSheet.scrollTo(imageHeightMinusToolbar, submissionPageLayout.isExpanded() /* smoothScroll */);

            if (imageHeight > visibleImageHeight) {
              // Image is scrollable. Let the user know about this.
              showImageScrollHint(imageHeight, visibleImageHeight);
            }
          });
        })
        .doOnSuccess(imageStream)
        .doOnError(e -> Timber.e(e, "Couldn't load image"))
        .doOnError(e -> contentLoadProgressView.setVisibility(View.GONE))
        .toCompletable();
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

  private static Bitmap bitmapFromDrawable(Drawable drawable) {
    if (drawable instanceof BitmapDrawable) {
      return ((BitmapDrawable) drawable).getBitmap();
    } else if (drawable instanceof GifDrawable) {
      return ((GifDrawable) drawable).getFirstFrame();
    } else {
      throw new IllegalStateException("Unknown Drawable: " + drawable);
    }
  }
}

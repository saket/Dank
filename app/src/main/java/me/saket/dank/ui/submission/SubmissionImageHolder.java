package me.saket.dank.ui.submission;

import android.graphics.Bitmap;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Size;
import android.view.Gravity;
import android.view.View;

import androidx.annotation.CheckResult;
import androidx.annotation.Nullable;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.RequestOptions;
import com.jakewharton.rxrelay2.PublishRelay;
import com.jakewharton.rxrelay2.Relay;

import net.dean.jraw.models.SubmissionPreview;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import dagger.Lazy;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.functions.Consumer;
import me.saket.dank.R;
import me.saket.dank.ui.UiEvent;
import me.saket.dank.ui.submission.events.SubmissionImageLoadStarted;
import me.saket.dank.ui.submission.events.SubmissionImageLoadSucceeded;
import me.saket.dank.urlparser.MediaLink;
import me.saket.dank.utils.Animations;
import me.saket.dank.utils.Optional;
import me.saket.dank.utils.Views;
import me.saket.dank.widgets.InboxUI.SimpleExpandablePageStateChangeCallbacks;
import me.saket.dank.widgets.ScrollingRecyclerViewSheet;
import me.saket.dank.widgets.ZoomableImageView;

import static io.reactivex.android.schedulers.AndroidSchedulers.mainThread;
import static io.reactivex.schedulers.Schedulers.io;
import static me.saket.dank.utils.Views.executeOnMeasure;

/**
 * Manages showing of content image in {@link SubmissionPageLayout}. Only supports showing a single image right now.
 */
public class SubmissionImageHolder {

  @BindView(R.id.submission_image_scroll_hint) View imageScrollHintView;
  @BindView(R.id.submission_image) ZoomableImageView imageView;
  @BindView(R.id.submission_comment_list_parent_sheet) ScrollingRecyclerViewSheet commentListParentSheet;

  private final Lazy<SubmissionImageLoader> imageLoader;

  private Relay<UiEvent> uiEvents;
  private SubmissionPageLifecycleStreams lifecycle;
  private SubmissionPageLayout submissionPageLayout;
  private Size deviceDisplaySize;
  private Relay<Drawable> imageStream = PublishRelay.create();
  private ZoomableImageView.OnPanChangeListener imagePanListener;

  @Inject
  public SubmissionImageHolder(Lazy<SubmissionImageLoader> imageLoader) {
    this.imageLoader = imageLoader;
  }

  /**
   * God knows why (if he/she exists), ButterKnife is failing to bind
   * <var>contentLoadProgressView</var>, so it's being manually supplied.
   */
  public void setup(
      Relay<UiEvent> uiEvents,
      SubmissionPageLifecycleStreams lifecycleStreams,
      View submissionLayout,
      SubmissionPageLayout submissionPageLayout,
      Size deviceDisplaySize)
  {
    this.uiEvents = uiEvents;
    this.lifecycle = lifecycleStreams;
    this.submissionPageLayout = submissionPageLayout;
    this.deviceDisplaySize = deviceDisplaySize;

    ButterKnife.bind(this, submissionLayout);

    imageView.setGravity(Gravity.TOP);

    // Reset everything when the page is collapsed.
    lifecycleStreams.onPageCollapseOrDestroy()
        .subscribe(resetViews());
  }

  @CheckResult
  public Observable<Optional<Bitmap>> streamImageBitmaps() {
    return imageStream.map(SubmissionImageHolder::bitmapFromDrawable);
  }

  private Consumer<Object> resetViews() {
    return o -> {
      if (imagePanListener != null) {
        imageView.removeOnImagePanChangeListener(imagePanListener);
      }
      imageView.resetState();
      imageScrollHintView.setVisibility(View.GONE);
    };
  }

  @CheckResult
  public Completable load(MediaLink mediaLink, @Nullable SubmissionPreview redditSuppliedThumbnails) {
    uiEvents.accept(SubmissionImageLoadStarted.create());

    RequestOptions imageLoadOptions = RequestOptions.priorityOf(Priority.IMMEDIATE);

    return imageLoader.get().load(imageView.getContext(), mediaLink, redditSuppliedThumbnails, io(), imageLoadOptions)
        .observeOn(mainThread())
        .doOnSuccess(drawable -> {
          imageView.setImageDrawable(drawable);
          if (drawable instanceof Animatable) {
            ((Animatable) drawable).start();
          }
        })
        .doOnSuccess(o -> uiEvents.accept(SubmissionImageLoadSucceeded.create()))
        .flatMap(drawable -> Views.rxWaitTillMeasured(imageView.view()).toSingleDefault(drawable))
        .doOnSuccess(drawable -> {
          float widthResizeFactor = deviceDisplaySize.getWidth() / (float) drawable.getIntrinsicWidth();
          float imageHeight = drawable.getIntrinsicHeight() * widthResizeFactor;
          float visibleImageHeight = Math.min(imageHeight, imageView.getHeight());

          commentListParentSheet.post(() -> {
            int imageHeightMinusToolbar = (int) (visibleImageHeight - commentListParentSheet.getTop());
            commentListParentSheet.setScrollingEnabled(true);
            commentListParentSheet.setMaxScrollY(imageHeightMinusToolbar);

            if (submissionPageLayout.shouldExpandMediaSmoothly()) {
              if (submissionPageLayout.isExpanded()) {
                commentListParentSheet.smoothScrollTo(imageHeightMinusToolbar);
              } else {
                lifecycle.onPageExpand()
                    .take(1)
                    .takeUntil(lifecycle.onPageCollapseOrDestroy())
                    .subscribe(o -> commentListParentSheet.smoothScrollTo(imageHeightMinusToolbar));
              }
            } else {
              commentListParentSheet.scrollTo(imageHeightMinusToolbar);
            }

            if (imageHeight > visibleImageHeight) {
              // Image is scrollable. Let the user know about this.
              showImageScrollHint(imageHeight, visibleImageHeight);
            }
          });
        })
        .doOnSuccess(imageStream)
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
    imagePanListener = new ZoomableImageView.OnPanChangeListener() {
      private boolean hidden = false;

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
          imageScrollHintView.animate()
              .alpha(0f)
              .translationY(-imageScrollHintView.getHeight() / 2)
              .setDuration(300)
              .setInterpolator(Animations.INTERPOLATOR)
              .withEndAction(() -> imageScrollHintView.setVisibility(View.GONE))
              .start();
        }
      }
    };
    imageView.addOnImagePanChangeListener(imagePanListener);
  }

  private static Optional<Bitmap> bitmapFromDrawable(Drawable drawable) {
    if (drawable instanceof BitmapDrawable) {
      return Optional.of(((BitmapDrawable) drawable).getBitmap());
    } else if (drawable instanceof GifDrawable) {
      // First frame of a GIF can be null if it has already started playing
      // and the first frame has been thrown away.
      return Optional.ofNullable(((GifDrawable) drawable).getFirstFrame());
    } else {
      throw new IllegalStateException("Unknown Drawable: " + drawable);
    }
  }
}

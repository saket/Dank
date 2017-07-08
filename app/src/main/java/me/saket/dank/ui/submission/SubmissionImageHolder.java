package me.saket.dank.ui.submission;

import static me.saket.dank.utils.Views.executeOnMeasure;

import android.view.Gravity;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.alexvasilkov.gestures.GestureController;
import com.alexvasilkov.gestures.State;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.saket.dank.R;
import me.saket.dank.data.MediaLink;
import me.saket.dank.utils.Animations;
import me.saket.dank.utils.GlideUtils;
import me.saket.dank.utils.Views;
import me.saket.dank.widgets.InboxUI.ExpandablePageLayout;
import me.saket.dank.widgets.InboxUI.SimpleExpandablePageStateChangeCallbacks;
import me.saket.dank.widgets.ScrollingRecyclerViewSheet;
import me.saket.dank.widgets.ZoomableImageView;
import timber.log.Timber;

/**
 * Manages showing of content image in {@link SubmissionFragment}. Only supports showing a single image right now.
 */
public class SubmissionImageHolder {

  @BindView(R.id.submission_image_scroll_hint) View imageScrollHintView;
  @BindView(R.id.submission_image) ZoomableImageView imageView;
  @BindView(R.id.submission_comment_list_parent_sheet) ScrollingRecyclerViewSheet commentListParentSheet;

  private final int deviceDisplayWidth;
  private final ExpandablePageLayout submissionPageLayout;
  private final ProgressBar contentLoadProgressView;

  private GestureController.OnStateChangeListener imageScrollListener;

  /**
   * God knows why (if he/she exists), ButterKnife is failing to bind <var>contentLoadProgressView</var>,
   * so we're supplying it manually from the fragment.
   */
  public SubmissionImageHolder(View submissionLayout, ProgressBar contentLoadProgressView, ExpandablePageLayout submissionPageLayout,
      int deviceDisplayWidth)
  {
    ButterKnife.bind(this, submissionLayout);
    this.submissionPageLayout = submissionPageLayout;
    this.contentLoadProgressView = contentLoadProgressView;
    this.deviceDisplayWidth = deviceDisplayWidth;
  }

  public void setup() {
    imageView.setGravity(Gravity.TOP);

    // Reset everything when the page is collapsed.
    submissionPageLayout.addStateCallbacks(new SimpleExpandablePageStateChangeCallbacks() {
      @Override
      public void onPageCollapsed() {
        resetViews();
      }
    });
  }

  private void resetViews() {
    if (imageScrollListener != null) {
      imageView.getController().removeOnStateChangeListener(imageScrollListener);
    }
    Glide.clear(imageView);
    imageScrollHintView.setVisibility(View.GONE);
  }

  public void load(MediaLink contentLink) {
    contentLoadProgressView.setIndeterminate(true);
    contentLoadProgressView.setVisibility(View.VISIBLE);

    Glide.with(imageView.getContext())
        .load(contentLink.optimizedImageUrl(deviceDisplayWidth))
        .priority(Priority.IMMEDIATE)
        .listener(new GlideUtils.SimpleRequestListener<String, GlideDrawable>() {
          @Override
          public void onResourceReady(GlideDrawable resource) {
            executeOnMeasure(imageView, () -> {
              float widthResizeFactor = deviceDisplayWidth / (float) resource.getMinimumWidth();
              float imageHeight = resource.getIntrinsicHeight() * widthResizeFactor;
              float visibleImageHeight = Math.min(imageHeight, imageView.getHeight());

              // Reveal the image smoothly or right away depending upon whether or not this
              // page is already expanded and visible.
              Views.executeOnNextLayout(commentListParentSheet, () -> {
                int revealDistance = (int) (visibleImageHeight - commentListParentSheet.getTop());
                commentListParentSheet.setPeekHeight(commentListParentSheet.getHeight() - revealDistance);
                //Timber.i("revealDistance: %s", revealDistance);
                //Timber.i("visibleImageHeight: %s", visibleImageHeight);
                //Timber.i("commentListParentSheet.getTop(): %s", commentListParentSheet.getTop());
                // TODO: 04/04/17 How do we handle images smaller than the toolbar's bottom?

                if (revealDistance < 0) {
                  Toast.makeText(imageView.getContext(), "Image is smaller than the toolbar+status bar", Toast.LENGTH_SHORT).show();
                }

                commentListParentSheet.setScrollingEnabled(true);
                commentListParentSheet.scrollTo(revealDistance, submissionPageLayout.isExpanded() /* smoothScroll */);

                if (imageHeight > visibleImageHeight) {
                  // Image is scrollable. Let the user know about this.
                  showImageScrollHint(imageHeight, visibleImageHeight);
                }
              });
            });

            contentLoadProgressView.setVisibility(View.GONE);
          }

          @Override
          public void onException(Exception e) {
            Timber.e("Couldn't load image");
            contentLoadProgressView.setVisibility(View.GONE);

            // TODO: 04/04/17 Show a proper error.
            Toast.makeText(imageView.getContext(), "Couldn't load image", Toast.LENGTH_SHORT).show();
          }
        })
        .into(imageView);
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
        submissionPageLayout.addStateCallbacks(new SimpleExpandablePageStateChangeCallbacks() {
          @Override
          public void onPageExpanded() {
            hintEntryAnimationRunnable.run();
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

}

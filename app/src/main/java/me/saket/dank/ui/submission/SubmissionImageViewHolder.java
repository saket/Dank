package me.saket.dank.ui.submission;

import static me.saket.dank.utils.Views.executeOnMeasure;

import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.view.Gravity;
import android.view.View;
import android.widget.Toast;

import com.alexvasilkov.gestures.GestureController;
import com.alexvasilkov.gestures.State;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;

import me.saket.dank.data.MediaLink;
import me.saket.dank.utils.GlideUtils;
import me.saket.dank.utils.Views;
import me.saket.dank.widgets.InboxUI.ExpandablePageLayout;
import me.saket.dank.widgets.InboxUI.SimpleExpandablePageCallbacks;
import me.saket.dank.widgets.ScrollingRecyclerViewSheet;
import me.saket.dank.widgets.SubmissionAnimatedProgressBar;
import me.saket.dank.widgets.ZoomableImageView;

/**
 * Manages showing of content image in {@link SubmissionFragment}.
 */
public class SubmissionImageViewHolder {

    private final int deviceDisplayWidth;
    private final View imageScrollHintView;
    private final ZoomableImageView imageView;
    private final ExpandablePageLayout submissionPageLayout;
    private final SubmissionAnimatedProgressBar contentLoadProgressView;
    private final ScrollingRecyclerViewSheet commentListParentSheet;

    private GestureController.OnStateChangeListener imageScrollListener;

    public SubmissionImageViewHolder(ExpandablePageLayout submissionPageLayout, SubmissionAnimatedProgressBar contentLoadProgressView,
            ZoomableImageView imageView, View imageScrollHintView, ScrollingRecyclerViewSheet commentListParentSheet,
            int deviceDisplayWidth)
    {
        this.submissionPageLayout = submissionPageLayout;
        this.contentLoadProgressView = contentLoadProgressView;
        this.imageView = imageView;
        this.imageScrollHintView = imageScrollHintView;
        this.commentListParentSheet = commentListParentSheet;
        this.deviceDisplayWidth = deviceDisplayWidth;
    }

    public void setup() {
        imageView.setGravity(Gravity.TOP);

        // Reset everything when the page is collapsed.
        submissionPageLayout.addCallbacks(new SimpleExpandablePageCallbacks() {
            @Override
            public void onPageCollapsed() {
                if (imageScrollListener != null) {
                    imageView.getController().removeOnStateChangeListener(imageScrollListener);
                }
                Glide.clear(imageView);
                imageScrollHintView.setVisibility(View.GONE);
            }
        });
    }

    public void load(MediaLink contentLink) {
        if (contentLink instanceof MediaLink.Imgur) {
            if (((MediaLink.Imgur) contentLink).isAlbum()) {
                contentLoadProgressView.hide();
                Toast.makeText(imageView.getContext(), "Imgur album", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        contentLoadProgressView.setIndeterminate(true);
        contentLoadProgressView.show();

        Glide.with(imageView.getContext())
                .load(contentLink.optimizedImageUrl(deviceDisplayWidth))
                .priority(Priority.IMMEDIATE)
                .listener(new GlideUtils.SimpleRequestListener<String, GlideDrawable>() {
                    @Override
                    public void onResourceReady(GlideDrawable resource) {
                        executeOnMeasure(imageView, () -> {
                            float widthResizeFactor = deviceDisplayWidth / (float) resource.getMinimumWidth();
                            float imageHeight = resource.getIntrinsicHeight() * widthResizeFactor;
                            float visibleImageHeight = (int) Math.min(imageHeight, imageView.getHeight());

                            // Reveal the image smoothly or right away depending upon whether or not this
                            // page is already expanded and visible.
                            Views.executeOnNextLayout(commentListParentSheet, () -> {
                                int revealDistance = (int) (visibleImageHeight - commentListParentSheet.getTop());
                                commentListParentSheet.setPeekHeight(commentListParentSheet.getHeight() - revealDistance);
                                //Timber.i("revealDistance: %s", revealDistance);
                                //Timber.i("visibleImageHeight: %s", visibleImageHeight);
                                //Timber.i("commentListParentSheet.getTop(): %s", commentListParentSheet.getTop());
                                // TODO: 04/04/17 How do we handle images smaller than the toolbar's bottom?

                                commentListParentSheet.setScrollingEnabled(true);
                                commentListParentSheet.scrollTo(revealDistance, submissionPageLayout.isExpanded() /* smoothScroll */);

                                if (imageHeight > visibleImageHeight) {
                                    // Image is scrollable. Let the user know about this.
                                    showImageScrollHint(imageHeight, visibleImageHeight);
                                }
                            });
                        });

                        contentLoadProgressView.hide();
                    }
                })
                .into(imageView);
    }

    private void showImageScrollHint(float imageHeight, float visibleImageHeight) {
        imageScrollHintView.setVisibility(View.VISIBLE);
        imageScrollHintView.setAlpha(0f);

        executeOnMeasure(imageScrollHintView, () -> {
            Runnable hintEntryAnimationRunnable = () -> {
                imageScrollHintView.setTranslationY(imageScrollHintView.getHeight() / 2);
                imageScrollHintView.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(300)
                        .setInterpolator(new FastOutSlowInInterpolator())
                        .start();
            };

            // Show the hint only when the page has expanded.
            if (submissionPageLayout.isExpanded()) {
                hintEntryAnimationRunnable.run();
            } else {
                submissionPageLayout.addCallbacks(new SimpleExpandablePageCallbacks() {
                    @Override
                    public void onPageExpanded() {
                        hintEntryAnimationRunnable.run();
                    }
                });
            }
        });

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

                if (scrolledPercentage > 0.1f) {
                    hidden = true;
                    imageScrollHintView.animate()
                            .alpha(0f)
                            .translationY(-imageScrollHintView.getHeight() / 2)
                            .setDuration(300)
                            .setInterpolator(new FastOutSlowInInterpolator())
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

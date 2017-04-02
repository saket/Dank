package me.saket.dank.ui.submission;

import static me.saket.dank.utils.Views.executeOnMeasure;

import android.view.Gravity;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;

import me.saket.dank.data.MediaLink;
import me.saket.dank.utils.GlideUtils;
import me.saket.dank.utils.Views;
import me.saket.dank.widgets.InboxUI.ExpandablePageLayout;
import me.saket.dank.widgets.ScrollingRecyclerViewSheet;
import me.saket.dank.widgets.SubmissionAnimatedProgressBar;
import me.saket.dank.widgets.ZoomableImageView;
import timber.log.Timber;

/**
 * Manages showing of content image in {@link SubmissionFragment}.
 */
public class SubmissionImageViewHolder {

    private ExpandablePageLayout submissionPageLayout;
    private SubmissionAnimatedProgressBar contentLoadProgressView;
    private ZoomableImageView contentImageView;
    private ScrollingRecyclerViewSheet commentListParentSheet;
    private int deviceDisplayWidth;

    public SubmissionImageViewHolder(ExpandablePageLayout submissionPageLayout, SubmissionAnimatedProgressBar contentLoadProgressView,
            ZoomableImageView contentImageView, ScrollingRecyclerViewSheet commentListParentSheet, int deviceDisplayWidth)
    {
        this.submissionPageLayout = submissionPageLayout;
        this.contentLoadProgressView = contentLoadProgressView;
        this.contentImageView = contentImageView;
        this.commentListParentSheet = commentListParentSheet;
        this.deviceDisplayWidth = deviceDisplayWidth;
    }

    public void setup() {
        contentImageView.setGravity(Gravity.TOP);
    }

    public void load(MediaLink contentLink) {
        if (contentLink instanceof MediaLink.Imgur) {
            if (((MediaLink.Imgur) contentLink).isAlbum()) {
                contentLoadProgressView.hide();
                Toast.makeText(contentImageView.getContext(), "Imgur album", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        contentLoadProgressView.setIndeterminate(true);
        contentLoadProgressView.show();

        Glide.with(contentImageView.getContext())
                .load(contentLink.optimizedImageUrl(deviceDisplayWidth))
                .priority(Priority.IMMEDIATE)
                .listener(new GlideUtils.SimpleRequestListener<String, GlideDrawable>() {
                    @Override
                    public void onResourceReady(GlideDrawable resource) {
                        executeOnMeasure(contentImageView, () -> {
                            int imageMaxVisibleHeight = contentImageView.getHeight();
                            float widthResizeFactor = deviceDisplayWidth / (float) resource.getMinimumWidth();
                            int visibleImageHeight = Math.min((int) (resource.getIntrinsicHeight() * widthResizeFactor), imageMaxVisibleHeight);

                            // Reveal the image smoothly or right away depending upon whether or not this
                            // page is already expanded and visible.
                            Views.executeOnNextLayout(commentListParentSheet, () -> {
                                int revealDistance = visibleImageHeight - commentListParentSheet.getTop();
                                commentListParentSheet.setPeekHeight(commentListParentSheet.getHeight() - revealDistance);
                                Timber.i("revealDistance: %s", revealDistance);
                                Timber.i("visibleImageHeight: %s", visibleImageHeight);
                                Timber.i("commentListParentSheet.getTop(): %s", commentListParentSheet.getTop());

                                commentListParentSheet.setScrollingEnabled(true);
                                commentListParentSheet.scrollTo(revealDistance, submissionPageLayout.isExpanded() /* smoothScroll */);
                            });
                        });

                        contentLoadProgressView.hide();
                    }
                })
                .into(contentImageView);
    }

}

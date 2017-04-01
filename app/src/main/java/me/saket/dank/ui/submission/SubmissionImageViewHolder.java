package me.saket.dank.ui.submission;

import static me.saket.dank.utils.GlideUtils.simpleImageViewTarget;
import static me.saket.dank.utils.Views.executeOnMeasure;

import android.view.Gravity;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;

import me.saket.dank.data.MediaLink;
import me.saket.dank.utils.Views;
import me.saket.dank.widgets.InboxUI.ExpandablePageLayout;
import me.saket.dank.widgets.ScrollingRecyclerViewSheet;
import me.saket.dank.widgets.SubmissionAnimatedProgressBar;
import me.saket.dank.widgets.ZoomableImageView;

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
        } else if (contentLink instanceof MediaLink.Gfycat) {
            contentLoadProgressView.hide();
            Toast.makeText(contentImageView.getContext(), "GFYCAT", Toast.LENGTH_SHORT).show();
            return;
        }

        contentLoadProgressView.setIndeterminate(true);
        contentLoadProgressView.show();

        Glide.with(contentImageView.getContext())
                .load(contentLink.optimizedImageUrl(deviceDisplayWidth))
                .priority(Priority.IMMEDIATE)
                .into(simpleImageViewTarget(contentImageView, resource -> {
                    executeOnMeasure(contentImageView, () -> {
                        contentLoadProgressView.hide();

                        int imageMaxVisibleHeight = contentImageView.getHeight();
                        float widthResizeFactor = deviceDisplayWidth / (float) resource.getMinimumWidth();
                        int visibleImageHeight = Math.min((int) (resource.getIntrinsicHeight() * widthResizeFactor), imageMaxVisibleHeight);

                        contentImageView.setImageDrawable(resource);

                        // Reveal the image smoothly or right away depending upon whether or not this
                        // page is already expanded and visible.
                        Views.executeOnNextLayout(commentListParentSheet, () -> {
                            int revealDistance = visibleImageHeight - commentListParentSheet.getTop();
                            commentListParentSheet.setPeekHeight(commentListParentSheet.getHeight() - revealDistance);

                            commentListParentSheet.setScrollingEnabled(true);
                            commentListParentSheet.scrollTo(revealDistance, submissionPageLayout.isExpanded() /* smoothScroll */);
                        });
                    });
                }));
    }

}

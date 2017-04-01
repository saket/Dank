package me.saket.dank.ui.submission;

import static com.google.common.base.Preconditions.checkNotNull;
import static me.saket.dank.utils.Views.executeOnNextLayout;
import static me.saket.dank.utils.Views.setHeight;

import android.net.Uri;

import com.devbrackets.android.exomedia.ui.widget.VideoView;

import me.saket.dank.data.MediaLink;
import me.saket.dank.di.Dank;
import me.saket.dank.utils.ExoPlayerManager;
import me.saket.dank.widgets.InboxUI.ExpandablePageLayout;
import me.saket.dank.widgets.ScrollingRecyclerViewSheet;
import me.saket.dank.widgets.SubmissionAnimatedProgressBar;
import timber.log.Timber;

/**
 * Manages loading of video in {@link SubmissionFragment}.
 */
public class SubmissionVideoViewHolder {

    private ExpandablePageLayout submissionPageLayout;
    private SubmissionAnimatedProgressBar contentLoadProgressView;
    private VideoView contentVideoView;
    private ScrollingRecyclerViewSheet commentListParentSheet;
    private ExoPlayerManager exoPlayerManager;

    public SubmissionVideoViewHolder(ExpandablePageLayout submissionPageLayout, SubmissionAnimatedProgressBar contentLoadProgressView,
            VideoView contentVideoView, ScrollingRecyclerViewSheet commentListParentSheet, ExoPlayerManager exoPlayerManager)
    {
        this.submissionPageLayout = checkNotNull(submissionPageLayout);
        this.contentLoadProgressView = checkNotNull(contentLoadProgressView);
        this.contentVideoView = checkNotNull(contentVideoView);
        this.commentListParentSheet = checkNotNull(commentListParentSheet);
        this.exoPlayerManager = checkNotNull(exoPlayerManager);
    }

    // TODO: 01/04/17 Move this logic to UrlParser.
    public void load(MediaLink videoLink) {
        String videoUrl = videoLink.url();
        if (videoUrl.endsWith("gifv")) {
            videoUrl = videoUrl.substring(0, videoUrl.length() - "gifv".length()) + "mp4";
        }
        Timber.i("videoUrl: %s", videoUrl);
        String cacheUri = Dank.httpProxyCacheServer().getProxyUrl(videoUrl);
        load(Uri.parse(cacheUri));
    }

    private void load(Uri videoUri) {
        contentLoadProgressView.show();

        Timber.d("load()");
        exoPlayerManager.setOnVideoSizeChangeListener((videoWidth, videoHeight) -> {
            setHeight(contentVideoView, videoHeight);

            // Wait for the height change to happen and then reveal the video.
            executeOnNextLayout(contentVideoView, () -> {
                contentLoadProgressView.hide();
                commentListParentSheet.setScrollingEnabled(true);

                int revealDistance = contentVideoView.getHeight() - commentListParentSheet.getTop();
                commentListParentSheet.setPeekHeight(commentListParentSheet.getHeight() - revealDistance);
                commentListParentSheet.scrollTo(revealDistance, submissionPageLayout.isExpandedOrExpanding() /* smoothScroll */);

                exoPlayerManager.setOnVideoSizeChangeListener(null);
            });
        });

        exoPlayerManager.playVideoInLoop(videoUri);
    }

}

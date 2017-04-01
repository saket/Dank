package me.saket.dank.ui.submission;

import static com.google.common.base.Preconditions.checkNotNull;
import static me.saket.dank.utils.RxUtils.applySchedulersSingle;
import static me.saket.dank.utils.RxUtils.logError;
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
import rx.Single;
import rx.Subscription;
import rx.functions.Action1;

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

    public Subscription load(MediaLink mediaLink) {
        Single<String> videoUrlObservable = mediaLink instanceof MediaLink.StreamableUnknown
                ? getStreamableVideoDetails(((MediaLink.StreamableUnknown) mediaLink))
                : Single.just(mediaLink.lowQualityVideoUrl());

        return videoUrlObservable
                .doOnSubscribe(() -> contentLoadProgressView.show())
                .subscribe(load(), error -> {
                    // TODO: 01/04/17 Handle error.
                    logError("Couldn't load video").call(error);
                });
    }

    private Action1<String> load() {
        return videoUrl -> {
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

            String cachedVideoUrl = Dank.httpProxyCacheServer().getProxyUrl(videoUrl);
            exoPlayerManager.playVideoInLoop(Uri.parse(cachedVideoUrl));
        };
    }

    // TODO: 01/04/17 Cache.
    private Single<String> getStreamableVideoDetails(MediaLink.StreamableUnknown streamableLink) {
        return Dank.api()
                .streamableVideoDetails(streamableLink.videoId())
                .compose(applySchedulersSingle())
                .map(response -> "https://" + response.files().lowQualityVideo().url());
    }

}

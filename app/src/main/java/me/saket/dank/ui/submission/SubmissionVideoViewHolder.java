package me.saket.dank.ui.submission;

import static me.saket.dank.utils.RxUtils.applySchedulersSingle;
import static me.saket.dank.utils.RxUtils.logError;
import static me.saket.dank.utils.Views.executeOnNextLayout;
import static me.saket.dank.utils.Views.setHeight;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.support.v7.graphics.Palette;
import android.view.View;
import android.view.ViewGroup;

import com.devbrackets.android.exomedia.core.video.exo.ExoTextureVideoView;
import com.devbrackets.android.exomedia.ui.widget.VideoView;

import me.saket.dank.R;
import me.saket.dank.data.MediaLink;
import me.saket.dank.di.Dank;
import me.saket.dank.utils.Colors;
import me.saket.dank.utils.ExoPlayerManager;
import me.saket.dank.widgets.DankVideoControlsView;
import me.saket.dank.widgets.InboxUI.ExpandablePageLayout;
import me.saket.dank.widgets.ScrollingRecyclerViewSheet;
import me.saket.dank.widgets.SubmissionAnimatedProgressBar;
import rx.Single;
import rx.Subscription;
import rx.functions.Action1;
import timber.log.Timber;

/**
 * Manages loading of video in {@link SubmissionFragment}.
 */
public class SubmissionVideoViewHolder {

    private ExpandablePageLayout submissionPageLayout;
    private SubmissionAnimatedProgressBar contentLoadProgressView;
    private ViewGroup contentVideoViewContainer;
    private VideoView contentVideoView;
    private ScrollingRecyclerViewSheet commentListParentSheet;
    private ExoPlayerManager exoPlayerManager;

    private Bitmap videoBitmap;
    private DankVideoControlsView controlsView;

    public SubmissionVideoViewHolder(ExpandablePageLayout submissionPageLayout, SubmissionAnimatedProgressBar contentLoadProgressView,
            VideoView contentVideoView, ViewGroup contentVideoViewContainer, ScrollingRecyclerViewSheet commentListParentSheet,
            ExoPlayerManager exoPlayerManager)
    {
        this.submissionPageLayout = submissionPageLayout;
        this.contentLoadProgressView = contentLoadProgressView;
        this.contentVideoViewContainer = contentVideoViewContainer;
        this.contentVideoView = contentVideoView;
        this.commentListParentSheet = commentListParentSheet;
        this.exoPlayerManager = exoPlayerManager;
    }

    public void setup() {
        controlsView = new DankVideoControlsView(contentVideoView.getContext());
        contentVideoView.setControls(controlsView);
        controlsView.insertSeekBarIn(contentVideoViewContainer);
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

                    int revealDistance = contentVideoViewContainer.getHeight() - commentListParentSheet.getTop();
                    commentListParentSheet.setPeekHeight(commentListParentSheet.getHeight() - revealDistance);
                    commentListParentSheet.scrollTo(revealDistance, submissionPageLayout.isExpanded() /* smoothScroll */);

                    exoPlayerManager.setOnVideoSizeChangeListener(null);
                });
            });

            String cachedVideoUrl = Dank.httpProxyCacheServer().getProxyUrl(videoUrl);
            exoPlayerManager.playVideoInLoop(Uri.parse(cachedVideoUrl));

            tintVideoControlsWithVideo();
        };
    }

    private void tintVideoControlsWithVideo() {
        LayerDrawable seekBarDrawable = (LayerDrawable) controlsView.getProgressSeekBar().getProgressDrawable();
        Drawable progressDrawable = seekBarDrawable.findDrawableByLayerId(android.R.id.progress);
        Drawable bufferDrawable = seekBarDrawable.findDrawableByLayerId(android.R.id.secondaryProgress);

        View viewById = contentVideoView.findViewById(R.id.exomedia_video_view);
        ExoTextureVideoView textureVideoView = (ExoTextureVideoView) viewById;

        // TODO: Move this to an async code block.
        // TODO: Transition colors smoothly + use default colors on start.

        controlsView.setVideoProgressChangeListener(() -> {
            final long startTime = System.currentTimeMillis();
            if (videoBitmap == null) {
                videoBitmap = Bitmap.createBitmap(textureVideoView.getResources().getDisplayMetrics(), 10, 10, Bitmap.Config.RGB_565);
            }

            Palette.from(textureVideoView.getBitmap(videoBitmap))
                    .generate(palette -> {
                        int progressColor = palette.getMutedColor(palette.getVibrantColor(-1));
                        if (progressColor != -1) {
                            progressDrawable.setTint(progressColor);
                            bufferDrawable.setTint(Colors.applyAlpha(progressColor, 2f));
                            controlsView.setVideoProgressChangeListener(null);
                        }
                    });
            //Timber.i("Palette in: %sms", System.currentTimeMillis() - startTime);
        });
    }

    // TODO: 01/04/17 Cache.
    private Single<String> getStreamableVideoDetails(MediaLink.StreamableUnknown streamableLink) {
        return Dank.api()
                .streamableVideoDetails(streamableLink.videoId())
                .compose(applySchedulersSingle())
                .map(response -> "https://" + response.files().lowQualityVideo().url());
    }

    public void pausePlayback() {
        exoPlayerManager.pauseVideoPlayback();
    }
}


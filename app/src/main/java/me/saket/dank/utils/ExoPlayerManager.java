package me.saket.dank.utils;

import android.net.Uri;
import android.view.Surface;
import android.view.ViewGroup;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.decoder.DecoderCounters;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.LoopingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.video.VideoRendererEventListener;

import me.saket.dank.ui.DankFragment;
import timber.log.Timber;

public class ExoPlayerManager {

    private SimpleExoPlayer exoPlayer;
    private SimpleExoPlayerView playerView;

    public static ExoPlayerManager newInstance(DankFragment fragment, SimpleExoPlayerView playerView) {
        ExoPlayerManager exoPlayerManager = new ExoPlayerManager(playerView);

        exoPlayerManager.setupVideoView();

        fragment.lifecycleEvents().subscribe(event -> {
            switch (event) {
                case RESUME:
                    exoPlayerManager.setupVideoView();
                    break;

                case PAUSE:
                    exoPlayerManager.release();
                    break;
            }
        });

        return exoPlayerManager;
    }

    public ExoPlayerManager(SimpleExoPlayerView playerView) {
        this.playerView = playerView;
    }

    public void setupVideoView() {
        Timber.i("setupVideoView()");
        if (exoPlayer != null) {
            return;
        }

        // 1. Create a default TrackSelector
        BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);
        TrackSelector trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);

        // 2. Create a default LoadControl
        LoadControl loadControl = new DefaultLoadControl();

        // 3. Create the exoPlayer
        exoPlayer = ExoPlayerFactory.newSimpleInstance(playerView.getContext(), trackSelector, loadControl);
        playerView.setPlayer(exoPlayer);
        playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH);

        exoPlayer.setVideoDebugListener(new SimpleVideoRendererEventListener() {
            @Override
            public void onVideoSizeChanged(int videoWidth, int videoHeight, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
                float widthFactor = (float) playerView.getWidth() / videoWidth;
                ViewGroup.LayoutParams layoutParams = playerView.getLayoutParams();
                layoutParams.height = (int) (videoHeight * widthFactor);
                playerView.setLayoutParams(layoutParams);
            }
        });
    }

    public void release() {
        Timber.i("release()");
        exoPlayer.release();
        exoPlayer = null;
    }

    public void playVideoInLoop(Uri videoUri) {
        // Measures bandwidth during playback. Can be null if not required.
        DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();

        // Produces DataSource instances through which media data is loaded.
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(playerView.getContext(),
                Util.getUserAgent(playerView.getContext(), playerView.getContext().getPackageName()),
                bandwidthMeter
        );

        // Produces Extractor instances for parsing the media data.
        ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();

        // This is the MediaSource representing the media to be played.
        MediaSource videoSource = new LoopingMediaSource(new ExtractorMediaSource(videoUri, dataSourceFactory, extractorsFactory, null, null));

        // Prepare the exoPlayer with the source.
        exoPlayer.prepare(videoSource);
        exoPlayer.setPlayWhenReady(true);
    }

    private abstract static class SimpleVideoRendererEventListener implements VideoRendererEventListener {
        @Override
        public void onVideoEnabled(DecoderCounters counters) {

        }

        @Override
        public void onVideoDecoderInitialized(String decoderName, long initializedTimestampMs, long initializationDurationMs) {

        }

        @Override
        public void onVideoInputFormatChanged(Format format) {

        }

        @Override
        public void onDroppedFrames(int count, long elapsedMs) {

        }

        @Override
        public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {

        }

        @Override
        public void onRenderedFirstFrame(Surface surface) {

        }

        @Override
        public void onVideoDisabled(DecoderCounters counters) {

        }
    }

}

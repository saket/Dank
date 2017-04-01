package me.saket.dank.utils;

import android.net.Uri;

import com.devbrackets.android.exomedia.ui.widget.VideoControlsMobile;
import com.devbrackets.android.exomedia.ui.widget.VideoView;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.LoopingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import me.saket.dank.ui.DankFragment;

public class ExoPlayerManager {

    private VideoView playerView;
    private OnVideoSizeChangeListener videoSizeChangeListener;

    public interface OnVideoSizeChangeListener {
        void onVideoSizeChange(int videoWidth, int videoHeight);
    }

    public static ExoPlayerManager newInstance(DankFragment fragment, VideoView playerView) {
        ExoPlayerManager exoPlayerManager = new ExoPlayerManager(playerView);

        exoPlayerManager.setupVideoView();

        fragment.lifecycleEvents().subscribe(event -> {
            switch (event) {
                case CREATE:
                    exoPlayerManager.setupVideoView();
                    break;

                case RESUME:
                    exoPlayerManager.resumeVideoPlayback();
                    break;

                case PAUSE:
                    exoPlayerManager.pauseVideoPlayback();
                    break;
            }
        });

        return exoPlayerManager;
    }

    public ExoPlayerManager(VideoView playerView) {
        this.playerView = playerView;
    }

    public void setOnVideoSizeChangeListener(OnVideoSizeChangeListener listener) {
        videoSizeChangeListener = listener;
    }

    private void setupVideoView() {
        playerView.setVideoSizeChangeListener((videoWidth, videoHeight) -> {
            if (videoSizeChangeListener != null) {
                float widthFactor = (float) playerView.getWidth() / videoWidth;
                int videoWidthAfterResize = (int) (videoWidth * widthFactor);
                int videoHeightAfterResize = (int) (videoHeight * widthFactor);
                videoSizeChangeListener.onVideoSizeChange(videoWidthAfterResize, videoHeightAfterResize);
            }
        });

        playerView.setControls(new VideoControlsMobile(playerView.getContext()));
    }

    public void playVideoInLoop(Uri videoUri) {
        // Produces DataSource instances through which media data is loaded.
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(playerView.getContext(),
                Util.getUserAgent(playerView.getContext(), playerView.getContext().getPackageName()),
                null
        );

        // Produces Extractor instances for parsing the media data.
        ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();

        MediaSource videoSource = new LoopingMediaSource(new ExtractorMediaSource(videoUri, dataSourceFactory, extractorsFactory, null, null));

        playerView.setVideoURI(videoUri, videoSource);
//        playerView.setOnPreparedListener(new OnPreparedListener() {
//            @Override
//            public void onPrepared() {
//                playerView.start();
//            }
//        });
    }

    private void pauseVideoPlayback() {
        playerView.pause();
    }

    private void resumeVideoPlayback() {
        playerView.start();
    }

}

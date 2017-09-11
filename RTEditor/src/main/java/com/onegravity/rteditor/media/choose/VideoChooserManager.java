/*
 * Copyright (C) 2015-2017 Emanuel Moecklin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.onegravity.rteditor.media.choose;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import com.onegravity.rteditor.R;
import com.onegravity.rteditor.api.RTMediaFactory;
import com.onegravity.rteditor.api.media.RTAudio;
import com.onegravity.rteditor.api.media.RTImage;
import com.onegravity.rteditor.api.media.RTVideo;
import com.onegravity.rteditor.media.MediaUtils;
import com.onegravity.rteditor.media.MonitoredActivity;
import com.onegravity.rteditor.media.choose.processor.VideoProcessor;
import com.onegravity.rteditor.media.choose.processor.VideoProcessor.VideoProcessorListener;
import com.onegravity.rteditor.utils.Constants.MediaAction;

import java.io.File;

class VideoChooserManager extends MediaChooserManager implements VideoProcessorListener {

    public interface VideoChooserListener extends MediaChooserListener {
        /**
         * Callback method to inform the caller that a video file has been processed
         */
        public void onVideoChosen(RTVideo video);
    }

    private static final String CAPTURED_VIDEO_TEMPLATE = "CAPTURED_VIDEO.mp4";

    private VideoChooserListener mListener;

    VideoChooserManager(MonitoredActivity activity, MediaAction mediaAction,
                        RTMediaFactory<RTImage, RTAudio, RTVideo> mediaFactory,
                        VideoChooserListener listener, Bundle savedInstanceState) {

        super(activity, mediaAction, mediaFactory, listener, savedInstanceState);

        mListener = listener;
    }

    @SuppressWarnings("incomplete-switch")
    @Override
    boolean chooseMedia() throws IllegalArgumentException {
        if (mListener == null) {
            throw new IllegalArgumentException("VideoChooserListener cannot be null");
        }
        switch (mMediaAction) {
            case CAPTURE_VIDEO:
                return captureVideo();
            case PICK_VIDEO:
                return pickVideo();
        }
        return false;
    }

    private boolean pickVideo() {
        Intent intent = new Intent(Intent.ACTION_PICK)
                .setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                .setType("video/*");
        String title = mActivity.getString(R.string.rte_pick_video);
        startActivity(Intent.createChooser(intent, title));
        return true;
    }

    private boolean captureVideo() {
        try {
            File videoPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            File videoFile = MediaUtils.createUniqueFile(videoPath, CAPTURED_VIDEO_TEMPLATE, false);
            videoPath.mkdirs();

            if (videoPath.exists() && videoPath.createNewFile()) {
                setOriginalFile(videoFile.getAbsolutePath());
                Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE)
                        .putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(getOriginalFile())));
                startActivity(intent);
            } else {
                Toast.makeText(mActivity, "Can't take picture without an sdcard", Toast.LENGTH_SHORT).show();
                return false;
            }
        } catch (Exception e) {
            Log.e(getClass().getSimpleName(), e.getMessage(), e);
        }

        return true;
    }

    @SuppressWarnings("incomplete-switch")
    @Override
    void processMedia(MediaAction mediaAction, Intent data) {
        switch (mediaAction) {
            case PICK_VIDEO:
                processPickedVideo(data);
                break;
            case CAPTURE_VIDEO:
                processCameraVideo(data);
                break;
        }
    }

    private void processPickedVideo(Intent data) {
        String originalFile = determineOriginalFile(data);
        if (originalFile != null) {
            startBackgroundJob(new VideoProcessor(originalFile, mMediaFactory, this));
        }
    }

    private void processCameraVideo(Intent intent) {
        String originalFile = getOriginalFile();
        if (originalFile != null) {
            startBackgroundJob(new VideoProcessor(originalFile, mMediaFactory, this));
        }
    }

    @Override
    /* VideoChooserListener */
    public void onVideoProcessed(RTVideo video) {
        if (mListener != null) {
            mListener.onVideoChosen(video);
        }
    }

}
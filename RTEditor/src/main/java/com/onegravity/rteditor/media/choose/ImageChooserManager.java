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
import com.onegravity.rteditor.media.choose.processor.ImageProcessor;
import com.onegravity.rteditor.media.choose.processor.ImageProcessor.ImageProcessorListener;
import com.onegravity.rteditor.utils.Constants.MediaAction;

import java.io.File;

class ImageChooserManager extends MediaChooserManager implements ImageProcessorListener {

    public interface ImageChooserListener extends MediaChooserListener {
        /**
         * Callback method to inform the caller that an image file has been processed
         */
        public void onImageChosen(RTImage image);
    }

    private static final String CAPTURED_IMAGE_TEMPLATE = "CAPTURED_IMAGE.jpeg";

    private ImageChooserListener mListener;

    ImageChooserManager(MonitoredActivity activity, MediaAction mediaAction,
                        RTMediaFactory<RTImage, RTAudio, RTVideo> mediaFactory,
                        ImageChooserListener listener, Bundle savedInstanceState) {

        super(activity, mediaAction, mediaFactory, listener, savedInstanceState);

        mListener = listener;
    }

    @SuppressWarnings("incomplete-switch")
    @Override
    boolean chooseMedia() throws IllegalArgumentException {
        if (mListener == null) {
            throw new IllegalArgumentException("ImageChooserListener cannot be null");
        }
        switch (mMediaAction) {
            case PICK_PICTURE:
                return pickPicture();
            case CAPTURE_PICTURE:
                return takePicture();
        }
        return false;
    }

    private boolean pickPicture() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT, null)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("image/*");
        String title = mActivity.getString(R.string.rte_pick_image);
        startActivity(Intent.createChooser(intent, title));
        return true;
    }

    // TODO: on Android M we need to ask the WRITE_EXTERNAL_STORAGE permission explicitly
    private boolean takePicture() {
        try {
            // Create an image file name (must be in "public area" or the camera app might not be able to access the file)
            File imagePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            File imageFile = MediaUtils.createUniqueFile(imagePath, CAPTURED_IMAGE_TEMPLATE, false);
            imagePath.mkdirs();
            if (imagePath.exists() && imageFile.createNewFile()) {
                setOriginalFile(imageFile.getAbsolutePath());
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                        .putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(imageFile));
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

            case PICK_PICTURE: {
                String originalFile = determineOriginalFile(data);
                if (originalFile != null) {
                    ImageProcessor processor = new ImageProcessor(originalFile, mMediaFactory, this);
                    startBackgroundJob(processor);
                }
                break;
            }

            case CAPTURE_PICTURE: {
                String originalFile = getOriginalFile();
                if (originalFile != null) {
                    ImageProcessor processor = new ImageProcessor(originalFile, mMediaFactory, this);
                    startBackgroundJob(processor);
                }
                break;
            }

        }
    }

    @Override
    /* ImageProcessorListener */
    public void onImageProcessed(RTImage image) {
        if (mListener != null) {
            mListener.onImageChosen(image);
        }
    }

}
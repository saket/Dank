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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.onegravity.rteditor.api.RTMediaFactory;
import com.onegravity.rteditor.api.format.RTFormat;
import com.onegravity.rteditor.api.media.RTAudio;
import com.onegravity.rteditor.api.media.RTImage;
import com.onegravity.rteditor.api.media.RTMedia;
import com.onegravity.rteditor.api.media.RTVideo;
import com.onegravity.rteditor.media.MonitoredActivity;
import com.onegravity.rteditor.media.choose.AudioChooserManager.AudioChooserListener;
import com.onegravity.rteditor.media.choose.ImageChooserManager.ImageChooserListener;
import com.onegravity.rteditor.media.choose.VideoChooserManager.VideoChooserListener;
import com.onegravity.rteditor.media.crop.CropImageActivity;
import com.onegravity.rteditor.utils.Constants;
import com.onegravity.rteditor.utils.Constants.MediaAction;

import org.greenrobot.eventbus.EventBus;

public class MediaChooserActivity extends MonitoredActivity implements
        ImageChooserListener,
        AudioChooserListener,
        VideoChooserListener {

    private static final String PREFIX = MediaChooserActivity.class.getSimpleName();

    public static final String EXTRA_MEDIA_ACTION = PREFIX + "EXTRA_MEDIA_ACTION";
    public static final String EXTRA_MEDIA_FACTORY = PREFIX + "EXTRA_MEDIA_FACTORY";

    private RTMediaFactory<RTImage, RTAudio, RTVideo> mMediaFactory;
    private MediaAction mMediaAction;

    transient private MediaChooserManager mMediaChooserMgr;

    private RTMedia mSelectedMedia;

    private static boolean mWorkInProgress;

    // ****************************************** Lifecycle Methods *******************************************

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String mediaAction = extras.getString(EXTRA_MEDIA_ACTION);
            mMediaAction = mediaAction == null ? null : MediaAction.valueOf(mediaAction);
            mMediaFactory = (RTMediaFactory<RTImage, RTAudio, RTVideo>) extras.getSerializable(EXTRA_MEDIA_FACTORY);
        }

        if (mMediaAction != null) {
            // retrieve parameters
            if (savedInstanceState != null) {
                mSelectedMedia = (RTMedia) savedInstanceState.getSerializable("mSelectedMedia");
            }

            switch (mMediaAction) {

                case PICK_PICTURE:
                case CAPTURE_PICTURE:
                    mMediaChooserMgr = new ImageChooserManager(this, mMediaAction, mMediaFactory, this, savedInstanceState);
                    break;

                case PICK_VIDEO:
                case CAPTURE_VIDEO:
                    mMediaChooserMgr = new VideoChooserManager(this, mMediaAction, mMediaFactory, this, savedInstanceState);
                    break;

                case PICK_AUDIO:
                case CAPTURE_AUDIO:
                    mMediaChooserMgr = new AudioChooserManager(this, mMediaAction, mMediaFactory, this, savedInstanceState);
                    break;
            }

            if (mMediaChooserMgr == null) {
                finish();
            } else if (!isWorkInProgress()) {
                setWorkInProgress(true);
                if (!mMediaChooserMgr.chooseMedia()) {
                    finish();
                }
            }
        } else {
            finish();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (mSelectedMedia != null) {
            outState.putSerializable("mSelectedMedia", mSelectedMedia);
        }
    }

    private void setWorkInProgress(boolean value) {
        mWorkInProgress = value;
    }

    private boolean isWorkInProgress() {
        return mWorkInProgress;
    }

    @Override
    public void finish() {
        super.finish();
        setWorkInProgress(false);
    }

    // ****************************************** Listener Methods *******************************************

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {

            if (requestCode == MediaAction.PICK_PICTURE.requestCode() && data != null) {
                mMediaChooserMgr.processMedia(MediaAction.PICK_PICTURE, data);
            } else if (requestCode == MediaAction.CAPTURE_PICTURE.requestCode()) {
                mMediaChooserMgr.processMedia(MediaAction.CAPTURE_PICTURE, data);    // data may be null here
            } else if (requestCode == Constants.CROP_IMAGE) {
                String path = data.getStringExtra(CropImageActivity.IMAGE_DESTINATION_FILE);
                if (path != null && mSelectedMedia instanceof RTImage) {
                    EventBus.getDefault().postSticky( new MediaEvent(mSelectedMedia) );
                    finish();
                }
            }

        } else {
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    @Override
    /* ImageChooserListener */
    public void onImageChosen(final RTImage image) {
        mSelectedMedia = image;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mMediaAction == MediaAction.CAPTURE_PICTURE) {
                    String filePath = image.getFilePath(RTFormat.SPANNED);

                    Intent intent = new Intent(MediaChooserActivity.this, CropImageActivity.class)

                            // tell CropImage activity to look for image to crop
                            .putExtra(CropImageActivity.IMAGE_SOURCE_FILE, filePath)
                            .putExtra(CropImageActivity.IMAGE_DESTINATION_FILE, filePath)

                                    // allow CropImage activity to re-scale image
                            .putExtra(CropImageActivity.SCALE, true)
                            .putExtra(CropImageActivity.SCALE_UP_IF_NEEDED, false)

                                    // no fixed aspect ratio
                            .putExtra(CropImageActivity.ASPECT_X, 0)
                            .putExtra(CropImageActivity.ASPECT_Y, 0);

                    // start activity CropImageActivity
                    startActivityForResult(intent, Constants.CROP_IMAGE);
                } else {
                    EventBus.getDefault().postSticky( new MediaEvent(mSelectedMedia) );
                    finish();
                }
            }
        });
    }

    @Override
    /* AudioChooserListener */
    public void onAudioChosen(RTAudio audio) {
        mSelectedMedia = audio;
        setWorkInProgress(false);
    }

    @Override
    /* VideoChooserListener */
    public void onVideoChosen(RTVideo video) {
        mSelectedMedia = video;
        setWorkInProgress(false);
    }

    @Override
    /* MediaChooserListener */
    public void onError(String reason) {
        Toast.makeText(this, reason, Toast.LENGTH_SHORT).show();
        setWorkInProgress(false);
    }

}

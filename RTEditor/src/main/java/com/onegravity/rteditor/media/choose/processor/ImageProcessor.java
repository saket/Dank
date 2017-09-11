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

package com.onegravity.rteditor.media.choose.processor;

import com.onegravity.rteditor.api.RTMediaFactory;
import com.onegravity.rteditor.api.media.RTAudio;
import com.onegravity.rteditor.api.media.RTImage;
import com.onegravity.rteditor.api.media.RTMediaSource;
import com.onegravity.rteditor.api.media.RTMediaType;
import com.onegravity.rteditor.api.media.RTVideo;

import java.io.IOException;
import java.io.InputStream;

public class ImageProcessor extends MediaProcessor {

    public interface ImageProcessorListener extends MediaProcessorListener {
        public void onImageProcessed(RTImage image);
    }

    private ImageProcessorListener mListener;

    public ImageProcessor(String originalFile, RTMediaFactory<RTImage, RTAudio, RTVideo> mediaFactory, ImageProcessorListener listener) {
        super(originalFile, mediaFactory, listener);
        mListener = listener;
    }

    @Override
    protected void processMedia() throws IOException, Exception {
        InputStream in = super.getInputStream();
        if (in == null) {
            if (mListener != null) {
                mListener.onError("No file found to process");
            }
        } else {
            RTMediaSource source = new RTMediaSource(RTMediaType.IMAGE, in, getOriginalFile(), getMimeType());
            RTImage image = mMediaFactory.createImage(source);
            if (image != null && mListener != null) {
                mListener.onImageProcessed(image);
            }
        }

    }

}
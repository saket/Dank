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

package com.onegravity.rteditor.api.media;

/**
 * This is a basic implementation of the RTVideo interface.
 */
public class RTVideoImpl extends RTMediaImpl implements RTVideo {
    private static final long serialVersionUID = 5966458878874846554L;

    private String mVideoPreviewImage;

    public RTVideoImpl(String filePath) {
        super(filePath);
    }

    @Override
    public void remove() {
        super.remove();
        removeFile(mVideoPreviewImage);
    }

    @Override
    public String getVideoPreviewImage() {
        return mVideoPreviewImage;
    }

    @Override
    public void setVideoPreviewImage(String videoPreviewImage) {
        mVideoPreviewImage = videoPreviewImage;
    }

    @Override
    public int getHeight() {
        return getHeight(mVideoPreviewImage);
    }

    @Override
    public int getWidth() {
        return getWidth(mVideoPreviewImage);
    }

}
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
 * This is a basic implementation of the RTAudio interface.
 */
public class RTAudioImpl extends RTMediaImpl implements RTAudio {
    private static final long serialVersionUID = -1213141231761752521L;

    private String mAudioPreviewImage;

    public RTAudioImpl(String filePath) {
        super(filePath);
    }

    @Override
    public void remove() {
        super.remove();
        removeFile(mAudioPreviewImage);
    }

    @Override
    public void setAudioPreviewImage(String audioPreviewImage) {
        mAudioPreviewImage = audioPreviewImage;
    }

    @Override
    public String getAudioPreviewImage() {
        return mAudioPreviewImage;
    }

    @Override
    public int getHeight() {
        return getHeight(mAudioPreviewImage);
    }

    @Override
    public int getWidth() {
        return getWidth(mAudioPreviewImage);
    }

}
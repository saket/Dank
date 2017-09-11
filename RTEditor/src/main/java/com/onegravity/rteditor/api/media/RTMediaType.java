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
 * The RTMediaType defines the specific type of a Media (image, video, audio, ...).
 * <p>
 * It's used by the RTMediaFactory to append a media type specific folder to the
 * file path where media files are stored. Images will be stored in <media file
 * path>/images/ while videos are stored in <media file path>/videos/. If no
 * media specific folders are needed, use a custom RTMediaFactory implementation
 * that ignores the media specific path.
 */
public enum RTMediaType {
    IMAGE("images"),
    VIDEO("videos"),
    AUDIO("audios");

    private String mMediaPath;

    RTMediaType(String mediaPath) {
        mMediaPath = mediaPath;
    }

    public String mediaPath() {
        return mMediaPath;
    }
}
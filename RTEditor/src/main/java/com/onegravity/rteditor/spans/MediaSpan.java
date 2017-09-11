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

package com.onegravity.rteditor.spans;

import com.onegravity.rteditor.api.RTApi;
import com.onegravity.rteditor.api.format.RTFormat;
import com.onegravity.rteditor.api.media.RTMedia;
import com.onegravity.rteditor.media.MediaUtils;

/**
 * A wrapper around android.text.style.ImageSpan that holds
 * the RTMedia object and whether the media was saved or not.
 */
public abstract class MediaSpan extends android.text.style.ImageSpan {

    final protected RTMedia mMedia;

    // when saving the text delete the Media if the MediaSpan was removed from the text
    // when dismissing the text delete the Media if the MediaSpan was removed from the text and if the Media wasn't saved
    final private boolean mIsSaved;

    public MediaSpan(RTMedia media, boolean isSaved) {
        super(RTApi.getApplicationContext(), MediaUtils.createFileUri(media.getFilePath(RTFormat.SPANNED)));
        mMedia = media;
        mIsSaved = isSaved;
    }

    public RTMedia getMedia() {
        return mMedia;
    }

    public boolean isSaved() {
        return mIsSaved;
    }

}
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

import java.io.InputStream;

/**
 * The RTMediaSource is used when a media is picked and inserted into the rich
 * text editor.
 * <p>
 * It contains meta information (name, mime type) and gives access to the
 * underlying InputStream through which the RTMediaFactory can create a Media
 * object which is then inserted into the editor. The creation of the Media
 * object requires some processing like copying the media to a dedicated media
 * storage area.
 * Splitting the actual copying of the file from retrieving the InputSource
 * (which includes picking the file and do some pre-processing e.g. when dealing
 * with ContentProviders) gives us the flexibility to implement any possible
 * storage scenario while keeping the "ugly stuff" under the hood.
 */
public class RTMediaSource {
    private final RTMediaType mMediaType;
    private final InputStream mIn;
    ;
    private final String mName;
    private final String mMimeType;

    public RTMediaSource(RTMediaType mediaType, InputStream in, String name, String mimeType) {
        mMediaType = mediaType;
        mIn = in;
        mName = name;
        mMimeType = mimeType;
    }

    public RTMediaType getMediaType() {
        return mMediaType;
    }

    public InputStream getInputStream() {
        return mIn;
    }

    public String getName() {
        return mName;
    }

    public String getMimeType() {
        return mMimeType;
    }

}
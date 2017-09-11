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

import com.onegravity.rteditor.api.media.RTMedia;

/**
 * This event is broadcast via EventBus when a media file has been selected.
 * It's received by the RTManager to insert the media into the active editor.
 */
public class MediaEvent {
    final private RTMedia mMedia;

    public MediaEvent(RTMedia selectedMedia) {
        mMedia = selectedMedia;
    }

    public RTMedia getMedia() {
        return mMedia;
    }
}

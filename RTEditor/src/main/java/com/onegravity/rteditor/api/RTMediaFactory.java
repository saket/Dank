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

package com.onegravity.rteditor.api;

import com.onegravity.rteditor.api.media.RTAudio;
import com.onegravity.rteditor.api.media.RTImage;
import com.onegravity.rteditor.api.media.RTMediaSource;
import com.onegravity.rteditor.api.media.RTVideo;

import java.io.Serializable;

/**
 * The MediaFactory creates Media objects.
 * <p>
 * By overriding this class (and possibly the Media classes) different storage
 * scenarios can be implemented.
 *
 * @see RTMediaFactoryImpl
 */
public interface RTMediaFactory<I extends RTImage, A extends RTAudio, V extends RTVideo> extends Serializable {

    /**
     * Use case 1: Inserting media objects into the rich text editor.
     * <p>
     * In this case media objects are picked by the user to insert them into the
     * editor. The media objects need to be copied to a dedicated media storage
     * area because the app needs access to them even if the source object isn't
     * available any more (deleted or no access any more).
     */
    public I createImage(RTMediaSource mediaSource);

    public A createAudio(RTMediaSource mediaSource);

    public V createVideo(RTMediaSource mediaSource);

    /**
     * Use case 2: Load a rich text with referenced media objects into the rich
     * text editor.
     * <p>
     * In this case media objects are loaded from the media storage area when
     * rich text is parsed and converted to a Spanned text. Media objects are
     * somehow referenced in the rich text. These reference need to be
     * translated into an absolute file path to be used in an ImageSpan.
     * <p>
     * E.g for editing purposes we would use an absolute path like
     * /data/data/com.package/files/image.png
     * and we'd store the text in a html file with references like
     * href="xmedia://image.png"
     * <p>
     * This allows us to save text in a storage independent format e.g. if we
     * want to switch between internal or external storage (copying the media
     * files) or if we want to use more than one storage area depending on the
     * use case (e.g. ContentProvider vs. SQLite database).
     */
    public I createImage(String path);

    public A createAudio(String path);

    public V createVideo(String path);

}
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

package com.onegravity.rteditor.api.format;

import com.onegravity.rteditor.api.RTMediaFactory;
import com.onegravity.rteditor.api.media.RTAudio;
import com.onegravity.rteditor.api.media.RTImage;
import com.onegravity.rteditor.api.media.RTVideo;
import com.onegravity.rteditor.converter.ConverterHtmlToSpanned;
import com.onegravity.rteditor.converter.ConverterHtmlToText;

import java.util.ArrayList;
import java.util.List;

/**
 * RTText representing an html text.
 * <p>
 * The text may contain referenced images.
 * Audio and video files aren't supported yet.
 */
public class RTHtml<I extends RTImage, A extends RTAudio, V extends RTVideo> extends RTText {

    private List<I> mImages;

    public RTHtml(CharSequence html) {
        this(RTFormat.HTML, html);
    }

    public RTHtml(RTFormat.Html rtFormat, CharSequence html) {
        this(rtFormat, html, new ArrayList<I>());
    }

    public RTHtml(RTFormat.Html rtFormat, CharSequence html, List<I> images) {
        super(rtFormat, html);
        mImages = images;
    }

    @Override
    public String getText() {
        CharSequence text = super.getText();
        return text != null ? text.toString() : "";
    }

    public List<I> getImages() {
        return mImages;
    }

    @Override
    public RTText convertTo(RTFormat destFormat, RTMediaFactory<RTImage, RTAudio, RTVideo> mediaFactory) {
        if (destFormat instanceof RTFormat.PlainText) {
            return ConverterHtmlToText.convert(this);
        } else if (destFormat instanceof RTFormat.Spanned) {
            return new ConverterHtmlToSpanned().convert(this, mediaFactory);
        }

        return super.convertTo(destFormat, mediaFactory);
    }

}
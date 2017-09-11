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

import android.text.Editable;
import android.view.inputmethod.BaseInputConnection;

import com.onegravity.rteditor.RTEditText;
import com.onegravity.rteditor.api.RTMediaFactory;
import com.onegravity.rteditor.api.media.RTAudio;
import com.onegravity.rteditor.api.media.RTImage;
import com.onegravity.rteditor.api.media.RTVideo;
import com.onegravity.rteditor.converter.ConverterSpannedToHtml;
import com.onegravity.rteditor.effects.Effects;

/**
 * RTText representing rich text in android.text.Spanned format.
 *
 * Use this class if the source text is an RTEditText.
 * This allows to pre-process the text before converting it (e.g. to eliminate
 * certain spans and to clean up the paragraph formatting).
 *
 * @see RTSpanned
 */
public final class RTEditable extends RTSpanned {

    private RTEditText mEditor;

    public RTEditable(RTEditText editor) {
        super(editor.getText());
        mEditor = editor;
    }

    @Override
    public RTText convertTo(RTFormat destFormat, RTMediaFactory<RTImage, RTAudio, RTVideo> mediaFactory) {
        if (destFormat instanceof RTFormat.Html) {
            clean();
            return new ConverterSpannedToHtml().convert(mEditor.getText(), (RTFormat.Html) destFormat);
        } else if (destFormat instanceof RTFormat.PlainText) {
            clean();
            RTHtml<RTImage, RTAudio, RTVideo> rtHtml = new ConverterSpannedToHtml().convert(mEditor.getText(), RTFormat.HTML);
            RTText rtText = rtHtml.convertTo(RTFormat.PLAIN_TEXT, mediaFactory);
            return new RTPlainText(rtText.getText());
        }

        return super.convertTo(destFormat, mediaFactory);
    }

    private void clean() {
        Editable text = mEditor.getText();
        BaseInputConnection.removeComposingSpans(text);

        /*
         Cleanup ParagraphStyles to:
          - make sure spans are applied to whole paragraphs
          - remove obsolete spans
          - Note: the sequence is important
        */
        Effects.cleanupParagraphs(mEditor);
    }
}
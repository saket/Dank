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

package com.onegravity.rteditor.converter;

import android.text.style.ParagraphStyle;

import com.onegravity.rteditor.spans.IndentationSpan;
import com.onegravity.rteditor.utils.Helper;

/*
 * This is a helper class for converting from Spanned to HTML and back.
 * It's the paragraph style for a single line.
 */
public class SingleParagraphStyle implements ParagraphStyle {
    final private ParagraphType mType;
    final private ParagraphStyle mStyle;

    public SingleParagraphStyle(ParagraphType type, ParagraphStyle style) {
        mType = type;
        mStyle = style;
    }

    public int getIndentation() {
        if (mType.isIndentation()) {
            float margin = Helper.getLeadingMarging();
            float indentation = ((IndentationSpan) mStyle).getValue();
            return Math.round(indentation / margin);
        } else if (mType.isBullet() || mType.isNumbering()) {
            return 1;
        }
        return 0;
    }

    public ParagraphType getType() {
        return mType;
    }

    public ParagraphStyle getStyle() {
        return mStyle;
    }

    @Override
    public String toString() {
        return mType.name() + " - " + mStyle.getClass().getSimpleName();
    }
}
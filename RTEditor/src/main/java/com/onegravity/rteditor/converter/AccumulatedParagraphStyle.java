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

/*
 * This is a helper class for converting from Spanned to HTML and back.
 * It's the accumulated paragraph styles (all SingleParagraphStyle of a paragraph together)
 */
public class AccumulatedParagraphStyle {
    final private ParagraphType mType;
    private int mAbsoluteIndent;
    private int mRelativeIndent;

    public AccumulatedParagraphStyle(ParagraphType type, int absoluteIndent, int relativeIndent) {
        mType = type;
        mAbsoluteIndent = absoluteIndent;
        mRelativeIndent = relativeIndent;
    }

    public ParagraphType getType() {
        return mType;
    }

    public int getAbsoluteIndent() {
        return mAbsoluteIndent;
    }

    public int getRelativeIndent() {
        return mRelativeIndent;
    }

    public void setAbsoluteIndent(int absoluteIndent) {
        mAbsoluteIndent = absoluteIndent;
    }

    public void setRelativeIndent(int relativeIndent) {
        mRelativeIndent = relativeIndent;
    }

    @Override
    public String toString() {
        return mType.name() + " - " + mAbsoluteIndent + "/" + mRelativeIndent;
    }
}

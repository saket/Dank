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

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.Layout;
import android.text.Spanned;
import android.text.style.LeadingMarginSpan;

/**
 * Paragraph numbering.
 * <p>
 * Android seems to add the leading margin for an empty paragraph to the previous paragraph
 * (]0, 4][4, 4] --> the leading margin of the second span is added to the ]0, 4] paragraph regardless of the Spanned.flags)
 * --> therefore we ignore the leading margin for the last, empty paragraph unless it's the only one
 */
public class NumberSpan implements LeadingMarginSpan, RTSpan<Boolean>, RTParagraphSpan<Boolean> {

    private final int mNr;
    private final int mGapWidth;
    private final boolean mIgnoreSpan;

    private float mTextSize = 10f;
    private float mWidth;

    public NumberSpan(int nr, int gapWidth, boolean isEmpty, boolean isFirst, boolean isLast) {
        mNr = nr;
        mGapWidth = gapWidth;
        mIgnoreSpan = isEmpty && isLast && !isFirst;
    }

    private NumberSpan(int nr, int gapWidth, boolean ignoreSpan) {
        mNr = nr;
        mGapWidth = gapWidth;
        mIgnoreSpan = ignoreSpan;
    }

    @Override
    public int getLeadingMargin(boolean first) {
        return mIgnoreSpan ? 0 : Math.max(Math.round(mWidth + 2), mGapWidth);
    }

    @Override
    public void drawLeadingMargin(Canvas c, Paint p, int x, int dir, int top, int baseline, int bottom,
                                  CharSequence text, int start, int end, boolean first, Layout l) {

        Spanned spanned = (Spanned) text;
        if (!mIgnoreSpan && spanned.getSpanStart(this) == start) {
            // set paint
            Paint.Style oldStyle = p.getStyle();
            float oldTextSize = p.getTextSize();
            p.setStyle(Paint.Style.FILL);
            mTextSize = baseline - top;
            p.setTextSize(mTextSize);
            mWidth = p.measureText(mNr + ".");

            // draw the number
            c.drawText(mNr + ".", x, baseline, p);

            // restore paint
            p.setStyle(oldStyle);
            p.setTextSize(oldTextSize);
        }
    }

    @Override
    public Boolean getValue() {
        return Boolean.TRUE;
    }

    @Override
    public NumberSpan createClone() {
        return new NumberSpan(mNr, mGapWidth, mIgnoreSpan);
    }

}

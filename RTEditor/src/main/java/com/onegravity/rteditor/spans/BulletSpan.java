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
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.text.Layout;
import android.text.Spanned;
import android.text.style.LeadingMarginSpan;

/**
 * Implementation for a bullet point LeadingMarginSpan
 * <p>
 * Android seems to add the leading margin for an empty paragraph to the
 * previous paragraph (]0, 4][4, 4] --> the leading margin of the second span is
 * added to the ]0, 4] paragraph regardless of the Spanned.flags) --> therefore
 * we ignore the leading margin for the last, empty paragraph unless it's the
 * only one
 */
public class BulletSpan implements LeadingMarginSpan, RTSpan<Boolean>, RTParagraphSpan<Boolean> {

    private static Path sBulletPath = null;

    private final int mGapWidth;
    private final boolean mIgnoreSpan;

    public BulletSpan(int gapWidth, boolean isEmpty, boolean isFirst, boolean isLast) {
        mGapWidth = gapWidth;
        mIgnoreSpan = isEmpty && isLast && !isFirst;
        if (sBulletPath == null) {
            sBulletPath = new Path();
        }
    }

    private BulletSpan(int gapWidth, boolean ignoreSpan) {
        mGapWidth = gapWidth;
        mIgnoreSpan = ignoreSpan;
    }

    @Override
    public int getLeadingMargin(boolean first) {
        return mIgnoreSpan ? 0 : mGapWidth;
    }

    @Override
    public void drawLeadingMargin(Canvas c, Paint p, int x, int dir, int top, int baseline, int bottom,
                                  CharSequence text, int start, int end, boolean first, Layout l) {
        Spanned spanned = (Spanned) text;
        if (!mIgnoreSpan && spanned.getSpanStart(this) == start) {
            // set paint
            Paint.Style oldStyle = p.getStyle();
            p.setStyle(Paint.Style.FILL);

            // draw the bullet point
            int size = Math.max(Math.round((baseline - top) / 9f), 4);
            draw(c, p, x, dir, top, bottom, size);

            // restore paint
            p.setStyle(oldStyle);
        }
    }

    private void draw(Canvas c, Paint p, int x, int dir, int top, int bottom, int size) {
        sBulletPath.reset();
        sBulletPath.addCircle(0.0f, 0.0f, size, Direction.CW);

        c.save();
        c.translate(x + dir * size, (top + bottom) / 2.0f);
        c.drawPath(sBulletPath, p);
        c.restore();
    }

    @Override
    public Boolean getValue() {
        return Boolean.TRUE;
    }

    @Override
    public BulletSpan createClone() {
        return new BulletSpan(mGapWidth, mIgnoreSpan);
    }

}

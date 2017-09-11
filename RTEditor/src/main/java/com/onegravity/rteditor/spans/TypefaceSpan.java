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

import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.TextPaint;

import com.onegravity.rteditor.fonts.RTTypeface;

/**
 * Custom TypefaceSpan class to add support for fonts
 */
public class TypefaceSpan extends android.text.style.TypefaceSpan implements RTSpan<RTTypeface> {

    private final RTTypeface mTypeface;

    public TypefaceSpan(RTTypeface typeface) {
        super("");
        mTypeface = typeface;
    }

    @Override
    public void updateDrawState(TextPaint paint) {
        applyCustomTypeFace(paint, mTypeface.getTypeface());
    }

    @Override
    public void updateMeasureState(TextPaint paint) {
        applyCustomTypeFace(paint, mTypeface.getTypeface());
    }

    private void applyCustomTypeFace(Paint paint, Typeface tf) {
        Typeface old = paint.getTypeface();
        int oldStyle = old == null ? 0 : old.getStyle();

        int fake = oldStyle & ~tf.getStyle();
        if ((fake & Typeface.BOLD) != 0) {
            paint.setFakeBoldText(true);
        }

        if ((fake & Typeface.ITALIC) != 0) {
            paint.setTextSkewX(-0.25f);
        }

        paint.setTypeface(tf);
    }

    @Override
    public RTTypeface getValue() {
        return mTypeface;
    }

}
